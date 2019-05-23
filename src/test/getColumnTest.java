package test;
    import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.GridLayout;

public class getColumnTest extends JPanel {
    public getColumnTest() {
        super(new GridLayout(1,0));

        JTable table = new JTable(new MyTableModel());

        JScrollPane scrollPane = new JScrollPane(table);

        add(scrollPane);

        table.getColumn("Column1").setCellRenderer(new TestCellRenderer());
        table.getColumn("Column2").setCellRenderer(new TestCellRenderer());
    }

    class TestCellRenderer extends DefaultTableCellRenderer{ }

    class MyTableModel extends AbstractTableModel {
        private String[] columnNames = { "Column1", "Column2" };
        private Object[][] data = { { "1", "1" }, { "2", "2" } };

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("TableDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getColumnTest newContentPane = new getColumnTest();
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);

        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
