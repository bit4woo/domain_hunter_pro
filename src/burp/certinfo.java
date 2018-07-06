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
import java.util.TreeSet;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class certinfo {
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
    

	public static Set<String> getSANs(String aURL) throws Exception{
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
        
        URL destinationURL = new URL(aURL);
        HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
        conn.connect();
        Certificate[] certs = conn.getServerCertificates();
        for (Certificate cert : certs) {
            //System.out.println("Certificate is: " + cert);
            if(cert instanceof X509Certificate) {
                X509Certificate x = (X509Certificate ) cert;
                //System.out.println(x.getSubjectDN()+"\r\n\r\n");
                //System.out.println(x.getSubjectAlternativeNames()+"\r\n\r\n");
                
                //Iterator item = x.getSubjectAlternativeNames().iterator();
                //java.lang.NullPointerException. why??? need to confirm collection is not null
                
                Collection<List<?>> xx = x.getSubjectAlternativeNames();
                if (xx!=null) {
                	Iterator<List<?>> item = xx.iterator();
                    while (item.hasNext()) {
                    	List<?> domainList =  item.next();
                    	if(domainList.get(1).toString().startsWith("*."))
                    	{	
                    		String domain = domainList.get(1).toString().replace("*.","");
                    		tmpSet.add(domain);
                    	}
                    	else {
                    		tmpSet.add(domainList.get(1).toString());
                    	}
                    }
                    //System.out.println(tmpSet);
                }
            }
        }
        return tmpSet;
    }
	public static void main(String[] args) {
		try {
			//certInformation("https://jd.hk");
			System.out.print(getSANs("https://202.77.129.30"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
