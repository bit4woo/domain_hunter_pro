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

	String sourceTabName;

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
			String sourceTabName, boolean autoAddToTarget, boolean showInGUI) {
		super();

		if (!lineModel.getClass().equals(LineTableModel.class) && !lineModel.getClass().equals(SearchTableModel.class)
				&& !lineModel.getClass().equals(TargetTableModel.class)) {
			stderr.println("wrong AbstractTableModel object");
		}

		this.lineModel = lineModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engineList = engineList;
		this.sourceTabName = sourceTabName;
		if (engineList.size() == 1) {
			putValue(Action.NAME, "API Search On " + engineList.get(0).trim());
		} else {
			putValue(Action.NAME, "API Search On " + engineList.size() + " engines");
		}
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine,
			String sourceTabName, boolean autoAddToTarget, boolean showInGUI) {
		this(lineModel, modelRows, columnIndex, new ArrayList<>(Collections.singletonList(engine)), sourceTabName,
				autoAddToTarget, showInGUI);
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine,
			String sourceTabName) {
		this(lineModel, modelRows, columnIndex, engine, sourceTabName, false, true);
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList,
			String sourceTabName) {
		this(lineModel, modelRows, columnIndex, engineList, sourceTabName, false, true);
	}

	@Override
	public final void actionPerformed(ActionEvent e) {
		Set<ToSearchItem> toSearch = new HashSet<>();
		for (int row : modelRows) {

			String searchType = null;
			String searchContent = null;

			if (lineModel.getClass().equals(LineTableModel.class)) {
				InfoTuple<String, String> result = ((LineTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
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

//		if (toSearch.size() >= 50) {
//			JOptionPane.showMessageDialog(null, "too many items selected!! should less than 50", "Alert",
//					JOptionPane.WARNING_MESSAGE);
//			stderr.print("too many items selected!! should less than 50");
//			return;
//		}

		// 把耗时操作放在最后。
		DoSearchAllWithXEnginesAtBG(toSearch, APISearchAction.this.engineList,
				this.sourceTabName);
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
	
	/**
	 * 在后台，用多个引擎搜索多个内容
	 * @param toSearch
	 * @param engineList
	 * @param sourceTabName
	 */
	public static void DoSearchAllWithXEnginesAtBG(Set<ToSearchItem> toSearch, List<String> engineList,
			String sourceTabName) {
		
		if (toSearch.isEmpty()) {
			return;
		}

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				for (ToSearchItem item:toSearch) {
					String searchType = item.getSearchType();
					String content = item.getSearchContent();
					
					DoSearchOneWithXEngines(searchType, content, engineList, sourceTabName, true, false);
					try {
						//避免 "errmsg":"[45012] 请求速度过快"				
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				return null;
			}

			@Override
			protected void done() {

			}
		};
		worker.execute();
	}
	
	/**
	 * 在后台，用多个引擎搜索一个内容
	 * @param searchType
	 * @param content
	 * @param engineList
	 * @param sourceTabName
	 */
	public static void DoSearchOneWithXEnginesAtBG(String searchType, String content, List<String> engineList,
			String sourceTabName) {

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				DoSearchOneWithXEngines(searchType, content, engineList, sourceTabName, true, false);
				return null;
			}

			@Override
			protected void done() {

			}
		};
		worker.execute();
	}

	/**
	 * 用多个引擎，搜索单个内容。不要直接调用这个方法
	 * @param searchType
	 * @param content
	 * @param engineList
	 * @param sourceTabName
	 * @param showInGUI
	 * @param autoAddToTarget
	 * @return
	 */
	private static List<SearchResultEntry> DoSearchOneWithXEngines(String searchType, String content, List<String> engineList,
			String sourceTabName, boolean showInGUI, boolean autoAddToTarget) {

		if (searchType == null) {
			if (DomainUtils.isValidDomainNoPort(content)) {
				searchType = SearchType.SubDomain;
			} else if (IPAddressUtils.isValidIPv4NoPort(content)) {
				searchType = SearchType.IP;
			} else {
				searchType = SearchType.OriginalString;
			}
		}

		if (StringUtils.isEmpty(content)) {
			BurpExtender.getStderr().print("nothing to search...");
			return null;
		}

		List<SearchResultEntry> entries = new ArrayList<>();
		
		// 避免重复搜索的逻辑
		String tabname = String.format("%s(%s)", searchType, content);

		// 这个方法在多线程中不行
		// Set<String> searchedContent =
		// BurpExtender.getGui().getSearchPanel().getAlreadySearchContent();
		// BurpExtender.getStdout().println(searchedContent);

		if (searchedContent.contains(tabname)) {
			System.out.println("skip search " + tabname);
			BurpExtender.getStdout().println("skip search " + tabname);
			// skip后，重新将tab的颜色改回来，以便提示这个tab被再次搜索了
			BurpExtender.getGui().getSearchPanel().changeTabColor(tabname, Color.RED);
			return null;
		} else {
			// 保证单次操作，不对相同项进行重复搜索
			System.out.println("begin search " + tabname);
			searchedContent.add(tabname);
			BurpExtender.getStdout().println("begin search " + tabname);
		}

		for (String engine : engineList) {
			if (engine.equals(SearchEngine.HUNTER_IO)) {
				// 这个逻辑有点不严谨，目前看时没问题的，后续有重大变更时注意
				searchType = SearchType.Email;
			}
			entries.addAll(DoSearch(searchType, content, engine));
		}

		if (showInGUI) {
			BurpExtender.getGui().getSearchPanel().addSearchTab(tabname, entries, engineList, sourceTabName);
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

