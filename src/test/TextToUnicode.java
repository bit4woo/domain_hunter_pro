public class TextToUnicode {

    public static String convertToUnicode(String text) {
        StringBuilder unicodeStringBuilder = new StringBuilder();
        for (char c : text.toCharArray()) {
            unicodeStringBuilder.append(String.format("\\u%04X", (int) c)); // 转换字符为Unicode编码
        }
        return unicodeStringBuilder.toString();
    }

    public static void main(String[] args) {
        String text = "你好，世界！"; // 要转换为Unicode编码的文本
        String unicodeString = convertToUnicode(text);
        System.out.println("Unicode编码结果：" + unicodeString);
    }
}


