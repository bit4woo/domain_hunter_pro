package InternetSearch;

import java.util.Objects;

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
	
	/**
	 * Java 的去重必须同时满足：
		hashCode() 相等
		equals() 返回 true
	 */
	@Override
	public int hashCode() {
		return getTabName().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (o == null || getClass() != o.getClass()) return false;
	    ToSearchItem item = (ToSearchItem) o;
	    return Objects.equals(getTabName(), item.getTabName());
	}
}