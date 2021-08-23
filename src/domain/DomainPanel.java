package domain;

import GUI.GUI;
import Tools.ToolPanel;
import burp.*;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import toElastic.VMP;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 *注意，所有直接对DomainObject中数据的修改，都不会触发该tableChanged监听器。
 *1、除非操作的逻辑中包含了firexxxx来主动通知监听器。比如DomainPanel.domainTableModel.fireTableChanged(null);
 *2、或者主动调用显示和保存的函数直接完成，不经过监听器。
	//GUI.getDomainPanel().showToDomainUI();
	//DomainPanel.autoSave();
 */
public class DomainPanel extends JPanel {

    private JRadioButton rdbtnAddRelatedToRoot;
    private JTextField textFieldUploadURL;
    private JButton btnSearch;
    private JButton btnCrawl;
    private JLabel lblSummary;
    private JLabel lblNewLabel_2;

    private JTextArea textAreaSubdomains;
    private JTextArea textAreaSimilarDomains;
    private JTextArea textAreaRelatedDomains;
    private JTextArea textAreaEmails;
    private JTextArea textAreaPackages;
    private JTextArea textAreaSubnets;

    private SortOrder sortedMethod;
    private JTable table;
    public static JPanel HeaderPanel;

    private boolean listenerIsOn = true;
    private static final Logger log = LogManager.getLogger(DomainPanel.class);

    public static DomainManager getDomainResult() {
        return domainResult;
    }

    public static void setDomainResult(DomainManager domainResult) {
        DomainPanel.domainResult = domainResult;
    }
    
    //流量分析进程需要用到这个变量，标记为volatile以获取正确的值。
    public volatile static DomainManager domainResult = null;//getter setter
    public static DefaultTableModel domainTableModel;
    PrintWriter stdout;
    PrintWriter stderr;


    public DomainPanel() {//构造函数
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setLayout(new BorderLayout(0, 0));

        try {
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        } catch (Exception e) {
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }


        ///////////////////////HeaderPanel//////////////


        HeaderPanel = new JPanel();
        FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
        fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
        this.add(HeaderPanel, BorderLayout.NORTH);

        JButton btnSaveDomainOnly = new JButton("SaveDomainOnly");
        btnSaveDomainOnly.setToolTipText("Only save data in Domain Panel");
        btnSaveDomainOnly.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveDomainOnly();
            }
        });
        HeaderPanel.add(btnSaveDomainOnly);

		/*
		JButton test = new JButton("test");
		test.setToolTipText("Only save data in Domain Panel");
		test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUI.getProjectMenu().changeTabName("");
			}});
		HeaderPanel.add(test);

		 */

		/*
		btnBrute = new JButton("Brute");
		btnBrute.setToolTipText("Do Brute for all root domains");
		btnBrute.addActionListener(new ActionListener() {
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
						btnBrute.setEnabled(false);
						//						threadBruteDomain = new ThreadBruteDomain(rootDomains);
						//						threadBruteDomain.Do();
						for (String rootDomain: rootDomains){
							threadBruteDomain2 = new ThreadBruteDomainWithDNSServer2(rootDomain);
							threadBruteDomain2.Do();
						}


						return null;
					}
					@Override
					protected void done() {
						try {
							get();
							showToDomainUI();
							autoSave();
							btnBrute.setEnabled(true);
							stdout.println("~~~~~~~~~~~~~brute Done~~~~~~~~~~~~~");
						} catch (Exception e) {
							btnBrute.setEnabled(true);
							e.printStackTrace(stderr);
						}
					}
				};
				worker.execute();
			}
		});

		Component verticalStrut = Box.createVerticalStrut(20);
		HeaderPanel.add(verticalStrut);
		//HeaderPanel.add(btnBrute);
		 */


        btnSearch = new JButton("Search");
        btnSearch.setToolTipText("Do a single search from site map");
        btnSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.

                    @Override
                    protected Map doInBackground() throws Exception {

                        Set<String> rootDomains = domainResult.fetchRootDomainSet();
                        Set<String> keywords = domainResult.fetchKeywordSet();

                        //stderr.print(keywords.size());
                        //System.out.println(rootDomains.toString());
                        //System.out.println("xxx"+keywords.toString());
                        btnSearch.setEnabled(false);
                        domainResult.getEmailSet().addAll(collectEmails());
                        return search(rootDomains, keywords);
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            showToDomainUI();
                            autoSave();
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
                        Set<String> rootDomains = domainResult.fetchRootDomainSet();
                        Set<String> keywords = domainResult.fetchKeywordSet();

                        btnCrawl.setEnabled(false);
                        return crawl(rootDomains, keywords);

                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            showToDomainUI();
                            autoSave();
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


        JButton btnZoneTransferCheck = new JButton("AXFR");
        btnZoneTransferCheck.setToolTipText("Zone Transfer Check");
        btnZoneTransferCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    @Override
                    protected Map doInBackground() throws Exception {
                        stdout.println("~~~~~~~~~~~~~Zone Transfer Checking~~~~~~~~~~~~~");
                        btnZoneTransferCheck.setEnabled(false);
                        ZoneTransferCheckAll();
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnZoneTransferCheck.setEnabled(true);
                        stdout.println("~~~~~~~~~~~~~Zone Transfer Check Done~~~~~~~~~~~~~");
                    }
                };
                worker.execute();
            }
        });
        HeaderPanel.add(btnZoneTransferCheck);
        
        /**
         * 从文本文件中导入域名
         */
        JButton btnImportDomain = new JButton("Import Domain");
        btnImportDomain.setToolTipText("Import Domain From Text File Which One Domain Per Line");
        btnImportDomain.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Chose Domain File");
                fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        File file = fc.getSelectedFile();
                        List<String> lines = Files.readLines(file, Charsets.UTF_8);
                        for (String line : lines) {
                            line = line.trim();
                            int type = domainResult.domainType(line);
                            if (type == DomainManager.SUB_DOMAIN) {
                                domainResult.getSubDomainSet().add(line);
                            } else if (type == DomainManager.SIMILAR_DOMAIN) {
                                domainResult.getSimilarDomainSet().add(line);
                            } else {
                                stdout.println("import skip " + line);
                            }
                        }
                        showToDomainUI();//保存配置并更新图形显示
                        stdout.println("Import domains finished from " + file.getName());
                        autoSave();
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

                if (domainResult != null) {
                    String newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
                    while (newProjectName.trim().equals("")) {
                        newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
                    }
                    domainResult.setProjectName(newProjectName);
                    GUI.displayProjectName();
                    autoSave();
                }

				/*重命名文件，开销太大不合适
				if (GUI.getCurrentDBFile() !=null){
					String newDBFileName = JOptionPane.showInputDialog("New Project Name", null).trim();
					while(newDBFileName.trim().equals("")){
						newDBFileName = JOptionPane.showInputDialog("New Project Name", null).trim();
					}
					if (!newDBFileName.endsWith(".db")){
						newDBFileName = newDBFileName+".db";
					}
					File des = new File(GUI.getCurrentDBFile().getParent(),newDBFileName);
					if (des.toString().equalsIgnoreCase(GUI.getCurrentDBFile().toString())) return;
					try {
						autoSave();
						FileUtils.copyFile(GUI.getCurrentDBFile(),des);
						BurpExtender.getGui().LoadData(des.toString());
						FileUtils.deleteQuietly(GUI.getCurrentDBFile());
					} catch (IOException ioException) {
						ioException.printStackTrace();
					}
					lblSummary.setText(domainResult.getSummary());
				}*/
            }
        });
        HeaderPanel.add(btnRenameProject);


        JButton btnBuckupDB = new JButton("Backup DB");
        btnBuckupDB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DomainPanel.this.backupDB();
            }
        });
        HeaderPanel.add(btnBuckupDB);

        textFieldUploadURL = new JTextField();
        textFieldUploadURL.setColumns(30);
        textFieldUploadURL.setToolTipText("input upload url here");
        HeaderPanel.add(textFieldUploadURL);
        // Listen for changes in the text
        textFieldUploadURL.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                saveURL();
            }

            public void removeUpdate(DocumentEvent e) {
                saveURL();
            }

            public void insertUpdate(DocumentEvent e) {
                saveURL();
            }

            public void saveURL() {
                String url = textFieldUploadURL.getText();
                try {
                    new URL(url);
                    domainResult.uploadURL = url;
                    autoSave();
                } catch (Exception e) {

                }
            }
        });


        JButton btnUpload = new JButton("Upload");
        btnUpload.setToolTipText("upload data to Server");
        btnUpload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                SwingWorker<Boolean, Boolean> worker = new SwingWorker<Boolean, Boolean>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        String url = domainResult.uploadURL;
                        String host = new URL(url).getHost();
                        String token = ToolPanel.textFieldUploadApiToken.getText().trim();
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("Content-Type", "application/json;charset=UTF-8");
                        if (token != null && !token.equals("")) {//vmp
                            headers.put("Authorization", "Token " + token);
                        }
                        if (host.startsWith("vmp.test.shopee.") ||
                                host.contains("burpcollaborator.net")) {
                            return VMP.uploadAllVMPEntries(url, headers);
                        } else {
                            return VMP.upload(domainResult.uploadURL, headers, domainResult.ToJson());
                        }
                    }

                    @Override
                    protected void done() {
                        //Do Nothing
                    }
                };
                worker.execute();
            }
        });
        HeaderPanel.add(btnUpload);


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

        table.addMouseListener(new MouseAdapter() {
            @Override//表格中的鼠标右键菜单
            public void mouseReleased(MouseEvent e) {//在windows中触发,因为isPopupTrigger在windows中是在鼠标释放是触发的，而在mac中，是鼠标点击时触发的。
                //https://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                        //getSelectionModel().setSelectionInterval(rows[0], rows[1]);
                        int[] rows = table.getSelectedRows();
                        int col = ((JTable) e.getSource()).columnAtPoint(e.getPoint()); // 获得列位置
                        if (rows.length > 0) {
                            rows = SelectedRowsToModelRows(table.getSelectedRows());
                            new RootDomainMenu(table, rows, col).show(e.getComponent(), e.getX(), e.getY());
                        } else {//在table的空白处显示右键菜单
                            //https://stackoverflow.com/questions/8903040/right-click-mouselistener-on-whole-jtable-component
                            //new LineEntryMenu(_this).show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) { //在mac中触发
                mouseReleased(e);
            }

            public int[] SelectedRowsToModelRows(int[] SelectedRows) {
                int[] rows = SelectedRows;
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = table.convertRowIndexToModel(rows[i]);//转换为Model的索引，否则排序后索引不对应〿
                }
                Arrays.sort(rows);//升序
                return rows;
            }
        });

        domainTableModel = new DefaultTableModel(
                new Object[][][]{
                        //{"1", "1","1"},
                },
                new String[]{
                        "Root Domain", "Keyword"//,"Comment"//, "Source"
                }
        );
        table.setModel(domainTableModel);

        /*
         * 注意，所有直接对DomainObject中数据的修改，都不会触发该tableChanged监听器。
         * 除非操作的逻辑中包含了firexxxx来主动通知监听器。
         * DomainPanel.domainTableModel.fireTableChanged(null);
         */
        domainTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (listenerIsOn) {
                    domainResult.setRootDomainMap(getTableMap());
                    autoSave();
                }
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

        //第一次分割
        JSplitPane CenterSplitPane = new JSplitPane();//中间的大模块，一分为二
        CenterSplitPane.setResizeWeight(0.5);
        this.add(CenterSplitPane, BorderLayout.CENTER);


        //第二次分割，左边
        JSplitPane leftOfCenterSplitPane = new JSplitPane();//放入左边的分区。再讲左边的分区一分为二
        leftOfCenterSplitPane.setResizeWeight(0.2);
        CenterSplitPane.setLeftComponent(leftOfCenterSplitPane);

        //第二次分割，右边
        JSplitPane rightOfCenterSplitPane = new JSplitPane();//放入右半部分分区，
        rightOfCenterSplitPane.setResizeWeight(0.7);
        CenterSplitPane.setRightComponent(rightOfCenterSplitPane);

        JSplitPane split1of4 = new JSplitPane();
        split1of4.setOrientation(JSplitPane.VERTICAL_SPLIT);
        split1of4.setResizeWeight(0.7);
        leftOfCenterSplitPane.setLeftComponent(split1of4);

        JSplitPane split1of8 = new JSplitPane();
        split1of8.setResizeWeight(1.0);
        split1of8.setOrientation(JSplitPane.VERTICAL_SPLIT);
        split1of8.setLeftComponent(TargetPanel);
        split1of4.setLeftComponent(split1of8);


        JSplitPane split2of8 = new JSplitPane();
        split2of8.setOrientation(JSplitPane.VERTICAL_SPLIT);
        split2of8.setResizeWeight(0.01);
        split1of4.setRightComponent(split2of8);

        JSplitPane split2of4 = new JSplitPane();//四分之二区域的分割者
        split2of4.setResizeWeight(0.5);
        leftOfCenterSplitPane.setRightComponent(split2of4);


        JSplitPane split3of4 = new JSplitPane();//四分之三区域的分割者，
        split3of4.setResizeWeight(0.5);
        rightOfCenterSplitPane.setLeftComponent(split3of4);


        JSplitPane split4of4 = new JSplitPane();
        rightOfCenterSplitPane.setRightComponent(split4of4);


        ///////////////////////////////Target Operations and Config//////////////////////


        JPanel ControlPanel = new JPanel();
        ControlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
        split1of8.setRightComponent(ControlPanel);


        JButton addButton = new JButton("Add");
        addButton.setToolTipText("add Top-Level domain");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (domainResult == null) {
                    JOptionPane.showMessageDialog(null, "you should create project db file first");
                } else {
                    String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
                    enteredRootDomain = enteredRootDomain.trim().toLowerCase();
                    if (enteredRootDomain.startsWith("http://") || enteredRootDomain.startsWith("https://")) {
                        try {
                            URL url = new URL(enteredRootDomain);
                            enteredRootDomain = url.getHost();
                        } catch (Exception e2) {

                        }
                    }
                    enteredRootDomain = InternetDomainName.from(enteredRootDomain).topPrivateDomain().toString();
                    String keyword = enteredRootDomain.substring(0, enteredRootDomain.indexOf("."));

                    domainResult.AddToRootDomainMap(enteredRootDomain, keyword);
                    showToDomainUI();
                    autoSave();
                }
            }
        });
        ControlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        ControlPanel.add(addButton);


        JButton addButton1 = new JButton("Add+");
        addButton1.setToolTipText("add Multiple-Level domain");
        addButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (domainResult == null) {
                    JOptionPane.showMessageDialog(null, "you should create project db file first");
                } else {
                    String enteredRootDomain = JOptionPane.showInputDialog("Enter Root Domain", null);
                    enteredRootDomain = enteredRootDomain.trim().toLowerCase();
                    if (enteredRootDomain.startsWith("http://") || enteredRootDomain.startsWith("https://")) {
                        try {
                            URL url = new URL(enteredRootDomain);
                            enteredRootDomain = url.getHost();
                        } catch (Exception e2) {

                        }
                    }
                    //enteredRootDomain = InternetDomainName.from(enteredRootDomain).topPrivateDomain().toString();
                    String keyword = enteredRootDomain.substring(0, enteredRootDomain.indexOf("."));

                    domainResult.AddToRootDomainMap(enteredRootDomain, keyword);
                    showToDomainUI();
                    autoSave();
                }
            }
        });
        ControlPanel.add(addButton1);


        JButton removeButton = new JButton("Remove");
        ControlPanel.add(removeButton);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int[] rowindexs = table.getSelectedRows();
                for (int i = 0; i < rowindexs.length; i++) {
                    rowindexs[i] = table.convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
                }
                Arrays.sort(rowindexs);

                domainTableModel = (DefaultTableModel) table.getModel();
                for (int i = rowindexs.length - 1; i >= 0; i--) {
                    domainTableModel.removeRow(rowindexs[i]);
                }
                // will trigger tableModel listener---due to "fireTableRowsDeleted" in removeRow
                //domainResult.setRootDomainMap(getTableMap()); //no need any more because tableModel Listener
            }
        });

        JButton blackButton = new JButton("Black");
        ControlPanel.add(blackButton);
        blackButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int[] rowindexs = table.getSelectedRows();
                for (int i = 0; i < rowindexs.length; i++) {
                    rowindexs[i] = table.convertRowIndexToModel(rowindexs[i]);//转换为Model的索引，否则排序后索引不对应。
                }
                Arrays.sort(rowindexs);

                domainTableModel = (DefaultTableModel) table.getModel();
                for (int i = rowindexs.length - 1; i >= 0; i--) {
                    String rootdomain = (String) domainTableModel.getValueAt(rowindexs[i], 0);
                    domainTableModel.removeRow(rowindexs[i]);
                    domainResult.AddToRootDomainMap("[exclude]" + rootdomain, "");
                }
                showToDomainUI();
                autoSave();
                // will trigger tableModel listener
                //domainResult.setRootDomainMap(getTableMap()); //no need any more because tableModel Listener
            }
        });

        JButton btnFresh = new JButton("Fresh");
        ControlPanel.add(btnFresh);
        btnFresh.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //to clear sub and similar domains
                DomainConsumer.QueueToResult();
                domainResult.getEmailSet().addAll(collectEmails());
                Set<String> tmpDomains = domainResult.getSubDomainSet();
                Set<String> newSubDomainSet = new HashSet<>();
                Set<String> newSimilarDomainSet = new HashSet<String>();
                tmpDomains.addAll(domainResult.getSimilarDomainSet());

                for (String domain : tmpDomains) {
                    domain = domain.toLowerCase().trim();
                    if (domain.endsWith(".")) {
                        domain = domain.substring(0, domain.length() - 1);
                    }

                    int type = domainResult.domainType(domain);
                    if (type == DomainManager.SUB_DOMAIN || type == DomainManager.IP_ADDRESS)
                    //包含手动添加的IP
                    {
                        newSubDomainSet.add(domain);
                    } else if (type == DomainManager.SIMILAR_DOMAIN) {
                        newSimilarDomainSet.add(domain);
                    }
                }

                //相关域名中也可能包含子域名，子域名才是核心，要将它们加到子域名
                tmpDomains = domainResult.getRelatedDomainSet();
                for (String domain : tmpDomains) {
                    domain = domain.toLowerCase().trim();
                    if (domain.endsWith(".")) {
                        domain = domain.substring(0, domain.length() - 1);
                    }

                    int type = domainResult.domainType(domain);
                    if (type == DomainManager.SUB_DOMAIN) {
                        newSubDomainSet.add(domain);
                    }
                }

                domainResult.setSubDomainSet(newSubDomainSet);
                domainResult.setSimilarDomainSet(newSimilarDomainSet);

                showToDomainUI();
                autoSave();
            }
        });

		/*
		JButton btnCopy = new JButton("Copy");
		btnCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(domainResult.fetchRootDomains());
				clipboard.setContents(selection, null);
			}
		});
		btnCopy.setToolTipText("Copy Root Domains To ClipBoard");
		ControlPanel.add(btnCopy);*/


        JPanel autoControlPanel = new JPanel();
        autoControlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
        split2of8.setLeftComponent(autoControlPanel);

        rdbtnAddRelatedToRoot = new JRadioButton("Auto Add Related Domain To Root Domain");
        rdbtnAddRelatedToRoot.setVerticalAlignment(SwingConstants.TOP);
        rdbtnAddRelatedToRoot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                domainResult.autoAddRelatedToRoot = rdbtnAddRelatedToRoot.isSelected();
                if (domainResult.autoAddRelatedToRoot) {
                    domainResult.relatedToRoot();
                    showToDomainUI();
                    autoSave();
					/*
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
        autoControlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        rdbtnAddRelatedToRoot.setSelected(false);
        autoControlPanel.add(rdbtnAddRelatedToRoot);


        ///////////////////////////////textAreas///////////////////////////////////////////////////////


        JScrollPane ScrollPaneSubnets = new JScrollPane(); //1of4

        JScrollPane ScrollPaneRelatedDomains = new JScrollPane(); //2of4
        JScrollPane ScrollPaneSubdomains = new JScrollPane();
        JScrollPane ScrollPaneSimilarDomains = new JScrollPane();
        JScrollPane ScrollPaneEmails = new JScrollPane();
        JScrollPane ScrollPanePackageNames = new JScrollPane();


        split2of8.setRightComponent(ScrollPaneSubnets);

        split2of4.setLeftComponent(ScrollPaneRelatedDomains);
        split2of4.setRightComponent(ScrollPaneSubdomains);

        split3of4.setLeftComponent(ScrollPaneSimilarDomains);
        split3of4.setRightComponent(ScrollPaneEmails);

        split4of4.setLeftComponent(ScrollPanePackageNames);
        split4of4.setRightComponent(null);//通过设置为空来隐藏它

        textAreaSubnets = new JTextArea();
        textAreaRelatedDomains = new JTextArea();
        textAreaSubdomains = new JTextArea();
        textAreaSimilarDomains = new JTextArea();
        textAreaEmails = new JTextArea();
        textAreaPackages = new JTextArea();

        textAreaSubnets.setColumns(10);
        textAreaRelatedDomains.setColumns(10);
        textAreaSubdomains.setColumns(10);
        textAreaSimilarDomains.setColumns(10);
        textAreaEmails.setColumns(10);
        textAreaPackages.setColumns(10);

        textAreaSubnets.setToolTipText("Subnets/IP for certain");
        textAreaRelatedDomains.setToolTipText("Related Domains");
        textAreaSubdomains.setToolTipText("Sub Domains");
        textAreaSimilarDomains.setToolTipText("Similar Domains");
        textAreaEmails.setToolTipText("Emails");
        textAreaPackages.setToolTipText("Package Names");

        ScrollPaneSubnets.setViewportView(textAreaSubnets);
        ScrollPaneRelatedDomains.setViewportView(textAreaRelatedDomains);
        ScrollPaneSubdomains.setViewportView(textAreaSubdomains);
        ScrollPaneSimilarDomains.setViewportView(textAreaSimilarDomains);
        ScrollPaneEmails.setViewportView(textAreaEmails);
        ScrollPanePackageNames.setViewportView(textAreaPackages);

        //实现编辑后自动保存
        textAreaSubnets.getDocument().addDocumentListener(new textAreaListener());
        textAreaRelatedDomains.getDocument().addDocumentListener(new textAreaListener());
        textAreaSubdomains.getDocument().addDocumentListener(new textAreaListener());
        textAreaSimilarDomains.getDocument().addDocumentListener(new textAreaListener());
        textAreaEmails.getDocument().addDocumentListener(new textAreaListener());
        textAreaPackages.getDocument().addDocumentListener(new textAreaListener());

        textAreaSubnets.addMouseListener(new TextAreaMouseListener(textAreaSubnets));
        textAreaRelatedDomains.addMouseListener(new TextAreaMouseListener(textAreaRelatedDomains));
        textAreaSubdomains.addMouseListener(new TextAreaMouseListener(textAreaSubdomains));
        textAreaSimilarDomains.addMouseListener(new TextAreaMouseListener(textAreaSimilarDomains));
        textAreaEmails.addMouseListener(new TextAreaMouseListener(textAreaEmails));
        textAreaPackages.addMouseListener(new TextAreaMouseListener(textAreaPackages));


        ///////////////////////////FooterPanel//////////////////


        JPanel footerPanel = new JPanel();
        FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
        fl_FooterPanel.setAlignment(FlowLayout.LEFT);
        this.add(footerPanel, BorderLayout.SOUTH);

        lblNewLabel_2 = new JLabel(BurpExtender.getExtenderName() + "    " + BurpExtender.getGithub());
        lblNewLabel_2.setFont(new Font("宋体", Font.BOLD, 12));
        lblNewLabel_2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    URI uri = new URI(BurpExtender.getGithub());
                    Desktop desktop = Desktop.getDesktop();
                    if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
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
        footerPanel.add(lblNewLabel_2);

        lblSummary = new JLabel("      ^_^");
        footerPanel.add(lblSummary);
        lblSummary.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Commons.OpenFolder(GUI.getCurrentDBFile().getParent());
                } catch (Exception e2) {
                    e2.printStackTrace(stderr);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                lblSummary.setForeground(Color.RED);
                lblSummary.setToolTipText(GUI.getCurrentDBFile().toString());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lblSummary.setForeground(Color.BLACK);
            }
        });


        //搜索域名，但是效果不怎么好
        //		JTextField textFieldSearch = new JTextField("");
        //		textFieldSearch.addFocusListener(new FocusAdapter() {
        //			@Override
        //			public void focusGained(FocusEvent e) {
        //				if (textFieldSearch.getText().equals("Input text to search")) {
        //					textFieldSearch.setText("");
        //				}
        //			}
        //			@Override
        //			public void focusLost(FocusEvent e) {
        //				/*
        //				 * if (textFieldSearch.getText().equals("")) {
        //				 * textFieldSearch.setText("Input text to search"); }
        //				 */
        //
        //			}
        //		});
        //
        //		textFieldSearch.addActionListener(new ActionListener() {
        //			public void actionPerformed(ActionEvent e) {
        //				String keyword = textFieldSearch.getText().trim();
        //				domainPanelSearch(keyword);
        //			}
        //
        //			public void domainPanelSearch(String keyword) {
        //				try {
        //					Highlighter h = textAreaSubdomains.getHighlighter();
        //					h.removeAllHighlights();
        //					int pos = textAreaSubdomains.getText().indexOf(keyword, 0);
        //					h.addHighlight(pos ,
        //					               pos  + keyword.length(),
        //					               DefaultHighlighter.DefaultPainter);
        //					textAreaRelatedDomains = new JTextArea();
        //					textAreaSubdomains = new JTextArea();
        //					textAreaSimilarDomains = new JTextArea();
        //					textAreaEmails = new JTextArea();
        //					textAreaPackages = new JTextArea();
        //				} catch (BadLocationException e) {
        //					e.printStackTrace(stderr);
        //				}
        //			}
        //		});
        //
        //		textFieldSearch.setColumns(30);
        //		footerPanel.add(textFieldSearch);
    }

    public void showToDomainUI() {

        listenerIsOn = false;
        domainResult.relatedToRoot();
        ClearTable();

        for (Map.Entry<String, String> entry : domainResult.getRootDomainMap().entrySet()) {
            domainTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        textFieldUploadURL.setText(domainResult.uploadURL);
        textAreaSubnets.setText(domainResult.fetchSubnets());
        textAreaSubdomains.setText(domainResult.fetchSubDomains());
        textAreaSimilarDomains.setText(domainResult.fetchSimilarDomains());
        textAreaRelatedDomains.setText(domainResult.fetchRelatedDomains());
        textAreaEmails.setText(domainResult.fetchEmails());
        textAreaPackages.setText(domainResult.fetchPackageNames());
        lblSummary.setText(domainResult.getSummary());
        rdbtnAddRelatedToRoot.setSelected(domainResult.autoAddRelatedToRoot);

        System.out.println("Load Domain Panel Data Done, " + domainResult.getSummary());
        stdout.println("Load Domain Panel Data Done, " + domainResult.getSummary());
        listenerIsOn = true;

    }


    //////////////////////////////methods//////////////////////////////////////
	/*
    执行完成后，就已将数据保存到了domainResult
	 */
    public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords) {
        IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
        IHttpRequestResponse[] messages = callbacks.getSiteMap(null);

        List<IHttpRequestResponse> AllMessages = new ArrayList<IHttpRequestResponse>();
        AllMessages.addAll(Arrays.asList(messages));
        AllMessages.addAll(collectPackageNameMessages());//包含错误回显的请求响应消息

        new ThreadSearhDomain(AllMessages).Do();
        //DomainPanel.autoSave();//每次搜索完成都应该进行一次保存,放在Do()函数中实现
        return null;
    }

    public void ZoneTransferCheckAll() {
        for (String rootDomain : domainResult.fetchRootDomainSet()) {
            Set<String> NS = Commons.GetAuthoritativeNameServer(rootDomain);
            for (String Server : NS) {
                //stdout.println("checking [Server: "+Server+" Domain: "+rootDomain+"]");
                List<String> Records = Commons.ZoneTransferCheck(rootDomain, Server);
                if (Records.size() > 0) {
                    try {
                        //stdout.println("!!! "+Server+" is zoneTransfer vulnerable for domain "+rootDomain+" !");
                        File file = new File(Server + "-ZoneTransfer-" + Commons.getNowTimeString() + ".txt");
                        file.createNewFile();
                        FileUtils.writeLines(file, Records);
                        stdout.println("!!! Records saved to " + file.getAbsolutePath());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public static Set<String> collectEmails() {
        Set<String> Emails = new HashSet<>();
        IScanIssue[] issues = BurpExtender.getCallbacks().getScanIssues(null);

        final String REGEX_EMAIL = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";
        Pattern pDomainNameOnly = Pattern.compile(REGEX_EMAIL);

        for (IScanIssue issue : issues) {
            if (issue.getIssueName().equalsIgnoreCase("Email addresses disclosed")) {
                String detail = issue.getIssueDetail();
                Matcher matcher = pDomainNameOnly.matcher(detail);
                while (matcher.find()) {//多次查找
                    String email = matcher.group();
                    if (domainResult.isRelatedEmail(email)) {
                        Emails.add(matcher.group());
                    }
                    System.out.println(matcher.group());
                }
            }
        }
        return Emails;
    }


    public static Set<IHttpRequestResponse> collectPackageNameMessages() {
        Set<IHttpRequestResponse> PackageNameMessages = new HashSet<>();
        IScanIssue[] issues = BurpExtender.getCallbacks().getScanIssues(null);
        for (IScanIssue issue : issues) {
            if (issue.getIssueName().toLowerCase().contains("error")) {
                //Detailed Error Messages Revealed  ---generated by other burp extender
                PackageNameMessages.addAll(Arrays.asList(issue.getHttpMessages()));
            }
        }
        return PackageNameMessages;
    }


    public Map<String, Set<String>> crawl(Set<String> rootdomains, Set<String> keywords) {
        int i = 0;
        while (i <= 2) {
            for (String rootdomain : rootdomains) {
                if (!rootdomain.contains(".") || rootdomain.endsWith(".") || rootdomain.equals("")) {
                    //如果域名为空，或者（不包含.号，或者点号在末尾的）
                } else {
                    IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
                    IHttpRequestResponse[] items = callbacks.getSiteMap(null); //null to return entire sitemap
                    //int len = items.length;
                    //stdout.println("item number: "+len);
                    Set<URL> NeedToCrawl = new HashSet<URL>();
                    for (IHttpRequestResponse x : items) {// 经过验证每次都需要从头开始遍历，按一定offset获取的数据每次都可能不同

                        IHttpService httpservice = x.getHttpService();
                        String shortUrlString = httpservice.toString();
                        String Host = httpservice.getHost();

                        try {
                            URL shortUrl = new URL(shortUrlString);

                            if (Host.endsWith("." + rootdomain) && Commons.isResponseNull(x)) {
                                // to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
                                NeedToCrawl.add(shortUrl);
                                // to reduce memory usage, use shortUrl. base on my test, spider will crawl entire site when send short URL to it.
                                // this may miss some single page, but the single page often useless for domain collection
                                // see spideralltest() function.
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace(stderr);
                        }
                    }


                    for (URL shortUrl : NeedToCrawl) {
                        if (!callbacks.isInScope(shortUrl)) { //reduce add scope action, to reduce the burp UI action.
                            callbacks.includeInScope(shortUrl);//if not, will always show confirm message box.
                        }
                        callbacks.sendToSpider(shortUrl);
                    }
                }
            }


            try {
                Thread.sleep(5 * 60 * 1000);//单位毫秒，60000毫秒=一分钟
                stdout.println("sleep 5 minutes to wait spider");
                //to wait spider
            } catch (InterruptedException e) {
                e.printStackTrace(stdout);
            }
            i++;
        }

        return search(rootdomains, keywords);
    }

    /*
    仅用于root domain数据表发生更改时获取表中数据。
     */
    public LinkedHashMap<String, String> getTableMap() {
        LinkedHashMap<String, String> tableMap = new LinkedHashMap<String, String>();

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

    public void ClearTable() {
        LinkedHashMap<String, String> tmp = domainResult.getRootDomainMap();

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        //this also trigger tableModel listener. lead to rootDomainMap to empty!!
        //so need to backup rootDomainMap and restore!
        domainResult.setRootDomainMap(tmp);
    }

    /*
    单独保存域名信息到另外的文件
     */
    public File saveDomainOnly() {
        try {
            File file = BurpExtender.getGui().dbfc.dialog(false);
            if (file != null) {
                DBHelper dbHelper = new DBHelper(file.toString());
                if (dbHelper.saveDomainObject(domainResult)) {
                    stdout.println("Save Domain Only Success! " + Commons.getNowTimeString());
                    return file;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(stderr);
        }
        stdout.println("Save Domain Only failed! " + Commons.getNowTimeString());
        return null;
    }

    /*
    自动保存，根据currentDBFile，如果currentDBFile为空或者不存在，就提示选择文件。
     */
    public static void autoSave() {
        File file = GUI.getCurrentDBFile();
        if (file == null) {
            if (null == DomainPanel.getDomainResult()) return;//有数据才弹对话框指定文件位置。
            file = BurpExtender.getGui().dbfc.dialog(false);
            GUI.setCurrentDBFile(file);
        }
        if (file != null) {
            DBHelper dbHelper = new DBHelper(file.toString());
            boolean success = dbHelper.saveDomainObject(domainResult);
            log.info("domain data saved");
        }
    }

    /*
    //用于各种domain的手动编辑后的保存（不包含rootdomain）
     */
    class textAreaListener implements DocumentListener {

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (listenerIsOn) {
                saveTextAreas();
                autoSave();
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (listenerIsOn) {
                saveTextAreas();
                autoSave();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            if (listenerIsOn) {
                saveTextAreas();
                autoSave();
            }
        }
    }

    public void saveTextAreas() {

        HashSet<String> oldSubdomains = new HashSet<String>();
        oldSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

        domainResult.setSubnetSet(getSetFromTextArea(textAreaSubnets));
        domainResult.setRelatedDomainSet(getSetFromTextArea(textAreaRelatedDomains));
        domainResult.setSubDomainSet(getSetFromTextArea(textAreaSubdomains));
        domainResult.setSimilarDomainSet(getSetFromTextArea(textAreaSimilarDomains));
        domainResult.setEmailSet(getSetFromTextArea(textAreaEmails));
        domainResult.setPackageNameSet(getSetFromTextArea(textAreaPackages));
        domainResult.getSummary();

        //用于存储新增的域名到一个临时集合
        HashSet<String> newSubdomains = new HashSet<String>();
        newSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

        newSubdomains.removeAll(oldSubdomains);
        DomainPanel.getDomainResult().getNewAndNotGetTitleDomainSet().addAll(newSubdomains);
        stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~", newSubdomains.size()));
        stdout.println(String.join(System.lineSeparator(), newSubdomains));
    }

    public static Set<String> getSetFromTextArea(JTextArea textarea) {
        //user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
        Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n")));
        domainList.remove("");
        return domainList;
    }

    public static void backupDB() {
        File file = GUI.getCurrentDBFile();
        if (file == null) return;
        File bakfile = new File(file.getAbsoluteFile().toString() + ".bak" + Commons.getNowTimeString());
        try {
            FileUtils.copyFile(file, bakfile);
            BurpExtender.getStdout().println("DB File Backed Up:" + bakfile.getAbsolutePath());
        } catch (IOException e1) {
            e1.printStackTrace(BurpExtender.getStderr());
        }
    }
}
