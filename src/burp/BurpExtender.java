package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import GUI.GUI;
import GUI.LineEntryMenuForBurp;
import GUI.ProjectMenu;
import Tools.ToolPanel;
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
	public static final String Extension_Setting_Name_Line_Config = "domain-Hunter-pro-line-config";
	public static final String Extension_Setting_Name_DB_File = "domain-Hunter-pro-db-file-path";
	//用于在配置中记录加载了多少个该插件，以区分显示右键菜单
	private static final Logger log=LogManager.getLogger(BurpExtender.class);
	public static DomainProducer liveAnalysisTread;
	public static BlockingQueue<IHttpRequestResponse> liveinputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo of proxy live
	public static BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo
	public static BlockingQueue<String> subDomainQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> similarDomainQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> relatedDomainQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> emailQueue = new LinkedBlockingQueue<String>();
	public static BlockingQueue<String> packageNameQueue = new LinkedBlockingQueue<String>();
	public static volatile boolean saveExecutedFlag = false;

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

	public static void displayDBFileName() {
		if (!ProjectMenu.isAlone()) {
			if (GUI.currentDBFile !=null){
				String newName = String.format(BurpExtender.getFullExtenderName()+
						" [%s]",GUI.currentDBFile.getName());
				BurpExtender.getCallbacks().setExtensionName(newName); //新插件名称

				GUI.getProjectMenu().AddDBNameMenuItem(GUI.currentDBFile.getName());
				GUI.getProjectMenu().AddDBNameTab(GUI.currentDBFile.getName());

				//gui.repaint();
			}
		}
	}

	public static void saveDBfilepathToExtension() {
		//to save domain result to extensionSetting
		//仅仅存储sqllite数据库的名称,也就是domainResult的项目名称
		if (GUI.currentDBFile != null)
			BurpExtender.getCallbacks().saveExtensionSetting(BurpExtender.Extension_Setting_Name_DB_File, GUI.currentDBFile.getAbsolutePath());
	}

	public static String loadDBfilepathFromExtension() {
		return BurpExtender.getCallbacks().loadExtensionSetting(BurpExtender.Extension_Setting_Name_DB_File);
	}


	//当更换DB文件时，需要清空。虽然不清空最终结果不受影响，但是输出内容会比较奇怪。
	public static void clearQueue() {
		liveinputQueue.clear();
		inputQueue.clear();

		subDomainQueue.clear();
		similarDomainQueue.clear();
		relatedDomainQueue.clear();
		emailQueue.clear();
		packageNameQueue.clear();
	}

	/*
	使用这种方法从Queue中取数据，一来避免了主动clear的操作，二来避免在使用数据后，clear操作之前加进来的数据的丢失。
	 */
	public static void moveQueueToSet(BlockingQueue<String> queue, Set<String> resultSet){
		while (!queue.isEmpty()){
			try {
				String item = queue.take();
				resultSet.add(item);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public static void QueueToResult() {
		//HashSet<String> oldSubdomains = new HashSet<String>();
		CopyOnWriteArraySet<String> oldSubdomains = new CopyOnWriteArraySet<String>();
		//java.util.ConcurrentModificationException 可能同时有其他线程在向subDomainSet中写数据，导致的这个错误。
		//http://ifeve.com/java-copy-on-write/
		//https://www.jianshu.com/p/c5b52927a61a
		DomainManager result= DomainPanel.getDomainResult();
		if (result != null) {
			oldSubdomains.addAll(result.getSubDomainSet());

			moveQueueToSet(subDomainQueue,result.getSubDomainSet());
			moveQueueToSet(similarDomainQueue,result.getSimilarDomainSet());
			moveQueueToSet(relatedDomainQueue,result.getRelatedDomainSet());
			moveQueueToSet(emailQueue,result.getEmailSet());
			moveQueueToSet(packageNameQueue,result.getPackageNameSet());

			//		result.getSubDomainSet().addAll(subDomainQueue);
			//		result.getSimilarDomainSet().addAll(similarDomainQueue);
			//		result.getRelatedDomainSet().addAll(relatedDomainQueue);
			//		result.getEmailSet().addAll(emailQueue);
			//		result.getPackageNameSet().addAll(packageNameQueue);

			HashSet<String> newSubdomains = new HashSet<String>();
			newSubdomains.addAll(result.getSubDomainSet());

			newSubdomains.removeAll(oldSubdomains);
			result.getNewAndNotGetTitleDomainSet().addAll(newSubdomains);

			if (newSubdomains.size()>0){
				stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~",newSubdomains.size()));
				stdout.println(String.join(System.lineSeparator(), newSubdomains));
				DomainPanel.autoSave();//进行一次主动保存
			}
		}
	}

	//插件加载过程中需要做的事
	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
	{
		BurpExtender.callbacks = callbacks;

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
		String dbFilePath = loadDBfilepathFromExtension();
		System.out.println("Database FileName From Extension Setting: "+dbFilePath);
		if (dbFilePath != null && dbFilePath.endsWith(".db")) {
			gui.LoadData(dbFilePath);
			displayDBFileName();
		}

		gui.getToolPanel().loadConfigToGUI();

		liveAnalysisTread = new DomainProducer(BurpExtender.liveinputQueue,BurpExtender.subDomainQueue,
				BurpExtender.similarDomainQueue,BurpExtender.relatedDomainQueue,
				BurpExtender.emailQueue,BurpExtender.packageNameQueue,9999);//必须是9999，才能保证流量进程不退出。
		liveAnalysisTread.start();
	}


	@Override
	public void extensionUnloaded() {
		gui.getProjectMenu().remove();
		QueueToResult();
		if (TitlePanel.threadGetTitle != null) {
			TitlePanel.threadGetTitle.interrupt();//maybe null
		}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源

		gui.getToolPanel().saveConfigToDisk();
		DomainPanel.autoSave();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
		saveDBfilepathToExtension();
	}



	//ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		return (ExtenderName.replaceAll(" ",""));
	}
	@Override
	public Component getUiComponent() {
		return gui.getContentPane();
	}

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		if (ToolPanel.DisplayContextMenuOfBurp.isSelected()) {
			return new LineEntryMenuForBurp().createMenuItemsForBurp(invocation);
		}else {
			return null;
		}
	}

	@Override
	public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
		if (toolFlag == IBurpExtenderCallbacks.TOOL_PROXY && !messageIsRequest) {
			liveinputQueue.add(messageInfo);
		}

		if ((new Date().getMinutes()) % 5 == 0 && saveExecutedFlag == false) {
			//这个条件可能导致同一分钟内可能被执行多次！所以加了一个flag
			saveExecutedFlag = true;
			QueueToResult();
		}else {
			saveExecutedFlag = false;
		}
	}

	public static void main(String[] args){
		while (true){
			int aaa = new Date().getMinutes();
			if (aaa % 5 == 0) {
				System.out.println(aaa);
			}
		}
	}
}