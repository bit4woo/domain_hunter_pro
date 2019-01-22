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
    }
}
