package dao;

import java.io.File;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.alibaba.fastjson.JSON;

import domain.DomainManager;
import domain.TextAreaType;

public class DomainDao {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	
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

	public boolean createOrUpdateByType(Set<String> content,TextAreaType Type){
		String sql = "insert or replace into DOMAINObject (ID, Type,Content)"
				+ " values(1,?,?)";

		int result = jdbcTemplate.update(sql, Type, JSON.toJSONString(content));
		return result>0;
	}

	/*
	 * 从数据库中读出存入的对象
	 */
	public CopyOnWriteArraySet<String> selectByType(TextAreaType Type){
		String sql ="select Content from DOMAINObject where Type = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { Type },new DomainObjectMapper());
	}

	public boolean deleteByType(TextAreaType Type){
		String sql="DELETE FROM DOMAINObject where Type= ?";
		return jdbcTemplate.update(sql, new Object[] { Type }) >0;
	}
	
	
	public DomainManager getDomainManager(){
		DomainManager result = new DomainManager();
		for (TextAreaType type:TextAreaType.values()) {
			String sql ="select Content from DOMAINObject where Type = ?";
			Set<String> content = jdbcTemplate.queryForObject(sql, new Object[] { type },new DomainObjectMapper());
			result.fillContentByType(type, content);
		}
		return result;
	}
	
	public void saveDomainManager(DomainManager domainResult){
		for (TextAreaType type:TextAreaType.values()) {
			switch (type) {
			case SubDomain:
				createOrUpdateByType(domainResult.getSubDomainSet(),type);
				break;
			case RelatedDomain:
				createOrUpdateByType(domainResult.getRelatedDomainSet(),type);
				break;
			case SimilarDomain:
				createOrUpdateByType(domainResult.getSimilarDomainSet(),type);
				break;
			case Email:
				createOrUpdateByType(domainResult.getEmailSet(),type);
				break;
			case SimilarEmail:
				createOrUpdateByType(domainResult.getSimilarEmailSet(),type);
				break;
			case IPSetOfSubnet:
				createOrUpdateByType(domainResult.getIPSetOfSubnet(),type);
				break;
			case IPSetOfCert:
				createOrUpdateByType(domainResult.getIPSetOfCert(),type);
				break;
			case SpecialPortTarget:
				createOrUpdateByType(domainResult.getSpecialPortTargets(),type);
				break;
			case PackageName:
				createOrUpdateByType(domainResult.getPackageNameSet(),type);
				break;
			case BlackIP:
				createOrUpdateByType(domainResult.getNotTargetIPSet(),type);
				break;
			}
		}
	}
	
	/**
	 * 上面没有提供setter函数，由此代替
	 * @param type
	 * @param content
	 */
	public void fillContentByType(TextAreaType type,Set<String> content) {
		
	}

}
