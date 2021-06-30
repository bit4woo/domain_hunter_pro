package domain;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import burp.Commons;

public class CertInfo {
	private static TrustManager myX509TrustManager = new X509TrustManager() { 

		@Override 
		public X509Certificate[] getAcceptedIssuers() { 
			return null; 
		} 

		@Override 
		public void checkServerTrusted(X509Certificate[] chain, String authType) 
				throws CertificateException { 
		} 

		@Override 
		public void checkClientTrusted(X509Certificate[] chain, String authType) 
				throws CertificateException { 
		}

	};


	public static Certificate[] getCerts(String aURL) throws Exception {
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		HttpsURLConnection conn = null;
		try {
			TrustManager[] tm = new TrustManager[]{myX509TrustManager};
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");    
			sslContext.init(null, tm, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);//do not check certification

			URL destinationURL = new URL(aURL);
			conn = (HttpsURLConnection) destinationURL.openConnection();
			conn.connect();
			Certificate[] certs = conn.getServerCertificates();
			return certs;
		}catch (Exception e) {
			throw e;
		}finally {
			if (conn!=null) {
				conn.disconnect();
			}
		}
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

	public static String getCertTime(String url) {
		try {
			if (url.startsWith("https://")){
				Certificate[] certs = getCerts(url);
				long outtime = getDateRange(certs);//过期时间
				return Commons.TimeToString(outtime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 根据证书主体来获取所有证书域名。证书主体域名必须包含关键词，否则任务是CDN证书，不获取其中的域名
	 */
	public static Set<String> getSANsbyKeyword(String aURL,Set<String> domainKeywords){//only when domain key word in the Principal,return SANs
		try {
			Certificate[] certs = getCerts(aURL);
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


	//get all SANs ---证书中所有的域名信息
	public static Set<String> getAllSANs(String aURL) throws Exception{
		Certificate[] certs = getCerts(aURL);
		return getAlternativeDomains(certs);
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

	public static void main(String[] args) {
		test2();
	}
}
