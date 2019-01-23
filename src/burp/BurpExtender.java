package burp;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class BurpExtender extends GUI implements IBurpExtender, ITab, IExtensionStateListener,IContextMenuFactory, IMessageEditorController{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static  IBurpExtenderCallbacks callbacks;
	private IExtensionHelpers helpers;
	LineTableModel TitletableModel;
	private ThreadGetTitle threadGetTitle;

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
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);
		addMenuTab();

		//add table and tablemodel to GUI
		TitletableModel = new LineTableModel(this);
		table_1 = new LineTable(TitletableModel,this);
		scrollPaneRequests.setViewportView(table_1);

		//recovery save domain results from extensionSetting
		String content = loadConfig();
		if (content!=null) {
			domainResult = domainResult.Open(content);
			showToUI(domainResult);

			////recovery lineEntries
			for (LineEntry entry:domainResult.getLineEntries()) {
				TitletableModel.addNewLineEntry(entry);
			}
			//TitletableModel.setLineEntries();//recovery lineEntries
		}
	}

	@Override
	public void extensionUnloaded() {
		//TODO to cancel SwingWorker in search and crawl function
		threadGetTitle.ces.shutdownNow();
		threadGetTitle.pes.shutdownNow();
	}

	public void saveConfig() {
		//to save domain result to extensionSetting
		String content= domainResult.Save();
		callbacks.saveExtensionSetting("domainHunter", content);
	}

	public String loadConfig() {
		return callbacks.loadExtensionSetting("domainHunter");
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
		domainResult.relatedDomainSet =tmpRelatedDomainSet;
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
				e.printStackTrace(stderr);
				continue;
			}
	    }
		 */


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
	}


	/*	public Map<String, Set<String>> spideralltest (String subdomainof, String domainlike) {

		int i = 0;
		while (i<=10) {
			try {
				callbacks.sendToSpider(new URL("http://www.baidu.com/"));
				Thread.sleep(1*60*1000);//单位毫秒，60000毫秒=一分钟
				stdout.println("sleep 1 min");
			} catch (Exception e) {
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

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		List<JMenuItem> list = new ArrayList<JMenuItem>();

		byte context = invocation.getInvocationContext();
		if (context == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TREE) {
			JMenuItem addToDomainHunter = new JMenuItem("^_^ Add To Domain Hunter");
			addToDomainHunter.addActionListener(new addHostToRootDomain(invocation));	
			list.add(addToDomainHunter);
		}
		return list;
	}

	public class addHostToRootDomain implements ActionListener{
		private IContextMenuInvocation invocation;
		public addHostToRootDomain(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				Set<String> domains = new HashSet<String>();
				for(IHttpRequestResponse message:messages) {
					String host = message.getHttpService().getHost();
					domains.add(host);
				}

				domainResult.relatedDomainSet.addAll(domains);
				if (domainResult.autoAddRelatedToRoot == true) {
					domainResult.relatedToRoot();
					domainResult.subDomainSet.addAll(domains);
				}
				showToUI(domainResult);
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}


	//method of IMessageEditorController
	@Override
	public IHttpService getHttpService() {
		return null;
	}

	//method of IMessageEditorController
	@Override
	public byte[] getRequest() {
		return null;
	}

	//method of IMessageEditorController
	@Override
	public byte[] getResponse() {
		return null;
	}

	public void getAllTitle(){
		Set<String> domains = domainResult.getSubDomainSet();
		threadGetTitle = new ThreadGetTitle(domains);
		threadGetTitle.Do();


		saveConfig();
	}

	//////////////////ThreadGetTitle block/////////////
	//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
	class ThreadGetTitle{
		Set<String> domains;
		ExecutorService pes;
		ExecutorService ces;
		public ThreadGetTitle(Set<String> domains) {
			this.domains = domains;
		}

		public void Do(){
			BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();//use to store domains
			BlockingQueue<IHttpRequestResponse> sharedQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
			BlockingQueue<String> lineQueue = new LinkedBlockingQueue<String>();//use to store output---line

			Iterator<String> it = domains.iterator();
			while(it.hasNext()) {
				String domain = it.next();
				domainQueue.add(domain);
			}

			pes = Executors.newFixedThreadPool(10);
			ces = Executors.newFixedThreadPool(10);
			for (int i=0;i<=10;i++) {
				pes.submit(new Producer(domainQueue,sharedQueue,i));
				//stdout.println("~~~~~~~~~~~~~pes  submit~~~~~~~~~~~~~");
			}

			for (int i=0;i<=10;i++) {
				ces.submit(new Consumer(sharedQueue,lineQueue,i));
				//stdout.println("~~~~~~~~~~~~~ces  submit~~~~~~~~~~~~~");
			}

			// shutdown should happen somewhere along with awaitTermination
			/* https://stackoverflow.com/questions/36644043/how-to-properly-shutdown-java-executorservice/36644320#36644320 */
			pes.shutdown();
			ces.shutdown();//will wait already submitted task to finish
			
/*			while(!ces.isTerminated()) {//to wait all threads exit.
				try {
					Thread.sleep(10*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}//单位毫秒，60000毫秒=一分钟
			}*/

			//to save lineEntries
			domainResult.setLineEntries(TitletableModel.getLineEntries());
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

		public Producer(BlockingQueue<String> domainQueue,BlockingQueue<IHttpRequestResponse> sharedQueue,int threadNo) {
			this.threadNo = threadNo;
			this.domainQueue = domainQueue;
			this.sharedQueue = sharedQueue;
		}

		@Override
		public void run() {
			while(true){
				try {
					if (domainQueue.isEmpty()) {
						//stdout.println("Producer break");
						break;
					}

					String host = domainQueue.take();
					//stdout.print(host+" ");
					List<IHttpService> HttpServiceList = new ArrayList();
					HttpServiceList.add(helpers.buildHttpService(host,80,"http"));
					HttpServiceList.add(helpers.buildHttpService(host,443,"https"));

					for (IHttpService item:HttpServiceList) {
						byte[] request = helpers.buildHttpRequest(new URL(item.toString()));
						IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(item, request);
						//stdout.println("messageinfo"+JSONObject.toJSONString(messageinfo));
						//messageinfo can't convert to json by fastjson
						sharedQueue.add(messageinfo);
					}
				} catch (RuntimeException err) {
					//err.printStackTrace();
					//stdout.println("request failed");
				} catch (Exception e) {
					//e.printStackTrace(stderr);
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
		private int threadNo;

		public Consumer (BlockingQueue<IHttpRequestResponse> sharedQueue,BlockingQueue<String> lineQueue,int threadNo) {
			this.sharedQueue = sharedQueue;
			this.lineQueue = lineQueue;
			this.threadNo = threadNo;

			List<LineEntry> lineEntries = new ArrayList<LineEntry>();
			TitletableModel.setLineEntries(lineEntries );//to clear history
		}

		@Override
		public void run() {
			while(true){
				//				if (sharedQueue.isEmpty()) {
				//					stderr.println("Consumer break");
				//					break;
				//				}
				try {
					IHttpRequestResponse messageinfo = sharedQueue.take();

					if (messageinfo.getResponse() ==null) {
						stdout.println("--- skip "+messageinfo.getHttpService().toString()+" due to no response.");
					}else {
						TitletableModel.addNewLineEntry(new LineEntry(messageinfo));
						//stdout.println(new LineEntry(messageinfo).getLineJson());
						stdout.println("+++ "+messageinfo.getHttpService().toString()+" get title done");
					}
					//we don't need to add row to table manually,just call fireTableRowsInserted in TableModel
				} catch (Exception err) {
					err.printStackTrace(stderr);
				}
			}
		}
	}


	public static IBurpExtenderCallbacks getCallbacks() {
		// TODO Auto-generated method stub
		return callbacks;
	}

	//////////////////ThreadGetTitle block/////////////

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