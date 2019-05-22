package burp;

import java.awt.EventQueue;
import java.io.File;
import java.io.PrintWriter;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.google.common.io.Files;


public class GUI extends JFrame {

	protected static DomainPanel domainPanel;
	protected static TitlePanel titlePanel;
	protected static File currentDBFile;
	protected PrintWriter stdout;
	protected PrintWriter stderr;
	protected dbFileChooser dbfc = new dbFileChooser();

	public static DomainPanel getDomainPanel() {
		return domainPanel;
	}

	public static TitlePanel getTitlePanel() {
		return titlePanel;
	}


	/**
	 * Create the frame.
	 */
	public GUI() {//构造函数
        try{
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        }catch (Exception e){
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }

		JTabbedPane tabbedWrapper = new JTabbedPane();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		setContentPane(tabbedWrapper);
		domainPanel = new DomainPanel();
		titlePanel = new TitlePanel();
		tabbedWrapper.addTab("Domains", null, domainPanel, null);
		tabbedWrapper.addTab("Titles", null, titlePanel, null);

		new ProjectMenu(this);
	}


	public boolean LoadData(String dbFilePath){
		try {//这其中的异常会导致burp退出
			stdout.println("Loading Data From: "+dbFilePath);
			DBHelper dbhelper = new DBHelper(dbFilePath);
			domainPanel.setDomainResult(dbhelper.getDomainObj());
			domainPanel.showToDomainUI();
			titlePanel.showToTitleUI(dbhelper.getTitles());
			currentDBFile = new File(dbFilePath);
			stdout.println("Loading Finished!");
			return true;
		} catch (Exception e) {
			stdout.println("Loading Failed!");
			e.printStackTrace(stderr);
			return false;
		}
	}

	public void saveDBfilepathToExtension() {
		//to save domain result to extensionSetting
		//仅仅存储sqllite数据库的名称,也就是domainResult的项目名称
		if (currentDBFile != null)
			BurpExtender.getCallbacks().saveExtensionSetting("domainHunterpro", currentDBFile.getAbsolutePath());
	}

	public File saveDialog(boolean includeTitle) {
		PrintWriter stdout;
		PrintWriter stderr;

		stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
		stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		try{

			File file;
			if (null != currentDBFile && currentDBFile.getAbsolutePath().endsWith(".db")) {
				file = currentDBFile;
			}else {
				file = dbfc.dialog(false);
				if (file == null) return null;
				if(!(file.getName().toLowerCase().endsWith(".db"))){
					file=new File(dbfc.getCurrentDirectory(),file.getName()+".db");
				}
				if(file.exists()){
					int result = JOptionPane.showConfirmDialog(null,"Are you sure to overwrite this file ?");
					if (result != JOptionPane.YES_OPTION) {
						return null;
					}
				}
				currentDBFile = file;
				saveDBfilepathToExtension();
			}

			if (domainPanel.domainResult.projectName.equals("")) {
				domainPanel.domainResult.projectName = file.getName();
			}


			DBHelper dbHelper = new DBHelper(file.toString());
			dbHelper.addDomainObject(domainPanel.domainResult);
			if (includeTitle){
				dbHelper.saveTitles(TitlePanel.getTitleTableModel().getLineEntries());
			}

			stdout.println("Save Success! includeTitle:"+includeTitle+" "+ Commons.getNowTimeString());
			return file;
		}catch(Exception e1){
			stdout.println("Save failed! includeTitle:"+includeTitle+" "+Commons.getNowTimeString());
			e1.printStackTrace(stderr);
			return null;
		}
	}

	public File openDialog() {
		try {
			File file = dbfc.dialog(true);
			if (file == null) {
				return null;
			}
			if (file.getName().endsWith(".json")){//兼容旧文件
				String contents = Files.toString(file, Charsets.UTF_8);//读取json文件的方式
				domainPanel.domainResult = JSON.parseObject(contents,DomainObject.class);
				if (domainPanel.domainResult != null) domainPanel.showToDomainUI();
			}else {
				DBHelper dbhelper = new DBHelper(file.toString());
				domainPanel.domainResult = dbhelper.getDomainObj();
				if (domainPanel.domainResult != null) domainPanel.showToDomainUI();
				titlePanel.showToTitleUI(dbhelper.getTitles());
			}
			currentDBFile = file;//就是应该在对话框完成后就更新
			saveDBfilepathToExtension();
			stdout.println("open Project ["+domainPanel.domainResult.projectName+"] From File "+ file.getName());
			return file;
		} catch (Exception e1) {
			e1.printStackTrace(stderr);
			return null;
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
