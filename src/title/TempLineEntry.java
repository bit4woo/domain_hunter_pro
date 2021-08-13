package title;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Tools.LineConfig;
import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IPAddress;
import domain.CertInfo;
import domain.DomainManager;
import domain.DomainPanel;

public class TempLineEntry {
	public static final String NotTargetBaseOnCertDomains = "NotTargetBaseOnCertDomains";
	String host;
	int port;

	URL defaultHttpUrl;
	URL defaultHttpsUrl;

	URL customPortHttpUrl;
	URL customPortHttpsUrl;

	List<URL> externalPortUrls = new ArrayList<>();

	Set<String> IPSet = new HashSet<>();
	Set<String> CDNSet = new HashSet<>();
	Set<String> certDomains = new HashSet<>();

	public TempLineEntry(String host){
		DomainManager domainResult = DomainPanel.getDomainResult();
		if (hostCheckAndParse(host) && domainResult != null){
			hostToURL(this.host);
			GetIPAndCDN(this.host);
			certDomains = getCertDomains();
		}
	}

	public Set<LineEntry> getFinalLineEntry(){
		if (host ==null) return new HashSet<>();//无效host直接返回

		if (ToolPanel.ignoreWrongCAHost.isSelected()){
			if (DomainPanel.getDomainResult().isTargetByCertInfo(certDomains)){
				return new HashSet<>();
			};
		}
		return doGetTitle();
	}

	//输入解析
	private boolean hostCheckAndParse(String inputHost){
		inputHost = inputHost.trim();

		if (inputHost.contains(":")) {//处理带有端口号的域名
			String tmpport = inputHost.substring(inputHost.indexOf(":") + 1);
			port = Integer.parseInt(tmpport);
			host = inputHost.substring(0, inputHost.indexOf(":"));
		}else {
			host = inputHost;
			port = -1;
		}

		return Commons.isValidIP(host) || Commons.isValidDomain(host);
	}

	//将域名或IP拼接成URL
	private void hostToURL(String host){
		try{
			defaultHttpUrl = new URL(String.format("http://%s:%s/",host,80));
			defaultHttpsUrl = new URL(String.format("https://%s:%s/",host,443));

			if (port == -1 || port == 80 || port == 443){
				//Nothing to do;
			}else{
				customPortHttpUrl = new URL(String.format("http://%s:%s/",host,port));
				customPortHttpsUrl = new URL(String.format("https://%s:%s/",host,port));
			}

			//处理用户指定的端口，for external port, eg. 8000,8080,
			Set<String> ExternalPorts = ToolPanel.getExternalPortSet();
			if (ExternalPorts.size() != 0) {
				for (String port: ExternalPorts) {
					int tmpPort = Integer.parseInt(port);
					if (tmpPort == 80 || tmpPort == 443) continue;
					URL ex_http = new URL(String.format("http://%s:%s/",host,tmpPort));
					URL ex_https = new URL(String.format("https://%s:%s/",host,tmpPort));
					externalPortUrls.add(ex_https);
					externalPortUrls.add(ex_http);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	private void GetIPAndCDN(String host){
		//第一步：IP解析
		boolean isInPrivateNetwork = TitlePanel.tempConfig.isHandlePriavte();

		if (Commons.isValidIP(host)) {//目标是一个IP
			if (IPAddress.isPrivateIPv4(host) && !isInPrivateNetwork) {//外网模式，内网IP，直接返回。
				return;
			}else {
				IPSet.add(host);
				CDNSet.add("");
			}
		}else {//目标是域名
			HashMap<String,Set<String>> result = Commons.dnsquery(host);
			IPSet = result.get("IP");
			CDNSet = result.get("CDN");
		}
	}

	private Set<String> getCertDomains() {
		try {
			Set<String> certDomains = CertInfo.getAllSANs(defaultHttpsUrl.toString());
			if (null == certDomains && customPortHttpsUrl != null) {
				certDomains = CertInfo.getAllSANs(customPortHttpsUrl.toString());
			}
			return certDomains;
		} catch (Exception e) {
			return new HashSet<String>();
		}
	}

	private Set<LineEntry> doGetTitle(){
		Set<LineEntry> resultSet = new HashSet<>();
		boolean isInPrivateNetwork = TitlePanel.tempConfig.isHandlePriavte();

		if (IPSet.size() <= 0) {
			//TODO 是否应该移除无效域名？理清楚：无效域名，黑名单域名，无响应域名等情况。
		}else {//默认过滤私有IP
			String ip = new ArrayList<>(IPSet).get(0);
			if (IPAddress.isPrivateIPv4(ip) && !isInPrivateNetwork) {//外网模式，内网域名，仅仅显示域名和IP。
				LineEntry entry = new LineEntry(host,IPSet);
				entry.setTitle("Private IP");
				addInfoToEntry(entry);
				resultSet.add(entry);
				return resultSet;
			}
		}

		//第二步：对成功解析的host进行HTTP请求。

		//https://superuser.com/questions/1054724/how-to-make-firefox-ignore-all-ssl-certification-errors
		//仍然改为先请求http，http不可用再请求https.避免浏览器中证书问题重复点击很麻烦

		//2.1 do http request first
		try {
			LineEntry httpEntry;
			if (customPortHttpUrl !=null){
				httpEntry = doRequest(customPortHttpUrl);
			}else {
				httpEntry = doRequest(defaultHttpUrl);
			}
			addInfoToEntry(httpEntry);
			//stdout.println("messageinfo"+JSONObject.toJSONString(messageinfo));
			//这里有2种异常情况：1.请求失败（连IP都解析不了,已经通过第一步过滤了）；2.请求成功但是响应包为空（可以解析IP，比如内网域名）。
			//第一种请求在这里就结束了，第二种情况的请求信息会传递到consumer中进行IP获取的操作。
			if (LineConfig.doFilter(httpEntry)) {
				String location = httpEntry.getHeaderValueOf(false,"Location");
				if (location == null || !location.startsWith("https://"+host)) {//如果是跳转到https，还是请求https
					resultSet.add(httpEntry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}

		//2.2 do https request:在http不可用，或者设置为不忽略https的情况下
		try {
			if (resultSet.size() ==0 || !LineConfig.isIgnoreHttpsOrHttpIfOneOK()) {
				LineEntry httpsEntry;
				if (null != customPortHttpsUrl){
					httpsEntry = doRequest(customPortHttpsUrl);
				}else {
					httpsEntry = doRequest(defaultHttpsUrl);
				}
				addInfoToEntry(httpsEntry);
				boolean httpsOK = LineConfig.doFilter(httpsEntry);
				if (httpsOK) {
					resultSet.add(httpsEntry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}

		//2.3 do externalPort request
		try {
			for(URL url:externalPortUrls) {
				LineEntry entry = doRequest(url);
				boolean pass = LineConfig.doFilter(entry);
				if (pass) {
					addInfoToEntry(entry);
					resultSet.add(entry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
		
		//设置Icon hash字段
		for (LineEntry entry:resultSet){
			String url = entry.getUrl();
			String hash = WebIcon.getHash(url);
			entry.setIcon_hash(hash);
		}
		
		//当域名可以解析，但是所有URL请求都失败的情况下。添加一条DNS解析记录
		//TODO 但是IP可以ping通但是无成功的web请求的情况还没有处理
		if (resultSet.isEmpty()){
			if (Commons.isValidDomain(host)&& !IPSet.isEmpty()) {
				LineEntry entry = new LineEntry(host,IPSet);
				entry.setUrl(host);//将host作为URL字段
				entry.setTitle("DNS Record");
				addInfoToEntry(entry);
				resultSet.add(entry);
				return resultSet;
			}
		}
		
		return resultSet;
	}

	private void addInfoToEntry(LineEntry entry) {
		if (entry == null) return;
		entry.setIPWithSet(IPSet);
		entry.setCDNWithSet(CDNSet);
		entry.setCertDomainWithSet(certDomains);
		if (DomainPanel.getDomainResult().isTargetByCertInfo(certDomains)){
			entry.removeComment(LineEntry.NotTargetBaseOnCertInfo);
		}else{
			entry.addComment(LineEntry.NotTargetBaseOnCertInfo);
		};
		if (Commons.isValidIP(host)) {
			if (DomainPanel.getDomainResult().isTargetByBlackList(host)){
				entry.removeComment(LineEntry.NotTargetBaseOnBlackList);
			}else{
				entry.addComment(LineEntry.NotTargetBaseOnBlackList);
			};
		}
	}

	//Just do request
	private static LineEntry doRequest(URL url) {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		String cookie = TitlePanel.tempConfig.getCookie();

		byte[] byteRequest = helpers.buildHttpRequest(url);//GET
		byteRequest = Commons.buildCookieRequest(helpers,cookie,byteRequest);

		IHttpService service = helpers.buildHttpService(url.getHost(),url.getPort(),url.getProtocol());
		IHttpRequestResponse https_Messageinfo = BurpExtender.getCallbacks().makeHttpRequest(service, byteRequest);
		LineEntry Entry = new LineEntry(https_Messageinfo);
		return Entry;
	}

	public static void test(){
		TempLineEntry tmp = new TempLineEntry("10.162.32.16:9100");
		tmp.getFinalLineEntry();
	}

	public static void main(String[] args ){
		test();
	}
}