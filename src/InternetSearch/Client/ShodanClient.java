package InternetSearch.Client;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import config.ConfigManager;
import config.ConfigName;

public class ShodanClient extends BaseClient {

	@Override
	public String getEngineName() {
		return SearchEngine.SHODAN;
	}

	//https://developer.shodan.io/api
	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			JSONArray results = obj.getJSONArray("matches");
			if (results != null) {
				for (Object item : results) {
					JSONObject entryitem = (JSONObject) item;
					SearchResultEntry entry = new SearchResultEntry();
					
					entry.setHost(entryitem.getJSONArray("hostnames").getString(0));
					entry.getIPSet().add(entryitem.getString("ip_str"));
					entry.setRootDomain(entryitem.getJSONArray("domains").getString(0));
					entry.setPort(entryitem.getInt("port"));
					
					entry.setASNInfo(entryitem.getString("asn"));
					if (entryitem.getJSONObject("http")!=null) {
						entry.setWebcontainer(entryitem.getJSONObject("http").getString("server"));
						entry.setWebcontainer(entryitem.getJSONObject("http").getString("title"));
					}
					entry.setProtocol(entryitem.getJSONObject("_shodan").getString("module"));

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
		// 使用“页面”访问第一页之后的结果。 对于第一页之后的每 100 个结果，将扣除 1 个查询积分。
		try {
			int size=100;
			ArrayList<String> result = JsonUtils.grepValueFromJson(respbody, "total");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * size) {//size=100
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
		String key = ConfigManager.getStringConfigByKey(ConfigName.ShodanAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("shodan key not configurated!");
			return null;
		}
		//curl -X GET "https://api.shodan.io/shodan/host/search?key=xxxxx&query=product:nginx&facets=country"
		String url = String.format(
				"https://api.shodan.io/shodan/host/search?key=%s&query=%s&page=%s",key,searchContent,page);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}

}
