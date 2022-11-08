package domain;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

public class TextAreaMouseListener extends MouseAdapter{

    private final JTextArea textArea;
	private DomainPanel domainPanel;

    public TextAreaMouseListener(DomainPanel domainPanel,JTextArea textArea){
        this.textArea = textArea;
        this.domainPanel = domainPanel;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    	JPopupMenu jp = new TextAreaMenu(domainPanel,textArea);
		jp.add("Be Happy ^_^");
        if (arg0.getButton() == MouseEvent.BUTTON3) {
            // 弹出菜单
            jp.show(textArea, arg0.getX(), arg0.getY());
        }
    }
}
