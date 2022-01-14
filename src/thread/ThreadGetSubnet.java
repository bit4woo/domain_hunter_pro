package thread;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;


//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadGetSubnet extends Thread{
	private Set<String> domains;
	private List<IPProducer> plist;
	

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadGetSubnet(Set<String> domains) {
		this.domains = domains;
	}
	public Set<String> IPset = new HashSet<>();

	@Override
	public void run(){
		stdout.println("~~~~~~~~~~~~~Start Get IPs~~~~~~~~~~~~~");
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> IPQueue = new LinkedBlockingQueue<String>();

		domainQueue.addAll(domains);

		plist = new ArrayList<IPProducer>();

		for (int i=0;i<=20;i++) {
			IPProducer p = new IPProducer(domainQueue,IPQueue,i);
			p.setDaemon(true);
			p.start();
			plist.add(p);
		}
		
		try {
			for (IPProducer p:plist) {
				p.join();
			}
			//让主线程等待各个子线程执行完成，才会结束。
			//https://www.cnblogs.com/zheaven/p/12054044.html
		} catch (InterruptedException e) {
			stdout.println("force stop received");
			e.printStackTrace();
		}
		
		stdout.println("~~~~~~~~~~~~~Get IP Done~~~~~~~~~~~~~");
		IPset.addAll(IPQueue);
	}

	@Deprecated
	public void stopThreads() {
		for (IPProducer p:plist) {
			p.stopThread();
		}
		stdout.println("threads stopped!");
	}
	
	public void forceStopThreads() {
		this.interrupt();//将子线程都设置为守护线程，会随着主线程的结束而立即结束
		stdout.println("~~~~~~~~~~~~~force stop main thread,all sub-threads will exit!~~~~~~~~~~~~~");
	}
}