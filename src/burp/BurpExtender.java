package burp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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
	public static PrintWriter getStdout() {
		return stdout;
	}

	public static PrintWriter getStderr() {
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
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
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

		byte context = invocation.getInvocationContext();
		if (context == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TREE) {
			JMenuItem addToDomainHunter = new JMenuItem("^_^ Add To Domain Hunter");
			addToDomainHunter.addActionListener(new addHostToRootDomain(invocation));
			list.add(addToDomainHunter);
		}

		JMenuItem runWithSamePathItem = new JMenuItem("^_^ Run Targets with this path");
		runWithSamePathItem.addActionListener(new runWithSamePath(invocation));
		list.add(runWithSamePathItem);
		
		JMenuItem addCommentItem = new JMenuItem("^_^ Add Title Comment");
		addCommentItem.addActionListener(new addComment(invocation));
		list.add(addCommentItem);
		
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
				Set<String> domains = new HashSet<String>();
				for(IHttpRequestResponse message:messages) {
					String host = message.getHttpService().getHost();
					domains.add(host);
				}

				DomainObject domainResult = DomainPanel.getDomainResult();

				domainResult.getRelatedDomainSet().addAll(domains);
				if (domainResult.autoAddRelatedToRoot) {
					domainResult.relatedToRoot();
					domainResult.getSubDomainSet().addAll(domains);
				}
				gui.getDomainPanel().showToDomainUI();
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
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				String shortUrlString = messages[0].getHttpService().toString();
				
				LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrlString);
				int index = TitlePanel.getTitleTableModel().getLineEntries().indexOf(entry);
				
				if (entry != null) {
					String commentAdd = JOptionPane.showInputDialog("Comments", null).trim();
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
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}
}