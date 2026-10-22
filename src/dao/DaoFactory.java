package dao;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO 工厂 —— 全项目获取 DAO 的唯一入口。
 * <p>
 * 解决的问题（改造前有 15 处 {@code new DomainDao/TargetDao/TitleDao(...)}）：
 * </p>
 * <ul>
 *   <li>每 new 一次就要走一遍构造逻辑：检查 sqlite_master、必要时建表、
 *       必要时跑 PRAGMA/ALTER，都是真实的数据库往返；</li>
 *   <li>同一个文件被多个 DAO 实例、多套 DataSource 持有，PRAGMA 与连接参数不一致；</li>
 *   <li>调用点分散后，谁在什么时候打开了数据库变得不可追踪（例如每敲一个键就 new 一个 DomainDao）。</li>
 * </ul>
 * <p>
 * 缓存以<b>数据库文件</b>为 key：同一个文件永远对应同一个 {@link ProjectDaos}；
 * 新建项目/另存为/导入另一个项目时自然会得到新的实例，互不干扰。
 * 切项目时<b>不需要</b>清理缓存——旧项目的 DAO 持有的是旧文件路径，
 * 残留的写入任务也只会写回它原本的项目文件，不会污染新项目。
 * </p>
 */
public final class DaoFactory {

	private static final Map<String, ProjectDaos> CACHE = new ConcurrentHashMap<String, ProjectDaos>();

	private DaoFactory() {
	}

	public static ProjectDaos of(File dbFile) {
		if (dbFile == null) {
			throw new IllegalArgumentException("dbFile is null");
		}
		return of(dbFile.getPath());
	}

	public static ProjectDaos of(String dbFilePath) {
		if (dbFilePath == null || dbFilePath.trim().isEmpty()) {
			throw new IllegalArgumentException("dbFilePath is empty");
		}
		String key = normalize(dbFilePath);
		ProjectDaos cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		synchronized (DaoFactory.class) {
			cached = CACHE.get(key);
			if (cached != null) {
				return cached;
			}
			ProjectDaos created = new ProjectDaos(key);
			CACHE.put(key, created);
			return created;
		}
	}

	/**
	 * 删除/重命名数据库文件之前调用，同时清掉 DataSource 与 DAO 缓存。
	 * <p>
	 * 必须先 {@link DBUtils#checkpoint(String)} 再 evict：否则 WAL 里的内容还在，
	 * 文件被删/改名后就彻底丢了。
	 * </p>
	 */
	public static void evict(File dbFile) {
		if (dbFile == null) {
			return;
		}
		String key = normalize(dbFile.getPath());
		CACHE.remove(key);
		DBUtils.evict(key);
	}

	private static String normalize(String dbFilePath) {
		try {
			return new File(dbFilePath).getCanonicalPath();
		} catch (IOException e) {
			return new File(dbFilePath).getAbsolutePath();
		}
	}
}
