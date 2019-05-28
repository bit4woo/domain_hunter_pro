package test;

import java.io.File;

public class FileCreateAndDeletetest {
    public static void main(String args[]){
        try {
            File file = new File("xxxxx");
            if (file.delete()){
                System.out.println("删除成功");
            }
            file = new File("xxxxx");
            file.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
