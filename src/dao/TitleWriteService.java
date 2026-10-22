package dao;

import title.LineEntry;

/**
 * Title 表写操作的入口（现在只是 {@link DbWriter} 的一层薄封装）。
 * <p>
 * <b>为什么不删掉这个类</b>：它的调用点（LineTableModel、TitlePanel）已经按
 * "所有写操作走统一入口"的方式写好了，保留同名方法可以让这些调用点零改动。
 * </p>
 * <p>
 * <b>为什么不再自己维护线程池</b>：原来这个类持有一个独立的单线程 ExecutorService。
 * 如果 Target/Domain 的写队列再开一个线程，就又变成"两个写线程并发写同一个 SQLite 文件"，
 * SQLITE_BUSY 会重新出现——串行化必须是<b>全库一个写线程</b>，而不是"每张表一个"。
 * 所以这里把线程移交给了 {@link DbWriter}，本类只做参数转换。
 * </p>
 */
public class TitleWriteService {

	private static final TitleWriteService INSTANCE = new TitleWriteService();

	private TitleWriteService() {
	}

	public static TitleWriteService getInstance() {
		return INSTANCE;
	}

	/**
	 * 异步 upsert 一条记录（新增，或按 url 覆盖）。
	 */
	public void upsert(TitleDao dao, LineEntry entry) {
		if (dao == null || entry == null) {
			return;
		}
		DbWriter.getInstance().upsertTitle(dao.getDbFilePath(), entry);
	}

	/**
	 * 异步按 url 删除一条记录。
	 */
	public void deleteByUrl(TitleDao dao, String url) {
		if (dao == null || url == null) {
			return;
		}
		DbWriter.getInstance().deleteTitle(dao.getDbFilePath(), url);
	}

	/**
	 * 冲刷：等待所有已提交的写任务执行完。
	 * <p>
	 * 语义与原来一致（FIFO 栅栏），但现在栅栏排在<b>同一个</b>队列里，
	 * 因此它同时保证了 target/domain 的写也已经落盘。
	 * </p>
	 *
	 * @param timeoutMillis 最长等待时间
	 * @return 是否在超时前全部写完
	 */
	public boolean flush(long timeoutMillis) {
		return DbWriter.getInstance().flush(timeoutMillis);
	}
}
