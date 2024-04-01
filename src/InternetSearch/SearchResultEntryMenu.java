package InternetSearch;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import GUI.GUIMain;
import burp.BurpExtender;

public class SearchResultEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	GUIMain guiMain;
	SearchTable searchTable;
	SearchTableModel searchTableModel;

	/**
	 * 这处理传入的行index数据是经过转换的 model中的index，不是原始的JTable中的index。
	 * @param lineTable
	 * @param modelRows
	 * @param columnIndex
	 */
	SearchResultEntryMenu(final GUIMain guiMain, SearchTable searchTable,final int[] modelRows,final int columnIndex){
		this.guiMain = guiMain;
		this.searchTable = searchTable;
		this.searchTableModel = searchTable.getSearchTableModel();

		JMenuItem itemNumber = new JMenuItem(new AbstractAction(modelRows.length+" Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});

		JMenuItem googleSearchItem = new JMenuItem(new BrowserSearchAction(this.searchTableModel,modelRows,columnIndex,"Google"));

		JMenuItem SearchOnGithubItem = new JMenuItem(new BrowserSearchAction(this.searchTableModel,modelRows,columnIndex,"Github"));

		JMenuItem SearchOnFoFaItem = new JMenuItem(new BrowserSearchAction(this.searchTableModel,modelRows,columnIndex,"FoFa"));

		JMenuItem SearchOnShodanItem = new JMenuItem(new BrowserSearchAction(this.searchTableModel,modelRows,columnIndex,"Shodan"));

		JMenuItem SearchOn360QuakeItem = new JMenuItem(new BrowserSearchAction(this.searchTableModel,modelRows,columnIndex,"360Quake"));

		JMenuItem SearchOnZoomEyeItem = new JMenuItem(new BrowserSearchAction(this.searchTableModel,modelRows,columnIndex,"ZoomEye"));

		JMenuItem APISearchOnfofaItem = new JMenuItem(new APISearchAction(this.searchTableModel,modelRows,columnIndex,"fofa"));


		/*
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


		JMenuItem doPortScan = new JMenuItem(new AbstractAction("Do Run Port Scan") {

			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					Set<String> IPs = lineTable.getLineTableModel().getIPs(modelRows);
					String nmapPath = guiMain.getConfigPanel().getLineConfig().getNmapPath();

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

			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					List<String> IPs = lineTable.getLineTableModel().getHosts(modelRows);

					String nmapPath = guiMain.getConfigPanel().getLineConfig().getNmapPath();
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
						String cmd = guiMain.getConfigPanel().getLineConfig().getDirSearchPath().replace("{url}", url);
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
						Commons.browserOpen(url,guiMain.getConfigPanel().getLineConfig().getBrowserPath());
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
		*/


		this.add(itemNumber);

		this.addSeparator();

		//常用多选操作
		/*
		 * this.add(genPortScanCmd); this.add(genDirSearchCmd);
		 */


		this.addSeparator();

		JMenu DoMenu = new JMenu("Do");
		this.add(DoMenu);

		JMenu SearchMenu = new JMenu("Search");
		this.add(SearchMenu);
		SearchMenu.addSeparator();

		SearchMenu.add(googleSearchItem);
		SearchMenu.add(SearchOnGithubItem);
		SearchMenu.addSeparator();//通用搜索引擎和GitHub

		SearchMenu.add(SearchOnFoFaItem);
		SearchMenu.add(SearchOnShodanItem);
		SearchMenu.add(SearchOnZoomEyeItem);
		SearchMenu.add(SearchOn360QuakeItem);
		SearchMenu.addSeparator();

		SearchMenu.add(APISearchOnfofaItem);

		SearchMenu.addSeparator();//网络搜索引擎

		JMenu CopyMenu = new JMenu("Copy");
		this.add(CopyMenu);

		/*
		 * CopyMenu.add(copyHostItem); CopyMenu.add(copyHostAndPortItem);
		 * CopyMenu.add(copyHostAndIPAddressItem); CopyMenu.add(copyIPItem);
		 * CopyMenu.add(copyIPWithCommaItem);//常用 CopyMenu.add(copyIPWithSpaceItem);
		 * CopyMenu.add(copyURLItem); CopyMenu.add(copyURLOfIconItem);
		 * CopyMenu.add(copyCommonURLItem); CopyMenu.add(copyCDNAndCertInfoItem);
		 * CopyMenu.add(copyIconhashItem);
		 */

	}
}
