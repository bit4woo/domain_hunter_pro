package Tools;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.IPAddressUtils;
import com.bit4woo.utilbox.utils.TextUtils;

import GUI.GUIMain;
import InternetSearch.APISearchAction;
import InternetSearch.SearchEngine;
import burp.BurpExtender;
import config.ConfigManager;
import config.ConfigName;
import domain.DomainManager;
import domain.target.AssetTrustLevel;
import utils.PortScanUtils;

public class TextAreaMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;

	TextAreaMenu(GUIMain guiMain,JTextArea textArea){
		String selectedText = textArea.getSelectedText();

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		List<String> selectedItems = TextUtils.textToLines(selectedText);

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
					String nmapPath = ConfigManager.getStringConfigByKey(ConfigName.PortScanCmd);
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
						if (IPAddressUtils.isValidIPv4MayPort(item)) {
							domainResult.getSpecialPortTargets().add(item);
						}
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				guiMain.getDomainPanel().saveDomainDataToDB();
			}
		});
		
		
		JMenuItem addToTarget = new JMenuItem(new AbstractAction("Add To Target") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
				for (String item:selectedItems) {
					try {
						if (IPAddressUtils.isValidIPv4MayPort(item)) {
							domainResult.getSpecialPortTargets().add(item);
						}else {
							domainResult.addToTargetAndSubDomain(item,true);
						}
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				guiMain.getDomainPanel().saveDomainDataToDB();
			}
		});
		
		
		JMenuItem addToTargetConfirm = new JMenuItem(new AbstractAction("Add To Target(Confirm)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
				for (String item:selectedItems) {
					try {
						if (IPAddressUtils.isValidIPv4MayPort(item)) {
							domainResult.getSpecialPortTargets().add(item);
						}else {
							domainResult.addToTargetAndSubDomain(item,true,AssetTrustLevel.Confirm);
						}
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				guiMain.getDomainPanel().saveDomainDataToDB();
			}
		});
		
		
		JMenuItem addToTargetWithComment = new JMenuItem(new AbstractAction("Add To Target With Comment") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

				String comment = JOptionPane.showInputDialog("Comment", "");
				if (StringUtils.isBlank(comment)) {
					return;
				}

				DomainManager domainResult = guiMain.getDomainPanel().getDomainResult();
				for (String item:selectedItems) {
					try {
						if (IPAddressUtils.isValidIPv4MayPort(item)) {
							domainResult.getSpecialPortTargets().add(item);
						}else {
							domainResult.addToTargetAndSubDomain(item,true,AssetTrustLevel.Confirm,comment);
						}
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
				guiMain.getDomainPanel().saveDomainDataToDB();
			}
		});

		JMenuItem doOnlineSearch = new JMenuItem(new AbstractAction("Do Online Search") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (String item:selectedItems) {
					try {
						//逻辑和search按钮一样 InternetSearch.SearchPanel.createButtonPanel()

						APISearchAction.DoSearchAllInOnAtBackGround(null, item, SearchEngine.getAssetSearchEngineList());
						
					} catch (Exception e2) {
						e2.printStackTrace(stderr);
					}
				}
			}
		});
		
		this.add(genPortScanCmd);
		this.add(addToTarget);
		this.add(addToTargetConfirm);
		this.add(addToTargetWithComment);
		this.add(doOnlineSearch);
	}
}
