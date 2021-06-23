package burp;

//这个类用于记录最近加载过得项目，用于多个插件加载时。

/* 1. open---> LoadedNumber++
 * 2. New ---> LoadedNumber++
 * 3. extensionLoad ---> LoadFromHere ---> LoadedNumber++,removeRecord
 * 
 * 4. extensionUnload ---> addRecored,LoadedNumber--
 */

public class RecentRecordManager {
	private static RecentModel model = new RecentModel();
	public static final String Extension_Setting_Name_Recent_Records = "domain-Hunter-pro-recent-db-path";
	

	//从磁盘加载，并获取最近记录
	//情况3
	public static String extensionLoaded() {
		System.out.println("extensionLoaded begin:"+model.toJson());
		RecentModel tmp = model;
		loadFromBurp();
		RecentModel tmp1 = model;
		String projectFilePath = model.LoadFromStack();
		saveToBurp();//修改后需要再写入
		System.out.println("extensionLoaded end:"+model.toJson());

		return projectFilePath;
	}

	//修改最近记录，并存入磁盘
	//情况4
	public static void extensionUnload(String projectFilePath) {
		if (projectFilePath != null) {
			System.out.println("extensionUnload begin:"+model.toJson());
			loadFromBurp();
			model.saveToStack(projectFilePath);
			saveToBurp();//修改后需要再写入
			System.out.println("extensionUnload end:"+model.toJson());
		}
	}
	
	//情况1和2
	public static void newOrOpen() {
		System.out.println("newOrOpen begin:"+model.toJson());
		loadFromBurp();
		model.setLoadedNumber(model.getLoadedNumber()+1);
		saveToBurp();//修改后需要再写入
		System.out.println("newOrOpen end:"+model.toJson());
	}

	public static int fetchLoadedNumber() {
		loadFromBurp();
		return model.getLoadedNumber();//仅获取，无修改，无需写入。
	}

	private static void saveToBurp() {
		//		String modelStr = JSON.toJSONString(model);
		String modelStr = model.toJson();
		System.out.println("saveToBurp:"+model.toJson());
		BurpExtender.getCallbacks().saveExtensionSetting(Extension_Setting_Name_Recent_Records, modelStr);
	}

	private static void loadFromBurp() {
		String modelStr = BurpExtender.getCallbacks().loadExtensionSetting(Extension_Setting_Name_Recent_Records);
		if (null != modelStr) {
			model = model.fromJson(modelStr);
		}else {//以前从未运行该插件，配置为空
			model = new RecentModel();
		}
		System.out.println("loadFromBurp:"+model.toJson());
	}
}