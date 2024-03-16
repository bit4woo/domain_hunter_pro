package InternetSearch;

import java.awt.Font;
import java.awt.FontMetrics;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;

import GUI.GUIMain;
import burp.BurpExtender;


public class SearchTable extends JTable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;

	public SearchTable(GUIMain guiMain)
	{
		//super(lineTableModel);//这个方法创建的表没有header
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.guiMain = guiMain;
	}


	public SearchTableModel getSearchTableModel(){
		return (SearchTableModel)getModel();
	}


	/**
	 * 必须在model设置过后调用才有效
	 */
	public void tableHeaderWidthinit(){
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int width = fm.stringWidth("A");//一个字符的宽度


		Map<String,Integer> preferredWidths = new HashMap<String,Integer>();
		preferredWidths.put("#",5);
		preferredWidths.put("URL",25);
		preferredWidths.put("Title",30);
		preferredWidths.put("IP",30);
		preferredWidths.put("CNAME|CertInfo",30);
		preferredWidths.put("Server",10);
		preferredWidths.put("IconHash", "-17480088888".length());
		for(String header:SearchTableModel.getTitleList()){
			try{//避免动态删除表字段时，出错
				if (preferredWidths.keySet().contains(header)){
					int multiNumber = preferredWidths.get(header);
					this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex(header)).setPreferredWidth(width*multiNumber);
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//配合横向滚动条
		
		//this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); 全自动调整列表，就用这个
	}
}