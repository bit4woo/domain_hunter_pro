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
import javax.swing.JTextArea;
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
	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain gui;

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


		//这里的修改也会触发textFieldListener监听器。
		//由于我们是多个组件共用一个保存逻辑，当前对一个组件设置值的时候，触发保存，从而导致整体数据的修改！！！
		//所以和domain和title中一样，显示数据时关闭监听器。
		listenerIsOn = false;
		ToolPanel.inputTextArea.setText(lineConfig.getToolPanelText());

		BrowserPath.setText(lineConfig.getBrowserPath());

		if (!lineConfig.getNmapPath().contains("{host}")) {//兼容新旧版本，
			lineConfig.setNmapPath(LineConfig.defaultNmap);
		}
		listenerIsOn = true;//显示完毕后打开监听器。
		
		 buttonGroup = new ButtonGroup();
	}
	
	private void addConfigItem(ConfigEntry config,int rowIndex) {
		String value = config.getValue();
		if (value.equalsIgnoreCase("true")||value.equalsIgnoreCase("false")) {
			JRadioButton radioButton = new JRadioButton(config.getKey());
			radioButton.setSelected(Boolean.parseBoolean(value));
			radioButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//如何
				}
			});
			
			if (config.getKey().contains("Ignore HTTP")) {
				buttonGroup.add(radioButton);
			}
			
			add(radioButton, new MyGridBagLayout(++rowIndex,2));
		}else {
			JLabel label = new JLabel(config.getKey());

	        JTextField textField = new JTextField();
	        textField.setText(config.getValue());
	        textField.setToolTipText(config.getComment());
	        textField.setColumns(50);

	        // 添加监听器，监听内容变化
	        textField.getDocument().addDocumentListener(new DocumentListener() {
	            @Override
	            public void insertUpdate(DocumentEvent e) {
	            	config.setValue(textField.getText());
	            	if (listenerIsOn) {
	    				gui.getDataLoadManager().saveCurrentConfig(null);
	    			}
	            }

	            @Override
	            public void removeUpdate(DocumentEvent e) {
	            	config.setValue(textField.getText());
	            	if (listenerIsOn) {
	    				gui.getDataLoadManager().saveCurrentConfig(null);
	    			}
	            }

	            @Override
	            public void changedUpdate(DocumentEvent e) {
	            	config.setValue(textField.getText());
	            	if (listenerIsOn) {
	    				gui.getDataLoadManager().saveCurrentConfig(null);
	    			}
	            }
	        });

	        // 添加到界面
			add(label, new MyGridBagLayout(++rowIndex,1));
			add(textField, new MyGridBagLayout(rowIndex,2));
		}
    }


	public ConfigManager getConfigFromGUI() {
		lineConfig.setBrowserPath(BrowserPath.getText());
		lineConfig.setDirSearchPath(textFieldDirSearch.getText());
		lineConfig.setBruteDict(textFieldDirBruteDict.getText());
		lineConfig.setNmapPath(textFieldPortScanner.getText());
		lineConfig.setElasticApiUrl(textFieldElasticURL.getText().trim());
		lineConfig.setElasticUsernameAndPassword(textFieldElasticUserPass.getText());
		lineConfig.setUploadApiToken(textFieldUploadApiToken.getText());

		lineConfig.setFofaEmail(textFieldFofaEmail.getText());
		lineConfig.setFofaKey(textFieldFofaKey.getText());

		lineConfig.setQuake_360_APIKey(textField360QuakeAPIKey.getText());
		lineConfig.setTi_360_APIKey(textField360TiAPIKey.getText());

		lineConfig.setQianxin_hunter_APIKey(textFieldQianxinHunterAPIKey.getText());
		lineConfig.setQianxin_ti_APIKey(textFieldQianxinTiAPIKey.getText());

		lineConfig.setZoomEyeAPIKey(textFieldZoomEyeAPIKey.getText());
		lineConfig.setShodanAPIKey(textFieldShodanAPIKey.getText());

		lineConfig.setProxy(textFieldProxyForGetCert.getText());

		lineConfig.setToolPanelText(((SuperJTextArea)ToolPanel.inputTextArea).getTextAsDisplay());
		lineConfig.setShowItemsInOne(showItemsInOne.isSelected());
		lineConfig.setEnableElastic(rdbtnSaveTrafficTo.isSelected());
		return lineConfig;
	}	

	public ConfigPanel(GUIMain gui) {
		this.gui = gui;
		setLayout(new GridBagLayout());

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
	}
}