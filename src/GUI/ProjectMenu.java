package GUI;

import burp.BurpExtender;
import burp.DBHelper;
import domain.DomainManager;
import domain.DomainPanel;
import title.IndexedLinkedHashMap;
import title.LineEntry;
import title.TitlePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ProjectMenu{
	GUI gui;
	static JMenu hunterMenu;

	public ProjectMenu(GUI gui){
		this.gui = gui;
		hunterMenu = Menu();
	}

	public void Add() {
		try{
			JMenuBar menuBar = getBurpFrame().getJMenuBar();
			menuBar.add(hunterMenu, menuBar.getMenuCount() - 1);
		}catch (Exception e){
			e.printStackTrace();
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	public void remove(){
		JMenuBar menuBar = getBurpFrame().getJMenuBar();
		menuBar.remove(hunterMenu);
		menuBar.repaint();
	}

	/**
	 * 最上面的项目菜单中显示项目名称
	 * @param name
	 */
	public void AddDBNameMenuItem(String name){
		if (null==name) return;
		String firstName = hunterMenu.getItem(0).getName();
		if (firstName != null && firstName.equals("JustDisplayDBFileName")){
			hunterMenu.remove(0);
		}
		JMenuItem nameItem = new JMenuItem("Project:"+name);
		nameItem.setName("JustDisplayDBFileName");
		nameItem.setEnabled(false);
		nameItem.addActionListener(new AbstractAction(){
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				String filename = GUI.currentDBFile.getName();
				int index = indexOfDomainHunter(filename);
				Container ccc = getBurpFrame().getContentPane();
				JTabbedPane ParentOfDomainHunter = (JTabbedPane) ccc.getComponent(0);//burpTabBar
				ParentOfDomainHunter.setSelectedIndex(index);//设置为选中,还是无效，操作失败
			}
		});
		hunterMenu.insert(nameItem,0);
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

				String tmpDbFile = ((GUI) tmp).currentDBFile.getName();
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
				String tmpDbFile = ((GUI) tmp).currentDBFile.getName();
				if (tmpDbFile.equals(dbFileName)){
					return i;
				}
			}
		}
		return -1;
	}

	public static boolean isAlone() {
		int num = 0;
		JMenuBar menuBar = getBurpFrame().getJMenuBar();
		int count = menuBar.getMenuCount();
		for (int i =0;i<count;i++) {
			if (menuBar.getMenu(i).getText().contains("Hunter")) {
				num++;
			}
		}
		return num<=1;
	}
	
	//判断是否加载了新的项目，如果是就需要刷新显示的项目名称。
	@Deprecated
	public static boolean needFreshDisplay() {
		String firstName = hunterMenu.getItem(0).getName();
		if (firstName != null && firstName.equals("JustDisplayDBFileName")){
			return true;
		}
		return false;
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

	public JMenu Menu() {
		JMenu menuButton = new JMenu("DomainHunter");

		JMenuItem newMenu = new JMenuItem(new AbstractAction("New")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {//实质就是save一个空的项目
				File file = gui.dbfc.dialog(false);//通过保存对话指定文件，这会是一个空文件。
				if (null != file) {
					DomainPanel.setDomainResult(new DomainManager(file.getName()));
					gui.saveData(file.toString(),true);
					gui.LoadData(file.toString());//然后加载，就是一个新的空项目了。
				}
			}
		});
		newMenu.setToolTipText("Create A New Project File(DB File)");
		menuButton.add(newMenu);

		JMenuItem openMenu = new JMenuItem(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				File file = gui.dbfc.dialog(true);
				if (null != file) {
					gui.LoadData(file.toString());
				}
			}
		});
		openMenu.setToolTipText("Open Domain Hunter Project File(DB File)");
		menuButton.add(openMenu);

		//导入db文件，将数据和当前DB文件进行合并。domain Panel中的内容是集合的合并,无需考虑覆盖问题;
		//title中的内容是新增和覆盖（如果存在的话），这里是导入的会覆盖当前的。
		JMenuItem ImportMenu = new JMenuItem(new AbstractAction("Import") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DomainPanel.backupDB();//导入前的备份。

				File file = gui.dbfc.dialog(true);
				if (null ==file) {
					return;
				}

				DBHelper dbhelper = new DBHelper(file.getAbsolutePath());
				DomainManager NewManager = dbhelper.getDomainObj();

				DomainPanel.getDomainResult().getRootDomainMap().putAll(NewManager.getRootDomainMap());//合并rootDomain
				DomainPanel.getDomainResult().getRelatedDomainSet().addAll(NewManager.getRelatedDomainSet());
				DomainPanel.getDomainResult().getSubDomainSet().addAll(NewManager.getSubDomainSet());
				DomainPanel.getDomainResult().getSimilarDomainSet().addAll(NewManager.getSimilarDomainSet());
				DomainPanel.getDomainResult().getEmailSet().addAll(NewManager.getEmailSet());
				DomainPanel.getDomainResult().getPackageNameSet().addAll(NewManager.getPackageNameSet());
				GUI.getDomainPanel().showToDomainUI();

				IndexedLinkedHashMap<String, LineEntry> titles = dbhelper.getTitles();
				for (LineEntry entry:titles.values()) {
					TitlePanel.getTitleTableModel().addNewLineEntry(entry);
				}
				System.out.println("Import finished");
				BurpExtender.getStdout().println("Import finished");
			}
		});
		ImportMenu.setToolTipText("Import Project File(DB File)");
		menuButton.add(ImportMenu);

		//TODO
		JMenuItem detachMenu = new JMenuItem(new AbstractAction("Detach")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {

			}
		});
		menuButton.add(detachMenu);

		//为了菜单能够区分
		File dbFile = GUI.getCurrentDBFile();
		if (dbFile != null){
			AddDBNameMenuItem(dbFile.getName());
		}
		/*
        JMenuItem saveMenu = new JMenuItem(new AbstractAction("Save as") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    @Override
                    protected Map doInBackground() throws Exception {
                        File file = gui.dbfc.dialog(false);
                        gui.saveData(file.toString(),false);
                        return new HashMap<String, String>();
                        //no use ,the return.
                    }
                    @Override
                    protected void done() {
                    }
                };
                worker.execute();
            }
        });
        saveMenu.setToolTipText("Save All Domains And Titles To Another File");
        menuButton.add(saveMenu);
        InputMap inputMap = saveMenu.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke sav = KeyStroke.getKeyStroke(KeyEvent.VK_S, 2); //2 --ctrl;  Ctrl+S
        inputMap.put(sav, "Save");

        saveMenu.getActionMap().put("Save", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                //saveDialog(false);
            }
        });
		 */

		return menuButton;
	}
}
