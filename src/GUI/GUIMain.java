package GUI;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.DBHelper;
import burp.dbFileChooser;
import config.ConfigPanel;
import domain.DomainPanel;
import domain.target.TargetTableModel;
import title.TitlePanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.Map;


public class GUIMain extends JFrame {

	public static DomainPanel domainPanel;
	public static TitlePanel titlePanel;
	private static File currentDBFile;
	public static ProjectMenu projectMenu;
	
	private PrintWriter stdout;
	private PrintWriter stderr;
	public static dbFileChooser dbfc = new dbFileChooser();
	
	private ToolPanel toolPanel;
	private ConfigPanel configPanel;

	public static ProjectMenu getProjectMenu() {
		return projectMenu;
	}

	public static void setProjectMenu(ProjectMenu projectMenu) {
		GUIMain.projectMenu = projectMenu;
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
	
	public ConfigPanel getConfigPanel() {
		return configPanel;
	}

	public void setConfigPanel(ConfigPanel configPanel) {
		this.configPanel = configPanel;
	}

	public static File getCurrentDBFile() {
		return currentDBFile;
	}

	public static void setCurrentDBFile(File currentDBFile) {
		GUIMain.currentDBFile = currentDBFile;
	}

	/**
	 * Create the frame.
	 */
	public GUIMain() {//构造函数
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
		configPanel = new ConfigPanel();
		tabbedWrapper.addTab("Domains", null, domainPanel, null);
		tabbedWrapper.addTab("Titles", null, titlePanel, null);
		tabbedWrapper.addTab("Tools", null,toolPanel,null);
		tabbedWrapper.addTab("Config", null,configPanel,null);
	}
	/**
	 * 仅仅锁住图形界面，不影响后台处理数据
	 */
	public void lockUnlock() {
		if (this.getContentPane().isEnabled()) {
			((JTabbedPane)this.getContentPane()).addTab("Locked",null,new JPanel(),null);
			int size = ((JTabbedPane)this.getContentPane()).getTabCount();
			((JTabbedPane)this.getContentPane()).setSelectedIndex(size-1);
			this.getContentPane().setEnabled(false);
			projectMenu.setText("DomainHunter*");
			projectMenu.lockMenu.setText("Unlock");
			ConfigPanel.DisplayContextMenuOfBurp.setSelected(false);//不显示burp右键菜单
		}else {
			this.getContentPane().setEnabled(true);
			int size = ((JTabbedPane)this.getContentPane()).getTabCount();
			((JTabbedPane)this.getContentPane()).removeTabAt(size-1);
			((JTabbedPane)this.getContentPane()).setSelectedIndex(0);
			projectMenu.lockMenu.setText("Lock");
			projectMenu.setText("DomainHunter");
			ConfigPanel.DisplayContextMenuOfBurp.setSelected(true);//显示右键菜单
		}
	}

	public static void LoadData(String dbFilePath){
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			@Override
			protected Map doInBackground() throws Exception {
				LoadDataPrivate(dbFilePath);
				return null;
			}
			@Override
			protected void done() {
			}
		};
		worker.execute();
	}

	private static boolean LoadDataPrivate(String dbFilePath){
		try {//这其中的异常会导致burp退出
			System.out.println("=================================");
			System.out.println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			BurpExtender.getStdout().println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			currentDBFile = new File(dbFilePath);
			if (!currentDBFile.exists()){
				BurpExtender.getStdout().println("==Load database file [" + dbFilePath+"] failed,file does not exist "+Commons.getNowTimeString()+"==");
				return false;
			}
			GUIMain.setCurrentDBFile(currentDBFile);

			DBHelper dbhelper = new DBHelper(dbFilePath);

			DomainPanel.setDomainResult(dbhelper.getDomainObj());//为了兼容就版本，
			DomainPanel.getTargetTable().loadData(dbhelper.getTargets());
			domainPanel.LoadData(dbhelper.getDomainObj());
			titlePanel.loadData(dbhelper.getTitles());
			

			ConfigPanel.getLineConfig().setDbfilepath(currentDBFile.getAbsolutePath());
			GUIMain.displayProjectName();
			System.out.println("==End Loading Data From: "+ dbFilePath+" "+Commons.getNowTimeString() +"==");//输出到debug console
			BurpExtender.getStdout().println("==End Loading Data From: "+ dbFilePath+" "+Commons.getNowTimeString() +"==");
			return true;
		} catch (Exception e) {
			BurpExtender.getStdout().println("Loading Failed!");
			e.printStackTrace();//输出到debug console
			e.printStackTrace(BurpExtender.getStderr());
			return false;
		}
	}

	//显示项目名称，加载多个该插件时，进行区分，避免混淆
	public static void displayProjectName() {
		if (DomainPanel.getDomainResult() !=null){
			String name = GUIMain.currentDBFile.getName();
			//String newName = String.format(BurpExtender.getFullExtenderName()+" [%s]",name);
			//v2021.8的版本中，邮件菜单会用到插件名称，所以减小名称的长度
			String newName = String.format(BurpExtender.getExtenderName()+" [%s]",name);
			
			BurpExtender.getCallbacks().setExtensionName(newName); //新插件名称
			GUIMain.getProjectMenu().AddDBNameMenuItem(name);
			GUIMain.getProjectMenu().AddDBNameTab(name);
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
					boolean targetSaved = dbHelper.saveTargets(DomainPanel.getTargetTable().getTargetModel());
					boolean domainSaved = dbHelper.saveDomainObject(DomainPanel.getDomainResult());
					boolean titleSaved = dbHelper.addTitles(TitlePanel.getTitleTableModel().getLineEntries());
					if (targetSaved && domainSaved && titleSaved){
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
					GUIMain frame = new GUIMain();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


}
