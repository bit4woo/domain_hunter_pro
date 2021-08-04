package burp;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.ZoneTransferIn;

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
		extendset.add(".css");//gif,jpg,png,css,woff
		extendset.add(".woff");
		Iterator<String> iter = extendset.iterator();
		while (iter.hasNext()) {
			if(urlpath.endsWith(iter.next().toString())) {//if no next(), this loop will not break out
				return true;
			}
		}
		return false;
	}

	//域名校验和域名提取还是要区分对待
	//domain.DomainProducer.grepDomain(String)是提取域名的，正则中包含了*号
	public static boolean isValidDomain(String domain) {
		if (null == domain) {
			return false;
		}
		final String DOMAIN_NAME_PATTERN = "([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
		Pattern pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
		Matcher matcher = pDomainNameOnly.matcher(domain);
		return matcher.matches();
	}

	//校验字符串是否是一个合格的IP地址
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
	
	@Deprecated
	public static boolean isValidSubnet(String subnet) {
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

	public static HashMap<String,Set<String>> dnsquery(String domain,String server) {
		HashMap<String,Set<String>> result = new HashMap<String,Set<String>>();
		try{
			Lookup lookup = new Lookup(domain, org.xbill.DNS.Type.A);
			Resolver resolver = new SimpleResolver(server);
			lookup.setResolver(resolver);
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
				}
			}
			result.put("IP", IPset);
			result.put("CDN", CDNSet);
			return result;

		}catch(Exception e){
			e.printStackTrace();
			return result;
		}
	}

	public static Set<String> GetAuthoritativeNameServer(String domain) {
		Set<String> NameServerSet = new HashSet<String>();
		try{
			Lookup lookup = new Lookup(domain, org.xbill.DNS.Type.NS);
			lookup.run();

			if(lookup.getResult() == Lookup.SUCCESSFUL){
				Record[] records=lookup.getAnswers();
				for (int i = 0; i < records.length; i++) {
					NSRecord a = (NSRecord) records[i];
					String server = a.getTarget().toString();
					if (server!=null) {
						NameServerSet.add(server);
					}
				}
			}
			return NameServerSet;

		}catch(Exception e){
			e.printStackTrace();
			return NameServerSet;
		}
	}

	public static List<String> ZoneTransferCheck(String domain,String NameServer) {
		List<String> Result = new ArrayList<String>();
		try {
			ZoneTransferIn zone = ZoneTransferIn.newAXFR(new Name(domain), NameServer, null);
			zone.run();
			Result = zone.getAXFR();
			BurpExtender.getStdout().println("!!! "+NameServer+" is zoneTransfer vulnerable for domain "+domain+" !");
			System.out.print(Result);
		} catch (Exception e1) {
			BurpExtender.getStdout().println(String.format("[Server:%s Domain:%s] %s", NameServer,domain,e1.getMessage()));
		} 
		return Result;
	}


	//////////////////////////////////////////IP  subnet  CIDR/////////////////////////////////
	/*
	To Class C Network
	 */
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
					if (Commons.isValidSubnet(ip)) {//这里的IP也可能是网段，不要被参数名称限定了
						smallSubNets.add(ip);
						continue;
					}
					if (!Commons.isValidIP(ip)) {
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
					if (!Commons.isValidIP(ip)) {
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
			}else if (subnet.contains("-")) {
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
							IPList.add(ip);
							continue;
						}
						//System.out.print(IPSet);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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

	public static String getNowTimeString() {
		SimpleDateFormat simpleDateFormat = 
				new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		return simpleDateFormat.format(new Date());
	}

	public static String TimeToString(long time) {
		SimpleDateFormat simpleDateFormat = 
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return simpleDateFormat.format(time);
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
			String[] cmdArray = new String[] {browser,urlString};

			//runtime.exec(browser+" "+urlString);//当命令中有空格时会有问题
			Runtime.getRuntime().exec(cmdArray);
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


	public static List<Integer> Port_prompt(Component prompt, String str){
		String defaultPorts = "8080,8000,8443";
		String user_input = JOptionPane.showInputDialog(prompt, str,defaultPorts);
		if (null == user_input || user_input.trim().equals("")) return  null; 
		List<Integer> portList = new ArrayList<Integer>();
		for (String port: user_input.trim().split(",")) {
			int portint = Integer.parseInt(port);
			portList.add(portint);
		}
		return portList;
	}

	public static boolean isWindows() {
		String OS_NAME = System.getProperties().getProperty("os.name").toLowerCase();
		if (OS_NAME.contains("windows")) {
			return true;
		} else {
			return false;
		}
	}

	public static ArrayList<String> regexFind(String regex,String content) {
		ArrayList<String> result = new ArrayList<String>();
		Pattern pRegex = Pattern.compile(regex);
		Matcher matcher = pRegex.matcher(content);
		while (matcher.find()) {//多次查找
			result.add(matcher.group());
		}
		return result;
	}


	public static void writeToClipboard(String text) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection selection = new StringSelection(text);
		clipboard.setContents(selection, null);
	}

	public static boolean isWindows10() {
		String OS_NAME = System.getProperties().getProperty("os.name").toLowerCase();
		if (OS_NAME.equalsIgnoreCase("windows 10")) {
			return true;
		}
		return false;
	}

	public static boolean isMac(){
		String os = System.getProperty("os.name").toLowerCase();
		//Mac
		return (os.indexOf( "mac" ) >= 0); 
	}

	public static boolean isUnix(){
		String os = System.getProperty("os.name").toLowerCase();
		//linux or unix
		return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
	}

	public static void OpenFolder(String path) throws IOException {
		String program = null;
		if (isWindows()){
			program = "explorer.exe";
		}else if(isMac()){
			program = "open";
		}else {
			program = "nautilus";
		}
		if ((path.startsWith("\"") && path.endsWith("\"")) || (path.startsWith("'") && path.endsWith("'"))){

		}else if (path.contains(" ")){
			path = "\""+path+"\"";
		}
		String[] cmdArray = new String[] {program,path};
		Runtime.getRuntime().exec(cmdArray);
	}

	/*
	 *将形如 https://www.runoob.com的URL统一转换为
	 * https://www.runoob.com:443/
	 * 
	 * 因为末尾的斜杠，影响URL类的equals的结果。
	 * 而默认端口影响String格式的对比结果。
	 */

	public static String formateURLString(String urlString) {
		try {
			//urlString = "https://www.runoob.com";
			URL url = new URL(urlString);
			String host = url.getHost();
			int port = url.getPort();
			String path = url.getPath();

			if (port == -1) {
				String newHost = url.getHost()+":"+url.getDefaultPort();
				urlString = urlString.replace(host, newHost);
			}

			if (path.equals("")) {
				urlString = urlString+"/";
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return urlString;
	}

	public static List<String> getLinesFromTextArea(JTextArea textarea){
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		String[] lines = textarea.getText().replaceAll("\r\n", "\n").split("\n");
		List<String> result = new ArrayList<String>();
		for(String line: lines) {
			line = line.trim();
			if (line!="") {
				result.add(line.trim());
			}
		}
		return result;
	}
	

	public static List<String> removePrefixAndSuffix(List<String> input,String Prefix,String Suffix) {
		ArrayList<String> result = new ArrayList<String>();
		if (Prefix == null && Suffix == null) {
			return result;
		} else {
			if (Prefix == null) {
				Prefix = "";
			}

			if (Suffix == null) {
				Suffix = "";
			}

			List<String> content = input;
			for (String item:content) {
				if (item.startsWith(Prefix)) {
					//https://stackoverflow.com/questions/17225107/convert-java-string-to-string-compatible-with-a-regex-in-replaceall
					String tmp = Pattern.quote(Prefix);//自动实现正则转义
					item = item.replaceFirst(tmp, "");
				}
				if (item.endsWith(Suffix)) {
					String tmp = Pattern.quote(reverse(Suffix));//自动实现正则转义
					item = reverse(item).replaceFirst(tmp, "");
					item = reverse(item);
				}
				result.add(item); 
			}
			return result;
		}
	}
	
	public static String reverse(String str) {
		if (str == null) {
			return null;
		}
		return new StringBuffer(str).reverse().toString();
	}
	
	public static void test1() {
		SubnetUtils net = new SubnetUtils("143.92.67.34/24");
		System.out.println(net.getInfo().isInRange("143.92.67.34:6443"));
	}
	public static void test2() {
				Set<String> IPSet = new HashSet<String>();
				IPSet.add("192.168.1.225");
				IPSet.add("192.168.1.128");
				IPSet.add("192.168.1.129");
				IPSet.add("192.168.1.155");
				IPSet.add("192.168.1.224");
				IPSet.add("192.168.1.130");
				Set<String> subnets = toSmallerSubNets(IPSet);
		
				System.out.println(toIPSet(subnets));
	}
	
	public static void test3() {
		Set<String>  a= new HashSet();
		a.add("218.213.102.6/31");
		System.out.println(toIPSet(a));
		Set<String> subnets = new HashSet<String>();
		subnets.add("2402:db40:1::/48");
		System.out.print(toIPSet(subnets));
		System.out.print(dnsquery("0g.jd.com"));
		System.out.print(GetAuthoritativeNameServer("jd.com"));
		ZoneTransferCheck("sf-express.com","ns4.sf-express.com");

		String Domains = "121.32.249.172, 120.92.174.135, 121.32.249.171, 58.220.29.72, 120.92.158.137, 119.147.34.242, 183.52.13.154, 222.186.49.111, 113.96.181.216, 61.147.235.208, 113.96.181.217, 106.7.64.1, 119.147.156.231, 119.147.156.230, 113.96.181.211, 113.96.181.214, 172.18.21.10, 119.125.41.87, 222.186.18.241, 113.96.109.95, 14.215.23.246, 114.80.24.230, 120.92.168.45, 125.77.163.235, 113.101.214.238, 124.239.234.105, 183.2.192.234, 119.125.43.15, 183.2.192.112, 110.43.33.232, 183.2.192.235, 36.25.252.1, 14.215.172.215, 121.12.122.79, 14.215.172.217, 175.6.49.240, 14.215.172.216, 14.215.172.219, 114.80.24.231, 14.215.57.226, 119.125.115.232, 113.96.98.73, 14.215.57.228, 183.131.203.6,  58.222.35.205, 121.11.2.189, 14.215.166.95, 180.122.78.242, 14.215.172.220, 58.215.158.241, 14.215.172.221, 180.101.150.112, 58.216.87.204, 119.147.70.221, 110.76.40.240, 119.147.33.73, 116.5.154.179, 222.188.43.132, 222.188.43.130, 58.216.4.238, 183.61.168.248, 222.188.43.131, 110.43.34.66, 58.216.4.239, 183.61.168.240, 119.147.41.240, 183.61.168.241, 183.61.168.242, 42.120.0.158, 221.231.83.241, 119.147.41.248, 59.36.226.242, 222.188.43.129, 125.94.49.226, 119.147.33.66,  106.7.64.1, 113.96.154.93, 121.12.123.229, 121.11.2.240, 218.94.206.241,  183.131.185.41, 113.113.127.240,  119.96.250.129, 119.3.238.64, 61.147.235.194, 113.113.127.241, 124.232.170.15, 117.50.8.201, 59.32.49.99, 119.125.41.206, 183.6.241.1, 119.147.41.239, 175.6.153.1, 119.147.41.238, 113.96.98.82, 59.36.226.239, 59.36.226.238, 58.216.87.229, 106.117.245.1,  27.159.125.1, 183.61.241.232, 91.195.241.136, 183.6.231.203, 121.14.131.238, 121.14.131.237, 58.216.107.214, 61.147.236.11, 61.147.236.12, 113.113.127.237, 113.113.127.238, 183.136.135.216, 61.150.82.6, 183.136.135.224, 183.136.135.223, 183.136.135.222, 183.136.135.221, 183.136.135.220, 121.12.123.201, 125.94.50.238, 58.220.28.104, 183.61.241.229, 124.229.52.1,  111.73.62.1, 106.117.213.218, 183.136.135.215, 139.196.14.154, 183.136.135.213, 172.18.21.243, 111.73.62.1, 113.96.155.122, 113.96.155.121, 67.198.130.7, 222.186.16.244, 115.238.195.19, 124.239.239.229, 115.238.195.18, 222.186.16.240, 222.186.16.241, 183.134.13.131, 222.186.16.242, 183.134.13.130, 222.186.16.243, 14.215.56.243, 122.228.232.71, 14.215.56.242, 122.228.232.70, 14.215.56.240, 14.215.166.205, 222.186.16.248, 14.17.124.239, 27.148.180.224, 113.100.189.152, 110.167.162.1, 27.128.214.219, 113.96.98.102, 14.215.167.253, 183.61.13.209, 183.60.159.171, 121.11.2.200, 122.228.77.85, 121.9.212.151, 121.9.212.150, 119.125.45.46, 124.239.158.238, 115.238.195.21, 183.134.13.129, 183.131.11.46, 122.228.232.69, 122.228.232.68, 222.186.35.80, 222.186.35.81, 125.77.130.22, 115.238.195.20, 212.64.117.140, 124.239.239.230, 180.122.76.238, 121.9.212.141, 119.147.39.226, 121.9.244.86, 115.231.191.216, 121.9.244.85, 183.61.13.234, 121.9.244.84, 116.5.155.130, 222.186.35.79, 122.227.201.1, 14.215.55.228, 113.96.83.98, 121.32.249.233, 113.100.189.29, 113.96.109.243, 221.228.219.62, 61.146.189.54, 121.9.246.110, 113.96.109.246, 183.2.192.198, 58.216.4.248, 117.91.177.238, 14.215.166.116, 58.216.4.241, 121.32.249.249,  124.229.52.1, 58.216.4.240, 58.216.4.243, 219.135.59.170, 222.186.16.238, 58.216.4.242, 222.186.16.239, 14.17.124.238, 58.216.4.244, 219.132.165.61, 113.64.94.76, 113.105.231.252,  110.43.33.137, 58.223.210.225,  36.25.252.1, 61.146.176.145, 113.105.155.219, 121.11.2.199, 14.29.104.122, 119.147.70.218, 121.11.2.195, 119.147.70.216, 58.215.146.119, 61.160.228.240, 119.3.70.188, 175.6.161.1, 221.231.81.239, 221.231.81.238, 183.2.200.243, 183.60.228.248, 183.2.200.244,  110.43.33.124, 117.25.159.243, 121.12.122.120, 183.60.228.242, 14.215.167.213, 121.12.122.81, 14.29.104.112, 14.215.55.230, 183.146.212.129, 27.148.151.64, 183.146.212.135, 124.238.245.63, 183.146.212.132, 119.96.250.129, 183.146.212.137, 124.238.245.66, 183.146.212.136, 119.125.40.80, 183.146.212.131, 183.146.212.130, 183.2.200.238, 27.159.125.1, 120.92.144.250, 139.159.241.37, 120.92.169.201, 219.152.56.1, 124.115.135.1,  110.43.33.229, 120.92.168.34, 61.140.13.246, 183.52.12.176, 120.92.112.150, 115.238.195.3, 124.238.245.104, 115.238.195.4, 115.223.28.41, 115.238.195.2, 113.96.108.116, 115.238.195.7, 58.222.35.201, 113.96.108.118, 58.222.35.200, 115.238.195.5, 115.238.195.6, 120.92.78.57, 221.228.219.98, 113.94.141.53, 58.222.35.204, 106.225.223.20, 58.222.35.203, 27.128.211.1, 124.232.162.213";
		Set<String> IPsOfDomain = new HashSet<>();
		IPsOfDomain.addAll(Arrays.asList(Domains.split(", ")));
		Set<String> subnets1 = Commons.toSmallerSubNets(IPsOfDomain);
		System.out.println(subnets1);
	}
	public static void test4() {
		String Prefix = "\"";
//		String Prefix = Pattern.quote("\"");
		System.out.println(Prefix);
		System.out.println("\"aaaa\"".replaceFirst(Prefix, ""));
	}
	
	public static void test5() {
		String aa = "10.12.12.12/";
		System.out.println(aa.split("/").length);
	}
	
	public static void test6() {
		String aa = "10.  12. 12.12/";
		System.out.println(aa.trim());
	}
	
	public static void test7() {
		HashMap<String,Set<String>> result = Commons.dnsquery("163.177.151.109");
		Set<String> IPSet = result.get("IP");
		Set<String> CDNSet = result.get("CDN");
		System.out.println(IPSet);
		System.out.println(CDNSet);
	}

	public static void main(String args[]) {
		test7();
	}
}
