package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.*;

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

	public static void flushStd(){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
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
		List<JMenuItem> list = new ArrayList<JMenuItem>();
		/*
		这里的逻辑有3重：
		1、仅将域名添加到子域名和target中，
		2、将请求添加到title列表中，包含1的行为
		3、对某一个请求添加comment，如果请求不存在，放弃；添加请求包含步骤1和2的行为。
		 */
//		byte context = invocation.getInvocationContext();
//		if (context == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TREE) {
//
//		}

		JMenuItem runWithSamePathItem = new JMenuItem("^_^[Domain Hunter] Run Targets with this path");
		runWithSamePathItem.addActionListener(new runWithSamePath(invocation));
		list.add(runWithSamePathItem);

		JMenuItem addDomainToDomainHunter = new JMenuItem("^_^[Domain Hunter] Add Domain");
		addDomainToDomainHunter.addActionListener(new addHostToRootDomain(invocation));
		list.add(addDomainToDomainHunter);

		JMenuItem addRequestToDomainHunter = new JMenuItem("^_^[Domain Hunter] Add Request");
		addRequestToDomainHunter.addActionListener(new addRequestToHunter(invocation));
		list.add(addRequestToDomainHunter);

		JMenuItem addCommentToDomainHunter = new JMenuItem("^_^[Domain Hunter] Add Comment");
		addCommentToDomainHunter.addActionListener(new addComment(invocation));
		list.add(addCommentToDomainHunter);

		list.add(createLevelMenu(invocation));

		JMenuItem addToTitleItem = new JMenuItem("^_^ Add TO Title");
		addToTitleItem.addActionListener(new addToTitle(invocation));
		//list.add(addToTitleItem);



		return list;
	}

	public class addHostToRootDomain implements ActionListener{
		private IContextMenuInvocation invocation;
		addHostToRootDomain(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				addToDomain(messages);
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}

	public class addRequestToHunter implements ActionListener{
		private IContextMenuInvocation invocation;
		addRequestToHunter(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				addToRequest(messages);
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}


	public class runWithSamePath implements ActionListener{
		private IContextMenuInvocation invocation;
		runWithSamePath(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				//using SwingWorker to prevent blocking burp main UI.

				@Override
				protected Map doInBackground() throws Exception {

					IHttpRequestResponse[] messages = invocation.getSelectedMessages();
					IHttpRequestResponse currentmessage =messages[0];
					byte[] request = currentmessage.getRequest();

					RunnerGUI runnergui = new RunnerGUI(request);
					return null;
				}
				@Override
				protected void done() {
				}
			};
			worker.execute();
		}
	}

	public class addComment implements ActionListener{
		private IContextMenuInvocation invocation;
		addComment(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			//还是要简化逻辑，如果找不到就不执行！
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();

				String urlString = helpers.analyzeRequest(messages[0]).getUrl().toString();
				LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(urlString);
				if (entry == null) {
					String shortUrlString = messages[0].getHttpService().toString();
					entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrlString);
				}

				if (entry != null) {
					addCommentForLine(entry);
				}
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}


	@Deprecated
	public class addToTitle implements ActionListener{
		private IContextMenuInvocation invocation;
		addToTitle(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				for (IHttpRequestResponse message:messages) {
					addToTitle(helpers,message);
				}
				addToDomain(messages);
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}

	@Deprecated
	public static LineEntry addToTitle(IExtensionHelpers helpers,IHttpRequestResponse messageInfo) throws Exception {
		//burp不允许在这个事件中发送请求
		//java.lang.RuntimeException: java.lang.RuntimeException: Extensions should not make HTTP requests in the Swing event dispatch thread
		String shortUrlString = messageInfo.getHttpService().toString();
		String host = messageInfo.getHttpService().getHost();
		LineEntry entry;
		if (Commons.isValidIP(host)) {
			entry = TitlePanel.getTitleTableModel().findLineEntryByIP(host);
		}else {
			entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrlString);
		}
		if (entry == null) {
			byte[] Request = helpers.buildHttpRequest(new URL(shortUrlString));
			String cookie = new Getter(helpers).getHeaderValueOf(true, messageInfo, "Cookie");

			Request = Commons.buildCookieRequest(helpers,cookie,Request);
			IHttpRequestResponse messageinfo = callbacks.makeHttpRequest(messageInfo.getHttpService(), Request);
			entry = new LineEntry(messageinfo,true,true,"");

			BurpExtender.getGui().getTitlePanel().getTitleTableModel().addNewLineEntry(entry);
		}
		return entry;
	}

	public static void addCommentForLine(LineEntry entry) {
		int index = TitlePanel.getTitleTableModel().getLineEntries().indexOf(entry);
		String commentAdd = JOptionPane.showInputDialog("Comments", null).trim();
		if (commentAdd == null) return;
		while(commentAdd.trim().equals("")){
			commentAdd = JOptionPane.showInputDialog("Comments", null).trim();
		}
		if (commentAdd != null) {
			String comment = entry.getComment().trim();
			if (comment == null || comment.equals("")) {
				comment = commentAdd;
			}else if(comment.contains(commentAdd)){
				//do nothing
			}else{
				comment = comment+","+commentAdd;
			}
			entry.setComment(comment);
			TitlePanel.getTitleTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
		}
	}

	public static void addToDomain(IHttpRequestResponse[] messages) {

		Set<String> domains = new HashSet<String>();
		for(IHttpRequestResponse message:messages) {
			String host = message.getHttpService().getHost();
			domains.add(host);
		}

		DomainObject domainResult = DomainPanel.getDomainResult();
		domainResult.addToDomainOject(domains);
	}


	public static void addToRequest(IHttpRequestResponse[] messages) {
		Set<String> domains = new HashSet<String>();
		for(IHttpRequestResponse message:messages) {
			String host = message.getHttpService().getHost();
			LineEntry entry = new LineEntry(message);
			TitlePanel.getTitleTableModel().addNewLineEntry(entry); //add request
			DomainPanel.getDomainResult().addToDomainOject(host); //add domain
		}
	}


	public JMenuItem createLevelMenu(IContextMenuInvocation invocation){
		JMenuItem setLevelMenu = null;
		try{
			IHttpRequestResponse[] messages = invocation.getSelectedMessages();

			String urlString = BurpExtender.getCallbacks().getHelpers().analyzeRequest(messages[0]).getUrl().toString();
			LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(urlString);
			if (entry == null) {
				String shortUrlString = messages[0].getHttpService().toString();
				entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrlString);
			}

			if (entry != null) {
				int index = TitlePanel.getTitleTable().getModel().getLineEntries().indexOf(entry);
				setLevelMenu = new LineEntryLevelMenu(TitlePanel.getTitleTable(),new int[] {index});
				setLevelMenu.setText("^_^[Domain Hunter] Set Level As");
			}
		}catch (Exception e){
		}
		return setLevelMenu;
	}
}