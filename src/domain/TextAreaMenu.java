package domain;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import GUI.GUIMain;
import Tools.DomainComparator;
import Tools.LengthComparator;
import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.IPAddressUtils;
import config.ConfigPanel;
import title.TitlePanel;
import title.search.SearchDork;

public class TextAreaMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	JTextArea textArea;
	String selectedText;
	List<String> selectedItems = new ArrayList<>();

	TextAreaMenu(JTextArea textArea){

		this.textArea = textArea;
		selectedText = textArea.getSelectedText();
		if (selectedText != null && !selectedText.equalsIgnoreCase("")){
			selectedItems = Commons.textToLines(selectedText);
		}
		List<String> AllItems = Commons.getLinesFromTextArea(textArea);


		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		if (selectedItems.size() >0 ){
			JMenuItem NumberItem = new JMenuItem(new AbstractAction(selectedItems.size()+" Items Selected") {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
				}
			});
			this.add(NumberItem);
			this.addSeparator();
		}


		JMenuItem goToItem = new JMenuItem(new AbstractAction("Go To Tilte Panel") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				JTabbedPane aa= (JTabbedPane) BurpExtender.getGui().getContentPane();
				aa.setSelectedIndex(1);
				//只会影响Domain Hunter中的选中，当选中的是proxy，使用这个方法并不能自动切换到domain hunter。
				//stdout.println(BurpExtender.getGui().getRootPane().getName());//null
				if (selectedItems.size() >0 ) {
					TitlePanel.getTextFieldSearch().setText(SearchDork.HOST.toString() + ":" + selectedItems.get(0));
				}
			}
		});

		JMenuItem openWithBrowserItem = new JMenuItem(new AbstractAction("Open With Browser") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (selectedItems.size() >=50) {
					return;
				}
				for (String item:selectedItems) {
					String url= "https://"+item;
					try {
						Commons.browserOpen(url, ConfigPanel.getLineConfig().getBrowserPath());
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		JMenuItem googleSearchItem = new JMenuItem(new AbstractAction("Google It") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (selectedItems.size() >=50) {
					return;
				}
				for (String item:selectedItems) {
					String url= "https://www.google.com/search?q=%22"+URLEncoder.encode(item)+"%22";
					try {
						Commons.browserOpen(url, null);
					} catch (Exception e) {
						e.printStackTrace(stderr);
					}
				}
			}
		});

		JMenuItem SearchOnGithubItem = new JMenuItem(new AbstractAction("Seach On Github") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (selectedItems.size() >=50) {
					return;
				}
				for (String item:selectedItems) {
					try {
						String url= "https://github.com/search?q=%s&type=Code";
						String keyword= String.format("\"%s\" \"jdbc.url\"",item);
						URI uri = new URI(String.format(url, URLEncoder.encode(keyword)));
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


		JMenuItem addTosubdomain = new JMenuItem(new AbstractAction("Add To Sub-domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (selectedItems.size() >=50) {
					return;
				}
				DomainManager domainResult = DomainPanel.getDomainResult();
				for (String item:selectedItems) {
					try {
						domainResult.addToTargetAndSubDomain(item,true);
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				DomainPanel.saveDomainDataToDB();
			}
		});


		JMenuItem whoisItem = new JMenuItem(new AbstractAction("Whois") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (String item:selectedItems) {
					try {
						Commons.browserOpen("https://whois.chinaz.com/"+item,null);
						Commons.browserOpen("https://www.whois.com/whois/"+item,null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem ASNInfoItem = new JMenuItem(new AbstractAction("ASN Info") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (String target:selectedItems) {
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

		JMenuItem removeMd5DomainItem = new JMenuItem(new AbstractAction("Remove MD5 Domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DomainPanel.getDomainResult().removeMd5Domain();
			}
		});



		/////不需要选中内容的菜单
		JMenuItem Sort = new JMenuItem(new AbstractAction("Sort") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(AllItems);
				textArea.setText(String.join(System.lineSeparator(), AllItems));
			}
		});


		JMenuItem SortByLength = new JMenuItem(new AbstractAction("Sort By Length") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(AllItems,new LengthComparator());
				textArea.setText(String.join(System.lineSeparator(), AllItems));
			}
		});


		JMenuItem SortDomain = new JMenuItem(new AbstractAction("Sort Domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(AllItems,new DomainComparator());
				textArea.setText(String.join(System.lineSeparator(), AllItems));
			}
		});


		JMenuItem SortFresh = new JMenuItem(new AbstractAction("Fresh") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				GUIMain.getDomainPanel().showDataToDomainGUI();
			}
		});
		
		//https://blog.csdn.net/opshres169/article/details/51913713
		JMenuItem SearchDomain = new JMenuItem(new AbstractAction("Search") {
			int searchBegin = 0;

			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String keyword = JOptionPane.showInputDialog("Find What?");
				searchBegin = search(keyword,searchBegin);

				/**
				 try {
				 JTextArea parent = (JTextArea)actionEvent.getSource();

				 boolean enterListenerAdded = false;
				 KeyListener[] keyListeners = parent.getKeyListeners();
				 for (KeyListener item: keyListeners) {
				 if (item.getClass() == EnterListener.class) {
				 enterListenerAdded = true;
				 }
				 }
				 if (!enterListenerAdded) {
				 parent.addKeyListener(new EnterListener());
				 }
				 } catch (Exception e) {
				 e.printStackTrace();
				 }*/
			}

			/**
			 * 从特定的位置开始搜索
			 * @param searchBegin
			 * @return
			 */
			public int search(String keyword, int searchBegin) {
				if (searchBegin >= textArea.getText().length()) {
					searchBegin = 0;
				}

				int offset = textArea.getText().toLowerCase().indexOf(keyword.toLowerCase(),searchBegin);
				int length = keyword.length();

				if (offset != -1) {
					textArea.setSelectionStart(offset);
					textArea.setSelectionEnd(offset + length);
					textArea.requestFocus();
					//offset = TextArea.getText().indexOf(search, offset + 1);//查找下一个
				}
				return offset + 1;//下一次查找开始的位置
			}

			/**
			 * 会和文本编辑冲突
			 * @author bit4woo
			 *
			 */
			class EnterListener extends KeyAdapter {
				@Override
				public void keyPressed(KeyEvent evt){
					if (evt.getKeyCode() == KeyEvent.VK_N) {
						String keyword = textArea.getSelectedText();
						if (keyword == null || "".equals(keyword)) {
							searchBegin = search(keyword,searchBegin);
						}
					}
				}
			}
		});

		SortDomain.setToolTipText("search something");

		//对选中内容起作用的菜单
		this.add(whoisItem);
		this.add(ASNInfoItem);
		this.add(googleSearchItem);
		this.add(SearchOnGithubItem);
		this.add(openWithBrowserItem);
		this.add(addTosubdomain);
		this.addSeparator();

		//无需选中的全局菜单
		this.add(SortFresh);
		this.add(SearchDomain);
		this.add(Sort);
		this.add(SortDomain);
		this.add(SortByLength);
		this.add(goToItem);

		this.add(removeMd5DomainItem);
	}
}
