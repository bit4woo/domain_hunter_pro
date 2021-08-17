package burp;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

//专门用于存储数据的对象，即被用户序列化和反序列进行存储的对象。

public class RecentModel {
	private Stack recentProjects = new Stack();
	public static final String localdir = 
			System.getProperty("user.home")+File.separator+".domainhunter"+File.separator+"DomainHunterProRecent";

	//为了fastjson反序列化，必须要有这个函数
	RecentModel(){

	}
	//getter setter为了序列化
	public Stack getRecentProjects() {
		return recentProjects;
	}

	public void setRecentProjects(Stack recentProjects) {
		this.recentProjects = recentProjects;
	}

	//序列化和反序列化
	public String toJson(){
		String modelStr = new Gson().toJson(this);
		return modelStr;
	}
	public static RecentModel fromJson(String modelStr){
		RecentModel model = new Gson().fromJson(modelStr,RecentModel.class);
		return model;
	}

	/**
	 * 当插件加载时，从磁盘加载，并获取最近记录
	 * @return
	 */
	public static String fetchRecent() {
		RecentModel model = loadFromDisk();
		String projectFilePath = model.popRecent();
		model.saveToDisk();//修改后需要再写入

		return projectFilePath;
	}

	/**
	 * 当插件卸载时，添加最近记录，并存入磁盘
	 * @param projectFilePath
	 */
	public static void saveRecent(String projectFilePath) {
		if (projectFilePath != null) {
			RecentModel model = loadFromDisk();
			model.pushRecent(projectFilePath);
			model.saveToDisk();//修改后需要再写入
		}
	}

	private String popRecent() {
		if (recentProjects.isEmpty()) {
			return null;
		}else {
			return recentProjects.pop();
		}
	}

	private void pushRecent(String project) {
		recentProjects.push(project);
	}

	private void saveToDisk() {
		File localFile = new File(localdir);
		try {
			FileUtils.write(localFile, this.toJson());
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	private static RecentModel loadFromDisk() {
		File localFile = new File(localdir);
		try {
			if (localFile.exists()) {
				String jsonstr = FileUtils.readFileToString(localFile);
				RecentModel config = fromJson(jsonstr);
				return config;
			}
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
		return new RecentModel();
	}
}

