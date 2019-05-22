package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;

import test.HTTPPost;

public class GUI extends JFrame {

	public String ExtenderName = "Domain Hunter Pro v1.4 by bit4";
	public String github = "https://github.com/bit4woo/bug_hunter";

	protected static DomainObject domainResult = null;//getter setter
	protected static LineTableModel titleTableModel; //getter setter
	protected static DefaultTableModel domainTableModel;
	protected static File currentDBFile = null;

	public PrintWriter stdout;
	public PrintWriter stderr;

	private JRadioButton rdbtnAddRelatedToRoot;
	private JTabbedPane tabbedWrapper;
	private JPanel contentPane;
	private JTextField textFieldUploadURL;
	private JButton btnSearch;
	private JButton btnUpload;
	private JButton btnCrawl;
	private JLabel lblSummary;
	private JPanel FooterPanel;
	private JLabel lblNewLabel_2;
	private JTextArea textAreaSubdomains;
	private JTextArea textAreaSimilarDomains;
	private SortOrder sortedMethod;
	private JTable table;
	private JButton RemoveButton;
	private JButton AddButton;
	private JTextArea textAreaRelatedDomains;
	private JButton btnSave;
	private JButton btnOpen;
	private Component verticalStrut;
	private Component verticalStrut_1;
	private JButton btnCopy;
	private JButton btnNew;
	private JPanel buttonPanel;
	public JButton btnGettitle;
	public JScrollPane scrollPaneRequests;
	public LineTable titleTable;
	private JButton btnImportDomain;
	private JButton btnSaveState;
	private JButton btnGetExtendtitle;
	private JFileChooser fc = new JFileChooser();
	public static JLabel lblSummaryOfTitle;
	public static JTextField textFieldSearch;
	protected JPanel TitlePanel;
	public static JRadioButton rdbtnHideCheckedItems;



	public static DomainObject getDomainResult() {
		return domainResult;
	}

	public void setDomainResult(DomainObject domainResult) {
		GUI.domainResult = domainResult;
	}


	public static LineTableModel getTitleTableModel() {
		return titleTableModel;
	}

	public static void setTitleTableModel(LineTableModel titleTableModel) {
		GUI.titleTableModel = titleTableModel;
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

	/**
	 * Create the frame.
	 */
	public GUI() {
		tabbedWrapper = new JTabbedPane();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		setContentPane(tabbedWrapper);

		DomainPanel();
		tabbedWrapper.addTab("Domains", null, contentPane, null);
		tabbedWrapper.addTab("Titles", null, TitlePanel(), null);

	}








	public boolean LoadData(String dbFilePath){
		try {//这其中的异常会导致burp退出
			stdout.println("Loading Data From: "+dbFilePath);
			DBHelper dbhelper = new DBHelper(dbFilePath);
			domainResult = dbhelper.getDomainObj();
			showToDomainUI(domainResult);
			showToTitleUI(dbhelper.getTitles());
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
			BurpExtender.callbacks.saveExtensionSetting("domainHunterpro", currentDBFile.getAbsolutePath());
	}


	public Boolean upload(String url,String content) {
		if ((url.toLowerCase().contains("http://") ||url.toLowerCase().contains("https://"))
				&& content != null){
			try {
				HTTPPost.httpPostRequest(url,content);
				return true;
			} catch (IOException e) {
				e.printStackTrace(stderr);
				return false;
			}
		}
		return false;
	}

	public LinkedHashMap<String, String> getTableMap() {
		LinkedHashMap<String,String> tableMap= new LinkedHashMap<String,String>();

		/*		for(int x=0;x<table.getRowCount();x++){
			String key =(String) table.getValueAt(x, 0);
			String value = (String) table.getValueAt(x, 1); //encountered a "ArrayIndexOutOfBoundsException" error here~~ strange!
			tableMap.put(key,value);
		}
		return tableMap;*/

		Vector data = domainTableModel.getDataVector();
		for (Object o : data) {
			Vector v = (Vector) o;
			String key = (String) v.elementAt(0);
			String value = (String) v.elementAt(1);
			if (key != null && value != null) {
				tableMap.put(key.trim(), value.trim());
			}
		}
		return tableMap;
	}

	public static Set<String> getSetFromTextArea(JTextArea textarea){
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll("\r\n", "\n").split("\n")));
		domainList.remove("");
		return domainList;
	}

	public void ClearTable() {
		LinkedHashMap<String, String> tmp = domainResult.getRootDomainMap();

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.setRowCount(0);
		//this also trigger tableModel listener. lead to rootDomainMap to empty!!
		//so need to backup rootDomainMap and restore!
		domainResult.setRootDomainMap(tmp);
	}

	public void showToTitleUI(List<LineEntry> lineEntries) {
		//titleTableModel.setLineEntries(new ArrayList<LineEntry>());//clear
		//这里没有fire delete事件，会导致排序号加载文件出错，但是如果fire了又会触发tableModel的删除事件，导致数据库删除。改用clear()
		titleTableModel.clear();
		for (LineEntry line:lineEntries) {
			titleTableModel.addNewLineEntry(line);
		}
		digStatus();
		stdout.println("Load Title Panel Data Done");
	}

	public File saveDomainOnly() {
		try {
			File file = dialog(false);
			if(!(file.getName().toLowerCase().endsWith(".db"))){
				file=new File(fc.getCurrentDirectory(),file.getName()+".db");
			}

			if (domainResult.projectName.equals("")) {
				domainResult.projectName = file.getName();
			}

			if(file.exists()){
				int result = JOptionPane.showConfirmDialog(null,"Are you sure to overwrite this file ?");
				if (result == JOptionPane.YES_OPTION) {
					file.createNewFile();
				}else {
					return null;
				}
			}else {
				file.createNewFile();
			}

			DBHelper dbHelper = new DBHelper(file.toString());
			dbHelper.addDomainObject(domainResult);
			stdout.println("Save Domain Only Success! "+ Commons.getNowTimeString());
			return file;
		} catch (HeadlessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stdout.println("Save Domain Only failed! "+ Commons.getNowTimeString());
		return null;
	}


	public File saveDialog(boolean includeTitle) {
		try{

			File file;
			if (null != currentDBFile && currentDBFile.getAbsolutePath().endsWith(".db")) {
				file = currentDBFile;
			}else {
				file = dialog(false);
				if (file == null) return null;
				if(!(file.getName().toLowerCase().endsWith(".db"))){
					file=new File(fc.getCurrentDirectory(),file.getName()+".db");
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

			if (domainResult.projectName.equals("")) {
				domainResult.projectName = file.getName();
			}


			DBHelper dbHelper = new DBHelper(file.toString());
			dbHelper.addDomainObject(domainResult);
			if (includeTitle){
				dbHelper.saveTitles(titleTableModel.getLineEntries());
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
			File file = dialog(true);
			if (file == null) {
				return null;
			}
			if (file.getName().endsWith(".json")){//兼容旧文件
				String contents = Files.toString(file, Charsets.UTF_8);//读取json文件的方式
				domainResult = JSON.parseObject(contents,DomainObject.class);
				if (domainResult != null) showToDomainUI(domainResult);
			}else {
				DBHelper dbhelper = new DBHelper(file.toString());
				domainResult = dbhelper.getDomainObj();
				if (domainResult != null) showToDomainUI(domainResult);
				showToTitleUI(dbhelper.getTitles());
			}
			currentDBFile = file;//就是应该在对话框完成后就更新
			saveDBfilepathToExtension();
			stdout.println("open Project ["+domainResult.projectName+"] From File "+ file.getName());
			return file;
		} catch (Exception e1) {
			e1.printStackTrace(stderr);
			return null;
		}
	}

	public File dialog(boolean isOpen) {
		if (fc.getCurrentDirectory() != null) {
			File xxx = fc.getCurrentDirectory();
			fc = new JFileChooser(fc.getCurrentDirectory());
		}else {
			fc = new JFileChooser();
		}

		JsonFileFilter jsonFilter = new JsonFileFilter(); //文件扩展名过滤器  
		fc.addChoosableFileFilter(jsonFilter);
		fc.setFileFilter(jsonFilter);
		fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

		int action;
		if (isOpen) {
			action = fc.showOpenDialog(null);
		}else {
			action = fc.showSaveDialog(null);
		}

		if(action==JFileChooser.APPROVE_OPTION){
			File file=fc.getSelectedFile();
			fc.setCurrentDirectory(new File(file.getParent()));//save latest used dir.
			return file;
		}
		return null;
	}

	public static void digStatus() {
		String status = titleTableModel.getStatusSummary();
		lblSummaryOfTitle.setText(status);
	}

	class JsonFileFilter extends FileFilter {
		public String getDescription() {
			return "*.db";
		}//sqlite
		public boolean accept(File file) {
			String name = file.getName();
			return file.isDirectory() || name.toLowerCase().endsWith(".db");  // 仅显示目录和json文件
		}
	}


	public String getSubnet(boolean isCurrent){
		Set<String> subnets;
		if (isCurrent) {//获取的是现有可成功连接的IP集合
			subnets = titleTableModel.GetSubnets();
		}else {//重新解析所有域名的IP
			Set<String> IPsOfDomain = new ThreadGetSubnet(domainResult.getSubDomainSet()).Do();
			//Set<String> CSubNetIPs = Commons.subNetsToIPSet(Commons.toSubNets(IPsOfDomain));
			subnets = Commons.toSmallerSubNets(IPsOfDomain);
		}
		return String.join(System.lineSeparator(), subnets);
	}

	public void getAllTitle(){
		return;
		//sub class should over write this function
	}
	protected void getExtendTitle() {
		// BurpExtender need to override this function
		return;
	}
}
