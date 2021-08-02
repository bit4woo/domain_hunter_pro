package domain;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

public class TextAreaMouseListener extends MouseAdapter{
	JTextArea textArea;
	String selected;
	
	public TextAreaMouseListener(JTextArea textArea){
		this.textArea = textArea;
	}
	
    @Override
    public void mouseClicked(MouseEvent arg0) {
    	JPopupMenu jp;
    	this.selected = textArea.getSelectedText();
    	if (selected != null && selected !="") {
    		jp = new TextAreaMenu(selected);

    	}else {
            jp = new TextAreaSortMenu(textArea);
            jp.add("^_^");
    	}
        if (arg0.getButton() == MouseEvent.BUTTON3) {
            // 弹出菜单
            jp.show(textArea, arg0.getX(), arg0.getY());
        }
    }
}
