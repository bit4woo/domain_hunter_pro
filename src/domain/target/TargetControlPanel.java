package domain.target;

import GUI.GUIMain;
import domain.DomainPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class TargetControlPanel extends JPanel {
	public TargetControlPanel() {
		setBorder(new LineBorder(new Color(0, 0, 0)));

		JButton addButton = new JButton("Add");
		addButton.setToolTipText("add Top-Level domain");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	if (DomainPanel.getDomainResult() == null) {
					DomainPanel.createOrOpenDB();
				} else {
					String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
					TargetEntry entry = new TargetEntry(enteredRootDomain);
					DomainPanel.fetchTargetModel().addRowIfValid(entry);
					DomainPanel.saveDomainDataToDB();
				}
			}
		});
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		add(addButton);


		JButton addButton1 = new JButton("Add+");
		addButton1.setToolTipText("add Multiple-Level domain");
		addButton1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (DomainPanel.getDomainResult() == null) {
					DomainPanel.createOrOpenDB();
				} else {
					String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
					TargetEntry entry = new TargetEntry(enteredRootDomain,false);
					DomainPanel.fetchTargetModel().addRowIfValid(entry);
					DomainPanel.saveDomainDataToDB();
				}
			}
		});
		add(addButton1);


		JButton removeButton = new JButton("Remove");
		add(removeButton);
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int[] rowindexs = DomainPanel.getTargetTable().getSelectedRows();
				for (int i = 0; i < rowindexs.length; i++) {
					rowindexs[i] = DomainPanel.getTargetTable().convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
				}
				Arrays.sort(rowindexs);

				TargetTableModel domainTableModel = DomainPanel.fetchTargetModel();
				for (int i = rowindexs.length - 1; i >= 0; i--) {
					domainTableModel.removeRow(rowindexs[i]);
				}
				// will trigger tableModel listener---due to "fireTableRowsDeleted" in removeRow function!
			}
		});

		JButton blackButton = new JButton("Black");
		add(blackButton);
		blackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedToBalck();
			}
		});

		JButton btnFresh = new JButton("Fresh");
		add(btnFresh);
		btnFresh.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				//to clear sub and similar domains
				DomainPanel.getDomainResult().freshBaseRule();
				GUIMain.getDomainPanel().showDataToDomainGUI();
				DomainPanel.saveDomainDataToDB();
			}
		});

		/*
		JButton btnCopy = new JButton("Copy");
		btnCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(domainResult.fetchRootDomains());
				clipboard.setContents(selection, null);
			}
		});
		btnCopy.setToolTipText("Copy Root Domains To ClipBoard");
		add(btnCopy);*/
	}

	public static void selectedToBalck(){
		int option = JOptionPane.showConfirmDialog(null, "Are you sure set target to black?", "WARNING",JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			int[] rowindexs = DomainPanel.getTargetTable().getSelectedRows();
			for (int i = 0; i < rowindexs.length; i++) {
				rowindexs[i] = DomainPanel.getTargetTable().convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
			}
			Arrays.sort(rowindexs);

			TargetTableModel domainTableModel = DomainPanel.fetchTargetModel();
			for (int i = rowindexs.length - 1; i >= 0; i--) {
				TargetEntry entry = domainTableModel.getValueAt(rowindexs[i]);
				entry.setBlack(true);
				domainTableModel.updateRow(entry);
			}
		}
	}
}
