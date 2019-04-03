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

import test.iteratortest;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
class ThreadGetTitle{
	private Set<String> domains;
	private List<Producer> plist;

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
		domainQueue.addAll(domains);

		plist = new ArrayList<Producer>();

		for (int i=0;i<=20;i++) {
			Producer p = new Producer(domainQueue,i);
			//Producer p = new Producer(callbacks,domainQueue,sharedQueue,i);
			p.start();
			plist.add(p);
		}

		while(true) {//to wait all threads exit.
			if (domainQueue.isEmpty() && isAllProductorFinished()) {
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
	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
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
					//stdout.println("Producer break");
					break;
				}
				String host = domainQueue.take();

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
					continue;
				}else {//默认过滤私有IP
					String ip = new ArrayList<>(IPSet).get(0);
					if (IPAddress.isPrivateIPv4(ip)) {
						continue;
					}
				}
				
				

				//第二步：对成功解析的host进行HTTP请求。
				Getter getter = new Getter(helpers);
				IHttpService http = helpers.buildHttpService(host,80,"http");
				IHttpService https = helpers.buildHttpService(host,443,"https");

				byte[] http_Request = helpers.buildHttpRequest(new URL(http.toString()));
				IHttpRequestResponse http_Messageinfo = callbacks.makeHttpRequest(http, http_Request);
				//stdout.println("messageinfo"+JSONObject.toJSONString(messageinfo));
				//这里有2种异常情况：1.请求失败（连IP都解析不了,已经通过第一步过滤了）；2.请求成功但是响应包为空（可以解析IP，比如内网域名）。
				//第一种请求在这里就结束了，第二种情况的请求信息会传递到consumer中进行IP获取的操作。
				byte[] http_Body = getter.getBody(false, http_Messageinfo);
				int http_Status = getter.getStatusCode(http_Messageinfo);//当为第二种异常时，httpStatus == -1
				String location = getter.getHeaderValueOf(false, http_Messageinfo, "Location");

				byte[] https_Request = helpers.buildHttpRequest(new URL(https.toString()));
				IHttpRequestResponse https_Messageinfo = callbacks.makeHttpRequest(https, https_Request);
				byte[] https_Body = getter.getBody(false, https_Messageinfo);
				int https_Status = getter.getStatusCode(https_Messageinfo);//当为第二种异常时，httpStatus == -1

				Set<IHttpRequestResponse> tmpSet = new HashSet<IHttpRequestResponse>();


				//去重
				if (http_Status == https_Status && Arrays.equals(http_Body,https_Body)) {//parameters can be null,great
					tmpSet.add(http_Messageinfo);
				}else if( 300 <= http_Status && http_Status <400 && location.equalsIgnoreCase(https.toString()+"/") ) {//redirect to https
					tmpSet.add(https_Messageinfo);
				}else {
					tmpSet.add(http_Messageinfo);
					tmpSet.add(https_Messageinfo);
				}

				//根据请求有效性分类处理
				Iterator<IHttpRequestResponse> it = tmpSet.iterator();
				while (it.hasNext()) {
					IHttpRequestResponse item = it.next();

					String url = item.getHttpService().toString();
					int status = getter.getStatusCode(item);
					
					if (item.getResponse() == null) {
						stdout.println("--- ["+url+"] --- has no response.");
						BurpExtender.getTitleTableModel().addNewNoResponseDomain(host, IPSet);
					}else if(status >= 500){
						stdout.println("--- ["+url+"] --- status code >= 500.");
						BurpExtender.getTitleTableModel().addNewNoResponseDomain(host, IPSet);
					}else {
						byte[] byteBody = getter.getBody(false, item);
						String body = new String(byteBody);
						
						String URLAndbodyText = item.getHttpService().toString()+body;
						
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
						BurpExtender.getTitleTableModel().addNewLineEntry(new LineEntry(item,isNew,isChecked,comment,IPSet,CDNSet));

						//stdout.println(new LineEntry(messageinfo,true).ToJson());
						stdout.println("+++ ["+url+"] +++ get title done.");
					}
				}
			} catch (Exception error) {
				error.printStackTrace(stderr);
				continue;
				//java.lang.RuntimeException can't been catched, why?
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
