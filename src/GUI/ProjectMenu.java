package GUI;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import burp.BurpExtender;
import burp.DBHelper;
import domain.DomainManager;
import domain.DomainPanel;
import title.IndexedLinkedHashMap;
import title.LineEntry;
import title.TitlePanel;

public class ProjectMenu{
	GUI gui;
	JMenu hunterMenu;

	public ProjectMenu(GUI gui){
		this.gui = gui;
		hunterMenu = Menu();
	}

	public void Add() {
		try{
			JMenuBar menuBar = getBurpFrame().getJMenuBar();
			menuBar.add(hunterMenu, menuBar.getMenuCount() - 1);
		}catch (Exception e){

		}
	}

	public void remove(){
		JMenuBar menuBar = getBurpFrame().getJMenuBar();
		menuBar.remove(hunterMenu);
		menuBar.repaint();
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
		JMenu menuButton = new JMenu("Domainhunter");

		JMenuItem newMenu = new JMenuItem(new AbstractAction("New")
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent) {//实质就是save一个空的项目
				File file = gui.dbfc.dialog(false);//通过保存对话指定文件，这会是一个空文件。
				if (null != file) {
					DomainPanel.setDomainResult(new DomainManager(file.getName()));
					gui.saveData(file.toString(),true);
					gui.LoadData(file.toString());//然后加载，就是一个新的空项目了。
					GUI.setCurrentDBFile(file);
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
					GUI.setCurrentDBFile(file);
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
