package title;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//https://stackoverflow.com/questions/10387290/how-to-get-position-of-key-value-in-linkedhashmap-using-its-key
public class IndexedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {

    /**
     * 为了可以获取LinkedHashMap中元素的index。
     */
    private static final long serialVersionUID = 1L;

    private ArrayList<K> al_Index = new ArrayList<K>();

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

    public K getKeyAtIndex(int i) {
        return (K) al_Index.get(i);
    }
    
    public V getValueAtIndex(int i){
        return (V) super.get(al_Index.get(i));
    }

    public int IndexOfKey(K key) {
        return al_Index.indexOf(key);
    }
    
    public static void main(String args[]) {
    	IndexedLinkedHashMap aaa = new IndexedLinkedHashMap();
    	aaa.put("1", "a");
    	aaa.put("2", "b");
    	aaa.put("3", "c");
    	aaa.put("4", "d");
    	aaa.put("5", "e");
    	aaa.put("6", "f");
    	aaa.put("7", "g");
    	
    	System.out.print(aaa.IndexOfKey("4"));
    	aaa.remove("2");
    	System.out.print(aaa.IndexOfKey("4"));
    }
}
