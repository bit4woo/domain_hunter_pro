package title.search;

public enum SearchDork {

	HOST("HOST"),
	PORT("PORT"),
	URL("URL"),
	STATUS("STATUS"),
	REQUEST("REQUEST"),
	RESPONSE("RESPONSE"),
	COMMENT("COMMENT"),
	TITLE("TITLE"),

	REGEX("REGEX");//这个用于正则搜索


	private String dork;

	SearchDork(String dork){
		this.dork = dork;
	}
	/**
	 * 判断是否是dork搜索语句,未区分大小写,即HOST: host:是同样效果
	 * @param input
	 * @return
	 */
	public static boolean isDork(String input) {
		SearchDork[] values = SearchDork.values();
		for (SearchDork value:values) {
			if (input.toLowerCase().startsWith(value.toString().toLowerCase()+":")) {
				return true;
			}
		}
		return false;
	}

	@Deprecated
	public static boolean isTagDork(String input) {
		SearchDork[] values = SearchDork.values();
		if (isRegDork(input)) {
			return false;
		}

		for (SearchDork value:values) {
			if (value.toString().equalsIgnoreCase(input)) {
				return true;
			}
		}
		return false;
	}

	@Deprecated
	public static boolean isRegDork(String input) {
		if (REGEX.toString().equalsIgnoreCase(input)) {
			return true;
		}else {
			return false;
		}
	}
	public static String  inputClean(String input){
		if (input.contains("\"") || input.contains("\'")){
			//为了处理输入是"dork:12345"的情况，下面的这种写法其实不严谨，中间也可能有引号，不过应付一般的搜索足够了。
			input = input.replaceAll("\"", "");
			input = input.replaceAll("\'", "");
		}
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

	public static void main(String args[]) {
		isTagDork("aaa");
	}
}
