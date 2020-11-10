package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import GUI.GUI;
import GUI.LineEntryMenuForBurp;
import bsh.This;
import domain.DomainManager;
import domain.DomainPanel;
import domain.DomainProducer;
import title.TitlePanel;

public class BurpExtender implements IBurpExtender, ITab, IExtensionStateListener,IContextMenuFactory,IHttpListener{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static IBurpExtenderCallbacks callbacks;
	private static PrintWriter stdout;
	private static PrintWriter stderr;
	private static String ExtenderName = "Domain Hunter Pro";
	private static String Version =  This.class.getPackage().getImplementationVersion();
	private static String Author = "by bit4woo";
	private static String github = "https://github.com/bit4woo/domain_hunter_pro";
	private static GUI gui;
	public static final String Extension_Setting_Name_DB_File = "domain-Hunter-pro-db-path";
	public static final String Extension_Setting_Name_Line_Config = "domain-Hunter-pro-line-config";
	private static final Logger log=LogManager.getLogger(BurpExtender.class);

	public static PrintWriter getStdout() {
		//不同的时候调用这个参数，可能得到不同的值
		try{
			stdout = new PrintWriter(callbacks.getStdout(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
		}
		return stdout;
	}

	public static PrintWriter getStderr() {
		try{
			stderr = new PrintWriter(callbacks.getStderr(), true);
		}catch (Exception e){
			stderr = new PrintWriter(System.out, true);
		}
		return stderr;
	}

	public static IBurpExtenderCallbacks getCallbacks() {
		return callbacks;
	}

	public static String getGithub() {
		return github;
	}

	public static GUI getGui() {
		return gui;
	}

	public static String getExtenderName() {
		return ExtenderName;
	}

	//name+version+author
	public static String getFullExtenderName(){
		return ExtenderName+" "+Version+" "+Author;
	}

	private IExtensionHelpers helpers;

	//插件加载过程中需要做的事
	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
	{
		BurpExtender.callbacks = callbacks;
		helpers = callbacks.getHelpers();

		getStdout();
		getStderr();
		stdout.println(getFullExtenderName());
		stdout.println(github);

		callbacks.setExtensionName(getFullExtenderName()); //插件名称
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);
		callbacks.registerHttpListener(this);//主动根据流量收集信息

		gui = new GUI();

		SwingUtilities.invokeLater(new Runnable()
		{//create GUI
			public void run()
			{
				BurpExtender.callbacks.addSuiteTab(BurpExtender.this); //这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由GUI()函数初始化。
				//如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
			}
		});

		//recovery save domain results from extensionSetting
		String content = callbacks.loadExtensionSetting(Extension_Setting_Name_DB_File);//file name of db file
		System.out.println(content);
		if (content != null && content.endsWith(".db")) {
			gui.LoadData(content);
		}

		gui.getToolPanel().loadConfig();

	}

	@Override
	public void extensionUnloaded() {
		if (TitlePanel.threadGetTitle != null) {
			TitlePanel.threadGetTitle.interrupt();//maybe null
		}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源

		gui.saveDBfilepathToExtension();
		gui.getProjectMenu().remove();

		gui.getToolPanel().saveConfig();
		DomainPanel.autoSave();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
	}

	//ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		return ("Domain Hunter Pro");
	}
	@Override
	public Component getUiComponent() {
		return gui.getContentPane();
	}

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		return new LineEntryMenuForBurp().createMenuItemsForBurp(invocation);
	}

	@Override
	public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {

		//		Date now = new Date();
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			//using SwingWorker to void slow down proxy http response time.

			@Override
			protected Map doInBackground() throws Exception {
				findDomainInTraffic(toolFlag,messageIsRequest,messageInfo);
				return null;
			}
			@Override
			protected void done() {
			}
		};
		worker.execute();
		//findDomainInTraffic(toolFlag,messageIsRequest,messageInfo);
		//		Date now1 = new Date();
		//		stderr.println("takes time to finish find domain: "+(now1.getTime()-now.getTime()));

	}


	public void findDomainInTraffic(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo){
		boolean dataChanged =false;
		if (toolFlag == IBurpExtenderCallbacks.TOOL_PROXY) {
			try {
				Getter getter = new Getter(helpers);
				if (messageIsRequest) {
					IHttpService httpservice = messageInfo.getHttpService();
					String Host = httpservice.getHost();

					int hostType = DomainPanel.domainResult.domainType(Host);
					if (hostType == DomainManager.SUB_DOMAIN)
					{	
						if (!DomainPanel.domainResult.getSubDomainSet().contains(Host) 
								&& !DomainPanel.domainResult.getNewAndNotGetTitleDomainSet().contains(Host)) {
							DomainPanel.domainResult.getNewAndNotGetTitleDomainSet().add(Host);
							stdout.println("new domain found: "+ Host);
							dataChanged = true;
						}
					}else if (hostType == DomainManager.SIMILAR_DOMAIN) {
						if (!DomainPanel.domainResult.getSimilarDomainSet().contains(Host)) {
							DomainPanel.domainResult.getSimilarDomainSet().add(Host);
							dataChanged = true;
						}
					}
				}else {//response

					IHttpService httpservice = messageInfo.getHttpService();
					String urlString = getter.getFullURL(messageInfo).getFile();

					String Host = httpservice.getHost();

					int hostType = DomainPanel.domainResult.domainType(Host);
					if (hostType != DomainManager.USELESS) {//grep domains from response and classify
						if (urlString.endsWith(".gif") ||urlString.endsWith(".jpg")
								|| urlString.endsWith(".png") ||urlString.endsWith(".css")||urlString.endsWith(".woff")) {

						}else {
							dataChanged = classifyDomains(messageInfo);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}

		if (dataChanged) {
			DomainPanel.autoSave();
		}
	}

	public boolean classifyDomains(IHttpRequestResponse messageinfo) {
		boolean dataChanged = false;
		byte[] response = messageinfo.getResponse();
		if (response != null) {
			Set<String> domains = DomainProducer.grepDomain(new String(response));
			for (String domain:domains) {
				int type = DomainPanel.domainResult.domainType(domain);
				if (type == DomainManager.SUB_DOMAIN)
				{
					if (!DomainPanel.domainResult.getSubDomainSet().contains(domain)
							&& !DomainPanel.domainResult.getNewAndNotGetTitleDomainSet().contains(domain)) {
						DomainPanel.domainResult.getNewAndNotGetTitleDomainSet().add(domain);
						stdout.println("new domain found: "+ domain);
						dataChanged = true;
					}
				}else if (type == DomainManager.SIMILAR_DOMAIN) {
					if (!DomainPanel.domainResult.getSimilarDomainSet().contains(domain)){
						DomainPanel.domainResult.getSimilarDomainSet().add(domain);
						dataChanged = true;
					}

				}else if (type == DomainManager.PACKAGE_NAME) {
					if (!DomainPanel.domainResult.getPackageNameSet().contains(domain)){
						DomainPanel.domainResult.getPackageNameSet().add(domain);
						dataChanged = true;
					}
				}
			}
		}
		return dataChanged;
	}
}