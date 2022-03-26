package burp;

import com.google.common.net.InternetDomainName;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainNameUtils {

	//域名校验和域名提取还是要区分对待
	//domain.DomainProducer.grepDomain(String)是提取域名的，正则中包含了*号
	public static boolean isValidDomain(String domain) {
		if (null == domain) {
			return false;
		}

		final String DOMAIN_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
		//final String DOMAIN_NAME_PATTERN = "([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";//-this.state.scroll 这种也会被认为是合法的。
		Pattern pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
		Matcher matcher = pDomainNameOnly.matcher(domain);
		boolean formateOk = matcher.matches();
		if (formateOk){
			//a86ba224e43010880724df4a4be78c11
			//administratoradministrator
			//虽然按照RFC的规定，域名的单个字符的模块长度可以是63。但是实际使用情况中，基本不可能有这样的域名。
			String tmp = domain.replaceAll("-", ".");
			String[] tmpArray= tmp.split("\\.");
			for (String item:tmpArray){
				if (item.length()>=32){
					return false;
				}
			}
			return true;
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


	/**
	 * 注意，仅用于relatedToRoot转换时
	 *
	 * @param inputDomain
	 * @return
	 */
	public static String getRootDomain(String inputDomain) {
		inputDomain = DomainNameUtils.cleanDomain(inputDomain);
		try {
			String rootDomain = InternetDomainName.from(inputDomain).topPrivateDomain().toString();
			return rootDomain;
		} catch (Exception e) {
			return inputDomain;
			//InternetDomainName.from("www.jd.local").topPrivateDomain()//Not under a public suffix: www.jd.local
		}
	}

	public static String cleanDomain(String domain) {
		if (domain == null){
			return null;
		}
		domain = domain.toLowerCase().trim();
		if (domain.startsWith("http://")|| domain.startsWith("https://")) {
			try {
				domain = new URL(domain).getHost();
			} catch (MalformedURLException e) {
				return null;
			}
		}else {
			if (domain.contains(":")) {//处理带有端口号的域名
				domain = domain.substring(0,domain.indexOf(":"));
			}
		}

		if (domain.endsWith(".")) {
			domain = domain.substring(0,domain.length()-1);
		}

		return domain;
	}

	/**
	 * 是否是TLD域名。比如 baidu.net 是baidu.com的TLD域名
	 * 注意：www.baidu.com不是baidu.com的TLD域名，但是是子域名！！！
	 *
	 * 这里的rootDomain不一定是 topPrivate。比如 shopeepay.shopee.sg 和shopeepay.shopee.io
	 * @param domain
	 * @param rootDomain
	 */
	@Deprecated //范围太广，误报太多
	private static boolean isTLDDomain(String domain,String rootDomain) {
		try {
			InternetDomainName suffixDomain = InternetDomainName.from(domain).publicSuffix();
			InternetDomainName suffixRootDomain = InternetDomainName.from(rootDomain).publicSuffix();
			if (suffixDomain != null && suffixRootDomain != null){
				String suffixOfDomain = suffixDomain.toString();
				String suffixOfRootDomain = suffixRootDomain.toString();//TODO 校验一下；gettitle控制
				//域名后缀比较
				if (suffixOfDomain.equalsIgnoreCase(suffixOfRootDomain)) {
					return false;
				}
				//去除后缀然后比较
				String tmpDomain = Commons.replaceLast(domain, suffixOfDomain, "");
				String tmpRootdomain = Commons.replaceLast(rootDomain, suffixOfRootDomain, "");
				if (tmpDomain.endsWith("."+tmpRootdomain) || tmpDomain.equalsIgnoreCase(tmpRootdomain)) {
					return true;
				}
			}
			return false;
		}catch (java.lang.IllegalArgumentException e){
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 相比isTLDDomain() 通过白名单缩小范围
	 * @param domain
	 * @param rootDomain
	 * @return
	 */
	public static boolean isWhiteListTDL(String domain,String rootDomain){
		String listStr = ".ac|.ad|.ae|.af|.ag|.ai|.al|.am|.ao|.aq|.ar|.as|.at|.au|.aw|.ax|.az|.ba|" +
				".bb|.bd|.be|.bf|.bg|.bh|.bi|.bj|.bm|.bn|.bo|.bq|.br|.bs|.bt|.bw|.by|.bz|.ca|" +
				".cc|.cd|.cf|.cg|.ch|.ci|.ck|.cl|.cm|.cn|.co|.com|.cr|.cu|.cv|.cw|.cx|.cy|.cz|" +
				".de|.dj|.dk|.dm|.do|.dz|.ec|.edu|.ee|.eg|.eh|.er|.es|.et|.eu|.fi|.fj|.fk|.fm|" +
				".fo|.fr|.ga|.gd|.ge|.gf|.gg|.gh|.gi|.gl|.gm|.gn|.gov|.gp|.gq|.gr|.gs|.gt|.gu|" +
				".gw|.gy|.hk|.hm|.hn|.hr|.ht|.hu|.id|.ie|.il|.im|.in|.int|.io|.iq|.ir|.is|.it|" +
				".je|.jm|.jo|.jp|.ke|.kg|.kh|.ki|.km|.kn|.kp|.kr|.kw|.ky|.kz|.la|.lb|.lc|.li|" +
				".lk|.lr|.ls|.lt|.lu|.lv|.ly|.ma|.mc|.md|.me|.mg|.mh|.mil|.mk|.ml|.mm|.mn|.mo|" +
				".mp|.mq|.mr|.ms|.mt|.mu|.mv|.mw|.mx|.my|.mz|.na|.nc|.ne|.net|.nf|.ng|.ni|.nl|" +
				".no|.np|.nr|.nu|.nz|.om|.org|.pa|.pe|.pf|.pg|.ph|.pk|.pl|.pm|.pn|.pr|.ps|.pt|" +
				".pw|.py|.qa|.re|.ro|.rs|.ru|.rw|.sa|.sb|.sc|.sd|.se|.sg|.sh|.si|.sk|.sl|.sm|" +
				".sn|.so|.sr|.ss|.st|.su|.sv|.sx|.sy|.sz|.tc|.td|.tf|.tg|.th|.tj|.tk|.tl|.tm|" +
				".tn|.to|.tr|.tt|.tv|.tw|.tz|.ua|.ug|.uk|.us|.uy|.uz|.va|.vc|.ve|.vg|.vi|.vn|" +
				".vu|.wf|.ws|.ye|.yt|.za|.zm|.zw";
		List<String> tlds = Arrays.asList(listStr.split("\\|"));

		try {
			if(isTLDDomain(domain,rootDomain)){
				String suffixOfDomain = InternetDomainName.from(domain).publicSuffix().toString();//没有保护点号
				String[] items = suffixOfDomain.split("\\.");
				for (String item:items){
					if (!tlds.contains("."+item)){
						return false;
					}
				}
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 由于这里的rootDomain是我们自己指定的不一定是topPrivate。
	 * 比如 shopeepay.shopee.sg 和shopeepay.shopee.io 应该返回false
	 * 比如 shopeepay.shopee.sg shopee.io 应该返回true
	 *
	 * 关键看rootDomain是不是topPrivate
	 *
	 * @param domain
	 * @param rootDomain
	 * @return
	 */
	public static boolean isTLDDomainOfTopPrivate(String domain,String rootDomain) {
		try {
			if(isTLDDomain(domain,rootDomain)){
				return !InternetDomainName.from(rootDomain).hasParent();
			}
			return false;
		}catch (java.lang.IllegalArgumentException e){
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		System.out.println(isWhiteListTDL("test.shopee.co.th","shopee.com"));
		//System.out.println(isValidDomain("www1.baidu.com"));
		//System.out.println(isValidDomain("aaaaaaaaa-aaaaaaaaaaaaaaa-aaaaaaaaaaaaaa.www1.baidu.com"));
	}
}
