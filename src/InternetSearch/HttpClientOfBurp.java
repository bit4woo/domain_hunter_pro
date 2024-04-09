package InternetSearch;

import java.net.URL;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;


public class HttpClientOfBurp {
	private static Logger logger = new Logger("HttpClientOfBurp_log.txt", 10 * 1024 * 1024);//10M
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
		HelperPlus getter = new HelperPlus(helpers);
		int code = getter.getStatusCode(message);
		if (code != 200) {
			logger.log(new String(message.getRequest()));
			logger.log(new String(message.getResponse()));
			BurpExtender.getStderr().print("see log file for more info: "+logger.getLogFile());
			return "";
		}
		byte[] byteBody = getter.getBody(false, message);
		return new String(byteBody);
	}
}
