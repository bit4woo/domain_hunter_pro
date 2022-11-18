package title;

import GUI.GUIMain;
import burp.*;
import dao.TitleDao;

import javax.swing.table.AbstractTableModel;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.*;


public class LineTableModel extends AbstractTableModel implements IMessageEditorController,Serializable {
	//https://stackoverflow.com/questions/11553426/error-in-getrowcount-on-defaulttablemodel
	//when use DefaultTableModel, getRowCount encounter NullPointerException. why?
	/**
	 * LineTableModel中数据如果类型不匹配，或者有其他问题，可能导致图形界面加载异常！
	 */
	private static final long serialVersionUID = 1L;
	private final TitleDao titleDao;
	private LineEntry currentlyDisplayedItem;
	private IndexedHashMap<String,LineEntry> lineEntries =new IndexedHashMap<String,LineEntry>();

	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;

	private static final String[] standardTitles = new String[] {
			"#", "URL", "Status", "Length", "Title","Comments","Server","isChecked",
			"AssetType","Source","CheckDoneTime","IP", "CNAME|CertInfo","ASNInfo","IconHash"};
	private static List<String> titleList = new ArrayList<>(Arrays.asList(standardTitles));
	//为了实现动态表结构
	public static List<String> getTitleList() {
		//titletList.remove("Server");
		//titletList.remove("Time");
		return titleList;
	}


	public LineTableModel(GUIMain guiMain){
		this.guiMain = guiMain;
		titleDao = new TitleDao(guiMain.getCurrentDBFile());
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
		//TableModelListener的主要作用是用来通知view即GUI数据发生了改变，不应该用于进行数据库的操作。
	}

	public LineTableModel(GUIMain guiMain,IndexedHashMap<String,LineEntry> lineEntries){
		this(guiMain);
		this.lineEntries = lineEntries;
	}

	public LineTableModel(GUIMain guiMain,List<LineEntry> entries){
		this(guiMain);
		for (LineEntry entry:entries) {
			lineEntries.put(entry.getUrl(), entry);
		}
	}
	////////getter setter//////////

	public IndexedHashMap<String, LineEntry> getLineEntries() {
		return lineEntries;
	}

	public void setLineEntries(IndexedHashMap<String, LineEntry> lineEntries) {
		this.lineEntries = lineEntries;
	}
	
	public LineEntry getCurrentlyDisplayedItem() {
		return this.currentlyDisplayedItem;
	}

	public void setCurrentlyDisplayedItem(LineEntry currentlyDisplayedItem) {
		this.currentlyDisplayedItem = currentlyDisplayedItem;
	}
	//////// ^^^^getter setter^^^^//////////

	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return titleList.size();//the one is the request String + response String,for search
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == titleList.indexOf("#")) {
			return Integer.class;//id
		}
		if (columnIndex == titleList.indexOf("Status")) {
			return Integer.class;//id
		}
		if (columnIndex == titleList.indexOf("Length")) {
			return Integer.class;//id
		}
		if (columnIndex == titleList.indexOf("isChecked")) {
			return String.class;//id
		}
		return String.class;
	}

	@Override
	public int getRowCount()
	{
		return lineEntries.size();
	}

	//define header of table???
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= titleList.size()) {
			return titleList.get(columnIndex);
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (titleList.get(columnIndex).equals("Comments")) {//可以编辑comment
			return true;
		}else {
			return false;
		}
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex >= lineEntries.size()) {
			return "IndexOutOfBoundsException";
		}
		LineEntry entry = lineEntries.get(rowIndex);
		//entry.parse();---
		//"#", "URL", "Status", "Length", "Server","Title", "IP", "CDN", "Comments","Time","isChecked"};
		if (columnIndex == titleList.indexOf("#")) {
			return rowIndex;
		}
		if (columnIndex == titleList.indexOf("URL")){
			return entry.getUrl();
		}
		if (columnIndex == titleList.indexOf("Status")){
			return entry.getStatuscode();
		}
		if (columnIndex == titleList.indexOf("Length")){
			return entry.getContentLength();
		}
		if (columnIndex == titleList.indexOf("Server")){
			return entry.getWebcontainer();
		}
		if (columnIndex == titleList.indexOf("Title")){
			return entry.getTitle();
		}
		if (columnIndex == titleList.indexOf("IP")){
			return String.join(",", entry.getIPSet());
		}
		if (columnIndex == titleList.indexOf("CNAME|CertInfo")){
			return entry.fetchCNAMEAndCertInfo();
		}
		if (columnIndex == titleList.indexOf("Comments")){
			return String.join(",", entry.getComments());
		}
		if (columnIndex == titleList.indexOf("CheckDoneTime")){
			return entry.getTime();
		}
		if (columnIndex == titleList.indexOf("isChecked")){
			return entry.getCheckStatus();
		}
		if (columnIndex == titleList.indexOf("AssetType")){
			return entry.getAssetType();
		}
		if (columnIndex == titleList.indexOf("IconHash")){
			return entry.getIcon_hash();
		}
		if (columnIndex == titleList.indexOf("ASNInfo")){
			return entry.getASNInfo();
		}
		if (columnIndex == titleList.indexOf("Source")) {
			return entry.getEntrySource();
		}
		return "";
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		LineEntry entry = lineEntries.get(row);
		if (col == titleList.indexOf("Comments")){
			String valueStr = ((String) value).trim();
			entry.setComments(new HashSet<>(Arrays.asList(valueStr.split(","))));
			fireTableCellUpdated(row, col);
		}
	}
	//////////////////////^^^extend AbstractTableModel^^^////////////////////////////////

	///////////////////// implement IMessageEditorController ////////////////////////////////
	// this allows our request/response viewers to obtain details about the messages being displayed

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
	///////////////////// ^^^^implement IMessageEditorController^^^^ ////////////////////////////////

	
	/**
	 *
	 * @return 获取已成功获取title的Entry的IP地址集合
	 */
	Set<String> getIPSetFromTitle() {
		Set<String> result = new HashSet<String>();

		for(LineEntry line:lineEntries.values()) {
			result.addAll(line.getIPSet());
		}

		return result;
	}

	/**
	 * 获取title记录中的所有公网IP
	 * @return
	 */
	Set<String> getPublicIPSetFromTitle() {
		HashSet<String> result = new HashSet<>();
		for (String ip:getIPSetFromTitle()){
			if (IPAddressUtils.isValidIP(ip)&& !IPAddressUtils.isPrivateIPv4(ip)){
				result.add(ip);
			}
		}
		return result;
	}

	/**
	 * 获取title记录中的所有公网IP计算出的公网网段
	 * @return
	 */
	public Set<String> getPublicSubnets() {
		Set<String> IPsOfDomain = getPublicIPSetFromTitle();
		Set<String> subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);
		return subnets;
	}

	/**
	 * 获取根据确定目标汇算出来的网段，减去已确定目标本身后，剩余的IP地址。
	 * @return 扩展IP集合
	 */
	public Set<String> GetExtendIPSet() {

		Set<String> IPsOfDomain = getIPSetFromTitle();//title记录中的IP
		Set<String> IPsOfcertainSubnets = guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		IPsOfDomain.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		//计算网段前，将CDN和云服务的IP排除在外，这就是这个集合的主要作用！

		Set<String> subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);//当前所有title结果+确定IP/网段计算出的IP网段

		Set<String> CSubNetIPs = IPAddressUtils.toIPSet(subnets);// 当前所有title结果计算出的IP集合

		CSubNetIPs.removeAll(IPsOfDomain);//删除域名对应的IP，之前已经请求过了
		CSubNetIPs.removeAll(IPsOfcertainSubnets);//删除网段对应的IP，之前已经请求过了

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
		Set<String> IPsOfcertainSubnets = guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);
		return subnets;
	}

	/**
	 * 获取title panel中所有entry中的Host+port字段，除了手动保存的请求包记录。
	 * 用target中的IP网段信息+子域名集合+特殊端口目标集合-这个host集合 = 需要去获取title的新域名集合
	 *
	 * 如果是默认端口 80 443就不显示端口
	 * 如果是特殊端口，就加上端口。
	 * @return
	 */
	public Set<String> GetHostsWithSpecialPort() {
		HashSet<String> result = new HashSet<>();
		for (LineEntry entry:lineEntries.values()){
			try{
				if (entry.getEntrySource().equalsIgnoreCase(LineEntry.Source_Manual_Saved)){
					continue;
				}
				if (entry.getPort() !=80 && entry.getPort() !=443){
					result.add(entry.getHost()+":"+entry.getPort());
				}else {
					result.add(entry.getHost());
				}

			}catch (Exception e){
				e.printStackTrace(stderr);
			}
		}
		return result;
	}

	/**
	 * 用于host碰撞
	 * @return
	 */
	public HashSet<String> getIPURLs() {
		HashSet<String> urls = new HashSet<>();
		for (LineEntry line:lineEntries.values()) {
			for (String ip:line.getIPSet()) {
				String url = line.getProtocol()+"://"+ip+":"+line.getPort();
				urls.add(url);
			}
		}
		return urls;
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
		Arrays.sort(rows); //升序
		List<String> hosts = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String host = lineEntries.get(rows[i]).getHost();
			hosts.add(host);
		}
		return hosts;
	}

	public List<String> getHostsAndPorts(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> hosts = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry line = lineEntries.get(rows[i]);
			String hostAndPort = line.getHost()+":"+line.getPort();
			hosts.add(hostAndPort);
		}
		return hosts;
	}
	
	public List<String> getHostsAndIPAddresses(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> hosts = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry line = lineEntries.get(rows[i]);
			String hostAndPort = line.getHost()+"\t"+String.join(",", line.getIPSet());
			hosts.add(hostAndPort);
		}
		return hosts;
	}

	public Set<String> getIPs(int[] rows) {
		Arrays.sort(rows); //升序
		Set<String> Result = new HashSet<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			Set<String> IPs = lineEntries.get(rows[i]).getIPSet();
			Result.addAll(IPs);
		}
		return Result;
	}

	public List<String> getURLs(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = lineEntries.get(rows[i]).getUrl();
			urls.add(url);
		}
		return urls;
	}

	public List<String> getCommonURLs(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = lineEntries.get(rows[i]).fetchUrlWithCommonFormate();
			urls.add(url);
		}
		return urls;
	}

	public List<String> getLocationUrls(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		Getter getter = new Getter(helpers);

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = lineEntries.get(rows[i]);
			String url = entry.getUrl();
			byte[] resp = entry.getResponse();
			if (resp == null) continue;
			String Locationurl = getter.getHeaderValueOf(false,entry.getResponse(),"Location");
			if (url !=null){
				urls.add(url+" "+Locationurl);
			}
		}
		return urls;
	}

	public List<String> getCDNAndCertInfos(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> results = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = lineEntries.get(rows[i]);
			String CDNAndCertInfo = entry.fetchCNAMEAndCertInfo();
			results.add(CDNAndCertInfo);
		}
		return results;
	}

	public List<String> getIconHashes(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> results = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = lineEntries.get(rows[i]);
			String hash = entry.getIcon_hash();
			results.add(hash);
		}
		return results;
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

	/////删改操作，需要操作数据库了//TODO/////

	public void removeRows(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = lineEntries.get(rows[i]).getUrl();
			lineEntries.remove(rows[i]);
			titleDao.deleteTitleByUrl(url);//写入数据库
			stdout.println("!!! "+url+" deleted");
		}
		fireDeleted(rows);
	}


	public void updateRowsStatus(int[] rows,String status) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry checked = lineEntries.get(rows[i]);
			checked.setCheckStatus(status);
			if (status.equalsIgnoreCase(LineEntry.CheckStatus_Checked)) {
				checked.setTime(Commons.getNowTimeString());
			}
			titleDao.addOrUpdateTitle(checked);//写入数据库
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


	public void updateAssetTypeOfRows(int[] rows,String assetType) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {
			LineEntry checked = lineEntries.get(rows[i]);
			if (assetType.equalsIgnoreCase(checked.getAssetType())) continue;
			checked.setAssetType(assetType);
			titleDao.addOrUpdateTitle(checked);//写入数据库
			stdout.println(String.format("$$$ %s updated [AssetType-->%s]",checked.getUrl(),assetType));
			//this.fireTableRowsUpdated(rows[i], rows[i]);
		}
		fireUpdated(rows);
	}


	public void updateComments(int[] rows, String commentAdd) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry checked = lineEntries.get(rows[i]);
			checked.addComment(commentAdd);
			titleDao.addOrUpdateTitle(checked);//写入数据库
			//				lineEntries.remove(rows[i]);
			//				lineEntries.add(rows[i], checked);
			//				//https://stackoverflow.com/questions/4352885/how-do-i-update-the-element-at-a-certain-position-in-an-arraylist
			stdout.println("$$$ "+checked.getUrl()+" updated");
			//this.fireTableRowsUpdated(rows[i], rows[i]);
		}
		//this.fireTableRowsUpdated(rows[0], rows[rows.length-1]);
		fireUpdated(rows);
	}

	public void freshASNInfo(int[] rows) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry checked = lineEntries.get(rows[i]);
			checked.freshASNInfo();
			titleDao.addOrUpdateTitle(checked);//写入数据库
			stdout.println("$$$ "+checked.getUrl()+"ASN Info updated");
		}
		fireUpdated(rows);
	}

	/**
	 * 	主要用于记录CDN或者云服务的IP地址，在做网段汇算时排除这些IP。
	 */
	public void addIPToTargetBlackList(int[] rows) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = lineEntries.get(rows[i]);
			guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet().addAll(entry.getIPSet());
			entry.getEntryTags().add(LineEntry.Tag_NotTargetBaseOnBlackList);
			stdout.println("### IP address "+ entry.getIPSet().toString() +" added to black list");
		}
	}

	/**
	 * 获取用于Host碰撞的域名列表
	 *
	 * 1、没有解析记录的域名
	 * 2、解析记录是内网地址的域名
	 *
	 * 3、解析是外网，但是外网无法访问的域名（比如403），但是绑定特定IP即可访问。大概率是走了不同的网关导致的.
	 * 想要准确地获取到这个结果，那么hunter的数据应该是在外网环境中获取的。如果是hunter的数据是内网环境中获取的，就会遗漏一部分数据。
	 * @return
	 */
	public HashSet<String> getDomainsForBypassCheck(){

		HashSet<String> allDomainSet = new HashSet<String>();//所有子域名列表
		allDomainSet.addAll(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());

		HashSet<String> tmp = new HashSet<String>();

		for (String item:allDomainSet) {//移除IP
			if (item.contains(":")) {//有可能domain:port的情况
				item = item.split(":")[0];
			}
			if (DomainNameUtils.isValidDomain(item)) {
				tmp.add(item);
			}
		}

		Collection<LineEntry> entries = getLineEntries().values();
		for (LineEntry entry:entries) {
			String ip = new ArrayList<String>(entry.getIPSet()).get(0);//这里可能不严谨，如果IP解析既有外网地址又有内网地址就会出错
			if (!IPAddressUtils.isPrivateIPv4(ip)) {//移除公网解析记录；剩下无解析记录和内网解析记录
				if (entry.getStatuscode() == 403 && DomainNameUtils.isValidDomain(entry.getHost())) {
					//do Nothing
				}else {
					tmp.remove(entry.getHost());
				}
			}
		}
		return tmp;
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


	///////////////////^^^多个行内容的增删查改^^^/////////////////////////////////

	/**
	 * 仅用于runner中，某个特殊场景:URL相同host不同的情况
	 * @param lineEntry
	 */
	public void addNewLineEntryWithTime(LineEntry lineEntry){
		if (lineEntry == null) {
			return;
		}
		String key = lineEntry.getUrl()+System.currentTimeMillis();
		lineEntries.put(key,lineEntry);
		int index = lineEntries.IndexOfKey(key);
		fireTableRowsInserted(index, index);
		titleDao.addOrUpdateTitle(lineEntry);//写入数据库
	}

	public void addNewLineEntry(LineEntry lineEntry){
		if (lineEntry == null) {
			return;
		}
		String key = lineEntry.getUrl();
		LineEntry ret = lineEntries.put(key,lineEntry);
		//以前的做法是，put之后再次统计size来判断是新增还是替换，这种方法在多线程时可能不准确，
		//concurrentHashMap的put方法会在替换时返回原来的值，可用于判断是替换还是新增
		int index = lineEntries.IndexOfKey(key);
		if (ret == null) {
			fireTableRowsInserted(index, index);
			//这里偶尔出现IndexOutOfBoundsException错误,
			// 但是debug发现javax.swing.DefaultRowSorter.checkAgainstModel在条件为false时(即未越界)抛出了异常，奇怪！
		}else {
			fireTableRowsUpdated(index, index);
		}

		titleDao.addOrUpdateTitle(lineEntry);//写入数据库

		//need to use row-1 when add setRowSorter to table. why??
		//https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
		//fireTableRowsInserted(newsize-1, newsize-1);
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
		LineEntry entry = findLineEntry(fullurl.toString());
		if (entry == null) {
			URL shortUrl = getter.getShortURL(message);
			if(!fullurl.equals(shortUrl)) {
				entry = findLineEntry(shortUrl.toString());
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

	public void freshAllASNInfo(){
		for (LineEntry entry : lineEntries.values()) {
			entry.freshASNInfo();
		}
		fireTableRowsUpdated(0,lineEntries.size()-1);
	}
}