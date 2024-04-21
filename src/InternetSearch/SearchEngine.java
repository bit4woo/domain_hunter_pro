package InternetSearch;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.AbstractTableModel;

import burp.BurpExtender;

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

	//https://search.censys.io/api
	//https://api-docs.fullhunt.io/#introduction
	//TODO
	public static final String CENSYS = "censys";
	public static final String FullHunt = "fullhunt";

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
		//result.add(SHODAN);//有cloudflare人机验证，不能直接调用，暂时剔除
		result.add(ZOOMEYE);
		result.add(QIANXIN_HUNTER);
		//result.add(QIANXIN_TI);
		result.add(QUAKE_360);
		//result.add(TI_360);
		result.add(CENSYS);
		result.add(FullHunt);
		return result;
	}

	public static List<String> getCommonSearchEngineList(){
		List<String> result = new ArrayList<String>();
		result.add(GOOGLE);
		result.add(GITHUB);
		return result;
	}


	//TODO 大量语法需要补全
	public static String buildSearchDork(String value,String engine,String type){
		if (SearchType.getSearchTypeList().contains(type)) {
			if (engine.equalsIgnoreCase(GOOGLE)) {
				//https://www.exploit-db.com/google-hacking-database
				if (type.equals(SearchType.Host)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.IP)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.SubDomain)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.Subnet)) {

				}else if(type.equals(SearchType.Title)) {
					value = "intitle:"+value;
				}else if(type.equals(SearchType.IconHash)) {

				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(FOFA)) {
				//https://en.fofa.info/api
				if (type.equals(SearchType.Host)) {
					value = String.format("host=\"%s\"", value);//查询结果比domain多一些
				} else if (type.equals(SearchType.IP)) {
					value = String.format("ip=\"%s\"", value);
				} else if (type.equals(SearchType.SubDomain)) {
					//value = String.format("domain=\"%s\"", value);
					//直接搜索原始字符串，反而有更多结果
				} else if(type.equals(SearchType.Subnet)) {
					value = String.format("ip=\"%s\"", value);
				} else if (type.equals(SearchType.Title)) {
					value = String.format("title=\"%s\"", value);
				} else if (type.equals(SearchType.IconHash)) {
					value = String.format("icon_hash=\"%s\"", value);
				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(ZOOMEYE)) {
				//https://www.zoomeye.hk/
				//title:"Cisco ASDM"
				//cidr:52.2.254.36/24
				//asn:42893
				//hostname:google.com
				//site:google.com--查子域名资产用这个
				//iconhash:"f3418a443e7d841097c714d69ec4bcb8"
				if (type.equals(SearchType.Host)) {
					value = "hostname:"+value;
				}else if(type.equals(SearchType.IP)) {
					value = String.format("ip:\"%s\"", value);
				}else if(type.equals(SearchType.SubDomain)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.Subnet)) {
					value = "cidr:"+value;
				}else if(type.equals(SearchType.Title)) {
					value = String.format("title:\"%s\"", value);
				}else if(type.equals(SearchType.IconHash)) {
					value = String.format("iconhash:\"%s\"", value);
				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(SHODAN)) {
				//https://beta.shodan.io/search/filters
				if (type.equals(SearchType.Host)) {

				}else if(type.equals(SearchType.IP)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.SubDomain)) {

				}else if(type.equals(SearchType.Subnet)) {

				}else if(type.equals(SearchType.Title)) {

				}else if(type.equals(SearchType.IconHash)) {

				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(QIANXIN_HUNTER)) {
				/**
				domain="qianxin.com"	
				搜索域名包含"qianxin.com"的网站 --这个可能包含第三方服务
				误报太多，会出现类似这样的域名： www.step-qianxin.com、www.webmail.qianxin.com.ar

				domain.suffix="qianxin.com" 
				搜索主域为"qianxin.com"的网站--使用这个

				header.server=="Microsoft-IIS/10"
				web.title="北京"
				web.body="网络空间测绘"
				ip="1.1.1.1"
				ip="220.181.111.0/24"
				web.icon="22eeab765346f14faf564a4709f98548"
				 */

				if (type.equals(SearchType.Host)) {

				}else if(type.equals(SearchType.IP)) {
					value = String.format("ip=\"%s\"", value);
				}else if(type.equals(SearchType.SubDomain)) {
					value = String.format("domain.suffix=\"%s\"", value);
				}else if(type.equals(SearchType.SimilarDomain)) {
					value = String.format("domain=\"%s\"", value);
				}else if(type.equals(SearchType.Subnet)) {
					value = String.format("ip=\"%s\"", value);
				}else if(type.equals(SearchType.Title)) {
					value = String.format("web.title=\"%s\"", value);
				}else if(type.equals(SearchType.IconHash)) {
					//这里的Hash及算法方法和fofa一致吗
					value = String.format("web.icon=\"%s\"", value);
				}else if(type.equals(SearchType.Server)) {
					value = String.format("header.server=\"%s\"", value);
				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(QIANXIN_TI)) {
				if (type.equals(SearchType.Host)) {

				}else if(type.equals(SearchType.IP)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.SubDomain)) {

				}else if(type.equals(SearchType.Subnet)) {

				}else if(type.equals(SearchType.Title)) {

				}else if(type.equals(SearchType.IconHash)) {

				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(TI_360)) {
				if (type.equals(SearchType.Host)) {

				}else if(type.equals(SearchType.IP)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.SubDomain)) {

				}else if(type.equals(SearchType.Subnet)) {

				}else if(type.equals(SearchType.Title)) {

				}else if(type.equals(SearchType.IconHash)) {

				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
			else if (engine.equalsIgnoreCase(QUAKE_360)) {
				//https://quake.360.net/quake/#/help?id=5e774244cb9954d2f8a0165a&title=%E6%9C%8D%E5%8A%A1%E6%95%B0%E6%8D%AE%E6%8E%A5%E5%8F%A3
				if (type.equals(SearchType.Host)) {

				}else if(type.equals(SearchType.IP)) {
					value = "site:"+value;
				}else if(type.equals(SearchType.SubDomain)) {
					value = "domain:"+value;
				}else if(type.equals(SearchType.Subnet)) {

				}else if(type.equals(SearchType.Title)) {

				}else if(type.equals(SearchType.IconHash)) {
					value = SearchType.IconHash+value;
					//语法上是不需要的，为了告诉请求构造逻辑，这是iconhash搜索。
				}else if(type.equals(SearchType.Server)) {

				}else if(type.equals(SearchType.Asn)) {

				}
			}
		}else {
			BurpExtender.getStderr().println("wrong search type");
		}
		return value;
	}


	public static void AddSearchMenuItems(JPopupMenu parentMenu,AbstractTableModel tableModel,int[] modelRows,int columnIndex) {
		JMenu BrowserAssetSearchMenu = new JMenu("Asset Search");
		List<JMenuItem> BrowserAssetSearchItems = new ArrayList<>();
		for (String engine:SearchEngine.getAssetSearchEngineList()) {//浏览器资产搜索
			JMenuItem Item = new JMenuItem(new BrowserSearchAction(tableModel,modelRows,columnIndex,engine));
			BrowserAssetSearchMenu.add(Item);
			BrowserAssetSearchItems.add(Item);
		}


		JMenu CommonSearchMenu = new JMenu("Common Search");
		for (String engine:SearchEngine.getCommonSearchEngineList()) {//通用搜索引擎和GitHub
			JMenuItem Item = new JMenuItem(new BrowserSearchAction(tableModel,modelRows,columnIndex,engine));
			CommonSearchMenu.add(Item);
		}


		JMenu APIAssetSearchMenu = new JMenu("API Asset Search");
		List<JMenuItem> APIAssetSearchItems = new ArrayList<>();
		for (String engine:SearchEngine.getAssetSearchEngineList()) {
			JMenuItem Item = new JMenuItem(new APISearchAction(tableModel,modelRows,columnIndex,engine));
			APIAssetSearchMenu.add(Item);
			APIAssetSearchItems.add(Item);
		}


		JMenu EmailSearchMenu = new JMenu("Email Search");
		for (String engine:SearchEngine.getEmailSearchEngineList()) {
			JMenuItem item = new JMenuItem(new BrowserSearchAction(tableModel,modelRows,columnIndex,engine));
			EmailSearchMenu.add(item);

			JMenuItem itemAPI = new JMenuItem(new APISearchAction(tableModel,modelRows,columnIndex,engine));
			EmailSearchMenu.add(itemAPI);
		}


		JMenu ExtendInfoSearchMenu = new JMenu("Extend Info Search");
		for (String engine:SearchEngine.getExtendInfoSearchEngineList()) {
			JMenuItem item = new JMenuItem(new BrowserSearchAction(tableModel,modelRows,columnIndex,engine));
			ExtendInfoSearchMenu.add(item);
		}


		JMenuItem BrowserSearchAllItem = new JMenuItem(new AbstractAction("Asset Search All In One") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (JMenuItem item:BrowserAssetSearchItems) {
					item.doClick();
				}
			}
		});

		JMenuItem APISearchAllItem = new JMenuItem(new AbstractAction("API Asset Search All In One") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				APISearchAction action = new APISearchAction(tableModel, modelRows, columnIndex, SearchEngine.getAssetSearchEngineList());
				action.actionPerformed(actionEvent);
			}
		});


		parentMenu.add(APISearchAllItem);
		parentMenu.add(BrowserSearchAllItem);

		parentMenu.addSeparator();

		parentMenu.add(APIAssetSearchMenu);
		parentMenu.add(BrowserAssetSearchMenu);
		parentMenu.add(CommonSearchMenu);
		parentMenu.add(EmailSearchMenu);
		parentMenu.add(ExtendInfoSearchMenu);
	}
}
