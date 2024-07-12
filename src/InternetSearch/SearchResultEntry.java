package InternetSearch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.UrlUtils;

import burp.BurpExtender;
import domain.DomainManager;

public class SearchResultEntry {
	private int port = -1;
	private String host = "";
	private String protocol = "";
	private String uri = null;
	private String rootDomain;
	private String webcontainer = "";
	private String title = "";
	private Set<String> CertDomainSet = new HashSet<String>();

	private Set<String> IPSet = new HashSet<String>();

	private String icon_url = "";
	private byte[] icon_bytes = new byte[0];
	private String icon_hash = "";
	private String source = "";
	private String ASNInfo = "";

	private int AsnNum =-1;

	public SearchResultEntry(String host) {

	}

	public SearchResultEntry() {

	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if (UrlUtils.isVaildUrl(host)) {
			this.uri = host;
			this.host = UrlUtils.getHost(host);
		}else if(DomainUtils.isValidDomainMayPort(host)){//包含端口的
			List<String> hosts = DomainUtils.grepDomainNoPort(host);
			if (hosts.size()>0) {
				this.host = hosts.get(0);
			}
		}else {
			List<String> hosts = IPAddressUtils.grepIPv4NoPort(host);
			if (hosts.size()>0) {
				this.host = hosts.get(0);
			}
		}
		if (StringUtils.isEmpty(this.host)) {
			this.host = host;
		}

		if (StringUtils.isEmpty(rootDomain)) {
			if(DomainUtils.isValidDomainMayPort(host)) {
				this.rootDomain = DomainUtils.getRootDomain(host);
			}
		}
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getRootDomain() {
		return rootDomain;
	}

	public void setRootDomain(String rootDomain) {
		if(DomainUtils.isValidDomainMayPort(rootDomain)) {
			this.rootDomain = DomainUtils.getRootDomain(rootDomain);
		}else{
			this.rootDomain = rootDomain;
		}
	}

	public String getWebcontainer() {
		return webcontainer;
	}

	public void setWebcontainer(String webcontainer) {
		this.webcontainer = webcontainer;
	}

	public Set<String> getCertDomainSet() {
		return CertDomainSet;
	}

	public void setCertDomainSet(Set<String> certDomainSet) {
		CertDomainSet = certDomainSet;
	}

	public Set<String> getIPSet() {
		return IPSet;
	}

	public void setIPSet(Set<String> iPSet) {
		IPSet = iPSet;
	}

	public String getIcon_url() {
		return icon_url;
	}

	public void setIcon_url(String icon_url) {
		this.icon_url = icon_url;
	}

	public byte[] getIcon_bytes() {
		return icon_bytes;
	}

	public void setIcon_bytes(byte[] icon_bytes) {
		this.icon_bytes = icon_bytes;
	}

	public String getIcon_hash() {
		return icon_hash;
	}

	public void setIcon_hash(String icon_hash) {
		this.icon_hash = icon_hash;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getASNInfo() {
		return ASNInfo;
	}

	public void setASNInfo(String aSNInfo) {
		ASNInfo = aSNInfo;
	}

	public int getAsnNum() {
		return AsnNum;
	}

	public void setAsnNum(int asnNum) {
		AsnNum = asnNum;
	}

	public String getIdentify() {
		//不同搜索引擎的结果，是否要去重？
		return getUri()+"#"+System.currentTimeMillis();
	}

	/**
	 * 类似： http://www.example.com:8442
	 * @return
	 */
	private String buildUri() {
		StringBuilder sb = new StringBuilder();
		if (protocol != null && protocol.length()>0) {
			sb.append(protocol);
			sb.append("://");
		}
		if (host != null && host.length()>0) {
			sb.append(host);
		}
		if (port>=0 && port<=65535) {
			sb.append(":").append(port);
		}
		try {
			uri = UrlUtils.getFullUrlWithDefaultPort(sb.toString());
		} catch (Exception e) {
			uri = sb.toString();
		}
		return uri;
	}


	public String getUri() {
		if (uri != null && uri.length()>0) {
			return uri;
		}
		return buildUri();
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	public void AddToTarget() {
		AddToTarget(null);
	}

	public void AddToTarget(String trustLevel) {
		DomainManager domainResult = BurpExtender.getGui().getDomainPanel().getDomainResult();
		if (IPAddressUtils.isValidIPv4NoPort(this.host)) {
			domainResult.getSpecialPortTargets().add(this.host);
			if (this.port >=0 && this.port <= 65535) {
				domainResult.getSpecialPortTargets().add(this.host+":"+this.port);
			}
		}

		if (DomainUtils.isValidDomainMayPort(this.host)) {
			domainResult.addToTargetAndSubDomain(this.host,true);
			if (this.port >=0 && this.port <= 65535) {
				domainResult.addToTargetAndSubDomain(this.host+":"+this.port,true,trustLevel);
			}
		}

		if (StringUtils.isEmpty(this.rootDomain)) {
			domainResult.addToTargetAndSubDomain(this.rootDomain,true,trustLevel);
		}
	}

	@Override
	public String toString() {
		return "SearchResultEntry [uri=" + uri +", port=" + port + ", host=" + host + ", protocol=" + protocol + ", rootDomain="
				+ rootDomain + ", webcontainer=" + webcontainer + ", title=" + title + ", CertDomainSet="
				+ CertDomainSet + ", IPSet=" + IPSet + ", icon_url=" + icon_url + ", icon_bytes="
				+ Arrays.toString(icon_bytes) + ", icon_hash=" + icon_hash + ", source=" + source + ", ASNInfo="
				+ ASNInfo + "]";
	}

	public static void main(String[] args) {
		SearchResultEntry item = new SearchResultEntry();
		item.setHost("11.11.11.11:7000");
		System.out.println(item.toString());
	}
}
