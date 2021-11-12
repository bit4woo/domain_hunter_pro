package title;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import GUI.LineEntryMenuForBurp;
import GUI.RunnerGUI;
import GUI.GUI;
import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.Getter;
import burp.IBurpExtenderCallbacks;
import burp.IHttpRequestResponse;
import domain.DomainPanel;
import title.search.SearchDork;

public class LineEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private static LineTable lineTable;

	/**
	 * 这处理传入的行index数据是经过转换的 model中的index，不是原始的JTable中的index。
	 * @param lineTable
	 * @param modleRows
	 * @param columnIndex
	 */
	LineEntryMenu(final LineTable lineTable, final int[] modleRows,final int columnIndex){
		this.lineTable = lineTable;

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(modleRows.length+" Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});

		/*
		JMenuItem setColorItem = new JMenuItem(new AbstractAction("Checking (Set to red)#TODO") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				//lineTable.setColor(1);
			}
		});
		 */

		JMenuItem googleSearchItem = new JMenuItem(new AbstractAction("Seach On Google (double click index)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (modleRows.length >=50) {
					return;
				}
				for (int row:modleRows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = getValue(firstEntry,columnIndex);
					searchContent = URLEncoder.encode(searchContent);
					String url= "https://www.google.com/search?q="+searchContent;
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}

			public String getValue(LineEntry firstEntry,int columnIndex) {

				String columnName = lineTable.getColumnName(columnIndex);

				if (columnName.equalsIgnoreCase("title")){
					String title = firstEntry.getTitle();
					return "intitle:"+title;
				}else if (columnName.equalsIgnoreCase("comments")){
					String comment = firstEntry.getComment();
					return comment;
				}else if (columnName.equalsIgnoreCase("IP")){
					String ip = firstEntry.getIP();
					return ip;
				}else if (columnName.equalsIgnoreCase("CDN")){
					String cdn = firstEntry.getCDN();
					return cdn;
				}else {
					String host = firstEntry.getHost();
					return "site:"+host;
				}
			}
		});


		JMenuItem SearchOnGithubItem = new JMenuItem(new AbstractAction("Seach On Github") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (modleRows.length >=50) {
					return;
				}
				for (int row:modleRows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = getValue(firstEntry,columnIndex);
					String url= "https://github.com/search?q=%22"+searchContent+"%22+%22jdbc.url%22&type=Code";
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}

			public String getValue(LineEntry firstEntry,int columnIndex) {

				String columnName = lineTable.getColumnName(columnIndex);

				if (columnName.equalsIgnoreCase("title")){
					String title = firstEntry.getTitle();
					return title;
				}else if (columnName.equalsIgnoreCase("comments")){
					String comment = firstEntry.getComment();
					return comment;
				}else if (columnName.equalsIgnoreCase("IP")){
					String ip = firstEntry.getIP();
					return ip;
				}else if (columnName.equalsIgnoreCase("CDN")){
					String cdn = firstEntry.getCDN();
					return cdn;
				}else {
					String host = firstEntry.getHost();
					return host;
				}
			}
		});

		JMenuItem SearchOnFoFaItem = new JMenuItem(new AbstractAction("Seach On FoFa") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modleRows.length >=50) {
					return;
				}
				for (int row:modleRows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = getValue(firstEntry,columnIndex);
					searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));
					String url= "https://fofa.so/result?qbase64=%s";
					url= String.format(url, searchContent);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}

			public String getValue(LineEntry firstEntry,int columnIndex) {

				String columnName = lineTable.getColumnName(columnIndex);

				if (columnName.equalsIgnoreCase("title")){
					String title = firstEntry.getTitle();
					return title;
				}else if (columnName.equalsIgnoreCase("comments")){
					String comment = firstEntry.getComment();
					return comment;
				}else if (columnName.equalsIgnoreCase("IP")){
					String ip = firstEntry.getIP();
					return ip;
				}else if (columnName.equalsIgnoreCase("CDN")){
					String cdn = firstEntry.getCDN();
					return cdn;
				}else {
					String host = firstEntry.getHost();
					return host;
				}
			}

		});

		JMenuItem SearchOnFoFaWithIconhashItem = new JMenuItem(new AbstractAction("Seach On FoFa With IconHash") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modleRows.length >=50) {
					return;
				}
				for (int row:modleRows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = firstEntry.getIcon_hash();
					searchContent = String.format("icon_hash=\"%s\"", searchContent);//icon_hash="-247388890"
					searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));
					String url= "https://fofa.so/result?qbase64=%s";
					url= String.format(url, searchContent);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		JMenuItem SearchOnShodanItem = new JMenuItem(new AbstractAction("Seach On Shodan") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modleRows.length >=50) {
					return;
				}
				for (int row:modleRows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = getValue(firstEntry,columnIndex);
					searchContent = URLEncoder.encode(searchContent);
					String url= "https://www.shodan.io/search?query=%s";
					//https://www.shodan.io/search?query=baidu.com
					url= String.format(url, searchContent);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}

			public String getValue(LineEntry firstEntry,int columnIndex) {

				String columnName = lineTable.getColumnName(columnIndex);

				if (columnName.equalsIgnoreCase("title")){
					String title = firstEntry.getTitle();
					return title;
				}else if (columnName.equalsIgnoreCase("comments")){
					String comment = firstEntry.getComment();
					return comment;
				}else if (columnName.equalsIgnoreCase("IP")){
					String ip = firstEntry.getIP();
					return ip;
				}else if (columnName.equalsIgnoreCase("CDN")){
					String cdn = firstEntry.getCDN();
					return cdn;
				}else {
					String host = firstEntry.getHost();
					return host;
				}
			}

		});

		//https://www.shodan.io/search?query=http.favicon.hash%3A-1588080585
		JMenuItem SearchOnShodanWithIconhashItem = new JMenuItem(new AbstractAction("Seach On Shodan With IconHash") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modleRows.length >=50) {
					return;
				}
				for (int row:modleRows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = firstEntry.getIcon_hash();
					String url= "https://www.shodan.io/search?query=http.favicon.hash:%s";
					url= String.format(url, searchContent);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		JMenuItem SearchOnHunterItem = new JMenuItem(new AbstractAction("Seach On Hunter") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(modleRows[0]);
				String columnName = lineTable.getColumnName(columnIndex);

				if (columnName.equalsIgnoreCase("Status")){
					int status = firstEntry.getStatuscode();
					TitlePanel.getTextFieldSearch().setText(SearchDork.STATUS.toString()+":"+status);
				}else if (columnName.equalsIgnoreCase("length")){
					int length = firstEntry.getContentLength();
					TitlePanel.getTextFieldSearch().setText(length+"");
				}else if (columnName.equalsIgnoreCase("title")){
					String title = firstEntry.getTitle();
					TitlePanel.getTextFieldSearch().setText(SearchDork.TITLE.toString()+":"+title);
				}else if (columnName.equalsIgnoreCase("comments")){
					String comment = firstEntry.getComment();
					TitlePanel.getTextFieldSearch().setText(SearchDork.COMMENT.toString()+":"+comment);
				}else if (columnName.equalsIgnoreCase("IP")){
					String ip = firstEntry.getIP();
					TitlePanel.getTextFieldSearch().setText(ip);
				}else if (columnName.equalsIgnoreCase("CDN|CertInfo")){
					String cdn = firstEntry.getCDN();
					TitlePanel.getTextFieldSearch().setText(cdn);
				}else if (columnName.equalsIgnoreCase("IconHash")){
					String hash = firstEntry.getIcon_hash();
					TitlePanel.getTextFieldSearch().setText(hash);
				}else {
					String host = firstEntry.getHost();
					TitlePanel.getTextFieldSearch().setText(SearchDork.HOST.toString()+":"+host);
				}
			}
		});

		JMenuItem copyHostItem = new JMenuItem(new AbstractAction("Copy Host") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getHosts(modleRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(textUrls);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyHostAndPortItem = new JMenuItem(new AbstractAction("Copy Host:Port") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> items = lineTable.getModel().getHostsAndPorts(modleRows);
					String text = String.join(System.lineSeparator(), items);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(text);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyIPItem = new JMenuItem(new AbstractAction("Copy IP Set") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					Set<String> IPs = lineTable.getModel().getIPs(modleRows);
					String text = String.join(System.lineSeparator(), IPs);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(text);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyURLItem = new JMenuItem(new AbstractAction("Copy URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getURLs(modleRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(textUrls);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyCommonURLItem = new JMenuItem(new AbstractAction("Copy URL With Common Formate") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getCommonURLs(modleRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(textUrls);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem dirSearchItem = new JMenuItem();
		dirSearchItem.setText("Do Dir Search");
		dirSearchItem.addActionListener(new DirSearchAction(lineTable, modleRows));

		JMenuItem iconHashItem = new JMenuItem();
		iconHashItem.setText("Do Get Icon Hash");
		iconHashItem.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					IndexedLinkedHashMap<String, LineEntry> entries = lineTable.getModel().getLineEntries();
					for(LineEntry entry:entries.values()) {
						entry.DoGetIconHash();
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(BurpExtender.getStderr());
				}
			}
		});

		JMenuItem doPortScan = new JMenuItem();
		doPortScan.setText("Do Port Scan");
		doPortScan.addActionListener(new NmapScanAction(lineTable, modleRows));

		JMenuItem doGateWayByPassCheck = new JMenuItem(new AbstractAction("Do GateWay ByPass Check") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				HashMap<String, IHttpRequestResponse> targetMap = new HashMap<String,IHttpRequestResponse>();

				Set<String> IPs = lineTable.getModel().getIPs(modleRows);
				if (IPs.size() >= 50){//避免一次开太多导致系统卡死
					BurpExtender.getStderr().println("too many task");
					return;
				}

				for (int row: modleRows){//根据IP地址去重
					LineEntry entry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					targetMap.put(entry.getIP(), new LineMessageInfo(entry));
				}
				for (IHttpRequestResponse messageInfo:targetMap.values()) {
					RunnerGUI runnergui = new RunnerGUI(messageInfo);
					runnergui.begainRunChangeHostInHeader();
					runnergui.setVisible(true);
				}
			}
		});


		JMenuItem openURLwithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser(double click url)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getURLs(modleRows);
					if (urls.size() >= 50){//避免一次开太多网页导致系统卡死
						return;
					}
					for (String url:urls){
						Commons.browserOpen(url,ToolPanel.getLineConfig().getBrowserPath());
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});



		JMenuItem addHostsToScope = new JMenuItem(new AbstractAction("Add To Scope") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getURLs(modleRows);
					IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
					for(String url:urls) {
						URL shortUrl = new URL(url);
						callbacks.includeInScope(shortUrl);
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});


		JMenuItem doActiveScan = new JMenuItem(new AbstractAction("Do Active Scan") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				IndexedLinkedHashMap<String,LineEntry> entries = lineTable.getModel().getLineEntries();
				IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
				for (int i=modleRows.length-1;i>=0 ;i-- ) {
					try{
						LineEntry entry = entries.getValueAtIndex(modleRows[i]);

						String host = entry.getHost();
						int port = entry.getPort();
						boolean useHttps;
						if (entry.getProtocol().equalsIgnoreCase("https")){
							useHttps = true;
						}else {
							useHttps = false;
						}
						byte[] request = entry.getRequest();

						callbacks.includeInScope(new URL(entry.getUrl()));
						callbacks.doActiveScan(host, port, useHttps, request);
					}
					catch (Exception e1)
					{
						e1.printStackTrace(stderr);
					}
				}
			}
		});

		JMenuItem checkingItem = new JMenuItem(new AbstractAction("Checking") {//checking
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				TitlePanel.getTitleTableModel().updateRowsStatus(modleRows,LineEntry.CheckStatus_Checking);			
				java.util.List<String> urls = lineTable.getModel().getURLs(modleRows);
				IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
				for(String url:urls) {
					URL shortUrl;
					try {
						shortUrl = new URL(url);
						callbacks.includeInScope(shortUrl);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem moreActionItem = new JMenuItem(new AbstractAction("Need More Action") {//checking
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				TitlePanel.getTitleTableModel().updateRowsStatus(modleRows,LineEntry.CheckStatus_MoreAction);			
				java.util.List<String> urls = lineTable.getModel().getURLs(modleRows);
				IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
				for(String url:urls) {
					URL shortUrl;
					try {
						shortUrl = new URL(url);
						callbacks.includeInScope(shortUrl);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem checkedItem = new JMenuItem(new AbstractAction("Check Done") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				TitlePanel.getTitleTableModel().updateRowsStatus(modleRows,LineEntry.CheckStatus_Checked);
				//				if (BurpExtender.rdbtnHideCheckedItems.isSelected()) {//实现自动隐藏，为了避免误操作，不启用
				//					String keyword = BurpExtender.textFieldSearch.getText().trim();
				//					lineTable.search(keyword);
				//				}
				BurpExtender.getGui().titlePanel.digStatus();
			}
		});


		JMenu assetTypeMenu = new JMenu("Set Asset Type As");
		LineEntryMenuForBurp.addLevelABC(assetTypeMenu, lineTable, modleRows);


		JMenuItem batchAddCommentsItem = new JMenuItem(new AbstractAction("Add Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String Comments = JOptionPane.showInputDialog("Comments", null).trim();
				while(Comments.trim().equals("")){
					Comments = JOptionPane.showInputDialog("Comments", null).trim();
				}
				TitlePanel.getTitleTableModel().updateComments(modleRows,Comments);
			}
		});


		JMenuItem copyLocationURLItem = new JMenuItem(new AbstractAction("Copy Location URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					List<String> urls = lineTable.getModel().getLocationUrls(modleRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(textUrls);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyCDNAndCertInfoItem = new JMenuItem(new AbstractAction("Copy CDN|CertInfo") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					List<String> urls = lineTable.getModel().getCDNAndCertInfos(modleRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(textUrls);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyIconhashItem = new JMenuItem(new AbstractAction("Copy Icon Hash") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					List<String> urls = lineTable.getModel().getIconHashes(modleRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(textUrls);
					clipboard.setContents(selection, null);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem SendToRepeater = new JMenuItem(new AbstractAction("Send To Repeater") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.

					@Override
					protected Map doInBackground() throws Exception {
						IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
						for (int row: modleRows){
							LineEntry entry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
							String host =entry.getHost();
							int port = entry.getPort();
							String protocol = entry.getProtocol();
							boolean useHttps =false;
							if (protocol.equalsIgnoreCase("https")) {
								useHttps =true;
							}
							byte[] request = entry.getRequest();

							String tabCaption = row+"DH";
							callbacks.sendToRepeater(
									host,
									port,
									useHttps,
									request,
									tabCaption);
						}
						return null;
					}
					@Override
					protected void done() {
					}
				};
				worker.execute();
			}
		});


		JMenuItem SendToRepeaterWithCookieItem = new JMenuItem(new AbstractAction("Send To Repeater With Cookie") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.

					@Override
					protected Map doInBackground() throws Exception {
						String cookieValue = JOptionPane.showInputDialog("cookie value", null).trim();
						while(cookieValue.trim().equals("")){
							cookieValue = JOptionPane.showInputDialog("cookie value", null).trim();
						}
						IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
						for (int row: modleRows){
							LineEntry entry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
							String host =entry.getHost();
							int port = entry.getPort();
							String protocol = entry.getProtocol();
							boolean useHttps =false;
							if (protocol.equalsIgnoreCase("https")) {
								useHttps =true;
							}

							byte[] request = entry.getRequest();
							Getter getter = new Getter(callbacks.getHelpers());
							LinkedHashMap<String, String> headers = getter.getHeaderMap(true,request);
							headers.put("Cookie",cookieValue);
							byte[] body = getter.getBody(true,request);
							request = callbacks.getHelpers().buildHttpMessage(getter.headerMapToHeaderList(headers),body);

							String tabCaption = row+"DH";
							callbacks.sendToRepeater(
									host,
									port,
									useHttps,
									request,
									tabCaption);
						}
						return null;
					}
					@Override
					protected void done() {
					}
				};
				worker.execute();
			}
		});

		/**
		 * 单纯从title记录中删除,不做其他修改
		 */
		JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete This Entry") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().removeRows(modleRows);
				}else {
					return;
				}
				GUI.titlePanel.digStatus();
			}
		});
		removeItem.setToolTipText("Just Delete Entry In Title Panel");

		/**
		 * 从子域名列表中删除对应资产，表面当前host（应该是一个IP）不是我们的目标资产。
		 * 那么应该同时做以下三点：
		 * 1、从domain panel中的SubDomainSet移除。
		 * 2、从title panel中删除记录。
		 * 3、把目标加入黑名单，以便下次跑网段如果有相同IP可以标记出来。
		 */
		@Deprecated
		JMenuItem removeSubDomainItem = new JMenuItem(new AbstractAction("Delete Host From Target(SubDomainSet)") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these Hosts from SubDomainSet ?");
				if (result == JOptionPane.YES_OPTION) {
					//java.util.List<String> hosts = lineTable.getModel().getHosts(rows);//不包含端口，如果原始记录包含端口就删不掉
					//如果有 domain domain:8888 两个记录，这种方式就会删错对象
					java.util.List<String> hostAndPort = lineTable.getModel().getHostsAndPorts(modleRows);//包含端口，如果原始记录
					for(String item:hostAndPort) {
						if (!DomainPanel.domainResult.getSubDomainSet().remove(item)) {
							DomainPanel.domainResult.getSubDomainSet().remove(item.split(":")[0]);
						}
					}
				}else {
					return;
				}
				GUI.titlePanel.digStatus();
			}
		});
		removeSubDomainItem.setToolTipText("Delete Host In Domain Panel");

		/**
		 * 从子域名列表中删除对应资产，表面当前host（应该是一个IP）不是我们的目标资产。
		 * 那么应该同时做以下三点：
		 * 1、从domain panel中的SubDomainSet移除。
		 * 2、从title panel中删除记录。
		 * 3、把目标加入黑名单，以便下次跑网段如果有相同IP可以标记出来。
		 */
		JMenuItem NotTargetHandleItem = new JMenuItem(new AbstractAction("Not My Target! Delete It!") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure these items are not your target, DELETE them?");
				if (result == JOptionPane.YES_OPTION) {
					//java.util.List<String> hosts = lineTable.getModel().getHosts(rows);//不包含端口，如果原始记录包含端口就删不掉
					//如果有 domain domain:8888 两个记录，这种方式就会删错对象
					java.util.List<String> hostAndPort = lineTable.getModel().getHostsAndPorts(modleRows);//包含端口，如果原始记录
					for(String item:hostAndPort) {
						if (!DomainPanel.domainResult.getSubDomainSet().remove(item)) {
							DomainPanel.domainResult.getSubDomainSet().remove(item.split(":")[0]);
						}
					}
					lineTable.getModel().addHostToNotTargetIPSet(modleRows);//当host是IP的时候，加入黑名单
					lineTable.getModel().removeRows(modleRows);//删除当前行，必须最后执行！
				}else {
					return;
				}
				GUI.titlePanel.digStatus();
			}
		});
		NotTargetHandleItem.setToolTipText("1.Delete Host From Domain Panel,2.Add Host to BlackList,3.Delete Entry From Title Panel");

		/**
		 * 认为资产不是目标资产，加入NotTargeIPSet，不做其他修改
		 * 黑名单仅用于标记title记录，不会不请求对应的web
		 */
		JMenuItem addToblackListItem = new JMenuItem(new AbstractAction("Add Host To Black List(NotTargeIPSet)") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to ADD Host(Must Be IP) to NotTargetIPSet ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().addHostToNotTargetIPSet(modleRows);
				}else {
					return;
				}
				GUI.titlePanel.digStatus();
			}
		});
		addToblackListItem.setToolTipText("If host is IP address,will be added to NotTargetIPSet[Black List]");

		/**
		 * 从黑名单中移除，不做其他修改。
		 * 黑名单仅用于标记title记录，不会不请求对应的web
		 */
		JMenuItem removeFromBlackListItem = new JMenuItem(new AbstractAction("Remove Host From Black List(NotTargeIPSet)") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to REMOVE Host(Must Be IP) from NotTargetIPSet ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().removeHostFromNotTargetIPSet(modleRows);
				}else {
					return;
				}
			}
		});
		removeFromBlackListItem.setToolTipText("If host is IP address,will be removed from NotTargetIPSet");


		this.add(itemNumber);
		this.add(checkingItem);
		this.add(moreActionItem);
		this.add(checkedItem);
		this.add(assetTypeMenu);
		this.add(batchAddCommentsItem);

		this.addSeparator();

		JMenu DoMenu = new JMenu("Do");
		this.add(DoMenu);
		//burp相关行为
		DoMenu.add(addHostsToScope);
		DoMenu.add(SendToRepeater);
		DoMenu.add(SendToRepeaterWithCookieItem);
		DoMenu.add(doActiveScan);
		
		DoMenu.addSeparator();
		//外部程序相关行为
		DoMenu.add(openURLwithBrowserItem);
		DoMenu.add(doPortScan);
		DoMenu.add(dirSearchItem);
		DoMenu.add(doGateWayByPassCheck);
		//this.add(iconHashItem);

		JMenu SearchMenu = new JMenu("Search");
		this.add(SearchMenu);
		SearchMenu.add(googleSearchItem);
		SearchMenu.add(SearchOnGithubItem);
		SearchMenu.add(SearchOnHunterItem);
		SearchMenu.add(SearchOnFoFaItem);
		SearchMenu.add(SearchOnFoFaWithIconhashItem);
		SearchMenu.add(SearchOnShodanItem);
		SearchMenu.add(SearchOnShodanWithIconhashItem);

		JMenu CopyMenu = new JMenu("Copy");
		this.add(CopyMenu);
		
		CopyMenu.add(copyHostItem);
		CopyMenu.add(copyHostAndPortItem);
		CopyMenu.add(copyIPItem);
		CopyMenu.add(copyURLItem);
		CopyMenu.add(copyCommonURLItem);
		CopyMenu.add(copyLocationURLItem);
		CopyMenu.add(copyCDNAndCertInfoItem);
		CopyMenu.add(copyIconhashItem);
		
		this.addSeparator();

		this.add(removeItem);
		//this.add(removeSubDomainItem);
		this.add(NotTargetHandleItem);
		this.add(addToblackListItem);
		this.add(removeFromBlackListItem);
	}
}
