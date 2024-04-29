package InternetSearch;

import java.net.URL;
import java.util.Date;

import com.bit4woo.utilbox.burp.HelperPlus;

import base.Commons;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import config.ConfigManager;
import config.ConfigName;
import title.LineEntry;


public class HttpClientOfBurp {
	
	
	public static IHttpService getHttpService(URL url) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();

		int port = url.getPort();
		if ( port ==-1) {
			if (url.getProtocol().equalsIgnoreCase("http")) {
				port = 80;
			}
			if (url.getProtocol().equalsIgnoreCase("https")) {
				port = 443;
			}
		}
		IHttpService service =helpers.buildHttpService(url.getHost(),port,url.getProtocol());
		return service;
	}

	public static String doRequest(URL url) {
		return doRequest(url,null);
	}

	/**
	 * 
	 * @param url
	 * @param byteRequest
	 * @return response body
	 */
	public static String doRequest(URL url,byte[] byteRequest) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();
		if (byteRequest == null) {
			byteRequest = helpers.buildHttpRequest(url);//GET
		}
		

		IHttpService service =getHttpService(url);
		IHttpRequestResponse message = callbacks.makeHttpRequest(service, byteRequest);

		HelperPlus getter = BurpExtender.getHelperPlus();
		int code = getter.getStatusCode(message);
		
		if (ConfigManager.getBooleanConfigByKey(ConfigName.ApiReqToTitle)
				|| code != 200) {
			try {
				//将debug请求存储到title中
				LineEntry entry = new LineEntry(message);
				entry.addComment("AssetInfo:"+Commons.TimeToString(new Date().getTime()));
				BurpExtender.getGui().getTitlePanel().getTitleTable().getLineTableModel().addNewLineEntryWithTime(entry);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (code != 200) {
			return "";
		}
		byte[] byteBody = getter.getBody(false, message);
		return new String(byteBody);
	}
	
	
	public static void main(String[] args) {
		System.out.println(Commons.TimeToString(new Date().getTime()));
	}
}
