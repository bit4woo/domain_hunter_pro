package burp;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.List;

public class DomainPanel extends JPanel {

    private JRadioButton rdbtnAddRelatedToRoot;
    private JTabbedPane tabbedWrapper;
    private JPanel domainContentPane;
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

    protected static DomainObject domainResult = null;//getter setter

    public DomainPanel() {//构造函数
        domainContentPane =  new JPanel();
        domainContentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        domainContentPane.setLayout(new BorderLayout(0, 0));


        PrintWriter stdout = new PrintWriter(System.out, true);
        PrintWriter stderr = new PrintWriter(System.out, true);
        ///////////////////////HeaderPanel//////////////


        JPanel HeaderPanel = new JPanel();
        FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
        fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
        domainContentPane.add(HeaderPanel, BorderLayout.NORTH);




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


        JButton btnRenameProject = new JButton("Rename Project");
        btnRenameProject.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (domainResult !=null){
                    String newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
                    while(newProjectName.trim().equals("")){
                        newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
                    }
                    domainResult.setProjectName(newProjectName);
                    lblSummary.setText(domainResult.getSummary());
                }
            }
        });
        HeaderPanel.add(btnRenameProject);

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
                enteredRootDomain = enteredRootDomain.trim().toLowerCase();
                if (enteredRootDomain.startsWith("http://") || enteredRootDomain.startsWith("https://")){
                    try {
                        URL url = new URL(enteredRootDomain);
                        enteredRootDomain = url.getHost();
                    } catch (Exception e2) {

                    }
                }
                enteredRootDomain = InternetDomainName.from(enteredRootDomain).topPrivateDomain().toString();
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

    public void showToDomainUI(DomainObject domainResult) {

        domainResult.relatedToRoot();
        ClearTable();

        for (Map.Entry<String, String> entry:domainResult.getRootDomainMap().entrySet()) {
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


    //////////////////////////////methods//////////////////////////////////////
    public Map<String, Set<String>> crawl (Set<String> rootdomains, Set<String> keywords) {
        System.out.println("spiderall testing... you need to over write this function!");
        return null;
    }

    public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords){
        System.out.println("search testing... you need to over write this function!");
        return null;
    }
}
