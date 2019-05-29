package burp;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;
import test.HTTPPost;

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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.*;
/*
以domainResult为核心的数据修改、数据保存和数据展示
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
    private SortOrder sortedMethod;
    private JTable table;
    private JTextArea textAreaRelatedDomains;
    private boolean listenerIsOn = true;

    public static DomainObject getDomainResult() {
        return domainResult;
    }

    public static void setDomainResult(DomainObject domainResult) {
        DomainPanel.domainResult = domainResult;
    }

    protected static DomainObject domainResult = null;//getter setter
    protected static DefaultTableModel domainTableModel;
    PrintWriter stdout;
    PrintWriter stderr;

    public DomainPanel() {//构造函数
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setLayout(new BorderLayout(0, 0));

        try{
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        }catch (Exception e){
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }


        ///////////////////////HeaderPanel//////////////


        JPanel HeaderPanel = new JPanel();
        FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
        fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
        this.add(HeaderPanel, BorderLayout.NORTH);


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

        Component verticalStrut = Box.createVerticalStrut(20);
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
                        Set<String> rootDomains = domainResult.fetchRootDomainSet();
                        Set<String> keywords= domainResult.fetchKeywordSet();

                        btnCrawl.setEnabled(false);
                        return crawl(rootDomains,keywords);

                    }
                    @Override
                    protected void done() {
                        try {
                            get();
                            showToDomainUI();
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

        JButton btnImportDomain = new JButton("Import Domain");
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
                        showToDomainUI();//保存配置并更新图形显示
                        stdout.println("Import domains finished from "+ file.getName());
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
                if (domainResult !=null){
                    String newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
                    while(newProjectName.trim().equals("")){
                        newProjectName = JOptionPane.showInputDialog("New Project Name", null).trim();
                    }
                    domainResult.setProjectName(newProjectName);
                    autoSave();
                    lblSummary.setText(domainResult.getSummary());
                }
            }
        });
        HeaderPanel.add(btnRenameProject);

        Component verticalStrut_1 = Box.createVerticalStrut(20);
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


        JButton btnUpload = new JButton("Upload");
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
                        //Do Nothing
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
                if (listenerIsOn){
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

        JSplitPane CenterSplitPane = new JSplitPane();
        CenterSplitPane.setResizeWeight(0.5);
        this.add(CenterSplitPane, BorderLayout.CENTER);


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


        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
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
                showToDomainUI();
                //将会触发listener，然后自动保存。无需主动调用了。
            }
        });
        panel.add(addButton);


        JButton removeButton = new JButton("Remove");
        panel.add(removeButton);
        removeButton.addActionListener(new ActionListener() {
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
                    int type = domainResult.domainType(domain);
                    if (type == DomainObject.SUB_DOMAIN)
                    {
                        newSubDomainSet.add(domain);
                    }else if (type == DomainObject.SIMILAR_DOMAIN) {
                        newSimilarDomainSet.add(domain);
                    }
                }

                domainResult.setSubDomainSet(newSubDomainSet);
                domainResult.setSimilarDomainSet(newSimilarDomainSet);

                showToDomainUI();
                autoSave();
            }
        });

        JButton btnCopy = new JButton("Copy");
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
                if (domainResult.autoAddRelatedToRoot) {
                    domainResult.relatedToRoot();
                    showToDomainUI();/*
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

        //实现编辑后自动保存
        textAreaRelatedDomains.getDocument().addDocumentListener(new textAreaListener());
        textAreaSubdomains.getDocument().addDocumentListener(new textAreaListener());
        textAreaSimilarDomains.getDocument().addDocumentListener(new textAreaListener());


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


        JPanel footerPanel = new JPanel();
        FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
        fl_FooterPanel.setAlignment(FlowLayout.LEFT);
        this.add(footerPanel, BorderLayout.SOUTH);

        lblNewLabel_2 = new JLabel(BurpExtender.getExtenderName()+"    "+BurpExtender.getGithub());
        lblNewLabel_2.setFont(new Font("宋体", Font.BOLD, 12));
        lblNewLabel_2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    URI uri = new URI(BurpExtender.getGithub());
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
        footerPanel.add(lblNewLabel_2);

    }

    public void showToDomainUI() {

        listenerIsOn = false;
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
        listenerIsOn = true;
    }


    //////////////////////////////methods//////////////////////////////////////
    /*
    执行完成后，就已将数据保存到了domainResult
     */
    public Map<String, Set<String>> search(Set<String> rootdomains, Set<String> keywords){
        IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
        IHttpRequestResponse[] messages = callbacks.getSiteMap(null);
        new ThreadSearhDomain(Arrays.asList(messages)).Do();
        return null;
    }


    public Map<String, Set<String>> crawl (Set<String> rootdomains, Set<String> keywords) {
        int i = 0;
        while(i<=2) {
            for (String rootdomain: rootdomains) {
                if (!rootdomain.contains(".")||rootdomain.endsWith(".")||rootdomain.equals("")){
                    //如果域名为空，或者（不包含.号，或者点号在末尾的）
                }
                else {
                    IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();
                    IHttpRequestResponse[] items = callbacks.getSiteMap(null); //null to return entire sitemap
                    //int len = items.length;
                    //stdout.println("item number: "+len);
                    Set<URL> NeedToCrawl = new HashSet<URL>();
                    for (IHttpRequestResponse x:items){// 经过验证每次都需要从头开始遍历，按一定offset获取的数据每次都可能不同

                        IHttpService httpservice = x.getHttpService();
                        String shortUrlString = httpservice.toString();
                        String Host = httpservice.getHost();

                        try {
                            URL shortUrl = new URL(shortUrlString);

                            if (Host.endsWith("."+rootdomain) && Commons.isResponseNull(x)) {
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


                    for (URL shortUrl:NeedToCrawl) {
                        if (!callbacks.isInScope(shortUrl)) { //reduce add scope action, to reduce the burp UI action.
                            callbacks.includeInScope(shortUrl);//if not, will always show confirm message box.
                        }
                        callbacks.sendToSpider(shortUrl);
                    }
                }
            }


            try {
                Thread.sleep(5*60*1000);//单位毫秒，60000毫秒=一分钟
                stdout.println("sleep 5 minutes to wait spider");
                //to wait spider
            } catch (InterruptedException e) {
                e.printStackTrace(stdout);
            }
            i++;
        }

        return search(rootdomains,keywords);
    }

    /*
    仅用于root domain数据表发生更改时获取表中数据。
     */
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

    public void ClearTable() {
        LinkedHashMap<String, String> tmp = domainResult.getRootDomainMap();

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        //this also trigger tableModel listener. lead to rootDomainMap to empty!!
        //so need to backup rootDomainMap and restore!
        domainResult.setRootDomainMap(tmp);
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

    /*
    单独保存域名信息到另外的文件
     */
    public File saveDomainOnly() {
        try {
            File file = BurpExtender.getGui().dbfc.dialog(false);
            if (file != null){
                DBHelper dbHelper = new DBHelper(file.toString());
                if (dbHelper.saveDomainObject(domainResult)){
                    stdout.println("Save Domain Only Success! "+ Commons.getNowTimeString());
                    return file;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(stderr);
        }
        stdout.println("Save Domain Only failed! "+ Commons.getNowTimeString());
        return null;
    }

    /*
    自动保存，根据currentDBFile，如果currentDBFile为空或者不存在，就提示选择文件。
     */
    public void autoSave(){
        File file = BurpExtender.getGui().getCurrentDBFile();
        if (file == null){
            file = BurpExtender.getGui().dbfc.dialog(false);
            GUI.setCurrentDBFile(file);
        }
        DBHelper dbHelper = new DBHelper(file.toString());
        boolean success = dbHelper.saveDomainObject(domainResult);
    }

    /*
    //用于各种domain的手动编辑后的保存（不包含rootdomain）
     */
    class textAreaListener implements DocumentListener {

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (listenerIsOn){
                saveTextAreas();
                autoSave();
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (listenerIsOn){
                saveTextAreas();
                autoSave();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            if (listenerIsOn){
                saveTextAreas();
                autoSave();
            }
        }
    }

    public void saveTextAreas(){
        domainResult.setRelatedDomainSet(getSetFromTextArea(textAreaRelatedDomains));
        domainResult.setSubDomainSet(getSetFromTextArea(textAreaSubdomains));
        domainResult.setSimilarDomainSet(getSetFromTextArea(textAreaSimilarDomains));
        domainResult.getSummary();
    }

    public static Set<String> getSetFromTextArea(JTextArea textarea){
        //user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
        Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll("\r\n", "\n").split("\n")));
        domainList.remove("");
        return domainList;
    }
}
