package GUI;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import domain.TextAreaListener;
import domain.TextAreaMouseListener;

public class JScrollPanelWithHeader extends JScrollPane{
	
	private JTextArea textArea;
	private JLabel headLabel;
	private String tipText;
	private String headLabelText;
	
	public JScrollPanelWithHeader(String headerViewText,String tipText) {
		this.textArea = new JTextArea();
		this.tipText = tipText;
		this.headLabelText = headerViewText;
		
		textArea.setColumns(10);
		textArea.setToolTipText(this.tipText);
		textArea.getDocument().addDocumentListener(new TextAreaListener());
		textArea.addMouseListener(new TextAreaMouseListener(textArea));
		this.setViewportView(textArea);
		
		Border blackline = BorderFactory.createLineBorder(Color.black);
		headLabel = new JLabel(this.headLabelText);
		headLabel.setBorder(blackline);
		headLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.setColumnHeaderView(headLabel);
	}

	public JTextArea getTextArea() {
		return textArea;
	}

	public void setTextArea(JTextArea textArea) {
		this.textArea = textArea;
	}

	public JLabel getHeadLabel() {
		return headLabel;
	}

	public void setHeadLabel(JLabel headLabel) {
		this.headLabel = headLabel;
	}

	public String getTipText() {
		return tipText;
	}

	public void setTipText(String tipText) {
		this.tipText = tipText;
	}

	public String getHeadLabelText() {
		return headLabelText;
	}

	public void setHeadLabelText(String headLabelText) {
		this.headLabelText = headLabelText;
	}
}
