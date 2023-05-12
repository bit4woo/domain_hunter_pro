package Tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.SwingWorker;

/**
 * 实现一个默认就使用SwingWorker的ActionListener
 *
 */
public class ActionListenerWithSwingWorker implements ActionListener{

	protected JButton button;
	protected ActionEvent actionEvent;

	public ActionListenerWithSwingWorker(JButton button){
		this.button = button;
	}

	@Override
	public final void actionPerformed(ActionEvent e) {
		SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			
			@Override
			protected Map doInBackground() throws Exception {
				button.setEnabled(false);
				ActionListenerWithSwingWorker.this.actionEvent = e;
				action();
				return null;
			}

			@Override
			protected void done() {
				button.setEnabled(true);
			}
		};
		worker.execute();
	}

	/**
	 * 使用时重写这个函数，来实现自己的逻辑
	 */
	protected void action() {

	}
	
	
	public static void main() {
		new ActionListenerWithSwingWorker(new JButton("aaa")){
			@Override
			protected void action() {
				this.actionEvent =null;
			}
		};
	}

}