package domain;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TextAreaMouseListener extends MouseAdapter{

    private final JTextArea textArea;

    public TextAreaMouseListener(JTextArea textArea){
        this.textArea = textArea;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    	JPopupMenu jp = new TextAreaMenu(textArea);
		jp.add("Be Happy ^_^");
        if (arg0.getButton() == MouseEvent.BUTTON3) {
            // 弹出菜单
            jp.show(textArea, arg0.getX(), arg0.getY());
        }
    }
}
