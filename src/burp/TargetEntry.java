package burp;

import java.util.Set;

public class TargetEntry {
	private String domain;
	private Set<String> childURLs;
	private long createdTime;
	private long updatedTime;
	private String group;
	
	public TargetEntry(String domain, Set<String> childURLs) {
		super();
		this.domain = domain;
		this.childURLs = childURLs;
		this.createdTime = System.currentTimeMillis();
	}
	
	
	public TargetEntry(String domain) {
		super();
		this.domain = domain;
		this.createdTime = System.currentTimeMillis();
	}
}
