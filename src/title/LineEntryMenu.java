package title;

import GUI.GUI;
import GUI.LineEntryMenuForBurp;
import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.Getter;
import burp.IBurpExtenderCallbacks;
import domain.DomainPanel;
import title.search.SearchDork;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LineEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private static LineTable lineTable;

	LineEntryMenu(final LineTable lineTable, final int[] rows,final int columnIndex){
		this.lineTable = lineTable;

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(rows.length+" Items Selected") {
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
				if (rows.length >=50) {
					return;
				}
				for (int row:rows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = getValue(firstEntry,columnIndex);
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
				if (rows.length >=50) {
					return;
				}
				for (int row:rows) {
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

				if (rows.length >=50) {
					return;
				}
				for (int row:rows) {
					LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(row);
					String searchContent = getValue(firstEntry,columnIndex);
					String url= "https://fofa.so/result?q=%22"+searchContent+"%22";
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

		JMenuItem SearchOnHunterItem = new JMenuItem(new AbstractAction("Seach On Hunter") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				LineEntry firstEntry = lineTable.getModel().getLineEntries().getValueAtIndex(rows[0]);
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
				}else if (columnName.equalsIgnoreCase("CDN")){
					String cdn = firstEntry.getCDN();
					TitlePanel.getTextFieldSearch().setText(cdn);
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
					java.util.List<String> urls = lineTable.getModel().getHosts(rows);
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
					java.util.List<String> items = lineTable.getModel().getHostsAndPorts(rows);
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


		JMenuItem copyURLItem = new JMenuItem(new AbstractAction("Copy URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getURLs(rows);
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
					java.util.List<String> urls = lineTable.getModel().getCommonURLs(rows);
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
		dirSearchItem.addActionListener(new DirSearchAction(lineTable, rows));

		JMenuItem doPortScan = new JMenuItem();
		doPortScan.setText("Do Port Scan");
		doPortScan.addActionListener(new NmapScanAction(lineTable, rows));


		JMenuItem openURLwithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser(double click url)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getURLs(rows);
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
					java.util.List<String> urls = lineTable.getModel().getURLs(rows);
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
				for (int i=rows.length-1;i>=0 ;i-- ) {
					try{
						LineEntry entry = entries.getValueAtIndex(rows[i]);

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
				TitlePanel.getTitleTableModel().updateRowsStatus(rows,LineEntry.CheckStatus_Checking);			
				java.util.List<String> urls = lineTable.getModel().getURLs(rows);
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
				TitlePanel.getTitleTableModel().updateRowsStatus(rows,LineEntry.CheckStatus_Checking);			
				java.util.List<String> urls = lineTable.getModel().getURLs(rows);
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
				TitlePanel.getTitleTableModel().updateRowsStatus(rows,LineEntry.CheckStatus_Checked);
				//				if (BurpExtender.rdbtnHideCheckedItems.isSelected()) {//实现自动隐藏，为了避免误操作，不启用
				//					String keyword = BurpExtender.textFieldSearch.getText().trim();
				//					lineTable.search(keyword);
				//				}
				BurpExtender.getGui().titlePanel.digStatus();
			}
		});


		JMenu assetTypeMenu = new JMenu("Set Asset Type As");
		LineEntryMenuForBurp.addLevelABC(assetTypeMenu, lineTable, rows);


		JMenuItem batchAddCommentsItem = new JMenuItem(new AbstractAction("Add Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String Comments = JOptionPane.showInputDialog("Comments", null).trim();
				while(Comments.trim().equals("")){
					Comments = JOptionPane.showInputDialog("Comments", null).trim();
				}
				TitlePanel.getTitleTableModel().updateComments(rows,Comments);
			}
		});


		JMenuItem copyLocationURLItem = new JMenuItem(new AbstractAction("Copy Location URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					List<String> urls = lineTable.getModel().getLocationUrls(rows);
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
					List<String> urls = lineTable.getModel().getCDNAndCertInfos(rows);
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
						for (int row: rows){
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
						for (int row: rows){
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

		//单纯从title记录中删除
		JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete This Entry") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().removeRows(rows);
				}else {
					return;
				}
				GUI.titlePanel.digStatus();
			}
		});
		
		//单纯从title记录中删除
		JMenuItem removeSubDomainItem = new JMenuItem(new AbstractAction("Delete Host From SubDomainSet") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these Hosts from SubDomainSet ?");
				if (result == JOptionPane.YES_OPTION) {
					//java.util.List<String> hosts = lineTable.getModel().getHosts(rows);//不包含端口，如果原始记录包含端口就删不掉
					//如果有 domain domain:8888 两个记录，这种方式就会删错对象
					java.util.List<String> hostAndPort = lineTable.getModel().getHostsAndPorts(rows);//包含端口，如果原始记录
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

		//认为资产不是目标资产，加入NotTargeIPSet
		JMenuItem addToblackListItem = new JMenuItem(new AbstractAction("Add Host To NotTargeIPSet") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to ADD Host(Must Be IP) to NotTargetIPSet ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().addHostToNotTargetIPSet(rows);
				}else {
					return;
				}
				GUI.titlePanel.digStatus();
			}
		});
		addToblackListItem.setToolTipText("If host is IP address,will be added to NotTargetIPSet");

		JMenuItem removeFromBlackListItem = new JMenuItem(new AbstractAction("Remove Host From NotTargeIPSet") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to REMOVE these Host(Must Be IP) from NotTargetIPSet ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().removeHostFromNotTargetIPSet(rows);
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

		this.add(addHostsToScope);
		this.add(doActiveScan);
		this.add(doPortScan);
		this.add(dirSearchItem);

		this.addSeparator();

		this.add(googleSearchItem);
		this.add(SearchOnGithubItem);
		this.add(SearchOnHunterItem);
		this.add(SearchOnFoFaItem);

		this.addSeparator();

		this.add(openURLwithBrowserItem);
		this.add(copyHostItem);
		this.add(copyHostAndPortItem);
		this.add(copyURLItem);
		this.add(copyCommonURLItem);
		this.add(copyLocationURLItem);
		this.add(copyCDNAndCertInfoItem);
		this.add(SendToRepeater);
		this.add(SendToRepeaterWithCookieItem);

		this.addSeparator();

		this.add(removeItem);
		this.add(removeSubDomainItem);
		this.add(addToblackListItem);
		this.add(removeFromBlackListItem);
	}
}
