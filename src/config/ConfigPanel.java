package config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import GUI.GUIMain;
import Tools.ToolPanel;
import burp.BurpExtender;
import title.search.History;

public class ConfigPanel extends JPanel{
	public volatile static boolean listenerIsOn = true;
	PrintWriter stdout;
	PrintWriter stderr;
	private static JTextField BrowserPath;

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
		lineConfig = LineConfig.loadFromDisk(projectConfigFile);//projectConfigFile可能为null

		History.setInstance(lineConfig.getSearchHistory());

		String dbFilePath = lineConfig.getDbfilepath();

		if (dbFilePath != null && dbFilePath.endsWith(".db")) {
			GUIMain.LoadData(dbFilePath);
		}
		//这里的修改也会触发textFieldListener监听器。
		//由于我们是多个组件共用一个保存逻辑，当前对一个组件设置值的时候，触发保存，从而导致整体数据的修改！！！
		//所以和domain和title中一样，显示数据时关闭监听器。
		listenerIsOn = false;
		ToolPanel.inputTextArea.setText(lineConfig.getToolPanelText());

		BrowserPath.setText(lineConfig.getBrowserPath());

		if (!lineConfig.getNmapPath().contains("{host}")) {//兼容新旧版本，
			lineConfig.setNmapPath(LineConfig.defaultNmap);
		}
		textFieldPortScanner.setText(lineConfig.getNmapPath());
		textFieldDirSearch.setText(lineConfig.getDirSearchPath());
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
		lineConfig.setNmapPath(textFieldPortScanner.getText());
		lineConfig.setElasticApiUrl(textFieldElasticURL.getText().trim());
		lineConfig.setElasticUsernameAndPassword(textFieldElasticUserPass.getText());
		lineConfig.setUploadApiToken(textFieldUploadApiToken.getText());

		lineConfig.setToolPanelText(ToolPanel.inputTextArea.getText());
		lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
		lineConfig.setEnableElastic(rdbtnSaveTrafficTo.isSelected());
	}	

	public ConfigPanel() {
		///////
		JPanel fourFourthPanel = new JPanel();
		GridBagLayout gbl_fourFourthPanel = new GridBagLayout();
		gbl_fourFourthPanel.columnWidths = new int[]{215, 215, 0};
		gbl_fourFourthPanel.rowHeights = new int[]{27, 0, 0, 0, 27, 0, 0, 0, 0, 0, 27, 27, 27, 27, 0, 0, 0, 0};
		gbl_fourFourthPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_fourFourthPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gbl_fourFourthPanel);

		JLabel lblNewLabel = new JLabel("Browser Path:");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.BOTH;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		BrowserPath = new JTextField();
		GridBagConstraints gbc_BrowserPath = new GridBagConstraints();
		gbc_BrowserPath.fill = GridBagConstraints.BOTH;
		gbc_BrowserPath.insets = new Insets(0, 0, 5, 0);
		gbc_BrowserPath.gridx = 1;
		gbc_BrowserPath.gridy = 0;
		add(BrowserPath, gbc_BrowserPath);
		BrowserPath.setColumns(50);
		BrowserPath.getDocument().addDocumentListener(new textFieldListener());

		JLabel lblPortScanner = new JLabel("PortScanner Command:");
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
		add(lblPortScanner, gbc_lblPortScanner);

		textFieldPortScanner = new JTextField();
		textFieldPortScanner.setColumns(50);
		textFieldPortScanner.getDocument().addDocumentListener(new textFieldListener());
		GridBagConstraints gbc_textFieldPortScanner = new GridBagConstraints();
		gbc_textFieldPortScanner.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPortScanner.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPortScanner.gridx = 1;
		gbc_textFieldPortScanner.gridy = 1;
		add(textFieldPortScanner, gbc_textFieldPortScanner);

		/*
			JLabel lblPythonPath = new JLabel("Python3 Path:");
			GridBagConstraints gbc_lblPythonPath = new GridBagConstraints();
			gbc_lblPythonPath.anchor = GridBagConstraints.WEST;
			gbc_lblPythonPath.insets = new Insets(0, 0, 5, 5);
			gbc_lblPythonPath.gridx = 0;
			gbc_lblPythonPath.gridy = 2;
			add(lblPythonPath, gbc_lblPythonPath);

			textFieldPython = new JTextField();
			textFieldPython.setColumns(50);
			GridBagConstraints gbc_textFieldPython = new GridBagConstraints();
			gbc_textFieldPython.insets = new Insets(0, 0, 5, 0);
			gbc_textFieldPython.fill = GridBagConstraints.HORIZONTAL;
			gbc_textFieldPython.gridx = 1;
			gbc_textFieldPython.gridy = 2;
			add(textFieldPython, gbc_textFieldPython);
		 */

		JLabel lblDirSearch = new JLabel("DirSearch Command:");
		lblDirSearch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					textFieldDirSearch.setText(LineConfig.defaultDirSearch);
				}
			}
		});
		GridBagConstraints gbc_lblDirSearch = new GridBagConstraints();
		gbc_lblDirSearch.anchor = GridBagConstraints.WEST;
		gbc_lblDirSearch.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirSearch.gridx = 0;
		gbc_lblDirSearch.gridy = 3;
		add(lblDirSearch, gbc_lblDirSearch);

		textFieldDirSearch = new JTextField();
		textFieldDirSearch.setColumns(50);
		textFieldDirSearch.getDocument().addDocumentListener(new textFieldListener());
		GridBagConstraints gbc_textFieldDirSearch = new GridBagConstraints();
		gbc_textFieldDirSearch.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldDirSearch.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldDirSearch.gridx = 1;
		gbc_textFieldDirSearch.gridy = 3;
		add(textFieldDirSearch, gbc_textFieldDirSearch);

		JLabel lblDirBruteDict = new JLabel("Dir Brute Dict:");
		GridBagConstraints gbc_lblDirBruteDict = new GridBagConstraints();
		gbc_lblDirBruteDict.anchor = GridBagConstraints.WEST;
		gbc_lblDirBruteDict.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirBruteDict.gridx = 0;
		gbc_lblDirBruteDict.gridy = 5;
		add(lblDirBruteDict, gbc_lblDirBruteDict);

		textFieldDirBruteDict = new JTextField();
		textFieldDirBruteDict.setToolTipText("path of dict");
		textFieldDirBruteDict.setColumns(50);
		textFieldDirBruteDict.getDocument().addDocumentListener(new textFieldListener());
		GridBagConstraints gbc_textFieldDirBruteDict = new GridBagConstraints();
		gbc_textFieldDirBruteDict.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldDirBruteDict.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldDirBruteDict.gridx = 1;
		gbc_textFieldDirBruteDict.gridy = 5;
		add(textFieldDirBruteDict, gbc_textFieldDirBruteDict);

		JLabel lblElasticURL = new JLabel("Elastic URL:");
		GridBagConstraints gbc_lblElasticURL = new GridBagConstraints();
		gbc_lblElasticURL.anchor = GridBagConstraints.WEST;
		gbc_lblElasticURL.insets = new Insets(0, 0, 5, 5);
		gbc_lblElasticURL.gridx = 0;
		gbc_lblElasticURL.gridy = 6;
		add(lblElasticURL, gbc_lblElasticURL);

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
		add(textFieldElasticURL, gbc_textFieldElasticURL);

		JLabel lblDirElasticUserPass = new JLabel("Elastic Username Password:");
		GridBagConstraints gbc_lblDirElasticUserPass = new GridBagConstraints();
		gbc_lblDirElasticUserPass.anchor = GridBagConstraints.WEST;
		gbc_lblDirElasticUserPass.insets = new Insets(0, 0, 5, 5);
		gbc_lblDirElasticUserPass.gridx = 0;
		gbc_lblDirElasticUserPass.gridy = 7;
		add(lblDirElasticUserPass, gbc_lblDirElasticUserPass);

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
		add(textFieldElasticUserPass, gbc_textField_1);

		JLabel lblUploadAPIToken = new JLabel("Upload API Token:");
		GridBagConstraints gbc_lblUploadAPIToken = new GridBagConstraints();
		gbc_lblUploadAPIToken.anchor = GridBagConstraints.WEST;
		gbc_lblUploadAPIToken.insets = new Insets(0, 0, 5, 5);
		gbc_lblUploadAPIToken.gridx = 0;
		gbc_lblUploadAPIToken.gridy = 8;
		add(lblUploadAPIToken, gbc_lblUploadAPIToken);

		textFieldUploadApiToken = new JTextField();
		textFieldUploadApiToken.setToolTipText("token of upload api");
		textFieldUploadApiToken.getDocument().addDocumentListener(new textFieldListener());
		textFieldUploadApiToken.setColumns(50);
		GridBagConstraints gbc_textFieldUploadApiToken = new GridBagConstraints();
		gbc_textFieldUploadApiToken.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldUploadApiToken.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldUploadApiToken.gridx = 1;
		gbc_textFieldUploadApiToken.gridy = 8;
		add(textFieldUploadApiToken, gbc_textFieldUploadApiToken);

		DisplayContextMenuOfBurp = new JRadioButton("Display Context Menu Of Burp");
		DisplayContextMenuOfBurp.setSelected(true);
		GridBagConstraints gbc_DisplayContextMenuOfBurp = new GridBagConstraints();
		gbc_DisplayContextMenuOfBurp.insets = new Insets(0, 0, 5, 0);
		gbc_DisplayContextMenuOfBurp.fill = GridBagConstraints.BOTH;
		gbc_DisplayContextMenuOfBurp.gridx = 1;
		gbc_DisplayContextMenuOfBurp.gridy = 9;
		add(DisplayContextMenuOfBurp, gbc_DisplayContextMenuOfBurp);

		JLabel label_1 = new JLabel("");
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.fill = GridBagConstraints.BOTH;
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 0;
		gbc_label_1.gridy = 10;
		add(label_1, gbc_label_1);


		showItemsInOne = new JRadioButton("Display Context Menu Items In One");
		GridBagConstraints gbc_showItemsInOne = new GridBagConstraints();
		gbc_showItemsInOne.fill = GridBagConstraints.BOTH;
		gbc_showItemsInOne.insets = new Insets(0, 0, 5, 0);
		gbc_showItemsInOne.gridx = 1;
		gbc_showItemsInOne.gridy = 10;
		add(showItemsInOne, gbc_showItemsInOne);
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
		add(label_2, gbc_label_2);

		ignoreHTTPS = new JRadioButton("Ignore HTTPS if HTTP is OK || Ignore HTTP if HTTPS is OK");
		GridBagConstraints gbc_ignoreHTTPS = new GridBagConstraints();
		gbc_ignoreHTTPS.fill = GridBagConstraints.BOTH;
		gbc_ignoreHTTPS.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreHTTPS.gridx = 1;
		gbc_ignoreHTTPS.gridy = 11;
		add(ignoreHTTPS, gbc_ignoreHTTPS);
		ignoreHTTPS.setSelected(true);

		JLabel label_3 = new JLabel("");
		GridBagConstraints gbc_label_3 = new GridBagConstraints();
		gbc_label_3.fill = GridBagConstraints.BOTH;
		gbc_label_3.insets = new Insets(0, 0, 5, 5);
		gbc_label_3.gridx = 0;
		gbc_label_3.gridy = 12;
		add(label_3, gbc_label_3);

		ignoreHTTPStaus500 = new JRadioButton("Ignore items which Status >= 500");
		GridBagConstraints gbc_ignoreHTTPStaus500 = new GridBagConstraints();
		gbc_ignoreHTTPStaus500.fill = GridBagConstraints.BOTH;
		gbc_ignoreHTTPStaus500.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreHTTPStaus500.gridx = 1;
		gbc_ignoreHTTPStaus500.gridy = 12;
		add(ignoreHTTPStaus500, gbc_ignoreHTTPStaus500);
		ignoreHTTPStaus500.setSelected(true);

		JLabel label_4 = new JLabel("");
		GridBagConstraints gbc_label_4 = new GridBagConstraints();
		gbc_label_4.fill = GridBagConstraints.BOTH;
		gbc_label_4.insets = new Insets(0, 0, 5, 5);
		gbc_label_4.gridx = 0;
		gbc_label_4.gridy = 13;
		add(label_4, gbc_label_4);

		ignoreHTTPStaus400 = new JRadioButton("Ignore http Status 400(The plain HTTP request was sent to HTTPS port)");
		GridBagConstraints gbc_ignoreHTTPStaus400 = new GridBagConstraints();
		gbc_ignoreHTTPStaus400.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreHTTPStaus400.fill = GridBagConstraints.BOTH;
		gbc_ignoreHTTPStaus400.gridx = 1;
		gbc_ignoreHTTPStaus400.gridy = 13;
		add(ignoreHTTPStaus400, gbc_ignoreHTTPStaus400);
		ignoreHTTPStaus400.setSelected(true);

		JLabel label_5 = new JLabel("");
		GridBagConstraints gbc_label_5 = new GridBagConstraints();
		gbc_label_5.insets = new Insets(0, 0, 5, 5);
		gbc_label_5.gridx = 0;
		gbc_label_5.gridy = 14;
		add(label_5, gbc_label_5);

		ignoreWrongCAHost = new JRadioButton("Ignore Host that IP Address and Certificate Authority not match");
		ignoreWrongCAHost.setSelected(false);
		GridBagConstraints gbc_ignoreWrongCAHost = new GridBagConstraints();
		gbc_ignoreWrongCAHost.insets = new Insets(0, 0, 5, 0);
		gbc_ignoreWrongCAHost.fill = GridBagConstraints.BOTH;
		gbc_ignoreWrongCAHost.gridx = 1;
		gbc_ignoreWrongCAHost.gridy = 14;
		add(ignoreWrongCAHost, gbc_ignoreWrongCAHost);

		rdbtnSaveTrafficTo = new JRadioButton("Save traffic to Elastic");
		rdbtnSaveTrafficTo.setSelected(false);
		GridBagConstraints gbc_rdbtnSaveTrafficTo = new GridBagConstraints();
		gbc_rdbtnSaveTrafficTo.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSaveTrafficTo.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnSaveTrafficTo.gridx = 1;
		gbc_rdbtnSaveTrafficTo.gridy = 15;
		add(rdbtnSaveTrafficTo, gbc_rdbtnSaveTrafficTo);

		JLabel label_6 = new JLabel("");
		GridBagConstraints gbc_label_6 = new GridBagConstraints();
		gbc_label_6.insets = new Insets(0, 0, 0, 5);
		gbc_label_6.gridx = 0;
		gbc_label_6.gridy = 16;
		add(label_6, gbc_label_6);
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
