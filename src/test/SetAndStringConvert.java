package test;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;

public class SetAndStringConvert {
	public static void main(String[] args) {
		test2();
	}

	public static void test() {
		HashSet<String> set = new HashSet();
		set.add("Apple");
		set.add("Google");
		set.add("Facebook");
		set.add("Amazon");

		String result = set.toString();
		String result1 = String.join(",", set);
		System.out.println(result1);


		set = new HashSet();
		set.addAll(Arrays.asList(result1.split(",")));
	}

	public static void test1() {
		HashSet<String> set = new HashSet();
		set.add("Alive");
		set.add("is");
		set.add("Awesome");

		String result = StringUtils.join(set, ",");
		System.out.println(result);

	}

	/**
	 * set and String convert
	 * 选择这个方法
	 */
	public static void test2() {
		HashSet<String> set = new HashSet();
		set.add("Alive");
		set.add("is");
		set.add("Awesome");

		String result = JSON.toJSONString(set);
		System.out.println(result);

		set = JSON.parseObject(result,HashSet.class);
		System.out.println(set);
	}


	public static void test3() {
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