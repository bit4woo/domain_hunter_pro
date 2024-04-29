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

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;

import com.bit4woo.utilbox.burp.HelperPlus;
import com.bit4woo.utilbox.utils.TextUtils;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.hash.Hashing;

import burp.BurpExtender;
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
			HelperPlus getter = BurpExtender.getHelperPlus();
			int status = getter.getStatusCode(info);
			String ContentType = getter.getHeaderValueOf(false, info, "Content-Type");
			if (status == 200 && ContentType != null &&
					(ContentType.toLowerCase().startsWith("image/") || ContentType.toLowerCase().contains("octet-stream"))) {
				// Content-Type: image/x-icon
				// Content-Type: binary/octet-stream 有的图片存储在OSS上的
				byte[] body = HelperPlus.getBody(false, info);// 这里不能使用静态方法。
				return body;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

	public static String getFaviconUrl(String urlStr, String html) {
		String iconUrl = FaviconExtractor(html);

		String baseUrl = getBaseUrl(urlStr);
		String parentUrl = urlStr.substring(0, urlStr.lastIndexOf('/') + 1);
		String protocol = urlStr.substring(0, urlStr.indexOf("//"));

		try {
			new URL(iconUrl); //本来就是一个单独的URL
			return iconUrl;
		} catch (MalformedURLException e) {

		}

		if (iconUrl.startsWith("//")) {
			return protocol + iconUrl;
		}else if (iconUrl.startsWith("/")) {
			return baseUrl + iconUrl;
		}else {
			return parentUrl + iconUrl;
		}
	}

	public static String getHash(byte[] imageData) {
		if (imageData == null) {
			return "";
		}
		if (imageData.length ==0) {
			return "";
		}
		if (new String(imageData).toLowerCase().contains("<html>")) {
			return "";
		}
		return calcHash(imageData) + "";

	}

	public static String getHash(String urlStr, String html) {
		String url = getFaviconUrl(urlStr, html);
		byte[] imageData = getFavicon(url);
		return getHash(imageData);
	}

	//	<link rel="icon" href="./logo.png">
	//	<link rel="icon" href="logo.png">
	//	<link rel="shortcut icon" href="//p3-x.xx.com/obj/xxx/favicon.ico">
	//	<link rel='icon' href='/favicon.ico' type='image/x-ico'/>
	//	<link href="/resources/admin-favicon.ico" rel="shortcut icon" type="image/x-ico"/>
	//	<link href="/resources/admin-favicon.ico" rel="icon" type="image/x-ico"/>
	public static String FaviconExtractor_old(String html) {
		String faviconPath = "/favicon.ico"; // 默认值

		if (StringUtils.isEmpty(html)) {
			return faviconPath;
		}
		String regex = "<link\\s+rel=[\"|\'][shortcut\\s+]*icon[\"|\']\\s+href=[\"|\'](.*?)[\"|\']";
		//String regex = "icon\"\\s+href=\"(.*?)\">";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);

		while (matcher.find()) {
			faviconPath = matcher.group(1);
		}
		return faviconPath;
	}

	/**
		<link rel="icon" href="./logo.png">
		<link rel="icon" href="logo.png">
		<link rel="shortcut icon" href="//p3-x.xx.com/obj/xxx/favicon.ico">
		<link rel='icon' href='/favicon.ico' type='image/x-ico'/>
		<link href="/resources/admin-favicon.ico" rel="shortcut icon" type="image/x-ico"/>
		<link href="/resources/admin-favicon.ico" rel="icon" type="image/x-ico"/>
	 * @param html
	 * @return
	 */
	public static String FaviconExtractor(String html) {
		String faviconPath = "/favicon.ico"; // 默认值

		if (StringUtils.isEmpty(html)) {
			return faviconPath;
		}

		html = formatHtmlAndGetHead(html);
		for (String line:html.split("\n|>")){
			String line_lower = line.toLowerCase();
			//System.out.println(line);
			if (line_lower.contains("<link") && line_lower.contains("rel=") && line_lower.contains("icon")) {
				String regex = "href=[\"|\'](.*?)[\"|\']";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(line);
				while (matcher.find()) {
					faviconPath = matcher.group(1);
					return faviconPath;
				}
			}
			if (line_lower.contains("</head>")) {
				return faviconPath;
			}
		}
		return faviconPath;
	}


	public static String formatHtmlAndGetHead(String unformattedHtml) {
		String head = "";
		try {
			// 使用JSoup解析HTML
			Document document = Jsoup.parse(unformattedHtml, "", Parser.xmlParser());

			// 设置输出格式，即进行美化
			document.outputSettings().prettyPrint(true);

			// 设置转义实体，避免特殊字符被转义
			document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

			// 获取HTML的<head>元素
			org.jsoup.nodes.Element headElement = document.head();

			// 返回<head>元素的内容
			head = headElement.html();
			if (head.contains("icon")){
				return head;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			List<String> heads = TextUtils.grepBetween("<head", "</head", unformattedHtml);
			if (heads.size()>0){
				head = heads.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return head;
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
		if (icoBytes == null || icoBytes.length==0) {
			return new byte[0];
		}
		if (isPNG(icoBytes)) {
			return icoBytes;
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
			return icoBytes;
		}
		return new byte[0];
	}

	public static boolean isPNG(byte[] data) {
		// PNG 文件的前八个字节的标识符
		byte[] pngSignature = { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };

		if (data.length < 8) {
			return false;  // 数据长度不足八个字节，无法检测
		}

		for (int i = 0; i < pngSignature.length; i++) {
			if (data[i] != pngSignature[i]) {
				return false;
			}
		}

		return true;
	}

	public static void main(String[] args) {
		// System.out.println(getHash("https://www.baidu.com"));
		//System.out.print(FaviconExtractor("<link rel='icon' href='/xxx-favicon.ico' type='image/x-ico'/>"));
	}
}
