package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import domain.DomainManager;

public class DomainDao {
	
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	void setDataSource(DataSource ds) {
		dataSource = ds;
		jdbcTemplate = new JdbcTemplate(ds);
	}
	

    private Set<String> subDomainSet = new CopyOnWriteArraySet<String>();
    private Set<String> similarDomainSet = new CopyOnWriteArraySet<String>();
    private Set<String> relatedDomainSet = new CopyOnWriteArraySet<String>();
    //private Set<String> IsTargetButUselessDomainSet = new CopyOnWriteArraySet<String>();
    //有效(能解析IP)但无用的域名，比如JD的网店域名、首页域名等对信息收集、聚合网段、目标界定有用，但是本身几乎不可能有漏洞的资产。

    private Set<String> NotTargetIPSet = new CopyOnWriteArraySet<String>();
    //存储域名解析到的CDN或云服务的IP。这类IP在做网段汇算时，应当被排除在外。

    //private ConcurrentHashMap<String, Integer> unkownDomainMap = new ConcurrentHashMap<String, Integer>();//记录域名和解析失败的次数，大于五次就从子域名中删除。
    private Set<String> EmailSet = new CopyOnWriteArraySet<String>();
    private Set<String> PackageNameSet = new CopyOnWriteArraySet<String>();
    private Set<String> SpecialPortTargets = new CopyOnWriteArraySet<String>();//用于存放指定了特殊端口的目标
	
	public boolean createTable() {
		String sqlTitle = "CREATE TABLE IF NOT EXISTS Domain" +
				"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
				" NAME TEXT NOT NULL," +
				" subdomains        TEXT    NOT NULL," +
				" similardomains        TEXT    NOT NULL," +
				" relateddomains        TEXT    NOT NULL," +
				" emails        TEXT    NOT NULL," +
				" packagenames        TEXT    NOT NULL," +
				" nottargetips        TEXT    NOT NULL," +
				" specialporttargets        TEXT    NOT NULL)";
		jdbcTemplate.execute(sqlTitle);
		return true;
	}
	
	public boolean createDomainObject(DomainManager domainResult){
		try {
			sql =""
			jdbcTemplate.update(null)
			"insert or replace into Book ( Name, TypeID, Level, Seen) values(\"SearchName\", ...)"
			pres = pres.prepareStatement("select ID From DOMAINObject");//会比select * From DOMAINObject 要快吧
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
