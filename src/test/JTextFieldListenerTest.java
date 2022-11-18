package test;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class JTextFieldListenerTest {

  public static void main(String[] a) {
    JTextField jTextField1 = new JTextField();

    jTextField1.setText("w3cschool.cn");

    jTextField1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("action");//只有按下enter才会触发。
      }
    });
    JOptionPane.showMessageDialog(null,jTextField1);
  }
}
