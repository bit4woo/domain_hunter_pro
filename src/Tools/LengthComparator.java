package Tools;

/**
 * 根据字符串长度进行排序
 * 
 */
public class LengthComparator implements java.util.Comparator<String> {
	@Override
    public int compare(String s1, String s2) {
        return s1.length() - s2.length();
    }
}