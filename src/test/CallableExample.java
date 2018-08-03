package test;

import java.util.*;
import java.util.concurrent.*;


public class CallableExample {
	
	  public static class WordLengthCallable implements Callable {
		
	    private String word;
	    
	    public WordLengthCallable(String word) {
	      this.word = word;
	    }
	    public Integer call() {
	      return Integer.valueOf(word.length());
	    }
	  }
  
  
  
  public static void main(String args[]) throws Exception {
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
      Future<Integer> future = pool.submit(callable);
      set.add(future);
    }
    
    int sum = 0;
    
    for (Future<Integer> future : set) {
    	 System.out.print(future.get());
      sum += future.get();
    }
    
    System.out.printf("The sum of lengths is %s%n", sum);
    System.exit(sum);
  }
  
  
}