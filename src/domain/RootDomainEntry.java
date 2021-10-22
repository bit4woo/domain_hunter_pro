package domain;

public class RootDomainEntry {
	private String rootDomain;
	private String keyword;
	private String AuthoritativeNameServer;
	private String ZoneTransfer;//域名所属的业务名称
	private String isBlack;//这个域名是否是黑名单根域名，需要排除的
	private String comment;
	private boolean useTLD;//TLD= Top-Level Domain 
	
	public RootDomainEntry(String rootDomain) {
		
	}
	
	public String getRootDomain() {
		return rootDomain;
	}
	public void setRootDomain(String rootDomain) {
		this.rootDomain = rootDomain;
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
	public String getZoneTransfer() {
		return ZoneTransfer;
	}
	public void setZoneTransfer(String zoneTransfer) {
		ZoneTransfer = zoneTransfer;
	}
	public String getIsBlack() {
		return isBlack;
	}
	public void setIsBlack(String isBlack) {
		this.isBlack = isBlack;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isUseTLD() {
		return useTLD;
	}

	public void setUseTLD(boolean useTLD) {
		this.useTLD = useTLD;
	}
}
