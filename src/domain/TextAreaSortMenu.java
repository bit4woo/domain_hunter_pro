package domain;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import Tools.DomainComparator;
import Tools.LengthComparator;
import burp.BurpExtender;

public class TextAreaSortMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	TextAreaSortMenu(final JTextArea TextArea){

        try{
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        }catch (Exception e){
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }
        
        List<String> selectedItems = Arrays.asList(TextArea.getText().split(System.lineSeparator()));
        
        JMenuItem Sort = new JMenuItem(new AbstractAction("Sort") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(selectedItems);
				TextArea.setText(String.join(System.lineSeparator(), selectedItems));
			}
		});
        this.add(Sort);
        
        JMenuItem SortByLength = new JMenuItem(new AbstractAction("Sort By Length") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(selectedItems,new LengthComparator());
				TextArea.setText(String.join(System.lineSeparator(), selectedItems));
			}
		});
        this.add(SortByLength);
        
        JMenuItem SortDomain = new JMenuItem(new AbstractAction("Sort Domain") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				Collections.sort(selectedItems,new DomainComparator());
				TextArea.setText(String.join(System.lineSeparator(), selectedItems));
			}
		});
        this.add(SortDomain);
        SortDomain.setToolTipText("Just for domain");
	}
}
