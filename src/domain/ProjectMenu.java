package domain;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import GUI.GUIMain;
import base.Commons;
import base.DictFileReader;
import base.dbFileChooser;
import burp.BurpExtender;
import dao.DBUtils;
import dao.DaoFactory;
import dao.DbWriter;
import dao.ProjectDaos;
import domain.target.TargetEntry;
import title.LineEntry;

/**
 * 构造项目菜单
 *
 */
public class ProjectMenu extends JPopupMenu{
	GUIMain gui;
	private PrintWriter stdout;
	private PrintWriter stderr;

	public ProjectMenu(GUIMain gui){
		this.gui = gui;

		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		JMenuItem newMenu = new JMenuItem(new AbstractAction("New")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {//实质就是save一个空的项目
				createNewDb(gui);
			}
		});
		newMenu.setToolTipText("Create A New Project File(DB File)");


		JMenuItem openMenu = new JMenuItem(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				openDb();
			}
		});
		openMenu.setToolTipText("Open Domain Hunter Project File(DB File)");



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

		JMenuItem renameMenu = new JMenuItem(new AbstractAction("Rename")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				renameDB();
			}
		});
		renameMenu.setToolTipText("Rename current DB File");



		JMenuItem saveDomainOnly = new JMenuItem(new AbstractAction("Save Domain Only")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				saveDomainOnly();
			}
		});
		saveDomainOnly.setToolTipText("Only save data in Domain Panel");



		JMenuItem buckupDB = new JMenuItem(new AbstractAction("Backup DB")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				backupDB(null);
			}
		});


		JMenuItem removeDB = new JMenuItem(new AbstractAction("Remove DB")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				removeDB();
			}
		});


		/**
		 * 导入db文件，将数据和当前DB文件进行合并。
		 * domain Panel中的内容是集合的合并,无需考虑覆盖问题;
		 * title中的内容是新增和覆盖（如果存在的话），这里是导入的会覆盖当前的。
		 */
		JMenuItem ImportMenu = new JMenuItem(new AbstractAction("Import DB") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				File file = new dbFileChooser().dialog(true,".db");
				importFromDB(file,true);
			}
		});
		ImportMenu.setToolTipText("Import Project File(DB File)");


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
						backupDB("before import");//导入前的备份。

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


		JMenuItem lockMenu = new JMenuItem(new AbstractAction("Lock&Unlock")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				gui.lockUnlock();
			}
		});


		this.add(newMenu);
		this.add(openMenu);
		this.add(openRecentMenu);
		this.add(renameMenu);
		this.add(saveDomainOnly);
		this.add(buckupDB);
		this.add(removeDB);
		this.add(ImportMenu);
		//this.add(ImportFromTextFileMenu);
		this.addSeparator();
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

	/*
    单独保存域名信息到另外的文件
	 */
	public File saveDomainOnly() {
		try {
			File file = new dbFileChooser().dialog(false,".db");
			if (file != null) {
				// 目标是新文件：先确保表结构存在，再把内存里的完整快照一次性写入
				ProjectDaos daos = DaoFactory.of(file);
				daos.domainDao();
				daos.targetDao();

				DbWriter writer = DbWriter.getInstance();
				writer.saveAllDomains(daos.getDbFilePath(), gui.getDomainPanel().getDomainResult());
				writer.saveAllTargets(daos.getDbFilePath(),
						gui.getDomainPanel().fetchTargetModel().getTargetEntries().values());
				// 同步等待落盘：返回的 File 必须是"已经写好"的文件
				if (writer.flush(120_000)) {
					stdout.println("Save Domain Only Success! " + Commons.getNowTimeString());
					return file;
				}
				stdout.println("Save Domain Only failed (flush timeout): " + Commons.getNowTimeString());
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace(stderr);
		}
		stdout.println("Save Domain Only failed! " + Commons.getNowTimeString());
		return null;
	}

	public void backupDB(String keyword) {
		File file = BurpExtender.getDataLoadManager().getCurrentDBFile();
		if (file == null) return;
		String suffix = ".bak" + Commons.getNowTimeString();
		if (!StringUtils.isEmpty(keyword)) {
			keyword = keyword.replaceAll("\\s+", "-");
			suffix += keyword;
		}
		File bakfile = new File(file.getAbsoluteFile().toString() + suffix);
		try {
			// 备份前必须做两件事，否则备份出来的文件会缺最新数据：
			// 1) flush：把还在写队列里的任务落盘；
			// 2) wal_checkpoint：把 WAL 里的内容合并回主数据库文件（WAL 模式下主文件本身可能不完整）
			DbWriter.getInstance().flush(30_000);
			DBUtils.checkpoint(file.getPath());
			FileUtils.copyFile(file, bakfile);
			BurpExtender.getStdout().println("DB File Backed Up:" + bakfile.getAbsolutePath());
		} catch (IOException e1) {
			e1.printStackTrace(BurpExtender.getStderr());
		}
	}

	public void removeDB() {
		File file = BurpExtender.getDataLoadManager().getCurrentDBFile();
		if (file == null) return;
		try {
			int result = JOptionPane.showConfirmDialog(null,"Are you sure to DELETE this DB file ?");
			if (result == JOptionPane.YES_OPTION) {
				// 先落盘并 checkpoint，再清掉缓存，最后删文件：
				// 反过来做会让 "db-writer" 线程在建表时报 "file not found"，也会丢掉 WAL 里的数据
				DbWriter.getInstance().flush(30_000);
				DBUtils.checkpoint(file.getPath());
				DaoFactory.evict(file);
				FileUtils.delete(file);
				BurpExtender.getStdout().println("DB File Removed:" + file.getAbsolutePath());
			}
		} catch (IOException e1) {
			e1.printStackTrace(BurpExtender.getStderr());
		}
	}

	public void renameDB() {
		File file = BurpExtender.getDataLoadManager().getCurrentDBFile();
		if (file == null) return;

		String currentName = file.getName();
		String currentPath = file.getParent();

		//File newFile = new dbFileChooser().dialog(false,".db");//通过保存对话指定文件，这会是一个空文件。
		String newFilename = JOptionPane.showInputDialog("Enter New DB File Name", currentName);

		if (null != newFilename) {
			try {
				File newFile = new File(currentPath+File.separator+newFilename);
				// 同 removeDB：先落盘 + checkpoint + 清缓存，再移动文件
				DbWriter.getInstance().flush(30_000);
				DBUtils.checkpoint(file.getPath());
				DaoFactory.evict(file);
				FileUtils.moveFile(file, newFile);
				if (newFile.exists()) {
					BurpExtender.getDataLoadManager().loadDbfileToHunter(newFile.toString());
				}
			} catch (IOException e) {
				e.printStackTrace(stderr);
			}
		}
	}

	public void createOrOpenDB() {
		Object[] options = { "Create","Open"};
		int user_input = JOptionPane.showOptionDialog(null, "You should Create or Open a DB file", "Chose Your Action",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		if (user_input == 0) {
			createNewDb(gui);
		}
		if (user_input == 1) {
			openDb();
		}
	}
	
	/**
	 * 如果mergeRootDomain为true。先合并TargetEntry，它是控制资产范围的。
	 * 否则，不合并TargetEntry.
	 * @param file
	 */
	public void importFromDB(File file,boolean mergeRootDomain) {
		if (null ==file) {
			return;
		}
		if (file.toString().equals(BurpExtender.getDataLoadManager().getCurrentDBFile().toString())) {
			return;
		}
		if (!file.exists()) {
			return;
		}
		backupDB("before import");//导入前的备份。
		// 源项目文件同样通过 DaoFactory 打开：它与当前项目是两个不同的文件，
		// 各自有独立的 DataSource，互不影响
		ProjectDaos sourceDaos = DaoFactory.of(file);
		DomainManager NewManager = sourceDaos.domainDao().getDomainManager();

		if (mergeRootDomain) {
			List<TargetEntry> targets = sourceDaos.targetDao().selectAll();
			for (TargetEntry target:targets) {
				gui.getDomainPanel().getTargetTable().getTargetModel().addOrUpdateRowIfValid(target);
			}
		}

		gui.getDomainPanel().getDomainResult().getRelatedDomainSet().addAll(NewManager.getRelatedDomainSet());
		gui.getDomainPanel().getDomainResult().getSubDomainSet().addAll(NewManager.getSubDomainSet());
		gui.getDomainPanel().getDomainResult().getSimilarDomainSet().addAll(NewManager.getSimilarDomainSet());
		gui.getDomainPanel().getDomainResult().getEmailSet().addAll(NewManager.getEmailSet());
		gui.getDomainPanel().getDomainResult().getPackageNameSet().addAll(NewManager.getPackageNameSet());

		gui.getDomainPanel().showDataToDomainGUI();
		gui.getDomainPanel().saveDomainDataToDB();

		List<LineEntry> titles = sourceDaos.titleDao().selectAllTitle();
		for (LineEntry entry:titles) {
			gui.getTitlePanel().getTitleTable().getLineTableModel().addNewLineEntry(entry);
		}
		
		System.out.println("Import finished, " + gui.getDomainPanel().getDomainResult().getSummary());
		stdout.println("Import finished, " + gui.getDomainPanel().getDomainResult().getSummary());
	}

	/**
	 * Domain Panel显示项目名称
	 * @param name
	 */
	public void displayDBNameAtDomainTab(String name){
		if (null == name) return;
		JTabbedPane panel = ((JTabbedPane)gui.getContentPane());
		String newName = String.format("domain [%s]",name);
		panel.setTitleAt(0,newName);
	}
}
