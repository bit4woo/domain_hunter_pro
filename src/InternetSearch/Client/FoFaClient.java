package InternetSearch.Client;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
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

					try {
						entry.setHost(parts.getString(0));
						entry.getIPSet().add(parts.getString(1));
						entry.setRootDomain(parts.getString(2));
						entry.setPort(Integer.parseInt(parts.getString(3)));
						entry.setProtocol(parts.getString(4));
						entry.setWebcontainer(parts.getString(5));
						entry.setTitle(parts.getString(6));
						try {
							entry.setAsnNum(Integer.parseInt(parts.getString(7)));
						} catch (Exception e) {

						}
						entry.setASNInfo(parts.getString(8));
						entry.setSource(getEngineName());
						result.add(entry);
					} catch (Exception e) {
						e.printStackTrace(stderr);
						stderr.println(parts.toString());
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
		// "size":83,"page":1,
		try {
			ArrayList<String> result = JsonUtils.grepValueFromJson(respbody, "size");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * 2000) {// size=2000
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
		String email = ConfigManager.getStringConfigByKey(ConfigName.FofaEmail);
		String key = ConfigManager.getStringConfigByKey(ConfigName.FofaKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("fofa.info emaill or key not configurated!");
			return null;
		}
		searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));
		// [820001] 没有权限搜索icon_hash字段
		String url = String.format(
				"https://fofa.info/api/v1/search/all?email=%s&key=%s&page=1&size=2000&fields=host,ip,domain,port,protocol,server,title,as_number,as_organization&qbase64=%s",
				email, key, searchContent);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}

}
