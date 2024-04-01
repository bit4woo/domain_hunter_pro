package InternetSearch;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import GUI.GUIMain;
import burp.BurpExtender;

public class SearchPanel extends JPanel {

	JLabel lblSummary;

	JTabbedPane centerPanel;
	GUIMain guiMain;
	PrintWriter stdout;
	PrintWriter stderr;

	public SearchPanel(GUIMain guiMain) {
		this.guiMain = guiMain;

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));
		this.add(createButtonPanel(), BorderLayout.NORTH);
		centerPanel = new JTabbedPane();

		this.add(centerPanel,BorderLayout.CENTER);
	}

	public void addSearchTab(String tabName,List<SearchResultEntry> entries) {

		SearchTableModel searchTableModel= new SearchTableModel(this.guiMain,entries);
		SearchTable searchTable = new SearchTable(this.guiMain,searchTableModel);
		JScrollPane scrollPane = new JScrollPane(searchTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area


		//用一个panel实现tab那个小块
		JPanel tabPanel = new JPanel(new BorderLayout());

		JLabel titleLabel = new JLabel(tabName);
		tabPanel.add(titleLabel, BorderLayout.CENTER);

		JButton closeButton = new JButton("x");
		closeButton.setMargin(new Insets(0, 2, 0, 2)); // 设置按钮边距
		closeButton.setFocusable(false); // 禁用焦点
		closeButton.addActionListener(new CloseTabListener(centerPanel, scrollPane));
		tabPanel.add(closeButton, BorderLayout.EAST);


		centerPanel.addTab(null, scrollPane);
		int index = centerPanel.getTabCount() - 1;
		centerPanel.setTabComponentAt(index, tabPanel);
	}


	static class CloseTabListener implements ActionListener {
		private JTabbedPane tabbedPane;
		private Component component;

		public CloseTabListener(JTabbedPane tabbedPane, Component component) {
			this.tabbedPane = tabbedPane;
			this.component = component;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			tabbedPane.remove(component);
		}
	}

	public JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		/**
		JButton buttonSearch = new JButton("Search");
		SearchTextField textFieldSearch = new SearchTextField("",buttonSearch);
		buttonPanel.add(textFieldSearch);

		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				searchTable.search(keyword);
			}
		});
		buttonPanel.add(buttonSearch);
		 */


		lblSummary = new JLabel("^_^");
		buttonPanel.add(lblSummary);
		buttonPanel.setToolTipText("");

		return buttonPanel;
	}
}
