package title.search;

import java.net.URL;
import java.util.ArrayList;

import title.LineEntry;
import title.TitlePanel;

public class SearchManager {

	private TitlePanel titlePanel;

	public SearchManager(TitlePanel panel){
		this.titlePanel = panel;
	}


	public boolean include(LineEntry line,String searchInput,boolean caseSensitive) {

		//第一层判断，根据按钮状态进行判断，如果为true，进行后面的逻辑判断，false直接返回。
		if (!entryNeedToShow(line)) {
			return false;
		}

		//目前只处理&&（and）逻辑的表达式
		if (searchInput.contains("&&")) {
			String[] searchConditions = searchInput.split("&&");
			for (String condition:searchConditions) {
				if (oneCondition(condition,line,caseSensitive)) {
					continue;
				}else {
					return false;
				}
			}
			return true;
		}else if (searchInput.contains("||")) {
			String[] searchConditions = searchInput.split("\\|\\|");
			for (String condition:searchConditions) {
				if (oneCondition(condition,line,caseSensitive)) {
					return true;
				}
			}
			return false;
		}
		else {
			return oneCondition(searchInput,line,caseSensitive);
		}
	}


	public boolean oneCondition(String Input,LineEntry line,boolean caseSensitive) {
		Input = Input.trim();//应该去除空格，符合java代码编写习惯

		if (SearchStringDork.isStringDork(Input)) {
			//stdout.println("do dork search,dork:"+dork+"   keyword:"+keyword);
			return SearchStringDork.doFilter(line,Input,caseSensitive);

		}else if (SearchNumbericDork.isNumbericDork(Input)) {
			return SearchNumbericDork.doFilter(line,Input);
		}else if (SearchRegex.isRegexSearch(Input)) {
			return SearchRegex.doFilter(line,Input);
		}else {
			return SearchManager.textFilter(line,Input,caseSensitive);
		}
	}

	//根据状态过滤
	public boolean entryNeedToShow(LineEntry entry) {
		if (!(titlePanel.getRdbtnUnCheckedItems().isSelected()||titlePanel.getRdbtnCheckingItems().isSelected()||
				titlePanel.getRdbtnCheckedItems().isSelected()||titlePanel.getRdbtnMoreActionItems().isSelected())) {
			//全部未选中时，全部返回。一来为了满足用户习惯全部未选择时全部返回，
			//二来是为了解决之前乱改CheckStatus常理带来的bug，之前CheckStatus_Checked == "Checked",现在CheckStatus_Checked== "done"导致选中checked的时候，Checked的那部分就不会被显示出来。
			return true;
		}

		if (titlePanel.getRdbtnCheckedItems().isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_Checked)) {
			return true;
		}

		if (titlePanel.getRdbtnCheckingItems().isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_Checking)) {
			return true;//小心 == 和 equals的区别，之前这里使用 ==就导致了checking状态的条目的消失。
		}

		if (titlePanel.getRdbtnUnCheckedItems().isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_UnChecked)) {
			return true;
		}

		if (titlePanel.getRdbtnMoreActionItems().isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_MoreAction)) {
			return true;
		}

		return false;
	}

	/**
	 * 关键词和搜索内容都进行了小写转换，尽量多得返回内容
	 * @param line
	 * @param keyword
	 * @return
	 */
	public static boolean textFilter(LineEntry line,String keyword,boolean caseSensitive) {
		if (keyword.length() == 0) {
			return true;
		}else {//全局搜索
			ArrayList<String> contentList = new ArrayList<String>();
			contentList.add(new String(line.getRequest()));
			contentList.add(new String(line.getResponse()));
			if (line.getEntryType().equals(LineEntry.EntryType_Web)) {
				contentList.add(line.fetchUrlWithCommonFormate());
				//之前这里有个bug，如果用了上面这段代码，数据库更新就会失败！why？
				//因为之前的fetchUrlWithCommonFormate()实现修改了URL的格式导致，where条件无法匹配。
			}else {
				contentList.add(line.getUrl());//本质是domain name
			}
			contentList.add(line.getIPSet().toString());
			contentList.add(line.getCNAMESet().toString());
			contentList.add(line.getCertDomainSet().toString());
			contentList.add(line.getComments().toString());
			contentList.add(line.getTitle());
			contentList.add(line.getIcon_hash());
			contentList.add(line.getASNInfo());
			if (caseSensitive) {
				for(String item:contentList) {
					if (item == null) continue;
					if (item.contains(keyword)) {
						return true;
					}
				}
			}else {
				keyword = keyword.toLowerCase();
				for(String item:contentList) {
					if (item == null) continue;
					if (item.toLowerCase().contains(keyword)) {
						return true;
					}
				}
			}
			return false;
		}
	}


	public static void test(){
		String title = "标题";
		System.out.println(title.toLowerCase().contains("标题"));

		//		String webpack_PATTERN = "app\\.([0-9a-z])*\\.js";//后文有小写转换
		//		System.out.println(webpack_PATTERN);
		//
		//		System.out.println("regex:app\\.([0-9a-z])*\\.js");
		//
		//		System.out.println(SearchDork.REGEX.toString()+":"+webpack_PATTERN);
	}
	public static void test1() throws Exception {
		System.out.println(new URL("https://partner.airpay.in.th/").equals(new URL("https://partner.airpay.in.th:443/")));
	}

	public static void main(String[] args) throws Exception {
		test1();
	}
}
