package burp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

public class LineEntry {
	
	//int has some different from int
	private String url = "";
	private int statuscode = -1;
	private int contentLength = -1;
	private String MIMEtype = "";
	private String title = "";
	private String IP = "";
	private String webcontainer = "";
	private String time = "";
	private String messageText = "";//use to search
	private String bodyText = "";//use to adjust the response changed or not
	private boolean isNew =true;
	
	@JSONField(serialize=false)//表明不序列号该字段,messageinfo对象不能被fastjson成功序列化
	private IHttpRequestResponse messageinfo;
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
	}
	
	public LineEntry(IHttpRequestResponse messageinfo,boolean isNew) {
		this.messageinfo = messageinfo;
		this.callbacks = BurpExtender.getCallbacks();
		this.helpers = this.callbacks.getHelpers();
	}

	public String getLineJson(){
		parse();
		return JSONObject.toJSONString(this);
	}

	public void parse() {
		try {
			IResponseInfo responseInfo = helpers.analyzeResponse(messageinfo.getResponse());
			Getter getter = new Getter(helpers);
			
			messageText = new String(messageinfo.getRequest())+new String(messageinfo.getResponse());
			
			statuscode = responseInfo.getStatusCode();
			
			MIMEtype = responseInfo.getStatedMimeType();
			if(MIMEtype == null) {
				MIMEtype = responseInfo.getInferredMimeType();
			}
			
			url = this.messageinfo.getHttpService().toString();
			
			contentLength = Integer.parseInt(getter.getHeaderValueOf(false, messageinfo, "Content-Length").trim());
			
			webcontainer = getter.getHeaderValueOf(false, messageinfo, "Server");
			
			String body = new String(getter.getBody(false, messageinfo));
			
			bodyText = messageinfo.getHttpService().toString()+body;

			Pattern p = Pattern.compile(">(.*?)</title>");
			//<title ng-bind="service.title">The Evolution of the Producer-Consumer Problem in Java - DZone Java</title>
			Matcher m  = p.matcher(body);
			while ( m.find() ) {
				title = m.group(0);
			}
			if (title != "") {
				title = title.replace("</title>", "").replaceAll(">", "");
			}
			if (title == "") {
				Pattern ph = Pattern.compile(">(.*?)</h[1-6]>");
				Matcher mh  = ph.matcher(body);
				while ( mh.find() ) {
					title = mh.group(0);
				}
			}
			
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

	public Object getValue(int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String args[]) {
		
	}
}
