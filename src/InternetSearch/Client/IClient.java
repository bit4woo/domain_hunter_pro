package InternetSearch.Client;

import java.util.List;

import InternetSearch.SearchResultEntry;


@Deprecated
public interface IClient {
	
	public String getEngineName();

	public String Search(String searchContent);
	
	public List<SearchResultEntry> Search1(String searchContent);

	public List<SearchResultEntry> parseResp(String respbody);
	
	public boolean hasNextPage(String respbody);

	public String buildSearchUrl(String searchContent, int page); 

	public byte[] buildRawData(String searchContent,int page);

	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.toLowerCase().substring(1);
	}
}
