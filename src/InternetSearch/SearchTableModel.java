package InternetSearch;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import GUI.GUIMain;
import base.IndexedHashMap;
import burp.BurpExtender;
import title.LineEntry;
import title.LineTableModel;
import utils.DomainNameUtils;


public class SearchTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private IndexedHashMap<String,SearchResultEntry> lineEntries =new IndexedHashMap<String,SearchResultEntry>();

	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;

	private static final String[] standardTitles = new String[] {
			"#", "URL/Host", "Title","Server","Source","IP", "CertInfo","ASNInfo","Favicon","IconHash"};
	
	//为了实现动态表结构
	public static List<String> getTitleList() {
		List<String> titleList = new ArrayList<>(Arrays.asList(standardTitles));
		//titletList.remove("Time");
		return titleList;
	}

	public SearchTableModel(GUIMain guiMain){
		this.guiMain = guiMain;
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	public SearchTableModel(GUIMain guiMain,IndexedHashMap<String,SearchResultEntry> lineEntries){
		this(guiMain);
		this.lineEntries = lineEntries;
	}

	public SearchTableModel(GUIMain guiMain,List<SearchResultEntry> entries){
		this(guiMain);
		for (SearchResultEntry entry:entries) {
			lineEntries.put(entry.getIdentify(), entry);
		}
	}

	////////getter setter//////////


	public IndexedHashMap<String, SearchResultEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(IndexedHashMap<String, SearchResultEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}


	public SearchResultEntry getRowAt(int rowIndex) {
		return getLineEntries().get(rowIndex);
	}

	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return getTitleList().size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == getTitleList().indexOf("#")) {
			return Integer.class;//id
		}
		if (columnIndex == getTitleList().indexOf("Favicon")) {
			return ImageIcon.class;
		}
		return String.class;
	}

	@Override
	public int getRowCount()
	{
		return lineEntries.size();
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= getTitleList().size()) {
			return getTitleList().get(columnIndex);
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex >= lineEntries.size()) {
			return "IndexOutOfBoundsException";
		}
//		"#", "URL/Host", "Title","Server","Source","IP", "CertInfo","ASNInfo","Favicon","IconHash"};
		SearchResultEntry entry = lineEntries.get(rowIndex);
		if (columnIndex == getTitleList().indexOf("#")) {
			return rowIndex;
		}
		else if (columnIndex == getTitleList().indexOf("URL/Host")){
			return entry.getHost();
		}
		else if (columnIndex == getTitleList().indexOf("Title")){
			return entry.getTitle();
		}
		else if (columnIndex == getTitleList().indexOf("Server")){
			return entry.getWebcontainer();
		}

		else if (columnIndex == getTitleList().indexOf("IP")){
			return String.join(",", new TreeSet<String>(entry.getIPSet()));//用TreeSet进行排序
		}

		else if (columnIndex == getTitleList().indexOf("Favicon")){
			//return entry.getIcon_hash();
			byte[] data = entry.getIcon_bytes();
			String hash = entry.getIcon_hash();
			//排序比较是获取对象的toString()结果进行的。当ImageIcon有描述description时，toString()的值就是description。
			//所以传递hash作为描述，可以实现图标的点击排序，还和hash的排序一致
			if (data != null) {
				return new ImageIcon(data,hash);
			}
			return null;
		}
		else if (columnIndex == getTitleList().indexOf("IconHash")){
			return entry.getIcon_hash();
		}
		else if (columnIndex == getTitleList().indexOf("CertInfo")){
			return String.join(",", new TreeSet<String>(entry.getCertDomainSet()));
		}
		else if (columnIndex == getTitleList().indexOf("ASNInfo")){
			return entry.getASNInfo();
		}
		else if (columnIndex == getTitleList().indexOf("Source")) {
			return entry.getSource();
		}
		return "";
	}
	

	
	
	/**
	 * 返回可以用于网络搜索引擎进行搜索地字段
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getValueForSearch(int rowIndex, int columnIndex,String engine) {
		String columnName = getTitleList().get(columnIndex);

		String[] Titles = new String[] {
				"Title","URL/Host","Server", "IP", "CertInfo","ASNInfo","IconHash"};
		List<String> titleList = new ArrayList<>(Arrays.asList(Titles));

		String value =null;
		boolean isHost =false;
		if (titleList.contains(columnName)) {
			value = getValueAt(rowIndex,columnIndex).toString();
		}else {
			SearchResultEntry firstEntry = lineEntries.get(rowIndex);
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
			if (engine.equalsIgnoreCase("hunter")) {
				if (DomainNameUtils.isValidDomain(value)){
					value = "domain=\""+value+"\"";
				}
			}
		}

		return value;
	}

	///////////////////^^^多个行内容的增删查改^^^/////////////////////////////////

	public void addNewEntry(SearchResultEntry entry){
		if (entry == null) {
			return;
		}
		String key = entry.getIdentify();
		SearchResultEntry ret = lineEntries.put(key,entry);
		//以前的做法是，put之后再次统计size来判断是新增还是替换，这种方法在多线程时可能不准确，
		//concurrentHashMap的put方法会在替换时返回原来的值，可用于判断是替换还是新增
		int index = lineEntries.IndexOfKey(key);
		if (ret == null) {
			try {
				fireTableRowsInserted(index, index);
			} catch (Exception e) {
				//出错只会暂时影响显示，不影响数据内容，不再打印
				//e.printStackTrace(BurpExtender.getStderr());
				//BurpExtender.getStderr().println("index: "+index+" url: "+key);
			}
			//这里偶尔出现IndexOutOfBoundsException错误,
			// 但是debug发现javax.swing.DefaultRowSorter.checkAgainstModel在条件为false时(即未越界)抛出了异常，奇怪！
		}else {
			fireTableRowsUpdated(index, index);
		}
	}

}