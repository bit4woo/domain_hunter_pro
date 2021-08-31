package GUI;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JOptionPane;

import burp.BurpExtender;
import burp.Commons;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import domain.DomainPanel;
import title.LineEntry;
import title.TitlePanel;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadRunner{
	public static final int ChangeService = 4;
	public static final int ChangeHostInService = 3;
	public static final int ChangeHostInHeader = 2;
	public static final int ChangeCancel = 1;
	public static final int ChangeHelp =0;
	public static final int ChangeClose = -1;
	
	private IHttpRequestResponse messageInfo;
	private List<RunnerProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private RunnerGUI runnerGUI;

	public ThreadRunner(RunnerGUI runnerGUI, IHttpRequestResponse messageInfo) {
		this.runnerGUI = runnerGUI;
		this.messageInfo = messageInfo;
	}

	@Deprecated
	public String getKeywordFromUI() {
		String responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
		while(responseKeyword.trim().equals("")){
			responseKeyword = JOptionPane.showInputDialog("Response Keyword", null);
		}
		responseKeyword = responseKeyword.trim();
		return responseKeyword;
	}
	
	/**
	 * 情况一：只修改host，那么protocol、port都不变，适合查找相同服务的IP、Name
	 * 情况二：修改httpService，那么protocol、host、port修改，适合验证cookie\token对于站点的有效性!
	 * 情况三：目标httpService不变，只修改Header中的host字段，用于检测Nginx或者gateway的防护弱点。
	 * @return Help=0 CANCEL=1 ....,即数组的index。
	 */
	private static int fetchChangeType() {
		Object[] options = { "Help","CANCEL","Host In Header","Host Of HttpService","HttpService"};
		int user_input = JOptionPane.showOptionDialog(null, "Which Part Do You Want To Repalce?", "Chose Replace Part",
		JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
		null, options, options[0]);
		if (user_input ==0) {
			try {
				Commons.browserOpen("https://github.com/bit4woo/domain_hunter_pro/blob/master/Help.md", null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			user_input = fetchChangeType();
		}
		return user_input;
	}

	public void Do(){
		runnerGUI.lblStatus.setText("running");
		BlockingQueue<LineEntry> lineEntryQueue = new LinkedBlockingQueue<LineEntry>();//use to store domains
		BlockingQueue<String> domainQueue = new LinkedBlockingQueue<String>();
		lineEntryQueue.addAll(TitlePanel.getTitleTableModel().getLineEntries().values());
		domainQueue.addAll(DomainPanel.getDomainResult().getSubDomainSet());
		stdout.println("~~~~~~~~~~~~~Start threading Runner~~~~~~~~~~~~~ total task number: "+lineEntryQueue.size());
		int changeType = fetchChangeType();
		if (changeType == ChangeCancel || changeType == ChangeClose ){//用户选了cancel（2）或者点击了关闭（-1）
			return;
		}
		plist = new ArrayList<RunnerProducer>();

		for (int i=0;i<=50;i++) {
			RunnerProducer p = new RunnerProducer(runnerGUI.getRunnerTableModel(),lineEntryQueue,domainQueue,messageInfo,changeType, i);
			p.start();
			plist.add(p);
		}

		long waitTime = 0;
		while(true) {//to wait all threads exit.
			if (lineEntryQueue.isEmpty() && isAllProductorFinished()) {
				stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
				break;
			}else if(lineEntryQueue.isEmpty() && waitTime >=10*60*1000){
				stdout.println("~~~~~~~~~~~~~Get Title Done(force exits due to time out)~~~~~~~~~~~~~");
				break;
			}else {
				try {
					Thread.sleep(60*1000);//1分钟
					waitTime =waitTime+60*1000;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;//unnecessary
			}
		}
		runnerGUI.lblStatus.setText("finished");
		return;
	}

	boolean isAllProductorFinished(){
		int i = 0;
		for (RunnerProducer p:plist) {
			if(p.isAlive()) {
				i = i+1;
			}
		}
		if (i>0){
			stdout.println( "~~~~~~~~~~~~~"+i +" productors are still alive~~~~~~~~~~~~~");
			return false;
		}else{
			stdout.println( "~~~~~~~~~~~~~All productor threads exited ~~~~~~~~~~~~~");
			return true;
		}
	}

	public void stopThreads() {
		if (plist != null) {
			for (RunnerProducer p:plist) {
				p.stopThread();
			}
			stdout.println("~~~~~~~~~~~~~All stop message sent! wait them to exit~~~~~~~~~~~~~");
		}
	}

	public static void main(String[] args){
		System.out.println(fetchChangeType());
	}
}

