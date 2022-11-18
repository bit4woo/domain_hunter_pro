package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import GUI.GUIMain;
import GUI.LineEntryMenuForBurp;
import bsh.This;
import config.ConfigPanel;

public class BurpExtender implements IBurpExtender, ITab, IExtensionStateListener, IContextMenuFactory, IHttpListener {
	/**
	 *
	 */
	private static IBurpExtenderCallbacks callbacks;
	private static PrintWriter stdout;
	private static PrintWriter stderr;
	private static String ExtenderName = "Domain Hunter Pro";
	private static String Version = This.class.getPackage().getImplementationVersion();
	private static String Author = "by bit4woo";
	private static String github = "https://github.com/bit4woo/domain_hunter_pro";
	private static final String Extension_Setting_Name_Line_Config = "domain-Hunter-pro-line-config";
	private static final String Extension_Setting_Name_DB_File = "DomainHunterProDbFilePath";
	private static final Logger log = LogManager.getLogger(BurpExtender.class);

	private GUIMain gui;




	public static PrintWriter getStdout() {
		//不同的时候调用这个参数，可能得到不同的值
		try {
			stdout = new PrintWriter(callbacks.getStdout(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
		}
		return stdout;
	}

	public static PrintWriter getStderr() {
		try {
			stderr = new PrintWriter(callbacks.getStderr(), true);
		} catch (Exception e) {
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

	public GUIMain getGui() {
		return gui;
	}

	public static String getExtenderName() {
		return ExtenderName;
	}

	//name+version+author
	public static String getFullExtenderName() {
		return ExtenderName + " " + Version + " " + Author;
	}


	@Deprecated
	public void saveDBfilepathToExtension() {
		//to save domain result to extensionSetting
		//仅仅存储sqllite数据库的名称,也就是domainResult的项目名称
		if (gui.getCurrentDBFile() != null) {
			String dbfilepath = gui.getCurrentDBFile().getAbsolutePath();
			stdout.println("Saving Current DB File Path To Disk: " + dbfilepath);
			System.out.println("Loaded DB File Path From Disk: " + dbfilepath);
			callbacks.saveExtensionSetting(Extension_Setting_Name_DB_File, dbfilepath);
		}
	}

	/**
	 * 很多时候都获取不到数据，都是null值！有bug
	 *
	 * @return
	 */
	@Deprecated
	public static String loadDBfilepathFromExtension() {
		String dbfilepath = callbacks.loadExtensionSetting(Extension_Setting_Name_DB_File);
		if (dbfilepath == null) {
			//dbfilepath = LineConfig.loadFromDisk().getDbfilepath();
		}
		stdout.println("Loaded DB File Path From Disk: " + dbfilepath);
		System.out.println("Loaded DB File Path From Disk: " + dbfilepath);
		return dbfilepath;
	}


	//插件加载过程中需要做的事
	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
		BurpExtender.callbacks = callbacks;

		getStdout();
		getStderr();
		stdout.println(getFullExtenderName());
		stdout.println(github);

		callbacks.setExtensionName(getFullExtenderName()); //插件名称
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);
		callbacks.registerHttpListener(this);//主动根据流量收集信息

		SwingUtilities.invokeLater(new Runnable() {//create GUI
			public void run() {
				gui = new GUIMain(BurpExtender.this);
				callbacks.addSuiteTab(BurpExtender.this);
				//这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由GUI()函数初始化。
				//如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
				gui.LoadData();
				gui.startLiveCapture();
			}
		});
	}

	@Override
	public void extensionUnloaded() {
		gui.stopLiveCapture();

		try {//避免这里错误导致保存逻辑的失效
			gui.getProjectMenu().remove();
			if (gui.getTitlePanel().getThreadGetTitle() != null) {
				gui.getTitlePanel().getThreadGetTitle().interrupt();//maybe null
				gui.getInputQueue().clear();
				gui.getLiveinputQueue().clear();
				gui.getHttpsChecked().clear();
			}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}

		try {
			if (null == gui.getDomainPanel().getDomainResult()) return;//有数据才弹对话框指定文件位置。
			gui.getDomainPanel().saveDomainDataToDB();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
			String configFilePath = gui.getConfigPanel().getLineConfig().saveToDisk();//包含db文件位置
			RecentModel.saveRecent(configFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		return (ExtenderName.replaceAll(" ", ""));
	}

	@Override
	public Component getUiComponent() {
		return gui.getContentPane();
	}

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		if (ConfigPanel.DisplayContextMenuOfBurp.isSelected()) {
			return new LineEntryMenuForBurp(gui).createMenuItemsForBurp(invocation);
		} else {
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
			gui.getLiveinputQueue().add(messageInfo);
		}
	}

	public static void main(String[] args) {
		while (true) {
			int aaa = new Date().getMinutes();
			if (aaa % 5 == 0) {
				System.out.println(aaa);
			}
		}
	}
}