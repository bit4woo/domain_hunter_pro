package title;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import domain.DomainPanel;

public class TempLineEntry {
	String host;
	URL httpURL;
	URL httpsURL;
	Set<String> IPSet = new HashSet<>();
	Set<String> CDNSet = new HashSet<>();

	public TempLineEntry(String host){
		this.host = host;
		hostToURL(this.host);
		GetIPAndCDN(this.host);
	}

	public Set<LineEntry> getFinalLineEntry(){
		return doGetTitle();
	}

	private void hostToURL(String host){
		try{
			if (host.contains(":")) {//处理带有端口号的域名
				String port = host.substring(host.indexOf(":")+1);
				host = host.substring(0,host.indexOf(":"));

				int tmpPort = Integer.parseInt(port);
				if (tmpPort ==80){
					httpURL = new URL(String.format("http://%s:%s/",host,tmpPort));
				}else if(tmpPort== 443){
					httpsURL = new URL(String.format("https://%s:%s/",host,tmpPort));
				}else{
					httpURL = new URL(String.format("http://%s:%s/",host,tmpPort));
					httpsURL = new URL(String.format("https://%s:%s/",host,tmpPort));
				}
			}else {
				httpURL = new URL(String.format("http://%s:%s/",host,80));
				httpsURL = new URL(String.format("https://%s:%s/",host,443));
			}
		}catch (Exception e){
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
				if (ToolPanel.ignoreWrongCAHost.isSelected() && null!=httpsURL){
					Set<String> certDomains = CertInfo.isTarget(httpsURL.toString(), DomainPanel.domainResult.fetchKeywordSet());
					if (null !=certDomains && certDomains.isEmpty()) {//只有成功获取证书，并且匹配集合为空，才能完全确定不是目标
						//在host是IP的情况下，证书不匹配，整改host都不需要再处理了。
						return;
					}else {//主机是host的情况下，将证书中的域名写入CDN字段，
						CDNSet = certDomains;
					}
				}
			}
		}else {//目标是域名
			HashMap<String,Set<String>> result = Commons.dnsquery(host);
			IPSet = result.get("IP");
			CDNSet = result.get("CDN");
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
				resultSet.add(entry);
				return resultSet;
			}
		}

		//第二步：对成功解析的host进行HTTP请求。

		//https://superuser.com/questions/1054724/how-to-make-firefox-ignore-all-ssl-certification-errors
		//仍然改为先请求http，http不可用再请求https.避免浏览器中证书问题重复点击很麻烦

		//do http request first
		if (httpURL!=null){
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
		}

		if (null != httpsURL){
			//在http不可用，或者设置为不忽略https的情况下
			if (resultSet.size() ==0 || !LineConfig.isIgnoreHttpsOrHttpIfOneOK()) {

				LineEntry httpsEntry = doRequest(httpsURL);
				httpsEntry.setIPWithSet(IPSet);
				httpsEntry.setCDNWithSet(CDNSet);

				boolean httpsOK = LineConfig.doFilter(httpsEntry);

				if (httpsOK) {
					resultSet.add(httpsEntry);
				}
			}
		}

		//do request for external port, 8000,8080,
		Set<String> ExternalPorts = ToolPanel.getExternalPortSet();
		if (ExternalPorts.size() != 0) {
			for (String port: ExternalPorts) {

				try{
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
				}catch (Exception e){

				}
			}
		}
		return resultSet;
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
}