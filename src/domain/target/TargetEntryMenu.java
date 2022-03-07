package domain.target;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import burp.BurpExtender;
import burp.Commons;
import domain.DomainPanel;
import domain.target.TargetTable;

public class TargetEntryMenu extends JPopupMenu {


	private static final long serialVersionUID = 1L;
	PrintWriter stdout = BurpExtender.getStdout();
	PrintWriter stderr = BurpExtender.getStderr();
	private static TargetTable rootDomainTable;

	public TargetEntryMenu(final TargetTable rootDomainTable, final int[] rows, final int columnIndex){
		this.rootDomainTable = rootDomainTable;

		JMenuItem getSubDomainsOf = new JMenuItem(new AbstractAction("Get All Subdomin Of This") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String results = "";
				for (int row:rows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					String line = DomainPanel.getDomainResult().fetchSubDomainsOf(rootDomain);
					results = results+System.lineSeparator()+line;
				}

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(results);
				clipboard.setContents(selection, null);
			}
		});

		JMenuItem whoisItem = new JMenuItem(new AbstractAction("Whois") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				for (int row:rows) {
					String rootDomain = (String) rootDomainTable.getTargetModel().getValueAt(row,0);
					try {
						Commons.browserOpen("https://whois.chinaz.com/"+rootDomain,null);
						Commons.browserOpen("https://www.whois.com/whois/"+rootDomain,null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		JMenuItem addToBlackItem = new JMenuItem(new AbstractAction("Add To Black List") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				TargetControlPanel.selectedToBalck();
			}
		});

		this.add(getSubDomainsOf);
		this.add(whoisItem);
		this.add(addToBlackItem);
	}

}
