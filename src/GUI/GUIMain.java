package GUI;

import java.awt.EventQueue;
import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import Tools.ToolPanel;
import burp.BurpExtender;
import burp.Commons;
import burp.IBurpExtender;
import burp.IHttpRequestResponse;
import burp.dbFileChooser;
import config.ConfigPanel;
import config.DataLoadManager;
import dao.DomainDao;
import dao.TargetDao;
import dao.TitleDao;
import domain.DomainPanel;
import domain.DomainProducer;
import title.TitlePanel;


public class GUIMain extends JFrame {

	//当多个实例都有相同的是static field时，对象间对static属性的修改会互相影响，因为多个对象共享一个属性的copy！！
	public IBurpExtender burp;
	public DomainPanel domainPanel;
	public TitlePanel titlePanel;
	public ToolPanel toolPanel;
	public ConfigPanel configPanel;

	public File currentDBFile;
	public ProjectMenu projectMenu;

	public PrintWriter stdout;
	public PrintWriter stderr;

	public dbFileChooser dbfc = new dbFileChooser();
	//use to store messageInfo
	private Set<String> httpsChecked = new CopyOnWriteArraySet<>();

	private BlockingQueue<IHttpRequestResponse> liveinputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//use to store messageInfo of proxy live
	private BlockingQueue<IHttpRequestResponse> inputQueue = new LinkedBlockingQueue<IHttpRequestResponse>();
	//temp variable to identify checked https用于记录已经做过HTTPS证书信息获取的httpService

	private DomainProducer liveAnalysisTread;
	private DataLoadManager dataLoadManager;

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

	public Set<String> getHttpsChecked() {
		return httpsChecked;
	}

	public void setHttpsChecked(Set<String> httpsChecked) {
		this.httpsChecked = httpsChecked;
	}

	public BlockingQueue<IHttpRequestResponse> getLiveinputQueue() {
		return liveinputQueue;
	}

	public void setLiveinputQueue(BlockingQueue<IHttpRequestResponse> liveinputQueue) {
		this.liveinputQueue = liveinputQueue;
	}

	public BlockingQueue<IHttpRequestResponse> getInputQueue() {
		return inputQueue;
	}

	public void setInputQueue(BlockingQueue<IHttpRequestResponse> inputQueue) {
		this.inputQueue = inputQueue;
	}

	public DomainProducer getLiveAnalysisTread() {
		return liveAnalysisTread;
	}

	public void setLiveAnalysisTread(DomainProducer liveAnalysisTread) {
		this.liveAnalysisTread = liveAnalysisTread;
	}

	public DataLoadManager getDataLoadManager() {
		return dataLoadManager;
	}
	public void setDataLoadManager(DataLoadManager dataLoadManager) {
		this.dataLoadManager = dataLoadManager;
	}
	public void startLiveCapture() {
		liveAnalysisTread = new DomainProducer(this, liveinputQueue, 9999);//必须是9999，才能保证流量进程不退出。
		liveAnalysisTread.start();
	}

	public void stopLiveCapture() {
		if (null != liveAnalysisTread) {
			liveAnalysisTread.interrupt();
			//9999线程只能这样结束，不受stopflag的影响
		}
	}

	public GUIMain(IBurpExtender burp) {//构造函数
		this.burp = burp;
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
		domainPanel = new DomainPanel(this);
		titlePanel = new TitlePanel(this);
		toolPanel = new ToolPanel(this);
		configPanel = new ConfigPanel(this);
		tabbedWrapper.addTab("Domains", null, domainPanel, null);
		tabbedWrapper.addTab("Titles", null, titlePanel, null);
		tabbedWrapper.addTab("Tools", null,toolPanel,null);
		tabbedWrapper.addTab("Config", null,configPanel,null);

		setProjectMenu(new ProjectMenu(this));
		getProjectMenu().Add();
		
		dataLoadManager = DataLoadManager.loadFromDisk(this);
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




	/**
	 * 加载数据库文件：
	 * 1、加载target对象到DomainPanel中的table内
	 * 2、加载domainManager对象到DomainPanel中的文本框
	 * 3、加载Title数据到TitlePanel
	 * @param dbFilePath
	 */
	public boolean loadDataBase(String dbFilePath){
		try {//这其中的异常会导致burp退出
			System.out.println("=================================");
			System.out.println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			BurpExtender.getStdout().println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			currentDBFile = new File(dbFilePath);
			if (!currentDBFile.exists()){
				BurpExtender.getStdout().println("==Load database file [" + dbFilePath+"] failed,file does not exist "+Commons.getNowTimeString()+"==");
				return false;
			}

			domainPanel.LoadTargetsData(currentDBFile.toString());
			domainPanel.LoadDomainData(currentDBFile.toString());
			titlePanel.loadData(currentDBFile.toString());

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
			if (dbFilePath != null && new File(dbFilePath).exists()){
				DomainDao domainDao = new DomainDao(dbFilePath.toString());
				TargetDao targetDao = new TargetDao(dbFilePath.toString());

				boolean targetSaved = targetDao.addOrUpdateTargets(domainPanel.fetchTargetModel().getTargetEntries());
				boolean domainSaved = domainDao.saveDomainManager(domainPanel.getDomainResult());

				if (domainOnly) {
					if ( targetSaved && domainSaved	) {
						stdout.println("Save Domain Only Success! " + Commons.getNowTimeString());
						return true;
					}
					return false;
				}else {
					TitleDao titleDao = new TitleDao(dbFilePath.toString());
					boolean titleSaved  = titleDao.addOrUpdateTitles(getTitlePanel().getTitleTable().getLineTableModel().getLineEntries());
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
					GUIMain frame = new GUIMain(null);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
