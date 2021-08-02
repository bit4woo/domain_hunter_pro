package domain;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.google.common.net.InternetDomainName;

import GUI.GUI;
import Tools.DomainComparator;
import burp.BurpExtender;
import burp.Commons;

/*
 *注意，所有直接对DomainObject中数据的修改，都不会触发该tableChanged监听器。
 *1、除非操作的逻辑中包含了firexxxx来主动通知监听器。比如DomainPanel.domainTableModel.fireTableChanged(null);
 *2、或者主动调用显示和保存的函数直接完成，不经过监听器。
	//GUI.getDomainPanel().showToDomainUI();
	//DomainPanel.autoSave();
 */
public class DomainManager {
	public String projectName = "";
	public String uploadURL = "Input Upload URL Here";
	public String summary = "";
	public boolean autoAddRelatedToRoot = false;

	private LinkedHashMap<String,String> rootDomainMap = new LinkedHashMap<String,String>();
	//private LinkedHashMap<String,String> rootBlackDomainMap = new LinkedHashMap<String,String>();
	// LinkedHashMap to keep the insert order 
	private Set<String> subnetSet = new HashSet<String>();
	private Set<String> subDomainSet = new HashSet<String>();
	private Set<String> similarDomainSet = new HashSet<String>();
	private Set<String> relatedDomainSet = new HashSet<String>();
	private Set<String> IsTargetButUselessDomainSet = new HashSet<String>();
	//有效(能解析IP)但无用的域名，比如JD的网店域名、首页域名等对信息收集、聚合网段、目标界定有用，但是本身几乎不可能有漏洞的资产。
	private Set<String> NotTargetIPSet = new HashSet<String>();//IP集合，那些非目标资产的IP集合。只存IP，不存网段。
	private HashMap<String,Integer> unkownDomainMap = new HashMap<String,Integer>();//记录域名和解析失败的次数，大于五次就从子域名中删除。
	private Set<String> EmailSet = new HashSet<String>();
	private Set<String> PackageNameSet = new HashSet<String>();

	private Set<String> newAndNotGetTitleDomainSet = new HashSet<String>();

	public static int SUB_DOMAIN=0;
	public static int SIMILAR_DOMAIN=1;
	public static int IP_ADDRESS=2;
	public static int PACKAGE_NAME=3;
	public static int USELESS = -1;
	//public static int BLACKLIST = -2;


	DomainManager(){
		//to resolve "default constructor not found" error
	}

	public DomainManager(String projectName){
		this.projectName = projectName;
	}


	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getUploadURL() {
		return uploadURL;
	}

	public void setUploadURL(String uploadURL) {
		this.uploadURL = uploadURL;
	}

	public boolean isAutoAddRelatedToRoot() {
		return autoAddRelatedToRoot;
	}

	public void setAutoAddRelatedToRoot(boolean autoAddRelatedToRoot) {
		this.autoAddRelatedToRoot = autoAddRelatedToRoot;
	}

	public LinkedHashMap<String, String> getRootDomainMap() {
		return rootDomainMap;
	}

	public void setRootDomainMap(LinkedHashMap<String, String> rootDomainMap) {
		this.rootDomainMap = rootDomainMap;
	}

	public Set<String> getSubnetSet() {
		return subnetSet;
	}

	public void setSubnetSet(Set<String> subnetSet) {
		this.subnetSet = subnetSet;
	}

	public Set<String> getSubDomainSet() {
		return subDomainSet;
	}

	public void setSubDomainSet(Set<String> subDomainSet) {
		this.subDomainSet = subDomainSet;
	}

	public Set<String> getSimilarDomainSet() {
		return similarDomainSet;
	}

	public void setSimilarDomainSet(Set<String> similarDomainSet) {
		this.similarDomainSet = similarDomainSet;
	}

	public Set<String> getRelatedDomainSet() {
		return relatedDomainSet;
	}

	public void setRelatedDomainSet(Set<String> relatedDomainSet) {
		this.relatedDomainSet = relatedDomainSet;
	}

	public Set<String> getIsTargetButUselessDomainSet() {
		return IsTargetButUselessDomainSet;
	}

	public void setIsTargetButUselessDomainSet(Set<String> isTargetButUselessDomainSet) {
		IsTargetButUselessDomainSet = isTargetButUselessDomainSet;
	}


	public Set<String> getNotTargetIPSet() {
		return NotTargetIPSet;
	}

	public void setNotTargetIPSet(Set<String> notTargetIPSet) {
		NotTargetIPSet = notTargetIPSet;
	}

	public Set<String> getEmailSet() {
		return EmailSet;
	}

	public void setEmailSet(Set<String> emailSet) {
		EmailSet = emailSet;
	}

	public Set<String> getPackageNameSet() {
		return PackageNameSet;
	}

	public void setPackageNameSet(Set<String> packageNameSet) {
		PackageNameSet = packageNameSet;
	}

	public Set<String> getNewAndNotGetTitleDomainSet() {
		return newAndNotGetTitleDomainSet;
	}

	public void setNewAndNotGetTitleDomainSet(Set<String> newAndNotGetTitleDomainSet) {
		this.newAndNotGetTitleDomainSet = newAndNotGetTitleDomainSet;
	}

	public String getSummary() {
		String filename ="unknown";
		if (GUI.currentDBFile != null){
			filename = GUI.currentDBFile.getName();
		}
		summary = String.format("  FileName:%s  Root-domain:%s  Related-domain:%s  Sub-domain:%s  Similar-domain:%s  ^_^",
				filename, rootDomainMap.size(),relatedDomainSet.size(),subDomainSet.size(),similarDomainSet.size());
		return summary;
	}

	public void setSummary(String Summary) {
		this.summary = Summary;

	}


	////////////////ser and deser///////////

	public String ToJson() {
		return JSON.toJSONString(this);
		//https://blog.csdn.net/qq_27093465/article/details/73277291
		//return new Gson().toJson(this);
	}


	public  static DomainManager FromJson(String instanceString) {// throws Exception {
		return JSON.parseObject(instanceString,DomainManager.class);
		//return new Gson().fromJson(instanceString, DomainObject.class);
	}


	// below methods is self-defined, function name start with "fetch" to void fastjson parser error

	public String fetchSubnets() {
		return String.join(System.lineSeparator(), subnetSet);
	}

	public String fetchRelatedDomains() {
		return String.join(System.lineSeparator(), relatedDomainSet);
	}

	public String fetchSimilarDomains() {
		return String.join(System.lineSeparator(), similarDomainSet);
	}

	public String fetchSubDomains() {
		List<String> tmplist= new ArrayList<>(subDomainSet);
		Collections.sort(tmplist,new DomainComparator());
		return String.join(System.lineSeparator(), tmplist);
	}

	public String fetchSubDomainsOf(String rootDomain) {
		List<String> tmplist = new ArrayList<>();
		if (domainType(rootDomain)==DomainManager.SUB_DOMAIN) {//判断是否有效rootDomain
			if (!rootDomain.startsWith(".")) {
				rootDomain = "."+rootDomain;
			}
			for (String item:subDomainSet) {
				if(item.endsWith(rootDomain)) {
					tmplist.add(item);
				}
			}
			Collections.sort(tmplist);
			return String.join(System.lineSeparator(), tmplist);
		}
		return "";
	}

	public String fetchEmails() {
		List<String> tmplist= new ArrayList<>(EmailSet);
		Collections.sort(tmplist);
		return String.join(System.lineSeparator(), tmplist);
	}

	public String fetchPackageNames() {
		return String.join(System.lineSeparator(), PackageNameSet);
	}

	public String fetchRootDomains() {
		return String.join(System.lineSeparator(), rootDomainMap.keySet());
	}


	public Set<String> fetchRootDomainSet() {
		Set<String> result = new HashSet<String>();
		for (String key:rootDomainMap.keySet()) {
			if (!key.trim().toLowerCase().startsWith("[exclude]")) {
				result.add(key.trim().toLowerCase());
			}
		}
		return result;
	}

	public Set<String> fetchRootBlackDomainSet() {
		Set<String> result = new HashSet<String>();
		for (String key:rootDomainMap.keySet()) {
			if (key.trim().toLowerCase().startsWith("[exclude]")) {
				result.add(key.trim().toLowerCase());
			}
		}
		return result;
	}

	public Set<String> fetchKeywordSet(){
		Set<String> result = new HashSet<String>();
		for (String key:rootDomainMap.keySet()) {
			String value = rootDomainMap.get(key);
			if (!value.trim().equals("")) {
				result.add(rootDomainMap.get(key));
			}
		}
		return result;
	}

	//主要用于package name的有效性判断
	public Set<String> fetchSuffixSet(){
		Set<String> result = new HashSet<String>();
		for (String key:rootDomainMap.keySet()) {
			String suffix;
			try {
				//InternetDomainName.from(key).publicSuffix() //当不是com、cn等公共的域名结尾时，将返回空。
				suffix = InternetDomainName.from(key).publicSuffix().toString();
			} catch (Exception e) {
				suffix = key.split("\\.",2)[1];//分割成2份
			}
			result.add(suffix);
		}
		return result;
	}


	//这种一般只会是IP类资产才会需要这样判断吧，域名类很容易确定是否属于目标
	public boolean isTargetByBlackList(String HostIP) {
		return !NotTargetIPSet.contains(HostIP);
	}

	/*
	 * 用于判断站点是否是我们的目标范围，原理是根据证书的所有域名中，是否有域名包含了关键词。
	 * 为了避免漏掉有效目标，只有完全确定非目标的才排除！！！
	 */
	public boolean isTargetByCertInfo(Set<String> certDomains) {
		if (certDomains.isEmpty() || certDomains ==null) {//不能判断的，还是暂时认为是在目标中的。
			return true;
		}
		for (String domain:certDomains) {
			if (domainType(domain) == DomainManager.SUB_DOMAIN) {
				return true;
			}
		}
		return false;
	}

	public void AddToRootDomainMap(String key,String value) {
		if (this.rootDomainMap.containsKey(key) && this.rootDomainMap.containsValue(value)) {
			//do nothing
		}else {
			this.rootDomainMap.put(key,value);//这个操作不会触发TableChanged事件。
		}
		//1\主动触发监听器，让监听器去执行数据的保存。
		//DomainPanel.domainTableModel.fireTableChanged(null);
		//2\或者主动调用显示和保存的函数直接完成，不经过监听器。
		//GUI.getDomainPanel().showToDomainUI();
		//DomainPanel.autoSave();
	}

	public void addToDomainOject(String domain){//仅用于鼠标右键，所以加了数据的展示和保存逻辑在里面
		relatedDomainSet.add(domain);
		relatedToRoot();
		GUI.getDomainPanel().showToDomainUI();
		DomainPanel.autoSave();
	}

	public void addToDomainOject(Set<String> domains){//仅用于鼠标右键，所以加了数据的展示和保存逻辑在里面
		relatedDomainSet.addAll(domains);
		relatedToRoot();
		GUI.getDomainPanel().showToDomainUI();
		DomainPanel.autoSave();
	}

	public void relatedToRoot() {
		if (this.autoAddRelatedToRoot == true) {
			for(String relatedDomain:this.relatedDomainSet) {
				if (relatedDomain!=null && relatedDomain.contains(".")) {
					String rootDomain =getRootDomain(relatedDomain);
					if (rootDomain != null) {
						if (fetchRootBlackDomainSet().contains("[exclude]"+rootDomain)){
							continue;
						}					
						String keyword = rootDomain.substring(0,rootDomain.indexOf("."));
						if (!rootDomainMap.keySet().contains(rootDomain) && rootDomain != null) {
							rootDomainMap.put(rootDomain,keyword);
						}
					}
					subDomainSet.add(relatedDomain);
				}else {
					System.out.println("error related domain : "+relatedDomain);
				}
			}
			relatedDomainSet.clear();
		}
		//System.out.println(similarDomainSet);


		Iterator<String> iterator = similarDomainSet.iterator();
		while(iterator.hasNext()){
			String similarDomain = iterator.next();

			String rootDomain =getRootDomain(similarDomain);
			if (rootDomainMap.keySet().contains(rootDomain) && rootDomain != null) {
				subDomainSet.add(similarDomain);
				iterator.remove();
			}
		}

		/*		for (String similarDomain:this.similarDomainSet) {
        	String rootDomain =getRootDomain(similarDomain);
			if (rootDomainMap.keySet().contains(rootDomain) && rootDomain != null) {
				subDomainSet.add(similarDomain);
				similarDomainSet.remove(similarDomain); //lead to "java.util.ConcurrentModificationException" error
			}
		}*/
	}


	public static String getRootDomain(String inputDomain) {
		try {
			if (inputDomain.toLowerCase().startsWith("http://") || inputDomain.toLowerCase().startsWith("https://")) {
				inputDomain = new URL(inputDomain).getHost();
			}
			String rootDomain =InternetDomainName.from(inputDomain).topPrivateDomain().toString();
			return rootDomain;
		}catch(Exception e) {
			return null;
			//InternetDomainName.from("www.jd.local").topPrivateDomain()//Not under a public suffix: www.jd.local
		}
	}

	public int domainType(String domain) {
		try {
			if (domain.contains(":")) {//处理带有端口号的域名
				domain = domain.substring(0,domain.indexOf(":"));
			}

			domain = domain.toLowerCase().trim();
			if (domain.endsWith(".")) {
				domain = domain.substring(0,domain.length()-1);
			}

			if (Commons.isValidIP(domain)) {//https://202.77.129.30
				return DomainManager.IP_ADDRESS;
			}
			if (!Commons.isValidDomain(domain)) {
				return DomainManager.USELESS;
			}
			if (isInRootBlackDomain(domain)) {
				return DomainManager.USELESS;
			}

			for (String rootdomain:fetchRootDomainSet()) {
				if (rootdomain.contains(".")&&!rootdomain.endsWith(".")&&!rootdomain.startsWith("."))
				{
					if (domain.endsWith("."+rootdomain)||domain.equalsIgnoreCase(rootdomain)){
						return DomainManager.SUB_DOMAIN;
					}
				}
			}

			for (String keyword:fetchKeywordSet()) {
				if (!keyword.equals("") && domain.contains(keyword)) {
					if (InternetDomainName.from(domain).hasPublicSuffix()) {//是否是以公开的 .com .cn等结尾的域名。//如果是以比如local结尾的域名，就不会被认可
						return DomainManager.SIMILAR_DOMAIN;
					}

					if (fetchSuffixSet().contains(domain.substring(0, domain.indexOf(".")))){
						return DomainManager.PACKAGE_NAME;
					}
				}
			}

			return DomainManager.USELESS;
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			return DomainManager.USELESS;
		}
	}

	public boolean isRelatedEmail(String email) {
		for (String keyword:fetchKeywordSet()) {
			if (!keyword.equals("") && keyword.length() >= 2 && email.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInRootBlackDomain(String domain) {
		if (domain.contains(":")) {//处理带有端口号的域名
			domain = domain.substring(0,domain.indexOf(":"));
		}
		for (String rootdomain:fetchRootBlackDomainSet()) {
			rootdomain = rootdomain.replace("[exclude]", "");
			if (rootdomain.contains(".")&&!rootdomain.endsWith(".")&&!rootdomain.startsWith("."))
			{
				if (domain.endsWith("."+rootdomain)||domain.equalsIgnoreCase(rootdomain)){
					return true;
				}
			}
		}
		return false;
	}

	public static void main(String args[]) {
		/*		String Host ="www.baidu.com";
		Set<String> rootdomains = new HashSet<String>();
		rootdomains.add("baidu.com");
		Set<String> keywords = new HashSet<String>();
		keywords.add("baidu");

		int type = new DomainObject("").domainType(Host);
		System.out.println(type);*/

		//		DomainObject xx = new DomainObject("");
		//		xx.getRelatedDomainSet().add("xxx.baidu.com");
		//		System.out.println(xx.getRelatedDomainSet());


		//		System.out.println(InternetDomainName.from("www.jd.local").publicSuffix());
		//		System.out.println(InternetDomainName.from("www.jd.local").topPrivateDomain());
		//		System.out.println(whois("jd.ru"));
	}

}
