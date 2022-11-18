package dao;

import java.io.File;

import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

public class DBUtils {

	public static DataSource getSqliteDataSource(String dbFilePath) {
		if (new File(dbFilePath).exists()) {
			SQLiteDataSource ds = new SQLiteDataSource();
			ds.setUrl("jdbc:sqlite:"+dbFilePath);
			return ds;
		}
		return null;
	}
}
