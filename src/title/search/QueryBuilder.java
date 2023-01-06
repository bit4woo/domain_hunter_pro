package title.search;

import java.net.URL;
import java.util.ArrayList;

import title.LineEntry;
import title.TitlePanel;

/**
 * 根据当前title panel中的条件构造出从数据库中查询内容的SQL语句。 
 *
 */
public class QueryBuilder {
	private TitlePanel titlePanel;

	public QueryBuilder(TitlePanel panel){
		this.titlePanel = panel;
	}
	
	public String oneCondition(String Input,boolean caseSensitive) {
		Input = Input.trim();//应该去除空格，符合java代码编写习惯
		if (SearchDork.isDork(Input)) {
			//stdout.println("do dork search,dork:"+dork+"   keyword:"+keyword);
			return dorkFilter(Input,caseSensitive);
		}else {
			return textFilter(Input,caseSensitive);
		}
	}
	
	public String buildWhere() {
		ArrayList<String> AllConditions = new ArrayList<String>();
		String statusCondition = entryNeedToShow();
		
		AllConditions.add("("+statusCondition+")");
		
		SearchTextField searchTextField = titlePanel.getTextFieldSearch();
		
		boolean caseSensitive = searchTextField.isCaseSensitive();
		String searchContent = searchTextField.getText();

		//目前只处理&&（and）逻辑的表达式
		String[] searchConditions = searchContent.split("&&");
		for (String condition:searchConditions) {
			String tmpcondition = oneCondition(condition,caseSensitive);
			if (!tmpcondition.trim().equals("")) {
				AllConditions.add("("+tmpcondition+")");
			}
		}
		String where = String.join(" and ", AllConditions);
		where = " where " +where +" limit 100";
		return where;
	}
	
	//根据状态过滤
	public String entryNeedToShow() {
		if (!(titlePanel.getRdbtnUnCheckedItems().isSelected()||titlePanel.getRdbtnCheckingItems().isSelected()||
				titlePanel.getRdbtnCheckedItems().isSelected()||titlePanel.getRdbtnMoreActionItems().isSelected())) {
			//全部未选中时，全部返回。为了满足用户习惯全部未选择时全部返回，
			return "";
		}
		
		ArrayList<String> conditions = new ArrayList<String>();

		if (titlePanel.getRdbtnCheckedItems().isSelected()) {
			conditions.add(String.format("CheckStatus = '%s' ",LineEntry.CheckStatus_Checked));
		}

		if (titlePanel.getRdbtnCheckingItems().isSelected()) {
			conditions.add(String.format("CheckStatus = '%s' ",LineEntry.CheckStatus_Checking));
		}

		if (titlePanel.getRdbtnUnCheckedItems().isSelected()) {
			conditions.add(String.format("CheckStatus = '%s' ",LineEntry.CheckStatus_UnChecked));
		}
		
		if (titlePanel.getRdbtnMoreActionItems().isSelected()) {
			conditions.add(String.format("CheckStatus = '%s' ",LineEntry.CheckStatus_MoreAction));
		}
		
		return String.join(" or ", conditions);
	}
	
	/**
	 * 关键词和搜索内容都进行了小写转换，尽量多地返回内容
	 * @param line
	 * @param keyword
	 * @return
	 */
	public static String textFilter(String keyword,boolean caseSensitive) {
		if (keyword ==null || keyword.length() == 0) {
			return "";
		}else {//全局搜索
			ArrayList<String> conditions = new ArrayList<String>();
			
			String[] columns = ("url,title,webcontainer,"
					+ "IPSet,CNAMESet,CertDomainSet,icon_hash,ASNInfo,"
					+ "request,response,comments").split(",");
			for (String column:columns) {
				if (caseSensitive) {
					conditions.add("request like '%"+keyword +"%'");
				}else {
					conditions.add("lower(request) like '%"+keyword.toLowerCase()+"%'");
				}
			}

			return String.join(" or ", conditions);
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
	public static String dorkFilter(String input,boolean caseSensitive) {
		if (SearchDork.isDork(input)){
			String dork = SearchDork.grepDork(input);
			String keyword = SearchDork.grepKeyword(input);

			if (keyword ==null || keyword.length() == 0) {
				return "";
			}
			
			//正则搜索
			if (dork.equalsIgnoreCase(SearchDork.REGEX.toString())) {
				return "";
				//TODO
//				return regexFilter(keyword);
			}
			
			//dork搜索
			boolean isNotCondition = false;
			if (keyword.startsWith("!")){
				isNotCondition = true;
				keyword = keyword.replaceFirst("!", "");
			}
			
			if (dork.equalsIgnoreCase(SearchDork.PORT.toString())) {
				try {
					if (isNotCondition) {
						return "port != "+Integer.parseInt(keyword);
					}else {
						return "port = "+Integer.parseInt(keyword);
					}
				} catch (NumberFormatException e) {
					//e.printStackTrace();
					return "";
				}
			}
			
			if (dork.equalsIgnoreCase(SearchDork.STATUS.toString())) {
				try {
					if (isNotCondition) {
						return "statuscode != "+Integer.parseInt(keyword);
					}else {
						return "statuscode = "+Integer.parseInt(keyword);
					}
				} catch (NumberFormatException e) {
					//e.printStackTrace();
					return "";
				}
			}

			String columnName = "";
			
			if (dork.equalsIgnoreCase(SearchDork.HOST.toString())) {
				columnName = "host";
			}
			
			if (dork.equalsIgnoreCase(SearchDork.URL.toString())) {
				columnName = "url";
			}

			if (dork.equalsIgnoreCase(SearchDork.REQUEST.toString())) {
				columnName = "request";
			}

			if (dork.equalsIgnoreCase(SearchDork.RESPONSE.toString())) {
				columnName = "response";
			}

			if (dork.equalsIgnoreCase(SearchDork.COMMENT.toString())) {
				columnName = "comments";
			}
			
			if (dork.equalsIgnoreCase(SearchDork.TITLE.toString())) {
				columnName = "title";
			}

			if (dork.equalsIgnoreCase(SearchDork.ASNINFO.toString())) {
				columnName = "ASNInfo";
			}
			
			if (columnName.length()>=3){
				if (caseSensitive) {
					if (isNotCondition) {
						return columnName+" not like '%"+keyword+"%'"; 
					}
					return columnName+" like '%"+keyword+"%'"; 
				}else {
					if (isNotCondition) {
						return "lower("+columnName+") not like '%"+keyword.toLowerCase()+"%'"; 
					}
					return "lower("+columnName+") like '%"+keyword.toLowerCase()+"%'"; 
				}
			}
		}
		return "";
	}
	
//	/**
//	 * 通过正则搜索，不应该进行大小写转换
//	 * 
//	 */
//	public static boolean regexFilter(String regex) {
//		//BurpExtender.getStdout().println("regexFilte: "+regex);
//		Pattern pRegex = Pattern.compile(regex);
//
//		if (regex.trim().length() == 0) {
//			return true;
//		} else {
//			if (pRegex.matcher(new String(line.getRequest())).find()) {
//				return true;
//			}
//			if (pRegex.matcher(new String(line.getResponse())).find()) {
//				return true;
//			}
//			if (pRegex.matcher(line.getUrl()).find()) {
//				return true;
//			}
//			if (pRegex.matcher(line.getIPSet().toString()).find()) {
//				return true;
//			}
//			if (pRegex.matcher(line.getCNAMESet().toString()).find()) {
//				return true;
//			}
//			if (pRegex.matcher(line.getComments().toString()).find()) {
//				return true;
//			}
//			if (pRegex.matcher(line.getASNInfo()).find()) {
//				return true;
//			}
//			return false;
//		}
//	}
	
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
