package utils;

import java.net.MalformedURLException;
import java.net.URL;

import base.Commons;
import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import com.bit4woo.utilbox.burp.HelperPlus;
import org.apache.commons.lang3.StringUtils;

public class NetworkUtils {

    //Just do request
    public static IHttpRequestResponse doRequest(URL url, String cookie) {
        IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();

        byte[] byteRequest = helpers.buildHttpRequest(url);//GET

        if (StringUtils.isNotEmpty(cookie)) {
            if (cookie.toLowerCase().startsWith("cookie:")) {
                cookie = cookie.substring("cookie:".length());
            }
            byteRequest = new HelperPlus(helpers).addOrUpdateHeader(true, byteRequest, "Cookie", cookie);
        }

        IHttpService service = helpers.buildHttpService(url.getHost(), url.getPort(), url.getProtocol());
        return BurpExtender.getCallbacks().makeHttpRequest(service, byteRequest);
    }

    public static IHttpRequestResponse doRequest(String url, String cookie) {
        try {
            URL Url = new URL(url);
            return doRequest(Url, cookie);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
    }
}