package Tools;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;

public class SuperJTextArea extends JTextArea{

	String preValue = "";
	String nextValue = "";
	private boolean useTempFile;
	private boolean supportFileSystem;
	private boolean contentIsFileOrPath =false;
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
	}



	public boolean isContentIsFileOrPath() {
		return contentIsFileOrPath;
	}



	public void setContentIsFileOrPath(boolean contentIsFileOrPath) {
		this.contentIsFileOrPath = contentIsFileOrPath;
	}



	@Override
	public void setText(String Text) {
		try {
			preValue = super.getText();

			if (useTempFile) {
				//避免大文件卡死整个burp
				if (Text.length() >= 10000) {
					File tmpFile = new File(tempFilePath);
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
				if (content.equals(tempFilePath)) {
					File tempFile = new File(content);
					if (tempFile.exists()){
						content = FileUtils.readFileToString(tempFile);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (supportFileSystem && contentIsFileOrPath) {
			return readDirOrFileContent(content);
		}

		return content;
	}

	public String getTextAsDisplay() {
		return super.getText();
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
	public String readDirOrFileContent(String content) {
		String result = "";

		//FilenameFilter filter = new SuffixFileFilter(".txt",".csv",".json",".js");
		if (new File(content).exists()) {//内容是个文件名，或这路径名
			File fileOrPath = new File(content);
			if (fileOrPath.isDirectory()) {
				for (File item:fileOrPath.listFiles()) {
					if (item.canRead()) {
						try {
							result = result + System.lineSeparator() + FileUtils.readFileToString(item);
						} catch (Exception e) {
						}
					}
				};
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

		private Action action;

		public ProxyAction(Action action) {
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			action.actionPerformed(e);//paste动作的实现javax.swing.text.DefaultEditorKit.PasteAction.actionPerformed(ActionEvent)
			//BurpExtender.getStdout().println("Paste Occured...");
			//如果想要在paste的时候避免卡死，就要在这里实现，有这个必要吗？
		}

	}


};