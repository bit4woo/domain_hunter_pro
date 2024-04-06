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
