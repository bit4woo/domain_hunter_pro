package title;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;
import burp.Commons;
import config.ConfigPanel;

class NmapScanAction implements ActionListener{

	public LineTable lineTable;
	int[] rows;

	public NmapScanAction(final LineTable lineTable, final int[] rows) {
		this.lineTable = lineTable;
		this.rows  = rows;
	}
	
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		try{
			java.util.List<String> urls = lineTable.getLineTableModel().getURLs(rows);
			for(String url:urls) {
				String host = new URL(url).getHost();
				String batFilePathString  = genbatFile(host);
				String command = NmapScanCommand(batFilePathString);
				Process process = Runtime.getRuntime().exec(command);
			}
		}
		catch (Exception e1)
		{
			e1.printStackTrace(BurpExtender.getStderr());
		}
	}
	
	public static String genbatFile(String host) {
		try {
			String basedir = (String) System.getProperties().get("java.io.tmpdir");
			
			String nmapPath = ConfigPanel.getLineConfig().getNmapPath();
			String command = nmapPath.replace("{host}", host.trim());

			//将命令写入剪切板
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(command.toString());
			clipboard.setContents(selection, null);
			
			File batFile = new File(basedir,"Nmap-latest-command.bat");
			batFile.deleteOnExit();
			batFile.createNewFile();
			
			if (Commons.isMac()){//这样才能弹出窗口
				command = String.format("osascript -e 'tell app \"Terminal\" to do script \"%s\"'",command);
			}
			FileUtils.writeByteArrayToFile(batFile, command.getBytes());
			return batFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace(BurpExtender.getStderr());
			return null;
		}
	}
	
	public static String NmapScanCommand(String batfilepath) {
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
	
	public static void main(String[] args) throws IOException, InterruptedException {
		//Process process = Runtime.getRuntime().exec("cmd /c start notepad.exe");
		Process process = Runtime.getRuntime().exec("/bin/sh /var/folders/z4/tnb4kh8s60x0361sg4qdw2gh0000gy/T/Nmap-latest-command.bat");
		process.waitFor();//等待执行完成
	}
}
