package Tools;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import GUI.GUIMain;
import base.Commons;
import burp.BurpExtender;
import burp.IPAddressUtils;
import domain.DomainManager;
import utils.PortScanUtils;

public class TextAreaMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	private GUIMain guiMain;
	private JTextArea textArea;
	private List<String> selectedItems = new ArrayList<>();;

	TextAreaMenu(GUIMain guiMain,JTextArea textArea){
		this.guiMain = guiMain;
		this.textArea = textArea;
		String selectedText = textArea.getSelectedText();
		if (selectedText != null && !selectedText.equalsIgnoreCase("")){
			selectedItems = Commons.textToLines(selectedText);
		}


		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		List<String> selectedItems = Commons.textToLines(selectedText);

		if (selectedItems.size() > 0){
			JMenuItem goToItem = new JMenuItem(new AbstractAction(selectedItems.size()+" items selected") {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {

				}
			});
			this.add(goToItem);
		}


		JMenuItem genPortScanCmd = new JMenuItem(new AbstractAction("Copy Port Scan Cmd") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					String nmapPath = guiMain.getConfigPanel().getLineConfig().getNmapPath();
					PortScanUtils.genCmdAndCopy(nmapPath, selectedItems);
				}
				catch (Exception e1)
				{
					e1.printStackTrace(stderr);
				}
			}
		});

		JMenuItem addTosubdomain = new JMenuItem(new AbstractAction("Add To Sub-domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
				for (String item:selectedItems) {
					try {
						domainResult.addToTargetAndSubDomain(item,true);
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				guiMain.getDomainPanel().saveDomainDataToDB();
			}
		});


		JMenuItem addToCustomAsset = new JMenuItem(new AbstractAction("Add To Custom Asset") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
				for (String item:selectedItems) {
					try {
						if (IPAddressUtils.isValidIP(item)) {
							domainResult.getSpecialPortTargets().add(item);
						}
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				guiMain.getDomainPanel().saveDomainDataToDB();
			}
		});

		this.add(genPortScanCmd);
		this.add(addTosubdomain);
		this.add(addToCustomAsset);
	}
}
