package toElastic;

import com.google.gson.Gson;

/**
 * {
  "product_type": 0,
  "type": 0,
  "env": 0,
  "level": 0,
  "title": "string",
  "status": 0,
  "label": "string",
  "tags": "string",
  "name": "string",
  "description": "string",
  "references": "string"
}
 */
public class VMPEntry {
	int product_type =2;//必填
	int type = 10; //资产类型：0未知，1服务器，2站点portal，3代码仓库，4APP，10other
	int env = 0;  // 环境，0未知
	int level = 0; //资产重要性 0默认
	String found_by = "burpsuite-plugin"; //发现者，如bp插件
	String name = ""; //资产名称，可以是ip，域名，网址等
	String description = "burp";// # 资产描述，关于资产相关信息可以拼接为本字段
	String references = "burp";//# 参考信息
	String label = "";
	String tags = "";  //可以存放Server字段
	String title = ""; //web title
	int status = 0; //web status code

	public VMPEntry(String hostOrUrl,String title){
		hostOrUrl = hostOrUrl.toLowerCase().trim();
		name = hostOrUrl;
		type = 2;
		description = !title.equals("") ? title:"from burp";
		/*
		if (hostOrUrl.startsWith("http://") || hostOrUrl.startsWith("https://")) {
			name = hostOrUrl;
			type = 2;
			description = title != "" ? title:"from burp";
		}else {
			name = hostOrUrl;
			type = 1;
			description = title != "" ? title:"from burp";
		}*/
	}
	
	public VMPEntry(String hostOrUrl,String title,String IPStr,String headerServer){
		hostOrUrl = hostOrUrl.toLowerCase().trim();
		name = hostOrUrl;
		type = 2;
		this.title = title;
		tags = headerServer;
		description = "IP: " + IPStr+System.lineSeparator();
		if (description.equals("")) {
			description = "from burp";
		}
	}

	public  String toJson() {
		return new Gson().toJson(this,VMPEntry.class);
	}
	
	public static void main(String[] args) {
		String tmp = new VMPEntry("shopee.com","shopee offical website").toJson();
		System.out.println(tmp);
	}
}
