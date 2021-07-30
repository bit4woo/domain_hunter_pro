package GUI;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JOptionPane;

import burp.BurpExtender;
import burp.Getter;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import title.LineEntry;
import title.LineTableModel;
import title.TitlePanel;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadRunner{
	private IHttpRequestResponse messageInfo;
	private List<RunnerProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private RunnerGUI runnerGUI;

	public ThreadRunner(RunnerGUI runnerGUI, IHttpRequestResponse messageInfo) {
		this.runnerGUI = runnerGUI;
		this.messageInfo = messageInfo;
	}

	@Deprecated
	public String getKeywordFromUI() {
		String responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
		while(responseKeyword.trim().equals("")){
			responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
		}
		responseKeyword = responseKeyword.trim();
		return responseKeyword;
	}
	
	/**
	 * 只修改host，那么protocol、port都不变，适合查找相同服务的IP、Name
	 * 修改httpService，那么protocol、host、port修改，适合验证cookie\token对于站点的有效性!
	 * @return
	 */
	private boolean justChangeHost() {
		int user_input = JOptionPane.showConfirmDialog(null, "Just change httpService[No] or Host[Yes]?","Change Option",JOptionPane.YES_NO_OPTION);
		if (JOptionPane.YES_OPTION == user_input) {
			return true;
		}else {
			return false;
		}
	}

	public void Do(){
		BlockingQueue<LineEntry> lineEntryQueue = new LinkedBlockingQueue<LineEntry>();//use to store domains
		lineEntryQueue.addAll(TitlePanel.getTitleTableModel().getLineEntries().values());
		stdout.println("~~~~~~~~~~~~~Start threading Runner~~~~~~~~~~~~~ total task number: "+lineEntryQueue.size());

		boolean justChangeHost = justChangeHost();
		plist = new ArrayList<RunnerProducer>();

		for (int i=0;i<=50;i++) {
			RunnerProducer p = new RunnerProducer(runnerGUI.getRunnerTableModel(),lineEntryQueue,messageInfo,justChangeHost, i);
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
		runnerGUI.lblStatus.setText("finished");
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
		if (plist != null) {
			for (RunnerProducer p:plist) {
				p.stopThread();
			}
			stdout.println("~~~~~~~~~~~~~All stop message sent! wait them to exit~~~~~~~~~~~~~");
		}
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

	private HelperPlus getter;
	private byte[] request;
	private byte[] response;
	private IHttpService httpService;

	LineTableModel runnerTableModel;
	boolean justChangeHost;

	public RunnerProducer(LineTableModel runnerTableModel,BlockingQueue<LineEntry> lineEntryQueue,IHttpRequestResponse messageInfo,boolean justChangeHost, int threadNo) {
		this.runnerTableModel = runnerTableModel;
		this.runnerTableModel.setListenerIsOn(false);//否则数据会写入title的数据库
		this.threadNo = threadNo;
		this.lineEntryQueue = lineEntryQueue;
		stopflag= false;

		//为了避免原始messageinfo的改变导致影响后续获取headers等参数，先完成解析存储下来。
		//而且也可以避免多线程下getter时常getHeaderMap的结果为空的情况！！！
		//虽然减少了getter的次数，但是还是每个线程执行了一次，目前看来没有出错，因为线程的启动是顺序执行的！
		getter = new HelperPlus(helpers);
		request = messageInfo.getRequest();
		response = messageInfo.getResponse();
		httpService = messageInfo.getHttpService();
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
				//只需要从line中获取host信息就可以了，其他信息都应该和当前的请求一致！
				LineEntry line = lineEntryQueue.take();
				String newHost = line.getHost();
				IHttpService newHttpService;
				byte[] newRequest;
				if (justChangeHost) {//适用于查找相同主机，比如 IP 、域名指向相同主机的情况。
					newHttpService = helpers.buildHttpService(newHost, httpService.getPort(), httpService.getProtocol());
				}else {//适用于验证token、cookie在其他站点的有效性
					newHttpService = helpers.buildHttpService(line.getHost(),line.getPort(),line.getProtocol());
				}

				String headerHost = newHttpService.toString().replaceFirst("http://", "").replaceFirst("https://", "");
				newRequest = getter.addOrUpdateHeader(true, request, "Host", headerHost);

				String headerVaule = getter.getHeaderValueOf(true, request,"Origin");
				if (null != headerVaule) {
					//headerVaule = headerVaule.replaceFirst(httpService.toString(), newHttpService.toString());
					//newRequest = getter.addOrUpdateHeader(true, newRequest, "Origin", headerVaule);
					newRequest = getter.addOrUpdateHeader(true, newRequest, "Origin", newHttpService.toString());
				}

				String headerVaule1 = getter.getHeaderValueOf(true, request,"Referer");
				if (null != headerVaule1) {
					//headerVaule1 = headerVaule1.replaceFirst(httpService.toString(), newHttpService.toString());
					//newRequest = getter.addOrUpdateHeader(true, newRequest, "Referer", headerVaule1);
					newRequest = getter.addOrUpdateHeader(true, newRequest, "Referer", newHttpService.toString()+"/");
				}

				int leftTaskNum = lineEntryQueue.size();

				//stdout.println(httpService.toString());
				//stdout.println(new String(neRequest));
				IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(newHttpService, newRequest);
				String fullurl = helpers.analyzeRequest(messageinfo).getUrl().toString();
				stdout.println(String.format("%s tasks left, Runner Checking: %s",leftTaskNum,fullurl));

				if (messageinfo !=null) {
					runnerTableModel.addNewLineEntry(new LineEntry(messageinfo,LineEntry.CheckStatus_UnChecked,"Runner"));
				}
			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}
}
