package base;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSON;

public class SetAndStr {
	
	public static void main(String[] args) {
		Set<String> set = new HashSet<String>();
		set.add("Alive");
		set.add("is");
		set.add("Awesome");
		System.out.println(set);
		String tmp = toStr(set);
		System.out.println(tmp);
		Set set1 = toSet(tmp);
		System.out.println(set1);
	}


	/**
	 * 将Set转换为字符串格式，方便存储到数据库等
	 * @param <T>
	 * @param <T>
	 */
	public static <T> String toStr(Set<T> inputSet) {
		String result = JSON.toJSONString(inputSet);
		return result;
	}
	
	/**
	 * 将字符串格式，还原成Set。方便对象数据的操作使用。
	 * @param <T>
	 * @param input
	 * @return
	 */
	public static <T> Set<T> toSet(String input) {
		Set<T> set = JSON.parseObject(input,Set.class);
		return set;
	}
}