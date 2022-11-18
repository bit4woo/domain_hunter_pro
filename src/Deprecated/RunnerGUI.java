package GUI;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import burp.IHttpRequestResponse;
import thread.ThreadBypassGatewayForAll;
import thread.ThreadDirBruter;
import thread.ThreadRunner;
import title.LineTable;
import title.LineTableModel;
import title.search.SearchTextField;

public class RunnerGUI extends JFrame {

	private JScrollPane runnerScrollPaneRequests;
	private JTabbedPane RequestPanel;
	private JTabbedPane ResponsePanel;
	private JPanel RunnerPanel;

	private LineTableModel runnerTableModel = new LineTableModel();
	private LineTable runnerTable = new LineTable(runnerTableModel);
	private IHttpRequestResponse messageInfo;
	public JLabel lblStatus;
	
	private ThreadRunner runner;
	private ThreadDirBruter bruter;
	private ThreadBypassGatewayForAll checker;

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

	public LineTableModel getRunnerTableModel() {
		return runnerTableModel;
	}

	public void setRunnerTableModel(LineTableModel runnerTableModel) {
		this.runnerTableModel = runnerTableModel;
	}

	public LineTable getRunnerTable() {
		return runnerTable;
	}

	public void setRunnerTable(LineTable runnerTable) {
		this.runnerTable = runnerTable;
	}

	public ThreadRunner getRunner() {
		return runner;
	}

	public void setRunner(ThreadRunner runner) {
		this.runner = runner;
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
	public RunnerGUI(IHttpRequestResponse messageInfo) {
		this.messageInfo = messageInfo;
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

		//显示请求和响应的table
		RunnerPanel.add(runnerTable.getTableAndDetailSplitPane(), BorderLayout.CENTER);
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

				if (runner !=null) {
					runner.interrupt();
				}

				if (bruter != null) {
					bruter.interrupt();
				}
				
				if (checker != null) {
					checker.interrupt();
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
	 * 对所有收集到的域名和IP进行Host碰撞
	 */
	public void begainGatewayBypassCheck() {
		checker = new ThreadBypassGatewayForAll(this);
		checker.start();
	}
	
	public void begainRun() {
		runner = new ThreadRunner(this,messageInfo);
		runner.start();
	}
	
	/**
	 * 对一个网关，尝试所有域名的网关绕过测试。（Host碰撞）
	 */
	public void begainRunChangeHostInHeader() {
		runner = new ThreadRunner(this,messageInfo,ThreadRunner.ChangeHostInHeader);
		runner.start();
	}

	public void begainDirBrute() {
		bruter = new ThreadDirBruter(this,messageInfo);
		bruter.start();
	}
}
