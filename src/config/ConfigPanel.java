package config;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.io.Files;

import GUI.GUIMain;
import GUI.MyGridBagLayout;
import Tools.SuperJTextArea;
import Tools.ToolPanel;
import base.MyFileFilter;
import burp.BurpExtender;

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
	public static JRadioButton ignoreHTTP;
	public static JRadioButton ignoreHTTPStaus500;
	public static JRadioButton ignoreHTTPStaus400;
	public static JRadioButton ignoreWrongCAHost;
	
	public static JRadioButton removeItemIfIgnored;
	
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
	public static JTextField textFieldProxyForGetCert;

	
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

		//History.setInstance(lineConfig.getSearchHistory());

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
		textFieldProxyForGetCert.setText(lineConfig.getProxy());

		showItemsInOne.setSelected(lineConfig.isShowItemsInOne());
		rdbtnSaveTrafficTo.setSelected(lineConfig.isEnableElastic());
		listenerIsOn = true;//显示完毕后打开监听器。
	}


	public LineConfig getConfigFromGUI() {
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
		lineConfig.setProxy(textFieldProxyForGetCert.getText());

		lineConfig.setToolPanelText(((SuperJTextArea)ToolPanel.inputTextArea).getTextAsDisplay());
		lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
		lineConfig.setEnableElastic(rdbtnSaveTrafficTo.isSelected());
		return lineConfig;
	}	

	public ConfigPanel(GUIMain gui) {
		this.gui = gui;
		setLayout(new GridBagLayout());
		JLabel lblBrowser = new JLabel("Browser Path:");

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

		JLabel lblDirBruteDict = new JLabel("Dir Brute Dict:");

		textFieldDirBruteDict = new JTextField();
		textFieldDirBruteDict.setToolTipText("path of dict");
		textFieldDirBruteDict.setColumns(50);
		textFieldDirBruteDict.getDocument().addDocumentListener(new textFieldListener());

		JLabel lblElasticURL = new JLabel("Elastic URL:");

		textFieldElasticURL = new JTextField();
		textFieldElasticURL.setToolTipText("URL of elastic API");
		textFieldElasticURL.setText("http://10.12.72.55:9200/");
		textFieldElasticURL.getDocument().addDocumentListener(new textFieldListener());
		//textFieldElasticURL.addFocusListener(new JTextFieldHintListener(textFieldElasticURL,"http://10.12.72.55:9200/"));
		//这个HintListener的操作，会触发DocumentListener的insertUpdate操作！
		textFieldElasticURL.setColumns(50);

		JLabel lblDirElasticUserPass = new JLabel("Elastic Username Password:");

		textFieldElasticUserPass = new JTextField();
		textFieldElasticUserPass.setText("elastic:changeme");
		textFieldElasticUserPass.setToolTipText("username and password of elastic API");
		textFieldElasticUserPass.getDocument().addDocumentListener(new textFieldListener());
		//textFieldElasticUserPass.addFocusListener(new JTextFieldHintListener(textFieldElasticUserPass,"elastic:changeme"));
		textFieldElasticUserPass.setColumns(50);

		JLabel lblUploadAPIToken = new JLabel("Upload API Token:");

		textFieldUploadApiToken = new JTextField();
		textFieldUploadApiToken.setToolTipText("token of upload api");
		textFieldUploadApiToken.getDocument().addDocumentListener(new textFieldListener());
		textFieldUploadApiToken.setColumns(50);

		JLabel lblUploadUrl = new JLabel("Upload URL:");

		textFieldUploadURL = new JTextField();
		textFieldUploadURL.setColumns(30);
		textFieldUploadURL.setToolTipText("input upload url here");
		textFieldUploadURL.getDocument().addDocumentListener(new textFieldListener());


		JLabel lblFofaEmail = new JLabel("Fofa Email:");

		textFieldFofaEmail = new JTextField();
		textFieldFofaEmail.setColumns(30);
		textFieldFofaEmail.getDocument().addDocumentListener(new textFieldListener());


		JLabel lblFofaKey = new JLabel("Fofa Key:");

		textFieldFofaKey = new JTextField();
		textFieldFofaKey.setColumns(30);
		textFieldFofaKey.getDocument().addDocumentListener(new textFieldListener());


		JLabel lblQuakeAPIKey = new JLabel("quake.360.net API Key:");

		textFieldQuakeAPIKey = new JTextField();
		textFieldQuakeAPIKey.setColumns(30);
		textFieldQuakeAPIKey.getDocument().addDocumentListener(new textFieldListener());


		JLabel lblHunterAPIKey = new JLabel("hunter.qianxin.com API Key:");

		textFieldHunterAPIKey = new JTextField();
		textFieldHunterAPIKey.setColumns(30);
		textFieldHunterAPIKey.getDocument().addDocumentListener(new textFieldListener());

		
		JLabel lblProxyForGetCert = new JLabel("proxy for get certificates:");

		textFieldProxyForGetCert = new JTextField();
		textFieldProxyForGetCert.setColumns(30);
		textFieldProxyForGetCert.getDocument().addDocumentListener(new textFieldListener());

		///////下方是JRadioButton/////

		DisplayContextMenuOfBurp = new JRadioButton("Display Context Menu Of Burp");
		DisplayContextMenuOfBurp.setSelected(true);



		showItemsInOne = new JRadioButton("Display Context Menu Items In One");
		showItemsInOne.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
			}
		});


		ignoreHTTPS = new JRadioButton("Ignore HTTPS if HTTP is OK");
		ignoreHTTPS.setSelected(false);
		ignoreHTTPS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (ignoreHTTPS.isSelected()) {
					ignoreHTTP.setSelected(false);
				}
			}
		});

		ignoreHTTP = new JRadioButton("Ignore HTTP if HTTPS is OK");
		ignoreHTTP.setSelected(true);
		ignoreHTTP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (ignoreHTTP.isSelected()) {
					ignoreHTTPS.setSelected(false);
				}
			}
		});

		ignoreHTTPStaus500 = new JRadioButton("Ignore items which Status >= 500");
		ignoreHTTPStaus500.setSelected(true);


		ignoreHTTPStaus400 = new JRadioButton("Ignore items that http Status is 400(The plain HTTP request was sent to HTTPS port)");
		ignoreHTTPStaus400.setSelected(true);



		ignoreWrongCAHost = new JRadioButton("Ignore items that IP Address and Certificate Authority do not match");
		ignoreWrongCAHost.setSelected(false);


		removeItemIfIgnored = new JRadioButton("Remove item if ignored(Marked as check done by default)");
		removeItemIfIgnored.setSelected(true);
		

		rdbtnSaveTrafficTo = new JRadioButton("Save traffic to Elastic");
		rdbtnSaveTrafficTo.setSelected(false);

		JButton loadConfig = new JButton("Load Config");
		loadConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc=new JFileChooser(LineConfig.localdir);
				MyFileFilter filter = new MyFileFilter("config"); //文件后缀过滤器  
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				fc.setDialogTitle("Chose Config File");
				fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
				if(fc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){
					try {
						File file=fc.getSelectedFile();
						gui.getDataLoadManager().loadConfigToHunter(file.getAbsolutePath());
					} catch (Exception e1) {
						e1.printStackTrace(stderr);
					}
				}
			}
		});

		JButton saveConfig = new JButton("Save Config As");
		saveConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//saveConfigToBurp();
				JFileChooser fc=new JFileChooser();
				MyFileFilter filter = new MyFileFilter("json"); //文件后缀过滤器  
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				fc.setDialogTitle("Save Config To A File:");
				fc.setDialogType(JFileChooser.SAVE_DIALOG);
				if(fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION){
					File file=fc.getSelectedFile();

					if(!(file.getName().toLowerCase().endsWith(".json"))){
						file=new File(fc.getCurrentDirectory(),file.getName()+".json");
					}
					gui.getDataLoadManager().saveCurrentConfig(null);//burp 用户目录下
					gui.getDataLoadManager().saveCurrentConfig(file.getAbsolutePath());
					String content= lineConfig.ToJson();
					try{
						if(file.exists()){
							int result = JOptionPane.showConfirmDialog(null,"Are you sure to overwrite this file ?");
							if (result == JOptionPane.YES_OPTION) {
								file.createNewFile();
							}else {
								return;
							}
						}else {
							file.createNewFile();
						}

						Files.write(content.getBytes(), file);
					}catch(Exception e1){
						e1.printStackTrace(stderr);
					}
				}

			}});
		
		int rowIndex = 0;
		add(loadConfig, new MyGridBagLayout(++rowIndex,1));
		add(saveConfig, new MyGridBagLayout(++rowIndex,1));
		
		add(lblBrowser, new MyGridBagLayout(++rowIndex,1));
		add(BrowserPath, new MyGridBagLayout(rowIndex,2));
		
		add(lblPortScanner, new MyGridBagLayout(++rowIndex,1));
		add(textFieldPortScanner, new MyGridBagLayout(rowIndex,2));
		
		add(lblDirSearch, new MyGridBagLayout(++rowIndex,1));
		add(textFieldDirSearch, new MyGridBagLayout(rowIndex,2));
		
		add(lblDirBruteDict, new MyGridBagLayout(++rowIndex,1));
		add(textFieldDirBruteDict, new MyGridBagLayout(rowIndex,2));
		
		add(lblElasticURL, new MyGridBagLayout(++rowIndex,1));
		add(textFieldElasticURL, new MyGridBagLayout(rowIndex,2));
		
		add(lblDirElasticUserPass, new MyGridBagLayout(++rowIndex,1));
		add(textFieldElasticUserPass, new MyGridBagLayout(rowIndex,2));
		
		add(lblUploadAPIToken, new MyGridBagLayout(++rowIndex,1));
		add(textFieldUploadApiToken, new MyGridBagLayout(rowIndex,2));
		
		add(lblUploadUrl, new MyGridBagLayout(++rowIndex,1));
		add(textFieldUploadURL,new MyGridBagLayout(rowIndex,2));
		
		add(lblFofaEmail, new MyGridBagLayout(++rowIndex,1));
		add(textFieldFofaEmail,new MyGridBagLayout(rowIndex,2));
		add(lblFofaKey, new MyGridBagLayout(++rowIndex,1));
		add(textFieldFofaKey,new MyGridBagLayout(rowIndex,2));
		
		add(lblQuakeAPIKey, new MyGridBagLayout(++rowIndex,1));
		add(textFieldQuakeAPIKey,new MyGridBagLayout(rowIndex,2));
		add(lblHunterAPIKey, new MyGridBagLayout(++rowIndex,1));
		add(textFieldHunterAPIKey,new MyGridBagLayout(rowIndex,2));
		
		add(lblProxyForGetCert, new MyGridBagLayout(++rowIndex,1));
		add(textFieldProxyForGetCert,new MyGridBagLayout(rowIndex,2));
		
		add(DisplayContextMenuOfBurp, new MyGridBagLayout(++rowIndex,2));
		add(showItemsInOne, new MyGridBagLayout(++rowIndex,2));
		add(ignoreHTTPS, new MyGridBagLayout(++rowIndex,2));
		add(ignoreHTTP, new MyGridBagLayout(++rowIndex,2));
		add(ignoreHTTPStaus500, new MyGridBagLayout(++rowIndex,2));
		add(ignoreHTTPStaus400, new MyGridBagLayout(++rowIndex,2));
		add(ignoreWrongCAHost, new MyGridBagLayout(++rowIndex,2));
		add(removeItemIfIgnored,new MyGridBagLayout(++rowIndex,2));
		add(rdbtnSaveTrafficTo, new MyGridBagLayout(++rowIndex,2));

	}
	//保存各个路径设置参数，自动保存的listener
	class textFieldListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				gui.getDataLoadManager().saveCurrentConfig(null);
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if (listenerIsOn) {
				gui.getDataLoadManager().saveCurrentConfig(null);
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			if (listenerIsOn) {
				gui.getDataLoadManager().saveCurrentConfig(null);
			}
		}
	}
}