package domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Deprecated //暂未启用
public class SubDomainEntry {
	
	private String domain;
	private String fromDomain;
	private String fromUrl;
	private String business;//域名所属的业务名称
	private String foundTime;//域名的发现时间，新发现的域名需重点关注。
	private String resolveFailedTimes;//解析失败的次数，多次解析失败的域名是否需要移除呢？
	private String isBlack;//这个域名是否是黑名单的
	
	//内网域名，外网域名
	//出现频率太高的域名，这类域名被很多人反复测试了，不需要关注 HighFrequencyDomain，但不太好界定
	//完全不能解析的域名：记录域名和解析失败的次数，大于五次并且时间超过一年，就从子域名中删除。invalidDomain
	//可以解析IP，但业务上无用的域名，比如JD的网店域名；唯一的用处是用来聚合网段。DoNotCareDomain

	
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
