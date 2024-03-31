package InternetSearch;

import java.awt.event.ActionEvent;
import java.net.URLEncoder;
import java.util.Base64;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.table.AbstractTableModel;

import base.Commons;
import burp.BurpExtender;
import domain.target.TargetTableModel;
import title.LineTableModel;



public class BrowserSearchAction extends AbstractAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1933197856582351336L;


	AbstractTableModel tableModel;
	int[] modelRows;
	int columnIndex;
	String engine;

	public BrowserSearchAction(AbstractTableModel tableModel, int[] modelRows, int columnIndex,String engine) {
		super();

		this.tableModel = tableModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engine = engine;
		putValue(Action.NAME, "Search On "+capitalizeFirstLetter(engine.trim()));
	}


	@Override
	public final void actionPerformed(ActionEvent e) {

		if (modelRows.length >=50) {
			BurpExtender.getStderr().print("too many items selected!! should less than 50");
			return;
		}
		
		for (int row:modelRows) {
			String searchContent =null;
			if (tableModel.getClass().equals(LineTableModel.class)) {
				searchContent = ((LineTableModel)tableModel).getValueForSearch(row,columnIndex,engine);
			}

			if (tableModel.getClass().equals(TargetTableModel.class)) {
				searchContent = ((TargetTableModel)tableModel).getValueForSearch(row,columnIndex,engine);
			}

			if (searchContent != null) {
				String url = buildSearchUrl(engine,searchContent);

				try {
					Commons.browserOpen(url, null);
				} catch (Exception err) {
					err.printStackTrace(BurpExtender.getStderr());
				}
			}
		}
	}


	public String buildSearchUrl(String engine,String searchContent) {
		searchContent = URLEncoder.encode(searchContent);
		String url = null;
		if (engine.equalsIgnoreCase("google")) {
			url= "https://www.google.com/search?q="+searchContent;
		}
		else if (engine.equalsIgnoreCase("Github")) {
			url= "https://github.com/search?q=%22"+searchContent+"%22&type=Code";
		}
		else if (engine.equalsIgnoreCase("fofa")) {
			searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));
			url= "https://fofa.info/result?qbase64="+searchContent;
		}
		else if (engine.equalsIgnoreCase("shodan")) {
			url= "https://www.shodan.io/search?query="+searchContent;
		}
		else if (engine.equalsIgnoreCase("360Quake")) {
			url= "https://quake.360.net/quake/#/searchResult?searchVal="+searchContent;
		}
		else if (engine.equalsIgnoreCase("ti.qianxin.com")) {
			url= "https://ti.qianxin.com/v2/search?type=domain&value="+searchContent;
		}
		else if (engine.equalsIgnoreCase("ti.360.net")) {
			url= "https://ti.360.net/#/detailpage/searchresult?query="+searchContent;
		}
		else if (engine.equalsIgnoreCase("ZoomEye")) {
			url= "https://www.zoomeye.org/searchResult?q="+searchContent;
		}
		else if (engine.equalsIgnoreCase("hunter")) {
			url= "https://hunter.qianxin.com/list?search="+searchContent;
		}
		
		else if (engine.equalsIgnoreCase("hunter.io")) {
			url= "https://hunter.io/try/search/"+searchContent;;
		}
		return url;
	}

	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}
