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
import com.google.common.net.InternetDomainName;

import java.awt.Component;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;


public class BurpExtender extends GUI implements IBurpExtender, ITab {
	private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    
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
		
		Set<String> httpsURLs = new HashSet<String>();
		
		Set<IHttpService> httpServiceSet = getHttpServiceFromSiteMap();
		
	    for (IHttpService httpservice:httpServiceSet){
	    	
	    	String shortURL = httpservice.toString();
	    	String protocol =  httpservice.getProtocol();
			String Host = httpservice.getHost();
			
			//callbacks.printOutput(rootdomains.toString());
			//callbacks.printOutput(keywords.toString());
			int type = domainResult.domainType(Host);
			//callbacks.printOutput(Host+":"+type);
			if (type == DomainObject.SUB_DOMAIN)
			{
				domainResult.subDomainSet.add(Host);
			}else if (type == DomainObject.SIMILAR_DOMAIN) {
				domainResult.similarDomainSet.add(Host);
			}
			
			if (type !=DomainObject.USELESS && protocol.equalsIgnoreCase("https")){
				httpsURLs.add(shortURL);
			}
	    }
			    
	    stdout.println("sub-domains and similar-domains search finished,starting get related-domains");
	    //stdout.println(httpsURLs);
			    
	    //多线程获取
	    //Set<Future<Set<String>>> set = new HashSet<Future<Set<String>>>();
    	Map<String,Future<Set<String>>> urlResultmap = new HashMap<String,Future<Set<String>>>();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        
        for (String url:httpsURLs) {
          Callable<Set<String>> callable = new ThreadCertInfo(url,keywords);
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
        domainResult.relatedDomainSet =tmpRelatedDomainSet;
		if (rdbtnAddRelatedToRoot.isSelected()==true) {
			domainResult.relatedToRoot();
			ShowDomainObjects(domainResult);
		}

	    return null;
    }
	
	@Override
	public Map<String, Set<String>> crawl (Set<String> rootdomains, Set<String> keywords) {
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
						
				    	IHttpService httpservice = x.getHttpService();
				    	String shortUrlString = httpservice.toString();
						String Host = httpservice.getHost();
				    	
						try {
							URL shortUrl = new URL(shortUrlString);
							
							if (Host.endsWith("."+rootdomain) && Commons.isResponseNull(x)) {
								// to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
								NeedToCrawl.add(shortUrl);
								// to reduce memory usage, use shortUrl. base on my test, spider will crawl entire site when send short URL to it.
								// this may miss some single page, but the single page often useless for domain collection
								// see spideralltest() function.
							}
						} catch (MalformedURLException e) {
							e.printStackTrace(stderr);
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
	
	
/*	public Map<String, Set<String>> spideralltest (String subdomainof, String domainlike) {
		
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
	}*/

	
	
	/**
	 * @return IHttpService set to void duplicate IHttpRequestResponse handling
	 * 
	 */
	Set<IHttpService> getHttpServiceFromSiteMap(){
		IHttpRequestResponse[] requestResponses = callbacks.getSiteMap(null);
		Set<IHttpService> HttpServiceSet = new HashSet<IHttpService>();
	    for (IHttpRequestResponse x:requestResponses){
	    	
	    	IHttpService httpservice = x.getHttpService();
	    	HttpServiceSet.add(httpservice);
/*	    	String shortURL = httpservice.toString();
	    	String protocol =  httpservice.getProtocol();
			String Host = httpservice.getHost();*/
	    }
	    return HttpServiceSet;
		
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
		return ("Domain Hunter");
	}
	@Override
	public Component getUiComponent() {
		return this.getContentPane();
	}
	//ITab必须实现的两个方法
	//各种burp必须的方法 --end
	
	
	
/*	public static void main(String[] args) {
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
	}*/
}