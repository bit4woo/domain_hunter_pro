package burp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.google.common.net.InternetDomainName;

public class DomainObject {
	private String objectName;
	
	public Map<String,String> rootDomainMap = new HashMap<String,String>();
	public Set<String> subDomainSet = new HashSet<String>();
	public Set<String> similarDomainSet = new HashSet<String>();
	public Set<String> relatedDomainSet = new HashSet<String>();
    
    public static int SUB_DOMAIN=0;
    public static int SIMILAR_DOMAIN=1;
    public static int IP_ADDRESS=2;
    public static int USELESS =-1;
    
    public String resultJson;
	
    
    DomainObject(String objectName){
		this.objectName = objectName;
	}



	public String getResultJson() {//to save this object
	    Map<String, Object> result = new HashMap<String, Object>();
	    result.put("objectName", objectName);
	    result.put("rootDomainMap", rootDomainMap);
	    result.put("subDomainSet", subDomainSet);
	    result.put("similarDomainSet", similarDomainSet);
	    result.put("relatedDomainSet", relatedDomainSet);
	    
	    resultJson = JSON.toJSONString(result);
		
		return resultJson;
	}



	public void setResultJson(String resultJson) {
		this.resultJson = resultJson;
		
		try {
			Map<String,Object> Json = (Map<String,Object>)JSON.parse(resultJson);
			for (String key:Json.keySet()) {
				if (key.equals("objectName"))
					this.objectName = (String) Json.get(key);
				if (key.equals("rootDomainMap"))
					this.rootDomainMap = (Map<String, String>) Json.get(key);
				if (key.equals("subDomainSet"))
					this.subDomainSet = (Set<String>) Json.get(key);
				if (key.equals("similarDomainSet"))
					this.similarDomainSet = (Set<String>) Json.get(key);
				if (key.equals("relatedDomainSet"))
					this.relatedDomainSet = (Set<String>) Json.get(key);
				
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	/////////////self implement/////////////////////
	
	public void relatedToRoot() {
		for(String relatedDomain:relatedDomainSet) {
        	String rootDomain =InternetDomainName.from(relatedDomain).topPrivateDomain().toString();
			String keyword = rootDomain.substring(0,rootDomain.indexOf("."));
			if (!rootDomainMap.keySet().contains(rootDomain)) {
				rootDomainMap.put(rootDomain,keyword);
			}
		}
		relatedDomainSet.clear();
			
	}
	
	

	public String getSummary() {
    	String summary = "      Related-domain:%s  Sub-domain:%s  Similar-domain:%s  ^_^";
    	return String.format(summary, relatedDomainSet.size(),subDomainSet.size(),similarDomainSet.size());
    }
    
    public String getRelatedDomains() {
    	return set2string(relatedDomainSet);
    }
	
    public String getSimilarDomains() {
    	return set2string(similarDomainSet);
    }
    
    public String getSubDomains() {
    	return set2string(subDomainSet);
    }
    
	public String getRootDomains() {
		return set2string(rootDomainMap.keySet());
	}
	
	
	public Set<String> getRootDomainSet() {
		return rootDomainMap.keySet();
	}
	
	public Set<String> getKeywordSet(){
		Set<String> result = new HashSet<String>();
		for (String key:rootDomainMap.keySet()) {
			result.add(rootDomainMap.get(key));
		}
		return result;
	}
	
	
	
	
	int domainType(String domain) {
		for (String rootdomain:getRootDomainSet()) {
			if (rootdomain.contains(".")&&!rootdomain.endsWith(".")&&!rootdomain.startsWith("."))
			{
				if (domain.endsWith("."+rootdomain)||domain.equalsIgnoreCase(rootdomain)){
					return DomainObject.SUB_DOMAIN;
				}
			}
		}
		
		for (String keyword:getKeywordSet()) {
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
		String Host ="www.baidu.com";
		Set<String> rootdomains = new HashSet<String>();
		rootdomains.add("baidu.com");
		Set<String> keywords = new HashSet<String>();
		keywords.add("baidu");
		
		int type = new DomainObject("").domainType(Host);
		System.out.println(type);
	}
	
}
