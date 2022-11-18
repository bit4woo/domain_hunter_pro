package test;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 结论：JTextArea的setText函数，会触发 REMOVE和INSERT两个事件！
 */
public class JTextAreaListenerTest {


	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame frame = new JFrameTest();

					JTextArea jText = new JTextArea();

					jText.setText("w3cschool.cn");

					jText.getDocument().addDocumentListener(new DocumentListener() {
						void print(DocumentEvent e) {
							System.out.println(e.getType());
						}

						@Override
						public void insertUpdate(DocumentEvent e) {
							print(e);

						}

						@Override
						public void removeUpdate(DocumentEvent e) {
							print(e);

						}

						@Override
						public void changedUpdate(DocumentEvent e) {
							print(e);
						}
					});

					JScrollPane scrollPane = new JScrollPane(jText);

					frame.getContentPane().add(scrollPane,BorderLayout.CENTER);

					JButton button = new JButton("change Data");
					button.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							jText.setText("ssssss");
						}

					});

					frame.getContentPane().add(button,BorderLayout.NORTH);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
