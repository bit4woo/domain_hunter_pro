package test;

import java.util.LinkedHashMap;
import java.util.Set;

//再考虑ConcurrentHashMap是否符合要求，LinkedHashMap缺少线程安全
public class LinkedHashMapTest {

	public static void main(String[] args)

	{
		//create a linked hash map instance
		LinkedHashMap<Integer, Integer> linkedhashmap =  new LinkedHashMap<Integer, Integer>();

		//Add mappings
		linkedhashmap.put(3,  5);
		linkedhashmap.put(7,  3);
		linkedhashmap.put(2,  9);
		linkedhashmap.put(6,  1);
		linkedhashmap.put(5,  11);

		//get the key set

		Set<Integer> keyset = linkedhashmap.keySet();
		Integer[] keyarray = keyset.toArray(new Integer[keyset.size()]);
		System.out.println(keyset.toString());//keySet是保持了顺序的！

		//taking input of index

		Integer index =  3;
		Integer key = keyarray[index -  1];

		//get value from the LinkedHashMap for the key
		System.out.println("Value at index " + index +  " is : " + linkedhashmap.get(key));
	}
}