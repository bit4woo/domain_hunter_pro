package burp;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import domain.DomainManager;
import title.IndexedLinkedHashMap;
import title.LineEntry;

/*
prepareStatement  //预编译方法，在有参数传入时用它
createStatement  //在固定语句时可以用它
它们都对应2种语句执行方法 executeQuery - select  \executeUpdate - insert、update、delete
 */
public class DBHelper {
	private Connection conn = null;       //连接
	private PreparedStatement pres = null;  //PreparedStatement对象
	private ResultSet rs = null;
	private String dbFilePath;

	private static final Logger log=LogManager.getLogger(DBHelper.class);
	public PrintWriter stdout;
	public PrintWriter stderr;
	/**
	 * 构造函数
	 * @param dbFilePath sqlite db 文件路径
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DBHelper(String dbFilePath){
		stdout = BurpExtender.getStdout();
		stderr = BurpExtender.getStderr();

		this.dbFilePath = dbFilePath;
		try {
			createTable();
		} catch ( Exception e ) {
			e.printStackTrace();
			e.printStackTrace(stderr);
			log.error(e);
			//System.exit(0);//就是这个导致了整个burp的退出！！！！
		}
	}

	private void createTable(){
		Statement stmt =null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			
			if (!tableExists("DOMAINObject")){
				String sql = "CREATE TABLE DOMAINObject" +
						"(ID INT PRIMARY KEY     NOT NULL," +
						" NAME           TEXT    NOT NULL," +
						" Content        TEXT    NOT NULL)";
				stmt.executeUpdate(sql);
				System.out.println("Table created successfully");
				log.info("Table created successfully");
			}

			if (!tableExists("Title") ){
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
			destroy();
		}
	}

	//何时创建连接，何时关闭连接呢？最佳实践是怎样的？
	private Connection getConnection() throws SQLException, ClassNotFoundException{
		if (conn == null || conn.isClosed()){
			Class.forName("org.sqlite.JDBC"); //https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html#drivermanager
			//JDBC 4.0以前需要这个语句来加载驱动，现在不需要了 
			if (new File(dbFilePath).exists()){
				conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
			}else {
				System.out.println("DB file not found");
				stderr.println("DB file not found");
				log.error("DB file not found");
			}
		}
		return conn;
	}


	//https://stackoverflow.com/questions/2225221/closing-database-connections-in-java
	private void destroy() {
		if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
		if (pres != null) try { pres.close(); } catch (SQLException ignore) {}
		if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
	}

	/*
	 * 只在createTable函数中使用，createTable中有异常捕获和关闭逻辑
	 */
	private boolean tableExists(String tableName) {
		try {
			//conn = getConnection();
			DatabaseMetaData md = conn.getMetaData();
			rs = md.getTables(null, null, tableName, null);
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

	public synchronized boolean saveDomainObject(DomainManager domainResult){
		try {
			conn = getConnection();
			
			pres = conn.prepareStatement("select ID From DOMAINObject");//会比select * From DOMAINObject 要快吧
			rs = pres.executeQuery();
			String sql = "";
			if (rs.next()){
				sql = "update DOMAINObject SET NAME=?,Content=? where ID=1";
			}else{
				sql = "insert into DOMAINObject(ID,NAME,Content) values(1,?,?)";
			}
			
			//让新增和更新的逻辑在一个语句中完成，减少查询。这是mysql中的语句，sqlit中不存在。
			//String sql = "insert into DOMAINObject(ID,NAME,Content) values(1,?,?) ON DUPLICATE KEY UPDATE NAME=VALUES(NAME),Content=VALUES(Content)";
			String name = domainResult.getProjectName();
			String content  = domainResult.ToJson();
			pres=conn.prepareStatement(sql);//预编译

			pres.setString(1,name);
			pres.setString(2,content);
			int n = pres.executeUpdate();
			if (n==1){
				System.out.println("save domain object successfully");
				return true;//不影响finally的执行
			}else {
				System.out.println("save domain object failed");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}

	/*
	 * 从数据库中读出存入的对象
	 */
	public DomainManager getDomainObj(){
		try {
			String sql="select * from DOMAINObject";
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			ResultSet res=pres.executeQuery();
			while(res.next()){
				String Content =res.getString("Content");//获取content部分的内容
				return DomainManager.FromJson(Content);
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return null;
	}


	//////////////////Title///////////////////////////////
	//https://stackoverflow.com/questions/964207/sqlite-exception-sqlite-busy
	public synchronized boolean addTitle(LineEntry entry){
		try {
			conn = getConnection();
			String sql="insert into Title(NAME,Content) values(?,?)";
			pres=conn.prepareStatement(sql);
			pres.setString(1, entry.getUrl());
			pres.setString(2,entry.ToJson());
			int result = pres.executeUpdate();
			if ( result == 1){
				System.out.println("add title successfully");
				return true;
			}else {
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when add "+entry.getUrl());
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}



	public synchronized boolean addTitles(LinkedHashMap<String,LineEntry> lineEntries){
		try {
			conn = getConnection();
			String sql="insert into Title(NAME,Content) values(?,?)";
			pres=conn.prepareStatement(sql);
			for(String key:lineEntries.keySet()){
				pres.setString(1, key);
				pres.setString(2,lineEntries.get(key).ToJson());
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == lineEntries.size()){
				System.out.println("add titles successfully");
				return true;
			}else {
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}


	public IndexedLinkedHashMap<String,LineEntry> getTitles(){
		IndexedLinkedHashMap<String,LineEntry> lineEntriesMap=new IndexedLinkedHashMap<String,LineEntry>();
		try {
			conn = getConnection();
			String sql="select * from Title";
			pres=conn.prepareStatement(sql);

			ResultSet res=pres.executeQuery();
			while(res.next()){
				String LineJson=res.getString("Content");
				LineEntry entry = LineEntry.FromJson(LineJson);
				lineEntriesMap.put(entry.getUrl(), entry);
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		System.out.println(lineEntriesMap.size() +" title lines found from DB file");
		return lineEntriesMap;
	}


	public synchronized void updateTitle(LineEntry entry){
		String sql="update Title SET Content=? where NAME=?";
		//UPDATE Person SET FirstName = 'Fred' WHERE LastName = 'Wilson' 

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			pres.setString(1, entry.ToJson());
			pres.setString(2, entry.getUrl());
			pres.executeUpdate();
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		}finally {
			destroy();
		}
	}

	public synchronized boolean updateTitles(List<LineEntry> lineEntries){
		try {
			conn = getConnection();
			String sql="update Title SET Content=? where NAME=?";
			//UPDATE Person SET FirstName = 'Fred' WHERE LastName = 'Wilson'
			pres=conn.prepareStatement(sql);
			for(LineEntry entry:lineEntries){
				pres.setString(1, entry.ToJson());
				pres.setString(2, entry.getUrl());
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == lineEntries.size()){
				System.out.println("update titles successfully: "+lineEntries.size());
				return true;
			}else {
				System.out.println("update titles failed");
				stderr.println("update titles failed");
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace();
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		}finally {
			destroy();
		}
		return false;
	}


	public synchronized void deleteTitle(LineEntry entry){
		String sql="DELETE FROM Title where NAME= ?";
		//DELETE FROM Person WHERE LastName = 'Wilson'  

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			pres.setString(1, entry.getUrl());
			pres.executeUpdate();
			//Statement.execute(String sql) method which is mainly intended to perform database queries.
			//To execute INSERT/UPDATE/DELETE statements it's recommended the use of Statement.executeUpdate() method instead.
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
	}

	public synchronized boolean deleteTitles(List<LineEntry> lineEntries){
		List<String> urls = new ArrayList<String>();
		for(LineEntry entry:lineEntries) {
			urls.add(entry.getUrl());
		}
		return deleteTitlesByUrl(urls);
	}
	
	
	public synchronized boolean deleteTitlesByUrl(List<String> urls){
		String sql="DELETE FROM Title where NAME= ?";
		//DELETE FROM Person WHERE LastName = 'Wilson'

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			for(String url:urls){
				pres.setString(1, url);
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == urls.size()){
				System.out.println("delete titles successfully: "+urls.size());
				return true;
			}else {
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}
	
	/*
	 * close后的connect不是null，
	 */
	public void testConnectionClose() throws Exception {
		DBHelper helper = new DBHelper("C:\\Users\\P52\\Desktop\\test.db");
		Connection aa = helper.getConnection();
		System.out.println(aa);
		aa.close();
		System.out.println(aa);//close后的connect不是null，
		aa = helper.getConnection();
		String sql="select * from DOMAINObject";
		ResultSet res=aa.prepareStatement(sql).executeQuery();
	}
	
	/*
	 * org.sqlite.SQLiteException: [SQLITE_ERROR] SQL error or missing database (table DOMAINObject already exists)
	 * 当一个表已经存在时，再尝试创建会报错，不会被覆盖。
	 */
	public static void testCreate() throws Exception {
		DBHelper helper = new DBHelper("C:\\Users\\P52\\Desktop\\test.db");
		Connection conn = helper.getConnection();
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
		//		DomainObject xxx = new DomainObject("test");
		//		helper.saveDomainObject(xxx);
		//		DomainObject yyyy = new DomainObject("yyyy");
		//		helper.saveDomainObject(yyyy);

		//		LineEntry aaa = new LineEntry();
		//		aaa.setUrl("www.baidu.com");
		//
		//		LineEntry bbb = new LineEntry();
		//		aaa.setUrl("www.jd.com");
		//
		//		helper.addTitle(aaa);
		//		helper.addTitle(bbb);
	}
}