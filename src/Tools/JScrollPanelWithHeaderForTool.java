package Tools;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class JScrollPanelWithHeaderForTool extends JScrollPane{

	private SuperJTextArea textArea;
	private JLabel headLabel;
	private String tipText;
	private String headLabelText;
	public JRadioButton handleContentInFileOrPath;

	public JScrollPanelWithHeaderForTool(String headerViewText, String tipText,boolean useTempFile,boolean supportFileSystem) {

		this.textArea = new SuperJTextArea(useTempFile,supportFileSystem);


		this.tipText = tipText;
		this.headLabelText = headerViewText;

		textArea.setColumns(20);
		textArea.setLineWrap(true);
		textArea.setToolTipText(this.tipText);
		this.setViewportView(textArea);

		JPanel headerViewPanel = new JPanel();

		Border blackline = BorderFactory.createLineBorder(Color.black);
		headerViewPanel.setBorder(blackline);

		headLabel = new JLabel(this.headLabelText);
		headLabel.setHorizontalAlignment(SwingConstants.CENTER);

		JButton leftButton = new JButton(" < ");
		leftButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea.showPreValue();
			}
		});

		JButton rightButton = new JButton(" > ");
		rightButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea.showNextValue();
			}
		});

		handleContentInFileOrPath= new JRadioButton("Handle Content In File/Path");
		handleContentInFileOrPath.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea.setContentIsFileOrPath(handleContentInFileOrPath.isSelected());
			}
		});

		headerViewPanel.add(leftButton);
		headerViewPanel.add(headLabel);
		headerViewPanel.add(rightButton);
		if (supportFileSystem) {
			headerViewPanel.add(handleContentInFileOrPath);
		}
		setColumnHeaderView(headerViewPanel);
	}


	public SuperJTextArea getTextArea() {
		return textArea;
	}


	public void setTextArea(SuperJTextArea textArea) {
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
