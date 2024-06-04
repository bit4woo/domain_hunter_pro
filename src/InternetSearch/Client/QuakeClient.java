package InternetSearch.Client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import InternetSearch.SearchType;
import config.ConfigManager;
import config.ConfigName;

public class QuakeClient extends BaseClient {


	@Override
	public String getEngineName() {
		return SearchEngine.QUAKE_360;
	}

	/**
	 *
	 */
	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			//q5000 或者 0
			String code = obj.get("code")+"";
			if (code.equals("0")) {
				JSONArray results = obj.getJSONArray("data");
				for (Object item : results) {

					JSONObject entryitem = (JSONObject) item;

					SearchResultEntry entry = new SearchResultEntry();

					entry.getIPSet().add(entryitem.getString("ip"));
					entry.setRootDomain(entryitem.getString("domain"));

					int port = entryitem.getInt("port");
					entry.setPort(port);

					String serviceName = entryitem.getJSONObject("service").getString("name");
					String protocol;
					if (serviceName.equalsIgnoreCase("http/ssl")) {
						protocol = "https";
					}else if (serviceName.equalsIgnoreCase("http")){
						protocol = "http";
					}else {
						protocol = serviceName;
					}

					entry.setProtocol(protocol);

					if (serviceName.equalsIgnoreCase("http/ssl") || serviceName.equalsIgnoreCase("http")) {
						String host= entryitem.getJSONObject("service").getJSONObject("http").getString("host");
						String server = entryitem.getJSONObject("service").getJSONObject("http").getString("server");
						String title = entryitem.getJSONObject("service").getJSONObject("http").getString("title");

						entry.setHost(host);
						entry.setWebcontainer(server);
						entry.setTitle(title);
					}else {
						entry.setHost(entryitem.getString("domain"));
					}

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
		// "size":83,"page":1,
		try {
			int size = 500;
			ArrayList<String> result = JsonUtils.grepValueFromJson(respbody, "total");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(result.size()-1));
				//取最后一个值，因为返回数据包中这部分信息在末尾
				if (total > currentPage * size) {//size=500
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
		//https://quake.360.net/quake/#/help?id=5e774244cb9954d2f8a0165a&title=%E6%9C%8D%E5%8A%A1%E6%95%B0%E6%8D%AE%E6%8E%A5%E5%8F%A3
		return "https://quake.360.net/api/v3/search/quake_service";
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		String key = ConfigManager.getStringConfigByKey(ConfigName.Quake360APIKey);
		int size = 500;
		int start = size*(page-1);
		String body;
		String raw;

		if (searchContent.startsWith(SearchType.IconHash)) {
			searchContent = searchContent.substring(SearchType.IconHash.length());

			body = "{\"favicon_hash\": \"%s\",\"similar\": 0.9, \"start\": %s, \"size\": %s}";

			raw = "POST /api/v3/query/similar_icon/aggregation HTTP/1.1\r\n" 
					+ "Host: quake.360.net\r\n"
					+ "User-Agent: curl/7.81.0\r\n" 
					+ "Accept: */*\r\n" + "X-Quaketoken: %s\r\n"
					+ "Content-Type: application/json\r\n" 
					+ "Connection: close\r\n"
					+ "Content-Length: %s\r\n"
					+ "\r\n"
					+ "%s";


		}else {
			body = "{\"query\": \"%s\",\"start\": %s, \"size\": %s}";

			raw = "POST /api/v3/search/quake_service HTTP/1.1\r\n" 
					+ "Host: quake.360.net\r\n"
					+ "User-Agent: curl/7.81.0\r\n" 
					+ "Accept: */*\r\n" 
					+ "X-Quaketoken: %s\r\n"
					+ "Content-Type: application/json\r\n" 
					+ "Connection: close\r\n"
					+ "Content-Length: %s\r\n"
					+ "\r\n"
					+ "%s";
		}
		//必须包含Content-Length,否则服务端报错

		body = String.format(body,searchContent,start,size);
		raw = String.format(raw, key, body.length(),body);

		return raw.getBytes();
	}

}
