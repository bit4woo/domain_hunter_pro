package burp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import com.alibaba.fastjson.JSON;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;


public class BurpExtender extends GUI implements IBurpExtender, ITab {
    public IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    private PrintWriter stdout;//现在这里定义变量，再在registerExtenderCallbacks函数中实例化，如果都在函数中就只是局部变量，不能在这实例化，因为要用到其他参数。
    private PrintWriter stderr;
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	stderr = new PrintWriter(callbacks.getStderr(), true);
    	stdout.println(ExtenderName);
    	stdout.println(github);
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName(ExtenderName); //插件名称
        addMenuTab();
        
    }
    
    @Override
	public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords){
			subdomainofset.clear();
			domainlikeset.clear();
			
			for (String rootdomain:rootdomains) {
				if (rootdomain.contains(".")&&!rootdomain.endsWith(".")&&!rootdomain.startsWith("."))
				{
					Set<String> httpsURLs = new HashSet<String>();
					IHttpRequestResponse[] requestResponses = callbacks.getSiteMap(null);
					    //stdout.println(response[1]);
				    for (IHttpRequestResponse x:requestResponses){
				    	
				    	IHttpService httpservice = x.getHttpService();
				    	String shortURL = httpservice.toString();
				    	String protocol =  httpservice.getProtocol();
						String Host = httpservice.getHost();
						
						//stdout.println(shortURL);
						//stdout.println(Host);
						
						if (Host.endsWith("."+rootdomain)||Host.equalsIgnoreCase(rootdomain)){
							subdomainofset.add(Host);
							//stdout.println(subdomainofset);
							//get SANs info to get related domain, only when the [subdomain] is using https.
							if(protocol.equalsIgnoreCase("https")) {
									httpsURLs.add(shortURL);
							}
							continue;
						}
						
						
						if (!domainlike.equals("") && Host.contains(domainlike)){
							domainlikeset.add(Host);
							if(protocol.equalsIgnoreCase("https")) {
								httpsURLs.add(shortURL);
							}
							//stdout.println(domainlikeset);
							continue;
						}
						
						if (Commons.validIP(Host)) {//https://202.77.129.30
							if(protocol.equalsIgnoreCase("https")) {
								httpsURLs.add(shortURL);
							}
						}
				    }
					    
				    stdout.println("sub-domains and similar-domains search finished,starting get related-domains");
				    //stdout.println(httpsURLs);
				    
				    //多线程获取
				    //Set<Future<Set<String>>> set = new HashSet<Future<Set<String>>>();
			    	Map<String,Future<Set<String>>> urlResultmap = new HashMap<String,Future<Set<String>>>();
			        ExecutorService pool = Executors.newFixedThreadPool(10);
			        
			        for (String url:httpsURLs) {
			          Callable<Set<String>> callable = new ThreadCertInfo(url,domainlike);
			          Future<Set<String>> future = pool.submit(callable);
			          //set.add(future);
			          urlResultmap.put(url, future);
			        }
			        
			        Set<String> tmpRelatedDomainSet = new HashSet<String>();
			        for(String url:urlResultmap.keySet()) {
			        	Future<Set<String>> future = urlResultmap.get(url);
			        //for (Future<Set<String>> future : set) {
			          try {
			        	  stdout.println("founded related-domains :"+future.get() +" from "+url);
			        	  if (future.get()!=null) {
			        		  tmpRelatedDomainSet.addAll(future.get());
			        	  }
			        	  
					} catch (Exception e) {
						//e.printStackTrace(stderr);
						stderr.println(e.getMessage());
			        }
			        }
				    
			        /* 单线程获取方式
				    Set<String> tmpRelatedDomainSet = new HashSet<String>();
				    //begin get related domains
				    for(String url:httpsURLs) {
				    	try {
				    		tmpRelatedDomainSet.addAll(CertInfo.getSANs(url));
						}catch(UnknownHostException e) {
							stderr.println("UnknownHost "+ url);
							continue;
						}catch(ConnectException e) {
							stderr.println("Connect Failed "+ url);
							continue;
						}
				    	catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace(stderr);
							continue;
						}
				    }
				    */
				    
				    //对 SANs的结果再做一次分类。
				    for (String item:tmpRelatedDomainSet) {
				    	if (item.contains(".")&&!item.endsWith(".")&&!item.startsWith(".")) {
					    	if (item.endsWith("."+rootdomain) || item.equalsIgnoreCase(rootdomain)){
								subdomainofset.add(item);
							}else if (!domainlike.equals("") && item.contains(domainlike)){
								domainlikeset.add(item);
							}else {
								relatedDomainSet.add(item);
							}
				    	}
				    }
				}
			}
			
		    
		    Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		    result.put("subdomainofset", subdomainofset);
		    result.put("domainlikeset", domainlikeset);
		    result.put("relatedDomainSet", relatedDomainSet);
		    
		    resultJson = JSON.toJSONString(result);
		    
		    return result;
		    
	}
	
	@Override
	public Map<String, Set<String>> spiderall (Set<String> rootdomains, Set<String> keywords) {
	    int i = 0;
	    while(i<=2) {
			for (String rootdomain: rootdomains) {
				if (!rootdomain.contains(".")||rootdomain.endsWith(".")||rootdomain.equals("")){
					//如果域名为空，或者（不包含.号，或者点号在末尾的）
				}
				else {
			    	IHttpRequestResponse[] items = callbacks.getSiteMap(null); //null to return entire sitemap
			    	//int len = items.length;
			    	//stdout.println("item number: "+len);
			    	Set<URL> NeedToCrawl = new HashSet<URL>();
				    for (IHttpRequestResponse x:items){// 经过验证每次都需要从头开始遍历，按一定offset获取的数据每次都可能不同
				    	IRequestInfo  analyzeRequest = helpers.analyzeRequest(x); //前面的操作可能已经修改了请求包，所以做后续的更改前，都需要从新获取。
						
						String Host = analyzeRequest.getUrl().getHost();
						
						URL shortUrl=analyzeRequest.getUrl();
						try {
							String urlstring = x.getHttpService().toString();
							shortUrl = new URL(urlstring);
						} catch (MalformedURLException e) {
							e.printStackTrace(stdout);
						}

						if (Host.endsWith("."+rootdomain) && Commons.isResponseNull(x)) {
							// to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
							NeedToCrawl.add(shortUrl);
							// to reduce memory usage, use shortUrl. base on my test, spider will crawl entire site when send short URL to it.
							// this may miss some single page, but the single page often useless for domain collection
							// see spideralltest() function.
						}
					}
				    
				    
					for (URL shortUrl:NeedToCrawl) {
						if (!callbacks.isInScope(shortUrl)) { //reduce add scope action, to reduce the burp UI action.
							callbacks.includeInScope(shortUrl);//if not, will always show confirm message box.
						}
						callbacks.sendToSpider(shortUrl);
					}
				}
			}
			
			
			try {
				Thread.sleep(5*60*1000);//单位毫秒，60000毫秒=一分钟
				stdout.println("sleep 5 minutes to wait spider");
				//to wait spider
			} catch (InterruptedException e) {
				e.printStackTrace(stdout);
			}
	    	i++;
		}
		
		
		
	    return search(rootdomains,keywords);
	    //search(subdomainof,domainlike);
	}
	
	
	public Map<String, Set<String>> spideralltest (String subdomainof, String domainlike) {
		
		int i = 0;
		while (i<=10) {
			try {
				callbacks.sendToSpider(new URL("http://www.baidu.com/"));
				Thread.sleep(1*60*1000);//单位毫秒，60000毫秒=一分钟
				stdout.println("sleep 1 min");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
			// to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
		}
		
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		return result;
	}
	
	@Override
	public Boolean upload(String url,String resultJson) {
		if ((url.toLowerCase().contains("http://") ||url.toLowerCase().contains("https://"))
				&& !resultJson.equals("")){
			try {
				HTTPPost.httpPostRequest(url,resultJson);
				return true;
			} catch (IOException e) {
				e.printStackTrace(stderr);
				return false;
			}
		}
		return false;
		
	}
	
	

	
//以下是各种burp必须的方法 --start
    
    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
        	BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this); //这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由CGUI()函数初始化。
        	//如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
        }
      });
    }
    
    
    //ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return ("Domain Hunter");
	}
	@Override
	public Component getUiComponent() {
		//return this.contentPane;
		callbacks.printOutput(this.getContentPane()+"");
		return this.getContentPane();
	}
	//ITab必须实现的两个方法
	//各种burp必须的方法 --end
	
	
	
/*	public static void main(String args[]) {
		System.out.println("aaa".contains(""));
	}*/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}