package Tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.EmailUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.JsonUtils;
import com.bit4woo.utilbox.utils.SwingUtils;
import com.bit4woo.utilbox.utils.SystemUtils;
import com.bit4woo.utilbox.utils.TextUtils;
import com.bit4woo.utilbox.utils.UrlUtils;

import GUI.GUIMain;
import base.BackGroundButton;
import burp.BurpExtender;
import burp.ProjectMenu;
import config.ConfigManager;
import config.ConfigName;
import domain.CertInfo;
import title.WebIcon;

/**
 * 所有配置的修改，界面的操作，都立即写入LineConfig对象，如有必要保存到磁盘，再调用一次SaveConfig函数，思路要清晰
 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
 * 保存： 具体各个控件的值---->LineConfig对象---->磁盘文件
 */

public class ToolPanel extends JPanel {

	private JLabel projectLabel;

	PrintWriter stdout;
	PrintWriter stderr;
	public static JTextArea searchResultTextArea;
	public static JTextArea inputTextArea;
	public static JTextArea outputTextArea;

	public boolean inputTextAreaChanged = true;

	String history = "";
	JLabel statusLabel = new JLabel("");


	private GUIMain guiMain;

	public GUIMain getGuiMain() {
		return guiMain;
	}

	public void setGuiMain(GUIMain guiMain) {
		this.guiMain = guiMain;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame frame = new JFrame();
					frame.setVisible(true);
					frame.setContentPane(new ToolPanel(null));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ToolPanel(GUIMain guiMain) {
		this.guiMain = guiMain;
		setForeground(Color.DARK_GRAY);//构造函数
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));

		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		///////////////////////HeaderPanel//////////////

		/*
		JPanel HeaderPanel = new JPanel();
		HeaderPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		this.add(HeaderPanel, BorderLayout.NORTH);

		JLabel lblNewLabelNull = new JLabel("  ");
		HeaderPanel.add(lblNewLabelNull);
		 */


		///////////////////////BodyPane//////////////


		JPanel BodyPane = new JPanel();//中间的大模块，一分为二
		this.add(BodyPane, BorderLayout.CENTER);
		BodyPane.setLayout(new GridLayout(1, 2, 0, 0));

		//JScrollPanelWithHeaderForTool searhResultPanel = new JScrollPanelWithHeaderForTool("Search Result","",true);
		//searchResultTextArea = searhResultPanel.getTextArea();


		JScrollPanelWithHeaderForTool InputPanel = new JScrollPanelWithHeaderForTool("Input", "", true, true);
		inputTextArea = InputPanel.getTextArea();
		inputTextArea.addMouseListener(new TextAreaMouseListener(guiMain, inputTextArea));
		inputTextArea.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				inputTextAreaChanged = true;
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				inputTextAreaChanged = true;
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				inputTextAreaChanged = true;
			}

		});

		InputPanel.getHeadLabel().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) { // 判断是否是双击事件
					// 在双击时执行的操作
					inputTextArea.setText(SuperJTextArea.tempFilePath);
				}
			}
		});

		JScrollPanelWithHeaderForTool OutPanel = new JScrollPanelWithHeaderForTool("OutPut", "", false, false);
		outputTextArea = OutPanel.getTextArea();
		outputTextArea.addMouseListener(new TextAreaMouseListener(guiMain, outputTextArea));

		JPanel buttonPanel = createButtons();

		BodyPane.add(InputPanel);
		BodyPane.add(OutPanel);

		this.add(buttonPanel, BorderLayout.EAST);//这样避免小屏幕按钮显示不完整！
		//BodyPane.add(buttonPanel);


		///////////////////////////FooterPanel//////////////////

		JPanel footerPanel = new JPanel();
		footerPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
		fl_FooterPanel.setAlignment(FlowLayout.LEFT);
		this.add(footerPanel, BorderLayout.SOUTH);

		projectLabel = new JLabel(BurpExtender.getExtenderName() + "    " + BurpExtender.getGithub());
		projectLabel.setFont(new Font("宋体", Font.BOLD, 12));
		projectLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					URI uri = new URI(BurpExtender.getGithub());
					Desktop desktop = Desktop.getDesktop();
					if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace(stderr);
				}

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				projectLabel.setForeground(Color.BLUE);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				projectLabel.setForeground(Color.BLACK);
			}
		});
		footerPanel.add(projectLabel);

	}

	public JPanel createButtons() {

		JButton outputToInput = new BackGroundButton("Input<----Output") {

			@Override
			protected void action() {
				statusLabel.setText("doing");
				String content = outputTextArea.getText();
				if (null != content) {
					inputTextArea.setText(content);
				}
				statusLabel.setText("Done!!!");
			}

		};

		JButton btnFindDomains = new BackGroundButton("Find Domains") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				//stdout.println(content);
				if (null != content) {
					List<String> domains = DomainUtils.grepDomainAndPort(content);
					Collections.sort(domains, new DomainComparator());
					outputTextArea.setText(String.join(System.lineSeparator(), domains));
					guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
				}
			}
		};

		JButton btnFindDomainsNoPort = new BackGroundButton("Find Domains(No Port)") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				//stdout.println(content);
				if (null != content) {
					List<String> domains = DomainUtils.grepDomainNoPort(content);
					Collections.sort(domains, new DomainComparator());
					outputTextArea.setText(String.join(System.lineSeparator(), domains));
					guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
				}
			}
		};
		
		JButton btnGetRootDomain = new BackGroundButton("Get Root Domain") {
			@Override
			protected void action() {
				List<String> lines = SwingUtils.getLinesFromTextArea(inputTextArea);
				List<String> result = new ArrayList<>();
				for (String line:lines) {
					String rootDomain = DomainUtils.getRootDomain(line);
					result.add(rootDomain);
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};


		JButton btnFindUrls = new BackGroundButton("Find URL") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = UrlUtils.grepUrls(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};

		JButton btnFindUrlsWithProtocol = new BackGroundButton("Find URL With Protocol") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = UrlUtils.grepUrlsWithProtocol(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};


		JButton btnFindUrlsInQuotes = new BackGroundButton("Find URL In Quotes('|\")") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = UrlUtils.grepUrlsInQuotes(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};

		JButton btnFindUrlsNotStartWithSlash = new BackGroundButton("Find URL(img/a.png)") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = UrlUtils.grepUrlPathNotStartWithSlash(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};

		JButton btnFindUrlsNotStartWithSlashInQuotes = new BackGroundButton("Find URL(\"img/a.png\")") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = UrlUtils.grepUrlPathNotStartWithSlashInQuotes(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};

		JButton btnCleanUrl = new BackGroundButton("Clean URL") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> lines = SwingUtils.getLinesFromTextArea(inputTextArea);
					List<String> result = new ArrayList<>();

					for (String item : lines) {
						if (UrlUtils.uselessExtension(item)) {
							continue;
						} else {
							result.add(item);
						}
					}//不在使用set方法去重，以便保持去重后的顺序！
					String output = String.join(System.lineSeparator(), result);
					outputTextArea.setText(output);
				}
			}
		};
		
		JButton btnGetBaseUrl = new BackGroundButton("Get Base URL") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> lines = SwingUtils.getLinesFromTextArea(inputTextArea);
					List<String> result = new ArrayList<>();

					for (String item : lines) {
						if (UrlUtils.isVaildUrl(item)) {
							result.add(UrlUtils.getBaseUrl(item));
						}
					}//不在使用set方法去重，以便保持去重后的顺序！
					String output = String.join(System.lineSeparator(), result);
					outputTextArea.setText(output);
				}
			}
		};


		JButton btnFindIP = new BackGroundButton("Find IP") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = IPAddressUtils.grepIPv4NoPort(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};

		JButton btnFindPublicIP = new BackGroundButton("Find Public IP") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = IPAddressUtils.grepPublicIPv4NoPort(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};


		JButton btnFindPrivateIP = new BackGroundButton("Find Private IP") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = IPAddressUtils.grepPrivateIPv4NoPort(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};


		JButton btnFindIPAndPort = new BackGroundButton("Find IP:Port") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = IPAddressUtils.grepIPv4MayPort(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};

		JButton btnFindPort = new BackGroundButton("Find Port") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> result = new ArrayList<String>();
					List<String> lines = TextUtils.textToLines(content);
					for (String line : lines) {
						List<String> portlist = IPAddressUtils.grepPort(line);
						result.addAll(portlist);
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}
		};


		JButton btnMasscanResultToNmap = new BackGroundButton("Masscan->Nmap") {

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (StringUtils.isNotEmpty(content)) {

					List<String> lines = TextUtils.textToLines(content);
					HashMap<String, Set<String>> ipAndPorts = new HashMap<String, Set<String>>();
					List<String> nmapCmds = new ArrayList<String>();
					for (String line : lines) {
						if (line.contains("Discovered open port")) {
							try {
								String port = line.split(" ")[3].split("/")[0];
								String host = line.split(" ")[5];
								Set<String> ports = ipAndPorts.get(host);
								if (ports == null) {
									ports = new HashSet<String>();
								}
								ports.add(port);
								ipAndPorts.put(host, ports);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}

					for (String host : ipAndPorts.keySet()) {
						nmapCmds.add("nmap -v -A -p " + String.join(",", ipAndPorts.get(host)) + " " + host);
					}

					outputTextArea.setText(String.join(System.lineSeparator(), nmapCmds));
				}
			}

		};

		JButton btnMasscanResultToHttp = new BackGroundButton("Masscan->Http") {

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (StringUtils.isNotEmpty(content)) {

					List<String> lines = TextUtils.textToLines(content);
					List<String> result = new ArrayList<String>();
					for (String line : lines) {
						if (line.contains("Discovered open port")) {
							try {
								String port = line.split(" ")[3].split("/")[0];
								String host = line.split(" ")[5];
								result.add("http://" + host + ":" + port);
								result.add("https://" + host + ":" + port);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}

		};


		JButton btnNmapResultToHttp = new BackGroundButton("Nmap->Http") {

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (StringUtils.isNotEmpty(content)) {
					List<String> result = new ArrayList<String>();

					List<String> iplist = IPAddressUtils.grepIPv4NoPort(content);
					List<String> lines = TextUtils.textToLines(content);

					for (String line : lines) {
						if (line.toLowerCase().contains("ssl")) {
							List<String> portlist = IPAddressUtils.grepPort(line);
							for (String port : portlist) {
								for (String host : iplist) {
									result.add("https://" + host + ":" + port);
								}
							}
						} else if (line.toLowerCase().contains("http")) {
							List<String> portlist = IPAddressUtils.grepPort(line);
							for (String port : portlist) {
								for (String host : iplist) {
									result.add("http://" + host + ":" + port);
								}
							}
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}

		};


		JButton btnNmapResultToHttp1 = new BackGroundButton("Nmap->Http 1") {

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (StringUtils.isNotEmpty(content)) {

					List<String> result = new ArrayList<String>();

					List<String> iplist = IPAddressUtils.grepIPv4NoPort(content);
					List<String> portlist = IPAddressUtils.grepPort(content);

					for (String host : iplist) {
						for (String port : portlist) {
							result.add("http://" + host + ":" + port);
							result.add("https://" + host + ":" + port);
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}
		};


		JButton btnFindSubnet = new BackGroundButton("Find Subnet") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> subnets = IPAddressUtils.grepSubnet(content);
					outputTextArea.setText(String.join(System.lineSeparator(), subnets));
				}
			}
		};


		JButton btnFindEmail = new BackGroundButton("Find Email") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> emails = EmailUtils.grepEmail(content);
					outputTextArea.setText(String.join(System.lineSeparator(), emails));
					guiMain.getDomainPanel().getDomainResult().addIfValidEmail(emails);
				}
			}
		};


		JButton btnOpenurls = new BackGroundButton("OpenURLs") {
			List<String> urls = new ArrayList<>();
			Iterator<String> it = urls.iterator();
			private int totalNumber;
			private int left;

			@Override
			protected void action() {
				if (inputTextAreaChanged) {//default is true
					urls = SwingUtils.getLinesFromTextArea(inputTextArea);
					totalNumber = urls.size();
					left = urls.size();
					it = urls.iterator();
					inputTextAreaChanged = false;
				}
				String browserPath = ConfigManager.getStringConfigByKey(ConfigName.BrowserPath);
				try {
					int i = 50;
					while (i > 0 && it.hasNext()) {
						String url = it.next();
						if (!url.toLowerCase().startsWith("https://") &&
								!url.toLowerCase().startsWith("http://")) {
							url = "http://" + url;
							URL tmpUrl = new URL(url);
							if (tmpUrl.getPort() == -1) {
								SystemUtils.browserOpen(url, browserPath);
								SystemUtils.browserOpen(url.replaceFirst("http://", "https://"), browserPath);
							} else if (Integer.toString(tmpUrl.getPort()).endsWith("443")) {
								SystemUtils.browserOpen(url.replaceFirst("http://", "https://"), browserPath);
							} else {
								SystemUtils.browserOpen(url, browserPath);
							}
						} else {
							SystemUtils.browserOpen(url, browserPath);
						}
						i--;
						left--;
					}
					setText("OpenURLs" + "(" + left + "/" + totalNumber + ")");
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton btnCertDomains = new BackGroundButton("GetCertDomains") {
			@Override
			protected void action() {
				ArrayList<String> result = new ArrayList<String>();
				List<String> urls = SwingUtils.getLinesFromTextArea(inputTextArea);
				Iterator<String> it = urls.iterator();
				while (it.hasNext()) {
					String url = it.next();
					Set<String> domains = new CertInfo().getAlternativeDomains(url);
					result.add(url + " " + domains.toString());
					System.out.println(url + " " + domains.toString());
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};


		JButton btnCertTime = new BackGroundButton("GetCertTime") {
			@Override
			protected void action() {
				ArrayList<String> result = new ArrayList<String>();
				List<String> urls = SwingUtils.getLinesFromTextArea(inputTextArea);
				Iterator<String> it = urls.iterator();
				while (it.hasNext()) {
					String url = it.next();
					String time = CertInfo.getCertTime(url);
					result.add(url + " " + time);
					System.out.println(url + " " + time);
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};

		JButton btnCertIssuer = new BackGroundButton("GetCertIssuer") {
			@Override
			protected void action() {
				ArrayList<String> result = new ArrayList<String>();
				List<String> urls = SwingUtils.getLinesFromTextArea(inputTextArea);
				Iterator<String> it = urls.iterator();
				while (it.hasNext()) {
					String url = it.next();
					String time = CertInfo.getCertIssuer(url);
					result.add(url + " " + time);
					System.out.println(url + " " + time);
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};

		JButton iconHashButton = new BackGroundButton("GetIconHash") {
			@Override
			protected void action() {
				try {
					ArrayList<String> result = new ArrayList<String>();
					List<String> urls = SwingUtils.getLinesFromTextArea(inputTextArea);
					Iterator<String> it = urls.iterator();
					while (it.hasNext()) {
						String url = it.next();
						String hash = WebIcon.getHash(url, null);
						result.add(hash);
						System.out.println(url + " " + hash);
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton dnsQueryButton = new BackGroundButton("DNS Query") {
			@Override
			protected void action() {
				try {
					ArrayList<String> result = new ArrayList<String>();
					List<String> domains = SwingUtils.getLinesFromTextArea(inputTextArea);
					Iterator<String> it = domains.iterator();
					while (it.hasNext()) {
						String domain = it.next();
						if (IPAddressUtils.isValidIPv4NoPort(domain)) {//目标是一个IP
							result.add(domain);
						} else if (DomainUtils.isValidDomainNoPort(domain)) {//目标是域名
							HashMap<String, Set<String>> temp = DomainUtils.dnsQuery(domain, null);
							Set<String> IPSet = temp.get("IP");
							result.addAll(IPSet);
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton grepChineseButton = new BackGroundButton("Grep Chinese") {
			@Override
			protected void action() {
				try {
					String content = inputTextArea.getText();
					List<String> result = TextUtils.grepChinese(content);
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton rows2List = new BackGroundButton("Rows To List") {

			@Override
			protected void action() {
				try {
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					outputTextArea.setText(content.toString());
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton rows2Array = new BackGroundButton("Rows To Array") {

			@Override
			protected void action() {
				try {
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					for (int i = 0; i < content.size(); i++) {
						content.set(i, "\"" + content.get(i) + "\"");
					}

					outputTextArea.setText(String.join(",", content));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton removeDuplicate = new BackGroundButton("Deduplicate") {

			@Override
			protected void action() {
				try {
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					List<String> result = new ArrayList<String>();

					for (String item : content) {
						if (result.contains(item)) {
							continue;
						} else {
							result.add(item);
						}
					}//不在使用set方法去重，以便保持去重后的顺序！
					String output = String.join(System.lineSeparator(), result);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton sort = new BackGroundButton("Sort") {

			@Override
			protected void action() {
				try {
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist = new ArrayList<>(contentSet);

					Collections.sort(tmplist);
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton sortReverse = new BackGroundButton("Sort(Reverse Str)") {

			@Override
			protected void action() {
				try {
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist = new ArrayList<>(contentSet);

					Collections.sort(tmplist, new ReverseStrComparator());
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton sortByLength = new BackGroundButton("Sort by Length") {

			@Override
			protected void action() {
				try {
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist = new ArrayList<>(contentSet);

					Collections.sort(tmplist, new LengthComparator());
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton btnGrepJson = new BackGroundButton("Grep Json") {
			@Override
			protected void action() {
				try {
					String content = inputTextArea.getText();
					String toFind = JOptionPane.showInputDialog("to find which value", history);
					if (toFind == null) {
						return;
					} else {
						history = toFind;
						ArrayList<String> result = JsonUtils.grepValueFromJson(content, toFind);
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		};

		JButton btnGrepLine = new BackGroundButton("Grep Line") {
			@Override
			protected void action() {
				try {
					String toFind = JOptionPane.showInputDialog("to find which value", history);
					ArrayList<String> result = new ArrayList<String>();
					if (toFind == null) {
						return;
					} else {
						history = toFind;
						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							if (item.toLowerCase().contains(toFind.toLowerCase().trim())) {
								result.add(item);
							}
						}
						//outputTextArea.setText(result.toString());
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					//e1.printStackTrace(stderr);
				}
			}
		};


		/**
		 * grep line的反面。如果某行数据包含了指定的关键词，就从结果中移除
		 * 即 nagetive search
		 */
		JButton btnRemoveLine = new BackGroundButton("Remove Line") {
			@Override
			protected void action() {
				try {
					String toFind = JOptionPane.showInputDialog("remove lines which contain:", history);
					ArrayList<String> result = new ArrayList<String>();
					if (toFind == null) {
						return;
					} else {
						history = toFind;
						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							if (!item.toLowerCase().contains(toFind.toLowerCase().trim())) {
								result.add(item);
							}
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		};


		JButton btnRegexGrep = new BackGroundButton("Grep Regex") {
			@Override
			protected void action() {
				try {
					String toFind = JOptionPane.showInputDialog("Input Regex", history);

					if (toFind == null) {
						return;
					} else {
						history = toFind;
						ArrayList<String> result = new ArrayList<String>();
						String PATTERN = toFind;
						Pattern pRegex = Pattern.compile(PATTERN);
						String content = inputTextArea.getText();
						Matcher matcher = pRegex.matcher(content);
						while (matcher.find()) {//多次查找
							// 判断是否有捕获组
							if (matcher.groupCount() > 0) {
								// 获取第一个捕获组的匹配结果
								String group1 = matcher.group(1);
								// 将匹配结果添加到列表中
								result.add(group1);
							}else {
								result.add(matcher.group());
							}
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton btnAddPrefix = new BackGroundButton("Add Prefix/Suffix") {
			@Override
			protected void action() {
				try {
					String toAddPrefix = JOptionPane.showInputDialog("prefix to add", null);
					String toAddSuffix = JOptionPane.showInputDialog("suffix to add", null);
					ArrayList<String> result = new ArrayList<String>();
					if (toAddPrefix == null && toAddSuffix == null) {
						return;
					} else {
						if (toAddPrefix == null) {
							toAddPrefix = "";
						}

						if (toAddSuffix == null) {
							toAddSuffix = "";
						}

						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = toAddPrefix + item + toAddSuffix;
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		};


		JButton btnRemovePrefix = new BackGroundButton("Remove Prefix/Suffix") {
			@Override
			protected void action() {
				try {
					String Prefix = JOptionPane.showInputDialog("prefix to remove", null);
					String Suffix = JOptionPane.showInputDialog("suffix to remove", null);
					List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
					List<String> result = TextUtils.removePrefixAndSuffix(content, Prefix, Suffix);
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton btnReplaceFirstStr = new BackGroundButton("ReplaceFirst(Str)") {

			@Override
			protected void action() {
				try {
					String replace = JOptionPane.showInputDialog("string (from)", null);
					String to = JOptionPane.showInputDialog("replacement (to)", null);
					ArrayList<String> result = new ArrayList<String>();
					if (replace == null && to == null) {
						return;
					} else {
						if (replace == null) {
							replace = "";
						}

						if (to == null) {
							to = "";
						}

						replace = Pattern.quote(replace);//输入的内容就完全是普通字符串，不再是正则表达式了
						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = item.replaceFirst(replace, to);
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};

		JButton btnReplaceFirstRegex = new BackGroundButton("ReplaceFirst(Regex)") {

			@Override
			protected void action() {
				try {
					String replace = JOptionPane.showInputDialog("regex (from)", null);
					String to = JOptionPane.showInputDialog("replacement (to)", null);
					ArrayList<String> result = new ArrayList<String>();
					if (replace == null && to == null) {
						return;
					} else {
						if (replace == null) {
							replace = "";
						}

						if (to == null) {
							to = "";
						}

						//replace = Pattern.quote(replace);
						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = item.replaceFirst(replace, to);
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};

		JButton btnReplaceAllStr = new BackGroundButton("ReplaceAll(Str)") {

			@Override
			protected void action() {
				try {
					String replace = JOptionPane.showInputDialog("string (from)", null);
					String to = JOptionPane.showInputDialog("replacement (to)", null);
					ArrayList<String> result = new ArrayList<String>();
					if (replace == null && to == null) {
						return;
					} else {
						if (replace == null) {
							replace = "";
						}

						if (to == null) {
							to = "";
						}

						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = item.replace(replace, to);
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};

		JButton btnReplaceAllRegex = new BackGroundButton("ReplaceAll(Regex)") {

			@Override
			protected void action() {
				try {
					String replace = JOptionPane.showInputDialog("regex (from)", null);
					String to = JOptionPane.showInputDialog("replacement (to)", null);
					ArrayList<String> result = new ArrayList<String>();
					if (replace == null && to == null) {
						return;
					} else {
						if (replace == null) {
							replace = "";
						}

						if (to == null) {
							to = "";
						}

						List<String> content = SwingUtils.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = item.replaceAll(replace, to);
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};



		JButton btnIPsToCIDR = new BackGroundButton("IPs To CIDR") {

			@Override
			protected void action() {
				try {
					List<String> IPs = SwingUtils.getLinesFromTextArea(inputTextArea);
					Set<String> subnets = IPAddressUtils.toSmallerSubNets(new HashSet<String>(IPs));

					List<String> tmplist = new ArrayList<>(subnets);//排序
					Collections.sort(tmplist);

					outputTextArea.setText(String.join(System.lineSeparator(), tmplist));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};

		JButton btnCIDRToIPs = new BackGroundButton("CIDR To IPs") {

			@Override
			protected void action() {
				try {
					List<String> subnets = SwingUtils.getLinesFromTextArea(inputTextArea);
					List<String> IPs = IPAddressUtils.toIPList(subnets);// 当前所有title结果计算出的IP集合
					outputTextArea.setText(String.join(System.lineSeparator(), IPs));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton unescapeJava = new BackGroundButton("UnescapeJava") {

			@Override
			protected void action() {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeJava(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton unescapeHTML = new BackGroundButton("UnescapeHTML") {

			@Override
			protected void action() {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeHtml4(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton ToUnicode = new BackGroundButton("To Unicode") {

			@Override
			protected void action() {
				try {
					outputTextArea.setText(convertToUnicode(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

			public String convertToUnicode(String text) {
				StringBuilder unicodeStringBuilder = new StringBuilder();
				for (char c : text.toCharArray()) {
					unicodeStringBuilder.append(String.format("\\u%04X", (int) c)); // 转换字符为Unicode编码
				}
				return unicodeStringBuilder.toString();
			}
		};

		JButton Base64ToFile = new BackGroundButton("Base64ToFile") {

			@Override
			protected void action() {
				try {
					byte[] payloadBytes = Base64.getDecoder().decode(inputTextArea.getText());
					File downloadFile = saveDialog();
					if (downloadFile != null) {
						FileUtils.writeByteArrayToFile(downloadFile, payloadBytes);
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

			public File saveDialog() {
				try {
					JFileChooser fc = new JFileChooser();
					if (fc.getCurrentDirectory() != null) {
						fc = new JFileChooser(fc.getCurrentDirectory());
					} else {
						fc = new JFileChooser();
					}

					fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

					int action = fc.showSaveDialog(null);

					if (action == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						return file;
					}
					return null;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

		};


		JButton splitButton = new BackGroundButton("Split") {

			@Override
			protected void action() {
				String separator = JOptionPane.showInputDialog("input separator", null);
				if (separator != null) {//&& !separator.trim().equals("")有可能本来就是空格分割，所以不能有这个条件
					String text = inputTextArea.getText();
					String[] items = text.split(separator);
					outputTextArea.setText(String.join(System.lineSeparator(), items));
				}
			}

		};


		JButton combineButton = new BackGroundButton("Combine") {

			@Override
			protected void action() {
				String separator = JOptionPane.showInputDialog("input connect char", null);
				if (separator != null) {// && !separator.trim().equals("")
					List<String> items = SwingUtils.getLinesFromTextArea(inputTextArea);
					outputTextArea.setText(String.join(separator, items));
				}
			}
		};


		JButton toLowerCaseButton = new BackGroundButton("toLowerCase") {

			@Override
			protected void action() {
				outputTextArea.setText(inputTextArea.getText().toLowerCase());
			}
		};


		JButton OpenFileButton = new BackGroundButton("Open File") {

			@Override
			protected void action() {
				String dir = ((SuperJTextArea) inputTextArea).getTextAsDisplay();
				try {
					Desktop.getDesktop().open(new File(dir));
				} catch (IOException e1) {
					e1.printStackTrace(stderr);
					statusLabel.setText("your input is not a valid path or file");
				}
			}
		};

		JButton setRemoveAllButton = new BackGroundButton("Remove All(diff)") {

			@Override
			protected void action() {
				// 创建一个 JTextArea
				JTextArea textArea = new JTextArea(10, 20); // 设置行数和列数
				// 将 JTextArea 放入 JScrollPane 中，以便可以滚动查看
				JScrollPane scrollPane = new JScrollPane(textArea);
				// 显示包含 JTextArea 的对话框
				int result = JOptionPane.showOptionDialog(
						null, // parentComponent
						scrollPane, // message
						"items to remove", // title
						JOptionPane.OK_CANCEL_OPTION, // optionType
						JOptionPane.PLAIN_MESSAGE, // messageType
						null, // icon
						null, // options
						null // initialValue
						);

				// 处理用户输入
				if (result == JOptionPane.OK_OPTION) {
					List<String> itemsToRemove = SwingUtils.getLinesFromTextArea(textArea);
					List<String> items = SwingUtils.getLinesFromTextArea(inputTextArea);
					items.removeAll(itemsToRemove);
					outputTextArea.setText(String.join(System.lineSeparator(), items));
				}
			}
		};


		JButton cartesianProductButton = new BackGroundButton("Cartesian Product") {

			@Override
			protected void action() {
				// 创建一个 JTextArea
				JTextArea textArea = new JTextArea(10, 20); // 设置行数和列数
				// 将 JTextArea 放入 JScrollPane 中，以便可以滚动查看
				JScrollPane scrollPane = new JScrollPane(textArea);
				// 显示包含 JTextArea 的对话框
				int result = JOptionPane.showOptionDialog(
						null, // parentComponent
						scrollPane, // message
						"item list", // title
						JOptionPane.OK_CANCEL_OPTION, // optionType
						JOptionPane.PLAIN_MESSAGE, // messageType
						null, // icon
						null, // options
						null // initialValue
						);

				// 处理用户输入
				if (result == JOptionPane.OK_OPTION) {
					List<String> out = new ArrayList<>();
					List<String> items2 = SwingUtils.getLinesFromTextArea(textArea);
					List<String> items = SwingUtils.getLinesFromTextArea(inputTextArea);
					for (String aa : items) {
						for (String bb : items2) {
							out.add(aa + bb);
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), out));
				}
			}
		};


		JButton testButton = new BackGroundButton("test") {

			@Override
			protected void action() {
				try {
					//outputTextArea.setText(ProjectMenu.listLoadedExtensions());
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};

		JButton trimButton = new BackGroundButton("Trim/Strip") {

			@Override
			protected void action() {
				try {
					ArrayList<String> result = new ArrayList<String>();
					List<String> items = SwingUtils.getLinesFromTextArea(inputTextArea);
					for (String item : items) {
						item = StringUtils.strip(item);
						result.add(item);
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		/*
		JButton JsonSimplify = new BackGroundButton("Simplify Json");

		JsonSimplify.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = ((SuperJTextArea) inputTextArea).getTextAsDisplay();
				try {
					Gson gson = new GsonBuilder().create();
					String json = gson.toJson(text);
					outputTextArea.setText(json);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
					statusLabel.setText("your input is not a valid json");
				}
			}
		});
		 */


		JButton JsonBeautify = new BackGroundButton("Beautify Json") {
			protected void action() {
				String text = ((SuperJTextArea) inputTextArea).getTextAsDisplay();
				try {
					outputTextArea.setText(JsonUtils.pretty(text));
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
					statusLabel.setText("your input is not a valid json");
				}
			}
		};


		//buttonPanel，里面放操作按钮
		JPanel buttonPanel = new JPanel();
		//buttonPanel.setLayout(new GridLayout(10, 1));
		//https://stackoverflow.com/questions/5709690/how-do-i-make-this-flowlayout-wrap-within-its-jsplitpane
		//buttonPanel.setMinimumSize(new Dimension(0, 0));//为了让button自动换行

		GridBagLayout gbl_buttonPanel = new GridBagLayout();
		buttonPanel.setLayout(gbl_buttonPanel);

		//查找提取类
		int rowIndex = 0;
		int cloumnIndex = 0;

		buttonPanel.add(outputToInput, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(statusLabel, new bagLayout(rowIndex, ++cloumnIndex));
		//buttonPanel.add(SearchToInput, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindDomains, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindDomainsNoPort, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnGetRootDomain, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindUrls, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindUrlsWithProtocol, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindUrlsInQuotes, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindUrlsNotStartWithSlash, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindUrlsNotStartWithSlashInQuotes, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCleanUrl, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnGetBaseUrl, new bagLayout(rowIndex, ++cloumnIndex));
		
		cloumnIndex = 0;
		buttonPanel.add(btnFindIP, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindPublicIP, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindPrivateIP, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindSubnet, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindIPAndPort, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindPort, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindEmail, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnMasscanResultToNmap, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnMasscanResultToHttp, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnNmapResultToHttp, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnNmapResultToHttp1, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnGrepJson, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnGrepLine, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnRegexGrep, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnRemoveLine, new bagLayout(rowIndex, ++cloumnIndex));

		//网络请求类
		cloumnIndex = 0;
		buttonPanel.add(btnOpenurls, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(dnsQueryButton, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(grepChineseButton, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnCertDomains, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCertTime, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCertIssuer, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(iconHashButton, new bagLayout(rowIndex, ++cloumnIndex));

		//数据转换类
		cloumnIndex = 0;
		buttonPanel.add(rows2List, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(rows2Array, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnIPsToCIDR, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCIDRToIPs, new bagLayout(rowIndex, ++cloumnIndex));


		cloumnIndex = 0;
		buttonPanel.add(removeDuplicate, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(trimButton, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnReplaceFirstStr, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnReplaceFirstRegex, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnReplaceAllStr, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnReplaceAllRegex, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(sort, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(sortReverse, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(sortByLength, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnAddPrefix, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnRemovePrefix, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(setRemoveAllButton, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(cartesianProductButton, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(unescapeJava, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(unescapeHTML, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(ToUnicode, new bagLayout(rowIndex, ++cloumnIndex));
		//buttonPanel.add(JsonSimplify, new bagLayout(rowIndex, ++cloumnIndex) );
		buttonPanel.add(JsonBeautify, new bagLayout(rowIndex, ++cloumnIndex) );

		cloumnIndex = 0;
		buttonPanel.add(toLowerCaseButton, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(splitButton, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(combineButton, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(Base64ToFile, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(OpenFileButton, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(testButton, new bagLayout(rowIndex, ++cloumnIndex));

		return buttonPanel;
	}

	public static String getContentFromFile(String filename) {
		File tmpfile = new File(filename);
		if (tmpfile.exists() && tmpfile.isFile()) {
			try {
				return FileUtils.readFileToString(tmpfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}


	class bagLayout extends GridBagConstraints {
		/**
		 * 采用普通的行列计数，从1开始
		 *
		 * @param row
		 * @param column
		 */
		bagLayout(int row, int column) {
			this.fill = GridBagConstraints.BOTH;
			this.insets = new Insets(0, 0, 5, 5);
			this.gridx = column - 1;
			this.gridy = row - 1;
		}
	}

}
