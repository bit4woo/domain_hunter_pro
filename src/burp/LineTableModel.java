package burp;

import javax.swing.table.AbstractTableModel;

import java.io.Serializable;
import java.util.*;


public class LineTableModel extends AbstractTableModel implements IMessageEditorController,Serializable {
	//https://stackoverflow.com/questions/11553426/error-in-getrowcount-on-defaulttablemodel
	//when use DefaultTableModel, getRowCount encounter NullPointerException. why?
	/**
	 * LineTableModel中数据如果类型不匹配，或者有其他问题，可能导致图形界面加载异常！
	 */
	private static final long serialVersionUID = 1L;
	private LineEntry currentlyDisplayedItem;
	private List<LineEntry> lineEntries =new ArrayList<LineEntry>();
	private List<LineEntry> hidenLineEntries =new ArrayList<LineEntry>();
	//是否隐藏只在LineTableModel中区分，其他地方获取都是合并了的。
	private HashMap<String,Set<String>> noResponseDomain =new HashMap<String,Set<String>>();
	private BurpExtender burp;
	private boolean EnableSearch = Runtime.getRuntime().totalMemory()/1024/1024/1024 > 16;//if memory >16GB enable Search. else disable.

	private static final String[] titles = new String[] {
			"#", "URL", "Status", "Length", "MIME Type", "Server","Title", "IP", "CDN", "Time","isNew","isChecked","Comments","Text"
	};

	public static String[] getTitles() {
		return titles;
	}

	public LineTableModel(final BurpExtender burp){
		this.burp = burp;

	}

	public List<LineEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(List<LineEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}

	@Deprecated//不存储到DomainObject,就不需要这个方法了
	public List<String> getLineJsons(){
		List<String> result = new ArrayList<String>();
		//lineEntries.addAll(hidenLineEntries);
		synchronized (lineEntries) {
			for(LineEntry line:lineEntries) {
				String linetext = line.ToJson();
				result.add(linetext);
			}
		}
		synchronized (hidenLineEntries) {
			for(LineEntry line:hidenLineEntries) {
				String linetext = line.ToJson();
				result.add(linetext);
			}
		}
		return result;
	}

	//no usage.
	@Deprecated
	private HashMap<String, Set<String>> getDomainIPSet() {
		HashMap<String, Set<String>> result = noResponseDomain;
		//lineEntries.addAll(hidenLineEntries);
		for(LineEntry line:lineEntries) {
			String[] linetext = line.getIP().split(", ");
			String domain = line.getHost();
			HashSet<String> ipset = new HashSet<String>(Arrays.asList(linetext));

			result.put(domain, ipset);
		}

		for(LineEntry line:hidenLineEntries) {
			String[] linetext = line.getIP().split(", ");
			String domain = line.getHost();
			HashSet<String> ipset = new HashSet<String>(Arrays.asList(linetext));

			result.put(domain, ipset);
		}
		return result;
	}

	private Set<String> getIPSet() {
		Set<String> result = new HashSet<String>();
		//lineEntries.addAll(hidenLineEntries);
		for(LineEntry line:lineEntries) {
			String[] linetext = line.getIP().split(", ");
			result.addAll(Arrays.asList(linetext));
		}

		for(LineEntry line:hidenLineEntries) {
			String[] linetext = line.getIP().split(", ");
			result.addAll(Arrays.asList(linetext));
		}

		for(Set<String> IPSet:noResponseDomain.values()) {
			result.addAll(IPSet);
		}

		return result;
	}

	public Set<String> GetExtendIPSet() {
		Set<String> IPsOfDomain = getIPSet();
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> subnets = Commons.toSmallerSubNets(IPsOfDomain);
		Set<String> CSubNetIPs = Commons.toIPSet(subnets);
		CSubNetIPs.removeAll(getIPSet());

		return CSubNetIPs;
	}

	public Set<String> GetSubnets() {
		Set<String> IPsOfDomain = getIPSet();
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> subnets = Commons.toSmallerSubNets(IPsOfDomain);	
		return subnets;
	}

	public String getStatusSummary() {
		int all = lineEntries.size()+hidenLineEntries.size();
		int checked = hidenLineEntries.size();
		for (LineEntry lineEntrie:lineEntries) {
			if (lineEntrie.isChecked()) {
				checked ++;
			}
		}
		return "     ALL: "+all+"     Checked: "+checked+"     unchecked: "+(all-checked);
	}

	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return titles.length;//the one is the request String + response String,for search
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{	switch(columnIndex) 
		{
		case 0: 
			return Integer.class;//id
		case 2: 
			return Integer.class;//Status
		case 3: 
			return Integer.class;//Length
		case 8:
			return boolean.class;//isNew
		case 9:
			return boolean.class;//isChecked
		default:
			return String.class;
		}

	}

	@Override
	public int getRowCount()
	{
		return lineEntries.size();
	}

	//define header of table???
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= titles.length) {
			return titles[columnIndex];
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex < titles.length-2) {//可以编辑comment
			return false;
		}else {
			return true;
		}
	}

	public void removeRows(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String url = lineEntries.get(rows[i]).getUrl();
				lineEntries.remove(rows[i]);
				this.burp.stdout.println("!!! "+url+" deleted");
				this.fireTableRowsDeleted(rows[i], rows[i]);
			}
		}
	}

	public List<String> getURLs(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> urls = new ArrayList<>();
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String url = lineEntries.get(rows[i]).getUrl();
				urls.add(url);
			}
			return urls;
		}
	}

	public void updateRows(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry checked = lineEntries.get(rows[i]);
				checked.setChecked(true);
				lineEntries.remove(rows[i]);
				lineEntries.add(rows[i], checked);
				this.burp.stdout.println("$$$ "+checked.getUrl()+" updated");
			}
			this.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
		}
	}

	public void addBlackList(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String Host = lineEntries.get(rows[i]).getHost();
				this.burp.domainResult.blackDomainSet.add(Host);
				String url = lineEntries.get(rows[i]).getUrl();
				lineEntries.remove(rows[i]);
				this.burp.stdout.println("### "+url+" added to black list and deleted");
				this.fireTableRowsDeleted(rows[i], rows[i]);
			}
		}
	}


	public void hideLines() {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			ArrayList<Integer> tmprows= new ArrayList<Integer>();
			for (LineEntry lineEntrie:lineEntries) {
				if (lineEntrie.isChecked()) {
					int index = lineEntries.indexOf(lineEntrie);
					tmprows.add(index);
				}
			}

			Integer[] rows = (Integer[])tmprows.toArray(new Integer[tmprows.size()]);
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry entry = lineEntries.get(rows[i]);
				//lineEntries.remove((int)rows[i]);//当没有转换为int前，删除是失败的！
				lineEntries.remove(entry);
				hidenLineEntries.add(entry);

				String url = entry.getUrl();
				this.burp.stdout.println("### "+url+" hidded");
				this.fireTableRowsDeleted(rows[i], rows[i]);
			}
		}
	}

	public void unHideLines() {
		synchronized (lineEntries) {
			Iterator<LineEntry> it = hidenLineEntries.iterator();
			while (it.hasNext()){
				LineEntry item = it.next();
				addNewLineEntry(item);
				it.remove();
				String url = item.getUrl();
				this.burp.stdout.println("### show "+url);
			}
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		LineEntry entry = lineEntries.get(rowIndex);
		entry.parse();
		switch (columnIndex)
		{
		case 0:
			return rowIndex;
		case 1:
			return entry.getUrl();
		case 2:
			return entry.getStatuscode();
		case 3:
			return entry.getContentLength();
		case 4:
			return entry.getMIMEtype();
		case 5:
			return entry.getWebcontainer();  
		case 6:
			return entry.getTitle();
		case 7:
			return entry.getIP();
		case 8:
			return entry.getCDN();
		case 9:
			return entry.getTime();
		case 10:
			return entry.isNew();
		case 11:
			return entry.isChecked();
		case 12:
			return entry.getComment();
		case 13:
			//return new String(entry.getResponse());// response text for search
			//it takes too many memories
			/*
			if (EnableSearch) {
				return new String(entry.getResponse());
			}else {
				return "";
			}*/
			//this is no need after override filter.
			return "";
		default:
			return "";
		}
	}


	@Override
	public void setValueAt(Object value, int row, int col) {
		LineEntry entry = lineEntries.get(row);
		switch (col)
		{
		case 12://comment
			entry.setComment((String) value);
			break;
		default:
			break;
		}
		fireTableCellUpdated(row, col);
	}
	//////////////////////extend AbstractTableModel////////////////////////////////

	public void addNewLineEntry(LineEntry lineEntry){
		synchronized (lineEntries) {
			while(lineEntries.size() >= (new LineConfig()).getMaximumEntries()){
				final LineEntry removed = lineEntries.remove(0);
			}

			lineEntries.add(lineEntry);
			int row = lineEntries.size();
			//fireTableRowsInserted(row, row);
			//need to use row-1 when add setRowSorter to table. why??
			//https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
			fireTableRowsInserted(row-1, row-1);
		}
	}

	public void addNewNoResponseDomain(String domain,Set<String> IPSet){
		synchronized (noResponseDomain) {
			noResponseDomain.put(domain,IPSet);
		}
	}


	public LineEntry getCurrentlyDisplayedItem() {
		return this.currentlyDisplayedItem;
	}

	public void setCurrentlyDisplayedItem(LineEntry currentlyDisplayedItem) {
		this.currentlyDisplayedItem = currentlyDisplayedItem;
	}

	//
	// implement IMessageEditorController
	// this allows our request/response viewers to obtain details about the messages being displayed
	//

	@Override
	public byte[] getRequest()
	{
		LineEntry item = getCurrentlyDisplayedItem();
		if(item==null) {
			return "".getBytes();
		}
		return item.getRequest();
	}

	@Override
	public byte[] getResponse()
	{
		LineEntry item = getCurrentlyDisplayedItem();
		if(item==null) {
			return "".getBytes();
		}
		return item.getResponse();
	}

	@Override
	public IHttpService getHttpService()
	{
		LineEntry item = getCurrentlyDisplayedItem();
		if(item==null) {
			return null;
		}
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		IHttpService service = helpers.buildHttpService(item.getHost(), 
				item.getPort(), item.getProtocol());
		return service;
	}


	/*    public class LineTable extends JTable
    {	
	 *//**
	 * 
	 *//*
    	private static final long serialVersionUID = 1L;
        public LineTable(LineTableModel lineTableModel)
        {
            super(lineTableModel);
        }

        @Override
        public void changeSelection(int row, int col, boolean toggle, boolean extend)
        {
            // show the log entry for the selected row
        	LineEntry Entry = lineEntries.get(super.convertRowIndexToModel(row));
            requestViewer.setMessage(Entry.messageinfo.getRequest(), true);
            responseViewer.setMessage(Entry.messageinfo.getResponse(), false);
            currentlyDisplayedItem = Entry.messageinfo;
            super.changeSelection(row, col, toggle, extend);
        }
    }*/
}