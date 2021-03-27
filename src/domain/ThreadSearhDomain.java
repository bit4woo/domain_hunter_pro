package domain;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadSearhDomain{
	private List<IHttpRequestResponse> messages;
	private List<DomainProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout;
	public PrintWriter stderr;
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadSearhDomain(List<IHttpRequestResponse> messages) {
		this.messages = messages;
		stdout = BurpExtender.getStdout();
		stderr = BurpExtender.getStderr();
	}

	public void Do(){
		stdout.println("~~~~~~~~~~~~~Start Search Domain~~~~~~~~~~~~~");

		BurpExtender.inputQueue.addAll(messages);

		plist = new ArrayList<DomainProducer>();

		for (int i=0;i<=20;i++) {
			DomainProducer p = new DomainProducer(BurpExtender.inputQueue,BurpExtender.subDomainQueue,
					BurpExtender.similarDomainQueue,BurpExtender.relatedDomainQueue,
					BurpExtender.emailQueue,BurpExtender.packageNameQueue,i);
			p.start();
			plist.add(p);
		}

		while(true) {//to wait all threads exit.
			if (BurpExtender.inputQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Search Domain Done~~~~~~~~~~~~~");
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

		BurpExtender.QueueToResult();
		return;
	}

	boolean isAllProductorFinished(){
		for (DomainProducer p:plist) {
			if(p.isAlive()) {
				return false;
			}
		}
		return true;
	}

	public void stopThreads() {
		for (DomainProducer p:plist) {
			p.stopThread();
		}
		stdout.println("threads stopped!");
	}

	//	public static void main(String args[]) {//test
	//		System.out.println(DomainProducer.grepDomain("http://www.jd.com/usr/www.baidu.com/xss.jd.com"));
	//	}
}
