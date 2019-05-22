package test;

import javax.swing.JSplitPane;

import burp.BurpExtender;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IMessageEditor;
import burp.IMessageEditorController;

public class MessageViewPanel extends JSplitPane implements IMessageEditorController {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IHttpRequestResponse currentlyDisplayedItem;
	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;
	/**
	 * Create the panel.
	 */
	public MessageViewPanel(BurpExtender burp) {
		requestViewer = BurpExtender.getCallbacks().createMessageEditor(this, false);
		responseViewer = BurpExtender.getCallbacks().createMessageEditor(this, false);
        this.setLeftComponent(requestViewer.getComponent());
        this.setRightComponent(responseViewer.getComponent());
        this.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.setResizeWeight(0.5);
	}

	@Override
	public IHttpService getHttpService() {
		return currentlyDisplayedItem.getHttpService();
	}

	@Override
	public byte[] getRequest() {
		return currentlyDisplayedItem.getRequest();
	}

	@Override
	public byte[] getResponse() {
		return currentlyDisplayedItem.getResponse();
	}
	
	public IHttpRequestResponse getCurrentlyDisplayedItem() {
		return currentlyDisplayedItem;
	}

	public void setCurrentlyDisplayedItem(IHttpRequestResponse currentlyDisplayedItem) {
		requestViewer.setMessage(currentlyDisplayedItem.getRequest(), true);
		responseViewer.setMessage(currentlyDisplayedItem.getResponse(), false);
		this.currentlyDisplayedItem = currentlyDisplayedItem;
	}
}
