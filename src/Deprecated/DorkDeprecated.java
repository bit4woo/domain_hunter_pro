package Deprecated;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//String dorkWords = "Host url request response comment";//header body 
@Deprecated //枚举类是更好的实现方式
public class DorkDeprecated {
	public final static String HOST = "host";
	public final static String URL = "url";
	public final static String REQUEST = "request";
	public final static String RESPONSE = "response";
	public final static String COMMENT = "comment";
	//public final static String HEAD = "head";
	//public final static String BODY = "body";

	public static void main(String args[]) {
		try {
			System.out.println(getDorkValues());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static List<String> getDorkValues() throws Exception{
		// 获取实体类的所有属性，返回Field数组  
		Field[] fields = DorkDeprecated.class.getDeclaredFields();

		List<String> dorkValues = new ArrayList<String>();
		for (Field field : fields) {
			
			if (field.getGenericType().toString().equals("class java.lang.String")) {// 如果type是类类型，则前面包含"class "，后面跟类名  
				//String varName = field.getName();// 对于每个属性，获取属性名
				String value = (String)field.get(DorkDeprecated.class);//获取属性值
				dorkValues.add(value);
			}
		}
		return dorkValues;
	}
	
	public static boolean isDorkString(String input) {

		String[] arr = input.split(":",2);//limit =2 分割成2份
		if (arr.length ==2) {
			String dork = arr[0].trim();
			String keyword =  arr[1].trim();
			if  (!keyword.equals("")) {
				try {
					if (getDorkValues().contains(dork)) return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return false;
	}
}
