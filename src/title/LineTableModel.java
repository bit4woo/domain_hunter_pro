package title;

import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import GUI.GUI;
import burp.BurpExtender;
import burp.Commons;
import burp.DBHelper;
import burp.Getter;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IMessageEditorController;
import burp.IntArraySlice;
import domain.DomainPanel;


public class LineTableModel extends AbstractTableModel implements IMessageEditorController,Serializable {
	//https://stackoverflow.com/questions/11553426/error-in-getrowcount-on-defaulttablemodel
	//when use DefaultTableModel, getRowCount encounter NullPointerException. why?
	/**
	 * LineTableModel中数据如果类型不匹配，或者有其他问题，可能导致图形界面加载异常！
	 */
	private static final long serialVersionUID = 1L;
	private LineEntry currentlyDisplayedItem;

	/*
	 * 为了提高LineEntry的查找速度，改为使用LinkedHashMap,
	 * http://infotechgems.blogspot.com/2011/11/java-collections-performance-time.html
	 * 
	 * LinkedHashMap是继承于HashMap，是基于HashMap和双向链表来实现的。
	 * HashMap无序；LinkedHashMap有序，可分为插入顺序和访问顺序两种。默认是插入顺序。
	 * 如果是访问顺序，那put和get操作已存在的Entry时，都会把Entry移动到双向链表的表尾(其实是先删除再插入)。
	 * LinkedHashMap是线程不安全的。
	 */
	private IndexedLinkedHashMap<String,LineEntry> lineEntries =new IndexedLinkedHashMap<String,LineEntry>();
	private IndexedLinkedHashMap<String,Set<String>> noResponseDomain =new IndexedLinkedHashMap<String,Set<String>>();
	//private boolean EnableSearch = Runtime.getRuntime().totalMemory()/1024/1024/1024 > 16;//if memory >16GB enable Search. else disable.
	private boolean ListenerIsOn = true;
	//private PrintWriter stderr = new PrintWriter(BurpExtender.callbacks.getStderr(), true);

	PrintWriter stdout;
	PrintWriter stderr;

	private static final String[] standardTitles = new String[] {
			"#", "URL", "Status", "Length", "Title","Comments","Server","isChecked","AssetType","CheckDoneTime","IP", "CDN|CertInfo", "IconHash"};
	private static List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));
	//为了实现动态表结构
	public static List<String> getTitletList() {
		return titletList;
	}


	public LineTableModel(){

		//titletList.remove("Server");
		//titletList.remove("Time");
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
					//int column = e.getColumn();//获取触发事件的列索引
					//stdout.println(rowstart+"---"+rowend);

					DBHelper dbHelper = new DBHelper(GUI.currentDBFile.toString());
					if (type == TableModelEvent.INSERT) {//插入事件使用批量方法好像不行，都是一个个插入的，每次都会触发
						//从使用场景来看也无需使用批量
						for (int i = rowstart; i <= rowend; i++) {
							dbHelper.addTitle(lineEntries.getValueAtIndex(i));
						}
					} else if (type == TableModelEvent.UPDATE) {
						/*
						for (int i = rowstart; i <= rowend; i++) {
							String key = lineEntries.getKeyAtIndex(i);
							LineEntry entry = lineEntries.get(key);
							entry.setTime(Commons.getNowTimeString());
							dbHelper.updateTitle(entry);
						}
						 */

						List<LineEntry> entries = new ArrayList<LineEntry>();
						for (int i = rowstart; i <= rowend; i++) {
							LineEntry entry = lineEntries.getValueAtIndex(i);
							//entry.setTime(Commons.getNowTimeString());
							//这里不再更新时间，时间只表示CheckDone的时间
							entries.add(entry);
						}
						dbHelper.updateTitles(entries);

					} else if (type == TableModelEvent.DELETE) {//可以批量操作
						/*
						for (int i = rowstart; i <= rowend; i++) {
							String key = lineEntries.getKeyAtIndex(i);
							dbHelper.deleteTitle(lineEntries.get(key));
							lineEntries.remove(key);
						}
						 */


						//必须从高位index进行删除，否则删除的对象会和预期不一致！！！
						List<String> urls = new ArrayList<String>();
						for (int i = rowend; i >= rowstart; i--) {
							String url = lineEntries.getValueAtIndex(i).getUrl();
							urls.add(url);
							lineEntries.removeByIndex(i);//删除tableModel中的元素。
							stdout.println("### "+url+" deleted");
						}
						dbHelper.deleteTitlesByUrl(urls);//删除数据库中的元素



						/*
						List<String> urls = new ArrayList<String>();
						for (int i = rowend; i >= rowstart; i--) {
							String key = lineEntries.getKeyAtIndex(i);
							urls.add(key);
						}
						dbHelper.deleteTitlesByUrl(urls);//删除数据库中的元素
						for(String url:urls) {
							lineEntries.remove(url);//删除tableModel中的元素。
							stdout.println("### "+url+" deleted");
						}
						 */


					} else {
						//System.out.println("此事件是由其他原因触发");
					}

				}
			}
		});
	}

	public IndexedLinkedHashMap<String, LineEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(IndexedLinkedHashMap<String, LineEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}

	public boolean isListenerIsOn() {
		return ListenerIsOn;
	}

	public void setListenerIsOn(boolean listenerIsOn) {
		this.ListenerIsOn = listenerIsOn;
	}


	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return titletList.size();//the one is the request String + response String,for search
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{

		if (columnIndex == titletList.indexOf("#")) {
			return Integer.class;//id
		}
		if (columnIndex == titletList.indexOf("Status")) {
			return Integer.class;//id
		}
		if (columnIndex == titletList.indexOf("Length")) {
			return Integer.class;//id
		}
		if (columnIndex == titletList.indexOf("isNew")) {
			return boolean.class;//id
		}
		if (columnIndex == titletList.indexOf("isChecked")) {
			return String.class;//id
		}
		return String.class;



		//		
		//		switch(columnIndex)
		//		{	
		//			case 0:
		//				return Integer.class;//id
		//			case 2:
		//				return Integer.class;//Status
		//			case 3:
		//				return Integer.class;//Length
		//			case 11:
		//				return boolean.class;//isNew
		//			case 12:
		//				return boolean.class;//isChecked
		//			default:
		//				return String.class;
		//		}//0-id, 1-url,2-status, 3-length,4-mimetype,5-server, 6-title, 7-ip, 8-cdn, 9-comments, 10-time, 11-isnew, 12-ischecked

	}

	@Override
	public int getRowCount()
	{
		return lineEntries.size();
	}

	//define header of table???
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= titletList.size()) {
			return titletList.get(columnIndex);
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (titletList.get(columnIndex).equals("Comments")) {//可以编辑comment
			return true;
		}else {
			return false;
		}
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		LineEntry entry = lineEntries.getValueAtIndex(rowIndex);
		//entry.parse();---
		//"#", "URL", "Status", "Length", "Server","Title", "IP", "CDN", "Comments","Time","isChecked"};
		if (columnIndex == titletList.indexOf("#")) {
			return rowIndex;
		}
		if (columnIndex == titletList.indexOf("URL")){
			return entry.getUrl();
		}
		if (columnIndex == titletList.indexOf("Status")){
			return entry.getStatuscode();
		}
		if (columnIndex == titletList.indexOf("Length")){
			return entry.getContentLength();
		}
		if (columnIndex == titletList.indexOf("Server")){
			return entry.getWebcontainer();
		}
		if (columnIndex == titletList.indexOf("Title")){
			return entry.getTitle();
		}
		if (columnIndex == titletList.indexOf("IP")){
			return entry.getIP();
		}
		if (columnIndex == titletList.indexOf("CDN|CertInfo")){
			return entry.getCDN();
		}
		if (columnIndex == titletList.indexOf("Comments")){
			return entry.getComment();
		}
		if (columnIndex == titletList.indexOf("CheckDoneTime")){
			return entry.getTime();
		}
		if (columnIndex == titletList.indexOf("isChecked")){
			return entry.getCheckStatus();
		}
		if (columnIndex == titletList.indexOf("AssetType")){
			return entry.getAssetType();
		}
		if (columnIndex == titletList.indexOf("IconHash")){
			return entry.getIcon_hash();
		}
		return "";
	}


	@Override
	public void setValueAt(Object value, int row, int col) {
		LineEntry entry = lineEntries.getValueAtIndex(row);
		if (col == titletList.indexOf("Comments")){
			String valueStr = ((String) value).trim();
			//if (valueStr.equals("")) return;
			entry.setComment(valueStr);
			fireTableCellUpdated(row, col);
		}
	}
	//////////////////////extend AbstractTableModel////////////////////////////////



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
		this.setLineEntries(new IndexedLinkedHashMap<String,LineEntry>());//如果ListenerIsOn，将会触发listener
		System.out.println("clean lines of old data,"+rows+" lines cleaned");
		if (rows-1 >=0)	fireTableRowsDeleted(0, rows-1);
		this.setListenerIsOn(true);
	}
	
	/**
	 * 
	 * @return 获取已成功获取title的Entry的IP地址集合
	 */
	private Set<String> getIPSetFromTitle() {
		Set<String> result = new HashSet<String>();
		//lineEntries.addAll(hidenLineEntries);
		for(LineEntry line:lineEntries.values()) {
			String IPString = line.getIP();
			if (IPString == null || IPString.length() <7) continue;//处理保存的请求，没有IP的情况
			String[] linetext = line.getIP().split(",");
			for (String ip:linetext){
				ip = Commons.ipClean(ip);
				if (Commons.isValidIP(ip)){
					result.add(ip);
				}else {
					System.out.println(ip + "invalid IP address, skip to handle it!");
				}
			}
		}

		for(Set<String> IPSet:noResponseDomain.values()) {
			for (String ip:IPSet){
				result.add(ip.trim());
			}
		}
		return result;
	}
	
	/**
	 * 获取根据确定目标汇算出来的网段，减去已确定目标本身后，剩余的IP地址。
	 * @return 扩展IP集合
	 */
	public Set<String> GetExtendIPSet() {
		Set<String> IPsOfDomain = getIPSetFromTitle();//title记录中的IP
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> IPsOfcertainSubnets = Commons.toIPSet(DomainPanel.getDomainResult().getSubnetSet());//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		
		Set<String> subnets = Commons.toSmallerSubNets(IPsOfDomain);//当前所有title结果+确定IP/网段计算出的IP网段
		
		Set<String> CSubNetIPs = Commons.toIPSet(subnets);// 当前所有title结果计算出的IP集合

		CSubNetIPs.removeAll(IPsOfDomain);//删除域名对应的IP
		CSubNetIPs.removeAll(IPsOfcertainSubnets);
		//Set<String> blackIPSet = DomainPanel.getDomainResult().getNotTargetIPSet();
		//CSubNetIPs.removeAll(blackIPSet);//删除黑名单中的IP

		return CSubNetIPs;
	}

	/**
	 * 1、title记录中成功解析的IP地址集合
	 * 2、用户指定的确信度很高的IP和网段的集合。
	 * 将2者合并算成网段。
	 * @return 根据确切目标算出的网段
	 */
	public Set<String> GetSubnets() {
		Set<String> IPsOfDomain = getIPSetFromTitle();//title记录中的IP
		Set<String> IPsOfcertainSubnets = Commons.toIPSet(DomainPanel.getDomainResult().getSubnetSet());//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> subnets = Commons.toSmallerSubNets(IPsOfDomain);
		return subnets;
	}

	public String getStatusSummary() {
		int all = lineEntries.size();
		int checked = 0;
		for (LineEntry lineEntrie:lineEntries.values()) {
			if (lineEntrie.getCheckStatus().equals(LineEntry.CheckStatus_Checked)) {
				checked ++;
			}
		}
		return String.format(" [ALL:%s Unchecked:%s]",all,all-checked);
	}


	///////////////////多个行内容的增删查改/////////////////////////////////

	public List<String> getHosts(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> hosts = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String host = lineEntries.getValueAtIndex(rows[i]).getHost();
				hosts.add(host);
			}
			return hosts;
		}
	}

	public List<String> getHostsAndPorts(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> hosts = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry line = lineEntries.getValueAtIndex(rows[i]);
				String hostAndPort = line.getHost()+":"+line.getPort();
				hosts.add(hostAndPort);
			}
			return hosts;
		}
	}

	public Set<String> getIPs(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			Set<String> Result = new HashSet<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				Set<String> IPs = lineEntries.getValueAtIndex(rows[i]).fetchIPSet();
				Result.addAll(IPs);
			}
			return Result;
		}
	}

	public List<String> getURLs(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> urls = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String url = lineEntries.getValueAtIndex(rows[i]).getUrl();
				urls.add(url);
			}
			return urls;
		}
	}
	
	public List<String> getCommonURLs(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> urls = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				String url = lineEntries.getValueAtIndex(rows[i]).fetchUrlWithCommonFormate();
				urls.add(url);
			}
			return urls;
		}
	}

	public List<String> getLocationUrls(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> urls = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry entry = lineEntries.getValueAtIndex(rows[i]);
				String url = entry.getUrl();
				String Locationurl = entry.getHeaderValueOf(false,"Location");
				if (url !=null){
					urls.add(url+" "+Locationurl);
				}
			}
			return urls;
		}
	}
	
	public List<String> getCDNAndCertInfos(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> results = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry entry = lineEntries.getValueAtIndex(rows[i]);
				String CDNAndCertInfo = entry.getCDN();
				results.add(CDNAndCertInfo);
			}
			return results;
		}
	}
	
	public List<String> getIconHashes(int[] rows) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			List<String> results = new ArrayList<>();

			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry entry = lineEntries.getValueAtIndex(rows[i]);
				String hash = entry.getIcon_hash();
				results.add(hash);
			}
			return results;
		}
	}

	public int[] getIndexes(List<LineEntry> entries) {
		int[] indexes = new int[entries.size()];
		int i=0;

		for (LineEntry entry:entries) {//降序删除才能正确删除每个元素
			int index = lineEntries.IndexOfKey(entry.getUrl());
			indexes[i] = index;
			i++;
		}
		return indexes;
	}

	/*
	//如果使用了tableModelListener,就需要注意：在监听事件中去执行具体动作，这里只是起通知作用！！！！
	尤其是改变了lineEntries数量的操作！index将发生改变。
	 */
	public void removeRows(int[] rows) {
		fireDeleted(rows);

		/*
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
		 */
	}


	public void updateRowsStatus(int[] rows,String status) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry checked = lineEntries.getValueAtIndex(rows[i]);
				checked.setCheckStatus(status);
				if (status.equalsIgnoreCase(LineEntry.CheckStatus_Checked)) {
					checked.setTime(Commons.getNowTimeString());
				}
				//				lineEntries.remove(rows[i]);
				//				lineEntries.add(rows[i], checked);
				//				//https://stackoverflow.com/questions/4352885/how-do-i-update-the-element-at-a-certain-position-in-an-arraylist
				//lineEntries.set(rows[i], checked);
				stdout.println("$$$ "+checked.getUrl()+" updated");
				//this.fireTableRowsUpdated(rows[i], rows[i]);
			}
			fireUpdated(rows);
			//this.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
			//最好还是一行一行地触发监听事件，因为自定义排序后的行号可能不是连续的，如果用批量触发，会做很多无用功，导致操作变慢。
		}
	}


	public void updateAssetTypeOfRows(int[] rows,String assetType) {
		synchronized (lineEntries) {
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {
				LineEntry checked = lineEntries.getValueAtIndex(rows[i]);
				if (assetType == checked.getAssetType()) continue;
				checked.setAssetType(assetType);
				stdout.println(String.format("$$$ %s updated [AssetType-->%s]",checked.getUrl(),assetType));
				//this.fireTableRowsUpdated(rows[i], rows[i]);
			}
			fireUpdated(rows);
		}
	}


	public void updateComments(int[] rows, String commentAdd) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry checked = lineEntries.getValueAtIndex(rows[i]);
				checked.addComment(commentAdd);
				//				lineEntries.remove(rows[i]);
				//				lineEntries.add(rows[i], checked);
				//				//https://stackoverflow.com/questions/4352885/how-do-i-update-the-element-at-a-certain-position-in-an-arraylist
				stdout.println("$$$ "+checked.getUrl()+" updated");
				//this.fireTableRowsUpdated(rows[i], rows[i]);
			}
			//this.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
			fireUpdated(rows);
		}
	}

	//如果记录的Host是IP，且认为不是目标资产，那么将其加入NotTarget集合
	public void addHostToNotTargetIPSet(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry entry = lineEntries.getValueAtIndex(rows[i]);
				String Host = entry.getHost();
				if (Commons.isValidIP(Host)) {
					DomainPanel.getDomainResult().getNotTargetIPSet().add(Host);
					entry.addComment(LineEntry.NotTargetBaseOnBlackList);
					stdout.println("### "+Host+" added to NotTargetIPSet");
				}
			}
		}
	}

	public void removeHostFromNotTargetIPSet(int[] rows) {
		synchronized (lineEntries) {
			//because thread let the delete action not in order, so we must loop in here.
			//list length and index changed after every remove.the origin index not point to right item any more.
			Arrays.sort(rows); //升序
			for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
				LineEntry entry = lineEntries.getValueAtIndex(rows[i]);
				String Host = entry.getHost();
				if (Commons.isValidIP(Host)) {
					DomainPanel.getDomainResult().getNotTargetIPSet().remove(Host);
					entry.removeComment(LineEntry.NotTargetBaseOnBlackList);
					stdout.println("### "+Host+" removed from NotTargetIPSet");
				}
			}
		}
	}

	//当记录是非目标资产时，将其IP集合添加到NotTargetIP集合中
	@Deprecated
	public void addIPSetOfEntryToNotTargetIPSet(int[] rows) {
		for (int i=rows.length-1;i>=0 ;i-- ) {
			Set<String> IPs = lineEntries.getValueAtIndex(rows[i]).fetchIPSet();
			DomainPanel.getDomainResult().getNotTargetIPSet().addAll(IPs);
			stdout.println("### "+IPs.toString()+" added to black list");
		}
	}

	//将IP集合算成C段，并加入NotTarget集合
	@Deprecated
	public void addSubnetBlackList(int[] rows) {
		for (int i=rows.length-1;i>=0 ;i-- ) {
			Set<String> IPs = lineEntries.getValueAtIndex(rows[i]).fetchIPSet();
			Set<String> subnets = Commons.toClassCSubNets(IPs);
			DomainPanel.getDomainResult().getNotTargetIPSet().addAll(subnets);
			stdout.println("### "+subnets.toString()+" added to black list");
		}
	}

	//为了同时fire多个不连续的行，自行实现这个方法。
	private void fireDeleted(int[] rows) {
		List<int[]> slice = IntArraySlice.slice(rows);
		//必须逆序，从高位index开始删除，否则删除的对象和预期不一致！！！
		//上面得到的顺序就是从高位开始的
		for(int[] sli:slice) {
			System.out.println(Arrays.toString(sli));
			this.fireTableRowsDeleted(sli[sli.length-1],sli[0]);//这里传入的值必须是低位数在前面，高位数在后面
		}
	}

	private void fireUpdated(int[] rows) {
		List<int[]> slice = IntArraySlice.slice(rows);
		for(int[] sli:slice) {
			System.out.println(Arrays.toString(sli));
			this.fireTableRowsUpdated(sli[sli.length-1],sli[0]);//同上，修复更新多个记录时的错误
		}
	}


	///////////////////多个行内容的增删查改/////////////////////////////////
	
	/**
	 * 仅用于runner中，某个特殊场景:URL相同host不同的情况
	 * @param lineEntry
	 */
	public void addNewLineEntryWithTime(LineEntry lineEntry){
		if (lineEntry == null) {
			return;
		}
		synchronized (lineEntries) {
			String key = lineEntry.getUrl()+System.currentTimeMillis();
			lineEntries.put(key,lineEntry);
			int index = lineEntries.IndexOfKey(key);
			fireTableRowsInserted(index, index);
		}
	}

	public void addNewLineEntry(LineEntry lineEntry){
		if (lineEntry == null) {
			return;
		}
		synchronized (lineEntries) {
			//			while(lineEntries.size() >= LineConfig.getMaximumEntries()){
			//				ListenerIsOn = false;
			//				final LineEntry removed = lineEntries.remove(0);
			//				ListenerIsOn = true;
			//			}
			int oldsize = lineEntries.size();
			String key = lineEntry.getUrl();
			lineEntries.put(key,lineEntry);
			int newsize = lineEntries.size();
			int index = lineEntries.IndexOfKey(key);
			if (oldsize == newsize) {//覆盖
				fireTableRowsUpdated(index, index);
			}else {//新增
				fireTableRowsInserted(index, index);
			}

			//need to use row-1 when add setRowSorter to table. why??
			//https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
			//fireTableRowsInserted(newsize-1, newsize-1);
		}
	}
	/*
	这个方法更新了URL的比对方法，无论是否包含默认端口都可以成功匹配
	 */
	public LineEntry findLineEntry(String url) {//这里的URL需要包含默认端口!!!
		if (lineEntries == null) return null;
		//之前的方法：统一使用URL的格式进行比较，最需要自己主动用for循环去遍历元素，然后对比。但这种方法不能发挥hashmap的查找速度优势。
		//更好的方法：用hashMap的get方法去查找，看是否能找到对象，get方法是根据key的hash值进行查找的速度比自行循环对比快很多。

		//统一URL字符串的格式
		url = Commons.formateURLString(url);
		return lineEntries.get(url);
	}
	
	/**
	 * 根据一个IHttpRequestResponse对象来查找对应的LineEntry记录
	 * 首先根据完整URL进行查找，如果没有找到，就使用baseURL进行查找。
	 * @param message
	 * @return
	 */
	public LineEntry findLineEntryByMessage(IHttpRequestResponse message) {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		Getter getter = new Getter(helpers);
		URL fullurl = getter.getFullURL(message);
		LineEntry entry = TitlePanel.getTitleTableModel().findLineEntry(fullurl.toString());
		if (entry == null) {
			URL shortUrl = getter.getShortURL(message);
			if(!fullurl.equals(shortUrl)) {
				entry = TitlePanel.getTitleTableModel().findLineEntry(shortUrl.toString());
			}
		}
		return entry;
	}

	/*
	 * find all lineEntries base host，当需要对整个主机的所有服务进行操作时用这个方法
	 * 正确的范围是一个service，即Host+port，弃用这个函数
	 */
	@Deprecated
	public List<LineEntry> findLineEntriesByHost(String host) {//
		if (lineEntries == null) return null;
		List<LineEntry> result = new ArrayList<LineEntry>();
		for (String urlkey:lineEntries.keySet()) {
			try{//根据host查找
				URL URL = new URL(urlkey);
				if (URL.getHost().equalsIgnoreCase(host)) {
					result.add(lineEntries.get(urlkey));
				}
			}catch (Exception e){
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return result;
	}

	/*
	 * find all lineEntries base host and port，通常根据IP+端口来确定一个服务。
	 */
	public List<LineEntry> findLineEntriesByHostAndPort(String host,int port) {//

		List<LineEntry> result = new ArrayList<LineEntry>();
		if (lineEntries == null) return result;
		for (LineEntry value:lineEntries.values()) {
			try{//根据host查找
				if (value.getHost().equalsIgnoreCase(host) && value.getPort() == port) {
					result.add(lineEntries.get(value.getUrl()));
				}
			}catch (Exception e){
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return result;
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

	public void addNewNoResponseDomain(String domain,Set<String> IPSet){
		synchronized (noResponseDomain) {
			noResponseDomain.put(domain,IPSet);
		}
	}

	public void addNewNoResponseDomain(String domain,String IPSet){
		synchronized (noResponseDomain) {
			noResponseDomain.put(domain,new HashSet<String>(Arrays.asList(IPSet.split(","))));
		}
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