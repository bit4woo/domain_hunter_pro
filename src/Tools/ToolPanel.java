package Tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.GrepUtils;
import burp.IPAddressUtils;
import config.ConfigPanel;
import domain.CertInfo;
import title.WebIcon;

/**
 * 所有配置的修改，界面的操作，都立即写入LineConfig对象，如有必要保存到磁盘，再调用一次SaveConfig函数，思路要清晰
 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
 * 保存： 具体各个控件的值---->LineConfig对象---->磁盘文件
 */

public class ToolPanel extends JPanel {

    private JLabel lblNewLabel_2;

    PrintWriter stdout;
    PrintWriter stderr;
    public static DraggableTextArea inputTextArea;
    public static JTextArea outputTextArea;

    public boolean inputTextAreaChanged = true;

    String history = "";

    private GUIMain guiMain;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    JFrame frame = new JFrame();
                    frame.setVisible(true);
                    frame.setContentPane(new ToolPanel(null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public ToolPanel(GUIMain guiMain) {
        this.guiMain = guiMain;
        setForeground(Color.DARK_GRAY);//构造函数
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


        JPanel HeaderPanel = new JPanel();
        HeaderPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
        FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
        fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
        this.add(HeaderPanel, BorderLayout.NORTH);

        JLabel lblNewLabelNull = new JLabel("  ");
        HeaderPanel.add(lblNewLabelNull);

        JButton outputToInput = new JButton("Input<----Output");
        HeaderPanel.add(outputToInput);
        outputToInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = outputTextArea.getText();
                if (null != content) {
                    inputTextArea.setText(content);
                }
            }
        });

        //第一次分割，中间的大模块一分为二
        JSplitPane CenterSplitPane = new JSplitPane();//中间的大模块，一分为二
        CenterSplitPane.setResizeWeight(0.5);
        this.add(CenterSplitPane, BorderLayout.CENTER);

        //第二次分割：二分为四
        JSplitPane LeftOfCenter = new JSplitPane();
        LeftOfCenter.setResizeWeight(0.5);
        CenterSplitPane.setLeftComponent(LeftOfCenter);

        JSplitPane RightOfCenter = new JSplitPane();
        RightOfCenter.setOrientation(JSplitPane.VERTICAL_SPLIT);
        RightOfCenter.setResizeWeight(1);
        CenterSplitPane.setRightComponent(RightOfCenter);

        //  1/4
        JScrollPane oneFourthPanel = new JScrollPane();
        LeftOfCenter.setLeftComponent(oneFourthPanel);
        //  2/4
        JScrollPane twoFourthPanel = new JScrollPane();
        LeftOfCenter.setRightComponent(twoFourthPanel);

        inputTextArea = new DraggableTextArea();
        inputTextArea.setColumns(20);
        inputTextArea.setLineWrap(true);
        inputTextArea.getDocument().addDocumentListener(new textAreaListener());
        inputTextArea.addMouseListener(new TextAreaMouseListener(inputTextArea));
        oneFourthPanel.setViewportView(inputTextArea);

        Border blackline = BorderFactory.createLineBorder(Color.black);

        JLabel lblNewLabel_1 = new JLabel("Input");
        lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
        lblNewLabel_1.setBorder(blackline);
        oneFourthPanel.setColumnHeaderView(lblNewLabel_1);

        outputTextArea = new JTextArea();
        outputTextArea.setLineWrap(true);
        outputTextArea.addMouseListener(new TextAreaMouseListener(outputTextArea));
        twoFourthPanel.setViewportView(outputTextArea);

        JLabel lblNewLabel_3 = new JLabel("Output");
        lblNewLabel_3.setHorizontalAlignment(SwingConstants.CENTER);
        lblNewLabel_3.setBorder(blackline);
        twoFourthPanel.setColumnHeaderView(lblNewLabel_3);


        JButton btnFindDomains = new JButton("Find Domains");

        btnFindDomains.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = inputTextArea.getText();
                if (null != content) {
                    Set<String> domains = GrepUtils.grepDomain(content);
                    ArrayList<String> tmpList = new ArrayList<String>(domains);
                    Collections.sort(tmpList, new DomainComparator());
                    outputTextArea.setText(String.join(System.lineSeparator(), tmpList));
                    guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
                }
            }
        });

        JButton btnFindUrls = new JButton("Find URLs");
        btnFindUrls.setToolTipText("only find entire URL");

        btnFindUrls.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = inputTextArea.getText();
                if (null != content) {
                    List<String> urls = GrepUtils.grepURL(content);
                    outputTextArea.setText(String.join(System.lineSeparator(), urls));
                }
            }
        });

        JButton btnFindIP = new JButton("Find IP");

        btnFindIP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = inputTextArea.getText();
                if (null != content) {
                    List<String> iplist = GrepUtils.grepIP(content);
                    outputTextArea.setText(String.join(System.lineSeparator(), iplist));
                }
            }
        });

        JButton btnFindIPAndPort = new JButton("Find IP:Port");

        btnFindIPAndPort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = inputTextArea.getText();
                if (null != content) {
                    List<String> iplist = GrepUtils.grepIPAndPort(content);
                    outputTextArea.setText(String.join(System.lineSeparator(), iplist));
                }
            }
        });

        JButton btnFindSubnet = new JButton("Find Subnet");

        btnFindSubnet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = inputTextArea.getText();
                if (null != content) {
                    List<String> subnets = GrepUtils.grepSubnet(content);
                    outputTextArea.setText(String.join(System.lineSeparator(), subnets));
                }
            }
        });

        JButton btnFindEmail = new JButton("Find Email");

        btnFindEmail.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = inputTextArea.getText();
                if (null != content) {
                    Set<String> emails = GrepUtils.grepEmail(content);
                    outputTextArea.setText(String.join(System.lineSeparator(), emails));
                }
            }
        });


        JButton btnOpenurls = new JButton("OpenURLs");

        btnOpenurls.addActionListener(new ActionListener() {
            List<String> urls = new ArrayList<>();
            Iterator<String> it = urls.iterator();
            private int totalNumber;
            private int left;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (inputTextAreaChanged) {//default is true
                    urls = Arrays.asList(guiMain.getConfigPanel().getLineConfig().getToolPanelText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n"));
                    totalNumber = urls.size();
                    left = urls.size();
                    it = urls.iterator();
                    inputTextAreaChanged = false;
                }
                try {
                    int i = 10;
                    while (i > 0 && it.hasNext()) {
                        String url = it.next();
                        if (!url.toLowerCase().startsWith("https://") &&
                                !url.toLowerCase().startsWith("http://")) {
                            url = "http://" + url;
                            URL tmpUrl = new URL(url);
                            if (tmpUrl.getPort() == -1) {
                                Commons.browserOpen(url, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
                                Commons.browserOpen(url.replaceFirst("http://", "https://"), guiMain.getConfigPanel().getLineConfig().getBrowserPath());
                            } else if (Integer.toString(tmpUrl.getPort()).endsWith("443")) {
                                Commons.browserOpen(url.replaceFirst("http://", "https://"), guiMain.getConfigPanel().getLineConfig().getBrowserPath());
                            } else {
                                Commons.browserOpen(url, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
                            }
                        } else {
                            Commons.browserOpen(url, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
                        }
                        i--;
                        left--;
                    }
                    btnOpenurls.setText("OpenURLs" + "(" + left + "/" + totalNumber + ")");
                } catch (Exception e1) {
                    e1.printStackTrace(stderr);
                }
            }

        });

        JButton btnCertDomains = new JButton("GetCertDomains");
        btnCertDomains.setToolTipText("get Alter Domains of Cert");

        btnCertDomains.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.
                    @Override
                    protected Map doInBackground() throws Exception {
                        ArrayList<String> result = new ArrayList<String>();
                        List<String> urls = Arrays.asList(guiMain.getConfigPanel().getLineConfig().getToolPanelText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n"));
                        Iterator<String> it = urls.iterator();
                        while (it.hasNext()) {
                            String url = it.next();
                            Set<String> domains = CertInfo.getAlternativeDomains(url);
                            result.add(url + " " + domains.toString());
                            System.out.println(url + " " + domains.toString());
                        }
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnCertDomains.setEnabled(true);
                        stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
                    }
                };
                worker.execute();
            }
        });

        JButton btnCertTime = new JButton("GetCertTime");
        btnCertTime.setToolTipText("get out-of-service time of Cert");

        btnCertTime.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.
                    @Override
                    protected Map doInBackground() throws Exception {
                        ArrayList<String> result = new ArrayList<String>();
                        List<String> urls = Arrays.asList(guiMain.getConfigPanel().getLineConfig().getToolPanelText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n"));
                        Iterator<String> it = urls.iterator();
                        while (it.hasNext()) {
                            String url = it.next();
                            String time = CertInfo.getCertTime(url);
                            result.add(url + " " + time);
                            System.out.println(url + " " + time);
                        }
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnCertTime.setEnabled(true);
                        stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
                    }
                };
                worker.execute();
            }
        });

        JButton btnCertIssuer = new JButton("GetCertIssuer");
        btnCertIssuer.setToolTipText("get issuer of Cert");

        btnCertIssuer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.
                    @Override
                    protected Map doInBackground() throws Exception {
                        ArrayList<String> result = new ArrayList<String>();
                        List<String> urls = Arrays.asList(guiMain.getConfigPanel().getLineConfig().getToolPanelText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n"));
                        Iterator<String> it = urls.iterator();
                        while (it.hasNext()) {
                            String url = it.next();
                            String time = CertInfo.getCertIssuer(url);
                            result.add(url + " " + time);
                            System.out.println(url + " " + time);
                        }
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnCertIssuer.setEnabled(true);
                        stdout.println("~~~~~~~~~~~~~Search Done~~~~~~~~~~~~~");
                    }
                };
                worker.execute();
            }
        });

        JButton iconHashButton = new JButton("GetIconHash");

        iconHashButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> result = new ArrayList<String>();

                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.
                    @Override
                    protected Map doInBackground() throws Exception {
                        try {
                            List<String> urls = Arrays.asList(guiMain.getConfigPanel().getLineConfig().getToolPanelText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n"));
                            Iterator<String> it = urls.iterator();
                            while (it.hasNext()) {
                                String url = it.next();
                                String hash = WebIcon.getHash(url);
                                result.add(hash);
                                System.out.println(url + " " + hash);
                            }
                            outputTextArea.setText(String.join(System.lineSeparator(), result));
                        } catch (Exception e1) {
                            outputTextArea.setText(e1.getMessage());
                            e1.printStackTrace(stderr);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                    }
                };
                worker.execute();
            }
        });

        JButton getIPAddressButton = new JButton("GetIPAddress");

        getIPAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> result = new ArrayList<String>();

                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.
                    @Override
                    protected Map doInBackground() throws Exception {
                        try {
                            List<String> domains = Arrays.asList(guiMain.getConfigPanel().getLineConfig().getToolPanelText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n"));
                            Iterator<String> it = domains.iterator();
                            while (it.hasNext()) {
                                String domain = it.next();
                                if (IPAddressUtils.isValidIP(domain)) {//目标是一个IP
                                    result.add(domain);
                                } else if (DomainNameUtils.isValidDomain(domain)) {//目标是域名
                                    HashMap<String, Set<String>> temp = DomainNameUtils.dnsquery(domain);
                                    Set<String> IPSet = temp.get("IP");
                                    result.addAll(IPSet);
                                }
                            }
                            outputTextArea.setText(String.join(System.lineSeparator(), result));
                        } catch (Exception e1) {
                            outputTextArea.setText(e1.getMessage());
                            e1.printStackTrace(stderr);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                    }
                };
                worker.execute();
            }
        });

        JButton rows2List = new JButton("Rows To List");

        rows2List.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                    outputTextArea.setText(content.toString());
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                }
            }

        });

        JButton rows2Array = new JButton("Rows To Array");

        rows2Array.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                    for (int i = 0; i < content.size(); i++) {
                        content.set(i, "\"" + content.get(i) + "\"");
                    }

                    outputTextArea.setText(String.join(",", content));
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                }
            }

        });

        JButton removeDuplicate = new JButton("Remove Duplicate");

        removeDuplicate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                    Set<String> contentSet = new HashSet<>(content);
                    List<String> tmplist = new ArrayList<>(contentSet);

                    Collections.sort(tmplist);
                    String output = String.join(System.lineSeparator(), tmplist);
                    outputTextArea.setText(output);
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                }
            }
        });

        JButton sortByLength = new JButton("Sort by Length");

        sortByLength.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                    Set<String> contentSet = new HashSet<>(content);
                    List<String> tmplist = new ArrayList<>(contentSet);

                    Collections.sort(tmplist, new LengthComparator());
                    String output = String.join(System.lineSeparator(), tmplist);
                    outputTextArea.setText(output);
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                }
            }
        });


        JButton btnGrep = new JButton("Grep Json");

        btnGrep.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String content = inputTextArea.getText();
                    String toFind = JOptionPane.showInputDialog("to find which value", history);
                    if (toFind == null) {
                        return;
                    } else {
                        history = toFind;
                        //stdout.println(content);
                        ArrayList<String> result = JSONHandler.grepValueFromJson(content, toFind);
                        //								stdout.println("##################Result of Grep JSON##################");
                        //								stdout.println(result.toString());
                        //								stdout.println("##################Result of Grep JSON##################");
                        //								stdout.println();

                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                    }

                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    //e1.printStackTrace(stderr);
                }
            }

        });

        JButton btnLine = new JButton("Grep Line");

        btnLine.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    String toFind = JOptionPane.showInputDialog("to find which value", history);
                    ArrayList<String> result = new ArrayList<String>();
                    if (toFind == null) {
                        return;
                    } else {
                        history = toFind;
                        List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                        for (String item : content) {
                            if (item.toLowerCase().contains(toFind.toLowerCase().trim())) {
                                result.add(item);
                            }
                        }
                        //outputTextArea.setText(result.toString());
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                    }

                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    //e1.printStackTrace(stderr);
                }
            }
        });

        JButton btnRegexGrep = new JButton("Regex Grep");

        btnRegexGrep.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String toFind = JOptionPane.showInputDialog("Input Regex", history);

                    if (toFind == null) {
                        return;
                    } else {
                        history = toFind;
                        ArrayList<String> result = new ArrayList<String>();
                        String PATTERN = toFind;
                        Pattern pRegex = Pattern.compile(PATTERN);
                        String content = inputTextArea.getText();
                        Matcher matcher = pRegex.matcher(content);
                        while (matcher.find()) {//多次查找
                            result.add(matcher.group());
                        }
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                    }
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });

        JButton btnAddPrefix = new JButton("Add Prefix/Suffix");

        btnAddPrefix.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String toAddPrefix = JOptionPane.showInputDialog("prefix to add", null);
                    String toAddSuffix = JOptionPane.showInputDialog("suffix to add", null);
                    ArrayList<String> result = new ArrayList<String>();
                    if (toAddPrefix == null && toAddSuffix == null) {
                        return;
                    } else {
                        if (toAddPrefix == null) {
                            toAddPrefix = "";
                        }

                        if (toAddSuffix == null) {
                            toAddSuffix = "";
                        }

                        List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                        for (String item : content) {
                            item = toAddPrefix + item + toAddSuffix;
                            result.add(item);
                        }
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                    }
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                }
            }
        });

        JButton btnRemovePrefix = new JButton("Remove Prefix/Suffix");

        btnRemovePrefix.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String Prefix = JOptionPane.showInputDialog("prefix to remove", null);
                    String Suffix = JOptionPane.showInputDialog("suffix to remove", null);
                    List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                    List<String> result = Commons.removePrefixAndSuffix(content, Prefix, Suffix);
                    outputTextArea.setText(String.join(System.lineSeparator(), result));
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });


        JButton btnReplace = new JButton("ReplaceFirstStr");

        btnReplace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String replace = JOptionPane.showInputDialog("string (from)", null);
                    String to = JOptionPane.showInputDialog("replacement (to)", null);
                    ArrayList<String> result = new ArrayList<String>();
                    if (replace == null && to == null) {
                        return;
                    } else {
                        if (replace == null) {
                            replace = "";
                        }

                        if (to == null) {
                            to = "";
                        }

                        replace = Pattern.quote(replace);
                        List<String> content = Commons.getLinesFromTextArea(inputTextArea);
                        for (String item : content) {
                            item = item.replaceFirst(replace, to);
                            result.add(item);
                        }
                        outputTextArea.setText(String.join(System.lineSeparator(), result));
                    }
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });


        JButton btnIPsToCIDR = new JButton("IPs To CIDR");

        btnIPsToCIDR.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> IPs = Commons.getLinesFromTextArea(inputTextArea);
                    Set<String> subnets = IPAddressUtils.toSmallerSubNets(new HashSet<String>(IPs));

                    List<String> tmplist = new ArrayList<>(subnets);//排序
                    Collections.sort(tmplist);

                    outputTextArea.setText(String.join(System.lineSeparator(), tmplist));
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });

        JButton btnCIDRToIPs = new JButton("CIDR To IPs");

        btnCIDRToIPs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> subnets = Commons.getLinesFromTextArea(inputTextArea);
                    List<String> IPs = IPAddressUtils.toIPList(subnets);// 当前所有title结果计算出的IP集合
                    outputTextArea.setText(String.join(System.lineSeparator(), IPs));
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });

        JButton unescapeJava = new JButton("UnescapeJava");

        unescapeJava.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    outputTextArea.setText(StringEscapeUtils.unescapeJava(inputTextArea.getText()));
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });

        JButton unescapeHTML = new JButton("UnescapeHTML");

        unescapeHTML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    outputTextArea.setText(StringEscapeUtils.unescapeHtml4(inputTextArea.getText()));
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }
        });

        JButton Base64ToFile = new JButton("Base64ToFile");

        Base64ToFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    byte[] payloadBytes = Base64.getDecoder().decode(inputTextArea.getText());
                    File downloadFile = saveDialog();
                    if (downloadFile != null) {
                        FileUtils.writeByteArrayToFile(downloadFile, payloadBytes);
                    }
                } catch (Exception e1) {
                    outputTextArea.setText(e1.getMessage());
                    e1.printStackTrace(stderr);
                }
            }

            public File saveDialog() {
                try {
                    JFileChooser fc = new JFileChooser();
                    if (fc.getCurrentDirectory() != null) {
                        fc = new JFileChooser(fc.getCurrentDirectory());
                    } else {
                        fc = new JFileChooser();
                    }

                    fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

                    int action = fc.showSaveDialog(null);

                    if (action == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        return file;
                    }
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        JButton splitButton = new JButton("Split");

        splitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String separator = JOptionPane.showInputDialog("input separator", null);
                if (separator != null) {//&& !separator.trim().equals("")有可能本来就是空格分割，所以不能有这个条件
                    String text = inputTextArea.getText();
                    String[] items = text.split(separator);
                    outputTextArea.setText(String.join(System.lineSeparator(), items));
                }
            }
        });

        JButton combineButton = new JButton("Combine");

        combineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String separator = JOptionPane.showInputDialog("input connect char", null);
                if (separator != null) {// && !separator.trim().equals("")
                    List<String> items = Commons.getLinesFromTextArea(inputTextArea);
                    outputTextArea.setText(String.join(separator, items));
                }
            }
        });

        JButton toLowerCaseButton = new JButton("toLowerCase");

        toLowerCaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputTextArea.setText(inputTextArea.getText().toLowerCase());
            }
        });


        JButton testButton = new JButton("test");

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
                    //using SwingWorker to prevent blocking burp main UI.
                    @Override
                    protected Map doInBackground() throws Exception {
                        try {
                            outputTextArea.setText(WebIcon.getHash(inputTextArea.getText()));
                        } catch (Exception e1) {
                            outputTextArea.setText(e1.getMessage());
                            e1.printStackTrace(stderr);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                    }
                };
                worker.execute();
            }
        });


        //四分之三部分放一个panel，里面放操作按钮
        JPanel threeFourthPanel = new JPanel();
        RightOfCenter.setLeftComponent(threeFourthPanel);
        threeFourthPanel.setLayout(new GridLayout(10, 1));
        //https://stackoverflow.com/questions/5709690/how-do-i-make-this-flowlayout-wrap-within-its-jsplitpane
        threeFourthPanel.setMinimumSize(new Dimension(0, 0));//为了让button自动换行

        GridBagLayout gbl_threeFourthPanel = new GridBagLayout();
        threeFourthPanel.setLayout(gbl_threeFourthPanel);

        //查找提取类
        threeFourthPanel.add(btnFindDomains, new bagLayout(1, 1));
        threeFourthPanel.add(btnFindUrls, new bagLayout(1, 2));
        threeFourthPanel.add(btnFindIP, new bagLayout(1, 3));
        threeFourthPanel.add(btnFindIPAndPort, new bagLayout(1, 4));
        threeFourthPanel.add(btnFindSubnet, new bagLayout(1, 5));
        threeFourthPanel.add(btnFindEmail, new bagLayout(2, 1));


        threeFourthPanel.add(btnGrep, new bagLayout(5, 1));
        threeFourthPanel.add(btnLine, new bagLayout(5, 2));
        threeFourthPanel.add(btnRegexGrep, new bagLayout(5, 3));


        //网络请求类
        threeFourthPanel.add(btnOpenurls, new bagLayout(15, 1));
        threeFourthPanel.add(btnCertDomains, new bagLayout(16, 1));
        threeFourthPanel.add(btnCertTime, new bagLayout(16, 2));
        threeFourthPanel.add(btnCertIssuer, new bagLayout(16, 3));
        threeFourthPanel.add(iconHashButton, new bagLayout(16, 4));
        threeFourthPanel.add(getIPAddressButton, new bagLayout(16, 5));


        //数据转换类
        threeFourthPanel.add(rows2List, new bagLayout(20, 1));
        threeFourthPanel.add(rows2Array, new bagLayout(20, 2));
        threeFourthPanel.add(btnIPsToCIDR, new bagLayout(20, 3));
        threeFourthPanel.add(btnCIDRToIPs, new bagLayout(20, 4));


        threeFourthPanel.add(removeDuplicate, new bagLayout(25, 1));
        threeFourthPanel.add(sortByLength, new bagLayout(25, 2));
        threeFourthPanel.add(btnAddPrefix, new bagLayout(25, 3));
        threeFourthPanel.add(btnRemovePrefix, new bagLayout(25, 4));
        threeFourthPanel.add(btnReplace, new bagLayout(25, 5));


        threeFourthPanel.add(unescapeJava, new bagLayout(30, 1));
        threeFourthPanel.add(unescapeHTML, new bagLayout(30, 2));


        threeFourthPanel.add(toLowerCaseButton, new bagLayout(35, 1));
        threeFourthPanel.add(splitButton, new bagLayout(35, 2));
        threeFourthPanel.add(combineButton, new bagLayout(35, 3));
        threeFourthPanel.add(Base64ToFile, new bagLayout(35, 4));
        threeFourthPanel.add(testButton, new bagLayout(35, 5));


        ///////////////////////////FooterPanel//////////////////


        JPanel footerPanel = new JPanel();
        footerPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
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

    }

    public static Set<String> getSetFromTextArea(JTextArea textarea) {
        //user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
        Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n")));
        domainList.remove("");
        return domainList;
    }

    public static String getContentFromFile(String filename) {
        File tmpfile = new File(filename);
        if (tmpfile.exists() && tmpfile.isFile()) {
            try {
                return FileUtils.readFileToString(tmpfile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    //保存文本框的数据
    class textAreaListener implements DocumentListener {

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (ConfigPanel.listenerIsOn) {
                guiMain.getConfigPanel().getLineConfig().setToolPanelText(inputTextArea.getText());
                inputTextAreaChanged = true;
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (ConfigPanel.listenerIsOn) {
                guiMain.getConfigPanel().getLineConfig().setToolPanelText(inputTextArea.getText());
                inputTextAreaChanged = true;
            }
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            if (ConfigPanel.listenerIsOn) {
                guiMain.getConfigPanel().getLineConfig().setToolPanelText(inputTextArea.getText());
                inputTextAreaChanged = true;
            }
        }
    }


    class bagLayout extends GridBagConstraints {
        /**
         * 采用普通的行列计数，从1开始
         *
         * @param row
         * @param column
         */
        bagLayout(int row, int column) {
            this.fill = GridBagConstraints.BOTH;
            this.insets = new Insets(0, 0, 5, 5);
            this.gridx = column - 1;
            this.gridy = row - 1;
        }
    }


}
