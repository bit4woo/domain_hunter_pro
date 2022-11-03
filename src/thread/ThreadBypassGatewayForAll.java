package thread;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import GUI.GUIMain;
import GUI.RunnerGUI;
import burp.BurpExtender;
import burp.Commons;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import domain.DomainPanel;
import title.LineEntry;
import title.TitlePanel;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
/**
 * 对请求包做各种变化，然后请求，类似Intruder的功能。
 * @author bit4woo
 *
 */
public class ThreadBypassGatewayForAll extends Thread{
	private List<GatewayBypassChecker> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private RunnerGUI runnerGUI;

	public ThreadBypassGatewayForAll(RunnerGUI runnerGUI) {
		this.runnerGUI = runnerGUI;
	}

	public static HashSet<String> getDomainsForBypassCheck(){
		HashSet<String> tmpSet = new HashSet<String>();//所有子域名列表
		tmpSet.addAll(GUIMain.instance.getDomainPanel().getDomainResult().getSubDomainSet());

		HashSet<String> unreachableSet = new HashSet<String>();
		Collection<LineEntry> entries = TitlePanel.getTitleTableModel().getLineEntries().values();
		for (LineEntry entry:entries) {
			if (entry.getEntryType().equals(LineEntry.EntryType_DNS) || entry.getStatuscode() == 403) {
				unreachableSet.add(entry.getHost());
			}
			tmpSet.remove(entry.getHost());
		}//删除了title中成功请求的域名

		tmpSet.addAll(unreachableSet);//添加了请求失败、只有解析、状态403的域名

		//		for (String item:tmpSet) {//移除IP，这步骤是否需要？
		//			if (Commons.isValidDomain(item)) {
		//				resultSet.add(item);
		//			}
		//		}
		//		
		//		return resultSet;
		return tmpSet;
	}

	@Override
	public void run(){
		runnerGUI.lblStatus.setText("running");
		BlockingQueue<String> InputQueue = new LinkedBlockingQueue<String>(500);//设置容量大小，避免导致大量内存占用
		HashSet<String> IPURLs = TitlePanel.getTitleTableModel().getIPURLs();
		HashSet<String> Domains = getDomainsForBypassCheck();

		GatewayBypassProducer gproducer = new GatewayBypassProducer(IPURLs,Domains,InputQueue);
		gproducer.setDaemon(true);//必须在关闭窗口后退出，否则依然写入数据，占用内存
		gproducer.start();

		plist = new ArrayList<GatewayBypassChecker>();

		for (int i=0;i<=100;i++) {
			GatewayBypassChecker p = new GatewayBypassChecker(runnerGUI.getRunnerTableModel(),InputQueue,i);
			p.setDaemon(true);//将子线程设置为守护线程，会随着主线程的结束而立即结束
			//当腰结束整个runner时，结束主线程即可，即调用当前类的.interrupt();
			p.start();
			plist.add(p);
		}


		try {
			for (GatewayBypassChecker p:plist) {
				p.join();
			}
			//让主线程等待各个子线程执行完成，才会结束。
			//https://www.cnblogs.com/zheaven/p/12054044.html
		} catch (InterruptedException e) {
			//当执行主线程的Interrupt方法（当前类的实例），会被这里catch，主线程就会退出，所有子线程也将退出。
			stdout.println("force stop received");
			System.out.println(this.getClass().getName()+"received force stop action");
			//e.printStackTrace();
		}

		stdout.println("all producer threads finished");
		runnerGUI.lblStatus.setText("finished");
		return;
	}
	
	public void forceStopThreads() {
		this.interrupt();//将子线程都设置为守护线程，会随着主线程的结束而立即结束,与setDaemon(true)结合
		stdout.println("~~~~~~~~~~~~~force stop main thread,all sub-threads will exit!~~~~~~~~~~~~~");
	}
}

