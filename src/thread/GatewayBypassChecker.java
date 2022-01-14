package thread;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import title.LineEntry;
import title.LineTableModel;

/**
 * Host碰撞检测线程
 * @author bit4woo
 *
 */
public class GatewayBypassChecker extends Thread {//Producer do
	private final BlockingQueue<String> inputQueue;
	private int threadNo;
	private volatile boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	public HelperPlus getter = new HelperPlus(helpers);

	LineTableModel runnerTableModel;

	public GatewayBypassChecker(LineTableModel runnerTableModel,BlockingQueue<String> inputQueue,
			int threadNo) {
		this.runnerTableModel = runnerTableModel;
		this.runnerTableModel.setListenerIsOn(false);//否则数据会写入title的数据库
		this.threadNo = threadNo;
		this.inputQueue = inputQueue;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
		this.interrupt();
	}

	@Override
	public void run() {
		while(true){
			try {
				if (inputQueue.isEmpty() || stopflag) {
					//stdout.println(threadNo+" Producer exited");
					break;
				}
				
				String IPAndDomain = inputQueue.take();
				System.out.println("checking : "+IPAndDomain);
				String ip = IPAndDomain.split("###")[0];
				String domain = IPAndDomain.split("###")[1];
				doRequest(ip,domain);

			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}
	
	public void doRequest(String IP,String domain) {
		//构造请求包
		URL httpUrl;
		try {
			httpUrl = new URL("http://"+IP);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		byte[] request = helpers.buildHttpRequest(httpUrl);
		byte[] newRequest = getter.addOrUpdateHeader(true, request, "Host", domain);
		
		//构造service
		IHttpService httpService = helpers.buildHttpService(IP, 80, false);
		IHttpService httpsService = helpers.buildHttpService(IP, 443, true);
		
		//发送请求
		IHttpRequestResponse messageinfo  = callbacks.makeHttpRequest(httpService, newRequest);
		LineEntry entry = new LineEntry(messageinfo,LineEntry.CheckStatus_UnChecked,"GatewayBypassCheck");
		if (entry.getStatuscode() != 403 && entry.getStatuscode() != -1) {
			runnerTableModel.addNewLineEntry(entry);
		}
		
		IHttpRequestResponse messageinfo1  = callbacks.makeHttpRequest(httpsService, newRequest);
		LineEntry entry1 = new LineEntry(messageinfo1,LineEntry.CheckStatus_UnChecked,"GatewayBypassCheck");
		if (entry.getStatuscode() != 403 && entry.getStatuscode() != -1) {
			runnerTableModel.addNewLineEntry(entry1);
		}
	}
}