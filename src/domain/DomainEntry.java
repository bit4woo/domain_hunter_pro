package domain;

public class DomainEntry {
	
	private String domain;
	private String fromDomain;
	private String fromUrl;
	private String business;//域名所属的业务名称
	private String foundTime;//域名的发现时间，新发现的域名需重点关注。
	private String resolveFailedTimes;//解析失败的次数，多次解析失败的域名是否需要移除呢？
	private String isBlack;//这个域名是否是很名单的
	
	
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getFromDomain() {
		return fromDomain;
	}
	public void setFromDomain(String fromDomain) {
		this.fromDomain = fromDomain;
	}
	public String getFromUrl() {
		return fromUrl;
	}
	public void setFromUrl(String fromUrl) {
		this.fromUrl = fromUrl;
	}
	public String getBusiness() {
		return business;
	}
	public void setBusiness(String business) {
		this.business = business;
	}
}
