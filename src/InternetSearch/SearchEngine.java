package InternetSearch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SearchEngine {

	//public static final String BAIDU = "baidu";
	//public static final String BING = "bing";

	public static final String GOOGLE = "google";
	public static final String GITHUB = "github";

	//资产搜索引擎
	public static final String FOFA = "fofa";
	public static final String SHODAN = "shodan";
	public static final String ZOOMEYE = "zoomeye";

	public static final String QIANXIN_HUNTER = "hunter.qianxin.com";
	public static final String QIANXIN_TI = "ti.qianxin.com";

	public static final String QUAKE_360 = "quake.360.net";
	public static final String TI_360 = "ti.360.net";

	//邮箱搜索
	public static final String HUNTER_IO = "hunter.io";


	//https://bgp.he.net/dns/shopee.com
	//https://bgp.he.net/net/143.92.111.0/24
	//https://bgp.he.net/ip/143.92.127.1
	public static final String ASN_INFO_BGP_HE_NET = "bgp.he.net";
	//	https://ipinfo.io/8.8.8.8
	public static final String IPINFO_IO = "ipinfo.io";
	
	public static final String WHOIS_CHINAZ = "whois.chinaz.com";
	public static final String WHOIS = "www.whois.com";
	


	public static List<String> getAllEngineList(){
		List<String> result = new ArrayList<String>();
		Field[] fields = SearchEngine.class.getDeclaredFields();
		for (Field field : fields) {
			//String varName = field.getName();// 对于每个属性，获取属性名
			if (field.getGenericType().toString().equals("class java.lang.String")) {// 如果type是类类型，则前面包含"class "，后面跟类名
				try {
					String value = (String) field.get(SearchEngine.class);//获取属性值
					result.add(value);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	public static List<String> getEmailSearchEngineList(){
		List<String> result = new ArrayList<String>();
		result.add(HUNTER_IO);
		return result;
	}
	
	/**
	 * 域名、网段、IP等对象的扩展信息搜索引擎
	 * @return
	 */
	public static List<String> getExtendInfoSearchEngineList(){
		List<String> result = new ArrayList<String>();
		result.add(ASN_INFO_BGP_HE_NET);
		result.add(IPINFO_IO);
		result.add(WHOIS_CHINAZ);
		result.add(WHOIS);
		return result;
	}
	
	
	public static List<String> getAssetSearchEngineList(){
		List<String> result = new ArrayList<String>();
		result.add(FOFA);
		result.add(SHODAN);
		result.add(ZOOMEYE);
		result.add(QIANXIN_HUNTER);
		result.add(QIANXIN_TI);
		result.add(QUAKE_360);
		result.add(TI_360);
		return result;
	}
	
	public static List<String> getCommonSearchEngineList(){
		List<String> result = new ArrayList<String>();
		result.add(GOOGLE);
		result.add(GITHUB);
		return result;
	}
}
