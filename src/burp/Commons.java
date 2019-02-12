package burp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	
	/*
	 * IP集合，转多个CIDR
	 */
	public static Set<String> toSubNets(Set<String> IPSet) {
		Set<String> subNets= new HashSet<String>();
		Set<String> smallSubNets= new HashSet<String>();
		for (String ip:IPSet) {
			String subnet = ip.trim().substring(0,ip.lastIndexOf("."))+".0/24";
			subNets.add(subnet);
		}
		for(String CNet:subNets) {//把所有IP按照C段进行分类
			SubnetUtils net = new SubnetUtils(CNet);
			Set<String> tmpIPSet = new HashSet<String>();
			for (String ip:IPSet) {
				if (net.getInfo().isInRange(ip) || net.getInfo().getBroadcastAddress().equals(ip.trim()) || net.getInfo().getNetworkAddress().equals(ip.trim())){
					//52.74.179.0 ---sometimes .0 address is a real address.
					tmpIPSet.add(ip);
				}
			}//每个tmpIPSet就是一个C段的IP集合
			smallSubNets.add(ipset2cidr(tmpIPSet));//把一个C段中的多个IP计算出其CIDR，即更小的网段
		}
		return smallSubNets;
	}

	/*
	 * 多个网段转IP集合，变更表现形式，变成一个个的IP
	 */
	public static Set<String> toIPSet (Set<String> subNets) {
		Set<String> IPSet = new HashSet<String>();
		for (String subnet:subNets) {
			SubnetUtils net = new SubnetUtils(subnet);
			String[] ips = net.getInfo().getAllAddresses();
			IPSet.addAll(Arrays.asList(ips));
		}
		return IPSet;
	}
	
	/*
	 * 将一个IP集合转为CIDR格式
	 */
	private static String ipset2cidr(Set<String> IPSet) {
		Set<Long> tmp = new HashSet<Long>();
		for (String ip:IPSet) {
			long ipLong = ip2long(ip);
			tmp.add(ipLong);
		}
		long max = (long) Collections.max(tmp);
		long min = (long) Collections.min(tmp);
		return iprange2cidr(long2ip(min),long2ip(max));
	}

	
	private static String iprange2cidr( String startIp, String endIp ) {
        // check parameters
        if (startIp == null || startIp.length() < 8 ||
            endIp == null || endIp.length() < 8) return null;
        long start = ip2long(startIp);
        long end = ip2long(endIp);
	    return iprange2cidr(start,end);
	}
	
	
	
	//https://stackoverflow.com/questions/33443914/how-to-convert-ip-address-range-to-cidr-in-java
	private static String iprange2cidr( long start, long end ) {
        // check parameters
        if (start > end) return null;

        List<String> result = new ArrayList<String>();
        while (start <= end) {
            // identify the location of first 1's from lower bit to higher bit of start IP
            // e.g. 00000001.00000001.00000001.01101100, return 4 (100)
            long locOfFirstOne = start & (-start);
            int maxMask = 32 - (int) (Math.log(locOfFirstOne) / Math.log(2));
            String ip = long2ip(start);
            return (ip + "/" + maxMask);
/*            // calculate how many IP addresses between the start and end
            // e.g. between 1.1.1.111 and 1.1.1.120, there are 10 IP address
            // 3 bits to represent 8 IPs, from 1.1.1.112 to 1.1.1.119 (119 - 112 + 1 = 8)
            double curRange = Math.log(end - start + 1) / Math.log(2);
            int maxDiff = 32 - (int) Math.floor(curRange);

            // why max?
            // if the maxDiff is larger than maxMask
            // which means the numbers of IPs from start to end is smaller than mask range
            // so we can't use as many as bits we want to mask the start IP to avoid exceed the end IP
            // Otherwise, if maxDiff is smaller than maxMask, which means number of IPs is larger than mask range
            // in this case we can use maxMask to mask as many as IPs from start we want.
            maxMask = Math.max(maxDiff, maxMask);

            // Add to results
            String ip = long2ip(start);
            result.add(ip + "/" + maxMask);
            // We have already included 2^(32 - maxMask) numbers of IP into result
            // So the next round start must add that number
            start += Math.pow(2, (32 - maxMask));*/
        }
        return null;
    }
	 
	private static long ip2long(String ipstring) {
	    String[] ipAddressInArray = ipstring.split("\\.");
	    long num = 0;
	    long ip = 0;
	    for (int x = 3; x >= 0; x--) {
	        ip = Long.parseLong(ipAddressInArray[3 - x]);
	        num |= ip << (x << 3);
	    }
	    return num;
	}
	 
	private static String long2ip(long longIP) {
	    StringBuffer sbIP = new StringBuffer("");
	    sbIP.append(String.valueOf(longIP >>> 24));
	    sbIP.append(".");
	    sbIP.append(String.valueOf((longIP & 0x00FFFFFF) >>> 16));
	    sbIP.append(".");
	    sbIP.append(String.valueOf((longIP & 0x0000FFFF) >>> 8));
	    sbIP.append(".");
	    sbIP.append(String.valueOf(longIP & 0x000000FF));
	 
	    return sbIP.toString();
	}

	public static void main(String args[]) {
		/*
		 * HashMap<String, Set<String>> result = dnsquery("www.baidu.com");
		 * System.out.print(result.get("IP").toString());
		 * System.out.print(dnsquery("www.baidu.com"));
		 */
		//System.out.print(new SubnetUtils("192.168.1.1/23").getInfo().getCidrSignature());
		
		Set<String> IPSet = new HashSet<String>();
		IPSet.add("192.168.1.225");
/*		IPSet.add("192.168.1.128");
		IPSet.add("192.168.1.129");
		IPSet.add("192.168.1.155");*/
		IPSet.add("192.168.1.224");
		String subnets = ipset2cidr(IPSet);
		System.out.println(subnets);
	}
}
