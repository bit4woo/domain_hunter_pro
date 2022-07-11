package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;

import domain.DomainManager;

public class DomainDao {

	public static boolean createTable() {
		if (!tableExists(conn,"Title") ){
			String sqlTitle = "CREATE TABLE Title" +
					"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
					" NAME           TEXT    NOT NULL," +
					" Content        TEXT    NOT NULL)";
			stmt.executeUpdate(sqlTitle);
			System.out.println("Table created successfully");
			log.info("Table created successfully");
		}
	}
	public synchronized boolean saveDomainObject(DomainManager domainResult){
		try {
			
			Connection conn = null;
			Class.forName(DRIVER); 
			//https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html#drivermanager
			//JDBC 4.0以前需要这个语句来加载驱动，现在不需要了 
			conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
			ResultSetHandler<Employee> resultHandler = new BeanHandler<Employee>(Employee.class);
			try {
				Employee emp = queryRunner.query(conn, "SELECT * FROM employees WHERE first=?", resultHandler, "Sumit");
				// Display values
				System.out.print("ID: " + emp.getId() + ", Age: " + emp.getAge() + ", First: " + emp.getFirst() + ", Last: " + emp.getLast());
			} finally {
				DbUtils.close(conn);
			}
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
			String name = "domain_hunter_pro_by_bit4woo";
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

}
