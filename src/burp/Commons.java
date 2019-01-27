package burp;

import java.awt.Window.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;
import org.xbill.DNS.ARecord;
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



	public static boolean isValidIP (String ip) {
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

	//http://www.xbill.org/dnsjava/dnsjava-current/examples.html
	public static HashMap<String,Set<String>> dnsquery(String domain) {
		HashMap<String,Set<String>> result = new HashMap<String,Set<String>>();
		try{
			Lookup lookup = new Lookup(domain, org.xbill.DNS.Type.A);
			lookup.run();

			Set<String> IPset = new HashSet<String>();
			Set<String> CDNSet = new HashSet<String>();
			if(lookup.getResult() == Lookup.SUCCESSFUL){
				Record[] records=lookup.getAnswers();
				for (int i = 0; i < records.length; i++) {
					ARecord a = (ARecord) records[i];
					String ip = a.getAddress().getHostAddress();
					String CName = a.getAddress().getHostName();
					if (ip!=null) {
						IPset.add(ip);
					}
					if (CName!=null) {
						CDNSet.add(CName);
					}
					//					System.out.println("getAddress "+ a.getAddress().getHostAddress());
					//					System.out.println("getAddress "+ a.getAddress().getHostName());
					//					System.out.println("getName "+ a.getName());
					//					System.out.println(a);
				}
				result.put("IP", IPset);
				result.put("CDN", CDNSet);
				//System.out.println(records);
			}
			return result;

		}catch(Exception e){
			e.printStackTrace();
			return result;
		}
	}


	public static Set<SubnetUtils> toSubNets(Set<String> IPSet) {
		Set<SubnetUtils> subNets= new HashSet<SubnetUtils>();
		for (String ip:IPSet) {
			String subnet = ip.trim()+"/24";

			SubnetUtils utils = new SubnetUtils(subnet);
			subNets.add(utils);
		}
		return subNets;
	}

	public static Set<SubnetUtils> toSmallSubNets(Set<String> IPSet) {
		Set<SubnetUtils> subNets= new HashSet<SubnetUtils>();
		Set<SubnetUtils> smallSubNets= new HashSet<SubnetUtils>();
		for (String ip:IPSet) {
			String subnet = ip.trim()+"/24";

			SubnetUtils utils = new SubnetUtils(subnet);
			subNets.add(utils);
		}
		for(SubnetUtils CNet:subNets) {
			Set<String> tmpIPSet = new HashSet<String>();
			for (String ip:IPSet) {
				if (CNet.getInfo().isInRange(ip)){
					tmpIPSet.add(ip);
				}
			}
			smallSubNets.add(SmallerNetwork(tmpIPSet));
		}
		return smallSubNets;
	}
	
	public static SubnetUtils SmallerNetwork(Set<String> IPset) {
		for (int mask=24;mask<=32;mask++) {
			SubnetUtils tmpNet = new SubnetUtils((String)IPset.toArray()[0]+"/"+mask);
			for(String ip:IPset) {
				if (tmpNet.getInfo().isInRange(ip)) {
					continue;
				} else {
					return new SubnetUtils((String)IPset.toArray()[0]+"/"+(mask-1));
				}
			}
			continue;
		}
		return new SubnetUtils((String)IPset.toArray()[0]+"/32");
	}

	public static Set<String> subNetsToIPSet (Set<SubnetUtils> subNets) {
		Set<String> IPSet = new HashSet<String>();
		for (SubnetUtils net:subNets) {
			String[] ips = net.getInfo().getAllAddresses();
			IPSet.addAll(Arrays.asList(ips));
		}
		return IPSet;
	}

	public static void main(String args[]) {
		/*
		 * HashMap<String, Set<String>> result = dnsquery("www.baidu.com");
		 * System.out.print(result.get("IP").toString());
		 * System.out.print(dnsquery("www.baidu.com"));
		 */
		System.out.print(new SubnetUtils("192.168.1.1/23").getInfo().getCidrSignature());
		
		Set<String> IPSet = new HashSet<String>();
		IPSet.add("192.168.1.225");
		IPSet.add("192.168.1.224");
		
		System.out.print(toSubNets(IPSet));
		System.out.print(toSmallSubNets(IPSet));
		
	}
}
