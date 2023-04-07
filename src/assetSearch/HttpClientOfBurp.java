package assetSearch;

import java.net.URL;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;

public class HttpClientOfBurp {

	public static IHttpRequestResponse doRequest(URL url) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();

		byte[] byteRequest = helpers.buildHttpRequest(url);//GET

		IHttpService service =getHttpService(url);
		IHttpRequestResponse message = callbacks.makeHttpRequest(service, byteRequest);
		return message;
	}

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


	public static String doRequestGetBody(URL url) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IExtensionHelpers helpers = callbacks.getHelpers();

		byte[] byteRequest = helpers.buildHttpRequest(url);//GET

		IHttpService service =getHttpService(url);
		IHttpRequestResponse message = callbacks.makeHttpRequest(service, byteRequest);
		HelperPlus getter = new HelperPlus(helpers);

		byte[] byteBody = getter.getBody(false, message);
		
		return new String(byteBody);
	}
}
