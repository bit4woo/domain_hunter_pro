package GUI;

import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
 
import javax.swing.JTextField;

/**
 * 当JTextField没有输入时，显示提示文字
 * @param jTextField
 * @param hintText
 */
public class JTextFieldHintListener implements FocusListener {
	private String hintText;
	private JTextField textField;
	public JTextFieldHintListener(JTextField jTextField,String hintText) {
		this.textField = jTextField;
		this.hintText = hintText;
		jTextField.setText(hintText);  //默认直接显示,这里的修改出触发内容变更监听器
		jTextField.setForeground(Color.GRAY);
	}
 
	@Override
	public void focusGained(FocusEvent e) {
		//获取焦点时，清空提示内容
		String temp = textField.getText();
		if(temp.equals(hintText)) {
			textField.setText("");
			textField.setForeground(Color.BLACK);
		}
	}
 
	@Override
	public void focusLost(FocusEvent e) {	
		//失去焦点时，没有输入内容，显示提示内容
		String temp = textField.getText();
		if(StringUtils.isEmpty(temp)) {
			textField.setForeground(Color.GRAY);
			textField.setText(hintText);
		}
	}
}