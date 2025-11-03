package InternetSearch;

public class ToSearchItem {
	String searchType = "";
	String searchContent = "";

	public ToSearchItem(String searchType, String searchContent) {
		this.searchType = searchType;
		this.searchContent = searchContent;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getSearchContent() {
		return searchContent;
	}

	public void setSearchContent(String searchContent) {
		this.searchContent = searchContent;
	}

	public String getTabName() {
		String tabname = String.format("%s(%s)", searchType, searchContent);
		return tabname;
	}

	@Override
	public int hashCode() {
		return getTabName().hashCode();
	}
}