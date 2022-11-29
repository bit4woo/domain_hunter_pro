package title;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;

import burp.BurpExtender;
import title.search.SearchTextField;


/**
 * 
 * 为了实现多实例，泛型？抽象出titlepanel的基础功能。 
 *
 */
public class TitlePanelBase extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public JPanel buttonPanel; //最上面的控制面板，搜索框、搜索按钮、状态显示等。
	public SearchTextField textFieldSearch;//自行实现的搜索框
	public JLabel lblSummaryOfTitle;//显示状态信息的label
	public TableAndDetailPanel tableAndDetail;//数据显示表和数据包请求响应显示面板

	PrintWriter stdout;
	PrintWriter stderr;
	public LineTable titleTable;


	public SearchTextField getTextFieldSearch() {
		return textFieldSearch;
	}

	public TableAndDetailPanel getTableAndDetail() {
		return tableAndDetail;
	}

	public JPanel getButtonPanel() {
		return buttonPanel;
	}

	public void setButtonPanel(JPanel buttonPanel) {
		this.buttonPanel = buttonPanel;
	}

	public void setTableAndDetail(TableAndDetailPanel tableAndDetail) {
		this.tableAndDetail = tableAndDetail;
	}

	public void setTextFieldSearch(SearchTextField textFieldSearch) {
		this.textFieldSearch = textFieldSearch;
	}

	public JLabel getLblSummaryOfTitle() {
		return lblSummaryOfTitle;
	}

	public void setLblSummaryOfTitle(JLabel lblSummaryOfTitle) {
		this.lblSummaryOfTitle = lblSummaryOfTitle;
	}


	public TitlePanelBase() {//无参数构造函数，会被子类隐式调用
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));

		buttonPanel = createButtonPanel();
		titleTable = new LineTable(this);
		tableAndDetail = new TableAndDetailPanel(titleTable);
		this.add(buttonPanel, BorderLayout.NORTH);
		this.add(tableAndDetail,BorderLayout.CENTER);
	}

	public JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();

		//搜索按钮
		JButton buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				titleTable.search(keyword);
				//searchHistory.addRecord(keyword);
				digStatus();
			}
		});

		//搜索框
		textFieldSearch = new SearchTextField("",buttonSearch);


		//显示状态信息的label
		lblSummaryOfTitle = new JLabel("^_^");
		buttonPanel.setToolTipText("");


		buttonPanel.add(textFieldSearch);
		buttonPanel.add(buttonSearch);
		buttonPanel.add(lblSummaryOfTitle);

		return buttonPanel;
	}


	/**
	 * 继承类可以再自行更加情况筛选
	 * @param entry
	 * @return
	 */
	public boolean entryNeedToShow(LineEntry entry) {
		return true;
	}

	public void loadData(LineTableModel titleTableModel){

		TableRowSorter<LineTableModel> tableRowSorter = new TableRowSorter<LineTableModel>(titleTableModel);
		titleTable.setRowSorter(tableRowSorter);
		titleTable.setModel(titleTableModel);
		//IndexOutOfBoundsException size为0，为什么会越界？
		//!!!注意：这里必须先setRowSorter，然后再setModel。否则就会出现越界问题。因为当setModel时，会触发数据变更事件，这个时候会调用Sorter。
		// 而这个时候的Sorter中还是旧数据，就会认为按照旧数据的容量去获取数据，从而导致越界。

		//titleTable.setAutoCreateRowSorter(true);//这样应该也可以❎，
		//这里设置后就进行了创建，创建过程会getModel这个时候新model还未设置呢，保险起见不使用这个方式
		//titleTable.setModel(titleTableModel);

		int row = titleTableModel.getLineEntries().size();
		System.out.println(row+" title entries loaded from database file");
		stdout.println(row+" title entries loaded from database file");
		search("");// hide checked items
		titleTable.tableHeaderWidthinit();//设置header宽度
		tableAndDetail.fillViewer();
	}

	/**
	 * 搜索功能，自动获取caseSensitive的值
	 * @param keyword
	 */
	public void search(String keyword) {
		boolean	caseSensitive = textFieldSearch.isCaseSensitive();
		titleTable.search(keyword,caseSensitive);
		digStatus();
	}

	/**
	 * 子类可以实现自己的右键菜单，基础类不创建
	 * 
	 * @return
	 */
	public void showRightClickMenu(MouseEvent e) {

	}


	/**
	 * 子类可以实现自己的右键菜单，基础类不创建
	 * 
	 * @return
	 */
	public void leftDoubleClick(MouseEvent e) {

	}


	public void digStatus() {
		String status = titleTable.getLineTableModel().getStatusSummary();
		lblSummaryOfTitle.setText(status);
	}

	public LineTable getTitleTable() {
		return titleTable;
	}
}
