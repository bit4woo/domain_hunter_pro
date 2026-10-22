package dao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * SQLite 连接入口。
 * <p>
 * 为什么要有这个类（而不是每个 DAO 里 new 一个 DataSource）：
 * </p>
 * <ol>
 *   <li><b>PRAGMA 必须统一设置</b>。默认的 SQLite 连接是 delete journal + 无 busy_timeout，
 *       只要有第二个线程在写，就会立刻抛 SQLITE_BUSY。这里统一开启
 *       WAL（1 写 N 读并发）、busy_timeout=5000（拿不到锁时等待而不是立刻失败）、
 *       synchronous=NORMAL（WAL 下的推荐档位，兼顾安全与速度）。</li>
 *   <li><b>同一个数据库文件只应有一个 DataSource</b>。原来的实现每处 {@code new DomainDao(...)}
 *       都会新建一个 DataSource，一次键入就会建一个；连接参数、PRAGMA 也各管各的。</li>
 * </ol>
 * <p>
 * 注意：这里缓存的是 DataSource，不是 Connection。DataSource 是轻量的，真正宝贵的
 * PRAGMA 配置挂在它上面；每次 getConnection 都会带上同样的配置。
 * </p>
 */
public class DBUtils {

	/** key = 数据库文件的规范化绝对路径，value = 已配置好 PRAGMA 的 DataSource */
	private static final Map<String, DataSource> DATA_SOURCES = new ConcurrentHashMap<String, DataSource>();

	/**
	 * 获取某个数据库文件对应的 DataSource（带缓存）。
	 *
	 * @param dbFilePath 数据库文件路径
	 * @return 文件不存在时返回 null（沿用旧行为，由调用方决定如何处理）
	 */
	public static DataSource getSqliteDataSource(String dbFilePath) {
		if (dbFilePath == null) {
			return null;
		}
		File file = new File(dbFilePath);
		if (!file.exists()) {
			return null;
		}
		String key = normalize(dbFilePath);
		DataSource cached = DATA_SOURCES.get(key);
		if (cached != null) {
			return cached;
		}
		synchronized (DBUtils.class) {
			cached = DATA_SOURCES.get(key);
			if (cached != null) {
				return cached;
			}
			DataSource created = create(key);
			DATA_SOURCES.put(key, created);
			return created;
		}
	}

	/**
	 * 规范化路径作为缓存 key。
	 * <p>
	 * 用 canonical path 而不是 raw path：同一个文件可能以 {@code a.db}、{@code ./a.db}、
	 * 大小写不同的形式传进来，用原始字符串会导致缓存里出现多个条目、开多套连接。
	 * </p>
	 */
	private static String normalize(String dbFilePath) {
		try {
			return new File(dbFilePath).getCanonicalPath();
		} catch (IOException e) {
			return new File(dbFilePath).getAbsolutePath();
		}
	}

	/**
	 * 创建带 PRAGMA 的 DataSource。
	 * <p>
	 * 会先用一次真实连接验证配置是否生效：WAL 依赖文件系统支持（网络盘、某些只读介质不支持），
	 * 失败时回退到默认配置，保证"能打开"优先于"性能好"。
	 * </p>
	 */
	private static DataSource create(String dbFilePath) {
		String url = "jdbc:sqlite:" + dbFilePath;
		try {
			SQLiteConfig config = new SQLiteConfig();
			config.setJournalMode(SQLiteConfig.JournalMode.WAL);
			config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
			config.setBusyTimeout(5000);

			SQLiteDataSource ds = new SQLiteDataSource(config);
			ds.setUrl(url);
			// 触发一次连接，让 xerial 应用上面的 PRAGMA；失败会在这里抛出
			try (Connection conn = ds.getConnection()) {
				// do nothing, just validate
			}
			return ds;
		} catch (Exception e) {
			System.err.println("[DBUtils] WAL mode unavailable for " + dbFilePath + ", fallback to default: "
					+ e.getMessage());
			SQLiteDataSource plain = new SQLiteDataSource();
			plain.setUrl(url);
			return plain;
		}
	}

	/**
	 * 把 WAL 里尚未合并的内容写回主数据库文件，并截断 WAL。
	 * <p>
	 * 为什么需要它：开启 WAL 后，最近提交的数据可能还在 {@code xxx.db-wal} 里，
	 * 主文件本身并不完整。此时直接 FileUtils.copyFile 备份、或者把 .db 拷走交给别的工具，
	 * 会得到"缺最后几条数据"的副本。备份/重命名/删除数据库文件前必须先调用这个方法。
	 * </p>
	 *
	 * @return true 表示 checkpoint 成功执行
	 */
	public static boolean checkpoint(String dbFilePath) {
		DataSource ds = getSqliteDataSource(dbFilePath);
		if (ds == null) {
			return false;
		}
		try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
			stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
			return true;
		} catch (Exception e) {
			System.err.println("[DBUtils] wal_checkpoint failed for " + dbFilePath + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * 从缓存里移除某个数据库文件的 DataSource。
	 * <p>
	 * 删除/重命名数据库文件之前调用：否则缓存里还拿着这个文件的配置，
	 * 之后同名文件重建时会复用旧条目（对 SQLite 影响不大，但会让排查问题变复杂）。
	 * </p>
	 */
	public static void evict(String dbFilePath) {
		if (dbFilePath == null) {
			return;
		}
		DATA_SOURCES.remove(normalize(dbFilePath));
	}
}
