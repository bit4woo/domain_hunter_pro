package burp;

import java.awt.Window.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;

public class Commons {
	
	public static String set2string(Set<?> set){
	    Iterator iter = set.iterator();
	    StringBuilder result = new StringBuilder();
	    while(iter.hasNext())
	    {
	        //System.out.println(iter.next());  	
	    	result.append(iter.next()).append("\n");
	    }
	    return result.toString();
	}
	
	public static boolean isResponseNull(IHttpRequestResponse message){
		try {
			int x = message.getResponse().length;
			return false;
		}catch(Exception e){
			//stdout.println(e);
			return true;
		}
	}
	
	public static boolean uselessExtension(String urlpath) {
		Set<String> extendset = new HashSet<String>();
		extendset.add(".gif");
		extendset.add(".jpg");
		extendset.add(".png");
		extendset.add(".css");
		Iterator<String> iter = extendset.iterator();
		while (iter.hasNext()) {
			if(urlpath.endsWith(iter.next().toString())) {//if no next(), this loop will not break out
				return true;
			}
		}
		return false;
	}
	
	

	public static boolean validIP (String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	
	public void dnsquery(String domain) {
		try{
			Record [] records =null;
			Lookup lookup = new Lookup("csdn.com", org.xbill.DNS.Type.A);
			lookup.run();
			
			if(lookup.getResult() == Lookup.SUCCESSFUL){
				records=lookup.getAnswers();
			}else{
				System.out.println("未查询到结果!");
				return;
			}
			for (int i = 0; i < records.length; i++) {
				MXRecord mx = (MXRecord) records[i];
				System.out.println("Host " + mx.getTarget() + " has preference "+ mx.getPriority());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
