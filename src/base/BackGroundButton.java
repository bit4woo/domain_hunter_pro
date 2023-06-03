package base;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.SwingWorker;

public abstract class BackGroundButton extends JButton {

	public BackGroundButton(String string) {
		super(string);
		addActionListener();
	}

	public final void addActionListener() {
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						setEnabled(false);
						action();
						return null;
					}

					@Override
					protected void done() {
						setEnabled(true);
					}
				};
				worker.execute();
			}
		});
	}

	/**
	 * 使用时重写这个函数，来实现自己的逻辑
	 */
	protected abstract void action();

	public static void main() {
		new BackGroundButton("aaa") {
			@Override
			protected void action() {

			}
		};
	}

}
