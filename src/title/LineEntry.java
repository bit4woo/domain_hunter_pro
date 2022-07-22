package title;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.hash.HashCode;

import ASN.ASNEntry;
import ASN.ASNQuery;
import burp.BurpExtender;
import burp.Commons;
import burp.Getter;
import burp.HelperPlus;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IPAddressUtils;
import burp.IResponseInfo;

public class LineEntry {
	private static final Logger log=LogManager.getLogger(LineEntry.class);
	public static final String AssetType_A = "重要目标";//像管理后台、统一登录等等一旦有漏洞就危害很高的系统。
	public static final String AssetType_B = "无价值无需再挖";//像官网、首页等对信息收集、目标界定有用；但是对“挖漏洞”来说没有价值的记录。
	public static final String AssetType_C = "未分类";//默认值，还未进行区分的资产或者普通价值资产
	public static final String AssetType_D = "非目标资产";//非目标资产，通常跑网段都会出现这类资产。

	public static final String[] AssetTypeArray = {LineEntry.AssetType_A, LineEntry.AssetType_B, LineEntry.AssetType_C, LineEntry.AssetType_D};

	public static final String CheckStatus_UnChecked = "UnChecked";
	public static final String CheckStatus_Checked = "Done";
	public static final String CheckStatus_Checking = "Checking";
	public static final String CheckStatus_MoreAction = "MoreAction";

	public static final String[] CheckStatusArray = {LineEntry.CheckStatus_UnChecked, LineEntry.CheckStatus_Checking,
			LineEntry.CheckStatus_Checked,LineEntry.CheckStatus_MoreAction};

	public static final String NotTargetBaseOnCertInfo = "CertInfoNotMatch";
	public static final String NotTargetBaseOnBlackList = "IPInBlackList";

	public static final String EntryType_Web = "Web";
	public static final String EntryType_DNS = "DNS";
	public static final String EntryType_Manual_Saved = "Manual_Saved";

	public static String systemCharSet = getSystemCharSet();

	//private transient IHttpRequestResponse messageinfo;
	//给不想被序列化的属性增加transient属性---java特性
	//remove IHttpRequestResponse field ,replace with request+response+httpService(host port protocol). for convert to json.

	//整个数据包IHttpRequestResponse=完整URL地址+请求数据包+响应数据包
	private String url = "";

	//URL中又包含如下三个属性，这个三个属性可以构造出burp中的IHttpService对象
	private int port =-1;
	private String host = "";
	private String protocol ="";
	//these three == IHttpService, helpers.buildHttpService to build. 

	private byte[] request = {};
	private byte[] response = {};
	// request+response+httpService == IHttpRequestResponse,burp的划分方式

	//从response中获取的字段,解析后赋值
	private int statuscode = -1;
	private int contentLength = -1;
	private String title = "";
	private String webcontainer = "";

	private String time = "";

	//需要通过查询或者请求获取的其他信息字段
	private String IP = "";//DNS解析
	private String CDN = "";//CNAME+从证书中获取的关联域名
	private String icon_hash = "";
	private String ASNInfo = "";

	//field for user
	private String CheckStatus =CheckStatus_UnChecked;
	private String AssetType = AssetType_C;
	private String EntryType = EntryType_Web;
	private String comment ="";
	private boolean isManualSaved = false;

	/**
	 * 默认构造函数，序列化、反序列化所需
	 */
	public LineEntry(){

	}

	/**
	 * 用于构造DNS记录
	 * @param host
	 * @param IPset
	 * @return
	 */
	public LineEntry(String host,Set<String> IPset) {
		this.host = host;
		this.port = 80;
		this.protocol ="http";

		if (this.IP != null) {
			this.IP = String.join(",", IPset);
		}
		this.EntryType = EntryType_DNS;
		this.CheckStatus = CheckStatus_Checked;
	}


	/**
	 * 用于构造DNS记录
	 * @param host
	 * @param IPset
	 * @return
	 */
	public LineEntry(String host,String IPStr) {
		this.host = host;
		this.port = 80;
		this.protocol ="http";
		this.IP = IPStr;

		this.EntryType = EntryType_DNS;
		this.CheckStatus = CheckStatus_Checked;
	}

	/**
	 * 用于解析IHttpRequestResponse对象
	 * @param messageinfo
	 */
	public LineEntry(IHttpRequestResponse messageinfo) {
		parse(messageinfo);
	}


	/**
	 * 用于从数据库中恢复对象
	 * @param messageinfo
	 */
	public LineEntry(URL url,byte[] request,byte[] response) {
		parse(url,request,response);
	}

	public LineEntry(IHttpRequestResponse messageinfo,String CheckStatus,String comment) {
		parse(messageinfo);
		this.CheckStatus = CheckStatus;
		this.comment = comment;
	}


	public String ToJson(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.toJSONString(this);
	}

	public static LineEntry FromJson(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.parseObject(json, LineEntry.class);
	}

	private void parse(IHttpRequestResponse messageinfo) {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		URL tmpurl = helpers.analyzeRequest(messageinfo).getUrl();//包含了默认端口

		parse(tmpurl,messageinfo.getRequest(),messageinfo.getResponse());
	}

	/**
	 * 可以用于从数据库中恢复对象
	 * @param url
	 * @param request
	 * @param response
	 */
	private void parse(URL url,byte[] request,byte[] response) {
		try {
			this.url = url.toString();
			port = url.getPort()== -1 ? url.getDefaultPort():url.getPort();
			host = url.getHost();
			protocol = url.getProtocol();

			if (request != null) this.request = request;

			if (response != null) {
				this.response = response;

				IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
				IResponseInfo responseInfo = helpers.analyzeResponse(response);
				statuscode = responseInfo.getStatusCode();

				HelperPlus getter = new HelperPlus(helpers);

				webcontainer = getter.getHeaderValueOf(false, response, "Server");
				byte[] byteBody = HelperPlus.getBody(false, response);
				try{
					contentLength = Integer.parseInt(getter.getHeaderValueOf(false, response, "Content-Length").trim());
				}catch (Exception e){
					if (contentLength==-1 && byteBody!=null) {
						contentLength = byteBody.length;
					}
				}

				title = fetchTitle(response);
			}
		}catch(Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			log.error(e);
		}
	}

	public void DoDirBrute() {

	}

	public void DoGetIconHash() {
		if (EntryType == EntryType_Web) {
			String tmpurl =getUrl();
			this.setIcon_hash(WebIcon.getHash(tmpurl));
		}
	}

	/**
	 * 获取到的URL是包含了默认端口的，因为burp方法的原因
	 * @return
	 */
	public String getUrl() {//为了格式统一，和查找匹配更精确，都包含了默认端口
		if (url == null || url.equals("")) {
			return protocol+"://"+host+":"+port+"/";
		}
		return url;
	}

	/**
	 * 返回通常意义上的URL格式，不包含默认端口。用于搜索
	 * 不要修改原始url的格式！即都包含默认端口。因为数据库中更新对应记录是以URL为依据的，否则不能成功更新记录。
	 * @return
	 */
	public String fetchUrlWithCommonFormate() {
		if (url == null || url.equals("")) {
			url = protocol+"://"+host+":"+port+"/";
		}
		//不要修改原始url的格式！即都包含默认端口。因为数据库中更新对应记录是以URL为依据的，否则不能成功更新记录。
		String usualUrl = HelperPlus.removeDefaultPort(url);
		return usualUrl;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getStatuscode() {
		return statuscode;
	}

	public void setStatuscode(int statuscode) {
		this.statuscode = statuscode;
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	//IPString 222.79.64.33, 124.225.183.63
	public String getIP() {
		return IP;
	}

	//return IP 的集合
	public HashSet<String> fetchIPSet() {
		HashSet<String> result = new HashSet<String>();
		for (String ip: IP.split(",")) {
			result.add(ip.trim());
		}
		return result;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public void setIPWithSet(Set<String> ipSet) {
		IP = String.join(",", ipSet);
	}
	//用于序列化
	public String getCDN() {
		return CDN;
	}
	//用于序列化
	public void setCDN(String cDN) {
		CDN = cDN;
	}

	public void setCDNWithSet(Set<String> cDNSet) {
		CDN = String.join(",", cDNSet);
	}

	public void setCertDomainWithSet(Set<String> certDomains) {
		CDN += " | "+String.join(",", certDomains);
	}

	public String getWebcontainer() {
		return webcontainer;
	}

	public void setWebcontainer(String webcontainer) {
		this.webcontainer = webcontainer;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}


	public String getIcon_hash() {
		return icon_hash;
	}

	public void setIcon_hash(String icon_hash) {
		this.icon_hash = icon_hash;
	}

	public String getBodyText() {
		Getter getter = new Getter(BurpExtender.getCallbacks().getHelpers());
		byte[] byte_body = getter.getBody(false, response);
		return new String(byte_body);
	}

	//https://javarevisited.blogspot.com/2012/01/get-set-default-character-encoding.html
	private static String getSystemCharSet() {
		return Charset.defaultCharset().toString();

		//System.out.println(System.getProperty("file.encoding"));
	}


	public String covertCharSet(byte[] response) {
		String originalCharSet = Commons.detectCharset(response);
		//BurpExtender.getStderr().println(url+"---"+originalCharSet);

		if (originalCharSet != null && !originalCharSet.equalsIgnoreCase(systemCharSet)) {
			try {
				System.out.println("正将编码从"+originalCharSet+"转换为"+systemCharSet+"[windows系统编码]");
				byte[] newResponse = new String(response,originalCharSet).getBytes(systemCharSet);
				return new String(newResponse,systemCharSet);
			} catch (Exception e) {
				e.printStackTrace(BurpExtender.getStderr());
				log.error(e);
				BurpExtender.getStderr().print("title 编码转换失败");
			}
		}
		return new String(response);
	}

	public String fetchTitle(byte[] response) {
		String bodyText = covertCharSet(response);

		Pattern p = Pattern.compile("<title(.*?)</title>");
		//<title ng-bind="service.title">The Evolution of the Producer-Consumer Problem in Java - DZone Java</title>
		Matcher m  = p.matcher(bodyText);
		while ( m.find() ) {
			title = m.group(0);
		}
		if (title.equals("")) {
			Pattern ph = Pattern.compile("<title [.*?]>(.*?)</title>");
			Matcher mh  = ph.matcher(bodyText);
			while ( mh.find() ) {
				title = mh.group(0);
			}
		}
		if (title.equals("")) {
			Pattern ph = Pattern.compile("<h[1-6]>(.*?)</h[1-6]>");
			Matcher mh  = ph.matcher(bodyText);
			while ( mh.find() ) {
				title = mh.group(0);
			}
		}
		title = title.replaceAll("<.*?>", "");

		//https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Redirections
		if (statuscode >= 300 && statuscode <= 308) {

			IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
			HelperPlus getter = new HelperPlus(helpers);
			String Locationurl = getter.getHeaderValueOf(false,response,"Location");

			if (null != Locationurl) {
				title  = " --> "+Locationurl;
			}
		}
		return title;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public byte[] getRequest() {
		return request;
	}

	public void setRequest(byte[] request) {
		this.request = request;
	}

	public byte[] getResponse() {
		return response;
	}

	public void setResponse(byte[] response) {
		this.response = response;
	}

	public String getCheckStatus() {
		return CheckStatus;
	}

	//反序列化时，如果没有这个属性是不会调用这个函数的。
	public void setCheckStatus(String checkStatus) {
		CheckStatus = checkStatus;
	}

	public String getAssetType() {
		return AssetType;
	}

	public void setAssetType(String AssetType) {
		if (Arrays.asList(AssetTypeArray).contains(AssetType)) {
			this.AssetType = AssetType;
		}
	}

	public String getEntryType() {
		return EntryType;
	}

	public void setEntryType(String entryType) {
		EntryType = entryType;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getASNInfo() {
		return ASNInfo;
	}

	public void setASNInfo(String ASNInfo) {
		this.ASNInfo = ASNInfo;
	}

	public String getFirstIP(){
		Iterator<String> it = this.fetchIPSet().iterator();
		if (it.hasNext()) {
			String ip = it.next();
			return ip;
		}
		return "";
	}
	public void freshASNInfo() {
		try {
			Iterator<String> it = this.fetchIPSet().iterator();
			if (it.hasNext()){
				String ip = it.next();
				if (IPAddressUtils.isValidIP(ip) && !IPAddressUtils.isPrivateIPv4(ip)){
					ASNEntry asn = ASNQuery.query(ip);
					if (null != asn){
						this.ASNInfo = asn.fetchASNDescription();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> getCommentList() {
		ArrayList<String> result = new ArrayList<String>();
		if (comment == null || comment.trim().equals("")){
			return result;
		}else{
			String[] comments = comment.split(",");
			for (String comment:comments) {
				if (!result.contains(comment)) {
					result.add(comment);
				}
			}
			return result;
		}
	}
	public void addComment(String commentToAdd) {
		if (commentToAdd ==null || commentToAdd.trim().equals("")) return;

		List<String> comments = getCommentList();
		if (!comments.contains(commentToAdd)) {
			comments.add(commentToAdd);
			this.setComment(String.join(",", comments));
		}
	}

	public void removeComment(String commentToRemove) {
		if (commentToRemove ==null || commentToRemove.trim().equals("")) return;

		List<String> comments = getCommentList();
		if (comments.contains(commentToRemove)) {
			comments.remove(commentToRemove);
			this.setComment(String.join(",", comments));
		}
	}

	public boolean isManualSaved() {
		return isManualSaved;
	}

	public void setManualSaved(boolean isManualSaved) {
		this.isManualSaved = isManualSaved;
	}

	public static void main(String args[]) {
		//		LineEntry x = new LineEntry();
		//		x.setRequest("xxxxxx".getBytes());
		//		//		System.out.println(yy);
		//		System.out.println(getSystemCharSet());
		//		System.out.println(System.getProperty("file.encoding"));
		//		System.out.println(Charset.defaultCharset());

		String item = "{\"bodyText\":\"<!DOCTYPE html PUBLIC \\\"-//W3C//DTD XHTML 1.0 Strict//EN\\\" \\\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\\\">\\r\\n<html xmlns=\\\"http://www.w3.org/1999/xhtml\\\">\\r\\n<head>\\r\\n<meta http-equiv=\\\"Content-Type\\\" content=\\\"text/html; charset=iso-8859-1\\\" />\\r\\n<title>IIS Windows Server</title>\\r\\n<style type=\\\"text/css\\\">\\r\\n<!--\\r\\nbody {\\r\\n\\tcolor:#000000;\\r\\n\\tbackground-color:#0072C6;\\r\\n\\tmargin:0;\\r\\n}\\r\\n\\r\\n#container {\\r\\n\\tmargin-left:auto;\\r\\n\\tmargin-right:auto;\\r\\n\\ttext-align:center;\\r\\n\\t}\\r\\n\\r\\na img {\\r\\n\\tborder:none;\\r\\n}\\r\\n\\r\\n-->\\r\\n</style>\\r\\n</head>\\r\\n<body>\\r\\n<div id=\\\"container\\\">\\r\\n<a href=\\\"http://go.microsoft.com/fwlink/?linkid=66138&amp;clcid=0x409\\\"><img src=\\\"iis-85.png\\\" alt=\\\"IIS\\\" width=\\\"960\\\" height=\\\"600\\\" /></a>\\r\\n</div>\\r\\n</body>\\r\\n</html>\",\"cDN\":\"\",\"checkStatus\":\"Checked\",\"comment\":\"\",\"contentLength\":701,\"host\":\"193.112.174.9\",\"iP\":\"193.112.174.9\",\"AssetType\":\"一般\",\"port\":80,\"protocol\":\"http\",\"request\":\"R0VUIC8gSFRUUC8xLjENCkhvc3Q6IDE5My4xMTIuMTc0LjkNCkFjY2VwdC1FbmNvZGluZzogZ3ppcCwgZGVmbGF0ZQ0KQWNjZXB0OiAqLyoNCkFjY2VwdC1MYW5ndWFnZTogZW4NClVzZXItQWdlbnQ6IE1vemlsbGEvNS4wIChXaW5kb3dzIE5UIDEwLjA7IFdpbjY0OyB4NjQpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS84MC4wLjM5ODcuMTMyIFNhZmFyaS81MzcuMzYNCkNvbm5lY3Rpb246IGNsb3NlDQoNCg==\",\"response\":\"SFRUUC8xLjEgMjAwIE9LDQpDb250ZW50LVR5cGU6IHRleHQvaHRtbA0KTGFzdC1Nb2RpZmllZDogVGh1LCAyOCBGZWIgMjAxOSAwOTozMzoyNyBHTVQNCkFjY2VwdC1SYW5nZXM6IGJ5dGVzDQpFVGFnOiAiYTg3ZGI4YTg0OGNmZDQxOjAiDQpWYXJ5OiBBY2NlcHQtRW5jb2RpbmcNClNlcnZlcjogTWljcm9zb2Z0LUlJUy84LjUNClgtUG93ZXJlZC1CeTogQVNQLk5FVA0KRGF0ZTogV2VkLCAxMyBNYXkgMjAyMCAxMDoyNDowNyBHTVQNCkNvbm5lY3Rpb246IGNsb3NlDQpDb250ZW50LUxlbmd0aDogNzAxDQoNCjwhRE9DVFlQRSBodG1sIFBVQkxJQyAiLS8vVzNDLy9EVEQgWEhUTUwgMS4wIFN0cmljdC8vRU4iICJodHRwOi8vd3d3LnczLm9yZy9UUi94aHRtbDEvRFREL3hodG1sMS1zdHJpY3QuZHRkIj4NCjxodG1sIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hodG1sIj4NCjxoZWFkPg0KPG1ldGEgaHR0cC1lcXVpdj0iQ29udGVudC1UeXBlIiBjb250ZW50PSJ0ZXh0L2h0bWw7IGNoYXJzZXQ9aXNvLTg4NTktMSIgLz4NCjx0aXRsZT5JSVMgV2luZG93cyBTZXJ2ZXI8L3RpdGxlPg0KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4NCjwhLS0NCmJvZHkgew0KCWNvbG9yOiMwMDAwMDA7DQoJYmFja2dyb3VuZC1jb2xvcjojMDA3MkM2Ow0KCW1hcmdpbjowOw0KfQ0KDQojY29udGFpbmVyIHsNCgltYXJnaW4tbGVmdDphdXRvOw0KCW1hcmdpbi1yaWdodDphdXRvOw0KCXRleHQtYWxpZ246Y2VudGVyOw0KCX0NCg0KYSBpbWcgew0KCWJvcmRlcjpub25lOw0KfQ0KDQotLT4NCjwvc3R5bGU+DQo8L2hlYWQ+DQo8Ym9keT4NCjxkaXYgaWQ9ImNvbnRhaW5lciI+DQo8YSBocmVmPSJodHRwOi8vZ28ubWljcm9zb2Z0LmNvbS9md2xpbmsvP2xpbmtpZD02NjEzOCZhbXA7Y2xjaWQ9MHg0MDkiPjxpbWcgc3JjPSJpaXMtODUucG5nIiBhbHQ9IklJUyIgd2lkdGg9Ijk2MCIgaGVpZ2h0PSI2MDAiIC8+PC9hPg0KPC9kaXY+DQo8L2JvZHk+DQo8L2h0bWw+\",\"statuscode\":200,\"time\":\"2020-05-22-11-07-45\",\"title\":\"<title>IIS Windows Server</title>\",\"url\":\"http://193.112.174.9:80/\",\"webcontainer\":\"Microsoft-IIS/8.5\"}";
		LineEntry entry = LineEntry.FromJson(item);
		System.out.println(entry.getCheckStatus());
		System.out.println(entry.getAssetType());
		System.out.println(entry.getTime());
		String key = HashCode.fromBytes(entry.getRequest()).toString();
		System.out.println(key);
	}
}
