package thread;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;

//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadGetSubnet extends Thread {
	private Set<String> domains;
	private final List<IPProducer> plist = Collections.synchronizedList(new ArrayList<>());

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();// 静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadGetSubnet(Set<String> domains) {
		this.domains = domains;
	}

	public Set<String> IPset = new HashSet<>();

	@Override
	public void run() {
		stdout.println("~~~~~~~~~~~~~Start Get IPs~~~~~~~~~~~~~");
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> IPQueue = new LinkedBlockingQueue<String>();

		domainQueue.addAll(domains);

		for (int i = 0; i <= 20; i++) {
			IPProducer p = new IPProducer(domainQueue, IPQueue, i);
			p.start();
			plist.add(p);
		}

		for (IPProducer p : plist) {
			try {
				p.join(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		stdout.println("~~~~~~~~~~~~~Get IP Done~~~~~~~~~~~~~");
		IPset.addAll(IPQueue);
	}

	public void stopAll() {
		if (plist == null)
			return;

		for (IPProducer p : plist) {
			p.interrupt();// 必须配合Thread.currentThread().isInterrupted()逻辑，否则不起作用
		}

		for (IPProducer p : plist) {
			try {
				p.join(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		stdout.println("~~~~~~~~~~~~~all sub-threads exit!~~~~~~~~~~~~~");
	}
}