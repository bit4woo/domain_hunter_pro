
import java.net.URL;

public class URLTest {
    public static void main(String[] args) {
        try {
            // 创建URL对象
            URL url = new URL("https://www.example.com/path/to/resource?param1=value1#111111");

            // 使用 getFile() 方法获取文件名,带查询参数
            String file = url.getFile();
            System.out.println("File: " + file);

            // 使用 getPath() 方法获取路径，不带查询参数
            String path = url.getPath();
            System.out.println("Path: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
