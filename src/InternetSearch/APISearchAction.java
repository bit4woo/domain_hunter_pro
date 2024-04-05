package InternetSearch;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.json.JSONArray;
import org.json.JSONObject;

import Tools.JSONHandler;
import burp.BurpExtender;
import burp.IPAddressUtils;
import config.ConfigPanel;
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
	String engine;

	private boolean autoAddToTarget;
	private boolean showInGUI;

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine,
			boolean autoAddToTarget, boolean showInGUI) {
		super();

		if (!lineModel.getClass().equals(LineTableModel.class) && !lineModel.getClass().equals(SearchTableModel.class)
				&& !lineModel.getClass().equals(TargetTableModel.class)) {
			BurpExtender.getCallbacks().printError("wrong AbstractTableModel object");
		}

		this.lineModel = lineModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engine = engine;
		putValue(Action.NAME, "Search On " + capitalizeFirstLetter(engine.trim()) + " API");
		this.autoAddToTarget = autoAddToTarget;
		this.showInGUI = showInGUI;
	}

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine) {
		this(lineModel, modelRows, columnIndex, engine, false, true);
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
					String searchContent = null;
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

					String resp_body = DoSearch(searchContent, engine);

					if (resp_body == null || resp_body.length() <= 0) {
						continue;
					}

					List<SearchResultEntry> entries = parseResp(resp_body, engine);
					if (showInGUI) {
						BurpExtender.getGui().getSearchPanel().addSearchTab(searchContent, entries);
					}

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
	
	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}
