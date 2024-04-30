package thread;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;

/**
 * 执行DNS解析的线程
 * @author bit4woo
 *
 */

public class IPProducer extends Thread {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private final BlockingQueue<String> IPQueue;
	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public IPProducer(BlockingQueue<String> domainQueue,BlockingQueue<String> IPQueue,int threadNo) {
		this.threadNo = threadNo;
		this.domainQueue = domainQueue;
		this.IPQueue = IPQueue;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (domainQueue.isEmpty() || stopflag) {
					//stdout.println("Producer break");
					break;
				}

				String host = domainQueue.take();

				Set<String> IPSet;
				Set<String> CDNSet;
				if (IPAddressUtils.isValidIPv4NoPort(host)) {
					IPSet = new HashSet<>();
					IPSet.add(host);
					CDNSet = new HashSet<>();
					CDNSet.add("");
				}else {
					HashMap<String,Set<String>> result = DomainUtils.dnsQuery(host,null);
					IPSet = result.get("IP");

					CDNSet = result.get("CDN");
				}

				IPQueue.addAll(IPSet);
			} catch (Throwable error) {//java.lang.RuntimeException can't been catched, why?
			}
		}
	}
}