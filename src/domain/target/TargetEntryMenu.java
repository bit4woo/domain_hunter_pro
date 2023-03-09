package domain.target;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Base64;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.IPAddressUtils;

public class TargetEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private TargetTable rootDomainTable;
	private GUIMain guiMain;

	public TargetEntryMenu(GUIMain guiMain,final TargetTable rootDomainTable, final int[] modelRows, final int columnIndex){
		this.rootDomainTable = rootDomainTable;
		this.guiMain = guiMain;

		JMenuItem getSubDomainsOf = new JMenuItem(new AbstractAction("Get All Subdomin Of This") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String results = "";
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					String line = guiMain.getDomainPanel().getDomainResult().fetchSubDomainsOf(rootDomain);
					results = results+System.lineSeparator()+line;
				}

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(results);
				clipboard.setContents(selection, null);
			}
		});

		JMenuItem whoisItem = new JMenuItem(new AbstractAction("Whois") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					try {
						Commons.browserOpen("https://whois.chinaz.com/"+rootDomain,null);
						Commons.browserOpen("https://www.whois.com/whois/"+rootDomain,null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem ASNInfoItem = new JMenuItem(new AbstractAction("ASN Info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String target = (String) rootDomainTable.getTargetModel().getValueAt(row,0);

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
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem OpenWithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					try {
						Commons.browserOpen("https://"+rootDomain, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
					} catch (Exception e) {
						e.printStackTrace();
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

		JMenuItem addToBlackItem = new JMenuItem(new AbstractAction("Add To Black List") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				guiMain.getDomainPanel().getControlPanel().selectedToBalck();
			}
		});

		/**
		 * 查找邮箱的搜索引擎
		 */
		JMenuItem SearchOnHunterIOItem = new JMenuItem(new AbstractAction("Seach Email On hunter.io") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
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


		JMenuItem SearchOnFoFaItem = new JMenuItem(new AbstractAction("Seach On FoFa") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
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



		JMenuItem SearchOnShodanItem = new JMenuItem(new AbstractAction("Seach On Shodan") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
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
		JMenuItem SearchOn360QuakeItem = new JMenuItem(new AbstractAction("Seach On 360Quake") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
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

		//https://quake.360.net/quake/#/searchResult?searchVal=favicon%3A%20%22c5618c85980459ce4325eb324428d622%22


		JMenuItem SearchOnZoomEyeItem = new JMenuItem(new AbstractAction("Seach On ZoomEye") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				if (modelRows.length >=50) {
					return;
				}
				for (int row:modelRows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
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

		this.add(getSubDomainsOf);
		this.add(batchAddCommentsItem);
		this.add(addToBlackItem);
		this.addSeparator();

		this.add(SearchOnFoFaItem);
		this.add(SearchOnShodanItem);
		this.add(SearchOn360QuakeItem);
		this.add(SearchOnZoomEyeItem);
		this.addSeparator();

		this.add(OpenWithBrowserItem);
		this.add(whoisItem);
		this.add(ASNInfoItem);
		this.add(SearchOnHunterIOItem);
		this.addSeparator();
	}

}
