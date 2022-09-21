package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import GUI.GUIMain;
import GUI.LineEntryMenuForBurp;
import GUI.ProjectMenu;
import Tools.ToolPanel;
import bsh.This;
import config.ConfigPanel;
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
	private static GUIMain gui;
	public static final String Extension_Setting_Name_Line_Config = "domain-Hunter-pro-line-config";
	private static final String Extension_Setting_Name_DB_File = "DomainHunterProDbFilePath";

	private static final Logger log=LogManager.getLogger(BurpExtender.class);
	public static DomainProducer liveAnalysisTread;
	public static BlockingQueue<IHttpRequestResponse> liveinputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo of proxy live
	public static BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo
	public static Set<String> httpsChecked = new CopyOnWriteArraySet<>();
	//temp variable to identify checked https用于记录已经做过HTTPS证书信息获取的httpService

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

	public static GUIMain getGui() {
		return gui;
	}

	public static String getExtenderName() {
		return ExtenderName;
	}

	//name+version+author
	public static String getFullExtenderName(){
		return ExtenderName+" "+Version+" "+Author;
	}


	@Deprecated
	public static void saveDBfilepathToExtension() {
		//to save domain result to extensionSetting
		//仅仅存储sqllite数据库的名称,也就是domainResult的项目名称
		if (GUIMain.getCurrentDBFile() != null) {
			String dbfilepath = GUIMain.getCurrentDBFile().getAbsolutePath();
			stdout.println("Saving Current DB File Path To Disk: "+dbfilepath);
			System.out.println("Loaded DB File Path From Disk: "+dbfilepath);
			callbacks.saveExtensionSetting(Extension_Setting_Name_DB_File, dbfilepath);
		}
	}

	/**
	 * 很多时候都获取不到数据，都是null值！有bug
	 * @return
	 */
	@Deprecated
	public static String loadDBfilepathFromExtension() {
		String dbfilepath = callbacks.loadExtensionSetting(Extension_Setting_Name_DB_File);
		if (dbfilepath == null) {
			//dbfilepath = LineConfig.loadFromDisk().getDbfilepath();
		}
		stdout.println("Loaded DB File Path From Disk: "+dbfilepath);
		System.out.println("Loaded DB File Path From Disk: "+dbfilepath);
		return dbfilepath;
	}

	public void startLiveCapture(){
		liveAnalysisTread = new DomainProducer(BurpExtender.liveinputQueue,9999);//必须是9999，才能保证流量进程不退出。
		liveAnalysisTread.start();
	}

	public void stopLiveCapture(){
		if (null != liveAnalysisTread){
			liveAnalysisTread.interrupt();
			//9999线程只能这样结束，不受stopflag的影响
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

		if (TitlePanel.threadGetTitle != null) {
			TitlePanel.threadGetTitle.interrupt();//maybe null
			//以前项目的数据可能加入到当前项目！当以前项目gettitle的线程未结束时
		}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源

		callbacks.setExtensionName(getFullExtenderName()); //插件名称
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);
		callbacks.registerHttpListener(this);//主动根据流量收集信息

		gui = new GUIMain();

		SwingUtilities.invokeLater(new Runnable()
		{//create GUI
			public void run()
			{
				BurpExtender.callbacks.addSuiteTab(BurpExtender.this); 
				GUIMain.setProjectMenu(new ProjectMenu(gui));
				GUIMain.getProjectMenu().Add();
				//这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由GUI()函数初始化。
				//如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
			}
		});
		String projectConfigFile = RecentModel.fetchRecent();//返回值可能为null
		gui.getConfigPanel().loadConfigToGUI(projectConfigFile);//包含db文件的加载
		startLiveCapture();
	}

	@Override
	public void extensionUnloaded() {
		try {//避免这里错误导致保存逻辑的失效
			GUIMain.getProjectMenu().remove();
			stopLiveCapture();
			if (TitlePanel.threadGetTitle != null) {
				TitlePanel.threadGetTitle.interrupt();//maybe null
				inputQueue.clear();
				liveinputQueue.clear();
				httpsChecked.clear();
			}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}

		
		try {
			if (null == DomainPanel.getDomainResult()) return;//有数据才弹对话框指定文件位置。
			if (DomainPanel.getDomainResult().isEmpty()) return;
			
			DomainPanel.saveDomainDataToDB();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
			gui.getConfigPanel();
			String configFilePath = ConfigPanel.getLineConfig().saveToDisk();//包含db文件位置
			RecentModel.saveRecent(configFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		if (ConfigPanel.DisplayContextMenuOfBurp.isSelected()) {
			return new LineEntryMenuForBurp().createMenuItemsForBurp(invocation);
		}else {
			return null;
		}
	}
	
	/**
	 * 包含extender，这样get title中的流量也可以收集。而且IP的get title也能通过证书信息判断其归属；
	 * 其实当通过浏览器打开URL时，也能完成这个收集过程，还是先移除extender，避免get title过程卡死。
	 */
	@Override
	public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
		if ((toolFlag == IBurpExtenderCallbacks.TOOL_PROXY || 
				toolFlag == IBurpExtenderCallbacks.TOOL_INTRUDER ||
				toolFlag == IBurpExtenderCallbacks.TOOL_REPEATER) 
				&& !messageIsRequest) {
			liveinputQueue.add(messageInfo);
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