package domain.target;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.net.InternetDomainName;
import com.google.gson.Gson;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.DBHelper;
import burp.DomainNameUtils;
import burp.IPAddressUtils;
import domain.DomainPanel;
import title.IndexedLinkedHashMap;

public class TargetTableModel extends AbstractTableModel {

	private IndexedLinkedHashMap<String,TargetEntry> targetEntries =new IndexedLinkedHashMap<String,TargetEntry>();

	PrintWriter stdout;
	PrintWriter stderr;

	private static final String[] standardTitles = new String[] {
			"Domain/Subnet/IP", "Keyword", "Comments","Black"};
	private static List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));

	private static final Logger log = LogManager.getLogger(TargetTableModel.class);

	//为了实现动态表结构
	public static List<String> getTitletList() {
		return titletList;
	}
	public TargetTableModel(){
		try{
			//stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			//stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		/**
		 * 
		 * 注意，所有直接对TargetTableModel中数据的修改，都不会触发该tableChanged监听器。
		 * 除非操作的逻辑中包含了firexxxx来主动通知监听器。
		 * DomainPanel.domainTableModel.fireTableChanged(null);
		 */
		addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				saveTargetToDB();
			}
		});
	}

	//getter setter是为了序列化和反序列化
	public IndexedLinkedHashMap<String, TargetEntry> getTargetEntries() {
		return targetEntries;
	}
	//getter setter是为了序列化和反序列化
	public void setTargetEntries(IndexedLinkedHashMap<String, TargetEntry> targetEntries) {
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

	public void saveTargetToDB() {
		File file = GUIMain.getCurrentDBFile();
		if (file == null) {
			if (null == DomainPanel.getDomainResult()) return;//有数据才弹对话框指定文件位置。
			file = BurpExtender.getGui().dbfc.dialog(false,".db");
			GUIMain.setCurrentDBFile(file);
		}
		if (file != null) {
			DBHelper dbHelper = new DBHelper(file.toString());
			boolean success = dbHelper.saveTargets(this);
			if (success) {
				log.info("target data saved");
			}else {
				log.error("target data save failed");
			}
		}
	}

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
		TargetEntry entry = targetEntries.getValueAtIndex(rowIndex);
		if (entry == null) return "";
		if (columnIndex == titletList.indexOf("Domain/Subnet/IP")) {
			return entry.getTarget();
		}
		if (columnIndex == titletList.indexOf("Keyword")) {
			return entry.getKeyword();
		}
		if (columnIndex == titletList.indexOf("Comments")) {
			return entry.getComment();
		}
		if (columnIndex == titletList.indexOf("Black")) {
			return entry.isBlack();
		}
		return "";
	}


	@Override
	public void setValueAt(Object value, int row, int col) {
		TargetEntry entry = targetEntries.getValueAtIndex(row);
		if (col == titletList.indexOf("Comments")){
			String valueStr = ((String) value).trim();
			entry.setComment(valueStr);
			fireTableCellUpdated(row, col);
		}
		if (col == titletList.indexOf("Keywords")){
			String valueStr = ((String) value).trim();
			entry.setKeyword(valueStr);
			fireTableCellUpdated(row, col);
		}
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
		if (titletList.get(columnIndex).equals("Comments")
				|| titletList.get(columnIndex).equals("Keywords")) {//可以编辑comment
			return true;
		}else {
			return false;
		}
	}

	public void clear() {
		int size = targetEntries.size();
		targetEntries = new IndexedLinkedHashMap<String,TargetEntry>();
		System.out.println("clean targets of old data,"+size+" targets cleaned");
		if (size-1 >=0)	fireTableRowsDeleted(0, size-1);
	}

	/**
	 * 用于加载数据时，直接初始化
	 * @param targetEntries
	 */
	public void setData(IndexedLinkedHashMap<String,TargetEntry> targetEntries) {
		clear();
		this.targetEntries = targetEntries;
		int size = targetEntries.size();
		if (size >=1) {
			fireTableRowsInserted(0, size-1);
		}
	}

	/**
	 * 数据的增删查改：新增
	 * @param key
	 * @param entry
	 */
	public void addRow(String key,TargetEntry entry) {
		int oldsize = targetEntries.size();
		targetEntries.put(key,entry);
		int rowIndex = targetEntries.IndexOfKey(key);
		int newsize = targetEntries.size();
		if (oldsize == newsize) {//覆盖修改
			fireTableRowsUpdated(rowIndex,rowIndex);
		}else {//新增
			fireTableRowsInserted(rowIndex,rowIndex);
		}
	}

	/**
	 * 数据的增删查改：删除
	 * @param rowIndex
	 */
	public void removeRow(int rowIndex) {
		targetEntries.remove(rowIndex);
		fireTableRowsDeleted(rowIndex, rowIndex);
	}

	/**
	 * 数据的增删查改：删除
	 */
	public void removeRow(String key) {
		int rowIndex = targetEntries.IndexOfKey(key);
		targetEntries.remove(key);
		fireTableRowsDeleted(rowIndex, rowIndex);
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
			if (!entry.isBlack() && entry.getType() == TargetEntry.Target_Type_Domain) {
				result.add(entry.getTarget());
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
			if (!entry.isBlack()) {
				if (entry.getType().equals(TargetEntry.Target_Type_IPaddress)) {
					result.add(entry.getTarget());
				}
				if (entry.getType().equals(TargetEntry.Target_Type_Subnet)) {
					List<String> tmpIPs = IPAddressUtils.toIPList(entry.getTarget());
					result.addAll(tmpIPs);
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
			if (entry.isBlack() && entry.getType() == TargetEntry.Target_Type_Domain) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}
	
	/**
	 * IP黑名单
	 * @return
	 */
	public Set<String> fetchBlackIPSet(boolean convertSubnetToIP ) {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
			if (entry.isBlack()) {
				if (entry.getType().equals(TargetEntry.Target_Type_IPaddress))
				result.add(entry.getTarget());
				if (entry.getType().equals(TargetEntry.Target_Type_Subnet)) {
					if (convertSubnetToIP) {
						List<String> tmpIPs = IPAddressUtils.toIPList(entry.getTarget());
						result.addAll(tmpIPs);
					}else{
						result.add(entry.getTarget());
					}
				}
			}
		}
		return result;
	}

	public Set<String> fetchKeywordSet(){
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry:targetEntries.values()) {
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
	 * 是否处于黑名单当中
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
		return false;
	}
	
	/**
	 * 用于判断收集到的域名或IP是不是我们的有效目标
	 * @param domain
	 * @return
	 */
	@Deprecated
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

	public static void main(String[] args) {
		TargetTableModel aaa= new TargetTableModel();
		aaa.addRow("111", new TargetEntry("www.baidu.com"));
		aaa.addRow("2222", new TargetEntry("www.baidu.com"));
		String bbb= aaa.ToJson();
		TargetTableModel ccc = TargetTableModel.FromJson(bbb);
		System.out.println(bbb);
		System.out.println(ccc);
		
		System.out.println(aaa.getValueAt(0).getType() == TargetEntry.Target_Type_Domain);
	}

}
