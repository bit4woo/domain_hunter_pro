package config;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import burp.BurpExtender;
import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import GUI.GUIMain;
import GUI.MyGridBagLayout;
import base.MyFileFilter;

public class ConfigPanel extends JPanel{
	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain gui;

	ButtonGroup buttonGroup;

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

	private void addConfigItem(ConfigEntry config,int rowIndex) {
		String value = config.getValue();
		if (value.equalsIgnoreCase("true")||value.equalsIgnoreCase("false")) {
			JRadioButton radioButton = new JRadioButton(config.getKey());
			radioButton.setSelected(Boolean.parseBoolean(value));
			radioButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					config.setValue(radioButton.isSelected()+"");
					BurpExtender.getDataLoadManager().saveCurrentConfig(null);
				}
			});

			if (config.getKey().contains("Ignore HTTP")) {
				buttonGroup.add(radioButton);
			}

			add(radioButton, new MyGridBagLayout(++rowIndex,2));
		}else {
			JLabel label = new JLabel(config.getKey()+":");

			JTextField textField = new JTextField();
			textField.setText(config.getValue());
			textField.setToolTipText(config.getComment());
			textField.setColumns(50);

			// 添加监听器，监听内容变化
			textField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					config.setValue(textField.getText());
					//TODO Check
					BurpExtender.getDataLoadManager().saveCurrentConfig(null);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					config.setValue(textField.getText());
					BurpExtender.getDataLoadManager().saveCurrentConfig(null);
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					config.setValue(textField.getText());
					BurpExtender.getDataLoadManager().saveCurrentConfig(null);
				}
			});

			// 添加到界面
			add(label, new MyGridBagLayout(rowIndex,1));
			add(textField, new MyGridBagLayout(rowIndex,2));
		}
	}

	public ConfigPanel(GUIMain gui) {
		this.gui = gui;
		setLayout(new GridBagLayout());
		buttonGroup = new ButtonGroup();

		int rowIndex = 0;
		for (JButton button:creatControlButtons()) {
			add(button, new MyGridBagLayout(++rowIndex,1));
		}
		//需要提取初始化ConfigManager
		for (ConfigEntry config:ConfigManager.getConfigList()) {
			addConfigItem(config,++rowIndex);
		}
	}

	public List<JButton> creatControlButtons() {
		List<JButton> result = new ArrayList<>();
		JButton loadConfig = new JButton("Load Config");
		loadConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc=new JFileChooser(ConfigManager.localdir);
				MyFileFilter filter = new MyFileFilter("config"); //文件后缀过滤器  
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				fc.setDialogTitle("Chose Config File");
				fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
				if(fc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){
					try {
						File file=fc.getSelectedFile();
						String content =FileUtils.readFileToString(file);
						ConfigManager.FromJson(content);
						gui.renewConfigPanel();
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
					BurpExtender.getDataLoadManager().saveCurrentConfig(null);//burp 用户目录下
					BurpExtender.getDataLoadManager().saveCurrentConfig(file.getAbsolutePath());
					String content= ConfigManager.ToJson();
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
			}
		});
		
		JButton restDefault = new JButton("Restore To Default");
		restDefault.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ConfigManager.initDefault();
				gui.renewConfigPanel();
			}
		});

		result.add(loadConfig);
		result.add(saveConfig);
		result.add(restDefault);
		return result;
	}
}