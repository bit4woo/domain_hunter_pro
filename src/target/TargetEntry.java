package target;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/*
为了正常序列号和反序列化，非getter和setter的函数，不要使用get、set开头！
或者使用@JSONField(serialize=false)//表明不序列号该字段 这个注解
 */
public class TargetEntry {
	private final static int LEVEL_HIGH = 3;
	private final static int LEVEL_MIDDLE = 2;
	private final static int LEVEL_LOW = 1;
	
	private String domain;
	private Set<TargetEntry> children = new HashSet<TargetEntry>();
	//private transient TargetEntry parent = null;
	//由于gson默认会递归的去序列化对象，如果这里用了TargetEntry去存储父节点，将陷入死循环！所以改为使用父节点名称（域名）
	private String parentName;
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


//	public TargetEntry getParent() {
//		return parent;
//	}
//
//	public void setParent(TargetEntry parent) {
//		this.parent = parent;
//	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
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
		return  new Gson().toJson(this);
	}

	public static TargetEntry restoreFromJson(String targetJson){
		//return JSON.parseObject(targetJson,TargetEntry.class);
		return new Gson().fromJson(targetJson,TargetEntry.class);
	}
	//TODO
	public void getTitle() {
		
	}

	public static void main(String[] args){
		TargetEntry test = new TargetEntry("test");
		TargetEntry test1 = new TargetEntry("test1");
		test.getChildren().add(test1);
		test1.setParentName(test.getDomain());
		System.out.println(test.toJson());

		TargetEntry restored = restoreFromJson(test.toJson());
		System.out.println(restored);
	}
}
