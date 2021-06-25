package Deprecated;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

//专门用于存储数据的对象，即被用户序列化和反序列进行存储的对象。
/* 1. open---> LoadedNumber++
 * 2. New ---> LoadedNumber++
 * 3. extensionLoad ---> LoadFromHere ---> LoadedNumber++,removeRecord
 * 
 * 4. extensionUnload ---> addRecored,LoadedNumber--
 */
public class RecentModel {
	private Stack recentProjects = new Stack();
	private int LoadedNumber = 0;//加载的该插件的数量。

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

	public void setLoadedNumber(int loadedNumber) {
		this.LoadedNumber = loadedNumber;
	}

	public int getLoadedNumber() {
		return LoadedNumber;
	}

	public String toJson(){
		String modelStr = new Gson().toJson(this);
		return modelStr;
	}
	public static RecentModel fromJson(String modelStr){
		RecentModel model = new Gson().fromJson(modelStr,RecentModel.class);
		return model;
	}

	//出栈，取出最新的记录
	//情况3
	public String LoadFromStack() {
		if (recentProjects.isEmpty()) {
			return null;
		}else {
			if (LoadedNumber++ < 0) LoadedNumber = 0;
			return recentProjects.pop();
		}
	}

	//入栈，写入最新的记录
	//情况4. extensionUnload ---> addRecored,LoadedNumber--
	//!!!!只应该在插件退出时调用这个函数
	public void saveToStack(String project) {
		recentProjects.push(project);
		if (LoadedNumber-- < 0) LoadedNumber = 0;
	}
}

