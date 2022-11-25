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
	private TitlePanel RunnerPanel;

	private IHttpRequestResponse messageInfo;
	public JLabel lblStatus;
	
	private ThreadDirBruter bruter;
	private LineTableModel runnerTableModel;
	private LineTable runnerTable;

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
		RunnerPanel = new TitlePanel();
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
		
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//if use "EXIT_ON_CLOSE",burp will exit!!
		setVisible(true);
		setTitle("Runner");

		RunnerPanel = new TitlePanel();
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

		this.add(splitPane,BorderLayout.CENTER);

		
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
	
	
	public void begainDirBrute() {
		bruter = new ThreadDirBruter(this,messageInfo);
		bruter.start();
	}
}
