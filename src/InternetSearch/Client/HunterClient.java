package InternetSearch.Client;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import Tools.JSONHandler;
import burp.BurpExtender;
import config.ConfigPanel;

public class HunterClient extends BaseClient {


	@Override
	public String getEngineName() {
		return SearchEngine.QIANXIN_HUNTER;
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
		try {
			ArrayList<String> result = JSONHandler.grepValueFromJson(respbody, "total");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * 100) {
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
		String key = ConfigPanel.textFieldHunterAPIKey.getText();
		String domainBase64 = new String(Base64.getEncoder().encode(searchContent.getBytes()));
		String url = String.format(
				"https://hunter.qianxin.com/openApi/search?&api-key=%s&search=%s&page=%s&page_size=100", key,
				domainBase64, page);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}

}
