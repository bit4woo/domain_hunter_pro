package title;

import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;

public class LineMessageInfo implements IHttpRequestResponse {
	LineEntry lineEntry;
	LineMessageInfo(LineEntry entry){
		this.lineEntry = entry; 
	}
	@Override
	public byte[] getRequest() {
		return lineEntry.getRequest();
	}

	@Override
	public void setRequest(byte[] message) {
		
	}

	@Override
	public byte[] getResponse() {
		return lineEntry.getResponse();
	}

	@Override
	public void setResponse(byte[] message) {
		
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public void setComment(String comment) {
		
	}

	@Override
	public String getHighlight() {
		return null;
	}

	@Override
	public void setHighlight(String color) {
		
	}

	@Override
	public IHttpService getHttpService() {
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		IHttpService newHttpService = helpers.buildHttpService(lineEntry.getHost(), lineEntry.getPort(), lineEntry.getProtocol());
		return newHttpService;
	}

	@Override
	public void setHttpService(IHttpService httpService) {
		
	}

}
