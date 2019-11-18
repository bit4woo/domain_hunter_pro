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
import java.util.List;
import java.util.stream.IntStream;

import com.alibaba.fastjson.JSON;

import Config.LineConfig;

/*
prepareStatement  //预编译方法，在有参数传入时用它
createStatement  //在固定语句时可以用它
它们都对应2种语句执行方法 executeQuery - select  \executeUpdate - insert、update、delete
 */
public class DBHelper {
	private Connection conn = null;                                      //连接
	private PreparedStatement pres;                                      //PreparedStatement对象
	private String dbFilePath;


	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout;
	public PrintWriter stderr;
	/**
	 * 构造函数
	 * @param dbFilePath sqlite db 文件路径
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DBHelper(String dbFilePath){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
		this.dbFilePath = dbFilePath;
		try {
			conn = getConnection();
			createTable();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			//System.exit(0);//就是这个导致了整个burp的退出！！！！
		}
	}

	public void createTable(){
		try {
			conn = getConnection();
			Statement stmt = conn.createStatement();

			if (!tableExists("DOMAINObject")){
				String sql = "CREATE TABLE DOMAINObject" +
						"(ID INT PRIMARY KEY     NOT NULL," +
						" NAME           TEXT    NOT NULL," +
						" Content        TEXT    NOT NULL)";
				stmt.executeUpdate(sql);
				System.out.println("Table created successfully");
			}

			if (!tableExists("Title") ){
				String sqlTitle = "CREATE TABLE Title" +
						"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
						" NAME           TEXT    NOT NULL," +
						" Content        TEXT    NOT NULL)";
				stmt.executeUpdate(sqlTitle);
				System.out.println("Table created successfully");
			}

			stmt.close();
			conn.close();
		} catch ( Exception e ) {
			System.out.println("Table create failed");
			e.printStackTrace(stderr);
		}

	}

	//何时创建连接，何时关闭连接呢？最佳实践是怎样的？
	private Connection getConnection() throws ClassNotFoundException, SQLException{
		if (conn == null || conn.isClosed()){
			Class.forName("org.sqlite.JDBC");
			if (new File(dbFilePath).exists()){
				conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
			}
		}
		if (conn == null){
			stderr.println("get connection failed --- "+dbFilePath);
		}
		return conn;
	}


	//http://www.cnblogs.com/haoqipeng/p/5300374.html
	public void destroy() {
		try {
			if (null != conn) {
				conn.close();
				conn = null;
			}
			if (null != pres) {
				pres.close();
				pres = null;
			}
		} catch (SQLException e) {
			System.out.println("error when close database");
		}
	}


	public boolean tableExists(String tableName) {
		try {
			conn = getConnection();
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs = md.getTables(null, null, tableName, null);
			if (rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace(stderr);
		} finally {
			//destroy();
		}
		return false;
	}

	public boolean saveDomainObject(DomainObject domainResult){
		try {
			conn = getConnection();
			pres = conn.prepareStatement("select * From DOMAINObject");
			ResultSet rs = pres.executeQuery();
			String sql = "";
			if (rs.next()){
				sql = "update DOMAINObject SET NAME=?,Content=? where ID=1";
			}else{
				sql = "insert into DOMAINObject(ID,NAME,Content) values(1,?,?)";
			}
			String name = domainResult.getProjectName();
			String content  = domainResult.ToJson();
			pres=conn.prepareStatement(sql);//预编译

			pres.setString(1,name);
			pres.setString(2,content);
			int n = pres.executeUpdate();
			if (n==1){
				System.out.println("save domain object successfully");
				return true;
			}else {
				System.out.println("save domain object failed");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}

	/*
	 * 从数据库中读出存入的对象
	 */
	public DomainObject getDomainObj(){
		try {
			String sql="select * from DOMAINObject";
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			ResultSet res=pres.executeQuery();
			while(res.next()){
				String Content =res.getString("Content");//获取content部分的内容
				return DomainObject.FromJson(Content);
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return null;
	}


	//////////////////Title///////////////////////////////
	public boolean addTitle(LineEntry entry){
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
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}



	public boolean addTitles(List<LineEntry> lineEntries){
		try {
			conn = getConnection();
			String sql="insert into Title(NAME,Content) values(?,?)";
			pres=conn.prepareStatement(sql);
			for(int i=0;i<lineEntries.size();i++){
				pres.setString(1, lineEntries.get(i).getUrl());
				pres.setString(2,lineEntries.get(i).ToJson());
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == lineEntries.size()){
				System.out.println("add titles successfully");
				return true;
			}else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}


	public List<LineEntry> getTitles(){
		List<LineEntry> list=new ArrayList<LineEntry>();
		try {
			conn = getConnection();
			String sql="select * from Title";
			pres=conn.prepareStatement(sql);

			ResultSet res=pres.executeQuery();
			while(res.next()){
				String LineJson=res.getString("Content");
				LineEntry entry = LineEntry.FromJson(LineJson);
				list.add(entry);
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		System.out.println(list.size() +" title lines found from DB file");
		return list;
	}

	@Deprecated
	public void updateTitle(LineEntry entry){
		String sql="update Title SET Content=? where NAME=?";
		//UPDATE Person SET FirstName = 'Fred' WHERE LastName = 'Wilson' 

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			pres.setString(1, entry.ToJson());
			pres.setString(2, entry.getUrl());
			pres.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}finally {
			destroy();
		}
	}

	public boolean updateTitles(List<LineEntry> lineEntries){
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
				System.out.println("update titles successfully");
				return true;
			}else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}finally {
			destroy();
		}
		return false;
	}

	@Deprecated
	public void deleteTitle(LineEntry entry){
		String sql="DELETE FROM Title where NAME= ?";
		//DELETE FROM Person WHERE LastName = 'Wilson'  

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			pres.setString(1, entry.getUrl());
			pres.executeUpdate();
			//Statement.execute(String sql) method which is mainly intended to perform database queries.
			//To execute INSERT/UPDATE/DELETE statements it's recommended the use of Statement.executeUpdate() method instead.
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
	}

	public boolean deleteTitles(List<LineEntry> lineEntries){
		String sql="DELETE FROM Title where NAME= ?";
		//DELETE FROM Person WHERE LastName = 'Wilson'

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			for(LineEntry entry:lineEntries){
				pres.setString(1, entry.getUrl());
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == lineEntries.size()){
				System.out.println("delete titles successfully");
				return true;
			}else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}

	public static void main(String args[]){
		DBHelper helper = new DBHelper("test1.db");
//		DomainObject xxx = new DomainObject("test");
//		helper.saveDomainObject(xxx);
//		DomainObject yyyy = new DomainObject("yyyy");
//		helper.saveDomainObject(yyyy);

		LineEntry aaa = new LineEntry();
		aaa.setUrl("www.baidu.com");

		LineEntry bbb = new LineEntry();
		aaa.setUrl("www.jd.com");

		helper.addTitle(aaa);
		helper.addTitle(bbb);
	}
}