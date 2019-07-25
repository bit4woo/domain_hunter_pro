package target;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TargetEntry {
	private final static int LEVEL_HIGH = 3;
	private final static int LEVEL_MIDDLE = 2;
	private final static int LEVEL_LOW = 1;
	
	private String domain;
	private Set<TargetEntry> children = new HashSet<TargetEntry>();
	private TargetEntry parent = null;
	private long createdTime;
	private long updatedTime;
	private String group;
	private boolean isBlack = false;
	private boolean isChecked = false;
	private int valueLevel = LEVEL_LOW;
	


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

	
	
	//////////// getter and setter///////////
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


	public TargetEntry getParent() {
		return parent;
	}

	public void setParent(TargetEntry parent) {
		this.parent = parent;
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

	public boolean isBlack() {
		return isBlack;
	}

	public void setBlack(boolean isBlack) {
		this.isBlack = isBlack;
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	public int getValueLevel() {
		return valueLevel;
	}

	public void setValueLevel(int valueLevel) {
		this.valueLevel = valueLevel;
	}
	
	////////////getter and setter///////////
	
	
	

	public Object getChildAt(int index) {
		return children.toArray()[index];
	}
	
    public int getIndexOfChild(TargetEntry kid) {
        return (new ArrayList<TargetEntry>(children)).indexOf(kid);
    }
    
	@Override
	public String toString() { return domain+isChecked(); }

	public String toJson(){
		return JSON.toJSONString(this);
	}

	public static TargetEntry restoreFromJson(String targetJson){
		return JSON.parseObject(targetJson,TargetEntry.class);
	}
	//TODO
	public void getTitle() {
		
	}

	public static void main(String[] args){
		TargetEntry test = new TargetEntry("test");
		System.out.println(test.toJson());
		System.out.println(restoreFromJson(test.toJson()));
	}
}
