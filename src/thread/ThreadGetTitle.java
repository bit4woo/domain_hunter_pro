package thread;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;

public class ThreadGetTitle extends Thread {
	private HashMap<String, String> domains;
	private final List<Producer> plist = new CopyOnWriteArrayList<>();

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();// 静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private int threadNumber;
	private GUIMain guiMain;

	public ThreadGetTitle(GUIMain guiMain, HashMap<String, String> domains, int threadNumber) {
		this.guiMain = guiMain;
		this.domains = domains;
		this.threadNumber = threadNumber;
	}

	@Override
	public void run() {
		stdout.println(String.format("~~~~~~~~~~~~~use %s threads~~~~~~~~~~~~~", threadNumber));
		stdout.println("~~~~~~~~~~~~~Start threading Get Title~~~~~~~~~~~~~ total task number: " + domains.size());
		BlockingQueue<Map.Entry<String, String>> domainQueue = new LinkedBlockingQueue<>();// use to store domains
		domainQueue.addAll(domains.entrySet());

		for (int i = 0; i < threadNumber; i++) {
			Producer p = new Producer(guiMain, domainQueue, i);
			// p.setDaemon(true);//将子线程设置为守护线程，会随着主线程的结束而立即结束。"主线程的结束"指的是”只有当JVM也退出时才可以！”
			plist.add(p);
			p.start();
		}

		// 必须等到所有producer真正退出（而不是固定只等1秒），否则任务还没跑完，协调线程就提前打印finished并死亡，
		// 导致 isAlive() 误判为“已结束”，从而无法正确 stop 旧批次、并可能重叠启动新批次。
		for (Producer p : plist) {
			try {
				p.join();// 阻塞直到该producer退出；被外部interrupt时会抛InterruptedException
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				stopAll();// 中断所有producer，确保它们也退出
				stdout.println(getName() + " stopped");
				return;
			}
		}

		stdout.println(getName() + " finished");
	}

	public void stopAll() {
		if (plist == null)
			return;

		// 第一步：给所有producer发送中断信号。
		// 注意：interrupt()只是“置位中断标志”，能否真正停下取决于线程代码是否响应：
		// - 若线程正阻塞在 poll()/join()/sleep() 等可中断调用上，会立即抛 InterruptedException 退出；
		// - 若线程正在执行不可中断的 makeHttpRequest，则要等它跑完当前任务、回到 poll() 时才退出。
		for (Producer p : plist) {
			p.interrupt();
		}

		// 第二步：有界等待producer真正退出（总超时30秒），避免UI永久卡死。
		long deadline = System.currentTimeMillis() + 30_000;
		for (Producer p : plist) {
			while (p.isAlive()) {
				long remain = deadline - System.currentTimeMillis();
				if (remain <= 0) {
					stdout.println("WARN " + p.getName() + " not stopped in 30s, skip waiting");
					break;
				}
				try {
					p.join(Math.min(remain, 1000));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		stdout.println("~~~~~~~~~~~~~all sub-threads exit!~~~~~~~~~~~~~");
	}
}
