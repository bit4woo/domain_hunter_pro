package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import domain.DomainManager;
import domain.TextAreaType;
import domain.target.TargetEntry;
import title.LineEntry;

/**
 * 数据库写入的统一入口（单写线程 + 批量 + 单事务）。
 *
 * <h3>为什么需要这一层</h3>
 * <p>
 * 改造前，写数据库这件事散落在 UI 事件里：单元格编辑、双击切换状态、批量操作、
 * 文本框的每一次按键、以及每 2 分钟的自动保存，全都直接调 DAO。
 * 由此带来四个问题：
 * </p>
 * <ol>
 *   <li><b>EDT 上有 SQL</b>。Swing 的事件线程一旦等 SQLite 的 fsync，整个界面就卡住；</li>
 *   <li><b>写次数 = 事件次数</b>。敲一个字符就整块重写一次子域名 JSON（可能是几 MB）；</li>
 *   <li><b>SQLITE_BUSY</b>。流量线程、爬虫线程、EDT 同时写同一个文件，
 *       SQLite 只允许一个写者，冲突时直接抛错；</li>
 *   <li><b>没有事务边界</b>。一次逻辑保存被拆成几十条独立事务，中途失败就留下半个状态。</li>
 * </ol>
 *
 * <h3>怎么解决</h3>
 * <ul>
 *   <li><b>单写线程</b>：所有写操作进同一个阻塞队列，天然串行，从根上消除写冲突；</li>
 *   <li><b>按主键合并</b>：同一个 target/url/type 在排队期间被多次写入时，只保留最后一次。
 *       因为跨主键的先后顺序对最终结果没有影响，合并是安全的；</li>
 *   <li><b>linger 窗口</b>：取到第一个任务后不立刻写，先等 {@link #LINGER_MS} 毫秒，
 *       把这段时间内到达的写合并成一批。这就是文本框 debounce 的实现方式，
 *       不需要额外的 Timer；连续输入时最多等 {@link #MAX_LINGER_MS} 就必须落盘，避免饿死；</li>
 *   <li><b>单事务批量</b>：一批任务在一个事务里用 addBatch 执行完再 commit，
 *       提交次数从 O(n) 降到 O(1)；三张表也在同一个事务里，保存动作具备原子性；</li>
 *   <li><b>失败回退</b>：整批失败时回滚，然后逐条重试，单条坏数据不会拖垮整批。</li>
 * </ul>
 *
 * <h3>语义与限制</h3>
 * <ul>
 *   <li>写入是异步的：{@code upsertTarget} 返回时数据可能还没落库。
 *       需要"确实落盘"的地方（保存项目、另存为、备份、关闭前）必须调用 {@link #flush(long)}；</li>
 *   <li>写线程是守护线程，JVM 退出时未写入的任务会丢。所以关键动作要 flush，
 *       不能指望"退出时自动保存"；</li>
 *   <li>同一批最多 {@link #MAX_BATCH} 个任务，超过就切分事务。
 *       也就是说"保存项目"的原子性以单批为界，超大项目会是若干个事务，而不是一个。</li>
 * </ul>
 */
public final class DbWriter {

	private static final Logger log = LogManager.getLogger(DbWriter.class);

	private static final DbWriter INSTANCE = new DbWriter();

	/** 空闲时的轮询间隔：决定 flush() 的唤醒延迟上限 */
	private static final long IDLE_POLL_MS = 100;
	/** 合并窗口：拿到第一个任务后再等这么久，把同类写攒成一批（即 debounce 时长） */
	private static final long LINGER_MS = 300;
	/** 合并窗口的上限：持续高频写入时不能无限等下去，到点必须落盘 */
	private static final long MAX_LINGER_MS = 1500;
	/** 单批最多任务数：太大则单事务过长、内存占用高；太小则提交次数多 */
	private static final int MAX_BATCH = 2000;

	private enum Kind {
		TARGET_UPSERT, TARGET_DELETE, DOMAIN_SAVE, TITLE_UPSERT, TITLE_DELETE, FLUSH
	}

	/**
	 * 一条写任务。
	 * <p>
	 * 注意 target / domain 走的是<b>快照</b>，title 走的是<b>引用</b>：
	 * 前者数据量小，拍快照可以彻底避免 UI 线程与写线程共享可变对象；
	 * 后者带报文（可能几十 KB），每条都复制会显著增加内存与 GC 压力，
	 * 这与其原本的行为一致（原本也是把引用直接交给写线程）。
	 * </p>
	 */
	private static final class WriteTask {
		final Kind kind;
		final String dbFilePath;
		/** 合并键：target 值 / url / type 名；FLUSH 任务为 null */
		final String key;
		final TargetEntry target;
		final TextAreaType domainType;
		final Set<String> domainValues;
		final LineEntry title;
		final CountDownLatch latch;

		private WriteTask(Kind kind, String dbFilePath, String key, TargetEntry target, TextAreaType domainType,
				Set<String> domainValues, LineEntry title, CountDownLatch latch) {
			this.kind = kind;
			this.dbFilePath = dbFilePath;
			this.key = key;
			this.target = target;
			this.domainType = domainType;
			this.domainValues = domainValues;
			this.title = title;
			this.latch = latch;
		}
	}

	/** 同一个数据库文件、同一批次的待写内容；三个 map 都按 key 合并 */
	private static final class Batch {
		final Map<TextAreaType, Set<String>> domains = new LinkedHashMap<TextAreaType, Set<String>>();
		final Map<String, WriteTask> targets = new LinkedHashMap<String, WriteTask>();
		final Map<String, WriteTask> titles = new LinkedHashMap<String, WriteTask>();

		boolean isEmpty() {
			return domains.isEmpty() && targets.isEmpty() && titles.isEmpty();
		}

		int size() {
			return domains.size() + targets.size() + titles.size();
		}
	}

	private final BlockingQueue<WriteTask> queue = new LinkedBlockingQueue<WriteTask>();
	/** 按数据库文件分组的待写内容（另存为时会有两个文件并存） */
	private final Map<String, Batch> batches = new LinkedHashMap<String, Batch>();
	/** 本批第一个任务入队的时刻，用于 MAX_LINGER_MS 判断 */
	private long firstTaskTime = 0;

	private final AtomicLong committedTasks = new AtomicLong();
	private final AtomicLong failedBatches = new AtomicLong();

	private DbWriter() {
		Thread writer = new Thread(this::loop, "db-writer");
		writer.setDaemon(true);
		writer.start();
	}

	public static DbWriter getInstance() {
		return INSTANCE;
	}

	///////////// 对外写入 API /////////////

	/**
	 * 新增或更新一个 target。入队即返回，不阻塞调用线程（通常是 EDT）。
	 */
	public void upsertTarget(String dbFilePath, TargetEntry entry) {
		if (dbFilePath == null || entry == null || entry.getTarget() == null) {
			return;
		}
		// 快照：入队之后 UI 还会继续改这个对象，写线程不能读到"改到一半"的状态
		TargetEntry snapshot = entry.snapshotForPersist();
		queue.offer(new WriteTask(Kind.TARGET_UPSERT, dbFilePath, snapshot.getTarget(), snapshot, null, null, null, null));
	}

	public void deleteTarget(String dbFilePath, String target) {
		if (dbFilePath == null || target == null) {
			return;
		}
		queue.offer(new WriteTask(Kind.TARGET_DELETE, dbFilePath, target, null, null, null, null, null));
	}

	/**
	 * 保存某个 TextArea 类型的内容（整块覆盖）。
	 * <p>
	 * 传入的是<b>快照</b>：调用方（DocumentListener）在按键事件里先取到当前文本行，
	 * 之后文本还会继续变化，写线程必须写"入队那一刻"的内容。
	 * 同一个 type 在 linger 窗口内被反复提交时只会写一次。
	 * </p>
	 */
	public void saveDomainType(String dbFilePath, TextAreaType type, Set<String> values) {
		if (dbFilePath == null || type == null || values == null) {
			return;
		}
		Set<String> snapshot = new java.util.HashSet<String>(values);
		queue.offer(new WriteTask(Kind.DOMAIN_SAVE, dbFilePath, type.name(), null, type, snapshot, null, null));
	}

	/**
	 * 保存 DomainManager 的全部类型（十个 type 各自入队，按 type 合并）。
	 */
	public void saveAllDomains(String dbFilePath, DomainManager domainManager) {
		if (dbFilePath == null || domainManager == null) {
			return;
		}
		for (Map.Entry<TextAreaType, Set<String>> entry : DomainDao.snapshotOf(domainManager).entrySet()) {
			saveDomainType(dbFilePath, entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 批量保存 target（另存为、导入、手动保存项目时使用）。
	 */
	public void saveAllTargets(String dbFilePath, Iterable<TargetEntry> entries) {
		if (dbFilePath == null || entries == null) {
			return;
		}
		for (TargetEntry entry : entries) {
			upsertTarget(dbFilePath, entry);
		}
	}

	public void upsertTitle(String dbFilePath, LineEntry entry) {
		if (dbFilePath == null || entry == null || entry.getUrl() == null) {
			return;
		}
		queue.offer(new WriteTask(Kind.TITLE_UPSERT, dbFilePath, entry.getUrl(), null, null, null, entry, null));
	}

	public void deleteTitle(String dbFilePath, String url) {
		if (dbFilePath == null || url == null) {
			return;
		}
		queue.offer(new WriteTask(Kind.TITLE_DELETE, dbFilePath, url, null, null, null, null, null));
	}

	public void saveAllTitles(String dbFilePath, Iterable<LineEntry> entries) {
		if (dbFilePath == null || entries == null) {
			return;
		}
		for (LineEntry entry : entries) {
			upsertTitle(dbFilePath, entry);
		}
	}

	/**
	 * 冲刷：等待调用时刻之前提交的所有写任务全部落盘。
	 * <p>
	 * 实现方式：往队列尾部放一个 FLUSH 任务，写线程处理到它时先把已攒的批次写完，
	 * 再 countDown；因为队列是 FIFO 且只有一个写线程，栅栏被处理到就意味着之前的写都执行完了。
	 * </p>
	 *
	 * @param timeoutMillis 最长等待时间
	 * @return true 表示在超时前全部写完
	 */
	public boolean flush(long timeoutMillis) {
		CountDownLatch latch = new CountDownLatch(1);
		queue.offer(new WriteTask(Kind.FLUSH, null, null, null, null, null, null, latch));
		try {
			return latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/** 当前待写任务数（用于监控写队列是否积压） */
	public int getPendingCount() {
		synchronized (batches) {
			int size = queue.size();
			for (Batch batch : batches.values()) {
				size += batch.size();
			}
			return size;
		}
	}

	public long getCommittedTasks() {
		return committedTasks.get();
	}

	public long getFailedBatches() {
		return failedBatches.get();
	}

	///////////// 写线程 /////////////

	private void loop() {
		while (true) {
			try {
				boolean hasPending = hasPending();
				WriteTask task = queue.poll(hasPending ? LINGER_MS : IDLE_POLL_MS, TimeUnit.MILLISECONDS);

				if (task == null) {
					// linger 窗口到期：把这段时间攒下来的写合并成一批提交
					if (hasPending()) {
						commitPending();
					}
					continue;
				}

				if (task.kind == Kind.FLUSH) {
					try {
						commitPending();
					} catch (Throwable t) {
						// 落盘失败也要放行等待者：否则 flush() 会一直等到超时，
						// 调用方（保存项目/备份）会被无谓地卡住 30~120 秒
						log.error("flush failed while committing pending tasks", t);
					} finally {
						if (task.latch != null) {
							task.latch.countDown();
						}
					}
					continue;
				}

				add(task);

				long now = System.currentTimeMillis();
				if (firstTaskTime == 0) {
					firstTaskTime = now;
				}
				// 连续高频写入时不能无限攒：到上限就落盘
				if (pendingSize() >= MAX_BATCH || now - firstTaskTime >= MAX_LINGER_MS) {
					commitPending();
				}
			} catch (InterruptedException e) {
				// 进程退出/线程被中断：保留中断标志，未写的任务由 flush 或守护线程退出处理
				Thread.currentThread().interrupt();
				log.warn("db writer interrupted, pending tasks may be lost");
				return;
			} catch (Throwable t) {
				// 写线程不能因为单次异常退出，否则之后所有写都会静默丢失
				log.error("db writer loop error", t);
			}
		}
	}

	private void add(WriteTask task) {
		synchronized (batches) {
			Batch batch = batches.get(task.dbFilePath);
			if (batch == null) {
				batch = new Batch();
				batches.put(task.dbFilePath, batch);
			}
			// 按 key 合并：同一个主键的多次写只保留最后一次，最终状态一致
			switch (task.kind) {
			case DOMAIN_SAVE:
				batch.domains.put(task.domainType, task.domainValues);
				break;
			case TARGET_UPSERT:
			case TARGET_DELETE:
				batch.targets.put(task.key, task);
				break;
			case TITLE_UPSERT:
			case TITLE_DELETE:
				batch.titles.put(task.key, task);
				break;
			default:
				break;
			}
		}
	}

	private boolean hasPending() {
		synchronized (batches) {
			for (Batch batch : batches.values()) {
				if (!batch.isEmpty()) {
					return true;
				}
			}
			return false;
		}
	}

	private int pendingSize() {
		synchronized (batches) {
			int size = 0;
			for (Batch batch : batches.values()) {
				size += batch.size();
			}
			return size;
		}
	}

	/**
	 * 把当前攒下的所有批次落盘。
	 * <p>
	 * 先把 {@link #batches} 整体摘走再写，避免写库期间新到的任务被混进"正在提交"的批次；
	 * 新任务会在下一轮循环里形成新批次。
	 * </p>
	 */
	private void commitPending() {
		Map<String, Batch> toWrite;
		synchronized (batches) {
			if (batches.isEmpty()) {
				firstTaskTime = 0;
				return;
			}
			toWrite = new LinkedHashMap<String, Batch>(batches);
			batches.clear();
			firstTaskTime = 0;
		}
		for (Map.Entry<String, Batch> entry : toWrite.entrySet()) {
			try {
				writeBatch(entry.getKey(), entry.getValue());
			} catch (Throwable t) {
				// 一个项目文件写失败（磁盘满、文件被删）不能把另一个项目的写也一起丢掉
				log.error("write batch aborted for " + entry.getKey(), t);
			}
		}
	}

	private void writeBatch(String dbFilePath, Batch batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}
		ProjectDaos daos = DaoFactory.of(dbFilePath);

		// !!! 顺序很重要：DAO 的构造函数里可能包含 CREATE TABLE / PRAGMA / 迁移 DDL。
		// 这些 DDL 走的是另一条连接；如果它们发生在"批量事务已经拿到写锁"之后，
		// SQLite 会让第二条连接直接抛 SQLITE_BUSY（实测就是在这里炸的）。
		// 所以先把需要用到的 DAO 全部构造好（完成建表），再开事务连接。
		try {
			if (!batch.domains.isEmpty()) {
				daos.domainDao();
			}
			if (!batch.targets.isEmpty()) {
				daos.targetDao();
			}
			if (!batch.titles.isEmpty()) {
				daos.titleDao();
			}
		} catch (Exception e) {
			log.error("cannot prepare tables for " + dbFilePath, e);
			failedBatches.incrementAndGet();
			return;
		}

		Connection conn = null;
		try {
			conn = daos.getConnection();
			boolean oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try {
				apply(conn, daos, batch);
				conn.commit();
				committedTasks.addAndGet(batch.size());
			} catch (Exception e) {
				rollbackQuietly(conn);
				failedBatches.incrementAndGet();
				log.error("batch write failed (file=" + dbFilePath + ", tasks=" + batch.size()
						+ "), retry row by row", e);
				applyRowByRow(dbFilePath, daos, batch);
			} finally {
				try {
					conn.setAutoCommit(oldAutoCommit);
				} catch (SQLException ignore) {
					// 恢复失败无所谓，连接马上就被关闭了
				}
			}
		} catch (Exception e) {
			log.error("cannot get connection for " + dbFilePath, e);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}

	/**
	 * 一个事务里写三张表。
	 * <p>
	 * 三张表互相独立，没有先后依赖；放在同一事务里的意义是"保存动作要么全成、要么全不成"，
	 * 而不是为了顺序。
	 * </p>
	 */
	private void apply(Connection conn, ProjectDaos daos, Batch batch) throws SQLException {
		if (!batch.domains.isEmpty()) {
			daos.domainDao().saveTypes(conn, batch.domains);
		}

		List<TargetEntry> targetUpserts = new ArrayList<TargetEntry>();
		List<String> targetDeletes = new ArrayList<String>();
		for (WriteTask task : batch.targets.values()) {
			if (task.kind == Kind.TARGET_UPSERT) {
				targetUpserts.add(task.target);
			} else {
				targetDeletes.add(task.key);
			}
		}
		if (!targetUpserts.isEmpty()) {
			daos.targetDao().upsertBatch(conn, targetUpserts);
		}
		if (!targetDeletes.isEmpty()) {
			daos.targetDao().deleteBatch(conn, targetDeletes);
		}

		List<LineEntry> titleUpserts = new ArrayList<LineEntry>();
		List<String> titleDeletes = new ArrayList<String>();
		for (WriteTask task : batch.titles.values()) {
			if (task.kind == Kind.TITLE_UPSERT) {
				titleUpserts.add(task.title);
			} else {
				titleDeletes.add(task.key);
			}
		}
		if (!titleUpserts.isEmpty()) {
			daos.titleDao().upsertBatch(conn, titleUpserts);
		}
		if (!titleDeletes.isEmpty()) {
			daos.titleDao().deleteBatch(conn, titleDeletes);
		}
	}

	/**
	 * 整批失败后的兜底：逐条、各自独立事务地重试。
	 * <p>
	 * 为什么不直接放弃这一批：SQLite 的错误常常是单条数据引起的
	 * （NOT NULL 约束、超长字符串、坏字节），整批回滚会把 2000 条好数据一起丢掉。
	 * 逐条重试的代价只在出错路径上付出，正常路径不受影响。
	 * </p>
	 */
	private void applyRowByRow(String dbFilePath, ProjectDaos daos, Batch batch) {
		for (Map.Entry<TextAreaType, Set<String>> entry : batch.domains.entrySet()) {
			try {
				daos.domainDao().createOrUpdateByType(entry.getValue(), entry.getKey());
			} catch (Exception e) {
				log.error("domain write failed: type=" + entry.getKey(), e);
			}
		}
		for (WriteTask task : batch.targets.values()) {
			try {
				if (task.kind == Kind.TARGET_UPSERT) {
					daos.targetDao().addOrUpdateTarget(task.target);
				} else {
					daos.targetDao().deleteByTarget(task.key);
				}
			} catch (Exception e) {
				log.error("target write failed: " + task.key, e);
			}
		}
		for (WriteTask task : batch.titles.values()) {
			try {
				if (task.kind == Kind.TITLE_UPSERT) {
					daos.titleDao().addOrUpdateTitle(task.title);
				} else {
					daos.titleDao().deleteTitleByUrl(task.key);
				}
			} catch (Exception e) {
				log.error("title write failed: " + task.key, e);
			}
		}
	}

	private void rollbackQuietly(Connection conn) {
		try {
			conn.rollback();
		} catch (SQLException e) {
			log.error("rollback failed", e);
		}
	}
}
