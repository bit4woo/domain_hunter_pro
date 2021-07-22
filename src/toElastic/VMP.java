package toElastic;

import java.util.Collection;
import java.util.HashMap;

import com.github.kevinsawicki.http.HttpRequest;

import burp.BurpExtender;
import burp.Commons;
import burp.HelperPlus;
import title.LineEntry;
import title.TitlePanel;

public class VMP {

	/**
	 * upload all vmp entry
	 * @return
	 */
	public static Collection<VMPEntry> getAllVmpEntry() {
		HashMap<String,VMPEntry> result = new HashMap<String,VMPEntry>();//使用hashmap实现去重

		Collection<LineEntry> titleEntries = TitlePanel.getTitleTableModel().getLineEntries().values();
		for (LineEntry entry:titleEntries) {
			String url = entry.getUrl();
			String host = entry.getHost();
			String title = entry.getTitle();

			if (!Commons.isValidIP(host)) {
				VMPEntry tmp = new VMPEntry(url,title);
				result.put(url,tmp);
				//tmp = new VMPEntry(host,title);
				//result.put(host,tmp);
			}
		}
		return result.values();
	}

	public static boolean uploadAllVMPEntries(String ApiUrl,HashMap<String, String> headers) {
		if (!isApiOk(ApiUrl,headers)) {
			System.out.println("API test failed,please check your config");
			return false;
		}
		
		Collection<VMPEntry> entries = getAllVmpEntry();
		for(VMPEntry entry:entries) {
			boolean succ = upload(ApiUrl,headers,entry.toJson());
		}
		return true;
	}
	
	public static boolean upload(String ApiUrl,HashMap<String, String> header,String jsonData) {
		HttpRequest request = HttpRequest.post(ApiUrl);
		//request.useProxy("localhost", 8080);//注意burp启用HTTP2的功能，会影响返回包的解析
		request.trustAllCerts();
		request.headers(header);
		
		request = request.send(jsonData);
		String body = "";
		request.body(body);
		int code = request.code();
		//System.out.print(body);
		//System.out.print(code);
		if (code == 201 || code == 200) {
			System.out.println(jsonData+"   "+true);
			return true;
		}else {
			System.out.println(jsonData+"   code:"+code+" body:"+body);
			return false;
		}
	}
	
	public static boolean isApiOk(String ApiUrl,HashMap<String, String> headers) {
		VMPEntry entry = new VMPEntry("shopee.com","shopee offical website");
		return upload(ApiUrl,headers,entry.toJson());
	}
	
	public static void main(String[] args) {
		String url = "https://vmp.test.shopee.io/api/v2/asset/";
		HashMap<String, String> headers = new HashMap<String,String>();
		headers.put("Authorization", "Token 87ecb070d5742acc25ad9bd2dd9a80d1bde9d090");
		headers.put("Content-Type","application/json;charset=UTF-8");
		System.out.println(isApiOk(url,headers));
	}
}
