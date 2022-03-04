package Deprecated;

import burp.*;
import domain.DomainPanel;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//////////////////Thread Parser Target: dnsquery to get full target area that include domains and IPs/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough
@Deprecated
class ThreadPaserTarget{
	private Set<String> domains;
	private Set<String> fullTarget;
	private List<ParserProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadPaserTarget(Set<String> domains) {
		this.domains = domains;
	}

	public Set<String> Do(){
		stdout.println("~~~~~~~~~~~~~Start threading DNS Query~~~~~~~~~~~~~ total task number: "+domains.size());
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();//use to store domains
		BlockingQueue<String> IPQueue = new LinkedBlockingQueue<String>();
		domainQueue.addAll(domains);

		plist = new ArrayList<ParserProducer>();

		for (int i=0;i<=50;i++) {
			ParserProducer p = new ParserProducer(domainQueue,IPQueue,i);
			//Producer p = new Producer(callbacks,domainQueue,sharedQueue,i);
			p.start();
			plist.add(p);
		}

		long waitTime = 0; 
		while(true) {//to wait all threads exit.
			if (domainQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~DNS Query Done~~~~~~~~~~~~~");
				break;
			}else if(domainQueue.isEmpty() && waitTime >=10*60*1000){
				stdout.println("~~~~~~~~~~~~~DNS Query Done(force exits due to time out)~~~~~~~~~~~~~");
				break;
			}else {
				try {
					Thread.sleep(60*1000);//1分钟
					waitTime =waitTime+60*1000;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;//unnecessary
			}
		}
		Set<String> IPset = new HashSet<>();
		IPset.addAll(IPQueue);
		
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> subnets = Commons.toSmallerSubNets(IPset);//当前所有title结果计算出的IP网段
		subnets.addAll(DomainPanel.getDomainResult().getSubnetSet());//确定的IP网段，用户自己输入的
		this.fullTarget = Commons.toIPSet(subnets);// 当前所有title结果计算出的IP集合
		this.fullTarget.removeAll(IPset);
		this.fullTarget.addAll(domains);
		stdout.println(String.format("Total Target: %s (include %s domains and %s IP Addresses)", 
				fullTarget.size(),domains.size(),fullTarget.size()-domains.size()));
		
		return fullTarget;
	}

	public Set<String> getFullTarget() {
		return fullTarget;
	}

	boolean isAllProductorFinished(){
		int i = 0;
		for (ParserProducer p:plist) {
			if(p.isAlive()) {
				i = i+1;
			}
		}
		if (i>0){
			stdout.println( "~~~~~~~~~~~~~"+i +" productors are still alive~~~~~~~~~~~~~");
			return false;
		}else{
			stdout.println( "~~~~~~~~~~~~~All productor threads exited ~~~~~~~~~~~~~");
			return true;
		}
	}

	public void stopThreads() {
		for (ParserProducer p:plist) {
			p.stopThread();
		}
		stdout.println("~~~~~~~~~~~~~All stop message sent! wait them to exit~~~~~~~~~~~~~");
	}
}

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

@Deprecated
class ParserProducer extends Thread {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private final BlockingQueue<String> IPQueue;//use to store domains
	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ParserProducer(BlockingQueue<String> domainQueue,BlockingQueue<String> IPQueue,int threadNo) {
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
					//stdout.println(threadNo+" Producer exited");
					break;
				}
				String host = domainQueue.take();
				int leftTaskNum = domainQueue.size();

				//第一步：IP解析
				Set<String> IPSet = new HashSet<>();
				Set<String> CDNSet = new HashSet<>();
				if (Commons.isValidIP(host)) {
					IPSet.add(host);
					CDNSet.add("");
				}else {
					HashMap<String,Set<String>> result = Commons.dnsquery(host);
					IPSet = result.get("IP");
					CDNSet = result.get("CDN");
				}

				if (IPSet.size() <= 0) {
					continue;
				}else {//默认过滤私有IP
					String ip = new ArrayList<>(IPSet).get(0);
					if (IPAddressUtils.isPrivateIPv4(ip)) {
						continue;
					}
				}
				IPQueue.addAll(IPSet);
			} catch (Exception error) {
				error.printStackTrace(stderr);
			}
		}
	}
}
