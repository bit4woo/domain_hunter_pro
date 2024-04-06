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

public class ZoomEyeClient extends BaseClient {

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
		String email = ConfigPanel.textFieldFofaEmail.getText();
		String key = ConfigPanel.textFieldFofaKey.getText();
		if (email.equals("") || key.equals("")) {
			BurpExtender.getStderr().println("fofa.info emaill or key not configurated!");
			return null;
		}
		//https://www.zoomeye.hk/api/domain/search?q=google.com&p=1&s=10&type=1
		//https://www.zoomeye.hk/api/search?q=site%3A%22baidu.com%22&page=1
		String url = String.format(
				"https://api.zoomeye.hk/api/search?q=%s&page=%s",searchContent,page);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		//site:"baidu.com"
		String raw = "GET /api/search?q=%s&page=%s HTTP/1.1\r\n"
				+ "Host: www.zoomeye.hk\r\n"
				+ "Accept: application/json, text/plain, */*\r\n"
				+ "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36\r\n"
				+ "Accept-Encoding: gzip, deflate\r\n"
				+ "Accept-Language: zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7\r\n"
				+ "API-KEY: %s\r\n"
				+ "Connection: close\r\n"
				+ "\r\n"
				+ "";
		
		searchContent = URLEncoder.encode(searchContent);
		String key = ConfigPanel.textFieldQuakeAPIKey.getText();
		int size = 500;
		int start = size*(page-1); 
		raw = String.format(raw,searchContent,page,key);
		return raw.getBytes();
		return null;
	}

}
