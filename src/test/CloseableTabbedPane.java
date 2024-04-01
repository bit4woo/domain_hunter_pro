
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CloseableTabbedPane extends JFrame {
    public CloseableTabbedPane() {
        super("Closeable Tabbed Pane");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);

        JTabbedPane tabbedPane = new JTabbedPane();

        // 添加几个选项卡
        for (int i = 0; i < 5; i++) {
            JPanel panel = new JPanel();
            panel.add(new JLabel("This is tab " + (i + 1)));
            tabbedPane.addTab("Tab " + (i + 1), panel);
            tabbedPane.setTabComponentAt(i, new CloseButtonTab(tabbedPane, "Tab " + (i + 1)));
        }

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CloseableTabbedPane();
            }
        });
    }
}

class CloseButtonTab extends JPanel {
    private final JTabbedPane tabbedPane;
    private final String title;

    public CloseButtonTab(final JTabbedPane tabbedPane, final String title) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.tabbedPane = tabbedPane;
        this.title = title;
        setOpaque(false);

        JLabel label = new JLabel(title);
        add(label);

        JButton closeButton = new JButton("x");
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = tabbedPane.indexOfTab(title);
                if (index != -1) {
                    tabbedPane.removeTabAt(index);
                }
            }
        });
        add(closeButton);
    }
}
