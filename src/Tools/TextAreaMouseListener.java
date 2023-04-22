package Tools;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import GUI.GUIMain;

public class TextAreaMouseListener extends MouseAdapter{
	JTextArea textArea;
	String selected;
	private GUIMain guiMain;

	public TextAreaMouseListener(GUIMain guiMain,JTextArea textArea){
		this.textArea = textArea;
		this.guiMain = guiMain;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON3) {
			// 弹出菜单
			JPopupMenu jp = new TextAreaMenu(guiMain,textArea);
			jp.show(textArea, arg0.getX(), arg0.getY());
		}
		
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			mouseEntered(arg0);
		}
	}

	public void mouseEntered(MouseEvent arg0) {
		new SwingWorker() {
			@Override
			protected Object doInBackground() throws Exception {
				String tips = ((SuperJTextArea) textArea).getTipsToShow();
				textArea.setToolTipText(tips);
				return null;
			}
		}.execute();
	}
}
