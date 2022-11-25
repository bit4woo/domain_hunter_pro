package bruter;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowFilter.Entry;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import burp.IHttpRequestResponse;
import title.LineEntry;
import title.LineTable;
import title.LineTableModel;
import title.search.History;
import title.search.LineSearch;
import title.search.SearchDork;
import title.search.SearchTextField;

public class RunnerGUI extends JFrame {

	private JScrollPane runnerScrollPaneRequests;
	private JTabbedPane RequestPanel;
	private JTabbedPane ResponsePanel;
	private JPanel RunnerPanel;

	private IHttpRequestResponse messageInfo;
	public JLabel lblStatus;
	
	private ThreadDirBruter bruter;
	private DefaultTableModel runnerTableModel;
	private JTable runnerTable;

	public JPanel getRunnerPanel() {
		return RunnerPanel;
	}

	public void setRunnerPanel(JPanel runnerPanel) {
		RunnerPanel = runnerPanel;
	}

	public JScrollPane getRunnerScrollPaneRequests() {
		return runnerScrollPaneRequests;
	}

	public void setRunnerScrollPaneRequests(JScrollPane runnerScrollPaneRequests) {
		this.runnerScrollPaneRequests = runnerScrollPaneRequests;
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

	public IHttpRequestResponse getMessageInfo() {
		return messageInfo;
	}

	public void setMessageInfo(IHttpRequestResponse messageInfo) {
		this.messageInfo = messageInfo;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RunnerGUI frame = new RunnerGUI(true);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}



	/**
	 * Create the frame.
	 * 
	 * 独立运行测试
	 */

	public RunnerGUI(boolean Alone) {//传递一个参数以示区分
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setBounds(100, 100, 1000, 500);
		RunnerPanel = new JPanel();
		RunnerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		RunnerPanel.setLayout(new BorderLayout(0, 0));
		setContentPane(RunnerPanel);//for test

		lblStatus = new JLabel("Status");
		RunnerPanel.add(lblStatus, BorderLayout.NORTH);

		//RunnerPanel.add(splitPane, BorderLayout.CENTER);
	}

	/**
	 * 对一个请求数据包，进行各种变化然后请求，类似Intruder的功能。
	 * 数据源都来自Domain Hunter
	 * @param messageInfo
	 */
	public RunnerGUI() {
		runnerTableModel = new DefaultTableModel();
		runnerTable = new JTable(runnerTableModel) {
			@Override
			public void changeSelection(int row, int col, boolean toggle, boolean extend)
			{
				// show the log entry for the selected row
				//LineEntry Entry = this.lineTableModel.getLineEntries().get(super.convertRowIndexToModel(row));
				LineEntry Entry = this.getRowAt(row);
				guiMain.getTitlePanel().getRequestViewer().setMessage(Entry.getRequest(), true);
				guiMain.getTitlePanel().getResponseViewer().setMessage(Entry.getResponse(), false);

				super.changeSelection(row, col, toggle, extend);
			}
			
			/**
			 * 搜索功能，自动获取caseSensitive的值
			 * @param keyword
			 */
			public void search(String keyword) {
				SearchTextField searchTextField = (SearchTextField)guiMain.getTitlePanel().getTextFieldSearch();
				boolean caseSensitive = searchTextField.isCaseSensitive();
				search(keyword,caseSensitive);
			}

			/**
			 * 搜索功能
			 * @param caseSensitive
			 */
			public void search(String Input,boolean caseSensitive) {
				//rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword));
				History.getInstance().addRecord(Input);//记录搜索历史,单例模式

				final RowFilter filter = new RowFilter() {
					@Override
					public boolean include(Entry entry) {
						//entry --- a non-null object that wraps the underlying object from the model
						int row = (int) entry.getIdentifier();
						LineEntry line = getModel().getLineEntries().get(row);

						//第一层判断，根据按钮状态进行判断，如果为true，进行后面的逻辑判断，false直接返回。
						if (!new LineSearch(guiMain.getTitlePanel()).entryNeedToShow(line)) {
							return false;
						}
						//目前只处理&&（and）逻辑的表达式
						if (Input.contains("&&")) {
							String[] searchConditions = Input.split("&&");
							for (String condition:searchConditions) {
								if (oneCondition(condition,line)) {
									continue;
								}else {
									return false;
								}
							}
							return true;
						}else {
							return oneCondition(Input,line);
						}
					}
					public boolean oneCondition(String Input,LineEntry line) {
						Input = Input.trim();//应该去除空格，符合java代码编写习惯
						if (SearchDork.isDork(Input)) {
							//stdout.println("do dork search,dork:"+dork+"   keyword:"+keyword);
							return LineSearch.dorkFilter(line,Input,caseSensitive);
						}else {
							return LineSearch.textFilter(line,Input,caseSensitive);
						}
					}
				};
				((TableRowSorter)LineTable.this.getRowSorter()).setRowFilter(filter);
			}
		};
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//if use "EXIT_ON_CLOSE",burp will exit!!
		setVisible(true);
		setTitle("Runner");

		RunnerPanel = new JPanel();
		RunnerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		RunnerPanel.setLayout(new BorderLayout(0, 0));
		setContentPane(RunnerPanel);//for test

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		RunnerPanel.add(buttonPanel,BorderLayout.NORTH);

		//搜索框
		JButton buttonSearch = new JButton("Search");
		SearchTextField textFieldSearch = new SearchTextField("",buttonSearch);
		buttonPanel.add(textFieldSearch);

		//搜索按钮
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				runnerTable.search(keyword,false);
			}
		});
		buttonPanel.add(buttonSearch);

		lblStatus = new JLabel("Status");
		buttonPanel.add(lblStatus);

		JSplitPane splitPane = new JSplitPane();//table area + detail area
		splitPane.setResizeWeight(0.5);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.add(splitPane,BorderLayout.CENTER);

		JScrollPane scrollPaneRequests = new JScrollPane(runnerTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area
		//显示请求和响应
		JSplitPane detailPanel = DetailPanel();
				
		splitPane.setLeftComponent(scrollPaneRequests);
		splitPane.setRightComponent(detailPanel);
		
		//frame.getRootPane().add(runnerTable.getSplitPane(), BorderLayout.CENTER);
		addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO 关闭多线程
				try{
					runnerTableModel = null;
				}catch (Exception e1){
					e1.printStackTrace();
				}

				if (bruter != null) {
					bruter.interrupt();
				}
			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});
		setBounds(100, 100, 1000, 500);

		//准备工作
	}
	
	/**
	 * 显示请求响应
	 * @return
	 */
	public JSplitPane DetailPanel(){

		JSplitPane RequestDetailPanel = new JSplitPane();//request and response
		RequestDetailPanel.setResizeWeight(0.5);

		RequestPanel = new JTabbedPane();
		RequestDetailPanel.setLeftComponent(RequestPanel);

		ResponsePanel = new JTabbedPane();
		RequestDetailPanel.setRightComponent(ResponsePanel);

		return RequestDetailPanel;
	}
	
	public void begainDirBrute() {
		bruter = new ThreadDirBruter(this,messageInfo);
		bruter.start();
	}
}
