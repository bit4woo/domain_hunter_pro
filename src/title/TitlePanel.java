package title;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.IPAddressUtils;
import burp.SystemUtils;
import dao.TitleDao;
import thread.ThreadGetSubnet;
import thread.ThreadGetTitleWithForceStop;
import title.search.SearchTextField;

public class TitlePanel extends TitlePanelBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JRadioButton rdbtnUnCheckedItems;
	private JRadioButton rdbtnCheckingItems;
	private JRadioButton rdbtnCheckedItems;
	private JRadioButton rdbtnMoreActionItems;

	//add table and tablemodel to GUI
	private TitleDao titleDao;
	PrintWriter stdout;
	PrintWriter stderr;
	private ThreadGetTitleWithForceStop threadGetTitle;
	private GetTitleTempConfig tempConfig; //每次获取title过程中的配置。
	private IndexedHashMap<String,LineEntry> BackupLineEntries;
	private GUIMain guiMain;


	public IndexedHashMap<String,LineEntry> getBackupLineEntries() {
		return BackupLineEntries;
	}

	public GetTitleTempConfig getTempConfig() {
		return tempConfig;
	}

	public void setTempConfig(GetTitleTempConfig tempConfig) {
		this.tempConfig = tempConfig;
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

		buttonPanel = createButtonPanel();
		titleTable = new LineTable(this);//这里的写法虽然和父类一样，但是传递的对象却不同！
		tableAndDetail = new TableAndDetailPanel(titleTable);
		this.add(buttonPanel, BorderLayout.NORTH);
		this.add(tableAndDetail,BorderLayout.CENTER);
	}

	@Override
	public JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton btnAction = new JButton("Action");
		btnAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new GetTitleMenu(guiMain).show(btnAction, btnAction.getX(), btnAction.getY());
			}
		});
		buttonPanel.add(btnAction);

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
				titleTable.getLineTableModel().addNewLineEntry(entry);
			}
		}
	}

	public static HashMap<String,String> AddWithSoureType(Set<String> input,String type) {
		HashMap<String,String> domains = new HashMap<String,String>();
		if (input != null && domains != null) {
			for (String in:input) {
				domains.put(in, type);
			}
		}
		return domains;
	}

	/**
	 *
	 * 多线程获取title的方法
	 */
	public void getTitleBase(HashMap<String,String> domainsWithSource){
		if (!stopGetTitleThread(true)){//其他get title线程未停止
			stdout.println("still have get title thread is running, will do nothing.");
			return;
		}

		stdout.println(domainsWithSource.size()+" targets to request");
		if (domainsWithSource.size() <= 0) {
			return;
		}
		tempConfig = new GetTitleTempConfig(domainsWithSource.size());
		if (tempConfig.getThreadNumber() <=0) {
			return;
		}

		setThreadGetTitle(new ThreadGetTitleWithForceStop(guiMain,domainsWithSource,tempConfig.getThreadNumber()));
		getThreadGetTitle().start();
	}

	/**
	 * 获取所有明确属于目标范围的域名、IP；排除了黑名单中的内容
	 * 子域名+确定的网段+证书IP-黑名单IP
	 * @return
	 */
	public Set<String> getCertainDomains() {
		Set<String> targetsToReq = new HashSet<String>();
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getSubDomainSet());
		targetsToReq.addAll(guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet());
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getIPSetOfCert());
		targetsToReq.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		return targetsToReq;
	}

	/**
	 * 获取所有用户自定义输入的域名、IP；排除了黑名单中的内容
	 * @return
	 */
	public Set<String> getCustomDomains() {
		Set<String> targetsToReq = new HashSet<String>();
		targetsToReq.addAll(guiMain.getDomainPanel().getDomainResult().getSpecialPortTargets());
		targetsToReq.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		return targetsToReq;
	}

	/**
	 *
	 * 根据所有已知域名获取title
	 */
	public void getAllTitle(){
		guiMain.getDomainPanel().backupDB("before-getTitle");
		//backup to history
		BackupLineEntries = titleTable.getLineTableModel().getLineEntries();
		//clear tableModel
		LineTableModel titleTableModel = new LineTableModel(guiMain.currentDBFile.toString());//clear
		loadData(titleTableModel);
		//转移以前手动保存的记录
		transferManualSavedItems();

		HashMap<String,String> mapToRun = AddWithSoureType(getCertainDomains(),LineEntry.Source_Certain);
		HashMap<String,String> mapToRun1 = AddWithSoureType(getCustomDomains(),LineEntry.Source_Custom_Input);
		mapToRun.putAll(mapToRun1);

		getTitleBase(mapToRun);
	}

	/**
	 * 需要跑的IP集合 = 网段汇算结果-黑名单-已请求域名的IP集合
	 */
	public void getExtendTitle(){
		guiMain.getDomainPanel().backupDB("before-getExtendTitle");

		Set<String> extendIPSet = GetExtendIPSet();
		Set<String> hostsInTitle = titleTable.getLineTableModel().GetHostsWithSpecialPort();
		extendIPSet.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		extendIPSet.removeAll(hostsInTitle);
		HashMap<String,String> mapToRun = AddWithSoureType(extendIPSet,LineEntry.Source_Subnet_Extend);

		getTitleBase(mapToRun);
	}


	/**
	 * 获取根据确定目标汇算出来的网段，减去已确定目标本身后，剩余的IP地址。
	 * @return 扩展IP集合
	 */
	public Set<String> GetExtendIPSet() {

		Set<String> IPsOfDomain = titleTable.getLineTableModel().getCertIPSetFromTitle();//title记录中的IP
		Set<String> IPsOfcertainSubnets = guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		IPsOfDomain.removeAll(guiMain.getDomainPanel().getDomainResult().getNotTargetIPSet());
		//计算网段前，将CDN和云服务的IP排除在外，这就是这个集合的主要作用！

		Set<String> subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);//当前所有title结果+确定IP/网段计算出的IP网段

		Set<String> CSubNetIPs = IPAddressUtils.toIPSet(subnets);// 当前所有title结果计算出的IP集合

		CSubNetIPs.removeAll(IPsOfDomain);//删除域名对应的IP，之前已经请求过了
		CSubNetIPs.removeAll(IPsOfcertainSubnets);//删除网段对应的IP，之前已经请求过了

		return CSubNetIPs;
	}

	/**
	 * 获取新发现域名、IP的title
	 * setToRun = 子域名+确定的网段+证书IP-黑名单IP-已请求域名的IP集合
	 */
	public void getTitleOfNewDomain(){
		guiMain.getDomainPanel().backupDB("before-getTitleOfNewDomain");

		Set<String> hostsInTitle = titleTable.getLineTableModel().GetHostsWithSpecialPort();

		Set<String> newDomains = getCertainDomains();
		newDomains.removeAll(hostsInTitle);
		HashMap<String,String> mapToRun = AddWithSoureType(newDomains,LineEntry.Source_Certain);

		newDomains = getCustomDomains();
		newDomains.removeAll(hostsInTitle);
		HashMap<String,String> mapToRun1 = AddWithSoureType(newDomains,LineEntry.Source_Custom_Input);

		mapToRun.putAll(mapToRun1);

		getTitleBase(mapToRun);
	}

	/**
	 * 1、title记录中成功解析的IP地址集合
	 * 2、用户指定的确信度很高的IP和网段的集合。
	 * 将2者合并算成网段。
	 * @return 根据确切目标算出的网段
	 */
	public Set<String> calcSubnets() {
		Set<String> IPsOfDomain = titleTable.getLineTableModel().getCertIPSetFromTitle();//title记录中的IP
		Set<String> IPsOfcertainSubnets = guiMain.getDomainPanel().fetchTargetModel().fetchTargetIPSet();//用户配置的确定IP+网段
		IPsOfDomain.addAll(IPsOfcertainSubnets);
		//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
		Set<String> subnets = IPAddressUtils.toSmallerSubNets(IPsOfDomain);
		return subnets;
	}


	public String getSubnet(boolean isCurrent,boolean justPulic){
		//stdout.println(" "+isCurrent+justPulic);
		Set<String> subnets;
		if (isCurrent) {//获取的是现有可成功连接的IP集合+用户指定的IP网段集合
			subnets = calcSubnets();
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
		LineTableModel titleTableModel = new LineTableModel(guiMain.currentDBFile.toString(), lines);
		loadData(titleTableModel);
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

	/**
	 * 右键点击事件--显示菜单
	 */
	@Override
	public void showRightClickMenu(MouseEvent e) {
		if ( SwingUtilities.isRightMouseButton( e )){//在windows中触发,因为isPopupTrigger在windows中是在鼠标释放是触发的，而在mac中，是鼠标点击时触发的。
			//https://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
			if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
				//getSelectionModel().setSelectionInterval(rows[0], rows[1]);
				int[] rows = titleTable.getSelectedRows();
				int[] modelRows = titleTable.SelectedRowsToModelRows(rows);

				int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
				int modelCol = titleTable.convertColumnIndexToModel(col);


				if (modelRows.length>0){
					JPopupMenu menu = new LineEntryMenu(guiMain, modelRows, modelCol);
					menu.show(e.getComponent(), e.getX(), e.getY());
				}else{//在table的空白处显示右键菜单
					//https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
					//new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}

	}



	/**
	 * 左键双击事件功能实现
	 * 
	 * @return
	 */
	@Override
	public void leftDoubleClick(MouseEvent e) {

		//双击进行google搜索、双击浏览器打开url、双击切换Check状态
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){//左键双击

			int[] modelRows = titleTable.SelectedRowsToModelRows(titleTable.getSelectedRows());
			int modelRow = modelRows[0];

			//int row = ((LineTable) e.getSource()).rowAtPoint(e.getPoint()); // 获得行位置
			int col = ((LineTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
			int modelCol = titleTable.convertColumnIndexToModel(col);

			LineTableModel lineTableModel = titleTable.getLineTableModel();

			LineEntry selecteEntry = lineTableModel.getLineEntries().get(modelRow);
			if ((modelCol == LineTableModel.getTitleList().indexOf("#") )) {//双击index在google中搜索host。
				String host = selecteEntry.getHost();
				String url= "https://www.google.com/search?q=site%3A"+host;
				try {
					URI uri = new URI(url);
					Desktop desktop = Desktop.getDesktop();
					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}else if(modelCol==LineTableModel.getTitleList().indexOf("URL")) {//双击url在浏览器中打开
				try{
					String url = selecteEntry.getUrl();
					if (url != null && !url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
						url = "http://"+url;//针对DNS记录中URL字段是host的情况
					}
					Commons.browserOpen(url,guiMain.getConfigPanel().getLineConfig().getBrowserPath());
				}catch (Exception e1){
					e1.printStackTrace(stderr);
				}
			}else if (modelCol == LineTableModel.getTitleList().indexOf("isChecked")) {
				try{
					//LineTable.this.lineTableModel.updateRowsStatus(rows,LineEntry.CheckStatus_Checked);//处理多行
					String currentStatus= selecteEntry.getCheckStatus();
					List<String> tmpList = Arrays.asList(LineEntry.CheckStatusArray);
					int index = tmpList.indexOf(currentStatus);
					String newStatus = tmpList.get((index+1)%LineEntry.CheckStatusArray.length);
					selecteEntry.setCheckStatus(newStatus);
					if (newStatus.equalsIgnoreCase(LineEntry.CheckStatus_Checked)) {
						selecteEntry.setTime(Commons.getNowTimeString());
					}
					stdout.println("$$$ "+selecteEntry.getUrl()+" status has been set to "+newStatus);
					lineTableModel.fireTableRowsUpdated(modelRow, modelRow);
				}catch (Exception e1){
					e1.printStackTrace(stderr);
				}
			}else if (modelCol == LineTableModel.getTitleList().indexOf("AssetType")) {
				String currentLevel = selecteEntry.getAssetType();
				List<String> tmpList = Arrays.asList(LineEntry.AssetTypeArray);
				int index = tmpList.indexOf(currentLevel);
				String newLevel = tmpList.get((index+1)%3);
				selecteEntry.setAssetType(newLevel);
				stdout.println(String.format("$$$ %s updated [AssetType-->%s]",selecteEntry.getUrl(),newLevel));
				lineTableModel.fireTableRowsUpdated(modelRow, modelRow);
			}else if (modelCol == LineTableModel.getTitleList().indexOf("ASNInfo")) {
				if (selecteEntry.getASNInfo().equals("")){
					selecteEntry.freshASNInfo();
				}else {
					SystemUtils.writeToClipboard(selecteEntry.getASNInfo());
				}
			} else{//LineTableModel.getTitleList().indexOf("CDN|CertInfo")
				//String value = getValueAt(rows[0], col).toString();//rows[0]是转换过的，不能再转换
				//调用的是原始Jtable中的getValueAt，它本质上也是调用model中的getValueAt，但是有一次转换的过程！！！
				String value = lineTableModel.getValueAt(modelRow,modelCol).toString();
				//调用的是我们自己实现的TableModel类中的getValueAt,相比Jtable类中的同名方法，就少了一次转换的过程！！！
				//String CDNAndCertInfo = selecteEntry.getCDN();
				SystemUtils.writeToClipboard(value);
			}
		}
	}

}