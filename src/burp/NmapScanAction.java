package burp;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import Config.LineConfig;

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
			java.util.List<String> urls = lineTable.getModel().getURLs(rows);
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
			
			String nmapPath = ToolPanel.getLineConfig().getNmapPath();
			if (nmapPath.contains(" ")) {//如果路径中包含空格，需要引号
				nmapPath = "\""+nmapPath+"\"";
			}
			
			String command = nmapPath+" -v -A -p 1-65535 "+host.trim();

			//将命令写入剪切板
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(command.toString());
			clipboard.setContents(selection, null);
			
			File batFile = new File(basedir,"Nmap-latest-command.bat");
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
	
	public static void main(String[] args){
	}
}
