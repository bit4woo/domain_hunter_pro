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
import base.Commons;
import burp.BurpExtender;
import utils.DomainNameUtils;
import utils.IPAddressUtils;

public class TargetEntryMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private TargetTable rootDomainTable;
	private GUIMain guiMain;
	private int rootDomainColumnIndex;

	public TargetEntryMenu(GUIMain guiMain,final TargetTable rootDomainTable, final int[] modelRows, final int columnIndex){
		this.rootDomainTable = rootDomainTable;
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

		JMenuItem whoisItem = new JMenuItem(new AbstractAction("Whois") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					try {
						Commons.browserOpen("https://whois.chinaz.com/"+rootDomain,null);
						Commons.browserOpen("https://www.whois.com/whois/"+rootDomain,null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		JMenuItem ASNInfoItem = new JMenuItem(new AbstractAction("ASN Info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String target = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);

					try {
						//https://bgp.he.net/dns/shopee.com
						//https://bgp.he.net/net/143.92.111.0/24
						//https://bgp.he.net/ip/143.92.127.1
						String url =null;
						if (IPAddressUtils.isValidIP(target)){
							url = "https://bgp.he.net/ip/"+target;
						}
						if (IPAddressUtils.isValidSubnet(target)){
							url = "https://bgp.he.net/net/"+target;
						}
						if (DomainNameUtils.isValidDomain(target)){
							url = "https://bgp.he.net/dns/"+target;
						}
						if (url!= null){
							Commons.browserOpen(url,null);
						}
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
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

		/**
		 * 查找邮箱的搜索引擎
		 */
		JMenuItem SearchEmailOnHunterIOItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"hunter.io"));

		JMenuItem SearchOnFoFaItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"fofa"));

		JMenuItem SearchOnShodanItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"shodan"));

		JMenuItem SearchOn360QuakeItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"quake"));

		JMenuItem SearchOnTiQianxinItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"ti.qianxin.com"));

		JMenuItem SearchOnTi360Item = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"ti.360.net"));

		JMenuItem SearchOnHunterQianxinItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"hunter"));

		JMenuItem SearchOnZoomEyeItem = new JMenuItem(new BrowserSearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"zoomeye"));


		JMenuItem SearchOnFoFaAutoItem = new JMenuItem(new APISearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"fofa",true,true));

		JMenuItem SearchOnQuakeAutoItem = new JMenuItem(new APISearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"quake",true,true));

		JMenuItem SearchOnHunterAutoItem = new JMenuItem(new APISearchAction(this.rootDomainTable.getTargetModel(),modelRows,columnIndex,"hunter",true,true));


		JMenuItem SearchAllItem = new JMenuItem(new AbstractAction("Search On All Engines") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SearchOnFoFaItem.doClick();
				SearchOn360QuakeItem.doClick();
				SearchOnTi360Item.doClick();
				SearchOnTiQianxinItem.doClick();
				SearchOnHunterQianxinItem.doClick();
				SearchOnZoomEyeItem.doClick();
				SearchOnShodanItem.doClick();
			}
		});


		JMenuItem SearchAllAutoItem = new JMenuItem(new AbstractAction("Auto Search On All Engines") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SearchOnFoFaAutoItem.doClick();
				SearchOnQuakeAutoItem.doClick();
				SearchOnHunterAutoItem.doClick();
			}
		});

		this.add(itemNumber);
		this.add(getSubDomainsOf);
		this.add(copyEmails);
		this.add(batchAddCommentsItem);
		this.add(batchClearCommentsItem);
		this.add(addToBlackItem);
		this.addSeparator();

		this.add(SearchAllItem);
		this.add(SearchOnFoFaItem);
		this.add(SearchOn360QuakeItem);
		this.add(SearchOnTi360Item);
		this.add(SearchOnTiQianxinItem);
		this.add(SearchOnHunterQianxinItem);
		this.add(SearchOnZoomEyeItem);
		this.add(SearchOnShodanItem);
		this.addSeparator();

		this.add(SearchAllAutoItem);
		this.add(SearchOnFoFaAutoItem);
		this.add(SearchOnQuakeAutoItem);
		this.add(SearchOnHunterAutoItem);
		this.addSeparator();

		this.add(OpenWithBrowserItem);
		this.add(whoisItem);
		this.add(ASNInfoItem);
		this.add(SearchEmailOnHunterIOItem);
		this.add(zoneTransferCheck);
		this.add(zoneTransferCheckAll);

		this.addSeparator();
	}

}
