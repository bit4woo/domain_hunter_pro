package title;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.SystemUtils;

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
			for(String url:urls) {
				String cmd = SystemUtils.genCmd(ToolPanel.getLineConfig().getPython3Path(),
						ToolPanel.getLineConfig().getDirSearchPath(), " -u "+url+" -e jsp");
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
