package domain;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import Tools.PatternsFromAndroid;
import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IPAddressUtils;
import config.ConfigPanel;
import title.LineEntry;
import toElastic.ElasticClient;

public class DomainProducer extends Thread {//Producer do
	private final BlockingQueue<IHttpRequestResponse> inputQueue;//use to store messageInfo


	private int threadNo;
	private volatile boolean stopflag = false;
	private volatile boolean currentSaved = false;//每分钟只保存一次的标志位

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public DomainProducer(BlockingQueue<IHttpRequestResponse> inputQueue,
						  int threadNo) {
		this.threadNo = threadNo;
		this.inputQueue = inputQueue;
		this.setName(this.getClass().getName()+threadNo);//方便调试
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (threadNo == 9999){//9999是流量进程，除非关闭，否则一直不退出。
					if (DomainPanel.getDomainResult() == null) {//当未加载项目时，暂时不处理
						stdout.println("No project loaded,traffic anlaysis thread will do nothing!");
						Thread.sleep(1*60*1000);
						continue;
					}

					//每两分钟保存一次
					if (Commons.getNowMinute()%2==0 ){
						if (!currentSaved && DomainPanel.getDomainResult().isChanged()){
							currentSaved = true;
							DomainPanel.saveDomainDataToDB();
						}
					}else {
						currentSaved = false;
					}

				}else {
					if (inputQueue.isEmpty()) {
						stdout.println(this.getName()+" break due to input queue empty!");
						break;
					}
					if (stopflag) {
						stdout.println(this.getName()+" break due to stop flag changed to true");
					}
				}

				IHttpRequestResponse messageinfo = inputQueue.take();

				IHttpService httpservice = messageinfo.getHttpService();
				String urlString = helpers.analyzeRequest(messageinfo).getUrl().toString();

				String shortURL = httpservice.toString();
				String protocol =  httpservice.getProtocol();
				String Host = httpservice.getHost();
				int port = httpservice.getPort();
				if (port !=80 && port!=443) {
					Host = Host+port;
				}

				//第一阶段：处理Host
				//当Host是一个IP地址时，它也有可能是我们的目标。如果它的证书域名又在目标中，那么它就是目标。
				int type = DomainPanel.fetchTargetModel().assetType(Host);

				if (type ==DomainManager.USELESS){
					continue;
				}else if (type == DomainManager.NEED_CONFIRM_IP){
					//当Host是一个IP，也有可能是目标，通过证书信息进一步判断。
					if (protocol.equalsIgnoreCase("https") && messageinfo.getResponse()!=null && !DomainPanel.getDomainResult().getIPSetOfCert().contains(Host)){
						if (isTargetByCertInfoForTarget(shortURL)){

							//确定这个IP是目标了，更新target
							//TargetEntry entry = new TargetEntry(Host);
							//entry.setComment("BaseOnCertInfo");
							//DomainPanel.fetchTargetModel().addRowIfValid(entry);

							//重新判断类型，应该是确定的IP类型了。
							//type = DomainPanel.fetchTargetModel().domainType(Host);
							DomainPanel.getDomainResult().getIPSetOfCert().add(Host);
						}
					}
				}else {
					DomainPanel.getDomainResult().addIfValid(Host);
				}

				//第二步：处理HTTPS证书
				if (type !=DomainManager.USELESS && protocol.equalsIgnoreCase("https")){//get related domains
					if (BurpExtender.httpsChecked.add(shortURL)) {//httpService checked or not
						//如果set中已存在，返回false，如果不存在，返回true。
						//必须先添加，否则执行在执行https链接的过程中，已经有很多请求通过检测进行相同的请求了。
						Set<String> tmpDomains = CertInfo.getSANsbyKeyword(shortURL,DomainPanel.fetchTargetModel().fetchKeywordSet());
						for (String domain:tmpDomains) {
							BurpExtender.getStdout().println("Target Related Asset Found :"+domain);
							if (DomainPanel.getDomainResult().isAutoAddRelatedToRoot()){
								DomainPanel.getDomainResult().addToTargetAndSubDomain(domain, true);
							}else{
								DomainPanel.getDomainResult().getRelatedDomainSet().add(domain);
							}
						}
					}
				}

				//第三步：对所有流量都进行抓取，这样可以发现更多域名，但同时也会有很多无用功，尤其是使用者同时挖掘多个目标的时候
				if (!Commons.uselessExtension(urlString)) {//grep domains from response and classify
					byte[] response = messageinfo.getResponse();

					if (response != null) {
						if (response.length >= 100000000) {//避免大数据包卡死整个程序
							response = subByte(response,0,100000000);
						}
						Set<String> domains = DomainProducer.grepDomain(new String(response));
						//List<String> IPs = DomainProducer.grepIPAndPort(new String(response));
						Set<String> emails = DomainProducer.grepEmail(new String(response));

						DomainPanel.getDomainResult().addIfValid(domains);
						//DomainPanel.getDomainResult().addIfValid(new HashSet<>(IPs));
						DomainPanel.getDomainResult().addIfValidEmail(emails);
					}
				}

				if (ConfigPanel.rdbtnSaveTrafficTo.isSelected()) {
					if (type != DomainManager.USELESS && !Commons.uselessExtension(urlString)) {//grep domains from response and classify
						if (threadNo == 9999) {
							try {//写入elastic的逻辑，只对目标资产生效
								LineEntry entry = new LineEntry(messageinfo);
								ElasticClient.writeData(entry);
							}catch(Exception e1) {
								e1.printStackTrace(BurpExtender.getStderr());
								e1.getMessage();
							}
						}
					}
				}
			} catch (InterruptedException error) {
				BurpExtender.getStdout().println(this.getName() +" exits due to Interrupt signal received");
			}catch (Exception error) {
				error.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	/**
	 * 这个函数必须返回确定的目标！不能确定的认为是false
	 * @param shortURL
	 * @return
	 */
	public boolean isTargetByCertInfoForTarget(String shortURL) throws Exception {
		Set<String> certDomains = CertInfo.getAllSANs(shortURL);
		for (String domain : certDomains) {
			int type = DomainPanel.fetchTargetModel().assetType(domain);
			if (type == DomainManager.SUB_DOMAIN || type == DomainManager.TLD_DOMAIN) {
				return true;
			}
		}
		return false;
	}

	public byte[] subByte(byte[] b,int srcPos,int length){
		byte[] b1 = new byte[length];
		System.arraycopy(b, srcPos, b1, 0, length);
		return b1;
	}

	/**
	 * 先解Unicode，再解url，应该才是正确操作吧
	 * @param line
	 * @return
	 */
	public static String decodeAll(String line) {
		line = line.trim();

		/*
		if (false) {// &#x URF-8编码的特征，对于域名的提取不需要对它进行处理
			while (true) {
				try {
					int oldlen = line.length();
					line = StringEscapeUtils.unescapeHtml4(line);
					int currentlen = line.length();
					if (oldlen > currentlen) {
						continue;
					}else {
						break;
					}
				}catch(Exception e) {
					//e.printStackTrace(BurpExtender.getStderr());
					break;//即使出错，也要进行后续的查找
				}
			}
		}
		 */

		if (needUnicodeConvert(line)) {
			while (true) {//unicode解码
				try {
					int oldlen = line.length();
					line = StringEscapeUtils.unescapeJava(line);
					int currentlen = line.length();
					if (oldlen > currentlen) {
						continue;
					}else {
						break;
					}
				}catch(Exception e) {
					//e.printStackTrace(BurpExtender.getStderr());
					break;//即使出错，也要进行后续的查找
				}
			}
		}

		if (needURLConvert(line)) {
			while (true) {
				try {
					int oldlen = line.length();
					line = URLDecoder.decode(line);
					int currentlen = line.length();
					if (oldlen > currentlen) {
						continue;
					}else {
						break;
					}
				}catch(Exception e) {
					//e.printStackTrace(BurpExtender.getStderr());
					break;//即使出错，也要进行后续的查找
				}
			}
		}

		return line;
	}

	public static Set<String> grepDomain(String httpResponse) {
		httpResponse = httpResponse.toLowerCase();
		//httpResponse = cleanResponse(httpResponse);
		Set<String> domains = new HashSet<>();
		//"^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$"
		final String DOMAIN_NAME_PATTERN = "((?!-)[A-Za-z0-9-*]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}(?::\\d{1,5})?";
		//加上(?::\\d{1,5})?部分，支持端口模式
		//加*号是为了匹配 类似 *.baidu.com的这种域名记录。

		List<String> lines = Commons.textToLines(httpResponse);

		for (String line:lines) {//分行进行提取，似乎可以提高成功率？
			line = decodeAll(line);
			Pattern pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
			Matcher matcher = pDomainNameOnly.matcher(line);
			while (matcher.find()) {//多次查找
				String tmpDomain = matcher.group();
				if (tmpDomain.startsWith("*.")) {
					tmpDomain = tmpDomain.replaceFirst("\\*\\.","");//第一个参数是正则
				}
				if (tmpDomain.toLowerCase().startsWith("252f")) {//url中的//的URL编码，上面的解码逻辑可能出错
					tmpDomain = tmpDomain.replaceFirst("252f","");
				}
				if (tmpDomain.toLowerCase().startsWith("2f")) {
					tmpDomain = tmpDomain.replaceFirst("2f","");
				}
				domains.add(tmpDomain);
			}
		}
		return domains;
	}

	//https://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java
	//https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
	public static List<String> grepURL(String httpResponse) {
		httpResponse = httpResponse.toLowerCase();
		Set<String> URLs = new HashSet<>();

		String[] lines = httpResponse.split("\r\n");

		//https://github.com/GerbenJavado/LinkFinder/blob/master/linkfinder.py
		String regex_str = "(?:\"|')"
				+ "("
				+ "((?:[a-zA-Z]{1,10}://|//)[^\"'/]{1,}\\.[a-zA-Z]{2,}[^\"']{0,})"
				+ "|"
				+ "((?:/|\\.\\./|\\./)[^\"'><,;| *()(%%$^/\\\\\\[\\]][^\"'><,;|()]{1,})"
				+ "|"
				+ "([a-zA-Z0-9_\\-/]{1,}/[a-zA-Z0-9_\\-/]{1,}\\.(?:[a-zA-Z]{1,4}|action)(?:[\\?|/][^\"|']{0,}|))"
				+ "|"
				+ "([a-zA-Z0-9_\\-]{1,}\\.(?:php|asp|aspx|jsp|json|action|html|js|txt|xml)(?:\\?[^\"|']{0,}|))"
				+ ")"
				+ "(?:\"|')";

		//regex_str = Pattern.quote(regex_str);
		Pattern pt = Pattern.compile(regex_str);
		for (String line:lines) {//分行进行提取，似乎可以提高成功率？PATH_AND_QUERY
			line = decodeAll(line);
			Matcher matcher = pt.matcher(line);
			while (matcher.find()) {//多次查找
				String url = matcher.group();
				URLs.add(url);
			}
		}

		//这部分提取的是含有协议头的完整URL地址
		for (String line:lines) {
			line = decodeAll(line);
			Matcher matcher = PatternsFromAndroid.WEB_URL.matcher(line);
			while (matcher.find()) {//多次查找
				String url = matcher.group();
				//即使是www.www也会被认为是URL（应该是被认作了主机名或文件名），所以必须过滤
				if (url.toLowerCase().startsWith("http://")
						||url.toLowerCase().startsWith("https://")
						||url.toLowerCase().startsWith("rtsp://")
						||url.toLowerCase().startsWith("ftp://")){
					URLs.add(url);
				}
			}
		}

		List<String> tmplist= new ArrayList<>(URLs);
		Collections.sort(tmplist);
		tmplist = Commons.removePrefixAndSuffix(tmplist,"\"","\"");
		return tmplist;
	}

	/**
	 * 会发现如下类型的IP，是有效的IP地址，但是实际情况却不会有人这么写。
	 * 应当从我们的正则中剔除
	 * PING 181.002.245.007 (181.2.245.7): 56 data bytes
	 * @param httpResponse
	 * @return
	 */
	public static List<String> grepIP(String httpResponse) {
		Set<String> IPSet = new HashSet<>();
		List<String> lines = Commons.textToLines(httpResponse);

		for (String line:lines) {
			Matcher matcher = PatternsFromAndroid.IP_ADDRESS.matcher(line);
			while (matcher.find()) {//多次查找
				String tmpIP = matcher.group();
				if (IPAddressUtils.isValidIP(tmpIP)) {
					IPSet.add(tmpIP);
				}
			}
		}

		List<String> tmplist= new ArrayList<>(IPSet);
		Collections.sort(tmplist);
		return tmplist;
	}

	/**
	 *  误报太高，不划算。不再使用
	 * @param httpResponse
	 * @return
	 */
	@Deprecated
	public static List<String> grepIPAndPort(String httpResponse) {
		Set<String> IPSet = new HashSet<>();
		String[] lines = httpResponse.split("(\r\n|\r|\n)");

		for (String line:lines) {
			String pattern = "\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})?";
			Pattern pt = Pattern.compile(pattern);
			Matcher matcher = pt.matcher(line);
			while (matcher.find()) {//多次查找
				String tmpIP = matcher.group();
				if (IPAddressUtils.isValidIP(tmpIP)) {
					IPSet.add(tmpIP);
				}
			}
		}

		List<String> tmplist= new ArrayList<>(IPSet);
		Collections.sort(tmplist);
		return tmplist;
	}

	/**
	 * 提取网段信息 比如143.11.99.0/24
	 * @param httpResponse
	 * @return
	 */
	public static List<String> grepSubnet(String httpResponse) {
		Set<String> IPSet = new HashSet<>();
		String[] lines = httpResponse.split("(\r\n|\r|\n)");

		for (String line:lines) {
			String pattern = "\\d{1,3}(?:\\.\\d{1,3}){3}(?:/\\d{1,2})?";
			Pattern pt = Pattern.compile(pattern);
			Matcher matcher = pt.matcher(line);
			while (matcher.find()) {//多次查找
				String tmpIP = matcher.group();
				IPSet.add(tmpIP);
			}
		}

		List<String> tmplist= new ArrayList<>(IPSet);
		Collections.sort(tmplist);
		return tmplist;
	}

	public static boolean needUnicodeConvert(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		//Pattern pattern = Pattern.compile("(\\\\u([A-Fa-f0-9]{4}))");//和上面的效果一样
		Matcher matcher = pattern.matcher(str.toLowerCase());
		if (matcher.find() ){
			return true;
		}else {
			return false;
		}
	}

	public static boolean needURLConvert(String str) {
		Pattern pattern = Pattern.compile("(%(\\p{XDigit}{2}))");

		Matcher matcher = pattern.matcher(str.toLowerCase());
		if (matcher.find() ){
			return true;
		}else {
			return false;
		}
	}

	/**
	 * 从burp的Email addresses disclosed这个issue中提取，废弃这个
	 * DomainPanel.collectEmails()，可以从issue中提取Email，但是不是实时的，只有search或者fresh的时候才会触发。
	 */
	public static Set<String> grepEmail(String httpResponse) {
		Set<String> Emails = new HashSet<>();
		final String REGEX_EMAIL = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";

		Pattern pDomainNameOnly = Pattern.compile(REGEX_EMAIL);
		Matcher matcher = pDomainNameOnly.matcher(httpResponse);
		while (matcher.find()) {//多次查找
			String item = matcher.group();
			if (DomainNameUtils.isValidDomain(item.split("@")[1])) {
				Emails.add(item);
				System.out.println(item);
			}
		}
		return Emails;
	}

	public static void main(String[] args) {
		test3();
	}
	
	
	public static void test4(){
		System.out.println(grepSubnet("202.181.90.0/24\tSHOPEE SINGAPORE PRIVATE LIMITEDSingapore\n" +
				"202.181.91.0/24\tSHOPEE SINGAPORE PRIVATE LIMITEDSingapore"));
	}

	public static void test3(){
		System.out.println(grepDomain("baidu.com."));
		System.out.println(grepDomain("http://baidu.com."));
		System.out.println(grepDomain("http://baidu.com:200."));
	}

	public static void test2() {
		String tmpDomain = "aaa *.baidu.com  bbb";
		if (tmpDomain.startsWith("*.")) {
			tmpDomain = tmpDomain.replaceFirst("\\*\\.","");//第一个参数是正则
		}
		System.out.println(tmpDomain);
	}

	public static void test1() {
//		String line = "\"%.@.\\\"xsrf\\\",";
		String line = "%2f%2fbaidu.com";
		System.out.println(needURLConvert(line));
		if (needURLConvert(line)) {
			while (true) {
				try {
					int oldlen = line.length();
					line = URLDecoder.decode(line);
					int currentlen = line.length();
					if (oldlen > currentlen) {
						continue;
					}else {
						break;
					}
				}catch(Exception e) {
					e.printStackTrace();
					break;//即使出错，也要进行后续的查找
				}
			}
		}
		System.out.println(line);
	}

	public static void test(){
		String aaa="  <div class=\"mod_confirm brandad_authority_failapply\">\n" +
				"    <a href=\"javascript:;\" class=\"mod_confirm_close\"><i></i></a>\n" +
				"    <div class=\"mod_confirm_hd\">申请开通账户权限</div>\n" +
				"    <div class=\"mod_confirm_bd\">\n" +
				"      <p class=\"mod_confirm_txt fapplyReason\"></p>\n" +
				"      <p class=\"mod_confirm_txt\">如有疑问请咨询：<em>bdm@jd.com</em></p>\n" +
				"    </div>\n" +
				"    <div class=\"mod_confirm_ft\">\n" +
				"      <a href=\"#\" class=\"mod_btn mod_btn_default reapply\">重新申请</a>\n" +
				"      <a href=\"#\" class=\"mod_btn mod_btn_white mod_close_btn\">关闭</a>\n" +
				"    </div>\n" +
				"  </div>"+"            <div class=\"footer-menu-left\">\r\n" +
				"                <dl>\r\n" +
				"                    <dt>京东众创</dt>\r\n" +
				"                    <dd>客服电话 400-088-8816</dd>\r\n" +
				"                    <dd>客服邮箱 zcfw@jd.com</dd>\r\n" +
				"                </dl>\r\n" +
				"            </div>"+"                        if (result.Code == 8)\r\n" +
				"                        {\r\n" +
				"                            $(\"#divInfo\").show();\r\n" +
				"                            $(\"#divInfo\").html(\"<b></b><span class=\\\"warntip_text\\\">ERP系统中信息不完整，请将邮箱地址、erp账户、手机号发送至itmail@jd.com邮箱中</span>\");//ERP系统中信息不完整，请联系邮件管理员!\r\n" +
				"                        }"+"/* 2019-03-12 11:16:22 joya.js @issue to lijiwen@jd.com Thanks */\r\n" +
				"try{window.fingerprint={},function t(){fingerprint.config={fpb_send_data:'body={\"appname\": \"jdwebm_hf\",\"jdkey\": \"\",\"whwswswws\": \"\",\"businness\": \"\",\"body\":";
		//System.out.println(grepEmail(aaa));

		String bbb="https%3A%2F%2F3pl.jd.com%2F";
		System.out.println(needURLConvert(bbb));

		String ccc = "5E44a6a6a1f3731fbaa00ca03e68a8d20c%5E%5E%E5%9C%A8%E7%BA%BF%E6%94%AF%E4%BB%98%5E%5Ehttps%3A%2F%2Fpay.bilibili.com%2Fpayplatform-h5%2Fcashierdesk.html";
		System.out.println(URLDecoder.decode(ccc));
		System.out.println(ccc.length());
		System.out.println(URLDecoder.decode(ccc).length());
		System.out.println("在线支付".length());
	}
}