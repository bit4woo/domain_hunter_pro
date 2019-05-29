package test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.UnsupportedEncodingException;

public class JtableFont {
    public static void main(String args[]){
        JTable table = new JTable();

        DefaultTableModel domainTableModel = new DefaultTableModel(
                new Object[][]{
                        //{"1", "1","1"},
                },
                new String[]{
                        "Root Domain", "Keyword"//, "Source"
                }
        );
        table.setModel(domainTableModel);
        domainTableModel.addRow(new String[]{"1111","京东白拿"});
        Font f = table.getFont();
        System.out.println(f);
    }

    public String changeCharset(String str, String originCharset,String newCharset)
            throws UnsupportedEncodingException {
        if (str != null) {
            //用默认字符编码解码字符串。
            byte[] bs = str.getBytes(originCharset);
            //用新的字符编码生成字符串
            return new String(bs, newCharset);
        }
        return null;
    }

}
