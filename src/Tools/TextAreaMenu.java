package Tools;

import burp.BurpExtender;
import burp.Commons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.List;

public class TextAreaMenu extends JPopupMenu {

	PrintWriter stdout;
	PrintWriter stderr;
	TextAreaMenu(final String selectedText){

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
	}
}
