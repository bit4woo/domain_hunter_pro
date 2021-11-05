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
import burp.Commons;
import burp.Getter;
import burp.HttpMessageCharSet;
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
	public static IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
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

		JMenuItem doDirBruteItem = new JMenuItem("^_^ Do Dir Brute");
		doDirBruteItem.addActionListener(new doDirBrute(invocation));

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

		JMenuItem sendToToolPanel = new JMenuItem("^_^ Send To Tool Panel");
		sendToToolPanel.addActionListener(new sendToToolPanel(invocation));

		JMenuItemList.add(setAsChecked);
		JMenuItemList.add(setLevelAs2);

		JMenuItemList.add(addRequestToDomainHunter);
		JMenuItemList.add(addCommentToDomainHunter);

		JMenuItemList.add(addDomainToDomainHunter);
		//JMenuItemList.add(doDirBruteItem);
		JMenuItemList.add(runWithSamePathItem);
		JMenuItemList.add(sendToToolPanel);

		if (ToolPanel.showItemsInOne.isSelected()) {
			ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();

			JMenu domainHunterPro = new JMenu("^_^ Domain Hunter Pro");
			if (!ProjectMenu.isAlone()) {
				String proName = DomainPanel.getDomainResult().getProjectName();
				domainHunterPro.setText(String.format("^_^ Domain Hunter Pro [%s]",proName));
			}
			result.add(domainHunterPro);
			for (JMenuItem item : JMenuItemList) {
				domainHunterPro.add(item);
			}
			return result;
		}else {
			if (!ProjectMenu.isAlone()) {
				String proName = DomainPanel.getDomainResult().getProjectName();
				for (JMenuItem item : JMenuItemList) {
					item.setText("^_^ "+proName+"-->"+item.getText().replace("^_^ ", ""));
				}
			}
			return JMenuItemList;
		}
	}

	public static void addLevelABC(JMenu topMenu,final LineTable lineTable, final int[] rows){
		String[] MainMenu = LineEntry.AssetTypeArray;
		for(int i = 0; i < MainMenu.length; i++){
			JMenuItem item = new JMenuItem(MainMenu[i]);
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					lineTable.getModel().updateAssetTypeOfRows(rows,e.getActionCommand());
				}

			});
			topMenu.add(item);
		}
	}

	
	/**
	 * 将查找行为放在事件触发之后进行。
	 * @param topMenu
	 * @param lineTable
	 * @param rows
	 */
	public static void createSubMenu(JMenu topMenu, IHttpRequestResponse[] messages){
		String[] MainMenu = LineEntry.AssetTypeArray;
		for(int i = 0; i < MainMenu.length; i++){
			JMenuItem item = new JMenuItem(MainMenu[i]);
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						if (messages== null || messages.length == 0 ||messages[0] ==null) {
							return;
						}
						LineEntry entry = TitlePanel.getTitleTableModel().findLineEntryByMessage(messages[0]);
						
						if (entry != null) {
							entry.setAssetType(e.getActionCommand());
						}else {
							//topMenu.add(new JMenuItem("Null"));
						}
					}
					catch (Exception e1)
					{
						e1.printStackTrace();
					}
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
					IHttpRequestResponse messageInfo =messages[0];

					RunnerGUI runnergui = new RunnerGUI(messageInfo);
					runnergui.begainRun();
					return null;
				}
				@Override
				protected void done() {
				}
			};
			worker.execute();
		}
	}

	public class sendToToolPanel implements ActionListener{
		private IContextMenuInvocation invocation;
		sendToToolPanel(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				//using SwingWorker to prevent blocking burp main UI.

				@Override
				protected Map doInBackground() throws Exception {
					ToolPanel.inputTextArea.setText(getSelectedStringByBurp());
					return null;
				}
				@Override
				protected void done() {
				}
			};
			worker.execute();
		}

		public String getSelectedStringByBurp(){
			String result = "";

			IHttpRequestResponse[] messages = invocation.getSelectedMessages();

			if (messages == null ) {
				return result;
			}

			if (messages.length == 1) {
				IHttpRequestResponse message = messages[0];
				/////////////selected url/////////////////
				byte[] source = null;


				int context = invocation.getInvocationContext();
				if (context==IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST
						|| context ==IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST
						|| context == IContextMenuInvocation.CONTEXT_PROXY_HISTORY
						|| context == IContextMenuInvocation.CONTEXT_INTRUDER_ATTACK_RESULTS
						|| context == IContextMenuInvocation.CONTEXT_SEARCH_RESULTS
						|| context == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TABLE
						|| context == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TREE) {
					source = message.getRequest();
				}else {
					source = message.getResponse();
				}

				int[] selectedIndex = invocation.getSelectionBounds();//当数据包中有中文或其他宽字符的时候，这里的返回值不正确。已报bug。
				//stdout.println(selectedIndex[0]+":"+selectedIndex[1]);
				//这里的index应该是字符串的index，进行选中操作时对象应该是字符文本内容，无论是一个中文还是一个字母，都是一个文本字符。这就是我们通常的文本操作啊，之前是想多了。
				//burp进行的byte和string之间的转换，没有考虑特定的编码，是一刀切的方式，所以将index用于byte序列上，就不能正确对应。

				if(source!=null && selectedIndex !=null && selectedIndex[1]-selectedIndex[0]>=3) {
					String originalCharSet = HttpMessageCharSet.getCharset(source);
					String text;
					try {
						text = new String(source,originalCharSet);
					}catch(Exception e) {
						text = new String(source);
					}
					result = text.substring(selectedIndex[0], selectedIndex[1]);
				}
			}
			return result;
		}
	}

	public class doDirBrute implements ActionListener{
		private IContextMenuInvocation invocation;
		doDirBrute(IContextMenuInvocation invocation) {
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
					runnergui.begainDirBrute();
					return null;
				}
				@Override
				protected void done() {
				}
			};
			worker.execute();
		}
	}

	/*
	 * 如果能找到与全URL匹配的记录，就更新这个全URL对应的记录，即精确匹配。
	 * 如果找不到，这对这个URL对应的service记录进行更新，即更新所有这个service下的URL
	 * 
	 *  实现时，先通过service查找记录，避免2次全查。减少等待时间。
	 */
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

				String host = messages[0].getHttpService().getHost();
				int port = messages[0].getHttpService().getPort();

				List<LineEntry> entries = TitlePanel.getTitleTableModel().findLineEntriesByHostAndPort(host, port);

				if (entries.size() == 0) {

				}else if (entries.size() == 1){
					addCommentForLine(entries.get(0));
				}else {
					URL fullurl = getter.getFullURL(messages[0]);
					LineEntry entry = findLineEntryByFullUrl(entries,fullurl.toString());
					if (entry != null) {
						addCommentForLine(entry);
					}else {
						for (LineEntry item:entries) {
							addCommentForLine(item);
						}
					}
				}
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}

	@Deprecated //似乎也没有太大必要，暂时先不用了
	public class addCommentForHost implements ActionListener{
		private IContextMenuInvocation invocation;
		addCommentForHost(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			//还是要简化逻辑，如果找不到就不执行！
			try{
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				List<LineEntry> entries = TitlePanel.getTitleTableModel().findLineEntriesByHostAndPort(messages[0].getHttpService().getHost()
						,messages[0].getHttpService().getPort());

				if (entries.size() > 0) {
					for (LineEntry entry:entries) {
						//addCommentForLine(entry);
					}
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
				IHttpRequestResponse[] messages = invocation.getSelectedMessages();
				Getter getter = new Getter(helpers);
				String host = getter.getHost(messages[0]);
				int port = messages[0].getHttpService().getPort();
				List<LineEntry> entries = TitlePanel.getTitleTableModel().findLineEntriesByHostAndPort(host,port);

				if (entries.size() > 0) {
					for (LineEntry entry:entries) {
						entry.setCheckStatus(LineEntry.CheckStatus_Checked);
						entry.setTime(Commons.getNowTimeString());
						int index = TitlePanel.getTitleTableModel().getLineEntries().IndexOfKey(entry.getUrl());
						stdout.println("$$$ "+entry.getUrl()+"status has been set to "+LineEntry.CheckStatus_Checked);
						TitlePanel.getTitleTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
						//这里不用一次fire多个行，否则还得去查找index,会花费更多时间。
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
		setLevelAsActionListener(IContextMenuInvocation invocation,JMenu topMenu) {
			createSubMenu(topMenu,invocation.getSelectedMessages());
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			//do nothing
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

	public static LineEntry findLineEntryByFullUrl(List<LineEntry> lineEntries, String url) {
		url = Commons.formateURLString(url);
		for(LineEntry item:lineEntries) {
			if (item.getUrl().equals(url)) {
				return item;
			}
		}
		return null;
	}


	public static void addCommentForLine(LineEntry entry) {
		int index = TitlePanel.getTitleTableModel().getLineEntries().IndexOfKey(entry.getUrl());
		String commentAdd = JOptionPane.showInputDialog("Comments", null);
		if (commentAdd == null) return;
		while(commentAdd.trim().equals("")){
			commentAdd = JOptionPane.showInputDialog("Comments", null).trim();
		}
		if (commentAdd != null) {
			entry.addComment(commentAdd);
			TitlePanel.getTitleTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
		}
	}

	public static void addToDomain(IHttpRequestResponse[] messages) {

		DomainManager domainResult = DomainPanel.getDomainResult();
		for(IHttpRequestResponse message:messages) {
			String host = message.getHttpService().getHost();
			domainResult.addToRootDomainAndSubDomain(host,true);
		}
		DomainPanel.autoSave();
	}


	public static void addToRequest(IHttpRequestResponse[] messages) {
		for(IHttpRequestResponse message:messages) {
			//当时为啥要用这个key来存储新增的Request？URL地址一样而数据包不一样的情况？
			//String hashKey = HashCode.fromBytes(message.getRequest()).toString();

			Getter getter = new Getter(helpers);
			URL fullurl = getter.getFullURL(message);
			LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());

			LineEntry newEntry = new LineEntry(message);
			newEntry.setComment("Manual-Saved");
			newEntry.setCheckStatus(LineEntry.CheckStatus_UnChecked);
			newEntry.setManualSaved(true);

			if (entry != null) {//存在相同URL的记录
				int user_input = JOptionPane.showConfirmDialog(null, "Do you want to overwrite?","Item already exist",JOptionPane.YES_NO_CANCEL_OPTION);
				if (JOptionPane.YES_OPTION == user_input) {
					TitlePanel.getTitleTableModel().addNewLineEntry(newEntry); //add request，覆盖
				}else if(JOptionPane.NO_OPTION == user_input){//不覆盖,修改后新增
					newEntry.setUrl(entry.getUrl()+"#"+System.currentTimeMillis());
					TitlePanel.getTitleTableModel().addNewLineEntry(newEntry); //add request，修改URL(加#时间戳)后新增
				}//cancel,do nothing
			}else {//不存在相同记录，直接新增
				TitlePanel.getTitleTableModel().addNewLineEntry(newEntry); //add request，新增
			}

			String host = message.getHttpService().getHost();
			DomainPanel.getDomainResult().addIfValid(host); //add domain

		}
	}
}
