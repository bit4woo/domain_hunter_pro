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
	private GUIMain guiMain;
	
	public TextAreaListener(GUIMain guiMain, JScrollPanelWithHeader panel){
		this.guiMain = guiMain;
		this.TextAreaPanel = panel;
	}

	public void saveDomainDataToDB(){
		File dbfile = guiMain.getCurrentDBFile();
		DomainDao dao = new DomainDao(dbfile);

		Set<String> content = Commons.getSetFromTextArea(TextAreaPanel.getTextArea());
		TextAreaType type = TextAreaPanel.getTextAreaType();
		dao.createOrUpdateByType(content, type);

		DomainManager dominResult = guiMain.getDomainPanel().getDomainResult();
		switch (type) {
		
		case SubDomain:
			//dao.createOrUpdateByType(content, type);
			dominResult.getSubDomainSet().clear();
			dominResult.getSubDomainSet().addAll(content);
			dominResult.getSummary();
			break;
		case RelatedDomain:
			dominResult.getRelatedDomainSet().clear();
			dominResult.getRelatedDomainSet().addAll(content);
			dominResult.getSummary();
			break;
		case SimilarDomain:
			dominResult.getSimilarDomainSet().clear();
			dominResult.getSimilarDomainSet().addAll(content);
			dominResult.getSummary();
			break;
		case Email:
			dominResult.getEmailSet().clear();
			dominResult.getEmailSet().addAll(content);
			dominResult.getSummary();
			break;
		case SimilarEmail:
			dominResult.getSimilarEmailSet().clear();
			dominResult.getSimilarEmailSet().addAll(content);
			dominResult.getSummary();
			break;
		case IPSetOfSubnet:
			dominResult.getIPSetOfSubnet().clear();
			dominResult.getIPSetOfSubnet().addAll(content);
			dominResult.getSummary();
			break;
		case IPSetOfCert:
			dominResult.getIPSetOfCert().clear();
			dominResult.getIPSetOfCert().addAll(content);
			dominResult.getSummary();
			break;
		case SpecialPortTarget:
			dominResult.getSpecialPortTargets().clear();
			dominResult.getSpecialPortTargets().addAll(content);
			dominResult.getSummary();
			break;
		case PackageName:
			dominResult.getPackageNameSet().clear();
			dominResult.getPackageNameSet().addAll(content);
			dominResult.getSummary();
			break;
		case BlackIP:
			dominResult.getNotTargetIPSet().clear();
			dominResult.getNotTargetIPSet().addAll(content);
			dominResult.getSummary();
			break;
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

