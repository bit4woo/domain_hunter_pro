package domain;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
		
		BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();//use to store messageInfo
		BlockingQueue<String> subDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> similarDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> relatedDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> emailQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> packageNameQueue = new LinkedBlockingQueue<String>();
		
		inputQueue.addAll(messages);

		plist = new ArrayList<DomainProducer>();

		for (int i=0;i<=20;i++) {
			DomainProducer p = new DomainProducer(inputQueue,subDomainQueue,
					similarDomainQueue,relatedDomainQueue,emailQueue,packageNameQueue,i);
			p.start();
			plist.add(p);
		}

		while(true) {//to wait all threads exit.
			if (inputQueue.isEmpty() && isAllProductorFinished()) {
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
		
		HashSet<String> oldSubdomains = new HashSet<String>();
		oldSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		DomainPanel.getDomainResult().getSubDomainSet().addAll(subDomainQueue);
		DomainPanel.getDomainResult().getSimilarDomainSet().addAll(similarDomainQueue);
		DomainPanel.getDomainResult().getRelatedDomainSet().addAll(relatedDomainQueue);
		DomainPanel.getDomainResult().getEmailSet().addAll(emailQueue);
		DomainPanel.getDomainResult().getPackageNameSet().addAll(packageNameQueue);
		//stdout.println(emailQueue.size());
		//stdout.println(packageNameQueue.size());

		HashSet<String> newSubdomains = new HashSet<String>();
		newSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());
		
		newSubdomains.removeAll(oldSubdomains);
		DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().addAll(newSubdomains);
		stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~",newSubdomains.size()));
		stdout.println(String.join(System.lineSeparator(), newSubdomains));
		if (newSubdomains.size()>0){
			DomainPanel.autoSave();//进行一次主动保存
		}
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
