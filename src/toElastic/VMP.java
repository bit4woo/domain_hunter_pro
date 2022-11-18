package toElastic;

import java.util.Collection;
import java.util.HashMap;

import com.github.kevinsawicki.http.HttpRequest;

import GUI.GUIMain;
import burp.IPAddressUtils;
import title.LineEntry;

public class VMP {

	GUIMain guiMain;
	/**
	 * upload all vmp entry
	 * @return
	 */
	
	public VMP(GUIMain guiMain) {
		this.guiMain = guiMain;
	}
	
	public Collection<VMPEntry> getAllVmpEntry() {
		HashMap<String,VMPEntry> result = new HashMap<String,VMPEntry>();//使用hashmap实现去重

		Collection<LineEntry> titleEntries = guiMain.getTitlePanel().getTitleTable().getLineTableModel().getLineEntries().values();
		for (LineEntry entry:titleEntries) {
			String url = entry.getUrl();
			String host = entry.getHost();
			String title = entry.getTitle();
			String IPStr = entry.getIPSet().toString();
			String header = entry.getHeaderValueOf(false, "Server");
			if (!IPAddressUtils.isValidIP(host)) {
				VMPEntry tmp = new VMPEntry(url,title,IPStr,header);
				result.put(url,tmp);//去重
			}
		}
		return result.values();
	}

	public boolean uploadAllVMPEntries(String ApiUrl,HashMap<String, String> headers) {
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

	}
}
