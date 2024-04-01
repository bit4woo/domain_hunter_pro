package InternetSearch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import utils.URLUtils;

public class SearchResultEntry {
	private int port = -1;
	private String host = "";
	private String protocol = "";
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
		this.host = host;
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
		this.rootDomain = rootDomain;
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
	
	public String getIdentify() {
		if (URLUtils.isVaildUrl(host)) {
			host = URLUtils.getHost(host);
		}
		return host+":"+port;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "SearchResultEntry [port=" + port + ", host=" + host + ", protocol=" + protocol + ", rootDomain="
				+ rootDomain + ", webcontainer=" + webcontainer + ", title=" + title + ", CertDomainSet="
				+ CertDomainSet + ", IPSet=" + IPSet + ", icon_url=" + icon_url + ", icon_bytes="
				+ Arrays.toString(icon_bytes) + ", icon_hash=" + icon_hash + ", source=" + source + ", ASNInfo="
				+ ASNInfo + "]";
	}
	
}
