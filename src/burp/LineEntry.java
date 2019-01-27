package burp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

public class LineEntry {
	
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
	private String MIMEtype = "";
	private String title = "";
	private String IP = "";
	private String CDN = "";
	private String webcontainer = "";
	private String time = "";
	
	private String messageText = "";//use to search
	private String bodyText = "";//use to adjust the response changed or not
	
	//field for user 
	private boolean isNew =true;
	private boolean isChecked =true;
	private String comment ="";
	
	@JSONField(serialize=false)//表明不序列号该字段,messageinfo对象不能被fastjson成功序列化
	private IHttpRequestResponse messageinfo;
	
	//remove IHttpRequestResponse field ,replace with request+response+httpService(host port protocol). for convert to json.
	
	@JSONField(serialize=false)//表明不序列号该字段
	private BurpExtender burp;
	@JSONField(serialize=false)
	private IExtensionHelpers helpers;
	@JSONField(serialize=false)
	private IBurpExtenderCallbacks callbacks;

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
		
		this.isNew = isNew;
		this.isChecked = Checked;
		this.comment = comment;
	}
	
	public LineEntry(IHttpRequestResponse messageinfo,boolean isNew,boolean Checked,String comment,Set<String> IPset,Set<String> CDNset) {
		this.messageinfo = messageinfo;
		this.callbacks = BurpExtender.getCallbacks();
		this.helpers = this.callbacks.getHelpers();
		parse();
		
		this.isNew = isNew;
		this.isChecked = Checked;
		this.comment = comment;
		this.IP = IPset.toString().replace("[", "").replace("]", "");
		this.CDN = CDNset.toString().replace("[", "").replace("]", "");
	}

	@JSONField(serialize=false)//表明不序列号该字段
	public String ToJson(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSONObject.toJSONString(this);
	}
	
	public LineEntry FromJson(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return JSON.parseObject(json, LineEntry.class);
	}

	public void parse() {
		try {
			IResponseInfo responseInfo = helpers.analyzeResponse(messageinfo.getResponse());
			IHttpService service = this.messageinfo.getHttpService();
			port = service.getPort();
			host = service.getHost();
			protocol = service.getProtocol();
			
			request = messageinfo.getRequest();
			response = messageinfo.getResponse();
			
			Getter getter = new Getter(helpers);
			
			messageText = new String(messageinfo.getRequest())+new String(messageinfo.getResponse());
			
			statuscode = responseInfo.getStatusCode();
			
			MIMEtype = responseInfo.getStatedMimeType();
			if(MIMEtype == null) {
				MIMEtype = responseInfo.getInferredMimeType();
			}
			
			url = this.messageinfo.getHttpService().toString();
			

			webcontainer = getter.getHeaderValueOf(false, messageinfo, "Server");
			
			bodyText = new String(getter.getBody(false, messageinfo));
			
			contentLength = Integer.parseInt(getter.getHeaderValueOf(false, messageinfo, "Content-Length").trim());
			if (contentLength==-1) {
				contentLength = bodyText.length();
			}

			Pattern p = Pattern.compile("<title(.*?)</title>");
			//<title ng-bind="service.title">The Evolution of the Producer-Consumer Problem in Java - DZone Java</title>
			Matcher m  = p.matcher(bodyText);
			while ( m.find() ) {
				title = m.group(0);
			}
			if (title == "") {
				Pattern ph = Pattern.compile("<title [.*?]>(.*?)</title>");
				Matcher mh  = ph.matcher(bodyText);
				while ( mh.find() ) {
					title = mh.group(0);
				}
			}
			if (title == "") {
				Pattern ph = Pattern.compile("<h[1-6]>(.*?)</h[1-6]>");
				Matcher mh  = ph.matcher(bodyText);
				while ( mh.find() ) {
					title = mh.group(0);
				}
			}
			
	        SimpleDateFormat simpleDateFormat = 
                    new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    
	        time = simpleDateFormat.format(new Date());
			
		}catch(Exception e) {
			//e.printStackTrace(burp.stderr);
		}
	}

	public void DoDirBrute() {

	}

	public String getUrl() {
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

	public String getMIMEtype() {
		return MIMEtype;
	}

	public void setMIMEtype(String mIMEtype) {
		MIMEtype = mIMEtype;
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

	public String getCDN() {
		return CDN;
	}

	public void setCDN(String cDN) {
		CDN = cDN;
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

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	
	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}
	
	public IHttpRequestResponse getMessageinfo() {
		return messageinfo;
	}

	public void setMessageinfo(IHttpRequestResponse messageinfo) {
		this.messageinfo = messageinfo;
	}
	
	public String getBodyText() {
		return bodyText;
	}

	public void setBodyText(String bodyText) {
		this.bodyText = bodyText;
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

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Object getValue(int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String args[]) {
		LineEntry x = new LineEntry();
		x.setRequest("xxxxxx".getBytes());
		System.out.println(JSON.toJSON(x));

		System.out.println(JSON.toJSONString(x));
		System.out.println(JSONObject.toJSONString(x));
		System.out.println(JSONObject.toJSON(x));
	}
}
