package Tools;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.apache.commons.io.FileUtils;

public class JScrollPanelWithHeaderForTool extends JScrollPane{

	private JTextArea textArea;
	private JLabel headLabel;
	private String tipText;
	private String headLabelText;

	String inputTextAreaOldValue = "";
	String inputTextAreaNewerValue = "";

	public JScrollPanelWithHeaderForTool(String headerViewText, String tipText,boolean useTempFile) {

		this.textArea = new JTextArea() {
			@Override
			public void setText(String Text) {
				try {
					inputTextAreaOldValue = super.getText();
					if (useTempFile) {
						//避免大文件卡死整个burp
						if (Text.length() >= 10000) {
							File tmpFile = new File(FileUtils.getTempDirectory()+File.separator+"ContentIsInTmpFile.txt");
							FileUtils.writeByteArrayToFile(tmpFile, Text.getBytes());
							Text = tmpFile.getAbsolutePath();
						}
					}
					super.setText(Text);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public String getText() {
				String content = super.getText();
				if (useTempFile) {
					try {
						if (content.endsWith("ContentIsInTmpFile.txt")) {
							File tempFile = new File(content);
							if (tempFile.exists()){
								content = FileUtils.readFileToString(tempFile);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return content;
			}
		};


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
				if (inputTextAreaOldValue != null && !inputTextAreaOldValue.equals("")) {
					inputTextAreaNewerValue = textArea.getText();
					textArea.setText(inputTextAreaOldValue);
				}
			}
		});

		JButton rightButton = new JButton(" > ");
		rightButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (inputTextAreaNewerValue != null && !inputTextAreaNewerValue.equals("")) {
					inputTextAreaOldValue = textArea.getText();
					textArea.setText(inputTextAreaNewerValue);
				}
			}
		});

		headerViewPanel.add(leftButton);
		headerViewPanel.add(headLabel);
		headerViewPanel.add(rightButton);
		setColumnHeaderView(headerViewPanel);
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
