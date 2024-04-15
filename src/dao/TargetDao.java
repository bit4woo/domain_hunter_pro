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
		if (!testSelect()){
			createTable();
		}
	}
	
	public TargetDao(File dbFilePath){
		this(dbFilePath.toString());
	}

	public boolean createTable() {
		//使用和旧版本不同的表名称,避免冲突
		String sqlTitle = "CREATE TABLE IF NOT EXISTS TargetTable" +
				"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
				" target           TEXT    NOT NULL," +
				" type        TEXT    NOT NULL,"+
				" keyword        TEXT    NOT NULL,"+
				" ZoneTransfer        BOOLEAN    NOT NULL,"+
				" isBlack        BOOLEAN    NOT NULL,"+
				" comment        TEXT    NOT NULL,"+
				" useTLD        BOOLEAN    NOT NULL)";
		jdbcTemplate.execute(sqlTitle);
		
		//https://www.sqlitetutorial.net/sqlite-replace-statement/
		//让target字段成为replace操作判断是否重复的判断条件。
		String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Target_target ON TargetTable (target)";
		jdbcTemplate.execute(sqlcreateIndex);
		return true;
	}
	
	/**
	 * 
	 * @param entry
	 * @return
	 */
	public boolean addOrUpdateTarget(TargetEntry entry){
		String sql = "insert or replace into TargetTable (target,type,keyword,ZoneTransfer,isBlack,comment,useTLD)"
					+ " values(?,?,?,?,?,?,?)";

		int result = jdbcTemplate.update(sql, entry.getTarget(),entry.getType(),entry.getKeyword(),entry.isZoneTransfer(),
				entry.isBlack(),SetAndStr.toStr(entry.getComments()),entry.isUseTLD());

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
	
	public boolean testSelect(){
		try {
			//String sql = "select * from TargetTable limit 1";
			String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = 'TargetTable'";
			SqlRowSet result = jdbcTemplate.queryForRowSet(sql);
			if (result.getRow() > 0 && result.getInt(0) > 0) {
				return true;
			}else {
				return false;
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
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
