package burp;

import java.awt.Component;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

public class BurpExtender implements IBurpExtender, ITab, IExtensionStateListener,IContextMenuFactory{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static IBurpExtenderCallbacks callbacks;
	private static PrintWriter stdout;
	private static PrintWriter stderr;
	private static String ExtenderName = "Domain Hunter Pro by bit4woo";
	private static String github = "https://github.com/bit4woo/domain_hunter_pro";
	private static GUI gui;

	private static void flushStd(){
		try{
			stdout = new PrintWriter(callbacks.getStdout(), true);
			stderr = new PrintWriter(callbacks.getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	public static PrintWriter getStdout() {
		flushStd();//不同的时候调用这个参数，可能得到不同的值
		return stdout;
	}

	public static PrintWriter getStderr() {
		flushStd();
		return stderr;
	}

	public static IBurpExtenderCallbacks getCallbacks() {
		return callbacks;
	}

	public static String getExtenderName() {
		return ExtenderName;
	}

	public static String getGithub() {
		return github;
	}

	public static GUI getGui() {
		return gui;
	}

	private IExtensionHelpers helpers;






	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
	{
		flushStd();
		stdout.println(ExtenderName);
		stdout.println(github);
		BurpExtender.callbacks = callbacks;
		helpers = callbacks.getHelpers();
		callbacks.setExtensionName(ExtenderName); //插件名称
		callbacks.registerExtensionStateListener(this);
		callbacks.registerContextMenuFactory(this);

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
		String content = callbacks.loadExtensionSetting("domainHunterpro");//file name of db file
		System.out.println(content);
		if (content != null && content.endsWith(".db")) {
			gui.LoadData(content);
		}
	}

	@Override
	public void extensionUnloaded() {
		if (gui.getTitlePanel().getThreadGetTitle() != null) {
			gui.getTitlePanel().getThreadGetTitle().stopThreads();//maybe null
		}//必须要先结束线程，否则获取数据的操作根本无法结束，因为线程一直通过sync占用资源
		if (DomainPanel.threadBruteDomain != null){
			DomainPanel.threadBruteDomain.stopThreads();
		}
		if (DomainPanel.threadBruteDomain2 != null){
			DomainPanel.threadBruteDomain2.stopThreads();
		}
		gui.saveDBfilepathToExtension();
		gui.getProjectMenu().remove();
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

	

}