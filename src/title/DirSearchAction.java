package title;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.SystemUtils;

class DirSearchAction implements ActionListener{

	private final GUIMain guiMain;
	public LineTable lineTable;
	int[] rows;

	public DirSearchAction(GUIMain guiMain, final LineTable lineTable, final int[] rows) {
		this.guiMain = guiMain;
		this.lineTable = lineTable;
		this.rows  = rows;
	}
	
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		try{
			java.util.List<String> urls = lineTable.getLineTableModel().getURLs(rows);
			for(String url:urls) {
				//python dirsearch.py -t 8 --proxy=localhost:7890 --random-agent -e * -f -x 400,404,500,502,503,514,550,564 -u url
				String cmd = guiMain.getConfigPanel().getLineConfig().getDirSearchPath().replace("{url}", url);
				String batFilePathString  = SystemUtils.genBatchFile(cmd, "dirsearch-latest-command.bat");
				
				SystemUtils.runBatchFile(batFilePathString);
			}
		}
		catch (Exception e1)
		{
			e1.printStackTrace(BurpExtender.getStderr());
		}
	}
	
	public static void main(String[] args){
	}
}
