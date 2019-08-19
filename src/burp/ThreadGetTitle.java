package burp;

import java.io.PrintWriter;
import java.net.MalformedURLException;
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

import Config.LineConfig;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
class ThreadGetTitle{
	private Set<String> domains;
	private List<Producer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public ThreadGetTitle(Set<String> domains) {
		this.domains = domains;
	}

	public void Do(){
		stdout.println("~~~~~~~~~~~~~Start threading Get Title~~~~~~~~~~~~~ total task number: "+domains.size());
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();//use to store domains
		domainQueue.addAll(domains);

		plist = new ArrayList<Producer>();

		for (int i=0;i<=50;i++) {
			Producer p = new Producer(domainQueue,i);
			//Producer p = new Producer(callbacks,domainQueue,sharedQueue,i);
			p.start();
			plist.add(p);
		}

		long waitTime = 0; 
		while(true) {//to wait all threads exit.
			if (domainQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
				break;
			}else if(domainQueue.isEmpty() && waitTime >=10*60*1000){
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
		for (Producer p:plist) {
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
		for (Producer p:plist) {
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

class Producer extends Thread {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public Producer(BlockingQueue<String> domainQueue,int threadNo) {
		this.threadNo = threadNo;
		this.domainQueue = domainQueue;
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
					//stdout.println(threadNo+" Producer exited");
					break;
				}
				String host = domainQueue.take();
				int leftTaskNum = domainQueue.size();
				
				stdout.print(String.format("%s tasks left ",leftTaskNum));

				Set<LineEntry> resultSet  = doGetTitle(host);
				//根据请求有效性分类处理
				Iterator<LineEntry> it = resultSet.iterator();
				while (it.hasNext()) {
					LineEntry item = it.next();

					String url = item.getProtocol()+"://"+item.getHost()+":"+item.getPort();
					
					String body = item.getBodyText();

					String URLAndbodyText = url+body;

					LineEntry linefound = findHistory(url);
					boolean isChecked = false;
					String comment = "";

					if (null != linefound) {
						comment = linefound.getComment();
						try {
							String text = linefound.getUrl()+linefound.getBodyText();
							if (text.equalsIgnoreCase(URLAndbodyText)) {
								isChecked = linefound.isChecked();
							}
						}catch(Exception err) {
							err.printStackTrace(stderr);
						}
					}
					
					item.setChecked(isChecked);
					item.setComment(comment);

					
					TitlePanel.getTitleTableModel().addNewLineEntry(item);

					//stdout.println(new LineEntry(messageinfo,true).ToJson());

					stdout.println(String.format("+++ [%s] +++ get title done",url));
				}
			} catch (Exception error) {
				error.printStackTrace(stderr);
				continue;//unnecessary
				//java.lang.RuntimeException can't been catched, why?
			}
		}
	}

	public static LineEntry findHistory(String url) {
		BurpExtender.getGui();
		List<LineEntry> HistoryLines = GUI.getTitlePanel().getBackupLineEntries();
		if (HistoryLines == null) return null;
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		for (LineEntry line:HistoryLines) {
			line.setHelpers(helpers);
			if (url.equalsIgnoreCase(line.getUrl())) {//根据url查找
				return line;
			}

			try{//根据host查找
				String host = new URL(url).getHost();

				List<String> lineHost = new ArrayList<>(Arrays.asList(line.getIP().trim().split(",")));
				lineHost.add(line.getHost());
				if (lineHost.contains(host)) {
					return line;
				}
			}catch (Exception e){
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return null;
	}
	
 	public static Set<LineEntry> doGetTitle(String host) throws MalformedURLException {
 		
 		Set<LineEntry> resultSet = new HashSet<LineEntry>();
 		
		//第一步：IP解析
		Set<String> IPSet = new HashSet<>();
		Set<String> CDNSet = new HashSet<>();
		if (Commons.isValidIP(host)) {
			IPSet.add(host);
			CDNSet.add("");
		}else {
			HashMap<String,Set<String>> result = Commons.dnsquery(host);
			IPSet = result.get("IP");
			CDNSet = result.get("CDN");
		}

		if (IPSet.size() <= 0) {
			return resultSet;
		}else {//默认过滤私有IP
			String ip = new ArrayList<>(IPSet).get(0);
			if (IPAddress.isPrivateIPv4(ip)) {
				return resultSet;
			}
		}


		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		PrintWriter stdout = BurpExtender.getStdout();
		PrintWriter stderr = BurpExtender.getStderr();
		
		//第二步：对成功解析的host进行HTTP请求。
		IHttpService http = helpers.buildHttpService(host,80,"http");
		IHttpService https = helpers.buildHttpService(host,443,"https");
		
		String cookie = TitlePanel.getTextFieldCookie().getText().trim();
		
		
		
		
		//do https request
		byte[] https_Request = helpers.buildHttpRequest(new URL(https.toString()));
		https_Request = Commons.buildCookieRequest(helpers,cookie,https_Request);
		IHttpRequestResponse https_Messageinfo = callbacks.makeHttpRequest(https, https_Request);
		LineEntry httpsEntry = new LineEntry(https_Messageinfo);
		httpsEntry.setIPWithSet(IPSet);
		httpsEntry.setCDNWithSet(CDNSet);

		boolean httpsOK = LineConfig.doFilter(httpsEntry);

		if (httpsOK) {
			resultSet.add(httpsEntry);
		}
		
		if (!httpsOK || !LineConfig.isIgnoreHttpIfHttpsOK()) {
			
			byte[] http_Request = helpers.buildHttpRequest(new URL(http.toString()));
			http_Request = Commons.buildCookieRequest(helpers,cookie,http_Request);
			IHttpRequestResponse http_Messageinfo = callbacks.makeHttpRequest(http, http_Request);
			LineEntry httpEntry = new LineEntry(http_Messageinfo);
			httpEntry.setIPWithSet(IPSet);
			httpEntry.setCDNWithSet(CDNSet);
			//stdout.println("messageinfo"+JSONObject.toJSONString(messageinfo));
			//这里有2种异常情况：1.请求失败（连IP都解析不了,已经通过第一步过滤了）；2.请求成功但是响应包为空（可以解析IP，比如内网域名）。
			//第一种请求在这里就结束了，第二种情况的请求信息会传递到consumer中进行IP获取的操作。
			if (LineConfig.doFilter(httpEntry)) {
//				if (httpEntry.getStatuscode() == httpsEntry.getStatuscode() && httpEntry.getContentLength() == httpsEntry.getContentLength()) {
//					
//				}else if( 300 <= httpEntry.getStatuscode() && httpEntry.getStatuscode() <400 ) {
//					String location = httpEntry.getHeaderValueOf(false,"Location");
//					if (location != null && location.equalsIgnoreCase(https.toString()+"/")) {
//						
//					}else {
//						resultSet.add(httpEntry);
//					}
//				}else {
//					resultSet.add(httpEntry);
//				}
							
				String location = httpEntry.getHeaderValueOf(false,"Location");
				if ((location == null || !location.equalsIgnoreCase(https.toString()+"/")) && 
						httpEntry.getStatuscode() != httpsEntry.getStatuscode()) {
					resultSet.add(httpEntry);
				}
			}
		}
		
		//do request for external port, 8000,8080, 
		
		if (TitlePanel.getExternalPortList() != null) {
			for (int port: TitlePanel.getExternalPortList()) {
				IHttpService ex_https = helpers.buildHttpService(host,port,"https");
				IHttpService ex_http = helpers.buildHttpService(host,port,"http");
				
				//do https request
//				stdout.println("port:"+ex_https);
				byte[] ex_https_Request = helpers.buildHttpRequest(new URL(ex_https.toString()));
				ex_https_Request = Commons.buildCookieRequest(helpers,cookie,ex_https_Request);
				IHttpRequestResponse ex_https_Messageinfo = callbacks.makeHttpRequest(ex_https, ex_https_Request);
				LineEntry exhttpsEntry = new LineEntry(ex_https_Messageinfo);
//				stdout.println("service:"+ex_https_Messageinfo.getHttpService().toString());
//				stdout.println("port:"+exhttpsEntry.getUrl());
				exhttpsEntry.setIPWithSet(IPSet);
				exhttpsEntry.setCDNWithSet(CDNSet);

				boolean exhttpsOK = LineConfig.doFilter(exhttpsEntry);

				if (exhttpsOK) {
					resultSet.add(exhttpsEntry);
					continue;
				}
				
				
				//do http request
				byte[] ex_http_Request = helpers.buildHttpRequest(new URL(ex_http.toString()));
				ex_http_Request = Commons.buildCookieRequest(helpers,cookie,ex_http_Request);
				IHttpRequestResponse ex_http_Messageinfo = callbacks.makeHttpRequest(ex_http, ex_http_Request);
				LineEntry exhttpEntry = new LineEntry(ex_http_Messageinfo);
				exhttpEntry.setIPWithSet(IPSet);
				exhttpEntry.setCDNWithSet(CDNSet);
				
				if (LineConfig.doFilter(exhttpEntry)) {
					resultSet.add(exhttpsEntry);
				}
			}
		}

		return resultSet;
	}
}
