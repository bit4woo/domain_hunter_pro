package burp;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.util.SubnetUtils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class Commons {

	public static String set2string(Set<?> set){
		Iterator iter = set.iterator();
		StringBuilder result = new StringBuilder();
		while(iter.hasNext())
		{
			//System.out.println(iter.next());  		
			result.append(iter.next()).append("\n");
		}
		return result.toString();
	}

	public static boolean isResponseNull(IHttpRequestResponse message){
		try {
			int x = message.getResponse().length;
			return false;
		}catch(Exception e){
			//stdout.println(e);
			return true;
		}
	}

	/**
	 * 对于信息收集来说，没有用的文件
	 * js是有用的
	 * pdf\doc\excel等也是有用的，可以收集到其中的域名
	 * rar\zip文件即使其中包含了有用信息，是无法直接读取的
	 * @param urlpath
	 * @return
	 */
	public static boolean uselessExtension(String urlpath) {
		String extensions = "css|jpeg|gif|jpg|png|rar|zip|svg|jpeg|ico|woff|woff2|ttf|otf";
		String[] extList = extensions.split("\\|");
		for ( String item:extList) {
			if(urlpath.endsWith("."+item)) {
				return true;
			}
		}
		return false;
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

	public static void browserOpen(Object url,String browser) throws Exception{
		String urlString = null;
		URI uri = null;
		if (url instanceof String) {
			urlString = (String) url;
			uri = new URI((String)url);
		}else if (url instanceof URL) {
			uri = ((URL)url).toURI();
			urlString = url.toString();
		}
		if(browser == null ||browser.equalsIgnoreCase("default") || browser.equalsIgnoreCase("")) {
			//whether null must be the first
			Desktop desktop = Desktop.getDesktop();
			if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
				desktop.browse(uri);
			}
		}else {
			String[] cmdArray = new String[] {browser,urlString};

			//runtime.exec(browser+" "+urlString);//当命令中有空格时会有问题
			Runtime.getRuntime().exec(cmdArray);
		}
	}


	public static byte[] buildCookieRequest(IExtensionHelpers helpers,String cookie, byte[] request) {
		if (cookie != null && !cookie.equals("")){
			if (!cookie.startsWith("Cookie: ")){
				cookie = "Cookie: "+cookie;
			}
			List<String > newHeader = helpers.analyzeRequest(request).getHeaders();
			int bodyOffset = helpers.analyzeRequest(request).getBodyOffset();
			byte[] byte_body = Arrays.copyOfRange(request, bodyOffset, request.length);
			newHeader.add(cookie);
			request = helpers.buildHttpMessage(newHeader,byte_body);
		}
		return request;
	}
	/**
	 * 尝试在响应包中寻找meta charset的标签，来判别响应包的编码
	 * 
	 * 一些常见格式：
	 * <meta HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<meta charset="UTF-8">
<meta charset="utf-8">
<meta charset=utf-8>
<meta http-equiv="Content-Language" content="zh-CN">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	 */
	static String detectCharsetInBody(byte[] requestOrResponse){
		String body = new String(requestOrResponse);
		if (body.split("\r\n\r\n").length >=2 ) {
			body = body.split("\r\n\r\n")[1];
		}
		if (body.length() >1000) {
			body = body.substring(0,1000);
		}
		String pattern = "charset=(.*?)[\"/\\s>]+";//加? 非贪婪模式
		//String patternExtract = "charset=(.*?)>";

		Pattern metaCharset = Pattern.compile(pattern);
		Matcher matcher = metaCharset.matcher(body);
		//System.out.println(body);
		if (matcher.find()) {//多次查找
			String charset = matcher.group(1);
			return charset;
		}
		return null;
	}

	/**
	 * utf8 utf-8都是可以的。
	 * @param requestOrResponse
	 * @return
	 */
	public static String detectCharset(byte[] requestOrResponse){
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		Getter getter = new Getter(helpers);
		boolean isRequest = true;
		if (new String(requestOrResponse).startsWith("HTTP/")) {//response
			isRequest = false;
		}

		String contentType = getter.getHeaderValueOf(isRequest,requestOrResponse,"Content-Type");

		//1、尝试从contentTpye中获取
		if (contentType != null){
			if (contentType.toLowerCase().contains("charset=")) {
				String tmpcharSet = contentType.toLowerCase().split("charset=")[1];
				if (tmpcharSet.contains(",")) {
					tmpcharSet = tmpcharSet.split(",")[0];
				}
				if (tmpcharSet != null && tmpcharSet.length() >0) {
					return tmpcharSet;
				}
			}
		}

		if (!isRequest) {
			String tmpCharset = detectCharsetInBody(requestOrResponse);
			System.out.println("响应包中编码识别结果："+tmpCharset);
			if (null != tmpCharset) {
				return tmpCharset;
			}
		}


		//2、尝试使用ICU4J进行编码的检测
		CharsetDetector detector = new CharsetDetector();
		detector.setText(requestOrResponse);
		CharsetMatch cm = detector.detect();
		System.out.println("ICU4J检测到编码："+cm.getName());
		if (cm != null) {
			return cm.getName();
		}

		//3、http post的默认编码
		return "ISO-8859-1";
	}


	public static List<Integer> Port_prompt(Component prompt, String str){
		String defaultPorts = "8080,8000,8443";
		String user_input = JOptionPane.showInputDialog(prompt, str,defaultPorts);
		if (null == user_input || user_input.trim().equals("")) return  null; 
		List<Integer> portList = new ArrayList<Integer>();
		for (String port: user_input.trim().split(",")) {
			int portint = Integer.parseInt(port);
			portList.add(portint);
		}
		return portList;
	}

	public static boolean isWindows() {
		String OS_NAME = System.getProperties().getProperty("os.name").toLowerCase();
		if (OS_NAME.contains("windows")) {
			return true;
		} else {
			return false;
		}
	}

	public static ArrayList<String> regexFind(String regex,String content) {
		ArrayList<String> result = new ArrayList<String>();
		Pattern pRegex = Pattern.compile(regex);
		Matcher matcher = pRegex.matcher(content);
		while (matcher.find()) {//多次查找
			result.add(matcher.group());
		}
		return result;
	}

	public static String replaceLast(String string, String toReplace, String replacement) {
		int pos = string.lastIndexOf(toReplace);
		if (pos > -1) {
			return string.substring(0, pos)
					+ replacement
					+ string.substring(pos + toReplace.length());
		} else {
			return string;
		}
	}


	public static void writeToClipboard(String text) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection selection = new StringSelection(text);
		clipboard.setContents(selection, null);
	}

	public static boolean isWindows10() {
		String OS_NAME = System.getProperties().getProperty("os.name").toLowerCase();
		if (OS_NAME.equalsIgnoreCase("windows 10")) {
			return true;
		}
		return false;
	}

	public static boolean isMac(){
		String os = System.getProperty("os.name").toLowerCase();
		//Mac
		return (os.indexOf( "mac" ) >= 0); 
	}

	public static boolean isUnix(){
		String os = System.getProperty("os.name").toLowerCase();
		//linux or unix
		return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
	}

	public static void OpenFolder(String path) throws IOException {
		Desktop.getDesktop().open(new File(path));
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

			if (path.equals("")) {
				urlString = urlString+"/";
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return urlString;
	}
	
	public static Set<String> getSetFromTextArea(JTextArea textarea) {
		Set<String> domainList = new HashSet<>(textToLines(textarea.getText()));
		domainList.remove("");
		return domainList;
	}

	public static List<String> getLinesFromTextArea(JTextArea textarea){
		return textToLines(textarea.getText());
	}
	

	/**
	 * 换行符的可能性有三种，都必须考虑到
	 * @param input
	 * @return
	 */
	public static List<String> textToLines(String input){
		String[] lines = input.split("(\r\n|\r|\n)", -1);
		List<String> result = new ArrayList<String>();
		for(String line: lines) {
			line = line.trim();
			if (!line.equalsIgnoreCase("")) {
				result.add(line.trim());
			}
		}
		return result;
	}


	public static List<String> removePrefixAndSuffix(List<String> input,String Prefix,String Suffix) {
		ArrayList<String> result = new ArrayList<String>();
		if (Prefix == null && Suffix == null) {
			return result;
		} else {
			if (Prefix == null) {
				Prefix = "";
			}

			if (Suffix == null) {
				Suffix = "";
			}

			List<String> content = input;
			for (String item:content) {
				if (item.startsWith(Prefix)) {
					//https://stackoverflow.com/questions/17225107/convert-java-string-to-string-compatible-with-a-regex-in-replaceall
					String tmp = Pattern.quote(Prefix);//自动实现正则转义
					item = item.replaceFirst(tmp, "");
				}
				if (item.endsWith(Suffix)) {
					String tmp = Pattern.quote(reverse(Suffix));//自动实现正则转义
					item = reverse(item).replaceFirst(tmp, "");
					item = reverse(item);
				}
				result.add(item); 
			}
			return result;
		}
	}

	public static String reverse(String str) {
		if (str == null) {
			return null;
		}
		return new StringBuffer(str).reverse().toString();
	}

	public static void test1() {
		SubnetUtils net = new SubnetUtils("143.92.67.34/24");
		System.out.println(net.getInfo().isInRange("143.92.67.34:6443"));
	}
	public static void test2() {
		Set<String> IPSet = new HashSet<String>();
		IPSet.add("192.168.1.225");
		IPSet.add("192.168.1.128");
		IPSet.add("192.168.1.129");
		IPSet.add("192.168.1.155");
		IPSet.add("192.168.1.224");
		IPSet.add("192.168.1.130");

	}

	public static void test4() {
		String Prefix = "\"";
		//		String Prefix = Pattern.quote("\"");
		System.out.println(Prefix);
		System.out.println("\"aaaa\"".replaceFirst(Prefix, ""));
	}

	public static void test5() {
		String aa = "10.12.12.12/";
		System.out.println(aa.split("/").length);
	}

	public static void test6() {
		String aa = "10.  12. 12.12/";
		System.out.println(aa.trim());
	}

	public static void test8() {
		System.out.println(uselessExtension("abc.css"));
	}

	public static void test9() throws Exception {
		byte[] body = FileUtils.readFileToByteArray(new File("/private/tmp/response.html"));
		System.out.println(detectCharsetInBody(body));
	}

	public static void test10() throws IOException {
		System.out.println(detectCharsetInBody(FileUtils.readFileToByteArray(new File("F://response.txt"))));
	}
	
	public static void testCharset() {
		String aaa = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n"
				+ "    <head>\n"
				+ "        <title>The page is not found</title>\n"
				+ "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n"
				+ "        <style type=\"text/css\">";
		System.out.print(detectCharsetInBody(aaa.getBytes()));
	}

	public static void main(String args[]) throws Exception {
		//test10();
		testCharset();
	}
}
