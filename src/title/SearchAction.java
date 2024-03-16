package title;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;

import base.Commons;
import burp.BurpExtender;


interface Searchable {
    Set<String> getUrls(); // 声明需要实现的函数
}

public class SearchAction extends AbstractAction implements Searchable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1933197856582351336L;

	@Override
	public final void actionPerformed(ActionEvent e) {
		//加final关键词，避免子类重写这个函数，
		for (String url:getUrls()) {
			try {
				Commons.browserOpen(url, null);
			} catch (Exception err) {
				err.printStackTrace(BurpExtender.getStderr());
			}
		}
	}
	
	@Override
	public Set<String> getUrls() {
		return new HashSet<String>();
	}
}
