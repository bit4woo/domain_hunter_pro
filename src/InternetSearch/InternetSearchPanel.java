package InternetSearch;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IMessageEditor;
import dao.TitleDao;
import title.LineEntry;
import title.LineTable;
import title.LineTableModel;
import title.search.SearchTextField;

public class InternetSearchPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel buttonPanel;
	private JLabel lblSummaryOfTitle;
	private JRadioButton rdbtnUnCheckedItems;
	private JRadioButton rdbtnCheckingItems;
	private JRadioButton rdbtnCheckedItems;
	private JRadioButton rdbtnMoreActionItems;
	private SearchTextField textFieldSearch;

	private IMessageEditor requestViewer;
	private IMessageEditor responseViewer;
	private JSplitPane detailPanel;

	//add table and tablemodel to GUI
	private LineTable searchTable;
	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;
	private JTabbedPane RequestPanel;
	private JTabbedPane ResponsePanel;


	public JTextField getTextFieldSearch() {
		return textFieldSearch;
	}

	public JLabel getLblSummaryOfTitle() {
		return lblSummaryOfTitle;
	}

	public void setLblSummaryOfTitle(JLabel lblSummaryOfTitle) {
		this.lblSummaryOfTitle = lblSummaryOfTitle;
	}

	public JRadioButton getRdbtnUnCheckedItems() {
		return rdbtnUnCheckedItems;
	}

	public void setRdbtnUnCheckedItems(JRadioButton rdbtnUnCheckedItems) {
		this.rdbtnUnCheckedItems = rdbtnUnCheckedItems;
	}

	public JRadioButton getRdbtnCheckingItems() {
		return rdbtnCheckingItems;
	}

	public void setRdbtnCheckingItems(JRadioButton rdbtnCheckingItems) {
		this.rdbtnCheckingItems = rdbtnCheckingItems;
	}

	public JRadioButton getRdbtnCheckedItems() {
		return rdbtnCheckedItems;
	}

	public void setRdbtnCheckedItems(JRadioButton rdbtnCheckedItems) {
		this.rdbtnCheckedItems = rdbtnCheckedItems;
	}

	public JRadioButton getRdbtnMoreActionItems() {
		return rdbtnMoreActionItems;
	}

	public void setRdbtnMoreActionItems(JRadioButton rdbtnMoreActionItems) {
		this.rdbtnMoreActionItems = rdbtnMoreActionItems;
	}

	public IMessageEditor getRequestViewer() {
		return requestViewer;
	}

	public IMessageEditor getResponseViewer() {
		return responseViewer;
	}

	public InternetSearchPanel(GUIMain guiMain) {//构造函数
		this.guiMain = guiMain;
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));
		this.add(createButtonPanel(), BorderLayout.NORTH);


		searchTable = new LineTable(guiMain);
		detailPanel = DetailPanel();

		JSplitPane splitPane = new JSplitPane();//table area + detail area
		splitPane.setResizeWeight(0.5);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		JScrollPane scrollPaneRequests = new JScrollPane(searchTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area

		splitPane.setLeftComponent(scrollPaneRequests);
		splitPane.setRightComponent(detailPanel);
		this.add(splitPane,BorderLayout.CENTER);

	}

	public JPanel createButtonPanel() {
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton buttonSearch = new JButton("Search");
		textFieldSearch = new SearchTextField("",buttonSearch);
		buttonPanel.add(textFieldSearch);

		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				searchTable.search(keyword);
			}
		});
		buttonPanel.add(buttonSearch);

		rdbtnUnCheckedItems = new JRadioButton(LineEntry.CheckStatus_UnChecked);
		rdbtnUnCheckedItems.setSelected(true);
		rdbtnUnCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnUnCheckedItems);

		rdbtnCheckingItems = new JRadioButton(LineEntry.CheckStatus_Checking);
		rdbtnCheckingItems.setSelected(true);
		rdbtnCheckingItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnCheckingItems);

		rdbtnCheckedItems = new JRadioButton(LineEntry.CheckStatus_Checked);
		rdbtnCheckedItems.setSelected(false);
		rdbtnCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnCheckedItems);

		rdbtnMoreActionItems = new JRadioButton(LineEntry.CheckStatus_MoreAction);
		rdbtnMoreActionItems.setSelected(false);
		rdbtnMoreActionItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnMoreActionItems);

		lblSummaryOfTitle = new JLabel("^_^");
		buttonPanel.add(lblSummaryOfTitle);
		buttonPanel.setToolTipText("");

		return buttonPanel;
	}

	public JSplitPane DetailPanel(){

		JSplitPane RequestDetailPanel = new JSplitPane();//request and response
		RequestDetailPanel.setResizeWeight(0.5);

		RequestPanel = new JTabbedPane();
		RequestDetailPanel.setLeftComponent(RequestPanel);

		ResponsePanel = new JTabbedPane();
		RequestDetailPanel.setRightComponent(ResponsePanel);

		return RequestDetailPanel;
	}


	/**
	 * 获取所有明确属于目标范围的域名、IP；排除了黑名单中的内容
	 * 子域名+确定的网段+证书IP-黑名单IP
	 * @return
	 */
	public Set<String> getCertainDomains() {
		Set<String> targetsToReq = new HashSet<String>();
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());
		targetsToReq.addAll(guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet());
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getIPSetOfCert());
		targetsToReq.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		return targetsToReq;
	}

	/**
	 * 获取所有用户自定义输入的域名、IP；排除了黑名单中的内容
	 * @return
	 */
	public Set<String> getCustomDomains() {
		Set<String> targetsToReq = new HashSet<String>();
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets());
		targetsToReq.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		return targetsToReq;
	}



	private void loadData(LineTableModel titleTableModel){

		TableRowSorter<LineTableModel> tableRowSorter = new TableRowSorter<LineTableModel>(titleTableModel);
		searchTable.setRowSorter(tableRowSorter);
		searchTable.setModel(titleTableModel);
		//IndexOutOfBoundsException size为0，为什么会越界？
		//!!!注意：这里必须先setRowSorter，然后再setModel。否则就会出现越界问题。因为当setModel时，会触发数据变更事件，这个时候会调用Sorter。
		// 而这个时候的Sorter中还是旧数据，就会认为按照旧数据的容量去获取数据，从而导致越界。

		//titleTable.setAutoCreateRowSorter(true);//这样应该也可以❎，
		//这里设置后就进行了创建，创建过程会getModel这个时候新model还未设置呢，保险起见不使用这个方式
		//titleTable.setModel(titleTableModel);

		int row = titleTableModel.getLineEntries().size();
		System.out.println(row+" title entries loaded from database file");
		stdout.println(row+" title entries loaded from database file");
		searchTable.search("");// hide checked items
		searchTable.tableHeaderWidthinit();//设置header宽度

		try {
			requestViewer = BurpExtender.getCallbacks().createMessageEditor(searchTable.getLineTableModel(), false);
			responseViewer = BurpExtender.getCallbacks().createMessageEditor(searchTable.getLineTableModel(), false);
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
