package domain.target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import com.alibaba.fastjson.JSON;
import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.google.common.net.InternetDomainName;

import base.Commons;
import burp.BurpExtender;
import domain.DomainManager;

public class TargetEntry {
	private String target = "";//根域名、网段、或者IP
	private String type = "";
	private String keyword = "";
	private Set<String> AuthoritativeNameServers = new HashSet<>();//权威服务器
	private boolean ZoneTransfer = false;//域名对应的权威服务器，是否存在域于传送漏洞
	private transient boolean isBlack = false;//这个域名是否是黑名单根域名，需要排除的。不再使用这个字段，由trustLevel代替
	private Set<String> comments = new HashSet<>();
	private boolean useTLD = true;//TLD= Top-Level Domain,比如 baidu.com为true，*.m.baidu.com为false
	private String trustLevel = AssetTrustLevel.Maybe;
	private int subdomainCount = 0;

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
	
	public TargetEntry(String input,boolean autoSub,String trustLevel,String comment) {
		this(input,autoSub);
		if (AssetTrustLevel.getLevelList().contains(trustLevel) && !this.trustLevel.equals(AssetTrustLevel.Cloud)) {
			this.setTrustLevel(trustLevel);
		}else {
			//已经有默认初始值了，无需再设置
		}
		addComment(comment);
	}
	
	public TargetEntry(String input,boolean autoSub,String trustLevel) {
		this(input,autoSub);
		if (AssetTrustLevel.getLevelList().contains(trustLevel)) {
			this.setTrustLevel(trustLevel);
		}else {
			//已经有默认初始值了，无需再设置
		}
	}
	
	private void autoDetectTrustLevel() {
		//resources/cloud_service_domain_names.txt
		String domains = "aliyun.com\r\n"
				+ "aliyuncs.com\r\n"
				+ "amazon.com\r\n"
				+ "amazonaws.com\r\n"
				+ "huaweicloud.com\r\n"
				+ "myhuaweicloud.com\r\n"
				+ "hwclouds-dns.com\r\n"
				+ "myqcloud.com\r\n"
				+ "tencent.com\r\n"
				+ "tencentcloudapi.com\r\n"
				+ "cloudfront.net";
		for (String item:domains.split("\r\n")) {
			if (target.toLowerCase().trim().endsWith(item)) {
				this.setTrustLevel(AssetTrustLevel.Cloud);
				break;
			}
		}
	}

	public TargetEntry(String input,boolean autoSub) {

		String domain = DomainUtils.clearDomainWithoutPort(input);

		if (EmailValidator.getInstance().isValid(domain)){
			domain = domain.substring(domain.indexOf("@")+1);
		}

		if (DomainUtils.isValidDomainNoPort(domain)) {
			type = Target_Type_Domain;

			useTLD = autoSub;
			if (autoSub) {
				target = DomainUtils.getRootDomain(domain);
			}else{
				target = domain;
			}
			keyword = target.substring(0, target.indexOf("."));
		} else if (IPAddressUtils.isValidSubnet(domain)) {
			type = Target_Type_Subnet;
			target = domain;
		} else if (DomainUtils.isValidWildCardDomain(domain)) {
			/**需要将它改造为正则表达式，去匹配域名
			 * seller.*.example.*
			 * seller.*.example.*
			 */
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
		}
		autoDetectTrustLevel();
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
	
	@Deprecated
	public boolean isBlack() {
		return isBlack;
	}
	
	@Deprecated
	public void setBlack(boolean isBlack) {
		this.isBlack = isBlack;
	}
	
	public boolean isNotTarget() {
		return trustLevel.equalsIgnoreCase(AssetTrustLevel.NonTarget);
	}
	
	public Set<String> getComments() {
		return comments;
	}

	public void setComments(Set<String> comments) {
		this.comments = comments;
	}

	public void addComment(String commentToAdd) {
		if (StringUtils.isBlank(commentToAdd)) return;
		comments.addAll(Arrays.asList(commentToAdd.split(",")));
	}

	public boolean isUseTLD() {
		return useTLD;
	}

	public void setUseTLD(boolean useTLD) {
		this.useTLD = useTLD;
	}

	public String getTrustLevel() {
		if (isBlack) {
			//兼容旧版本
			trustLevel = AssetTrustLevel.NonTarget;
		}
		return trustLevel;
	}

	public void setTrustLevel(String trustLevel) {
		this.trustLevel = trustLevel;
	}
	
	public String switchTrustLevel() {
		return trustLevel =  AssetTrustLevel.getNextLevel(trustLevel);
	}
	

	public int getSubdomainCount() {
		return subdomainCount;
	}

	public void setSubdomainCount(int subdomainCount) {
		this.subdomainCount = subdomainCount;
	}
	
	
	public void countSubdomain(Set<String> domains) {
		this.subdomainCount = 0;
		if (this.type.equals(Target_Type_Domain)) {
			for (String domain:domains) {
				if (domain.endsWith("." + this.target) || domain.equalsIgnoreCase(this.target)) {
					this.subdomainCount++;
				}
			}
		}
		
		if (this.type.equals(Target_Type_Wildcard_Domain)) {
			for (String domain:domains) {
				if (DomainUtils.isMatchWildCardDomain(this.target, domain)) {
					this.subdomainCount++;
				}
			}
		}

		if (this.type.equals(Target_Type_Subnet)) {
			this.subdomainCount =IPAddressUtils.toIPList(this.target).size();
		}
	}

	public void zoneTransferCheck() {
		String rootDomain = InternetDomainName.from(target).topPrivateDomain().toString();
		AuthoritativeNameServers = new HashSet<>(DomainUtils.GetAuthServer(rootDomain,null));
		
		for (String Server : AuthoritativeNameServers) {
			//stdout.println("checking [Server: "+Server+" Domain: "+rootDomain+"]");
			List<String> Records = DomainUtils.ZoneTransferCheck(rootDomain, Server);
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
				this.addComment("AXFR!");
			}
		}
	}
}
