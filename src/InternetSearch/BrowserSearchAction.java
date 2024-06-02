package InternetSearch;

import java.awt.event.ActionEvent;
import java.net.URLEncoder;
import java.util.Base64;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;

import base.Commons;
import burp.BurpExtender;
import domain.target.TargetTableModel;
import title.LineTableModel;
import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.SystemUtils;



public class BrowserSearchAction extends AbstractAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1933197856582351336L;


	AbstractTableModel tableModel;
	int[] modelRows;
	int columnIndex;
	String engine;

	public BrowserSearchAction(AbstractTableModel tableModel, int[] modelRows, int columnIndex,String engine) {
		super();

		this.tableModel = tableModel;
		this.modelRows = modelRows;
		this.columnIndex = columnIndex;
		this.engine = engine;
		putValue(Action.NAME, "Search On "+engine.trim());
	}


	@Override
	public final void actionPerformed(ActionEvent e) {

		if (modelRows.length >=50) {
			BurpExtender.getStderr().print("too many items selected!! should less than 50");
			return;
		}

		for (int row:modelRows) {
			String searchType = null;
			String searchContent =null;

			if (tableModel.getClass().equals(LineTableModel.class)) {
				InfoTuple<String, String> result = ((LineTableModel) tableModel).getSearchTypeAndValue(row, columnIndex);
				searchType = result.first;
				searchContent = result.second;
			}

			if (tableModel.getClass().equals(SearchTableModel.class)) {
				InfoTuple<String, String> result = ((SearchTableModel) tableModel).getSearchTypeAndValue(row, columnIndex);
				searchType = result.first;
				searchContent = result.second;
			}

			if (tableModel.getClass().equals(TargetTableModel.class)) {
				InfoTuple<String, String> result = ((TargetTableModel) tableModel).getSearchTypeAndValue(row, columnIndex);
				searchType = result.first;
				searchContent = result.second;
			}

			if (StringUtils.isEmpty(searchContent) || StringUtils.isEmpty(searchType)) return;


			searchContent = SearchEngine.buildSearchDork(searchContent, engine, searchType);
			String url = buildSearchUrl(engine,searchContent);

			try {
				SystemUtils.browserOpen(url, null);
			} catch (Exception err) {
				err.printStackTrace(BurpExtender.getStderr());
			}
		}
	}


	public String buildSearchUrl(String engine,String searchContent) {
		searchContent = URLEncoder.encode(searchContent);
		String url = null;
		if (engine.equalsIgnoreCase(SearchEngine.GOOGLE)) {
			url= "https://www.google.com/search?q="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.GITHUB)) {
			url= "https://github.com/search?q=%22"+searchContent+"%22&type=Code";
		}


		else if (engine.equalsIgnoreCase(SearchEngine.FOFA)) {
			searchContent = new String(Base64.getEncoder().encode(searchContent.getBytes()));
			url= "https://fofa.info/result?qbase64="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.SHODAN)) {
			url= "https://www.shodan.io/search?query="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.QUAKE_360)) {
			url= "https://quake.360.net/quake/#/searchResult?searchVal="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.QIANXIN_TI)) {
			url= "https://ti.qianxin.com/v2/search?type=domain&value="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.TI_360)) {
			url= "https://ti.360.net/#/detailpage/searchresult?query="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.ZOOMEYE)) {
			url= "https://www.zoomeye.org/searchResult?q="+searchContent;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.QIANXIN_HUNTER)) {
			url= "https://hunter.qianxin.com/list?search="+searchContent;
		}

		//邮箱搜索
		else if (engine.equalsIgnoreCase(SearchEngine.HUNTER_IO)) {
			url= "https://hunter.io/try/search/"+searchContent;;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.ASN_INFO_BGP_HE_NET)) {
			//https://bgp.he.net/dns/shopee.com
			//https://bgp.he.net/net/143.92.111.0/24
			//https://bgp.he.net/ip/143.92.127.1
			if (IPAddressUtils.isValidIPv4NoPort(searchContent)){
				url = "https://bgp.he.net/ip/"+searchContent;
			}
			if (IPAddressUtils.isValidSubnet(searchContent)){
				url = "https://bgp.he.net/net/"+searchContent;
			}
			if (DomainUtils.isValidDomainNoPort(searchContent)){
				url = "https://bgp.he.net/dns/"+searchContent;
			}
		}

		//whois查询
		else if (engine.equalsIgnoreCase(SearchEngine.WHOIS)) {
			url= "https://www.whois.com/whois/"+searchContent;;
		}
		else if (engine.equalsIgnoreCase(SearchEngine.WHOIS_CHINAZ)) {
			url= "https://whois.chinaz.com/"+searchContent;;
		}
		return url;
	}

	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}
