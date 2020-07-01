package title;

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

import GUI.GUI;
import Tools.LineConfig;
import burp.BurpExtender;
import burp.Commons;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IPAddress;

/** 
* @author bit4woo
* @github https://github.com/bit4woo 
* @version CreateTime：Jun 25, 2020 2:35:31 PM 
*/

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

public class Producer extends Thread {//Producer do
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
					String url = item.getUrl();
					LineEntry linefound = findHistory(url);
					if (null != linefound) {
						item.setComment(linefound.getComment());
						item.setLevel(linefound.getLevel());
						try {
							if (url.equalsIgnoreCase(linefound.getUrl()) && item.getBodyText().length() == linefound.getBodyText().length()) {
								item.setCheckStatus(linefound.getCheckStatus());
								item.setTime(linefound.getTime());
							}
						}catch(Exception err) {
							err.printStackTrace(stderr);
						}
					}

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
		IndexedLinkedHashMap<String,LineEntry> HistoryLines = GUI.getTitlePanel().getBackupLineEntries();
		if (HistoryLines == null) return null;
		LineEntry found = HistoryLines.get(url);
		if (found != null) {
			HistoryLines.replace(url,null);//不使用remove和put操作，避免//ConcurrentModificationException问题
			//因为这2个操作都会让map的长度发生变化，从而导致问题
			return found;
		}
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		for (LineEntry line:HistoryLines.values()) {
			if (line== null) {
				continue;
			}
			line.setHelpers(helpers);
			try{//根据host查找
				String host = new URL(url).getHost();

				List<String> lineHost = new ArrayList<>(Arrays.asList(line.getIP().trim().split(",")));
				lineHost.add(line.getHost());
				if (lineHost.contains(host)) {
					//HistoryLines.remove(line.getUrl());//如果有相同URL的记录，就删除这个记录。//ConcurrentModificationException
					HistoryLines.replace(url,null);
					return line;
				}
			}catch (Exception e){
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return null;
	}

	public static LineEntry doRequest(URL url) {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		String cookie = TitlePanel.getCookie();

		byte[] byteRequest = helpers.buildHttpRequest(url);//GET
		byteRequest = Commons.buildCookieRequest(helpers,cookie,byteRequest);

		IHttpService service = helpers.buildHttpService(url.getHost(),url.getPort(),url.getProtocol());
		IHttpRequestResponse https_Messageinfo = callbacks.makeHttpRequest(service, byteRequest);
		LineEntry Entry = new LineEntry(https_Messageinfo);
		return Entry;
	}

	public static Set<LineEntry> doGetTitle(String host) throws MalformedURLException {

		int httpPort = 80;
		int httpsPort = 443;
		Set<LineEntry> resultSet = new HashSet<LineEntry>();


		if (host.contains(":")) {//处理带有端口号的域名
			String port = host.substring(host.indexOf(":")+1,host.length());
			host = host.substring(0,host.indexOf(":"));
			if (port.length()>0) {
				httpPort = Integer.parseInt(port);
				httpsPort = httpPort;
			}
		}

		URL httpURL = new URL(String.format("http://%s:%s/",host,httpPort));
		URL httpsURL = new URL(String.format("https://%s:%s/",host,httpsPort));

		//第一步：IP解析
		Set<String> IPSet = new HashSet<>();
		Set<String> CDNSet = new HashSet<>();

		boolean isInPrivateNetwork = LineConfig.isPrivateNetworkWorkingModel(); 
		if (Commons.isValidIP(host)) {//目标是一个IP
			if (IPAddress.isPrivateIPv4(host) && !isInPrivateNetwork) {//外网模式，内网IP，直接返回。
				return resultSet;
			}else {
				IPSet.add(host);
				CDNSet.add("");
			}
		}else {//目标是域名
			HashMap<String,Set<String>> result = Commons.dnsquery(host);
			IPSet = result.get("IP");
			CDNSet = result.get("CDN");

			if (IPSet.size() <= 0) {
				//TODO 是否应该移除无效域名？理清楚：无效域名，黑名单域名，无响应域名等情况。

				return resultSet;
			}else {//默认过滤私有IP
				String ip = new ArrayList<>(IPSet).get(0);
				if (IPAddress.isPrivateIPv4(ip) && !isInPrivateNetwork) {//外网模式，内网域名，仅仅显示域名和IP。
					LineEntry entry = new LineEntry(host,IPSet);
					entry.setTitle("Private IP");
					resultSet.add(entry);
					return resultSet;
				}
			}
		}

		//第二步：对成功解析的host进行HTTP请求。

		//https://superuser.com/questions/1054724/how-to-make-firefox-ignore-all-ssl-certification-errors
		//仍然改为先请求http，http不可用再请求https.避免浏览器中证书问题重复点击很麻烦

		//do http request first
		LineEntry httpEntry = doRequest(httpURL);
		httpEntry.setIPWithSet(IPSet);
		httpEntry.setCDNWithSet(CDNSet);
		//stdout.println("messageinfo"+JSONObject.toJSONString(messageinfo));
		//这里有2种异常情况：1.请求失败（连IP都解析不了,已经通过第一步过滤了）；2.请求成功但是响应包为空（可以解析IP，比如内网域名）。
		//第一种请求在这里就结束了，第二种情况的请求信息会传递到consumer中进行IP获取的操作。
		if (LineConfig.doFilter(httpEntry)) {	
			String location = httpEntry.getHeaderValueOf(false,"Location");
			if (location == null || !location.startsWith("https://"+host)) {//如果是跳转到https，还是请求https
				resultSet.add(httpEntry);
			}
		}


		//在http不可用，或者设置为不忽略https的情况下
		if (resultSet.size() ==0 || !LineConfig.isIgnoreHttpsIfHttpOK()) {

			LineEntry httpsEntry = doRequest(httpsURL);
			httpsEntry.setIPWithSet(IPSet);
			httpsEntry.setCDNWithSet(CDNSet);

			boolean httpsOK = LineConfig.doFilter(httpsEntry);

			if (httpsOK) {
				resultSet.add(httpsEntry);
			}
		}


		//do request for external port, 8000,8080, 

		if (TitlePanel.getExternalPortList() != null && TitlePanel.getExternalPortList().size() != 0) {
			for (int port: TitlePanel.getExternalPortList()) {

				//do http request
				URL ex_http = new URL("http://"+host+":"+port+"/");
				LineEntry exhttpEntry = doRequest(ex_http);
				exhttpEntry.setIPWithSet(IPSet);
				exhttpEntry.setCDNWithSet(CDNSet);

				if (LineConfig.doFilter(exhttpEntry)) {
					resultSet.add(exhttpEntry);
					continue;
				}

				//do https request
				URL ex_https = new URL("https://"+host+":"+port+"/");
				LineEntry exhttpsEntry = doRequest(ex_https);
				exhttpsEntry.setIPWithSet(IPSet);
				exhttpsEntry.setCDNWithSet(CDNSet);

				if (LineConfig.doFilter(exhttpsEntry)) {
					resultSet.add(exhttpsEntry);
				}

			}
		}

		return resultSet;
	}
}