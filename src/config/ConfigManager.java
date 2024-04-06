package config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

import GUI.GUIMain;
import base.Commons;
import burp.BurpExtender;
import title.LineEntry;

public class ConfigManager {

	private String configManagerName = "";
	private List<ConfigEntry> configList = new ArrayList<ConfigEntry>();
	
	private static int MaximumEntries = 1000;//控制显示的条目数，减少内存占用

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
	public static final  String defaultDirDictPath ="D:\\github\\webdirscan\\dict\\dict.txt";
	

	private GUIMain gui;

	ConfigManager(){
		//to resolve "default constructor not found" error
	}
	
	public String getBrowserPath() {
		if (Commons.isMac()) {
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

	public ConfigManager(String Name){
		this.configManagerName = Name;
		configList.add(new ConfigEntry(ConfigName.BrowserPath,getBrowserPath(),"",true,true));
		configList.add(new ConfigEntry(ConfigName.PortScanCmd,defaultNmap,"",true,true));
		configList.add(new ConfigEntry(ConfigName.DirBruteCmd,defaultDirSearch,"",true,true));
		configList.add(new ConfigEntry(ConfigName.DirDictPath,"","",true,true));
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
		
		configList.add(new ConfigEntry(ConfigName.ProxyForGetCert,"127.0.0.1:7890","",true,true));
		
		
		configList.add(new ConfigEntry(ConfigName.showBurpMenu,"true","",true,true));
		configList.add(new ConfigEntry(ConfigName.showMenuItemsInOne,"true","",true,true));
		configList.add(new ConfigEntry(ConfigName.ignoreHTTPS,"false","",true,true));
		configList.add(new ConfigEntry(ConfigName.ignoreHTTP,"true","",true,true));
		configList.add(new ConfigEntry(ConfigName.ignoreHTTPStaus500,"true","",true,true));
		configList.add(new ConfigEntry(ConfigName.ignoreHTTPStaus400,"true","",true,true));
		configList.add(new ConfigEntry(ConfigName.ignoreWrongCAHost,"false","",true,true));
		configList.add(new ConfigEntry(ConfigName.removeItemIfIgnored,"true","",true,true));
		configList.add(new ConfigEntry(ConfigName.SaveTrafficToElastic,"false","",true,true));
	}

	public String getConfigManagerName() {
		return configManagerName;
	}

	public void setConfigManagerName(String configManagerName) {
		this.configManagerName = configManagerName;
	}


	public String ToJson(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return new Gson().toJson(this);
	}

	public static ConfigManager FromJson(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return new Gson().fromJson(json, ConfigManager.class);
	}
	

	/**
	 * 当从文件恢复出当前对象后，需要通过setter来设置gui
	 * @param gui
	 */
	public void setGui(GUIMain gui) {
		this.gui = gui;
	}

	public static void main(String[] args) {
		System.out.println(new LineConfig().ToJson());
	}

	

	public static int getMaximumEntries() {
		return MaximumEntries;
	}

	public void setMaximumEntries(int maximumEntries) {
		MaximumEntries = maximumEntries;
	}


	/**
	 * 注意：这里获取到的lineConfig对象，其中的gui属性是null
	 * @param projectFile
	 * @return
	 */
	public static ConfigManager loadFromDisk(String projectFile) {
		if (projectFile == null){
			return  null;
		}
		try {
			File localFile = new File(projectFile);
			if (localFile.exists()) {
				String jsonstr = FileUtils.readFileToString(localFile);
				ConfigManager config = FromJson(jsonstr);
				return config;
			}
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
		return null;
	}

	/**
	 *是否是从http跳转到相同URL的https 
	 **/
	public static boolean isRedirectToHttps(LineEntry item) {
		if (item.getProtocol().equalsIgnoreCase("http")) {
			if (400>item.getStatuscode() && item.getStatuscode() >=300) {
				String locationUrl = item.getHeaderValueOf(false,"Location");//不包含默认端口
				locationUrl = locationUrl.toLowerCase().replace("https://", "http://");
				if (locationUrl.equalsIgnoreCase(item.fetchUrlWithCommonFormate())) {
					return true;
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

		if (!ConfigPanel.ignoreHTTP.isSelected() && !ConfigPanel.ignoreHTTPS.isSelected()) {
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
			if (ConfigPanel.ignoreHTTP.isSelected()) {
				httpOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}else if (ConfigPanel.ignoreHTTPS.isSelected()) {
				httpsOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}
		}

		if (otherPorthttpsOk !=null && otherPorthttpOk !=null ) {
			if (ConfigPanel.ignoreHTTP.isSelected()) {
				otherPorthttpOk.setCheckStatus(LineEntry.CheckStatus_Checked);
			}else if (ConfigPanel.ignoreHTTPS.isSelected()) {
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

		if (entry.getStatuscode() >=500 && ConfigPanel.ignoreHTTPStaus500.isSelected()) {
			stdout.println(String.format("--- [%s] --- status code >= 500",entry.getUrl()));
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}

		if (entry.getStatuscode() == 400 && ConfigPanel.ignoreHTTPStaus400.isSelected()) {//400 The plain HTTP request was sent to HTTPS port
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
