package GUI;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.DBHelper;
import burp.dbFileChooser;
import domain.DomainPanel;
import title.TitlePanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;


public class GUI extends JFrame {

	protected static DomainPanel domainPanel;
	public static TitlePanel titlePanel;
	public static File currentDBFile;
	protected PrintWriter stdout;
	protected PrintWriter stderr;
	public dbFileChooser dbfc = new dbFileChooser();
	protected static ProjectMenu projectMenu;
	private ToolPanel toolPanel;

	public static ProjectMenu getProjectMenu() {
		return projectMenu;
	}

	public static DomainPanel getDomainPanel() {
		return domainPanel;
	}

	public static TitlePanel getTitlePanel() {
		return titlePanel;
	}


	public ToolPanel getToolPanel() {
		return toolPanel;
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
		tabbedWrapper.setName("DomainHunterPro");//需要在从burpFrame向下查找该插件时用到
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		setContentPane(tabbedWrapper);
		domainPanel = new DomainPanel();
		titlePanel = new TitlePanel();
		toolPanel = new ToolPanel();
		tabbedWrapper.addTab("Domains", null, domainPanel, null);
		tabbedWrapper.addTab("Titles", null, titlePanel, null);
		tabbedWrapper.addTab("Tools", null,toolPanel,null);

		projectMenu = new ProjectMenu(this);
		projectMenu.Add();

	}

	public boolean LoadData(String dbFilePath){
		try {//这其中的异常会导致burp退出
			BurpExtender.clearQueue();//更换DB文件前进行，否则Queue中会包含之前的数据。
			System.out.println("=================================");
			System.out.println("==Start Loading Data From: " + dbFilePath+"==");
			stdout.println("==Start Loading Data From: " + dbFilePath+"==");
			currentDBFile = new File(dbFilePath);
			DBHelper dbhelper = new DBHelper(dbFilePath);
			domainPanel.setDomainResult(dbhelper.getDomainObj());
			domainPanel.showToDomainUI();
			titlePanel.showToTitleUI(dbhelper.getTitles());
			GUI.setCurrentDBFile(currentDBFile);
			GUI.displayProjectName();
			BurpExtender.saveDBfilepathToExtension();
			System.out.println("==End Loading Data From: "+ dbFilePath +"==");//输出到debug console
			stdout.println("==End Loading Data From: "+ dbFilePath +"==");
			return true;
		} catch (Exception e) {
			stdout.println("Loading Failed!");
			e.printStackTrace();//输出到debug console
			e.printStackTrace(stderr);
			return false;
		}
	}

	//显示项目名称，加载多个该插件时，进行区分，避免混淆
	public static void displayProjectName() {
		if (DomainPanel.getDomainResult() !=null){
			//String name = GUI.currentDBFile.getName();
			String name = DomainPanel.getDomainResult().getProjectName();
			String newName = String.format(BurpExtender.getFullExtenderName()+
					" [%s]",name);
			
			BurpExtender.getCallbacks().setExtensionName(newName); //新插件名称
			GUI.getProjectMenu().AddDBNameMenuItem(name);
			GUI.getProjectMenu().AddDBNameTab(name);
			//gui.repaint();//NO need
		}
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
