package test;
import java.net.MalformedURLException;
/*
 * source code: https://github.com/bit4woo/burp-api-common/blob/master/src/main/java/burp/Getter.java
 * author: bit4woo
 * github: https://github.com/bit4woo
 */
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IParameter;
import burp.IRequestInfo;
import burp.IResponseInfo;

public class TestGetter {
	private static IExtensionHelpers helpers;
	private final static String Header_Spliter = ": ";
	private final static String Header_Spliter_Alteration = ":";
	private final static String Header_firstLine_Spliter = " ";

	public TestGetter(IExtensionHelpers helpers) {
		TestGetter.helpers = helpers;
	}

	/*
	 * 获取header的字符串数组，是构造burp中请求需要的格式。
	 * return headers list
	 */
	public synchronized List<String> getHeaderList(boolean messageIsRequest,final IHttpRequestResponse messageInfo) {
		if (null == messageInfo) return null;
		byte[] requestOrResponse = null;
		if(messageIsRequest) {
			requestOrResponse = messageInfo.getRequest();
		}else {
			requestOrResponse = messageInfo.getResponse();
		}
		return getHeaderList(messageIsRequest,requestOrResponse);
	}
	
	/*
	 * 获取请求包或者响应包中的header List
	 */
	public synchronized List<String> getHeaderList(boolean IsRequest,final byte[] requestOrResponse) {
		if (null == requestOrResponse){
		    BurpExtender.getStderr().println("requestOrResponse is null,return null");
		    return null;
        }
		if(IsRequest) {
			IRequestInfo analyzeRequest = helpers.analyzeRequest(requestOrResponse);
			List<String> headers = analyzeRequest.getHeaders();
			return headers;
		}else {
			IResponseInfo analyzeResponse = helpers.analyzeResponse(requestOrResponse);
			List<String> headers = analyzeResponse.getHeaders();
			return headers;
		}
	}

	/*
	 * 获取所有headers，当做一个string看待。
	 * 主要用于判断是否包含某个特殊字符串
	 * List<String> getHeaders 调用toString()方法，得到如下格式：[111111, 2222]
	 * 就能满足上面的场景了,废弃这个函数
	 */
	@Deprecated
	public synchronized String getHeaderString(boolean messageIsRequest,final IHttpRequestResponse messageInfo) {
		List<String> headers =null;
		StringBuilder headerString = new StringBuilder();
		if(messageIsRequest) {
			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
			headers = analyzeRequest.getHeaders();
		}else {
			IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
			headers = analyzeResponse.getHeaders();
		}

		for (String header : headers) {
			headerString.append(header);
		}

		return headerString.toString();
	}

	/*
	 * 获取header的map格式，key:value形式
	 * 这种方式可以用put函数轻松实现：如果有则update，如果无则add。
	 * ！！！注意：这个方法获取到的map，第一行将分割成形如 key = "GET", value= "/cps.gec/limit/information.html HTTP/1.1"
	 * 响应包则分割成形如：key =  "HTTP/1.1", value="200 OK"
	 */
	public synchronized LinkedHashMap<String,String> getHeaderMap(boolean messageIsRequest,final IHttpRequestResponse messageInfo) {
		if (messageInfo == null) {
		    BurpExtender.getStderr().println("messageInfo is null, return null");
		    return null;
        }

		List<String> headers=getHeaderList(messageIsRequest, messageInfo);
		return headerListToHeaderMap(headers);
	}

	/*
	 * use LinkedHashMap to keep headers in order
	 */
	public synchronized LinkedHashMap<String,String> getHeaderMap(boolean messageIsRequest,final byte[] requestOrResponse) {
		if (requestOrResponse == null) return null;
		List<String> headers=getHeaderList(messageIsRequest, requestOrResponse);
		return headerListToHeaderMap(headers);
	}

	/*
	 * 仅该类内部调用
	 */
	private synchronized static LinkedHashMap<String, String> headerListToHeaderMap(List<String> headers) {
		LinkedHashMap<String,String> result = new LinkedHashMap<String, String>();
		if (null == headers) return null;
		for (String header : headers) {
			if (headers.indexOf(header) == 0) {
				String headerName = header.split(Header_firstLine_Spliter, 2)[0];//这里的limit=2 可以理解成分割成2份
				String headerValue = header.split(Header_firstLine_Spliter, 2)[1];
				result.put(headerName, headerValue);
			}else {
				try {
					String headerName = header.split(Header_Spliter, 2)[0];//这里的limit=2 可以理解成分割成2份，否则referer可能别分成3份
					String headerValue = header.split(Header_Spliter, 2)[1];
					result.put(headerName, headerValue);
				}catch (Exception e) {
					String headerName = header.split(Header_Spliter_Alteration, 2)[0];
					String headerValue = header.split(Header_Spliter_Alteration, 2)[1];
					result.put(headerName, headerValue);
				}
			}
		}
		return result;
	}



	public synchronized List<String> headerMapToHeaderList(LinkedHashMap<String,String> Headers){
		List<String> result = new ArrayList<String>();
		for (Entry<String,String> header:Headers.entrySet()) {
			String key = header.getKey();
			String value = header.getValue();
			if (key.contains("HTTP/") || value.contains("HTTP/")) {//识别第一行
				String item = key+Header_firstLine_Spliter+value;
				result.add(0, item);
			}else {
				String item = key+Header_Spliter+value;
				result.add(item);
			}
		}
		return result;
	}

	/*
	 * 获取某个header的值，如果没有此header，返回null。
	 */
	public synchronized String getHeaderValueOf(boolean messageIsRequest,final IHttpRequestResponse messageInfo, String headerName) {
		LinkedHashMap<String, String> headers = getHeaderMap(messageIsRequest,messageInfo);
		if (null ==headers || headerName ==null) return null;
		return headers.get(headerName.trim());
	}

	/*
	 * 获取某个header的值，如果没有此header，返回null。
	 */
	public synchronized String getHeaderValueOf(boolean messageIsRequest,final byte[] requestOrResponse, String headerName) {
		LinkedHashMap<String, String> headers=getHeaderMap(messageIsRequest,requestOrResponse);
		if (null ==headers || headerName ==null) return null;
		return headers.get(headerName.trim());
	}


	public synchronized byte[] getBody(boolean messageIsRequest,final IHttpRequestResponse messageInfo) {
		if (messageInfo == null){
			return null;
		}
		byte[] requestOrResponse = null;
		if(messageIsRequest) {
			requestOrResponse = messageInfo.getRequest();
		}else {
			requestOrResponse = messageInfo.getResponse();
		}
		return getBody(messageIsRequest, requestOrResponse);
	}

	public synchronized byte[] getBody(boolean isRequest,final byte[] requestOrResponse) {
		if (requestOrResponse == null){
			return null;
		}
		int bodyOffset = -1;
		if(isRequest) {
			IRequestInfo analyzeRequest = helpers.analyzeRequest(requestOrResponse);
			bodyOffset = analyzeRequest.getBodyOffset();
		}else {
			IResponseInfo analyzeResponse = helpers.analyzeResponse(requestOrResponse);
			bodyOffset = analyzeResponse.getBodyOffset();
		}
		byte[] byte_body = Arrays.copyOfRange(requestOrResponse, bodyOffset, requestOrResponse.length);//not length-1
		//String body = new String(byte_body); //byte[] to String
		return byte_body;
	}


	/*
	 * 注意，这里获取的URL包含了默认端口！
	 * this return value of url contains default port, 80 :443
	 * eg. http://bit4woo.com:80/
	 */
	public synchronized String getShortUrlWithDefaultPort(final IHttpRequestResponse messageInfo) {
		//return messageInfo.getHttpService().toString(); //this result of this method doesn't contains default port
		URL fullUrl = getURLWithDefaultPort(messageInfo);
		String shortUrl = fullUrl.toString().replace(fullUrl.getFile(), "/");
		return shortUrl;
	}

	/*
	 * 注意，这里获取的URL包含了默认端口！
	 * this return value of url contains default port, 80 :443
	 * eg. http://bit4woo.com:80/
	 */
	public synchronized URL getURLWithDefaultPort(final IHttpRequestResponse messageInfo){
		if (null == messageInfo) return null;
		IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
		return analyzeRequest.getUrl();
	}
	
	/*
	 *
	 * this return value of url will NOT contains default port, 80 :443
	 * eg.  https://www.baidu.com
	 */
	public synchronized String getShortUrl(final IHttpRequestResponse messageInfo) {
		return messageInfo.getHttpService().toString(); //this result of this method doesn't contains default port
	}

	/*
	 * this return value of url will NOT contains default port, 80 :443
	 */
	public synchronized URL getURL(final IHttpRequestResponse messageInfo){
		if (null == messageInfo) return null;
		IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
		URL url = analyzeRequest.getUrl();
		if (url.getProtocol().equalsIgnoreCase("https") && url.getPort() == 443) {
			try {
				return new URL(url.toString().replaceFirst(":443/", ":/"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if (url.getProtocol().equalsIgnoreCase("http") && url.getPort() == 80) {
			try {
				return new URL(url.toString().replaceFirst(":80/", ":/"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return url;
	}

	public synchronized String getHost(final IHttpRequestResponse messageInfo) {
		return messageInfo.getHttpService().getHost();
	}

	public synchronized short getStatusCode(final IHttpRequestResponse messageInfo) {
		if (messageInfo == null || messageInfo.getResponse() == null) {
			return -1;
		}
		IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse());
		return analyzedResponse.getStatusCode();
	}

	public synchronized short getStatusCode(byte[] response) {
		if (response == null) {
			return -1;
		}
		try {
			IResponseInfo analyzedResponse = helpers.analyzeResponse(response);
			return analyzedResponse.getStatusCode();
		} catch (Exception e) {
			return -1;
		}
	}

	public synchronized List<IParameter> getParas(final IHttpRequestResponse messageInfo){
		IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
		return analyzeRequest.getParameters();
	}

	public synchronized List<IParameter> getParas(byte[] request){
		IRequestInfo analyzeRequest = helpers.analyzeRequest(request);
		return analyzeRequest.getParameters();
	}

	public synchronized String getMethod(final IHttpRequestResponse messageInfo){
		if (messageInfo == null || messageInfo.getRequest() == null) {
			return null;
		}
		IRequestInfo analyzedRequest = helpers.analyzeRequest(messageInfo.getRequest());
		return analyzedRequest.getMethod();
	}

	public synchronized String getMethod(byte[] request){
		if (request == null) {
			return null;
		}
		try {
			IRequestInfo analyzedRequest = helpers.analyzeRequest(request);
			return analyzedRequest.getMethod();
		} catch (Exception e) {
			return null;
		}
	}

	public synchronized String getHTTPBasicCredentials(final IHttpRequestResponse messageInfo) throws Exception{
		String authHeader  = getHeaderValueOf(true, messageInfo, "Authorization").trim();
		String[] parts = authHeader.split("\\s");

		if (parts.length != 2)
			throw new Exception("Wrong number of HTTP Authorization header parts");

		if (!parts[0].equalsIgnoreCase("Basic"))
			throw new Exception("HTTP authentication must be Basic");

		return parts[1];
	}

	public static void main(String args[]) {
		String a= "xxxxx%s%bxxxxxxx";
		System.out.println(String.format(a, "111"));
	}
}
