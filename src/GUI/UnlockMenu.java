package GUI;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 */
public class UnlockMenu extends JPopupMenu{
	GUIMain gui;
	public JMenuItem lockMenu;

	public UnlockMenu(GUIMain gui){
		this.gui = gui;
		
		JMenuItem lock = new JMenuItem(new AbstractAction("Lock&Unlock")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				gui.lockUnlock();
			}
		});
		this.add(lock);
	}
}
