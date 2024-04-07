package InternetSearch;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import GUI.GUIMain;
import base.Commons;
import burp.BurpExtender;
import burp.IPAddressUtils;
import burp.SystemUtils;
import config.ConfigManager;
import config.ConfigName;
import domain.DomainManager;
import utils.DomainNameUtils;
import utils.GrepUtils;
import utils.PortScanUtils;

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

		
		JMenuItem copyHostItem = new JMenuItem(new AbstractAction("Copy URL/Host") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows,"URL/Host");
					String textUrls = String.join(System.lineSeparator(), urls);
					SystemUtils.writeToClipboard(textUrls);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyIPItem = new JMenuItem(new AbstractAction("Copy IP") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> ip_list = searchTableModel.getMultipleValue(modelRows,"IP");
					String textUrls = String.join(System.lineSeparator(), ip_list);
					SystemUtils.writeToClipboard(textUrls);
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
					java.util.List<String> ip_list = searchTableModel.getMultipleValue(modelRows,"IP");
					String nmapPath = ConfigManager.getStringConfigByKey(ConfigName.PortScanCmd);
					PortScanUtils.genCmdAndCopy(nmapPath, new HashSet<>(ip_list));
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
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows,"URL/Host");
					if (urls.size() >= 50){//避免一次开太多网页导致系统卡死
						return;
					}
					for (String url:urls){
						Commons.browserOpen(url,ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem addToTargetItem = new JMenuItem(new AbstractAction("Add To Target") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows,"URL/Host");
					String textUrls = String.join(System.lineSeparator(), urls);
					Set<String> Domains = GrepUtils.grepDomain(textUrls);
					List<String> ips = GrepUtils.grepIP(textUrls);
					
					DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
					for (String item:Domains) {
						try {
							domainResult.addToTargetAndSubDomain(item,true);
						} catch (Exception e2) {
							e2.printStackTrace(stderr);
						}
					}
					
					for (String item:ips) {
						try {
							if (IPAddressUtils.isValidIP(item)) {
								domainResult.getSpecialPortTargets().add(item);
							}
						} catch (Exception e2) {
							e2.printStackTrace(stderr);
						}
					}
					
					guiMain.getDomainPanel().saveDomainDataToDB();
					
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});
		
		this.add(itemNumber);

		this.addSeparator();

		//常用多选操作
		this.add(addToTargetItem);
		this.add(copyHostItem);
		this.add(copyIPItem);
		this.add(openURLwithBrowserItem);
		this.add(genPortScanCmd);


		this.addSeparator();
		SearchEngine.AddSearchMenuItems(this,searchTableModel,modelRows,columnIndex);
		this.addSeparator();

	}
}
