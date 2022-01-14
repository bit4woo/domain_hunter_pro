package title;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import burp.BurpExtender;
import burp.Commons;
import burp.IPAddress;
import domain.DomainPanel;
import thread.ThreadGetSubnet;
import thread.ThreadGetTitleWithForceStop;
import title.search.SearchTextField;

public class TitlePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel buttonPanel;
	private static LineTable titleTable;
	private JLabel lblSummaryOfTitle;
	public static JRadioButton rdbtnUnCheckedItems;
	public static JRadioButton rdbtnCheckingItems;
	public static JRadioButton rdbtnCheckedItems;
	public static JRadioButton rdbtnMoreActionItems;

	//add table and tablemodel to GUI
	private static LineTableModel titleTableModel = new LineTableModel();
	PrintWriter stdout;
	PrintWriter stderr;
	public static ThreadGetTitleWithForceStop threadGetTitle;
	public static GetTitleTempConfig tempConfig; //每次获取title过程中的配置。
	private IndexedLinkedHashMap<String,LineEntry> BackupLineEntries;

	private static SearchTextField textFieldSearch;

	public static JTextField getTextFieldSearch() {
		return textFieldSearch;
	}

	/*
	public static void setTextFieldSearch(JTextField textFieldSearch) {
		TitlePanel.textFieldSearch = textFieldSearch;
	}*/

	public static LineTable getTitleTable() {
		return titleTable;
	}

	public static LineTableModel getTitleTableModel() {
		return titleTableModel;
	}

	public IndexedLinkedHashMap<String,LineEntry> getBackupLineEntries() {
		return BackupLineEntries;
	}

	public TitlePanel() {//构造函数

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

		/////////////////////////////////////////
		//		JSplitPane TargetAndTitlePanel = new JSplitPane();//存放目标域名
		//		TargetAndTitlePanel.setResizeWeight(0.2);
		//		this.add(TargetAndTitlePanel,BorderLayout.CENTER);
		//
		//		JScrollPane TargetMapPane = new JScrollPane();
		//		TargetMapPane.setPreferredSize(new Dimension(200, 200));
		//		TargetAndTitlePanel.setLeftComponent(TargetMapPane);
		//
		//
		//		titleTable = new LineTable(titleTableModel);
		//		TargetAndTitlePanel.setRightComponent(titleTable.getTableAndDetailSplitPane());

		titleTable = new LineTable(titleTableModel);
		this.add(titleTable.getTableAndDetailSplitPane(),BorderLayout.CENTER);
	}

	public JPanel createButtonPanel() {
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton btnAction = new JButton("Action");
		btnAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new GetTitleMenu().show(btnAction, btnAction.getX(), btnAction.getY());
			}
		});
		buttonPanel.add(btnAction);

		/*
		//通过tableModelListener实现自动保存后，无需这个模块了
		JButton btnSaveState = new JButton("Save");
		btnSaveState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnSaveState.setEnabled(false);
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
		//buttonPanel.add(btnSaveState);


		InputMap inputMap1 = btnSaveState.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke Save = KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK); //Ctrl+S
		inputMap1.put(Save, "Save");

		btnSaveState.getActionMap().put("Save", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnSaveState.setEnabled(false);
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
		 */


		
		JButton buttonSearch = new JButton("Search");
		textFieldSearch = new SearchTextField("",buttonSearch);
		buttonPanel.add(textFieldSearch);

		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText();
				titleTable.search(keyword);
				//searchHistory.addRecord(keyword);
				digStatus();
			}
		});
		buttonPanel.add(buttonSearch);

		rdbtnUnCheckedItems = new JRadioButton(LineEntry.CheckStatus_UnChecked);
		rdbtnUnCheckedItems.setSelected(true);
		rdbtnUnCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnUnCheckedItems);

		rdbtnCheckingItems = new JRadioButton(LineEntry.CheckStatus_Checking);
		rdbtnCheckingItems.setSelected(true);
		rdbtnCheckingItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnCheckingItems);

		rdbtnCheckedItems = new JRadioButton(LineEntry.CheckStatus_Checked);
		rdbtnCheckedItems.setSelected(false);
		rdbtnCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnCheckedItems);

		rdbtnMoreActionItems = new JRadioButton(LineEntry.CheckStatus_MoreAction);
		rdbtnMoreActionItems.setSelected(false);
		rdbtnMoreActionItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonSearch.doClick();
			}
		});
		buttonPanel.add(rdbtnMoreActionItems);

		lblSummaryOfTitle = new JLabel("^_^");
		buttonPanel.add(lblSummaryOfTitle);
		buttonPanel.setToolTipText(titleTableModel.getStatusSummary());

		return buttonPanel;
	}

	/*
	 * 根据所有已知域名获取title
	 */
	public void getAllTitle(){

		DomainPanel.backupDB();

		Set<String> domains = new HashSet<>();//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。

		//将新发现的域名也移动到子域名集合中，以便跑一次全量。 ---DomainConsumer.QueueToResult()中的逻辑已经保证了SubDomainSet一直是最全的。
		domains.addAll(DomainPanel.getDomainResult().getSubDomainSet());
		domains.addAll(Commons.toIPSet(DomainPanel.getDomainResult().getSubnetSet()));//确定的IP网段，用户自己输入的
		//remove domains in black list that is not our target
		//domains.removeAll(DomainPanel.getDomainResult().fetchNotTargetIPList());//无需移除，会标记出来的。
		tempConfig = new GetTitleTempConfig(domains.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}
		//backup to history
		BackupLineEntries = titleTableModel.getLineEntries();
		//clear tableModel

		titleTableModel.clear(true);//clear
		if (threadGetTitle != null){
			threadGetTitle.interrupt();
		}

		threadGetTitle = new ThreadGetTitleWithForceStop(domains,tempConfig.getThreadNumber());
		threadGetTitle.start();
		DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().clear();
	}


	public void getExtendTitle(){
		DomainPanel.backupDB();

		Set<String> extendIPSet = titleTableModel.GetExtendIPSet();
		stdout.println(extendIPSet.size()+" extend IP Address founded"+extendIPSet);
		
		tempConfig = new GetTitleTempConfig(extendIPSet.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}
		
		if (threadGetTitle != null){
			threadGetTitle.interrupt();
		}
		threadGetTitle = new ThreadGetTitleWithForceStop(extendIPSet,tempConfig.getThreadNumber());
		threadGetTitle.start();

		//转移手动保存的结果
		for (LineEntry entry:BackupLineEntries.values()) {
			//			if (entry.getComment().contains("Manual-Saved")) {
			//				TitlePanel.getTitleTableModel().addNewLineEntry(entry);
			//			}
			if (entry != null) {
				TitlePanel.getTitleTableModel().addNewLineEntry(entry);//保存所有历史记录中没有匹配到的记录。
			}
		}
	}

	/*
	 * 获取新发现域名的title
	 */
	public void getTitleOfNewDomain(){
		DomainPanel.backupDB();

		Set<String> domains = new HashSet<>();//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。
		domains.addAll(DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet());
		//remove domains in black list
		//domains.removeAll(DomainPanel.getDomainResult().fetchNotTargetIPList());//无需移除，会标记出来的。
		tempConfig = new GetTitleTempConfig(domains.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}
		
		if (threadGetTitle != null){
			threadGetTitle.interrupt();
		}
		threadGetTitle = new ThreadGetTitleWithForceStop(domains,tempConfig.getThreadNumber());
		threadGetTitle.start();
		//清空新发现域名集合前，应该都添加到子域名集合中！！！---DomainConsumer.QueueToResult()中的逻辑已经保证了SubDomainSet一直是最全的。
		DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().clear();
	}


	public String getSubnet(boolean isCurrent,boolean justPulic){
		//stdout.println(" "+isCurrent+justPulic);
		Set<String> subnets;
		if (isCurrent) {//获取的是现有可成功连接的IP集合+用户指定的IP网段集合
			subnets = titleTableModel.GetSubnets();
		}else {//重新解析所有域名的IP
			ThreadGetSubnet thread = new ThreadGetSubnet(DomainPanel.getDomainResult().getSubDomainSet());
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "thread Interrupted";
			}
			Set<String> IPsOfDomain = thread.IPset;
			Set<String> IPsOfcertainSubnets = Commons.toIPSet(DomainPanel.getDomainResult().getSubnetSet());//用户配置的确定IP+网段
			IPsOfDomain.addAll(IPsOfcertainSubnets);
			subnets = Commons.toSmallerSubNets(IPsOfDomain);
		}

		HashSet<String> result = new HashSet<>(subnets);
		if (justPulic) {
			//stdout.println("删除私有IP");
			for (String subnet :subnets) {
				String tmp = subnet.split("/")[0];
				if (IPAddress.isPrivateIPv4(tmp)) {
					result.remove(subnet);
					//stdout.println("删除"+subnet);
				}
			}
		}
		List<String> tmplist= new ArrayList<>(result);
		Collections.sort(tmplist);
		return String.join(System.lineSeparator(), tmplist);
	}

	/*
	 * 用于从DB文件中加载数据，没有去重检查。
	 */
	public void showToTitleUI(IndexedLinkedHashMap<String,LineEntry> lineEntries) {
		//titleTableModel.setLineEntries(new ArrayList<LineEntry>());//clear
		//这里没有fire delete事件，会导致排序号加载文件出错，但是如果fire了又会触发tableModel的删除事件，导致数据库删除。改用clear()
		titleTableModel.clear(false);//clear
		titleTableModel.setListenerIsOn(false);
		int row = lineEntries.size();
		titleTableModel.setLineEntries(lineEntries);//如果listener是on，将触发listener--同步到db文件
		if (row>=1) {
			titleTableModel.fireTableRowsInserted(0, row-1);
		}
		titleTableModel.setListenerIsOn(true);
		System.out.println(row+" title entries loaded from database file");
		stdout.println(row+" title entries loaded from database file");
		digStatus();
		TitlePanel.getTitleTable().search("");// hide checked items
	}


	public void digStatus() {
		String status = titleTableModel.getStatusSummary();
		lblSummaryOfTitle.setText(status);
	}

}
