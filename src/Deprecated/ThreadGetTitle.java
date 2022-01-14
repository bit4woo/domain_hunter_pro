package Deprecated;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import thread.Producer;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadGetTitle{
	private Set<String> domains;
	private List<Producer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadGetTitle(Set<String> domains) {
		this.domains = domains;
	}

	public void Do(){
		stdout.println("~~~~~~~~~~~~~Start threading Get Title~~~~~~~~~~~~~ total task number: "+domains.size());
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();//use to store domains
		domainQueue.addAll(domains);

		plist = new ArrayList<Producer>();

		for (int i=0;i<=50;i++) {
			Producer p = new Producer(domainQueue,i);
			//Producer p = new Producer(callbacks,domainQueue,sharedQueue,i);
			p.start();
			plist.add(p);
		}

		long waitTime = 0; 
		while(true) {//to wait all threads exit.
			if (domainQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
				break;
			}else if(domainQueue.isEmpty() && waitTime >=10*60*1000){
				stdout.println("~~~~~~~~~~~~~Get Title Done(force exits due to time out)~~~~~~~~~~~~~");
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
		return;
	}

	boolean isAllProductorFinished(){
		int i = 0;
		for (Producer p:plist) {
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
		for (Producer p:plist) {
			p.stopThread();
		}
		stdout.println("~~~~~~~~~~~~~All stop message sent! wait them to exit~~~~~~~~~~~~~");
	}
}
