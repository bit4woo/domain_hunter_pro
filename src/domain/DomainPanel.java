package domain;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;

import GUI.GUIMain;
import GUI.ProjectMenu;
import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.DBHelper;
import burp.IBurpExtenderCallbacks;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IScanIssue;
import domain.target.TargetControlPanel;
import domain.target.TargetEntry;
import domain.target.TargetTable;
import domain.target.TargetTableModel;
import thread.ThreadSearhDomain;
import toElastic.VMP;

/*
 *注意，所有直接对DomainObject中数据的修改，都不会触发该tableChanged监听器。
 *1、除非操作的逻辑中包含了firexxxx来主动通知监听器。比如DomainPanel.domainTableModel.fireTableChanged(null);
 *2、或者主动调用显示和保存的函数直接完成，不经过监听器。
	//GUI.getDomainPanel().showToDomainUI();
	//DomainPanel.autoSave();
 */
public class DomainPanel extends JPanel {

	private JRadioButton rdbtnAddRelatedToRoot;
	private JTextField textFieldUploadURL;
	private JButton btnSearch;
	private JButton btnCrawl;
	private JLabel lblSummary;

	private JTextArea textAreaFoundIPs;
	private JTextArea textAreaSubdomains;
	private JTextArea textAreaSimilarDomains;
	private JTextArea textAreaRelatedDomains;
	private JTextArea textAreaEmails;
	private JTextArea textAreaPackages;
	//private JTextArea textAreaSubnets;

	private static TargetTable targetTable;
	private static JPanel HeaderPanel;

	//流量分析进程需要用到这个变量，标记为volatile以获取正确的值。
	/**
	 * DomainPanel对应DomainManager
	 * TargetTable对应TargetTableModel
	 * 
	 * targetTable可以从DomainPanel中获取，是GUI的线路关系
	 * targetTableModel则对应地从DomainManager中的对象获取，是数据模型的线路关系
	 */
	private volatile static DomainManager domainResult = new DomainManager();//getter setter
	private PrintWriter stdout;
	private PrintWriter stderr;

	private static boolean listenerIsOn = true;
	private static final Logger log = LogManager.getLogger(DomainPanel.class);


	public static boolean isListenerIsOn() {
		return listenerIsOn;
	}

	public static void setListenerIsOn(boolean listenerIsOn) {
		DomainPanel.listenerIsOn = listenerIsOn;
	}

	public static DomainManager getDomainResult() {
		return domainResult;
	}

	public static void setDomainResult(DomainManager domainResult) {
		DomainPanel.domainResult = domainResult;
	}

	public static TargetTable getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(TargetTable targetTable) {
		this.targetTable = targetTable;
	}

	public static TargetTableModel fetchTargetModel() {
		return targetTable.getTargetModel();
	}

	//	public static TargetTableModel getTargetTableModel() {
	//		return targetTableModel;
	//	}
	//
	//	public static void setTargetTableModel(TargetTableModel targetTableModel) {
	//		DomainPanel.targetTableModel = targetTableModel;
	//	}

	public static void createOrOpenDB() {
		Object[] options = { "Create","Open"};
		int user_input = JOptionPane.showOptionDialog(null, "You should Create or Open a DB file", "Chose Your Action",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		if (user_input == 0) {
			ProjectMenu.createNewDb(BurpExtender.getGui());
		}
		if (user_input == 1) {
			ProjectMenu.openDb();
		}
	}

	public DomainPanel() {//构造函数
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


		HeaderPanel = new JPanel();
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		this.add(HeaderPanel, BorderLayout.NORTH);

		JButton btnSaveDomainOnly = new JButton("SaveDomainOnly");
		btnSaveDomainOnly.setToolTipText("Only save data in Domain Panel");
		btnSaveDomainOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveDomainOnly();
			}
		});
		HeaderPanel.add(btnSaveDomainOnly);

		/*
		JButton test = new JButton("test");
		test.setToolTipText("Only save data in Domain Panel");
		test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUI.getProjectMenu().changeTabName("");
			}});
		HeaderPanel.add(test);

		 */

		/*
		btnBrute = new JButton("Brute");
		btnBrute.setToolTipText("Do Brute for all root domains");
		btnBrute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.

					@Override
					protected Map doInBackground() throws Exception {

						Set<String> rootDomains = domainResult.fetchRootDomainSet();
						Set<String> keywords= domainResult.fetchKeywordSet();

						//stderr.print(keywords.size());
						//System.out.println(rootDomains.toString());
						//System.out.println("xxx"+keywords.toString());
						btnBrute.setEnabled(false);
						//						threadBruteDomain = new ThreadBruteDomain(rootDomains);
						//						threadBruteDomain.Do();
						for (String rootDomain: rootDomains){
							threadBruteDomain2 = new ThreadBruteDomainWithDNSServer2(rootDomain);
							threadBruteDomain2.Do();
						}


						return null;
					}
					@Override
					protected void done() {
						try {
							get();
							showToDomainUI();
							autoSave();
							btnBrute.setEnabled(true);
							stdout.println("~~~~~~~~~~~~~brute Done~~~~~~~~~~~~~");
						} catch (Exception e) {
							btnBrute.setEnabled(true);
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});

		Component verticalStrut = Box.createVerticalStrut(20);
		HeaderPanel.add(verticalStrut);
		//HeaderPanel.add(btnBrute);
		 */


		btnSearch = new JButton("Search");
		btnSearch.setToolTipText("Do a single search from site map");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.
					@Override
					protected Map doInBackground() throws Exception {
						Set<String> rootDomains = fetchTargetModel().fetchTargetDomainSet();
						Set<String> keywords = fetchTargetModel().fetchKeywordSet();

						//stderr.print(keywords.size());
						//System.out.println(rootDomains.toString());
						//System.out.println("xxx"+keywords.toString());
						btnSearch.setEnabled(false);
						domainResult.getEmailSet().addAll(collectEmails());
						return search(rootDomains, keywords);
					}

					@Override
					protected void done() {
						try {
							get();
							showDataToDomainGUI();
							saveDomainDataToDB();
							btnSearch.setEnabled(true);
						} catch (Exception e) {
							btnSearch.setEnabled(true);
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});

		HeaderPanel.add(btnSearch);

		btnCrawl = new JButton("Crawl");
		btnCrawl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//可以在一个类中实现另一个类，直接实现原始类，没有变量处理的困扰；
					//之前的想法是先单独实现一个worker类，在它里面处理各种，就多了一层实现，然后在这里调用，变量调用会是一个大问题。
					//https://stackoverflow.com/questions/19708646/how-to-update-swing-ui-while-actionlistener-is-in-progress
					@Override
					protected Map doInBackground() throws Exception {
						Set<String> rootDomains = fetchTargetModel().fetchTargetDomainSet();
						Set<String> keywords = fetchTargetModel().fetchKeywordSet();

						btnCrawl.setEnabled(false);
						return crawl(rootDomains, keywords);

					}

					@Override
					protected void done() {
						try {
							get();
							showDataToDomainGUI();
							saveDomainDataToDB();
							btnCrawl.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		btnCrawl.setToolTipText("Crawl all subdomains recursively,This may take a long time and large Memory Usage!!!");
		HeaderPanel.add(btnCrawl);


		JButton btnZoneTransferCheck = new JButton("AXFR");
		btnZoneTransferCheck.setToolTipText("Zone Transfer Check");
		btnZoneTransferCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						stdout.println("~~~~~~~~~~~~~Zone Transfer Checking~~~~~~~~~~~~~");
						btnZoneTransferCheck.setEnabled(false);
						fetchTargetModel().ZoneTransferCheckAll();
						return null;
					}

					@Override
					protected void done() {
						btnZoneTransferCheck.setEnabled(true);
						stdout.println("~~~~~~~~~~~~~Zone Transfer Check Done~~~~~~~~~~~~~");
					}
				};
				worker.execute();
			}
		});
		HeaderPanel.add(btnZoneTransferCheck);

		JButton btnRenameProject = new JButton("Rename Project");
		btnRenameProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (domainResult != null) {
					String newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
					while (newProjectName.trim().equals("")) {
						newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
					}
					domainResult.setProjectName(newProjectName);
					GUIMain.displayProjectName();
					saveDomainDataToDB();
				}

				/*重命名文件，开销太大不合适
				if (GUI.getCurrentDBFile() !=null){
					String newDBFileName = JOptionPane.showInputDialog("New Project Name", null).trim();
					while(newDBFileName.trim().equals("")){
						newDBFileName = JOptionPane.showInputDialog("New Project Name", null).trim();
					}
					if (!newDBFileName.endsWith(".db")){
						newDBFileName = newDBFileName+".db";
					}
					File des = new File(GUI.getCurrentDBFile().getParent(),newDBFileName);
					if (des.toString().equalsIgnoreCase(GUI.getCurrentDBFile().toString())) return;
					try {
						autoSave();
						FileUtils.copyFile(GUI.getCurrentDBFile(),des);
						BurpExtender.getGui().LoadData(des.toString());
						FileUtils.deleteQuietly(GUI.getCurrentDBFile());
					} catch (IOException ioException) {
						ioException.printStackTrace();
					}
					lblSummary.setText(domainResult.getSummary());
				}*/
			}
		});
		HeaderPanel.add(btnRenameProject);


		JButton btnBuckupDB = new JButton("Backup DB");
		btnBuckupDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DomainPanel.backupDB();
			}
		});
		HeaderPanel.add(btnBuckupDB);

		textFieldUploadURL = new JTextField();
		textFieldUploadURL.setColumns(30);
		textFieldUploadURL.setToolTipText("input upload url here");
		HeaderPanel.add(textFieldUploadURL);
		// Listen for changes in the text
		textFieldUploadURL.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				saveURL();
			}

			public void removeUpdate(DocumentEvent e) {
				saveURL();
			}

			public void insertUpdate(DocumentEvent e) {
				saveURL();
			}

			public void saveURL() {
				String url = textFieldUploadURL.getText();
				try {
					new URL(url);
					domainResult.uploadURL = url;
					saveDomainDataToDB();
				} catch (Exception e) {

				}
			}
		});


		JButton btnUpload = new JButton("Upload");
		btnUpload.setToolTipText("upload data to Server");
		btnUpload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Boolean, Boolean> worker = new SwingWorker<Boolean, Boolean>() {
					@Override
					protected Boolean doInBackground() throws Exception {
						String url = domainResult.uploadURL;
						String host = new URL(url).getHost();
						String token = ToolPanel.textFieldUploadApiToken.getText().trim();
						HashMap<String, String> headers = new HashMap<String, String>();
						headers.put("Content-Type", "application/json;charset=UTF-8");
						if (token != null && !token.equals("")) {//vmp
							headers.put("Authorization", "Token " + token);
						}
						if (host.startsWith("vmp.test.shopee.") ||
								host.contains("burpcollaborator.net")) {
							return VMP.uploadAllVMPEntries(url, headers);
						} else {
							return VMP.upload(domainResult.uploadURL, headers, domainResult.ToJson());
						}
					}

					@Override
					protected void done() {
						//Do Nothing
					}
				};
				worker.execute();
			}
		});
		HeaderPanel.add(btnUpload);


		////////////////////////////////////target area///////////////////////////////////////////////////////


		JScrollPane TargetPanel = new JScrollPane();//存放目标域名
		TargetPanel.setViewportBorder(new LineBorder(new Color(0, 0, 0)));

		targetTable = new TargetTable();
		TargetPanel.setViewportView(targetTable);


		////////////////////////////////////target area////////////////////////////////////////////////////

		//第一次分割
		JSplitPane CenterSplitPane = new JSplitPane();//中间的大模块，一分为二
		CenterSplitPane.setResizeWeight(0.5);
		this.add(CenterSplitPane, BorderLayout.CENTER);


		//第二次分割，左边
		JSplitPane leftOfCenterSplitPane = new JSplitPane();//放入左边的分区。再讲左边的分区一分为二
		leftOfCenterSplitPane.setResizeWeight(0.2);
		CenterSplitPane.setLeftComponent(leftOfCenterSplitPane);

		//第二次分割，右边
		JSplitPane rightOfCenterSplitPane = new JSplitPane();//放入右半部分分区，
		rightOfCenterSplitPane.setResizeWeight(0.7);
		CenterSplitPane.setRightComponent(rightOfCenterSplitPane);

		JSplitPane split1of4 = new JSplitPane();
		split1of4.setOrientation(JSplitPane.VERTICAL_SPLIT);
		split1of4.setResizeWeight(0.7);
		leftOfCenterSplitPane.setLeftComponent(split1of4);

		JSplitPane split1of8 = new JSplitPane();
		split1of8.setResizeWeight(1.0);
		split1of8.setOrientation(JSplitPane.VERTICAL_SPLIT);
		split1of8.setLeftComponent(TargetPanel);
		split1of4.setLeftComponent(split1of8);


		JSplitPane split2of8 = new JSplitPane();
		split2of8.setOrientation(JSplitPane.VERTICAL_SPLIT);
		split2of8.setResizeWeight(0.01);
		split1of4.setRightComponent(split2of8);

		JSplitPane split2of4 = new JSplitPane();//四分之二区域的分割者
		split2of4.setResizeWeight(0.5);
		leftOfCenterSplitPane.setRightComponent(split2of4);


		JSplitPane split3of4 = new JSplitPane();//四分之三区域的分割者，
		split3of4.setResizeWeight(0.5);
		rightOfCenterSplitPane.setLeftComponent(split3of4);


		JSplitPane split4of4 = new JSplitPane();
		rightOfCenterSplitPane.setRightComponent(split4of4);


		///////////////////////////////Target Operations and Config//////////////////////


		JPanel ControlPanel = new TargetControlPanel();
		split1of8.setRightComponent(ControlPanel);

		JPanel autoControlPanel = new JPanel();
		autoControlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		split2of8.setLeftComponent(autoControlPanel);

		rdbtnAddRelatedToRoot = new JRadioButton("Auto Add Related Domain To Root Domain");
		rdbtnAddRelatedToRoot.setVerticalAlignment(SwingConstants.TOP);
		rdbtnAddRelatedToRoot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				domainResult.autoAddRelatedToRoot = rdbtnAddRelatedToRoot.isSelected();
				if (domainResult.autoAddRelatedToRoot) {
					showDataToDomainGUI();
					saveDomainDataToDB();
				}
			}
		});
		autoControlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		rdbtnAddRelatedToRoot.setSelected(false);
		autoControlPanel.add(rdbtnAddRelatedToRoot);


		///////////////////////////////textAreas///////////////////////////////////////////////////////


		JScrollPane ScrollPaneFoundIPs = new JScrollPane(); //1of4

		JScrollPane ScrollPaneRelatedDomains = new JScrollPane(); //2of4
		JScrollPane ScrollPaneSubdomains = new JScrollPane();
		JScrollPane ScrollPaneSimilarDomains = new JScrollPane();
		JScrollPane ScrollPaneEmails = new JScrollPane();
		JScrollPane ScrollPanePackageNames = new JScrollPane();


		split2of8.setRightComponent(ScrollPaneFoundIPs);

		split2of4.setLeftComponent(ScrollPaneRelatedDomains);
		split2of4.setRightComponent(ScrollPaneSubdomains);

		split3of4.setLeftComponent(ScrollPaneSimilarDomains);
		split3of4.setRightComponent(ScrollPaneEmails);

		split4of4.setLeftComponent(ScrollPanePackageNames);
		split4of4.setRightComponent(null);//通过设置为空来隐藏它

		textAreaFoundIPs = new JTextArea();
		textAreaRelatedDomains = new JTextArea();
		textAreaSubdomains = new JTextArea();
		textAreaSimilarDomains = new JTextArea();
		textAreaEmails = new JTextArea();
		textAreaPackages = new JTextArea();

		textAreaFoundIPs.setColumns(10);
		textAreaRelatedDomains.setColumns(10);
		textAreaSubdomains.setColumns(10);
		textAreaSimilarDomains.setColumns(10);
		textAreaEmails.setColumns(10);
		textAreaPackages.setColumns(10);

		textAreaFoundIPs.setToolTipText("Found IPs");
        textAreaRelatedDomains.setToolTipText("Related Domains");
        textAreaSubdomains.setToolTipText("Sub Domains");
        textAreaSimilarDomains.setToolTipText("Similar Domains");
        textAreaEmails.setToolTipText("Emails");
        textAreaPackages.setToolTipText("Package Names");

        ScrollPaneFoundIPs.setViewportView(textAreaFoundIPs);
		ScrollPaneRelatedDomains.setViewportView(textAreaRelatedDomains);
		ScrollPaneSubdomains.setViewportView(textAreaSubdomains);
		ScrollPaneSimilarDomains.setViewportView(textAreaSimilarDomains);
		ScrollPaneEmails.setViewportView(textAreaEmails);
		ScrollPanePackageNames.setViewportView(textAreaPackages);

		Border blackline = BorderFactory.createLineBorder(Color.black);

		JLabel lblNewLabel = new JLabel("Found IPs");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setBorder(blackline);
		ScrollPaneFoundIPs.setColumnHeaderView(lblNewLabel);
		JLabel lblNewLabel_1 = new JLabel("Related Domains");
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_1.setBorder(blackline);
		ScrollPaneRelatedDomains.setColumnHeaderView(lblNewLabel_1);
		JLabel lblNewLabel_2 = new JLabel("Sub Domains");
		lblNewLabel_2.setBorder(blackline);
		lblNewLabel_2.setHorizontalAlignment(SwingConstants.CENTER);
		ScrollPaneSubdomains.setColumnHeaderView(lblNewLabel_2);
		JLabel lblNewLabel_3 = new JLabel("Similar Domains");
		lblNewLabel_3.setBorder(blackline);
		lblNewLabel_3.setHorizontalAlignment(SwingConstants.CENTER);
		ScrollPaneSimilarDomains.setColumnHeaderView(lblNewLabel_3);
		JLabel lblNewLabel_4 = new JLabel("Emails");
		lblNewLabel_4 .setBorder(blackline);
		lblNewLabel_4.setHorizontalAlignment(SwingConstants.CENTER);
		ScrollPaneEmails.setColumnHeaderView(lblNewLabel_4);
		JLabel lblNewLabel_5 = new JLabel("Package Names");
		lblNewLabel_5.setBorder(blackline);
		lblNewLabel_5.setHorizontalAlignment(SwingConstants.CENTER);
		ScrollPanePackageNames.setColumnHeaderView(lblNewLabel_5);

		//实现编辑后自动保存
		textAreaFoundIPs.getDocument().addDocumentListener(new textAreaListener());
		textAreaRelatedDomains.getDocument().addDocumentListener(new textAreaListener());
		textAreaSubdomains.getDocument().addDocumentListener(new textAreaListener());
		textAreaSimilarDomains.getDocument().addDocumentListener(new textAreaListener());
		textAreaEmails.getDocument().addDocumentListener(new textAreaListener());
		textAreaPackages.getDocument().addDocumentListener(new textAreaListener());

		textAreaFoundIPs.addMouseListener(new TextAreaMouseListener(textAreaFoundIPs));
		textAreaRelatedDomains.addMouseListener(new TextAreaMouseListener(textAreaRelatedDomains));
		textAreaSubdomains.addMouseListener(new TextAreaMouseListener(textAreaSubdomains));
		textAreaSimilarDomains.addMouseListener(new TextAreaMouseListener(textAreaSimilarDomains));
		textAreaEmails.addMouseListener(new TextAreaMouseListener(textAreaEmails));
		textAreaPackages.addMouseListener(new TextAreaMouseListener(textAreaPackages));


		///////////////////////////FooterPanel//////////////////


		JPanel footerPanel = new JPanel();
		FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
		fl_FooterPanel.setAlignment(FlowLayout.LEFT);
		this.add(footerPanel, BorderLayout.SOUTH);

		JLabel footerLabel = new JLabel(BurpExtender.getGithub());
		footerLabel.setFont(new Font("宋体", Font.BOLD, 12));
		footerLabel.addMouseListener(new MouseAdapter() {
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
				footerLabel.setForeground(Color.BLUE);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				footerLabel.setForeground(Color.BLACK);
			}
		});
		footerPanel.add(footerLabel);

		lblSummary = new JLabel("      ^_^");
		footerPanel.add(lblSummary);
		lblSummary.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Commons.OpenFolder(GUIMain.getCurrentDBFile().getParent());
				} catch (Exception e2) {
					e2.printStackTrace(stderr);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				lblSummary.setForeground(Color.RED);
				lblSummary.setToolTipText(GUIMain.getCurrentDBFile().toString());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				lblSummary.setForeground(Color.BLACK);
			}
		});


		//搜索域名，但是效果不怎么好
		//		JTextField textFieldSearch = new JTextField("");
		//		textFieldSearch.addFocusListener(new FocusAdapter() {
		//			@Override
		//			public void focusGained(FocusEvent e) {
		//				if (textFieldSearch.getText().equals("Input text to search")) {
		//					textFieldSearch.setText("");
		//				}
		//			}
		//			@Override
		//			public void focusLost(FocusEvent e) {
		//				/*
		//				 * if (textFieldSearch.getText().equals("")) {
		//				 * textFieldSearch.setText("Input text to search"); }
		//				 */
		//
		//			}
		//		});
		//
		//		textFieldSearch.addActionListener(new ActionListener() {
		//			public void actionPerformed(ActionEvent e) {
		//				String keyword = textFieldSearch.getText().trim();
		//				domainPanelSearch(keyword);
		//			}
		//
		//			public void domainPanelSearch(String keyword) {
		//				try {
		//					Highlighter h = textAreaSubdomains.getHighlighter();
		//					h.removeAllHighlights();
		//					int pos = textAreaSubdomains.getText().indexOf(keyword, 0);
		//					h.addHighlight(pos ,
		//					               pos  + keyword.length(),
		//					               DefaultHighlighter.DefaultPainter);
		//					textAreaRelatedDomains = new JTextArea();
		//					textAreaSubdomains = new JTextArea();
		//					textAreaSimilarDomains = new JTextArea();
		//					textAreaEmails = new JTextArea();
		//					textAreaPackages = new JTextArea();
		//				} catch (BadLocationException e) {
		//					e.printStackTrace(stderr);
		//				}
		//			}
		//		});
		//
		//		textFieldSearch.setColumns(30);
		//		footerPanel.add(textFieldSearch);
	}

	/**
	 * 显示DomainPanel中的数据。
	 * 未包含target信息
	 */
	public void showDataToDomainGUI() {
		listenerIsOn = false;
		domainResult.relatedToRoot();

		textFieldUploadURL.setText(domainResult.uploadURL);
		textAreaFoundIPs.setText(domainResult.fetchFoundIPs());
		textAreaSubdomains.setText(domainResult.fetchSubDomains());
		textAreaSimilarDomains.setText(domainResult.fetchSimilarDomains());
		textAreaRelatedDomains.setText(domainResult.fetchRelatedDomains());
		textAreaEmails.setText(domainResult.fetchEmails());
		textAreaPackages.setText(domainResult.fetchPackageNames());
		lblSummary.setText(domainResult.getSummary());
		rdbtnAddRelatedToRoot.setSelected(domainResult.autoAddRelatedToRoot);

		System.out.println("Load Domain Panel Data Done, " + domainResult.getSummary());
		stdout.println("Load Domain Panel Data Done, " + domainResult.getSummary());
		listenerIsOn = true;
	}

	public void LoadData(DomainManager domainResult) {
		setDomainResult(domainResult);
		showDataToDomainGUI();
	}


	//////////////////////////////methods//////////////////////////////////////
	/*
    执行完成后，就已将数据保存到了domainResult
	 */
	public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords) {
		IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
		IHttpRequestResponse[] messages = callbacks.getSiteMap(null);

		List<IHttpRequestResponse> AllMessages = new ArrayList<IHttpRequestResponse>();
		AllMessages.addAll(Arrays.asList(messages));
		AllMessages.addAll(collectPackageNameMessages());//包含错误回显的请求响应消息

		ThreadSearhDomain searchinstance = new ThreadSearhDomain(AllMessages);
		searchinstance.start();
		try {
			searchinstance.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//DomainPanel.saveDomainDataToDB();//SwingWorker中的Done()每次搜索完成都应该进行一次保存
		return null;
	}



	public static Set<String> collectEmails() {
		Set<String> Emails = new HashSet<>();
		IScanIssue[] issues = BurpExtender.getCallbacks().getScanIssues(null);

		final String REGEX_EMAIL = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";
		Pattern pDomainNameOnly = Pattern.compile(REGEX_EMAIL);

		for (IScanIssue issue : issues) {
			if (issue.getIssueName().equalsIgnoreCase("Email addresses disclosed")) {
				String detail = issue.getIssueDetail();
				Matcher matcher = pDomainNameOnly.matcher(detail);
				while (matcher.find()) {//多次查找
					String email = matcher.group();
					if (fetchTargetModel().isRelatedEmail(email)) {
						Emails.add(matcher.group());
					}
					System.out.println(matcher.group());
				}
			}
		}
		return Emails;
	}


	public static Set<IHttpRequestResponse> collectPackageNameMessages() {
		Set<IHttpRequestResponse> PackageNameMessages = new HashSet<>();
		IScanIssue[] issues = BurpExtender.getCallbacks().getScanIssues(null);
		for (IScanIssue issue : issues) {
			if (issue.getIssueName().toLowerCase().contains("error")) {
				//Detailed Error Messages Revealed  ---generated by other burp extender
				PackageNameMessages.addAll(Arrays.asList(issue.getHttpMessages()));
			}
		}
		return PackageNameMessages;
	}


	public Map<String, Set<String>> crawl(Set<String> rootdomains, Set<String> keywords) {
		int i = 0;
		while (i <= 2) {
			for (String rootdomain : rootdomains) {
				if (!rootdomain.contains(".") || rootdomain.endsWith(".") || rootdomain.equals("")) {
					//如果域名为空，或者（不包含.号，或者点号在末尾的）
				} else {
					IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
					IHttpRequestResponse[] items = callbacks.getSiteMap(null); //null to return entire sitemap
					//int len = items.length;
					//stdout.println("item number: "+len);
					Set<URL> NeedToCrawl = new HashSet<URL>();
					for (IHttpRequestResponse x : items) {// 经过验证每次都需要从头开始遍历，按一定offset获取的数据每次都可能不同

						IHttpService httpservice = x.getHttpService();
						String shortUrlString = httpservice.toString();
						String Host = httpservice.getHost();

						try {
							URL shortUrl = new URL(shortUrlString);

							if (Host.endsWith("." + rootdomain) && Commons.isResponseNull(x)) {
								// to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
								NeedToCrawl.add(shortUrl);
								// to reduce memory usage, use shortUrl. base on my test, spider will crawl entire site when send short URL to it.
								// this may miss some single page, but the single page often useless for domain collection
								// see spideralltest() function.
							}
						} catch (MalformedURLException e) {
							e.printStackTrace(stderr);
						}
					}


					for (URL shortUrl : NeedToCrawl) {
						if (!callbacks.isInScope(shortUrl)) { //reduce add scope action, to reduce the burp UI action.
							callbacks.includeInScope(shortUrl);//if not, will always show confirm message box.
						}
						callbacks.sendToSpider(shortUrl);
					}
				}
			}


			try {
				Thread.sleep(5 * 60 * 1000);//单位毫秒，60000毫秒=一分钟
				stdout.println("sleep 5 minutes to wait spider");
				//to wait spider
			} catch (InterruptedException e) {
				e.printStackTrace(stdout);
			}
			i++;
		}

		return search(rootdomains, keywords);
	}


	/*
    单独保存域名信息到另外的文件
	 */
	public File saveDomainOnly() {
		try {
			File file = BurpExtender.getGui().dbfc.dialog(false,".db");
			if (file != null) {
				DBHelper dbHelper = new DBHelper(file.toString());
				if (dbHelper.saveDomainObject(domainResult) && dbHelper.saveTargets(fetchTargetModel())) {
					stdout.println("Save Domain Only Success! " + Commons.getNowTimeString());
					return file;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		stdout.println("Save Domain Only failed! " + Commons.getNowTimeString());
		return null;
	}

	/*
    自动保存，根据currentDBFile，如果currentDBFile为空或者不存在，就提示选择文件。
	 */
	public static void saveDomainDataToDB() {
		File file = GUIMain.getCurrentDBFile();
		if (file == null) {
			if (null == DomainPanel.getDomainResult()) return;//有数据才弹对话框指定文件位置。
			file = BurpExtender.getGui().dbfc.dialog(false,".db");
			GUIMain.setCurrentDBFile(file);
		}
		if (file != null) {
			DBHelper dbHelper = new DBHelper(file.toString());
			boolean success = dbHelper.saveDomainObject(domainResult);
			if (success) {
				log.info("domain data saved");
			}else {
				log.error("domain data save failed");
			}
		}
	}

	/*
    //用于各种domain的手动编辑后的保存（不包含rootdomain）
	 */
	class textAreaListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				saveTextAreas();
				saveDomainDataToDB();
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				saveTextAreas();
				saveDomainDataToDB();
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			if (listenerIsOn) {
				saveTextAreas();
				saveDomainDataToDB();
			}
		}
	}

	public void saveTextAreas() {

		HashSet<String> oldSubdomains = new HashSet<String>();
		oldSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		domainResult.setFoundIPSet(getSetFromTextArea(textAreaFoundIPs));
		domainResult.setRelatedDomainSet(getSetFromTextArea(textAreaRelatedDomains));
		domainResult.setSubDomainSet(getSetFromTextArea(textAreaSubdomains));
		domainResult.setSimilarDomainSet(getSetFromTextArea(textAreaSimilarDomains));
		domainResult.setEmailSet(getSetFromTextArea(textAreaEmails));
		domainResult.setPackageNameSet(getSetFromTextArea(textAreaPackages));
		domainResult.getSummary();

		//用于存储新增的域名到一个临时集合
		HashSet<String> newSubdomains = new HashSet<String>();
		newSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

		newSubdomains.removeAll(oldSubdomains);
		DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().addAll(newSubdomains);
		stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~", newSubdomains.size()));
		stdout.println(String.join(System.lineSeparator(), newSubdomains));
	}

	public static Set<String> getSetFromTextArea(JTextArea textarea) {
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n")));
		domainList.remove("");
		return domainList;
	}

	public static void backupDB() {
		File file = GUIMain.getCurrentDBFile();
		if (file == null) return;
		File bakfile = new File(file.getAbsoluteFile().toString() + ".bak" + Commons.getNowTimeString());
		try {
			FileUtils.copyFile(file, bakfile);
			BurpExtender.getStdout().println("DB File Backed Up:" + bakfile.getAbsolutePath());
		} catch (IOException e1) {
			e1.printStackTrace(BurpExtender.getStderr());
		}
	}

	public static void main(String[] args) {
		String tmp = InternetDomainName.from("baidu.xxx.com.br").topPrivateDomain().toString();
		String tmp1 = InternetDomainName.from("baidu.xxx.com.br").publicSuffix().toString();
		System.out.println(tmp);
		System.out.println(tmp1);
	}

}
