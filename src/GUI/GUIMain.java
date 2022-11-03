package GUI;

import java.awt.EventQueue;
import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.DBHelper;
import burp.dbFileChooser;
import config.ConfigPanel;
import dao.DomainDao;
import domain.DomainPanel;
import title.TitlePanel;


public class GUIMain extends JFrame {
	
	public static GUIMain instance;

	//当多个实例都有相同的是static field时，对象间对static属性的修改会互相影响，因为多个对象共享一个属性的copy！！
	public DomainPanel domainPanel;
	public TitlePanel titlePanel;
	public ToolPanel toolPanel;
	public ConfigPanel configPanel;

	public File currentDBFile;
	public ProjectMenu projectMenu;

	public PrintWriter stdout;
	public PrintWriter stderr;

	public dbFileChooser dbfc = new dbFileChooser();

	public DomainPanel getDomainPanel() {
		return domainPanel;
	}
	public void setDomainPanel(DomainPanel domainPanel) {
		this.domainPanel = domainPanel;
	}
	public TitlePanel getTitlePanel() {
		return titlePanel;
	}
	public void setTitlePanel(TitlePanel titlePanel) {
		this.titlePanel = titlePanel;
	}
	public ToolPanel getToolPanel() {
		return toolPanel;
	}
	public void setToolPanel(ToolPanel toolPanel) {
		this.toolPanel = toolPanel;
	}
	public ConfigPanel getConfigPanel() {
		return configPanel;
	}
	public void setConfigPanel(ConfigPanel configPanel) {
		this.configPanel = configPanel;
	}
	public File getCurrentDBFile() {
		return currentDBFile;
	}
	public void setCurrentDBFile(File currentDBFile) {
		this.currentDBFile = currentDBFile;
	}
	public ProjectMenu getProjectMenu() {
		return projectMenu;
	}
	public void setProjectMenu(ProjectMenu projectMenu) {
		this.projectMenu = projectMenu;
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
		
		instance = this;
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

	public void LoadData(String dbFilePath){
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

	private boolean LoadDataPrivate(String dbFilePath){
		try {//这其中的异常会导致burp退出
			System.out.println("=================================");
			System.out.println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			BurpExtender.getStdout().println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			currentDBFile = new File(dbFilePath);
			if (!currentDBFile.exists()){
				BurpExtender.getStdout().println("==Load database file [" + dbFilePath+"] failed,file does not exist "+Commons.getNowTimeString()+"==");
				return false;
			}
			//TODO 重新梳理数据加载逻辑
			domainPanel.LoadTargetsData(currentDBFile);
			domainPanel.LoadDomainData(currentDBFile);//TODO
			titlePanel.loadData(currentDBFile);
			configPanel.loadData(currentDBFile);

			ConfigPanel.getLineConfig().setDbfilepath(currentDBFile.getAbsolutePath());
			displayProjectName();
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
	public void displayProjectName() {
		if (domainPanel.getDomainResult() !=null){
			String name = currentDBFile.getName();
			//String newName = String.format(BurpExtender.getFullExtenderName()+" [%s]",name);
			//v2021.8的版本中，邮件菜单会用到插件名称，所以减小名称的长度
			String newName = String.format(BurpExtender.getExtenderName()+" [%s]",name);

			BurpExtender.getCallbacks().setExtensionName(newName); //新插件名称
			getProjectMenu().AddDBNameMenuItem(name);
			getProjectMenu().AddDBNameTab(name);
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
					if (dbHelper.saveDomainObject(domainPanel.getDomainResult())){
						stdout.println("Save Domain Only Success! "+ Commons.getNowTimeString());
						return true;
					}
				}else {
					boolean targetSaved = dbHelper.saveTargets(domainPanel.getTargetTable().getTargetModel());
					boolean domainSaved = dbHelper.saveDomainObject(domainPanel.getDomainResult());
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
