package dao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import title.LineEntry;

/**
 * 增删查改
 *
 */
public class TitleDao {
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	
	public TitleDao(String dbFilePath){
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!testSelect()){
			createTable();
		}
	}

	public TitleDao(File dbFile){
		this(dbFile.toString());
	}

	public boolean testSelect(){
		try {
			String sql = "select * from Title limit 1";
			jdbcTemplate.execute(sql);
			return true;
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean createTable() {
		String sqlTitle = "CREATE TABLE IF NOT EXISTS Title" +
				"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
				" url           TEXT    NOT NULL," +
				" statuscode           INT    NOT NULL," +
				" contentLength           INT    NOT NULL," +
				" title           TEXT    NOT NULL," +
				" IP           TEXT    NOT NULL," +
				" CDN           TEXT    NOT NULL," +
				" webcontainer           TEXT    NOT NULL," +
				" time           TEXT    NOT NULL," +
				" icon_hash           TEXT    NOT NULL," +
				" ASNInfo           TEXT    NOT NULL," +
				" request           BLOB    NOT NULL," +
				" response           BLOB    NOT NULL," +
				" protocol           TEXT    NOT NULL," +
				" host           TEXT    NOT NULL," +
				" port        INT    NOT NULL)";
		jdbcTemplate.execute(sqlTitle);
		
		//https://www.sqlitetutorial.net/sqlite-replace-statement/
		String sqlcreateIndex = "CREATE UNIQUE INDEX idx_Title_url ON Title (url)";
		jdbcTemplate.execute(sqlcreateIndex);
		
		return true;
	}

	/**
	 * 写入记录，如果URL存在相同的URL，则覆盖
	 * @param entry
	 * @return
	 */
	public boolean addOrUpdateTitle(LineEntry entry){
		String sql = "insert or replace into Title (url,statuscode,contentLength,title,IP,CDN"
					+ "webcontainer,time,icon_hash,ASNInfo,request,response,"
					+ "protocol,host,port,"
					+ "CheckStatus,AssetType,EntryType,comment,isManualSaved)"
					+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		int result = jdbcTemplate.update(sql, entry.getUrl(),entry.getStatuscode(),entry.getContentLength(),entry.getTitle(),
				entry.getIP(),entry.getCDN(),entry.getWebcontainer(),entry.getTime(),entry.getIcon_hash(),entry.getASNInfo(),
				entry.getRequest(),entry.getResponse(),
				entry.getProtocol(),entry.getHost(),entry.getPort(),
				entry.getCheckStatus(),entry.getAssetType(),entry.getEntryType(),entry.getComment(),entry.isManualSaved());
		return result > 0;
	}


	public boolean addOrUpdateTitles(List<LineEntry> lineEntries){
		int num = 0;
		for (LineEntry entry:lineEntries) {
			if (addOrUpdateTitle(entry)) {
				num++;
			}
		}
		return num == lineEntries.size();
	}


	/**
	 * 
	 * @param id
	 * @return
	 */
	public LineEntry selectTitleByID(int id){
		String sql = "select * from Title where id=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id },new TitleMapper());
	}
	
	
	public LineEntry selectTitleByUrl(String url){
		String sql = "select * from Title where url=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { url },new TitleMapper());
	}

	public List<LineEntry> selectAllTitle(){
		String sql = "select * from Title";
		return jdbcTemplate.query(sql,new TitleMapper());
	}
	
	public boolean deleteTitle(LineEntry entry){
		String sql="DELETE FROM Title where url= ?";
		return jdbcTemplate.update(sql, entry.getUrl()) > 0;
	}
	
	public boolean deleteTitleByUrl(String url){
		String sql="DELETE FROM Title where url= ?";
		return jdbcTemplate.update(sql, url) > 0;
	}

	public boolean deleteTitleByID(int id){
		String sql="DELETE FROM Title where ID= ?";
		return jdbcTemplate.update(sql, id) > 0;
	}

	public boolean deleteTitles(List<LineEntry> lineEntries){
		List<String> urls = new ArrayList<String>();
		for(LineEntry entry:lineEntries) {
			urls.add(entry.getUrl());
		}
		String sql="DELETE FROM Title where NAME In ?";
		
		return jdbcTemplate.update(sql, urls) == urls.size();
	}

	public int getRowCount(){
		String sql = "select count(*) from Title";
		return jdbcTemplate.queryForObject(
				sql, Integer.class);
	}
}
