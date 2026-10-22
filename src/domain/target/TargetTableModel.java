package domain.target;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;
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
import dao.DbWriter;
import dao.TargetDao;
import InternetSearch.SearchType;
import base.IndexedHashMap;
import base.IntArraySlice;
import burp.BurpExtender;
import domain.DomainManager;

public class TargetTableModel extends AbstractTableModel {

	private IndexedHashMap<String, TargetEntry> targetEntries = new IndexedHashMap<String, TargetEntry>();
	transient PrintWriter stdout;
	transient PrintWriter stderr;
	private GUIMain guiMain;

	private static final transient String[] standardTitles = new String[] { "#", "Domain/Subnet", "Keyword", "Comment",
			"TrustLevel", "Count", "DigDone" };
	private static transient List<String> titletList = new ArrayList<>(Arrays.asList(standardTitles));

	private static final transient Logger log = LogManager.getLogger(TargetTableModel.class);

	// 为了实现动态表结构
	public static List<String> getTitleList() {
		return titletList;
	}

	private TargetTableModel(GUIMain guiMain) {
		this.guiMain = guiMain;
		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	public TargetTableModel(GUIMain guiMain, List<TargetEntry> entries) {
		this(guiMain);
		for (TargetEntry entry : entries) {
			if (isValid(entry)) {
				targetEntries.put(entry.getTarget(), entry);
			}
		}
	}

	// getter setter是为了序列化和反序列化
	public IndexedHashMap<String, TargetEntry> getTargetEntries() {
		return targetEntries;
	}

	// getter setter是为了序列化和反序列化
	public void setTargetEntries(IndexedHashMap<String, TargetEntry> targetEntries) {
		this.targetEntries = targetEntries;
	}

	/**
	 * 转为Json格式
	 *
	 * @return
	 */
	public String ToJson() {
		return new Gson().toJson(this);
	}

	/**
	 * 转换为数据模型
	 *
	 * @param instanceString
	 * @return
	 */
	public static TargetTableModel FromJson(String instanceString) {
		return new Gson().fromJson(instanceString, TargetTableModel.class);
	}

	///////////// AbstractTableModel的实现函数////////////////////

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
		if (rowIndex >= targetEntries.size()) {
			return "xxxxx";// TODO test
		}
		TargetEntry entry = targetEntries.get(rowIndex);
		if (entry == null)
			return "";

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
		if (columnIndex == titletList.indexOf("TrustLevel")) {
			return entry.getTrustLevel();
		}
		if (columnIndex == titletList.indexOf("Count")) {
			return entry.getSubdomainCount();
		}
		if (columnIndex == titletList.indexOf("DigDone")) {
			return entry.isDigDone();
		}
		return "";
	}

	/**
	 * 返回可以用于网络搜索引擎进行搜索地字段
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public InfoTuple<String, String> getSearchTypeAndValue(int rowIndex, int columnIndex) {
		if (columnIndex == getTitleList().indexOf("Keyword")) {
			String value = getValueAt(rowIndex, columnIndex).toString();
			return new InfoTuple<>(SearchType.OriginalString, value);
		} else if (columnIndex == getTitleList().indexOf("Comment")) {
			String value = getValueAt(rowIndex, columnIndex).toString();
			return new InfoTuple<>(SearchType.OriginalString, value);
		} else {
			columnIndex = getTitleList().indexOf("Domain/Subnet");
			String value = getValueAt(rowIndex, columnIndex).toString();
			return new InfoTuple<>(SearchType.SubDomain, value);
		}
	}

	/**
	 * 当前项目数据库文件路径。
	 * <p>
	 * 优先用 DAO 上记录的文件：读写必须指向同一个文件，否则会出现"读到新项目、写到旧项目"。
	 * 项目尚未打开时返回 null，此时所有写操作静默跳过（原来会直接 NPE）。
	 * </p>
	 */
	private String currentDbFilePath() {
		if (guiMain != null && guiMain.getDomainPanel() != null) {
			TargetDao dao = guiMain.getDomainPanel().getTargetDao();
			if (dao != null) {
				return dao.getDbFilePath();
			}
		}
		File file = BurpExtender.getDataLoadManager().getCurrentDBFile();
		return file == null ? null : file.getPath();
	}

	/**
	 * target 写入的统一出口。
	 * <p>
	 * 为什么所有写操作都必须走这里：原来是"谁改谁直接 addOrUpdateTarget"，
	 * 于是 EDT（单元格编辑、双击切状态）上直接有 SQL，收集过程中也同步写库。
	 * 现在统一进 {@link DbWriter} 的单写线程队列：EDT 不再等 IO，
	 * 同一个 target 在 300ms 内的多次修改会被合并成一次写入。
	 * </p>
	 * <p>
	 * 注意：入队不等于落库。需要确定性落盘的地方（保存项目、另存为）
	 * 由调用方执行 {@code DbWriter.flush()}。
	 * </p>
	 */
	public void saveTarget(TargetEntry entry) {
		String dbFilePath = currentDbFilePath();
		if (dbFilePath == null || entry == null) {
			return;
		}
		DbWriter.getInstance().upsertTarget(dbFilePath, entry);
	}

	/** target 删除的统一出口，同 {@link #saveTarget(TargetEntry)} */
	private void enqueueDelete(String target) {
		String dbFilePath = currentDbFilePath();
		if (dbFilePath == null || target == null) {
			return;
		}
		DbWriter.getInstance().deleteTarget(dbFilePath, target);
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		TargetEntry entry = targetEntries.get(row);
		if (col == titletList.indexOf("Comment")) {
			String valueStr = ((String) value).trim();
			entry.setComments(new HashSet<>(Arrays.asList(valueStr.split(","))));
			fireTableCellUpdated(row, col);
		}
		if (col == titletList.indexOf("Keyword")) {
			String valueStr = ((String) value).trim();
			entry.setKeyword(valueStr);
			fireTableCellUpdated(row, col);
		}

		if (col == titletList.indexOf("TrustLevel")) {
			entry.setTrustLevel((String) value);
			fireTableCellUpdated(row, col);
		}

		if (col == titletList.indexOf("DigDone")) {
			entry.setDigDone((Boolean) value);
			fireTableCellUpdated(row, col);
		}

		saveTarget(entry);
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex >= 0 && columnIndex <= titletList.size()) {
			return titletList.get(columnIndex);
		} else {
			return "";
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == titletList.indexOf("Black") || columnIndex == titletList.indexOf("DigDone")) {
			return boolean.class;
		} else if (columnIndex == titletList.indexOf("#") || columnIndex == titletList.indexOf("Count")) {
			return Integer.class;// 如果返回int.class排序会有问题，why？
		} else {
			return String.class;
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (titletList.get(columnIndex).equals("Comment") || titletList.get(columnIndex).equals("Keyword")) {// 可以编辑comment
			return true;
		} else {
			return false;
		}
	}
	///////////// ^^^^^AbstractTableModel的实现函数^^^^^////////////////////

	/**
	 * TargetEntry的有效性检查
	 *
	 * @param entry
	 * @return
	 */
	public static boolean isValid(TargetEntry entry) {
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
	 *
	 * @param entry
	 */
	public boolean addOrUpdateRowIfValid(TargetEntry entry) {
		if (isValid(entry)) {
			String key = entry.getTarget();
			addOrUpdateRow(key, entry);
			return true;
		}
		return false;
	}

	/**
	 * 数据的增删查改
	 *
	 * @param key
	 * @param entry
	 */
	private void addOrUpdateRow(String key, TargetEntry entry) {
		SwingUtilities.invokeLater(() -> {
			TargetEntry oldentry = targetEntries.get(key);
			if (oldentry != null) {// 如果有旧的记录，就需要用旧的内容做修改
				// entry.setBlack(oldentry.isBlack());
				if (entry.getTrustLevel().equals(AssetTrustLevel.Maybe)) {
					// 当新记录的类型是maybe，那么它是确信度最低的，使用旧值。否则使用新的值
					entry.setTrustLevel(oldentry.getTrustLevel());
				}
				entry.getComments().addAll(oldentry.getComments());
				entry.setKeyword(oldentry.getKeyword());
				entry.setSubdomainCount(oldentry.getSubdomainCount());
				entry.setDigDone(oldentry.isDigDone());
			}

			int oldsize = targetEntries.size();
			targetEntries.put(key, entry);
			int rowIndex = targetEntries.indexOfKey(key);
			int newsize = targetEntries.size();
			if (oldsize == newsize) {// 覆盖修改
				fireTableRowsUpdated(rowIndex, rowIndex);
			} else {// 新增
				fireTableRowsInserted(rowIndex, rowIndex);
			}
			saveTarget(entry);
		});
	}

	/**
	 * 数据的增删查改：删除
	 *
	 * @param rowIndex
	 */
	public void removeRow(int rowIndex) {
		SwingUtilities.invokeLater(() -> {
			String key = targetEntries.get(rowIndex).getTarget();
			targetEntries.remove(rowIndex);
			fireTableRowsDeleted(rowIndex, rowIndex);
			enqueueDelete(key);
		});
	}

	/**
	 * 数据的增删查改：删除
	 */
	public void removeRow(String key) {
		SwingUtilities.invokeLater(() -> {
			int rowIndex = targetEntries.indexOfKey(key);
			targetEntries.remove(key);
			fireTableRowsDeleted(rowIndex, rowIndex);
			enqueueDelete(key);
		});
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
	public void addOrUpdateRow(TargetEntry entry) {
		String key = entry.getTarget();
		addOrUpdateRow(key, entry);
	}

	/**
	 * 获取数据集的方法
	 *
	 * @return
	 */
	public String fetchRootDomains() {
		return String.join(System.lineSeparator(), targetEntries.keySet());
	}

	/**
	 * 返回目标集合，包含域名、IP、网段。 8.8.6.0/25 example.com 8.8.8.8
	 *
	 * @return
	 */
	public Set<String> fetchTargetSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!isValid(entry))
				continue;
			if (!entry.isNotTarget()) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}

	/**
	 * 返回目标集合，只返回域名；不包含IP、网段。 example.com
	 *
	 * @return
	 */
	public Set<String> fetchTargetDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!isValid(entry))
				continue;
			try {
				if (!entry.isNotTarget() && entry.getType().equals(TargetEntry.Target_Type_Domain)) {
					result.add(entry.getTarget());
				}
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
		return result;
	}

	/**
	 * 返回目标集合，只返回正则格式的域名；不包含IP、网段、和纯文本的域名。 seller.*.example.*
	 *
	 * @return
	 */
	public Set<String> fetchTargetWildCardDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!isValid(entry))
				continue;
			try {
				if (!entry.isNotTarget() && entry.getType().equals(TargetEntry.Target_Type_Wildcard_Domain)) {
					result.add(entry.getTarget());
				}
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
		return result;
	}

	/**
	 * 返回目标中的IP 和 网段 目标。可选择是否将网段转换为IP列表；不包含域名。 8.8.6.0/25 ---默认均转换为IP列表 8.8.8.8
	 *
	 * @return
	 */
	public Set<String> fetchTargetIPSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (isValid(entry)) {
				if (!entry.isNotTarget()) {
					if (entry.getTarget() == null || entry.getType() == null)
						continue;
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
	 *
	 * @return
	 */
	private Set<String> fetchTargetBlackDomainSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!isValid(entry))
				continue;
			if (entry.isNotTarget() && entry.getType().equals(TargetEntry.Target_Type_Domain)) {
				result.add(entry.getTarget());
			}
		}
		return result;
	}

	/**
	 * IP黑名单
	 *
	 * @return
	 */
	public Set<String> fetchBlackIPSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!isValid(entry))
				continue;
			if (entry.isNotTarget()) {
				if (entry.getType().equals(TargetEntry.Target_Type_Subnet)) {
					List<String> tmpIPs = IPAddressUtils.toIPList(entry.getTarget());
					result.addAll(tmpIPs);
				}
			}
		}
		return result;
	}

	public Set<String> fetchKeywordSet() {
		Set<String> result = new HashSet<String>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!isValid(entry))
				continue;
			if (!entry.isNotTarget() && !entry.getKeyword().trim().equals("")) {
				result.add(entry.getKeyword());
			}
		}
		return result;
	}

	/**
	 * 主要用于package name的有效性判断
	 *
	 * @return
	 */
	public Set<String> fetchSuffixSet() {
		Set<String> result = new HashSet<String>();
		for (String key : targetEntries.keySet()) {
			String suffix;
			try {
				// InternetDomainName.from(key).publicSuffix() //当不是com、cn等公共的域名结尾时，将返回空。
				suffix = InternetDomainName.from(key).publicSuffix().toString();
			} catch (Exception e) {
				suffix = key.split("\\.", 2)[1];// 分割成2份
			}
			result.add(suffix);
		}
		return result;
	}

	public void ZoneTransferCheckAll() {
		for (TargetEntry entry : targetEntries.values()) {
			entry.zoneTransferCheck();
		}
	}

	/**
	 * 是否处于黑名单当中; 1、target中的域名黑名单、网段黑名单、IP黑名单 2、domainResult中的NotTargetIPSet
	 *
	 * @param domain
	 * @return
	 */
	public boolean isBlack(String domain) {
		if (domain.contains(":")) {// 处理带有端口号的域名
			domain = domain.substring(0, domain.indexOf(":"));
		}
		if (!(DomainUtils.isValidDomainNoPort(domain) || IPAddressUtils.isValidIPv4NoPort(domain))) {
			return false;
		}
		for (String rootdomain : fetchTargetBlackDomainSet()) {
			if (rootdomain.contains(".") && !rootdomain.endsWith(".") && !rootdomain.startsWith(".")) {
				if (domain.endsWith("." + rootdomain) || domain.equalsIgnoreCase(rootdomain)) {
					return true;
				}
			}
		}

		if (fetchBlackIPSet().contains(domain)) {
			return true;
		}

		if (guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet().contains(domain)) {
			return true;
		}
		return false;
	}

	public void debugPrint(String domain, int type, String reason) {
		boolean debug = false;
		if (debug) {
			try {
				String typeStr = "";
				if (type == DomainManager.SUB_DOMAIN) {
					typeStr = "SUB_DOMAIN";
				}
				if (type == DomainManager.IP_ADDRESS) {
					typeStr = "IP_ADDRESS";
				}
				if (type == DomainManager.PACKAGE_NAME) {
					typeStr = "PACKAGE_NAME";
				}
				if (type == DomainManager.TLD_DOMAIN) {
					typeStr = "TLD_DOMAIN";
				}
				if (type == DomainManager.USELESS) {
					typeStr = "USELESS";
				}
				if (type == DomainManager.NEED_CONFIRM_IP) {
					typeStr = "NEED_CONFIRM_IP";
				}

				String line = String.format("%s is recognised as %s, reason: %s", domain, typeStr, reason);
				System.out.println(line);
				BurpExtender.getStdout().println(line);
			} catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}

	/**
	 * 全量刷新：把所有 target 的 subdomainCount 按当前的 subDomainSet 重新计算一遍。
	 * <p>
	 * 只在手动 Refresh（DomainManager.freshBaseRule）时调用。逐个 target 做一次全集合遍历，
	 * 复杂度是 target 数 × 子域名数，因此不适合在收集过程中频繁调用。
	 * </p>
	 */
	public void refreshSubdomainCount() {
		SwingUtilities.invokeLater(() -> {
			Set<String> subDomains = fetchSubDomainSet();
			if (subDomains == null) {
				return;
			}
			for (TargetEntry entry : targetEntries.values()) {
				// 减少数据库操作：计数没变就不写库
				countSubdomainAndSave(entry, subDomains);
			}
			int size = targetEntries.size();
			if (size >= 1) {
				fireTableRowsUpdated(0, targetEntries.size() - 1);
			}
		});

	}

	/**
	 * 单个 target 的版本：只重新计算并写入一个 target 的 subdomainCount。
	 * <p>
	 * 与全量版本的区别：不再遍历整张表格，只对目标行做一次计算和（必要时）一次 upsert，
	 * 可用于单个 target 被新增、编辑之后，或者用户手动修改了某一行之后立即校正计数。
	 * </p>
	 * <p>
	 * 线程说明：计数与写库在调用线程完成（写库是一次小事务，不建议在 EDT 上批量调用），
	 * 只有 UI 通知 {@link #fireTableRowsUpdated} 通过 {@link SwingUtilities#invokeLater} 切回 EDT。
	 * </p>
	 *
	 * @param target TargetEntry.target，即表格 Domain/Subnet 列的值
	 * @return 计数发生变化并已提交写入时返回 true；target 不存在、未加载项目或计数未变时返回 false
	 */
	public boolean refreshSubdomainCount(String target) {
		if (StringUtils.isEmpty(target)) {
			return false;
		}
		TargetEntry entry = findTargetEntry(target);
		if (entry == null) {
			return false;
		}
		Set<String> subDomains = fetchSubDomainSet();
		if (subDomains == null) {
			return false;
		}

		boolean changed = countSubdomainAndSave(entry, subDomains);
		if (changed) {
			int rowIndex = targetEntries.indexOfKey(entry.getTarget());
			if (rowIndex >= 0) {
				SwingUtilities.invokeLater(() -> {
					if (rowIndex < targetEntries.size()) {
						fireTableRowsUpdated(rowIndex, rowIndex);
					}
				});
			}
		}
		return changed;
	}

	/**
	 * 单个域名的增量版本：当有一个新的域名被加入 subDomainSet 时调用，
	 * 只更新那些计数会因此发生变化的 target 行（通常只有 1 行，通配符场景可能多行）。
	 * <p>
	 * 与全量版本的区别：先用 {@link TargetEntry#matchesSubdomain(String)} 过滤，只对归属的 target
	 * 重新计数，避免对无关 target 反复遍历整个子域名集合。
	 * </p>
	 * <p>
	 * 注意：这里每变化一行就写一次库。收集流量时如果逐条调用，会产生大量小事务，
	 * 需要配合批量/异步写队列使用，否则会明显拖慢收集速度。
	 * </p>
	 *
	 * @param domain 单个域名，可带端口（内部会去掉端口）
	 * @return 受影响的 target 行数
	 */
	public int refreshSubdomainCountForDomain(String domain) {
		if (StringUtils.isEmpty(domain)) {
			return 0;
		}
		String host = DomainUtils.clearDomainWithoutPort(domain);
		Set<String> subDomains = fetchSubDomainSet();
		if (subDomains == null) {
			return 0;
		}

		List<Integer> changedRows = new ArrayList<>();
		for (TargetEntry entry : targetEntries.values()) {
			if (!entry.matchesSubdomain(host)) {
				continue;
			}
			if (countSubdomainAndSave(entry, subDomains)) {
				int rowIndex = targetEntries.indexOfKey(entry.getTarget());
				if (rowIndex >= 0) {
					changedRows.add(rowIndex);
				}
			}
		}

		if (!changedRows.isEmpty()) {
			SwingUtilities.invokeLater(() -> {
				for (int rowIndex : changedRows) {
					if (rowIndex < targetEntries.size()) {
						fireTableRowsUpdated(rowIndex, rowIndex);
					}
				}
			});
		}
		return changedRows.size();
	}

	/**
	 * 重新计算单个 target 的计数，只有计数发生变化时才提交写入。
	 *
	 * @return 计数发生变化（已入队，稍后由写线程落盘）返回 true
	 */
	private boolean countSubdomainAndSave(TargetEntry entry, Set<String> subDomains) {
		if (entry == null) {
			return false;
		}
		int oldCount = entry.getSubdomainCount();
		entry.countSubdomain(subDomains);
		if (oldCount == entry.getSubdomainCount()) {
			return false;
		}
		saveTarget(entry);
		return true;
	}

	/**
	 * 取出当前的子域名集合，未加载项目时返回 null。
	 */
	private Set<String> fetchSubDomainSet() {
		DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
		if (domainResult == null) {
			return null;
		}
		return domainResult.getSubDomainSet();
	}

	/**
	 * 按 target 字符串查找对应的 TargetEntry，先精确匹配，再忽略大小写匹配。
	 */
	private TargetEntry findTargetEntry(String target) {
		TargetEntry entry = targetEntries.get(target);
		if (entry != null) {
			return entry;
		}
		for (TargetEntry item : targetEntries.values()) {
			if (target.equalsIgnoreCase(item.getTarget())) {
				return item;
			}
		}
		return null;
	}

	/**
	 * 判断域名或IP，是否为我们的目标资产。完全是根据target中的配置来判断的。
	 * <p>
	 * 当输入exampe.com:8080进行判断时，去除端口不影响结果。
	 *
	 * @param domainOrIP
	 * @return
	 */
	public int assetType(String domainOrIP) {
		try {
			domainOrIP = DomainUtils.clearDomainWithoutPort(domainOrIP);

			// 格式校验，package那么也是符合域名的正则格式的。
			if (!DomainUtils.isValidDomainNoPort(domainOrIP) && !IPAddressUtils.isValidIPv4NoPort(domainOrIP)) {
				debugPrint(domainOrIP, DomainManager.USELESS, "Not a valid domain or IP address");
				return DomainManager.USELESS;
			}

			if (isBlack(domainOrIP)) {
				debugPrint(domainOrIP, DomainManager.USELESS, "In black list");
				return DomainManager.USELESS;
			}

			Set<String> targetDomains = fetchTargetDomainSet();
			for (String rootdomain : targetDomains) {
				rootdomain = DomainUtils.clearDomainWithoutPort(rootdomain);
				if (domainOrIP.endsWith("." + rootdomain) || domainOrIP.equalsIgnoreCase(rootdomain)) {
					debugPrint(domainOrIP, DomainManager.SUB_DOMAIN, "sub-domain of " + rootdomain);
					return DomainManager.SUB_DOMAIN;
				}

				if (DomainUtils.isWhiteListTLD(domainOrIP, rootdomain)) {
					debugPrint(domainOrIP, DomainManager.TLD_DOMAIN, "TLD-domain of " + rootdomain);
					return DomainManager.TLD_DOMAIN;
				}
			}

			if (fetchTargetIPSet().contains(domainOrIP)) {
				debugPrint(domainOrIP, DomainManager.IP_ADDRESS, "target IP set contains it");
				return DomainManager.IP_ADDRESS;
			}

			Set<String> targetWildCardDomains = fetchTargetWildCardDomainSet();
			for (String rootdomain : targetWildCardDomains) {
				rootdomain = DomainUtils.clearDomainWithoutPort(rootdomain);
				if (DomainUtils.isMatchWildCardDomain(rootdomain, domainOrIP)) {
					debugPrint(domainOrIP, DomainManager.SUB_DOMAIN, "sub-domain of " + rootdomain);
					return DomainManager.SUB_DOMAIN;
				}
			}

			for (String keyword : fetchKeywordSet()) {
				if (!keyword.equals("") && domainOrIP.contains(keyword)) {
					if (InternetDomainName.from(domainOrIP).hasPublicSuffix()) {// 是否是以公开的 .com
																				// .cn等结尾的域名。//如果是以比如local结尾的域名，就不会被认可
						debugPrint(domainOrIP, DomainManager.SIMILAR_DOMAIN, "contains keyword " + keyword);
						return DomainManager.SIMILAR_DOMAIN;
					}

					if (fetchSuffixSet().contains(domainOrIP.substring(0, domainOrIP.indexOf(".")))) {
						debugPrint(domainOrIP, DomainManager.PACKAGE_NAME, "starts with target domain suffix");
						return DomainManager.PACKAGE_NAME;
					}
				}
			}

			if (IPAddressUtils.isValidIPv4NoPort(domainOrIP)) {
				debugPrint(domainOrIP, DomainManager.NEED_CONFIRM_IP,
						"is a valid IP address, but not in target IP Set");
				return DomainManager.NEED_CONFIRM_IP;
			}
			debugPrint(domainOrIP, DomainManager.USELESS, "not match any rule of targets ");
			return DomainManager.USELESS;
		} catch (java.lang.IllegalArgumentException e) {
			// java.lang.IllegalArgumentException: Not a valid domain name:
			// '-this.state.scroll'
			e.printStackTrace(BurpExtender.getStderr());
			debugPrint(domainOrIP, DomainManager.USELESS, "IllegalArgumentException encountered");
			return DomainManager.USELESS;
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			debugPrint(domainOrIP, DomainManager.USELESS, "Exception encountered");
			return DomainManager.USELESS;
		}
	}

	public int emailType(String email) {

		for (String rootDomain : fetchTargetDomainSet()) {
			if (rootDomain.length() >= 2 && email.toLowerCase().endsWith(rootDomain.toLowerCase())) {
				return DomainManager.CERTAIN_EMAIL;
			}
		}
		for (String keyword : fetchKeywordSet()) {
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

	/**
	 * 后续朝这个方向修改，避免重复定义多个操作的函数
	 *
	 * @param rows
	 * @param functionName
	 * @param params
	 */
	public void batchAction(int[] rows, String functionName, Object... params) {

		SwingUtilities.invokeLater(() -> {
			Arrays.sort(rows); // 升序

			// 解决 Boolean 变成 boolean.class 没有成功
			Object[] fixedParams = Arrays.stream(params)
					.map(param -> (param instanceof Boolean) ? ((Boolean) param).booleanValue() : param).toArray();

			for (int i = rows.length - 1; i >= 0; i--) {// 降序删除才能正确删除每个元素
				TargetEntry entry = targetEntries.get(rows[i]);
				MethodInvoker.invokeMethod(entry, functionName, fixedParams);
				saveTarget(entry);
			}
			fireUpdated(rows);
		});

	}

	public void updateDigDone(int[] rows, boolean digDone) {
		SwingUtilities.invokeLater(() -> {
			Arrays.sort(rows); // 升序
			for (int i = rows.length - 1; i >= 0; i--) {// 降序删除才能正确删除每个元素
				TargetEntry checked = targetEntries.get(rows[i]);
				checked.setDigDone(digDone);
				saveTarget(checked);
			}
			fireUpdated(rows);
		});

	}

	public void updateComments(int[] rows, String commentAdd) {
		// because thread let the delete action not in order, so we must loop in here.
		// list length and index changed after every remove.the origin index not point
		// to right item any more.
		SwingUtilities.invokeLater(() -> {
			Arrays.sort(rows); // 升序
			for (int i = rows.length - 1; i >= 0; i--) {// 降序删除才能正确删除每个元素
				TargetEntry checked = targetEntries.get(rows[i]);
				checked.addComment(commentAdd);
				saveTarget(checked);
			}
			fireUpdated(rows);
		});

	}

	public void removeComments(int[] rows, String commentToRemove) {
		// because thread let the delete action not in order, so we must loop in here.
		// list length and index changed after every remove.the origin index not point
		// to right item any more.
		SwingUtilities.invokeLater(() -> {
			Arrays.sort(rows); // 升序
			for (int i = rows.length - 1; i >= 0; i--) {// 降序删除才能正确删除每个元素
				TargetEntry checked = targetEntries.get(rows[i]);
				checked.getComments().remove(commentToRemove);
				saveTarget(checked);
			}
			fireUpdated(rows);
		});

	}

	public void clearComments(int[] rows) {
		SwingUtilities.invokeLater(() -> {
			Arrays.sort(rows); // 升序
			for (int i = rows.length - 1; i >= 0; i--) {// 降序删除才能正确删除每个元素
				TargetEntry checked = targetEntries.get(rows[i]);
				checked.getComments().clear();
				saveTarget(checked);
			}
			fireUpdated(rows);
		});

	}

	private void fireUpdated(int[] rows) {
		List<int[]> slice = IntArraySlice.slice(rows);
		for (int[] sli : slice) {
			System.out.println(Arrays.toString(sli));
			this.fireTableRowsUpdated(sli[sli.length - 1], sli[0]);// 同上，修复更新多个记录时的错误
		}
	}

	public void removeRows(int[] rows) {
		SwingUtilities.invokeLater(() -> {
			Arrays.sort(rows); // 升序
			for (int i = rows.length - 1; i >= 0; i--) {// 降序删除才能正确删除每个元素
				try {
					int index = rows[i];
					TargetEntry checked = targetEntries.get(index);
					if (checked == null) {
						throw new ArrayIndexOutOfBoundsException("can't find item with index " + index);
					}
					targetEntries.remove(index);
					enqueueDelete(checked.getTarget());
					fireTableRowsDeleted(index, index);
				} catch (Exception e) {
					e.printStackTrace(stderr);
				}
			}
		});

	}

	public static void test() {
		TargetEntry aaa = new TargetEntry("103.125.112.0/23");
		System.out.println(isValid(aaa));
		System.out.println(IPAddressUtils.isValidSubnet("103.125.112.0/23"));
	}

	public static void main(String[] args) {
		test();
	}

}
