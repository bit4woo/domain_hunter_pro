package domain.target;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import GUI.GUIMain;
import InternetSearch.Search;
import base.Commons;
import burp.BurpExtender;
import config.ConfigPanel;
import utils.DomainNameUtils;
import utils.GrepUtils;
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
		JMenuItem SearchEmailOnHunterIOItem = new JMenuItem(new AbstractAction("Seach Email On hunter.io") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					String url= "https://hunter.io/try/search/%s";
					//https://hunter.io/try/search/shopee.com?locale=en
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		JMenuItem SearchOnFoFaItem = new JMenuItem(new AbstractAction("Seach On fofa.info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					rootDomain = new String(Base64.getEncoder().encode(rootDomain.getBytes()));
					String url= "https://fofa.info/result?qbase64=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});



		JMenuItem SearchOnShodanItem = new JMenuItem(new AbstractAction("Seach On shodan.io") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://www.shodan.io/search?query=%s";
					//https://www.shodan.io/search?query=baidu.com
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		//360quake,zoomeye,hunter,shodan
		//https://quake.360.net/quake/#/searchResult?searchVal=baidu.com
		JMenuItem SearchOn360QuakeItem = new JMenuItem(new AbstractAction("Seach On quake.360.net") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://quake.360.net/quake/#/searchResult?searchVal=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		//https://ti.qianxin.com/v2/search?type=domain&value=example.com
		JMenuItem SearchOnTiQianxinItem = new JMenuItem(new AbstractAction("Seach On ti.qianxin.com") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://ti.qianxin.com/v2/search?type=domain&value=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		//https://ti.360.net/#/detailpage/searchresult?query=baidu.com
		JMenuItem SearchOnTi360Item = new JMenuItem(new AbstractAction("Seach On ti.360.net") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://ti.360.net/#/detailpage/searchresult?query=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		//https://hunter.qianxin.com/list?search=domain%3D%22example.com%22
		JMenuItem SearchOnHunterQianxinItem = new JMenuItem(new AbstractAction("Seach On hunter.qianxin.com") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					String domainPara = String.format("domain=\"%s\"",rootDomain);
					domainPara = URLEncoder.encode(domainPara);
					String url= "https://hunter.qianxin.com/list?search=%s";
					url= String.format(url, domainPara);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		//https://quake.360.net/quake/#/searchResult?searchVal=favicon%3A%20%22c5618c85980459ce4325eb324428d622%22


		JMenuItem SearchOnZoomEyeItem = new JMenuItem(new AbstractAction("Seach On zoomeye.org") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
					rootDomain = URLEncoder.encode(rootDomain);
					String url= "https://www.zoomeye.org/searchResult?q=%s";
					url= String.format(url, rootDomain);
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});


		JMenuItem SearchOnFoFaAutoItem = new JMenuItem(new AbstractAction("Auto Search On fofa.info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						String email = ConfigPanel.textFieldFofaEmail.getText();
						String key = ConfigPanel.textFieldFofaKey.getText();
						if (email.equals("") ||key.equals("")) {
							stdout.println("fofa.info emaill or key not configurated!");
							return null;
						}
						for (int row:modelRows) {
							String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
							String responseBody = Search.searchFofa(email,key,rootDomain);

							Set<String> domains = GrepUtils.grepDomain(responseBody);
							List<String> iplist = GrepUtils.grepIP(responseBody);
							stdout.println(String.format("%s: %s sub-domain names; %s ip addresses found by fofa.info",rootDomain,domains.size(),iplist.size()));
							guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().addAll(iplist);
							if (domains.size()==0 && iplist.size()==0) {
								stdout.println("fofa.info No assets found for ["+rootDomain+"], print reponse for debug");
								stdout.println(responseBody);
							}
						}
						return null;
					}

					@Override
					protected void done(){
					}
				};
				worker.execute();
			}
		});


		JMenuItem SearchOnQuakeAutoItem = new JMenuItem(new AbstractAction("Auto Search On quake.360.net") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						String key = ConfigPanel.textFieldQuakeAPIKey.getText();
						if (key.equals("")) {
							stdout.println("quake.360.net API key not configurated!");
							return null;
						}
						for (int row:modelRows) {
							String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
							String responseBody = Search.searchQuake(key,rootDomain);

							Set<String> domains = GrepUtils.grepDomain(responseBody);
							List<String> iplist = GrepUtils.grepIP(responseBody);
							stdout.println(String.format("%s: %s sub-domain names; %s ip addresses found by quake.360.net",rootDomain,domains.size(),iplist.size()));
							guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().addAll(iplist);
							if (domains.size()==0 && iplist.size()==0) {
								stdout.println("quake.360.net No assets found for ["+rootDomain+"], print reponse for debug");
								stdout.println(responseBody);
							}
						}
						return null;
					}

					@Override
					protected void done(){
					}
				};
				worker.execute();
			}
		});


		JMenuItem SearchOnHunterAutoItem = new JMenuItem(new AbstractAction("Auto Search On hunter.qianxin.com") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						String key = ConfigPanel.textFieldHunterAPIKey.getText();
						if (key.equals("")) {
							stdout.println("hunter.qianxin.com API key not configurated!");
							return null;
						}
						for (int row:modelRows) {
							String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,rootDomainColumnIndex);
							String responseBody = Search.searchHunter(key,rootDomain);

							Set<String> domains = GrepUtils.grepDomain(responseBody);
							List<String> iplist = GrepUtils.grepIP(responseBody);
							stdout.println(String.format("%s: %s sub-domain names; %s ip addresses found by hunter.qianxin.com",rootDomain,domains.size(),iplist.size()));
							guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().addAll(iplist);
							if (domains.size()==0 && iplist.size()==0) {
								stdout.println("hunter.qianxin.com No assets found for ["+rootDomain+"], print reponse for debug");
								stdout.println(responseBody);
							}
						}
						return null;
					}

					@Override
					protected void done(){
					}
				};
				worker.execute();
			}
		});

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
