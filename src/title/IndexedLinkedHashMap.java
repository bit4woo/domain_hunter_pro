package title;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import domain.target.TargetEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//https://stackoverflow.com/questions/10387290/how-to-get-position-of-key-value-in-linkedhashmap-using-its-key
public class IndexedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {

    /**
     * 为了可以获取LinkedHashMap中元素的index。
     */
    private static final long serialVersionUID = 1L;

    private ArrayList<K> al_Index = new ArrayList<K>();

    /**
     * LinkedHashMap的方法
     * @param key
     * @param val
     * @return
     */
    @Override
    public V put(K key,V val) {
        if (!super.containsKey(key)) al_Index.add(key);
        V returnValue = super.put(key,val);
        return returnValue;
    }
    
    @Override
    public V remove(Object key) {
        if (super.containsKey(key)) al_Index.remove((K)key);
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
        K key = (K) al_Index.get(index);
        V returnValue = super.remove(key);
        al_Index.remove(key);
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
        return (V) super.get(al_Index.get(i));
    }

    public int IndexOfKey(K key) {
        return al_Index.indexOf(key);
    }

    public static void test() {
    	IndexedLinkedHashMap<String,TargetEntry> targetEntries =new IndexedLinkedHashMap<String,TargetEntry>();
    	targetEntries.put("1111",new TargetEntry("www.baidu.com"));
    	
    	String str = JSON.toJSONString(targetEntries);
    	System.out.println(str);
    	System.out.println( JSON.parseObject(str,IndexedLinkedHashMap.class));
		//https://blog.csdn.net/qq_27093465/article/details/73277291
    	
    	String str1 = JSON.toJSONString(targetEntries);
    	System.out.println(str1);
    	System.out.println( new Gson().fromJson(str,IndexedLinkedHashMap.class));
    }
    public static void test1() {
    	IndexedLinkedHashMap aaa = new IndexedLinkedHashMap();
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
        test1();
    }


}
