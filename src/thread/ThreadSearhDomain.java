package thread;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import GUI.GUIMain;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadSearhDomain extends Thread {
	private List<IHttpRequestResponse> messages;
	private final List<DomainProducer> plist = Collections.synchronizedList(new ArrayList<>());

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();// 静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout;
	public PrintWriter stderr;
	public IExtensionHelpers helpers = callbacks.getHelpers();
	GUIMain guiMain;
	private boolean searchThirdPart;

	public ThreadSearhDomain(GUIMain guiMain, List<IHttpRequestResponse> messages, boolean searchThirdPart) {
		this.guiMain = guiMain;
		this.messages = messages;
		stdout = BurpExtender.getStdout();
		stderr = BurpExtender.getStderr();
		this.setName(this.toString());
		this.searchThirdPart = searchThirdPart;
	}

	@Override
	public void run() {
		stdout.println("~~~~~~~~~~~~~Start Search Domain~~~~~~~~~~~~~");

		guiMain.getInputQueue().addAll(messages);

		for (int i = 0; i <= 20; i++) {
			DomainProducer p = new DomainProducer(guiMain, guiMain.getInputQueue(), i, searchThirdPart);
			plist.add(p);
			p.start();
		}

		for (DomainProducer p : plist) {
			try {
				p.join(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		stdout.println("~~~~~~~~~~~~~Search Domain Done~~~~~~~~~~~~~");
	}

	public void stopAll() {
		if (plist == null)
			return;

		for (DomainProducer p : plist) {
			p.interrupt();// 必须配合Thread.currentThread().isInterrupted()逻辑，否则不起作用
		}

		for (DomainProducer p : plist) {
			try {
				p.join(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		stdout.println("~~~~~~~~~~~~~all sub-threads exit!~~~~~~~~~~~~~");
	}
}
