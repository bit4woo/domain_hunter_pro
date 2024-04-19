package config;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

public class ConfigEntry {

	private String key = "";
	private String value = "";
	private String type ="";
	private boolean enable = true;
	private boolean display = true;
	private String comment = "";
	public static final String Value_Type_String = "String";
	public static final String Value_Type_Boolean = "Boolean";

	public ConfigEntry(){
		//to resolve "default constructor not found" error
	}

	public ConfigEntry(String key,String value,String comment,boolean enable){
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.enable = enable;
	}

	public ConfigEntry(String key,String value,String comment,boolean enable,boolean display){
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.enable = enable;
		this.display = display;
		detectType();
	}

	public ConfigEntry(String key,String value,String comment,String type,boolean enable,boolean display){
		this.key = key;
		this.value = value;
		this.comment = comment;
		this.enable = enable;
		this.display = display;
		this.type = type;
	}

	private void detectType(){
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")){
			this.type = Value_Type_Boolean;
		}else {
			this.type = Value_Type_String;
		}
	}
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		if (StringUtils.isEmpty(type)){
			detectType();
		}
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isDisplay() {
		return display;
	}

	public void setDisplay(boolean display) {
		this.display = display;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String ToJson(){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return new Gson().toJson(this);
	}

	public ConfigEntry FromJson(String json){//注意函数名称，如果是get set开头，会被认为是Getter和Setter函数，会在序列化过程中被调用。
		return new Gson().fromJson(json, ConfigEntry.class);
	}
}
