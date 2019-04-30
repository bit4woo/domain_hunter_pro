package test;

import java.net.URLDecoder;
import org.apache.commons.text.StringEscapeUtils;

public class decodeTest {
	public static void main(String args[]) {
		String xxString = URLDecoder.decode("%25%36%38%25%37%34%25%37%34%25%37%30%25%37%33%25%33%61%25%32%66%25%32%66%25%37%37%25%37%37%25%37%37%25%32%65%25%36%32%25%36%31%25%36%39%25%36%34%25%37%35%25%32%65%25%36%33%25%36%66%25%36%64");
		xxString = URLDecoder.decode(xxString);
		System.out.println(xxString);
		
		String yyyString = StringEscapeUtils.unescapeJava("\\u4f60\\u597d\\u554a\\uff01");
		System.out.println(yyyString);
	}
}
