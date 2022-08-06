package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import domain.target.TargetEntry;
import domain.target.TargetTableModel;
import title.LineEntry;

public class TargetDao {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	void setDataSource(DataSource ds) {
		dataSource = ds;
		jdbcTemplate = new JdbcTemplate(ds);
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

	/**
	 * 
	 * @param model
	 * @return
	 */
	public List<TargetEntry> selectAll(){
		String sql = "select * from Target";
		return jdbcTemplate.query(sql,new TargetMapper());
	}
	
	public TargetEntry selectByID(int id){
		String sql = "select * from Target where id=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id },new TargetMapper());
	}
	
	public TargetEntry selectByTarget(String target){
		String sql = "select * from Title where target=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { target },new TargetMapper());
	}

	/**
	 * 从数据库中读出存入的对象
	 */
	public TargetTableModel getTargets(){
		try {
			String sql="select * from Targets";
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			ResultSet res=pres.executeQuery();
			while(res.next()){
				String Content =res.getString("Content");//获取content部分的内容
				return TargetTableModel.FromJson(Content);
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return null;
	}
}
