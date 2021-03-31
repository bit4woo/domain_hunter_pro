package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import GUI.GUI;
import GUI.LineEntryMenuForBurp;
import Tools.ToolPanel;
import bsh.This;
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
	private IExtensionHelpers helpers;

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
		HashSet<String> oldSubdomains = new HashSet<String>();
		oldSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		moveQueueToSet(subDomainQueue,DomainPanel.getDomainResult().getSubDomainSet());
		moveQueueToSet(similarDomainQueue,DomainPanel.getDomainResult().getSimilarDomainSet());
		moveQueueToSet(relatedDomainQueue,DomainPanel.getDomainResult().getRelatedDomainSet());
		moveQueueToSet(emailQueue,DomainPanel.getDomainResult().getEmailSet());
		moveQueueToSet(packageNameQueue,DomainPanel.getDomainResult().getPackageNameSet());

		//		DomainPanel.getDomainResult().getSubDomainSet().addAll(subDomainQueue);
		//		DomainPanel.getDomainResult().getSimilarDomainSet().addAll(similarDomainQueue);
		//		DomainPanel.getDomainResult().getRelatedDomainSet().addAll(relatedDomainQueue);
		//		DomainPanel.getDomainResult().getEmailSet().addAll(emailQueue);
		//		DomainPanel.getDomainResult().getPackageNameSet().addAll(packageNameQueue);

		HashSet<String> newSubdomains = new HashSet<String>();
		newSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		newSubdomains.removeAll(oldSubdomains);
		DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().addAll(newSubdomains);

		if (newSubdomains.size()>0){
			stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~",newSubdomains.size()));
			stdout.println(String.join(System.lineSeparator(), newSubdomains));
			DomainPanel.autoSave();//进行一次主动保存
		}
	}

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

		gui.getToolPanel().loadConfigToGUI();

		liveAnalysisTread = new DomainProducer(BurpExtender.liveinputQueue,BurpExtender.subDomainQueue,
				BurpExtender.similarDomainQueue,BurpExtender.relatedDomainQueue,
				BurpExtender.emailQueue,BurpExtender.packageNameQueue,9999);//必须是9999，才能保证流量进程不退出。
		liveAnalysisTread.start();
	}

	@Override
	public void extensionUnloaded() {
		QueueToResult();
		if (TitlePanel.threadGetTitle != null) {
			TitlePanel.threadGetTitle.interrupt();//maybe null
		}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源

		gui.saveDBfilepathToExtension();
		gui.getProjectMenu().remove();

		gui.getToolPanel().saveConfigToDisk();
		DomainPanel.autoSave();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
	}

	//ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		return (ExtenderName);
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

		if ((new Date().getMinutes()) % 5 == 0) {
			QueueToResult();
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