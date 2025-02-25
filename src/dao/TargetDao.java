package dao;

import java.io.File;
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

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	public TargetDao(String dbFilePath) {
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!TableUpdater.tableExists(jdbcTemplate, "TargetTable")) {
			createTable();
		} else {
			alterTable();
		}
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
		String sql = "INSERT INTO TargetTable (target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel, subDomainCount,digDone)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
				+ " ON CONFLICT(target) DO UPDATE SET type = excluded.type, keyword = excluded.keyword, "
				+ " ZoneTransfer = excluded.ZoneTransfer, comment = excluded.comment, "
				+ " useTLD = excluded.useTLD, trustLevel = excluded.trustLevel, subDomainCount = excluded.subDomainCount, digDone=excluded.digDone";

		int result = jdbcTemplate.update(sql, entry.getTarget(), entry.getType(), entry.getKeyword(),
				entry.isZoneTransfer(), SetAndStr.toStr(entry.getComments()), entry.isUseTLD(), entry.getTrustLevel(),
				entry.getSubdomainCount(),entry.isDigDone());

		return result > 0;
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
