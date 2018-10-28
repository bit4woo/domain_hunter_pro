package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingWorker;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.FlowLayout;
import javax.swing.JSplitPane;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;


import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.border.LineBorder;
import javax.swing.ListSelectionModel;
import javax.swing.JRadioButton;

public class xxxx extends JFrame {
	
    public String ExtenderName = "Domain Hunter v1.2 by bit4";
    public String github = "https://github.com/bit4woo/domain_hunter";
    private String summary = "      Related-domain:%s  Sub-domain:%s  Similar-domain:%s  ^_^";
    public Set<String> rootDomainSet = new HashSet<String>();
    public Set<String> subDomainSet = new HashSet<String>();
    public Set<String> similarDomainSet = new HashSet<String>();
    public Set<String> relatedDomainSet = new HashSet<String>();
    
    public static int SUB_DOMAIN=0;
    public static int SIMILAR_DOMAIN=1;
    public static int IP_ADDRESS=2;
    public static int USELESS =-1;
    
    public String resultJson;
    
    public PrintWriter stdout;
    public PrintWriter stderr;
    
	private JPanel contentPane;
	private JTextField textFieldUploadURL;
	private JButton btnSearch;
	private JButton btnUpload;
	private JButton btnSpiderAll;
	private JLabel lblSummary;
	private JPanel FooterPanel;
	private JLabel lblNewLabel_2;
	private JScrollPane TargetPanel;
	private JTextArea textAreaSubdomains;
	private JTextArea textAreaSimilarDomains;
	
	
	
	public boolean autoAddRelatedDomainToRootDomain = true;
	public int sortedColumn;
	public SortOrder sortedMethod;
	private JTable table;
	private JPanel panel;
	private JButton RemoveButton;
	private JButton AddButton;
	private JSplitPane TargetSplitPane;
	private JTextArea textAreaRelatedDomains;
	private JRadioButton rdbtnNewRadioButton;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					xxxx frame = new xxxx();
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
	public xxxx() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1174, 497);
		contentPane =  new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		
		JPanel HeaderPanel = new JPanel();
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		contentPane.add(HeaderPanel, BorderLayout.NORTH);
		
		JLabel lblUploadURL = new JLabel("Upload URL ");
		HeaderPanel.add(lblUploadURL);
		
		textFieldUploadURL = new JTextField("http://");
		HeaderPanel.add(textFieldUploadURL);
		textFieldUploadURL.setColumns(20);
		
		btnSearch = new JButton("Search");
		btnSearch.setToolTipText("Do a single search from site map");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			    	//using SwingWorker to prevent blocking burp main UI.

			        @Override
			        protected Map doInBackground() throws Exception {
						Set<String> rootDomains = getRootDomains();
						stderr.print(rootDomains.size());
						Set<String> keywords= getKeywords();
						stderr.print(rootDomains.size());
						btnSearch.setEnabled(false);
						return search(rootDomains,keywords);
			        }
			        @Override
			        protected void done() {
			            try {
				        	Map result = get();
				        	subDomainSet = (Set) result.get("subDomainSet"); //之前的set变成了object
				        	similarDomainSet = (Set) result.get("similarDomainSet");
				        	relatedDomainSet = (Set) result.get("relatedDomainSet");
							textAreaSubdomains.setText(Commons.set2string(subDomainSet));
							textAreaSimilarDomains.setText(Commons.set2string(similarDomainSet));
							btnSearch.setEnabled(true);
							lblSummary.setText(String.format(summary, subDomainSet.size(),similarDomainSet.size(),relatedDomainSet.size()));
			            } catch (Exception e) {
			            	btnSearch.setEnabled(true);
			                //e.printStackTrace(stderr);
			            }
			        }
			    };      
			    worker.execute();
				
			}
		});
		HeaderPanel.add(btnSearch);
		
		btnSpiderAll = new JButton("Spider All");
		btnSpiderAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
			    SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
			    	//可以在一个类中实现另一个类，直接实现原始类，没有变量处理的困扰；
			    	//之前的想法是先单独实现一个worker类，在它里面处理各种，就多了一层实现，然后在这里调用，变量调用会是一个大问题。
			    	//https://stackoverflow.com/questions/19708646/how-to-update-swing-ui-while-actionlistener-is-in-progress
			        @Override
			        protected Map doInBackground() throws Exception {                
						Set<String> rootDomains = getRootDomains();
						Set<String> keywords= getKeywords();
						//stdout.println(subdomain);
						//stdout.println(domainlike);
						btnSpiderAll.setEnabled(false);
						return spiderall(rootDomains,keywords);
					
			        }
			        @Override
			        protected void done() {
			            try {
				        	Map result = get();
				        	subDomainSet = (Set<String>) result.get("subDomainSet"); //之前的set变成了object
				        	similarDomainSet = (Set<String>) result.get("domainlikeset");
							textAreaSubdomains.setText(Commons.set2string(subDomainSet));
							textAreaSimilarDomains.setText(Commons.set2string(similarDomainSet));
							btnSpiderAll.setEnabled(true);
			            } catch (Exception e) {
			                e.printStackTrace(stderr);
			            }
			        }
			    };
			    worker.execute();
			}
		});
		btnSpiderAll.setToolTipText("Spider all subdomains recursively,This may take a long time!!!");
		HeaderPanel.add(btnSpiderAll);
		
		
		btnUpload = new JButton("Upload");
		btnUpload.setToolTipText("Do a single search from site map");
		btnUpload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Boolean, Boolean> worker = new SwingWorker<Boolean, Boolean>() {
			        @Override
			        protected Boolean doInBackground() throws Exception {                
			        	return upload("","");
			        }
			        @Override
			        protected void done() {
			        }
			    };  
			    worker.execute();
				
			}
		});
		HeaderPanel.add(btnUpload);
		
		lblSummary = new JLabel("      ^_^");
		HeaderPanel.add(lblSummary);
		
		TargetPanel = new JScrollPane();
		TargetPanel.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
		//contentPane.add(TargetPanel, BorderLayout.WEST);
		
		table = new JTable();
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setBorder(new LineBorder(new Color(0, 0, 0)));
		

		
		table.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					sortedColumn = table.getRowSorter().getSortKeys().get(0).getColumn();
					//System.out.println(sortedColumn);
					sortedMethod = table.getRowSorter().getSortKeys().get(0).getSortOrder();
					System.out.println(sortedMethod); //ASCENDING   DESCENDING
				} catch (Exception e1) {
					sortedColumn = -1;
					sortedMethod = null;
					e1.printStackTrace(stderr);
				}
			}
		});
		
		DefaultTableModel tableModel =new DefaultTableModel(
			new Object[][] {
				//{"1", "1","1"},
			},
			new String[] {
				"Root Domain", "Keyword", "Source"
			}
		);
		table.setModel(tableModel);
		//table.setTableHeader(tableHeader);
		
		
		RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
		table.setRowSorter(sorter);
		
		table.setColumnSelectionAllowed(true);
		table.setCellSelectionEnabled(true);
		table.setSurrendersFocusOnKeystroke(true);
		table.setFillsViewportHeight(true);
		table.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		TargetPanel.setViewportView(table);
		
		TargetSplitPane = new JSplitPane();
		TargetSplitPane.setResizeWeight(0.5);
		TargetSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		contentPane.add(TargetSplitPane, BorderLayout.WEST);
		
		TargetSplitPane.setLeftComponent(TargetPanel);
		
		panel = new JPanel();
		TargetSplitPane.setRightComponent(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		
		AddButton = new JButton("Add");
		AddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
				enteredRootDomain = enteredRootDomain.trim();
				String keyword = enteredRootDomain.substring(0,enteredRootDomain.lastIndexOf("."));
				tableModel.addRow(new Object[]{enteredRootDomain,keyword,"manual added"});
			}
		});
		panel.add(AddButton);
		
		
		RemoveButton = new JButton("Remove");
		panel.add(RemoveButton);
		
		rdbtnNewRadioButton = new JRadioButton("Auto Add Related Domain To Root Domain");
		panel.add(rdbtnNewRadioButton);
		RemoveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] rowindexs = table.getSelectedRows();
				for (int i=0; i < rowindexs.length; i++){
					rowindexs[i] = table.convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
				}
				Arrays.sort(rowindexs);
				
				DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
				for(int i=rowindexs.length-1;i>=0;i--){
					tableModel.removeRow(rowindexs[i]);
				}
			}
		});

		
		
		
		JSplitPane ResultSplitPane = new JSplitPane();

		contentPane.add(ResultSplitPane, BorderLayout.EAST);
		
		textAreaSubdomains = new JTextArea();
		ResultSplitPane.setLeftComponent(textAreaSubdomains);
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
		
		textAreaSimilarDomains = new JTextArea();
		ResultSplitPane.setRightComponent(textAreaSimilarDomains);
		textAreaSimilarDomains.setColumns(30);
		
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
		
		textAreaRelatedDomains = new JTextArea();
		contentPane.add(textAreaRelatedDomains, BorderLayout.CENTER);
	}
	
	public Map<String, Set<String>> spiderall (Set<String> rootdomains, Set<String> keywords) {
	    System.out.println("spiderall testing... you need to over write this function!");
	    return null;
	}
	
	
	public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords){
		System.out.println("search testing... you need to over write this function!");
		return null;
	}
	public Boolean upload(String url,String resultJson) {
		System.out.println("upload testing... you need to over write this function!");
		return null;
	}
	
	
	public Set<String> getRootDomains() {
		
		Set<String> result = new HashSet<String>();
		int index=0;
		for (int i=0;i<table.getColumnCount();i++) {
			if (table.getColumnName(i).equals("Root Domain"));
				index = i;
		}
    	
		for(int j=0;j<table.getRowCount();j++){
		  String onecell= (String)table.getValueAt(j,index);
		  if (!onecell.equals("") && !onecell.equals(null)) {
			  result.add(onecell);
		  }
		}
		return result;
	}
	
	
	public Set<String> getKeywords() {
		
		Set<String> result = new HashSet<String>();
		int index=0;
		for (int i=0;i<table.getColumnCount();i++) {
			if (table.getColumnName(i).equals("Keyword"));
				index = i;
		}
    	
		for(int j=0;j<table.getRowCount();j++){
		  String onecell= (String)table.getValueAt(j,index);
		  if (!onecell.equals("") && !onecell.equals(null)) {
			  result.add(onecell);
		  }
		}
		return result;
	}
}
