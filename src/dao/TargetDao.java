package dao;

import java.io.File;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

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
		dataSource = DBUtils.getSqliteDataSource(dbFilePath.toString());
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!testSelect()){
			createTable();
		}
	}

	public boolean createTable() {
		String sqlTitle = "CREATE TABLE IF NOT EXISTS Target" +
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
		String sqlcreateIndex = "CREATE UNIQUE INDEX idx_Target_target ON Target (target)";
		jdbcTemplate.execute(sqlcreateIndex);
		return true;
	}
	
	/**
	 * 
	 * @param entry
	 * @return
	 */
	public boolean addOrUpdateTarget(TargetEntry entry){
		String sql = "insert or replace into Target (target,type,keyword,ZoneTransfer,isBlack,comment,useTLD)"
					+ " values(?,?,?,?,?,?,?)";

		int result = jdbcTemplate.update(sql, entry.getTarget(),entry.getType(),entry.getKeyword(),entry.isZoneTransfer(),
				entry.isBlack(),entry.getComment(),entry.isUseTLD());
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

	/**
	 * 
	 * @return
	 */
	public List<TargetEntry> selectAll(){
		String sql = "select * from Target";
		return jdbcTemplate.query(sql,new TargetMapper());
	}
	
	public TargetEntry selectByID(int id){
		try {
			String sql = "select * from Target where id=?";
			return jdbcTemplate.queryForObject(sql, new Object[] { id },new TargetMapper());
		} catch (DataAccessException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	public TargetEntry selectByTarget(String target){
		try {
			String sql = "select * from Target where target=?";
			return jdbcTemplate.queryForObject(sql, new Object[] { target },new TargetMapper());
		} catch (DataAccessException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	public boolean testSelect(){
		try {
			String sql = "select * from Target limit 1";
			jdbcTemplate.execute(sql);
			return true;
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
	public boolean deleteByID(int id) {
		String sql = "delete from Target where ID=?";
		return jdbcTemplate.update(sql, id) > 0;
	}
	
	public boolean deleteByTarget(String targetDomain) {
		String sql = "delete from Target where target=?";
		return jdbcTemplate.update(sql, targetDomain) > 0;
	}
	
	public boolean deleteTarget(TargetEntry entry) {
		String sql = "delete from Target where target=?";
		return jdbcTemplate.update(sql, entry.getTarget()) > 0;
	}


	public int getRowCount(){
		String sql = "select count(*) from Target";
		return jdbcTemplate.queryForObject(
				sql, Integer.class);
	}

}
