package InternetSearch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SearchType {

	public static final String Host = "Host";
	public static final String IP = "IP";
	public static final String Subnet = "Subnet";
	public static final String Domain = "Domain";
	public static final String Title = "Title";
	public static final String IconHash = "IconHash";
	
	
	public static List<String> getSearchTypeList(){
		List<String> result = new ArrayList<String>();
		Field[] fields = SearchType.class.getDeclaredFields();
		for (Field field : fields) {
			//String varName = field.getName();// 对于每个属性，获取属性名
			if (field.getGenericType().toString().equals("class java.lang.String")) {// 如果type是类类型，则前面包含"class "，后面跟类名
				try {
					String value = (String) field.get(SearchType.class);//获取属性值
					result.add(value);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
}
