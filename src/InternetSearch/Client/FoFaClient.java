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
import config.ConfigManager;
import config.ConfigName;

public class FoFaClient extends BaseClient {

	@Override
	public String getEngineName() {
		return SearchEngine.FOFA;
	}

	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			Boolean error = (Boolean) obj.get("error");
			if (!error) {
				JSONArray results = obj.getJSONArray("results");
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
			}else {
				BurpExtender.getStderr().println(respbody.substring(0,200));
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			BurpExtender.getStderr().println(respbody.substring(0,200));
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
		String email = ConfigManager.getStringConfigByKey(ConfigName.FofaEmail);
		String key = ConfigManager.getStringConfigByKey(ConfigName.FofaKey);
		if (email.equals("") || key.equals("")) {
			BurpExtender.getStderr().println("fofa.info emaill or key not configurated!");
			return null;
		}
		searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));

		String url = String.format(
				"https://fofa.info/api/v1/search/all?email=%s&key=%s&page=1&size=2000&fields=host,ip,domain,port,protocol,server&qbase64=%s",
				email, key, searchContent);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}

}
