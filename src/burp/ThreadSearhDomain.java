package burp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
class ThreadSearhDomain{
	private List<IHttpRequestResponse> messages;
	private List<DomainProducer> plist;
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();
	
	public ThreadSearhDomain(List<IHttpRequestResponse> messages) {
		this.messages = messages;
	}

	public void Do(){
		stdout.println("~~~~~~~~~~~~~Start Search Domain~~~~~~~~~~~~~");
		
		BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();//use to store messageInfo
		BlockingQueue<String> subDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> similarDomainQueue = new LinkedBlockingQueue<String>();
		BlockingQueue<String> relatedDomainQueue = new LinkedBlockingQueue<String>();
		
		inputQueue.addAll(messages);

		plist = new ArrayList<DomainProducer>();

		for (int i=0;i<=20;i++) {
			DomainProducer p = new DomainProducer(inputQueue,subDomainQueue,
					similarDomainQueue,relatedDomainQueue,i);
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
		
		BurpExtender.getDomainResult().subDomainSet.addAll(subDomainQueue);
		BurpExtender.getDomainResult().similarDomainSet.addAll(similarDomainQueue);
		BurpExtender.getDomainResult().relatedDomainSet.addAll(relatedDomainQueue);
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
	
	public static void main(String args[]) {//test
		System.out.println(DomainProducer.grepDomain("http://www.jd.com/usr/www.baidu.com/xss.jd.com"));
	}
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
	private BlockingQueue<String> httpsQueue = new LinkedBlockingQueue<>();//temp variable to identify checked https

	private int threadNo;
	private boolean stopflag = false;
	
	private static IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

	public DomainProducer(BlockingQueue<IHttpRequestResponse> inputQueue, BlockingQueue<String> subDomainQueue,
			BlockingQueue<String> similarDomainQueue,BlockingQueue<String> relatedDomainQueue,int threadNo) {
		this.threadNo = threadNo;
		this.inputQueue = inputQueue;
		this.subDomainQueue = subDomainQueue;
		this.similarDomainQueue = similarDomainQueue;
		this.relatedDomainQueue = relatedDomainQueue;
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
				int type = BurpExtender.domainResult.domainType(Host);
				//callbacks.printOutput(Host+":"+type);
				if (type == DomainObject.SUB_DOMAIN)
				{	
					if (!subDomainQueue.contains(Host)) {
						subDomainQueue.add(Host);
					}
				}else if (type == DomainObject.SIMILAR_DOMAIN) {
					if (!similarDomainQueue.contains(Host)) {
						similarDomainQueue.add(Host);
					}
				}

				if (type !=DomainObject.USELESS && protocol.equalsIgnoreCase("https")){//get related domains
					if (!httpsQueue.contains(shortURL)) {//httpService checked or not
						Set<String> tmpDomains = CertInfo.getSANs(shortURL,BurpExtender.domainResult.fetchKeywordSet());
						for (String domain:tmpDomains) {
							if (!relatedDomainQueue.contains(domain)) {
								relatedDomainQueue.add(domain);
							}
						}
						httpsQueue.add(shortURL);
					}
				}
				
				if (type != DomainObject.USELESS) {//grep domains from response and classify
					if (urlString.endsWith(".gif") ||urlString.endsWith(".jpg")
							|| urlString.endsWith(".png") ||urlString.endsWith(".css")) {
						
					}else {
						classifyDomains(messageinfo);
					}
					
				}
				
			} catch (Throwable error) {//java.lang.RuntimeException can't been catched, why?
			}
		}
	}
	
	public void classifyDomains(IHttpRequestResponse messageinfo) {
		byte[] response = messageinfo.getResponse();
		if (response != null) {
			Set<String> domains = DomainProducer.grepDomain(new String(response));
			for (String domain:domains) {
				int type = BurpExtender.domainResult.domainType(domain);
				if (type == DomainObject.SUB_DOMAIN)
				{
					subDomainQueue.add(domain);
					
				}else if (type == DomainObject.SIMILAR_DOMAIN) {
					similarDomainQueue.add(domain);
				}
			}
		}
	}

	public static Set<String> grepDomain(String httpResponse) {
		Set<String> domains = new HashSet<>();
		//"^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$"
		final String DOMAIN_NAME_PATTERN = "([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
		
		Pattern pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
		Matcher matcher = pDomainNameOnly.matcher(httpResponse);
		while (matcher.find()) {//多次查找
			domains.add(matcher.group());
		}
		return domains;
	}
}
