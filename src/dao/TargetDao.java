package dao;

import java.io.File;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import base.IndexedHashMap;
import base.SetAndStr;
import domain.target.TargetEntry;

public class TargetDao {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	
	public TargetDao(String dbFilePath){
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!tableExists("TargetTable")){
			createTable();
		}else {
			alterTable();
		}
	}
	
	public TargetDao(File dbFilePath){
		this(dbFilePath.toString());
	}
	
	public String genCreateTableSql(String tableName) {
		//使用和旧版本不同的表名称,避免冲突
		String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName +" "+
				"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
				" target           TEXT    NOT NULL UNIQUE," + // -- UNIQUE关键词 为 target 字段添加唯一约束，这样INSERT OR REPLACE语句才能根据这段进行
				" type        TEXT    NOT NULL,"+
				" keyword        TEXT    NOT NULL,"+
				" ZoneTransfer        BOOLEAN    NOT NULL,"+
				//" isBlack        BOOLEAN    NOT NULL,"+
				" trustLevel        TEXT    NOT NULL,"+
				" comment        TEXT    NOT NULL,"+
				" useTLD        BOOLEAN    NOT NULL,"+ 
				" subDomainCount	INTEGER)";
		return createTableSql;
	}

	public boolean createTable() {

		jdbcTemplate.execute(genCreateTableSql("TargetTable"));
		
		//https://www.sqlitetutorial.net/sqlite-replace-statement/
		//让target字段成为replace操作判断是否重复的判断条件。
		String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Target_target ON TargetTable (target)";
		jdbcTemplate.execute(sqlcreateIndex);
		return true;
	}
	
	public void alterTable() {
		removeColumnIsBlackIfPresent();
		AddSubDomainCount();
	}
	
	/**
	 * 实现：删除isBlack字段，新增trustLevel字段的逻辑。需要确保旧表有isBlack字段。
	 * @return
	 */
	public boolean removeColumnIsBlackIfPresent() {
	    if (!columnExists("TargetTable", "isBlack")) {
	        // 如果 isBlack 列不存在，直接返回
	        return true;
	    }

	    String createNewTableSql = genCreateTableSql("TargetTable_new");
	    
	    //org.sqlite.SQLiteException: [SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: TargetTable_new.target)
	    String copyDataSql = "INSERT OR IGNORE INTO TargetTable_new (ID, target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel) " +
	            "SELECT ID, target, type, keyword, ZoneTransfer, comment, useTLD, " +
	            "CASE WHEN isBlack = 1 THEN 'NonTarget' ELSE 'Maybe' END as trustLevel " +
	            "FROM TargetTable";
	    
	    String dropOldTableSql = "DROP TABLE TargetTable";
	    
	    String renameTableSql = "ALTER TABLE TargetTable_new RENAME TO TargetTable";
	    
	    String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Target_target ON TargetTable (target)";
	    try {
	        jdbcTemplate.execute(createNewTableSql);
	        jdbcTemplate.execute(copyDataSql);
	        jdbcTemplate.execute(dropOldTableSql);
	        jdbcTemplate.execute(renameTableSql);
	        jdbcTemplate.execute(sqlcreateIndex);
	        return true;
	    } catch (DataAccessException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	/**
	 * 实现：删除isBlack字段，新增trustLevel字段的逻辑。需要确保旧表有isBlack字段。
	 * @return
	 */
	public boolean AddSubDomainCount() {
	    if (columnExists("TargetTable", "subDomainCount")) {
	        // 如果 subDomainCount 列存在，表面是最新的表结构，直接返回
	        return true;
	    }

	    String createNewTableSql = genCreateTableSql("TargetTable_new");
	    
	    String copyDataSql = "INSERT OR IGNORE INTO TargetTable_new (ID, target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel) " +
	            "SELECT ID, target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel " +
	            "FROM TargetTable";
	    
	    String dropOldTableSql = "DROP TABLE TargetTable";
	    
	    String renameTableSql = "ALTER TABLE TargetTable_new RENAME TO TargetTable";
	    
	    String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Target_target ON TargetTable (target)";
	    try {
	        jdbcTemplate.execute(createNewTableSql);
	        jdbcTemplate.execute(copyDataSql);
	        jdbcTemplate.execute(dropOldTableSql);
	        jdbcTemplate.execute(renameTableSql);
	        jdbcTemplate.execute(sqlcreateIndex);
	        return true;
	    } catch (DataAccessException e) {
	        e.printStackTrace();
	        return false;
	    }
	}

	private boolean columnExists(String tableName, String columnName) {
	    try {
	        String sql = "PRAGMA table_info(" + tableName + ")";
	        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
	        while (rowSet.next()) {
	            if (columnName.equals(rowSet.getString("name"))) {
	                return true;
	            }
	        }
	    } catch (DataAccessException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	public boolean tableExists(String tableName){
		try {
			String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = '"+tableName+"'";
			SqlRowSet result = jdbcTemplate.queryForRowSet(sql);
			if (result.next() && result.getInt(1) > 0) {
				return true;
			} else {
				return false;
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	
	/**
	 * 不再使用isBlack字段，由trustLevel字段代替
	 * @param entry
	 * @return
	 */
	public boolean addOrUpdateTarget(TargetEntry entry) {
	    String sql = "insert or replace into TargetTable (target, type, keyword, ZoneTransfer, comment, useTLD, trustLevel,subDomainCount)"
	            + " values(?, ?, ?, ?, ?, ?, ?,?)";

	    int result = jdbcTemplate.update(sql, entry.getTarget(), entry.getType(), entry.getKeyword(), entry.isZoneTransfer(),
	            SetAndStr.toStr(entry.getComments()), entry.isUseTLD(), 
	            entry.getTrustLevel(),entry.getSubdomainCount());

	    return result > 0;
	}

	
	public boolean addOrUpdateTargets(List<TargetEntry> entries){
		int num=0;
		for (TargetEntry entry:entries) {
			if (addOrUpdateTarget(entry)) {
				num++;
			};
		}
		return num==entries.size();
	}

	public boolean addOrUpdateTargets(IndexedHashMap<String, TargetEntry> entries){
		int num=0;
		for (TargetEntry entry:entries.values()) {
			if (addOrUpdateTarget(entry)) {
				num++;
			};
		}
		return num==entries.size();
	}


	/**
	 * 
	 * @return
	 */
	public List<TargetEntry> selectAll(){
		String sql = "select * from TargetTable order by ID";
		//在SQL中，如果你没有显式地指定排序规则（使用ORDER BY子句），那么结果的顺序是不确定的
		return jdbcTemplate.query(sql,new TargetMapper());
	}

	@Deprecated
	public TargetEntry selectByID(int id){
		try {
			String sql = "select * from TargetTable where id=?";
			return jdbcTemplate.queryForObject(sql, new Object[] { id },new TargetMapper());
		} catch (DataAccessException e) {
			//e.printStackTrace();
			return null;
		}
	}

	/**
	 * 根据TableModel的rowIndex进行查询
	 * @param rowIndex
	 * @return
	 */
	public TargetEntry selectByRowIndex(int rowIndex){
		try {//查询第一行，就是limit 1 offset 0;第二行就是limit 1 offset 1.所以从0开始的RowIndex恰好作为offset值
			String sql = "select * from TargetTable LIMIT 1 OFFSET ?";
			return jdbcTemplate.queryForObject(sql, new Object[] { rowIndex },new TargetMapper());
		} catch (DataAccessException e) {
			//e.printStackTrace();
			return null;
		}
	}


	public TargetEntry selectByTarget(String target){
		try {
			String sql = "select * from TargetTable where target=?";
			return jdbcTemplate.queryForObject(sql, new Object[] { target },new TargetMapper());
		} catch (DataAccessException e) {
			//e.printStackTrace();
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


	public int getRowCount(){
		String sql = "select count(*) from TargetTable";
		return jdbcTemplate.queryForObject(
				sql, Integer.class);
	}
}
