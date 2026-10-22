package dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import base.IndexedHashMap;
import base.SetAndStr;
import title.LineEntry;

/**
 * 增删查改
 *
 */
public class TitleDao {

	/** 列顺序在所有写语句中必须保持一致，这里集中定义一次，避免多处硬编码写错位数 */
	private static final String COLUMNS = "url, statuscode, contentLength, title, webcontainer, "
			+ "IPSet, CNAMESet, CertDomainSet, icon_url, icon_bytes, icon_hash, ASNInfo, time, "
			+ "protocol, host, port, request, response, "
			+ "CheckStatus, AssetType, EntryType, EntrySource, comments, EntryTags";

	private static final String VALUES_24 = "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 单行/批量共用的 upsert 语句。
	 * <p>
	 * 集中定义一次：原来单行写和批量写各自硬编码了同一份 24 个字段的 SQL，
	 * 改字段时很容易只改一处、导致两边字段顺序错位（这种 bug 只在特定路径出现，很难查）。
	 * </p>
	 */
	private static final String UPSERT_SQL = "INSERT INTO TitleTable (" + COLUMNS + ") "
			+ VALUES_24 + " ON CONFLICT(url) DO UPDATE SET "
			+ "statuscode = excluded.statuscode, contentLength = excluded.contentLength, "
			+ "title = excluded.title, webcontainer = excluded.webcontainer, IPSet = excluded.IPSet, "
			+ "CNAMESet = excluded.CNAMESet, CertDomainSet = excluded.CertDomainSet, "
			+ "icon_url = excluded.icon_url, icon_bytes = excluded.icon_bytes, icon_hash = excluded.icon_hash, "
			+ "ASNInfo = excluded.ASNInfo, time = excluded.time, protocol = excluded.protocol, "
			+ "host = excluded.host, port = excluded.port, request = excluded.request, "
			+ "response = excluded.response, CheckStatus = excluded.CheckStatus, "
			+ "AssetType = excluded.AssetType, EntryType = excluded.EntryType, "
			+ "EntrySource = excluded.EntrySource, comments = excluded.comments, EntryTags = excluded.EntryTags";

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	/** 本 DAO 对应的数据库文件路径，供批量写入/日志使用 */
	private final String dbFilePath;

	public TitleDao(String dbFilePath){
		this.dbFilePath = dbFilePath;
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!testSelect()){
			createTable();
		}
		addColumnIfNotExists();
	}

	public String getDbFilePath() {
		return dbFilePath;
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

				" icon_url           TEXT    NOT NULL," +
				" icon_bytes           BLOB    NOT NULL," +
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

	public void addColumnIfNotExists() {
		boolean columnExists = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pragma_table_info(?) WHERE name = ?",
				new Object[]{"TitleTable", "icon_url"},
				Integer.class
				) > 0;

				if (!columnExists) {
					// Column doesn't exist, so add it
					String addColumnSQL = "ALTER TABLE TitleTable "
							+ "ADD COLUMN icon_url TEXT;";
					String addColumnSQL1 = "ALTER TABLE TitleTable "
							+ "ADD COLUMN icon_bytes BLOB;";
					jdbcTemplate.execute(addColumnSQL);
					jdbcTemplate.execute(addColumnSQL1);
				}
	}

	/**
	 * 写入记录，如果URL存在相同的URL，则覆盖
	 * @param entry
	 * @return
	 * 
	 * replace 的逻辑不是在原始记录上修改，而是删除旧数据，添加新数据，导致ID变化。即修改后的数据会排到最后。
	 */
	@Deprecated
	public boolean addOrUpdateTitle_old(LineEntry entry){
		String sql = "insert or replace into TitleTable (url,statuscode,contentLength,title,webcontainer,"
				+ "IPSet,CNAMESet,CertDomainSet,icon_url,icon_bytes,icon_hash,ASNInfo,time,"
				+ "protocol,host,port,request,response,"
				+ "CheckStatus,AssetType,EntryType,EntrySource,comments,EntryTags)"
				+ "values(?,?,?,?,?,"
				+ "?,?,?,?,?,?,?,?,"
				+ "?,?,?,?,?,"
				+ "?,?,?,?,?,?)";

		int result = jdbcTemplate.update(sql, 
				entry.getUrl(),entry.getStatuscode(),entry.getContentLength(),entry.getTitle(),entry.getWebcontainer(),
				SetAndStr.toStr(entry.getIPSet()),SetAndStr.toStr(entry.getCNAMESet()),
				SetAndStr.toStr(entry.getCertDomainSet()),entry.getIcon_url(),entry.getIcon_bytes(),entry.getIcon_hash(),entry.getASNInfo(),entry.getTime(),
				entry.getProtocol(),entry.getHost(),entry.getPort(),entry.getRequest(),entry.getResponse(),
				entry.getCheckStatus(),entry.getAssetType(),entry.getEntryType(),entry.getEntrySource(),
				SetAndStr.toStr(entry.getComments()),SetAndStr.toStr(entry.getEntryTags()));
		return result > 0;
	}

	
	public boolean addOrUpdateTitle(LineEntry entry) {
		int result = jdbcTemplate.update(UPSERT_SQL,
				entry.getUrl(), entry.getStatuscode(), entry.getContentLength(), entry.getTitle(), entry.getWebcontainer(),
				SetAndStr.toStr(entry.getIPSet()), SetAndStr.toStr(entry.getCNAMESet()),
				SetAndStr.toStr(entry.getCertDomainSet()), entry.getIcon_url(), entry.getIcon_bytes(), entry.getIcon_hash(),
				entry.getASNInfo(), entry.getTime(),
				entry.getProtocol(), entry.getHost(), entry.getPort(), entry.getRequest(), entry.getResponse(),
				entry.getCheckStatus(), entry.getAssetType(), entry.getEntryType(), entry.getEntrySource(),
				SetAndStr.toStr(entry.getComments()), SetAndStr.toStr(entry.getEntryTags()));
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
	/**
	 * 批量 upsert，由调用方负责事务。
	 * <p>
	 * 与 {@link #addOrUpdateTitle(LineEntry)} 用同一条 SQL，只是改成 addBatch 提交，
	 * 由调用方统一控制事务边界。
	 * </p>
	 *
	 * @param conn    外部管理的连接，本方法不 commit、不 close
	 * @param entries 待写入的记录
	 * @return 写入行数
	 */
	public int upsertBatch(Connection conn, List<LineEntry> entries) throws SQLException {
		if (entries == null || entries.isEmpty()) {
			return 0;
		}
		int count = 0;
		try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
			for (LineEntry entry : entries) {
				if (entry == null || entry.getUrl() == null) {
					continue;
				}
				ps.setString(1, entry.getUrl());
				ps.setInt(2, entry.getStatuscode());
				ps.setInt(3, entry.getContentLength());
				ps.setString(4, entry.getTitle());
				ps.setString(5, entry.getWebcontainer());
				ps.setString(6, SetAndStr.toStr(entry.getIPSet()));
				ps.setString(7, SetAndStr.toStr(entry.getCNAMESet()));
				ps.setString(8, SetAndStr.toStr(entry.getCertDomainSet()));
				ps.setString(9, entry.getIcon_url());
				ps.setBytes(10, entry.getIcon_bytes());
				ps.setString(11, entry.getIcon_hash());
				ps.setString(12, entry.getASNInfo());
				ps.setString(13, entry.getTime());
				ps.setString(14, entry.getProtocol());
				ps.setString(15, entry.getHost());
				ps.setInt(16, entry.getPort());
				ps.setBytes(17, entry.getRequest());
				ps.setBytes(18, entry.getResponse());
				ps.setString(19, entry.getCheckStatus());
				ps.setString(20, entry.getAssetType());
				ps.setString(21, entry.getEntryType());
				ps.setString(22, entry.getEntrySource());
				ps.setString(23, SetAndStr.toStr(entry.getComments()));
				ps.setString(24, SetAndStr.toStr(entry.getEntryTags()));
				ps.addBatch();
				count++;
			}
			ps.executeBatch();
		}
		return count;
	}

	/**
	 * 批量删除，由调用方负责事务。
	 */
	public int deleteBatch(Connection conn, List<String> urls) throws SQLException {
		if (urls == null || urls.isEmpty()) {
			return 0;
		}
		int count = 0;
		try (PreparedStatement ps = conn.prepareStatement("DELETE FROM TitleTable where url=?")) {
			for (String url : urls) {
				ps.setString(1, url);
				ps.addBatch();
				count++;
			}
			ps.executeBatch();
		}
		return count;
	}

	public LineEntry selectTitleByID(int id){
		String sql = "select * from TitleTable where id=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id },new TitleMapper());
	}


	public LineEntry selectTitleByUrl(String url){
		String sql = "select * from TitleTable where url=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { url },new TitleMapper());
	}

	/**
	 * 加载全部记录，包含 request/response。
	 * <p>
	 * 报文一并加载（而不是只查列表列）是刻意的选择：标题搜索要匹配报文内容
	 * （见 SearchManager.textFilter / SearchRegex / SearchStringDork 的 request:/response:），
	 * 只查元数据会让搜索静默漏结果。代价是内存占用随记录数线性增长。
	 * </p>
	 */
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
	public boolean clearData(){
		//DELETE 命令会删除表中的所有行，但表的结构仍然会保留。如果你想连表结构也一并清空，可以使用 TRUNCATE 命令
		String sql="DELETE FROM TitleTable";
		return jdbcTemplate.update(sql) >= 1;
	}


	public int getRowCount(){
		String sql = "select count(*) from TitleTable";
		return jdbcTemplate.queryForObject(
				sql, Integer.class);
	}
}
