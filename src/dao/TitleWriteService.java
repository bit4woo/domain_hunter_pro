package dao;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import title.LineEntry;

/**
 * Title表所有写操作的统一串行化入口（单例）。
 *
 * <p>要解决的三个问题：</p>
 * <ol>
 *   <li>旧实现里每写一条记录就 {@code new Thread(...).start()}，跑几千个域名就会创建几千个线程，造成线程爆炸；</li>
 *   <li>多线程并发写同一个SQLite文件会触发 {@code SQLITE_BUSY / "database is locked"}；</li>
 *   <li>写库线程与EDT并发操作同一个LineEntry对象，可能读到改到一半的数据。</li>
 * </ol>
 *
 * <p>方案：把 upsert / delete 全部丢进一个单线程 {@link ExecutorService} 排队执行，天然保证串行与顺序。</p>
 *
 * <p>为什么不用 {@code synchronized} 方法：{@code synchronized} 只能保证单个方法原子，多个方法之间
 * （例如先 upsert 后 delete）的顺序依然无法保证；而单线程队列天然保证 FIFO 顺序。</p>
 */
public class TitleWriteService {

    private static final TitleWriteService INSTANCE = new TitleWriteService();

    /**
     * 唯一的写线程。所有写任务都在这一条线程上按提交顺序执行。
     * 守护线程：不阻塞 Burp/JVM 退出；缺点是退出瞬间仍在排队的写会被丢弃，故关键节点要调用 {@link #flush(long)}。
     */
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "title-db-writer");
        t.setDaemon(true);
        return t;
    });

    private TitleWriteService() {
    }

    public static TitleWriteService getInstance() {
        return INSTANCE;
    }

    /**
     * 异步 upsert 一条记录（新增，或按 url 覆盖）。
     * 调用方把 LineEntry 的引用交过来即可；写入动作在后台串行执行，不会阻塞调用线程（通常是EDT）。
     */
    public void upsert(TitleDao dao, LineEntry entry) {
        if (dao == null || entry == null) {
            return;
        }
        writer.submit(() -> {
            try {
                dao.addOrUpdateTitle(entry);
            } catch (Exception e) {
                // 单个写失败不能影响后续写任务，也不允许抛到后台线程导致线程死亡
                e.printStackTrace();
            }
        });
    }

    /**
     * 异步按 url 删除一条记录。
     */
    public void deleteByUrl(TitleDao dao, String url) {
        if (dao == null || url == null) {
            return;
        }
        writer.submit(() -> {
            try {
                dao.deleteTitleByUrl(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 冲刷：等待所有“已提交”的写任务全部执行完（FIFO 语义下，也意味着它们之间顺序已定）。
     *
     * <p>实现原理：向队列尾部塞一个“栅栏”任务（countDown），因为单线程按提交顺序执行，
     * 当栅栏被执行到时，说明它之前的所有写都已完成；调用方 {@code await} 这个栅栏即可。</p>
     *
     * <p>本方法不会 shutdown 执行器，可以反复调用。</p>
     *
     * @param timeoutMillis 最长等待时间
     * @return 是否在超时前全部写完
     */
    public boolean flush(long timeoutMillis) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            writer.submit(latch::countDown);
        } catch (Exception e) {
            // 执行器已被关闭等极端情况
            return false;
        }
        try {
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
