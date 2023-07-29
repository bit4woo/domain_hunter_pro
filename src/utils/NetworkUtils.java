package utils;

import java.net.MalformedURLException;
import java.net.URL;

import base.Commons;
import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;

public class NetworkUtils {

    //Just do request
	public static IHttpRequestResponse doRequest(URL url,String cookie) {
        IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();

        byte[] byteRequest = helpers.buildHttpRequest(url);//GET
        
        if (cookie !=null) {
            byteRequest = Commons.buildCookieRequest(helpers,cookie,byteRequest);
        }

        IHttpService service = helpers.buildHttpService(url.getHost(),url.getPort(),url.getProtocol());
        IHttpRequestResponse https_Messageinfo = BurpExtender.getCallbacks().makeHttpRequest(service, byteRequest);
        return https_Messageinfo;
    }
    
    public static IHttpRequestResponse doRequest(String url,String cookie) {
		try {
			URL Url = new URL(url);
			return doRequest(Url,cookie);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
    }

    public static void main(String[] args ){
    }
}