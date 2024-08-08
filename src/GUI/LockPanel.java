package GUI;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class LockPanel extends JPanel {
	
	private GUIMain gui;

	public LockPanel(GUIMain gui) {
		this.gui = gui;
		setLayout(new GridBagLayout());
		
		JButton btnUnlock = new JButton("Unlock");

		btnUnlock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.lockUnlock();
			}
		});

		int rowIndex = 0;	
		add(btnUnlock, new MyGridBagLayout(++rowIndex,1));
	
	}
}
