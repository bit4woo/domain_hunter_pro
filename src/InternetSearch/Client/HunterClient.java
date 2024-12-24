package InternetSearch.Client;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import config.ConfigManager;
import config.ConfigName;

public class HunterClient extends BaseClient {

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
			if (code == 429) {
				//{"code":429,"data":null,"message":"请求太多啦，稍后再试试"}
				return result;
			}
			
			if (code == 200 || code == 40205) {
				JSONObject data = obj.getJSONObject("data");
				if (!data.get("arr").toString().equals("null")) {
					// "arr":null 这里有点反直觉
					JSONArray items = data.getJSONArray("arr");
					for (Object item : items) {
						JSONObject entryitem = (JSONObject) item;
						SearchResultEntry entry = new SearchResultEntry();

						try {
							String url = entryitem.getString("url");
							String ip = entryitem.getString("ip");
							if (StringUtils.isNotEmpty(url)) {
								entry.setHost(url);
							} else {
								entry.setHost(ip);
							}

							entry.getIPSet().add(ip);
							entry.setRootDomain(entryitem.getString("domain"));
							entry.setPort(entryitem.getInt("port"));
							entry.setProtocol(entryitem.getString("protocol"));

							String component = entryitem.get("component").toString();
							try {
								ArrayList<String> names = JsonUtils.grepValueFromJson(component, "name");
								entry.setWebcontainer(String.join(",", names));
							} catch (JSONException e) {
								entry.setWebcontainer(component);
							}
							entry.setTitle(entryitem.getString("web_title"));
							entry.setASNInfo(entryitem.getString("as_org"));
							entry.setSource(getEngineName());
							result.add(entry);
						} catch (Exception e) {
							e.printStackTrace(stderr);
							stderr.println(entryitem.toString());
						}
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
		String key = ConfigManager.getStringConfigByKey(ConfigName.QianxinHunterAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println(ConfigName.QianxinHunterAPIKey+" not configurated!");
			return null;
		}
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

	public static void main(String[] args) {
		String aaa = "{\"code\":200,\"data\":{\"account_type\":\"个人账号\",\"total\":0,\"time\":331,\"arr\":null,\"consume_quota\":\"消耗积分：1\",\"rest_quota\":\"今日剩余积分：5176\",\"syntax_prompt\":\"\"},\"message\":\"success\"}";
		System.out.println(new HunterClient().parseResp(aaa));
	}
}
