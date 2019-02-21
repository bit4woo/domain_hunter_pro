package test;

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

		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getCidrSignature());
		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getBroadcastAddress());
		System.out.println(new SubnetUtils("192.168.1.254/23").getInfo().getNetworkAddress());
		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getLowAddress());
		System.out.println(new SubnetUtils("192.168.1.254/24").getInfo().getNetworkAddress()+new SubnetUtils("192.168.1.254/24").getInfo().getNetmask());
		String networkaddress = new SubnetUtils("192.168.1.254/24").getInfo().getNetworkAddress();
		String mask = new SubnetUtils("192.168.1.254/24").getInfo().getNetmask();
		//new SubnetUtils(networkaddress+"/"+mask);
		System.out.println(new SubnetUtils(networkaddress,mask).getInfo().getCidrSignature());
	}
}
