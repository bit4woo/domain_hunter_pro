package burp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class BurpExtender extends GUI implements IBurpExtender, ITab, IExtensionStateListener,IContextMenuFactory, IMessageEditorController{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static  IBurpExtenderCallbacks callbacks;
	private IExtensionHelpers helpers;
	private ThreadGetTitle threadGetTitle;
	private static List<LineEntry> backupLineEntries;

	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
	{

		stdout = new PrintWriter(callbacks.getStdout(), true);
		stderr = new PrintWriter(callbacks.getStderr(), true);
		stdout.println(ExtenderName);
		stdout.println(github);
		BurpExtender.callbacks = callbacks;
		helpers = callbacks.getHelpers();
		callbacks.setExtensionName(ExtenderName); //插件名称
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);
		addMenuTab();

		//add table and tablemodel to GUI
		titleTableModel = new LineTableModel(this);
		titleTable = new LineTable(titleTableModel,this);
		TitlePanel.add(titleTable.getSplitPane(), BorderLayout.CENTER);

		//recovery save domain results from extensionSetting
		loadConfigFromExtension();

	}

	@Override
	public void extensionUnloaded() {
		threadGetTitle.stopThreads();
		saveConfigToExtension();
	}

	public static IBurpExtenderCallbacks getCallbacks() {
		return callbacks;
	}

	//重写GUI中的方法，以确保能获取到title面板中的配置
	@Override
	public String getConfig(boolean includeTitle) {
		if(includeTitle) {
			List<String> LineJsons = titleTableModel.getLineJsons();
			domainResult.setLineJsons(LineJsons);
		}else {
			domainResult.setLineJsons(new ArrayList<String>());
			domainResult.setHistoryLineJsons(new ArrayList<String>());
		}

		String content= domainResult.Save();
		return content;
	}

	//重写GUI中的方法，以确保能获取到title面板中的配置
	@Override
	public DomainObject loadConfigAndShow(String config) {
		if (config!=null) {
			domainResult = domainResult.Open(config);
			showToDomainUI(domainResult);
			showToTitleUI();
		}
		return domainResult;
	}

	@Override
	public void saveConfigToExtension() {
		//to save domain result to extensionSetting
		stdout.println("saving config to extension setting");
		callbacks.saveExtensionSetting("domainHunter", getConfig(true));
		stdout.println("config saved to extension setting");
	}

	public void loadConfigFromExtension() {//the most important thing is to recovery the domainResult Object 
		stdout.println("Loading config from extension setting");
		String content = callbacks.loadExtensionSetting("domainHunter");
		loadConfigAndShow(content);
		stdout.println("config Loaded from extension setting");
	}

	public void showToTitleUI() {
		titleTableModel.setLineEntries(new ArrayList<LineEntry>());//clear

		List<String> lineJsons = domainResult.getLineJsons();
		Set<String> lineJsonSet = new HashSet<>(lineJsons);//去重
		for (String line:lineJsonSet) {
			LineEntry lineObject = new LineEntry().FromJson(line);
			titleTableModel.addNewLineEntry(lineObject);
		}
	}

	@Override
	public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords){
		IHttpRequestResponse[] messages = callbacks.getSiteMap(null);
		new ThreadSearhDomain(Arrays.asList(messages)).Do();
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
				BurpExtender.callbacks.addSuiteTab(BurpExtender.this); //这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由CGUI()函数初始化。
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
		
		JMenuItem runWithSamePathItem = new JMenuItem("^_^ Run Targets with this path");
		runWithSamePathItem.addActionListener(new runWithSamePath(invocation));
		list.add(runWithSamePathItem);
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
				showToDomainUI(domainResult);
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}

	
	public class runWithSamePath implements ActionListener{
		private IContextMenuInvocation invocation;
		public runWithSamePath(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			
	        String responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
	        while(responseKeyword.trim().equals("")){
	        	responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
	        }
	        responseKeyword = responseKeyword.trim();
			final String keyword = responseKeyword;
			
			
			RunnerGUI frame = new RunnerGUI();
			frame.setVisible(true);
			frame.setTitle("Runner");

			LineTableModel runnerTableModel = new LineTableModel(BurpExtender.this);
			LineTable runnerTable = new LineTable(runnerTableModel,BurpExtender.this);
			frame.getRunnerPanel().add(runnerTable.getSplitPane(), BorderLayout.CENTER);
			//frame.getRootPane().add(runnerTable.getSplitPane(), BorderLayout.CENTER);
			
			
			SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
		    	//using SwingWorker to prevent blocking burp main UI.

		        @Override
		        protected Map doInBackground() throws Exception {
		        	
		        	IHttpRequestResponse[] messages = invocation.getSelectedMessages();
					IHttpRequestResponse currentmessage =messages[0];
					byte[] request = currentmessage.getRequest();

					for(LineEntry line:titleTableModel.getLineEntries()) {
						String protocol = line.getProtocol();
						boolean useHttps =false;
						if (protocol.equalsIgnoreCase("https")) {
							useHttps =true;
						}
						IHttpService httpService = helpers.buildHttpService(line.getHost(), line.getPort(), useHttps);
						IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(httpService, request);
						Getter getter = new Getter(helpers);
						String body = new String(getter.getBody(false, messageinfo));

						if (body != null && body.toLowerCase().contains(keyword)) {
							runnerTableModel.addNewLineEntry(new LineEntry(messageinfo,false,false,"Runner"));
						}
					}
					return null;
		        }
		        @Override
		        protected void done() {
		        }
		    };      
		    worker.execute();
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

	public List<String> getAllTitle(){
		Set<String> domains = domainResult.getSubDomainSet();

		//remove domains in black list
		domains.removeAll(domainResult.getBlackDomainSet());

		//backup to history
		domainResult.setHistoryLineJsons(domainResult.getLineJsons());
		//clear LineJsons
		domainResult.setLineJsons(new ArrayList<String>());
		//clear tableModel
		titleTableModel.setLineEntries(new ArrayList<LineEntry>());//clear

		threadGetTitle = new ThreadGetTitle(domains);
		List<String> result = threadGetTitle.Do();
		return result;
	}

	@Override
	public List<String> getExtendTitle(){
		Set<String> extendIPSet = titleTableModel.GetExtendIPSet();
		stdout.println(extendIPSet.size()+" extend IP Address founded"+extendIPSet);
		threadGetTitle = new ThreadGetTitle(extendIPSet);
		List<String> result = threadGetTitle.Do();
		return result;
	}
	
	@Override
	public String getSubnet(){
		Set<String> subnets = titleTableModel.GetSubnets();
		return String.join(System.lineSeparator(), subnets);
	}

	public String digStatus() {
		return titleTableModel.getStatusSummary();
	}
	
	@Override
	public void showSearchResult(String keyword) {
		String searchkeyword = textFieldSearch.getText();
		titleTable.search(searchkeyword);
	}

	public static List<LineEntry> getBackupLineEntries() {
		return backupLineEntries;
	}

	public void setBackupLineEntries(List<LineEntry> backupLineEntries) {
		BurpExtender.backupLineEntries = backupLineEntries;
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