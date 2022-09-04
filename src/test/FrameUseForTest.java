package test;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FrameUseForTest extends JFrame {

	public FrameUseForTest() throws HeadlessException {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1000, 500);
		setVisible(true);
	}

	public static void main(String[] args) {
        example();
	}

    public static void example() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    JPanel RunnerPanel = new JPanel();
                    RunnerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
                    RunnerPanel.setLayout(new BorderLayout(0, 0));
                    JLabel lblStatus = new JLabel("Status");
                    RunnerPanel.add(lblStatus, BorderLayout.NORTH);

                    new FrameUseForTest().setContentPane(RunnerPanel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
