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
	public HelperPlus getter = BurpExtender.getHelperPlus();

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
				//System.out.println("checking : "+IPAndDomain);
				String IPURL = IPAndDomain.split("###")[0];
				String domain = IPAndDomain.split("###")[1];
				doRequest(IPURL,domain);

			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}
	
	public void doRequest(String IPURL,String domain) {
		//构造请求包
		URL Url;
		try {
			Url = new URL(IPURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		byte[] request = helpers.buildHttpRequest(Url);
		byte[] newRequest = getter.addOrUpdateHeader(true, request, "Host", domain);
		
		//构造service
		IHttpService service = helpers.buildHttpService(Url.getHost(), Url.getPort(), Url.getProtocol());
		
		//发送请求
		IHttpRequestResponse messageinfo  = callbacks.makeHttpRequest(service, newRequest);
		LineEntry entry = new LineEntry(messageinfo,LineEntry.CheckStatus_UnChecked,"GatewayBypassCheck");
		System.out.println(String.format("curl %s -H 'Host: %s' -k -vv  %s", service.toString(),domain,entry.getStatuscode()));
		if (entry.getStatuscode() <= 400 && entry.getStatuscode() >= -1) {
			runnerTableModel.addNewLineEntryWithHost(entry,domain);
		}
	}
}