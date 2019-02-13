package test;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class tablewidth {
  public static void main(String[] argv) {
    int rows = 3;
    int cols = 3;
    JTable table = new JTable(rows, cols);

    // 
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);    
    TableColumn col = table.getColumnModel().getColumn(0);
    int width = 100;
    col.setPreferredWidth(width);
  }
}