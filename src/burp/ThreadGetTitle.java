package burp;

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

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
class ThreadGetTitle{
	private Set<String> domains;
	private List<Producer> plist;
	private List<Consumer> clist;
	
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();
	
	public ThreadGetTitle(Set<String> domains) {
		this.domains = domains;
	}

	public List<String> Do(){
		stdout.println("~~~~~~~~~~~~~Start threading Get Title~~~~~~~~~~~~~");
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();//use to store domains
		BlockingQueue<IHttpRequestResponse> sharedQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
		BlockingQueue<String> lineQueue = new LinkedBlockingQueue<String>();//use to store output---line
		Iterator<String> it = domains.iterator();
		while(it.hasNext()) {
			String domain = it.next();
			domainQueue.add(domain);
		}

		plist = new ArrayList<Producer>();
		clist = new ArrayList<Consumer>();

		for (int i=0;i<=10;i++) {
			Producer p = new Producer(domainQueue,sharedQueue,i);
			//Producer p = new Producer(callbacks,domainQueue,sharedQueue,i);
			p.start();
			plist.add(p);
		}


		for (int i=0;i<=10;i++) {
			Consumer c = new Consumer(sharedQueue,lineQueue,i);
			c.start();
			clist.add(c);
		}

		while(true) {//to wait all threads exit.
			if (domainQueue.isEmpty() && isAllProductorFinished()) {
				for (Consumer c:clist) {
					c.stopThread();
				}
				stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
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

		//save line as json
		BurpExtender.getDomainResult().setLineJsons(BurpExtender.getTitleTableModel().getLineJsons());
		return BurpExtender.getTitleTableModel().getLineJsons();
	}

	boolean isAllProductorFinished(){
		for (Producer p:plist) {
			if(p.isAlive()) {
				return false;
			}
		}
		return true;
	}
	
	public void stopThreads() {
		for (Producer p:plist) {
			p.stopThread();
		}
		for (Consumer c:clist) {
			c.stopThread();
		}
		stdout.println("threads stopped!");
	}
}

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

class Producer extends Thread {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private final BlockingQueue<IHttpRequestResponse> sharedQueue;
	private int threadNo;
	private boolean stopflag = false;
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

	public Producer(BlockingQueue<String> domainQueue,BlockingQueue<IHttpRequestResponse> sharedQueue,int threadNo) {
		this.threadNo = threadNo;
		this.domainQueue = domainQueue;
		this.sharedQueue = sharedQueue;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (domainQueue.isEmpty() || stopflag) {
					//stdout.println("Producer break");
					break;
				}

				String host = domainQueue.take();
				Getter getter = new Getter(helpers);
				//stdout.print(host+" ");
				IHttpService http = helpers.buildHttpService(host,80,"http");
				IHttpService https = helpers.buildHttpService(host,443,"https");

				byte[] httpRequest = helpers.buildHttpRequest(new URL(http.toString()));
				IHttpRequestResponse httpMessageinfo = callbacks.makeHttpRequest(http, httpRequest);
				//stdout.println("messageinfo"+JSONObject.toJSONString(messageinfo));
				//这里有2种异常情况：1.请求失败（连IP都解析不了）；2.请求成功但是响应包为空（可以解析IP，比如内网域名）。
				//第一种请求在这里就结束了，第二种情况的请求信息会传递到consumer中进行IP获取的操作。
				byte[] httpBody = getter.getBody(false, httpMessageinfo);
				int httpStatus = getter.getStatusCode(httpMessageinfo);//当为第二种异常时，httpStatus == -1
				String location = getter.getHeaderValueOf(false, httpMessageinfo, "Location");

				byte[] httpsRequest = helpers.buildHttpRequest(new URL(https.toString()));
				IHttpRequestResponse httpsMessageinfo = callbacks.makeHttpRequest(https, httpsRequest);
				byte[] httpsBody = getter.getBody(false, httpsMessageinfo);
				int httpsStatus = getter.getStatusCode(httpsMessageinfo);
				//	sharedQueue.add(httpMessageinfo);
				//	sharedQueue.add(httpsMessageinfo);
				
				if (httpStatus == httpsStatus && Arrays.equals(httpBody,httpsBody)) {//parameters can be null,great
					sharedQueue.add(httpMessageinfo);
				}else if( 300 <= httpStatus && httpStatus <400 && location.equalsIgnoreCase(https.toString()+"/") ) {//redirect to https
					sharedQueue.add(httpsMessageinfo);
				}else {
					sharedQueue.add(httpMessageinfo);
					sharedQueue.add(httpsMessageinfo);
				}
			} catch (Throwable error) {//java.lang.RuntimeException can't been catched, why?
			}
		}
	}
}

/*
 * parse IResponseInfo object to line object
 * 
 */

class Consumer extends Thread{// Consumer
	private final BlockingQueue<IHttpRequestResponse> sharedQueue;
	private final BlockingQueue<String> lineQueue;//use to store output---line
	private int threadNo;
	private boolean stopflag = false;
	
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

	public Consumer (BlockingQueue<IHttpRequestResponse> sharedQueue,BlockingQueue<String> lineQueue,int threadNo) {
		this.sharedQueue = sharedQueue;
		this.lineQueue = lineQueue;
		this.threadNo = threadNo;
		stopflag = false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			if (stopflag) {//消费者需要的时间更短，不能使用sharedQueue是否为空作为进程是否结束的依据。
				break;
			}
			try {
				IHttpRequestResponse messageinfo = sharedQueue.take();
				String host = messageinfo.getHttpService().getHost();
				Set<String> IPSet;
				Set<String> CDNSet;
				if (Commons.isValidIP(host)) {
					IPSet = new HashSet<>();
					IPSet.add(host);
					CDNSet = new HashSet<>();
					CDNSet.add("");
				}else {
					HashMap<String,Set<String>> result = Commons.dnsquery(host);
					IPSet = result.get("IP");
					CDNSet = result.get("CDN");
				}

				if (messageinfo.getResponse() ==null) {
					stdout.println("--- ["+messageinfo.getHttpService().toString()+"] --- has no response.");
					BurpExtender.getTitleTableModel().addNewNoResponseDomain(host, IPSet);
				}else {
					Getter getter = new Getter(helpers);
					String body = new String(getter.getBody(false, messageinfo));
					String url = messageinfo.getHttpService().toString();
					String URLAndbodyText = messageinfo.getHttpService().toString()+body;


					LineEntry linefound = findHistory(url);
					boolean isChecked = false;
					String comment = "";
					boolean isNew = true;

					if (null != linefound) {
						isChecked = linefound.isChecked();
						comment = linefound.getComment();
						//stderr.println(new String(linefound.getResponse()));
						try {
							String text = linefound.getUrl()+linefound.getBodyText();
							if (text.equalsIgnoreCase(URLAndbodyText) && isChecked) {
								isNew = false;
							}
						}catch(Exception err) {
							err.printStackTrace(stderr);
						}
					}

					BurpExtender.getTitleTableModel().addNewLineEntry(new LineEntry(messageinfo,isNew,isChecked,comment,IPSet,CDNSet));

					//stdout.println(new LineEntry(messageinfo,true).ToJson());
					stdout.println("+++ ["+messageinfo.getHttpService().toString()+"] +++ get title done.");
				}
				//we don't need to add row to table manually,just call fireTableRowsInserted in TableModel
			} catch (Exception err) {
				err.printStackTrace(stderr);
			}
		}
	}

	public LineEntry findHistory(String url) {
		List<String> HistoryLines = BurpExtender.getDomainResult().getHistoryLineJsons();
		for (String his:HistoryLines) {
			LineEntry line = new LineEntry().FromJson(his);
			line.setHelpers(helpers);
			if (url.equalsIgnoreCase(line.getUrl())) {
				return line;
			}
		}
		return null;
	}
}
