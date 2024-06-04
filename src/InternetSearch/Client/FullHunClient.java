package InternetSearch.Client;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import config.ConfigManager;
import config.ConfigName;

public class FullHunClient extends BaseClient {


	@Override
	public String getEngineName() {
		return SearchEngine.QIANXIN_HUNTER;
	}

	/**
	 */
	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			int code = obj.getInt("code");
			if (code ==200) {
				JSONObject data = obj.getJSONObject("data");
				if (!data.get("arr").toString().equals("null")) {
					//"arr":null 这里有点反直觉
					JSONArray items = data.getJSONArray("arr");
					for (Object item : items) {
						JSONObject entryitem = (JSONObject) item;
						SearchResultEntry entry = new SearchResultEntry();
						entry.setHost(entryitem.getString("url"));
						entry.getIPSet().add(entryitem.getString("ip"));
						entry.setRootDomain(entryitem.getString("domain"));
						entry.setPort(entryitem.getInt("port"));
						entry.setProtocol(entryitem.getString("protocol"));
						entry.setWebcontainer(entryitem.get("component").toString());
						entry.setTitle(entryitem.getString("web_title"));
						entry.setSource(getEngineName());
						result.add(entry);
					}
					return result;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		printDebugInfo();
		return result;
	}

	@Override
	public boolean hasNextPage(String respbody,int currentPage) {
		try {
			ArrayList<String> result = JsonUtils.grepValueFromJson(respbody, "total");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * 100) {
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
		//curl "https://fullhunt.io/api/v1/domain/kaspersky.com/details" -H "X-API-KEY: xxxx-xxxx-xxxx-xxxxxx"
		String key = ConfigManager.getStringConfigByKey(ConfigName.QianxinHunterAPIKey);
		String url = String.format(
				"https://fullhunt.io/api/v1/domain/%s/details", key,
				searchContent, page);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}

	public static void main(String[] args) {
		String aaa = "{\"code\":200,\"data\":{\"account_type\":\"个人账号\",\"total\":0,\"time\":331,\"arr\":null,\"consume_quota\":\"消耗积分：1\",\"rest_quota\":\"今日剩余积分：5176\",\"syntax_prompt\":\"\"},\"message\":\"success\"}";
		System.out.println(new FullHunClient().parseResp(aaa));
	}
}
