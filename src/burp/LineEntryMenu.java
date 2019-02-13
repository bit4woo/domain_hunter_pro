package burp;

import javax.swing.*;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

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
