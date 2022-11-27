package title;

import java.io.PrintWriter;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import burp.BurpExtender;
import burp.IMessageEditor;

/**
 * 抽离出【table+数据包显示面板】的模块，以便可以在runner中使用
 *
 */
public class TableAndDetailPanel extends JSplitPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PrintWriter stdout;
	PrintWriter stderr;

	private LineTable titleTable;

	private JSplitPane detailPanel;

	private JTabbedPane RequestPanel;
	private JTabbedPane ResponsePanel;

	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;


	public IMessageEditor getRequestViewer() {
		return requestViewer;
	}

	public IMessageEditor getResponseViewer() {
		return responseViewer;
	}

	public void setRequestViewer(IMessageEditor requestViewer) {
		this.requestViewer = requestViewer;
	}

	public void setResponseViewer(IMessageEditor responseViewer) {
		this.responseViewer = responseViewer;
	}

	public JTabbedPane getRequestPanel() {
		return RequestPanel;
	}

	public void setRequestPanel(JTabbedPane requestPanel) {
		RequestPanel = requestPanel;
	}

	public JTabbedPane getResponsePanel() {
		return ResponsePanel;
	}

	public void setResponsePanel(JTabbedPane responsePanel) {
		ResponsePanel = responsePanel;
	}

	public LineTable getTitleTable() {
		return titleTable;
	}

	public void setTitleTable(LineTable titleTable) {
		this.titleTable = titleTable;
	}

	public TableAndDetailPanel(LineTable titleTable) {//构造函数
		this.titleTable = titleTable;
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		setResizeWeight(0.5);
		setOrientation(JSplitPane.VERTICAL_SPLIT);

		//Table部分
		JScrollPane scrollPaneRequests = new JScrollPane(titleTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area
		//显示数据包详情的部分
		detailPanel = CreateDetailPanel();

		setLeftComponent(scrollPaneRequests);
		setRightComponent(detailPanel);
	}

	public JSplitPane CreateDetailPanel(){

		JSplitPane RequestDetailPanel = new JSplitPane();//request and response
		RequestDetailPanel.setResizeWeight(0.5);

		RequestPanel = new JTabbedPane();
		RequestDetailPanel.setLeftComponent(RequestPanel);

		ResponsePanel = new JTabbedPane();
		RequestDetailPanel.setRightComponent(ResponsePanel);

		return RequestDetailPanel;
	}

	/**
	 * 设置请求响应数据包的viewer
	 * 当table的model是一个新对象时，需要调用这个函数
	 */
	public void fillViewer(){
		try {
			requestViewer = BurpExtender.getCallbacks().createMessageEditor(titleTable.getLineTableModel(), false);
			responseViewer = BurpExtender.getCallbacks().createMessageEditor(titleTable.getLineTableModel(), false);

			RequestPanel.removeAll();
			ResponsePanel.removeAll();

			RequestPanel.addTab("Request", requestViewer.getComponent());
			ResponsePanel.addTab("Response", responseViewer.getComponent());
		} catch (Exception e) {
			//捕获异常，以便程序以非burp插件运行时可以启动
			//e.printStackTrace();
		}
	}
}
