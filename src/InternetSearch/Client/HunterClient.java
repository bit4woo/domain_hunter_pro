package InternetSearch.Client;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import Tools.JSONHandler;
import burp.BurpExtender;
import config.ConfigPanel;

public class HunterClient extends BaseClient {


	@Override
	public String getEngineName() {
		return SearchEngine.QIANXIN_HUNTER;
	}

	
	/**
	 * 样例数据
	 * {
    "code": 200,
    "data": {
        "account_type": "个人账号",
        "total": 192,
        "time": 55,
        "arr": [
            {
                "is_risk": "",
                "url": "http://www.xxx.com:2082",
                "ip": "xxx.xx.xxx.xxx",
                "port": 2082,
                "web_title": "Sign in",
                "domain": "www.xxx.com",
                "is_risk_protocol": "",
                "protocol": "http",
                "base_protocol": "tcp",
                "status_code": 200,
                "component": [
                    {
                        "name": "Cloudflare",
                        "version": ""
                    }
                ],
                "os": "",
                "company": "",
                "number": "",
                "country": "美国",
                "province": "",
                "city": "",
                "updated_at": "2024-03-25",
                "is_web": "是",
                "as_org": "Cloudflare, Inc.",
                "isp": "Cloudflare, Inc.",
                "banner": "HTTP/1.1 400 Bad Request",
                "vul_list": ""
            }
        ]
	    }
	}
	 */
	@Override
	public List<SearchResultEntry> parseResp(String respbody) {
		List<SearchResultEntry> result = new ArrayList<SearchResultEntry>();
		try {
			JSONObject obj = new JSONObject(respbody);
			int code = obj.getInt("code");
			if (code ==200) {
				JSONObject data = obj.getJSONObject("data");
				JSONArray items = data.getJSONArray("arr");
				for (Object item : items) {				
					JSONObject entryitem = (JSONObject) item;
					SearchResultEntry entry = new SearchResultEntry();
					entry.setHost(entryitem.getString("url"));
					entry.getIPSet().add(entryitem.getString("ip"));
					entry.setRootDomain(entryitem.getString("domain"));
					entry.setPort(entryitem.getInt("port"));
					entry.setProtocol(entryitem.getString("protocol"));
					entry.setWebcontainer(entryitem.get("component").toString());
					entry.setSource(getEngineName());
					result.add(entry);
				}
			}else {
				BurpExtender.getStderr().println(respbody);
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
		return result;
	}

	@Override
	public boolean hasNextPage(String respbody,int currentPage) {
		try {
			ArrayList<String> result = JSONHandler.grepValueFromJson(respbody, "total");
			if (result.size() >= 1) {
				int total = Integer.parseInt(result.get(0));
				if (total > currentPage * 100) {
					return true;
				} 
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
		return false;
	}

	@Override
	public String buildSearchUrl(String searchContent, int page) {
		String key = ConfigPanel.textFieldHunterAPIKey.getText();
		String domainBase64 = new String(Base64.getEncoder().encode(searchContent.getBytes()));
		String url = String.format(
				"https://hunter.qianxin.com/openApi/search?&api-key=%s&search=%s&page=%s&page_size=100", key,
				domainBase64, page);
		return url;
	}

	@Override
	public byte[] buildRawData(String searchContent, int page) {
		return null;
	}
	
	public static void main(String[] args) {
		String aaa = "";
		System.out.println(new HunterClient().parseResp(aaa));
	}
}
