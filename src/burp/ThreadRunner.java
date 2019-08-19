package burp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
class ThreadRunner{
	private IHttpRequestResponse messageInfo;
	private List<RunnerProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadRunner(IHttpRequestResponse messageInfo) {
		this.messageInfo = messageInfo;
	}

	public void Do(){
		BlockingQueue<LineEntry> lineEntryQueue = new LinkedBlockingQueue<LineEntry>();//use to store domains
		lineEntryQueue.addAll(TitlePanel.getTitleTableModel().getLineEntries());
		stdout.println("~~~~~~~~~~~~~Start threading Runner~~~~~~~~~~~~~ total task number: "+lineEntryQueue.size());

		plist = new ArrayList<RunnerProducer>();

		for (int i=0;i<=50;i++) {
			RunnerProducer p = new RunnerProducer(RunnerGUI.getRunnerTableModel(),lineEntryQueue,messageInfo, RunnerGUI.getKeyword(), i);
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
	private IHttpRequestResponse messageInfo;
	LineTableModel runnerTableModel;
	String keyword;

	public RunnerProducer(LineTableModel runnerTableModel,BlockingQueue<LineEntry> lineEntryQueue,IHttpRequestResponse messageInfo, String keyword, int threadNo) {
		this.runnerTableModel = runnerTableModel;
		this.runnerTableModel.setListenerIsOn(false);//否则数据会写入title的数据库
		this.threadNo = threadNo;
		this.lineEntryQueue = lineEntryQueue;
		this.messageInfo = messageInfo;
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
				//只需要从line中获取host信息就可以了，其他信息都应该和当前的请求一致！
				LineEntry line = lineEntryQueue.take();
				
				String host = line.getHost();
				String protocol = messageInfo.getHttpService().getProtocol();
				int port = messageInfo.getHttpService().getPort();
				
				IHttpService httpService = helpers.buildHttpService(host, port, protocol);

				Getter getter = new Getter(helpers);
				LinkedHashMap<String, String> headers = getter.getHeaderMap(true, messageInfo);
				headers.put("Host", host+":"+port);//update host of request header
				byte[] body = getter.getBody(true, messageInfo);
				
				byte[] neRequest = helpers.buildHttpMessage(getter.headerMapToHeaderList(headers), body);

				int leftTaskNum = lineEntryQueue.size();
				stdout.println(String.format("%s tasks left, Runner Checking: %s",leftTaskNum,line.getUrl()));
				
				IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(httpService, neRequest);
				if (messageinfo !=null) {
					byte[] bodybyte = getter.getBody(false, messageinfo);
					if (bodybyte != null) {
						String responseBody = new String(bodybyte);
						if (responseBody.toLowerCase().contains(keyword)) {
							runnerTableModel.addNewLineEntry(new LineEntry(messageinfo,false,false,"Runner"));
						}
					}
				}
			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}

	}
}
