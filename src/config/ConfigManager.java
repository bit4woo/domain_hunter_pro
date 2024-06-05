package config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.SystemUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import GUI.GUIMain;
import burp.BurpExtender;
import title.LineEntry;

public class ConfigManager {

	private String configManagerName = "";
	private static List<ConfigEntry> configList = new ArrayList<ConfigEntry>();

	//用于本地保存的路径
	public static final String localdir = 
			System.getProperty("user.home")+File.separator+".domainhunter";

	public static final String[] winDefaultBrowserPaths = {
			"C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe",
			"C:\\Program Files\\Mozilla Firefox\\firefox.exe",
			"D:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe",
	"D:\\Program Files\\Mozilla Firefox\\firefox.exe"};
	public static final String defaultNmap = "nmap -Pn -sT -sV --min-rtt-timeout 1ms "
			+ "--max-rtt-timeout 1000ms --max-retries 0 --max-scan-delay 0 --min-rate 3000 {host}";
	public static final String defaultMasscan = "masscan -p0-65535 --rate=5000 {host}";
	public static final String defaultDirSearch = "python3 dirsearch.py -t 8 --proxy=127.0.0.1:8080 "
			+ "--random-agent -e * -f -x 400,404,500,502,503,514,550,564 -u {url}";
	public static final String macDefaultBrowserPath = "/Applications/Firefox.app/Contents/MacOS/firefox";
	public static final String defaultDirDictPath ="D:\\github\\webdirscan\\dict\\dict.txt";

	private GUIMain gui;

	ConfigManager(){
		//to resolve "default constructor not found" error
	}

	public static String getBrowserPath() {
		if (SystemUtils.isMac()) {
			return macDefaultBrowserPath;
		}else {
			for (String path : winDefaultBrowserPaths) {
				if (new File(path).exists()) {
					return path;
				}
			}
		}
		return "";
	}

	public static List<ConfigEntry> getConfigList() {
		return configList;
	}

	public static void setConfigList(List<ConfigEntry> configList) {
		ConfigManager.configList = configList;
	}

	public static void init(String configManagerFile) {
		if (initFromFile(configManagerFile) && configList.size() >0) {
			return;
		}
		initDefault();
	}

	/**
	 * 从文件中初始化ConfigManager对象
	 * @param configManagerFile
	 * @return
	 */
	private static boolean initFromFile(String configManagerFile) {
		if (configManagerFile != null){
			try {
				File localFile = new File(configManagerFile);
				if (localFile.exists()) {
					String jsonstr = FileUtils.readFileToString(localFile,"UTF-8");
					FromJson(jsonstr);
					mergeConfig();
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return false;
	}

	private static boolean initDefault(){
		try {
			configList = getInitConfigs();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 向当前配置中，添加新的配置项
	 * @return
	 */
	private static void mergeConfig(){
		try {
			for (ConfigEntry entry:getInitConfigs()) {
				if(!isConfigExist(entry.getKey())) {
					configList.add(entry);//应该根据类型选择位置
				}
			}

			// 按照name属性进行排序
			Collections.sort(configList, new Comparator<ConfigEntry>() {
				@Override
				public int compare(ConfigEntry entry1, ConfigEntry entry2) {
					//按类型排序就OK了，String的在前面，boolean的在后面
					return -entry1.getType().compareTo(entry2.getType());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static List<ConfigEntry> getInitConfigs() throws Exception{
		List<ConfigEntry> configList = new ArrayList<ConfigEntry>();
		try {
			configList.add(new ConfigEntry(ConfigName.BrowserPath,getBrowserPath(),"",true,true));
			configList.add(new ConfigEntry(ConfigName.PortScanCmd,defaultNmap,"",true,true));
			configList.add(new ConfigEntry(ConfigName.DirBruteCmd,defaultDirSearch,"",true,true));
			configList.add(new ConfigEntry(ConfigName.DirDictPath,defaultDirDictPath,"",true,true));
			configList.add(new ConfigEntry(ConfigName.ElasticURL,"http://10.12.72.55:9200/","",true,true));
			configList.add(new ConfigEntry(ConfigName.ElasticUserPass,"elastic:changeme","username and password of elastic API",true,true));

			configList.add(new ConfigEntry(ConfigName.UploadApiURL,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.UploadApiToken,"","",true,true));

			configList.add(new ConfigEntry(ConfigName.FofaEmail,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.FofaKey,"","",true,true));

			configList.add(new ConfigEntry(ConfigName.Quake360APIKey,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.Ti360APIKey,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.QianxinHunterAPIKey,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.QianxinTiAPIKey,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.ZoomEyeAPIKey,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.ShodanAPIKey,"","",true,true));
			configList.add(new ConfigEntry(ConfigName.HunterIoAPIKey,"","",true,true));

			configList.add(new ConfigEntry(ConfigName.ProxyForGetCert,"127.0.0.1:7890","",true,true));


			configList.add(new ConfigEntry(ConfigName.showBurpMenu,"true","",true,true));
			configList.add(new ConfigEntry(ConfigName.showMenuItemsInOne,"false","",true,true));
			configList.add(new ConfigEntry(ConfigName.ignoreHTTPS,"false","",true,true));
			configList.add(new ConfigEntry(ConfigName.ignoreHTTP,"true","",true,true));
			configList.add(new ConfigEntry(ConfigName.ignoreHTTPStaus500,"true","",true,true));
			configList.add(new ConfigEntry(ConfigName.ignoreHTTPStaus400,"true","",true,true));
			configList.add(new ConfigEntry(ConfigName.ignoreWrongCAHost,"false","",true,true));
			configList.add(new ConfigEntry(ConfigName.removeItemIfIgnored,"true","",true,true));
			configList.add(new ConfigEntry(ConfigName.SaveTrafficToElastic,"false","",true,true));
			configList.add(new ConfigEntry(ConfigName.ApiReqToTitle,"true","",true,true));

		} catch (Exception e) {
			throw e;
		}
		return configList;
	}

	public String getConfigManagerName() {
		return configManagerName;
	}

	public void setConfigManagerName(String configManagerName) {
		this.configManagerName = configManagerName;
	}

	@Deprecated
	public String ToJsonDeprecated(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return new Gson().toJson(this);
	}

	@Deprecated
	public static ConfigManager FromJsonDeprecated(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return new Gson().fromJson(json, ConfigManager.class);
	}


	public static String ToJson(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return configListToJson();
	}

	public static void FromJson(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		configList = configListFromJson(json);
	}

	public static String configListToJson(){
		return new Gson().toJson(configList);
	}

	public static List<ConfigEntry> configListFromJson(String json){
		// 创建一个Gson对象
		Gson gson = new Gson();
		// 使用TypeToken来获取List<ConfigEntry>的Type
		java.lang.reflect.Type listType = new TypeToken<List<ConfigEntry>>(){}.getType();

		// 使用fromJson方法将JSON字符串反序列化为List<ConfigEntry>对象
		List<ConfigEntry> configList = gson.fromJson(json, listType);
		return configList;
	}



	// 现在你可以使用configList了，它是一个List<ConfigEntry>对象

	public static String getStringConfigByKey(String configKey) {
		if(ConfigName.getAllConfigNames().contains(configKey)) {
			for (ConfigEntry entry:configList) {
				if (entry.getKey().equalsIgnoreCase(configKey)) {
					return entry.getValue();
				}
			}
		}else {
			BurpExtender.getStderr().println("Config key not found: " + configKey);
		}
		return null;
	}

	public static boolean getBooleanConfigByKey(String configKey){
		if(ConfigName.getAllConfigNames().contains(configKey)) {
			for (ConfigEntry entry : configList) {
				if (entry.getKey().equalsIgnoreCase(configKey)) {
					try {
						return Boolean.parseBoolean(entry.getValue());
					} catch (IllegalArgumentException e) {
						BurpExtender.getStderr().println("Invalid boolean value for config key: " + configKey);
					}
				}
			}
		}else {
			BurpExtender.getStderr().println("Config key not found: " + configKey);
		}
		return false;
	}

	public static boolean isConfigExist(String configKey){
		for (ConfigEntry entry : configList) {
			if (entry.getKey().equalsIgnoreCase(configKey)) {
				return true;
			}
		}
		return false;
	}

	public static void setConfigValue(String configKey,String configValue) {
		if(ConfigName.getAllConfigNames().contains(configKey)) {
			for (ConfigEntry entry:configList) {
				if (entry.getKey().equalsIgnoreCase(configKey)) {
					entry.setValue(configValue);
				}
			}
		}else {
			BurpExtender.getStderr().println("Config key not found: " + configKey);
		}
	}

	public static boolean setConfigValue(String configKey,boolean configValue){
		if(ConfigName.getAllConfigNames().contains(configKey)) {
			for (ConfigEntry entry : configList) {
				if (entry.getKey().equalsIgnoreCase(configKey)) {
					if (entry.getKey().equalsIgnoreCase(configKey)) {
						entry.setValue(configValue+"");
					}
				}
			}
		}else {
			BurpExtender.getStderr().println("Config key not found: " + configKey);
		}
		return false;
	}


	/**
	 * 当从文件恢复出当前对象后，需要通过setter来设置gui
	 * @param gui
	 */
	public void setGui(GUIMain gui) {
		this.gui = gui;
	}

	public static void main(String[] args) {
		System.out.println(new ConfigManager().ToJson());
	}

	/**
	 *是否是从http跳转到相同URL的https 
	 **/
	public static boolean isRedirectToHttps(LineEntry item) {
		if (item.getProtocol().equalsIgnoreCase("http")) {
			if (400>item.getStatuscode() && item.getStatuscode() >=300) {
				String locationUrl = item.getHeaderValueOf(false,"Location");//不包含默认端口
				if (StringUtils.isNotEmpty(locationUrl)){
					locationUrl = locationUrl.toLowerCase().replace("https://", "http://");
					return locationUrl.equalsIgnoreCase(item.fetchUrlWithCommonFormate());
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * 同一个主机的多个请求，可以根据某些条件丢弃一些。比如
	 * http跳转https的丢弃http
	 * 
	 * @param entries
	 * @return
	 */
	public static List<LineEntry> doSameHostFilter(List<LineEntry> entries) {

		if (!ConfigManager.getBooleanConfigByKey(ConfigName.ignoreHTTP) && 
				!ConfigManager.getBooleanConfigByKey(ConfigName.ignoreHTTPS)) {
			return entries;
		}

		LineEntry httpsOk =null;
		LineEntry httpOk =null;

		LineEntry otherPorthttpsOk =null;
		LineEntry otherPorthttpOk =null;

		for (LineEntry item:entries) {
			if (item.getPort()==443 ||item.getPort()==80) {
				if (item.getProtocol().equalsIgnoreCase("https")  && item.getStatuscode()>0) {
					httpsOk = item;
				}

				if (item.getProtocol().equalsIgnoreCase("http") && item.getStatuscode()>0) {
					httpOk = item;
					if (isRedirectToHttps(item)) {
						httpOk = null;
					}
				}
			}else {
				if (item.getProtocol().equalsIgnoreCase("https") && item.getStatuscode()>0) {
					otherPorthttpsOk = item;
				}

				if (item.getProtocol().equalsIgnoreCase("http") && item.getStatuscode()>0) {
					otherPorthttpOk = item;
					if (isRedirectToHttps(item)) {
						otherPorthttpOk = null;
					}
				}
			}
		}

		if (httpsOk !=null && httpOk !=null ) {
			if (ConfigManager.getBooleanConfigByKey(ConfigName.ignoreHTTP)) {
				httpOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}else if (ConfigManager.getBooleanConfigByKey(ConfigName.ignoreHTTPS)) {
				httpsOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}
		}

		if (otherPorthttpsOk !=null && otherPorthttpOk !=null ) {
			if (ConfigManager.getBooleanConfigByKey(ConfigName.ignoreHTTP)) {
				otherPorthttpOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}else if (ConfigManager.getBooleanConfigByKey(ConfigName.ignoreHTTPS)) {
				otherPorthttpsOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}
		}

		return entries;
	}
	/*
	 * 返回结果值影响是否添加到table中，其实这里面可以对entry进行一些操作
	 * 能通过过滤器返回true，否则返回false。判断是否是有用的记录。
	 */
	public static LineEntry doFilter(LineEntry entry) {

		PrintWriter stdout = BurpExtender.getStdout();
		PrintWriter stderr = BurpExtender.getStderr();

		if (entry == null) return entry;
		//default requirement
		if (entry.getStatuscode() <=0 ) {
			stdout.println(String.format("--- [%s] --- no response",entry.getUrl()));
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}

		if (entry.getStatuscode() >=500 && getBooleanConfigByKey(ConfigName.ignoreHTTPStaus500)) {
			stdout.println(String.format("--- [%s] --- status code >= 500",entry.getUrl()));
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}

		if (entry.getStatuscode() == 400 && getBooleanConfigByKey(ConfigName.ignoreHTTPStaus400)) {//400 The plain HTTP request was sent to HTTPS port
			stdout.println(String.format("--- [%s] --- status code == 400",entry.getUrl()));
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}

		/*显示出来，方便加入黑名单，提高下次请求的效率
		if (entry.getStatuscode() == 403 && entry.getTitle().equals("Direct IP access not allowed | Cloudflare")) {
			stdout.println(String.format("--- [%s] --- Direct Cloudflare IP access",entry.getUrl()));
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}*/

		//<head><title>403 Forbidden</title></head>
		/*
		if (entry.getStatuscode() == 403 && entry.getTitle().equals("403 Forbidden")){
			byte[] body = HelperPlus.getBody(false,entry.getResponse());
			if (body != null){
				if (new String(body).toLowerCase().contains("<hr><center>nginx</center>")){
					return false;
				}
			}
		}*/

		//<title>Welcome to nginx!</title>
		/*
		if (entry.getStatuscode() == 200 && entry.getTitle().equals("Welcome to nginx!")
				&& entry.getContentLength()<=612 ){
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}*/

		/*
		if (null != blacklistStatusCodeSet && blacklistStatusCodeSet.size()>0) {
			if (blacklistStatusCodeSet.contains(Integer.toString(entry.getStatuscode()))) {
				stdout.println(String.format("--- [%s] --- due to status code black list",entry.getUrl()));
				return false;
			}
		}

		if (null != blacklistIPSet && blacklistIPSet.size()>0) {
			for (String IP:entry.getIP().split(",")) {
				if (blacklistIPSet.contains(IP.trim())) {
					stdout.println(String.format("--- [%s] --- due to IP black list",entry.getUrl()));
					return false;
				}
			}
		}

		if (null != blacklistCDNSet && blacklistCDNSet.size()>0) {
			String cdn = entry.getCDN();
			if (null != cdn && !cdn.trim().equals("")) {
				for (String cdnitem:cdn.split(",")) {
					if (blacklistCDNSet.contains(cdnitem.trim())) {
						stdout.println(String.format("--- [%s] --- due to CDN black list",entry.getUrl()));
						return false;
					}
				}
			}
		}		

		if (null != blacklistWebContainerSet && blacklistWebContainerSet.size()>0) {
			if (blacklistWebContainerSet.contains(entry.getWebcontainer())) {
				stdout.println(String.format("--- [%s] --- due to web container black list",entry.getUrl()));
				return false;
			}
		}

		//放到最后，其他匹配项可能更常用
		if (null != blacklistHostSet && blacklistHostSet.size()>0) {
			if (blacklistHostSet.contains(entry.getHost())) {
				return false;
			}
		}
		 */

		return entry;
	}
}
