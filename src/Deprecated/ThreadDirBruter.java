package thread;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import GUI.DictReader;
import GUI.RunnerGUI;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import config.ConfigPanel;

//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
/**
 * 执行路径爆破的主线程
 * @author bit4woo
 *
 */
public class ThreadDirBruter extends Thread{
	private IHttpRequestResponse messageInfo;
	private List<DirBruterProducer> plist;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private RunnerGUI runnerGUI;
	BlockingQueue<String> pathDict = new LinkedBlockingQueue<String>();//创建对象时就加载字典，避免字典因加载慢而为空

	public ThreadDirBruter(RunnerGUI runnerGUI,IHttpRequestResponse messageInfo) {
		this.runnerGUI = runnerGUI;
		this.messageInfo = messageInfo;
		File dictFile = new File(ConfigPanel.textFieldDirBruteDict.getText().trim());
		if (dictFile.exists()){
			DictReader reader = new DictReader(dictFile.toString(),pathDict);
			reader.start();
			reader.setDaemon(true);//TODO 字典还未加载完成，就执行爆破，线程会退出。
		}else {
			stdout.println("dict file not found!");
			return;
		}
	}

	@Override
	public void run(){
		BlockingQueue<String> pathDict = new LinkedBlockingQueue<String>();
		File dictFile = new File(ConfigPanel.textFieldDirBruteDict.getText().trim());
		if (dictFile.exists()){
			DictReader reader = new DictReader(dictFile.toString(),pathDict);
			reader.start();
		}else {
			stdout.println("dict file not found!");
			return;
		}

		stdout.println("~~~~~~~~~~~~~Start threading Runner~~~~~~~~~~~~~ total task number: "+pathDict.size());

		plist = new ArrayList<DirBruterProducer>();

		for (int i=0;i<=50;i++) {
			DirBruterProducer p = new DirBruterProducer(runnerGUI.getRunnerTableModel(),pathDict,messageInfo, i);
			p.setDaemon(true);
			p.start();
			plist.add(p);
		}
		
		try {
			for (DirBruterProducer p:plist) {
				p.join();
			}
			//让主线程等待各个子线程执行完成，才会结束。
			//https://www.cnblogs.com/zheaven/p/12054044.html
		} catch (InterruptedException e) {
			stdout.println("force stop received");
			e.printStackTrace();
		}
		
		stdout.println("~~~~~~~~~~~~~Get Title Done~~~~~~~~~~~~~");
		runnerGUI.lblStatus.setText("finished");
	}

	@Deprecated
	public void stopThreads() {
		if (plist != null) {
			for (DirBruterProducer p:plist) {
				p.stopThread();
			}
			stdout.println("~~~~~~~~~~~~~All stop message sent! wait them to exit~~~~~~~~~~~~~");
		}
	}
	
	public void forceStopThreads() {
		this.interrupt();//将子线程都设置为守护线程，会随着主线程的结束而立即结束
		stdout.println("~~~~~~~~~~~~~force stop main thread,all sub-threads will exit!~~~~~~~~~~~~~");
	}
}


