package config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import GUI.GUIMain;
import burp.BurpExtender;

//专门用于存储数据的对象，即被用户序列化和反序列进行存储的对象。

public class DataLoadManager {
	private Stack recentProjectDatabaseFiles = new Stack();
	private String recentToolPanelConfigPath = "";
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


	public String getRecentToolPanelConfigPath() {
		return recentToolPanelConfigPath;
	}

	public void setRecentToolPanelConfigPath(String recentToolPanelConfigPath) {
		this.recentToolPanelConfigPath = recentToolPanelConfigPath;
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


	///////以下是做为管理者应该具备的能力////////

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

	/**
	 * 加载数据库到domain hunter
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
		pushRecentDatabaseFile(dbFilePath);//保存最近的加载
		saveToDisk();
	}

	/**
	 * 
	 * @param dbFilePath
	 */
	public void saveCurrentDB(String dbFilePath) {
		if (dbFilePath == null || dbFilePath.equals("")) {
			dbFilePath = gui.getCurrentDBFile().getAbsolutePath();
		}
		if (null == gui.getDomainPanel().getDomainResult()) return;//有数据才弹对话框指定文件位置。
		gui.getDomainPanel().saveDomainDataToDB();//域名面板自动保存逻辑有点复杂，退出前再自动保存一次
		saveToDisk();
	}

	/**
	 * 加载tool panel config 到 domain hunter
	 * @param ConfigFilePath
	 */
	public void loadConfigToHunter(String ConfigFilePath) {
		if (ConfigFilePath == null || ConfigFilePath.equals("")) {
			ConfigFilePath = recentToolPanelConfigPath;
		}
		if (ConfigFilePath == null || ConfigFilePath.equals("")) {
			ConfigFilePath = defaultConfigFilename;
		}

		gui.configPanel.loadConfigToGUI(ConfigFilePath);
		recentToolPanelConfigPath = ConfigFilePath;//保存最近的加载
		saveToDisk();
	}

	/**
	 * 
	 * @param ConfigFilePath
	 */
	public void saveCurrentConfig(String ConfigFilePath) {
		if (ConfigFilePath == null || ConfigFilePath.equals("")) {
			ConfigFilePath = recentToolPanelConfigPath;
		}
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
		saveToDisk();
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

	public void saveDbAndConfig() {
		saveCurrentDB(null);
		saveCurrentConfig(null);
	}

}