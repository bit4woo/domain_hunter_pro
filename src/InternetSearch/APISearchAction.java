package InternetSearch;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import Tools.JSONHandler;
import burp.BurpExtender;
import config.ConfigPanel;
import domain.target.TargetTableModel;
import title.LineTableModel;



public class APISearchAction extends AbstractAction{

	/**
	 */
	private static final long serialVersionUID = 1933197856582351336L;


	AbstractTableModel lineModel;
	int[] modelRows;
	int columnIndex;
	String engine;

	public APISearchAction(AbstractTableModel lineModel, int[] modelRows, int columnIndex, String engine) {
		super();

		if (!lineModel.getClass().equals(LineTableModel.class) && 
				!lineModel.getClass().equals(SearchTableModel.class) &&
				!lineModel.getClass().equals(TargetTableModel.class)) {
			BurpExtender.getCallbacks().printError("wrong AbstractTableModel object");
		}

		this.lineModel = lineModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engine = engine;
		putValue(Action.NAME, "Search On "+capitalizeFirstLetter(engine.trim())+" API");
	}


	@Override
	public final void actionPerformed(ActionEvent e) {
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			@Override
			protected Map doInBackground() throws Exception {

				if (modelRows.length >=50) {
					BurpExtender.getStderr().print("too many items selected!! should less than 50");
					return null;
				}
				for (int row:modelRows) {
					String searchContent =null;
					if (lineModel.getClass().equals(LineTableModel.class)) {
						searchContent = ((LineTableModel)lineModel).getValueForSearch(row,columnIndex,engine);
					}

					if (lineModel.getClass().equals(SearchTableModel.class)) {
						searchContent = ((SearchTableModel)lineModel).getValueForSearch(row,columnIndex,engine);
					}

					if (lineModel.getClass().equals(TargetTableModel.class)) {
						searchContent = ((TargetTableModel)lineModel).getValueForSearch(row,columnIndex,engine);
					}

					if (searchContent != null) {
						if (engine.equalsIgnoreCase("hunter")) {
							String queryResult = searchHunter(searchContent);
						}
						else {
							String url = buildSearchUrl(engine,searchContent,-1);
							byte[] raw = buildRawData(engine,searchContent);

							try {
								String resp_body = HttpClientOfBurp.doRequest(new URL(url),raw);
//								TODO parse body and create new table
							} catch (Exception err) {
								err.printStackTrace(BurpExtender.getStderr());
							}
						}

					}
				}
				return null;
			}

			@Override
			protected void done(){
			}
		};
		worker.execute();
	}

	public String searchHunter(String searchContent) {
		String queryResult = "";
		int page =1;
		while(true){
			try {
				String url = buildSearchUrl("hunter",searchContent,page);
				String body = HttpClientOfBurp.doRequest(new URL(url));
				if (!body.contains("\"code\":200,")) {
					BurpExtender.getStderr().print(body);
					break;
				}else {
					queryResult = queryResult+"  "+body;
					ArrayList<String> result = JSONHandler.grepValueFromJson(body, "total");
					if (result.size() >=1) {
						int total = Integer.parseInt(result.get(0));
						if (total> page*100) {
							page++;
							continue;
						}else {
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return queryResult;
	}


	public String buildSearchUrl(String engine,String searchContent,int page) {
		searchContent = URLEncoder.encode(searchContent);
		String url = null;
		if (engine.equalsIgnoreCase("google")) {
			url= "https://www.google.com/search?q="+searchContent;
		}
		else if (engine.equalsIgnoreCase("Github")) {
			url= "https://github.com/search?q=%22"+searchContent+"%22&type=Code";
		}
		else if (engine.equalsIgnoreCase("fofa")) {

			String email = ConfigPanel.textFieldFofaEmail.getText();
			String key = ConfigPanel.textFieldFofaKey.getText();
			if (email.equals("") ||key.equals("")) {
				BurpExtender.getStderr().println("fofa.info emaill or key not configurated!");
				return null;
			}
			searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));

			url= String.format("https://fofa.info/api/v1/search/all?email=%s&key=%s&page=1&size=2000&fields=host,ip&qbase64=%s",
					email,key,searchContent);
		}
		else if (engine.equalsIgnoreCase("shodan")) {
			url= "https://www.shodan.io/search?query="+searchContent;
		}
		else if (engine.equalsIgnoreCase("360Quake")) {
			url= "https://quake.360.net/quake/#/searchResult?searchVal="+searchContent;
		}
		else if (engine.equalsIgnoreCase("ZoomEye")) {
			url= "https://www.zoomeye.org/searchResult?q="+searchContent;
		}
		else if (engine.equalsIgnoreCase("ti.360.net")) {
		}
		else if (engine.equalsIgnoreCase("hunter")) {

			if (page>0) {
				String key = ConfigPanel.textFieldHunterAPIKey.getText();
				String domainBase64 = new String(Base64.getEncoder().encode(searchContent.getBytes()));
				url= String.format("https://hunter.qianxin.com/openApi/search?&api-key=%s&search=%s&page=%s&page_size=100",
						key,domainBase64,page);
			}
		}
		return url;
	}


	public byte[] buildRawData(String engine,String searchContent) {
		searchContent = URLEncoder.encode(searchContent);
		if (engine.equalsIgnoreCase("360Quake")) {
			String key = ConfigPanel.textFieldFofaKey.getText();
			String raw = "POST /api/v3/search/quake_service HTTP/1.1\r\n"
					+ "Host: quake.360.net\r\n"
					+ "User-Agent: curl/7.81.0\r\n"
					+ "Accept: */*\r\n"
					+ "X-Quaketoken: %s\r\n"
					+ "Content-Type: application/json\r\n"
					+ "Content-Length: 52\r\n"
					+ "Connection: close\r\n"
					+ "\r\n"
					+ "{\"query\": \"domain:%s\", \"start\": 0, \"size\": 500}";
			raw = String.format(raw, key, searchContent);
			return raw.getBytes();
		}
		return null;
	}

	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}
