package domain;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import burp.BurpExtender;
import burp.Getter;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadSearhDomain{
	private List<IHttpRequestResponse> messages;
	private List<DomainProducer> plist;
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout;
    public PrintWriter stderr;
    public IExtensionHelpers helpers = callbacks.getHelpers();
	
	public ThreadSearhDomain(List<IHttpRequestResponse> messages) {
		this.messages = messages;
		stdout = BurpExtender.getStdout();
		stderr = BurpExtender.getStderr();
	}

	public void Do(){
		stdout.println("~~~~~~~~~~~~~Start Search Domain~~~~~~~~~~~~~");
		
		BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();//use to store messageInfo
		BlockingQueue<String> subDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> similarDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> relatedDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> emailQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> packageNameQueue = new LinkedBlockingQueue<String>();
		
		inputQueue.addAll(messages);

		plist = new ArrayList<DomainProducer>();

		for (int i=0;i<=20;i++) {
			DomainProducer p = new DomainProducer(inputQueue,subDomainQueue,
					similarDomainQueue,relatedDomainQueue,emailQueue,packageNameQueue,i);
			p.start();
			plist.add(p);
		}

		while(true) {//to wait all threads exit.
			if (inputQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Search Domain Done~~~~~~~~~~~~~");
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
		
		HashSet<String> oldSubdomains = new HashSet<String>();
		oldSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		DomainPanel.getDomainResult().getSubDomainSet().addAll(subDomainQueue);
		DomainPanel.getDomainResult().getSimilarDomainSet().addAll(similarDomainQueue);
		DomainPanel.getDomainResult().getRelatedDomainSet().addAll(relatedDomainQueue);
		DomainPanel.getDomainResult().getEmailSet().addAll(emailQueue);
		DomainPanel.getDomainResult().getPackageNameSet().addAll(packageNameQueue);
		//stdout.println(emailQueue.size());
		//stdout.println(packageNameQueue.size());

		HashSet<String> newSubdomains = new HashSet<String>();
		newSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());
		
		newSubdomains.removeAll(oldSubdomains);
		DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().addAll(newSubdomains);
		stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~",newSubdomains.size()));
		stdout.println(String.join(System.lineSeparator(), newSubdomains));

		return;
	}

	boolean isAllProductorFinished(){
		for (DomainProducer p:plist) {
			if(p.isAlive()) {
				return false;
			}
		}
		return true;
	}
	
	public void stopThreads() {
		for (DomainProducer p:plist) {
			p.stopThread();
		}
		stdout.println("threads stopped!");
	}
	
//	public static void main(String args[]) {//test
//		System.out.println(DomainProducer.grepDomain("http://www.jd.com/usr/www.baidu.com/xss.jd.com"));
//	}
}

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

class DomainProducer extends Thread {//Producer do
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
			Getter getter = new Getter(helpers);
			try {
				if (inputQueue.isEmpty() || stopflag) {
					//stdout.println("Producer break");
					break;
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
						Set<String> tmpDomains = CertInfo.getSANs(shortURL,DomainPanel.domainResult.fetchKeywordSet());
						for (String domain:tmpDomains) {
							if (!relatedDomainQueue.contains(domain)) {
								relatedDomainQueue.add(domain);
							}
						}
					}
				}
				
				if (type != DomainManager.USELESS) {//grep domains from response and classify
					if (urlString.endsWith(".gif") ||urlString.endsWith(".jpg")
							|| urlString.endsWith(".png") ||urlString.endsWith(".css")) {
						
					}else {
						classifyDomains(messageinfo);
						//classifyEmails(messageinfo);
					}
				}
			} catch (Exception error) {
				error.printStackTrace(BurpExtender.getStderr());
			}
		}
	}
	
	public void classifyDomains(IHttpRequestResponse messageinfo) {
		byte[] response = messageinfo.getResponse();
		if (response != null) {
			Set<String> domains = DomainProducer.grepDomain(new String(response));
			for (String domain:domains) {
				int type = DomainPanel.domainResult.domainType(domain);
				if (type == DomainManager.SUB_DOMAIN)
				{
					subDomainQueue.add(domain);
					
				}else if (type == DomainManager.SIMILAR_DOMAIN) {
					similarDomainQueue.add(domain);
				}else if (type == DomainManager.PACKAGE_NAME) {
					packageNameQueue.add(domain);
				}
			}
		}
	}
	
	@Deprecated
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

	public static Set<String> grepDomain(String httpResponse) {
		httpResponse = httpResponse.toLowerCase();
		//httpResponse = cleanResponse(httpResponse);
		Set<String> domains = new HashSet<>();
		//"^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$"
		final String DOMAIN_NAME_PATTERN = "([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
		
		String[] lines = httpResponse.split("\r\n");
		
		for (String line:lines) {//分行进行提取，似乎可以提高成功率？
			line = line.trim();
			int counter =0;
//			while (line.contains("&#x") && counter<3) {// &#x URF-8编码的特征，对于域名的提取不需要对它进行处理
//				try {
//					line = StringEscapeUtils.unescapeHtml4(line);
//					counter++;
//				}catch(Exception e) {
//					e.printStackTrace(BurpExtender.getStderr());
//					break;//即使出错，也要进行后续的查找
//				}
//			}
			
			counter = 0;
			while (needURLConvert(line) && counter<3) {// %对应的URL编码
				try {
					line = URLDecoder.decode(line);
					counter++;
				}catch(Exception e) {
					//e.printStackTrace(BurpExtender.getStderr());
					break;//即使出错，也要进行后续的查找
				}
			}
			//保险起见，再做一层处理
			if (line.toLowerCase().contains("%2f")) {
				line.replace("%2f"," ");
			}
			
			if (line.toLowerCase().contains("%3a")) {
				line.replace("%3a"," ");
			}
			
			
			
			counter = 0;
			while (needUnicodeConvert(line) && counter<3) {//unicode解码
				try {
					line = StringEscapeUtils.unescapeJava(line);
					counter++;
				}catch(Exception e) {
					//e.printStackTrace(BurpExtender.getStderr());
					break;//即使出错，也要进行后续的查找
				}
			}
			
			        
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
	
	@Deprecated
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
    }
}
