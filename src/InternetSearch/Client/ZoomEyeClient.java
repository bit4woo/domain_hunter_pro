package InternetSearch.Client;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.bit4woo.utilbox.utils.JsonUtils;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import config.ConfigManager;
import config.ConfigName;

public class ZoomEyeClient extends BaseClient {

	@Override
	public String getEngineName() {
		return SearchEngine.ZOOMEYE;
	}

	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			int status = obj.getInt("status");
			if (status ==200) {
				JSONArray results = obj.getJSONArray("matches");
				for (Object item : results) {

					JSONObject entryitem = (JSONObject) item;
					Set<String> ipSet = getIPSet(entryitem);
					SearchResultEntry entry = new SearchResultEntry();

					entry.getIPSet().addAll(ipSet);

					try {
						entry.setHost(entryitem.getString("rdns"));
					} catch (Exception e) {
						entry.setHost((String)(ipSet.toArray())[0]);
					}

					int port = entryitem.getJSONObject("portinfo").getInt("port");
					entry.setPort(port);

					String serviceName = entryitem.getJSONObject("portinfo").getString("service");
					String title = entryitem.getJSONObject("portinfo").get("title").toString();

					entry.setProtocol(serviceName);
					entry.setTitle(title);

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

	public static Set<String> getIPSet(JSONObject entryitem){
		Set<String> result = new HashSet<String>();

		try {
			//title:xxx 获得的是IP string
			result.add(entryitem.getString("ip"));
		}catch(Exception e) {
			//site:xxx.com 获得的是IP List
			JSONArray ipList = entryitem.getJSONArray("ip");
			for (int i = 0; i < ipList.length(); i++) {
				String element = ipList.getString(i);
				result.add(element);
			}
		}
		return result;
	}

	@Override
	public boolean hasNextPage(String respbody,int currentPage) {
		// "size":83,"page":1,
		try {
			int pageSize = 10;
			ArrayList<String> tmp_result = JsonUtils.grepValueFromJson(respbody, "total");
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
		String key = ConfigManager.getStringConfigByKey(ConfigName.ZoomEyeAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("zoomeye key not configurated!");
			return null;
		}
		//https://www.zoomeye.hk/api/domain/search?q=google.com&p=1&s=10&type=1
		//https://www.zoomeye.hk/api/search?q=site%3A%22baidu.com%22&page=1
		String url = String.format(
				"https://www.zoomeye.hk/api/search?q=%s&page=%s",searchContent,page);
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
		String key = ConfigManager.getStringConfigByKey(ConfigName.ZoomEyeAPIKey);
		if (StringUtils.isEmpty(key)) {
			stderr.println("zoomeye key not configurated!");
			return null;
		}
		int size = 500;
		int start = size*(page-1); 
		raw = String.format(raw,searchContent,page,key);
		return raw.getBytes();
	}

	public static void main(String[] args) throws IOException {
		String aaa = FileUtils.readFileToString(new File("G:/github/domain_hunter_pro/src/InternetSearch/Client/example_data_ZoomEye.txt"),"UTF-8");
		System.out.println(new ZoomEyeClient().parseResp(aaa));
	}
}
