package burp;

import javax.sql.RowSet;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import java.io.PrintWriter;
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
	private HashMap<String,Set<String>> noResponseDomain =new HashMap<String,Set<String>>();
	private BurpExtender burp;
	private boolean EnableSearch = Runtime.getRuntime().totalMemory()/1024/1024/1024 > 16;//if memory >16GB enable Search. else disable.
	private boolean ListenerIsOn = true;
	//private PrintWriter stderr = new PrintWriter(BurpExtender.callbacks.getStderr(), true);

	private static final String[] titles = new String[] {
			"#", "URL", "Status", "Length", "MIME Type", "Server","Title", "IP", "CDN", "Time","isNew","isChecked","Comments","Text"
	};

	public static String[] getTitles() {
		return titles;
	}

	public LineTableModel(final BurpExtender burp){
		this.burp = burp;
		this.addTableModelListener(new TableModelListener() {//表格模型监听
			@Override
			public void tableChanged(TableModelEvent e) {
				if (ListenerIsOn) {//开关，记载数据文件的过程中，要清空之前的数据包，这时关闭这个监听
					int type = e.getType();//获取事件类型(增、删、改等)
					int rowstart = e.getFirstRow();//获取触发事件的行索引
					int rowend = e.getLastRow();
					int column = e.getColumn();//获取触发事件的列索引
					if (type == TableModelEvent.INSERT) {//如果是"插入"事件
						//System.out.println("此事件是由\"插入\"触发,在" + row + "行" + column + "列");
					} else if (type == TableModelEvent.UPDATE) {
						DBHelper dbHelper = new DBHelper(GUI.currentDBFile.toString());
						for (int i = rowstart; i <= rowend; i++) {
							dbHelper.updateTitle(lineEntries.get(i));
						}
					} else if (type == TableModelEvent.DELETE) {
						DBHelper dbHelper = new DBHelper(GUI.currentDBFile.toString());
						for (int i = rowstart; i <= rowend; i++) {
							dbHelper.deleteTitle(lineEntries.get(i));
						}
					} else {
						//System.out.println("此事件是由其他原因触发");
					}
				}
			}
		});
	}

	public List<LineEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(List<LineEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}

	public boolean isListenerIsOn() {
		return ListenerIsOn;
	}

	public void setListenerIsOn(boolean listenerIsOn) {
		ListenerIsOn = listenerIsOn;
	}

	public void clear() {
		this.setListenerIsOn(false);
		int rows = this.getRowCount();
		PrintWriter stderr = new PrintWriter(BurpExtender.callbacks.getStderr(), true);
		stderr.print("rows:"+rows);
		this.setLineEntries(new ArrayList<LineEntry>());
		System.out.println(rows);
		if (rows-1 >=0)	fireTableRowsDeleted(0, rows-1);
		this.setListenerIsOn(true);
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
		return result;
	}

	private Set<String> getIPSet() {
		Set<String> result = new HashSet<String>();
		//lineEntries.addAll(hidenLineEntries);
		for(LineEntry line:lineEntries) {
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
		int all = lineEntries.size();
		int checked = 0;
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

	@Deprecated //getMessageinfo方法有问题
	public List<IHttpRequestResponse> getMessages(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<IHttpRequestResponse> messages = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				IHttpRequestResponse url = lineEntries.get(rows[i]).getMessageinfo();
				messages.add(url);
			}
			return messages;
		}
	}

	public List<String> getLocationUrls(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> urls = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String url = lineEntries.get(rows[i]).getHeaderValueOf(false,"Location");
				if (url !=null){
					urls.add(url);
				}
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
				//				lineEntries.remove(rows[i]);
				//				lineEntries.add(rows[i], checked);
				//				//https://stackoverflow.com/questions/4352885/how-do-i-update-the-element-at-a-certain-position-in-an-arraylist
				lineEntries.set(rows[i], checked);
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
				this.burp.domainResult.getBlackDomainSet().add(Host);
				String url = lineEntries.get(rows[i]).getUrl();
				lineEntries.remove(rows[i]);
				this.burp.stdout.println("### "+url+" added to black list and deleted");
				this.fireTableRowsDeleted(rows[i], rows[i]);
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