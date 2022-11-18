package test;

import java.util.Arrays;
import java.util.HashSet;

public class SetToStringTest {
	public static void main(String[] args) {
		HashSet<String> set = new HashSet<String>();
		set.add("aaaa");
		set.add("bbb");
		String temp = set.toString();
		String temp1 = String.join(",", set);
		System.out.println(temp);
		System.out.println(temp1);
		System.out.println(temp.split(","));
		System.out.println(new HashSet<String>(Arrays.asList(temp1.split(","))));
	}
}
