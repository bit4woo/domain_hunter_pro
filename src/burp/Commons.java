package burp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Commons {
	
	public static String set2string(Set set){
	    Iterator iter = set.iterator();
	    String result = "";
	    while(iter.hasNext())
	    {
	        //System.out.println(iter.next());  	
	    	result +=iter.next();
	    	result +="\n";
	    }
	    return result;
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
}
