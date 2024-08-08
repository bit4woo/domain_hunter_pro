package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import config.DataLoadManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bit4woo.utilbox.burp.HelperPlus;

import GUI.GUIMain;
import bsh.This;
import config.ConfigManager;
import config.ConfigName;
import config.ConfigPanel;

public class BurpExtender implements IBurpExtender, ITab, IExtensionStateListener, IContextMenuFactory, IHttpListener {
	/**
	 *
	 */
	private static IBurpExtenderCallbacks callbacks;
	private static IExtensionHelpers helpers;
	private static HelperPlus helperPlus;

	private static PrintWriter stdout;
	private static PrintWriter stderr;
	private static String ExtenderName = "Domain Hunter Pro";
	private static String Version = This.class.getPackage().getImplementationVersion();
	private static String Author = "by bit4woo";
	private static String github = "https://github.com/bit4woo/domain_hunter_pro";
	private static final String Extension_Setting_Name_DB_File = "DomainHunterProDbFilePath";

	private static GUIMain gui;
	private static DataLoadManager dataLoadManager;
	// 通常情况下，当使用了静态变量，多个实例之间对静态变量的修改会相互影响，见src/test/StaticFieldTest.java
	// 然而为了快捷访问某些对象（通过类名称访问很直接），又会常常使用静态变量。
	// 好消息是：burp的插件机制是每个插件实例都由独立的类加载器加载，保证了插件之间的相互隔离。也就是说，使用了静态变量也没有关系。
	// 后续解决方案：创建一个context对象，各个对象构造函数都传递这个context对象。当要访问某个对象时，就通过context对象进行访问。

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

	public static IExtensionHelpers getHelpers() {
		return helpers;
	}

	public static HelperPlus getHelperPlus() {
		return helperPlus;
	}

	public static GUIMain getGui() {
		return gui;
	}

	public static DataLoadManager getDataLoadManager(){
		return dataLoadManager;
	}
	public static String getExtenderName() {
		return ExtenderName;
	}

	//name+version+author
	public static String getFullExtenderName() {
		return ExtenderName + " " + Version + " " + Author;
	}


	//插件加载过程中需要做的事
	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
		BurpExtender.callbacks = callbacks;
		BurpExtender.helpers = callbacks.getHelpers();
		BurpExtender.helperPlus = new HelperPlus(helpers);

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
				gui = new GUIMain();
				dataLoadManager = DataLoadManager.loadFromDisk(gui);
				callbacks.addSuiteTab(BurpExtender.this);
				//这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由GUI()函数初始化。
				//如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
				dataLoadManager.loadDbAndConfig();
				gui.startLiveCapture();
			}
		});
	}

	@Override
	public void extensionUnloaded() {
		
		try {
			if (dataLoadManager != null) {
				dataLoadManager.unloadDbfile(null);
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		
		try {
			gui.stopLiveCapture();
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}

		try {//避免这里错误导致保存逻辑的失效
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
			if (dataLoadManager != null) {
				dataLoadManager.saveCurrentConfig(null);
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
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
		if (ConfigManager.getBooleanConfigByKey(ConfigName.showBurpMenu)) {
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
				&& !messageIsRequest && gui != null) {
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