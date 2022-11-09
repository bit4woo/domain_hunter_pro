package config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import title.LineEntry;
import title.search.History;

public class LineConfig {
	private static int MaximumEntries = 1000;//控制显示的条目数，减少内存占用

	//用于本地保存的路径
	private static final String localdir = 
			System.getProperty("user.home")+File.separator+".domainhunter";

	//跑title时根据各字段过滤某些条目
	//private static Set<String> blacklistHostSet = new HashSet<String>(); //其实不需要
	private Set<String> blacklistStatusCodeSet = new HashSet<String>(); 
	private Set<String> blacklistIPSet = new HashSet<String>(); 
	private Set<String> blacklistCDNSet = new HashSet<String>(); 
	private Set<String> blacklistWebContainerSet = new HashSet<String>(); 
	//对于内外网域名或IP的处理分为2种情况：
	//1、外网模式，即在自己公司挖掘别人公司的漏洞。这个是时候收集到的域名如果是解析到私有IP的，仅仅显示就可以了；如果是私有IP地址则直接忽略。
	//2、内网模式，即在自己公司挖掘自己公司的漏洞。这个时候所有域名一视同仁，全部和外网域名一样进行请求并获取title，因为内网的IP也是可以访问的。
	public static final String[] winDefaultBrowserPaths = {
			"C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe",
			"C:\\Program Files\\Mozilla Firefox\\firefox.exe",
			"D:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe",
	"D:\\Program Files\\Mozilla Firefox\\firefox.exe"};
	public static final String defaultNmap = "nmap -Pn -sT -sV --min-rtt-timeout 1ms "
			+ "--max-rtt-timeout 1000ms --max-retries 0 --max-scan-delay 0 --min-rate 3000 {host}";
	public static final String defaultDirSearch = "python3 dirsearch.py -t 8 --proxy=127.0.0.1:8080 "
			+ "--random-agent -e * -f -x 400,404,500,502,503,514,550,564 -u {url}";
	public static final String macDefaultBrowserPath = "/Applications/Firefox.app/Contents/MacOS/firefox";

	private String dirSearchPath = defaultDirSearch;
	private String browserPath = "";
	private String nmapPath =defaultNmap;
	private String bruteDict ="D:\\github\\webdirscan\\dict\\dict.txt";
	private String toolPanelText = "";
	private String elasticApiUrl = "http://10.12.72.55:9200/";
	private String elasticUsernameAndPassword = "elastic:changeme";
	private String uploadApiToken = "";
	private String uploadUrl = "";
	private boolean showItemsInOne = false;
	private boolean enableElastic = false;
	private String dbfilepath ="";
	private History searchHistory;
	private GUIMain gui;

	/**
	 * 当从文件恢复出当前对象后，需要通过setter来设置gui
	 * @param gui
	 */
	public void setGui(GUIMain gui) {
		this.gui = gui;
	}

	LineConfig(GUIMain gui){
		this.gui = gui;
		if (Commons.isMac()) {
			browserPath = macDefaultBrowserPath;
		}else {
			for (String path : winDefaultBrowserPaths) {
				if (new File(path).exists()) {
					browserPath = path;
				}
			}
		}
	}

	public static void main(String[] args) {
		System.out.println(new LineConfig().ToJson());
	}

	/**
	 * 用于JSON的序列化反序列化
	 */
	LineConfig(){

	}

	public static int getMaximumEntries() {
		return MaximumEntries;
	}

	public void setMaximumEntries(int maximumEntries) {
		MaximumEntries = maximumEntries;
	}


	public static boolean isIgnoreHttpsOrHttpIfOneOK() {
		return ConfigPanel.ignoreHTTPS.isSelected();
	}

	public Set<String> getBlacklistStatusCodeSet() {
		return blacklistStatusCodeSet;
	}


	public void setBlacklistStatusCodeSet(Set<String> blacklistStatusCodeSet) {
		this.blacklistStatusCodeSet = blacklistStatusCodeSet;
	}


	public Set<String> getBlacklistIPSet() {
		return blacklistIPSet;
	}


	public void setBlacklistIPSet(Set<String> blacklistIPSet) {
		this.blacklistIPSet = blacklistIPSet;
	}


	public Set<String> getBlacklistCDNSet() {
		return blacklistCDNSet;
	}


	public void setBlacklistCDNSet(Set<String> blacklistCDNSet) {
		this.blacklistCDNSet = blacklistCDNSet;
	}


	public Set<String> getBlacklistWebContainerSet() {
		return blacklistWebContainerSet;
	}


	public void setBlacklistWebContainerSet(Set<String> blacklistWebContainerSet) {
		this.blacklistWebContainerSet = blacklistWebContainerSet;
	}


	public static void setIgnoreHttpsIfHttpOK(boolean ignoreHttpsIfHttpOK) {
		ConfigPanel.ignoreHTTPS.setSelected(ignoreHttpsIfHttpOK);
	}

	public String getDirSearchPath() {
		return dirSearchPath;
	}

	public void setDirSearchPath(String dirSearchPath) {
		this.dirSearchPath = dirSearchPath;
	}

	public String getBrowserPath() {
		return browserPath;
	}

	public void setBrowserPath(String browserPath) {
		this.browserPath = browserPath;
	}

	public String getNmapPath() {
		return nmapPath;
	}

	public void setNmapPath(String nmapPath) {
		this.nmapPath = nmapPath;
	}

	public String getBruteDict() {
		return bruteDict;
	}

	public void setBruteDict(String bruteDict) {
		this.bruteDict = bruteDict;
	}

	public String getToolPanelText() {
		return toolPanelText;
	}

	public void setToolPanelText(String toolPanelText) {
		this.toolPanelText = toolPanelText;
	}

	public String getElasticApiUrl() {
		return elasticApiUrl;
	}



	public void setElasticApiUrl(String elasticApiUrl) {
		this.elasticApiUrl = elasticApiUrl;
	}



	public String getElasticUsernameAndPassword() {
		return elasticUsernameAndPassword;
	}



	public void setElasticUsernameAndPassword(String elasticUsernameAndPassword) {
		this.elasticUsernameAndPassword = elasticUsernameAndPassword;
	}



	public String getUploadApiToken() {
		return uploadApiToken;
	}



	public void setUploadApiToken(String uploadApiToken) {
		this.uploadApiToken = uploadApiToken;
	}



	public String getUploadUrl() {
		return uploadUrl;
	}


	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}


	public boolean isShowItemsInOne() {
		return showItemsInOne;
	}

	public void setShowItemsInOne(boolean showItemsInOne) {
		this.showItemsInOne = showItemsInOne;
	}

	public boolean isEnableElastic() {
		return enableElastic;
	}



	public void setEnableElastic(boolean enableElastic) {
		this.enableElastic = enableElastic;
	}



	public String getDbfilepath() {
		return dbfilepath;
	}

	public void setDbfilepath(String dbfilepath) {
		this.dbfilepath = dbfilepath;
	}

	public History getSearchHistory() {
		return searchHistory;
	}

	public void setSearchHistory(History searchHistory) {
		this.searchHistory = searchHistory;
	}


	public String saveToDisk() {
		try {
			gui.getConfigPanel().saveToConfigFromGUI();
			this.setSearchHistory(History.getInstance());
			//this.setDbfilepath(gui.getCurrentDBFile().getAbsolutePath());//加载数据库时有设置，无需再次获取。
		} catch (Exception e1) {
			e1.printStackTrace();
			e1.printStackTrace(BurpExtender.getStderr());
		}

		try {
			File localFile = new File(localdir+File.separator+gui.getCurrentDBFile().getName()+".config");
			FileUtils.write(localFile, this.ToJson());
			BurpExtender.getStdout().println("Tool Panel Config Saved To Disk!");
			return localFile.toString();
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
			return null;
		}
	}

	/**
	 * 注意：这里获取到的lineConfig对象，其中的gui属性是null
	 * @param projectFile
	 * @return
	 */
	public static LineConfig loadFromDisk(String projectFile) {
		if (projectFile == null){
			return  null;
		}
		try {
			File localFile = new File(projectFile);
			if (localFile.exists()) {
				String jsonstr = FileUtils.readFileToString(localFile);
				LineConfig config = FromJson(jsonstr);
				return config;
			}
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
		return null;
	}


	public String ToJson() {
		return JSON.toJSONString(this);
		//https://blog.csdn.net/qq_27093465/article/details/73277291
		//return new Gson().toJson(this);
	}


	public  static LineConfig FromJson(String instanceString) {// throws Exception {
		return JSON.parseObject(instanceString,LineConfig.class);
		//return new Gson().fromJson(instanceString, LineConfig.class);
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
		if (entry.getStatuscode() == 200 && entry.getTitle().equals("Welcome to nginx!")
				&& entry.getContentLength()<=612 ){
			entry.setCheckStatus(LineEntry.CheckStatus_Checked);
			return entry;
		}

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
