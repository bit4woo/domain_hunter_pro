package InternetSearch;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import com.bit4woo.utilbox.utils.IPAddressUtils;

import GUI.GUIMain;
import base.IndexedHashMap;
import burp.BurpExtender;


public class SearchTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private IndexedHashMap<String,SearchResultEntry> lineEntries =new IndexedHashMap<String,SearchResultEntry>();

	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;
	public static final List<String> HeadList = SearchTableHead.getTableHeadList();


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
		return HeadList.size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == HeadList.indexOf(SearchTableHead.Index)) {
			return Integer.class;//id
		}
		if (columnIndex == HeadList.indexOf(SearchTableHead.Favicon)) {
			return ImageIcon.class;
		}
		if (columnIndex == HeadList.indexOf(SearchTableHead.Port)) {
			return Integer.class;
		}
		if (columnIndex == HeadList.indexOf(SearchTableHead.ASN)) {
			return Integer.class;
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
		if (columnIndex >= 0 && columnIndex <= HeadList.size()) {
			return HeadList.get(columnIndex);
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}


	public int getColumnIndexByName(String Name) {
		return HeadList.indexOf(Name);
	}


	public List<SearchResultEntry> getEntries(int[] rowIndexes)
	{
		List<SearchResultEntry> result = new ArrayList<>();
		for (int rowIndex:rowIndexes) {
			SearchResultEntry entry = lineEntries.get(rowIndex);
			result.add(entry);
		}
		return result;
	}


	public List<String> getMultipleValue(int[] rowIndexes, String columnName)
	{
		List<String> result = new ArrayList<>();
		int columnIndex = getColumnIndexByName(columnName);
		if (columnIndex < 0) {
			result.add("Wrong column name to get value");
		}
		for (int rowIndex:rowIndexes) {
			result.add((String)getValueAt(rowIndex,columnIndex));
		}
		return result;
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex >= lineEntries.size()) {
			return "IndexOutOfBoundsException";
		}
		SearchResultEntry entry = lineEntries.get(rowIndex);
		if (columnIndex == HeadList.indexOf(SearchTableHead.Index)) {
			return rowIndex;
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.URL)){
			return entry.getUri();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Protocol)){
			return entry.getProtocol();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Host)){
			return entry.getHost();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Port)){
			return entry.getPort();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.RootDomain)){
			return entry.getRootDomain();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Title)){
			return entry.getTitle();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Server)){
			return entry.getWebcontainer();
		}

		else if (columnIndex == HeadList.indexOf(SearchTableHead.IP)){
			return String.join(",", new TreeSet<String>(entry.getIPSet()));//用TreeSet进行排序
		}

		else if (columnIndex == HeadList.indexOf(SearchTableHead.Favicon)){
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
		else if (columnIndex == HeadList.indexOf(SearchTableHead.IconHash)){
			return entry.getIcon_hash();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.CertInfo)){
			return String.join(",", new TreeSet<String>(entry.getCertDomainSet()));
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.ASNInfo)){
			return entry.getASNInfo();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.ASN)){
			return entry.getAsnNum();
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Source)) {
			return entry.getSource();
		}
		return "";
	}


	/**
	 * 返回可以用于网络搜索引擎进行搜索的字段
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public InfoTuple<String, String> getSearchTypeAndValue(int rowIndex, int columnIndex)
	{
		if (rowIndex >= lineEntries.size()) {
			return new InfoTuple<>(null, null);
		}
		SearchResultEntry entry = lineEntries.get(rowIndex);
		if (columnIndex == HeadList.indexOf(SearchTableHead.Port)){
			String value =  entry.getPort()+"";
			return new InfoTuple<>(SearchType.OriginalString, value);
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.RootDomain)){
			String value =  entry.getRootDomain();
			return new InfoTuple<>(SearchType.SubDomain, value);
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Title)){
			String value =  entry.getTitle();
			return new InfoTuple<>(SearchType.Title, value);
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Server)){
			String value =  entry.getWebcontainer();
			return new InfoTuple<>(SearchType.Server, value);
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.IP)){
			if (entry.getIPSet().iterator().hasNext()) {
				String value = entry.getIPSet().iterator().next();
				if (IPAddressUtils.isPublicIPv4NoPort(value)) {
					return new InfoTuple<>(SearchType.IP, value);
				}
			}
			return new InfoTuple<>(null, null);
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.Favicon) || columnIndex == HeadList.indexOf(SearchTableHead.IconHash)){
			String value = entry.getIcon_hash();
			return new InfoTuple<>(SearchType.IconHash, value);
		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.CertInfo)){
			if (entry.getCertDomainSet().iterator().hasNext()) {
				String value = entry.getCertDomainSet().iterator().next();
				return new InfoTuple<>(SearchType.SubDomain, value);
			}
			return new InfoTuple<>(null, null);

		}
		else if (columnIndex == HeadList.indexOf(SearchTableHead.ASNInfo) || columnIndex == HeadList.indexOf(SearchTableHead.ASN)){
			//应该获取ASN编号
			int num =  entry.getAsnNum();
			if (num > 0){
				return new InfoTuple<>(SearchType.Asn, num+"");
			}else {
				return new InfoTuple<>(SearchType.Asn, null);
			}
		}
		else {
			String value = entry.getHost();
			if (IPAddressUtils.isValidIPv4NoPort(value)) {
				return new InfoTuple<>(SearchType.IP, value);
			}else {
				return new InfoTuple<>(SearchType.SubDomain, value);
			}
		}
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

	public static void main(String[] args) {
		System.out.println(HeadList);
	}
}