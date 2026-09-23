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

    // 默认最大跳转次数
    public static final int DEFAULT_MAX_REDIRECTS = 5;

    //Just do request（默认跟踪跳转，最多 DEFAULT_MAX_REDIRECTS 次）
    public static IHttpRequestResponse doRequest(URL url, String cookie) {
        return doRequest(url, cookie, true, DEFAULT_MAX_REDIRECTS);
    }

    public static IHttpRequestResponse doRequest(String url, String cookie) {
        return doRequest(url, cookie, true, DEFAULT_MAX_REDIRECTS);
    }

    /**
     * 发起GET请求，并可选地跟踪HTTP跳转。
     *
     * @param url            目标URL
     * @param cookie         Cookie（可为空；若以"cookie:"开头会自动去掉前缀）
     * @param followRedirect 是否跟踪跳转（301/302/303/307/308）
     * @param maxRedirects   最大跳转次数；&lt;=0 时等同不跟踪跳转
     * @return 最终的请求响应；发生错误或达到跳转上限时返回最后一次拿到的响应
     */
    public static IHttpRequestResponse doRequest(URL url, String cookie, boolean followRedirect, int maxRedirects) {
        if (url == null) {
            return null;
        }

        IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
        HelperPlus getter = new HelperPlus(helpers);

        URL currentUrl = url;
        int redirectCount = 0;

        while (true) {
            byte[] byteRequest = helpers.buildHttpRequest(currentUrl);//GET

            if (StringUtils.isNotEmpty(cookie)) {
                String c = cookie;
                if (c.toLowerCase().startsWith("cookie:")) {
                    c = c.substring("cookie:".length());
                }
                byteRequest = getter.addOrUpdateHeader(true, byteRequest, "Cookie", c);
            }

            int port = currentUrl.getPort();
            if (port == -1) {
                port = currentUrl.getDefaultPort();
            }
            IHttpService service = helpers.buildHttpService(currentUrl.getHost(), port, currentUrl.getProtocol());
            IHttpRequestResponse response = BurpExtender.getCallbacks().makeHttpRequest(service, byteRequest);

            // 不跟踪跳转、或没有响应时，直接返回
            if (!followRedirect || response == null || response.getResponse() == null) {
                return response;
            }

            short statusCode = getter.getStatusCode(response);
            if (!isRedirect(statusCode)) {
                return response;
            }

            // 已达跳转上限，返回最后一次（仍是3xx跳转）的响应
            if (redirectCount >= maxRedirects) {
                return response;
            }

            String location = HelperPlus.getHeaderValueOf(false, response.getResponse(), "Location");
            if (StringUtils.isEmpty(location)) {
                // 3xx但没有Location头，无法继续跳转
                return response;
            }

            URL nextUrl;
            try {
                // 支持绝对地址、相对地址、协议相对(//host)等Location格式
                nextUrl = new URL(currentUrl, location.trim());
            } catch (MalformedURLException e) {
                // Location非法，无法继续跳转
                return response;
            }

            currentUrl = nextUrl;
            redirectCount++;
        }
    }

    /**
     * 字符串形式的重载，额外处理URL解析异常。
     */
    public static IHttpRequestResponse doRequest(String url, String cookie, boolean followRedirect, int maxRedirects) {
        try {
            URL Url = new URL(url);
            return doRequest(Url, cookie, followRedirect, maxRedirects);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断状态码是否为需要跟踪的跳转（301/302/303/307/308）。
     * 300(多选)、304(未修改)等不在此列。
     */
    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    public static void main(String[] args) {
    }
}