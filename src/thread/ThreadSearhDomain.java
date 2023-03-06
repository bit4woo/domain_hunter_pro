package thread;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import domain.DomainProducer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import GUI.GUIMain;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadSearhDomain extends Thread{
	private List<IHttpRequestResponse> messages;
	private List<DomainProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout;
	public PrintWriter stderr;
	public IExtensionHelpers helpers = callbacks.getHelpers();
	GUIMain guiMain;
	private boolean searchThirdPart;

	public ThreadSearhDomain(GUIMain guiMain,List<IHttpRequestResponse> messages,boolean searchThirdPart) {
		this.guiMain = guiMain;
		this.messages = messages;
		stdout = BurpExtender.getStdout();
		stderr = BurpExtender.getStderr();
		this.setName(this.toString());
		this.searchThirdPart = searchThirdPart;
	}
	
	@Override
	public void run(){
		stdout.println("~~~~~~~~~~~~~Start Search Domain~~~~~~~~~~~~~");

		guiMain.getInputQueue().addAll(messages);

		plist = new ArrayList<DomainProducer>();

		for (int i=0;i<=20;i++) {
			DomainProducer p = new DomainProducer(guiMain,guiMain.getInputQueue(),i,searchThirdPart);
			p.setDaemon(true);
			p.start();
			plist.add(p);
		}
		
		try {
			for (DomainProducer p:plist) {
				p.join();
			}
			//让主线程等待各个子线程执行完成，才会结束。
			//https://www.cnblogs.com/zheaven/p/12054044.html
		} catch (InterruptedException e) {
			stdout.println("force stop received");
			e.printStackTrace();
		}
		stdout.println("~~~~~~~~~~~~~Search Domain Done~~~~~~~~~~~~~");
	}

	@Deprecated
	public void stopThreads() {
		for (DomainProducer p:plist) {
			p.stopThread();
		}
		stdout.println("threads stopped!");
	}
	
	public void forceStopThreads() {
		this.interrupt();//将子线程都设置为守护线程，会随着主线程的结束而立即结束,与setDaemon(true)结合
		stdout.println("~~~~~~~~~~~~~force stop main thread,all sub-threads will exit!~~~~~~~~~~~~~");
	}

	//	public static void main(String args[]) {//test
	//		System.out.println(DomainProducer.grepDomain("http://www.jd.com/usr/www.baidu.com/xss.jd.com"));
	//	}
}
