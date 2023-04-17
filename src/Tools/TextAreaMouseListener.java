package Tools;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

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
