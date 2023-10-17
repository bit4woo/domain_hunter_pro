import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;

public class DisplayIcon2 {

    public static void main(String[] args) {
        new DisplayIcon2();
    }

    public DisplayIcon2() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new TestPane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public class TestPane extends JPanel {

        private final JTable statusTable;

        public TestPane() {
            statusTable = new javax.swing.JTable();
            statusTable.setRowHeight(200);

            statusTable.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{},
                    new String[]{
                        "Icons", "Message"
                    }
            ) {
                Class[] types = new Class[]{
                    javax.swing.ImageIcon.class, java.lang.String.class
                };
                boolean[] canEdit = new boolean[]{
                    false, false
                };

                public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }
            });
            setLayout(new BorderLayout());
            add(new JScrollPane(statusTable));

            JButton add = new JButton("Add");
            add(add, BorderLayout.SOUTH);
            add.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DefaultTableModel model = (DefaultTableModel) statusTable.getModel();
                    Object[] row = new Object[2]; // so I can add row to table

                    ImageIcon icon = new ImageIcon("C:/1.png");

                    row[0] = icon;
                    row[1] = "Boo";
                    model.addRow(row);
                }
            });
        }

    }

}