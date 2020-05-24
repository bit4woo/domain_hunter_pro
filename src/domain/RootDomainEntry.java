package domain;

public class RootDomainEntry {
	private String rootDomain;
	private String keyword;
	private String AuthoritativeNameServer;
	private String ZoneTransfer;//域名所属的业务名称
	private String isBlack;//这个域名是否是黑名单根域名，需要排除的
}
