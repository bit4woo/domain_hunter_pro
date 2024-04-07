package InternetSearch;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import InternetSearch.Client.FoFaClient;
import InternetSearch.Client.HunterClient;
import InternetSearch.Client.QuakeClient;
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

	private boolean autoAddToTarget;
	private boolean showInGUI;
	private List<String> engineList;//适配一键用多个搜索引擎进行搜索的逻辑

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, List<String> engineList,
			boolean autoAddToTarget, boolean showInGUI) {
		super();

		if (!lineModel.getClass().equals(LineTableModel.class) && !lineModel.getClass().equals(SearchTableModel.class)
				&& !lineModel.getClass().equals(TargetTableModel.class)) {
			BurpExtender.getCallbacks().printError("wrong AbstractTableModel object");
		}

		this.lineModel = lineModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engineList = engineList;
		if(engineList.size() ==1) {
			putValue(Action.NAME, "Search On " + engineList.get(0).trim());
		}else {
			putValue(Action.NAME, "Search On " + engineList.size()+" engines");
		}
		this.autoAddToTarget = autoAddToTarget;
		this.showInGUI = showInGUI;
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
					BurpExtender.getStderr().print("too many items selected!! should less than 50");
					return null;
				}
				for (int row : modelRows) {

					List<SearchResultEntry> entries = new ArrayList<>();
					String searchContent = null;
					for (String engine:engineList) {

						if (lineModel.getClass().equals(LineTableModel.class)) {
							searchContent = ((LineTableModel) lineModel).getValueForSearch(row, columnIndex, engine);
						}

						if (lineModel.getClass().equals(SearchTableModel.class)) {
							searchContent = ((SearchTableModel) lineModel).getValueForSearch(row, columnIndex, engine);
						}

						if (lineModel.getClass().equals(TargetTableModel.class)) {
							searchContent = ((TargetTableModel) lineModel).getValueForSearch(row, columnIndex, engine);
						}

						if (searchContent == null || searchContent.equals("")) {
							BurpExtender.getStderr().print("nothing to search...");
							return null;
						}
						List<SearchResultEntry> tmp_entries = DoSearch(searchContent, engine);
						entries.addAll(tmp_entries);
					}

					if (showInGUI) {
						//searchContent是最后一个搜索引擎的搜索内容
						BurpExtender.getGui().getSearchPanel().addSearchTab(searchContent, entries, engineList.toString());
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
				}
				return null;
			}

			@Override
			protected void done() {

			}
		};
		worker.execute();
	}

	protected List<SearchResultEntry> DoSearch(String searchContent, String engine) {
		List<SearchResultEntry> entries = new ArrayList<>();
		if (engine.equals(SearchEngine.FOFA)) {
			entries = new FoFaClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.SHODAN)) {

		}else if (engine.equals(SearchEngine.ZOOMEYE)) {
			entries = new ZoomEyeClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.QIANXIN_HUNTER)) {
			entries = new HunterClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.QIANXIN_TI)) {

		}else if (engine.equals(SearchEngine.QUAKE_360)) {
			entries = new QuakeClient().SearchToGetEntry(searchContent);
		}else if (engine.equals(SearchEngine.TI_360)) {

		}else if (engine.equals(SearchEngine.HUNTER_IO)) {

		}
		return entries;
	}

	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}
