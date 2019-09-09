package test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;

public class subnettest {
	public static void main(String args[]) {

/*		HashMap<String, Set<String>> result = dnsquery("www.baidu.com");
		System.out.println(result.get("IP").toString());
		System.out.println(dnsquery("www.baidu.com"));*/

/*		System.out.print(new SubnetUtils("192.168.1.1/23").getInfo().getCidrSignature());

		Set<String> IPSet = new HashSet<String>();
		IPSet.add("192.168.1.225");
		IPSet.add("192.168.1.128");
		IPSet.add("192.168.1.129");
		IPSet.add("192.168.1.155");
		IPSet.add("192.168.1.224");
		String subnets = ipset2cidr(IPSet);
		System.out.println(subnets);*/

//		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getCidrSignature());
//		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getBroadcastAddress());
//		System.out.println(new SubnetUtils("192.168.1.254/23").getInfo().getNetworkAddress());
//		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getLowAddress());
//		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getNetworkAddress()+new SubnetUtils("192.168.1.254/24").getInfo().getNetmask());
//		String networkaddress = new SubnetUtils("192.168.1.254/24").getInfo().getNetworkAddress();
//		String mask = new SubnetUtils("192.168.1.254/24").getInfo().getNetmask();
//		//new SubnetUtils(networkaddress+"/"+mask);
//		System.out.println(new SubnetUtils(networkaddress,mask).getInfo().getCidrSignature());
		
		IPrangeToIPSet("123.58.44.160-192");
	}
	
	public static void IPrangeToIPSet(String subnet) {
		Set<String> IPSet = new HashSet<String>();
		String[] ips = subnet.split("-");
		if (ips.length ==2) {
			try {
				String startip = ips[0].trim();
				String endip = ips[1].trim();
				//System.out.println(startip);
				//System.out.println(endip);
				//Converts a String that represents an IP to an int.
				InetAddress i = InetAddress.getByName(startip);
				int startIPInt= ByteBuffer.wrap(i.getAddress()).getInt();
				
				if (endip.indexOf(".") == -1) {
					endip = startip.substring(0,startip.lastIndexOf("."))+endip;
					//System.out.println(endip);
				}
				InetAddress j = InetAddress.getByName(endip);
				int endIPInt= ByteBuffer.wrap(j.getAddress()).getInt();
				
				while (startIPInt <= endIPInt) {
					//System.out.println(startIPInt);
					startIPInt  = startIPInt+1;
					//This convert an int representation of ip back to String
					i= InetAddress.getByName(String.valueOf(startIPInt));
					String ip= i.getHostAddress();
					IPSet.add(ip);
					continue;
				}
				//System.out.print(IPSet);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public long ipToLong(String ipAddress) {

	    String[] ipAddressInArray = ipAddress.split("\\.");

	    long result = 0;
	    for (int i = 0; i < ipAddressInArray.length; i++) {

	        int power = 3 - i;
	        int ip = Integer.parseInt(ipAddressInArray[i]);
	        result += ip * Math.pow(256, power);

	    }

	    return result;
	  }
}
