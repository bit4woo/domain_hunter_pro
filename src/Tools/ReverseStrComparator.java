package Tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 字符串倒序（翻转），然后比较排序
 *
 *
 */
public class ReverseStrComparator implements java.util.Comparator<String> {
	@Override
    public int compare(String item1, String item2) {
		item1 = new StringBuilder(item1).reverse().toString();
		
		item2 = new StringBuilder(item2).reverse().toString();
        return item1.compareTo(item2);
    }
	
	public static void main(String[] args) {
		String[] aaa = {"ccc.baidu.com","aaa.jd.com","bbb.ali.com"};
		List bbb= Arrays.asList(aaa);
		Collections.sort(bbb,new ReverseStrComparator());
		System.out.println(bbb.toString());
	}
}