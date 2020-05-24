package domain;

public class DomainEntry {
	
	private String domain;
	private String fromDomain;
	private String fromUrl;
	private String business;//域名所属的业务名称
	
	
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
