package test;

import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpService;

public class myHttpService implements IHttpService {
	String host="";
	String protocol = "";
	int port = -1;
	
	myHttpService(){
		
	}
	
	myHttpService(IHttpService service){
		host=service.getHost();
		protocol = service.getProtocol();
		port = service.getPort();
	}
	
	public void setHost(String host) {
		this.host = host;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	public static void main(String args[]) {
		IExtensionHelpers helpers = BurpExtender.callbacks.getHelpers();
		IHttpService http = helpers.buildHttpService("www.jd.com",80,"http");
		IHttpService https = helpers.buildHttpService("www.jd.com",443,"https");
		System.out.println(http);
		System.out.println(https);
	}
}
