package dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import base.IndexedHashMap;
import base.SetAndStr;
import domain.target.TargetEntry;

public class TargetDao {

	/**
	 * 单行 upsert 的 SQL。
	 * <p>
	 * 用 INSERT ... ON CONFLICT(target) DO UPDATE 而不是 insert or replace：
	 * replace 的语义是"删掉旧行再插新行"，会换掉主键 ID、改变行序，
	 * 而行序是界面表格"#"列的隐含依赖。ON CONFLICT 是原地更新，行序稳定。
	 * </p>
	 */
	private static final String UPSERT_SQL = "INSERT INTO TargetTable (target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel, subDomainCount,digDone)"
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
			+ " ON CONFLICT(target) DO UPDATE SET type = excluded.type, keyword = excluded.keyword, "
			+ " ZoneTransfer = excluded.ZoneTransfer, comment = excluded.comment, "
			+ " useTLD = excluded.useTLD, trustLevel = excluded.trustLevel, subDomainCount = excluded.subDomainCount, digDone=excluded.digDone";

	private static final String DELETE_SQL = "delete from TargetTable where target=?";

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	/** 本 DAO 对应的数据库文件路径，供批量写入/日志使用 */
	private final String dbFilePath;

	public TargetDao(String dbFilePath) {
		this.dbFilePath = dbFilePath;
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!TableUpdater.tableExists(jdbcTemplate, "TargetTable")) {
			createTable();
		} else {
			alterTable();
		}
	}

	public String getDbFilePath() {
		return dbFilePath;
	}

	public TargetDao(File dbFilePath) {
		this(dbFilePath.toString());
	}

	public static String genCreateTableSql(String tableName) {
		// 使用和旧版本不同的表名称,避免冲突
		String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " "
				+ "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + // 自动增长 https://www.sqlite.org/autoinc.html
				" target           TEXT    NOT NULL UNIQUE," + // -- UNIQUE关键词 为 target 字段添加唯一约束，这样INSERT OR
																// REPLACE语句才能根据这段进行
				" type        TEXT    NOT NULL," 
				+ " keyword        TEXT    NOT NULL,"
				+ " ZoneTransfer        BOOLEAN    NOT NULL," 
				+ " trustLevel        TEXT    NOT NULL," 
				+ " comment        TEXT    NOT NULL,"
				+ " useTLD        BOOLEAN    NOT NULL," 
				+ " digDone        BOOLEAN    NOT NULL,"
				+ " subDomainCount	INTEGER)";
		return createTableSql;
	}

	public boolean createTable() {

		jdbcTemplate.execute(genCreateTableSql("TargetTable"));

		// https://www.sqlitetutorial.net/sqlite-replace-statement/
		// 让target字段成为replace操作判断是否重复的判断条件。
		String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Target_target ON TargetTable (target)";
		jdbcTemplate.execute(sqlcreateIndex);
		return true;
	}

	public void alterTable() {

		String createNewTableSql = TargetDao.genCreateTableSql("TargetTable_new");
		List<String> newTableColumnNames = TableUpdater.extractColumnNames(createNewTableSql);

		List<String> oldTableColumnNames = TableUpdater.getTableColumns(jdbcTemplate, "TargetTable");

		List<String> added = TableUpdater.getAddedColumnNames(oldTableColumnNames, newTableColumnNames);

		if (added.size() > 0) {

			Map<String, String> columnMappings = buildColumnMappings(oldTableColumnNames, newTableColumnNames);

			String copyDataSql = TableUpdater.genMigrateDataSql("TargetTable", "TargetTable_new", columnMappings);

			String dropOldTableSql = "DROP TABLE TargetTable";

			String renameTableSql = "ALTER TABLE TargetTable_new RENAME TO TargetTable";

			String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Target_target ON TargetTable (target)";
			try {
				jdbcTemplate.execute(createNewTableSql);
				jdbcTemplate.execute(copyDataSql);
				jdbcTemplate.execute(dropOldTableSql);
				jdbcTemplate.execute(renameTableSql);
				jdbcTemplate.execute(sqlcreateIndex);

			} catch (DataAccessException e) {
				e.printStackTrace();
			}
		}
	}

	// 创建表结构和字段迁移
	public Map<String, String> buildColumnMappings(List<String> oldTableColumnNames, List<String> newTableColumnNames) {

		// 构建字段映射
		Map<String, String> columnMappings = new HashMap<>();

		for (String columnNameOfNewTable : newTableColumnNames) {
			if (oldTableColumnNames.contains(columnNameOfNewTable)) {
				columnMappings.put(columnNameOfNewTable, columnNameOfNewTable);
			} else {

				// 第一次修改：删除isBlack字段，新增trustLevel字段的逻辑。需要确保旧表有isBlack字段。
				if (columnNameOfNewTable.equals("trustLevel")) {
					if (oldTableColumnNames.contains(columnNameOfNewTable)) {
						// 特殊处理 "isBlack" 字段 -> 映射到 "trustLevel"
						columnMappings.put("trustLevel",
								"CASE WHEN isBlack = 1 THEN 'NonTarget' ELSE 'Maybe' END as trustLevel");
					} else {

						columnMappings.put("trustLevel", "'Maybe' as trustLevel");
					}

				} else if (columnNameOfNewTable.equals("subDomainCount")) {

					columnMappings.put("subDomainCount", "0 as subDomainCount");

				} else if (columnNameOfNewTable.equals("digDone")) {

					columnMappings.put("digDone", "false as digDone");

				} else {
					System.out.println(columnNameOfNewTable + " mapping logic missed!!!");
				}
			}
		}

		return columnMappings;

	}


	/**
	 * 不再使用isBlack字段，由trustLevel字段代替
	 * 
	 * @param entry
	 * @return
	 * 
	 *         insert or replace 的工作机制: 查找匹配主键 或者 唯一约束列 的记录。
	 *         如果找到匹配记录，则删除这条记录，然后插入新的记录。 如果没有匹配记录，则直接插入新的记录。
	 *         由于删除后再插入的过程会生成新的记录，因此数据实际上是“创建了新的记录”，而不是直接修改现有记录。
	 */
	@Deprecated
	public boolean addOrUpdateTarget_old(TargetEntry entry) {
		String sql = "insert or replace into TargetTable (target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel,subDomainCount)"
				+ " values(?, ?, ?, ?, ?, ?, ?,?)";

		int result = jdbcTemplate.update(sql, entry.getTarget(), entry.getType(), entry.getKeyword(),
				entry.isZoneTransfer(), SetAndStr.toStr(entry.getComments()), entry.isUseTLD(), entry.getTrustLevel(),
				entry.getSubdomainCount());

		return result > 0;
	}

	public boolean addOrUpdateTarget(TargetEntry entry) {
		int result = jdbcTemplate.update(UPSERT_SQL, entry.getTarget(), entry.getType(), entry.getKeyword(),
				entry.isZoneTransfer(), SetAndStr.toStr(entry.getComments()), entry.isUseTLD(), entry.getTrustLevel(),
				entry.getSubdomainCount(),entry.isDigDone());

		return result > 0;
	}

	/**
	 * 批量 upsert，由调用方负责事务。
	 * <p>
	 * 为什么需要它：原实现 {@link #addOrUpdateTargets} 是"循环调用单行 upsert"，
	 * 每行都是一次独立事务（SQLite 默认每条语句一次隐式提交），几千个 target 就是几千次提交。
	 * 这里用 PreparedStatement.addBatch 把整批塞进一个事务，提交次数从 O(n) 降到 O(1)。
	 * </p>
	 *
	 * @param conn    外部管理的连接，本方法不 commit、不 close
	 * @param entries 待写入的 target 快照
	 * @return 写入行数
	 */
	public int upsertBatch(Connection conn, List<TargetEntry> entries) throws SQLException {
		if (entries == null || entries.isEmpty()) {
			return 0;
		}
		int count = 0;
		try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
			for (TargetEntry entry : entries) {
				ps.setString(1, entry.getTarget());
				ps.setString(2, entry.getType());
				ps.setString(3, entry.getKeyword());
				ps.setBoolean(4, entry.isZoneTransfer());
				ps.setString(5, SetAndStr.toStr(entry.getComments()));
				ps.setBoolean(6, entry.isUseTLD());
				ps.setString(7, entry.getTrustLevel());
				ps.setInt(8, entry.getSubdomainCount());
				ps.setBoolean(9, entry.isDigDone());
				ps.addBatch();
				count++;
			}
			ps.executeBatch();
		}
		return count;
	}

	/**
	 * 批量删除，由调用方负责事务。
	 *
	 * @param conn    外部管理的连接，本方法不 commit、不 close
	 * @param targets 待删除的 target 值
	 * @return 提交的删除语句条数
	 */
	public int deleteBatch(Connection conn, List<String> targets) throws SQLException {
		if (targets == null || targets.isEmpty()) {
			return 0;
		}
		int count = 0;
		try (PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
			for (String target : targets) {
				ps.setString(1, target);
				ps.addBatch();
				count++;
			}
			ps.executeBatch();
		}
		return count;
	}

	public boolean addOrUpdateTargets(List<TargetEntry> entries) {
		int num = 0;
		for (TargetEntry entry : entries) {
			if (addOrUpdateTarget(entry)) {
				num++;
			}
			;
		}
		return num == entries.size();
	}

	public boolean addOrUpdateTargets(IndexedHashMap<String, TargetEntry> entries) {
		int num = 0;
		for (TargetEntry entry : entries.values()) {
			if (addOrUpdateTarget(entry)) {
				num++;
			}
			;
		}
		return num == entries.size();
	}

	/**
	 * 
	 * @return
	 */
	public List<TargetEntry> selectAll() {
		String sql = "select * from TargetTable order by ID";
		// 在SQL中，如果你没有显式地指定排序规则（使用ORDER BY子句），那么结果的顺序是不确定的
		return jdbcTemplate.query(sql, new TargetMapper());
	}

	@Deprecated
	public TargetEntry selectByID(int id) {
		try {
			String sql = "select * from TargetTable where id=?";
			return jdbcTemplate.queryForObject(sql, new Object[] { id }, new TargetMapper());
		} catch (DataAccessException e) {
			// e.printStackTrace();
			return null;
		}
	}

	/**
	 * 根据TableModel的rowIndex进行查询
	 * 
	 * @param rowIndex
	 * @return
	 */
	public TargetEntry selectByRowIndex(int rowIndex) {
		try {// 查询第一行，就是limit 1 offset 0;第二行就是limit 1 offset 1.所以从0开始的RowIndex恰好作为offset值
			String sql = "select * from TargetTable LIMIT 1 OFFSET ?";
			return jdbcTemplate.queryForObject(sql, new Object[] { rowIndex }, new TargetMapper());
		} catch (DataAccessException e) {
			// e.printStackTrace();
			return null;
		}
	}

	public TargetEntry selectByTarget(String target) {
		try {
			String sql = "select * from TargetTable where target=?";
			return jdbcTemplate.queryForObject(sql, new Object[] { target }, new TargetMapper());
		} catch (DataAccessException e) {
			// e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	@Deprecated
	public boolean deleteByID(int id) {
		String sql = "delete from TargetTable where ID=?";
		return jdbcTemplate.update(sql, id) > 0;
	}

	public boolean deleteByTarget(String targetDomain) {
		String sql = "delete from TargetTable where target=?";
		return jdbcTemplate.update(sql, targetDomain) > 0;
	}

	public boolean deleteTarget(TargetEntry entry) {
		String sql = "delete from TargetTable where target=?";
		return jdbcTemplate.update(sql, entry.getTarget()) > 0;
	}

	public int getRowCount() {
		String sql = "select count(*) from TargetTable";
		return jdbcTemplate.queryForObject(sql, Integer.class);
	}
}
