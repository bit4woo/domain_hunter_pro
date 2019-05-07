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

import org.omg.CORBA.PUBLIC_MEMBER;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;

import test.HTTPPost;

public class GUI extends JFrame {

	public String ExtenderName = "Domain Hunter Pro v1.4 by bit4";
	public String github = "https://github.com/bit4woo/bug_hunter";

	protected static DomainObject domainResult = new DomainObject("");//getter setter
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


	public void DomainPanel() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		contentPane =  new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));



		stdout = new PrintWriter(System.out, true);
		stderr = new PrintWriter(System.out, true);
		///////////////////////HeaderPanel//////////////


		JPanel HeaderPanel = new JPanel();
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		contentPane.add(HeaderPanel, BorderLayout.NORTH);


		btnNew = new JButton("New");
		btnNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (domainResult != null){
					//saveDialog(true);//save old project
					int result = JOptionPane.showConfirmDialog(null,"Save Current Project?");

					/*     是:   JOptionPane.YES_OPTION
					 *     否:   JOptionPane.NO_OPTION
					 *     取消: JOptionPane.CANCEL_OPTION
					 *     关闭: JOptionPane.CLOSED_OPTION*/
					if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
						return;
					}else if (result == JOptionPane.YES_OPTION) {
						saveDialog(true);
					}else if (result == JOptionPane.NO_OPTION) {
						// nothing to do
					}
				}

				domainResult = new DomainObject("");
				titleTableModel.clear();
				currentDBFile = null;
				saveDialog(false);
				showToDomainUI(domainResult);

			}
		});
		btnNew.setToolTipText("Create A New Project");
		HeaderPanel.add(btnNew);



		btnOpen = new JButton("Open");
		btnOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openDialog();
			}
		});
		btnOpen.setToolTipText("Open Domain Hunter Project File");
		HeaderPanel.add(btnOpen);

		btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveDialog(false);
			}});

		InputMap inputMap = btnSave.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke sav = KeyStroke.getKeyStroke(KeyEvent.VK_S, 2); //2 --ctrl;  Ctrl+S
		inputMap.put(sav, "Save");

		btnSave.getActionMap().put("Save", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				saveDialog(false);
			}
		});

		btnSave.setToolTipText("Save Domain Hunter Project File, not include title");
		HeaderPanel.add(btnSave);


		JButton btnSaveDomainOnly = new JButton("Save Domain Only");
		btnSaveDomainOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveDomainOnly();
			}});
		HeaderPanel.add(btnSaveDomainOnly);



		btnSearch = new JButton("Search");
		btnSearch.setToolTipText("Do a single search from site map");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//using SwingWorker to prevent blocking burp main UI.

					@Override
					protected Map doInBackground() throws Exception {

						Set<String> rootDomains = domainResult.fetchRootDomainSet();
						Set<String> keywords= domainResult.fetchKeywordSet();

						//stderr.print(keywords.size());
						//System.out.println(rootDomains.toString());
						//System.out.println("xxx"+keywords.toString());
						btnSearch.setEnabled(false);
						return search(rootDomains,keywords);
					}
					@Override
					protected void done() {
						try {
							get();
							showToDomainUI(domainResult);
							saveDialog(false);
							btnSearch.setEnabled(true);
							stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
						} catch (Exception e) {
							btnSearch.setEnabled(true);
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();

			}
		});

		verticalStrut = Box.createVerticalStrut(20);
		HeaderPanel.add(verticalStrut);
		HeaderPanel.add(btnSearch);

		btnCrawl = new JButton("Crawl");
		btnCrawl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					//可以在一个类中实现另一个类，直接实现原始类，没有变量处理的困扰；
					//之前的想法是先单独实现一个worker类，在它里面处理各种，就多了一层实现，然后在这里调用，变量调用会是一个大问题。
					//https://stackoverflow.com/questions/19708646/how-to-update-swing-ui-while-actionlistener-is-in-progress
					@Override
					protected Map doInBackground() throws Exception {
						domainResult.setRootDomainMap(getTableMap());
						Set<String> rootDomains = domainResult.fetchRootDomainSet();
						Set<String> keywords= domainResult.fetchKeywordSet();

						btnCrawl.setEnabled(false);
						return crawl(rootDomains,keywords);

					}
					@Override
					protected void done() {
						try {
							get();
							showToDomainUI(domainResult);
							btnCrawl.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		btnCrawl.setToolTipText("Crawl all subdomains recursively,This may take a long time and large Memory Usage!!!");
		HeaderPanel.add(btnCrawl);

		btnImportDomain = new JButton("Import Domain");
		btnImportDomain.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc=new JFileChooser();
				fc.setDialogTitle("Chose Domain File");
				fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
				if(fc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION){
					try {
						File file=fc.getSelectedFile();
						List<String> lines = Files.readLines(file, Charsets.UTF_8);
						for (String line:lines) {
							line = line.trim();
							int type =  domainResult.domainType(line);
							if (type == DomainObject.SUB_DOMAIN) {
								domainResult.getSubDomainSet().add(line);
							}else if(type == DomainObject.SIMILAR_DOMAIN) {
								domainResult.getSimilarDomainSet().add(line);
							}else {
								stdout.println("import skip "+line);
							}
						}
						showToDomainUI(domainResult);//保存配置并更新图形显示
						stdout.println("Import domains finished from "+ file.getName());
						//List<String> lines = Files.readLines(file, Charsets.UTF_8);

					} catch (IOException e1) {
						e1.printStackTrace(stderr);
					}
				}
			}
		});
		HeaderPanel.add(btnImportDomain);

		verticalStrut_1 = Box.createVerticalStrut(20);
		HeaderPanel.add(verticalStrut_1);

		textFieldUploadURL = new JTextField("Input Upload URL Here");
		textFieldUploadURL.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (textFieldUploadURL.getText().equals("Input Upload URL Here")) {
					textFieldUploadURL.setText("");
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				if (textFieldUploadURL.getText().equals("")) {
					textFieldUploadURL.setText("Input Upload URL Here");
				}

			}
		});
		//HeaderPanel.add(textFieldUploadURL);
		textFieldUploadURL.setColumns(30);


		btnUpload = new JButton("Upload");
		btnUpload.setToolTipText("Do a single search from site map");
		btnUpload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Boolean, Boolean> worker = new SwingWorker<Boolean, Boolean>() {
					@Override
					protected Boolean doInBackground() throws Exception {
						return upload(domainResult.uploadURL,domainResult.ToJson());
					}
					@Override
					protected void done() {
						//TODO
					}
				};
				worker.execute();
			}
		});
		//HeaderPanel.add(btnUpload);

		lblSummary = new JLabel("      ^_^");
		HeaderPanel.add(lblSummary);


		////////////////////////////////////target area///////////////////////////////////////////////////////


		JScrollPane TargetPanel = new JScrollPane();//存放目标域名
		TargetPanel.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
		//contentPane.add(TargetPanel, BorderLayout.WEST);

		table = new JTable();
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setBorder(new LineBorder(new Color(0, 0, 0)));

		table.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					table.getRowSorter().getSortKeys().get(0).getColumn();
					//System.out.println(sortedColumn);
					sortedMethod = table.getRowSorter().getSortKeys().get(0).getSortOrder();
					System.out.println(sortedMethod); //ASCENDING   DESCENDING
				} catch (Exception e1) {
					sortedMethod = null;
					e1.printStackTrace(stderr);
				}
			}
		});

		domainTableModel = new DefaultTableModel(
				new Object[][] {
					//{"1", "1","1"},
				},
				new String[] {
						"Root Domain", "Keyword"//, "Source"
				}
				);
		table.setModel(domainTableModel);
		domainTableModel.addTableModelListener(new TableModelListener(){
			@Override
			public void tableChanged(TableModelEvent e) {
				domainResult.setRootDomainMap(getTableMap());
			}
		});



		RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(domainTableModel);
		table.setRowSorter(sorter);

		table.setColumnSelectionAllowed(true);
		table.setCellSelectionEnabled(true);
		table.setSurrendersFocusOnKeystroke(true);
		table.setFillsViewportHeight(true);
		table.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		TargetPanel.setViewportView(table);

		JSplitPane CenterSplitPane = new JSplitPane();
		CenterSplitPane.setResizeWeight(0.5);
		contentPane.add(CenterSplitPane, BorderLayout.CENTER);


		JSplitPane leftOfCenterSplitPane = new JSplitPane();
		leftOfCenterSplitPane.setResizeWeight(0.5);
		CenterSplitPane.setLeftComponent(leftOfCenterSplitPane);


		JSplitPane rightOfCenterSplitPane = new JSplitPane();//右半部分
		rightOfCenterSplitPane.setResizeWeight(0.5);
		CenterSplitPane.setRightComponent(rightOfCenterSplitPane);

		JSplitPane TargetSplitPane = new JSplitPane();//1/4
		TargetSplitPane.setResizeWeight(0.5);
		TargetSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftOfCenterSplitPane.setLeftComponent(TargetSplitPane);

		TargetSplitPane.setLeftComponent(TargetPanel);


		///////////////////////////////Target Operations and Config//////////////////////


		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0)));
		TargetSplitPane.setRightComponent(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));


		AddButton = new JButton("Add");
		AddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
				enteredRootDomain = enteredRootDomain.trim();
				enteredRootDomain =InternetDomainName.from(enteredRootDomain).topPrivateDomain().toString();
				String keyword = enteredRootDomain.substring(0,enteredRootDomain.indexOf("."));

				domainResult.AddToRootDomainMap(enteredRootDomain, keyword);
				showToDomainUI(domainResult);


				/*				if (domainResult.rootDomainMap.containsKey(enteredRootDomain) && domainResult.rootDomainMap.containsValue(keyword)) {
					//do nothing
				}else {
					domainResult.rootDomainMap.put(enteredRootDomain,keyword);
					showToUI(domainResult);
				}*/
			}
		});
		panel.add(AddButton);


		RemoveButton = new JButton("Remove");
		panel.add(RemoveButton);
		RemoveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int[] rowindexs = table.getSelectedRows();
				for (int i=0; i < rowindexs.length; i++){
					rowindexs[i] = table.convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
				}
				Arrays.sort(rowindexs);

				domainTableModel = (DefaultTableModel) table.getModel();
				for(int i=rowindexs.length-1;i>=0;i--){
					domainTableModel.removeRow(rowindexs[i]);
				}
				// will trigger tableModel listener

				//domainResult.setRootDomainMap(getTableMap()); //no need any more because tableModel Listener

				
			}
		});
		
		JButton btnFresh = new JButton("Fresh");
		panel.add(btnFresh);
		btnFresh.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//to clear sub and similar domains
				Set<String> tmpDomains = domainResult.getSubDomainSet();
				Set<String> newSubDomainSet = new HashSet<>();
				Set<String> newSimilarDomainSet = new HashSet<String>();
				tmpDomains.addAll(domainResult.getSimilarDomainSet());
				for (String domain:tmpDomains) {
					int type = BurpExtender.domainResult.domainType(domain);
					if (type == DomainObject.SUB_DOMAIN)
					{	
						newSubDomainSet.add(domain);
					}else if (type == DomainObject.SIMILAR_DOMAIN) {
						newSimilarDomainSet.add(domain);
					}
				}
				
				domainResult.setSubDomainSet(newSubDomainSet);
				domainResult.setSimilarDomainSet(newSimilarDomainSet);

				showToDomainUI(domainResult);
			}
		});

		btnCopy = new JButton("Copy");
		btnCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(domainResult.fetchRootDomains());
				clipboard.setContents(selection, null);

			}
		});
		btnCopy.setToolTipText("Copy Root Domains To ClipBoard");
		panel.add(btnCopy);


		rdbtnAddRelatedToRoot = new JRadioButton("Auto Add Related Domain To Root Domain");
		rdbtnAddRelatedToRoot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				domainResult.autoAddRelatedToRoot = rdbtnAddRelatedToRoot.isSelected();
				if (domainResult.autoAddRelatedToRoot==true) {
					domainResult.relatedToRoot();
					showToDomainUI(domainResult);/*
					Set<String> tableRootDomains = getColumnValues("Root Domain");
					for(String relatedDomain:domainResult.relatedDomainSet) {
			        	String rootDomain =InternetDomainName.from(relatedDomain).topPrivateDomain().toString();
						String keyword = rootDomain.substring(0,rootDomain.indexOf("."));
						if (!tableRootDomains.contains(rootDomain)) {
							tableModel.addRow(new Object[]{rootDomain,keyword});
						}
						//after this, tableModelListener will auto update rootDomainMap.
					}

					for (String similarDomain:domainResult.similarDomainSet) {
						String rootDomain =InternetDomainName.from(similarDomain).topPrivateDomain().toString();
						if (domainResult.rootDomainMap.keySet().contains(rootDomain)) {
							domainResult.subDomainSet.add(similarDomain);
							domainResult.similarDomainSet.remove(similarDomain);
						}
					}*/
				}
			}
		});
		rdbtnAddRelatedToRoot.setSelected(false);
		panel.add(rdbtnAddRelatedToRoot);


		///////////////////////////////textAreas///////////////////////////////////////////////////////

		JScrollPane ScrollPaneRelatedDomains = new JScrollPane();
		JScrollPane ScrollPaneSubdomains = new JScrollPane();
		JScrollPane ScrollPaneSimilarDomains = new JScrollPane();


		leftOfCenterSplitPane.setRightComponent(ScrollPaneRelatedDomains);
		rightOfCenterSplitPane.setLeftComponent(ScrollPaneSubdomains);
		rightOfCenterSplitPane.setRightComponent(ScrollPaneSimilarDomains);

		textAreaRelatedDomains = new JTextArea();
		textAreaSubdomains = new JTextArea();
		textAreaSimilarDomains = new JTextArea();

		ScrollPaneRelatedDomains.setViewportView(textAreaRelatedDomains);
		ScrollPaneSubdomains.setViewportView(textAreaSubdomains);
		ScrollPaneSimilarDomains.setViewportView(textAreaSimilarDomains);
		


		textAreaRelatedDomains.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				domainResult.setRelatedDomainSet(getSetFromTextArea(textAreaRelatedDomains));
				lblSummary.setText(domainResult.getSummary());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				domainResult.setRelatedDomainSet(getSetFromTextArea(textAreaRelatedDomains));
				lblSummary.setText(domainResult.getSummary());
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				domainResult.setRelatedDomainSet(getSetFromTextArea(textAreaRelatedDomains));
				lblSummary.setText(domainResult.getSummary());
			}
		});

		textAreaSubdomains.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				domainResult.setSubDomainSet(getSetFromTextArea(textAreaSubdomains));
				lblSummary.setText(domainResult.getSummary());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				domainResult.setSubDomainSet(getSetFromTextArea(textAreaSubdomains));
				lblSummary.setText(domainResult.getSummary());
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				domainResult.setSubDomainSet(getSetFromTextArea(textAreaSubdomains));
				lblSummary.setText(domainResult.getSummary());
			}
		});

		textAreaSimilarDomains.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {				
				domainResult.setSimilarDomainSet(getSetFromTextArea(textAreaSimilarDomains));
				lblSummary.setText(domainResult.getSummary());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				domainResult.setSimilarDomainSet(getSetFromTextArea(textAreaSimilarDomains));
				lblSummary.setText(domainResult.getSummary());
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				domainResult.setSimilarDomainSet(getSetFromTextArea(textAreaSimilarDomains));
				lblSummary.setText(domainResult.getSummary());
			}
		});

		textAreaSubdomains.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				final JPopupMenu jp = new JPopupMenu();
				jp.add("^_^");
				textAreaSubdomains.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON3) {
							// 弹出菜单
							jp.show(textAreaSubdomains, e.getX(), e.getY());
						}
					}
				});
			}
		});
		textAreaSubdomains.setColumns(30);


		///////////////////////////FooterPanel//////////////////


		FooterPanel = new JPanel();
		FlowLayout fl_FooterPanel = (FlowLayout) FooterPanel.getLayout();
		fl_FooterPanel.setAlignment(FlowLayout.LEFT);
		contentPane.add(FooterPanel, BorderLayout.SOUTH);

		lblNewLabel_2 = new JLabel(ExtenderName+"    "+github);
		lblNewLabel_2.setFont(new Font("宋体", Font.BOLD, 12));
		lblNewLabel_2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					URI uri = new URI(github);
					Desktop desktop = Desktop.getDesktop();
					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace(stderr);
				}

			}
			@Override
			public void mouseEntered(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLUE);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLACK);
			}
		});
		FooterPanel.add(lblNewLabel_2);

	}



	public JPanel TitlePanel() {
		//
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		TitlePanel = new JPanel();
		TitlePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		TitlePanel.setLayout(new BorderLayout(0, 0));

		buttonPanel = new JPanel();
		TitlePanel.add(buttonPanel, BorderLayout.NORTH);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		btnGettitle = new JButton("Get Title");
		btnGettitle.setToolTipText("A fresh start");
		btnGettitle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//https://stackabuse.com/how-to-use-threads-in-java-swing/

				//method one: // don't need to wait threads in getAllTitle to exits
				//but hard to know the finish time of task
				//// Runs inside of the Swing UI thread
				/*			    SwingUtilities.invokeLater(new Runnable() {
			        public void run() {// don't need to wait threads in getAllTitle to exits
			        	btnGettitle.setEnabled(false);
			        	getAllTitle();
			        	btnGettitle.setEnabled(true);
			        	//domainResult.setLineEntries(TitletableModel.getLineEntries());
			        }
			    });*/

				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnGettitle.setEnabled(false);
						getAllTitle();
						saveDialog(true);
						btnGettitle.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							btnGettitle.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(btnGettitle);

		btnGetExtendtitle = new JButton("Get Extend Title");
		btnGetExtendtitle.setToolTipText("Get title of the host that in same subnet,you should do this after get domain title done!");
		btnGetExtendtitle.setEnabled(true);//default is false,only true after "get title" is done.
		btnGetExtendtitle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnGetExtendtitle.setEnabled(false);
						getExtendTitle();
						saveDialog(true);
						btnGetExtendtitle.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							btnGetExtendtitle.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(btnGetExtendtitle);

		JButton btnGetSubnet = new JButton("Get Subnet");
		btnGetSubnet.setEnabled(true);
		btnGetSubnet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {

						btnGetSubnet.setEnabled(false);
						int result = JOptionPane.showConfirmDialog(null,"Just get IP Subnets of [Current] lines ?");
						String subnetsString;
						if (result == JOptionPane.YES_OPTION) {
							subnetsString = getSubnet(true);
						}else {
							subnetsString = getSubnet(false);
						}
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection selection = new StringSelection(subnetsString);
						clipboard.setContents(selection, null);
						stdout.print(subnetsString);
						btnGetSubnet.setEnabled(true);
						return new HashMap<String, String>();
						//no use ,the return.
					}
					@Override
					protected void done() {
						try {
							btnGetSubnet.setEnabled(true);
						} catch (Exception e) {
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(btnGetSubnet);


		btnSaveState = new JButton("Save");
		btnSaveState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnSaveState.setEnabled(false);
						saveDialog(true);//both tilte and domain
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
		btnSaveState.setToolTipText("Save Data To DataBase");
		buttonPanel.add(btnSaveState);


		InputMap inputMap1 = btnSaveState.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke Save = KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK); //Ctrl+S
		inputMap1.put(Save, "Save");

		btnSaveState.getActionMap().put("Save", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
					@Override
					protected Map doInBackground() throws Exception {
						btnSaveState.setEnabled(false);
						saveDialog(true);
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

		textFieldSearch = new JTextField("");
		textFieldSearch.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (textFieldSearch.getText().equals("Input text to search")) {
					textFieldSearch.setText("");
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				/*
				 * if (textFieldSearch.getText().equals("")) {
				 * textFieldSearch.setText("Input text to search"); }
				 */

			}
		});

		textFieldSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText().trim();
				titleTable.search(keyword);

			}
		});
		textFieldSearch.setColumns(30);
		buttonPanel.add(textFieldSearch);


		JButton buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = textFieldSearch.getText().trim();
				titleTable.search(keyword);
			}
		});
		buttonPanel.add(buttonSearch);

		rdbtnHideCheckedItems = new JRadioButton("Hide Checked");
		rdbtnHideCheckedItems.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = BurpExtender.textFieldSearch.getText().trim();
				titleTable.search(keyword);
				//lineTable.getModel().unHideLines();
			}
		});
		buttonPanel.add(rdbtnHideCheckedItems);
		
		JButton btnRefresh = new JButton("Refresh");//主要目的是隐藏新标注的条目，代替自动隐藏
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String keyword = BurpExtender.textFieldSearch.getText().trim();
				titleTable.search(keyword);
			}
		});
		buttonPanel.add(btnRefresh);

		JButton btnStatus = new JButton("status");
		btnStatus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				digStatus();
			}
		});
		btnStatus.setToolTipText("Show Status Of Digging.");
		buttonPanel.add(btnStatus);

		lblSummaryOfTitle = new JLabel("      ^_^");
		buttonPanel.add(lblSummaryOfTitle);




		///need to replace this part with LineTableModel and LineTable
		//		table_1 = new JTable();
		//		scrollPaneRequests.setViewportView(table_1);
		//LineTableModel will replace this table


		return TitlePanel;
	}

	//////////////////////////////methods//////////////////////////////////////
	public Map<String, Set<String>> crawl (Set<String> rootdomains, Set<String> keywords) {
		System.out.println("spiderall testing... you need to over write this function!");
		return null;
	}


	public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords){
		System.out.println("search testing... you need to over write this function!");
		return null;
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

	public void showToDomainUI(DomainObject domainResult) {

		domainResult.relatedToRoot();
		ClearTable();

		for (Entry<String, String> entry:domainResult.getRootDomainMap().entrySet()) {
			domainTableModel.addRow(new Object[]{entry.getKey(),entry.getValue()});
		}

		textFieldUploadURL.setText(domainResult.uploadURL);
		textAreaSubdomains.setText(domainResult.fetchSubDomains());
		textAreaSimilarDomains.setText(domainResult.fetchSimilarDomains());
		textAreaRelatedDomains.setText(domainResult.fetchRelatedDomains());
		lblSummary.setText(domainResult.getSummary());
		rdbtnAddRelatedToRoot.setSelected(domainResult.autoAddRelatedToRoot);

		stdout.println("Load Domain Panel Data Done");
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
			dbHelper.saveDomainObject(domainResult);
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
			dbHelper.saveDomainObject(domainResult);
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
