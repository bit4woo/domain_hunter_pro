package dao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import burp.SetAndStr;
import title.IndexedHashMap;
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
			//使用和旧版本不同的表名称,避免冲突
			//String sql = "select * from TitleTable limit 1";
			String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = 'TitleTable'";
			SqlRowSet result = jdbcTemplate.queryForRowSet(sql);
			if (result.getRow() > 0 && result.getInt(0) > 0) {
				return true;
			}else {
				return false;
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean createTable() {
		String sqlTitle = "CREATE TABLE IF NOT EXISTS TitleTable" +
				"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +//自动增长 https://www.sqlite.org/autoinc.html
				" url           TEXT    NOT NULL," +
				" statuscode           INT    NOT NULL," +
				" contentLength           INT    NOT NULL," +
				" title           TEXT    NOT NULL," +
				" webcontainer           TEXT    NOT NULL," +
				
				" IPSet           TEXT    NOT NULL," +
				" CNAMESet           TEXT    NOT NULL," +
				" CertDomainSet           TEXT    NOT NULL," +
				
				" icon_hash           TEXT    NOT NULL," +
				" ASNInfo           TEXT    NOT NULL," +
				" time           TEXT    NOT NULL," +
				
				" request           BLOB    NOT NULL," +
				" response           BLOB    NOT NULL," +
				" protocol           TEXT    NOT NULL," +
				" host           TEXT    NOT NULL," +
				" port        INT    NOT NULL,"+
				
				" CheckStatus        INT    NOT NULL,"+
				" AssetType        INT    NOT NULL,"+
				" EntryType        INT    NOT NULL,"+
				" EntrySource        INT    NOT NULL,"+
				" comments        INT    NOT NULL,"+
				" EntryTags        INT    NOT NULL)";

		jdbcTemplate.execute(sqlTitle);

		//https://www.sqlitetutorial.net/sqlite-replace-statement/
		//确保replace语句是根据url作为判断依据的。
		String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_Title_url ON TitleTable (url)";
		jdbcTemplate.execute(sqlcreateIndex);

		return true;
	}

	/**
	 * 写入记录，如果URL存在相同的URL，则覆盖
	 * @param entry
	 * @return
	 */
	public boolean addOrUpdateTitle(LineEntry entry){
		String sql = "insert or replace into TitleTable (url,statuscode,contentLength,title,webcontainer,"
				+ "IPSet,CNAMESet,CertDomainSet,icon_hash,ASNInfo,time,"
				+ "protocol,host,port,request,response,"
				+ "CheckStatus,AssetType,EntryType,EntrySource,comments,EntryTags)"
				+ "values(?,?,?,?,?,"
				+ "?,?,?,?,?,?,"
				+ "?,?,?,?,?,"
				+ "?,?,?,?,?,?)";

		int result = jdbcTemplate.update(sql, 
				entry.getUrl(),entry.getStatuscode(),entry.getContentLength(),entry.getTitle(),entry.getWebcontainer(),
				SetAndStr.toStr(entry.getIPSet()),SetAndStr.toStr(entry.getCNAMESet()),
				SetAndStr.toStr(entry.getCertDomainSet()),entry.getIcon_hash(),entry.getASNInfo(),entry.getTime(),
				entry.getProtocol(),entry.getHost(),entry.getPort(),entry.getRequest(),entry.getResponse(),
				entry.getCheckStatus(),entry.getAssetType(),entry.getEntryType(),entry.getEntrySource(),
				SetAndStr.toStr(entry.getComments()),SetAndStr.toStr(entry.getEntryTags()));
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
	
	public boolean addOrUpdateTitles(IndexedHashMap<String, LineEntry> lineEntries){
		int num = 0;
		for (LineEntry entry:lineEntries.values()) {
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
		String sql = "select * from TitleTable where id=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id },new TitleMapper());
	}


	public LineEntry selectTitleByUrl(String url){
		String sql = "select * from TitleTable where url=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { url },new TitleMapper());
	}

	public List<LineEntry> selectAllTitle(){
		String sql = "select * from TitleTable";
		return jdbcTemplate.query(sql,new TitleMapper());
	}

	public boolean deleteTitle(LineEntry entry){
		String sql="DELETE FROM TitleTable where url= ?";
		return jdbcTemplate.update(sql, entry.getUrl()) > 0;
	}

	public boolean deleteTitleByUrl(String url){
		String sql="DELETE FROM TitleTable where url= ?";
		return jdbcTemplate.update(sql, url) > 0;
	}

	public boolean deleteTitleByID(int id){
		String sql="DELETE FROM TitleTable where ID= ?";
		return jdbcTemplate.update(sql, id) > 0;
	}

	public boolean deleteTitles(List<LineEntry> lineEntries){
		List<String> urls = new ArrayList<String>();
		for(LineEntry entry:lineEntries) {
			urls.add(entry.getUrl());
		}
		String sql="DELETE FROM TitleTable where url In ?";

		return jdbcTemplate.update(sql, urls) == urls.size();
	}

	public int getRowCount(){
		String sql = "select count(*) from TitleTable";
		return jdbcTemplate.queryForObject(
				sql, Integer.class);
	}
}
