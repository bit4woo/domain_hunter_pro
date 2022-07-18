package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import title.IndexedLinkedHashMap;
import title.LineEntry;

/**
 * 增删查改
 *
 */
public class TitleDao {
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	void setDataSource(DataSource ds) {
		dataSource = ds;
		jdbcTemplate = new JdbcTemplate(ds);
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
		return true;
	}

	/**
	 * 写入记录，如果URL存在相同的URL，是否需要
	 * @param entry
	 * @param overWriteIfExist
	 * @return
	 */
	public boolean addTitle(LineEntry entry,boolean overWriteIfExist){
		String sql;
		if (overWriteIfExist) {
			sql = "insert or replace into DOMAINObject (url,statuscode,contentLength,title,IP,CDN"
					+ "webcontainer,time,icon_hash,ASNInfo,request,response,"
					+ "protocol,host,port,"
					+ "CheckStatus,AssetType,EntryType,comment,isManualSaved)"
					+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		}else {
			sql = "insert into DOMAINObject (url,statuscode,contentLength,title,IP,CDN"
					+ "webcontainer,time,icon_hash,ASNInfo,request,response,protocol,host,port)"
					+ "webcontainer,time,icon_hash,ASNInfo,request,response,"
					+ "protocol,host,port,"
					+ "CheckStatus,AssetType,EntryType,comment,isManualSaved)"
					+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}

		int result = jdbcTemplate.update(sql, entry.getUrl(),entry.getStatuscode(),entry.getContentLength(),entry.getTitle(),
				entry.getIP(),entry.getCDN(),entry.getWebcontainer(),entry.getTime(),entry.getIcon_hash(),entry.getASNInfo(),
				entry.getRequest(),entry.getResponse(),
				entry.getProtocol(),entry.getHost(),entry.getPort(),
				entry.getCheckStatus(),entry.getAssetType(),entry.getEntryType(),entry.getComment(),entry.isManualSaved());
		return result > 0;
		
	}


	public boolean addTitles(LinkedHashMap<String,LineEntry> lineEntries){
		try {
			conn = getConnection();
			String sql="insert into Title(NAME,Content) values(?,?)";
			pres=conn.prepareStatement(sql);
			for(String key:lineEntries.keySet()){
				pres.setString(1, key);
				pres.setString(2,lineEntries.get(key).ToJson());
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == lineEntries.size()){
				System.out.println("add titles successfully");
				return true;
			}else {
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}


	public IndexedLinkedHashMap<String,LineEntry> getTitles(){
		IndexedLinkedHashMap<String,LineEntry> lineEntriesMap=new IndexedLinkedHashMap<String,LineEntry>();
		try {
			conn = getConnection();
			String sql="select * from Title";
			pres=conn.prepareStatement(sql);

			ResultSet res=pres.executeQuery();
			while(res.next()){
				String LineJson=res.getString("Content");
				LineEntry entry = LineEntry.FromJson(LineJson);
				lineEntriesMap.put(entry.getUrl(), entry);
			}
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		System.out.println(lineEntriesMap.size() +" title lines found from DB file");
		return lineEntriesMap;
	}


	public void updateTitle(LineEntry entry){
		String sql="update Title SET Content=? where NAME=?";
		//UPDATE Person SET FirstName = 'Fred' WHERE LastName = 'Wilson' 

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			pres.setString(1, entry.ToJson());
			pres.setString(2, entry.getUrl());
			pres.executeUpdate();
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
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
				System.out.println("update titles successfully: "+lineEntries.size());
				return true;
			}else {
				System.out.println("update titles failed");
				stderr.println("update titles failed");
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace();
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		}finally {
			destroy();
		}
		return false;
	}


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
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
	}

	public synchronized boolean deleteTitles(List<LineEntry> lineEntries){
		List<String> urls = new ArrayList<String>();
		for(LineEntry entry:lineEntries) {
			urls.add(entry.getUrl());
		}
		return deleteTitlesByUrl(urls);
	}


	public synchronized boolean deleteTitlesByUrl(List<String> urls){
		String sql="DELETE FROM Title where NAME= ?";
		//DELETE FROM Person WHERE LastName = 'Wilson'

		try {
			conn = getConnection();
			pres=conn.prepareStatement(sql);
			for(String url:urls){
				pres.setString(1, url);
				pres.addBatch();                                   //实现批量插入
			}
			int[] result = pres.executeBatch();                                   //批量插入到数据库中
			if ( IntStream.of(result).sum() == urls.size()){
				System.out.println("delete titles successfully: "+urls.size());
				return true;
			}else {
				return false;
			}
		} catch(SQLiteException e) {
			if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
				log.error("SQLITE_BUSY The database file is locked when call addTitles()");
			}
			e.printStackTrace(stderr);
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace(stderr);
		} finally {
			destroy();
		}
		return false;
	}
}
