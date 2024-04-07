package domain;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import base.Commons;
import config.ConfigManager;
import config.ConfigName;
import config.ConfigPanel;

public class CertInfo {
	
	
	private static String proxyHost;
	private static Integer proxyPort;
	
	@Deprecated
	public CertInfo(String proxyHost, Integer proxyPort) {
		CertInfo.proxyHost = proxyHost;
		CertInfo.proxyPort = proxyPort;
	}
	
	public CertInfo() {
		try {
			String proxy = ConfigManager.getStringConfigByKey(ConfigName.ProxyForGetCert);
			if (proxy != null && !proxy.isEmpty() && proxy.contains(":")) {
				String[] parts = proxy.split(":");
				if (parts.length==2) {
					CertInfo.proxyHost = parts[0];
					CertInfo.proxyPort = Integer.parseInt(parts[1]);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 核心方法
	 * @param url
	 * @param proxyHost
	 * @param proxyPort
	 * @return
	 * @throws Exception
	 */
	private static Certificate[] getCertificates(String url, String proxyHost, Integer proxyPort) throws Exception {
		// 全局忽略主机验证
		HostnameVerifier allHostsValid = (hostname, session) -> true;

		HttpsURLConnection conn = null;
		try {
			// 初始化 SSL 上下文
			TrustManager[] tm = {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			}};
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, tm, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

			URL urlObject = new URL(url);
			
            // 如果代理相关参数为空，则不设置代理
            if (proxyHost != null && proxyPort != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                conn = (HttpsURLConnection) urlObject.openConnection(proxy);
            } else {
                conn = (HttpsURLConnection) urlObject.openConnection();
            }
			conn.connect();

			// 获取证书信息
			return conn.getServerCertificates();//Certificate[]
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}




	/**
	 * 	get all SANs ---证书中所有的域名信息
	 * @param url
	 * @return
	 */
	public Set<String> getAlternativeDomains(String url){
		try {
			if (url.toLowerCase().startsWith("https://")){
				Certificate[] certs = getCertificates(url,proxyHost,proxyPort);
				Set<String> domains = getAlternativeDomains(certs);
				return domains;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new HashSet<String>(); 
	}

	public static Set<String> getAlternativeDomains(Certificate[] certs) throws Exception {
		Set<String> tmpSet = new HashSet<String>();
		if (certs ==null) {return tmpSet;}
		for (Certificate cert:certs) {
			if(cert instanceof X509Certificate) {
				X509Certificate cer = (X509Certificate ) cert;
				try {
					Collection<List<?>> alterDomains = cer.getSubjectAlternativeNames();//其他可变主体。
					if (alterDomains!=null) {
						Iterator<List<?>> item = alterDomains.iterator();
						while (item.hasNext()) {
							List<?> domainList =  item.next();
							if(domainList.get(1).toString().startsWith("*."))
							{	
								String relateddomain = domainList.get(1).toString().replace("*.","");
								tmpSet.add(relateddomain);
							}
							else {
								tmpSet.add(domainList.get(1).toString());
							}
						}
					}
				} catch (CertificateParsingException e) {
					throw e;
				}
			}
		}
		return tmpSet;
	}

	private static long getDateRange(Certificate[] certs) {
		if (certs ==null) {return -1;}
		for (Certificate cert:certs) {
			if(cert instanceof X509Certificate) {
				X509Certificate cer = (X509Certificate ) cert;
				try {
					long day = cer.getNotAfter().getTime();
					return day;
				} catch (Exception e) {
					throw e;
				}
			}
		}
		return -1;
	}

	private static String getCertIssuer(Certificate[] certs) {
		if (certs == null) return null;
		StringBuffer result = new StringBuffer();
		for (Certificate cert:certs) {
			if(cert instanceof X509Certificate) {
				X509Certificate cer = (X509Certificate ) cert;
				System.out.println(cer.getIssuerDN());
				//System.out.println(cer.getIssuerUniqueID());
				//System.out.println(cer.getIssuerX500Principal());
				result.append(cer.getIssuerDN());
			}
		}
		return result.toString();
	}
	public static String getCertIssuer(String url) {
		try {
			if (url.startsWith("https://")){
				Certificate[] certs = getCertificates(url,proxyHost,proxyPort);
				String info = getCertIssuer(certs);
				String org = info.split(",")[1];
				org = org.replaceFirst("O=", "");
				return org;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getCertTime(String url) {
		try {
			if (url.startsWith("https://")){
				Certificate[] certs = getCertificates(url, proxyHost, proxyPort);
				long outtime = getDateRange(certs);//过期时间
				return Commons.TimeToString(outtime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据证书主体来获取所有证书域名。证书主体域名必须包含关键词，否则任务是CDN证书，不获取其中的域名
	 */
	public static Set<String> getSANsbyKeyword(String aURL,Set<String> domainKeywords){//only when domain key word in the Principal,return SANs
		try {
			Certificate[] certs = getCertificates(aURL, proxyHost, proxyPort);
			for (Certificate cert : certs) {
				//System.out.println("Certificate is: " + cert);
				if(cert instanceof X509Certificate) {
					X509Certificate cer = (X509Certificate ) cert;
					//System.out.println("xxxxx"+cer.getSubjectDN().getName()+"xxxxxxxxxx\r\n\r\n");
					//System.out.println("xxxxx"+cer.getSubjectX500Principal().getName()+"xxxxxxxxxx\r\n\r\n");
					//System.out.println(x.getSubjectAlternativeNames()+"\r\n\r\n");

					//Iterator item = x.getSubjectAlternativeNames().iterator();
					//java.lang.NullPointerException. why??? need to confirm collection is not null

					String Principal = cer.getSubjectX500Principal().getName();//证书主体域名
					for (String domainKeyword:domainKeywords) {
						if (Principal.toLowerCase().contains(domainKeyword)) {
							//this may lead to miss some related domains, eg. https://www.YouTube.com ,it's principal is *.google.com
							//but our target is to get useful message, so we need to do this to void CDN provider,I think it's worth~, or any good idea?
							return getAlternativeDomains(certs);
						}
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return new HashSet<String>();
	}

	public static void test1() {
		Set<String> set = new HashSet<>();
		set.add("jd");
		set.add("taobao");
		try {
			//certInformation("https://jd.hk");
			//System.out.println(getSANs("https://202.77.129.10","jd"));
			//			System.out.println(getSANsbyKeyword("https://m.hemaos.com/",set));
			//			System.out.println(getSANsbyKeyword("https://browser.taobao.com/",set));
			//System.out.println(getSANs("https://open.163.com","163.com"));
			//System.out.println(getSANs("https://open.163.com"));
			//			System.out.println(isTarget("https://111.202.65.185/",set));
			//			System.out.println(isTarget("https://www.baidu.com/",set));
			//			System.out.println(isTarget("https://120.52.148.166/",set));

			System.out.println(getCertTime("https://www.baidu.com/"));


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void test2() {
		System.out.println("aaaa".contains(""));
	}

	public static void test3() {
		String url = "https://google.com";
		String proxyHost = "127.0.0.1";
		int proxyPort = 8080;

		try {
			Certificate[] certs = getCertificates(url, proxyHost, proxyPort);
			for (Certificate cert : certs) {
				System.out.println(cert);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		test3();
	}
}
