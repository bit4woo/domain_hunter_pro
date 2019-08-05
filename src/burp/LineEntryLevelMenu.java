package burp;


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LineEntryLevelMenu extends JMenu {
    public String[] MainMenu = {"A", "B", "C"};

    public LineEntryLevelMenu(final LineTable lineTable, final int[] rows){
        this.setText("Set Level As");

        for(int i = 0; i < MainMenu.length; i++){
            JMenuItem item = new JMenuItem(MainMenu[i]);
            item.addActionListener((ActionListener) new LevelItemListener(lineTable,rows));
            this.add(item);
        }
    }

    class LevelItemListener implements ActionListener {

        LineTable lineTable;
        int[] rows;
        LevelItemListener(final LineTable lineTable, final int[] rows) {
            this.lineTable = lineTable;
            this.rows = rows;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            lineTable.getModel().updateLevelofRows(rows,e.getActionCommand());
        }
    }
}
