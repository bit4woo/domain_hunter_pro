package test;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class JFrameTest extends  JFrame{

	public JFrameTest(){

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(100, 100, 1000, 500);
		setVisible(true);
		
		JPanel jPanel = new JPanel();
		jPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		jPanel.setLayout(new BorderLayout(0, 0));
        JLabel lblStatus = new JLabel("Status");
        jPanel.add(lblStatus, BorderLayout.NORTH);
        
        setContentPane(jPanel);
	}
}
