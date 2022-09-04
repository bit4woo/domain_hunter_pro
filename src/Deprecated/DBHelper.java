package Deprecated;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
prepareStatement  //预编译方法，在有参数传入时用它
createStatement  //在固定语句时可以用它
它们都对应2种语句执行方法 executeQuery - select  \executeUpdate - insert、update、delete
 */
@Deprecated
public class DBHelper {
	private static final Logger log=LogManager.getLogger(DBHelper.class);
	public static PrintWriter stdout;
	public static PrintWriter stderr;
	public static String DRIVER = "org.sqlite.JDBC";
//	public static String DRIVER = "com.mysql.jdbc.Driver";


	public static void createTable(Connection conn){
		PreparedStatement stmt =null;
		try {
			
			if (!tableExists(conn,"Targets")){
				String sql = "CREATE TABLE Targets" +
						"(ID INT PRIMARY KEY     NOT NULL," +
						" Content        TEXT    NOT NULL)";
				stmt.executeUpdate(sql);
				System.out.println("Table Targets created successfully");
				log.info("Table Targets created successfully");
			}
			
			if (!tableExists(conn,"DOMAINObject")){
				String sql = "CREATE TABLE DOMAINObject" +
						"(ID INT PRIMARY KEY     NOT NULL," +
						" NAME           TEXT    NOT NULL," +
						" Content        TEXT    NOT NULL)";
				stmt.executeUpdate(sql);
				System.out.println("Table created successfully");
				log.info("Table created successfully");
			}

			if (!tableExists(conn,"Title") ){
				String sqlTitle = "CREATE TABLE Title" +
						"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
						" NAME           TEXT    NOT NULL," +
						" Content        TEXT    NOT NULL)";
				stmt.executeUpdate(sqlTitle);
				System.out.println("Table created successfully");
				log.info("Table created successfully");
			}
		} catch ( Exception e ) {
			System.out.println("Table create failed");
			e.printStackTrace();
			e.printStackTrace(stderr);
			log.error(e);
		}finally{
			try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			destroyclose(conn,null,null);
		}
	}

	//何时创建连接，何时关闭连接呢？最佳实践是怎样的？
	public static Connection getConnection(String dbFilePath) throws SQLException, ClassNotFoundException{
		Connection conn = null;
		Class.forName(DRIVER); 
		//https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html#drivermanager
		//JDBC 4.0以前需要这个语句来加载驱动，现在不需要了 
		conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
		return conn;
	}
	

	//https://stackoverflow.com/questions/2225221/closing-database-connections-in-java
	public static void destroyclose(Connection conn, PreparedStatement pres, ResultSet rs) {
		if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
		if (pres != null) try { pres.close(); } catch (SQLException ignore) {}
		if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
	}

	/*
	 * 只在createTable函数中使用，createTable中有异常捕获和关闭逻辑
	 */
	private static boolean tableExists(Connection conn,String tableName) {
		try {
			//conn = getConnection();
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs = md.getTables(null, null, tableName, null);
			if (rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			ex.printStackTrace(stderr);
		} finally {
			//destroy();
		}
		return false;
	}
	
	
	/*
	 * close后的connect不是null，
	 */
	public void testConnectionClose() throws Exception {
		Connection aa = getConnection("C:\\Users\\P52\\Desktop\\test.db");
		System.out.println(aa);
		aa.close();
		System.out.println(aa);//close后的connect不是null，
		String sql="select * from DOMAINObject";
		ResultSet res=aa.prepareStatement(sql).executeQuery();
	}
	
	/*
	 * org.sqlite.SQLiteException: [SQLITE_ERROR] SQL error or missing database (table DOMAINObject already exists)
	 * 当一个表已经存在时，再尝试创建会报错，不会被覆盖。
	 */
	public static void testCreate() throws Exception {
		Connection conn = getConnection("C:\\Users\\P52\\Desktop\\test.db");
		Statement stmt = conn.createStatement();
		String sql = "CREATE TABLE DOMAINObject" +
				"(ID INT PRIMARY KEY     NOT NULL," +
				" NAME           TEXT    NOT NULL," +
				" Content        TEXT    NOT NULL)";
		stmt.executeUpdate(sql);
		System.out.println("Table created successfully");
		log.info("Table created successfully");
	}
	public static boolean testTry() {
		try {
			return true;
		}catch(Exception e) {
			return false;
		}finally {
			System.out.print("finally");
		}
	}

	public static void main(String args[]) throws Exception{
		testTry();
	}
}