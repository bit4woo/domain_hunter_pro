package burp;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class HttpMessageCharSet {
	private static String getSystemCharSet() {
		return Charset.defaultCharset().toString();
	}
	
	public static byte[] covertCharSetToByte(byte[] response) {
		String originalCharSet = getCharset(response);
		//BurpExtender.getStderr().println(url+"---"+originalCharSet);
		if (originalCharSet == null) {
			return response;
		}else {
			byte[] newResponse;
			try {
				newResponse = new String(response,originalCharSet).getBytes(getSystemCharSet());
				return newResponse;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace(BurpExtender.getStderr());
				return response;
			}
		}
	}
	
	public static String getCharset(byte[] requestOrResponse){
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		Getter getter = new Getter(helpers);
		boolean isRequest = true;
		if (new String(requestOrResponse).startsWith("HTTP/")) {//response
			isRequest = false;
		}
		String contentType = getter.getHeaderValueOf(isRequest,requestOrResponse,"Content-Type");
		String tmpcharSet = "ISO-8859-1";//http post的默认编码
		
		if (contentType != null){//1、尝试从contentTpye中获取
			if (contentType.toLowerCase().contains("charset=")) {
				tmpcharSet = contentType.toLowerCase().split("charset=")[1];
			}
		}

		if (tmpcharSet == null){//2、尝试使用ICU4J进行编码的检测
			CharsetDetector detector = new CharsetDetector();
			detector.setText(requestOrResponse);
			CharsetMatch cm = detector.detect();
			tmpcharSet = cm.getName();
		}

		tmpcharSet = tmpcharSet.toLowerCase().trim();
		//常见的编码格式有ASCII、ANSI、GBK、GB2312、UTF-8、GB18030和UNICODE等。
		List<String> commonCharSet = Arrays.asList("ASCII,ANSI,GBK,GB2312,UTF-8,GB18030,UNICODE,utf8".toLowerCase().split(","));
		for (String item:commonCharSet) {
			if (tmpcharSet.contains(item)) {
				tmpcharSet = item;
			}
		}
		
		if (tmpcharSet.equals("utf8")) tmpcharSet = "utf-8";
		return tmpcharSet;
	}
}

