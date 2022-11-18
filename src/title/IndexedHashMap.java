package title;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

import domain.target.TargetEntry;

/**
 *需要构造一个map，满足如下要求：
 * 1、线程安全ConcurrentHashMap是线程安全的，但是不是按序存储。
 * 2、尽可能快的查询速度。参考http://infotechgems.blogspot.com/2011/11/java-collections-performance-time.html
 * 为了提高LineEntry的查找速度，改为使用LinkedHashMap,
 * LinkedHashMap是继承于HashMap，是基于HashMap和双向链表来实现的。
 * HashMap无序；LinkedHashMap有序，可分为插入顺序和访问顺序两种。默认是插入顺序。
 * 如果是访问顺序，那put和get操作已存在的Entry时，都会把Entry移动到双向链表的表尾(其实是先删除再插入)。
 * LinkedHashMap是线程不安全的。
 * 
 * 虽然费劲找到了ConcurrentLinkedHashMap
 * https://cayenne.apache.org/docs/3.1/api/org/apache/cayenne/util/concurrentlinkedhashmap/ConcurrentLinkedHashMap.html
 * 但是他还是不能直接使用，依然要自己实现通过Index获取元素的方法，而且它还不能被继承。
 * 
 * 所以再次修改，继承ConcurrentHashMap来自行实现。
 * 参考：https://stackoverflow.com/questions/10387290/how-to-get-position-of-key-value-in-linkedhashmap-using-its-key
 */

public class IndexedHashMap<K,V> extends ConcurrentHashMap<K,V> {


    private static final long serialVersionUID = 1L;

    /**
     * 为了可以获取LinkedHashMap中元素的index。
     */
    private List<K> Index = Collections.synchronizedList(new ArrayList<K>());

    /**
     * LinkedHashMap的方法
     * @param key
     * @param val
     * @return
     */
    @Override
    public V put(K key,V val) {
        if (!super.containsKey(key)) Index.add(key);
        V returnValue = super.put(key,val);
        return returnValue;
    }
    
    @Override
    public V remove(Object key) {
        if (super.containsKey(key)) Index.remove((K)key);
        V returnValue = super.remove(key);
        return returnValue;
    }

    /**
     * 为了符合平常的使用习惯，减少出错
     * @param key
     * @return
     */
    public V remove(int key) {
        return removeByIndex(key);
    }

    private V removeByIndex(int index) {
        K key = (K) Index.get(index);
        V returnValue = super.remove(key);
        Index.remove(key);
        return returnValue;
    }

    /**
     * 为了符合平常的使用习惯，减少出错
     * @param i
     * @return
     */
    public V get(int i){
        return getValueAtIndex(i);
    }

    @Override
    public V get(Object key) {
        return super.get(key);
    }

    private V getValueAtIndex(int i){
        if (i >= Index.size()) {
            throw new ArrayIndexOutOfBoundsException(i + " >= " + Index.size());
        }
        return (V) super.get(Index.get(i));
    }

    public int IndexOfKey(K key) {
        return Index.indexOf(key);
    }

    public static void test() {
    	IndexedHashMap<String,TargetEntry> targetEntries =new IndexedHashMap<String,TargetEntry>();
    	targetEntries.put("1111",new TargetEntry("www.baidu.com"));
    	
    	String str = JSON.toJSONString(targetEntries);
    	System.out.println(str);
    	System.out.println( JSON.parseObject(str,IndexedHashMap.class));
		//https://blog.csdn.net/qq_27093465/article/details/73277291
    	
    	String str1 = JSON.toJSONString(targetEntries);
    	System.out.println(str1);
    	System.out.println( new Gson().fromJson(str,IndexedHashMap.class));
    }
    public static void test1() {
    	IndexedHashMap aaa = new IndexedHashMap();
    	aaa.put("1", "a");
    	aaa.put("2", "b");
    	aaa.put("3", "c");
    	aaa.put("4", "d");
    	aaa.put("5", "e");
    	aaa.put("6", "f");
    	aaa.put("7", "g");
        aaa.put("7", "x");


    	//System.out.println("index of 4: "+aaa.IndexOfKey("4"));
    	//aaa.remove("2");
        //aaa.put("7", "x");
        System.out.println(aaa.get("1"));
        System.out.println(aaa.get(1));
    	//System.out.println("index of 4: "+aaa.IndexOfKey("4"));
    }
    public static void main(String[] args) {
    	test();
        test1();
    }

}
