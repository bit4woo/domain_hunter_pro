package config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import GUI.GUIMain;
import base.Stack;
import burp.BurpExtender;

//专门用于存储数据的对象，即被用户序列化和反序列进行存储的对象。

public class DataLoadManager {
	private Stack recentProjectDatabaseFiles = new Stack();
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
	public Stack getRecentProjectDatabaseFiles() {
		return recentProjectDatabaseFiles;
	}

	public void setRecentProjectDatabaseFiles(Stack recentProjectDatabaseFiles) {
		this.recentProjectDatabaseFiles = recentProjectDatabaseFiles;
	}

	//序列化和反序列化
	public String toJson(){
		String modelStr = new Gson().toJson(this);
		return modelStr;
	}


	public static DataLoadManager fromJson(String modelStr){
		DataLoadManager model = new Gson().fromJson(modelStr,DataLoadManager.class);
		return model;
	}


	public void saveToDisk() {
		File localFile = new File(localdir);
		try {
			FileUtils.write(localFile, this.toJson());
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
				String jsonstr = FileUtils.readFileToString(localFile);
				DataLoadManager manager = fromJson(jsonstr);
				return manager;
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
		if (recentProjectDatabaseFiles.isEmpty()) {
			return null;
		}else {
			String result = recentProjectDatabaseFiles.pop();
			return result;
		}
	}

	private void pushRecentDatabaseFile(String dbfilename) {
		recentProjectDatabaseFiles.push(dbfilename);
	}

	///////以下是做为管理者应该具备的能力////////
	/**
	 * 加载数据库到domain hunter
	 * 如果指定的db文件路径为空，则加载最近加载过的db。
	 * 如果最近加载记录仍然为空，则不执行任何操作。
	 * @param dbFilePath
	 */
	public void loadDbfileToHunter(String dbFilePath) {
		if (dbFilePath == null || dbFilePath.equals("")) {
			dbFilePath = popRecentDatabaseFile();
		}
		if (dbFilePath == null || dbFilePath.equals("")) {
			return;
		}
		gui.loadDataBase(dbFilePath);
		//pushRecentDatabaseFile(dbFilePath);//保存最近的加载
		saveToDisk();
	}
	
	
	/**
	 * 单纯用于记录最近情况
	 * @param dbFilePath
	 */
	public void unloadDbfile(String dbFilePath) {
		if (dbFilePath == null || dbFilePath.equals("")) {
			dbFilePath =gui.getCurrentDBFile().getAbsolutePath();
		}
		if (dbFilePath == null || dbFilePath.equals("")) {
			return;
		}
		pushRecentDatabaseFile(dbFilePath);//保存最近的加载
		saveToDisk();
	}

	/**
	 * 加载tool panel config 到 domain hunter
	 * 
	 * 所有指定文件的加载，都复制到默认路径文件。这样实现多个项目共享一个配置。设想的常见的是一个电脑用户下，一般都应该只有一份配置。
	 * 
	 * @param ConfigFilePath
	 */
	public void loadConfigToHunter(String ConfigFilePath) {
		if (ConfigFilePath == null || ConfigFilePath.equals("")) {
			ConfigFilePath = defaultConfigFilename;
		}
		gui.configPanel.loadConfigToGUI(ConfigFilePath);
		if (new File(ConfigFilePath).exists() && !ConfigFilePath.equals(defaultConfigFilename)) {
			try {
				FileUtils.copyFile(new File(ConfigFilePath), new File(defaultConfigFilename));//会自动覆盖
			} catch (IOException e) {
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	/**
	 * 1、指定文件名，相当于save as
	 * 2、未指定文件名，保存到默认路径文件
	 * @param ConfigFilePath
	 */
	public void saveCurrentConfig(String ConfigFilePath) {
		if (ConfigFilePath == null || ConfigFilePath.equals("")) {
			ConfigFilePath = defaultConfigFilename;
		}
		LineConfig config = gui.configPanel.getConfigFromGUI();

		try {
			File localFile = new File(ConfigFilePath);
			FileUtils.write(localFile, config.ToJson());
			BurpExtender.getStdout().println("Tool Panel Config Saved To Disk!");
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
	}
	

	public void loadDbAndConfig() {
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			@Override
			protected Map doInBackground() throws Exception {
				loadDbfileToHunter(null);
				loadConfigToHunter(null);
				return null;
			}
			@Override
			protected void done() {
			}
		};
		worker.execute();
	}
}