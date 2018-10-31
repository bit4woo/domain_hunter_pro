package burp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.alibaba.fastjson.JSON;
import com.google.common.net.InternetDomainName;

public class DomainObject {
	public String projectName = "";
	public String uploadURL = "Input Upload URL Here";
	public String summary = "";
	public boolean autoAddRelatedToRoot = false; 
	
	public LinkedHashMap<String,String> rootDomainMap = new LinkedHashMap<String,String>();
	// LinkedHashMap to keep the insert order 
	public Set<String> subDomainSet = new HashSet<String>();
	public Set<String> similarDomainSet = new HashSet<String>();
	public Set<String> relatedDomainSet = new HashSet<String>();
	
	
	
    public static int SUB_DOMAIN=0;
    public static int SIMILAR_DOMAIN=1;
    public static int IP_ADDRESS=2;
    public static int USELESS =-1;
    
    
    DomainObject(){
    	//to resolve "default constructor not found" error
	}
    
    DomainObject(String projectName){
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
	

	public String getSummary() {
		summary = String.format("      Related-domain:%s  Sub-domain:%s  Similar-domain:%s  ^_^", relatedDomainSet.size(),subDomainSet.size(),similarDomainSet.size());
		return summary;
    }
	
	public void setSummary(String Summary) {
		this.summary = Summary;
		
	}
    

	////////////////ser and deser///////////
	
	public String Save() {
    	return JSON.toJSONString(this);
    }
    
    
    public  DomainObject Open(String instanceString) {// throws Exception {
    	return JSON.parseObject(instanceString, DomainObject.class);
    }
	
    
    // below methods is self-defined, function name start with "fetch" to void fastjson parser error
    

    public String fetchRelatedDomains() {
    	return set2string(relatedDomainSet);
    }
	
    public String fetchSimilarDomains() {
    	return set2string(similarDomainSet);
    }
    
    public String fetchSubDomains() {
    	return set2string(subDomainSet);
    }
    
	public String fetchRootDomains() {
		return set2string(rootDomainMap.keySet());
	}
	
	
	public Set<String> fetchRootDomainSet() {
		return rootDomainMap.keySet();
	}
	
	public Set<String> fetchKeywordSet(){
		Set<String> result = new HashSet<String>();
		for (String key:rootDomainMap.keySet()) {
			result.add(rootDomainMap.get(key));
		}
		return result;
	}
    
    
    
	
    public void AddToRootDomainMap(String key,String value) {
    	if (this.rootDomainMap.containsKey(key) && this.rootDomainMap.containsValue(value)) {
    		//do nothing
    	}else {
    		this.rootDomainMap.put(key,value);
    	}
    }

	
	public void relatedToRoot() {
		if (this.autoAddRelatedToRoot == true) {
			for(String relatedDomain:this.relatedDomainSet) {
	        	String rootDomain =getRootDomain(relatedDomain);
				String keyword = rootDomain.substring(0,rootDomain.indexOf("."));
				if (!rootDomainMap.keySet().contains(rootDomain) && rootDomain != null) {
					rootDomainMap.put(rootDomain,keyword);
				}
			}
			//relatedDomainSet.clear();
		}
		System.out.println(similarDomainSet);
		

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
			String rootDomain =InternetDomainName.from(inputDomain).topPrivateDomain().toString();
			return rootDomain;
		}catch(Exception e) {
			return null;
		}
	}
    
    public int domainType(String domain) {
		for (String rootdomain:fetchRootDomainSet()) {
			if (rootdomain.contains(".")&&!rootdomain.endsWith(".")&&!rootdomain.startsWith("."))
			{
				if (domain.endsWith("."+rootdomain)||domain.equalsIgnoreCase(rootdomain)){
					return DomainObject.SUB_DOMAIN;
				}
			}
		}
		
		for (String keyword:fetchKeywordSet()) {
			if (!keyword.equals("") && domain.contains(keyword)){
				return DomainObject.SIMILAR_DOMAIN;
			}
		}
			
		if (Commons.validIP(domain)) {//https://202.77.129.30
			return DomainObject.IP_ADDRESS;
		}
		return DomainObject.USELESS;
	}
	
	
	public static String set2string(Set<?> set){
	    Iterator iter = set.iterator();
	    StringBuilder result = new StringBuilder();
	    while(iter.hasNext())
	    {
	        //System.out.println(iter.next());  	
	    	result.append(iter.next()).append("\n");
	    }
	    return result.toString();
	}
	
	
	
	public static void main(String args[]) {
/*		String Host ="www.baidu.com";
		Set<String> rootdomains = new HashSet<String>();
		rootdomains.add("baidu.com");
		Set<String> keywords = new HashSet<String>();
		keywords.add("baidu");
		
		int type = new DomainObject("").domainType(Host);
		System.out.println(type);*/
		
		DomainObject xx = new DomainObject("");
		xx.rootDomainMap.put("baidu.com", "baidu");
		xx.relatedDomainSet.add("xxx.baidu.com");
		xx.relatedToRoot();
		System.out.println(xx.rootDomainMap.keySet());
		
	}
	
}
