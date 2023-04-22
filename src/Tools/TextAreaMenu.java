package Tools;

import burp.BurpExtender;
import burp.Commons;
import domain.DomainPanel;
import utils.PortScanUtils;

import javax.swing.*;

import GUI.GUIMain;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
		
		this.add(genPortScanCmd);
	}
}
