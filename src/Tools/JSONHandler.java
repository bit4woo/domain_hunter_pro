package Tools;


import java.util.ArrayList;
import java.util.Iterator;

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
		if (isJSONObject(test) || isJSONArray(test)) {
			return true;
		}else {
			return false;
		}
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
	
	public static ArrayList<String> grepValueFromJson(String jsonString,String toFind) throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		
		if (isJSONObject(jsonString)) {
			JSONObject obj = new JSONObject(jsonString);
			Iterator<String> iterator = obj.keys();
			while (iterator.hasNext()) {
				// We need to know keys of Jsonobject
				String key = (String) iterator.next();
				String value = obj.get(key).toString();
				
				if (key.equals(toFind)) {
					result.add(value);
				}
				
				// if it's jsonobject or jsonarray
				if (isJSONObject(value) || isJSONArray(value)) {
					result.addAll(grepValueFromJson(value,toFind));
				}
			}
		}else if(isJSONArray(jsonString)){
			//JSONArray中每个元素都是JSON
			JSONArray obj = new JSONArray(jsonString);
			for (int i=0;i<obj.length();i++) {
				String item = obj.get(i).toString();				
				result.addAll(grepValueFromJson(item,toFind));
			}
		}else {
			throw new Exception("wrong json formate");
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
				"        \"f2.png\": {\r\n" + 
				"            \"intext\": \"A\",\r\n" + 
				"            \"inval\": 0,\r\n" + 
				"            \"inbinary\": true\r\n" + 
				"        }\r\n" + 
				"    }\r\n" + 
				"}";
		try {
			System.out.println(grepValueFromJson(ccc,"intext"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
