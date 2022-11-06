package GUI;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import domain.DomainPanel;
import domain.TextAreaListener;
import domain.TextAreaMouseListener;
import domain.TextAreaType;

public class JScrollPanelWithHeader extends JScrollPane{
	
	private JTextArea textArea;
	private JLabel headLabel;
	private String tipText;
	private String headLabelText;
	private TextAreaType textAreaType;
	private DomainPanel domainPanel;

	public JScrollPanelWithHeader(DomainPanel domainPanel, TextAreaType type, String headerViewText, String tipText) {
		this.domainPanel = domainPanel;
		this.textAreaType = type;
		this.textArea = new JTextArea();
		this.tipText = tipText;
		this.headLabelText = headerViewText;
		
		textArea.setColumns(10);
		textArea.setToolTipText(this.tipText);
		this.setViewportView(textArea);
		
		Border blackline = BorderFactory.createLineBorder(Color.black);
		headLabel = new JLabel(this.headLabelText);
		headLabel.setBorder(blackline);
		headLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.setColumnHeaderView(headLabel);

		textArea.getDocument().addDocumentListener(new TextAreaListener(domainPanel,this));
		textArea.addMouseListener(new TextAreaMouseListener(domainPanel,textArea));
	}

	
	public TextAreaType getTextAreaType() {
		return textAreaType;
	}


	public void setTextAreaType(TextAreaType textAreaType) {
		this.textAreaType = textAreaType;
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
