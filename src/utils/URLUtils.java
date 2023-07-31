package utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URLUtils {
	public static void main(String[] args) {
		String aaa = "https://api.example.vn:443/Execute#1653013013763";
		String bbb = "https://api.example.vn/Execute#1653013013763";

		String url1 = "http://www.example.com";
		String url2 = "https://www.example.com:8080";
		String url3 = "ftp://www.example.com:21/files#1111";

		System.out.println(getUrlWithDefaultPort(url1));
		System.out.println(getUrlWithDefaultPort(url2));
		System.out.println(getUrlWithDefaultPort(url3));

		try {
			System.out.println(new URL(bbb).toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	// 添加默认端口的方法
	public static String getUrlWithDefaultPort(String url) {
		try {
			URI uri = new URI(url);

			// 如果URL中没有明确指定端口，且协议为http，则添加默认端口80
			if (uri.getPort() == -1 ) {
				if ("http".equalsIgnoreCase(uri.getScheme())) {
					return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), 80, uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
				}
				if ("https".equalsIgnoreCase(uri.getScheme())) {
					return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), 443, uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
				}
			}
			// 其他情况直接返回原始URL
			return url;
		} catch (URISyntaxException e) {
			// 处理URI语法错误
			e.printStackTrace();
			return url;
		}
	}

}
