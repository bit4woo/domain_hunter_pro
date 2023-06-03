package base;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.SwingWorker;

public abstract class BackGroundActionListener implements ActionListener{

    /**
     * Invoked when an action occurs.
     * @param e the event to be processed
     */
    public void actionPerformed(ActionEvent e) {
    	SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			@Override
			protected Map doInBackground() throws Exception {
				action();
				return null;
			}

			@Override
			protected void done() {
				
			}
		};
		worker.execute();
    }
    
	/**
	 * 使用时重写这个函数，来实现自己的逻辑
	 */
	protected abstract void action();
}
