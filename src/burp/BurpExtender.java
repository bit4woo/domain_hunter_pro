package burp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;


public class BurpExtender implements IBurpExtender, IHttpListener, ITab, IContextMenuFactory
{
    public IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    private PrintWriter stdout;//现在这里定义变量，再在registerExtenderCallbacks函数中实例化，如果都在函数中就只是局部变量，不能在这实例化，因为要用到其他参数。
    private PrintWriter stderr;
    private String ExtenderName = "Domain Hunter v0.6 by bit4";
    private String github = "https://github.com/bit4woo/domain_hunter";
    private Set<String> subdomainofset = new HashSet<String>();
    private Set<String> domainlikeset = new HashSet<String>();
    private Set<String> relatedDomainSet = new HashSet<String>();
    private Set<URL> url_spidered_set = new HashSet<URL>();
	private JPanel contentPane;
	private JTextField textFieldSubdomains;
	private JTextField textFieldDomainsLike;
	private JLabel lblSubDomainsOf;
	private JButton btnSearch;
	private JPanel panel_2;
	private JLabel lblNewLabel_2;
	private JSplitPane splitPane;
	private Component verticalStrut;
	private JTextArea textArea;
	private JTextArea textArea_1;
	private JButton btnNewButton;
	private JTextArea textArea_2;
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	stderr = new PrintWriter(callbacks.getStderr(), true);
    	stdout.println(ExtenderName);
    	stdout.println(github);
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName(ExtenderName); //插件名称
        callbacks.registerHttpListener(this); //如果没有注册，下面的processHttpMessage方法是不会生效的。处理请求和响应包的插件，这个应该是必要的
        callbacks.registerContextMenuFactory(this);
        addMenuTab();
        
    }

	@Override
	public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
		// TODO Auto-generated method stub
		
	}
	public Map search(String subdomainof, String domainlike){
			subdomainofset.clear();
			domainlikeset.clear();
			relatedDomainSet.clear();
			Set<String> httpsURLs = new HashSet<String>();
		   IHttpRequestResponse[] requestResponses = callbacks.getSiteMap(null);
		    //stdout.println(response[1]);
		    for (IHttpRequestResponse x:requestResponses){
		    	
		    	IHttpService httpservice = x.getHttpService();
		    	String shortURL = httpservice.toString();
				String Host = httpservice.getHost();
				
				//stdout.println(url);
				//stdout.println(Host);

				if (!subdomainof.contains(".")||subdomainof.endsWith(".")){
					//如果域名为空，或者（不包含.号，或者点号在末尾的）
				}
				else if (Host.endsWith("."+subdomainof)){
					subdomainofset.add(Host);
					//stdout.println(subdomainofset);
					
					//get SANs info to get related domain, only when the [subdomain] is using https.
					if(httpservice.getProtocol().equalsIgnoreCase("https")) {
							httpsURLs.add(shortURL);
					}
				}
				
				
				else if (domainlike.equals("")){
					
				}
				else if (Host.contains(domainlike) && !Host.equalsIgnoreCase(subdomainof)){
					domainlikeset.add(Host);
					//stdout.println(domainlikeset);
				}
		    }
		    
		    stdout.println("sub-domains and similar-domains search finished\n");
		    
		    //多线程获取
		    //Set<Future<Set<String>>> set = new HashSet<Future<Set<String>>>();
	    	Map<String,Future<Set<String>>> urlResultmap = new HashMap<String,Future<Set<String>>>();
	        ExecutorService pool = Executors.newFixedThreadPool(10);
	        
	        for (String url:httpsURLs) {
	          Callable<Set<String>> callable = new ThreadCertInfo(url);
	          Future<Set<String>> future = pool.submit(callable);
	          //set.add(future);
	          urlResultmap.put(url, future);
	        }
	        
	        Set<String> tmpRelatedDomainSet = new HashSet<String>();
	        for(String url:urlResultmap.keySet()) {
	        	Future<Set<String>> future = urlResultmap.get(url);
	        //for (Future<Set<String>> future : set) {
	          try {
	        	  stdout.println("founded related-domains :"+future.get() +" from "+url);
	        	  if (future.get()!=null) {
	        		  tmpRelatedDomainSet.addAll(future.get());
	        	  }
	        	  
			} catch (Exception e) {
				//e.printStackTrace(stderr);
				stderr.println(e.getMessage());
	        }
	        }
		    
	        /* 单线程获取方式
		    Set<String> tmpRelatedDomainSet = new HashSet<String>();
		    //begin get related domains
		    for(String url:httpsURLs) {
		    	try {
		    		tmpRelatedDomainSet.addAll(CertInfo.getSANs(url));
				}catch(UnknownHostException e) {
					stderr.println("UnknownHost "+ url);
					continue;
				}catch(ConnectException e) {
					stderr.println("Connect Failed "+ url);
					continue;
				}
		    	catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace(stderr);
					continue;
				}
		    }
		    */
		    
		    //对 SANs的结果再做一次分类。
		    for (String item:tmpRelatedDomainSet) {
		    	if (item.endsWith("."+subdomainof)){
					subdomainofset.add(item);
				}else if (item.contains(domainlike) && !item.equalsIgnoreCase(subdomainof)){
					domainlikeset.add(item);
				}else {
					relatedDomainSet.add(item);
				}
		    }
		    
		    
		    Map result = new HashMap();
		    result.put("subdomainofset", subdomainofset);
		    result.put("domainlikeset", domainlikeset);
		    result.put("relatedDomainSet", relatedDomainSet);
		    return result;
		    
	}
 
	public Map spiderall (String subdomainof, String domainlike) {
		url_spidered_set.clear();
	    
	    int i = 0;
	    int url_number = 0;
	    int item_len = 0;
	    while(i<=2) {
	    	IHttpRequestResponse[] items = callbacks.getSiteMap(null); //null to return entire sitemap
	    	int len = items.length;
	    	//stdout.println("item number: "+len);
	    	
		    for (IHttpRequestResponse x:items){// 经过验证每次都需要从头开始遍历，按一定offset获取的数据每次都可能不同
		    	IRequestInfo  analyzeRequest = helpers.analyzeRequest(x); //前面的操作可能已经修改了请求包，所以做后续的更改前，都需要从新获取。
				URL url = analyzeRequest.getUrl();
				String Host = url.getHost();
				String url_path = url.getPath();


				if (Host.endsWith("."+subdomainof) && !url_spidered_set.contains(url)) {					
					if(!uselessExtension(url_path)) {//exclude useless file extension request.
						callbacks.includeInScope(url);//if not, will always show confirm message box.
						callbacks.sendToSpider(url);
						url_spidered_set.add(url);
						//stdout.println(url+" has been added to spider");
					}
				}
			}
		    
			try {
				Thread.sleep(5*60000);//单位毫秒，60000毫秒=一分钟
				stdout.println("sleep 1 min");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				stdout.print(e);
			}
		    
		    int size = url_spidered_set.size();
		    if(size==url_number) {
		    	stdout.println("spider finished");
		    	break; //break while loop,when no new url to spider.
		    }
		    url_number = size;
	    	i++;
		    }

	    return search(subdomainof,domainlike);
	    //search(subdomainof,domainlike);
	    }
	
	
	public String set2string(Set set){
	    Iterator iter = set.iterator();
	    String result = "";
	    while(iter.hasNext())
	    {
	        //System.out.println(iter.next());  	
	    	result +=iter.next();
	    	result +="\n";
	    }
	    return result;
	}
	
	public Boolean uselessExtension(String urlpath) {
		Set extendset = new HashSet();
		extendset.add(".gif");
		extendset.add(".jpg");
		extendset.add(".png");
		extendset.add(".css");
		Iterator iter = extendset.iterator();
		while (iter.hasNext()) {
			if(urlpath.endsWith(iter.next().toString())) {//if no next(), this loop will not break out
				return true;
			}
		}
		return false;
	}
		
	public void CGUI() {
			contentPane =  new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setLayout(new BorderLayout(0, 0));
	
			
			JPanel panel = new JPanel();
			FlowLayout flowLayout_1 = (FlowLayout) panel.getLayout();
			flowLayout_1.setAlignment(FlowLayout.LEFT);
			contentPane.add(panel, BorderLayout.NORTH);
			
			lblSubDomainsOf = new JLabel("SubDomains of  ");
			panel.add(lblSubDomainsOf);
			
			textFieldSubdomains = new JTextField();
			textFieldSubdomains.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					String domain = textFieldSubdomains.getText();
					textFieldDomainsLike.setText(domain.substring(0,domain.lastIndexOf(".")));
				}
			});
			panel.add(textFieldSubdomains);
			textFieldSubdomains.setColumns(20);
			
			verticalStrut = Box.createVerticalStrut(20);
			panel.add(verticalStrut);
			
			JLabel lblDomainsLike = new JLabel("Domains like ");
			panel.add(lblDomainsLike);
			
			textFieldDomainsLike = new JTextField();
			panel.add(textFieldDomainsLike);
			textFieldDomainsLike.setColumns(20);
			
			btnSearch = new JButton("search");
			btnSearch.setToolTipText("Do a single search from site map");
			btnSearch.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				    	//using SwingWorker to prevent blocking burp main UI.

				        @Override
				        protected Map doInBackground() throws Exception {                
							String subdomain = textFieldSubdomains.getText();
							String domainlike = textFieldDomainsLike.getText();
							btnSearch.setEnabled(false);
							return search(subdomain,domainlike);
				        }
				        @Override
				        protected void done() {
				            try {
					        	Map result = get();
					        	subdomainofset = (Set) result.get("subdomainofset"); //之前的set变成了object
					        	domainlikeset = (Set) result.get("domainlikeset");
					        	relatedDomainSet = (Set) result.get("relatedDomainSet");
								textArea.setText(set2string(subdomainofset));
								textArea_1.setText(set2string(domainlikeset));
								textArea_2.setText(set2string(relatedDomainSet));
								btnSearch.setEnabled(true);
				            } catch (Exception e) {
				            	btnSearch.setEnabled(true);
				                e.printStackTrace(stderr);
				            }
				        }
				    };      
				    worker.execute();
					
				}
			});
			panel.add(btnSearch);
			
			btnNewButton = new JButton("Spider all");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					
				    SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				    	//可以在一个类中实现另一个类，直接实现原始类，没有变量处理的困扰；
				    	//之前的想法是先单独实现一个worker类，在它里面处理各种，就多了一层实现，然后在这里调用，变量调用会是一个大问题。
				    	//https://stackoverflow.com/questions/19708646/how-to-update-swing-ui-while-actionlistener-is-in-progress
				        @Override
				        protected Map doInBackground() throws Exception {                
							String subdomain = textFieldSubdomains.getText();
							String domainlike = textFieldDomainsLike.getText();
							//stdout.println(subdomain);
							//stdout.println(domainlike);
							btnNewButton.setEnabled(false);
							return spiderall(subdomain,domainlike);
						
				        }
				        @Override
				        protected void done() {
				            try {
					        	Map result = get();
					        	subdomainofset = (Set) result.get("subdomainofset"); //之前的set变成了object
					        	domainlikeset = (Set) result.get("domainlikeset");
								textArea.setText(set2string(subdomainofset));
								textArea_1.setText(set2string(domainlikeset));
								btnNewButton.setEnabled(true);
				            } catch (Exception e) {
				                e.printStackTrace();
				            }
				        }
				    };      
				    worker.execute();
				}
			});
			btnNewButton.setToolTipText("Spider all subdomains recursively,This may take a long time!!!");
			panel.add(btnNewButton);
			
			splitPane = new JSplitPane();
			splitPane.setDividerLocation(0.5);
			contentPane.add(splitPane, BorderLayout.WEST);
			
			textArea = new JTextArea();
			textArea.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					final JPopupMenu jp = new JPopupMenu();
			        jp.add("^_^");
			        textArea.addMouseListener(new MouseAdapter() {
			            @Override
			            public void mouseClicked(MouseEvent e) {
			                if (e.getButton() == MouseEvent.BUTTON3) {
			                    // 弹出菜单
			                    jp.show(textArea, e.getX(), e.getY());
			                }
			            }
			        });
				}
			});
			textArea.setColumns(30);
			splitPane.setLeftComponent(textArea);
			
			textArea_1 = new JTextArea();
			textArea_1.setColumns(30);
			splitPane.setRightComponent(textArea_1);
			
			JSplitPane splitPane_1 = new JSplitPane();
			splitPane.setDividerLocation(0.5);
			contentPane.add(splitPane_1, BorderLayout.EAST);
			
			textArea_2 = new JTextArea();
			textArea_2.setColumns(30);
			splitPane_1.setLeftComponent(textArea_2);
			
			panel_2 = new JPanel();
			FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
			flowLayout.setAlignment(FlowLayout.LEFT);
			contentPane.add(panel_2, BorderLayout.SOUTH);
			
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
						// TODO: handle exception
						BurpExtender.this.callbacks.printError(e2.getMessage());
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
			panel_2.add(lblNewLabel_2);
	}
		
//以下是各种burp必须的方法 --start
    
    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          BurpExtender.this.CGUI();
          BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this); //这里的BurpExtender.this实质是指ITab对象，也就是getUiComponent()中的contentPane.这个参数由CGUI()函数初始化。
          //如果这里报java.lang.NullPointerException: Component cannot be null 错误，需要排查contentPane的初始化是否正确。
        }
      });
    }
    
    
    
    //ITab必须实现的两个方法
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return ("Domain Hunter");
	}
	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return this.contentPane;
	}
	//ITab必须实现的两个方法

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	//IContextMenuFactory 必须实现的方法
	//各种burp必须的方法 --end
}