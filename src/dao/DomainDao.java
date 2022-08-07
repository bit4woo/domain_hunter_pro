package dao;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import domain.DomainManager;

public class DomainDao {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	
	public DomainDao(String dbFilePath){
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public boolean createTable() {
		String sql = "CREATE TABLE IF NOT EXISTS DOMAINObject" +
				"(ID INT PRIMARY KEY     NOT NULL," +
				" NAME           TEXT    NOT NULL," +
				" Content        TEXT    NOT NULL)";
		jdbcTemplate.execute(sql);
		return true;
	}

	public boolean createOrUpdateDomainObject(DomainManager domainResult){
		String sql = "insert or replace into DOMAINObject (ID, NAME,Content)"
				+ " values(1,?,?)";

		jdbcTemplate.update(sql, domainResult.ToJson());

		String name = "domain_hunter_pro_by_bit4woo";
		String content  = domainResult.ToJson();

		int result = jdbcTemplate.update(sql, name, content);

		return result>0;
	}

	/*
	 * 从数据库中读出存入的对象
	 */
	public List<DomainManager> selectDomainObject(){
		String sql ="select Content from DOMAINObject";
		return jdbcTemplate.query(sql,new DomainObjectMapper());
	}
	

	public boolean deleteDomainObject(List<String> urls){
		String sql="DELETE FROM DOMAINObject where NAME= domain_hunter_pro_by_bit4woo";
		return jdbcTemplate.update(sql) >0;
	}
}
