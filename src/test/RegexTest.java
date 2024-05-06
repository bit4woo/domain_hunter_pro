import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    public static void main(String[] args) {
        String input = "Today's date is 2024-04-27.";

        // 定义日期正则表达式，并使用捕获组提取年、月、日
        Pattern pattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            // 提取年份
            String year = matcher.group(1);
            // 提取月份
            String month = matcher.group(2);
            // 提取日期
            String day = matcher.group(3);

            // 输出结果
            System.out.println("Year: " + year);
            System.out.println("Month: " + month);
            System.out.println("Day: " + day);
        } else {
            System.out.println("Date not found.");
        }
    }
}