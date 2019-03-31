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
    	
        JMenuItem googleSearchItem = new JMenuItem(new AbstractAction("Google It (double click)") {
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

        JMenuItem copyURLItem = new JMenuItem(new AbstractAction("Copy URL") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    java.util.List<String> urls = lineTable.getModel().getURLs(rows);
                    String textUrls = String.join(System.lineSeparator(), urls);

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
        
        JMenuItem doActiveScan = new JMenuItem(new AbstractAction("Do Active Scan") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    java.util.List<LineEntry> entries = lineTable.getModel().getLineEntries();
                    IBurpExtenderCallbacks callbacks = lineTable.getBurp().callbacks;
                    for (int i=rows.length-1;i>=0 ;i-- ) {
                    	LineEntry entry = entries.get(rows[i]);
                    	
                        String host = entry.getHost();
                        int port = entry.getPort();
                        boolean useHttps;
						if (entry.getProtocol().equalsIgnoreCase("https")){
                        	useHttps = true;
                        }else {
							useHttps = false;
						}
                        byte[] request = entry.getRequest();

                        callbacks.includeInScope(new URL(entry.getUrl()));
                        callbacks.doActiveScan(host, port, useHttps, request);
                    }
                }
                catch (Exception e1)
                {
                    e1.printStackTrace(lineTable.getBurp().stderr);
                }
            }
        });
        this.add(doActiveScan);

        
        JMenuItem checkedItem = new JMenuItem(new AbstractAction("Mark As Checked") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().updateRows(rows);
            	BurpExtender.getTitleTableModel().updateRows(rows);
//            	if (lineTable.getName().equalsIgnoreCase("runnerTable")) {//it trigger this action in runner. need to update titleTableModel
//            		BurpExtender.getTitleTableModel().updateRows(rows);
//            	}
            }
        });
        this.add(checkedItem);
        
        this.addSeparator();
        
        JMenuItem removeItem = new JMenuItem(new AbstractAction("Delete") {//need to show dialog to confirm
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items ?");
            	if (result == JOptionPane.YES_OPTION) {
            		lineTable.getModel().removeRows(rows);
            	}else {
            		return;
            	}
            }
        });
        this.add(removeItem);
        
        JMenuItem blackListItem = new JMenuItem(new AbstractAction("Add To Black List") {//need to show dialog to confirm
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE these items and Add To BLACK LIST ?");
            	if (result == JOptionPane.YES_OPTION) {
            		lineTable.getModel().addBlackList(rows);
            	}else {
            		return;
            	}
            }
        });
        blackListItem.setToolTipText("will not get title from next time");
        this.add(blackListItem);
        
        this.addSeparator();//添加一个分割线，上面的需要选中，下面的无需
        
        //this(lineTable);
        JMenuItem hideCheckedItem = new JMenuItem(new AbstractAction("Hide All Checked Items") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().hideLines();
            }
        });
        this.add(hideCheckedItem);
        
        JMenuItem unhideAllItem = new JMenuItem(new AbstractAction("Show All Items") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().unHideLines();
            }
        });
        this.add(unhideAllItem);
    }
    
    
    //同名方法，在table中没有可显示内容时使用
    LineEntryMenu(final LineTable lineTable){
    	
        JMenuItem hideCheckedItem = new JMenuItem(new AbstractAction("Hide All Checked Items") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().hideLines();
            }
        });
        this.add(hideCheckedItem);
    	
        JMenuItem unhideAllItem = new JMenuItem(new AbstractAction("Show All Items") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
            	lineTable.getModel().unHideLines();
            }
        });
        this.add(unhideAllItem);
    }
}
