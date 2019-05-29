package test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JtableFont {

    	/*
Content-Type: text/html;charset=UTF-8

<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta charset="utf-8">
<script type="text/javascript" charset="utf-8" src="./resources/jrf-resource/js/jrf.min.js"></script>
 */
    public static void main(String args[]){
        String body = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>";
        String title = "中文";
    			final String DOMAIN_NAME_PATTERN = "charset=(.*?)>";
			Pattern pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
			Matcher matcher = pDomainNameOnly.matcher(body);
			while (matcher.find()) {//多次查找

				String charSet = matcher.group(0);
				charSet = charSet.replace("\"","");
				charSet = charSet.replace(">","");
				charSet = charSet.replace("/","");
				System.out.println(charSet);
			}
        //System.out.println(new String(title.getBytes(),"gbk"));

    }
}
