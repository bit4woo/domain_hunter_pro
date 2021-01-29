package title;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;

class DirSearchAction implements ActionListener{

	public LineTable lineTable;
	int[] rows;

	public DirSearchAction(final LineTable lineTable, final int[] rows) {
		this.lineTable = lineTable;
		this.rows  = rows;
	}
	
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		try{
			java.util.List<String> urls = lineTable.getModel().getURLs(rows);
			String textUrls = String.join(System.lineSeparator(), urls);
			
//			String targetfile = URLsToFile(textUrls);TODO

			String batFilePathString  = genbatFile(textUrls);
			String command = dirSearchCommand(batFilePathString);
			Process process = Runtime.getRuntime().exec(command);
		}
		catch (Exception e1)
		{
			e1.printStackTrace(BurpExtender.getStderr());
		}
	}
	
	/*
	 * 请求包存入文件
	 */
	@Deprecated
	public String URLsToFile(String urls) {
		try {

			SimpleDateFormat simpleDateFormat = 
					new SimpleDateFormat("MMdd-HHmmss");
			String timeString = simpleDateFormat.format(new Date());
			String filename = "Target."+timeString+".txt";

			String basedir = (String) System.getProperties().get("java.io.tmpdir");

			File targetFile = new File(basedir,filename);
			FileUtils.writeByteArrayToFile(targetFile, urls.getBytes());
			return targetFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace(BurpExtender.getStderr());
			return null;
		}
	}
	
	public static String genbatFile(String targetFilePath) {
		try {
			String basedir = (String) System.getProperties().get("java.io.tmpdir");
			
			String command = ToolPanel.getLineConfig().getPython3Path()+" "+ToolPanel.getLineConfig().getDirSearchPath()+" -L "+targetFilePath+" -e jsp";

			//将命令写入剪切板
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(command.toString());
			clipboard.setContents(selection, null);
			
			File batFile = new File(basedir,"dirsearch-latest-command.bat");
			if (!batFile.exists()) {
			    batFile.createNewFile();
			}
			
			FileUtils.writeByteArrayToFile(batFile, command.toString().getBytes());
			return batFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace(BurpExtender.getStderr());
			return null;
		}
	}
	
	public static String dirSearchCommand(String batfilepath) {
		String command = "";
		if (Commons.isWindows()) {
			command="cmd /c start " + batfilepath;
		} else {
			if (new File("/bin/sh").exists()) {
				command="/bin/sh " + batfilepath;
			}
			else if (new File("/bin/bash").exists()) {
				command="/bin/bash " + batfilepath;
			}
		}
		return command;
	}
	
	public static void main(String[] args){
		try {
			//String command = "C:\\Python37\\python.exe D:\\github\\dirsearch\\dirsearch.py -L C:\\Users\\01374214\\AppData\\Local\\Temp\\Target.0820-180114.txt -e jsp";
			String batFilePathString  = genbatFile("C:\\Users\\01374214\\AppData\\Local\\Temp\\Target.0820-180114.txt");
			String command = dirSearchCommand(batFilePathString);
			Process process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
