package dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.alibaba.fastjson.JSON;

import domain.DomainManager;
import domain.TextAreaType;

public class DomainDao {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	/** 本 DAO 对应的数据库文件路径，供批量写入/日志使用 */
	private final String dbFilePath;

	public static void main(String[] args) {
		System.out.println("111");
		System.out.println(new DomainDao("C:\\Users\\P52\\Documents\\test.db").testSelect());
	}

	public DomainDao(String dbFilePath){
		this.dbFilePath = dbFilePath;
		dataSource = DBUtils.getSqliteDataSource(dbFilePath);
		jdbcTemplate = new JdbcTemplate(dataSource);
		if (!testSelect()){
			createTable();
		}
	}

	public String getDbFilePath() {
		return dbFilePath;
	}

	public DomainDao(File dbFile){
		this(dbFile.toString());
	}

	public boolean testSelect(){
		try {
			//使用和旧版本不同的表名称,避免冲突
			//String sql = "select * from DomainTable limit 1";
			String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = 'DomainTable'";
			SqlRowSet result = jdbcTemplate.queryForRowSet(sql);
			if (result.getRow() > 0 && result.getInt(0) > 0) {
				return true;
			}else {
				return false;
			}
		} catch (DataAccessException e) {
			//e.printStackTrace();
			return false;
		}
	}

	/**
	 * 一行一个type，比如subdomain存一行，similardomain存另外一行。
	 * @return
	 */
	public boolean createTable() {
		//AUTOINCREMENT is only allowed on an INTEGER PRIMARY KEY
		String sql = "CREATE TABLE IF NOT EXISTS DomainTable" +
				"(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
				" Type           TEXT    NOT NULL," +
				" Content        TEXT    NOT NULL)";
		jdbcTemplate.execute(sql);

		//https://www.sqlitetutorial.net/sqlite-replace-statement/
		//确保replace语句是根据Type作为判断依据的。
		String sqlcreateIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_DomainTable_Type ON DomainTable (Type)";
		jdbcTemplate.execute(sqlcreateIndex);

		return true;
	}

	public boolean createOrUpdateByType(Set<String> content,TextAreaType Type){
		String sql = "insert or replace into DomainTable (Type,Content)"
				+ " values(?,?)";

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


	/**
	 * 从数据库中恢复出DomainManager对象，记得设置GUIMain参数
	 * @return
	 */
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

	/**
	 * 保存全部 type。
	 * <p>
	 * 只拍一次快照再逐 type 写入：如果按 type 循环调用
	 * {@link #saveDomainManagerByType}，每个 type 都会重新拍一次快照，
	 * 十个 type 就是十次全量拷贝（子域名多的时候是十次几万条的复制）。
	 * </p>
	 */
	public boolean saveDomainManager(DomainManager domainResult){
		boolean allOk = true;
		for (Map.Entry<TextAreaType, Set<String>> entry : snapshotOf(domainResult).entrySet()) {
			if (!createOrUpdateByType(entry.getValue(), entry.getKey())) {
				allOk = false;
			}
		}
		return allOk;
	}

	/**
	 * 把 DomainManager 的十个集合拍成 type -> 内容 的快照。
	 * <p>
	 * 为什么集中在这里：type 与集合的对应关系原来只存在于 saveDomainManagerByType 的
	 * switch 里，而现在"单条保存""整块保存""异步批量保存"三条路径都需要这份映射，
	 * 分散写迟早会漂移（比如新增一个 type 只改了两处）。统一到这里，改一处即可。
	 * </p>
	 * <p>
	 * 返回的是<b>新集合</b>：调用方可以安全地把它交给后台写线程，
	 * 不必担心 UI 线程随后修改 DomainManager 里的集合导致并发问题。
	 * </p>
	 */
	public static Map<TextAreaType, Set<String>> snapshotOf(DomainManager domainResult) {
		Map<TextAreaType, Set<String>> snapshot = new LinkedHashMap<TextAreaType, Set<String>>();
		snapshot.put(TextAreaType.SubDomain, new HashSet<String>(domainResult.getSubDomainSet()));
		snapshot.put(TextAreaType.RelatedDomain, new HashSet<String>(domainResult.getRelatedDomainSet()));
		snapshot.put(TextAreaType.SimilarDomain, new HashSet<String>(domainResult.getSimilarDomainSet()));
		snapshot.put(TextAreaType.Email, new HashSet<String>(domainResult.getEmailSet()));
		snapshot.put(TextAreaType.SimilarEmail, new HashSet<String>(domainResult.getSimilarEmailSet()));
		snapshot.put(TextAreaType.IPSetOfSubnet, new HashSet<String>(domainResult.getIPSetOfSubnet()));
		snapshot.put(TextAreaType.IPSetOfCert, new HashSet<String>(domainResult.getIPSetOfCert()));
		snapshot.put(TextAreaType.SpecialPortTarget, new HashSet<String>(domainResult.getSpecialPortTargets()));
		snapshot.put(TextAreaType.PackageName, new HashSet<String>(domainResult.getPackageNameSet()));
		snapshot.put(TextAreaType.BlackIP, new HashSet<String>(domainResult.getNotTargetIPSet()));
		return snapshot;
	}

	public boolean saveDomainManagerByType(DomainManager domainResult,TextAreaType type){
		try {
			Set<String> content = snapshotOf(domainResult).get(type);
			if (content == null) {
				return false;
			}
			return createOrUpdateByType(content, type);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 批量写入多个 type 的内容，由调用方负责事务（commit / rollback）。
	 * <p>
	 * 为什么需要这个方法：DomainTable 的存储粒度是"一个 type 一行（整块 JSON）"，
	 * 每次保存都要重写整行。如果一次操作要同时刷新多个 type（保存项目、刷新后保存），
	 * 用 {@link #createOrUpdateByType} 会走多次独立事务，每个事务都要 fsync 一次；
	 * 而这里把它们塞进同一个事务、一次 executeBatch，写放大和延迟都低一个数量级。
	 * </p>
	 *
	 * @param conn           外部管理的连接，本方法不负责关闭，也不会 commit
	 * @param contentByType  type -> 该 type 的完整内容快照
	 * @return 实际写入的行数
	 */
	public int saveTypes(Connection conn, Map<TextAreaType, Set<String>> contentByType) throws SQLException {
		if (contentByType == null || contentByType.isEmpty()) {
			return 0;
		}
		String sql = "insert or replace into DomainTable (Type,Content) values(?,?)";
		int count = 0;
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (Map.Entry<TextAreaType, Set<String>> entry : contentByType.entrySet()) {
				ps.setString(1, entry.getKey().toString());
				ps.setString(2, JSON.toJSONString(entry.getValue()));
				ps.addBatch();
				count++;
			}
			if (count > 0) {
				ps.executeBatch();
			}
		}
		return count;
	}
}
