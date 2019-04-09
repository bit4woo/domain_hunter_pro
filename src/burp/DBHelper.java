package burp;

import com.alibaba.fastjson.JSON;

import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DBHelper {
	private static Connection conn = null;                                      //连接
	private static Statement stmt = null;
	private PreparedStatement pres;                                      //PreparedStatement对象
	private String dbFilePath;


	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();
	/**
	 * 构造函数
	 * @param dbFilePath sqlite db 文件路径
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DBHelper(String dbFilePath){
		this.dbFilePath = dbFilePath;
		try {
			conn = getConnection();
			if (!tableExists("DOMAINObject") && conn != null){
				createTable();
			}
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			//System.exit(0);//就是这个导致了整个burp的退出！！！！
		}
	}

	public void createTable(){
		try {
			Statement stmt = conn.createStatement();
			String sql = "CREATE TABLE DOMAINObject" +
					"(ID INT PRIMARY KEY     NOT NULL," +
					" NAME           TEXT    NOT NULL," +
					" Content        TEXT    NOT NULL)";
			stmt.executeUpdate(sql);

			String sqlTitle = "CREATE TABLE Title" +
					"(ID INT PRIMARY KEY     NOT NULL," +
					" NAME           TEXT    NOT NULL," +
					" Content        TEXT    NOT NULL)";
			stmt.executeUpdate(sqlTitle);

			stmt.close();
			conn.close();
		} catch ( Exception e ) {
			e.printStackTrace(stderr);
			//System.exit(0);
		}
		System.out.println("Table created successfully");
	}

	//何时创建连接，何时关闭连接呢？最佳实践是怎样的？
	private Connection getConnection() throws ClassNotFoundException, SQLException{
		if (conn == null || conn.isClosed()){
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
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

			if (null != stmt) {
				stmt.close();
				stmt = null;
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
		}
		destroy();
		return false;
	}

	public void saveDomainObject(DomainObject domainResult){
		try {
			//clear table
			conn = getConnection();
			Statement  stmt = conn.createStatement();
			String sqlclear = "Delete From DOMAINObject";
			stmt.executeUpdate(sqlclear);

			String name = domainResult.getProjectName();
			String content  = domainResult.ToJson();
			String sql="insert into DOMAINObject(ID,NAME,Content) values(1,?,?)";
			pres=conn.prepareStatement(sql);//预编译

			pres.setString(1,name);
			pres.setObject(2,content);
			pres.execute();
			destroy();
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
	}

	/*
	 * 从数据库中读出存入的对象
	 * return:
	 * 	list:Person对象列表
	 */
	public DomainObject getDomainObj(){
		String sql="select * from DOMAINObject";

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			ResultSet res=pres.executeQuery();
			while(res.next()){
				String Content =res.getString("Content");//获取content部分的内容
				return JSON.parseObject(Content,DomainObject.class);
			}
			destroy();
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		return null;
	}



	public void saveTitles(List<LineEntry> lineEntries){
		String sql="insert into Title(ID,NAME,Content) values(?,?,?)";

		try {
			//clear table
			conn = getConnection();
			Statement  stmt = conn.createStatement();
			String sqlclear = "Delete From Title";
			stmt.executeUpdate(sqlclear);

			pres=conn.prepareStatement(sql);
			for(int i=0;i<lineEntries.size();i++){
				pres.setInt(1,i+1);
				pres.setString(2, lineEntries.get(i).getUrl());
				pres.setString(3,lineEntries.get(i).ToJson());
				pres.addBatch();                                   //实现批量插入
			}
			pres.executeBatch();                                      //批量插入到数据库中
			destroy();
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
	}


	public List<LineEntry> getTitles(){
		List<LineEntry> list=new ArrayList<LineEntry>();
		String sql="select * from Title";

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);

			ResultSet res=pres.executeQuery();
			while(res.next()){
				String LineJson=res.getString("Content");
				LineEntry entry = JSON.parseObject(LineJson,LineEntry.class);
				list.add(entry);
			}
			destroy();
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		return list;
	}

	public static void main(String args[]){
		DBHelper helper = new DBHelper("test1.db");
		DomainObject xxx = new DomainObject("test");
		helper.saveDomainObject(xxx);
		DomainObject yyyy = new DomainObject("yyyy");
		helper.saveDomainObject(yyyy);
		System.out.println(helper.getDomainObj().ToJson());
	}
}