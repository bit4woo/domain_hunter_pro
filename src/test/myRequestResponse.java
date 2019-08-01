package test;

import burp.IHttpRequestResponse;
import burp.IHttpService;

public class myRequestResponse implements IHttpRequestResponse  {

	private byte[] request;
	private byte[] response;
	private String comment;
	private String highlight;
	private myHttpService HttpService;
	
	myRequestResponse(){
		
	}
	
	myRequestResponse(IHttpRequestResponse messageinfo){
		
	}

	@Override
	public byte[] getRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRequest(byte[] message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getResponse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setResponse(byte[] message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setComment(String comment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getHighlight() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHighlight(String color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IHttpService getHttpService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHttpService(IHttpService httpService) {
		// TODO Auto-generated method stub
		
	}



	public void setHttpService(myHttpService httpService) {
		HttpService = httpService;
	}
	
	public static void main(String args[]) {
//		myRequestResponse x =new myRequestResponse();
//		x.setRequest("xxxxx".getBytes());
//		x.setResponse("yyy".getBytes());
//		x.setHttpService((IHttpService)new myHttpService());
//		System.out.println(new Gson().toJson(x));
	}
}
