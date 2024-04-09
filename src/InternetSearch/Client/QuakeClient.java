package InternetSearch.Client;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import Tools.JSONHandler;
import config.ConfigManager;
import config.ConfigName;

public class QuakeClient extends BaseClient {


	@Override
	public String getEngineName() {
		return SearchEngine.QUAKE_360;
	}

	/**
	 *
	 */
	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			//q5000 或者 0
			String code = obj.get("code")+"";
			if (code.equals("0")) {
				JSONArray results = obj.getJSONArray("data");
				for (Object item : results) {

					JSONObject entryitem = (JSONObject) item;

					SearchResultEntry entry = new SearchResultEntry();


					entry.getIPSet().add(entryitem.getString("ip"));
					entry.setRootDomain(entryitem.getString("domain"));

					int port = entryitem.getInt("port");
					entry.setPort(port);

					String serviceName = entryitem.getJSONObject("service").getString("name");
					String protocol;
					if (serviceName.equalsIgnoreCase("http/ssl")) {
						protocol = "https";
					}else if (serviceName.equalsIgnoreCase("http")){
						protocol = "http";
					}else {
						protocol = serviceName;
					}

					entry.setProtocol(protocol);

					if (serviceName.equalsIgnoreCase("http/ssl") || serviceName.equalsIgnoreCase("http")) {
						String host= entryitem.getJSONObject("service").getJSONObject("http").getString("host");
						String server = entryitem.getJSONObject("service").getJSONObject("http").getString("server");
						String title = entryitem.getJSONObject("service").getJSONObject("http").getString("title");

						entry.setHost(host);
						entry.setWebcontainer(server);
						entry.setTitle(title);
					}else {
						entry.setHost(entryitem.getString("domain"));
					}

					entry.setSource(getEngineName());
					result.add(entry);
				}
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		printDebugInfo();
		return result;
	}

	@Override
	public boolean hasNextPage(String respbody,int currentPage) {
		// "size":83,"page":1,
		try {
			int size = 500;
			ArrayList<String> result = JSONHandler.grepValueFromJson(respbody, "total");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * size) {//size=500
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		return false;
	}

	@Override
	public String buildSearchUrl(String searchContent, int page) {
		return "https://quake.360.net/api/v3/search/quake_service";
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		searchContent = URLEncoder.encode(searchContent);
		String key = ConfigManager.getStringConfigByKey(ConfigName.Quake360APIKey);
		int size = 500;
		int start = size*(page-1); 
		String raw = "POST /api/v3/search/quake_service HTTP/1.1\r\n" + "Host: quake.360.net\r\n"
				+ "User-Agent: curl/7.81.0\r\n" + "Accept: */*\r\n" + "X-Quaketoken: %s\r\n"
				+ "Content-Type: application/json\r\n" + "Connection: close\r\n" + "\r\n"
				+ "{\"query\": \"domain:%s\", \"start\": %s, \"size\": %s}";
		raw = String.format(raw, key, searchContent,start,size);
		return raw.getBytes();
	}

}
