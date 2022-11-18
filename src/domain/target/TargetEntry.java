package domain.target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.net.InternetDomainName;

import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.IPAddressUtils;

public class TargetEntry {
	private String target = "";//根域名、网段、或者IP
	private String type = "";
	private String keyword = "";
	private Set<String> AuthoritativeNameServers = new HashSet<String>();
	private boolean ZoneTransfer = false;//域名对应的权威服务器，是否域于传送漏洞
	private boolean isBlack = false;//这个域名是否是黑名单根域名，需要排除的
	private String comment = "";
	private boolean useTLD = true;//TLD= Top-Level Domain,比如 baidu.com为true，*.m.baidu.com为false

	public static final String Target_Type_Domain = "Domain";
	public static final String Target_Type_Wildcard_Domain = "WildcardDomain"; //
	public static final String Target_Type_Subnet = "Subnet";
	//public static final String Target_Type_IPaddress = "IP";//弃用，target应该是一个范围控制记录，单个IP不适合，直接放入Certain IP集合

	private static final String[]  TargetTypeArray = {Target_Type_Domain,Target_Type_Wildcard_Domain,Target_Type_Subnet};
	public static List<String> TargetTypeList = new ArrayList<>(Arrays.asList(TargetTypeArray));

	public static void main(String[] args) {
		TargetEntry aa = new TargetEntry("www.baidu.com");
		System.out.println(JSON.toJSONString(aa));
	}

	public TargetEntry() {

	}

	public TargetEntry(String input) {
		this(input,true);
	}


	public TargetEntry(String input,boolean autoSub) {

		String domain = DomainNameUtils.clearDomainWithoutPort(input);
		if (DomainNameUtils.isValidDomain(domain)) {
			type = Target_Type_Domain;

			useTLD = autoSub;
			if (autoSub) {
				target = DomainNameUtils.getRootDomain(domain);
			}else{
				target = domain;
			}
			keyword = target.substring(0, target.indexOf("."));

			/**
			 * 假如用户手动编辑了target。那么就需要依靠刷新的操作来更新数据。所以单纯靠添加时的处理逻辑是不够的。
			try {
				DomainPanel.getDomainResult().getSubDomainSet().add(domain);
				DomainPanel.getDomainResult().getRelatedDomainSet().remove(domain);//刷新时不操作相关域名集合，所有要有删除操作。
			} catch (Exception e) {
				e.printStackTrace();
			}*/
		}
		else if (IPAddressUtils.isValidSubnet(domain)) {
			type = Target_Type_Subnet;
			target = domain;
		}

		/**需要将它改造为正则表达式，去匹配域名
		 * seller.*.example.*
		 * seller.*.example.*
		 */
		else if (DomainNameUtils.isValidWildCardDomain(domain)) {
			type = Target_Type_Wildcard_Domain;
			target = domain;

			//先替换*.如果末尾有*自然会被剩下。

			//final String DOMAIN_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
			String domainKeyword = domain;
			domainKeyword = domainKeyword.replaceAll("\\*","");
			domainKeyword = domainKeyword.replaceAll("\\.\\.","\\.");
			if (domainKeyword.indexOf(".") > 0){
				keyword = domainKeyword.substring(0, domainKeyword.indexOf("."));
			}else {
				keyword = domainKeyword;
			}

			/**
			 * 假如用户手动编辑了target。那么就需要依靠刷新的操作来更新数据。所以单纯靠添加时的处理逻辑是不够的。
			HashSet<String> tmpSet = new HashSet<>(DomainPanel.getDomainResult().getRelatedDomainSet());
			for (String tmp : tmpSet){
				//replaceFirst的参数也是正则，能代替正则匹配？
				if (DomainNameUtils.isMatchWildCardDomain(domain,tmp)){
					DomainPanel.getDomainResult().getSubDomainSet().add(tmp);
					DomainPanel.getDomainResult().getRelatedDomainSet().remove(tmp);//刷新时不操作相关域名集合，所有要有删除操作。
				}
			}
			*/
		}
	}


	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public Set<String> getAuthoritativeNameServers() {
		return AuthoritativeNameServers;
	}

	public void setAuthoritativeNameServers(Set<String> authoritativeNameServers) {
		AuthoritativeNameServers = authoritativeNameServers;
	}

	public boolean isZoneTransfer() {
		return ZoneTransfer;
	}
	public void setZoneTransfer(boolean zoneTransfer) {
		ZoneTransfer = zoneTransfer;
	}
	public boolean isBlack() {
		return isBlack;
	}
	public void setBlack(boolean isBlack) {
		this.isBlack = isBlack;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}

	public void addComment(String commentToAdd) {
		if (commentToAdd == null || commentToAdd.trim().equals("")) return;

		String comments =getComment();
		if (!comments.contains(commentToAdd)) {
			comments += ","+commentToAdd;
			this.setComment(comments);
		}
	}

	public boolean isUseTLD() {
		return useTLD;
	}

	public void setUseTLD(boolean useTLD) {
		this.useTLD = useTLD;
	}

	public void zoneTransferCheck() {
		String rootDomain = InternetDomainName.from(target).topPrivateDomain().toString();
		AuthoritativeNameServers = DomainNameUtils.GetAuthoritativeNameServer(rootDomain);
		
		for (String Server : AuthoritativeNameServers) {
			//stdout.println("checking [Server: "+Server+" Domain: "+rootDomain+"]");
			List<String> Records = DomainNameUtils.ZoneTransferCheck(rootDomain, Server);
			if (Records.size() > 0) {
				try {
					//stdout.println("!!! "+Server+" is zoneTransfer vulnerable for domain "+rootDomain+" !");
					File file = new File(Server + "-ZoneTransfer-" + Commons.getNowTimeString() + ".txt");
					file.createNewFile();
					FileUtils.writeLines(file, Records);
					BurpExtender.getStdout().println("!!! Records saved to " + file.getAbsolutePath());
				} catch (IOException e1) {
					e1.printStackTrace(BurpExtender.getStderr());
				}
			}
		}
	}
}
