package base;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ResourceFileReader {


    public static List<String> readFile(String filename) {
        URL url = BurpExtender.class.getClassLoader().getResource(filename);
        File copyFile = new File("."+filename);

        try {
            FileUtils.copyURLToFile(url,copyFile);
            copyFile.deleteOnExit();//这是在程序退出时删除文件，临时文件的用法
            List<String> dictList = FileUtils.readLines(copyFile);
            return dictList;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<String>();
        }

    }


    /** * 读取文件指定行。 */
    public static void main(String[] args) throws IOException {

    }
}