package burp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

public class LineEntry {

	public int id = 0;
	public String url = "";
	public int statuscode = -1;
	public String contentLength = "";
	public String MIMEtype = "";
	public String title = "";
	public String IP = "";
	public String webcontainer = "";
	public String time = "";
	public IHttpRequestResponse messageinfo;
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

	public String getLineJson(){
		parse();
		return JSON.toJSONString(this);
	}

	public void parse() {
		try {
			IResponseInfo responseInfo = helpers.analyzeResponse(messageinfo.getResponse());
			int code = responseInfo.getStatusCode();
			String MIME = responseInfo.getStatedMimeType();
			if(MIME == null) {
				responseInfo.getInferredMimeType();
			}
			this.statuscode = code;
			this.MIMEtype = MIME;
			
			Getter getter = new Getter(helpers);
			String url = this.messageinfo.getHttpService().toString();
			String length = getter.getHeaderValueOf(false, messageinfo, "Content-Length");
			String webContainer = getter.getHeaderValueOf(false, messageinfo, "Server");
			String body = new String(getter.getBody(false, messageinfo));

			String title = "Null";
			Pattern p = Pattern.compile("<title>(.*?)<title>");
			Matcher m  = p.matcher(body);
			while ( m.find() ) {
				title = m.group(0);
			}

			this.url = url;
			this.contentLength = length;
			this.title = title;
			this.webcontainer = webContainer;
		}catch(Exception e) {

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

	public String getContentLength() {
		return contentLength;
	}

	public void setContentLength(String contentLength) {
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

	public Object getValue(int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}
}
