package title;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IMessageEditor;
import burp.IPAddressUtils;
import dao.TitleDao;
import thread.ThreadGetSubnet;
import thread.ThreadGetTitleWithForceStop;
import title.search.SearchTextField;


public class TitlePanelBase extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel buttonPanel;
	private SearchTextField textFieldSearch;
	private TableAndDetailPanel tableAndDetail;
	
	//add table and tablemodel to GUI
	private LineTable titleTable;
	private TitleDao titleDao;
	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;
	


	public JTextField getTextFieldSearch() {
		return textFieldSearch;
	}


	public LineTable getTitleTable() {
		return titleTable;
	}

	public TitlePanelBase() {//构造函数
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
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton buttonSearch = new JButton("Search");
		textFieldSearch = new SearchTextField("",buttonSearch);
		buttonPanel.add(textFieldSearch);

		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				search(keyword);
				//searchHistory.addRecord(keyword);
				digStatus();
			}
		});
		buttonPanel.add(buttonSearch);

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


	/**
	 * 用于从DB文件中加载数据，没有去重检查。
	 * 这种加载方式没有改变tableModel，所以tableModelListener也还在。
	 */
	public void loadData(String currentDBFile) {
		titleDao = new TitleDao(currentDBFile);
		List<LineEntry> lines = titleDao.selectAllTitle();
		LineTableModel titleTableModel = new LineTableModel(guiMain, lines);
		loadData(titleTableModel);
	}

	private void loadData(LineTableModel titleTableModel){

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

		try {
			tableAndDetail.setre() = BurpExtender.getCallbacks().createMessageEditor(titleTable.getLineTableModel(), false);
			responseViewer = BurpExtender.getCallbacks().createMessageEditor(titleTable.getLineTableModel(), false);
			tableAndDetail.getRequestPanel().removeAll();
			tableAndDetail.getResponsePanel().removeAll();
			tableAndDetail.getRequestPanel().addTab("Request", requestViewer.getComponent());
			tableAndDetail.getResponsePanel().addTab("Response", responseViewer.getComponent());
		} catch (Exception e) {
			//捕获异常，以便程序以非burp插件运行时可以启动
			//e.printStackTrace();
		}
	}

	/**
	 * 搜索功能，自动获取caseSensitive的值
	 * @param keyword
	 */
	public void search(String keyword) {
		boolean	caseSensitive = textFieldSearch.isCaseSensitive();
		titleTable.search(keyword,caseSensitive);
	}
}
