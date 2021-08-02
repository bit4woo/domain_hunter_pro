package Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 域名排序方法，这样更符合阅读习惯
 * 
 * 先将域名已点号分割，然后倒序拼接（类似于java的package name了）
 * 相当于安装package name排序
 * 
 * www.pan.baidu.com
 * img.baidu.com
 * www.baidu.com
 *
 *
 */
public class DomainComparator implements java.util.Comparator<String> {
	@Override
    public int compare(String domain1, String domain2) {
		List<String> parts1 = Arrays.asList(domain1.split("\\."));
		Collections.reverse(parts1);
		
		List<String> parts2 = Arrays.asList(domain2.split("\\."));
		//System.out.println(parts2.toString());
		Collections.reverse(parts2);
		//System.out.println(parts2.toString());
        return parts1.toString().compareTo(parts2.toString());
    }
	
	public static void main(String[] args) {
		String[] aaa = {"ccc.baidu.com","aaa.baidu.com","bbb.baidu.com"};
		List bbb= Arrays.asList(aaa);
		Collections.sort(bbb,new DomainComparator());
		System.out.println(bbb.toString());
	}
}