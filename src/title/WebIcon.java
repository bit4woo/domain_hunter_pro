package title;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.hash.Hashing;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import net.sf.image4j.codec.ico.ICODecoder;

public class WebIcon {

	/**
	 * 使用burp的HTTP请求方法
	 * 
	 * @param urlStr
	 * @return
	 */
	public static byte[] getFavicon(String faviconUrl) {
		try {
			URL url = new URL(faviconUrl);
			// https://www.baidu.com/favicon.ico
			IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
			byte[] requsetbyte = helpers.buildHttpRequest(url);
			int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
			IHttpService service = helpers.buildHttpService(url.getHost(), port, url.getProtocol());
			IHttpRequestResponse info = BurpExtender.getCallbacks().makeHttpRequest(service, requsetbyte);
			HelperPlus getter = new HelperPlus(helpers);
			int status = getter.getStatusCode(info);
			String ContentType = getter.getHeaderValueOf(false, info, "Content-Type");
			if (status == 200 && ContentType != null && ContentType.toLowerCase().startsWith("image/")) {
				// Content-Type: image/x-icon
				byte[] body = getter.getBody(false, info);// 这里不能使用静态方法。
				return body;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String getFaviconUrl(String urlStr, String html) {
		String baseUrl = getBaseUrl(urlStr);
		try {
			URL url = new URL(baseUrl + FaviconExtractor(html));
			return url.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getHash(byte[] imageData) {
		if (imageData == null) {
			return "";
		} else {
			return calcHash(imageData) + "";
		}
	}

	public static String getHash(String urlStr, String html) {
		String url = getFaviconUrl(urlStr, html);
		byte[] imageData = getFavicon(url);
		return getHash(imageData);
	}

	public static String FaviconExtractor(String html) {
		String faviconPath = "/favicon.ico"; // 默认值

		if (html == null || html.equals("")) {
			return faviconPath;
		}

		String regex = "<link\\s+rel=(?:\"shortcut\\s+)?icon\"\\s+href=\"([^\"]+)\"";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);

		while (matcher.find()) {
			faviconPath = matcher.group(1);
		}
		return faviconPath;
	}

	/**
	 * 使用非burp方法，这个方法没有的计算结果不对。应该是byte[]和string的转换导致的！
	 * 
	 * @param urlStr
	 * @return
	 */
	@Deprecated
	public static String getHashWithoutBurp(String urlStr) {
		String baseUrl = getBaseUrl(urlStr);
		try {
			URL url = new URL(baseUrl + "/favicon.ico");
			// https://www.baidu.com/favicon.ico
			byte[] body = HttpRequest.get(url).body().getBytes();
			int hash = calcHash(body);
			return hash + "";
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String getBaseUrl(String urlStr) {
		try {
			URL url = new URL(urlStr);
			String baseUrl = url.getProtocol() + "://" + url.getHost();
			if (url.getPort() != -1) {
				baseUrl = baseUrl + ":" + url.getPort();
			}
			return baseUrl;
		} catch (Exception e) {
			return "";
		}
	}

	public static int calcHash(byte[] content) {
		// String base64Str = new String(Base64.getEncoder().encode(content));

		// String base64Str = new sun.misc.BASE64Encoder().encode(content);
		String base64Str = new BASE64Encoder().encode(content);
		// System.out.println(base64Str);
		int hashvalue = Hashing.murmur3_32().hashString(base64Str.replaceAll("\r", "") + "\n", StandardCharsets.UTF_8)
				.asInt();
		return hashvalue;
	}

	public static byte[] imageToByteArray(String imagePath) throws IOException {
		File file = new File(imagePath);
		byte[] imageData = new byte[(int) file.length()];

		try (FileInputStream fis = new FileInputStream(file)) {
			fis.read(imageData);
		}
		return imageData;
	}

	public static byte[] convertIcoToPng(byte[] icoBytes) {
		if (icoBytes == null) {
			return null;
		}
		try {
			List<BufferedImage> images = ICODecoder.read(new ByteArrayInputStream(icoBytes));

			if (images.size() > 0) {
				BufferedImage bi = images.get(0);
				ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
				ImageIO.write(bi, "PNG", pngOutputStream);
				return pngOutputStream.toByteArray();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		// System.out.println(getHash("https://www.baidu.com"));
		System.out.println(getHash("https://www.baidu.com", ""));
	}
}
