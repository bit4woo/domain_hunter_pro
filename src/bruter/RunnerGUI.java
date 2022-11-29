package bruter;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintWriter;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import burp.BurpExtender;
import burp.IHttpRequestResponse;
import title.LineTable;
import title.LineTableModel;
import title.TableAndDetailPanel;
import title.TitlePanelBase;

public class RunnerGUI extends JFrame {

	private IHttpRequestResponse messageInfo;
	private TitlePanelBase RunnerPanel;
	private JLabel lblStatus;
	private ThreadDirBruter bruter;

	public IHttpRequestResponse getMessageInfo() {
		return messageInfo;
	}

	public void setMessageInfo(IHttpRequestResponse messageInfo) {
		this.messageInfo = messageInfo;
	}

	public TitlePanelBase getRunnerPanel() {
		return RunnerPanel;
	}

	public void setRunnerPanel(TitlePanelBase runnerPanel) {
		RunnerPanel = runnerPanel;
	}

	public JLabel getLblStatus() {
		return lblStatus;
	}

	public void setLblStatus(JLabel lblStatus) {
		this.lblStatus = lblStatus;
	}

	public ThreadDirBruter getBruter() {
		return bruter;
	}

	public void setBruter(ThreadDirBruter bruter) {
		this.bruter = bruter;
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
		RunnerPanel = new TitlePanelBase();
		RunnerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		RunnerPanel.setLayout(new BorderLayout(0, 0));
		setContentPane(RunnerPanel);//for test

		lblStatus = new JLabel("Status");
		RunnerPanel.add(lblStatus, BorderLayout.NORTH);
	}

	/**
	 * 对一个请求数据包，进行各种变化然后请求，类似Intruder的功能。
	 * 数据源都来自Domain Hunter
	 */
	public RunnerGUI(IHttpRequestResponse messageInfo) {
		this.messageInfo = messageInfo;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//if use "EXIT_ON_CLOSE",burp will exit!!
		setVisible(true);
		setTitle("Runner");

		RunnerPanel = new TitlePanelBase();
		this.add(RunnerPanel,BorderLayout.CENTER);

		
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
					RunnerPanel = null;
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
