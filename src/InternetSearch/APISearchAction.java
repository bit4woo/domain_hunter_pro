package InternetSearch;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;

import InternetSearch.Client.FoFaClient;
import InternetSearch.Client.HunterClient;
import InternetSearch.Client.HunterIoClient;
import InternetSearch.Client.QuakeClient;
import InternetSearch.Client.ShodanClient;
import InternetSearch.Client.ZoomEyeClient;
import burp.BurpExtender;
import domain.DomainManager;
import domain.target.TargetTableModel;
import title.LineTableModel;

public class APISearchAction extends AbstractAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 1933197856582351336L;
	private List<String> engineList = new ArrayList<>();

	AbstractTableModel lineModel;
	int[] modelRows;
	int columnIndex;

	private PrintWriter stdout;
	private PrintWriter stderr;

	// 记录搜索过并且没有关闭的tab
	public static Set<String> searchedContent = new HashSet<String>();

	APISearchAction() {
		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList,
			boolean autoAddToTarget, boolean showInGUI) {
		super();

		if (!lineModel.getClass().equals(LineTableModel.class) && !lineModel.getClass().equals(SearchTableModel.class)
				&& !lineModel.getClass().equals(TargetTableModel.class)) {
			stderr.println("wrong AbstractTableModel object");
		}

		this.lineModel = lineModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engineList = engineList;
		if (engineList.size() == 1) {
			putValue(Action.NAME, "API Search On " + engineList.get(0).trim());
		} else {
			putValue(Action.NAME, "API Search On " + engineList.size() + " engines");
		}
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine,
			boolean autoAddToTarget, boolean showInGUI) {
		this(lineModel, modelRows, columnIndex, new ArrayList<>(Collections.singletonList(engine)), autoAddToTarget,
				showInGUI);
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine) {
		this(lineModel, modelRows, columnIndex, engine, false, true);
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList) {
		this(lineModel, modelRows, columnIndex, engineList, false, true);
	}

	@Override
	public final void actionPerformed(ActionEvent e) {
		Set<ToSearchItem> toSearch = new HashSet<>();
		for (int row : modelRows) {

			String searchType = null;
			String searchContent = null;

			if (lineModel.getClass().equals(LineTableModel.class)) {
				InfoTuple<String, String> result = ((LineTableModel) lineModel).getSearchTypeAndValue(row,
						columnIndex);
				searchType = result.first;
				searchContent = result.second;
			}

			if (lineModel.getClass().equals(SearchTableModel.class)) {
				InfoTuple<String, String> result = ((SearchTableModel) lineModel).getSearchTypeAndValue(row,
						columnIndex);
				searchType = result.first;
				searchContent = result.second;
			}

			if (lineModel.getClass().equals(TargetTableModel.class)) {
				InfoTuple<String, String> result = ((TargetTableModel) lineModel).getSearchTypeAndValue(row,
						columnIndex);
				searchType = result.first;
				searchContent = result.second;
			}

			ToSearchItem item = new ToSearchItem(searchType, searchContent);

			toSearch.add(item);
		}

		if (toSearch.size() >= 50) {
			JOptionPane.showMessageDialog(null, "too many items selected!! should less than 50", "Alert",
					JOptionPane.WARNING_MESSAGE);
			stderr.print("too many items selected!! should less than 50");
			return;
		}

		// 把耗时操作放在最后。
		for (ToSearchItem item : toSearch) {
			// 可能存在，一个搜索结果还未显示，又有另外一次相同内容搜索出现的情况。但是影响不大，就不管了
			DoSearchAllInOnAtBackGround(item.getSearchType(), item.getSearchContent(), APISearchAction.this.engineList);

		}
	}

	public static List<SearchResultEntry> DoSearch(String searchType, String searchContent, String engine) {
		List<SearchResultEntry> entries = new ArrayList<>();
		if (StringUtils.isEmpty(searchContent)) {
			return entries;
		}

		if (engine.equals(SearchEngine.FOFA)) {
			entries = new FoFaClient().SearchToGetEntry(searchContent, searchType);
		} else if (engine.equals(SearchEngine.SHODAN)) {
			entries = new ShodanClient().SearchToGetEntry(searchContent, searchType);
		} else if (engine.equals(SearchEngine.ZOOMEYE)) {
			entries = new ZoomEyeClient().SearchToGetEntry(searchContent, searchType);
		} else if (engine.equals(SearchEngine.QIANXIN_HUNTER)) {
			entries = new HunterClient().SearchToGetEntry(searchContent, searchType);
		} else if (engine.equals(SearchEngine.QIANXIN_TI)) {
			// entries = new Client().SearchToGetEntry(searchContent, searchType);
			// TODO
		} else if (engine.equals(SearchEngine.QUAKE_360)) {
			entries = new QuakeClient().SearchToGetEntry(searchContent, searchType);
		} else if (engine.equals(SearchEngine.TI_360)) {
			// entries = new Client().SearchToGetEntry(searchContent, searchType);
			// TODO
		} else if (engine.equals(SearchEngine.HUNTER_IO)) {
			entries = new HunterIoClient().SearchToGetEntry(searchContent, searchType);
		}
		// https://api.hunter.io/v2/domain-search?domain=intercom.com
		return entries;
	}
	
	
	public static void DoSearchAllInOnAtBackGround(String search_Type, String content, List<String> engineList) {
		
		String searchType;
		if (search_Type == null) {
			if (DomainUtils.isValidDomainNoPort(content)) {
				searchType = SearchType.SubDomain;
			} else if (IPAddressUtils.isValidIPv4NoPort(content)) {
				searchType = SearchType.IP;
			} else {
				searchType = SearchType.OriginalString;
			}
		}else {
			searchType = search_Type;
		}
		
		//避免重复搜索的逻辑
		String tabname = String.format("%s(%s)", searchType, content);
		if (searchedContent.add(tabname)) {
			// 保证单次操作，不对相同项进行重复搜索
			System.out.println("begin search " + tabname);
			BurpExtender.getStdout().println("begin search " + tabname);
		} else {
			System.out.println("skip search " + tabname);
			BurpExtender.getStdout().println("skip search " + tabname);
			// skip后，重新将tab的颜色改回来，以便提示这个tab被再次搜索了
			BurpExtender.getGui().getSearchPanel().changeTabColor(tabname, Color.WHITE);
			return;
		}
		
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				DoSearchAllInOn(searchType, content, engineList, true, false);
				return null;
			}

			@Override
			protected void done() {

			}
		};
		worker.execute();
		//"errmsg":"[45012] 请求速度过快"
		try {
			Thread.sleep(200);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	
	/**
	 * 多个搜索引擎 进行同类型搜索时使用，比如都搜索子域名
	 *
	 * @param searchType
	 * @param content
	 * @param engineList
	 * @return
	 */
	@Deprecated //使用 DoSearchAllInOnAtBackGround() 方法,不要直接调用这个方法
	private static List<SearchResultEntry> DoSearchAllInOn(String searchType, String content, List<String> engineList) {
		return DoSearchAllInOn(searchType, content, engineList, true, false);
	}

	@Deprecated //使用 DoSearchAllInOnAtBackGround() 方法,不要直接调用这个方法
	private static List<SearchResultEntry> DoSearchAllInOn(String searchType, String content, List<String> engineList,
			boolean showInGUI, boolean autoAddToTarget) {
		if (StringUtils.isEmpty(content) || StringUtils.isEmpty(searchType)) {
			BurpExtender.getStderr().print("nothing to search...");
			return null;
		}
		
		List<SearchResultEntry> entries = new ArrayList<>();

		for (String engine : engineList) {
			if (engine.equals(SearchEngine.HUNTER_IO)) {
				// 这个逻辑有点不严谨，目前看时没问题的，后续有重大变更时注意
				searchType = SearchType.Email;
			}
			entries.addAll(DoSearch(searchType, content, engine));
		}

		if (showInGUI) {
			String tabname = String.format("%s(%s)", searchType, content);
			BurpExtender.getGui().getSearchPanel().addSearchTab(tabname, entries, engineList);
		}

		// 暂时不启用，之所以要设计图形界面，就是为了加入人为判断。
		autoAddToTarget = false;
		if (autoAddToTarget) {
			DomainManager result = BurpExtender.getGui().getDomainPanel().getDomainResult();
			for (SearchResultEntry entry : entries) {
				String host = entry.getHost();
				String rootDomain = entry.getRootDomain();
				result.addIfValid(host);
				List<String> ips = IPAddressUtils.grepIPv4MayPort(host);
				for (String ip : ips) {
					if (IPAddressUtils.isValidIPv4MayPort(ip)) {
						result.getSpecialPortTargets().add(ip);
					}
				}
				result.getSimilarDomainSet().add(rootDomain);
			}
		}

		return entries;
	}

	public static String capitalizeFirstLetter(String str) {
		if (StringUtils.isEmpty(str)) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}

class ToSearchItem {
	String searchType = "";
	String searchContent = "";

	ToSearchItem(String searchType, String searchContent) {
		this.searchType = searchType;
		this.searchContent = searchContent;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getSearchContent() {
		return searchContent;
	}

	public void setSearchContent(String searchContent) {
		this.searchContent = searchContent;
	}

	public String getTabName() {
		String tabname = String.format("%s(%s)", searchType, searchContent);
		return tabname;
	}

	@Override
	public int hashCode() {
		return getTabName().hashCode();
	}
}
