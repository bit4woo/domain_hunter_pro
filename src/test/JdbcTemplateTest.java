package test;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

public class JdbcTemplateTest {
	public static void main(String[] args) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(getDataSource("/Users/xxx/all-test2.db"));
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS beers(name VARCHAR(100))");
		//Insert a record:
		jdbcTemplate.execute("INSERT INTO beers VALUES ('Stella')");
	}
	
	public static DataSource getDataSource(String dbFilePath) {
		SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:"+dbFilePath);
        return ds;
	}

}