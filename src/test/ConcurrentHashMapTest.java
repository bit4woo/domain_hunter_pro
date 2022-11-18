package test;

import java.util.concurrent.ConcurrentHashMap;
/**
 * 线程安全，但是无序
 * https://github.com/ben-manes/concurrentlinkedhashmap 
 * 
 * 
 * 为了提高LineEntry的查找速度，改为使用LinkedHashMap,
 * http://infotechgems.blogspot.com/2011/11/java-collections-performance-time.html
 *
 * LinkedHashMap是继承于HashMap，是基于HashMap和双向链表来实现的。
 * HashMap无序；LinkedHashMap有序，可分为插入顺序和访问顺序两种。默认是插入顺序。
 * 如果是访问顺序，那put和get操作已存在的Entry时，都会把Entry移动到双向链表的表尾(其实是先删除再插入)。
 * LinkedHashMap是线程不安全的。
 * 
 * ConcurrentLinkedHashMap
 * 
 * 
 * https://cayenne.apache.org/docs/3.1/api/org/apache/cayenne/util/concurrentlinkedhashmap/ConcurrentLinkedHashMap.html
 *
 */
public class ConcurrentHashMapTest {
	public static void main(String[] args) {
		ConcurrentHashMap<String,String> text = new ConcurrentHashMap<String,String>();
		text.put("1", "1");
		text.put("8", "8");
		text.put("2", "2");
		text.put("6", "6");
		text.put("5", "5");
		for (String key:text.keySet()) {
			System.out.println(key);
		}
	}
}
