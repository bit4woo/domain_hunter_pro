package GUI;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Tools.ToolPanel;

import burp.*;
import title.LineEntry;
import title.LineTableModel;

//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadDirBruter{
	private IHttpRequestResponse messageInfo;
	private List<DirBruterProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private RunnerGUI runnerGUI;
	BlockingQueue<String> pathDict = new LinkedBlockingQueue<String>();//创建对象时就加载字典，避免字典因加载慢而为空

	public ThreadDirBruter(RunnerGUI runnerGUI,IHttpRequestResponse messageInfo) {
		this.runnerGUI = runnerGUI;
		this.messageInfo = messageInfo;
		File dictFile = new File(ToolPanel.textFieldDirBruteDict.getText().trim());
		if (dictFile.exists()){
			DictReader reader = new DictReader(dictFile.toString(),pathDict);
			reader.start();
			reader.setDaemon(true);//TODO 字典还未加载完成，就执行爆破，线程会退出。
		}else {
			stdout.println("dict file not found!");
			return;
		}
	}

	public void Do(){
		BlockingQueue<String> pathDict = new LinkedBlockingQueue<String>();
		File dictFile = new File(ToolPanel.textFieldDirBruteDict.getText().trim());
		if (dictFile.exists()){
			DictReader reader = new DictReader(dictFile.toString(),pathDict);
			reader.start();
		}else {
			stdout.println("dict file not found!");
			return;
		}

		stdout.println("~~~~~~~~~~~~~Start threading Runner~~~~~~~~~~~~~ total task number: "+pathDict.size());

		plist = new ArrayList<DirBruterProducer>();

		for (int i=0;i<=50;i++) {
			DirBruterProducer p = new DirBruterProducer(runnerGUI.getRunnerTableModel(),pathDict,messageInfo, i);
			p.start();
			plist.add(p);
		}

		long waitTime = 0;
		while(true) {//to wait all threads exit.
			if (pathDict.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
				break;
			}else if(pathDict.isEmpty() && waitTime >=10*60*1000){
				stdout.println("~~~~~~~~~~~~~Get Title Done(force exits due to time out)~~~~~~~~~~~~~");
				break;
			}else {
				try {
					Thread.sleep(60*1000);//1分钟
					waitTime =waitTime+60*1000;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//continue;//unnecessary
			}
		}
		runnerGUI.lblStatus.setText("finished");
		return;
	}

	boolean isAllProductorFinished(){
		int i = 0;
		for (DirBruterProducer p:plist) {
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
		if (plist != null) {
			for (DirBruterProducer p:plist) {
				p.stopThread();
			}
			stdout.println("~~~~~~~~~~~~~All stop message sent! wait them to exit~~~~~~~~~~~~~");
		}
	}
}

class DirBruterProducer extends Thread {//Producer do
	private final BlockingQueue<String> pathDict;//use to store domains
	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	private String shorturl;
	private IHttpService service;

	LineTableModel runnerTableModel;

	public DirBruterProducer(LineTableModel runnerTableModel,BlockingQueue<String> pathDict,IHttpRequestResponse messageInfo, int threadNo) {
		this.runnerTableModel = runnerTableModel;
		this.runnerTableModel.setListenerIsOn(false);//否则数据会写入title的数据库
		this.threadNo = threadNo;
		this.pathDict = pathDict;
		stopflag= false;

		//为了避免原始messageinfo的改变导致影响后续获取headers等参数，先完成解析存储下来。
		//而且也可以避免多线程下getter时常getHeaderMap的结果为空的情况！！！
		//虽然减少了getter的次数，但是还是每个线程执行了一次，目前看来没有出错，因为线程的启动是顺序执行的！
		service = messageInfo.getHttpService();
		shorturl = service.toString();
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (pathDict.isEmpty() || stopflag) {
					stdout.println(threadNo+" DirBruterProducer exited");
					break;
				}
				//只需要从line中获取host信息就可以了，其他信息都应该和当前的请求一致！
				String path = pathDict.take();
				if (!path.startsWith("/")){
					path = "/"+path;
				}
				
				URL url = new URL(shorturl+path);
				byte[] request = helpers.buildHttpRequest(url);
				String req = new String(request);
				IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(service, request);
				int leftTaskNum = pathDict.size();
				stdout.println(String.format("%s tasks left, Runner Checking: %s",leftTaskNum,url.toString()));
				Getter getter = new Getter(helpers);
				if (messageinfo !=null) {
					byte[] response = messageinfo.getResponse();
					int status = getter.getStatusCode(messageinfo);
					if (response != null && status !=404) {
						runnerTableModel.addNewLineEntry(new LineEntry(messageinfo,LineEntry.CheckStatus_UnChecked,"dirBruter"));
					}
				}
			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}
}
