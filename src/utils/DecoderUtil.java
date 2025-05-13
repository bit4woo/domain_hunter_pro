package utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.text.StringEscapeUtils;

public class DecoderUtil {

    /**
     * 先进行 HTML 实体解码，再进行 URL 解码
     */
    public static String decodeHtmlAndUrl(String input) {
        if (input == null) return null;

        // 第一步：HTML 解码
        // StringEscapeUtils.unescapeHtml4() 不需要 try-catch，因为它不会抛出 checked 异常。
        String htmlDecoded = StringEscapeUtils.unescapeHtml4(input);

        // 第二步：URL 解码
        try {
            return URLDecoder.decode(htmlDecoded, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return htmlDecoded; // 解码失败时返回 HTML 解码的结果
        }
    }
    
    
    public static String decodeJava(String input) {
        if (input == null) return null;

        // 第一步：将某些引号包含的部分解码，方便域名信息提取
        try {
            return StringEscapeUtils.unescapeJava(input);
        } catch (Exception e) {
            return input; // 解码失败时返回 HTML 解码的结果
        }
    }
    
}
