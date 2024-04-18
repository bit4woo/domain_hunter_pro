package config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ConfigName {
	//boolean config
	public static final String showBurpMenu = "Display context menu of burp";
	public static final String showMenuItemsInOne = "Display context menu items in one";
	public static final String ignoreHTTPS = "Ignore HTTPS if HTTP is OK";
	public static final String ignoreHTTP = "Ignore HTTP if HTTPS is OK";
	public static final String ignoreHTTPStaus500 = "Ignore items which Status >= 500";
	public static final String ignoreHTTPStaus400 = "Ignore items that http status is 400(The plain HTTP request was sent to HTTPS port)";
	public static final String ignoreWrongCAHost = "Ignore items that IP Address and Certificate Authority do not match";

	public static final String removeItemIfIgnored = "Remove item if ignored(Marked as check done by default)";

	public static final String SaveTrafficToElastic = "Save traffic to Elastic";
	public static final String ApiReqToTitle = "Save all API search request to title";

	//String config
	public static final String BrowserPath = "Browser Path";
	public static final String PortScanCmd = "Port Scan Command";
	public static final String DirBruteCmd = "Dir Brute Command";
	public static final String DirDictPath = "Dir Dict Path";
	public static final String ElasticURL = "Elastic URL";
	public static final String ElasticUserPass = "Elastic Username Password";
	public static final String UploadApiToken = "Upload API Token";
	public static final String UploadApiURL = "Upload API URL";
	public static final String FofaEmail = "Fofa Email";
	public static final String FofaKey = "Fofa Key";

	public static final String Quake360APIKey = "quake.360.net API Key";
	public static final String Ti360APIKey = "ti.360.net API Key";

	public static final String QianxinHunterAPIKey = "hunter.qianxin.com API Key";
	public static final String QianxinTiAPIKey = "ti.qianxin.com API Key";

	public static final String ZoomEyeAPIKey = "zoomeye.hk API Key";
	public static final String ShodanAPIKey = "shodan.io API Key";
	public static final String HunterIoAPIKey = "hunter.io API Key";

	public static final String ProxyForGetCert = "Proxy for get certificates";

	
	public static List<String> getAllConfigNames(){
		List<String> result = new ArrayList<String>();
		Field[] fields = ConfigName.class.getDeclaredFields();
		for (Field field : fields) {
			//String varName = field.getName();// 对于每个属性，获取属性名
			if (field.getGenericType().toString().equals("class java.lang.String")) {// 如果type是类类型，则前面包含"class "，后面跟类名
				try {
					String value = (String) field.get(ConfigName.class);//获取属性值
					result.add(value);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
}
