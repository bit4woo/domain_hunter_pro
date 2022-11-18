package domain;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.Commons;
import burp.GrepUtils;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import config.ConfigPanel;
import title.LineEntry;
import toElastic.ElasticClient;

public class DomainProducer extends Thread {//Producer do
	private final BlockingQueue<IHttpRequestResponse> inputQueue;//use to store messageInfo


	private int threadNo;
	private volatile boolean stopflag = false;
	private volatile boolean currentSaved = false;//每分钟只保存一次的标志位

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private GUIMain guiMain;

	public DomainProducer(GUIMain gui,BlockingQueue<IHttpRequestResponse> inputQueue,
						  int threadNo) {
		this.guiMain = gui;
		this.threadNo = threadNo;
		this.inputQueue = inputQueue;
		this.setName(this.getClass().getName()+threadNo);//方便调试
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		DomainPanel DomainPanel = guiMain.getDomainPanel();
		int times = 5;
		while(true){
			try {
				if (threadNo == 9999){//9999是流量进程，除非关闭，否则一直不退出。
					if (DomainPanel.getDomainResult() == null ) {//当未加载项目时，暂时不处理
						if (times>0){
							stdout.println("No project loaded,traffic anlaysis thread will do nothing!");
							times--;
						}
						Thread.sleep(1*60*1000);
						continue;
					}

					//每两分钟保存一次
					if (Commons.getNowMinute()%2==0 ){
						if (!currentSaved && DomainPanel.getDomainResult().isChanged()){
							currentSaved = true;
							DomainPanel.saveDomainDataToDB();
						}
					}else {
						currentSaved = false;
					}

				}else {
					if (inputQueue.isEmpty()) {
						stdout.println(this.getName()+" break due to input queue empty!");
						break;
					}
					if (stopflag) {
						stdout.println(this.getName()+" break due to stop flag changed to true");
					}
				}

				IHttpRequestResponse messageinfo = inputQueue.take();

				IHttpService httpservice = messageinfo.getHttpService();
				String urlString = helpers.analyzeRequest(messageinfo).getUrl().toString();

				String shortURL = httpservice.toString();
				String protocol =  httpservice.getProtocol();
				String Host = httpservice.getHost();
				int port = httpservice.getPort();
				if (port !=80 && port!=443) {
					Host = Host+port;
				}

				//第一阶段：处理Host
				//当Host是一个IP地址时，它也有可能是我们的目标。如果它的证书域名又在目标中，那么它就是目标。
				int type = DomainPanel.fetchTargetModel().assetType(Host);

				if (type ==DomainManager.USELESS){
					continue;
				}else if (type == DomainManager.NEED_CONFIRM_IP){
					//当Host是一个IP，也有可能是目标，通过证书信息进一步判断。
					if (protocol.equalsIgnoreCase("https") && messageinfo.getResponse()!=null && !DomainPanel.getDomainResult().getIPSetOfCert().contains(Host)){
						if (isTargetByCertInfoForTarget(shortURL)){

							//确定这个IP是目标了，更新target
							//TargetEntry entry = new TargetEntry(Host);
							//entry.setComment("BaseOnCertInfo");
							//DomainPanel.fetchTargetModel().addRowIfValid(entry);

							//重新判断类型，应该是确定的IP类型了。
							//type = DomainPanel.fetchTargetModel().domainType(Host);
							DomainPanel.getDomainResult().getIPSetOfCert().add(Host);
						}
					}
				}else {
					DomainPanel.getDomainResult().addIfValid(Host);
				}

				//第二步：处理HTTPS证书
				if (type !=DomainManager.USELESS && protocol.equalsIgnoreCase("https")){//get related domains
					if (guiMain.getHttpsChecked().add(shortURL)) {//httpService checked or not
						//如果set中已存在，返回false，如果不存在，返回true。
						//必须先添加，否则执行在执行https链接的过程中，已经有很多请求通过检测进行相同的请求了。
						Set<String> tmpDomains = CertInfo.getSANsbyKeyword(shortURL,DomainPanel.fetchTargetModel().fetchKeywordSet());
						for (String domain:tmpDomains) {
							BurpExtender.getStdout().println("Target Related Asset Found :"+domain);
							if (DomainPanel.getDomainResult().isAutoAddRelatedToRoot()){
								DomainPanel.getDomainResult().addToTargetAndSubDomain(domain, true);
							}else{
								DomainPanel.getDomainResult().getRelatedDomainSet().add(domain);
							}
						}
					}
				}

				//第三步：对所有流量都进行抓取，这样可以发现更多域名，但同时也会有很多无用功，尤其是使用者同时挖掘多个目标的时候
				if (!Commons.uselessExtension(urlString)) {//grep domains from response and classify
					byte[] response = messageinfo.getResponse();

					if (response != null) {
						if (response.length >= 100000000) {//避免大数据包卡死整个程序
							response = subByte(response,0,100000000);
						}
						Set<String> domains = GrepUtils.grepDomain(new String(response));
						//List<String> IPs = DomainProducer.grepIPAndPort(new String(response));
						Set<String> emails = GrepUtils.grepEmail(new String(response));

						DomainPanel.getDomainResult().addIfValid(domains);
						//DomainPanel.getDomainResult().addIfValid(new HashSet<>(IPs));
						DomainPanel.getDomainResult().addIfValidEmail(emails);
					}
				}

				if (ConfigPanel.rdbtnSaveTrafficTo.isSelected()) {
					if (type != DomainManager.USELESS && !Commons.uselessExtension(urlString)) {//grep domains from response and classify
						if (threadNo == 9999) {
							try {//写入elastic的逻辑，只对目标资产生效
								LineEntry entry = new LineEntry(messageinfo);
								ElasticClient.writeData(entry);
							}catch(Exception e1) {
								e1.printStackTrace(BurpExtender.getStderr());
								e1.getMessage();
							}
						}
					}
				}
			} catch (InterruptedException error) {
				BurpExtender.getStdout().println(this.getName() +" exits due to Interrupt signal received");
			}catch (Exception error) {
				error.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	/**
	 * 这个函数必须返回确定的目标！不能确定的认为是false
	 * @param shortURL
	 * @return
	 */
	public boolean isTargetByCertInfoForTarget(String shortURL) throws Exception {
		Set<String> certDomains = CertInfo.getAllSANs(shortURL);
		for (String domain : certDomains) {
			int type = guiMain.getDomainPanel().fetchTargetModel().assetType(domain);
			if (type == DomainManager.SUB_DOMAIN || type == DomainManager.TLD_DOMAIN) {
				return true;
			}
		}
		return false;
	}

	public byte[] subByte(byte[] b,int srcPos,int length){
		byte[] b1 = new byte[length];
		System.arraycopy(b, srcPos, b1, 0, length);
		return b1;
	}
}