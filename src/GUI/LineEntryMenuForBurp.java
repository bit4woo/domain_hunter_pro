package GUI;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Getter;
import burp.IContextMenuInvocation;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import domain.DomainManager;
import domain.DomainPanel;
import title.LineEntry;
import title.LineTable;
import title.TitlePanel;

public class LineEntryMenuForBurp{

	public PrintWriter stderr = BurpExtender.getStderr();
	public IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
	public PrintWriter stdout = BurpExtender.getStdout();


	public List<JMenuItem> createMenuItemsForBurp(IContextMenuInvocation invocation) {
		List<JMenuItem> JMenuItemList = new ArrayList<JMenuItem>();
		/*
		这里的逻辑有3重：
		1、仅将域名添加到子域名和target中，
		2、将请求添加到title列表中，包含1的行为
		3、对某一个请求添加comment，如果请求不存在，放弃；添加请求包含步骤1和2的行为。
		 */

		JMenuItem runWithSamePathItem = new JMenuItem("^_^ Run Targets with this path");
		runWithSamePathItem.addActionListener(new runWithSamePath(invocation));

		JMenuItem addDomainToDomainHunter = new JMenuItem("^_^ Add Domain");
		addDomainToDomainHunter.addActionListener(new addHostToRootDomain(invocation));

		JMenuItem addRequestToDomainHunter = new JMenuItem("^_^ Add Request");
		addRequestToDomainHunter.addActionListener(new addRequestToHunter(invocation));

		JMenuItem addCommentToDomainHunter = new JMenuItem("^_^ Add Comment");
		addCommentToDomainHunter.addActionListener(new addComment(invocation));

		JMenuItem setAsChecked = new JMenuItem("^_^ Check Done");
		setAsChecked.addActionListener(new setAsChecked(invocation));

		//list.add(createLevelMenu(invocation));//这是导致右键菜单反应慢的根源，因为在每次在构造右键菜单时都要执行一遍tilte的查找。

		//替换方案1：当鼠标进入菜单时再去查询。//弃用，这种方式响应速度较慢，效果不好
		/*
		 * JMenu setLevelAs = new JMenu("Set Level As");
		 * setAsChecked.addMouseListener(new
		 * setLevelAsMouseListener(invocation,setLevelAs)); list.add(setLevelAs);
		 */

		//替换方案2：
		JMenu setLevelAs2 = new JMenu("^_^ Set Level As");
		setAsChecked.addActionListener(new setLevelAsActionListener(invocation,setLevelAs2));
		
		
		
		JMenuItemList.add(setAsChecked);
		JMenuItemList.add(setLevelAs2);
		
		JMenuItemList.add(addRequestToDomainHunter);
		JMenuItemList.add(addCommentToDomainHunter);
		
		JMenuItemList.add(addDomainToDomainHunter);
		JMenuItemList.add(runWithSamePathItem);
		
		
		if (ToolPanel.showItemsInOne.isSelected()) {
			ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();
			JMenu domainHunterPro = new JMenu("^_^ Domain Hunter Pro");
			result.add(domainHunterPro);
			for (JMenuItem item : JMenuItemList) {
				domainHunterPro.add(item);
			}
			return result;
		}else {
			return JMenuItemList;
		}
	}

	public static void addLevelABC(JMenu topMenu,final LineTable lineTable, final int[] rows){
		String[] MainMenu = {LineEntry.Level_A, LineEntry.Level_B, LineEntry.Level_C};
		for(int i = 0; i < MainMenu.length; i++){
			JMenuItem item = new JMenuItem(MainMenu[i]);
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					lineTable.getModel().updateLevelofRows(rows,e.getActionCommand());
				}

			});
			topMenu.add(item);
		}
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
				e1.printStackTrace(stderr );
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
				Getter getter = new Getter(helpers);
				URL fullurl = getter.getFullURL(messages[0]);
				LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());
				if (entry != null) {
					int user_input = JOptionPane.showConfirmDialog(null, "Do you want to overwrite?","Item already exist",JOptionPane.YES_NO_OPTION);
					if (JOptionPane.YES_OPTION == user_input) {
						addToRequest(messages);
					}
				}else {
					addToRequest(messages);
				}
				
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
					IHttpRequestResponse messageInfo =messages[0];

					RunnerGUI runnergui = new RunnerGUI(messageInfo);
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
				Getter getter = new Getter(helpers);
				URL fullurl = getter.getFullURL(messages[0]);
				LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());
				if (entry == null) {
					URL shortUrl = getter.getShortURL(messages[0]);
					if(!fullurl.equals(shortUrl)) {
						entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrl.toString());
					}
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



	public class setAsChecked implements ActionListener{
		private IContextMenuInvocation invocation;
		setAsChecked(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try{
//				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
//				Getter getter = new Getter(helpers);
//				URL fullurl = getter.getFullURL(messages[0]);
//				LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());
//				if (entry == null) {
//					URL shortUrl = getter.getShortURL(messages[0]);
//					if(!fullurl.equals(shortUrl)) {
//						entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrl.toString());
//					}
//				}
//
//				if (entry != null) {
//					int index = TitlePanel.getTitleTableModel().getLineEntries().IndexOfKey(entry.getUrl());
//					entry.setChecked(true);
//					stdout.println("$$$ "+entry.getUrl()+" updated");
//					TitlePanel.getTitleTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
//				}
				
				
				
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				Getter getter = new Getter(helpers);
				String host = getter.getHost(messages[0]);
				List<LineEntry> entries = TitlePanel.getTitleTableModel().findLineEntriesByHost(host);

				if (entries.size() > 0) {
					for (LineEntry entry:entries) {
						entry.setCheckStatus(LineEntry.CheckStatus_Checked);
						int index = TitlePanel.getTitleTableModel().getLineEntries().IndexOfKey(entry.getUrl());
						stdout.println("$$$ "+entry.getUrl()+"status has been set to "+LineEntry.CheckStatus_Checked);
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

	public class setLevelAsActionListener implements ActionListener{
		private IContextMenuInvocation invocation;
		private JMenu topMenu;
		setLevelAsActionListener(IContextMenuInvocation invocation,JMenu topMenu) {
			this.invocation  = invocation;
			this.topMenu = topMenu;
			addSubMenuInBackGround();
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			//do nothing
		}

		public void addSubMenuInBackGround() {
			SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				@Override
				protected Map doInBackground() throws Exception {
					try{
						IHttpRequestResponse[] messages = invocation.getSelectedMessages();
						Getter getter = new Getter(helpers);
						if (messages== null || messages.length == 0 ||messages[0] ==null) {
							return null;
						}
						URL fullurl = getter.getFullURL(messages[0]);
						LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());
						if (entry == null) {
							URL shortUrl = getter.getShortURL(messages[0]);
							if(!fullurl.equals(shortUrl)) {
								entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrl.toString());
							}
						}

						if (entry != null) {
							int index = TitlePanel.getTitleTable().getModel().getLineEntries().IndexOfKey(entry.getUrl());
							addLevelABC(topMenu,TitlePanel.getTitleTable(),new int[] {index});
						}else {
							//topMenu.add(new JMenuItem("Null"));
						}
						
						
//						IHttpRequestResponse[] messages = invocation.getSelectedMessages();
//						Getter getter = new Getter(helpers);
//						if (messages[0] != null) {
//							String host = getter.getHost(messages[0]);
//							List<LineEntry> entries = TitlePanel.getTitleTableModel().findLineEntriesByHost(host);
//							if (entries.size() > 0) {
//								for (LineEntry entry:entries) {
//									int index = TitlePanel.getTitleTable().getModel().getLineEntries().IndexOfKey(entry.getUrl());
//									addLevelABC(topMenu,TitlePanel.getTitleTable(),new int[] {index});
//								}
//							}
//						}
					}
					catch (Exception e1)
					{
						e1.printStackTrace(stderr);
					}
					return null;
				}
				@Override
				protected void done() {
					topMenu.updateUI();
				}
			};
			worker.execute();
		}

	}
	
	@Deprecated//该方案响应速度慢，弃用
	public class setLevelAsMouseListener extends MouseAdapter{
		private IContextMenuInvocation invocation;
		private JMenu topMenu;
		setLevelAsMouseListener(IContextMenuInvocation invocation,JMenu topMenu) {
			this.invocation  = invocation;
			this.topMenu = topMenu;
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			if (topMenu.getItemCount() == 0) {
				try{
					IHttpRequestResponse[] messages = invocation.getSelectedMessages();
					Getter getter = new Getter(helpers);
					URL fullurl = getter.getFullURL(messages[0]);
					LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());
					if (entry == null) {
						URL shortUrl = getter.getShortURL(messages[0]);
						if(!fullurl.equals(shortUrl)) {
							entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrl.toString());
						}
					}

					if (entry != null) {
						int index = TitlePanel.getTitleTable().getModel().getLineEntries().IndexOfKey(entry.getUrl());

						addLevelABC(topMenu,TitlePanel.getTitleTable(),new int[] {index});
						System.out.println("111");
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		}
	}


	public static void addCommentForLine(LineEntry entry) {
		int index = TitlePanel.getTitleTableModel().getLineEntries().IndexOfKey(entry.getUrl());
		String commentAdd = JOptionPane.showInputDialog("Comments", null);
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

		DomainManager domainResult = DomainPanel.getDomainResult();
		domainResult.addToDomainOject(domains);
		DomainPanel.autoSave();
	}


	public static void addToRequest(IHttpRequestResponse[] messages) {
		for(IHttpRequestResponse message:messages) {
			String host = message.getHttpService().getHost();
			LineEntry entry = new LineEntry(message);
			entry.setComment("Manual-Saved");
			entry.setCheckStatus(LineEntry.CheckStatus_UnChecked);
			TitlePanel.getTitleTableModel().addNewLineEntry(entry); //add request
			DomainPanel.getDomainResult().addToDomainOject(host); //add domain
		}
	}
}
