package title;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.bit4woo.utilbox.burp.HelperPlus;
import com.bit4woo.utilbox.utils.CharsetUtils;
import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.UrlUtils;
import com.google.common.hash.HashCode;

import ASN.ASNEntry;
import ASN.ASNQuery;
import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IResponseInfo;
import domain.CertInfo;
import utils.NetworkUtils;

public class LineEntry {
	private static final Logger log = LogManager.getLogger(LineEntry.class);

	// 资产重要性
	public static final String AssetType_A = "重要目标";// 像管理后台、统一登录等等一旦有漏洞就危害很高的系统。
	public static final String AssetType_B = "无价值无需再挖";// 像官网、首页等对信息收集、目标界定有用；但是对“挖漏洞”来说没有价值的记录。
	public static final String AssetType_C = "未分类";// 默认值，还未进行区分的资产或者普通价值资产
	public static final String AssetType_D = "非目标资产";// 非目标资产，通常跑网段都会出现这类资产。

	public static final String[] AssetTypeArray = { LineEntry.AssetType_A, LineEntry.AssetType_B, LineEntry.AssetType_C,
			LineEntry.AssetType_D };

	// 对当前资产的检测进度
	public static final String CheckStatus_UnChecked = "UnChecked";
	public static final String CheckStatus_Checked = "Done";
	public static final String CheckStatus_Checking = "Checking";
	public static final String CheckStatus_MoreAction = "MoreAction";

	public static final String[] CheckStatusArray = { LineEntry.CheckStatus_UnChecked, LineEntry.CheckStatus_Checking,
			LineEntry.CheckStatus_Checked, LineEntry.CheckStatus_MoreAction };

	// 资产归属判断依据
	public static final String Tag_NotTargetBaseOnCertInfo = "CertNotMatch";
	public static final String Tag_NotTargetBaseOnBlackList = "IPIsBlack";
	public static final String Tag_NotTargetByUser = "NotTarget";

	public static final String EntryType_Web = "Web";
	public static final String EntryType_DNS = "DNS";

	// 记录的来源
	public static final String Source_Certain = "certain"; // 确定属于目标范围的:子域名\指定的网段\确定属于网段的IP地址\根据证书信息确定属于目标的IP。
	public static final String Source_Custom_Input = "custom"; // 来自用户自定义输入
	public static final String Source_Subnet_Extend = "extend"; // 来自网段扩展汇算
	public static final String Source_Manual_Saved = "saved"; // 来自用户手动保存

	public static String systemCharSet = getSystemCharSet();

	/**
	 * 从burp的角度，一个数据包由三部分组成：request、response、httpService
	 * 而httpService又可以分为：protocol、host、port
	 */
	private int port = -1;
	private String host = "";
	private String protocol = "";
	// these three == IHttpService, helpers.buildHttpService to build.

	private byte[] request = {};
	private byte[] response = {};
	// request+response+httpService == IHttpRequestResponse,burp的划分方式

	/**
	 * 如下是显示界面使用到的字段，部分来自数据包本身，部分需要额外的请求
	 *
	 */
	private String url = "";
	private int statuscode = -1;
	private int contentLength = -1;
	private String title = "";
	private String webcontainer = "";
	// 如上几个字段都是来自数据包本身

	private Set<String> IPSet = new HashSet<String>();
	private Set<String> CNAMESet = new HashSet<String>();
	private Set<String> CertDomainSet = new HashSet<String>();
	private String icon_url = "";
	private byte[] icon_bytes = new byte[0];
	private String icon_hash = "";
	//关于icon_hash，fofa、zoomeye使用了同一种算法，hunter、quake使用了同一种算法（都是md5）
	private String ASNInfo = "";
	private int AsnNum =-1;
	private String time = "";
	// 如上几个字段需要网络请求或查询

	// Gson中，加了transient表示不序列化，是最简单的方法
	// 给不想被序列化的属性增加transient属性---java特性
	// private transient String messageText = "";//use to search
	// private transient String bodyText = "";//use to adjust the response changed
	// or not
	// don't store these two field to reduce config file size.

	// field for user用户标记的字段，表明状态、类型、来源、备注、tag等等
	private String CheckStatus = CheckStatus_UnChecked;
	private String AssetType = AssetType_C;
	private String EntryType = EntryType_Web;
	private String EntrySource = "";
	private Set<String> comments = new HashSet<String>();
	private Set<String> EntryTags = new HashSet<String>();

	// remove IHttpRequestResponse field ,replace with
	// request+response+httpService(host port protocol). for convert to json.

	/**
	 * 默认构造函数，序列化、反序列化所需
	 */
	LineEntry() {

	}

	/**
	 * 这个构造函数用于发送请求前，LineEntry的构建。 传递一个URL,请求URL，解析数据包。
	 * 
	 * @param url
	 */
	public LineEntry(URL url) {
		parseURL(url);
	}

	/**
	 * 用于构造DNS记录
	 * 
	 * @param host
	 * @param IPset
	 * @return
	 */
	public LineEntry(String host, Set<String> IPset) {
		this.host = host;
		this.port = 80;
		this.protocol = "http";
		this.IPSet = IPset;
		this.EntryType = EntryType_DNS;
		this.CheckStatus = CheckStatus_Checked;
	}

	public LineEntry(IHttpRequestResponse messageinfo) {
		parse(messageinfo);
	}

	/**
	 * 用于从数据库中恢复对象
	 */
	public LineEntry(URL url, byte[] request, byte[] response) {
		parse(url, request, response);
	}

	public LineEntry(IHttpRequestResponse messageinfo, String CheckStatus, String comment) {
		parse(messageinfo);

		this.CheckStatus = CheckStatus;
		addComment(comment);
	}

	public String ToJson() {// 注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.toJSONString(this);
	}

	public static LineEntry FromJson(String json) {// 注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.parseObject(json, LineEntry.class);
	}

	private void parse(IHttpRequestResponse messageinfo) {
		if (messageinfo == null)
			return;
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		URL tmpurl = helpers.analyzeRequest(messageinfo).getUrl();// 包含了默认端口
		parse(tmpurl, messageinfo.getRequest(), messageinfo.getResponse());
	}

	/**
	 * 可以用于从数据库中恢复对象
	 * 
	 * @param url
	 * @param request
	 * @param response
	 */
	private void parse(URL url, byte[] request, byte[] response) {
		try {
			parseURL(url);

			if (request != null)
				this.request = request;

			if (response != null) {
				this.response = response;

				IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
				IResponseInfo responseInfo = helpers.analyzeResponse(response);
				statuscode = responseInfo.getStatusCode();

				HelperPlus getter = BurpExtender.getHelperPlus();
				String tmpServer = getter.getHeaderValueOf(false, response, "Server");
				if (tmpServer != null) {
					webcontainer = tmpServer;
				}

				byte[] byteBody = HelperPlus.getBody(false, response);
				try {
					contentLength = Integer.parseInt(getter.getHeaderValueOf(false, response, "Content-Length").trim());
				} catch (Exception e) {
					if (contentLength == -1 && byteBody != null) {
						contentLength = byteBody.length;
					}
				}

				title = fetchTitle(response);
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			log.error(e);
		}
	}

	private void parseURL(URL url) {
		this.url = UrlUtils.getFullUrlWithDefaultPort(url.toString());// 统一格式，包含默认端口
		port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
		host = url.getHost();
		protocol = url.getProtocol();
	}

	public void DoDirBrute() {

	}

	public void DoGetIconHash() {
		if (EntryType.equals(EntryType_Web)) {
			handleFavicon();
		}
	}

	/**
	 * 第一次根据一个URL去获取它的详细信息
	 */
	public LineEntry firstRequest(GetTitleTempConfig config) {
		// 第一步：DNS解析
		HashMap<String, Set<String>> dnsResult = DomainUtils.dnsQuery(host, null);
		this.IPSet = dnsResult.get("IP");
		this.CNAMESet = dnsResult.get("CDN");
		if (IPSet == null || IPSet.size() <= 0) {
			// TODO 是否应该移除无效域名？理清楚：无效域名，黑名单域名，无响应域名等情况。
			// 依然添加一条记录，以便人工判断，人工可以根据此记录来清理收集到的域名列表。
			setTitle("No DNS Record");
			return this;
		} else {// 默认过滤私有IP
			boolean isInPrivateNetwork = config.isHandlePriavte();
			String ip = new ArrayList<>(IPSet).get(0);
			if (IPAddressUtils.isPrivateIPv4NoPort(ip) && !isInPrivateNetwork) {// 外网模式，内网域名，仅仅显示域名和IP。
				setTitle("Private IP");
				return this;
			}
			freshASNInfo();
		}

		// 第二步：http请求
		String cookie = config.getCookie();
		IHttpRequestResponse info = NetworkUtils.doRequest(url, cookie);
		parse(info);
		// https://superuser.com/questions/1054724/how-to-make-firefox-ignore-all-ssl-certification-errors
		// 仍然改为先请求http，http不可用再请求https.避免浏览器中证书问题重复点击很麻烦
		// 即使是单纯跳转HTTPS的http请求，也有其独特之处，比如之前的Nginx Vhost traffic
		// monitor，默认只能在http中成功，https下就不行。

		// 第三步：计算icon hash
		if (getStatuscode() >= 0) {
			// 没有响应包的，不做这个
			handleFavicon();

			// 第四步：获取证书域名
			if (this.url.toLowerCase().startsWith("https://")) {
				this.CertDomainSet = new CertInfo().getAlternativeDomains(url);
			}
		} else {
			// 当域名可以解析，但是所有URL请求都失败的情况下。添加一条DNS解析记录
			// TODO 但是IP可以ping通但是无成功的web请求的情况还没有处理
			if (DomainUtils.isValidDomainNoPort(host) && !IPSet.isEmpty()) {
				setTitle("DNS Record");
			}
		}
		return this;
	}

	public void DoRequestAgain() throws MalformedURLException {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		IHttpService service = helpers.buildHttpService(host, port, protocol);

		if (EntryType.equals(EntryType_DNS)) {
			request = helpers.buildHttpRequest(new URL(service.toString()));
		}

		IHttpRequestResponse info = BurpExtender.getCallbacks().makeHttpRequest(service, request);
		// BurpExtender.getStdout().println(new String(info.getResponse()));
		if (info != null) {
			parse(info);
		}
	}

	public void DoRequestCertInfoAgain() throws MalformedURLException {
		try {
			String url = this.getUrl();
			if (url.toLowerCase().startsWith("https")) {
				CertDomainSet = new CertInfo().getAlternativeDomains(url);
			}

			if (!IPAddressUtils.isValidIPv4NoPort(host)) {// 目标是域名
				HashMap<String, Set<String>> result = DomainUtils.dnsQuery(host, null);
				CNAMESet = result.get("CDN");
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	/**
	 * 获取到的URL是包含了默认端口的，因为burp方法的原因
	 * 
	 * @return
	 */
	public String getUrl() {// 为了格式统一，和查找匹配更精确，都包含了默认端口
		if (StringUtils.isEmpty(url)) {
			return protocol + "://" + host + ":" + port + "/";
		}
		return UrlUtils.getFullUrlWithDefaultPort(url);
	}

	/**
	 * 返回通常意义上的URL格式，不包含默认端口。用于搜索
	 * 不要修改原始url的格式！即都包含默认端口。因为数据库中更新对应记录是以URL为依据的，否则不能成功更新记录。
	 * 
	 * @return
	 */
	public String fetchUrlWithCommonFormate() {
		if (StringUtils.isEmpty(url)) {
			url = protocol + "://" + host + ":" + port + "/";
		}
		// 不要修改原始url的格式！即都包含默认端口。因为数据库中更新对应记录是以URL为依据的，否则不能成功更新记录。
		String usualUrl = HelperPlus.removeUrlDefaultPort(url);
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

	public Set<String> getIPSet() {
		return IPSet;
	}

	public void setIPSet(Set<String> iPSet) {
		IPSet = iPSet;
	}

	// 用于序列化
	public Set<String> getCNAMESet() {
		return CNAMESet;
	}

	// 用于序列化
	public void setCNAMESet(Set<String> cNAMESet) {
		CNAMESet = cNAMESet;
	}

	public boolean isCDN() {
		if (webcontainer.contains("cloudflare")) {
			return true;
		}
		if (CNAMESet.size() > 1) {
			return true;
		}
		if (IPSet.size() > 1) {
			return true;
		}
		return false;
	}

	public Set<String> getCertDomainSet() {
		return CertDomainSet;
	}

	public void setCertDomainSet(Set<String> certDomainSet) {
		CertDomainSet = certDomainSet;
	}

	public String fetchCNAMEAndCertInfo() {
		String CNames = String.join(",", getCNAMESet());
		String CertDomains = String.join(",", getCertDomainSet());
		Set<String> tmp = new HashSet<>();
		if (!StringUtils.isEmpty(CNames)) {
			tmp.add(CNames);
		}
		if (!StringUtils.isEmpty(CertDomains)) {
			tmp.add(CertDomains);
		}
		return String.join("|", tmp);
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

	public String getIcon_url() {
		return icon_url;
	}

	public void setIcon_url(String icon_url) {
		this.icon_url = icon_url;
	}

	public byte[] getIcon_bytes() {
		return icon_bytes;
	}

	public void setIcon_bytes(byte[] icon_bytes) {
		this.icon_bytes = icon_bytes;
	}

	public String getIcon_hash() {
		return icon_hash;
	}

	public void setIcon_hash(String icon_hash) {
		this.icon_hash = icon_hash;
	}

	public String getBodyText() {
		byte[] byte_body = HelperPlus.getBody(false, response);
		return new String(byte_body);
	}

	// https://javarevisited.blogspot.com/2012/01/get-set-default-character-encoding.html
	private static String getSystemCharSet() {
		return Charset.defaultCharset().toString();

		// System.out.println(System.getProperty("file.encoding"));
	}

	public static String covertCharSet(byte[] response) {
		String originalCharSet = CharsetUtils.detectCharset(response);
		// BurpExtender.getStderr().println(url+"---"+originalCharSet);

		if (originalCharSet != null && !originalCharSet.equalsIgnoreCase(systemCharSet)) {
			try {
				//System.out.println("正将编码从" + originalCharSet + "转换为" + systemCharSet + "[windows系统编码]");
				byte[] newResponse = new String(response, originalCharSet).getBytes(systemCharSet);
				return new String(newResponse, systemCharSet);
			} catch (UnsupportedEncodingException e) {
				// DO Nothing
			} catch (Exception e) {
				e.printStackTrace(BurpExtender.getStderr());
				log.error(e);
				//BurpExtender.getStderr().print("title 编码转换失败");
			}
		}
		return new String(response);
	}

	/**
	 * 
	 * @param response
	 * @return
	 */
	public String fetchTitle(byte[] response) {
		if (response == null)
			return "";

		// https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Redirections
		if (statuscode >= 300 && statuscode <= 308) {
			IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
			HelperPlus getter = BurpExtender.getHelperPlus();
			String Locationurl = getter.getHeaderValueOf(false, response, "Location");
			if (null != Locationurl) {
				title = " --> " + Locationurl;
				return title;
			}
		}

		String bodyText = covertCharSet(response);

		return grepTitle(bodyText);
	}

	/**
	 * 
	 */
	public void handleFavicon() {
		if (response == null || statuscode != 200) {
			icon_url = WebIcon.getFaviconUrl(url, null);
		} else {
			String bodyText = covertCharSet(response);
			icon_url = WebIcon.getFaviconUrl(url, bodyText);
		}
		if (statuscode>0 && statuscode <500){
			//只有 web有正常的响应时才考虑请求favicon
			icon_bytes = WebIcon.getFavicon(icon_url);
			icon_hash = WebIcon.getHash(icon_bytes);
			icon_bytes = WebIcon.convertIcoToPng(icon_bytes);//存储的是PNG格式的数据，不是原始格式了
		}
	}

	public String getHeaderValueOf(boolean isRequest, String headerName) {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		HelperPlus getter = BurpExtender.getHelperPlus();
		if (isRequest) {
			return getter.getHeaderValueOf(false, request, headerName);
		} else {
			return getter.getHeaderValueOf(false, response, headerName);
		}
	}

	/**
	 * 从响应包中提取title <title>Kênh Quản Lý Shop - Phần Mềm Quản Lý Bán Hàng Miễn
	 * Phí</title> <title ng-bind="service.title">The Evolution of the
	 * Producer-Consumer Problem in Java - DZone Java</title>
	 * 
	 * 正则要求： 1、title名称不能区分大小写 TITLE
	 * 
	 * @param bodyText
	 * @return
	 */
	private static String grepTitle(String bodyText) {
		String title = "";

		if (!isHtml(bodyText)){
			return title;
		}

		String regex = "<title(.*?)>(.*?)</title>";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(bodyText);
		while (m.find()) {
			title = m.group(2);// 注意
			if (!StringUtils.isEmpty(title)) {
				return title;
			}
		}

		String regex1 = "<h[1-6](.*?)>(.*?)</h[1-6]>";
		Pattern ph = Pattern.compile(regex1, Pattern.CASE_INSENSITIVE);
		Matcher mh = ph.matcher(bodyText);
		while (mh.find()) {
			title = mh.group(2);
			if (!StringUtils.isEmpty(title)) {
				return title;
			}
		}
		return title;
	}

	private static boolean isHtml(String bodyText){
		if (StringUtils.isEmpty(bodyText)){
			return false;
		}

		return bodyText.contains("<!DOCTYPE html>") || bodyText.contains("<html>");
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

	// 反序列化时，如果没有这个属性是不会调用这个函数的。
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

	public String getEntrySource() {
		return EntrySource;
	}

	public void setEntrySource(String entrySource) {
		EntrySource = entrySource;
	}

	public Set<String> getComments() {
		return comments;
	}

	public void setComments(Set<String> comments) {
		this.comments = comments;
	}

	public Set<String> getEntryTags() {
		return EntryTags;
	}

	public void setEntryTags(Set<String> entryTags) {
		EntryTags = entryTags;
	}

	public String getASNInfo() {
		return ASNInfo;
	}

	public void setASNInfo(String ASNInfo) {
		this.ASNInfo = ASNInfo;
	}

	public int getAsnNum() {
		return AsnNum;
	}

	public void setAsnNum(int asnNum) {
		AsnNum = asnNum;
	}

	public String getFirstIP() {
		Iterator<String> it = this.IPSet.iterator();
		if (it.hasNext()) {
			String ip = it.next();
			return ip;
		}
		return "";
	}

	@Deprecated
	public void freshASNInfo() {
		try {
			Iterator<String> it = this.IPSet.iterator();
			if (it.hasNext()) {
				String ip = it.next();
				if (IPAddressUtils.isValidIPv4NoPort(ip) && !IPAddressUtils.isPrivateIPv4NoPort(ip)) {
					ASNEntry asn = ASNQuery.getInstance().query(ip);
					if (null != asn) {
						this.ASNInfo = asn.fetchASNDescription();
						this.AsnNum = Integer.parseInt(asn.getAsn());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addComment(String commentToAdd) {
		if (StringUtils.isEmpty(commentToAdd))
			return;
		// commentToAdd本身可能就是以逗号分隔的
		comments.addAll(Arrays.asList(commentToAdd.split(",")));
	}

	public void removeComment(String commentToRemove) {
		if (StringUtils.isEmpty(commentToRemove))
			return;

		comments.remove(commentToRemove);
	}

	public static void testGrepTitle() {
		String aa = " <title>Kênh Quản Lý Shop - Phần Mềm Quản Lý Bán Hàng Miễn Phí</title>";
		String bb = "<title ng-bind=\"service.title\">The Evolution of the Producer-Consumer Problem in Java - DZone Java</title>";
		String cc = " <TITLE>Kênh Quản Lý Shop - Phần Mềm Quản Lý Bán Hàng Miễn Phí</title>";
		String dd = " <h1aaa>h1</h1>";

		System.out.println(grepTitle(dd));
	}

	public static void test() {
		// LineEntry x = new LineEntry();
		// x.setRequest("xxxxxx".getBytes());
		// // System.out.println(yy);
		// System.out.println(getSystemCharSet());
		// System.out.println(System.getProperty("file.encoding"));
		// System.out.println(Charset.defaultCharset());

		String item = "{\"bodyText\":\"<!DOCTYPE html PUBLIC \\\"-//W3C//DTD XHTML 1.0 Strict//EN\\\" \\\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\\\">\\r\\n<html xmlns=\\\"http://www.w3.org/1999/xhtml\\\">\\r\\n<head>\\r\\n<meta http-equiv=\\\"Content-Type\\\" content=\\\"text/html; charset=iso-8859-1\\\" />\\r\\n<title>IIS Windows Server</title>\\r\\n<style type=\\\"text/css\\\">\\r\\n<!--\\r\\nbody {\\r\\n\\tcolor:#000000;\\r\\n\\tbackground-color:#0072C6;\\r\\n\\tmargin:0;\\r\\n}\\r\\n\\r\\n#container {\\r\\n\\tmargin-left:auto;\\r\\n\\tmargin-right:auto;\\r\\n\\ttext-align:center;\\r\\n\\t}\\r\\n\\r\\na img {\\r\\n\\tborder:none;\\r\\n}\\r\\n\\r\\n-->\\r\\n</style>\\r\\n</head>\\r\\n<body>\\r\\n<div id=\\\"container\\\">\\r\\n<a href=\\\"http://go.microsoft.com/fwlink/?linkid=66138&amp;clcid=0x409\\\"><img src=\\\"iis-85.png\\\" alt=\\\"IIS\\\" width=\\\"960\\\" height=\\\"600\\\" /></a>\\r\\n</div>\\r\\n</body>\\r\\n</html>\",\"cDN\":\"\",\"checkStatus\":\"Checked\",\"comment\":\"\",\"contentLength\":701,\"host\":\"193.112.174.9\",\"iP\":\"193.112.174.9\",\"AssetType\":\"一般\",\"port\":80,\"protocol\":\"http\",\"request\":\"R0VUIC8gSFRUUC8xLjENCkhvc3Q6IDE5My4xMTIuMTc0LjkNCkFjY2VwdC1FbmNvZGluZzogZ3ppcCwgZGVmbGF0ZQ0KQWNjZXB0OiAqLyoNCkFjY2VwdC1MYW5ndWFnZTogZW4NClVzZXItQWdlbnQ6IE1vemlsbGEvNS4wIChXaW5kb3dzIE5UIDEwLjA7IFdpbjY0OyB4NjQpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS84MC4wLjM5ODcuMTMyIFNhZmFyaS81MzcuMzYNCkNvbm5lY3Rpb246IGNsb3NlDQoNCg==\",\"response\":\"SFRUUC8xLjEgMjAwIE9LDQpDb250ZW50LVR5cGU6IHRleHQvaHRtbA0KTGFzdC1Nb2RpZmllZDogVGh1LCAyOCBGZWIgMjAxOSAwOTozMzoyNyBHTVQNCkFjY2VwdC1SYW5nZXM6IGJ5dGVzDQpFVGFnOiAiYTg3ZGI4YTg0OGNmZDQxOjAiDQpWYXJ5OiBBY2NlcHQtRW5jb2RpbmcNClNlcnZlcjogTWljcm9zb2Z0LUlJUy84LjUNClgtUG93ZXJlZC1CeTogQVNQLk5FVA0KRGF0ZTogV2VkLCAxMyBNYXkgMjAyMCAxMDoyNDowNyBHTVQNCkNvbm5lY3Rpb246IGNsb3NlDQpDb250ZW50LUxlbmd0aDogNzAxDQoNCjwhRE9DVFlQRSBodG1sIFBVQkxJQyAiLS8vVzNDLy9EVEQgWEhUTUwgMS4wIFN0cmljdC8vRU4iICJodHRwOi8vd3d3LnczLm9yZy9UUi94aHRtbDEvRFREL3hodG1sMS1zdHJpY3QuZHRkIj4NCjxodG1sIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hodG1sIj4NCjxoZWFkPg0KPG1ldGEgaHR0cC1lcXVpdj0iQ29udGVudC1UeXBlIiBjb250ZW50PSJ0ZXh0L2h0bWw7IGNoYXJzZXQ9aXNvLTg4NTktMSIgLz4NCjx0aXRsZT5JSVMgV2luZG93cyBTZXJ2ZXI8L3RpdGxlPg0KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4NCjwhLS0NCmJvZHkgew0KCWNvbG9yOiMwMDAwMDA7DQoJYmFja2dyb3VuZC1jb2xvcjojMDA3MkM2Ow0KCW1hcmdpbjowOw0KfQ0KDQojY29udGFpbmVyIHsNCgltYXJnaW4tbGVmdDphdXRvOw0KCW1hcmdpbi1yaWdodDphdXRvOw0KCXRleHQtYWxpZ246Y2VudGVyOw0KCX0NCg0KYSBpbWcgew0KCWJvcmRlcjpub25lOw0KfQ0KDQotLT4NCjwvc3R5bGU+DQo8L2hlYWQ+DQo8Ym9keT4NCjxkaXYgaWQ9ImNvbnRhaW5lciI+DQo8YSBocmVmPSJodHRwOi8vZ28ubWljcm9zb2Z0LmNvbS9md2xpbmsvP2xpbmtpZD02NjEzOCZhbXA7Y2xjaWQ9MHg0MDkiPjxpbWcgc3JjPSJpaXMtODUucG5nIiBhbHQ9IklJUyIgd2lkdGg9Ijk2MCIgaGVpZ2h0PSI2MDAiIC8+PC9hPg0KPC9kaXY+DQo8L2JvZHk+DQo8L2h0bWw+\",\"statuscode\":200,\"time\":\"2020-05-22-11-07-45\",\"title\":\"<title>IIS Windows Server</title>\",\"url\":\"http://193.112.174.9:80/\",\"webcontainer\":\"Microsoft-IIS/8.5\"}";
		LineEntry entry = LineEntry.FromJson(item);
		System.out.println(entry.getCheckStatus());
		System.out.println(entry.getAssetType());
		System.out.println(entry.getTime());
		String key = HashCode.fromBytes(entry.getRequest()).toString();
		System.out.println(key);
	}

	public static void main(String args[]) {
		testGrepTitle();
	}
}
