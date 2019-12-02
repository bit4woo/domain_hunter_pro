package test;

import java.util.LinkedHashMap;

public class linkedHashMapTest {
	public static void test() {
		LinkedHashMap<String,String> a = new LinkedHashMap<String,String> ();
		a.put("111", "aaa");
		a.put("222", "bbb");
		System.out.print(a.get(1));
	}
	
	public static void main(String args[]) {
		test();
	}
}
