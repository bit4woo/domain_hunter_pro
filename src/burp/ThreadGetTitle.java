package burp;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import title.LineObject;

//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadGetTitle{
	private BurpExtender burp;
	Set<String> domains;
	public ThreadGetTitle(Set<String> domains,BurpExtender burp) {
		this.domains = domains;
		this.burp = burp;
		burp.stderr.println("ThreadGetTitle construtor");
	}
	
	public static void main(String args[]){
		
		
	}
	public void test() {
		burp.stderr.print("test");
	}
	
	public void Do() throws Exception{
		burp.stderr.print("doingdoing~~~~~~~~~~~~~~");
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();//use to store domains
		BlockingQueue<IHttpRequestResponse> sharedQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
		BlockingQueue<String> lineQueue = new LinkedBlockingQueue<String>();//use to store output---line

		ExecutorService pes = Executors.newFixedThreadPool(10);
		ExecutorService ces = Executors.newFixedThreadPool(10);
		for (int i=0;i<=10;i++) {
			pes.submit(new Producer(domainQueue,sharedQueue,i,this.burp));
		}
		
		for (int i=0;i<=10;i++) {
			ces.submit(new Consumer(sharedQueue,lineQueue,i,this.burp));
		}
		
		// shutdown should happen somewhere along with awaitTermination
		/* https://stackoverflow.com/questions/36644043/how-to-properly-shutdown-java-executorservice/36644320#36644320 */
		pes.shutdown();
		ces.shutdown();
	}


}

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

class Producer implements Runnable {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private final BlockingQueue<IHttpRequestResponse> sharedQueue;
	private int threadNo;

	private BurpExtender burp;
	private IExtensionHelpers helpers;
	private IBurpExtenderCallbacks callbacks;

	public Producer(BlockingQueue<String> domainQueue,BlockingQueue<IHttpRequestResponse> sharedQueue,int threadNo,BurpExtender burp) {
		this.burp = burp;
		this.helpers = burp.callbacks.getHelpers();
		this.callbacks = burp.callbacks;

		this.threadNo = threadNo;
		this.domainQueue = domainQueue;
		this.sharedQueue = sharedQueue;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (domainQueue.isEmpty()) {
					break;
				}
				String host = domainQueue.take();
				List<IHttpService> HttpServiceList = new ArrayList();
				HttpServiceList.add(helpers.buildHttpService(host,80,"http"));
				HttpServiceList.add(helpers.buildHttpService(host,443,"https"));
				

				for (IHttpService item:HttpServiceList) {
					try {
						byte[] request = helpers.buildHttpRequest(new URL(item.toString()));
						IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(item, request);
						sharedQueue.add(messageinfo);
					} catch (Exception err) {
						err.printStackTrace();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace(burp.stderr);
			}
		}
	}
}

/*
 * parse IResponseInfo object to line object
 * 
 */

class Consumer implements Runnable{// Consumer
	private final BlockingQueue<IHttpRequestResponse> sharedQueue;
	private final BlockingQueue<String> lineQueue;//use to store output---line
	private BurpExtender burp;
	private int threadNo;

	public Consumer (BlockingQueue<IHttpRequestResponse> sharedQueue,BlockingQueue<String> lineQueue,int threadNo,BurpExtender burp) {
		this.sharedQueue = sharedQueue;
		this.lineQueue = lineQueue;
		this.threadNo = threadNo;
		
		this.burp = burp;
	}

	@Override
	public void run() {
		while(true){
			if (sharedQueue.isEmpty()) {
				break;
			}
			try {
				IHttpRequestResponse messageinfo = sharedQueue.take();
				burp.addTitleRow(messageinfo);
			} catch (Exception err) {
				err.printStackTrace(burp.stderr);
			}
		}
	}
}
