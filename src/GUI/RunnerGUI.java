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
	private ThreadRunner runner;
	private ThreadDirBruter bruter;
	private IHttpRequestResponse messageInfo;
	public JLabel lblStatus;

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
					RunnerGUI frame = new RunnerGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}



	/**
	 * Create the frame.
	 */

	public RunnerGUI() {
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
		SearchTextField textFieldSearch = new SearchTextField("");
		buttonPanel.add(textFieldSearch);

		//搜索按钮
		JButton buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				runnerTable.search(keyword);
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
				runnerTableModel.clear(false);
				if (runner !=null) {
					runner.stopThreads();
				}

				if (bruter != null) {
					bruter.stopThreads();
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

	public void begainRun() {
		runner = new ThreadRunner(this,messageInfo);
		runner.Do();
	}

	public void begainDirBrute() {
		bruter = new ThreadDirBruter(this,messageInfo);
		bruter.Do();
	}
}
