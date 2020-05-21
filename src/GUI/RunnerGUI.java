package GUI;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import burp.IHttpRequestResponse;
import title.LineTable;
import title.LineTableModel;

public class RunnerGUI extends JFrame {

	private JScrollPane runnerScrollPaneRequests;
	private JTabbedPane RequestPanel;
	private JTabbedPane ResponsePanel;
	private JPanel RunnerPanel;
	
	
	private static LineTableModel runnerTableModel = new LineTableModel();
	private static LineTable runnerTable = new LineTable(runnerTableModel);
	private static ThreadRunner runner;
	private static IHttpRequestResponse messageInfo;
	private static String keyword;
	
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

	public static LineTableModel getRunnerTableModel() {
		return runnerTableModel;
	}

	public static void setRunnerTableModel(LineTableModel runnerTableModel) {
		RunnerGUI.runnerTableModel = runnerTableModel;
	}

	public static LineTable getRunnerTable() {
		return runnerTable;
	}

	public static void setRunnerTable(LineTable runnerTable) {
		RunnerGUI.runnerTable = runnerTable;
	}

	public static ThreadRunner getRunner() {
		return runner;
	}

	public static void setRunner(ThreadRunner runner) {
		RunnerGUI.runner = runner;
	}

	public static IHttpRequestResponse getMessageInfo() {
		return messageInfo;
	}

	public static void setMessageInfo(IHttpRequestResponse messageInfo) {
		RunnerGUI.messageInfo = messageInfo;
	}

	public static String getKeyword() {
		return keyword;
	}

	public static void setKeyword(String keyword) {
		RunnerGUI.keyword = keyword;
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

	
	public String getKeywordFromUI() {
		String responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
		while(responseKeyword.trim().equals("")){
			responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
		}
		responseKeyword = responseKeyword.trim();
		this.keyword = responseKeyword;
		return keyword;
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
		
		//RunnerPanel.add(splitPane, BorderLayout.CENTER);
	}
	
	
	public RunnerGUI(IHttpRequestResponse messageInfo) {
		getKeywordFromUI();
		this.messageInfo = messageInfo;
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//if use "EXIT_ON_CLOSE",burp will exit!!
		setVisible(true);
		setTitle("Runner");
		
		RunnerPanel = new JPanel();
		RunnerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		RunnerPanel.setLayout(new BorderLayout(0, 0));
		setContentPane(RunnerPanel);//for test
		
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
				runner.stopThreads();
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
		
		begainRun();
	}
	
	public void begainRun() {
		runner = new ThreadRunner(messageInfo);
		runner.Do();
	}
}
