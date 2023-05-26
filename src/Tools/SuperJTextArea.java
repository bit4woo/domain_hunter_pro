package Tools;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;

public class SuperJTextArea extends JTextArea{

	String preValue = "";
	String nextValue = "";
	private boolean useTempFile;
	private boolean supportFileSystem;
	private boolean contentIsFileOrPath =false;
	private int location=-1;
	public static int maxLength = 100000;

	final String tempFilePath = FileUtils.getTempDirectory()+File.separator+"ContentIsInTmpFile.txt"; 

	/**
	 * 
	 * @param useTempFile 当文件内容过大时，将文件内容存入零时文件，避免程序卡死
	 * @param supportFileSystem 支持输入一个路径，读取内容时从路径中的文件读取文件的内容。
	 */
	public SuperJTextArea(boolean useTempFile,boolean supportFileSystem){
		this.useTempFile = useTempFile;
		this.supportFileSystem = supportFileSystem;

		Action action = getActionMap().get("paste-from-clipboard");
		getActionMap().put("paste-from-clipboard", new ProxyAction(action));
		//https://stackoverflow.com/questions/25276020/listen-to-the-paste-events-jtextarea



		this.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				SuperJTextArea.this.autoAdjustIsFile();
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				SuperJTextArea.this.autoAdjustIsFile();
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				SuperJTextArea.this.autoAdjustIsFile();
			}
		});
	}



	public boolean isContentIsFileOrPath() {
		return contentIsFileOrPath;
	}



	public void setContentIsFileOrPath(boolean contentIsFileOrPath) {
		this.contentIsFileOrPath = contentIsFileOrPath;
	}

	/**
	 * 换行符的可能性有三种，都必须考虑到
	 * @param input
	 * @return
	 */
	public static boolean isSingleLine(String input){
		String[] lines = input.split("(\r\n|\r|\n)", 2);
		return (lines.length)==1;
	}

	public void changeView(boolean isSelected) {
		try {
			((JScrollPanelWithHeaderForTool)this.getParent().getParent()).handleContentInFileOrPath.setSelected(isSelected);
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	public void autoAdjustIsFile() {
		String data = getTextAsDisplay();
		if (isSingleLine(data)) {
			if (new File(data).exists()) {
				contentIsFileOrPath = true;
				changeView(contentIsFileOrPath);
			}else{
				contentIsFileOrPath = false;
				changeView(contentIsFileOrPath);
			}
		}else {
			contentIsFileOrPath = false;
			changeView(contentIsFileOrPath);
		}
	}


	/**
	 * paste时，当选中了其中部分数据，应该使用替换逻辑
	 * @param pasteData
	 * @return
	 */
	private String insertPaste(String pasteData) {
		Caret caret = getCaret();
		int p0 = Math.min(caret.getDot(), caret.getMark());
		int p1 = Math.max(caret.getDot(), caret.getMark());

		try {
			Document doc = getDocument();
			String prefix = doc.getText(0, p0);
			//BurpExtender.getStderr().println(prefix);
			//String txt = doc.getText(p0, p1 - p0);
			String suffix = doc.getText(p1,doc.getLength()-p1);
			//BurpExtender.getStderr().println(suffix);
			//BurpExtender.getStderr().println(prefix+pasteData+suffix);
			location = p0+pasteData.length();

			return prefix+pasteData+suffix;

		} catch (BadLocationException e) {
			e.printStackTrace(BurpExtender.getStderr());
			return pasteData;
		}
	}

	@Override
	public void paste() {
		//javax.swing.text.JTextComponent.paste()
		//https://stackoverflow.com/questions/25276020/listen-to-the-paste-events-jtextarea
		//通过这个连接的答案，找到了paste的实现方法。这里进行重写，避免内容大时，卡死GUI。
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			String data = (String) clipboard.getData(DataFlavor.stringFlavor);
			data = insertPaste(data);
			setText(data);
			if (location>-1){//设置光标位置
				setSelectionStart(location);
				setSelectionEnd(location);
				location =-1;
			}
			//autoAdjustIsFile();
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
	}


	@Override
	public void setText(String Text) {
		try {
			preValue = super.getText();

			if (useTempFile) {
				//避免大文件卡死整个burp
				if (Text.length() >= maxLength) {
					File tmpFile = new File(tempFilePath);
					FileUtils.writeByteArrayToFile(tmpFile, Text.getBytes());
					Text = tmpFile.getAbsolutePath();
				}
			}
			super.setText(Text);
			//autoAdjustIsFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getText() {
		String content = super.getText();

		if (useTempFile) {
			try {
				if (content.equals(tempFilePath)) {
					File tempFile = new File(content);
					if (tempFile.exists()){
						content = FileUtils.readFileToString(tempFile);
						return content;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (contentIsFileOrPath) {
			content = super.getText();
			return readDirOrFileContent(content);
		}

		return content;
	}

	public String getTextAsDisplay() {
		return super.getText();
	}


	public String getTipsToShow() {
		try {
			int line = getLineOfOffset( getCaretPosition() );
			int start = getLineStartOffset( line );
			int end = getLineEndOffset( line );
			String lineText = getDocument().getText(start, end - start);
			String content = readDirOrFileContent(lineText);
			String length = "Length: "+content.length()+System.lineSeparator();
			if (content.length()<=500) {
				return length+content;
			} else {
				return length+content.substring(0,501);
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			return null;
		}
	}

	/**
	 * 显示上一个值
	 * @return
	 */
	public String showPreValue() {
		if (preValue != null && !preValue.equals("")) {
			nextValue = super.getText(); //获取原始值，也就是显示的值
			super.setText(preValue);
		}
		return super.getText();
	}

	/**
	 * 显示下一个值
	 * @return
	 */
	public String showNextValue() {
		if (nextValue != null && !nextValue.equals("")) {
			preValue = super.getText(); //获取原始值，也就是显示的值
			super.setText(nextValue);
		}
		return super.getText();
	}

	/**
	 * 
	 * @param content
	 * @return
	 */
	public static String readDirOrFileContent(String content) {
		String result = "";

		//FilenameFilter filter = new SuffixFileFilter(".txt",".csv",".json",".js");
		if (new File(content).exists()) {//内容是个文件名，或这路径名
			File fileOrPath = new File(content);
			if (fileOrPath.isDirectory()) {
				for (File item:fileOrPath.listFiles()) {
					result = result+readDirOrFileContent(item.getAbsolutePath());
				}
			}else {
				try {
					result = result + System.lineSeparator() + FileUtils.readFileToString(fileOrPath);
				} catch (Exception e) {
				}
			}
		};
		return result;
	}

	class ProxyAction extends AbstractAction {
		//https://stackoverflow.com/questions/25276020/listen-to-the-paste-events-jtextarea
		private Action action;

		public ProxyAction(Action action) {
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			action.actionPerformed(e);//paste动作的实现javax.swing.text.DefaultEditorKit.PasteAction.actionPerformed(ActionEvent)
			//BurpExtender.getStdout().println("Paste Occured...");
			//如果想要在paste的时候避免卡死，就要在这里实现，有这个必要吗？
			//
		}

	}

	public static void main(String[] args) {
		String aa= "";
		System.out.println(readDirOrFileContent(aa));
	}
};