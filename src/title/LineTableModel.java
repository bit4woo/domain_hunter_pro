package title;

import burp.*;
import dao.TitleDao;
import domain.DomainPanel;

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
	private LineEntry currentlyDisplayedItem;
	TitleDao titeDao;

	PrintWriter stdout;
	PrintWriter stderr;

	private static final String[] standardTitles = new String[] {
			"#", "URL", "Status", "Length", "Title","Comments","Server","isChecked",
			"AssetType","CheckDoneTime","IP", "CDN|CertInfo","ASNInfo","IconHash"};
	private static List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));
	//为了实现动态表结构
	public static List<String> getTitletList() {
		return titletList;
	}


	public LineTableModel(String dbfile){
		titeDao = new TitleDao(dbfile);
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return titletList.size();
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

	}

	@Override
	public int getRowCount()
	{
		return titeDao.getRowCount();
	}

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
		LineEntry entry = titeDao.selectTitleByID(rowIndex);
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
		if (columnIndex == titletList.indexOf("ASNInfo")){
			return entry.getASNInfo();
		}
		return "";
	}


	@Override
	public void setValueAt(Object value, int row, int col) {
		if (col == titletList.indexOf("Comments")){
			LineEntry entry = titeDao.selectTitleByID(row);
			String valueStr = ((String) value).trim();
			//if (valueStr.equals("")) return;
			entry.setComment(valueStr);
			titeDao.addOrUpdateTitle(entry);
			fireTableCellUpdated(row, col);
		}
	}
	//////////////////////extend AbstractTableModel////////////////////////////////


	/**
	 *
	 * @return 获取已成功获取title的Entry的IP地址集合
	 */
	Set<String> getIPSetFromTitle() {
		Set<String> result = new HashSet<String>();
		//lineEntries.addAll(hidenLineEntries);
		for(LineEntry line:titeDao.selectAllTitle()) {
			String IPString = line.getIP();
			if (IPString == null || IPString.length() <7) continue;//处理保存的请求，没有IP的情况
			HashSet<String> ips = line.fetchIPSet();
			for (String ip:ips){
				ip = IPAddressUtils.ipClean(ip);
				if (IPAddressUtils.isValidIP(ip)){
					result.add(ip);
				}else {
					System.out.println(ip + "invalid IP address, skip to handle it!");
				}
			}
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
		Set<String> IPsOfcertainSubnets = DomainPanel.fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		IPsOfDomain.removeAll(DomainPanel.getDomainResult().getNotTargetIPSet());
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
		Set<String> IPsOfcertainSubnets = DomainPanel.fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
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
		for (LineEntry entry:titeDao.selectAllTitle()){
			try{
				if (entry.getEntryType().equalsIgnoreCase(LineEntry.EntryType_Manual_Saved)){
					continue;
				}
				if (entry.getComment().contains("Manual-Saved")){
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
		for (LineEntry line:titeDao.selectAllTitle()) {
			for (String ip:line.fetchIPSet()) {
				String url = line.getProtocol()+"://"+ip+":"+line.getPort();
				urls.add(url);
			}
		}
		return urls;
	}

	public String getStatusSummary() {
		int all = titeDao.getRowCount();
		int checked = 0;
		for (LineEntry lineEntrie:titeDao.selectAllTitle()) {
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
			String host = titeDao.selectTitleByID(rows[i]).getHost();
			hosts.add(host);
		}
		return hosts;
	}

	public List<String> getHostsAndPorts(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> hosts = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry line = titeDao.selectTitleByID(rows[i]);
			String hostAndPort = line.getHost()+":"+line.getPort();
			hosts.add(hostAndPort);
		}
		return hosts;
	}

	public Set<String> getIPs(int[] rows) {
		Arrays.sort(rows); //升序
		Set<String> Result = new HashSet<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			Set<String> IPs = titeDao.selectTitleByID(rows[i]).fetchIPSet();
			Result.addAll(IPs);
		}
		return Result;
	}

	public List<String> getURLs(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = titeDao.selectTitleByID(rows[i]).getUrl();
			urls.add(url);
		}
		return urls;
	}

	public List<String> getCommonURLs(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = titeDao.selectTitleByID(rows[i]).fetchUrlWithCommonFormate();
			urls.add(url);
		}
		return urls;
	}

	public List<String> getLocationUrls(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = titeDao.selectTitleByID(rows[i]);
			String url = entry.getUrl();
			String Locationurl = entry.getHeaderValueOf(false,"Location");
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
			LineEntry entry = titeDao.selectTitleByID(rows[i]);
			String CDNAndCertInfo = entry.getCDN();
			results.add(CDNAndCertInfo);
		}
		return results;
	}

	public List<String> getIconHashes(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> results = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = titeDao.selectTitleByID(rows[i]);
			String hash = entry.getIcon_hash();
			results.add(hash);
		}
		return results;
	}

	/*
	//如果使用了tableModelListener,就需要注意：在监听事件中去执行具体动作，这里只是起通知作用！！！！
	尤其是改变了lineEntries数量的操作！index将发生改变。
	 */
	public void removeRows(int[] rows) {
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			titeDao.deleteTitleByID(rows[i]);
		}
		fireDeleted(rows);
	}


	public void updateRowsStatus(int[] rows,String status) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry checked = titeDao.selectTitleByID(rows[i]);
			checked.setCheckStatus(status);
			if (status.equalsIgnoreCase(LineEntry.CheckStatus_Checked)) {
				checked.setTime(Commons.getNowTimeString());
			}
			titeDao.addOrUpdateTitle(checked);
		}
		fireUpdated(rows);
	}


	public void updateAssetTypeOfRows(int[] rows,String assetType) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {
			LineEntry checked = titeDao.selectTitleByID(rows[i]);
			if (assetType.equalsIgnoreCase(checked.getAssetType())) continue;
			checked.setAssetType(assetType);
			titeDao.addOrUpdateTitle(checked);
			stdout.println(String.format("$$$ %s updated [AssetType-->%s]",checked.getUrl(),assetType));
			//this.fireTableRowsUpdated(rows[i], rows[i]);
		}
		fireUpdated(rows);
	}


	public void updateComments(int[] rows, String commentAdd) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry checked = titeDao.selectTitleByID(rows[i]);
			checked.addComment(commentAdd);
			//				lineEntries.remove(rows[i]);
			//				lineEntries.add(rows[i], checked);
			//				//https://stackoverflow.com/questions/4352885/how-do-i-update-the-element-at-a-certain-position-in-an-arraylist
			stdout.println("$$$ "+checked.getUrl()+" updated");
			titeDao.addOrUpdateTitle(checked);
		}
		fireUpdated(rows);
	}

	public void freshASNInfo(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry checked = titeDao.selectTitleByID(rows[i]);
			checked.freshASNInfo();
			titeDao.addOrUpdateTitle(checked);
			stdout.println("$$$ "+checked.getUrl()+"ASN Info updated");
		}
		fireUpdated(rows);
	}

	/**
	 * 	主要用于记录CDN或者云服务的IP地址，在做网段汇算时排除这些IP。
	 */
	public void addIPToTargetBlackList(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = titeDao.selectTitleByID(rows[i]);
			String Host = entry.getHost();
			if (DomainNameUtils.isValidDomain(Host)) {
				DomainPanel.getDomainResult().getNotTargetIPSet().addAll(entry.fetchIPSet());
				entry.addComment(LineEntry.NotTargetBaseOnBlackList);
				stdout.println("### IP address of "+ Host +" added to black list");
				titeDao.addOrUpdateTitle(entry);
			}
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
		allDomainSet.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		HashSet<String> tmp = new HashSet<String>();

		for (String item:allDomainSet) {//移除IP
			if (item.contains(":")) {//有可能domain:port的情况
				item = item.split(":")[0];
			}
			if (DomainNameUtils.isValidDomain(item)) {
				tmp.add(item);
			}
		}

		for (LineEntry entry:titeDao.selectAllTitle()) {
			String ip = entry.getIP().split(",")[0];//这里可能不严谨，如果IP解析既有外网地址又有内网地址就会出错
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


	///////////////////多个行内容的增删查改/////////////////////////////////

	/**
	 * 仅用于runner中，某个特殊场景:URL相同host不同的情况
	 * @param lineEntry
	 */
	public void addNewLineEntryWithTime(LineEntry lineEntry){
		if (lineEntry == null) {
			return;
		}
		String key = lineEntry.getUrl()+System.currentTimeMillis();
		lineEntry.setUrl(key);
		titeDao.addOrUpdateTitle(lineEntry);
		fireTableRowsInserted(titeDao.getRowCount(), titeDao.getRowCount());
	}

	/**
	 * 用于Host碰撞场景
	 * @param lineEntry
	 */
	public void addNewLineEntryWithHost(LineEntry lineEntry,String Host){
		if (lineEntry == null) {
			return;
		}
		String key = lineEntry.getUrl()+Host;
		lineEntry.setUrl(key);
		titeDao.addOrUpdateTitle(lineEntry);
		int index = titeDao.selectTitleByUrl(key).getID();
		fireTableRowsInserted(index, index);
	}

	public void addNewLineEntry(LineEntry lineEntry){
		if (lineEntry == null) {
			return;
		}
		titeDao.addOrUpdateTitle(lineEntry);
		int index = titeDao.selectTitleByUrl(lineEntry.getUrl()).getID();
		fireTableRowsUpdated(index, index);//同时调用这2个方法，会有什么问题吗？？？//TODO
		fireTableRowsInserted(index, index);

		//need to use row-1 when add setRowSorter to table. why??
		//https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
		//fireTableRowsInserted(newsize-1, newsize-1);
	}
	/*
	这个方法更新了URL的比对方法，无论是否包含默认端口都可以成功匹配
	 */
	public LineEntry findLineEntry(String url) {//这里的URL需要包含默认端口!!!
		//之前的方法：统一使用URL的格式进行比较，最需要自己主动用for循环去遍历元素，然后对比。但这种方法不能发挥hashmap的查找速度优势。
		//更好的方法：用hashMap的get方法去查找，看是否能找到对象，get方法是根据key的hash值进行查找的速度比自行循环对比快很多。

		//统一URL字符串的格式
		url = Commons.formateURLString(url);
		return titeDao.selectTitleByUrl(url);
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
		List<LineEntry> result = new ArrayList<LineEntry>();
		for (LineEntry entry:titeDao.selectAllTitle()) {
			String urlkey = entry.getUrl();
			try{//根据host查找
				URL URL = new URL(urlkey);
				if (URL.getHost().equalsIgnoreCase(host)) {
					result.add(titeDao.selectTitleByUrl(urlkey));
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
		for (LineEntry value:titeDao.selectAllTitle()) {
			try{//根据host查找
				if (value.getHost().equalsIgnoreCase(host) && value.getPort() == port) {
					result.add(titeDao.selectTitleByUrl(value.getUrl()));
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

	public void freshAllASNInfo(){
		for (LineEntry entry : titeDao.selectAllTitle()) {
			entry.freshASNInfo();
		}
		fireTableRowsUpdated(0,titeDao.getRowCount()-1);
	}
}