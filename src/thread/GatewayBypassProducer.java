package thread;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import burp.BurpExtender;
import burp.HelperPlus;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import title.LineEntry;
import title.LineTableModel;

/**
 * Host碰撞检测线程
 * @author bit4woo
 *
 */
public class GatewayBypassProducer extends Thread {//Producer do
	private volatile boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	private Set<String> IPSet;
	private Set<String> domainSet;
	private BlockingQueue<String> outQueue;

	public GatewayBypassProducer(Set<String> IPSet,Set<String> domainSet,
			BlockingQueue<String> outQueue) {
		this.IPSet = IPSet;
		this.domainSet = domainSet;
		this.outQueue = outQueue;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (stopflag) {
					break;
				}
				
				for (String ip:IPSet) {
					for (String domain:domainSet) {
						outQueue.put(ip+"###"+domain);
					}
				}
			}catch (Exception e) {
				e.printStackTrace(stderr);
			}
		}
	}
}