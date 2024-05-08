package InternetSearch;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import com.bit4woo.utilbox.utils.SystemUtils;

import GUI.GUIMain;
import base.Commons;
import burp.BurpExtender;
import config.ConfigManager;
import config.ConfigName;
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


		JMenuItem copyUrlItem = new JMenuItem(new AbstractAction("Copy URL") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows,"URL");
					String textUrls = String.join(System.lineSeparator(), urls);
					SystemUtils.writeToClipboard(textUrls);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem copyHostItem = new JMenuItem(new AbstractAction("Copy Host") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					java.util.List<String> urls = searchTableModel.getMultipleValue(modelRows,"Host");
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
						SystemUtils.browserOpen(url,ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
					}
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem addToTargetItem = new JMenuItem(new AbstractAction("Add Host/Domain To Target") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new SwingWorker(){
					@Override
					protected Object doInBackground() throws Exception {
						try{
							List<SearchResultEntry> entries = searchTableModel.getEntries(modelRows);
							for (SearchResultEntry entry:entries) {
								entry.AddToTarget();
							}
							guiMain.getDomainPanel().saveDomainDataToDB();
						}
						catch (Exception e1)
						{
							e1.printStackTrace(stderr);
						}
						return null;
					}
				}.execute();

			}
		});

		this.add(itemNumber);

		this.addSeparator();

		//常用多选操作
		this.add(addToTargetItem);
		this.add(copyUrlItem);
		this.add(copyHostItem);
		this.add(copyIPItem);
		this.add(openURLwithBrowserItem);
		this.add(genPortScanCmd);


		this.addSeparator();
		SearchEngine.AddSearchMenuItems(this,searchTableModel,modelRows,columnIndex);
		this.addSeparator();

	}
}
