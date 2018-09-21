package burp;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
    

	public static Set<String> getSANs(String aURL,String domainKeyword) throws Exception{//only when domain key word in the Principal,return SANs
	    HostnameVerifier allHostsValid = new HostnameVerifier() {
	        public boolean verify(String hostname, SSLSession session) {
	            return true;
	        }
	    };
	    
        Set<String> tmpSet = new HashSet<String>();
	    
        TrustManager[] tm = new TrustManager[]{myX509TrustManager};
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");    
        sslContext.init(null, tm, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);//do not check certification
        
        HttpsURLConnection conn = null;
        try {
        	URL destinationURL = new URL(aURL);
            conn = (HttpsURLConnection) destinationURL.openConnection();
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            conn.disconnect();
            for (Certificate cert : certs) {
                //System.out.println("Certificate is: " + cert);
                if(cert instanceof X509Certificate) {
                    X509Certificate cer = (X509Certificate ) cert;
                    //System.out.println("xxxxx"+cer.getSubjectDN().getName()+"xxxxxxxxxx\r\n\r\n");
                    //System.out.println("xxxxx"+cer.getSubjectX500Principal().getName()+"xxxxxxxxxx\r\n\r\n");
                    //System.out.println(x.getSubjectAlternativeNames()+"\r\n\r\n");
                    
                    //Iterator item = x.getSubjectAlternativeNames().iterator();
                    //java.lang.NullPointerException. why??? need to confirm collection is not null
                    
                    String Principal = cer.getSubjectX500Principal().getName();
                    if (Principal.toLowerCase().contains(domainKeyword)) {
                    	//this may lead to miss some related domains, eg. https://www.YouTube.com ,it's principal is *.google.com
                    	//but our target is to get useful message, so we need to do this to void CDN provider,I think it's worth~, or any good idea?
                        Collection<List<?>> alterDomains = cer.getSubjectAlternativeNames();
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
                            //System.out.println(tmpSet);
                        }
                    }

                }
            }
        } catch (Exception e) {
        	throw e;
        }finally {
        	if (conn!=null) {
        		conn.disconnect();
        	}
        }
        return tmpSet;
    }
	
	
	
	public static Set<String> getSANs(String aURL) throws Exception{//get all SANs
	    HostnameVerifier allHostsValid = new HostnameVerifier() {
	        public boolean verify(String hostname, SSLSession session) {
	            return true;
	        }
	    };
	    
        Set<String> tmpSet = new HashSet<String>();
	    
        TrustManager[] tm = new TrustManager[]{myX509TrustManager};
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");    
        sslContext.init(null, tm, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);//do not check certification
        
        HttpsURLConnection conn = null;
        try {
        	URL destinationURL = new URL(aURL);
            conn = (HttpsURLConnection) destinationURL.openConnection();
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            conn.disconnect();
	        for (Certificate cert : certs) {
	            if(cert instanceof X509Certificate) {
	                X509Certificate cer = (X509Certificate ) cert;
	                
	                Collection<List<?>> alterDomains = cer.getSubjectAlternativeNames();
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
	            }
	        }
        } catch (Exception e) {
        	throw e;
        }finally {
        	if (conn!=null) {
        		conn.disconnect();
        	}
        }
        return tmpSet;
    }
	
	
	public static void main(String[] args) {
		try {
			//certInformation("https://jd.hk");
			System.out.println(getSANs("https://202.77.129.10","jd"));
			//System.out.println(getSANs("https://open.163.com","163.com"));
			//System.out.println(getSANs("https://open.163.com"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
