package burp;

import com.alibaba.fastjson.JSON;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DBHelper {
    private static Connection conn = null;                                      //连接
    private PreparedStatement pres;                                      //PreparedStatement对象
    private String dbFilePath;

    /**
     * 构造函数
     * @param dbFilePath sqlite db 文件路径
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DBHelper(String dbFilePath){
        this.dbFilePath = dbFilePath;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
            

            if (!tableExists("DOMAINObject")){
                createTable();
            }
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Opened database successfully");

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
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println("Table created successfully");
    }

    //何时创建连接，何时关闭连接呢？最佳实践是怎样的？
    private Connection getConnection(){
        try {
            if (conn == null || conn.isClosed()){
                conn = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
            }
            return conn;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
    private void ClearTable(){
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql = "Delete From DOMAINObject";
            stmt.executeUpdate(sql);

            String sqlTitle = "Delete From Title";
            stmt.executeUpdate(sqlTitle);

            //stmt.close();
            //conn.close();
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println("Table Clean successfully");
    }


    public boolean tableExists(String tableName) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, tableName, null);
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
            String content  = JSON.toJSONString(domainResult);
            String sql="insert into DOMAINObject(ID,NAME,Content) values(1,?,?)";
            pres=conn.prepareStatement(sql);//预编译

            pres.setString(1,name);
            pres.setObject(2,content);
            pres.execute();
            if(pres!=null)
                pres.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
                if(pres!=null)
                    pres.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
				pres.close();
				conn.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
                pres.setString(3,JSON.toJSONString(lineEntries.get(i)));
                pres.addBatch();                                   //实现批量插入
            }
            pres.executeBatch();                                      //批量插入到数据库中

            if(pres!=null)
                pres.close();
			pres.close();
			conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
                if(pres!=null)
                    pres.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
                if(pres!=null)
                    pres.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return list;
    }

    public static void main(String args[]){
        DBHelper helper = new DBHelper("test1.db");
        DomainObject xxx = new DomainObject("test");
        helper.saveDomainObject(xxx);
        DomainObject yyyy = new DomainObject("yyyy");
        helper.saveDomainObject(yyyy);
        System.out.println(helper.getDomainObj().Save());
    }
}