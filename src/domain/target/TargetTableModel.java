package domain.target;

import GUI.GUIMain;
import burp.*;
import dao.TargetDao;
import dao.TitleDao;

import com.alibaba.fastjson.JSON;
import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;

import Deprecated.DBHelper;
import domain.DomainManager;
import domain.DomainPanel;
import domain.DomainProducer;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import title.IndexedLinkedHashMap;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TargetTableModel extends AbstractTableModel {

	//private IndexedLinkedHashMap<String,TargetEntry> targetEntries =new IndexedLinkedHashMap<String,TargetEntry>();
	TargetDao dao = new TargetDao("F:\\test.db");

	transient PrintWriter stdout;
	transient PrintWriter stderr;

	private static final transient String[] standardTitles = new String[] {
			"Domain/Subnet/IP", "Keyword", "Comment","Black"};
	private static transient List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));

	private static final transient Logger log = LogManager.getLogger(TargetTableModel.class);

	//为了实现动态表结构
	public static List<String> getTitletList() {
		return titletList;
	}
	public TargetTableModel(){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	@Override
	public int getRowCount() {
		return dao.getRowCount();
	}

	@Override
	public int getColumnCount() {
		return standardTitles.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TargetEntry entry = dao.selectByID(rowIndex);
		if (entry == null) return "";
		if (columnIndex == titletList.indexOf("Domain/Subnet/IP")) {
			return entry.getTarget();
		}
		if (columnIndex == titletList.indexOf("Keyword")) {
			return entry.getKeyword();
		}
		if (columnIndex == titletList.indexOf("Comment")) {
			return entry.getComment();
		}
		if (columnIndex == titletList.indexOf("Black")) {
			return entry.isBlack();
		}
		return "";
	}


	@Override
	public void setValueAt(Object value, int row, int col) {
		TargetEntry entry = dao.selectByID(row);
		if (col == titletList.indexOf("Comment")){
			String valueStr = ((String) value).trim();
			entry.setComment(valueStr);
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
		dao.addOrUpdateTarget(entry);
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

	public List<TargetEntry> getAllTargets()
	{
		return dao.selectAll();
	}


	public boolean ifValid(TargetEntry entry) {
		if (entry.getTarget() == null || entry.getTarget().equals("")) {
			return false;
		}
		if (entry.getType() == null || !TargetEntry.TargetTypeList.contains(entry.getType())) {
			return false;
		}

		String target = entry.getTarget();
		if (!(DomainNameUtils.isValidDomain(target) ||
				IPAddressUtils.isValidIP(target)||
				IPAddressUtils.isValidSubnet(target)||
				DomainNameUtils.isValidWildCardDomain(target))) {
			return false;
		}
		return true;
	}

	/**
	 * 最新添加的记录，都是没有ID值的，只有当从数据库中读取出来后才会有
	 * @param entry
	 */
	public void addRowIfValid(TargetEntry entry) {
		if (ifValid(entry)){
				String key = entry.getTarget();
				TargetEntry oldentry = dao.selectByTarget(key);
				if (oldentry != null) {//如果有旧的记录，就需要用旧的内容做修改
					entry.setBlack(oldentry.isBlack());
					entry.setComment(oldentry.getComment());
					entry.setKeyword(oldentry.getKeyword());
				}
				dao.addOrUpdateTarget(entry);
				if (oldentry != null){
					fireTableRowsUpdated(oldentry.getID(),oldentry.getID());
				}else {
					fireTableRowsInserted(dao.getRowCount(),dao.getRowCount());
				}
			}
		}

	/**
	 * 数据的增删查改：删除
	 * @param rowIndex
	 */
	public void removeRow(int rowIndex) {
		dao.deleteByID(rowIndex);
		fireTableRowsDeleted(rowIndex, rowIndex);
	}

	/**
	 * 数据的增删查改：查询
	 */
	public TargetEntry getValueAt(int rowIndex) {
		TargetEntry entry = dao.selectByID(rowIndex);
		return entry;
	}

	/**
	 * 数据的增删查改：修改更新
	 */
	public void updateRow(int rowIndex,TargetEntry entry) {
		dao.addOrUpdateTarget(entry);
		fireTableRowsUpdated(rowIndex,rowIndex);
	}


	/**
	 * 返回目标集合，只返回域名；不包含IP、网段。
	 * example.com
	 * @return
	 */
	public Set<String> fetchTargetDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:dao.selectAll()) {
			if (!ifValid(entry)) continue;
			try {
				if (!entry.isBlack() && entry.getType().equals(TargetEntry.Target_Type_Domain)) {
					result.add(entry.getTarget());
				}
			}catch (Exception e){
				e.printStackTrace();
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
		for (TargetEntry entry:dao.selectAll()) {
			if (!ifValid(entry)) continue;
			try {
				if (!entry.isBlack() && entry.getType().equals(TargetEntry.Target_Type_Wildcard_Domain)) {
					result.add(entry.getTarget());
				}
			}catch (Exception e){
				e.printStackTrace();
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
		for (TargetEntry entry:dao.selectAll()) {
			if (ifValid(entry)) {
				if (!entry.isBlack()) {
					if (entry.getTarget() == null || entry.getType() == null) continue;
					if (entry.getType().equals(TargetEntry.Target_Type_IPaddress)) {
						result.add(entry.getTarget());
					}
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
		for (TargetEntry entry:dao.selectAll()) {
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
		for (TargetEntry entry:dao.selectAll()) {
			if (!ifValid(entry)) continue;
			if (entry.isBlack()) {
				if (entry.getType().equals(TargetEntry.Target_Type_IPaddress))
					result.add(entry.getTarget());
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
		for (TargetEntry entry:dao.selectAll()) {
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
		for (TargetEntry entry:dao.selectAll()) {
			String key = entry.getTarget();
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
		for (String rootDomain : fetchTargetDomainSet()) {
			Set<String> NS = DomainNameUtils.GetAuthoritativeNameServer(rootDomain);
			for (String Server : NS) {
				//stdout.println("checking [Server: "+Server+" Domain: "+rootDomain+"]");
				List<String> Records = DomainNameUtils.ZoneTransferCheck(rootDomain, Server);
				if (Records.size() > 0) {
					try {
						//stdout.println("!!! "+Server+" is zoneTransfer vulnerable for domain "+rootDomain+" !");
						File file = new File(Server + "-ZoneTransfer-" + Commons.getNowTimeString() + ".txt");
						file.createNewFile();
						FileUtils.writeLines(file, Records);
						stdout.println("!!! Records saved to " + file.getAbsolutePath());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
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
		if (!(DomainNameUtils.isValidDomain(domain)||
				IPAddressUtils.isValidIP(domain))) {
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
		
		if (DomainPanel.getDomainResult().getNotTargetIPSet().contains(domain)) {
			return true;
		}
		return false;
	}

	/**
	 * 用于判断收集到的域名或IP是不是我们的有效目标
	 * @param domain
	 * @return
	 */
	@Deprecated //
	public boolean isTargetDep(String domain) {
		if (domain.contains(":")) {//处理带有端口号的域名
			domain = domain.substring(0,domain.indexOf(":"));
		}
		if (!(DomainNameUtils.isValidDomain(domain)||
				IPAddressUtils.isValidIP(domain))) {
			return false;
		}

		if (isBlack(domain))return false;
		//先过黑名单，如果在黑名单中，直接排除

		for (String rootdomain:fetchTargetDomainSet()) {
			if (domain.endsWith("."+rootdomain)||domain.equalsIgnoreCase(rootdomain)){
				return true;
			}
		}

		return fetchTargetIPSet().contains(domain);
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
				e.printStackTrace();
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
			domainOrIP = DomainNameUtils.clearDomainWithoutPort(domainOrIP);

			//格式校验，package那么也是符合域名的正则格式的。
			if (!DomainNameUtils.isValidDomain(domainOrIP) && !IPAddressUtils.isValidIP(domainOrIP)) {
				debugPrint(domainOrIP,DomainManager.USELESS,"Not a valid domain or IP address");
				return DomainManager.USELESS;
			}

			if (isBlack(domainOrIP)) {
				debugPrint(domainOrIP,DomainManager.USELESS,"In black list");
				return DomainManager.USELESS;
			}

			Set<String> targetDomains = fetchTargetDomainSet();
			for (String rootdomain:targetDomains) {
				rootdomain  = DomainNameUtils.clearDomainWithoutPort(rootdomain);
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
				rootdomain  = DomainNameUtils.clearDomainWithoutPort(rootdomain);
				if (DomainNameUtils.isWhiteListTLD(domainOrIP,rootdomain)) {
					debugPrint(domainOrIP,DomainManager.TLD_DOMAIN,"TLD-domain of "+rootdomain);
					return DomainManager.TLD_DOMAIN;
				}
			}

			Set<String> targetWildCardDomains = fetchTargetWildCardDomainSet();
			for (String rootdomain:targetWildCardDomains) {
				rootdomain  = DomainNameUtils.clearDomainWithoutPort(rootdomain);
				if (DomainNameUtils.isMatchWildCardDomain(rootdomain,domainOrIP)){
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

			if(IPAddressUtils.isValidIP(domainOrIP)){
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
		domain = DomainNameUtils.clearDomainWithoutPort(domain);
		Set<String> targetDomains = fetchTargetDomainSet();
		for (String rootdomain : targetDomains) {
			rootdomain = DomainNameUtils.clearDomainWithoutPort(rootdomain);
			if (DomainNameUtils.isWhiteListTLD(domain, rootdomain)) {
				InternetDomainName suffixDomain = InternetDomainName.from(domain).publicSuffix();
				InternetDomainName suffixRootDomain = InternetDomainName.from(rootdomain).publicSuffix();
				if (suffixDomain != null && suffixRootDomain != null) {
					String suffixOfDomain = suffixDomain.toString();
					String suffixOfRootDomain = suffixRootDomain.toString();

					String result = Commons.replaceLast(rootdomain, suffixOfRootDomain, suffixOfDomain);
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
				TargetEntry checked = dao.selectByID(rows[i]);
				checked.addComment(commentAdd);
				dao.addOrUpdateTarget(checked);
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
		fireDeleted(rows);
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

	public static void main(String[] args) {
		TargetTableModel aaa= new TargetTableModel();
		aaa.addRowIfValid(new TargetEntry("www.baidu.com"));
	}

}
