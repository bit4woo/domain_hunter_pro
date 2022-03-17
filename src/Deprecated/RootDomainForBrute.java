package Deprecated;

import burp.Commons;
import burp.DomainNameUtils;

import org.apache.commons.net.whois.WhoisClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
class RootDomainForBrute{
	public String rootDomain;
	public Set<String> nameServers = new HashSet();
	public Set<String> wildIPset = new HashSet<String>(); //泛解析的IP
	public Set<String> wildCDNSet = new HashSet<String>(); //泛解析的CDN
	public RootDomainForBrute(String rootDomain){
		this.rootDomain = rootDomain;
		nameServers = nameServer(this.rootDomain);

		String badDomain = "domain-hunter-pro-test."+rootDomain;
		HashMap<String,Set<String>> result = query(badDomain);
		wildCDNSet = result.get("CDN");
		wildIPset = result.get("IP");
	}


	public static String whois(String domainName) {
		StringBuilder result = new StringBuilder("");

		WhoisClient whois = new WhoisClient();
		try {
			//default is internic.net
			whois.connect(WhoisClient.DEFAULT_HOST);
			String whoisData1 = whois.query("=" + domainName);
			result.append(whoisData1);
			whois.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result.toString();
	}

	public static Set<String> nameServer(String domain){
		String result = whois(domain).toLowerCase();
		System.out.println(result);
		Set<String> domains = new HashSet<>();
		if (!result.equalsIgnoreCase("")){
			final String NAME_SERVER_PATTERN = "name server: (.+?)*";
			Pattern pNameServer = Pattern.compile(NAME_SERVER_PATTERN);
			Matcher matcher = pNameServer.matcher(result);
			while (matcher.find()) {//多次查找
				String server = matcher.group().replace("name server:","").trim();
				domains.add(server);
			}
		}

		if (domains.size() == 0){
			//result = Commons.dnsquery(domain); //使用本地dns服务器进行爆破，会导致本地访问很慢
			domains.add("223.5.5.5");
			domains.add("223.6.6.6");
		}

		return domains;
	}

	public HashMap<String,Set<String>> query(String domain){
		HashMap<String,Set<String>> result = new HashMap();
		for (String server:nameServers){
			try {
				result = DomainNameUtils.dnsquery(domain,server);
				return result;
			}catch (Exception e){
				;//do nothing
			}
		}
		return result;
	}

	public static void main(String args[]){
//        System.out.println(Commons.dnsquery("xxxsdfdsfasf.jr.jd.com","223.5.5.5"));
		//RootDomainForBrute a = new RootDomainForBrute("jd.com");
		System.out.println(nameServer("jd.com"));
	}
}