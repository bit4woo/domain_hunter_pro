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

	public void saveDomainDataToDB(){
		if (domainPanel.isListenerIsOn()){
			DomainDao dao = new DomainDao(domainPanel.getGuiMain().currentDBFile);
			Set<String> content = Commons.getSetFromTextArea(TextAreaPanel.getTextArea());
			TextAreaType type = TextAreaPanel.getTextAreaType();
			dao.createOrUpdateByType(content, type);
		}
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		saveDomainDataToDB();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		saveDomainDataToDB();
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		saveDomainDataToDB();
	}
}

