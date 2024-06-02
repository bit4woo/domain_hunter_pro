package base;

import burp.IHttpRequestResponse;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Commons {

	public static boolean isResponseNull(IHttpRequestResponse message){
		try {
			int x = message.getResponse().length;
			return false;
		}catch(Exception e){
			//stdout.println(e);
			return true;
		}
	}

	public static String getNowTimeString() {
		SimpleDateFormat simpleDateFormat = 
				new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		return simpleDateFormat.format(new Date());
	}

	public static String TimeToString(long time) {
		SimpleDateFormat simpleDateFormat = 
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return simpleDateFormat.format(time);
	}

	public static int getNowMinute(){
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		return cal.get(Calendar.MINUTE);
	}


	/*
	 *将形如 https://www.runoob.com的URL统一转换为
	 * https://www.runoob.com:443/
	 * 
	 * 因为末尾的斜杠，影响URL类的equals的结果。
	 * 而默认端口影响String格式的对比结果。
	 */

	public static String formateURLString(String urlString) {
		try {
			//urlString = "https://www.runoob.com";
			URL url = new URL(urlString);
			String host = url.getHost();
			int port = url.getPort();
			String path = url.getPath();

			if (port == -1) {
				String newHost = url.getHost()+":"+url.getDefaultPort();
				urlString = urlString.replace(host, newHost);
			}

			if (StringUtils.isEmpty(path)) {
				urlString = urlString+"/";
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return urlString;
	}

	public static void main(String args[]) throws Exception {
	}
}
