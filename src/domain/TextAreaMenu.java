package domain;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import burp.BurpExtender;
import burp.Commons;
import title.TitlePanel;
import title.search.SearchDork;

public class TextAreaMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	TextAreaMenu(final String selectedText){

        try{
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        }catch (Exception e){
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }
        
        List<String> selectedItems = Arrays.asList(selectedText.split(System.lineSeparator()));
        
        JMenuItem goToItem = new JMenuItem(new AbstractAction("Go To Tilte") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				
				JTabbedPane aa= (JTabbedPane) BurpExtender.getGui().getContentPane();
				aa.setSelectedIndex(1);
				//只会影响Domain Hunter中的选中，当选中的是proxy，使用这个方法并不能自动切换到domain hunter。
				//stdout.println(BurpExtender.getGui().getRootPane().getName());//null
				
				TitlePanel.getTextFieldSearch().setText(SearchDork.HOST.toString()+":"+selectedItems.get(0));
			}
		});
        this.add(goToItem);

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

		this.add(googleSearchItem);
		
		
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

		this.add(SearchOnGithubItem);
		
		JMenuItem addTosubdomain = new JMenuItem(new AbstractAction("Add To Sub-domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (selectedItems.size() >=50) {
					return;
				}
				DomainManager domainResult = DomainPanel.getDomainResult();
				for (String item:selectedItems) {
					try {
						domainResult.addToRootDomainAndSubDomain(item,true);
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				DomainPanel.autoSave();
			}
		});
		
		this.add(addTosubdomain);
		
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
		
		this.add(whoisItem);
	}
}
