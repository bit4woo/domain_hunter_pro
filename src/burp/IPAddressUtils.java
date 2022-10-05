package burp;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

import java.util.*;

public class IPAddressUtils {

	public static boolean isPrivateIPv4(String ipAddress) {
		try {
			String[] ipAddressArray = ipAddress.split("\\.");
			int[] ipParts = new int[ipAddressArray.length];
			for (int i = 0; i < ipAddressArray.length; i++) {
				ipParts[i] = Integer.parseInt(ipAddressArray[i].trim());
			}

			switch (ipParts[0]) {
				case 10:
				case 127:
					return true;
				case 172:
					return (ipParts[1] >= 16) && (ipParts[1] < 32);
				case 192:
					return (ipParts[1] == 168);
				case 169:
					return (ipParts[1] == 254);
			}
		} catch (Exception ex) {
		}

		return false;
	}

	public static boolean isPrivateIPv6(String ipAddress) {
		boolean isPrivateIPv6 = false;
		String[] ipParts = ipAddress.trim().split(":");
		if (ipParts.length > 0) {
			String firstBlock = ipParts[0];
			String prefix = firstBlock.substring(0, 2);

			if (firstBlock.equalsIgnoreCase("fe80")
					|| firstBlock.equalsIgnoreCase("100")
					|| ((prefix.equalsIgnoreCase("fc") && firstBlock.length() >= 4))
					|| ((prefix.equalsIgnoreCase("fd") && firstBlock.length() >= 4))) {
				isPrivateIPv6 = true;
			}
		}
		return isPrivateIPv6;
	}

	/**
	 * 校验字符串是否是一个合格的IP地址
	 * 会发现如下类型的IP，是有效的IP地址，但是实际情况却不会有人这么写。
	 * 应当从我们的正则中剔除
	 * PING 181.002.245.007 (181.2.245.7): 56 data bytes
	 * @param ip
	 * @return
	 */
	public static boolean isValidIP (String ip) {
		try {
			if ( ip == null || ip.isEmpty() ) {
				return false;
			}
			if (ip.contains(":")) {
				String[] ipAndPort = ip.split(":");
				if (ipAndPort.length !=2) {
					return false;
				}
				ip = ipAndPort[0];
				int port = Integer.parseInt(ipAndPort[1]);
				if ( port < 0 || port > 65535 ) {
					return false;
				}
			}

			String[] parts = ip.split( "\\." );
			if ( parts.length != 4 ) {
				return false;
			}

			for ( String s : parts ) {
				if (s.startsWith("0") && s.length() > 1 ) {
					return false;
				}
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

	public static boolean isValidSubnet(String subnet) {
		if (null == subnet) return false;
		subnet = subnet.replaceAll(" ", "");
		if (subnet.contains("/")) {
			String[] parts = subnet.split("/");
			if (parts.length ==2) {
				String ippart = parts[0];
				int num = Integer.parseInt(parts[1]);
				if (isValidIP(ippart) && num>1 && num < 32) {
					return true;
				}
			}
		}
		if (subnet.contains("-")) {//这里的方法不完整
			String[] parts = subnet.split("-");
			if (parts.length ==2) {
				String ippart1 = parts[0];
				String ippart2 = parts[1];
				if (isValidIP(ippart1) && isValidIP(ippart2)) {
					return true;
				}
			}
		}
		return false;
	}


	public static Set<String> toClassCSubNets(Set<String> IPSet) {
		Set<String> subNets= new HashSet<String>();
		for (String ip:IPSet) {
			ip = ipClean(ip);
			if (isValidIP(ip)) {
				String subnet = ip.substring(0,ip.lastIndexOf("."))+".0/24";
				subNets.add(subnet);
			}else if(isValidSubnet(ip)) {//这里的IP也可能是网段，不要被参数名称限定了
				subNets.add(ip);
			}
		}
		return subNets;
	}

	/*
	 * IP集合，转多个CIDR,smaller newtworks than Class C Networks
	 */
	public static Set<String> toSmallerSubNets(Set<String> IPSet) {
		Set<String> subNets= toClassCSubNets(IPSet);
		Set<String> smallSubNets= new HashSet<String>();

		try {
			for(String CNet:subNets) {//把所有IP按照C段进行分类
				SubnetUtils net = new SubnetUtils(CNet);
				Set<String> tmpIPSet = new HashSet<String>();
				for (String ip:IPSet) {
					ip = ipClean(ip);
					if (isValidSubnet(ip)) {//这里的IP也可能是网段，不要被参数名称限定了
						smallSubNets.add(ip);
						continue;
					}
					if (!isValidIP(ip)) {
						System.out.println(ip+" invalid IP address, skip to handle it!");
						continue;
					}

					if (net.getInfo().isInRange(ip) || net.getInfo().getBroadcastAddress().equals(ip.trim()) || net.getInfo().getNetworkAddress().equals(ip.trim())){
						//52.74.179.0 ---sometimes .0 address is a real address.
						tmpIPSet.add(ip);
					}
				}//每个tmpIPSet就是一个C段的IP集合
				String tmpSmallNet = ipset2cidr(tmpIPSet);
				if (tmpSmallNet != null && !tmpSmallNet.equals("")){
					smallSubNets.add(tmpSmallNet);//把一个C段中的多个IP计算出其CIDR，即更小的网段
				}
			}
			return smallSubNets;
		}catch(Exception e) {
			throw e;
		}

	}
	/*
	To get a smaller network with a set of IP addresses
	 */
	private static String ipset2cidr(Set<String> IPSet) {
		try {
			if (IPSet == null || IPSet.size() <=0){
				return null;
			}
			if (IPSet.size() ==1){
				return IPSet.toArray(new String[0])[0];
			}
			List<String> list = new ArrayList<String>(IPSet);
			SubnetUtils oldsamllerNetwork =new SubnetUtils(list.get(0).trim()+"/24");
			for (int mask=24;mask<=32;mask++){
				//System.out.println(mask);
				SubnetUtils samllerNetwork = new SubnetUtils(list.get(0).trim()+"/"+mask);
				for (String ip:IPSet) {
					ip = ipClean(ip);
					if (!isValidIP(ip)) {
						System.out.println(ip + "invalid IP address, skip to handle it!");
						continue;
					}
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
		}catch (Exception e) {
			throw e;
		}
	}

	public static String ipClean(String ip){
		ip = ip.trim();
		if (ip.endsWith(".")){
			ip = ip.substring(0,ip.lastIndexOf("."));
		}
		if (ip.contains(":")) {
			ip = ip.substring(0,ip.lastIndexOf(":"));
		}
		return ip;
	}

	/*
	 * 多个网段转IP集合，变更表现形式，变成一个个的IP
	 */
	public static Set<String> toIPSet (Set<String> subNets) {
		Set<String> IPSet = new HashSet<String>();
		List<String> result = toIPList(new ArrayList<>(subNets));
		IPSet.addAll(result);
		return IPSet;
	}

	public static List<String> toIPList (String subnet) {
		List<String> IPList = new ArrayList<String>();
		try {
			if (subnet.contains(":")) {
				return IPList;//暂时先不处理IPv6,需要研究一下
				//TODO
			}
			if (subnet.contains("/")){
				SubnetUtils net = new SubnetUtils(subnet);
				SubnetInfo xx = net.getInfo();
				String[] ips = xx.getAllAddresses();
				IPList.add(xx.getNetworkAddress());//.0
				IPList.addAll(Arrays.asList(ips));
				IPList.add(xx.getBroadcastAddress());//.255
			}else if (subnet.contains("-")) {//178.170.186.0-178.170.186.255
				String[] ips = subnet.split("-");
				if (ips.length ==2) {
					String startip = ips[0].trim();
					String endip = ips[1].trim();
					//System.out.println(startip);
					//System.out.println(endip);
					IPAddressString string1 = new IPAddressString(startip);
					IPAddressString string2 = new IPAddressString(endip);
					IPAddress addr1 = string1.getAddress();
					IPAddress addr2 = string2.getAddress();
					IPAddressSeqRange range = addr1.toSequentialRange(addr2);
					Iterator<? extends IPAddress> it = range.iterator();
					while (it.hasNext()){
						IPAddress item = it.next();
						//System.out.println(item.toString());
						IPList.add(item.toString());
					}
				}
			}else { //单IP
				IPList.add(subnet);
			}
		}catch(Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
		return IPList;
	}

	public static List<String> toIPList (List<String> subNets) {
		List<String> IPList = new ArrayList<String>();
		for (String subnet:subNets) {
			IPList.addAll(toIPList(subnet));
		}
		return IPList;
	}

	public static boolean checkIPIsInGivenRange (String inputIP, String rangeStartIP, String rangeEndIP)
			throws AddressStringException {
		IPAddress startIPAddress = new IPAddressString(rangeStartIP).getAddress();
		IPAddress endIPAddress = new IPAddressString(rangeEndIP).getAddress();
		IPAddressSeqRange ipRange = startIPAddress.toSequentialRange(endIPAddress);
		IPAddress inputIPAddress = new IPAddressString(inputIP).toAddress();

		return ipRange.contains(inputIPAddress);
	}

	public static void test3() {
		Set<String>  a= new HashSet();
		a.add("218.213.102.6/31");
		System.out.println(toIPSet(a));
		Set<String> subnets = new HashSet<String>();
		subnets.add("2402:db40:1::/48");
		System.out.print(toIPSet(subnets));

		String Domains = "121.32.249.172, 120.92.174.135, 121.32.249.171, 58.220.29.72, 120.92.158.137, 119.147.34.242, 183.52.13.154, 222.186.49.111, 113.96.181.216, 61.147.235.208, 113.96.181.217, 106.7.64.1, 119.147.156.231, 119.147.156.230, 113.96.181.211, 113.96.181.214, 172.18.21.10, 119.125.41.87, 222.186.18.241, 113.96.109.95, 14.215.23.246, 114.80.24.230, 120.92.168.45, 125.77.163.235, 113.101.214.238, 124.239.234.105, 183.2.192.234, 119.125.43.15, 183.2.192.112, 110.43.33.232, 183.2.192.235, 36.25.252.1, 14.215.172.215, 121.12.122.79, 14.215.172.217, 175.6.49.240, 14.215.172.216, 14.215.172.219, 114.80.24.231, 14.215.57.226, 119.125.115.232, 113.96.98.73, 14.215.57.228, 183.131.203.6,  58.222.35.205, 121.11.2.189, 14.215.166.95, 180.122.78.242, 14.215.172.220, 58.215.158.241, 14.215.172.221, 180.101.150.112, 58.216.87.204, 119.147.70.221, 110.76.40.240, 119.147.33.73, 116.5.154.179, 222.188.43.132, 222.188.43.130, 58.216.4.238, 183.61.168.248, 222.188.43.131, 110.43.34.66, 58.216.4.239, 183.61.168.240, 119.147.41.240, 183.61.168.241, 183.61.168.242, 42.120.0.158, 221.231.83.241, 119.147.41.248, 59.36.226.242, 222.188.43.129, 125.94.49.226, 119.147.33.66,  106.7.64.1, 113.96.154.93, 121.12.123.229, 121.11.2.240, 218.94.206.241,  183.131.185.41, 113.113.127.240,  119.96.250.129, 119.3.238.64, 61.147.235.194, 113.113.127.241, 124.232.170.15, 117.50.8.201, 59.32.49.99, 119.125.41.206, 183.6.241.1, 119.147.41.239, 175.6.153.1, 119.147.41.238, 113.96.98.82, 59.36.226.239, 59.36.226.238, 58.216.87.229, 106.117.245.1,  27.159.125.1, 183.61.241.232, 91.195.241.136, 183.6.231.203, 121.14.131.238, 121.14.131.237, 58.216.107.214, 61.147.236.11, 61.147.236.12, 113.113.127.237, 113.113.127.238, 183.136.135.216, 61.150.82.6, 183.136.135.224, 183.136.135.223, 183.136.135.222, 183.136.135.221, 183.136.135.220, 121.12.123.201, 125.94.50.238, 58.220.28.104, 183.61.241.229, 124.229.52.1,  111.73.62.1, 106.117.213.218, 183.136.135.215, 139.196.14.154, 183.136.135.213, 172.18.21.243, 111.73.62.1, 113.96.155.122, 113.96.155.121, 67.198.130.7, 222.186.16.244, 115.238.195.19, 124.239.239.229, 115.238.195.18, 222.186.16.240, 222.186.16.241, 183.134.13.131, 222.186.16.242, 183.134.13.130, 222.186.16.243, 14.215.56.243, 122.228.232.71, 14.215.56.242, 122.228.232.70, 14.215.56.240, 14.215.166.205, 222.186.16.248, 14.17.124.239, 27.148.180.224, 113.100.189.152, 110.167.162.1, 27.128.214.219, 113.96.98.102, 14.215.167.253, 183.61.13.209, 183.60.159.171, 121.11.2.200, 122.228.77.85, 121.9.212.151, 121.9.212.150, 119.125.45.46, 124.239.158.238, 115.238.195.21, 183.134.13.129, 183.131.11.46, 122.228.232.69, 122.228.232.68, 222.186.35.80, 222.186.35.81, 125.77.130.22, 115.238.195.20, 212.64.117.140, 124.239.239.230, 180.122.76.238, 121.9.212.141, 119.147.39.226, 121.9.244.86, 115.231.191.216, 121.9.244.85, 183.61.13.234, 121.9.244.84, 116.5.155.130, 222.186.35.79, 122.227.201.1, 14.215.55.228, 113.96.83.98, 121.32.249.233, 113.100.189.29, 113.96.109.243, 221.228.219.62, 61.146.189.54, 121.9.246.110, 113.96.109.246, 183.2.192.198, 58.216.4.248, 117.91.177.238, 14.215.166.116, 58.216.4.241, 121.32.249.249,  124.229.52.1, 58.216.4.240, 58.216.4.243, 219.135.59.170, 222.186.16.238, 58.216.4.242, 222.186.16.239, 14.17.124.238, 58.216.4.244, 219.132.165.61, 113.64.94.76, 113.105.231.252,  110.43.33.137, 58.223.210.225,  36.25.252.1, 61.146.176.145, 113.105.155.219, 121.11.2.199, 14.29.104.122, 119.147.70.218, 121.11.2.195, 119.147.70.216, 58.215.146.119, 61.160.228.240, 119.3.70.188, 175.6.161.1, 221.231.81.239, 221.231.81.238, 183.2.200.243, 183.60.228.248, 183.2.200.244,  110.43.33.124, 117.25.159.243, 121.12.122.120, 183.60.228.242, 14.215.167.213, 121.12.122.81, 14.29.104.112, 14.215.55.230, 183.146.212.129, 27.148.151.64, 183.146.212.135, 124.238.245.63, 183.146.212.132, 119.96.250.129, 183.146.212.137, 124.238.245.66, 183.146.212.136, 119.125.40.80, 183.146.212.131, 183.146.212.130, 183.2.200.238, 27.159.125.1, 120.92.144.250, 139.159.241.37, 120.92.169.201, 219.152.56.1, 124.115.135.1,  110.43.33.229, 120.92.168.34, 61.140.13.246, 183.52.12.176, 120.92.112.150, 115.238.195.3, 124.238.245.104, 115.238.195.4, 115.223.28.41, 115.238.195.2, 113.96.108.116, 115.238.195.7, 58.222.35.201, 113.96.108.118, 58.222.35.200, 115.238.195.5, 115.238.195.6, 120.92.78.57, 221.228.219.98, 113.94.141.53, 58.222.35.204, 106.225.223.20, 58.222.35.203, 27.128.211.1, 124.232.162.213";
		Set<String> IPsOfDomain = new HashSet<>();
		IPsOfDomain.addAll(Arrays.asList(Domains.split(", ")));
		Set<String> subnets1 = toSmallerSubNets(IPsOfDomain);
		System.out.println(subnets1);
	}

	public static void main(String[] args) throws AddressStringException {
		//System.out.println(toIPList("178.170.186.0-178.170.186.255"));
		//System.out.println(checkIPIsInGivenRange("178.170.186.1","178.170.186.0","178.170.186.255"));
		System.out.println(isValidIP("033.072.025.009"));
	}
}