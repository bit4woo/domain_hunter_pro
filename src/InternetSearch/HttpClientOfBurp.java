package InternetSearch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.bit4woo.utilbox.burp.HelperPlus;

import base.Commons;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IRequestInfo;
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
		return doRequest(url,null,"");
	}

	/**
	 * 
	 * @param url
	 * @param byteRequest
	 * @return response body
	 */
	public static String doRequest(URL url,byte[] byteRequest,String comment) {
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
				entry.addComment(comment);
				//entry.addComment("AssetInfo:"+Commons.TimeToString(new Date().getTime()));
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
	
	/**
	 * 仅针对GET
	 * @param url
	 * @param headersToAdd
	 * @return
	 */
	public static byte[] buildHttpRequest(URL url,List<String> headersToAdd) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();
		
	    List<String> headers = new ArrayList<>();

	    headers.add("GET " + url.getPath() + " HTTP/1.1");
	    headers.add("Host: " + url.getHost());
	    headers.add("User-Agent: Mozilla/5.0");
	    headers.add("Accept: */*");

	    // 👇 你要加的 header
	    headers.addAll(headersToAdd);

	    byte[] body = null; // GET一般无body

	    byte[] byteRequest = helpers.buildHttpMessage(headers, body);
		
	    return byteRequest;
	}
	
	public static byte[] addHeader(byte[] byteRequest,List<String> headersToAdd) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();
		IRequestInfo requestInfo = helpers.analyzeRequest(byteRequest);

		List<String> headers = new ArrayList<>(requestInfo.getHeaders());

		// 添加新 header
		headers.addAll(headersToAdd);

		// 取 body
		byte[] body = Arrays.copyOfRange(
		    byteRequest,
		    requestInfo.getBodyOffset(),
		    byteRequest.length
		);

		// 重新构造
		byteRequest = helpers.buildHttpMessage(headers, body);
		return byteRequest;
	}
	
	
	public static void main(String[] args) {
		System.out.println(Commons.TimeToString(new Date().getTime()));
	}
}
