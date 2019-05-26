package burp;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;


public class GUI extends JFrame {

	protected static DomainPanel domainPanel;
	protected static TitlePanel titlePanel;
	protected static File currentDBFile;
	protected PrintWriter stdout;
	protected PrintWriter stderr;
	protected dbFileChooser dbfc = new dbFileChooser();
	protected ProjectMenu projectMenu;

	public ProjectMenu getProjectMenu() {
		return projectMenu;
	}

	public static DomainPanel getDomainPanel() {
		return domainPanel;
	}

	public static TitlePanel getTitlePanel() {
		return titlePanel;
	}


	public static File getCurrentDBFile() {
		return currentDBFile;
	}

	public static void setCurrentDBFile(File currentDBFile) {
		GUI.currentDBFile = currentDBFile;
	}

	/**
	 * Create the frame.
	 */
	public GUI() {//构造函数
		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		JTabbedPane tabbedWrapper = new JTabbedPane();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		setContentPane(tabbedWrapper);
		domainPanel = new DomainPanel();
		titlePanel = new TitlePanel();
		tabbedWrapper.addTab("Domains", null, domainPanel, null);
		tabbedWrapper.addTab("Titles", null, titlePanel, null);

		projectMenu = new ProjectMenu(this);
		projectMenu.Add();

	}

	public boolean LoadData(String dbFilePath){
		try {//这其中的异常会导致burp退出
			stdout.println("Loading Data From: " + dbFilePath);
			DBHelper dbhelper = new DBHelper(dbFilePath);
			domainPanel.setDomainResult(dbhelper.getDomainObj());
			domainPanel.showToDomainUI();
			titlePanel.showToTitleUI(dbhelper.getTitles());
			currentDBFile = new File(dbFilePath);
			stdout.println("Loading Project ["+domainPanel.domainResult.projectName+"] Finished From File "+ dbFilePath);
			return true;
		} catch (Exception e) {
			stdout.println("Loading Failed!");
			e.printStackTrace(stderr);
			return false;
		}
	}


	public void saveDBfilepathToExtension() {
		//to save domain result to extensionSetting
		//仅仅存储sqllite数据库的名称,也就是domainResult的项目名称
		if (currentDBFile != null)
			BurpExtender.getCallbacks().saveExtensionSetting("domainHunterpro", currentDBFile.getAbsolutePath());
	}


	/*
	使用数据模型监听后，不需再自行单独保存当前项目了。
	但是需要用于另存为，单独保存域名(和saveDomainOnly) 2个功能。
	都需要文件选择对话框
	 */
	public Boolean saveData(String dbFilePath,boolean domainOnly) {
		try{
			if (dbFilePath != null){
				DBHelper dbHelper = new DBHelper(dbFilePath.toString());
				if (domainOnly){
					if (dbHelper.saveDomainObject(DomainPanel.getDomainResult())){
						stdout.println("Save Domain Only Success! "+ Commons.getNowTimeString());
						return true;
					}
				}else {
					boolean domainSaved = dbHelper.saveDomainObject(DomainPanel.getDomainResult());
					boolean titleSaved = dbHelper.addTitles(TitlePanel.getTitleTableModel().getLineEntries());
					if (domainSaved && titleSaved){
						stdout.println("Save Domain And Title Success! "+ Commons.getNowTimeString());
						return true;
					}
				}
			}
			return false;
		}catch(Exception e1){
			e1.printStackTrace(stderr);
			return false;
		}
	}



	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
