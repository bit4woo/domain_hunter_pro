package title.search;

import java.util.regex.Pattern;

import title.LineEntry;

public class SearchRegex {

	public static final String REGEX = "REGEX";

	public static boolean isRegexSearch(String input) {
		input = input.toLowerCase();
		input = input.replaceAll("\\s", "");
		if (input.startsWith((REGEX+":").toLowerCase())) {
			return true;
		}
		return false;
	}

	public static String grepKeyword(String input) {
		input = input.replaceAll("\\s", "");

		String[] arr = input.split(":",2);//limit =2 分割成2份
		if (arr.length ==2) {
			String keyword = arr[1].trim();
			return keyword;
		}else {
			return input;
		}
	}

	/**
	 * 通过正则搜索，不应该进行大小写转换
	 * 
	 */
	public static boolean doFilter(LineEntry line,String regexInput) {
		if (isRegexSearch(regexInput)) {
			String regex =grepKeyword(regexInput);
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
				if (pRegex.matcher(line.getIPSet().toString()).find()) {
					return true;
				}
				if (pRegex.matcher(line.getCNAMESet().toString()).find()) {
					return true;
				}
				if (pRegex.matcher(line.getComments().toString()).find()) {
					return true;
				}
				if (pRegex.matcher(line.getASNInfo()).find()) {
					return true;
				}
				return false;
			}
		}else {
			//表达式不正确，默认显示，以便后续的过滤
			return true;
		}
	}
}
