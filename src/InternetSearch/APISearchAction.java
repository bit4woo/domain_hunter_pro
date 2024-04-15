package InternetSearch;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;

import InternetSearch.Client.FoFaClient;
import InternetSearch.Client.HunterClient;
import InternetSearch.Client.QuakeClient;
import InternetSearch.Client.ShodanClient;
import InternetSearch.Client.ZoomEyeClient;
import burp.BurpExtender;
import burp.IPAddressUtils;
import domain.DomainManager;
import domain.target.TargetTableModel;
import title.LineTableModel;
import utils.GrepUtils;

public class APISearchAction extends AbstractAction {

	/**
	 */
	private static final long serialVersionUID = 1933197856582351336L;

	AbstractTableModel lineModel;
	int[] modelRows;
	int columnIndex;

	private PrintWriter stdout;
	private PrintWriter stderr;

	APISearchAction(){
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
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
		if(engineList.size() ==1) {
			putValue(Action.NAME, "Search On " + engineList.get(0).trim());
		}else {
			putValue(Action.NAME, "Search On " + engineList.size()+" engines");
		}
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine,boolean autoAddToTarget, boolean showInGUI) {
		this(lineModel, modelRows, columnIndex,new ArrayList<>(Collections.singletonList(engine)), autoAddToTarget, showInGUI);
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine) {
		this(lineModel, modelRows, columnIndex,engine, false, true);
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList) {
		this(lineModel, modelRows, columnIndex,engineList, false, true);
	}

	@Override
	public final void actionPerformed(ActionEvent e) {
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			@Override
			protected Map doInBackground() throws Exception {

				if (modelRows.length >= 50) {
					stderr.print("too many items selected!! should less than 50");
					return null;
				}
				for (int row : modelRows) {

					String searchType = null;
					String searchContent = null;

					if (lineModel.getClass().equals(LineTableModel.class)) {
						InfoTuple<String, String> result = ((LineTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
						searchType = result.first;
						searchContent = result.second;
					}

					if (lineModel.getClass().equals(SearchTableModel.class)) {
						InfoTuple<String, String> result = ((SearchTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
						searchType = result.first;
						searchContent = result.second;
					}

					if (lineModel.getClass().equals(TargetTableModel.class)) {
						InfoTuple<String, String> result = ((TargetTableModel) lineModel).getSearchTypeAndValue(row, columnIndex);
						searchType = result.first;
						searchContent = result.second;
					}

					DoAllInOnSearch(searchType,searchContent);

				}
				return null;
			}

			@Override
			protected void done() {

			}
		};
		worker.execute();
	}

	public static List<SearchResultEntry> DoSearch(String searchType,String searchContent, String engine) {
		List<SearchResultEntry> entries = new ArrayList<>();
		if (StringUtils.isEmpty(searchContent)){
			return entries;
		}

		searchContent = SearchEngine.buildSearchDork(searchContent, engine, searchType);

		if (engine.equals(SearchEngine.FOFA)) {
			entries = new FoFaClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.SHODAN)) {
			entries = new ShodanClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.ZOOMEYE)) {
			entries = new ZoomEyeClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.QIANXIN_HUNTER)) {
			entries = new HunterClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.QIANXIN_TI)) {
			//entries = new Client().SearchToGetEntry(searchContent);
			//TODO
		}else if (engine.equals(SearchEngine.QUAKE_360)) {
			entries = new QuakeClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.TI_360)) {
			//entries = new Client().SearchToGetEntry(searchContent);
			//TODO
		}else if (engine.equals(SearchEngine.HUNTER_IO)) {
			//entries = new Client().SearchToGetEntry(searchContent);
			//TODO
		}
		return entries;
	}

	public static List<SearchResultEntry> DoAllInOnSearch(String searchType,String content) {
		return DoAllInOnSearch(searchType,content,true,false);
	}

	public static List<SearchResultEntry> DoAllInOnSearch(String searchType,String content,boolean showInGUI,boolean autoAddToTarget) {
		if (StringUtils.isEmpty(content) || StringUtils.isEmpty(searchType)) {
			BurpExtender.getStderr().print("nothing to search...");
			return null;
		}
		List<SearchResultEntry> entries = new ArrayList<>();

		List<String> engines = SearchEngine.getAssetSearchEngineList();
		for (String engine:engines) {
			entries.addAll(APISearchAction.DoSearch(searchType,content,engine));
		}

		if (showInGUI) {
			String tabname = String.format("%s(%s)",searchType,content);
			BurpExtender.getGui().getSearchPanel().addSearchTab(tabname, entries, engines);
		}

		//暂时不启用，之所以要设计图形界面，就是为了加入人为判断。
		autoAddToTarget = false;
		if (autoAddToTarget) {
			DomainManager result = BurpExtender.getGui().getDomainPanel().getDomainResult();
			for (SearchResultEntry entry : entries) {
				String host = entry.getHost();
				String rootDomain = entry.getRootDomain();
				result.addIfValid(host);
				List<String> ips = GrepUtils.grepIPAndPort(host);
				for (String ip:ips) {
					if (IPAddressUtils.isValidIP(ip)) {
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
