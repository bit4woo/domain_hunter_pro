package Tools;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * 本类使用org.json作为处理类
 */
public class JSONHandler {

	public static void main(String args[]) {
		System.out.println();
		test();
	}

	//org.json
	public static boolean isJSON(String test) {
		return isJSONObject(test) || isJSONArray(test);
	}

	//org.json
	public static boolean isJSONObject(String test) {
		try {
			new JSONObject(test);
			return true;
		} catch (JSONException ex) {
			return false;
		}
	}


	public static boolean isJSONArray(String test) {
		try {
			new JSONArray(test);
			return true;
		} catch (JSONException ex) {
			return false;
		}
	}

	public static ArrayList<String> grepValueFromJson(String jsonString,String keyName) throws Exception {
		ArrayList<String> result = new ArrayList<String>();

		if(jsonString.startsWith("HTTP/") && jsonString.contains("\r\n\r\n")) {//response
			String[] parts = jsonString.split("\r\n\r\n", 2);
			if (parts.length ==2) {
				jsonString = parts[1];
			}
		}

		if (isJSONObject(jsonString)) {
			JSONObject obj = new JSONObject(jsonString);
			Iterator<String> iterator = obj.keys();
			while (iterator.hasNext()) {
				// We need to know keys of Jsonobject
				String key = (String) iterator.next();
				String value = obj.get(key).toString();

				if (key.equals(keyName)) {
					result.add(value);
				}

				result.addAll(grepValueFromJson(value,keyName));
			}
		}else if(isJSONArray(jsonString)){
			//JSONArray中每个元素都是JSON
			JSONArray obj = new JSONArray(jsonString);
			for (int i=0;i<obj.length();i++) {
				String item = obj.get(i).toString();				
				result.addAll(grepValueFromJson(item,keyName));
			}
		}else {
			String reg = String.format("\"%s\":[\\s]*[\"]{0,1}(.*?)[\"]{0,1}[,}]+", keyName);

			Pattern pattern = Pattern.compile(reg);
			Matcher matcher = pattern.matcher(jsonString);
			while (matcher.find()) {//多次查找
				String item = matcher.group(1);
				//System.out.println("111"+item+"111");
				result.add(item);
			}
		}
		return result;
	}


	public static void test() {
		String aaa = "[\r\n" + 
				"  {\r\n" + 
				"    \"amount\": \" 12185\",\r\n" + 
				"    \"job\": \"GAPA\",\r\n" + 
				"    \"month\": \"JANUARY\",\r\n" + 
				"    \"year\": \"2010\"\r\n" + 
				"  },\r\n" + 
				"  {\r\n" + 
				"    \"amount\": \"147421\",\r\n" + 
				"    \"job\": \"GAPA\",\r\n" + 
				"    \"month\": \"MAY\",\r\n" + 
				"    \"year\": \"2010\"\r\n" + 
				"  },\r\n" + 
				"  {\r\n" + 
				"    \"amount\": \"2347\",\r\n" + 
				"    \"job\": \"GAPA\",\r\n" + 
				"    \"month\": \"AUGUST\",\r\n" + 
				"    \"year\": \"2010\"\r\n" + 
				"  }\r\n" + 
				"]";
		String bbb = "[{\"amount\":\" 12185\",\"job\":\"GAPA\",\"month\":\"JANUARY\",\"year\":\"2010\"},{\"amount\":\"147421\",\"job\":\"GAPA\",\"month\":\"MAY\",\"year\":\"2010\"},{\"amount\":\"2347\",\"job\":\"GAPA\",\"month\":\"AUGUST\",\"year\":\"2010\"}]";
		String ccc = "{\r\n" + 
				"    \"files\": {\r\n" + 
				"        \"f1.png\": {\r\n" + 
				"            \"intext\": \"A\",\r\n" + 
				"            \"inval\": 0,\r\n" + 
				"            \"inbinary\": false\r\n" + 
				"        },\r\n" + 
				"        \"f2.png\": 11{\r\n" + 
				"            \"intext\": \"A\",\r\n" + 
				"            \"inval\": 0,\r\n" + 
				"            \"inbinary\": true\r\n" + 
				"        }\r\n" + 
				"    }\r\n" + 
				"}";
		try {
			System.out.println(grepValueFromJson(aaa,"month"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
