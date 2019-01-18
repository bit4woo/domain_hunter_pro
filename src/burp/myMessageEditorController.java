package burp;

public class myMessageEditorController implements IMessageEditorController{
	private IHttpRequestResponse message;
	
	myMessageEditorController( IHttpRequestResponse message){
		this.message = message;
	}
	
	@Override
	public IHttpService getHttpService() {
		// TODO Auto-generated method stub
		
		return message.getHttpService();
	}

	@Override
	public byte[] getRequest() {
		// TODO Auto-generated method stub
		return message.getRequest();
	}

	@Override
	public byte[] getResponse() {
		// TODO Auto-generated method stub
		return message.getResponse();
	}

}
