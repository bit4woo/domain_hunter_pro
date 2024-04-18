package InternetSearch.Client;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import Tools.JSONHandler;
import config.ConfigManager;
import config.ConfigName;

public class HunterIoClient extends BaseClient {

	int limit =10;
	int pageSize = limit;

	@Override
	public String getEngineName() {
		return SearchEngine.HUNTER_IO;
	}

	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			JSONArray results = obj.getJSONObject("data").getJSONArray("emails");
			for (Object item : results) {
				JSONObject entryitem = (JSONObject) item;
				JSONArray sources = entryitem.getJSONArray("sources");
				String email = entryitem.getString("value");
				String position = entryitem.getString("position");

				for (Object source:sources){
					JSONObject sourceitem = (JSONObject) source;
					String url = sourceitem.getString("uri");
					SearchResultEntry entry = new SearchResultEntry();
					entry.setUri(url);
					entry.setRootDomain(email);
					entry.setSource(getEngineName());
					entry.setTitle(position);
					result.add(entry);
				}
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		printDebugInfo();
		return result;
	}

	//https://hunter.io/api-documentation/v2#domain-search
	@Override
	public boolean hasNextPage(String respbody,int currentPage) {
		// "size":83,"page":1,
		try {
			ArrayList<String> tmp_result = JSONHandler.grepValueFromJson(respbody, "results");
			if (tmp_result.size() >= 1) {
				int total = Integer.parseInt(tmp_result.get(0));
				if (total > currentPage * pageSize) {
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
		String key = ConfigManager.getStringConfigByKey(ConfigName.HunterIoAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("hunter.io API key not configurated!");
			return null;
		}
		int offset = pageSize *(page-1);
		String url = String.format(
				"https://api.hunter.io/v2/domain-search?domain=%s&offset=%s",searchContent,offset);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}

	public static void main(String[] args) throws IOException {
		String aaa = FileUtils.readFileToString(new File("G:/github/domain_hunter_pro/src/InternetSearch/Client/example_data_Hunter.io.txt"),"UTF-8");
		System.out.println(new HunterIoClient().parseResp(aaa));
	}
}
