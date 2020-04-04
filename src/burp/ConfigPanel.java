package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;


public class ConfigPanel extends JPanel {

	private JLabel lblNewLabel_2;

	private boolean listenerIsOn = true;
	PrintWriter stdout;
	PrintWriter stderr;
	private JTextField BrowserPath;
	private JTextArea URLS;

	public ConfigPanel() {//构造函数
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		///////////////////////HeaderPanel//////////////


		JPanel HeaderPanel = new JPanel();
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		this.add(HeaderPanel, BorderLayout.NORTH);
		
		JLabel lblNewLabel = new JLabel("Browser Path:");
		HeaderPanel.add(lblNewLabel);
		
		BrowserPath = new JTextField();
		HeaderPanel.add(BrowserPath);
		BrowserPath.setColumns(50);
		BrowserPath.setText(BurpExtender.getLineConfig().getBrowserPath());
		BrowserPath.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
            	File browser = new File(BrowserPath.getText().trim());
            	if (browser.exists()) {
            		BurpExtender.getLineConfig().setBrowserPath(browser.getAbsolutePath());
            		BurpExtender.getCallbacks().saveExtensionSetting("domain_hunter_pro",BurpExtender.getLineConfig().ToJson());
            	}
            }
            @Override
            public void mouseEntered(MouseEvent e) {
 
            }
		});


		//第一次分割
		JSplitPane CenterSplitPane = new JSplitPane();//中间的大模块，一分为二
		CenterSplitPane.setResizeWeight(0.5);
		this.add(CenterSplitPane, BorderLayout.CENTER);
		
		URLS = new JTextArea();
		CenterSplitPane.setLeftComponent(URLS);
		URLS.setColumns(10);
		
		JButton btnOpenurls = new JButton("OpenURLs");
		btnOpenurls.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					for (String url:getSetFromTextArea(URLS)) {
						Commons.browserOpen(url, BurpExtender.getLineConfig().getBrowserPath());
					}
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
			
		});
		CenterSplitPane.setRightComponent(btnOpenurls);


		/*
		 * //第二次分割，左边 JSplitPane leftOfCenterSplitPane = new
		 * JSplitPane();//放入左边的分区。再讲左边的分区一分为二
		 * leftOfCenterSplitPane.setResizeWeight(0.2);
		 * CenterSplitPane.setLeftComponent(leftOfCenterSplitPane);
		 * 
		 * //第二次分割，右边 JSplitPane rightOfCenterSplitPane = new JSplitPane();//放入右半部分分区，
		 * rightOfCenterSplitPane.setResizeWeight(0.7);
		 * CenterSplitPane.setRightComponent(rightOfCenterSplitPane);
		 */


		///////////////////////////FooterPanel//////////////////


		JPanel footerPanel = new JPanel();
		FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
		fl_FooterPanel.setAlignment(FlowLayout.LEFT);
		this.add(footerPanel, BorderLayout.SOUTH);

		lblNewLabel_2 = new JLabel(BurpExtender.getExtenderName()+"    "+BurpExtender.getGithub());
		lblNewLabel_2.setFont(new Font("宋体", Font.BOLD, 12));
		lblNewLabel_2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					URI uri = new URI(BurpExtender.getGithub());
					Desktop desktop = Desktop.getDesktop();
					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace(stderr);
				}

			}
			@Override
			public void mouseEntered(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLUE);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLACK);
			}
		});
		footerPanel.add(lblNewLabel_2);
	}

	public static Set<String> getSetFromTextArea(JTextArea textarea){
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n")));
		domainList.remove("");
		return domainList;
	}
}
