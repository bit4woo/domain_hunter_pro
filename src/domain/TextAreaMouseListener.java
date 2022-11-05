package domain;

import javax.swing.*;

import GUI.GUIMain;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TextAreaMouseListener extends MouseAdapter{

    private final JTextArea textArea;
	private GUIMain guiMain;

    public TextAreaMouseListener(GUIMain guiMain,JTextArea textArea){
        this.textArea = textArea;
        this.guiMain = guiMain;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    	JPopupMenu jp = new TextAreaMenu(guiMain,textArea);
		jp.add("Be Happy ^_^");
        if (arg0.getButton() == MouseEvent.BUTTON3) {
            // 弹出菜单
            jp.show(textArea, arg0.getX(), arg0.getY());
        }
    }
}
