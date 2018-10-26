package burp;

import java.util.Set;

import com.alibaba.fastjson.JSON;

public class DomainResult {
	private String objectName;
	private Set<String> rootDomains;
	private Set<String> keyWords;
	
	private Set<String> subDomains;
	private Set<String> similarDomains;
	
	
	DomainResult(String objectName){
		this.objectName = objectName;
	}
	
	
	
	public String save() {
		JSON json = null;
		JSON.toJavaObject(json, this.getClass());
		return objectName;
	}
}
