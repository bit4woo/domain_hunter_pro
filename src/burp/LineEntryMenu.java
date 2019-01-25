package burp;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class LineEntryMenu extends JPopupMenu {

    LineEntryMenu(final LineTable lineTable, final int[] rows){
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
