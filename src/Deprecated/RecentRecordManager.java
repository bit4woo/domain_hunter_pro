package Deprecated;

//这个类用于记录最近加载过得项目，用于多个插件加载时。

/* 1. open---> LoadedNumber++
 * 2. New ---> LoadedNumber++
 * 3. extensionLoad ---> LoadFromHere ---> LoadedNumber++,removeRecord
 * 
 * 4. extensionUnload ---> addRecored,LoadedNumber--
 */

import java.nio.charset.StandardCharsets;

import burp.BurpExtender;

public class RecentRecordManager {
	public static final String Extension_Setting_Name_Recent_Records = "domain-Hunter-pro-recent-db-path";
	
	//从磁盘加载，并获取最近记录
	//情况3
	public static String extensionLoaded() {
		RecentModel model = loadFromBurp();
		String projectFilePath = model.LoadFromStack();
		saveToBurp(model.toJson());//修改后需要再写入

		return projectFilePath;
	}

	//修改最近记录，并存入磁盘
	//情况4
	public static void extensionUnload(String projectFilePath) {
		if (projectFilePath != null) {
			RecentModel model = loadFromBurp();
			model.saveToStack(projectFilePath);
			saveToBurp(model.toJson());//修改后需要再写入
		}
	}
	
	//情况1和2
	public static void newOrOpen() {
		RecentModel model = loadFromBurp();
		model.setLoadedNumber(model.getLoadedNumber()+1);
		saveToBurp(model.toJson());//修改后需要再写入
	}

	public static int fetchLoadedNumber() {
		RecentModel model = loadFromBurp();
		return model.getLoadedNumber();//仅获取，无修改，无需写入。
	}

	private static void saveToBurp(String modelStr) {
		System.out.println("saveToBurp############beforeSave:"+modelStr);
		BurpExtender.getCallbacks().saveExtensionSetting(Extension_Setting_Name_Recent_Records, modelStr);
		
		String tmp = BurpExtender.getCallbacks().loadExtensionSetting(Extension_Setting_Name_Recent_Records);
		System.out.println("saveToBurp############saveVerfy:"+tmp);
	}

	public static void cleanSave() {
		BurpExtender.getCallbacks().saveExtensionSetting(Extension_Setting_Name_Recent_Records, null);
	}

	private static RecentModel loadFromBurp() {
		String modelStr = BurpExtender.getCallbacks().loadExtensionSetting(Extension_Setting_Name_Recent_Records);
		RecentModel model;
		if (null != modelStr) {
			model = RecentModel.fromJson(modelStr);
			System.out.println("LoadedFromExtensionSetting:");
		}else {//以前从未运行该插件，配置为空
			model = new RecentModel();
			System.out.println("new RecentModel()");

		}
		System.out.println("loadFromBurp "+model.toJson());
		return model;
	}
}