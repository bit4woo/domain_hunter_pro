package burp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadCertInfo implements Callable<Set<String>>{
	/*
    private Set<String> urls;
    public ThreadCertInfo(Set<String> urls) {
    	this.urls = urls;
    }
    
    
    @Override
    public Set<String> call(){
    	Set<String> tmpDomains = new HashSet<String>();
      for (int i=0;i<=urls.size()/10+1;i++) {//一般根据   【资源数量%线程数量+1】 来确定，保证资源得到处理
        
    	//如果线程类实现 runnable 接口获取当前的线程，只能用 Thread.currentThread() 获取当前的线程名
        Thread.currentThread().getName();
        
        if(urls.iterator().hasNext()){
        	try {
				tmpDomains = CertInfo.getSANs(urls.iterator().next());
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
      }
	return null;
    }
	*/
    
	private String url;
	private Set<String> domainKeywords;
    public ThreadCertInfo(String url,Set<String> domainKeywords) {
    	this.url = url;
    	this.domainKeywords = domainKeywords;
    }
    
    
    @Override
    public Set<String> call() throws Exception{
		Set<String> tmpDomains = CertInfo.getSANs(url,domainKeywords);
		return tmpDomains;
    }
	
    public static void main(String[] args) {

    	Set<String> urls = new HashSet<String>();
    	urls.add("https://202.77.129.30");
    	urls.add("https://ebppweb.alipay.com");
    	urls.add("https://opendoc.cloud.alipay.com");
    	urls.add("https://ab.alipay.com");
    	urls.add("https://goldetfprod.alipay.com");
    	urls.add("https://tfs.alipay.com");
    	urls.add("https://docs.alipay.com");
    	urls.add("https://emembercenter.alipay.com");
    	urls.add("https://docs.open.alipay.com");
    	urls.add("https://benefitprod.alipay.com");
    	urls.add("https://mapi.alipay.com");
    	urls.add("https://ie.alipay.com");
    	urls.add("https://fun.alipay.com");
    	urls.add("https://shenghuo.alipay.com");
    	urls.add("https://home.alipay.com");
    	
    	Set<Future<Set<String>>> set = new HashSet<Future<Set<String>>>();
    	Map<String,Future<Set<String>>> urlResultmap = new HashMap<String,Future<Set<String>>>();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        
		Set<String> set1 = new HashSet<>();
		set1.add("alibaba");
		set1.add("taobao");
		set1.add("alipay");
		
        for (String word: urls) {

          Callable<Set<String>> callable = new ThreadCertInfo(word,set1);
          Future<Set<String>> future = pool.submit(callable);
          set.add(future);
          urlResultmap.put(word, future);
        }
        
        
        
        Set<String> Domains = new HashSet<String>();
        for(String url:urlResultmap.keySet()) {
        	Future<Set<String>> future = urlResultmap.get(url);
        //for (Future<Set<String>> future : set) {
          try {
        	  System.out.println(url);
        	  System.out.println(future.get());
        	  if (future.get()!=null) {
        		  Domains.addAll(future.get());
        	  }
        	  
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} catch (ExecutionException e) {
			System.out.println(e.getMessage());
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
        }
        
        System.out.println(Commons.set2string(Domains));
      }
}
