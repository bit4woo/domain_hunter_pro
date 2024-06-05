package domain.target;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.TextUtils;
import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;

import GUI.GUIMain;
import InternetSearch.InfoTuple;
import InternetSearch.SearchType;
import base.IndexedHashMap;
import base.IntArraySlice;
import burp.BurpExtender;
import domain.DomainManager;

public class TargetTableModel extends AbstractTableModel {

	private IndexedHashMap<String,TargetEntry> targetEntries =new IndexedHashMap<String,TargetEntry>();
	transient PrintWriter stdout;
	transient PrintWriter stderr;
	private GUIMain guiMain;

	private static final transient String[] standardTitles = new String[] {
			"#","Domain/Subnet", "Keyword", "Comment","Black"};
	private static transient List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));

	private static final transient Logger log = LogManager.getLogger(TargetTableModel.class);

	//为了实现动态表结构
	public static List<String> getTitleList() {
		return titletList;
	}


	private TargetTableModel(GUIMain guiMain){
		this.guiMain = guiMain;
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	public TargetTableModel(GUIMain guiMain,List<TargetEntry> entries){
		this(guiMain);
		for (TargetEntry entry:entries) {
			if (ifValid(entry)) {
				targetEntries.put(entry.getTarget(), entry);
			}
		}
	}

	//getter setter是为了序列化和反序列化
	public IndexedHashMap<String, TargetEntry> getTargetEntries() {
		return targetEntries;
	}
	//getter setter是为了序列化和反序列化
	public void setTargetEntries(IndexedHashMap<String, TargetEntry> targetEntries) {
		this.targetEntries = targetEntries;
	}

	/**
	 * 转为Json格式
	 * @return
	 */
	public String ToJson() {
		return new Gson().toJson(this);
	}

	/**
	 * 转换为数据模型
	 * @param instanceString
	 * @return
	 */
	public static TargetTableModel FromJson(String instanceString) {
		return new Gson().fromJson(instanceString, TargetTableModel.class);
	}

	/////////////AbstractTableModel的实现函数////////////////////

	@Override
	public int getRowCount() {
		return targetEntries.size();
	}

	@Override
	public int getColumnCount() {
		return standardTitles.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex>=targetEntries.size()){
			return "xxxxx";//TODO test
		}
		TargetEntry entry = targetEntries.get(rowIndex);
		if (entry == null) return "";

		if (columnIndex == titletList.indexOf("#")) {
			return rowIndex;
		}
		if (columnIndex == titletList.indexOf("Domain/Subnet")) {
			return entry.getTarget();
		}
		if (columnIndex == titletList.indexOf("Keyword")) {
			return entry.getKeyword();
		}
		if (columnIndex == titletList.indexOf("Comment")) {
			return String.join(",", entry.getComments());
		}
		if (columnIndex == titletList.indexOf("Black")) {
			return entry.isBlack();
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
		if(columnIndex ==getTitleList().indexOf("Keyword")) {
			String value = getValueAt(rowIndex,columnIndex).toString();
			return new InfoTuple<>(SearchType.OriginalString, value);
		}else if(columnIndex ==getTitleList().indexOf("Comment")) {
			String value = getValueAt(rowIndex,columnIndex).toString();
			return new InfoTuple<>(SearchType.OriginalString, value);
		}else {
			columnIndex =getTitleList().indexOf("Domain/Subnet");
			String value = getValueAt(rowIndex,columnIndex).toString();
			return new InfoTuple<>(SearchType.SubDomain, value);
		}
	}


	@Override
	public void setValueAt(Object value, int row, int col) {
		TargetEntry entry = targetEntries.get(row);
		if (col == titletList.indexOf("Comment")){
			String valueStr = ((String) value).trim();
			entry.setComments(new HashSet<>(Arrays.asList(valueStr.split(","))));
			fireTableCellUpdated(row, col);
		}
		if (col == titletList.indexOf("Keyword")){
			String valueStr = ((String) value).trim();
			entry.setKeyword(valueStr);
			fireTableCellUpdated(row, col);
		}
		if (col == titletList.indexOf("Black")){
			boolean valueStr = ((boolean) value);
			entry.setBlack(valueStr);
			fireTableCellUpdated(row, col);
		}
		guiMain.getDomainPanel().getTargetDao().addOrUpdateTarget(entry);
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
	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == titletList.indexOf("Black")){
			return boolean.class;
		}else if (columnIndex == titletList.indexOf("#")){
			return Integer.class;//如果返回int.class排序会有问题，why？
		}else {
			return String.class;
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (titletList.get(columnIndex).equals("Comment")
				|| titletList.get(columnIndex).equals("Keyword")) {//可以编辑comment
			return true;
		}else {
			return false;
		}
	}
	/////////////^^^^^AbstractTableModel的实现函数^^^^^////////////////////


	/**
	 * TargetEntry的有效性检查
	 * @param entry
	 * @return
	 */
	public static boolean ifValid(TargetEntry entry) {
		if (StringUtils.isEmpty(entry.getTarget())) {
			return false;
		}
		if (StringUtils.isEmpty(entry.getType()) || !TargetEntry.TargetTypeList.contains(entry.getType())) {
			return false;
		}
		return true;
	}

	/**
	 * 应该包含数据库写入操作
	 * @param entry
	 */
	public boolean addRowIfValid(TargetEntry entry) {
		if (ifValid(entry)){
			String key = entry.getTarget();
			addRow(key, entry);
			return true;
		}
		return false;
	}

	/**
	 * 数据的增删查改：新增
	 * TODO 应该操作数据库
	 * @param key
	 * @param entry
	 */
	private void addRow(String key,TargetEntry entry) {
		TargetEntry oldentry = targetEntries.get(key);
		if (oldentry != null) {//如果有旧的记录，就需要用旧的内容做修改
			entry.setBlack(oldentry.isBlack());
			entry.setComments(oldentry.getComments());
			entry.setKeyword(oldentry.getKeyword());
		}

		int oldsize = targetEntries.size();
		targetEntries.put(key,entry);
		int rowIndex = targetEntries.IndexOfKey(key);
		int newsize = targetEntries.size();
		if (oldsize == newsize) {//覆盖修改
			fireTableRowsUpdated(rowIndex,rowIndex);
		}else {//新增
			fireTableRowsInserted(rowIndex,rowIndex);
		}
		guiMain.getDomainPanel().getTargetDao().addOrUpdateTarget(entry);
	}

	/**
	 * 数据的增删查改：删除
	 * @param rowIndex
	 */
	public void removeRow(int rowIndex) {
		String key = targetEntries.get(rowIndex).getTarget();
		targetEntries.remove(rowIndex);
		fireTableRowsDeleted(rowIndex, rowIndex);
		guiMain.getDomainPanel().getTargetDao().deleteByTarget(key);
	}

	/**
	 * 数据的增删查改：删除
	 */
	public void removeRow(String key) {
		int rowIndex = targetEntries.IndexOfKey(key);
		targetEntries.remove(key);
		fireTableRowsDeleted(rowIndex, rowIndex);
		guiMain.getDomainPanel().getTargetDao().deleteByTarget(key);
	}

	/**
	 * 数据的增删查改：查询
	 */
	public TargetEntry getValueAt(int rowIndex) {
		TargetEntry entry = targetEntries.get(rowIndex);
		return entry;
	}

	/**
	 * 数据的增删查改：修改更新
	 */
	public void updateRow(TargetEntry entry) {
		String key = entry.getTarget();
		addRow(key,entry);
	}


	/**
	 * 获取数据集的方法
	 * @return
	 */
	public String fetchRootDomains() {
		return String.join(System.lineSeparator(), targetEntries.keySet());
	}


	/**
	 * 返回目标集合，包含域名、IP、网段。
	 * 8.8.6.0/25
	 * example.com
	 * 8.8.8.8
	 * @return
	 */
	public Set<String> fetchTargetSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!ifValid(entry)) continue;
			if (!entry.isBlack()) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}

	/**
	 * 返回目标集合，只返回域名；不包含IP、网段。
	 * example.com
	 * @return
	 */
	public Set<String> fetchTargetDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!ifValid(entry)) continue;
			try {
				if (!entry.isBlack() && entry.getType().equals(TargetEntry.Target_Type_Domain)) {
					result.add(entry.getTarget());
				}
			}catch (Exception e){
				e.printStackTrace(stderr);
			}
		}
		return result;
	}


	/**
	 * 返回目标集合，只返回正则格式的域名；不包含IP、网段、和纯文本的域名。
	 * seller.*.example.*
	 * @return
	 */
	public Set<String> fetchTargetWildCardDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!ifValid(entry)) continue;
			try {
				if (!entry.isBlack() && entry.getType().equals(TargetEntry.Target_Type_Wildcard_Domain)) {
					result.add(entry.getTarget());
				}
			}catch (Exception e){
				e.printStackTrace(stderr);
			}
		}
		return result;
	}

	/**
	 * 返回目标中的IP 和 网段 目标。可选择是否将网段转换为IP列表；不包含域名。
	 * 8.8.6.0/25 ---默认均转换为IP列表
	 * 8.8.8.8
	 * @return
	 */
	public Set<String> fetchTargetIPSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (ifValid(entry)) {
				if (!entry.isBlack()) {
					if (entry.getTarget() == null || entry.getType() == null) continue;
					if (entry.getType().equals(TargetEntry.Target_Type_Subnet)) {
						List<String> tmpIPs = IPAddressUtils.toIPList(entry.getTarget());
						result.addAll(tmpIPs);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 域名黑名单
	 * @return
	 */
	private Set<String> fetchTargetBlackDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!ifValid(entry)) continue;
			if (entry.isBlack() && entry.getType().equals(TargetEntry.Target_Type_Domain)) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}

	/**
	 * IP黑名单
	 * @return
	 */
	public Set<String> fetchBlackIPSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!ifValid(entry)) continue;
			if (entry.isBlack()) {
				if (entry.getType().equals(TargetEntry.Target_Type_Subnet)) {
					List<String> tmpIPs = IPAddressUtils.toIPList(entry.getTarget());
					result.addAll(tmpIPs);
				}
			}
		}
		return result;
	}

	public Set<String> fetchKeywordSet(){
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (!ifValid(entry)) continue;
			if (!entry.isBlack() && !entry.getKeyword().trim().equals("")) {
				result.add(entry.getKeyword());
			}
		}
		return result;
	}

	/**
	 * 主要用于package name的有效性判断
	 * @return
	 */
	public Set<String> fetchSuffixSet(){
		Set<String> result = new HashSet<String>();
		for (String key:targetEntries.keySet()) {
			String suffix;
			try {
				//InternetDomainName.from(key).publicSuffix() //当不是com、cn等公共的域名结尾时，将返回空。
				suffix = InternetDomainName.from(key).publicSuffix().toString();
			} catch (Exception e) {
				suffix = key.split("\\.",2)[1];//分割成2份
			}
			result.add(suffix);
		}
		return result;
	}

	public void ZoneTransferCheckAll() {
		for (TargetEntry entry: targetEntries.values()) {
			entry.zoneTransferCheck();
		}
	}

	/**
	 * 是否处于黑名单当中;
	 * 1、target中的域名黑名单、网段黑名单、IP黑名单
	 * 2、domainResult中的NotTargetIPSet
	 * @param domain
	 * @return
	 */
	public boolean isBlack(String domain) {
		if (domain.contains(":")) {//处理带有端口号的域名
			domain = domain.substring(0,domain.indexOf(":"));
		}
		if (!(DomainUtils.isValidDomainNoPort(domain)||
				IPAddressUtils.isValidIPv4NoPort(domain))) {
			return false;
		}
		for (String rootdomain:fetchTargetBlackDomainSet()) {
			if (rootdomain.contains(".")&&!rootdomain.endsWith(".")&&!rootdomain.startsWith("."))
			{
				if (domain.endsWith("."+rootdomain)||domain.equalsIgnoreCase(rootdomain)){
					return true;
				}
			}
		}

		if (fetchBlackIPSet().contains(domain)){
			return true;
		}

		if (guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet().contains(domain)) {
			return true;
		}
		return false;
	}

	public void debugPrint(String domain,int type,String reason) {
		boolean debug= false;
		if (debug){
			try {
				String typeStr ="";
				if (type == DomainManager.SUB_DOMAIN){
					typeStr= "SUB_DOMAIN";
				}
				if (type == DomainManager.IP_ADDRESS){
					typeStr= "IP_ADDRESS";
				}
				if (type == DomainManager.PACKAGE_NAME){
					typeStr= "PACKAGE_NAME";
				}
				if (type == DomainManager.TLD_DOMAIN){
					typeStr= "TLD_DOMAIN";
				}
				if (type == DomainManager.USELESS){
					typeStr= "USELESS";
				}
				if (type == DomainManager.NEED_CONFIRM_IP){
					typeStr= "NEED_CONFIRM_IP";
				}

				String line = String.format("%s is recognised as %s, reason: %s",domain,typeStr,reason);
				System.out.println(line);
				BurpExtender.getStdout().println(line);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	/**
	 * 判断域名或IP，是否为我们的目标资产。完全是根据target中的配置来判断的。
	 * 
	 * 当输入exampe.com:8080进行判断时，去除端口不影响结果。
	 * @param domainOrIP
	 * @return
	 */
	public int assetType(String domainOrIP) {
		try {
			domainOrIP = DomainUtils.clearDomainWithoutPort(domainOrIP);

			//格式校验，package那么也是符合域名的正则格式的。
			if (!DomainUtils.isValidDomainNoPort(domainOrIP) && !IPAddressUtils.isValidIPv4NoPort(domainOrIP)) {
				debugPrint(domainOrIP,DomainManager.USELESS,"Not a valid domain or IP address");
				return DomainManager.USELESS;
			}

			if (isBlack(domainOrIP)) {
				debugPrint(domainOrIP,DomainManager.USELESS,"In black list");
				return DomainManager.USELESS;
			}

			Set<String> targetDomains = fetchTargetDomainSet();
			for (String rootdomain:targetDomains) {
				rootdomain  = DomainUtils.clearDomainWithoutPort(rootdomain);
				if (domainOrIP.endsWith("."+rootdomain)||domainOrIP.equalsIgnoreCase(rootdomain)){
					debugPrint(domainOrIP,DomainManager.SUB_DOMAIN,"sub-domain of "+rootdomain);
					return DomainManager.SUB_DOMAIN;
				}
			}

			if (fetchTargetIPSet().contains(domainOrIP)) {
				debugPrint(domainOrIP,DomainManager.IP_ADDRESS,"target IP set contains it");
				return DomainManager.IP_ADDRESS;
			}

			for (String rootdomain:targetDomains) {
				rootdomain  = DomainUtils.clearDomainWithoutPort(rootdomain);
				if (DomainUtils.isWhiteListTLD(domainOrIP,rootdomain)) {
					debugPrint(domainOrIP,DomainManager.TLD_DOMAIN,"TLD-domain of "+rootdomain);
					return DomainManager.TLD_DOMAIN;
				}
			}

			Set<String> targetWildCardDomains = fetchTargetWildCardDomainSet();
			for (String rootdomain:targetWildCardDomains) {
				rootdomain  = DomainUtils.clearDomainWithoutPort(rootdomain);
				if (DomainUtils.isMatchWildCardDomain(rootdomain,domainOrIP)){
					debugPrint(domainOrIP,DomainManager.SUB_DOMAIN,"sub-domain of "+rootdomain);
					return DomainManager.SUB_DOMAIN;
				}
			}

			for (String keyword:fetchKeywordSet()) {
				if (!keyword.equals("") && domainOrIP.contains(keyword)) {
					if (InternetDomainName.from(domainOrIP).hasPublicSuffix()) {//是否是以公开的 .com .cn等结尾的域名。//如果是以比如local结尾的域名，就不会被认可
						debugPrint(domainOrIP,DomainManager.SIMILAR_DOMAIN,"contains keyword "+keyword);
						return DomainManager.SIMILAR_DOMAIN;
					}

					if (fetchSuffixSet().contains(domainOrIP.substring(0, domainOrIP.indexOf(".")))){
						debugPrint(domainOrIP,DomainManager.PACKAGE_NAME,"starts with target domain suffix");
						return DomainManager.PACKAGE_NAME;
					}
				}
			}

			if(IPAddressUtils.isValidIPv4NoPort(domainOrIP)){
				debugPrint(domainOrIP,DomainManager.NEED_CONFIRM_IP,"is a valid IP address, but not in target IP Set");
				return DomainManager.NEED_CONFIRM_IP;
			}
			debugPrint(domainOrIP,DomainManager.USELESS,"not match any rule of targets ");
			return DomainManager.USELESS;
		}catch (java.lang.IllegalArgumentException e) {
			//java.lang.IllegalArgumentException: Not a valid domain name: '-this.state.scroll'
			BurpExtender.getStderr().println(e.getMessage());
			debugPrint(domainOrIP,DomainManager.USELESS,"IllegalArgumentException encountered");
			return DomainManager.USELESS;
		}
		catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			debugPrint(domainOrIP,DomainManager.USELESS,"Exception encountered");
			return DomainManager.USELESS;
		}
	}


	public int emailType(String email) {

		for (String rootDomain:fetchTargetDomainSet()) {
			if (rootDomain.length() >= 2 && email.toLowerCase().endsWith(rootDomain.toLowerCase())) {
				return DomainManager.CERTAIN_EMAIL;
			}
		}
		for (String keyword:fetchKeywordSet()) {
			if (keyword.length() >= 2 && email.contains(keyword)) {
				return DomainManager.SIMILAR_EMAIL;
			}
		}
		return DomainManager.USELESS;
	}


	public String getTLDDomainToAdd(String domain) {
		domain = DomainUtils.clearDomainWithoutPort(domain);
		Set<String> targetDomains = fetchTargetDomainSet();
		for (String rootdomain : targetDomains) {
			rootdomain = DomainUtils.clearDomainWithoutPort(rootdomain);
			if (DomainUtils.isWhiteListTLD(domain, rootdomain)) {
				InternetDomainName suffixDomain = InternetDomainName.from(domain).publicSuffix();
				InternetDomainName suffixRootDomain = InternetDomainName.from(rootdomain).publicSuffix();
				if (suffixDomain != null && suffixRootDomain != null) {
					String suffixOfDomain = suffixDomain.toString();
					String suffixOfRootDomain = suffixRootDomain.toString();

					String result = TextUtils.replaceLast(rootdomain, suffixOfRootDomain, suffixOfDomain);
					return result;
				}
			}
		}
		return domain;
	}


	public void updateComments(int[] rows, String commentAdd) {
		//because thread let the delete action not in order, so we must loop in here.
		//list length and index changed after every remove.the origin index not point to right item any more.
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			TargetEntry checked = targetEntries.get(rows[i]);
			checked.addComment(commentAdd);
			guiMain.getDomainPanel().getTargetDao().addOrUpdateTarget(checked);
		}
		fireUpdated(rows);
	}

	public void clearComments(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			TargetEntry checked = targetEntries.get(rows[i]);
			checked.getComments().clear();
			guiMain.getDomainPanel().getTargetDao().addOrUpdateTarget(checked);
		}
		fireUpdated(rows);
	}

	private void fireUpdated(int[] rows) {
		List<int[]> slice = IntArraySlice.slice(rows);
		for(int[] sli:slice) {
			System.out.println(Arrays.toString(sli));
			this.fireTableRowsUpdated(sli[sli.length-1],sli[0]);//同上，修复更新多个记录时的错误
		}
	}

	public void removeRows(int[] rows) {
		Arrays.sort(rows); //升序
		for (int i=rows.length-1;i>=0 ;i-- ) {//降序删除才能正确删除每个元素
			try {
				int index = rows[i];
				TargetEntry checked = targetEntries.get(index);
				if (checked == null) {
					throw new ArrayIndexOutOfBoundsException("can't find item with index "+index);
				}
				targetEntries.remove(index);
				guiMain.getDomainPanel().getTargetDao().deleteTarget(checked);
				fireTableRowsDeleted(index,index);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	public static void test() {
		TargetEntry aaa = new TargetEntry("103.125.112.0/23");
		System.out.println(ifValid(aaa));
		System.out.println(IPAddressUtils.isValidSubnet("103.125.112.0/23"));
	}

	public static void main(String[] args) {
		test();
	}

}
