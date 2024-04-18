package InternetSearch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class SearchType {

	@Deprecated
	public static final String Host = "Host";//Host和Domain有时候分不清，还需研究TODO
	
	public static final String IP = "IP";
	public static final String Subnet = "Subnet";
	public static final String SubDomain = "SubDomain";

	public static final String Email = "Email";

	public static final String SimilarDomain = "SimilarDomain";
	public static final String Title = "Title";
	public static final String IconHash = "IconHash";
	public static final String Server = "Server"; //server="Microsoft-IIS/10"
	public static final String Asn = "Asn"; //asn="19551"

	public static final String OriginalString = "OriginalString";

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

	public static String choseFromList(String[] options) {
		// 创建一个包含选项的数组
		//String[] options = { "Option 1", "Option 2", "Option 3" };

		// 显示弹窗并获取用户选择的选项
		String selectedOption = (String) JOptionPane.showInputDialog(
				null,
				"Choose one option:",
				"Options",
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);

		return selectedOption;
	}

	public static String choseSearchType() {
		String[] array = getSearchTypeList().toArray(new String[0]);
		return choseFromList(array);
	}


	public static void main(String[] args) {
		//choseFromList();
	}

}
