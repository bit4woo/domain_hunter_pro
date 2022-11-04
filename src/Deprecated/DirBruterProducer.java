package thread;

import java.io.PrintWriter;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

import burp.BurpExtender;
import burp.Getter;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import title.LineEntry;
import title.LineTableModel;

/**
 * 执行路径爆破的线程
 * @author bit4woo
 *
 */
public class DirBruterProducer extends Thread {//Producer do
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