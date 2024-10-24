package config;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingWorker;

import base.Commons;
import base.FileTypeAdapter;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import GUI.GUIMain;
import base.Stack;
import burp.BurpExtender;

//专门用于存储数据的对象，即被用户序列化和反序列进行存储的对象。

public class DataLoadManager {
	private Stack recentDbFiles = new Stack();
	private transient int recentStackHash = -1;
	//使用transient便反序列化时不处理这个field，避免修改其初始值，这样才能在第一次加载时创建recent menu
	private File currentDBFile;
	private static GUIMain gui;
	public static final String localdir = 
			System.getProperty("user.home")+File.separator+".domainhunter"+File.separator+"DomainHunterProRecent";
	public static final String defaultConfigFilename = 
			System.getProperty("user.home")+File.separator+".domainhunter"+File.separator+"DomainHunterToolPanelConfig.json";


	//为了fastjson反序列化，必须要有这个函数
	DataLoadManager(){

	}

	public DataLoadManager(GUIMain guiMain ){
		gui = guiMain;
	}
	//getter setter为了序列化

	public Stack getRecentDbFiles() {
		return recentDbFiles;
	}

	public void setRecentDbFiles(Stack recentDbFiles) {
		this.recentDbFiles = recentDbFiles;
	}

	public File getCurrentDBFile() {
		return currentDBFile;
	}

	public void setCurrentDBFile(File currentDBFile) {
		this.currentDBFile = currentDBFile;
	}

	//序列化和反序列化
	public String toJson(){
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(File.class, new FileTypeAdapter())
				.create();
		return gson.toJson(this);
	}


	public static DataLoadManager fromJson(String modelStr){
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(File.class, new FileTypeAdapter())
				.create();
		return gson.fromJson(modelStr,DataLoadManager.class);
	}


	/**
	 * DataLoadManager的自我保存
	 */
	public void saveToDisk() {
		File localFile = new File(localdir);
		try {
			FileUtils.write(localFile, this.toJson(),"UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	/**
	 * DataLoadManager的自我加载
	 * @return
	 */
	public static DataLoadManager loadFromDisk(GUIMain guiMain) {
		File localFile = new File(localdir);
		gui = guiMain;
		try {
			if (localFile.exists()) {
				String jsonstr = FileUtils.readFileToString(localFile,"UTF-8");
				return fromJson(jsonstr);
			}
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
		return new DataLoadManager();
	}

	///////以上是DataLoadManager的自我加载、序列化反序列化函数////////


	///////以下是栈操作////////

	private String popRecentDatabaseFile() {
		if (recentDbFiles.isEmpty()) {
			return null;
		}else {
			return recentDbFiles.pop();
		}
	}

	private void pushRecentDatabaseFile(String dbfilename) {
		if (StringUtils.isEmpty(dbfilename)||!new File(dbfilename).exists()) {
			return;
		}
		recentDbFiles.push(dbfilename);
	}

	///////以下是做为管理者应该具备的能力////////
	/**
	 * 加载数据库到domain hunter
	 * 如果指定的db文件路径为空，则加载最近加载过的db。
	 * 如果最近加载记录仍然为空，则不执行任何操作。
	 * @param dbFilePath
	 */
	public void loadDbfileToHunter(String dbFilePath) {
		if (StringUtils.isEmpty(dbFilePath)) {
			dbFilePath = popRecentDatabaseFile();
		}
		if (StringUtils.isEmpty(dbFilePath)) {
			return;
		}
		loadDataBase(dbFilePath);
		pushRecentDatabaseFile(dbFilePath);//保存最近的加载
		saveToDisk();
	}


	/**
	 * 单纯用于记录最近情况
	 * @param dbFilePath
	 */
	public void unloadDbfile(String dbFilePath) {
		if (StringUtils.isEmpty(dbFilePath)) {
			if (getCurrentDBFile() != null) {
				dbFilePath =getCurrentDBFile().getAbsolutePath();
			}
		}
		if (StringUtils.isEmpty(dbFilePath)) {
			return;
		}
		pushRecentDatabaseFile(dbFilePath);//保存最近的加载
		saveToDisk();
	}



	/**
	 * 加载数据库文件：
	 * 1、加载target对象到DomainPanel中的table内
	 * 2、加载domainManager对象到DomainPanel中的文本框
	 * 3、加载Title数据到TitlePanel
	 * @param dbFilePath
	 */
	private boolean loadDataBase(String dbFilePath){
		try {//这其中的异常会导致burp退出
			System.out.println("=================================");
			System.out.println("==Start Loading Data From: " + dbFilePath+" "+ Commons.getNowTimeString()+"==");
			BurpExtender.getStdout().println("==Start Loading Data From: " + dbFilePath+" "+Commons.getNowTimeString()+"==");
			currentDBFile = new File(dbFilePath);
			if (!currentDBFile.exists()){
				BurpExtender.getStdout().println("==Load database file [" + dbFilePath+"] failed,file does not exist "+Commons.getNowTimeString()+"==");
				return false;
			}

			gui.getDomainPanel().LoadTargetsData(currentDBFile.toString());
			gui.getDomainPanel().LoadDomainData(currentDBFile.toString());
			gui.getTitlePanel().loadData(currentDBFile.toString());

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
		if (gui.getDomainPanel().getDomainResult() !=null){
			String name = currentDBFile.getName();
			//String newName = String.format(BurpExtender.getFullExtenderName()+" [%s]",name);
			//v2021.8的版本中，邮件菜单会用到插件名称，所以减小名称的长度
			String newName = String.format(BurpExtender.getExtenderName()+" [%s]",name);

			BurpExtender.getCallbacks().setExtensionName(newName); //新插件名称
			
			//gui.getProjectMenu().displayDBNameAtProjectMenu(name);
			//gui.getProjectMenu().displayDBNameAtProjectMenuItem(name);
			gui.getProjectMenu().displayDBNameAtDomainTab(name);
		}
	}

	/**
	 * 1、指定文件名，相当于save as
	 * 2、未指定文件名，保存到默认路径文件
	 * @param ConfigFilePath
	 */
	public void saveCurrentConfig(String ConfigFilePath) {
		if (StringUtils.isEmpty(ConfigFilePath)) {
			ConfigFilePath = defaultConfigFilename;
		}

		try {
			File localFile = new File(ConfigFilePath);
			FileUtils.write(localFile, ConfigManager.ToJson(),"UTF-8");
			BurpExtender.getStdout().println("Tool Panel Config Saved To Disk!");
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	/**
	 * 加载tool panel config 到 domain hunter
	 * 
	 * 所有指定文件的加载，都复制到默认路径文件。这样实现多个项目共享一个配置。设想的常见的是一个电脑用户下，一般都应该只有一份配置。
	 * 
	 * @param ConfigFilePath
	 */
	public void loadConfigToHunter(String ConfigFilePath) {
		if (StringUtils.isEmpty(ConfigFilePath)) {
			ConfigFilePath = defaultConfigFilename;
		}
		ConfigManager.init(ConfigFilePath);
		gui.renewConfigPanel();
		if (new File(ConfigFilePath).exists() && !ConfigFilePath.equals(defaultConfigFilename)) {
			try {
				FileUtils.copyFile(new File(ConfigFilePath), new File(defaultConfigFilename));//会自动覆盖
			} catch (IOException e) {
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	public void loadDbAndConfig() {
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			@Override
			protected Map doInBackground() throws Exception {
				//loadDbfileToHunter(null);
				loadConfigToHunter(null);
				return null;
			}
			@Override
			protected void done() {
			}
		};
		worker.execute();
	}

	public void createRecentOpenItem(JMenu parentMenu){
		if (isRecentStackChanged()){
			parentMenu.removeAll();

			List<String> list = recentDbFiles.getItemList();
			Collections.reverse(list);
			for(String item:list) {
				if (StringUtils.isEmpty(item)) {
					recentDbFiles.remove(item);
					continue;
				}
				if (!new File(item).exists()) {
					recentDbFiles.remove(item);
					continue;
				}
				JMenuItem menuItem = new JMenuItem(new AbstractAction(item) {
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						loadDbfileToHunter(item);
					}
				});
				parentMenu.add(menuItem);
			}
		}
	}
	public boolean isRecentStackChanged(){
		if (recentDbFiles.hashCode() == recentStackHash){
			return false;
		}else {
			//当一个list，其中的元素发生改变，它的内存地址会改变吗?它的hashcode会改变吗?
			//在 Java 中，当一个 List 对象的元素发生改变时，它的内存地址不会改变。List 是一个对象引用类型，它保存的是对存储在堆内存中的元素的引用。因此，无论 List 中的元素如何改变，List 对象本身的引用都不会改变。
			//Java 中的 List 类并没有提供重写 hashCode 方法的行为。因此，List 对象的 hashCode 方法仍然是基于其内存地址计算的，即使列表中的元素发生改变，List 对象的 hashCode 也不会改变。
			recentStackHash = recentDbFiles.hashCode();
			return true;
		}
	}
}