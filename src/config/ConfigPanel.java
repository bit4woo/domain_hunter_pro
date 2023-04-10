package config;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import GUI.GUIMain;
import GUI.MyGridBagLayout;
import Tools.ToolPanel;
import burp.BurpExtender;
import title.search.History;

public class ConfigPanel extends JPanel{
	public volatile static boolean listenerIsOn = true;
	PrintWriter stdout;
	PrintWriter stderr;
	private JTextField textFieldUploadURL;
	private GUIMain gui;
	private LineConfig lineConfig;

	public static JTextField BrowserPath;
	public static JRadioButton showItemsInOne;
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
	
	public static JTextField textFieldFofaEmail;
	public static JTextField textFieldFofaKey;
	
	public static JTextField textFieldQuakeAPIKey;
	public static JTextField textFieldHunterAPIKey;

	public LineConfig getLineConfig() {
		return lineConfig;
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
					frame.setContentPane(new ConfigPanel(null));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	/**
	 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
	 * 注意对监听器的影响
	 */
	public void loadConfigToGUI(String projectConfigFile) {
		BurpExtender.getStdout().println("Loading Tool Panel Config From Disk");
		lineConfig = LineConfig.loadFromDisk(projectConfigFile);//projectConfigFile可能为null
		if (lineConfig == null){
			lineConfig = new LineConfig(gui);
		}else{
			lineConfig.setGui(gui);
		}

		History.setInstance(lineConfig.getSearchHistory());

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
		textFieldFofaEmail.setText(lineConfig.getFofaEmail());
		textFieldFofaKey.setText(lineConfig.getFofaKey());
		textFieldQuakeAPIKey.setText(lineConfig.getQuakeAPIKey());
		textFieldHunterAPIKey.setText(lineConfig.getHunterAPIKey());

		showItemsInOne.setSelected(lineConfig.isShowItemsInOne());
		rdbtnSaveTrafficTo.setSelected(lineConfig.isEnableElastic());
		listenerIsOn = true;//显示完毕后打开监听器。
	}


	public void saveToConfigFromGUI() {
		lineConfig.setBrowserPath(BrowserPath.getText());
		lineConfig.setDirSearchPath(textFieldDirSearch.getText());
		lineConfig.setBruteDict(textFieldDirBruteDict.getText());
		lineConfig.setNmapPath(textFieldPortScanner.getText());
		lineConfig.setElasticApiUrl(textFieldElasticURL.getText().trim());
		lineConfig.setElasticUsernameAndPassword(textFieldElasticUserPass.getText());
		lineConfig.setUploadApiToken(textFieldUploadApiToken.getText());
		lineConfig.setFofaEmail(textFieldFofaEmail.getText());
		lineConfig.setFofaKey(textFieldFofaKey.getText());
		lineConfig.setQuakeAPIKey(textFieldQuakeAPIKey.getText());
		lineConfig.setHunterAPIKey(textFieldHunterAPIKey.getText());
		
		lineConfig.setToolPanelText(ToolPanel.inputTextArea.getText());
		lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
		lineConfig.setEnableElastic(rdbtnSaveTrafficTo.isSelected());
	}	

	public ConfigPanel(GUIMain gui) {
		this.gui = gui;
		GridBagLayout gbl_fourFourthPanel = new GridBagLayout();
		gbl_fourFourthPanel.columnWidths = new int[]{215, 215, 0};
		gbl_fourFourthPanel.rowHeights = new int[]{27, 0, 0, 0, 27, 0, 0, 0, 0, 0, 27, 27, 27, 27, 0, 0, 0, 0};
		gbl_fourFourthPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_fourFourthPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gbl_fourFourthPanel);
		setLayout(gbl_fourFourthPanel);
		//setLayout(new GridLayout(20, 2));
		JLabel lblNewLabel = new JLabel("Browser Path:");

		BrowserPath = new JTextField();
		BrowserPath.setColumns(50);
		BrowserPath.getDocument().addDocumentListener(new textFieldListener());

		JLabel lblPortScanner = new JLabel("PortScanner Command:");
		lblPortScanner.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					String current = textFieldPortScanner.getText();
					if (current.equals(LineConfig.defaultNmap)) {
						textFieldPortScanner.setText(LineConfig.defaultMasscan);
					}else if(current.equals(LineConfig.defaultMasscan)) {
						textFieldPortScanner.setText(LineConfig.defaultNmap);
					}else {
						textFieldPortScanner.setText(LineConfig.defaultMasscan);
					}
				}
			}
		});

		textFieldPortScanner = new JTextField();
		textFieldPortScanner.setColumns(50);
		textFieldPortScanner.getDocument().addDocumentListener(new textFieldListener());

		int rowIndex = 0;
		add(lblNewLabel, new MyGridBagLayout(++rowIndex,1));
		add(BrowserPath, new MyGridBagLayout(rowIndex,2));
		add(lblPortScanner, new MyGridBagLayout(++rowIndex,1));
		add(textFieldPortScanner, new MyGridBagLayout(rowIndex,2));


		JLabel lblDirSearch = new JLabel("DirSearch Command:");
		lblDirSearch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击
					textFieldDirSearch.setText(LineConfig.defaultDirSearch);
				}
			}
		});

		textFieldDirSearch = new JTextField();
		textFieldDirSearch.setColumns(50);
		textFieldDirSearch.getDocument().addDocumentListener(new textFieldListener());

		add(lblDirSearch, new MyGridBagLayout(++rowIndex,1));
		add(textFieldDirSearch, new MyGridBagLayout(rowIndex,2));


		JLabel lblDirBruteDict = new JLabel("Dir Brute Dict:");

		textFieldDirBruteDict = new JTextField();
		textFieldDirBruteDict.setToolTipText("path of dict");
		textFieldDirBruteDict.setColumns(50);
		textFieldDirBruteDict.getDocument().addDocumentListener(new textFieldListener());

		add(lblDirBruteDict, new MyGridBagLayout(++rowIndex,1));
		add(textFieldDirBruteDict, new MyGridBagLayout(rowIndex,2));


		JLabel lblElasticURL = new JLabel("Elastic URL:");

		textFieldElasticURL = new JTextField();
		textFieldElasticURL.setToolTipText("URL of elastic API");
		textFieldElasticURL.setText("http://10.12.72.55:9200/");
		textFieldElasticURL.getDocument().addDocumentListener(new textFieldListener());
		//textFieldElasticURL.addFocusListener(new JTextFieldHintListener(textFieldElasticURL,"http://10.12.72.55:9200/"));
		//这个HintListener的操作，会触发DocumentListener的insertUpdate操作！
		textFieldElasticURL.setColumns(50);

		add(lblElasticURL, new MyGridBagLayout(++rowIndex,1));
		add(textFieldElasticURL, new MyGridBagLayout(rowIndex,2));


		JLabel lblDirElasticUserPass = new JLabel("Elastic Username Password:");

		textFieldElasticUserPass = new JTextField();
		textFieldElasticUserPass.setText("elastic:changeme");
		textFieldElasticUserPass.setToolTipText("username and password of elastic API");
		textFieldElasticUserPass.getDocument().addDocumentListener(new textFieldListener());
		//textFieldElasticUserPass.addFocusListener(new JTextFieldHintListener(textFieldElasticUserPass,"elastic:changeme"));
		textFieldElasticUserPass.setColumns(50);

		add(lblDirElasticUserPass, new MyGridBagLayout(++rowIndex,1));
		add(textFieldElasticUserPass, new MyGridBagLayout(rowIndex,2));

		JLabel lblUploadAPIToken = new JLabel("Upload API Token:");

		textFieldUploadApiToken = new JTextField();
		textFieldUploadApiToken.setToolTipText("token of upload api");
		textFieldUploadApiToken.getDocument().addDocumentListener(new textFieldListener());
		textFieldUploadApiToken.setColumns(50);

		add(lblUploadAPIToken, new MyGridBagLayout(++rowIndex,1));
		add(textFieldUploadApiToken, new MyGridBagLayout(rowIndex,2));

		JLabel lblUploadUrl = new JLabel("Upload URL:");

		textFieldUploadURL = new JTextField();
		textFieldUploadURL.setColumns(30);
		textFieldUploadURL.setToolTipText("input upload url here");
		textFieldUploadURL.getDocument().addDocumentListener(new textFieldListener());

		add(lblUploadUrl, new MyGridBagLayout(++rowIndex,1));
		add(textFieldUploadURL,new MyGridBagLayout(rowIndex,2));

		
		JLabel lblFofaEmail = new JLabel("Fofa Email:");

		textFieldFofaEmail = new JTextField();
		textFieldFofaEmail.setColumns(30);
		textFieldFofaEmail.getDocument().addDocumentListener(new textFieldListener());

		add(lblFofaEmail, new MyGridBagLayout(++rowIndex,1));
		add(textFieldFofaEmail,new MyGridBagLayout(rowIndex,2));
		
		
		JLabel lblFofaKey = new JLabel("Fofa Key:");

		textFieldFofaKey = new JTextField();
		textFieldFofaKey.setColumns(30);
		textFieldFofaKey.getDocument().addDocumentListener(new textFieldListener());

		add(lblFofaKey, new MyGridBagLayout(++rowIndex,1));
		add(textFieldFofaKey,new MyGridBagLayout(rowIndex,2));
		
		
		JLabel lblQuakeAPIKey = new JLabel("quake.360.net API Key:");

		textFieldQuakeAPIKey = new JTextField();
		textFieldQuakeAPIKey.setColumns(30);
		textFieldQuakeAPIKey.getDocument().addDocumentListener(new textFieldListener());

		add(lblQuakeAPIKey, new MyGridBagLayout(++rowIndex,1));
		add(textFieldQuakeAPIKey,new MyGridBagLayout(rowIndex,2));
		
		
		JLabel lblHunterAPIKey = new JLabel("hunter.qianxin.com API Key:");

		textFieldHunterAPIKey = new JTextField();
		textFieldHunterAPIKey.setColumns(30);
		textFieldHunterAPIKey.getDocument().addDocumentListener(new textFieldListener());

		add(lblHunterAPIKey, new MyGridBagLayout(++rowIndex,1));
		add(textFieldHunterAPIKey,new MyGridBagLayout(rowIndex,2));
		

		///////下方是JRadioButton/////

		DisplayContextMenuOfBurp = new JRadioButton("Display Context Menu Of Burp");
		DisplayContextMenuOfBurp.setSelected(true);

		add(new JLabel(""), new MyGridBagLayout(++rowIndex,1));
		add(DisplayContextMenuOfBurp, new MyGridBagLayout(rowIndex,2));



		showItemsInOne = new JRadioButton("Display Context Menu Items In One");
		showItemsInOne.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
			}
		});
		add(new JLabel(""),new MyGridBagLayout(++rowIndex,1));
		add(showItemsInOne, new MyGridBagLayout(rowIndex,2));

		JLabel label_2 = new JLabel("");

		ignoreHTTPS = new JRadioButton("Ignore HTTPS if HTTP is OK || Ignore HTTP if HTTPS is OK");
		ignoreHTTPS.setSelected(false);

		add(label_2, new MyGridBagLayout(++rowIndex,1));
		add(ignoreHTTPS, new MyGridBagLayout(rowIndex,2));


		JLabel label_3 = new JLabel("");

		ignoreHTTPStaus500 = new JRadioButton("Ignore items which Status >= 500");
		ignoreHTTPStaus500.setSelected(true);

		add(label_3, new MyGridBagLayout(++rowIndex,1));
		add(ignoreHTTPStaus500, new MyGridBagLayout(rowIndex,2));


		JLabel label_4 = new JLabel("");

		ignoreHTTPStaus400 = new JRadioButton("Ignore http Status 400(The plain HTTP request was sent to HTTPS port)");
		ignoreHTTPStaus400.setSelected(true);

		add(label_4, new MyGridBagLayout(++rowIndex,1));
		add(ignoreHTTPStaus400, new MyGridBagLayout(rowIndex,2));

		JLabel label_5 = new JLabel("");

		ignoreWrongCAHost = new JRadioButton("Ignore Host that IP Address and Certificate Authority not match");
		ignoreWrongCAHost.setSelected(false);

		add(label_5, new MyGridBagLayout(++rowIndex,1));
		add(ignoreWrongCAHost, new MyGridBagLayout(rowIndex,2));


		rdbtnSaveTrafficTo = new JRadioButton("Save traffic to Elastic");
		rdbtnSaveTrafficTo.setSelected(false);

		JLabel label_6 = new JLabel("");

		add(label_6, new MyGridBagLayout(++rowIndex,1));
		add(rdbtnSaveTrafficTo, new MyGridBagLayout(rowIndex,2));


	}
	//保存各个路径设置参数，自动保存的listener
	class textFieldListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				saveToConfigFromGUI();
				lineConfig.saveToDisk();
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				saveToConfigFromGUI();
				lineConfig.saveToDisk();
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			if (listenerIsOn) {
				saveToConfigFromGUI();
				lineConfig.saveToDisk();
			}
		}
	}
}