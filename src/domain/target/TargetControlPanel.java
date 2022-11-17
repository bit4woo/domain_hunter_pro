package domain.target;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;

import burp.BurpExtender;
import domain.DomainPanel;

public class TargetControlPanel extends JPanel {
	
	JRadioButton rdbtnAddRelatedToRoot;
	DomainPanel domainPanel;
	private JButton btnFresh;

	public JRadioButton getRdbtnAddRelatedToRoot() {
		return rdbtnAddRelatedToRoot;
	}

	public void setRdbtnAddRelatedToRoot(JRadioButton rdbtnAddRelatedToRoot) {
		this.rdbtnAddRelatedToRoot = rdbtnAddRelatedToRoot;
	}

	public TargetControlPanel(DomainPanel domainPanel) {
		this.domainPanel = domainPanel;
		
		setBorder(new LineBorder(new Color(0, 0, 0)));

		JButton addButton = new JButton("Add");
		addButton.setToolTipText("add Top-Level domain");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (domainPanel.getDomainResult() == null || domainPanel.getGuiMain().getCurrentDBFile() == null) {
					domainPanel.createOrOpenDB();
				} else {
					String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
					TargetEntry entry = new TargetEntry(enteredRootDomain);
					if(domainPanel.fetchTargetModel().addRowIfValid(entry)){
						btnFresh.doClick();
					}
				}
			}
		});


		JButton addButton1 = new JButton("Add+");
		addButton1.setToolTipText("add Multiple-Level domain");
		addButton1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (domainPanel.getDomainResult() == null) {
					domainPanel.createOrOpenDB();
				} else {
					String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
					TargetEntry entry = new TargetEntry(enteredRootDomain,false);
					if(domainPanel.fetchTargetModel().addRowIfValid(entry)){
						btnFresh.doClick();
					}
				}
			}
		});


		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int[] rowindexs = domainPanel.getTargetTable().getSelectedRows();
				for (int i = 0; i < rowindexs.length; i++) {
					rowindexs[i] = domainPanel.getTargetTable().convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
				}
				Arrays.sort(rowindexs);

				TargetTableModel domainTableModel = domainPanel.fetchTargetModel();
				for (int i = rowindexs.length - 1; i >= 0; i--) {
					domainTableModel.removeRow(rowindexs[i]);
				}
				// will trigger tableModel listener---due to "fireTableRowsDeleted" in removeRow function!
			}
		});

		
		JButton blackButton = new JButton("Black");
		blackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedToBalck();
			}
		});

		
		btnFresh = new JButton("Refresh");
		btnFresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnFresh.setEnabled(false);
						domainPanel.refreshShowSave();
						return null;
					}

					@Override
					protected void done(){
						btnFresh.setEnabled(true);
					}
				};
				worker.execute();
			}
		});

		rdbtnAddRelatedToRoot = new JRadioButton("Auto Add Related Domain To Root Domain");
		rdbtnAddRelatedToRoot.setVerticalAlignment(SwingConstants.TOP);
		rdbtnAddRelatedToRoot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rdbtnAddRelatedToRoot.setEnabled(false);
				try {
					domainPanel.getDomainResult().autoAddRelatedToRoot = rdbtnAddRelatedToRoot.isSelected();
					if (domainPanel.getDomainResult().autoAddRelatedToRoot) {
						domainPanel.getDomainResult().relatedToRoot();
						btnFresh.doClick();
					}
				} catch (Exception exception) {
					exception.printStackTrace(BurpExtender.getStderr());
				}
				rdbtnAddRelatedToRoot.setEnabled(true);
			}
		});
		rdbtnAddRelatedToRoot.setSelected(false);
		
		GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);
		
		this.add(addButton,new bagLayout(1,1));
		this.add(addButton1,new bagLayout(1,2));
		this.add(removeButton,new bagLayout(1,3));
		this.add(blackButton,new bagLayout(1,4));
		this.add(btnFresh,new bagLayout(1,5));
		
		bagLayout tmp = new bagLayout(2,1);
		tmp.gridwidth = 5;//可以占用5个单元格
		this.add(rdbtnAddRelatedToRoot,tmp);
	}

	public void selectedToBalck(){
		int option = JOptionPane.showConfirmDialog(null, "Are you sure set target to black?", "WARNING",JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			int[] rowindexs = domainPanel.getTargetTable().getSelectedRows();
			for (int i = 0; i < rowindexs.length; i++) {
				rowindexs[i] = domainPanel.getTargetTable().convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
			}
			Arrays.sort(rowindexs);

			TargetTableModel domainTableModel = domainPanel.fetchTargetModel();
			for (int i = rowindexs.length - 1; i >= 0; i--) {
				TargetEntry entry = domainTableModel.getValueAt(rowindexs[i]);
				entry.setBlack(true);
				domainTableModel.updateRow(entry);
			}
		}
	}
	
	class bagLayout extends GridBagConstraints {
		/**
		 * 采用普通的行列计数，从1开始
		 * @param row
		 * @param colum
		 */
		bagLayout(int row,int column){
			this.fill = GridBagConstraints.BOTH;
			this.insets = new Insets(0, 0, 5, 5);
			this.gridx = column-1;
			this.gridy = row-1;
		}
	}
}
