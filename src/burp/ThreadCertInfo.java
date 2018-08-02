package burp;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import test.CallableExample.WordLengthCallable;

public class ThreadCertInfo implements Callable<Set<String>>{
 
    private Set<String> Domains;
    private Set<String> urls;
    public ThreadCertInfo(Set<String> urls) {
    	this.urls = urls;
    }
    
    
    @Override
    public Set<String> call() throws Exception{
    	Set<String> tmpDomains = new HashSet<String>();
      for (int i=0;i<=urls.size()/10+1;i++) {//一般根据   【资源数量%线程数量+1】 来确定，保证资源得到处理
        
    	//如果线程类实现 runnable 接口获取当前的线程，只能用 Thread.currentThread() 获取当前的线程名
        Thread.currentThread().getName();
        
        if(urls.iterator().hasNext()){
        	try {
				tmpDomains = CertInfo.getSANs(urls.iterator().next());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
      }
	return null;
    }

    
    public static void main(String[] args) {
        
        ExecutorService pool = Executors.newFixedThreadPool(3);
        Set<Future<Integer>> set = new HashSet<Future<Integer>>();
        
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
    	
        
        for (String word: urls) {
          Callable<Integer> callable = new WordLengthCallable(word);
          Future<Set<String>> future = pool.submit(callable);
          set.add(future);
        }
        
        int sum = 0;
        
        for (Future<Integer> future : set) {
          try {
			sum += future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        }
        
        System.out.printf("The sum of lengths is %s%n", sum);
        System.exit(sum);
        
      }

}
