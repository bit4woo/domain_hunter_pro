package burp;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
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
//				result.put("IP", IPset);
//				result.put("CDN", CDNSet);
				//System.out.println(records);
			}
			result.put("IP", IPset);
			result.put("CDN", CDNSet);
			return result;

		}catch(Exception e){
			e.printStackTrace();
			return result;
		}
	}



	//////////////////////////////////////////IP  subnet  CIDR/////////////////////////////////
	/*
	To Class C Network
	 */
	private static Set<String> toClassCSubNets(Set<String> IPSet) {
		Set<String> subNets= new HashSet<String>();
		Set<String> smallSubNets= new HashSet<String>();
		for (String ip:IPSet) {
			String subnet = ip.trim().substring(0,ip.lastIndexOf("."))+".0/24";
			subNets.add(subnet);
		}
		return subNets;
	}

	/*
	 * IP集合，转多个CIDR,smaller newtworks than Class C Networks
	 */
	public static Set<String> toSmallerSubNets(Set<String> IPSet) {
		Set<String> subNets= toClassCSubNets(IPSet);
		Set<String> smallSubNets= new HashSet<String>();

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
	To get a smaller network with a set of IP addresses
	 */
	private static String ipset2cidr(Set<String> IPSet) {
		if (IPSet == null || IPSet.size() <=0){
			return null;
		}
		if (IPSet.size() ==1){
			return IPSet.toArray(new String[0])[0];
		}
		Set<Long> tmp = new HashSet<Long>();
		List<String> list = new ArrayList<String>(IPSet);
		SubnetUtils oldsamllerNetwork =new SubnetUtils(list.get(0).trim()+"/24");
		for (int mask=24;mask<=32;mask++){
			//System.out.println(mask);
			SubnetUtils samllerNetwork = new SubnetUtils(list.get(0).trim()+"/"+mask);
			for (String ip:IPSet) {
				if (samllerNetwork.getInfo().isInRange(ip) || samllerNetwork.getInfo().getBroadcastAddress().equals(ip.trim()) || samllerNetwork.getInfo().getNetworkAddress().equals(ip.trim())){
					//52.74.179.0 ---sometimes .0 address is a real address.
					continue;
				}
				else {
					String networkaddress = oldsamllerNetwork.getInfo().getNetworkAddress();
					String tmpmask = oldsamllerNetwork.getInfo().getNetmask();
					return new SubnetUtils(networkaddress,tmpmask).getInfo().getCidrSignature();
				}
			}
			oldsamllerNetwork = samllerNetwork;
		}
		return null;
	}


	/*
	 * 多个网段转IP集合，变更表现形式，变成一个个的IP
	 */
	public static Set<String> toIPSet (Set<String> subNets) {
		Set<String> IPSet = new HashSet<String>();
		for (String subnet:subNets) {
			if (subnet.contains(":")) {
				continue;//暂时先不处理IPv6,需要研究一下
				//TODO
			}
			if (subnet.contains("/")){
				SubnetUtils net = new SubnetUtils(subnet);
				SubnetInfo xx = net.getInfo();
				String[] ips = xx.getAllAddresses();
				Set<String> resultIPs = new HashSet<>(Arrays.asList(ips));
				resultIPs.add(xx.getNetworkAddress());
				resultIPs.add(xx.getBroadcastAddress());
				IPSet.addAll(resultIPs);
			}else { //单IP
				IPSet.add(subnet);
			}

		}
		return IPSet;
	}
	
	public static String getNowTimeString() {
		SimpleDateFormat simpleDateFormat = 
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		return simpleDateFormat.format(new Date());
	}


	public static void browserOpen(Object url,String browser) throws Exception{
		String urlString = null;
		URI uri = null;
		if (url instanceof String) {
			urlString = (String) url;
			uri = new URI((String)url);
		}else if (url instanceof URL) {
			uri = ((URL)url).toURI();
			urlString = url.toString();
		}
		if(browser == null ||browser.equalsIgnoreCase("default") || browser.equalsIgnoreCase("")) {
			//whether null must be the first
			Desktop desktop = Desktop.getDesktop();
			if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
				desktop.browse(uri);
			}
		}else {
			Runtime runtime = Runtime.getRuntime();
			runtime.exec(browser+" "+urlString);
			//C:\Program Files\Mozilla Firefox\firefox.exe
			//C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe
		}
	}
	
	
	public static byte[] buildCookieRequest(IExtensionHelpers helpers,String cookie, byte[] request) {
		if (cookie != null && !cookie.equals("")){
			if (!cookie.startsWith("Cookie: ")){
				cookie = "Cookie: "+cookie;
			}
			List<String > newHeader = helpers.analyzeRequest(request).getHeaders();
			int bodyOffset = helpers.analyzeRequest(request).getBodyOffset();
			byte[] byte_body = Arrays.copyOfRange(request, bodyOffset, request.length);
			newHeader.add(cookie);
			request = helpers.buildHttpMessage(newHeader,byte_body);
		}
		return request;
	}

	public static void main(String args[]) {
		
//		HashMap<String, Set<String>> result = dnsquery("www.baidu.com");
//		System.out.println(result.get("IP").toString());
		System.out.println(dnsquery("www.baidu111.com"));
		 
//		//System.out.println(new SubnetUtils("192.168.1.1/23").getInfo().getCidrSignature());
//		
//		Set<String> IPSet = new HashSet<String>();
//		IPSet.add("192.168.1.225");
///*		IPSet.add("192.168.1.128");
//		IPSet.add("192.168.1.129");
//		IPSet.add("192.168.1.155");
//		IPSet.add("192.168.1.224");
//		IPSet.add("192.168.1.130");*/
//		Set<String> subnets = toSmallerSubNets(IPSet);
//
//		System.out.println(toIPSet(subnets));
//		
//		Set<String>  a= new HashSet();
//		a.add("218.213.102.6/31");
//		System.out.println(toIPSet(a));
		Set<String> subnets = new HashSet<String>();
		subnets.add("2402:db40:1::/48");
		System.out.print(toIPSet(subnets));
	}
}
