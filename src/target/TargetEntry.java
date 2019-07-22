package target;

import java.util.HashSet;
import java.util.Set;

public class TargetEntry {
	private String domain;
	private Set<TargetEntry> children = new HashSet<TargetEntry>();
	private long createdTime;
	private long updatedTime;
	private String group;
	
	public TargetEntry(String domain, Set<TargetEntry> childURLs) {
		super();
		this.domain = domain;
		this.children = childURLs;
		this.createdTime = System.currentTimeMillis();
	}
	
	public TargetEntry(String domain) {
		super();
		this.domain = domain;
		this.createdTime = System.currentTimeMillis();
	}
	
	
	public String getDomain() {
		return domain;
	}


	public void setDomain(String domain) {
		this.domain = domain;
	}


	public Set<TargetEntry> getChildren() {
		return children;
	}


	public void setChildren(Set<TargetEntry> children) {
		this.children = children;
	}


	public long getCreatedTime() {
		return createdTime;
	}


	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}


	public long getUpdatedTime() {
		return updatedTime;
	}


	public void setUpdatedTime(long updatedTime) {
		this.updatedTime = updatedTime;
	}


	public String getGroup() {
		return group;
	}


	public void setGroup(String group) {
		this.group = group;
	}

	public Object getChildAt(int index) {
		return children.toArray()[index];
	}
}
