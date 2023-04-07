package assetSearch;

import java.net.MalformedURLException;
import java.net.URL;

public class Fofa {
	public static String doSearch(String email,String key,String domainBase64){
		try {
			String url= String.format("https://fofa.info/api/v1/search/all?email=%s&key=%s&page=1&size=1000&fields=host,ip,domain,port,protocol,server&qbase64=%s",
					email,key,domainBase64);
			String body = HttpClientOfBurp.doRequestGetBody(new URL(url));
			
			return body;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}
}
