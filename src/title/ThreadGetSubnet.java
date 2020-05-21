package title;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import burp.BurpExtender;
import burp.Commons;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;


//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadGetSubnet{
	private Set<String> domains;
	private List<IPProducer> plist;
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();
	
	public ThreadGetSubnet(Set<String> domains) {
		this.domains = domains;
	}

	public Set<String> Do(){
		stdout.println("~~~~~~~~~~~~~Start Get IPs~~~~~~~~~~~~~");
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> IPQueue = new LinkedBlockingQueue<String>();
		
		domainQueue.addAll(domains);

		plist = new ArrayList<IPProducer>();

		for (int i=0;i<=20;i++) {
			IPProducer p = new IPProducer(domainQueue,IPQueue,i);
			p.start();
			plist.add(p);
		}

		while(true) {//to wait all threads exit.
			if (domainQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Get IP Done~~~~~~~~~~~~~");
				break;
			}else {
				try {
					Thread.sleep(1*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
		}
		
		Set<String>IPset = new HashSet<>();
		IPset.addAll(IPQueue);
		return IPset;
	}

	boolean isAllProductorFinished(){
		for (IPProducer p:plist) {
			if(p.isAlive()) {
				return false;
			}
		}
		return true;
	}
	
	public void stopThreads() {
		for (IPProducer p:plist) {
			p.stopThread();
		}
		stdout.println("threads stopped!");
	}
}

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

class IPProducer extends Thread {//Producer do
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
				if (Commons.isValidIP(host)) {
					IPSet = new HashSet<>();
					IPSet.add(host);
					CDNSet = new HashSet<>();
					CDNSet.add("");
				}else {
					HashMap<String,Set<String>> result = Commons.dnsquery(host);
					IPSet = result.get("IP");
					
					CDNSet = result.get("CDN");
				}
				
				IPQueue.addAll(IPSet);
			} catch (Throwable error) {//java.lang.RuntimeException can't been catched, why?
			}
		}
	}
}
