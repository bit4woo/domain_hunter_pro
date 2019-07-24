package burp;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;


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
	private boolean EnableSearch = Runtime.getRuntime().totalMemory()/1024/1024/1024 > 16;//if memory >16GB enable Search. else disable.
	private boolean ListenerIsOn = true;
	//private PrintWriter stderr = new PrintWriter(BurpExtender.callbacks.getStderr(), true);

	PrintWriter stdout;
	PrintWriter stderr;

	private static final String[] titles = new String[] {
			"#", "URL", "Status", "Length", "MIME Type", "Server","Title", "IP", "CDN", "Comments","Time","isNew","isChecked"};
	//       0-id, 1-url,2-status, 3-length,4-mimetype,5-server, 6-title, 7-ip, 8-cdn, 9-comments, 10-time, 11-isnew, 12-ischecked

	public static String[] getTitles() {
		return titles;
	}

	public LineTableModel(){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
		/*
		关于这个listener，主要的目标的是当数据发生改变时，更新到数据库。通过fireTableRowsxxxx来触发。
		但是clear()中对lineEntries的操作也触发了，注意
		The call to fireTableRowsDeleted simply fires off the event to indicate rows have been deleted, you still need to actually remove them from the model.
		 */
		this.addTableModelListener(new TableModelListener() {//表格模型监听
			@Override
			public void tableChanged(TableModelEvent e) {
				if (ListenerIsOn) {//开关，加载数据文件的过程中，这时关闭这个监听
					int type = e.getType();//获取事件类型(增、删、改等)
					int rowstart = e.getFirstRow();//获取触发事件的行索引，即是fireTableRowxxx中的2个参数。
					int rowend = e.getLastRow();
					int column = e.getColumn();//获取触发事件的列索引
					//stdout.println(rowstart+"---"+rowend);

					DBHelper dbHelper = new DBHelper(GUI.currentDBFile.toString());
					if (type == TableModelEvent.INSERT) {//插入事件使用批量方法好像不行，都是一个个插入的，每次都会触发

						for (int i = rowstart; i <= rowend; i++) {
							dbHelper.addTitle(lineEntries.get(i));
						}
					} else if (type == TableModelEvent.UPDATE) {//可以批量数据库操作//这里的逻辑也不对，lineEntries已经改变了，在触发这个事件之前
						//dbHelper.updateTitles()
						for (int i = rowstart; i <= rowend; i++) {
							dbHelper.updateTitle(lineEntries.get(i));
						}
					} else if (type == TableModelEvent.DELETE) {//可以批量操作
						for (int i = rowstart; i <= rowend; i++) {
							dbHelper.deleteTitle(lineEntries.get(i));
							lineEntries.remove(i);
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
		this.ListenerIsOn = listenerIsOn;
	}

	public void clear(boolean syncToFile) {
//		if (syncToFile){
//			this.setListenerIsOn(true);
//		}else {
//			this.setListenerIsOn(false);
//		}
		this.setListenerIsOn(false);//这里之所以要关闭listener，是因为LineEntries为空时，执行listener中的逻辑将出错而退出。而后续获取title的逻辑就会中断。就丢失了title的历史记录。
		int rows = this.getRowCount();
		stderr.print("rows:"+rows);
		//this.setLineEntries(new ArrayList<LineEntry>());//这个方式无法通过listenser去同步数据库，因为LineEntries已经空了。
		//虽然触发了，却无法更新数据库。
		if (syncToFile){
			try {
				if (GUI.currentDBFile.delete()){
					GUI.currentDBFile.createNewFile();//文件存在时，不会创建新文件!必须先删除就文件
				}
				DBHelper dbHelper = new DBHelper(GUI.currentDBFile.toString());
				dbHelper.saveDomainObject(DomainPanel.getDomainResult());//效果等同于删除所有title。速度更快
				//dbHelper.deleteTitles(this.getLineEntries());
			}catch (Exception e){
				e.printStackTrace(stderr);
			}

		}
		this.setLineEntries(new ArrayList<LineEntry>());//如果ListenerIsOn，将会触发listener
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
		Set<String> subnets = Commons.toSmallerSubNets(IPsOfDomain);//当前所有title结果计算出的IP网段
		subnets.addAll(DomainPanel.getDomainResult().getSubnetSet());//确定的IP网段，用户自己输入的
		Set<String> CSubNetIPs = Commons.toIPSet(subnets);// 当前所有title结果计算出的IP集合
		
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
	{
		switch(columnIndex)
		{
			case 0:
				return Integer.class;//id
			case 2:
				return Integer.class;//Status
			case 3:
				return Integer.class;//Length
			case 11:
				return boolean.class;//isNew
			case 12:
				return boolean.class;//isChecked
			default:
				return String.class;
		}//0-id, 1-url,2-status, 3-length,4-mimetype,5-server, 6-title, 7-ip, 8-cdn, 9-comments, 10-time, 11-isnew, 12-ischecked

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
		if (titles[columnIndex].equals("Comments")) {//可以编辑comment
			return true;
		}else {
			return false;
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
				String url = lineEntries.get(rows[i]).getUrl();
				String Locationurl = lineEntries.get(rows[i]).getHeaderValueOf(false,"Location");
				if (url !=null){
					urls.add(url+" "+Locationurl);
				}
			}
			return urls;
		}
	}


	/*
	//如果使用了tableModelListener,就需要注意：在监听事件中去执行具体动作，这里只是起通知作用！！！！
	尤其是改变了lineEntries数量的操作！index将发生改变。
	 */


	public void removeRows(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
//				String url = lineEntries.get(rows[i]).getUrl();
//				lineEntries.remove(rows[i]);//在监听事件中去执行具体动作，这里只是起通知作用！！！！
//				stdout.println("!!! "+url+" deleted");
				this.fireTableRowsDeleted(rows[i], rows[i]);
			}
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
				//lineEntries.set(rows[i], checked);
				stdout.println("$$$ "+checked.getUrl()+" updated");
				this.fireTableRowsUpdated(rows[i], rows[i]);
			}
			//this.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
			//最好还是一行一行地触发监听事件，因为自定义排序后的行号可能不是连续的，如果用批量触发，会做很多无用功，导致操作变慢。
		}
	}

	public void updateComments(int[] rows, String commentAdd) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry checked = lineEntries.get(rows[i]);
				String commentToSet = checked.getComment();
				if (commentToSet == null || commentToSet.trim().equals("")){
					commentToSet = commentAdd;
				}else if(commentToSet.contains(commentAdd)){
					//do nothing
				}else {
					commentToSet = commentToSet+","+commentAdd;
				}
				checked.setComment(commentToSet);
				//				lineEntries.remove(rows[i]);
				//				lineEntries.add(rows[i], checked);
				//				//https://stackoverflow.com/questions/4352885/how-do-i-update-the-element-at-a-certain-position-in-an-arraylist
				stdout.println("$$$ "+checked.getUrl()+" updated");
				this.fireTableRowsUpdated(rows[i], rows[i]);
			}
			//this.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
		}
	}

	public void addBlackList(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String Host = lineEntries.get(rows[i]).getHost();
				DomainPanel.getDomainResult().getBlackDomainSet().add(Host);
				String url = lineEntries.get(rows[i]).getUrl();
				//lineEntries.remove(rows[i]);
				stdout.println("### "+url+" added to black list and deleted");
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
		{//0-id, 1-url,2-status, 3-length,4-mimetype,5-server, 6-title, 7-ip, 8-cdn, 9-comments, 10-time, 11-isnew, 12-ischecked
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
				return entry.getComment();
			case 10:
				return entry.getTime();
			case 11:
				return entry.isNew();
			case 12:
				return entry.isChecked();
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
			case 9://comment
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
//			while(lineEntries.size() >= (new LineConfig()).getMaximumEntries()){
//				final LineEntry removed = lineEntries.remove(0);
//			}
			if (findLineEntry(lineEntry.getUrl()) !=null) return;
			lineEntries.add(lineEntry);
			int row = lineEntries.size();
			//fireTableRowsInserted(row, row);
			//need to use row-1 when add setRowSorter to table. why??
			//https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
			fireTableRowsInserted(row-1, row-1);
		}
	}

	public LineEntry findLineEntry(String url) {
		if (lineEntries == null) return null;
		for (LineEntry line:lineEntries) {
			line.setHelpers(BurpExtender.getCallbacks().getHelpers());
			if (url.equalsIgnoreCase(line.getUrl())) {
				return line;
			}
		}
		return null;
	}
	
	public LineEntry findLineEntryByIP(String IP) {
		if (lineEntries == null) return null;
		for (LineEntry line:lineEntries) {
			line.setHelpers(BurpExtender.getCallbacks().getHelpers());
			List<String> IPlist = Arrays.asList(line.getIP().split(","));
			if (IPlist.contains(IP)) {
				return line;
			}
		}
		return null;
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