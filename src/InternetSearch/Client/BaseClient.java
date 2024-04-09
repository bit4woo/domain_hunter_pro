package InternetSearch.Client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import InternetSearch.HttpClientOfBurp;
import InternetSearch.SearchResultEntry;
import burp.BurpExtender;
import utils.URLUtils;

/**
 * 什么时候使用Interface，什么时候使用abstract class?
 * 
 * 当所有函数都需要自行实现时，使用Interface
 * 当部分函数的逻辑是确定的，有共享的函数、属性时，使用抽象类。
 *
 */
public abstract class BaseClient {
	

	public abstract String getEngineName();

	/**
	 * 返回body 的list
	 * @param searchContent
	 * @return
	 */
	public List<String> Search(String searchContent) {
		List<String> resp_bodies = new ArrayList<>();
		int page=1;
		while (true) {
			try {
				String url = buildSearchUrl(searchContent, page);
				byte[] raw = buildRawData(searchContent, page);
				if (URLUtils.isVaildUrl(url)) {
					String body = HttpClientOfBurp.doRequest(new URL(url),raw);
					
					if (body.length()<=0) {
						break;
					} else {
						resp_bodies.add(body);
						if (hasNextPage(body,page)) {
							page++;
							continue;
						}else {
							break;
						}
					}
				}else {
					BurpExtender.getStderr().println(this.getClass()+" invalid URL to search: "+url);
					break;
				}
			} catch (Exception e) {
				e.printStackTrace(BurpExtender.getStderr());
				break;
			}
		}
		return resp_bodies;
	}
	
	public List<SearchResultEntry> SearchToGetEntry(String searchContent){
		List<SearchResultEntry> result = new ArrayList<>();
		List<String> resp_bodies = Search(searchContent);
		for (String body:resp_bodies) {
			List<SearchResultEntry> tmp_result = parseResp(body);
			if (tmp_result != null && tmp_result.size()>0) {
				result.addAll(tmp_result);
			}
		}
		return result;
	};

	public abstract List<SearchResultEntry> parseResp(String respbody);
	
	public abstract boolean hasNextPage(String respbody,int currentPage);

	public abstract String buildSearchUrl(String searchContent, int page); 

	public abstract byte[] buildRawData(String searchContent,int page);
}
