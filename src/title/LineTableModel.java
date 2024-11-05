package title;

import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import com.bit4woo.utilbox.burp.HelperPlus;
import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.UrlUtils;

import GUI.GUIMain;
import InternetSearch.InfoTuple;
import InternetSearch.SearchType;
import base.Commons;
import base.IndexedHashMap;
import base.IntArraySlice;
import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IMessageEditorController;
import dao.TitleDao;
import domain.DomainManager;
import domain.target.TargetTableModel;

/**
 * 关于firexxx，目的是通知各个modelListener。默认的listener中，有一种的目的是：当数据发生变化时，更新GUI的显示。
 * 看DefaultTableModel的源码可以知道，它的数据变动都调用了firexxx函数，如果直接使用DefaultTableModel就无需再主动调用firexxx函数。
 * 而在自己实现的TableModel中，则需要注意主动调用，否则监听器时不会知道的。GUI不会更新，或者其他报错数据的监听器也不会执行。
 *
 */
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
	public static final List<String> HeadList = LineTableHead.getTableHeadList();



	public LineTableModel(GUIMain guiMain){
		this.guiMain = guiMain;
		titleDao = new TitleDao(BurpExtender.getDataLoadManager().getCurrentDBFile());
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

	/**
	 * 当执行get title时，逻辑是重新
	 * @return
	 */
	public boolean clearDataInDBFile(){
		return titleDao.clearData();
	}

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

	public LineEntry getRowAt(int rowIndex) {
		return getLineEntries().get(rowIndex);
	}

	////////////////////// extend AbstractTableModel////////////////////////////////

	@Override
	public int getColumnCount()
	{
		return HeadList.size();//the one is the request String + response String,for search
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == HeadList.indexOf("#")) {
			return Integer.class;//id
		}
		if (columnIndex == HeadList.indexOf("Status")) {
			return Integer.class;
		}
		if (columnIndex == HeadList.indexOf("Length")) {
			return Integer.class;
		}
		if (columnIndex == HeadList.indexOf("isChecked")) {
			return String.class;
		}
		if (columnIndex == HeadList.indexOf("Favicon")) {
			return ImageIcon.class;
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
		if (columnIndex >= 0 && columnIndex <= HeadList.size()) {
			return HeadList.get(columnIndex);
		}else {
			return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (HeadList.get(columnIndex).equals("Comments")) {//可以编辑comment
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
		if (columnIndex == HeadList.indexOf("#")) {
			return rowIndex;
		}
		else if (columnIndex == HeadList.indexOf("URL")){
			return entry.getUrl();
		}
		else if (columnIndex == HeadList.indexOf("Status")){
			return entry.getStatuscode();
		}
		else if (columnIndex == HeadList.indexOf("Length")){
			return entry.getContentLength();
		}
		else if (columnIndex == HeadList.indexOf("Server")){
			return entry.getWebcontainer();
		}
		else if (columnIndex == HeadList.indexOf("Title")){
			return entry.getTitle();
		}
		else if (columnIndex == HeadList.indexOf("IP")){
			return String.join(",", new TreeSet<String>(entry.getIPSet()));//用TreeSet进行排序
		}
		else if (columnIndex == HeadList.indexOf("CNAME|CertInfo")){
			return entry.fetchCNAMEAndCertInfo();
		}
		else if (columnIndex == HeadList.indexOf("Comments")){
			return String.join(",", new TreeSet<String>(entry.getComments()));//用TreeSet进行排序
		}
		else if (columnIndex == HeadList.indexOf("CheckDoneTime")){
			return entry.getTime();
		}
		else if (columnIndex == HeadList.indexOf("isChecked")){
			return entry.getCheckStatus();
		}
		else if (columnIndex == HeadList.indexOf("AssetType")){
			return entry.getAssetType();
		}
		else if (columnIndex == HeadList.indexOf("Favicon")){
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
		else if (columnIndex == HeadList.indexOf("IconHash")){
			return entry.getIcon_hash();
		}
		else if (columnIndex == HeadList.indexOf("ASNInfo")){
			return entry.getASNInfo();
		}
		else if (columnIndex == HeadList.indexOf("Source")) {
			return entry.getEntrySource();
		}
		return "";
	}


	/**
	 * 返回可以用于网络搜索引擎进行搜索地字段
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public InfoTuple<String, String> getSearchTypeAndValue(int rowIndex, int columnIndex) {
		if (rowIndex >= lineEntries.size()) {
			return new InfoTuple<>(null,null);
		}
		LineEntry entry = lineEntries.get(rowIndex);

		if (columnIndex == HeadList.indexOf(LineTableHead.Title)){
			String value =  entry.getTitle();
			return new InfoTuple<>(SearchType.Title, value);
		}else if (columnIndex == HeadList.indexOf(LineTableHead.Server)){
			String value =  entry.getWebcontainer();
			return new InfoTuple<>(SearchType.Server, value);
		}else if (columnIndex == HeadList.indexOf(LineTableHead.IP)){
			if (entry.getIPSet().iterator().hasNext()) {
				String value = entry.getIPSet().iterator().next();
				if (IPAddressUtils.isPublicIPv4NoPort(value)) {
					return new InfoTuple<>(SearchType.IP, value);
				}
			}
			return new InfoTuple<>(null, null);
		}else if (columnIndex == HeadList.indexOf(LineTableHead.Favicon) || columnIndex == HeadList.indexOf(LineTableHead.IconHash)){
			String value = entry.getIcon_hash();
			return new InfoTuple<>(SearchType.IconHash, value);
		}else if (columnIndex == HeadList.indexOf(LineTableHead.CNAMEAndCertInfo)){
			//String value =  String.join(",", new TreeSet<String>(entry.getCertDomainSet()));
			if (entry.getCertDomainSet().iterator().hasNext()) {
				String value = entry.getCertDomainSet().iterator().next();
				return new InfoTuple<>(SearchType.SubDomain, value);
			}
			return new InfoTuple<>(null, null);
		}else if (columnIndex == HeadList.indexOf(LineTableHead.ASNInfo)){
			//应该获取ASN编号
			int num =  entry.getAsnNum();
			if (num > 0){
				return new InfoTuple<>(SearchType.Asn, num+"");
			}else {
				return new InfoTuple<>(SearchType.Asn, null);
			}
		}else if (columnIndex == HeadList.indexOf(LineTableHead.Comments)){
			if (entry.getComments().iterator().hasNext()) {
				String value = entry.getComments().iterator().next();
				return new InfoTuple<>(SearchType.OriginalString, value);
			}
			return new InfoTuple<>(null, null);
		}else {
			String value = entry.getHost();
			if (IPAddressUtils.isValidIPv4NoPort(value)) {
				return new InfoTuple<>(SearchType.IP, value);
			}else {
				return new InfoTuple<>(SearchType.SubDomain, value);
			}
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		LineEntry entry = lineEntries.get(row);
		if (entry == null) {
			throw new ArrayIndexOutOfBoundsException("can't find item with index "+row);
		}
		if (col == HeadList.indexOf(LineTableHead.Comments)){
			String valueStr = ((String) value).trim();
			entry.setComments(new HashSet<>(Arrays.asList(valueStr.split(","))));
			titleDao.addOrUpdateTitle(entry);//写入数据库
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
	Set<String> getIPSetFromTitle(boolean excludeCDN,boolean excludePrivate) {
		Set<String> result = new HashSet<String>();

		for(LineEntry line:lineEntries.values()) {
			if (excludeCDN && line.isCDN()) {
				continue;
			}

			for (String ip:line.getIPSet()) {
				if (excludePrivate && IPAddressUtils.isPrivateIPv4NoPort(ip)) {
					continue;
				}
				result.add(ip);
			}
			continue;
		}
		return result;
	}

	/**
	 * 获取根据确定目标汇算出来的网段，减去已确定目标本身后，剩余的IP地址。
	 * @return 扩展IP集合
	 */
	public Set<String> GetExtendIPSet(boolean excludeCDN,boolean excludePrivate) {

		Set<String> IPsOfDomain = getIPSetFromTitle(excludeCDN,excludePrivate);//title记录中的IP
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
	public Set<String> GetSubnets(boolean excludeCDN,boolean excludePrivate) {
		Set<String> IPsOfDomain = getIPSetFromTitle(excludeCDN,excludePrivate);//title记录中的IP
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
	
	
	public List<String> getURLsDeduplicatedByIP(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();
		
		Map<String,String> tmpDict = new HashMap<>();
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = lineEntries.get(rows[i]).getUrl();
			
			Set<String> sortedSet = new TreeSet<>(lineEntries.get(rows[i]).getIPSet());
	        // Join the sorted elements into a single string
	        String uniqueString = sortedSet.stream()
	                                       .collect(Collectors.joining(","));
			tmpDict.put(uniqueString, url);
			
		}
		urls.addAll(tmpDict.values());
		return urls;
	}

	public List<String> getURLsOfFavicon(int[] rows) {
		Arrays.sort(rows); //升序
		List<String> urls = new ArrayList<>();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			String url = lineEntries.get(rows[i]).getUrl();
			if (url != null) {
				url = UrlUtils.getBaseUrl(url)+"/favicon.ico";
				urls.add(url);
			}
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
		return getHeaderValues(rows,false,"Location");
	}

	/**
	 * Content-Security-Policy 通常包含很多域名信息
	 * @param rows
	 * @return
	 */
	public List<String> getContentSecurityPolicy(int[] rows) {
		return getHeaderValues(rows,false,"Content-Security-Policy");
	}

	public List<String> getHeaderValues(int[] rows,boolean isRequest,String headerName) {
		List<String> result = new ArrayList<>();
		if (org.apache.commons.lang3.StringUtils.isEmpty(headerName)) {
			return result;
		}

		headerName = headerName.trim();
		if (headerName.endsWith(":")) {
			headerName = headerName.substring(0,headerName.length()-1);
		}

		Arrays.sort(rows); //升序

		HelperPlus getter = BurpExtender.getHelperPlus();

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = lineEntries.get(rows[i]);

			byte[] pack;
			if (isRequest) {
				pack = entry.getRequest();
			}else {
				pack = entry.getResponse();
			}

			if (pack == null) continue;
			String value = getter.getHeaderValueOf(isRequest,pack,headerName);
			result.add(value);
		}
		return result;
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


	public void updateIconHashes(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry entry = lineEntries.get(rows[i]);
			entry.DoGetIconHash();
			this.fireTableRowsUpdated(rows[i], rows[i]);
			titleDao.addOrUpdateTitle(entry);//写入数据库
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

	/////删改操作，需要操作数据库了//TODO/////

	public void removeRows(int[] rows) {
		Arrays.sort(rows); //升序

		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				String url = entry.getUrl();
				lineEntries.remove(index);
				titleDao.deleteTitleByUrl(url);//写入数据库
				stdout.println("!!! "+url+" deleted");
				this.fireTableRowsDeleted(index,index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	public void removeRow(int row) {
		try {
			LineEntry entry = lineEntries.get(row);
			if (entry == null) {
				throw new ArrayIndexOutOfBoundsException("can't find item with index "+row);
			}
			String url = entry.getUrl();
			lineEntries.remove(row);
			titleDao.deleteTitleByUrl(url);//写入数据库
			stdout.println("!!! "+url+" deleted");
			this.fireTableRowsDeleted(row,row);
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
	}
	
	/**
	 * 删除明显非目标的记录
	 * 1、host的类型是useless，并且来源是certain。这类记录往往是由于删除了某些根域名造成的。
	 * 2、来自custom(网络搜索引擎添加、手动添加),但是其证书域名明显不是目标的
	 */
	public void MarkNotTargetRows() {
		TargetTableModel model = guiMain.getDomainPanel().getTargetTable().getTargetModel();

		for (int i=lineEntries.size()-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				LineEntry entry = lineEntries.get(i);
				if (entry == null) {
					continue;
				}
				if (entry.getEntrySource().equalsIgnoreCase(LineEntry.Source_Manual_Saved)){
					continue;
				}
				if (entry.getCertDomainSet().contains("ingress.local")){
					continue;
				}

				
				String host = entry.getHost();
				int port = entry.getPort();

				int type = model.assetType(host);
				
				if (type == DomainManager.IP_ADDRESS) {
					//避免网段内IP、内网IP被删除，应该通过网段信息判断
					continue;
				}
				
				//规则1
				if (DomainUtils.isValidDomainNoPort(host)) {
					if ((type == DomainManager.USELESS || type==DomainManager.SIMILAR_DOMAIN)
							&& entry.getEntrySource().equals(LineEntry.Source_Certain)) {
						
						entry.addComment("Non-Target[host is not target]");
						titleDao.addOrUpdateTitle(entry);//写入数据库
						this.fireTableRowsUpdated(i, i);
						
						continue;
					}
				}else {
					//规则2，根据证书域名进行判断，注意，像ingress.local这种也会被删除
					Set<String> certDomains = entry.getCertDomainSet();
					if (certDomains.size()>0) {

						int uselessCount = 0;
						for (String domain:certDomains) {
							type = model.assetType(domain);
							if (type == DomainManager.USELESS || type==DomainManager.SIMILAR_DOMAIN) {
								uselessCount++;
							}
						}

						if (uselessCount == certDomains.size() &&
								(entry.getEntrySource().equals(LineEntry.Source_Custom_Input) ||
										entry.getEntrySource().equals(LineEntry.Source_Subnet_Extend))
								) {
							guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets().remove(host);
							guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet().add(host);
							
							entry.addComment("Non-Target[cert domain is not target]");
							titleDao.addOrUpdateTitle(entry);//写入数据库
							this.fireTableRowsUpdated(i, i);
							
							continue;
						}
					}else {
						//无证书信息,并且自定义资产中不包含的，删除
						Set<String> customIP = guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets();

						if (!customIP.contains(host) && !customIP.contains(host+":"+port)) {
							
							entry.addComment("Non-Target[host IP not in custom assets]");
							titleDao.addOrUpdateTitle(entry);//写入数据库
							this.fireTableRowsUpdated(i, i);
							
							continue;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	/**
	 * 先标记，再删除，给用户识别空间，以便优化规则。
	 */
	public void removeRowsNotInTargets() {
		MarkNotTargetRows();

		for (int i=lineEntries.size()-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				LineEntry entry = lineEntries.get(i);
				if (entry == null) {
					continue;
				}
				if (entry.getComments().toString().contains("Non-Target[")) {
					String url = entry.getUrl();
					lineEntries.remove(i);
					titleDao.deleteTitleByUrl(url);//写入数据库
					stdout.println("!!! "+url+" deleted, due to : "+entry.getComments().toString());
					this.fireTableRowsDeleted(i,i);
				}
				
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	/**
	 * 暂时只是标记重复项目，然后用户可以手动标记，不直接执行删除操作
	 */
	public void findAndMarkDuplicateItems() {
		for (int i=lineEntries.size()-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				LineEntry entry = lineEntries.get(i);
				markFullSameEntries(entry);

			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
		stdout.println("mark duplicate items done!");
	}


	public void updateRowsStatus(int[] rows,String status) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				entry.setCheckStatus(status);
				if (status.equalsIgnoreCase(LineEntry.CheckStatus_Checked)) {
					entry.setTime(Commons.getNowTimeString());
				}
				titleDao.addOrUpdateTitle(entry);//写入数据库
				stdout.println("$$$ "+entry.getUrl()+" updated");
				this.fireTableRowsUpdated(index, index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}


	public void updateAssetTypeOfRows(int[] rows,String assetType) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				if (assetType.equalsIgnoreCase(entry.getAssetType())) continue;
				entry.setAssetType(assetType);
				titleDao.addOrUpdateTitle(entry);//写入数据库
				stdout.println(String.format("$$$ %s updated [AssetType-->%s]",entry.getUrl(),assetType));
				this.fireTableRowsUpdated(index, index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}


	public void updateComments(int[] rows, String commentAdd) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				entry.addComment(commentAdd);
				titleDao.addOrUpdateTitle(entry);//写入数据库
				stdout.println("$$$ "+entry.getUrl()+" updated");
				this.fireTableRowsUpdated(index, index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}


	public void clearComments(int[] rows) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				entry.getComments().clear();
				titleDao.addOrUpdateTitle(entry);//写入数据库
				stdout.println("$$$ "+entry.getUrl()+" updated");
				this.fireTableRowsUpdated(index, index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	public void freshASNInfo(int[] rows) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				entry.freshASNInfo();
				titleDao.addOrUpdateTitle(entry);//写入数据库
				stdout.println("$$$ "+entry.getUrl()+"ASN Info updated");
				this.fireTableRowsUpdated(index, index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	/**
	 * 	主要用于记录CDN或者云服务的IP地址，在做网段汇算时排除这些IP。
	 */
	public void addIPToTargetBlackList(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				LineEntry entry = lineEntries.get(index);
				if (entry == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet().addAll(entry.getIPSet());
				entry.getEntryTags().add(LineEntry.Tag_NotTargetBaseOnBlackList);
				stdout.println("### IP address "+ entry.getIPSet().toString() +" added to black list");
			} catch (Exception e) {
				e.printStackTrace(stderr);
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
		allDomainSet.addAll(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());

		HashSet<String> tmp = new HashSet<String>();

		for (String item:allDomainSet) {//移除IP
			if (item.contains(":")) {//有可能domain:port的情况
				item = item.split(":")[0];
			}
			if (DomainUtils.isValidDomainNoPort(item)) {
				tmp.add(item);
			}
		}

		Collection<LineEntry> entries = getLineEntries().values();
		for (LineEntry entry:entries) {
			String ip = new ArrayList<String>(entry.getIPSet()).get(0);//这里可能不严谨，如果IP解析既有外网地址又有内网地址就会出错
			if (!IPAddressUtils.isPrivateIPv4NoPort(ip)) {//移除公网解析记录；剩下无解析记录和内网解析记录
				if (entry.getStatuscode() == 403 && DomainUtils.isValidDomainNoPort(entry.getHost())) {
					//do Nothing
				}else {
					tmp.remove(entry.getHost());
				}
			}
		}
		return tmp;
	}

	//为了同时fire多个不连续的行，自行实现这个方法。
	@Deprecated
	private void fireDeleted(int[] rows) {
		List<int[]> slice = IntArraySlice.slice(rows);
		//必须逆序，从高位index开始删除，否则删除的对象和预期不一致！！！
		//上面得到的顺序就是从高位开始的
		for(int[] sli:slice) {
			System.out.println(Arrays.toString(sli));
			this.fireTableRowsDeleted(sli[sli.length-1],sli[0]);//这里传入的值必须是低位数在前面，高位数在后面
		}
	}

	@Deprecated
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
		String key = lineEntry.getUrl()+"#"+System.currentTimeMillis();
		lineEntry.setUrl(key);
		lineEntries.put(key,lineEntry);
		int index = lineEntries.IndexOfKey(key);
		fireTableRowsInserted(index, index);//有毫秒级时间戳，只会是新增
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
		titleDao.addOrUpdateTitle(lineEntry);//写入数据库

		int index = lineEntries.IndexOfKey(key);
		try {
			if (ret == null) {
				fireTableRowsInserted(index, index);
				//出错只会暂时影响显示，不影响数据内容，不再打印
				//这里偶尔出现IndexOutOfBoundsException错误，但是debug发现javax.swing.DefaultRowSorter.checkAgainstModel在条件为false时(即未越界)抛出了异常，奇怪！
				//大概率是因为排序器和数据模型不同步导致的，但是每次同步排序器会导致界面数据不停刷新，这个过程中难以操作数据表。
			}else {
				fireTableRowsUpdated(index, index);
			}
		} catch (Exception e) {
			//e.printStackTrace(stderr);
		}

		//need to use row-1 when add setRowSorter to table. why??
		//https://stackoverflow.com/questions/6165060/after-adding-a-tablerowsorter-adding-values-to-model-cause-java-lang-indexoutofb
		//fireTableRowsInserted(newsize-1, newsize-1);
	}

	/**
	 *
	 * 这个方法更新了URL的比对方法，无论是否包含默认端口都可以成功匹配
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
		HelperPlus getter = BurpExtender.getHelperPlus();
		URL fullurl = getter.getFullURL(message);
		LineEntry entry = findLineEntry(fullurl.toString());
		if (entry == null) {
			URL shortUrl = HelperPlus.getBaseURL(message);
			if(!fullurl.equals(shortUrl)) {
				entry = findLineEntry(shortUrl.toString());
			}
		}
		return entry;
	}

	/**
	 * find all lineEntries base host，当需要对整个主机的所有服务进行操作时用这个方法
	 * 当要获得host的IP、CDN信息时可以用这个函数
	 */
	public List<LineEntry> findSameHostEntries(String host) {//
		if (lineEntries == null) return null;
		List<LineEntry> result = new ArrayList<LineEntry>();
		for (String urlkey:lineEntries.keySet()) {
			try{//根据host查找
				URL URL = new URL(urlkey);
				if (URL.getHost().equalsIgnoreCase(host)) {
					result.add(lineEntries.get(urlkey));
				}
			}catch (java.net.MalformedURLException e) {
				if (urlkey.equalsIgnoreCase(host)) {//DNS record
					result.add(lineEntries.get(urlkey));
				}
			}catch (Exception e){
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return result;
	}


	/**
	 *
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


	/**
	 * 查找完全一模一样的数据包（带时间戳锚点的URL可以不同）
	 *
	 * 相同IP和端口,URL path下，即使域名不同，返回包不同（页面包含随机js、css链接），只要status和length相同，就是重复的web服务了
	 */
	public void markFullSameEntries(LineEntry entry) {//
		if (lineEntries == null) return;

		for (int i=lineEntries.size()-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			LineEntry value = lineEntries.get(i);

			if (entry.getComments().contains("duplicateItem")) {
				//已经被标注过，不用再找它的相同项了
				continue;
			}

			if (value.equals(entry)){
				continue;//首先得排除自己，否则删除时就全删除了
			}

			if (value.getStatuscode()!=entry.getStatuscode()){
				continue;
			}

			if (value.getContentLength()!=entry.getContentLength()){
				continue;
			}

			if (value.getPort()!=entry.getPort()){
				continue;
			}

			if (!value.getIPSet().equals(entry.getIPSet())|| value.getIPSet().isEmpty()){
				//只有当IP不为空才有比较的必要
				continue;
			}

			String url1 = value.getUrl().replaceFirst(value.getHost(),"");
			String url2 = entry.getUrl().replaceFirst(entry.getHost(),"");
			if (!Objects.equals(url1, url2)){
				continue;
			}

			value.addComment("duplicateItem");
			value.setCheckStatus(LineEntry.CheckStatus_Checked);
			fireTableRowsUpdated(i,i);//主动通知更新，否则不会写入数据库!!!
		}
	}
}