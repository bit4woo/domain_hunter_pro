package burp;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.burp.HelperPlus;
import com.bit4woo.utilbox.utils.CharsetUtils;

import GUI.GUIMain;
import Tools.ToolPanel;
import base.BackGroundActionListener;
import base.Commons;
import config.ConfigManager;
import config.ConfigName;
import domain.DomainManager;
import title.LineEntry;
import title.LineTable;
import title.TitlePanel;

public class LineEntryMenuForBurp{

	public PrintWriter stderr = BurpExtender.getStderr();
	public static IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
	public PrintWriter stdout = BurpExtender.getStdout();
	TitlePanel titlepanel;
	List<JMenuItem> JMenuItemList = new ArrayList<JMenuItem>();
	private GUIMain guiMain;

	public LineEntryMenuForBurp(GUIMain guiMain) {
		this.guiMain = guiMain;
	}

	public List<JMenuItem> createMenuItemsForBurp(IContextMenuInvocation invocation) {
		titlepanel= guiMain.getTitlePanel();
		/*
		这里的逻辑有3重：
		1、仅将域名添加到子域名和target中，
		2、将请求添加到title列表中，包含1的行为
		3、对某一个请求添加comment，如果请求不存在，放弃；添加请求包含步骤1和2的行为。
		 */

		//JMenuItem runWithSamePathItem = new JMenuItem("^_^ Run Targets with this path");
		//runWithSamePathItem.addActionListener(new runWithSamePath(invocation));

		//JMenuItem doDirBruteItem = new JMenuItem("^_^ Do Dir Brute");
		//doDirBruteItem.addActionListener(new doDirBrute(invocation));

		JMenuItem addDomainToDomainHunter = new JMenuItem("^_^ Add Domain");
		addDomainToDomainHunter.addActionListener(new addHostToRootDomain(invocation));

		JMenuItem addRequestToDomainHunter = new JMenuItem("^_^ Add Request");
		addRequestToDomainHunter.addActionListener(new addRequestToHunter(invocation));

		JMenuItem addCommentToDomainHunter = new JMenuItem("^_^ Add Comment");
		addCommentToDomainHunter.addActionListener(new addComment(invocation));

		JMenuItem saveRequestAndAddComment = new JMenuItem("^_^ Add Request And Comment");
		saveRequestAndAddComment.addActionListener(new saveAndComment(invocation));

		JMenuItem setAsChecked = new JMenuItem("^_^ Check Done");
		setAsChecked.addActionListener(new setAsChecked(invocation));

		//当目标使用了的第三方的服务，那么非目标域名中也含有有用信息，这时可以进行一次主动的搜索
		JMenuItem doSearch = new JMenuItem("^_^ Do Search");
		doSearch.addActionListener(new doSingleSearch(invocation));

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

		JMenuItemList.add(saveRequestAndAddComment);//保存数据包并且添加备注信息
		JMenuItemList.add(addRequestToDomainHunter);
		JMenuItemList.add(addCommentToDomainHunter);

		JMenuItemList.add(addDomainToDomainHunter);
		//JMenuItemList.add(doDirBruteItem);
		//JMenuItemList.add(runWithSamePathItem);
		JMenuItemList.add(sendToToolPanel);
		JMenuItemList.add(doSearch);


		if (ConfigManager.getBooleanConfigByKey(ConfigName.showMenuItemsInOne)) {
			ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();

			JMenu domainHunterPro = new JMenu("^_^ Domain Hunter Pro");
			
			String fileName = BurpExtender.getDataLoadManager().getCurrentDBFile().getName();
			if (StringUtils.isNoneBlank(fileName)) {
				domainHunterPro.setText(String.format("^_^ Domain Hunter Pro [%s]",fileName));
			}
			result.add(domainHunterPro);
			for (JMenuItem item : JMenuItemList) {
				domainHunterPro.add(item);
			}
			return result;
		}
		return JMenuItemList;
	}


	/**
	 * 选中的请求数据包
	 * @param invocation
	 * @return
	 */
	private IHttpRequestResponse[] getSelectedMessages(IContextMenuInvocation invocation) {

		IHttpRequestResponse[] messages = invocation.getSelectedMessages();
		//stdout.println("ToolFlag "+invocation.getToolFlag());
		//stdout.println("messages.length "+messages.length);
		if (messages!=null){
			return messages;
		}

		List<IHttpRequestResponse> tmp = new ArrayList<>();

		if (invocation.getToolFlag() == 16){//issue中的请求数据包
			IScanIssue[] issues = invocation.getSelectedIssues();
			for (IScanIssue issue:issues) {
				tmp.addAll(Arrays.asList(issue.getHttpMessages()));
			}
		}else if (invocation.getToolFlag() == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST) {//sitemap中的数据包
			IHttpRequestResponse[] siteMapMessage = invocation.getSelectedMessages();
			for (IHttpRequestResponse message:siteMapMessage) {
				String prefix = message.getHttpService().toString();
				tmp.addAll(Arrays.asList(BurpExtender.getCallbacks().getSiteMap(prefix)));
			}
		}

		//这时候，上面的messages是null，所以需要一个新的数组变量
		IHttpRequestResponse[] messages1 = new IHttpRequestResponse[tmp.size()];
		tmp.toArray(messages1);
		return messages1;
	}


	public static void addLevelABC(JMenu topMenu,final LineTable lineTable, final int[] rows){
		String[] MainMenu = LineEntry.AssetTypeArray;
		for(int i = 0; i < MainMenu.length; i++){
			JMenuItem item = new JMenuItem(MainMenu[i]);
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					lineTable.getLineTableModel().updateAssetTypeOfRows(rows,e.getActionCommand());
				}

			});
			topMenu.add(item);
		}
	}


	/**
	 * 将查找行为放在事件触发之后进行。
	 * @param topMenu
	 * @param messages
	 */
	public void createSubMenu(JMenu topMenu, IHttpRequestResponse[] messages){
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
						LineEntry entry = titlepanel.getTitleTable().getLineTableModel().findLineEntryByMessage(messages[0]);

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
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
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
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
				addRequests(messages,"");
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
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
					ToolPanel.inputTextArea.setText(getSelectedStringFromBurp(invocation));
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
	public class addComment extends BackGroundActionListener{
		private IContextMenuInvocation invocation;
		addComment(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		protected void action() {
			//还是要简化逻辑，如果找不到就不执行！
			try{
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
				HelperPlus getter = BurpExtender.getHelperPlus();
				String comment = getCommentInfo();
				if (StringUtils.isEmpty(comment)) {
					return;
				}
				for (IHttpRequestResponse message:messages){
					String host = message.getHttpService().getHost();
					int port = message.getHttpService().getPort();

					List<LineEntry> entries = titlepanel.getTitleTable().getLineTableModel().findLineEntriesByHostAndPort(host, port);

					if (entries.size() == 0) {
						//Do Nothing
					}else if (entries.size() == 1){
						addCommentForLine(entries.get(0),comment);
					}else {
						URL fullurl = getter.getFullURL(message);
						List<LineEntry> query_list = findLineEntryByFullUrl(entries,fullurl.toString());
						if (query_list.size() >= 1){//不对所有记录添加备注，只对最新的添加
							LineEntry item = query_list.get(query_list.size() - 1);
							addCommentForLine(item,comment);
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

	public class saveAndComment implements ActionListener{
		private IContextMenuInvocation invocation;
		saveAndComment(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try{
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
				if (messages.length>=1) {
					String comment = getCommentInfo();
					addRequests(messages,comment);
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
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
				List<LineEntry> entries = titlepanel.getTitleTable().getLineTableModel().findLineEntriesByHostAndPort(messages[0].getHttpService().getHost()
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


	public class setAsChecked extends BackGroundActionListener{
		private IContextMenuInvocation invocation;
		setAsChecked(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		protected void action()
		{
			try{
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
				String host = HelperPlus.getHost(messages[0]);
				int port = messages[0].getHttpService().getPort();
				List<LineEntry> entries = titlepanel.getTitleTable().getLineTableModel().findLineEntriesByHostAndPort(host,port);

				if (entries.size() > 0) {
					for (LineEntry entry:entries) {
						entry.setCheckStatus(LineEntry.CheckStatus_Checked);
						entry.setTime(Commons.getNowTimeString());
						int index = titlepanel.getTitleTable().getLineTableModel().getLineEntries().IndexOfKey(entry.getUrl());
						stdout.println("$$$ "+entry.getUrl()+"status has been set to "+LineEntry.CheckStatus_Checked);
						titlepanel.getTitleTable().getLineTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
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


	public class doSingleSearch extends BackGroundActionListener{
		private IContextMenuInvocation invocation;
		doSingleSearch(IContextMenuInvocation invocation) {
			this.invocation  = invocation;
		}
		@Override
		protected void action()
		{
			try{
				IHttpRequestResponse[] messages = getSelectedMessages(invocation);
				stdout.println(String.format("Search %s selected items",messages.length));
				guiMain.getDomainPanel().searchBackground(Arrays.asList(messages));
			}
			catch (Exception e1)
			{
				e1.printStackTrace(stderr);
			}
		}
	}

	public class setLevelAsActionListener extends BackGroundActionListener{
		setLevelAsActionListener(IContextMenuInvocation invocation,JMenu topMenu) {
			createSubMenu(topMenu,invocation.getSelectedMessages());
		}
		@Override
		protected void action()
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
					IHttpRequestResponse[] messages = getSelectedMessages(invocation);
					HelperPlus getter = BurpExtender.getHelperPlus();
					URL fullurl = getter.getFullURL(messages[0]);
					LineEntry entry = titlepanel.getTitleTable().getLineTableModel().findLineEntry(fullurl.toString());
					if (entry == null) {
						URL shortUrl = HelperPlus.getBaseURL(messages[0]);
						if(!fullurl.equals(shortUrl)) {
							entry = titlepanel.getTitleTable().getLineTableModel().findLineEntry(shortUrl.toString());
						}
					}

					if (entry != null) {
						int index = titlepanel.getTitleTable().getLineTableModel().getLineEntries().IndexOfKey(entry.getUrl());

						addLevelABC(topMenu,titlepanel.getTitleTable(),new int[] {index});
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

	/**
	 * https://api.example.vn:443/Execute#1653013013763
	 * https://api.example.vn:443/Execute
	 * 由于存在如上情况，需要返回list
	 * @param lineEntries
	 * @param url
	 * @return
	 */
	public static List<LineEntry> findLineEntryByFullUrl(List<LineEntry> lineEntries, String url) {
		List<LineEntry> result = new ArrayList<LineEntry>();
		url = Commons.formateURLString(url);
		for(LineEntry item:lineEntries) {
			if (item.getUrl().equals(url)) {
				result.add(item);
			}
			if (item.getUrl().startsWith(url+"#")){
				result.add(item);
			}
		}
		return result;
	}

	/**
	 *
	 * @param entry
	 */
	public void addCommentForLine(LineEntry entry,String comment) {
		int index = titlepanel.getTitleTable().getLineTableModel().getLineEntries().IndexOfKey(entry.getUrl());
		if (comment != null) {
			entry.addComment(comment);
			titlepanel.getTitleTable().getLineTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
		}
	}

	/**
	 * 对多个记录添加备注
	 * @param entries
	 */
	public void addCommentForLines(List<LineEntry> entries,String comment) {
		for (LineEntry entry:entries){
			int index = titlepanel.getTitleTable().getLineTableModel().getLineEntries().IndexOfKey(entry.getUrl());
			if (comment != null) {
				entry.addComment(comment);
				titlepanel.getTitleTable().getLineTableModel().fireTableRowsUpdated(index,index);//主动通知更新，否则不会写入数据库!!!
			}
		}
	}

	/**
	 * 弹窗，获取需要备注的信息
	 */
	public String getCommentInfo() {
		String commentAdd = JOptionPane.showInputDialog("Comments", null);
		if (commentAdd == null) return "";
		while(commentAdd.trim().equals("")){
			commentAdd = JOptionPane.showInputDialog("Comments", null).trim();
		}
		return commentAdd;
	}

	public void addToDomain(IHttpRequestResponse[] messages) {

		DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
		for(IHttpRequestResponse message:messages) {
			String host = message.getHttpService().getHost();
			domainResult.addToTargetAndSubDomain(host,true);
		}
		guiMain.getDomainPanel().saveDomainDataToDB();
	}


	public void addRequests(IHttpRequestResponse[] messages,String comment) {
		if (titlepanel.getTitleTable().getLineTableModel() ==null) {
			stderr.println("Title Table Model is Null, Maybe no database file loaded yet.");
			return;
		}

		if (messages== null) {
			stderr.println("messages is Null.");
			return;
		}

		for(IHttpRequestResponse message:messages) {
			//当时为啥要用这个key来存储新增的Request？URL地址一样而数据包不一样的情况？
			//String hashKey = HashCode.fromBytes(message.getRequest()).toString();

			HelperPlus getter = BurpExtender.getHelperPlus();
			URL fullurl = getter.getFullURL(message);
			LineEntry entry = titlepanel.getTitleTable().getLineTableModel().findLineEntry(fullurl.toString());

			LineEntry newEntry = new LineEntry(message);
			newEntry.setEntrySource(LineEntry.Source_Manual_Saved);
			newEntry.setCheckStatus(LineEntry.CheckStatus_UnChecked);
			newEntry.addComment(comment);
			addIpAndCdnInfoUsingSameHostEntries(newEntry);

			if (entry != null) {//存在相同URL的记录
				titlepanel.getTitleTable().getLineTableModel().addNewLineEntryWithTime(newEntry); //add request，修改URL(加#时间戳)后新增
			}else {//不存在相同记录，直接新增
				titlepanel.getTitleTable().getLineTableModel().addNewLineEntry(newEntry); //add request，新增
			}

			String host = message.getHttpService().getHost();
			guiMain.getDomainPanel().getDomainResult().addIfValid(host); //add domain
		}
	}

	/**
	 * 查找相同host的记录，并将其IP和CDN信息添加到新的entry上
	 * @return
	 */
	public LineEntry addIpAndCdnInfoUsingSameHostEntries(LineEntry entry){
		String host = entry.getHost();
		List<LineEntry> historyEntries = titlepanel.getTitleTable().getLineTableModel().findSameHostEntries(host);
		for (LineEntry hisentry:historyEntries){
			if (!hisentry.getEntrySource().equals(LineEntry.Source_Manual_Saved)){
				Set<String> IPset = hisentry.getIPSet();
				if (IPset.size() >=1){
					entry.setCertDomainSet(hisentry.getCertDomainSet());
					entry.setCNAMESet(hisentry.getCNAMESet());
					entry.setIPSet(hisentry.getIPSet());
					return entry;
				}
			}
		}
		return entry;
	}

	/**
	 * 
	 * @return
	 */
	public String getSelectedStringFromBurp(IContextMenuInvocation invocation){
		String result = "";

		IHttpRequestResponse[] messages = getSelectedMessages(invocation);

		if (messages == null ) {
			return result;
		}

		stdout.println(String.format("%s selected items",messages.length));

		int[] selectedIndex = invocation.getSelectionBounds();//当数据包中有中文或其他宽字符的时候，这里的返回值不正确。已报bug。
		//stdout.println(selectedIndex[0]+":"+selectedIndex[1]);
		//这里的index应该是字符串的index，进行选中操作时对象应该是字符文本内容，无论是一个中文还是一个字母，都是一个文本字符。这就是我们通常的文本操作啊，之前是想多了。
		//burp进行的byte和string之间的转换，没有考虑特定的编码，是一刀切的方式，所以将index用于byte序列上，就不能正确对应。

		//单独选中某段文字的情况
		if (messages.length ==1 && selectedIndex != null && selectedIndex[1]-selectedIndex[0]>=3) {

			IHttpRequestResponse message = messages[0];
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

			if(source!=null) {
				String originalCharSet = CharsetUtils.detectCharset(source);
				String text;
				try {
					text = new String(source,originalCharSet);
				}catch(Exception e) {
					text = new String(source);
				}
				result = text.substring(selectedIndex[0], selectedIndex[1]);
			}

			return result;
		}

		for (IHttpRequestResponse message:messages) {

			String content = "";
			byte[] request = message.getRequest();
			byte[] response = message.getResponse();

			if (request!= null) {
				content = new String(request);
			}

			if (response!= null) {
				content = content+System.lineSeparator()+new String(response);
			}
			result = result+System.lineSeparator()+content;
		}
		return result;
	}
}
