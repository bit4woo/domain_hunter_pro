package domain;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import burp.BurpExtender;
import burp.Commons;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;

public class DomainProducer extends Thread {//Producer do
	private final BlockingQueue<IHttpRequestResponse> inputQueue;//use to store messageInfo
	private final BlockingQueue<String> subDomainQueue;
	private final BlockingQueue<String> similarDomainQueue;
	private final BlockingQueue<String> relatedDomainQueue;
	private final BlockingQueue<String> EmailQueue;
	private final BlockingQueue<String> packageNameQueue;
	private BlockingQueue<String> httpsQueue = new LinkedBlockingQueue<>();//temp variable to identify checked https

	private int threadNo;
	private boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public DomainProducer(BlockingQueue<IHttpRequestResponse> inputQueue, 
			BlockingQueue<String> subDomainQueue,
			BlockingQueue<String> similarDomainQueue,
			BlockingQueue<String> relatedDomainQueue,
			BlockingQueue<String> EmailQueue,
			BlockingQueue<String> packageNameQueue,
			int threadNo) {
		this.threadNo = threadNo;
		this.inputQueue = inputQueue;
		this.subDomainQueue = subDomainQueue;
		this.similarDomainQueue = similarDomainQueue;
		this.relatedDomainQueue = relatedDomainQueue;
		this.EmailQueue = EmailQueue;
		this.packageNameQueue = packageNameQueue;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (threadNo!= 9999){//9999是流量进程，除非关闭，否则一直不退出。
					if (inputQueue.isEmpty() || stopflag) {
						//stdout.println("Producer break");
						break;
					}
				}

				IHttpRequestResponse messageinfo = inputQueue.take();

				IHttpService httpservice = messageinfo.getHttpService();
				String urlString = helpers.analyzeRequest(messageinfo).getUrl().toString();

				String shortURL = httpservice.toString();
				String protocol =  httpservice.getProtocol();
				String Host = httpservice.getHost();

				//callbacks.printOutput(rootdomains.toString());
				//callbacks.printOutput(keywords.toString());
				int type = DomainPanel.domainResult.domainType(Host);
				//callbacks.printOutput(Host+":"+type);
				if (type == DomainManager.SUB_DOMAIN)
				{	
					if (!subDomainQueue.contains(Host)) {
						stdout.print("new domain found: "+Host);
						subDomainQueue.add(Host);
					}
				}else if (type == DomainManager.SIMILAR_DOMAIN) {
					if (!similarDomainQueue.contains(Host)) {
						similarDomainQueue.add(Host);
					}
				}

				if (type !=DomainManager.USELESS && protocol.equalsIgnoreCase("https")){//get related domains
					if (!httpsQueue.contains(shortURL)) {//httpService checked or not
						httpsQueue.put(shortURL);//必须先添加，否则执行在执行https链接的过程中，已经有很多请求通过检测进行相同的请求了。
						Set<String> tmpDomains = CertInfo.getSANsbyKeyword(shortURL,DomainPanel.domainResult.fetchKeywordSet());
						for (String domain:tmpDomains) {
							if (!relatedDomainQueue.contains(domain)) {
								relatedDomainQueue.add(domain);
							}
						}
					}
				}

				if (type != DomainManager.USELESS && !Commons.uselessExtension(urlString)) {//grep domains from response and classify
					byte[] response = messageinfo.getResponse();
					if (response != null) {
						classifyDomains(new String(response));
					}
				}
			} catch (Exception error) {
				error.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	public void classifyDomains(String content) {
		if (content != null) {
			Set<String> domains = DomainProducer.grepDomain(content);
			for (String domain:domains) {
				int type = DomainPanel.domainResult.domainType(domain);
				if (type == DomainManager.SUB_DOMAIN)
				{
					if (!subDomainQueue.contains(domain)) {
						stdout.print("new domain found: "+domain);
						subDomainQueue.add(domain);
					}
				}else if (type == DomainManager.SIMILAR_DOMAIN) {
					similarDomainQueue.add(domain);
				}else if (type == DomainManager.PACKAGE_NAME) {
					packageNameQueue.add(domain);
				}
			}
		}
	}

	@Deprecated //从burp的Email addresses disclosed这个issue中提取，废弃这个
	public void classifyEmails(IHttpRequestResponse messageinfo) {
		byte[] response = messageinfo.getResponse();
		if (response != null) {
			Set<String> emails = DomainProducer.grepEmail(new String(response));
			for (String email:emails) {
				if (DomainPanel.domainResult.isRelatedEmail(email)) {
					EmailQueue.add(email);
				}
			}
			//EmailQueue.addAll(emails);
		}
	}

	@Deprecated
	public static String cleanResponse(String response) {
		String[] toReplace = {"<em>","<b>","</b>","</em>","<strong>","</strong>","<wbr>","</wbr>",">", ":", "=", "<", "/", "\\", ";", "&", "%3A", "%3D", "%3C"};

		for (String item:toReplace) {
			if (response.toLowerCase().contains(item)) {
				response = response.replaceAll(item, "");
			}
		}
		return response;
	}

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
		return line;
	}

	public static Set<String> grepDomain(String httpResponse) {
		httpResponse = httpResponse.toLowerCase();
		//httpResponse = cleanResponse(httpResponse);
		Set<String> domains = new HashSet<>();
		//"^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$"
		final String DOMAIN_NAME_PATTERN = "([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";

		String[] lines = httpResponse.split("\r\n");

		for (String line:lines) {//分行进行提取，似乎可以提高成功率？
			line = decodeAll(line);
			Pattern pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
			Matcher matcher = pDomainNameOnly.matcher(line);
			while (matcher.find()) {//多次查找
				domains.add(matcher.group());
			}
		}
		return domains;
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

	@Deprecated //从burp的Email addresses disclosed这个issue中提取，废弃这个
	public static Set<String> grepEmail(String httpResponse) {
		Set<String> Emails = new HashSet<>();
		final String REGEX_EMAIL = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";

		Pattern pDomainNameOnly = Pattern.compile(REGEX_EMAIL);
		Matcher matcher = pDomainNameOnly.matcher(httpResponse);
		while (matcher.find()) {//多次查找
			Emails.add(matcher.group());
			System.out.println(matcher.group());
		}

		return Emails;
	}

	public static void main(String args[]){
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