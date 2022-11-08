package domain;

import java.io.File;
import java.util.Set;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import GUI.GUIMain;
import GUI.JScrollPanelWithHeader;
import burp.Commons;
import dao.DomainDao;

/**
 * 保存TextArea的变更
 */
public class TextAreaListener implements DocumentListener {
	JScrollPanelWithHeader TextAreaPanel;
	private DomainPanel domainPanel;
	
	public TextAreaListener(DomainPanel domainPanel, JScrollPanelWithHeader panel){
		this.domainPanel = domainPanel;
		this.TextAreaPanel = panel;
	}

	/**
	 * 当UI中的数据发生变化时,需要同步到数据模型、并写入数据库
	 */
	public void saveToDBAndSyncModel(){
		if (domainPanel.isListenerIsOn()){
			DomainDao dao = new DomainDao(domainPanel.getGuiMain().currentDBFile);
			Set<String> content = Commons.getSetFromTextArea(TextAreaPanel.getTextArea());
			TextAreaType type = TextAreaPanel.getTextAreaType();
			domainPanel.getDomainResult().fillContentByType(type,content);
			dao.createOrUpdateByType(content, type);

		}
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		saveToDBAndSyncModel();

	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		saveToDBAndSyncModel();
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		saveToDBAndSyncModel();
	}
}

