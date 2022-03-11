package domain.target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.net.InternetDomainName;

import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.IPAddressUtils;
import domain.DomainPanel;

public class TargetEntry {
	private String target;//根域名、网段、或者IP
	private String type;
	private String keyword = "";
	private String AuthoritativeNameServer;
	private boolean ZoneTransfer = false;//域名对应的权威服务器，是否域于传送漏洞
	private boolean isBlack = false;//这个域名是否是黑名单根域名，需要排除的
	private String comment = "";
	private boolean useTLD = true;//TLD= Top-Level Domain,比如 baidu.com为true，*.m.baidu.com为false

	public static final String Target_Type_Domain = "Domain";
	public static final String Target_Type_Subnet = "Subnet";
	public static final String Target_Type_IPaddress = "IP";

	private static final String[]  TargetTypeArray = {Target_Type_Domain,Target_Type_Subnet,Target_Type_IPaddress};
	public static List<String> TargetTypeList = new ArrayList<>(Arrays.asList(TargetTypeArray));

	public static void main(String[] args) {
		TargetEntry aa = new TargetEntry("www.baidu.com");
		System.out.println(JSON.toJSONString(aa));
	}

	public TargetEntry(String input) {
		this(input,true);
	}
	public TargetEntry(String input,boolean autoSub) {
		if (input == null) return;
		String domain = DomainNameUtils.cleanDomain(input);
		if (DomainNameUtils.isValidDomain(domain)) {
			type = Target_Type_Domain;

			useTLD = autoSub;
			if (autoSub) {
				target = DomainNameUtils.getRootDomain(domain);
			}
			keyword = target.substring(0, target.indexOf("."));

			try {
				DomainPanel.getDomainResult().getSubDomainSet().add(domain);
				DomainPanel.getDomainResult().getRelatedDomainSet().remove(domain);//刷新时不能清空，所有要有删除操作。
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//IP
		if (domain != null && IPAddressUtils.isValidIP(domain)) {
			type = Target_Type_IPaddress;
			target = domain;
		}
		
		if (domain != null && IPAddressUtils.isValidSubnet(domain)) {
			type = Target_Type_Subnet;
			target = domain;
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
	public String getAuthoritativeNameServer() {
		return AuthoritativeNameServer;
	}
	public void setAuthoritativeNameServer(String authoritativeNameServer) {
		AuthoritativeNameServer = authoritativeNameServer;
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
		Set<String> NS = DomainNameUtils.GetAuthoritativeNameServer(rootDomain);
		for (String Server : NS) {
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
