package title;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.hash.Hashing;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;

public class WebIcon {
	/**
	 * 使用burp的HTTP请求方法
	 * @param urlStr
	 * @return
	 */
	public static String getHash(String urlStr) {
		String baseUrl = getBaseUrl(urlStr);
		try {
			URL url = new URL(baseUrl+"/favicon.ico");
			//https://www.baidu.com/favicon.ico
			IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
			byte[] requsetbyte = helpers.buildHttpRequest(url);
			int port = url.getPort()==-1?url.getDefaultPort():url.getPort();
			IHttpService service = helpers.buildHttpService(url.getHost(), port, url.getProtocol());
			IHttpRequestResponse info = BurpExtender.getCallbacks().makeHttpRequest(service,requsetbyte);
			byte[] body = new HelperPlus(helpers).getBody(false, info);//这里不能使用静态方法。
			if (body ==null) return "";
			int hash = calcHash(body);
			return hash+"";
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 使用非burp方法，这个方法没有的计算结果不对。应该是byte[]和string的转换导致的！
	 * @param urlStr
	 * @return
	 */
	@Deprecated
	public static String getHashWithoutBurp(String urlStr) {
		String baseUrl = getBaseUrl(urlStr);
		try {
			URL url = new URL(baseUrl+"/favicon.ico");
			//https://www.baidu.com/favicon.ico
			byte[] body = HttpRequest.get(url).body().getBytes();
			int hash = calcHash(body);
			return hash+"";
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}


	public static String getBaseUrl(String urlStr) {
		try {
			URL url = new URL(urlStr);
			String baseUrl = url.getProtocol()+"://"+url.getHost();
			if (url.getPort() != -1) {
				baseUrl = baseUrl+":"+url.getPort();
			}
			return baseUrl;
		}catch(Exception e) {
			return "";
		}
	}

	public static int calcHash(byte[] content) {
		//String base64Str = new String(Base64.getEncoder().encode(content));

		//String base64Str = new sun.misc.BASE64Encoder().encode(content);
		String base64Str = new BASE64Encoder().encode(content);
		//System.out.println(base64Str);
		int hashvalue = Hashing.murmur3_32().hashString(base64Str.replaceAll("\r","")+"\n",StandardCharsets.UTF_8).asInt();
		return hashvalue;
	}

	public static void main(String[] args) {
		//System.out.println(getHash("https://www.baidu.com"));
		System.out.println(getHashWithoutBurp("https://www.baidu.com"));
	}
}
