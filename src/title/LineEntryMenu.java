package title;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import com.bit4woo.utilbox.burp.HelperPlus;
import com.bit4woo.utilbox.utils.SystemUtils;

import ASN.ASNEntry;
import ASN.ASNQuery;
import GUI.GUIMain;
import InternetSearch.SearchEngine;
import Tools.ToolPanel;
import base.IndexedHashMap;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.LineEntryMenuForBurp;
import config.ConfigManager;
import config.ConfigName;
import title.search.SearchNumbericDork;
import title.search.SearchStringDork;
import utils.PortScanUtils;

public class LineEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private LineTable lineTable;
	private TitlePanel titlepanel;
	private GUIMain guiMain;
	private LineTableModel lineTableModel;

	/**
	 * 这处理传入的行index数据是经过转换的 model中的index，不是原始的JTable中的index。
	 * @param lineTable
	 * @param modelRows
	 * @param columnIndex
	 */
	LineEntryMenu(final GUIMain guiMain, final int[] modelRows,final int columnIndex){
		this.guiMain = guiMain;
		this.titlepanel = guiMain.getTitlePanel();
		this.lineTable = titlepanel.getTitleTable();
		this.lineTableModel = lineTable.getLineTableModel();

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(modelRows.length+" Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});


		JMenuItem SearchOnHunterItem = new JMenuItem(new AbstractAction("Seach This") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				LineEntry firstEntry = lineTable.getLineTableModel().getLineEntries().get(modelRows[0]);
				String columnName = lineTable.getColumnName(columnIndex);

				if (columnName.equalsIgnoreCase("Status")){
					int status = firstEntry.getStatuscode();
					titlepanel.getTextFieldSearch().setText(SearchNumbericDork.STATUS.toString()+":"+status);
				}else if (columnName.equalsIgnoreCase("length")){
					int length = firstEntry.getContentLength();
					titlepanel.getTextFieldSearch().setText(length+"");
				}else if (columnName.equalsIgnoreCase("title")){
					String title = firstEntry.getTitle();
					titlepanel.getTextFieldSearch().setText(SearchStringDork.TITLE.toString()+":"+title);
				}else if (columnName.equalsIgnoreCase("comments")){
					String comment = firstEntry.getComments().toString();
					titlepanel.getTextFieldSearch().setText(SearchStringDork.COMMENT.toString()+":"+comment);
				}else if (columnName.equalsIgnoreCase("IP")){
					String ip = firstEntry.getIPSet().toString();
					titlepanel.getTextFieldSearch().setText(ip);
				}else if (columnName.equalsIgnoreCase("CNAME|CertInfo")){
					String cdn = firstEntry.getCNAMESet().toString();
					titlepanel.getTextFieldSearch().setText(cdn);
				}else if (columnName.equalsIgnoreCase("IconHash")){
					String hash = firstEntry.getIcon_hash();
					titlepanel.getTextFieldSearch().setText(hash);
				}else {
					String host = firstEntry.getHost();
					titlepanel.getTextFieldSearch().setText(SearchStringDork.HOST.toString()+":"+host);
				}
			}
		});

		JMenuItem copyHostItem = new JMenuItem(new AbstractAction("Copy Host") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getLineTableModel().getHosts(modelRows);
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
					java.util.List<String> items = lineTable.getLineTableModel().getHostsAndPorts(modelRows);
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

		JMenuItem copyHostAndIPAddressItem = new JMenuItem(new AbstractAction("Copy Host+IPAddress") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> items = lineTable.getLineTableModel().getHostsAndIPAddresses(modelRows);
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
					Set<String> IPs = lineTable.getLineTableModel().getIPs(modelRows);
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

		/**
		 * 逗号分隔的IP地址，可以用于masscan扫描
		 * 空格分隔的IP地址，可以用于nmap扫描
		 */
		JMenuItem copyIPWithCommaItem = new JMenuItem(new AbstractAction("Copy IP Set (comma separated)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					Set<String> IPs = lineTable.getLineTableModel().getIPs(modelRows);
					String text = String.join(",", IPs);

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


		JMenuItem copyIPWithSpaceItem = new JMenuItem(new AbstractAction("Copy IP Set (space separated)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					Set<String> IPs = lineTable.getLineTableModel().getIPs(modelRows);
					String text = String.join(" ", IPs);

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
					java.util.List<String> urls = lineTable.getLineTableModel().getURLs(modelRows);
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
		
		JMenuItem copyDistinctURLItem = new JMenuItem(new AbstractAction("Copy URL deduplicate by IP") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getLineTableModel().getURLsDeduplicatedByIP(modelRows);
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
					java.util.List<String> urls = lineTable.getLineTableModel().getCommonURLs(modelRows);
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

		JMenuItem copyURLOfIconItem = new JMenuItem(new AbstractAction("Copy URL Of favicon.ico") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getLineTableModel().getURLsOfFavicon(modelRows);
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

		/**
		 * 获取用于Host碰撞的域名
		 */
		JMenuItem copyHostCollisionDomainsItem = new JMenuItem(new AbstractAction("Copy Domains For Host Collision") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					HashSet<String> domains = titlepanel.getTitleTable().getLineTableModel().getDomainsForBypassCheck();
					String textUrls = String.join(System.lineSeparator(), domains);

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
		dirSearchItem.setText("Do Run Dir Search");
		dirSearchItem.addActionListener(new DirSearchAction(guiMain,lineTable, modelRows));

		JMenuItem iconHashItem = new JMenuItem();
		iconHashItem.setText("Do Get Icon Hash");
		iconHashItem.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					IndexedHashMap<String, LineEntry> entries = lineTable.getLineTableModel().getLineEntries();
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

		JMenuItem doPortScan = new JMenuItem(new AbstractAction("Do Run Port Scan") {

			/**
			 * 逗号分隔的IP地址，可以用于masscan扫描
			 * 空格分隔的IP地址，可以用于nmap扫描
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					Set<String> IPs = lineTable.getLineTableModel().getIPs(modelRows);
					String nmapPath = ConfigManager.getStringConfigByKey(ConfigName.PortScanCmd);

					String command = PortScanUtils.genCmd(nmapPath, IPs);

					String filepath = SystemUtils.genBatchFile(command, "Nmap-latest-command.bat");
					SystemUtils.runBatchFile(filepath);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});


		JMenuItem genPortScanCmd = new JMenuItem(new AbstractAction("Copy Port Scan Cmd") {

			/**
			 * 逗号分隔的IP地址，可以用于masscan扫描
			 * 空格分隔的IP地址，可以用于nmap扫描
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					List<String> IPs = lineTable.getLineTableModel().getHosts(modelRows);

					String nmapPath = ConfigManager.getStringConfigByKey(ConfigName.PortScanCmd);
					PortScanUtils.genCmdAndCopy(nmapPath, new HashSet<>(IPs));
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem genDirSearchCmd = new JMenuItem(new AbstractAction("Copy DirSearch Cmd") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					java.util.List<String> urls = lineTable.getLineTableModel().getURLs(modelRows);
					java.util.List<String> cmds = new ArrayList<String>();
					for(String url:urls) {
						//python dirsearch.py -t 8 --proxy=localhost:7890 --random-agent -e * -f -x 400,404,500,502,503,514,550,564 -u url
						String cmd = ConfigManager.getStringConfigByKey(ConfigName.DirBruteCmd).replace("{url}", url);
						cmds.add(cmd);
					}
					SystemUtils.writeToClipboard(String.join(System.lineSeparator(), cmds));
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem openURLwithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser(double click url)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getLineTableModel().getURLs(modelRows);
					if (urls.size() >= 50){//避免一次开太多网页导致系统卡死
						return;
					}
					for (String url:urls){
						SystemUtils.browserOpen(url,ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
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
					java.util.List<String> urls = lineTable.getLineTableModel().getURLs(modelRows);
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


		JMenuItem requestAgain = new JMenuItem(new AbstractAction("Do Request Item Again") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker(){
					//to void "java.lang.RuntimeException: java.lang.RuntimeException: Extensions should not make HTTP requests in the Swing event dispatch thread"
					@Override
					protected Object doInBackground() throws Exception {
						IndexedHashMap<String,LineEntry> entries = lineTable.getLineTableModel().getLineEntries();
						for (int i=modelRows.length-1;i>=0 ;i-- ) {
							try{
								LineEntry entry = entries.get(modelRows[i]);
								entry.DoRequestAgain();
								lineTable.getLineTableModel().addNewLineEntry(entry);
							}
							catch (Exception e1)
							{
								e1.printStackTrace(stderr);
							}
						}
						return null;
					}
				}.execute();
			}
		});


		JMenuItem requestCertInfoAgain = new JMenuItem(new AbstractAction("Do Request Cert Info Again") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker(){
					//to void "java.lang.RuntimeException: java.lang.RuntimeException: Extensions should not make HTTP requests in the Swing event dispatch thread"
					@Override
					protected Object doInBackground() throws Exception {
						IndexedHashMap<String,LineEntry> entries = lineTable.getLineTableModel().getLineEntries();
						for (int i=modelRows.length-1;i>=0 ;i-- ) {
							try{
								LineEntry entry = entries.get(modelRows[i]);
								entry.DoRequestCertInfoAgain();
								lineTable.getLineTableModel().addNewLineEntry(entry);
							}
							catch (Exception e1)
							{
								e1.printStackTrace(stderr);
							}
						}
						return null;
					}
				}.execute();
			}
		});


		JMenuItem doActiveScan = new JMenuItem(new AbstractAction("Do Active Scan") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				IndexedHashMap<String,LineEntry> entries = lineTable.getLineTableModel().getLineEntries();
				IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
				for (int i=modelRows.length-1;i>=0 ;i-- ) {
					try{
						LineEntry entry = entries.get(modelRows[i]);

						String host = entry.getHost();
						int port = entry.getPort();
						boolean useHttps;
						if (entry.getProtocol().equalsIgnoreCase("https")){
							useHttps = true;
						}else {
							useHttps = false;
						}
						byte[] request = entry.getRequest();

						if (request ==null) {
							continue;
						}
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
				titlepanel.getTitleTable().getLineTableModel().updateRowsStatus(modelRows,LineEntry.CheckStatus_Checking);
				java.util.List<String> urls = lineTable.getLineTableModel().getURLs(modelRows);
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
				titlepanel.getTitleTable().getLineTableModel().updateRowsStatus(modelRows,LineEntry.CheckStatus_MoreAction);
				java.util.List<String> urls = lineTable.getLineTableModel().getURLs(modelRows);
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
				titlepanel.getTitleTable().getLineTableModel().updateRowsStatus(modelRows,LineEntry.CheckStatus_Checked);
				//				if (BurpExtender.rdbtnHideCheckedItems.isSelected()) {//实现自动隐藏，为了避免误操作，不启用
				//					String keyword = BurpExtender.textFieldSearch.getText().trim();
				//					lineTable.search(keyword);
				//				}
				titlepanel.digStatus();
			}
		});


		JMenu assetTypeMenu = new JMenu("Set Asset Type As");
		LineEntryMenuForBurp.addLevelABC(assetTypeMenu, lineTable, modelRows);


		JMenuItem batchAddCommentsItem = new JMenuItem(new AbstractAction("Add Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String Comments = JOptionPane.showInputDialog("Comments", null).trim();
				while(Comments.trim().equals("")){
					Comments = JOptionPane.showInputDialog("Comments", null).trim();
				}
				titlepanel.getTitleTable().getLineTableModel().updateComments(modelRows,Comments);
			}
		});

		JMenuItem batchClearCommentsItem = new JMenuItem(new AbstractAction("Clear Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker(){
					@Override
					protected Object doInBackground() throws Exception {
						titlepanel.getTitleTable().getLineTableModel().clearComments(modelRows);
						return null;
					}
				}.execute();
			}
		});

		JMenuItem batchRefreshASNInfoItem = new JMenuItem(new AbstractAction("Refresh ASN Info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				titlepanel.getTitleTable().getLineTableModel().freshASNInfo(modelRows);
			}
		});


		JMenuItem batchRefreshIconHashItem = new JMenuItem(new AbstractAction("Refresh Icon Hash") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker(){
					//to void "java.lang.RuntimeException: java.lang.RuntimeException: Extensions should not make HTTP requests in the Swing event dispatch thread"
					@Override
					protected Object doInBackground() throws Exception {
						titlepanel.getTitleTable().getLineTableModel().updateIconHashes(modelRows);
						return null;
					}
				}.execute();
			}
		});

		JMenuItem setASNAliasItem = new JMenuItem(new AbstractAction("Set ASN Alias") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				LineEntry firstEntry = lineTable.getLineTableModel().getLineEntries().get(modelRows[0]);
				String ip = firstEntry.getFirstIP();
				ASNEntry asnEntry = ASNQuery.getInstance().query(ip);
				String alias = JOptionPane.showInputDialog("Input Alias",asnEntry.getAsname_long());
				asnEntry.setAlias(alias);
				ASNQuery.saveRecentToFile();
				titlepanel.getTitleTable().getLineTableModel().freshASNInfo(modelRows);
			}
		});


		JMenuItem copyLocationURLItem = new JMenuItem(new AbstractAction("Copy Location URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					List<String> urls = lineTable.getLineTableModel().getLocationUrls(modelRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					SystemUtils.writeToClipboard(textUrls);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});


		JMenuItem copyContentSecurityPolicyItem = new JMenuItem(new AbstractAction("Copy Content-Security-Policy") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					List<String> urls = lineTable.getLineTableModel().getContentSecurityPolicy(modelRows);
					String textUrls = String.join(System.lineSeparator(), urls);

					SystemUtils.writeToClipboard(textUrls);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});


		JMenuItem copyHeaderValueItem = new JMenuItem(new AbstractAction("Copy Response Header Value") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					String value = JOptionPane.showInputDialog("input header name");
					if (value != null && value.length()>0) {
						List<String> results = lineTable.getLineTableModel().getHeaderValues(modelRows,false,value);
						String textUrls = String.join(System.lineSeparator(), results);
						SystemUtils.writeToClipboard(textUrls);
					}
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
					List<String> urls = lineTable.getLineTableModel().getCDNAndCertInfos(modelRows);
					String textUrls = String.join(System.lineSeparator(), urls);
					SystemUtils.writeToClipboard(textUrls);
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
					List<String> urls = lineTable.getLineTableModel().getIconHashes(modelRows);
					String textUrls = String.join(System.lineSeparator(), urls);
					SystemUtils.writeToClipboard(textUrls);
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
						for (int row: modelRows){
							LineEntry entry = lineTable.getLineTableModel().getLineEntries().get(row);
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
						for (int row: modelRows){
							LineEntry entry = lineTable.getLineTableModel().getLineEntries().get(row);
							String host =entry.getHost();
							int port = entry.getPort();
							String protocol = entry.getProtocol();
							boolean useHttps =false;
							if (protocol.equalsIgnoreCase("https")) {
								useHttps =true;
							}

							byte[] request = entry.getRequest();
							HelperPlus getter = BurpExtender.getHelperPlus();
							request = getter.addOrUpdateHeader(true, request, "Cookie", cookieValue);

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


		JMenuItem SendToToolPanel = new JMenuItem(new AbstractAction("Send To Tool Panel") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.

					@Override
					protected Map doInBackground() throws Exception {
						String result = "";
						for (int row: modelRows){
							LineEntry entry = lineTable.getLineTableModel().getLineEntries().get(row);

							String content = "";
							byte[] request = entry.getRequest();
							byte[] response = entry.getResponse();

							if (request!= null) {
								content = new String(request);
							}

							if (response!= null) {
								content = content+System.lineSeparator()+new String(response);
							}
							result = result+System.lineSeparator()+content;
						}

						ToolPanel.inputTextArea.setText(result);
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
		JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete Entry") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Map,Map>(){
					@Override
					protected Map doInBackground() throws Exception {
						int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
						if (result == JOptionPane.YES_OPTION) {
							lineTable.getLineTableModel().removeRows(modelRows);
							titlepanel.digStatus();
						}
						return null;
					}
				}.execute();
			}
		});
		removeItem.setToolTipText("Just Delete Entry In Title Panel");


		/**
		 * 删除明显非目标的记录
		 */
		JMenuItem removeItemsNotInTargets = new JMenuItem(new AbstractAction("Delete Entries That Not in Targets") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Map,Map>(){

					@Override
					protected Map doInBackground() throws Exception {
						int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
						if (result == JOptionPane.YES_OPTION) {
							lineTable.getLineTableModel().removeRowsNotInTargets();
							titlepanel.digStatus();
						}
						return null;
					}
				}.execute();
			}
		});


		JMenuItem markDuplicateItems = new JMenuItem(new AbstractAction("Mark Duplicate Items") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker<Map,Map>(){

					@Override
					protected Map doInBackground() throws Exception {
						int result = JOptionPane.showConfirmDialog(null,"Are you sure to Mark duplicate items ?");
						if (result == JOptionPane.YES_OPTION) {
							lineTable.getLineTableModel().findAndMarkDuplicateItems();
						}
						return null;
					}
				}.execute();
			}
		});



		/**
		 * 从子域名列表中删除对应资产，表明当前host（应该是一个IP）不是我们的目标资产。
		 * 那么应该同时做以下三点：
		 * 1、从domain panel中的SubDomainSet移除。
		 * 2、从title panel中删除记录。
		 * 3、把目标加入黑名单，以便下次跑网段如果有相同IP可以标记出来。
		 */
		@Deprecated
		JMenuItem removeSubDomainItem = new JMenuItem(new AbstractAction("Delete Host From SubDomain Set") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Delete these hosts from sub-domain set?");
				if (result == JOptionPane.YES_OPTION) {
					//java.util.List<String> hosts = lineTable.getLineTableModel().getHosts(rows);//不包含端口，如果原始记录包含端口就删不掉
					//如果有 domain domain:8888 两个记录，这种方式就会删错对象
					java.util.List<String> hostAndPort = lineTable.getLineTableModel().getHostsAndPorts(modelRows);//包含端口，如果原始记录
					for(String item:hostAndPort) {
						if (!guiMain.getDomainPanel().getDomainResult().getSubDomainSet().remove(item)) {
							guiMain.getDomainPanel().getDomainResult().getSubDomainSet().remove(item.split(":")[0]);
						}
					}
				}
			}
		});
		removeSubDomainItem.setToolTipText("Delete Host From Subdomain Set In Domain Panel");


		JMenuItem removeCustomAssetItem = new JMenuItem(new AbstractAction("Delete Host From Custom Assets") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Delete these hosts from Custom Assets?");
				if (result == JOptionPane.YES_OPTION) {
					//java.util.List<String> hosts = lineTable.getLineTableModel().getHosts(rows);//不包含端口，如果原始记录包含端口就删不掉
					//如果有 domain domain:8888 两个记录，这种方式就会删错对象
					java.util.List<String> hostAndPort = lineTable.getLineTableModel().getHostsAndPorts(modelRows);//包含端口，如果原始记录
					for(String item:hostAndPort) {
						if (!guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().remove(item)) {
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().remove(item.split(":")[0]);
						}
					}
				}
			}
		});
		removeSubDomainItem.setToolTipText("Delete Host From Subdomain Set In Domain Panel");

		
		JMenuItem DeleteHostFromTargetItem = new JMenuItem(new AbstractAction("Delete Host From Target") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Delete these hosts from Target(Subdomains and Custom Assets)?");
				if (result == JOptionPane.YES_OPTION) {
					//java.util.List<String> hosts = lineTable.getLineTableModel().getHosts(rows);//不包含端口，如果原始记录包含端口就删不掉
					//如果有 domain domain:8888 两个记录，这种方式就会删错对象
					java.util.List<String> hostAndPort = lineTable.getLineTableModel().getHostsAndPorts(modelRows);//包含端口，如果原始记录
					for(String item:hostAndPort) {
						if (!guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().remove(item)) {
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().remove(item.split(":")[0]);
						}
						if (!guiMain.getDomainPanel().getDomainResult().getSubDomainSet().remove(item)) {
							guiMain.getDomainPanel().getDomainResult().getSubDomainSet().remove(item.split(":")[0]);
						}
					}
				}
			}
		});
		removeSubDomainItem.setToolTipText("Delete Host From Subdomain Set In Domain Panel");

		/**
		 * 黑名单主要用于记录CDN或者云服务IP，避免计算网段时包含这些IP。
		 */
		JMenuItem addToblackListItem = new JMenuItem(new AbstractAction("Add IP To Black List") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Add these IP to black list?" +
						"\n\rwill exclude them when calculate subnet");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getLineTableModel().addIPToTargetBlackList(modelRows);
				}
			}
		});
		addToblackListItem.setToolTipText("IP addresses will be added to Black List");

		/**
		 * 黑名单主要用于记录CDN或者云服务IP，避免计算网段时包含这些IP。
		 */
		JMenuItem addToblackListAndDeleteItem = new JMenuItem(new AbstractAction("Add IP To Black List And Delete Entry") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Add these IP to black list and Delete entry?" +
						"\n\rwill exclude them when calculate subnet");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getLineTableModel().addIPToTargetBlackList(modelRows);
					lineTable.getLineTableModel().removeRows(modelRows);
					titlepanel.digStatus();
				}
			}
		});
		addToblackListItem.setToolTipText("IP addresses will be added to Black List");

		this.add(itemNumber);

		this.addSeparator();

		this.add(checkingItem);
		this.add(moreActionItem);
		this.add(checkedItem);

		this.addSeparator();

		//常用多选操作
		this.add(assetTypeMenu);
		this.add(doActiveScan);
		this.add(genPortScanCmd);
		this.add(genDirSearchCmd);
		this.add(batchAddCommentsItem);


		this.addSeparator();

		JMenu DoMenu = new JMenu("Do");
		this.add(DoMenu);
		//burp相关行为
		DoMenu.add(addHostsToScope);
		DoMenu.add(SendToRepeater);
		DoMenu.add(SendToRepeaterWithCookieItem);
		DoMenu.add(SendToToolPanel);
		DoMenu.add(requestAgain);
		DoMenu.add(requestCertInfoAgain);

		//DoMenu.add(doActiveScan);常用

		DoMenu.addSeparator();
		//外部程序相关行为
		DoMenu.add(openURLwithBrowserItem);
		DoMenu.add(doPortScan);
		DoMenu.add(dirSearchItem);
		//DoMenu.add(doGateWayByPassCheck);
		//this.add(iconHashItem);
		DoMenu.addSeparator();
		DoMenu.add(batchRefreshASNInfoItem);
		DoMenu.add(batchRefreshIconHashItem);
		DoMenu.add(batchClearCommentsItem);

		//DoMenu.add(setASNAliasItem);//基本用不上


		JMenu CopyMenu = new JMenu("Copy");
		this.add(CopyMenu);

		CopyMenu.add(copyHostItem);
		CopyMenu.add(copyHostAndPortItem);
		CopyMenu.add(copyHostAndIPAddressItem);
		CopyMenu.add(copyIPItem);
		CopyMenu.add(copyIPWithCommaItem);//常用
		CopyMenu.add(copyIPWithSpaceItem);
		CopyMenu.add(copyURLItem);
		CopyMenu.add(copyDistinctURLItem);
		CopyMenu.add(copyURLOfIconItem);
		CopyMenu.add(copyCommonURLItem);
		CopyMenu.add(copyHostCollisionDomainsItem);
		CopyMenu.add(copyLocationURLItem);
		CopyMenu.add(copyContentSecurityPolicyItem);
		CopyMenu.add(copyHeaderValueItem);
		CopyMenu.add(copyCDNAndCertInfoItem);
		CopyMenu.add(copyIconhashItem);
		
		this.addSeparator();
		SearchEngine.AddSearchMenuItems(this,lineTableModel,modelRows,columnIndex);
		this.add(SearchOnHunterItem);//在插件内搜索

		this.addSeparator();
		this.add(addToblackListItem);//加入黑名单
		this.add(addToblackListAndDeleteItem);//加入黑名单并删除
		this.add(removeItem);//单纯删除记录
		this.add(removeItemsNotInTargets);
		//this.add(removeSubDomainItem);
		//this.add(removeCustomAssetItem);
		this.add(DeleteHostFromTargetItem);
		this.add(markDuplicateItems);

	}

	/**
	 * 只返回有搜索价值的字段，如果鼠标位置未对应有价值的字段，默认返回host字段。
	 * @param firstEntry
	 * @param columnIndex
	 * @return
	 */
	@Deprecated
	public String getSearchValueFromEntry11(LineEntry firstEntry,int columnIndex) {

		String columnName = lineTable.getColumnName(columnIndex);

		if (columnName.equalsIgnoreCase("Title")){
			String title = firstEntry.getTitle();
			return title;
		}else if (columnName.equalsIgnoreCase("Comments")){
			String comment = firstEntry.getComments().toString();
			return comment;
		}else if (columnName.equalsIgnoreCase("IP")){
			String ip = firstEntry.getIPSet().toString();
			return ip;
		}else if (columnName.equalsIgnoreCase("CNAME|CertInfo")){
			String cdn = firstEntry.getCNAMESet().toString();
			return cdn;
		}else if (columnName.equalsIgnoreCase("Favicon") || columnName.equalsIgnoreCase("iconHash")){
			String hash = firstEntry.getIcon_hash().toString();
			return hash;
		}else {
			String host = firstEntry.getHost();
			return host;
		}
	}
}