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

					if (searchContent == null ||  searchContent.equals("")) {
						BurpExtender.getStderr().print("nothing to search...");
						return null;
					}

					String resp_body = DoSearch(searchContent,engine);
					if (resp_body != null && resp_body.length()>0){
						List<SearchResultEntry> entries = parseResp(resp_body,engine);

						BurpExtender.getGui().addSearchPanel(entries);
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

	public String DoSearch(String searchContent,String engine) {
		if (searchContent == null || searchContent.equals("")){
			return "";
		}
		if (engine.equalsIgnoreCase("hunter")) {
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
		}else {
			String url = buildSearchUrl(engine,searchContent,-1);
			byte[] raw = buildRawData(engine,searchContent);

			try {
				String resp_body = HttpClientOfBurp.doRequest(new URL(url),raw);
				return resp_body;
			} catch (Exception err) {
				err.printStackTrace(BurpExtender.getStderr());
				return null;
			}
		}
	}

	public static List<SearchResultEntry> parseResp(String respbody,String engine) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		if (engine.equalsIgnoreCase("fofa")) {
			try {
				JSONObject obj = new JSONObject(respbody);
				Boolean error = (Boolean)obj.get("error");
				if (!error) {
					JSONArray results = (JSONArray) obj.get("results");
					for(Object item:results) {
						JSONArray parts = (JSONArray)item; 
						//host,ip,domain,port,protocol,server
						//["www.xxx.com","11.11.11.11","xxx.com","80","http","nginx/1.20.1"]
						SearchResultEntry entry = new SearchResultEntry();
						entry.setHost(parts.getString(0));
						entry.getIPSet().add(parts.getString(1));
						entry.setRootDomain(parts.getString(2));
						entry.setPort(Integer.parseInt(parts.getString(3)));
						entry.setProtocol(parts.getString(4));
						entry.setWebcontainer(parts.getString(5));
						result.add(entry);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(BurpExtender.getStderr());
			}
		}

		return result;
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

			url= String.format("https://fofa.info/api/v1/search/all?email=%s&key=%s&page=1&size=2000&fields=host,ip,domain,port,protocol,server&qbase64=%s",
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
	
	public static void main(String[] args) {
		String aaa = "{\"error\":false,\"consumed_fpoint\":0,\"required_fpoints\":0,\"size\":457,\"page\":1,\"mode\":\"extended\",\"query\":\"icon_hash=\\\"-1551025814\\\"\",\"results\":[[\"lab-paas-dev.utest.21kunpeng.com\",\"119.29.46.72\",\"21kunpeng.com\",\"80\",\"http\",\"APISIX/3.0.0\"],[\"https://lab-paas-dev.utest.21kunpeng.com\",\"119.29.46.72\",\"21kunpeng.com\",\"443\",\"https\",\"APISIX/3.0.0\"],[\"mobility-analytics.smart-mobility.alstom.com\",\"52.138.155.206\",\"alstom.com\",\"80\",\"http\",\"\"],[\"https://rancher.collab.infraview.net\",\"185.207.63.122\",\"infraview.net\",\"443\",\"https\",\"\"],[\"44.204.19.4:8080\",\"44.204.19.4\",\"\",\"8080\",\"http\",\"\"],[\"https://121.40.65.104\",\"121.40.65.104\",\"\",\"443\",\"https\",\"nginx\"],[\"149.28.209.94:8080\",\"149.28.209.94\",\"\",\"8080\",\"http\",\"\"],[\"https://rancher.master.k3s.getvisibility.com\",\"172.67.26.114\",\"getvisibility.com\",\"443\",\"https\",\"cloudflare\"],[\"rancher.gvdevelopment.k3s.getvisibility.com\",\"172.67.26.114\",\"getvisibility.com\",\"80\",\"http\",\"cloudflare\"],[\"rancher.master.k3s.getvisibility.com\",\"104.22.59.129\",\"getvisibility.com\",\"80\",\"http\",\"cloudflare\"],[\"https://rancher.gvdevelopment.k3s.getvisibility.com\",\"104.22.59.129\",\"getvisibility.com\",\"443\",\"https\",\"cloudflare\"],[\"39.96.65.93:8080\",\"39.96.65.93\",\"\",\"8080\",\"http\",\"\"],[\"121.199.161.48:8080\",\"121.199.161.48\",\"\",\"8080\",\"http\",\"\"],[\"120.27.16.243:8080\",\"120.27.16.243\",\"\",\"8080\",\"http\",\"\"],[\"https://appvzt.com.br\",\"167.172.13.62\",\"appvzt.com.br\",\"443\",\"https\",\"\"],[\"139.155.140.158:8080\",\"139.155.140.158\",\"\",\"8080\",\"http\",\"\"],[\"8.130.102.96:8080\",\"8.130.102.96\",\"\",\"8080\",\"http\",\"\"],[\"114.116.241.40:8089\",\"114.116.241.40\",\"\",\"8089\",\"http\",\"\"],[\"13.251.126.163:8080\",\"13.251.126.163\",\"\",\"8080\",\"http\",\"\"],[\"8.134.76.220:8080\",\"8.134.76.220\",\"\",\"8080\",\"http\",\"\"],[\"58.87.71.252:8080\",\"58.87.71.252\",\"\",\"8080\",\"http\",\"\"],[\"https://harvester.dl-labs.ai\",\"16.1.32.148\",\"dl-labs.ai\",\"443\",\"https\",\"nginx/1.18.0 (Ubuntu)\"],[\"https://20.4.127.238\",\"20.4.127.238\",\"\",\"443\",\"https\",\"\"]]}";
		List<SearchResultEntry> bbb = parseResp(aaa,"fofa");
		for (SearchResultEntry item:bbb) {
			System.out.println(item);
		}
	}
}
