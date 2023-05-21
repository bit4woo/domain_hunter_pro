package title.search;

import title.LineEntry;

public enum SearchStringDork {

	HOST,
	URL,
	REQUEST,
	RESPONSE,
	COMMENT,
	TITLE,
	ASNINFO,
	SOURCE,
	SERVER;

	//REGEX;//这个用于正则搜索


	SearchStringDork(){

	}
	/**
	 * 判断是否是dork搜索语句,未区分大小写,即HOST: host:是同样效果
	 * @param input
	 * @return
	 */
	public static boolean isStringDork(String input) {
		input = input.toLowerCase();
		SearchStringDork[] values = SearchStringDork.values();
		for (SearchStringDork value:values) {
			String valueStr = value.toString().toLowerCase();
			if (input.startsWith(valueStr+":")) {
				return true;
			}
		}
		return false;
	}


	public static String  inputClean(String input){
		input = input.replaceAll("\"", "");
		input = input.replaceAll("\'", "");
		input = input.replaceAll("\\s", "");
		return input;
	}


	public static String grepDork(String input) {

		input = inputClean(input);

		String[] arr = input.split(":",2);//limit =2 分割成2份
		if (arr.length ==2) {
			String dork = arr[0].trim();
			return dork;
		}else {
			return "";
		}
	}

	public static String grepKeyword(String input) {
		input = inputClean(input);

		String[] arr = input.split(":",2);//limit =2 分割成2份
		if (arr.length ==2) {
			String keyword = arr[1].trim();
			return keyword;
		}else {
			return input;
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
	public static boolean doFilter(LineEntry line,String input,boolean caseSensitive) {
		if (isStringDork(input)){
			String dork = grepDork(input);
			String keyword = grepKeyword(input);

			if (keyword.length() == 0) {
				return true;
			}

			String tempContent = "";

			if (dork.equalsIgnoreCase(HOST.toString())) {
				tempContent = line.getHost();
			}

			if (dork.equalsIgnoreCase(URL.toString())) {
				tempContent = line.getUrl();
			}

			if (dork.equalsIgnoreCase(REQUEST.toString())) {
				tempContent = new String(line.getRequest());
			}

			if (dork.equalsIgnoreCase(RESPONSE.toString())) {
				tempContent = new String(line.getResponse());
			}

			if (dork.equalsIgnoreCase(COMMENT.toString())) {
				tempContent = line.getComments().toString();
			}

			if (dork.equalsIgnoreCase(TITLE.toString())) {
				tempContent = line.getTitle();
			}

			if (dork.equalsIgnoreCase(ASNINFO.toString())) {
				tempContent = line.getASNInfo();
			}

			if (dork.equalsIgnoreCase(SOURCE.toString())) {
				tempContent = line.getEntrySource();
			}
			
			if (dork.equalsIgnoreCase(SERVER.toString())) {
				tempContent = line.getEntrySource();
			}

			if (caseSensitive) {
				return tempContent.contains(keyword); 
			}else {
				keyword = keyword.toLowerCase();
				return tempContent.toLowerCase().contains(keyword); 
			}
		}
		//表达式不正确，默认显示，以便后续的过滤
		return true;
	}


	public static void main(String args[]) {
	}
}
