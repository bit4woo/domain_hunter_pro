package title.search;

import title.LineEntry;
import title.TitlePanel;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class LineSearch {
	
	//根据状态过滤
	public static boolean entryNeedToShow(LineEntry entry) {
		if (!(TitlePanel.rdbtnCheckedItems.isSelected()||TitlePanel.rdbtnCheckingItems.isSelected()||
				TitlePanel.rdbtnUnCheckedItems.isSelected()||TitlePanel.rdbtnMoreActionItems.isSelected())) {
			//全部未选中时，全部返回。一来为了满足用户习惯全部未选择时全部返回，
			//二来是为了解决之前乱改CheckStatus常理带来的bug，之前CheckStatus_Checked == "Checked",现在CheckStatus_Checked== "done"导致选中checked的时候，Checked的那部分就不会被显示出来。
			return true;
		}

		if (TitlePanel.rdbtnCheckedItems.isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_Checked)) {
			return true;
		}

		if (TitlePanel.rdbtnCheckingItems.isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_Checking)) {
			return true;//小心 == 和 equals的区别，之前这里使用 ==就导致了checking状态的条目的消失。
		}

		if (TitlePanel.rdbtnUnCheckedItems.isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_UnChecked)) {
			return true;
		}
		
		if (TitlePanel.rdbtnMoreActionItems.isSelected()&& entry.getCheckStatus().equals(LineEntry.CheckStatus_MoreAction)) {
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
			contentList.add(line.fetchUrlWithCommonFormate());
			contentList.add(line.getIP());
			contentList.add(line.getCDN());
			contentList.add(line.getComment());
			contentList.add(line.getTitle());
			if (caseSensitive) {
				for(String item:contentList) {
					if (item.contains(keyword)) {
						return true;
					}
				}
			}else {
				keyword = keyword.toLowerCase();
				for(String item:contentList) {
					if (item.toLowerCase().contains(keyword)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * 支持部分类似google dork的搜索语法。对搜索内容和条件都进行了小写转换，尽量多的返回内容。
	 * Host url header body request response comment
	 * host:www.baidu.com ----host是dork,www.baidu.com是keyword
	 * @param line
	 * @param input
	 * @return
	 */
	public static boolean dorkFilter(LineEntry line,String input,boolean caseSensitive) {
		if (SearchDork.isDork(input)){
			String dork = SearchDork.grepDork(input);
			String keyword = SearchDork.grepKeyword(input);

			if (keyword.length() == 0) {
				return true;
			}

			if (dork.equalsIgnoreCase(SearchDork.REGEX.toString())) {
				return regexFilter(line,keyword);
			}

			if (dork.equalsIgnoreCase(SearchDork.PORT.toString())) {
				if (line.getPort() == Integer.parseInt(keyword)) {
					return true;
				}else {
					return false;
				}
			}

			if (dork.equalsIgnoreCase(SearchDork.STATUS.toString())) {
				if (line.getStatuscode() == Integer.parseInt(keyword)) {
					return true;
				}else {
					return false;
				}
			}

			String tempContent = "";
			
			if (dork.equalsIgnoreCase(SearchDork.HOST.toString())) {
				tempContent = line.getHost();
			}
			
			if (dork.equalsIgnoreCase(SearchDork.URL.toString())) {
				tempContent = line.getUrl();
			}

			if (dork.equalsIgnoreCase(SearchDork.REQUEST.toString())) {
				tempContent = new String(line.getRequest());
			}

			if (dork.equalsIgnoreCase(SearchDork.RESPONSE.toString())) {
				tempContent = new String(line.getResponse());
			}

			if (dork.equalsIgnoreCase(SearchDork.COMMENT.toString())) {
				tempContent = new String(line.getComment());
			}
			
			if (dork.equalsIgnoreCase(SearchDork.TITLE.toString())) {
				tempContent = new String(line.getTitle());
			}
			
			if (caseSensitive) {
				return tempContent.contains(keyword); 
			}else {
				keyword = keyword.toLowerCase();
				return tempContent.toLowerCase().contains(keyword); 
			}
		}
		return false;
	}
	/**
	 * 通过正则搜索，不应该进行大小写转换
	 * 
	 */
	public static boolean regexFilter(LineEntry line,String regex) {
		//BurpExtender.getStdout().println("regexFilte: "+regex);
		Pattern pRegex = Pattern.compile(regex);

		if (regex.trim().length() == 0) {
			return true;
		} else {
			if (pRegex.matcher(new String(line.getRequest())).find()) {
				return true;
			}
			if (pRegex.matcher(new String(line.getResponse())).find()) {
				return true;
			}
			if (pRegex.matcher(line.getUrl()).find()) {
				return true;
			}
			if (pRegex.matcher(line.getIP()).find()) {
				return true;
			}
			if (pRegex.matcher(line.getCDN()).find()) {
				return true;
			}
			if (pRegex.matcher(line.getComment()).find()) {
				return true;
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
