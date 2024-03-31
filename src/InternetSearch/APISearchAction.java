package InternetSearch;

import java.awt.event.ActionEvent;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import base.Commons;
import burp.BurpExtender;
import title.LineEntry;
import title.LineTableModel;



public class APISearchAction extends AbstractAction{

	/**
	 * TODO改造
	 */
	private static final long serialVersionUID = 1933197856582351336L;


	LineTableModel lineTableModel;
	int[] modelRows;
	int columnIndex;
	String engine;

	public APISearchAction(LineTableModel lineTableModel, int[] modelRows, int columnIndex,String engine) {
		super();
		
		this.lineTableModel = lineTableModel;
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
			String searchContent = getValueForSearch(row,columnIndex,engine);
			String url = buildSearchUrl(engine,searchContent);

			try {
				Commons.browserOpen(url, null);
			} catch (Exception err) {
				err.printStackTrace(BurpExtender.getStderr());
			}
		}
	}
	
		
	
	/**
	 * 返回可以用于网络搜索引擎进行搜索地字段
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getValueForSearch(int rowIndex, int columnIndex,String engine) {
		String columnName = LineTableModel.getTitleList().get(columnIndex);

		String[] Titles = new String[] {
				"Title","Comments","Server", "IP", "CNAME|CertInfo","ASNInfo","IconHash"};
		List<String> titleList = new ArrayList<>(Arrays.asList(Titles));

		String value =null;
		boolean isHost =false;
		if (titleList.contains(columnName)) {
			value = lineTableModel.getValueAt(rowIndex,columnIndex).toString();
		}else {
			LineEntry firstEntry = lineTableModel.getLineEntries().get(rowIndex);
			if (columnName.equalsIgnoreCase("Favicon")) {
				value = firstEntry.getIcon_hash().toString();
			}else {
				value = firstEntry.getHost();
				isHost = true;
			}
		}

		if (columnName.equalsIgnoreCase("Title")){
			if (engine.equalsIgnoreCase("google")) {
				value =  "intitle:"+value;
			}
		}else if (columnName.equalsIgnoreCase("Comments")){
		}else if (columnName.equalsIgnoreCase("Server")){
		}else if (columnName.equalsIgnoreCase("IP")){
		}else if (columnName.equalsIgnoreCase("CNAME|CertInfo")){
		}else if (columnName.equalsIgnoreCase("ASNInfo")){
		}else if (columnName.equalsIgnoreCase("Favicon") || columnName.equalsIgnoreCase("iconHash")){
			if (engine.equalsIgnoreCase("fofa")) {
				value = "icon_hash=\""+value+"\"";
			}
			else if (engine.equalsIgnoreCase("fofa")) {
				value = "http.favicon.hash:"+value;
			}
			else if (engine.equalsIgnoreCase("ZoomEye")) {
				value = "iconhash:"+value;
			}
		}else if (isHost){
			if (engine.equalsIgnoreCase("google")) {
				value = "site:"+value;
			}
		}

		return value;
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
		else if (engine.equalsIgnoreCase("ZoomEye")) {
			url= "https://www.zoomeye.org/searchResult?q="+searchContent;
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
