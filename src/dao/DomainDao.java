package dao;

import java.io.File;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
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
			//使用和旧版本不同的表名称,避免冲突
			String sql = "select * from DomainTable limit 1";
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
		String sql = "CREATE TABLE IF NOT EXISTS DomainTable" +
				"(ID INT PRIMARY KEY     NOT NULL," +
				" Type           TEXT    NOT NULL," +
				" Content        TEXT    NOT NULL)";
		jdbcTemplate.execute(sql);
		return true;
	}

	public boolean createOrUpdateByType(Set<String> content,TextAreaType Type){
		String sql = "insert or replace into DomainTable (ID, Type,Content)"
				+ " values(1,?,?)";

		int result = jdbcTemplate.update(sql, Type, JSON.toJSONString(content));
		return result>0;
	}

	/*
	 * 从数据库中读出存入的对象
	 */
	public CopyOnWriteArraySet<String> selectByType(TextAreaType Type){
		String sql ="select Content from DomainTable where Type = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { Type },new DomainObjectMapper());
	}

	public boolean deleteByType(TextAreaType Type){
		String sql="DELETE FROM DomainTable where Type= ?";
		return jdbcTemplate.update(sql, new Object[] { Type }) >0;
	}
	
	
	public DomainManager getDomainManager(){
		DomainManager result = new DomainManager();
		for (TextAreaType type:TextAreaType.values()) {
			String sql ="select Content from DomainTable where Type = ?";
			Set<String> content;
			try {
				content = jdbcTemplate.queryForObject(sql, new String[] { type.toString() },new DomainObjectMapper());
				result.fillContentByType(type, content);
			} catch (EmptyResultDataAccessException e) {
				//EmptyResultDataAccessException表示没有对应的记录
			}
		}
		return result;
	}
	
	public boolean saveDomainManager(DomainManager domainResult){
		try {
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
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
