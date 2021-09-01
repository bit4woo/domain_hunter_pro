package GUI;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import title.LineEntry;
import title.LineTableModel;

public class RunnerProducer extends Thread {//Producer do
	private final BlockingQueue<LineEntry> lineEntryQueue;//use to store domains
	private final BlockingQueue<String> domainQueue;
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
	int changeType;
	boolean ChangeRaw;

	public RunnerProducer(LineTableModel runnerTableModel,BlockingQueue<LineEntry> lineEntryQueue,BlockingQueue<String> domainQueue,
			IHttpRequestResponse messageInfo,int changeType, boolean ChangeRaw,int threadNo) {
		this.runnerTableModel = runnerTableModel;
		this.runnerTableModel.setListenerIsOn(false);//否则数据会写入title的数据库
		this.threadNo = threadNo;
		this.lineEntryQueue = lineEntryQueue;
		this.domainQueue = domainQueue;
		this.changeType = changeType;
		stopflag= false;
		this.ChangeRaw = ChangeRaw;

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
				
				
				///构造Service
				String newHost;
				IHttpService newHttpService;
				if (changeType == ThreadRunner.ChangeHostInHeader) {
					newHost = domainQueue.take();
					newHttpService = httpService;
				}else if (changeType == ThreadRunner.ChangeHostInService) {//适用于查找相同主机，比如 IP 、域名指向相同主机的情况。
					LineEntry line = lineEntryQueue.take();
					newHost = line.getHost();
					newHttpService = helpers.buildHttpService(newHost, httpService.getPort(), httpService.getProtocol());
				}else if (changeType == ThreadRunner.ChangeService){//适用于验证token、cookie在其他站点的有效性
					LineEntry line = lineEntryQueue.take();
					newHost = line.getHost();
					int newPort = line.getPort();
					String newProtocol = line.getProtocol();
					newHttpService = helpers.buildHttpService(newHost,newPort,newProtocol);
					if (line.getEntryType().equals(LineEntry.EntryType_DNS)) {
						//使用已有httpService,DNS记录即跳过；使用host时，可能有特殊端口，DNS记录就有价值
						continue;
					}
				}else {
					BurpExtender.getStderr().println("wrong change type");
					return;
				}

				///构造数据包
				byte[] newRequest;
				if (ChangeRaw){
					if (changeType == ThreadRunner.ChangeHostInHeader) {//这里用全部域名更好，外网不能解析的域名，很可能在这个场景中可以解析
						newRequest = getter.addOrUpdateHeader(true, request, "Host", newHost);
					}else {
						String headerHost = newHttpService.toString().replaceFirst("http://", "").replaceFirst("https://", "");
						newRequest = getter.addOrUpdateHeader(true, request, "Host", headerHost);
					}

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
				}else {
					newRequest = request;
				}
				

				//stdout.println(httpService.toString());
				//stdout.println(new String(neRequest));
				IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(newHttpService, newRequest);
				
				if (changeType == ThreadRunner.ChangeHostInHeader) {
					int leftTaskNumDomain = domainQueue.size();
					stdout.println(String.format("%s tasks left, Runner Checking: %s",leftTaskNumDomain,newHost));
				}else {
					int leftTaskNum = lineEntryQueue.size();
					String fullurl = helpers.analyzeRequest(messageinfo).getUrl().toString();
					stdout.println(String.format("%s tasks left, Runner Checking: %s",leftTaskNum,fullurl));
				}

				if (messageinfo !=null) {
					if (changeType == ThreadRunner.ChangeHostInHeader) {
						runnerTableModel.addNewLineEntryWithTime(new LineEntry(messageinfo,LineEntry.CheckStatus_UnChecked,"Runner"));
					}else {
						runnerTableModel.addNewLineEntry(new LineEntry(messageinfo,LineEntry.CheckStatus_UnChecked,"Runner"));
					}
				}
				
			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}
}