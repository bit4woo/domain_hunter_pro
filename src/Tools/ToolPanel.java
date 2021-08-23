package Tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import GUI.GUI;
import burp.BurpExtender;
import burp.Commons;
import domain.CertInfo;
import domain.DomainProducer;
import title.WebIcon;

/*
 * 所有配置的修改，界面的操作，都立即写入LineConfig对象，如有必要保存到磁盘，再调用一次SaveConfig函数，思路要清晰
 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
 * 保存： 具体各个控件的值---->LineConfig对象---->磁盘文件
 */

public class ToolPanel extends JPanel {

	private JLabel lblNewLabel_2;

	private volatile boolean listenerIsOn = true;
	PrintWriter stdout;
	PrintWriter stderr;
	private static JTextField BrowserPath;
	public static JTextField PortList;
	public static JTextArea inputTextArea;
	public static JTextArea outputTextArea;

	public boolean inputTextAreaChanged = true;
	public static JRadioButton showItemsInOne;
	private static LineConfig lineConfig;
	public static JRadioButton ignoreHTTPS;
	public static JRadioButton ignoreHTTPStaus500;
	public static JRadioButton ignoreHTTPStaus400;
	public static JRadioButton ignoreWrongCAHost;
	public static JRadioButton DisplayContextMenuOfBurp;
	public static JRadioButton rdbtnSaveTrafficTo;
	public static JTextField textFieldPortScanner;
	public static JTextField textFieldDirSearch;
	public static JTextField textFieldDirBruteDict;
	public static JTextField textFieldPython;
	public static JTextField textFieldElasticURL;
	public static JTextField textFieldElasticUserPass;
	public static JTextField textFieldUploadApiToken;

	public static LineConfig getLineConfig() {
		return lineConfig;
	}

	/**
	 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
	 * 注意对监听器的影响
	 */
	public void loadConfigToGUI(String projectConfigFile) {
		BurpExtender.getStdout().println("Loading Tool Panel Config From Disk");
//		String content = BurpExtender.getCallbacks().loadExtensionSetting(BurpExtender.Extension_Setting_Name_Line_Config);
//		if (content == null) {
//			lineConfig = LineConfig.loadFromDisk();
//		}else {
//			lineConfig = LineConfig.FromJson(content);
//		}
		if (projectConfigFile == null) {
			lineConfig = new LineConfig();
		}else {
			lineConfig = LineConfig.loadFromDisk(projectConfigFile);//projectConfigFile可能为null
		}
		
		if (lineConfig == null) {
			BurpExtender.getStdout().println("Loading From Disk Failed, Use Default");
			lineConfig = new LineConfig();
		}
		
		String dbFilePath = lineConfig.getDbfilepath();
		
		if (dbFilePath != null && dbFilePath.endsWith(".db")) {
			GUI.LoadData(dbFilePath);
		}
		//这里的修改也会触发textFieldListener监听器。
		//由于我们是多个组件共用一个保存逻辑，当前对一个组件设置值的时候，触发保存，从而导致整体数据的修改！！！
		//所以和domain和title中一样，显示数据时关闭监听器。
		listenerIsOn = false;
		inputTextArea.setText(lineConfig.getToolPanelText());

		BrowserPath.setText(lineConfig.getBrowserPath());

		if (!lineConfig.getNmapPath().contains("{host}")) {//兼容新旧版本，
			lineConfig.setNmapPath(LineConfig.defaultNmap);
		}
		textFieldPortScanner.setText(lineConfig.getNmapPath());
		textFieldDirSearch.setText(lineConfig.getDirSearchPath());
		textFieldPython.setText(lineConfig.getPython3Path());
		textFieldDirBruteDict.setText(lineConfig.getBruteDict());
		textFieldElasticURL.setText(lineConfig.getElasticApiUrl());
		textFieldElasticUserPass.setText(lineConfig.getElasticUsernameAndPassword());
		textFieldUploadApiToken.setText(lineConfig.getUploadApiToken());

		showItemsInOne.setSelected(lineConfig.isShowItemsInOne());
		rdbtnSaveTrafficTo.setSelected(lineConfig.isEnableElastic());
		listenerIsOn = true;//显示完毕后打开监听器。
	}


	public static void saveToConfigFromGUI() {
		lineConfig.setBrowserPath(BrowserPath.getText());
		lineConfig.setDirSearchPath(textFieldDirSearch.getText());
		lineConfig.setBruteDict(textFieldDirBruteDict.getText());
		lineConfig.setPython3Path(textFieldPython.getText());
		lineConfig.setNmapPath(textFieldPortScanner.getText());
		lineConfig.setElasticApiUrl(textFieldElasticURL.getText().trim());
		lineConfig.setElasticUsernameAndPassword(textFieldElasticUserPass.getText());
		lineConfig.setUploadApiToken(textFieldUploadApiToken.getText());

		lineConfig.setToolPanelText(inputTextArea.getText());
		lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
		lineConfig.setEnableElastic(rdbtnSaveTrafficTo.isSelected());
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
					frame.setContentPane(new ToolPanel());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ToolPanel() {
		setForeground(Color.DARK_GRAY);//构造函数
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
		HeaderPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		this.add(HeaderPanel, BorderLayout.NORTH);

		JLabel lblNewLabelNull = new JLabel("  ");
		HeaderPanel.add(lblNewLabelNull);

		JButton outputToInput = new JButton("Input<----Output");
		HeaderPanel.add(outputToInput);
		outputToInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String content = outputTextArea.getText();
				if (null != content) {
					inputTextArea.setText(content);
				}
			}
		});

		//第一次分割，中间的大模块一分为二
		JSplitPane CenterSplitPane = new JSplitPane();//中间的大模块，一分为二
		CenterSplitPane.setResizeWeight(0.5);
		this.add(CenterSplitPane, BorderLayout.CENTER);

		//第二次分割：二分为四
		JSplitPane LeftOfCenter = new JSplitPane();
		LeftOfCenter.setResizeWeight(0.5);
		CenterSplitPane.setLeftComponent(LeftOfCenter);

		JSplitPane RightOfCenter = new JSplitPane();
		RightOfCenter.setOrientation(JSplitPane.VERTICAL_SPLIT);
		RightOfCenter.setResizeWeight(0.5);
		CenterSplitPane.setRightComponent(RightOfCenter);

		//  1/4
		JScrollPane oneFourthPanel = new JScrollPane();
		LeftOfCenter.setLeftComponent(oneFourthPanel);
		//  2/4
		JScrollPane twoFourthPanel = new JScrollPane();
		LeftOfCenter.setRightComponent(twoFourthPanel);

		inputTextArea = new JTextArea();
		inputTextArea.setColumns(20);
		inputTextArea.setLineWrap(true);
		inputTextArea.getDocument().addDocumentListener(new textAreaListener());
		oneFourthPanel.setViewportView(inputTextArea);

		outputTextArea = new JTextArea();
		outputTextArea.setLineWrap(true);
		twoFourthPanel.setViewportView(outputTextArea);


		//四分之三部分放一个panel，里面放操作按钮
		JPanel threeFourthPanel = new JPanel();
		RightOfCenter.setLeftComponent(threeFourthPanel);
		threeFourthPanel.setLayout(new FlowLayout());
		//https://stackoverflow.com/questions/5709690/how-do-i-make-this-flowlayout-wrap-within-its-jsplitpane
		threeFourthPanel.setMinimumSize(new Dimension(0, 0));//为了让button自动换行

		JButton btnFindDomains = new JButton("Find Domains");
		threeFourthPanel.add(btnFindDomains);
		btnFindDomains.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String content = inputTextArea.getText();
				if (null != content) {
					Set<String> domains = DomainProducer.grepDomain(content);
					ArrayList<String> tmpList = new ArrayList<String>(domains);
					Collections.sort(tmpList,new DomainComparator());
					outputTextArea.setText(String.join(System.lineSeparator(), tmpList));
					BurpExtender.liveAnalysisTread.classifyDomains(domains);
				}
			}
		});

		JButton btnFindUrls = new JButton("Find URLs");
		btnFindUrls.setToolTipText("only find entire URL");
		threeFourthPanel.add(btnFindUrls);
		btnFindUrls.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = DomainProducer.grepURL(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		});

		JButton btnFindIP = new JButton("Find IP");
		threeFourthPanel.add(btnFindIP);
		btnFindIP.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = DomainProducer.grepIP(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		});

		JButton btnFindIPAndPort = new JButton("Find IP:Port");
		threeFourthPanel.add(btnFindIPAndPort);
		btnFindIPAndPort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = DomainProducer.grepIPAndPort(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		});

		JButton btnOpenurls = new JButton("OpenURLs");
		threeFourthPanel.add(btnOpenurls);
		btnOpenurls.addActionListener(new ActionListener() {
			List<String> urls = new ArrayList<>();
			Iterator<String> it = urls.iterator();

			@Override
			public void actionPerformed(ActionEvent e) {
				if (inputTextAreaChanged) {//default is true
					urls = Arrays.asList(lineConfig.getToolPanelText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n"));
					it = urls.iterator();
					inputTextAreaChanged = false;
				}
				try {
					int i =10;
					while(i>0 && it.hasNext()) {
						String url = it.next();
						//stdout.println(url);
						Commons.browserOpen(url, lineConfig.getBrowserPath());
						i--;
					}
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}

		});
		
		JButton btnCertDomains = new JButton("GetCertDomains");
		btnCertDomains.setToolTipText("get Alter Domains of Cert");
		threeFourthPanel.add(btnCertDomains);
		btnCertDomains.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						ArrayList<String> result = new ArrayList<String>();
						List<String> urls = Arrays.asList(lineConfig.getToolPanelText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n"));
						Iterator<String> it = urls.iterator();
						while(it.hasNext()) {
							String url = it.next();
							Set<String> domains = CertInfo.getAlternativeDomains(url);
							result.add(url+" "+domains.toString());
							System.out.println(url+" "+domains.toString());
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
						return null;
					}
					@Override
					protected void done() {
						btnCertDomains.setEnabled(true);
						stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
					}
				};
				worker.execute();
			}
		});

		JButton btnCertTime = new JButton("GetCertTime");
		btnCertTime.setToolTipText("get out-of-service time of Cert");
		threeFourthPanel.add(btnCertTime);
		btnCertTime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						ArrayList<String> result = new ArrayList<String>();
						List<String> urls = Arrays.asList(lineConfig.getToolPanelText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n"));
						Iterator<String> it = urls.iterator();
						while(it.hasNext()) {
							String url = it.next();
							String time = CertInfo.getCertTime(url);
							result.add(url+" "+time);
							System.out.println(url+" "+time);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
						return null;
					}
					@Override
					protected void done() {
						btnCertTime.setEnabled(true);
						stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
					}
				};
				worker.execute();
			}
		});
		
		JButton btnCertIssuer = new JButton("GetCertIssuer");
		btnCertIssuer.setToolTipText("get issuer of Cert");
		threeFourthPanel.add(btnCertIssuer);
		btnCertIssuer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						ArrayList<String> result = new ArrayList<String>();
						List<String> urls = Arrays.asList(lineConfig.getToolPanelText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n"));
						Iterator<String> it = urls.iterator();
						while(it.hasNext()) {
							String url = it.next();
							String time = CertInfo.getCertIssuer(url);
							result.add(url+" "+time);
							System.out.println(url+" "+time);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
						return null;
					}
					@Override
					protected void done() {
						btnCertIssuer.setEnabled(true);
						stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
					}
				};
				worker.execute();
			}
		});
		
		JButton iconHashButton = new JButton("GetIconHash");
		threeFourthPanel.add(iconHashButton);
		iconHashButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<String> result = new ArrayList<String>();
				
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						try {
							List<String> urls = Arrays.asList(lineConfig.getToolPanelText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n"));
							Iterator<String> it = urls.iterator();
							while(it.hasNext()) {
								String url = it.next();
								String hash = WebIcon.getHash(url);
								result.add(hash);
								System.out.println(url+" "+hash);
							}
							outputTextArea.setText(String.join(System.lineSeparator(), result));
						} catch (Exception e1) {
							outputTextArea.setText(e1.getMessage());
							e1.printStackTrace(stderr);
						}
						return null;
					}
					@Override
					protected void done() {
					}
				};
				worker.execute();
			}
		});

		JButton rows2List = new JButton("Rows To List");
		threeFourthPanel.add(rows2List);
		rows2List.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);					
					outputTextArea.setText(content.toString());
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		});

		JButton rows2Array = new JButton("Rows To Array");
		threeFourthPanel.add(rows2Array);
		rows2Array.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					for (int i=0;i<content.size();i++) {
						content.set(i, "\""+content.get(i)+"\"");
					}

					outputTextArea.setText(String.join(",", content));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		});

		JButton removeDuplicate = new JButton("Remove Duplicate");
		threeFourthPanel.add(removeDuplicate);
		removeDuplicate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist= new ArrayList<>(contentSet);

					Collections.sort(tmplist);
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		});

		JButton sortByLength = new JButton("Sort by Length");
		threeFourthPanel.add(sortByLength);
		sortByLength.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist= new ArrayList<>(contentSet);

					Collections.sort(tmplist,new LengthComparator());
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		});


		JButton btnGrep = new JButton("Grep Json");
		threeFourthPanel.add(btnGrep);
		btnGrep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String content = inputTextArea.getText();
					String toFind = JOptionPane.showInputDialog("to find which value", null);
					if (toFind == null) {
						return;
					} else {
						//stdout.println(content);
						ArrayList<String> result = JSONHandler.grepValueFromJson(content, toFind);
						//								stdout.println("##################Result of Grep JSON##################");
						//								stdout.println(result.toString());
						//								stdout.println("##################Result of Grep JSON##################");
						//								stdout.println();

						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					//e1.printStackTrace(stderr);
				}
			}

		});

		JButton btnLine = new JButton("Grep Line");
		threeFourthPanel.add(btnLine);
		btnLine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String toFind = JOptionPane.showInputDialog("to find which value", null);
					ArrayList<String> result = new ArrayList<String>();
					if (toFind == null) {
						return;
					} else {
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item:content) {
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
		});

		JButton btnRegexGrep = new JButton("Regex Grep");
		threeFourthPanel.add(btnRegexGrep);
		btnRegexGrep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String toFind = JOptionPane.showInputDialog("Input Regex", null);
					if (toFind == null) {
						return;
					} else {
						ArrayList<String> result = new ArrayList<String>();
						String PATTERN = toFind;
						Pattern pRegex = Pattern.compile(PATTERN);
						String content = inputTextArea.getText();
						Matcher matcher = pRegex.matcher(content);
						while (matcher.find()) {//多次查找
							result.add(matcher.group());
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JButton btnAddPrefix = new JButton("Add Prefix/Suffix");
		threeFourthPanel.add(btnAddPrefix);
		btnAddPrefix.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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

						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item:content) {
							item = toAddPrefix+item+toAddSuffix;
							result.add(item); 
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		});

		JButton btnRemovePrefix = new JButton("Remove Prefix/Suffix");
		threeFourthPanel.add(btnRemovePrefix);
		btnRemovePrefix.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String Prefix = JOptionPane.showInputDialog("prefix to remove", null);
					String Suffix = JOptionPane.showInputDialog("suffix to remove", null);
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					List<String> result = Commons.removePrefixAndSuffix(content,Prefix,Suffix);
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});


		JButton btnReplace = new JButton("ReplaceFirstStr");
		threeFourthPanel.add(btnReplace);
		btnReplace.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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

						replace = Pattern.quote(replace);
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item:content) {
							item = item.replaceFirst(replace,to);
							result.add(item); 
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});


		JButton btnIPsToCIDR = new JButton("IPs To CIDR");
		threeFourthPanel.add(btnIPsToCIDR);
		btnIPsToCIDR.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> IPs = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> subnets = Commons.toSmallerSubNets(new HashSet<String>(IPs));
					outputTextArea.setText(String.join(System.lineSeparator(), subnets));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JButton btnCIDRToIPs = new JButton("CIDR To IPs");
		threeFourthPanel.add(btnCIDRToIPs);
		btnCIDRToIPs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> subnets = Commons.getLinesFromTextArea(inputTextArea);
					List<String> IPs = Commons.toIPList(subnets);// 当前所有title结果计算出的IP集合
					outputTextArea.setText(String.join(System.lineSeparator(), IPs));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JButton unescapeJava = new JButton("UnescapeJava");
		threeFourthPanel.add(unescapeJava);
		unescapeJava.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeJava(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JButton unescapeHTML = new JButton("UnescapeHTML");
		threeFourthPanel.add(unescapeHTML);
		unescapeHTML.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeHtml4(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JButton Base64ToFile = new JButton("Base64ToFile");
		threeFourthPanel.add(Base64ToFile);
		Base64ToFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					byte[] payloadBytes = Base64.getDecoder().decode(inputTextArea.getText());
					File downloadFile = saveDialog();
					if (downloadFile!= null) {
						FileUtils.writeByteArrayToFile(downloadFile, payloadBytes);
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

			public File saveDialog() {
				try {
					JFileChooser fc =  new JFileChooser();
					if (fc.getCurrentDirectory() != null) {
						fc = new JFileChooser(fc.getCurrentDirectory());
					}else {
						fc = new JFileChooser();
					}

					fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

					int action = fc.showSaveDialog(null);

					if(action==JFileChooser.APPROVE_OPTION){
						File file=fc.getSelectedFile();
						return file;
					}
					return null;
				}catch (Exception e){
					e.printStackTrace();
					return null;
				}
			}
		});

		JButton splitButton = new JButton("Split");
		threeFourthPanel.add(splitButton);
		splitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String separator = JOptionPane.showInputDialog("input separator", null);
				if (separator != null) {//&& !separator.trim().equals("")有可能本来就是空格分割，所以不能有这个条件
					String text = inputTextArea.getText();
					String[] items = text.split(separator);
					outputTextArea.setText(String.join(System.lineSeparator(), items));
				}
			}
		});

		JButton combineButton = new JButton("Combine");
		threeFourthPanel.add(combineButton);
		combineButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String separator = JOptionPane.showInputDialog("input connect char", null);
				if (separator != null) {// && !separator.trim().equals("")
					List<String> items = Commons.getLinesFromTextArea(inputTextArea);
					outputTextArea.setText(String.join(separator, items));
				}
			}
		});

		JButton toLowerCaseButton = new JButton("toLowerCase");
		threeFourthPanel.add(toLowerCaseButton);
		toLowerCaseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				outputTextArea.setText(inputTextArea.getText().toLowerCase());
			}
		});
		
		
		JButton testButton = new JButton("test");
		threeFourthPanel.add(testButton);
		testButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						try {
							outputTextArea.setText(WebIcon.getHash(inputTextArea.getText()));
						} catch (Exception e1) {
							outputTextArea.setText(e1.getMessage());
							e1.printStackTrace(stderr);
						}
						return null;
					}
					@Override
					protected void done() {
					}
				};
				worker.execute();
			}
		});
		
		///////
		JPanel fourFourthPanel = new JPanel();
		RightOfCenter.setRightComponent(fourFourthPanel);
		GridBagLayout gbl_fourFourthPanel = new GridBagLayout();
		gbl_fourFourthPanel.columnWidths = new int[]{215, 215, 0};
		gbl_fourFourthPanel.rowHeights = new int[]{27, 0, 0, 0, 27, 0, 0, 0, 0, 0, 27, 27, 27, 27, 0, 0, 0, 0};
		gbl_fourFourthPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_fourFourthPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		fourFourthPanel.setLayout(gbl_fourFourthPanel);

		JLabel lblNewLabel = new JLabel("Browser Path:");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.BOTH;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		fourFourthPanel.add(lblNewLabel, gbc_lblNewLabel);
		BrowserPath = new JTextField();
		GridBagConstraints gbc_BrowserPath = new GridBagConstraints();
		gbc_BrowserPath.fill = GridBagConstraints.BOTH;
		gbc_BrowserPath.insets = new Insets(0, 0, 5, 0);
		gbc_BrowserPath.gridx = 1;
		gbc_BrowserPath.gridy = 0;
		fourFourthPanel.add(BrowserPath, gbc_BrowserPath);
		BrowserPath.setColumns(50);
		BrowserPath.getDocument().addDocumentListener(new textFieldListener());

		JLabel lblPortScanner = new JLabel("PortScanner Path:");
		lblPortScanner.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					textFieldPortScanner.setText(LineConfig.defaultNmap);
				}
			}
		});
		GridBagConstraints gbc_lblPortScanner = new GridBagConstraints();
		gbc_lblPortScanner.anchor = GridBagConstraints.WEST;
		gbc_lblPortScanner.insets = new Insets(0, 0, 5, 5);
		gbc_lblPortScanner.gridx = 0;
		gbc_lblPortScanner.gridy = 1;
		fourFourthPanel.add(lblPortScanner, gbc_lblPortScanner);

		textFieldPortScanner = new JTextField();
		textFieldPortScanner.setColumns(50);
		textFieldPortScanner.getDocument().addDocumentListener(new textFieldListener());
		GridBagConstraints gbc_textFieldPortScanner = new GridBagConstraints();
		gbc_textFieldPortScanner.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPortScanner.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPortScanner.gridx = 1;
		gbc_textFieldPortScanner.gridy = 1;
		fourFourthPanel.add(textFieldPortScanner, gbc_textFieldPortScanner);

		JLabel lblPythonPath = new JLabel("Python3 Path:");
		GridBagConstraints gbc_lblPythonPath = new GridBagConstraints();
		gbc_lblPythonPath.anchor = GridBagConstraints.WEST;
		gbc_lblPythonPath.insets = new Insets(0, 0, 5, 5);
		gbc_lblPythonPath.gridx = 0;
		gbc_lblPythonPath.gridy = 2;
		fourFourthPanel.add(lblPythonPath, gbc_lblPythonPath);

		textFieldPython = new JTextField();
		textFieldPython.setColumns(50);
		GridBagConstraints gbc_textFieldPython = new GridBagConstraints();
		gbc_textFieldPython.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPython.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPython.gridx = 1;
		gbc_textFieldPython.gridy = 2;
		fourFourthPanel.add(textFieldPython, gbc_textFieldPython);

		JLabel lblDirSearch = new JLabel("DirSearch Path:");
		GridBagConstraints gbc_lblDirSearch = new GridBagConstraints();
		gbc_lblDirSearch.anchor = GridBagConstraints.WEST;
		gbc_lblDirSearch.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirSearch.gridx = 0;
		gbc_lblDirSearch.gridy = 3;
		fourFourthPanel.add(lblDirSearch, gbc_lblDirSearch);

		textFieldDirSearch = new JTextField();
		textFieldDirSearch.setColumns(50);
		textFieldDirSearch.getDocument().addDocumentListener(new textFieldListener());
		GridBagConstraints gbc_textFieldDirSearch = new GridBagConstraints();
		gbc_textFieldDirSearch.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldDirSearch.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldDirSearch.gridx = 1;
		gbc_textFieldDirSearch.gridy = 3;
		fourFourthPanel.add(textFieldDirSearch, gbc_textFieldDirSearch);


		JLabel lblExternalPorts = new JLabel("External Port:");
		GridBagConstraints gbc_lblExternalPorts = new GridBagConstraints();
		gbc_lblExternalPorts.fill = GridBagConstraints.BOTH;
		gbc_lblExternalPorts.insets = new Insets(0, 0, 5, 5);
		gbc_lblExternalPorts.gridx = 0;
		gbc_lblExternalPorts.gridy = 4;
		fourFourthPanel.add(lblExternalPorts, gbc_lblExternalPorts);
		PortList = new JTextField();
		GridBagConstraints gbc_PortList = new GridBagConstraints();
		gbc_PortList.fill = GridBagConstraints.BOTH;
		gbc_PortList.insets = new Insets(0, 0, 5, 0);
		gbc_PortList.gridx = 1;
		gbc_PortList.gridy = 4;
		fourFourthPanel.add(PortList, gbc_PortList);
		PortList.setColumns(50);
		PortList.setToolTipText("eg.: 8080,8088");

		JLabel lblDirBruteDict = new JLabel("Dir Brute Dict:");
		GridBagConstraints gbc_lblDirBruteDict = new GridBagConstraints();
		gbc_lblDirBruteDict.anchor = GridBagConstraints.WEST;
		gbc_lblDirBruteDict.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirBruteDict.gridx = 0;
		gbc_lblDirBruteDict.gridy = 5;
		fourFourthPanel.add(lblDirBruteDict, gbc_lblDirBruteDict);

		textFieldDirBruteDict = new JTextField();
		textFieldDirBruteDict.setToolTipText("path of dict");
		textFieldDirBruteDict.setColumns(50);
		textFieldDirBruteDict.getDocument().addDocumentListener(new textFieldListener());
		GridBagConstraints gbc_textFieldDirBruteDict = new GridBagConstraints();
		gbc_textFieldDirBruteDict.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldDirBruteDict.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldDirBruteDict.gridx = 1;
		gbc_textFieldDirBruteDict.gridy = 5;
		fourFourthPanel.add(textFieldDirBruteDict, gbc_textFieldDirBruteDict);

		JLabel lblElasticURL = new JLabel("Elastic URL:");
		GridBagConstraints gbc_lblElasticURL = new GridBagConstraints();
		gbc_lblElasticURL.anchor = GridBagConstraints.WEST;
		gbc_lblElasticURL.insets = new Insets(0, 0, 5, 5);
		gbc_lblElasticURL.gridx = 0;
		gbc_lblElasticURL.gridy = 6;
		fourFourthPanel.add(lblElasticURL, gbc_lblElasticURL);

		textFieldElasticURL = new JTextField();
		textFieldElasticURL.setToolTipText("URL of elastic API");
		textFieldElasticURL.setText("http://10.12.72.55:9200/");
		textFieldElasticURL.getDocument().addDocumentListener(new textFieldListener());
		//textFieldElasticURL.addFocusListener(new JTextFieldHintListener(textFieldElasticURL,"http://10.12.72.55:9200/"));
		//这个HintListener的操作，会触发DocumentListener的insertUpdate操作！
		textFieldElasticURL.setColumns(50);
		GridBagConstraints gbc_textFieldElasticURL = new GridBagConstraints();
		gbc_textFieldElasticURL.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldElasticURL.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldElasticURL.gridx = 1;
		gbc_textFieldElasticURL.gridy = 6;
		fourFourthPanel.add(textFieldElasticURL, gbc_textFieldElasticURL);

		JLabel lblDirElasticUserPass = new JLabel("Elastic Username Password:");
		GridBagConstraints gbc_lblDirElasticUserPass = new GridBagConstraints();
		gbc_lblDirElasticUserPass.anchor = GridBagConstraints.WEST;
		gbc_lblDirElasticUserPass.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirElasticUserPass.gridx = 0;
		gbc_lblDirElasticUserPass.gridy = 7;
		fourFourthPanel.add(lblDirElasticUserPass, gbc_lblDirElasticUserPass);

		textFieldElasticUserPass = new JTextField();
		textFieldElasticUserPass.setText("elastic:changeme");
		textFieldElasticUserPass.setToolTipText("username and password of elastic API");
		textFieldElasticUserPass.getDocument().addDocumentListener(new textFieldListener());
		//textFieldElasticUserPass.addFocusListener(new JTextFieldHintListener(textFieldElasticUserPass,"elastic:changeme"));
		textFieldElasticUserPass.setColumns(50);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 7;
		fourFourthPanel.add(textFieldElasticUserPass, gbc_textField_1);

		JLabel lblUploadAPIToken = new JLabel("Upload API Token:");
		GridBagConstraints gbc_lblUploadAPIToken = new GridBagConstraints();
		gbc_lblUploadAPIToken.anchor = GridBagConstraints.WEST;
		gbc_lblUploadAPIToken.insets = new Insets(0, 0, 5, 5);
		gbc_lblUploadAPIToken.gridx = 0;
		gbc_lblUploadAPIToken.gridy = 8;
		fourFourthPanel.add(lblUploadAPIToken, gbc_lblUploadAPIToken);

		textFieldUploadApiToken = new JTextField();
		textFieldUploadApiToken.setToolTipText("token of upload api");
		textFieldUploadApiToken.getDocument().addDocumentListener(new textFieldListener());
		textFieldUploadApiToken.setColumns(50);
		GridBagConstraints gbc_textFieldUploadApiToken = new GridBagConstraints();
		gbc_textFieldUploadApiToken.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldUploadApiToken.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldUploadApiToken.gridx = 1;
		gbc_textFieldUploadApiToken.gridy = 8;
		fourFourthPanel.add(textFieldUploadApiToken, gbc_textFieldUploadApiToken);

		DisplayContextMenuOfBurp = new JRadioButton("Display Context Menu Of Burp");
		DisplayContextMenuOfBurp.setSelected(true);
		GridBagConstraints gbc_DisplayContextMenuOfBurp = new GridBagConstraints();
		gbc_DisplayContextMenuOfBurp.insets = new Insets(0, 0, 5, 0);
		gbc_DisplayContextMenuOfBurp.fill = GridBagConstraints.BOTH;
		gbc_DisplayContextMenuOfBurp.gridx = 1;
		gbc_DisplayContextMenuOfBurp.gridy = 9;
		fourFourthPanel.add(DisplayContextMenuOfBurp, gbc_DisplayContextMenuOfBurp);

		JLabel label_1 = new JLabel("");
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.fill = GridBagConstraints.BOTH;
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 0;
		gbc_label_1.gridy = 10;
		fourFourthPanel.add(label_1, gbc_label_1);


		showItemsInOne = new JRadioButton("Display Context Menu Items In One");
		GridBagConstraints gbc_showItemsInOne = new GridBagConstraints();
		gbc_showItemsInOne.fill = GridBagConstraints.BOTH;
		gbc_showItemsInOne.insets = new Insets(0, 0, 5, 0);
		gbc_showItemsInOne.gridx = 1;
		gbc_showItemsInOne.gridy = 10;
		fourFourthPanel.add(showItemsInOne, gbc_showItemsInOne);
		showItemsInOne.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
			}
		});

		JLabel label_2 = new JLabel("");
		GridBagConstraints gbc_label_2 = new GridBagConstraints();
		gbc_label_2.fill = GridBagConstraints.BOTH;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 0;
		gbc_label_2.gridy = 11;
		fourFourthPanel.add(label_2, gbc_label_2);

		ignoreHTTPS = new JRadioButton("Ignore HTTPS if HTTP is OK || Ignore HTTP if HTTPS is OK");
		GridBagConstraints gbc_ignoreHTTPS = new GridBagConstraints();
		gbc_ignoreHTTPS.fill = GridBagConstraints.BOTH;
		gbc_ignoreHTTPS.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreHTTPS.gridx = 1;
		gbc_ignoreHTTPS.gridy = 11;
		fourFourthPanel.add(ignoreHTTPS, gbc_ignoreHTTPS);
		ignoreHTTPS.setSelected(true);

		JLabel label_3 = new JLabel("");
		GridBagConstraints gbc_label_3 = new GridBagConstraints();
		gbc_label_3.fill = GridBagConstraints.BOTH;
		gbc_label_3.insets = new Insets(0, 0, 5, 5);
		gbc_label_3.gridx = 0;
		gbc_label_3.gridy = 12;
		fourFourthPanel.add(label_3, gbc_label_3);

		ignoreHTTPStaus500 = new JRadioButton("Ignore items which Status >= 500");
		GridBagConstraints gbc_ignoreHTTPStaus500 = new GridBagConstraints();
		gbc_ignoreHTTPStaus500.fill = GridBagConstraints.BOTH;
		gbc_ignoreHTTPStaus500.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreHTTPStaus500.gridx = 1;
		gbc_ignoreHTTPStaus500.gridy = 12;
		fourFourthPanel.add(ignoreHTTPStaus500, gbc_ignoreHTTPStaus500);
		ignoreHTTPStaus500.setSelected(true);

		JLabel label_4 = new JLabel("");
		GridBagConstraints gbc_label_4 = new GridBagConstraints();
		gbc_label_4.fill = GridBagConstraints.BOTH;
		gbc_label_4.insets = new Insets(0, 0, 5, 5);
		gbc_label_4.gridx = 0;
		gbc_label_4.gridy = 13;
		fourFourthPanel.add(label_4, gbc_label_4);

		ignoreHTTPStaus400 = new JRadioButton("Ignore http Status 400(The plain HTTP request was sent to HTTPS port)");
		GridBagConstraints gbc_ignoreHTTPStaus400 = new GridBagConstraints();
		gbc_ignoreHTTPStaus400.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreHTTPStaus400.fill = GridBagConstraints.BOTH;
		gbc_ignoreHTTPStaus400.gridx = 1;
		gbc_ignoreHTTPStaus400.gridy = 13;
		fourFourthPanel.add(ignoreHTTPStaus400, gbc_ignoreHTTPStaus400);
		ignoreHTTPStaus400.setSelected(true);

		JLabel label_5 = new JLabel("");
		GridBagConstraints gbc_label_5 = new GridBagConstraints();
		gbc_label_5.insets = new Insets(0, 0, 5, 5);
		gbc_label_5.gridx = 0;
		gbc_label_5.gridy = 14;
		fourFourthPanel.add(label_5, gbc_label_5);

		ignoreWrongCAHost = new JRadioButton("Ignore Host that IP Address and Certificate Authority not match");
		ignoreWrongCAHost.setSelected(false);
		GridBagConstraints gbc_ignoreWrongCAHost = new GridBagConstraints();
		gbc_ignoreWrongCAHost.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreWrongCAHost.fill = GridBagConstraints.BOTH;
		gbc_ignoreWrongCAHost.gridx = 1;
		gbc_ignoreWrongCAHost.gridy = 14;
		fourFourthPanel.add(ignoreWrongCAHost, gbc_ignoreWrongCAHost);

		rdbtnSaveTrafficTo = new JRadioButton("Save traffic to Elastic");
		rdbtnSaveTrafficTo.setSelected(false);
		GridBagConstraints gbc_rdbtnSaveTrafficTo = new GridBagConstraints();
		gbc_rdbtnSaveTrafficTo.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSaveTrafficTo.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnSaveTrafficTo.gridx = 1;
		gbc_rdbtnSaveTrafficTo.gridy = 15;
		fourFourthPanel.add(rdbtnSaveTrafficTo, gbc_rdbtnSaveTrafficTo);

		JLabel label_6 = new JLabel("");
		GridBagConstraints gbc_label_6 = new GridBagConstraints();
		gbc_label_6.insets = new Insets(0, 0, 0, 5);
		gbc_label_6.gridx = 0;
		gbc_label_6.gridy = 16;
		fourFourthPanel.add(label_6, gbc_label_6);

		///////////////////////////FooterPanel//////////////////


		JPanel footerPanel = new JPanel();
		footerPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
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

	public static HashSet<String> getExternalPortSet(){
		String input = PortList.getText();
		HashSet<String> result = new HashSet<String>();
		for (String item:input.split(",")) {
			item = item.trim();
			try {
				Integer.parseInt(item);
				result.add(item);
			}catch(Exception e) {

			}
		}
		return result;
	}

	//保存文本框的数据
	class textAreaListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				lineConfig.setToolPanelText(inputTextArea.getText());
				inputTextAreaChanged = true;
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				lineConfig.setToolPanelText(inputTextArea.getText());
				inputTextAreaChanged = true;
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			if (listenerIsOn) {
				lineConfig.setToolPanelText(inputTextArea.getText());
				inputTextAreaChanged = true;
			}
		}
	}

	//保存各个路径设置参数，自动保存的listener
	class textFieldListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				saveToConfigFromGUI();
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				saveToConfigFromGUI();
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			if (listenerIsOn) {
				saveToConfigFromGUI();
			}
		}
	}

}
