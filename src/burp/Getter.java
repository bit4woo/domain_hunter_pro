package burp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Getter {
	private static IExtensionHelpers helpers;
	private final static String Header_Spliter = ": ";
	public Getter(IExtensionHelpers helpers) {
		this.helpers = helpers;
	}

	/*
	 * 获取header的字符串数组，是构造burp中请求需要的格式。
	 */
	public List<String> getHeaderList(boolean messageIsRequest,IHttpRequestResponse messageInfo) {
		if(messageIsRequest) {
			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
			List<String> headers = analyzeRequest.getHeaders();
			return headers;
		}else {
			IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
			List<String> headers = analyzeResponse.getHeaders();
			return headers;
		}
	}

	public List<String> getHeaderList(boolean IsRequest,byte[] requestOrResponse) {
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
	 * 就能满足上面的场景了
	 */
	public String getHeaderString(boolean messageIsRequest,IHttpRequestResponse messageInfo) {
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
	 * ！！！注意：这个方法获取到的map，会少了协议头GET /cps.gec/limit/information.html HTTP/1.1
	 */
	public HashMap<String,String> getHeaderHashMap(boolean messageIsRequest,IHttpRequestResponse messageInfo) {
		List<String> headers=null;
		HashMap<String,String> result = new HashMap<String, String>();
		if(messageIsRequest) {
			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
			headers = analyzeRequest.getHeaders();
		}else {
			IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
			headers = analyzeResponse.getHeaders();
		}

		for (String header : headers) {
			if(header.contains(Header_Spliter)) {//to void trigger the Exception
				try {
					String headerName = header.split(Header_Spliter, 0)[0];
					String headerValue = header.split(Header_Spliter, 0)[1];
					//POST /login.pub HTTP/1.1  the first line of header will tirgger error here
					result.put(headerName, headerValue);
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
		}
		return result;
	}

	public List<String> MapToList(HashMap<String,String> Headers){
		List<String> result = new ArrayList<String>();
		for (Entry<String,String> header:Headers.entrySet()) {
			String item = header.getKey()+Header_Spliter+header.getValue();
			result.add(item);
		}
		return result;
	}

	/*
	 * 获取某个header的值，如果没有此header，返回null。
	 */
	public String getHeaderValueOf(boolean messageIsRequest,IHttpRequestResponse messageInfo, String headerName) {
		List<String> headers=null;
		if(messageIsRequest) {
			if (messageInfo.getRequest() == null) {
				return null;
			}
			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
			headers = analyzeRequest.getHeaders();
		}else {
			if (messageInfo.getResponse() == null) {
				return null;
			}
			IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
			headers = analyzeResponse.getHeaders();
		}


		headerName = headerName.toLowerCase().replace(":", "");
		for (String header : headers) {
			if (header.toLowerCase().startsWith(headerName)) {
				return header.split(Header_Spliter, 2)[1];//分成2部分，Location: https://www.jd.com
			}
		}
		return null;
	}


	public byte[] getBody(boolean messageIsRequest,IHttpRequestResponse messageInfo) {
		if (messageInfo == null){
			return null;
		}
		if(messageIsRequest) {
			if (messageInfo.getRequest() ==null) {
				return null;
			}
			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
			int bodyOffset = analyzeRequest.getBodyOffset();
			byte[] byte_Request = messageInfo.getRequest();

			byte[] byte_body = Arrays.copyOfRange(byte_Request, bodyOffset, byte_Request.length);//not length-1
			//String body = new String(byte_body); //byte[] to String
			return byte_body;
		}else {
			if (messageInfo.getResponse() ==null) {
				return null;
			}
			IResponseInfo analyzeResponse = helpers.analyzeResponse(messageInfo.getResponse());
			int bodyOffset = analyzeResponse.getBodyOffset();
			byte[] byte_Request = messageInfo.getResponse();

			byte[] byte_body = Arrays.copyOfRange(byte_Request, bodyOffset, byte_Request.length);//not length-1
			return byte_body;
		}
	}

	public byte[] getBody(boolean isRequest,byte[] requestOrResponse) {
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


	public String getShortUrl(IHttpRequestResponse messageInfo) {
		return messageInfo.getHttpService().toString();
	}

	public URL getURL(IHttpRequestResponse messageInfo){
		IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
		return analyzeRequest.getUrl();

		//callbacks.getHelpers().analyzeRequest(baseRequestResponse).getUrl();
	}

	public String getHost(IHttpRequestResponse messageInfo) {
		return messageInfo.getHttpService().getHost();
	}

	public short getStatusCode(IHttpRequestResponse messageInfo) {
		if (messageInfo == null || messageInfo.getResponse() == null) {
			return -1;
		}
		IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse());
		return analyzedResponse.getStatusCode();
	}

	public List<IParameter> getParas(IHttpRequestResponse messageInfo){
		IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
		return analyzeRequest.getParameters();
	}


	public String getHTTPBasicCredentials(IHttpRequestResponse messageInfo) throws Exception{
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
