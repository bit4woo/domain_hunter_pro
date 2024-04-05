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
						Commons.browserOpen("https://"+rootDomain, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
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
				while(Comments.trim().equals("")){
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


		for (String engine:SearchEngine.getCommonSearchEngineList()) {
			JMenuItem Item = new JMenuItem(new BrowserSearchAction(this.targetTableModel,modelRows,columnIndex,engine));
			this.add(Item);
		}

		this.addSeparator();
		
		List<JMenuItem> AssetSearchItems = new ArrayList<>();
		for (String engine:SearchEngine.getAssetSearchEngineList()) {
			JMenuItem Item = new JMenuItem(new BrowserSearchAction(this.targetTableModel,modelRows,columnIndex,engine));
			this.add(Item);
			AssetSearchItems.add(Item);
		}
		
		JMenuItem SearchAllItem = new JMenuItem(new AbstractAction("Browser Search On All Engines") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (JMenuItem item:AssetSearchItems) {
					item.doClick();
				}
			}
		});
		
		this.add(SearchAllItem);
		this.addSeparator();
		
		
		for (String engine:SearchEngine.getEmailSearchEngineList()) {
			JMenuItem Item = new JMenuItem(new BrowserSearchAction(this.targetTableModel,modelRows,columnIndex,engine));
			this.add(Item);
		}
		
		for (String engine:SearchEngine.getExtendInfoSearchEngineList()) {
			JMenuItem Item = new JMenuItem(new BrowserSearchAction(this.targetTableModel,modelRows,columnIndex,engine));
			this.add(Item);
		}
		this.addSeparator();
		
		List<JMenuItem> APIAssetSearchItems = new ArrayList<>();
		for (String engine:SearchEngine.getAssetSearchEngineList()) {
			JMenuItem Item = new JMenuItem(new APISearchAction(this.targetTableModel,modelRows,columnIndex,engine));
			this.add(Item);
		}
		
		
		
		JMenuItem SearchAllAutoItem = new JMenuItem(new AbstractAction("API Search On All Engines") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (JMenuItem item:APIAssetSearchItems) {
					item.doClick();
				}
			}
		});
		this.add(SearchAllItem);
		this.add(SearchAllAutoItem);
		
		
		this.addSeparator();
		
		this.add(OpenWithBrowserItem);
		this.add(zoneTransferCheck);
		this.add(zoneTransferCheckAll);
		
		this.addSeparator();
	}
}
