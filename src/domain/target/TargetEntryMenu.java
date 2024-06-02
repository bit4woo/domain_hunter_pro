package domain.target;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import GUI.GUIMain;
import InternetSearch.APISearchAction;
import InternetSearch.BrowserSearchAction;
import InternetSearch.SearchEngine;
import base.Commons;
import burp.BurpExtender;
import config.ConfigManager;
import config.ConfigName;
import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.SystemUtils;

public class TargetEntryMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private TargetTable rootDomainTable;
	private GUIMain guiMain;
	private int rootDomainColumnIndex;
	private TargetTableModel targetTableModel;

	public TargetEntryMenu(GUIMain guiMain,final TargetTable rootDomainTable, final int[] modelRows, final int columnIndex){
		this.rootDomainTable = rootDomainTable;
		this.targetTableModel = rootDomainTable.getTargetModel();
		this.guiMain = guiMain;
		this.rootDomainColumnIndex =1;//设定为rootDomain所在列


		JMenuItem itemNumber = new JMenuItem(new AbstractAction(modelRows.length+" Items Selected") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
			}
		});

		JMenuItem getSubDomainsOf = new JMenuItem(new AbstractAction("Copy All Subdomins Of This") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				List<String> results = new ArrayList<String>();
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					String line = guiMain.getDomainPanel().getDomainResult().fetchSubDomainsOf(rootDomain);
					results.add(line);
				}

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(String.join(System.lineSeparator(), results));
				clipboard.setContents(selection, null);
			}
		});

		JMenuItem copyEmails = new JMenuItem(new AbstractAction("Copy All Emails Of This") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				List<String> results = new ArrayList<String>();
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					String line = guiMain.getDomainPanel().getDomainResult().fetchEmailsOf(rootDomain);
					results.add(line);
				}

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(String.join(System.lineSeparator(), results));
				clipboard.setContents(selection, null);
			}
		});

		JMenuItem zoneTransferCheck = new JMenuItem(new AbstractAction("Do Zone Transfer Check") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						for (int row:modelRows) {
							TargetEntry entry = rootDomainTable.getTargetModel().getTargetEntries().get(row);
							entry.zoneTransferCheck();
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

		JMenuItem zoneTransferCheckAll = new JMenuItem(new AbstractAction("Do Zone Transfer Check(All)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						rootDomainTable.getTargetModel().ZoneTransferCheckAll();
						return null;
					}
					@Override
					protected void done() {
					}
				};
				worker.execute();
			}
		});

		JMenuItem OpenWithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					try {
						SystemUtils.browserOpen("https://"+rootDomain, ConfigManager.getStringConfigByKey(ConfigName.BrowserPath));
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		JMenuItem batchAddCommentsItem = new JMenuItem(new AbstractAction("Add Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String Comments = JOptionPane.showInputDialog("Comments", null).trim();
				while(StringUtils.isBlank(Comments)){
					Comments = JOptionPane.showInputDialog("Comments", null).trim();
				}
				rootDomainTable.getTargetModel().updateComments(modelRows,Comments);
			}
		});

		JMenuItem batchClearCommentsItem = new JMenuItem(new AbstractAction("Clear Comments") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				rootDomainTable.getTargetModel().clearComments(modelRows);
			}
		});

		JMenuItem addToBlackItem = new JMenuItem(new AbstractAction("Add To Black List") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				guiMain.getDomainPanel().getControlPanel().selectedToBalck();
			}
		});


		this.add(itemNumber);
		this.add(getSubDomainsOf);
		this.add(copyEmails);
		this.add(batchAddCommentsItem);
		this.add(batchClearCommentsItem);
		this.add(addToBlackItem);
		this.addSeparator();

		SearchEngine.AddSearchMenuItems(this,targetTableModel,modelRows,columnIndex);
		
		this.addSeparator();
		
		this.add(OpenWithBrowserItem);
		this.add(zoneTransferCheck);
		this.add(zoneTransferCheckAll);
		
		this.addSeparator();
	}
}
