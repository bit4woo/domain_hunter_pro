package burp;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

public class LineEntryMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	LineEntryMenu(final LineTable lineTable, final int[] rows){

        try{
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        }catch (Exception e){
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(rows.length+" Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});

		this.add(itemNumber);

		JMenuItem googleSearchItem = new JMenuItem(new AbstractAction("Google It (double click)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (rows.length >=50) {
					return;
				}
				for (int row:rows) {
					String host = lineTable.getModel().getLineEntries().get(row).getHost();
					String url= "https://www.google.com/search?q=site%3A"+host;
					try {
						URI uri = new URI(url);
						Desktop desktop = Desktop.getDesktop();
						if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
							desktop.browse(uri);
						}
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		});

		this.add(googleSearchItem);
		
		
		JMenuItem SearchOnGithubItem = new JMenuItem(new AbstractAction("Seach On Github") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (rows.length >=50) {
					return;
				}
				for (int row:rows) {
					try {
						String host = lineTable.getModel().getLineEntries().get(row).getHost();
						String url= "https://github.com/search?q=%22"+host+"%22+%22jdbc.url%22&type=Code";
						URI uri = new URI(url);
						Desktop desktop = Desktop.getDesktop();
						if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
							desktop.browse(uri);
						}
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
			}
		});

		this.add(SearchOnGithubItem);

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
		this.add(copyURLItem);

		JMenuItem openURLwithBrowserItem = new JMenuItem(new AbstractAction("Open URL With Browser") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = lineTable.getModel().getURLs(rows);
					if (urls.size() >= 50){//避免一次开太多网页导致系统卡死
						return;
					}
					for (String url:urls){
						Commons.open(url,"C:\\Program Files\\Mozilla Firefox\\firefox.exe");
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});
		this.add(openURLwithBrowserItem);


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
		this.add(addHostsToScope);

		JMenuItem doActiveScan = new JMenuItem(new AbstractAction("Do Active Scan") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<LineEntry> entries = lineTable.getModel().getLineEntries();
					IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
					for (int i=rows.length-1;i>=0 ;i-- ) {
						LineEntry entry = entries.get(rows[i]);

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
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});
		this.add(doActiveScan);


		JMenuItem checkedItem = new JMenuItem(new AbstractAction("Mark As Checked") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				BurpExtender.getGui().getTitlePanel().getTitleTableModel().updateRows(rows);
//				if (BurpExtender.rdbtnHideCheckedItems.isSelected()) {//实现自动隐藏，为了避免误操作，不启用
//					String keyword = BurpExtender.textFieldSearch.getText().trim();
//					lineTable.search(keyword);
//				}
				BurpExtender.getGui().titlePanel.digStatus();
			}
		});
		this.add(checkedItem);

		JMenuItem batchAddCommentsItem = new JMenuItem(new AbstractAction("Add Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String Comments = JOptionPane.showInputDialog("Comments", null).trim();
				while(Comments.trim().equals("")){
					Comments = JOptionPane.showInputDialog("Comments", null).trim();
				}
				BurpExtender.getGui().getTitlePanel().getTitleTableModel().updateComments(rows,Comments);
			}
		});
		this.add(batchAddCommentsItem);

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
		this.add(copyLocationURLItem);

		JMenuItem SendToRepeaterWithCookieItem = new JMenuItem(new AbstractAction("Send to repeater With Cookie") {
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
							LineEntry entry = lineTable.getModel().getLineEntries().get(row);
							String host =entry.getHost();
							int port = entry.getPort();
							String protocol = entry.getProtocol();
							boolean useHttps =false;
							if (protocol.equalsIgnoreCase("https")) {
								useHttps =true;
							}

							byte[] request = entry.getRequest();
							Getter getter = new Getter(callbacks.getHelpers());
							List<String> headers = getter.getHeaderList(true,request);
							headers.add("Cookie: "+cookieValue);
							byte[] body = getter.getBody(true,request);
							request = callbacks.getHelpers().buildHttpMessage(headers,body);

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
		this.add(SendToRepeaterWithCookieItem);


		this.addSeparator();

		JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().removeRows(rows);
				}else {
					return;
				}
				BurpExtender.getGui().titlePanel.digStatus();
			}
		});
		this.add(removeItem);

		JMenuItem blackListItem = new JMenuItem(new AbstractAction("Add To Black List") {//need to show dialog to confirm
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items and Add To BLACK LIST ?");
				if (result == JOptionPane.YES_OPTION) {
					lineTable.getModel().addBlackList(rows);
				}else {
					return;
				}
				BurpExtender.getGui().titlePanel.digStatus();
			}
		});
		blackListItem.setToolTipText("will not get title from next time");
		this.add(blackListItem);
	}
}
