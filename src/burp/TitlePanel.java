package burp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class TitlePanel extends JPanel {

    private JPanel titlePanel;//this

    private JPanel buttonPanel;

    public TitlePanel() {
		titlePanel = new JPanel();
		titlePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		titlePanel.setLayout(new BorderLayout(0, 0));

		buttonPanel = new JPanel();
		titlePanel.add(buttonPanel, BorderLayout.NORTH);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton btnGettitle = new JButton("Get Title");
		btnGettitle.setToolTipText("A fresh start");
		btnGettitle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//https://stackabuse.com/how-to-use-threads-in-java-swing/

				//method one: // don't need to wait threads in getAllTitle to exits
				//but hard to know the finish time of task
				//// Runs inside of the Swing UI thread
				/*			    SwingUtilities.invokeLater(new Runnable() {
			        public void run() {// don't need to wait threads in getAllTitle to exits
			        	btnGettitle.setEnabled(false);
			        	getAllTitle();
			        	btnGettitle.setEnabled(true);
			        	//domainResult.setLineEntries(TitletableModel.getLineEntries());
			        }
			    });*/

				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnGettitle.setEnabled(false);
						getAllTitle();
						saveDialog(true);
						btnGettitle.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							btnGettitle.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(btnGettitle);

		btnGetExtendtitle = new JButton("Get Extend Title");
		btnGetExtendtitle.setToolTipText("Get title of the host that in same subnet,you should do this after get domain title done!");
		btnGetExtendtitle.setEnabled(true);//default is false,only true after "get title" is done.
		btnGetExtendtitle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnGetExtendtitle.setEnabled(false);
						getExtendTitle();
						saveDialog(true);
						btnGetExtendtitle.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							btnGetExtendtitle.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(btnGetExtendtitle);

		JButton btnGetSubnet = new JButton("Get Subnet");
		btnGetSubnet.setEnabled(true);
		btnGetSubnet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {

						btnGetSubnet.setEnabled(false);
						int result = JOptionPane.showConfirmDialog(null,"Just get IP Subnets of [Current] lines ?");
						String subnetsString;
						if (result == JOptionPane.YES_OPTION) {
							subnetsString = getSubnet(true);
						}else {
							subnetsString = getSubnet(false);
						}
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection selection = new StringSelection(subnetsString);
						clipboard.setContents(selection, null);
						stdout.print(subnetsString);
						btnGetSubnet.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							btnGetSubnet.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(btnGetSubnet);


		btnSaveState = new JButton("Save");
		btnSaveState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnSaveState.setEnabled(false);
						saveDialog(true);//both tilte and domain
						//saveDBfileToExtension();
						btnSaveState.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						btnSaveState.setEnabled(true);
					}
				};
				worker.execute();
			}
		});
		btnSaveState.setToolTipText("Save Data To DataBase");
		buttonPanel.add(btnSaveState);


		InputMap inputMap1 = btnSaveState.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke Save = KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK); //Ctrl+S
		inputMap1.put(Save, "Save");

		btnSaveState.getActionMap().put("Save", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnSaveState.setEnabled(false);
						saveDialog(true);
						//saveDBfileToExtension();
						btnSaveState.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						btnSaveState.setEnabled(true);
					}
				};
				worker.execute();
			}
		});

		textFieldSearch = new JTextField("");
		textFieldSearch.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (textFieldSearch.getText().equals("Input text to search")) {
					textFieldSearch.setText("");
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				/*
				 * if (textFieldSearch.getText().equals("")) {
				 * textFieldSearch.setText("Input text to search"); }
				 */

			}
		});

		textFieldSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText().trim();
				titleTable.search(keyword);

			}
		});
		textFieldSearch.setColumns(30);
		buttonPanel.add(textFieldSearch);


		JButton buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText().trim();
				titleTable.search(keyword);
			}
		});
		buttonPanel.add(buttonSearch);

		rdbtnHideCheckedItems = new JRadioButton("Hide Checked");
		rdbtnHideCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = BurpExtender.textFieldSearch.getText().trim();
				titleTable.search(keyword);
				//lineTable.getModel().unHideLines();
			}
		});
		buttonPanel.add(rdbtnHideCheckedItems);

		JButton btnRefresh = new JButton("Refresh");//主要目的是隐藏新标注的条目，代替自动隐藏
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = BurpExtender.textFieldSearch.getText().trim();
				titleTable.search(keyword);
			}
		});
		buttonPanel.add(btnRefresh);

		JButton btnStatus = new JButton("status");
		btnStatus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				digStatus();
			}
		});
		btnStatus.setToolTipText("Show Status Of Digging.");
		buttonPanel.add(btnStatus);

		lblSummaryOfTitle = new JLabel("      ^_^");
		buttonPanel.add(lblSummaryOfTitle);




		///need to replace this part with LineTableModel and LineTable
		//		table_1 = new JTable();
		//		scrollPaneRequests.setViewportView(table_1);
		//LineTableModel will replace this table


		return TitlePanel;
	}

}
