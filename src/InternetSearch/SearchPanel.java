package InternetSearch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.SystemUtils;
import com.bit4woo.utilbox.utils.UrlUtils;
import com.google.gson.Gson;

import GUI.GUIMain;
import burp.BurpExtender;
import title.WebIcon;

public class SearchPanel extends JPanel {

	JLabel lblSummary;

	JTabbedPane centerTabbedPanel;
	GUIMain guiMain;
	PrintWriter stdout;
	PrintWriter stderr;
	private Set<String> selectedTabs = new HashSet<>(); // 用于存储已选择过的Tab名称

	public static void main(String[] args) {
		test();
	}

	public static void test() {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("test");
			frame.setSize(400, 300);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);

			SearchResultEntry test = new SearchResultEntry();
			test.setHost("8.8.8.8");
			test.setPort(88);
			test.setProtocol("https");

			SearchTableModel searchTableModel = new SearchTableModel(null,
					new ArrayList<SearchResultEntry>(Collections.singletonList(test)));
			SearchTable searchTable = new SearchTable(null, searchTableModel, "searchContent");

			frame.getContentPane().add(searchTable);
		});
	}

	public static void test1() {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("test");
			SearchPanel spanel = new SearchPanel(null);
			frame.getContentPane().add(spanel);
			frame.setSize(400, 300);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
			SearchResultEntry test = new SearchResultEntry();
			test.setHost("8.8.8.8");
			test.setPort(88);
			test.setProtocol("https");
			spanel.addSearchTab("111", new ArrayList<SearchResultEntry>(Collections.singletonList(test)),
					new ArrayList<String>(Collections.singletonList("xxx")), "SourceTabName");
		});
	}

	public SearchPanel(GUIMain guiMain) {
		this.guiMain = guiMain;

		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));
		this.add(createButtonPanel(), BorderLayout.NORTH);
		centerTabbedPanel = new JTabbedPane();

		this.add(centerTabbedPanel, BorderLayout.CENTER);
	}

	// 外部函数用于改变指定Tab的颜色
	public void changeTabColor(String tabName, Color color) {
		for (int i = 0; i < centerTabbedPanel.getTabCount(); i++) {
			Component tabComponent = centerTabbedPanel.getTabComponentAt(i);
			if (tabComponent instanceof JPanel) {
				JPanel tabPanel = (JPanel) tabComponent;
				JLabel label = (JLabel) tabPanel.getComponent(0);
				if (label.getText().equals(tabName)) {
					tabPanel.setBackground(color);
				}
			}
		}
	}

	/**
	 * @param tabName
	 * @param entries
	 * @param engines
	 * @param sourceTabName 当searchTable中右键搜索时，当前tab就是新建tab的source tab.
	 */
	public void addSearchTab(String tabName, List<SearchResultEntry> entries, List<String> engines,
			String sourceTabName) {
		JPanel tabPanelBody = new JPanel();// Tab的最外层容器面板
		tabPanelBody.setLayout(new BorderLayout(0, 0));

		SearchTableModel searchTableModel = new SearchTableModel(this.guiMain, entries);

		// 注意，传递当前搜索结果、和对应的搜索内容（即tabName）
		SearchTable searchTable = new SearchTable(this.guiMain, searchTableModel, tabName);
		JScrollPane scrollPane = new JScrollPane(searchTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);// table area

		JLabel status = new JLabel("^_^");
		status.setText(getStatusInfo(entries, engines, sourceTabName));

		tabPanelBody.add(scrollPane, BorderLayout.CENTER);
		tabPanelBody.add(status, BorderLayout.SOUTH);
		// tabPanelBody body部分；其中存放 可滚动的表 + 状态栏。

		// 用一个panel实现tab header那个小块
		JPanel tabPanelHeader = new JPanel(new BorderLayout());

		JLabel titleLabel = new JLabel(tabName);
		tabPanelHeader.add(titleLabel, BorderLayout.CENTER);

		JButton closeButton = new JButton("x");
		closeButton.setMargin(new Insets(0, 2, 0, 2)); // 设置按钮边距
		closeButton.setFocusable(false); // 禁用焦点
		closeButton.addActionListener(new CloseTabListener(centerTabbedPanel, tabPanelBody, tabName));
		tabPanelHeader.add(closeButton, BorderLayout.EAST);

		// 用于存储 当前搜索的来源tab名称
		if (StringUtils.isEmpty(sourceTabName)) {
			sourceTabName = "";
		}
		tabPanelHeader.putClientProperty("sourceTabName", sourceTabName);

		// centerTabbedPanel 是存储多个Tab的外层容器。关联header和body
		// Tab和TabComponent一一对应，tab就是 实体body，TabComponent就是header
		centerTabbedPanel.addTab(null, tabPanelBody);
		int index = centerTabbedPanel.getTabCount() - 1;
		centerTabbedPanel.setTabComponentAt(index, tabPanelHeader);

		centerTabbedPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					showPopupMenu(centerTabbedPanel, e);
				} else if (SwingUtilities.isLeftMouseButton(e)) {
					int tabIndex = centerTabbedPanel.getSelectedIndex();
					if (tabIndex != -1) {
						Component selectedComponent = centerTabbedPanel.getTabComponentAt(tabIndex);
						if (selectedComponent instanceof JPanel) {
							JPanel selectedTabPanel = (JPanel) selectedComponent;
							JLabel label = (JLabel) selectedTabPanel.getComponent(0);
							if (label.getText().equals(tabName)) {
								selectedTabs.add(tabName); // 将此Tab名称添加到已选择过的集合中
								selectedTabPanel.setBackground(Color.GRAY); // 设置Tab颜色为灰色
							}
						}
					}
				}
			}
		});
	}

	public String getStatusInfo(List<SearchResultEntry> entries, List<String> engines, String sourceTab) {
		Map<String, Integer> status = new HashMap<>();
		Set<String> engineSet = new HashSet<>(engines); // 使用 Set 提高 contains() 查找效率
		int unknown = 0;

		for (SearchResultEntry entry : entries) {
			String source = entry.getSource();
			if (engineSet.contains(source)) {
				status.compute(source, (k, v) -> (v == null) ? 1 : v + 1);
			} else {
				unknown++;
			}
		}

		if (unknown > 0) {
			status.put("unknown", unknown);
		}

		// **去除值为 0 的元素**
		// status.entrySet().removeIf(entry -> entry.getValue() == 0);

		String str = new Gson().toJson(status);

		return "Source: " + sourceTab + " | " + str;
	}

	static class CloseTabListener implements ActionListener {
		private final String tabName;
		private JTabbedPane tabbedPane;
		private Component component;

		public CloseTabListener(JTabbedPane tabbedPane, Component component, String tabName) {
			this.tabbedPane = tabbedPane;
			this.component = component;
			this.tabName = tabName;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			APISearchAction.searchedContent.remove(tabName);
			tabbedPane.remove(component);
		}
	}

	/**
	 * 显示右键菜单
	 *
	 * @param tabbedPane 是存储多个Tab的外层容器，也就是centerPanel
	 * @param e
	 */
	private void showPopupMenu(JTabbedPane tabbedPane, MouseEvent e) {
		JPopupMenu popupMenu = new JPopupMenu();

		int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
		if (tabIndex == -1) {
			return;
		}
		// 添加菜单项：关闭当前 tab
		JMenuItem closeCurrentTabMenuItem = new JMenuItem("Close Current Tab");
		closeCurrentTabMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// 需要先获取tabText，再移除tab，否则会获取失败
				APISearchAction.searchedContent.remove(getTabTextByIndex(tabIndex));
				tabbedPane.remove(tabIndex);
			}
		});
		popupMenu.add(closeCurrentTabMenuItem);

		// 添加菜单项：关闭所有 tab
		JMenuItem closeAllTabsMenuItem = new JMenuItem("Close All Tabs");
		closeAllTabsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				APISearchAction.searchedContent.clear();
				tabbedPane.removeAll();
			}
		});
		popupMenu.add(closeAllTabsMenuItem);

		// 添加菜单项：关闭至左边
		JMenuItem closeTabsToLeftMenuItem = new JMenuItem("Close Tabs to Left");
		closeTabsToLeftMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = tabIndex - 1; i >= 0; i--) {
					APISearchAction.searchedContent.remove(getTabTextByIndex(i));
					tabbedPane.remove(i);
				}
			}
		});
		popupMenu.add(closeTabsToLeftMenuItem);

		// 添加菜单项：关闭至右边
		JMenuItem closeTabsToRightMenuItem = new JMenuItem("Close Tabs to Right");
		closeTabsToRightMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = tabbedPane.getTabCount() - 1; i > tabIndex; i--) {
					APISearchAction.searchedContent.remove(getTabTextByIndex(i));
					tabbedPane.remove(i);
				}
			}
		});
		popupMenu.add(closeTabsToRightMenuItem);

		JMenuItem copyTabNameMenuItem = new JMenuItem("Copy Tab Name");
		copyTabNameMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SystemUtils.writeToClipboard(getTabTextByIndex(tabIndex));
			}
		});
		popupMenu.add(copyTabNameMenuItem);

		JMenuItem gotoSourceTabMenuItem = new JMenuItem("Goto Source Tab");
		gotoSourceTabMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// tabbedPane 是存储多个Tab的外层容器，实例就是centerPanel
				JPanel currentTabHeaderPanel = (JPanel) tabbedPane.getTabComponentAt(tabIndex);
				String soureTabName = (String) currentTabHeaderPanel.getClientProperty("sourceTabName");
				if (StringUtils.isNotEmpty(soureTabName)) {
					setSelectedTabTitle(tabbedPane, soureTabName);
				}
			}
		});
		popupMenu.add(gotoSourceTabMenuItem);

		// 显示右键菜单
		popupMenu.show(tabbedPane, e.getX(), e.getY());
	}

	public String getTabTextByIndex(int i) {
		try {
			// 获得tab header那个小方块中的组件面包
			JPanel panel = ((JPanel) centerTabbedPanel.getTabComponentAt(i));

			// 获取面板中的第一个组件，即Label，最后获取其中文本
			JLabel lab = (JLabel) panel.getComponent(0);
			return lab.getText();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public Set<String> getAlreadySearchContent() {
		HashSet<String> result = new HashSet<String>();
		for (int i = centerTabbedPanel.getTabCount() - 1; i >= 0; i--) {
			result.add(getTabTextByIndex(i));
		}
		return result;
	}

	/**
	 * @param tabbedPane centerPanel
	 * @param title
	 */
	public void setSelectedTabTitle(JTabbedPane tabbedPane, String title) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			if (tabbedPane.getTitleAt(i).equals(title)) {
				tabbedPane.setSelectedIndex(i);
				return;
			}
		}
	}

	public JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JTextField textFieldSearch = new JTextField();
		textFieldSearch.setColumns(30);
		buttonPanel.add(textFieldSearch);

		JButton buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String content = textFieldSearch.getText();
				APISearchAction.DoSearchAllInOnAtBackGround(null, content, SearchEngine.getAssetSearchEngineList(),
						"buttonSearch");
			}
		});
		buttonPanel.add(buttonSearch);

		JButton buttonSearchAs = new JButton("Search As");
		buttonSearchAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String content = textFieldSearch.getText();

				String searchType = SearchType.choseSearchType();
				switch (searchType) {
				case SearchType.Email:
					APISearchAction.DoSearchAllInOnAtBackGround(searchType, content,
							SearchEngine.getEmailSearchEngineList(), "buttonSearchAs");
					break;
				case SearchType.IconHash:
					if (UrlUtils.isVaildUrl(content)) {
						byte[] imageData = WebIcon.downloadFavicon(content);
						if (imageData.length > 0) {
							content = WebIcon.getHash(imageData);
						}
					}
				default:
					APISearchAction.DoSearchAllInOnAtBackGround(searchType, content,
							SearchEngine.getAssetSearchEngineList(), "buttonSearchAs");
				}
			}
		});
		buttonPanel.add(buttonSearchAs);

		lblSummary = new JLabel("^_^");
		buttonPanel.add(lblSummary);
		buttonPanel.setToolTipText("");

		return buttonPanel;
	}
}
