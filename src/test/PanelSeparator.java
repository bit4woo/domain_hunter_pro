package test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

public class PanelSeparator
{
	JFrame f = null;
	public PanelSeparator()
	{
		f = new JFrame("Separator Example");
		Container contentPane = f.getContentPane();
		contentPane.setLayout(new BorderLayout(1,2));
		JPanel panel1 = new JPanel(new GridLayout(2,1));
		JLabel label = new JLabel("水平7a64e78988e69d8331333262373965分隔线",JLabel.CENTER);
		JSeparator seph = new JSeparator();
		panel1.add(label);
		panel1.add(seph);
		JTextArea textarea = new JTextArea();
		textarea.setPreferredSize(new Dimension(150,100));
		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.add(panel1,BorderLayout.NORTH);
		panel2.add(textarea,BorderLayout.CENTER);
		JPanel panel3 = new JPanel(new GridLayout(1,3));
		label = new JLabel("垂直");
		label.setVerticalAlignment(JLabel.CENTER);
		panel3.add(label);
		JSeparator sepv = new JSeparator();
		sepv.setOrientation(JSeparator.VERTICAL);
		panel3.add(sepv);
		contentPane.add(panel2,BorderLayout.CENTER);
		contentPane.add(panel3,BorderLayout.EAST);
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	}
	public static void main(String[] arg)
	{
		new PanelSeparator();
	}
}