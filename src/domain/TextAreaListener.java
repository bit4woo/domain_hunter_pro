package domain;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import GUI.GUIMain;

/*
//用于各种domain的手动编辑后的保存（不包含rootdomain）
 */
public class TextAreaListener implements DocumentListener {

	@Override
	public void removeUpdate(DocumentEvent e) {
		if (DomainPanel.isListenerIsOn()) {
			GUIMain.getDomainPanel().saveTextAreas();
			DomainPanel.saveDomainDataToDB();
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		if (DomainPanel.isListenerIsOn()) {
			GUIMain.getDomainPanel().saveTextAreas();
			DomainPanel.saveDomainDataToDB();
		}
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		if (DomainPanel.isListenerIsOn()) {
			GUIMain.getDomainPanel().saveTextAreas();
			DomainPanel.saveDomainDataToDB();
		}
	}
}

