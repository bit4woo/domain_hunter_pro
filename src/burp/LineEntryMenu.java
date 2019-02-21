package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URL;

public class LineEntryMenu extends JPopupMenu {

    LineEntryMenu(final LineTable lineTable, final int[] rows){
    	
        JMenuItem googleSearchItem = new JMenuItem(new AbstractAction("Google it (double click)") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
				String host = lineTable.getModel().getLineEntries().get(rows[0]).getHost();
				String url= "https://www.google.com/search?q=site%3A"+host;
				try {
					URI uri = new URI(url);
					Desktop desktop = Desktop.getDesktop();
					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
            }
        });
        this.add(googleSearchItem);

        JMenuItem copyURLItem = new JMenuItem(new AbstractAction("Copy URL(s)") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    java.util.List<String> urls = lineTable.getModel().getURLs(rows);
                    String textUrls = String.join("\n", urls);

                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    StringSelection selection = new StringSelection(textUrls);
                    clipboard.setContents(selection, null);
                }
                catch (Exception e1)
                {
                    e1.printStackTrace(lineTable.getBurp().stderr);
                }
            }
        });
        this.add(copyURLItem);


        JMenuItem addHostsToScope = new JMenuItem(new AbstractAction("Add To Scope") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    java.util.List<String> urls = lineTable.getModel().getURLs(rows);
                    IBurpExtenderCallbacks callbacks = lineTable.getBurp().callbacks;
                    for(String url:urls) {
                        URL shortUrl = new URL(url);
                        callbacks.includeInScope(shortUrl);
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace(lineTable.getBurp().stderr);
                }
            }
        });
        this.add(addHostsToScope);

        
        JMenuItem removeItem = new JMenuItem(new AbstractAction("Remove Item") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().removeRows(rows);
            }
        });
        this.add(removeItem);
        
        JMenuItem checkedItem = new JMenuItem(new AbstractAction("Mark as checked") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().updateRows(rows);
            }
        });
        this.add(checkedItem);
        
        
        JMenuItem blackListItem = new JMenuItem(new AbstractAction("Add to black List") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().addBlackList(rows);
            }
        });
        blackListItem.setToolTipText("will not get title from next time");
        this.add(blackListItem);
    }
}
