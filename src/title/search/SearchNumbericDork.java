package title.search;

import burp.BurpExtender;
import title.LineEntry;

public enum SearchNumbericDork {

	PORT,
	STATUS,
	LENGTH;

	private static String dork = "";
	private static String condition = "";
	private static int value = -999;

	public static final String[] compareStrings = {">=","<=","==","!=",  "=","<",">"};

	public static void main(String args[]) {

	}

	public static boolean isNumbericDork(String input) {
		input = input.toLowerCase();
		input = input.replaceAll("\\s", "");
		SearchNumbericDork[] values = SearchNumbericDork.values();
		for (SearchNumbericDork value:values) {
			String valueStr = value.toString().toLowerCase();
			for (String compare:compareStrings) {
				if (input.startsWith(valueStr+compare)) {
					return true;
				}
			}
		}
		return false;
	}



	public static String  inputClean(String input){
		//为了处理输入是"dork:12345"的情况，下面的这种写法其实不严谨，中间也可能有引号，不过应付一般的搜索足够了。
		input = input.replaceAll("\"", "");
		input = input.replaceAll("\'", "");
		input = input.replaceAll("\\s", "");
		return input;
	}


	public static boolean parse(String input) {
		input = inputClean(input);
		for (String compare:compareStrings) {
			if (input.contains(compare)) {
				try {
					String[] arr = input.split(compare,2);//limit =2 分割成2份
					if (arr.length ==2) {
						dork = arr[0].trim();//limit =2 分割成2份
						condition = compare;
						value = Integer.parseInt(arr[1].trim());
						return true;
					}
				} catch (NumberFormatException e) {
					e.printStackTrace(BurpExtender.getStderr());
				}
			}
		}
		return false;
	}


	/**
	 * //{">=","<=","==","!=",  "=","<",">"};
	 * @param number
	 * @return
	 */
	public static boolean compare(int number) {
		if (condition.equals(">=")) {
			if (number >= value) {
				return true;
			}else {
				return false;
			}
		}

		if (condition.equals("<=")) {
			if (number <= value) {
				return true;
			}else {
				return false;
			}
		}

		if (condition.equals("==") || condition.equals("=")) {
			if (number == value) {
				return true;
			}else {
				return false;
			}
		}

		if (condition.equals("!=")) {
			if (number != value) {
				return true;
			}else {
				return false;
			}
		}

		if (condition.equals("<")) {
			if (number < value) {
				return true;
			}else {
				return false;
			}
		}

		if (condition.equals(">")) {
			if (number > value) {
				return true;
			}else {
				return false;
			}
		}else {
			//表达式不正确，默认显示，以便后续的过滤
			return true;
		}
	}

	public static boolean doFilter(LineEntry line,String input) {
		if (isNumbericDork(input)){
			if (!parse(input)) {
				//解析失败，不是合法的搜索条件，全显示
				return true;
			}

			if (dork.equalsIgnoreCase(PORT.toString())) {
				return compare(line.getPort());
			}

			if (dork.equalsIgnoreCase(STATUS.toString())) {
				return compare(line.getStatuscode());
			}

			if (dork.equalsIgnoreCase(LENGTH.toString())) {
				return compare(line.getContentLength());
			}
		}
		//表达式不正确，默认显示，以便后续的过滤
		return true;
	}

}
