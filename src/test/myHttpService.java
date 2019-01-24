package test;

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

}
