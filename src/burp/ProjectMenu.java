package burp;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import GUI.GUIMain;
import base.DictFileReader;
import base.dbFileChooser;
import dao.DomainDao;
import dao.TitleDao;
import domain.DomainManager;
import title.LineEntry;

/**
 * 构造项目菜单
 *
 */
public class ProjectMenu extends JMenu{
	GUIMain gui;
	public JMenuItem lockMenu;

	public ProjectMenu(GUIMain gui){
		this.gui = gui;
		this.setText("DomainHunter");

		JMenuItem newMenu = new JMenuItem(new AbstractAction("New")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {//实质就是save一个空的项目
				createNewDb(gui);
			}
		});
		newMenu.setToolTipText("Create A New Project File(DB File)");
		this.add(newMenu);

		JMenuItem openMenu = new JMenuItem(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				openDb();
			}
		});
		openMenu.setToolTipText("Open Domain Hunter Project File(DB File)");
		this.add(openMenu);


		JMenu openRecentMenu = new JMenu("Open Recent");

		/*
		openRecentMenu.addMouseMotionListener(new MouseMotionAdapter() {
		//这个事件会被频繁触发！弃用！
			@Override
			public void mouseMoved(MouseEvent e) {
				// 当鼠标移动到菜单上时创建新的菜单项并添加到菜单中
				gui.getDataLoadManager().createRecentOpenItem(openRecentMenu);
			}
		});
		 */

		openRecentMenu.setToolTipText("Open Recent Domain Hunter Project File(DB File)");
		this.add(openRecentMenu);


		openRecentMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				// 当菜单被选中时触发的逻辑
				BurpExtender.getDataLoadManager().createRecentOpenItem(openRecentMenu);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				// 当菜单被取消选中时触发的逻辑
			}

			@Override
			public void menuCanceled(MenuEvent e) {
				// 当菜单被取消时触发的逻辑
			}
		});

		JMenuItem renameMenu = new JMenuItem(new AbstractAction("Rename(Save As)")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				gui.getDomainPanel().renameDB();
			}
		});
		renameMenu.setToolTipText("Rename current DB File");
		this.add(renameMenu);

		/**
		 * 导入db文件，将数据和当前DB文件进行合并。
		 * domain Panel中的内容是集合的合并,无需考虑覆盖问题;
		 * title中的内容是新增和覆盖（如果存在的话），这里是导入的会覆盖当前的。
		 */
		JMenuItem ImportMenu = new JMenuItem(new AbstractAction("Import DB") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				gui.getDomainPanel().backupDB("before import");//导入前的备份。

				File file = new dbFileChooser().dialog(true,".db");
				if (null ==file) {
					return;
				}
				if (file.toString().equals(BurpExtender.getDataLoadManager().getCurrentDBFile().toString())) {
					return;
				}

				DomainDao dao = new DomainDao(file.getAbsolutePath());
				DomainManager NewManager = dao.getDomainManager();

				gui.getDomainPanel().getDomainResult().getRelatedDomainSet().addAll(NewManager.getRelatedDomainSet());
				gui.getDomainPanel().getDomainResult().getSubDomainSet().addAll(NewManager.getSubDomainSet());
				gui.getDomainPanel().getDomainResult().getSimilarDomainSet().addAll(NewManager.getSimilarDomainSet());
				gui.getDomainPanel().getDomainResult().getEmailSet().addAll(NewManager.getEmailSet());
				gui.getDomainPanel().getDomainResult().getPackageNameSet().addAll(NewManager.getPackageNameSet());

				gui.getDomainPanel().showDataToDomainGUI();
				gui.getDomainPanel().saveDomainDataToDB();

				TitleDao titledao = new TitleDao(file.getAbsolutePath());
				List<LineEntry> titles = titledao.selectAllTitle();
				for (LineEntry entry:titles) {
					gui.getTitlePanel().getTitleTable().getLineTableModel().addNewLineEntry(entry);
				}
				System.out.println("Import finished");
				BurpExtender.getStdout().println("Import finished");
			}
		});
		ImportMenu.setToolTipText("Import Project File(DB File)");
		this.add(ImportMenu);


		/**
		 * 导入文本文件，将数据和当前DB文件进行合并。
		 * domain Panel中的内容是集合的合并,无需考虑覆盖问题;
		 *
		 */
		JMenuItem ImportFromTextFileMenu = new JMenuItem(new AbstractAction("Import Domain List") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						gui.getDomainPanel().backupDB("before import");//导入前的备份。

						File file = new dbFileChooser().dialog(true,".txt");
						if (null ==file) {
							return null;
						}

						DictFileReader readline = new DictFileReader(file.getAbsolutePath());
						while(true){
							List<String> tmp = readline.next(10000,"");
							if (tmp.size() == 0) {
								BurpExtender.getStdout().println("Import from file: "+file.getAbsolutePath()+" done");
								break;
							}else {
								for (String item:tmp) {
									gui.getDomainPanel().getDomainResult().addIfValid(item);
								}
							}
						}

						gui.getDomainPanel().showDataToDomainGUI();
						gui.getDomainPanel().saveDomainDataToDB();
						return null;
					}
					@Override
					protected void done() {
					}
				};
				worker.execute();
			}
		});
		ImportMenu.setToolTipText("Import Domain From Text File");
		this.add(ImportFromTextFileMenu);

		//TODO
		JMenuItem detachMenu = new JMenuItem(new AbstractAction("Detach")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

			}
		});
		//this.add(detachMenu);

		lockMenu = new JMenuItem(new AbstractAction("Lock")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				gui.lockUnlock();
			}
		});
		this.add(lockMenu);

	}

	public void createNewDb(GUIMain gui) {
		File file = new dbFileChooser().dialog(false,".db");//通过保存对话指定文件，这会是一个空文件。
		if (null != file) {
			gui.getDomainPanel().setDomainResult(new DomainManager(gui));
			BurpExtender.getDataLoadManager().loadDbfileToHunter(file.toString());//然后加载，就是一个新的空项目了。
		}
	}

	public void openDb() {
		File file = new dbFileChooser().dialog(true,".db");
		if (null != file) {
			BurpExtender.getDataLoadManager().loadDbfileToHunter(file.toString());
		}
	}


	public void openRecentDb() {
		File file = new dbFileChooser().dialog(true,".db");
		if (null != file) {
			BurpExtender.getDataLoadManager().loadDbfileToHunter(file.toString());
		}
	}

	public void Add() {
		int count = 500;
		JMenuBar menuBar = getBurpFrame().getJMenuBar();
		while(count > 0) {
			try{
				if (menuBar == null){
					try {
						Thread.sleep(100);
						count--;
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}else {
					menuBar.add(this, menuBar.getMenuCount() - 1);
					menuBar.repaint();
					break;
				}
			}catch (Exception e){
				e.printStackTrace();
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	public void remove(){
		JMenuBar menuBar = getBurpFrame().getJMenuBar();
		if (menuBar != null){
			menuBar.remove(this);
			menuBar.repaint();
		}
	}

	/**
	 * 最上面的项目菜单中显示项目名称
	 * @param name
	 */
	public void AddDBNameMenuItem(String name){
		if (null==name) return;
		String firstName = this.getItem(0).getName();
		if (firstName != null && firstName.equals("JustDisplayDBFileName")){
			this.remove(0);
		}
		JMenuItem nameItem = new JMenuItem("Project:"+name);
		nameItem.setName("JustDisplayDBFileName");
		nameItem.setEnabled(false);
		this.insert(nameItem,0);
	}

	/**
	 * Domain Panel显示项目名称
	 * @param name
	 */
	public void AddDBNameTab(String name){
		if (null == name) return;
		JTabbedPane panel = ((JTabbedPane)gui.getContentPane());
		String newName = String.format("domain [%s]",name);
		panel.setTitleAt(0,newName);
	}

	//修改之后不刷新，弃用
	@Deprecated
	public void changeTabName(String dbFileName){
		Container ccc = getBurpFrame().getContentPane();
		JTabbedPane ParentOfDomainHunter = (JTabbedPane) ccc.getComponent(0);//burpTabBar
		int n = ParentOfDomainHunter.getComponentCount();

		//find index of current DomainHunter
		for (int i=n-1;i>=0;i--){
			Component tmp = ParentOfDomainHunter.getComponent(i);
			if (tmp.getName().equals("DomainHunterPro")){
				ParentOfDomainHunter.setTitleAt(i,"xxxxx");//似乎burp不会刷新这个title的显示。

				String tmpDbFile = BurpExtender.getDataLoadManager().getCurrentDBFile().getName();
				if (tmpDbFile.equals(dbFileName)){
					ParentOfDomainHunter.setTitleAt(i,tmpDbFile);
				}
			}//domain.DomainPanel
		}
	}

	public int indexOfDomainHunter(String dbFileName){
		Container ccc = getBurpFrame().getContentPane();
		JTabbedPane ParentOfDomainHunter = (JTabbedPane) ccc.getComponent(0);//burpTabBar
		int n = ParentOfDomainHunter.getComponentCount();

		//find index of current DomainHunter
		for (int i=n-1;i>=0;i--){//倒序查找更快
			Component tmp = ParentOfDomainHunter.getComponent(i);
			if (tmp.getName().equals("DomainHunterPro")){
				String tmpDbFile = BurpExtender.getDataLoadManager().getCurrentDBFile().getName();
				if (tmpDbFile.equals(dbFileName)){
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * DomainHunter*表示locked
	 * DomainHunter表示unlocked
	 * @return
	 */
	public static boolean isAlone() {
		int num = 0;
		JMenuBar menuBar = getBurpFrame().getJMenuBar();
		int count = menuBar.getMenuCount();
		for (int i =0;i<count;i++) {
			if (menuBar.getMenu(i).getText().endsWith("Hunter")) {
				num++;
			}
		}
		return num<=1;
	}

	public static JFrame getBurpFrame()
	{
		for(Frame f : Frame.getFrames())
		{
			if(f.isVisible() && f.getTitle().startsWith(("Burp Suite")))
			{
				return (JFrame) f;
			}
		}
		return null;
	}
}
