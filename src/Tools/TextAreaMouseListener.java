package Tools;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TextAreaMouseListener extends MouseAdapter{
	JTextArea textArea;
	String selected;
	
	public TextAreaMouseListener(JTextArea textArea){
		this.textArea = textArea;
	}
	
    @Override
    public void mouseClicked(MouseEvent arg0) {
        if (arg0.getButton() == MouseEvent.BUTTON3) {
            // 弹出菜单
			this.selected = textArea.getSelectedText();
			JPopupMenu jp = new TextAreaMenu(selected);
            jp.show(textArea, arg0.getX(), arg0.getY());
        }
    }
}
