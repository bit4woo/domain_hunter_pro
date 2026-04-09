package InternetSearch.Client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.HttpClientOfBurp;
import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import config.ConfigManager;
import config.ConfigName;

/**
curl -X POST https://0.zone/api/data/ \
-H "Content-Type: application/json" \
-d '{"query":"零零信安", "query_type":"site", "page":1, "pagesize":10, "zone_key_id":"YOUR_API_KEY"}'

{"code":1,"message":"该 API Key 不合法或不存在"}

 */
public class ZeroZoneClient extends BaseClient {

	@Override
	public String getEngineName() {
		return SearchEngine.ZERO_ZONE;
	}

	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			int code = obj.getInt("code");
			if (code ==0 ) {
				JSONArray results = obj.getJSONArray("data");
				for (Object item : results) {
					JSONObject entryitem = (JSONObject) item;

					SearchResultEntry entry = new SearchResultEntry();

					try {
						String url = entryitem.getString("url");
						String ip = entryitem.getString("ip");
						String hostname = entryitem.getString("hostname");
						if (StringUtils.isNotEmpty(url)) {
							entry.setHost(url);
						} else {
							entry.setHost(hostname);
						}

						entry.getIPSet().add(ip);
						entry.setRootDomain(entryitem.getString("domain"));
						entry.setPort(entryitem.getInt("port"));
						entry.setProtocol(entryitem.getString("service"));
						entry.setWebcontainer(entryitem.getString("server_name"));
						
						entry.setTitle(entryitem.getString("title"));
						entry.setASNInfo(entryitem.getString("as_org"));
						entry.setSource(getEngineName());
						result.add(entry);
					} catch (Exception e) {
						e.printStackTrace(stderr);
						stderr.println(entryitem.toString());
					}
					
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
	public boolean hasNextPage(String respbody, int currentPage) {
		int maxTotal = 10000;
		try {
			ArrayList<String> result = JsonUtils.grepValueFromJson(respbody, "next");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total >= maxTotal) {
					total = maxTotal;
				}
				if (total > currentPage * 1000) {// size=2000
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		return false;
	}

	/*
	 * https://en.fofa.info/api
	 */
	@Override
	public String buildSearchUrl(String searchContent, int page) {
		String key = ConfigManager.getStringConfigByKey(ConfigName.ZeroZoneAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("0.zone api key not configurated!");
			return null;
		}
		return "https://0.zone/api/data/";
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		URL url;
		try {
			url = new URL("https://0.zone/api/data/");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		
	    List<String> headers = new ArrayList<>();
	    headers.add("Content-Type: application/json");
	    
		String key = ConfigManager.getStringConfigByKey(ConfigName.ZeroZoneAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("0.zone api key not configurated!");
			return null;
		}
		
		String body = String.format(
				"{\"query\":\"%s\", \"query_type\":\"site\", \"page\":%s, \"pagesize\":1000, \"zone_key_id\":\"%s\"}",
				searchContent,page,key);
		
		byte[] raw = HttpClientOfBurp.buildHttpRequest(url,headers,"POST",body);
		
		return raw;
	}
	
	public static void main(String[] args) {
		List<SearchResultEntry> result = new ZeroZoneClient().parseResp("");
		System.out.println(result.size());
	}

}
