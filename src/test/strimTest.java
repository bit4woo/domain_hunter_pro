import java.util.ArrayList;
import java.util.List;


public class strimTest {
	public static void main(String[] args) {
		test1();
	}

	public static void test() {
		try {
			String aaa = "xxxxx.com              ";
			ArrayList<String> result = new ArrayList<String>();
			List<String> items = new ArrayList<String>();
			items.add(aaa);
			for (String item:items) {
				try {
//					item = item.strip();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					item = item.trim();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println(item);
				result.add(item);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void test1() {
		try {
			String aaa = "xxxx163.com              ";
			ArrayList<String> result = new ArrayList<String>();
			List<String> items = new ArrayList<String>();
			// 将字符串中的特殊空格字符替换为标准空格字符
			aaa = aaa.replace("\u00A0", " ").replace("\u2002", " ").replace("\u2003", " ").replace("\u2004", " ").replace("\u2005", " ").replace("\u2006", " ").replace("\u2007", " ").replace("\u2008", " ").replace("\u2009", " ").replace("\u200A", " ").replace("\u3000", " ");
			items.add(aaa);
			for (String item : items) {
				try {
//					item = item.strip();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println(item);
				result.add(item);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}
