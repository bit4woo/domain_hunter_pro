package title;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.IPAddressUtils;
import dao.TitleDao;
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
	private JLabel lblSummaryOfTitle;
	private JRadioButton rdbtnUnCheckedItems;
	private JRadioButton rdbtnCheckingItems;
	private JRadioButton rdbtnCheckedItems;
	private JRadioButton rdbtnMoreActionItems;
	private SearchTextField textFieldSearch;

	//add table and tablemodel to GUI
	private LineTable titleTable;
	private LineTableModel titleTableModel;
	private TitleDao titleDao;
	PrintWriter stdout;
	PrintWriter stderr;
	private ThreadGetTitleWithForceStop threadGetTitle;
	private GetTitleTempConfig tempConfig; //每次获取title过程中的配置。
	private IndexedHashMap<String,LineEntry> BackupLineEntries;
	private GUIMain guiMain;

	

	public JTextField getTextFieldSearch() {
		return textFieldSearch;
	}

	/*
	public static void setTextFieldSearch(JTextField textFieldSearch) {
		TitlePanel.textFieldSearch = textFieldSearch;
	}*/

	public LineTable getTitleTable() {
		return titleTable;
	}

	public LineTableModel getTitleTableModel() {
		return titleTableModel;
	}

	public IndexedHashMap<String,LineEntry> getBackupLineEntries() {
		return BackupLineEntries;
	}

	public GetTitleTempConfig getTempConfig() {
		return tempConfig;
	}

	public void setTempConfig(GetTitleTempConfig tempConfig) {
		this.tempConfig = tempConfig;
	}

	public JLabel getLblSummaryOfTitle() {
		return lblSummaryOfTitle;
	}

	public void setLblSummaryOfTitle(JLabel lblSummaryOfTitle) {
		this.lblSummaryOfTitle = lblSummaryOfTitle;
	}

	public JRadioButton getRdbtnUnCheckedItems() {
		return rdbtnUnCheckedItems;
	}

	public void setRdbtnUnCheckedItems(JRadioButton rdbtnUnCheckedItems) {
		this.rdbtnUnCheckedItems = rdbtnUnCheckedItems;
	}

	public JRadioButton getRdbtnCheckingItems() {
		return rdbtnCheckingItems;
	}

	public void setRdbtnCheckingItems(JRadioButton rdbtnCheckingItems) {
		this.rdbtnCheckingItems = rdbtnCheckingItems;
	}

	public JRadioButton getRdbtnCheckedItems() {
		return rdbtnCheckedItems;
	}

	public void setRdbtnCheckedItems(JRadioButton rdbtnCheckedItems) {
		this.rdbtnCheckedItems = rdbtnCheckedItems;
	}

	public JRadioButton getRdbtnMoreActionItems() {
		return rdbtnMoreActionItems;
	}

	public void setRdbtnMoreActionItems(JRadioButton rdbtnMoreActionItems) {
		this.rdbtnMoreActionItems = rdbtnMoreActionItems;
	}

	public ThreadGetTitleWithForceStop getThreadGetTitle() {
		return threadGetTitle;
	}

	public void setThreadGetTitle(ThreadGetTitleWithForceStop threadGetTitle) {
		this.threadGetTitle = threadGetTitle;
	}

	public TitlePanel(GUIMain guiMain) {//构造函数
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

		titleTable = new LineTable(guiMain);
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
		buttonPanel.setToolTipText("");

		return buttonPanel;
	}

	/**
	 * 转移手动保存的记录
	 */
	public void transferManualSavedItems(){
		if (BackupLineEntries == null){
			System.out.println("BackupLineEntries is Null");
			stderr.println("BackupLineEntries is Null");
			return;
		}
		for (LineEntry entry:BackupLineEntries.values()) {
			if (entry.getEntrySource().equalsIgnoreCase(LineEntry.Source_Manual_Saved)) {
				titleTableModel.addNewLineEntry(entry);
			}
		}
	}

	/**
	 *
	 * 根据所有已知域名获取title
	 */
	public void getAllTitle(){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			return;
		}
		guiMain.getDomainPanel().backupDB();

		Set<String> domains = new HashSet<>();//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。

		//将新发现的域名也移动到子域名集合中，以便跑一次全量。 ---DomainConsumer.QueueToResult()中的逻辑已经保证了SubDomainSet一直是最全的。
		domains.addAll(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());
		domains.addAll(guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet());//确定的IP网段，用户自己输入的
		domains.addAll(guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets());//特殊端口目标
		//remove domains in black list that is not our target
		//domains.removeAll(DomainPanel.getDomainResult().fetchNotTargetIPList());//无需移除，会标记出来的。
		tempConfig = new GetTitleTempConfig(domains.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}
		//backup to history
		BackupLineEntries = titleTableModel.getLineEntries();

		//clear tableModel
		titleTableModel = new LineTableModel(guiMain);//clear

		//转移以前手动保存的记录
		transferManualSavedItems();

		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,domains,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();
	}


	public void getExtendTitle(){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			return;
		}
		guiMain.getDomainPanel().backupDB();

		Set<String> extendIPSet = titleTableModel.GetExtendIPSet();
		stdout.println(extendIPSet.size()+" extend IP Address founded"+extendIPSet);
		
		tempConfig = new GetTitleTempConfig(extendIPSet.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}


		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,extendIPSet,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();

	}

	/**
	 * 获取新发现域名的title，这里会尝试之前请求失败的域名，可能需要更多时间
	 */
	public void getTitleOfNewDomain(){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			return;
		}

		guiMain.getDomainPanel().backupDB();

		Set<String> newDomains = new HashSet<>(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());//新建一个对象，直接赋值后的删除操作，实质是对domainResult的操作。
		Set<String> targetIPSet = new HashSet<>(guiMain.getDomainPanel().getTargetTable().getTargetModel().fetchTargetIPSet());
		Set<String> newDomainsWithPort = new HashSet<>(guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets());

		newDomains.addAll(targetIPSet);
		newDomains.addAll(newDomainsWithPort);

		Set<String> hostsInTitle = titleTableModel.GetHostsWithSpecialPort();
		newDomains.removeAll(hostsInTitle);

		//remove domains in black list
		newDomains.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		tempConfig = new GetTitleTempConfig(newDomains.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}


		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,newDomains,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();
	}


	public String getSubnet(boolean isCurrent,boolean justPulic){
		//stdout.println(" "+isCurrent+justPulic);
		Set<String> subnets;
		if (isCurrent) {//获取的是现有可成功连接的IP集合+用户指定的IP网段集合
			subnets = titleTableModel.GetSubnets();
		}else {//重新解析所有域名的IP
			ThreadGetSubnet thread = new ThreadGetSubnet(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "thread Interrupted";
			}
			Set<String> IPsOfDomain = thread.IPset;
			Set<String> IPsOfcertainSubnets = guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
			IPsOfDomain.addAll(IPsOfcertainSubnets);
			subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);
		}

		HashSet<String> result = new HashSet<>(subnets);
		if (justPulic) {
			//stdout.println("删除私有IP");
			for (String subnet :subnets) {
				String tmp = subnet.split("/")[0];
				if (IPAddressUtils.isPrivateIPv4(tmp)) {
					result.remove(subnet);
					//stdout.println("删除"+subnet);
				}
			}
		}
		List<String> tmplist= new ArrayList<>(result);
		Collections.sort(tmplist);
		return String.join(System.lineSeparator(), tmplist);
	}

	/**
	 * 用于从DB文件中加载数据，没有去重检查。
	 * 这种加载方式没有改变tableModel，所以tableModelListener也还在。
	 */
	public void loadData(String currentDBFile) {
		titleDao = new TitleDao(currentDBFile);
		List<LineEntry> lines = titleDao.selectAllTitle();
		titleTableModel = new LineTableModel(guiMain,lines);
		titleTable.setModel(titleTableModel);
		TableRowSorter<LineTableModel> tableRowSorter = new TableRowSorter<LineTableModel>(titleTableModel);
		titleTable.setRowSorter(tableRowSorter);
		int row = lines.size();
		System.out.println(row+" title entries loaded from database file");
		stdout.println(row+" title entries loaded from database file");
		digStatus();
		titleTable.search("");// hide checked items
	}


	public void digStatus() {
		String status = titleTableModel.getStatusSummary();
		lblSummaryOfTitle.setText(status);
	}

	/**
	 * 返回是否发送了stop信号
	 * @param askBeforeStop
	 * @return
	 */
	public boolean stopGetTitleThread(boolean askBeforeStop){
		if (getThreadGetTitle() != null && getThreadGetTitle().isAlive()){
			if (askBeforeStop){
				int confirm = JOptionPane.showConfirmDialog(null,"Other Get Title Thread Is Running," +
						"Are you sure to stop it and run this task?");
				if (confirm != JOptionPane.YES_OPTION){
					return false;
				}
			}
			getThreadGetTitle().interrupt();
			return true;
		}
		return true;
	}
}
