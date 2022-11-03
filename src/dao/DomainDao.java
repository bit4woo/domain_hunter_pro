package dao;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sql.DataSource;

import com.alibaba.fastjson.JSON;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import domain.DomainManager;

public class DomainDao {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	public static final String Type_SubDomain = "Type_SubDomain";
	public static final String Type_RelatedDomain = "Type_RelatedDomain";
	public static final String Type_SimilarDomain = "Type_SimilarDomain";

	public static final String Type_Email = "Type_Email";
	public static final String Type_SimilarEmail = "Type_SimilarEmail";

	public static final String Type_IPSetOfSubnet = "Type_IPSetOfSubnet";
	public static final String Type_IPSetOfCert = "Type_IPSetOfCert";

	public static final String Type_SpecialPortTarget = "Type_SpecialPortTarget";
	public static final String Type_PackageName = "Type_PackageName";
	public static final String Type_BlackIP = "Type_BlackIP"; //对应NotTargetIPSet

	
	public DomainDao(String dbFilePath){
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!testSelect()){
			createTable();
		}
	}

	public DomainDao(File dbFile){
		this(dbFile.toString());
	}

	public boolean testSelect(){
		try {
			String sql = "select * from DOMAINObject limit 1";
			jdbcTemplate.execute(sql);
			return true;
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 一行一个type，比如subdomain存一行，similardomain存另外一行。
	 * @return
	 */
	public boolean createTable() {
		String sql = "CREATE TABLE IF NOT EXISTS DOMAINObject" +
				"(ID INT PRIMARY KEY     NOT NULL," +
				" Type           TEXT    NOT NULL," +
				" Content        TEXT    NOT NULL)";
		jdbcTemplate.execute(sql);
		return true;
	}

	public boolean createOrUpdateByType(CopyOnWriteArraySet<String> content,String Type){
		String sql = "insert or replace into DOMAINObject (ID, Type,Content)"
				+ " values(1,?,?)";

		int result = jdbcTemplate.update(sql, Type, JSON.toJSONString(content));
		return result>0;
	}

	/*
	 * 从数据库中读出存入的对象
	 */
	public CopyOnWriteArraySet<String> selectByType(String Type){
		String sql ="select Content from DOMAINObject where Type = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { Type },new DomainObjectMapper());
	}

	public boolean deleteByType(String Type){
		String sql="DELETE FROM DOMAINObject where Type= ?";
		return jdbcTemplate.update(sql, new Object[] { Type }) >0;
	}
	
	
	public DomainManager getDomainManager(){
		
		String sql ="select Content from DOMAINObject where Type = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { Type },new DomainObjectMapper());
	}

}
