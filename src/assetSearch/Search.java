package assetSearch;

import java.net.MalformedURLException;
import java.net.URL;

public class Search {
	public static String searchFofa(String email,String key,String domainBase64){
		try {
			String url= String.format("https://fofa.info/api/v1/search/all?email=%s&key=%s&page=1&size=1000&fields=host,ip&qbase64=%s",
					email,key,domainBase64);
			String body = HttpClientOfBurp.doRequest(new URL(url));
			
			return body;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}
	
//	TODO
	public static String searchHunter(String key,String domainBase64){
		try {
			String url= String.format("https://hunter.qianxin.com/openApi/search?&api-key=%s&search=%s&page=1&page_size=1000&is_web=1&start_time=\"2000-01-01 00:00:00\"&end_time=\"2030-03-01 00:00:00\"",
					key,domainBase64);
			String body = HttpClientOfBurp.doRequest(new URL(url));
			
			return body;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}
	
//	TODO
	public static String searchTi(String key,String domainBase64){
		try {
			String url= String.format("https://hunter.qianxin.com/openApi/search?&api-key=%s&search=%s&page=1&page_size=1000&is_web=2&start_time=\"2000-01-01 00:00:00\"&end_time=\"2030-03-01 00:00:00\"",
					key,domainBase64);
			String body = HttpClientOfBurp.doRequest(new URL(url));
			
			return body;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	
	//curl -X POST -H "X-QuakeToken: XXXXXX" -H "Content-Type: application/json" https://quake.360.net/api/v3/search/quake_service -d '{"query": "domain: baidu.com", "start": 0, "size": 500}' --proxy http://127.0.0.1:8080

	public static String searchQuake(String key,String domain){
		try {
			String url = "https://quake.360.net/api/v3/search/quake_service";
			String raw = "POST /api/v3/search/quake_service HTTP/1.1\r\n"
					+ "Host: quake.360.net\r\n"
					+ "User-Agent: curl/7.81.0\r\n"
					+ "Accept: */*\r\n"
					+ "X-Quaketoken: %s\r\n"
					+ "Content-Type: application/json\r\n"
					+ "Content-Length: 52\r\n"
					+ "Connection: close\r\n"
					+ "\r\n"
					+ "{\"query\": \"domain:%s\", \"start\": 0, \"size\": 500}";
			raw = String.format(raw, key, domain);
			String respbody = HttpClientOfBurp.doRequest(new URL(url),raw.getBytes());
			
			return respbody;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}
}
