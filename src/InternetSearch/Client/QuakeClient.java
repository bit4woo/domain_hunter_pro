package InternetSearch.Client;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import Tools.JSONHandler;
import burp.BurpExtender;
import config.ConfigPanel;

public class QuakeClient extends BaseClient {


	@Override
	public String getEngineName() {
		return SearchEngine.QUAKE_360;
	}

	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			Boolean error = (Boolean) obj.get("error");
			if (!error) {
				JSONArray results = (JSONArray) obj.get("results");
				for (Object item : results) {
					JSONArray parts = (JSONArray) item;
					// host,ip,domain,port,protocol,server
					// ["www.xxx.com","11.11.11.11","xxx.com","80","http","nginx/1.20.1"]
					SearchResultEntry entry = new SearchResultEntry();
					entry.setHost(parts.getString(0));
					entry.getIPSet().add(parts.getString(1));
					entry.setRootDomain(parts.getString(2));
					entry.setPort(Integer.parseInt(parts.getString(3)));
					entry.setProtocol(parts.getString(4));
					entry.setWebcontainer(parts.getString(5));
					entry.setSource(getEngineName());
					result.add(entry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
		return result;
	}

	@Override
	public boolean hasNextPage(String respbody,int currentPage) {
		// "size":83,"page":1,
		try {
			ArrayList<String> result = JSONHandler.grepValueFromJson(respbody, "size");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * 2000) {//size=2000
					return true;
				} 
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
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
		String key = ConfigPanel.textFieldFofaKey.getText();
		String raw = "POST /api/v3/search/quake_service HTTP/1.1\r\n" + "Host: quake.360.net\r\n"
				+ "User-Agent: curl/7.81.0\r\n" + "Accept: */*\r\n" + "X-Quaketoken: %s\r\n"
				+ "Content-Type: application/json\r\n" + "Content-Length: 52\r\n" + "Connection: close\r\n" + "\r\n"
				+ "{\"query\": \"domain:%s\", \"start\": 0, \"size\": 500}";
		raw = String.format(raw, key, searchContent);
		return raw.getBytes();
	}

}
