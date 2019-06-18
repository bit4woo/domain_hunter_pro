package burp;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JOptionPane;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
class ThreadRunner{
	private byte[] request;
	private List<RunnerProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadRunner(byte[] request) {
		this.request = request;
	}

	public void Do(){
		BlockingQueue<LineEntry> lineEntryQueue = new LinkedBlockingQueue<LineEntry>();//use to store domains
		lineEntryQueue.addAll(TitlePanel.getTitleTableModel().getLineEntries());
		stdout.println("~~~~~~~~~~~~~Start threading Runner~~~~~~~~~~~~~ total task number: "+lineEntryQueue.size());

		plist = new ArrayList<RunnerProducer>();

		for (int i=0;i<=50;i++) {
			RunnerProducer p = new RunnerProducer(RunnerGUI.getRunnerTableModel(),lineEntryQueue,request, RunnerGUI.getKeyword(), i);
			p.start();
			plist.add(p);
		}

		long waitTime = 0; 
		while(true) {//to wait all threads exit.
			if (lineEntryQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
				break;
			}else if(lineEntryQueue.isEmpty() && waitTime >=10*60*1000){
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
		for (RunnerProducer p:plist) {
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
		for (RunnerProducer p:plist) {
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

class RunnerProducer extends Thread {//Producer do
	private final BlockingQueue<LineEntry> lineEntryQueue;//use to store domains
	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private byte[] request;
	LineTableModel runnerTableModel;
	String keyword;

	public RunnerProducer(LineTableModel runnerTableModel,BlockingQueue<LineEntry> lineEntryQueue,byte[] request, String keyword, int threadNo) {
		this.runnerTableModel = runnerTableModel;
		this.threadNo = threadNo;
		this.lineEntryQueue = lineEntryQueue;
		this.request = request;
		this.keyword = keyword;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (lineEntryQueue.isEmpty() || stopflag) {
					//stdout.println(threadNo+" Producer exited");
					break;
				}
				LineEntry line = lineEntryQueue.take();
				String protocol = line.getProtocol();
				boolean useHttps =false;
				if (protocol.equalsIgnoreCase("https")) {
					useHttps =true;
				}
				IHttpService httpService = helpers.buildHttpService(line.getHost(), line.getPort(), useHttps);

				int leftTaskNum = lineEntryQueue.size();

				IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(httpService, this.request);
				Getter getter = new Getter(helpers);
				String body = new String(getter.getBody(false, messageinfo));
				stdout.println("Runner Checking: "+line.getUrl());

				if (body != null && body.toLowerCase().contains(keyword)) {
					runnerTableModel.addNewLineEntry(new LineEntry(messageinfo,false,false,"Runner"));
				}
				stdout.println(String.format("%s tasks left",leftTaskNum));
			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}

	}
}
