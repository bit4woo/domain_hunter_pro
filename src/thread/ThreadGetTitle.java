package thread;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;

public class ThreadGetTitle extends Thread{
	private HashMap<String,String> domains;
	private final List<Producer> plist = Collections.synchronizedList(new ArrayList<>());


	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private int threadNumber;
	private GUIMain guiMain;

	public ThreadGetTitle(GUIMain guiMain,HashMap<String,String> domains,int threadNumber) {
		this.guiMain = guiMain;
		this.domains = domains;
		this.threadNumber = threadNumber;
	}

	@Override
	public void run(){
		stdout.println(String.format("~~~~~~~~~~~~~use %s threads~~~~~~~~~~~~~",threadNumber));
		stdout.println("~~~~~~~~~~~~~Start threading Get Title~~~~~~~~~~~~~ total task number: "+domains.size());
		BlockingQueue<Map.Entry<String,String>> domainQueue = new LinkedBlockingQueue<>();//use to store domains
		domainQueue.addAll(domains.entrySet());

		for (int i=0;i<threadNumber;i++) {
			Producer p = new Producer(guiMain,domainQueue,i);
			//p.setDaemon(true);//将子线程设置为守护线程，会随着主线程的结束而立即结束。"主线程的结束"指的是”只有当JVM也退出时才可以！”
			plist.add(p);
	        p.start();
		}
		
	    for (Producer p : plist) {
	        try {
	            p.join(1000);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	            break;
	        }
	    }
	    
		stdout.println(getName()+" finished");
	}
	
	public void stopAll() {
	    if (plist == null) return;

	    for (Producer p : plist) {
	        p.interrupt();//必须配合Thread.currentThread().isInterrupted()逻辑，否则不起作用
	    }

	    for (Producer p : plist) {
	        try {
	            p.join(1000);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }
	    }
	    stdout.println("~~~~~~~~~~~~~all sub-threads exit!~~~~~~~~~~~~~");
	}
}
