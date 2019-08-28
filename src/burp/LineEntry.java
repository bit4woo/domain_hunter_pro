package burp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.Charsets;

import com.alibaba.fastjson.JSON;

public class LineEntry {

	public static final String Level_A = "A";
	public static final String Level_B = "B";
	public static final String Level_C = "C";

	private int port =-1;
	private String host = "";
	private String protocol ="";
	//these three == IHttpService, helpers.buildHttpService to build. 

	private byte[] request = {};
	private byte[] response = {};
	// request+response+httpService == IHttpRequestResponse

	//used in UI,the fields to show
	private String url = "";
	private int statuscode = -1;
	private int contentLength = -1;
	private String title = "";
	private String IP = "";
	private String CDN = "";
	private String webcontainer = "";
	private String time = "";

	//Gson中，加了transient表示不序列号，是最简单的方法
	//给不想被序列化的属性增加transient属性---java特性
	private transient String messageText = "";//use to search
	//private transient String bodyText = "";//use to adjust the response changed or not
	//don't store these two field to reduce config file size.

	//field for user
	private boolean isChecked =false;
	private String Level = Level_C;
	private String comment ="";

	private transient IHttpRequestResponse messageinfo;

	//remove IHttpRequestResponse field ,replace with request+response+httpService(host port protocol). for convert to json.

	private transient BurpExtender burp;
	private transient IExtensionHelpers helpers;
	private transient IBurpExtenderCallbacks callbacks;

	LineEntry(){

	}

	public LineEntry(IHttpRequestResponse messageinfo) {
		this.messageinfo = messageinfo;
		this.callbacks = BurpExtender.getCallbacks();
		this.helpers = this.callbacks.getHelpers();
		parse();
	}

	public LineEntry(IHttpRequestResponse messageinfo,boolean isNew,boolean Checked,String comment) {
		this.messageinfo = messageinfo;
		this.callbacks = BurpExtender.getCallbacks();
		this.helpers = this.callbacks.getHelpers();
		parse();

		this.isChecked = Checked;
		this.comment = comment;
	}

	public LineEntry(IHttpRequestResponse messageinfo,boolean isNew,boolean Checked,String comment,Set<String> IPset,Set<String> CDNset) {
		this.messageinfo = messageinfo;
		this.callbacks = BurpExtender.getCallbacks();
		this.helpers = this.callbacks.getHelpers();
		parse();

		this.isChecked = Checked;
		this.comment = comment;
		if (this.IP != null) {
			this.IP = IPset.toString().replace("[", "").replace("]", "");
		}

		if (this.CDN != null) {
			this.CDN = CDNset.toString().replace("[", "").replace("]", "");
		}
	}

	public String ToJson(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.toJSONString(this);
	}

	public static LineEntry FromJson(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.parseObject(json, LineEntry.class);
	}

	private void parse() {
		try {

			//time = Commons.getNowTimeString();//这是动态的，会跟随系统时间自动变化,why?--是因为之前LineTableModel的getValueAt函数每次都主动调用了该函数。

			IHttpService service = this.messageinfo.getHttpService();

			//url = service.toString();
			url = helpers.analyzeRequest(messageinfo).getUrl().toString();//包含了默认端口
			port = service.getPort();
			host = service.getHost();
			protocol = service.getProtocol();

			if (messageinfo.getRequest() != null){
				request = messageinfo.getRequest();
			}

			if (messageinfo.getResponse() != null){
				response = messageinfo.getResponse();
				IResponseInfo responseInfo = helpers.analyzeResponse(response);
				statuscode = responseInfo.getStatusCode();

				//				MIMEtype = responseInfo.getStatedMimeType();
				//				if(MIMEtype == null) {
				//					MIMEtype = responseInfo.getInferredMimeType();
				//				}


				Getter getter = new Getter(helpers);
				messageText = new String(messageinfo.getRequest())+new String(response);


				webcontainer = getter.getHeaderValueOf(false, messageinfo, "Server");
				byte[] byteBody = getter.getBody(false, messageinfo);
				try{
					contentLength = Integer.parseInt(getter.getHeaderValueOf(false, messageinfo, "Content-Length").trim());
				}catch (Exception e){
					if (contentLength==-1 && byteBody!=null) {
						contentLength = byteBody.length;
					}
				}

				title = fetchTitle(response);

			}
		}catch(Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	public void DoDirBrute() {

	}

	public String getUrl() {//为了格式统一，和查找匹配更精确，都包含了默认端口
		if (url == null || url.equals("")) {
			return protocol+"://"+host+":"+port+"/";
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getStatuscode() {
		return statuscode;
	}

	public void setStatuscode(int statuscode) {
		this.statuscode = statuscode;
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIP() {
		return IP;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public void setIPWithSet(Set<String> ipSet) {
		IP = ipSet.toString().replace("[", "").replace("]", "");
	}

	public String getCDN() {
		return CDN;
	}

	public void setCDN(String cDN) {
		CDN = cDN;
	}

	public void setCDNWithSet(Set<String> cDNSet) {
		CDN = cDNSet.toString().replace("[", "").replace("]", "");
	}
	public String getWebcontainer() {
		return webcontainer;
	}

	public void setWebcontainer(String webcontainer) {
		this.webcontainer = webcontainer;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}


	public IHttpRequestResponse getMessageinfo() {
		//		if (messageinfo == null){
		//			try{
		//				messageinfo = callbacks.getHelpers().buildHttpMessage()
		//				IHttpRequestResponse messageinfo = new IHttpRequestResponse();
		//				messageinfo.setRequest(this.request);//始终为空，why??? because messageinfo is null ,no object to set content.
		//				messageinfo.setRequest(this.response);
		//				IHttpService service = callbacks.getHelpers().buildHttpService(this.host,this.port,this.protocol);
		//				messageinfo.setHttpService(service);
		//			}catch (Exception e){
		//				System.out.println("error "+url);
		//			}
		//		}
		return messageinfo;
	}

	public void setMessageinfo(IHttpRequestResponse messageinfo) {
		this.messageinfo = messageinfo;
	}

	public String getBodyText() {
		Getter getter = new Getter(BurpExtender.getCallbacks().getHelpers());
		byte[] byte_body = getter.getBody(false, response);
		return new String(byte_body);
	}


	/*
Content-Type: text/html;charset=UTF-8

<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta charset="utf-8">
<script type="text/javascript" charset="utf-8" src="./resources/jrf-resource/js/jrf.min.js"></script>
	 */
	private String getResponseCharset(byte[] response){
		Getter getter = new Getter(helpers);
		String contentType = getter.getHeaderValueOf(false,response,"Content-Type");
		String tmpcharSet = null;
		if (contentType != null && contentType.toLowerCase().contains("charset=")){
			tmpcharSet = contentType.toLowerCase().split("charset=")[1];
		}else {
			String body = new String(getter.getBody(false,response));
			Pattern pDomainNameOnly = Pattern.compile("charset=(.*?)>");
			Matcher matcher = pDomainNameOnly.matcher(body);
			if (matcher.find()) {
				tmpcharSet = matcher.group(0).toLowerCase();
//				tmpcharSet = tmpcharSet.replace("\"","");
//				tmpcharSet = tmpcharSet.replace(">","");
//				tmpcharSet = tmpcharSet.replace("/","");
//				tmpcharSet = tmpcharSet.replace("charset=","");
			}
		}
		//常见的编码格式有ASCII、ANSI、GBK、GB2312、UTF-8、GB18030和UNICODE等。
		List<String> commonCharSet = Arrays.asList("ASCII,ANSI,GBK,GB2312,UTF-8,GB18030,UNICODE,ISO-8859-1".toLowerCase().split(","));
		if (tmpcharSet == null){
			return null;
		}else if (tmpcharSet.contains("utf8")){
			return "utf-8";
		}else {
			for (String item:commonCharSet) {
				if (tmpcharSet.contains(item)) {
					return item;
				}
			}
		}
		return null;
	}

	//https://javarevisited.blogspot.com/2012/01/get-set-default-character-encoding.html
	private static String getSystemCharSet() {
		return Charset.defaultCharset().toString();
	}


	public String covertCharSet(byte[] response) {
		String originalCharSet = getResponseCharset(response);
		//BurpExtender.getStderr().println(url+"---"+originalCharSet);
		if (originalCharSet == null) {
			return new String(response);
		}else {
			byte[] newResponse;
			try {
				newResponse = new String(response,originalCharSet).getBytes(getSystemCharSet());
				return new String(newResponse,getSystemCharSet());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace(BurpExtender.getStderr());
				return new String(response);
			}
		}
	}

	public String fetchTitle(byte[] response) {
		String bodyText = covertCharSet(response);

		Pattern p = Pattern.compile("<title(.*?)</title>");
		//<title ng-bind="service.title">The Evolution of the Producer-Consumer Problem in Java - DZone Java</title>
		Matcher m  = p.matcher(bodyText);
		while ( m.find() ) {
			title = m.group(0);
		}
		if (title.equals("")) {
			Pattern ph = Pattern.compile("<title [.*?]>(.*?)</title>");
			Matcher mh  = ph.matcher(bodyText);
			while ( mh.find() ) {
				title = mh.group(0);
			}
		}
		if (title.equals("")) {
			Pattern ph = Pattern.compile("<h[1-6]>(.*?)</h[1-6]>");
			Matcher mh  = ph.matcher(bodyText);
			while ( mh.find() ) {
				title = mh.group(0);
			}
		}
		return title;
	}

	public String getHeaderValueOf(boolean messageIsRequest,String headerName) {
		helpers = BurpExtender.getCallbacks().getHelpers();
		List<String> headers=null;
		if(messageIsRequest) {
			if (this.request == null) {
				return null;
			}
			IRequestInfo analyzeRequest = helpers.analyzeRequest(this.request);
			headers = analyzeRequest.getHeaders();
		}else {
			if (this.response == null) {
				return null;
			}
			IResponseInfo analyzeResponse = helpers.analyzeResponse(this.response);
			headers = analyzeResponse.getHeaders();
		}


		headerName = headerName.toLowerCase().replace(":", "");
		String Header_Spliter = ": ";
		for (String header : headers) {
			if (header.toLowerCase().startsWith(headerName)) {
				return header.split(Header_Spliter, 2)[1];//分成2部分，Location: https://www.jd.com
			}
		}
		return null;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public byte[] getRequest() {
		return request;
	}

	public void setRequest(byte[] request) {
		this.request = request;
	}

	public byte[] getResponse() {
		return response;
	}

	public void setResponse(byte[] response) {
		this.response = response;
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	public String getLevel() {
		return Level;
	}

	public void setLevel(String level) {
		if (level.equalsIgnoreCase(Level_A)
				||level.equalsIgnoreCase(Level_B)
				||level.equalsIgnoreCase(Level_C)) {
			Level = level;
		}
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public IExtensionHelpers getHelpers() {
		return helpers;
	}

	public void setHelpers(IExtensionHelpers helpers) {
		this.helpers = helpers;
	}

	public Object getValue(int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String args[]) {
		LineEntry x = new LineEntry();
		x.setRequest("xxxxxx".getBytes());
		//		System.out.println(yy);
	}
}
