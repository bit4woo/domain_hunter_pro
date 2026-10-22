package dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * 一个项目的全部 DAO 集合（DomainDao / TargetDao / TitleDao）。
 * <p>
 * 为什么需要它：
 * </p>
 * <ul>
 *   <li>三个 DAO 必须共用<b>同一个 DataSource</b>，否则 PRAGMA 配置、连接参数会各不相同；</li>
 *   <li>批量写入需要一个"能拿到连接"的地方，用来把三张表的写操作放进同一个事务；</li>
 *   <li>DAO 实例可以懒加载：只打开过域名的项目不需要建 TitleTable。</li>
 * </ul>
 * <p>
 * 对外只通过 {@link DaoFactory#of} 获取，不要自己 new。
 * </p>
 */
public final class ProjectDaos {

	private final String dbFilePath;

	// 懒加载 + 双重检查：DAO 构造时会检表/建表，能省就省
	private volatile DomainDao domainDao;
	private volatile TargetDao targetDao;
	private volatile TitleDao titleDao;

	ProjectDaos(String dbFilePath) {
		this.dbFilePath = dbFilePath;
	}

	public String getDbFilePath() {
		return dbFilePath;
	}

	public DomainDao domainDao() {
		DomainDao local = domainDao;
		if (local == null) {
			synchronized (this) {
				local = domainDao;
				if (local == null) {
					local = new DomainDao(dbFilePath);
					domainDao = local;
				}
			}
		}
		return local;
	}

	public TargetDao targetDao() {
		TargetDao local = targetDao;
		if (local == null) {
			synchronized (this) {
				local = targetDao;
				if (local == null) {
					local = new TargetDao(dbFilePath);
					targetDao = local;
				}
			}
		}
		return local;
	}

	public TitleDao titleDao() {
		TitleDao local = titleDao;
		if (local == null) {
			synchronized (this) {
				local = titleDao;
				if (local == null) {
					local = new TitleDao(dbFilePath);
					titleDao = local;
				}
			}
		}
		return local;
	}

	/**
	 * 取一个连接，专门给"批量写入 + 单事务"使用。
	 * <p>
	 * 调用方负责 setAutoCommit/commit/rollback/close。
	 * 每次批量写入开一条短连接、写完即关，而不是长期持有：
	 * 长期持有会让本进程一直占用 .db 文件句柄，在 Windows 上会导致
	 * 用户无法删除/重命名/用其他工具独占打开项目文件。
	 * </p>
	 */
	public Connection getConnection() throws SQLException {
		DataSource ds = DBUtils.getSqliteDataSource(dbFilePath);
		if (ds == null) {
			throw new SQLException("database file not found: " + dbFilePath);
		}
		return ds.getConnection();
	}
}
