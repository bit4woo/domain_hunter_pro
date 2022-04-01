package thread;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import title.IndexedLinkedHashMap;
import title.LineEntry;
import title.TempLineEntry;
import title.TitlePanel;

/** 
 * @author bit4woo
 * @github https://github.com/bit4woo 
 * @version CreateTime：Jun 25, 2020 2:35:31 PM 
 */

/**
 * 执行web请求，获取title的线程
 * 
 * @author bit4woo
 *
 */

public class Producer extends Thread {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private volatile boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public Producer(BlockingQueue<String> domainQueue,int threadNo) {
		this.domainQueue = domainQueue;
		stopflag= false;
		this.setName(this.getClass().getName()+threadNo);
	}

	public void setStopflag(boolean stop) {
		stopflag = stop;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (domainQueue.isEmpty() ) {
					stdout.println(getName()+" exited. due to domainQueue is empty");
					break;
				}
				if (Thread.interrupted()){//没有起作用！
					stdout.println(getName()+" exited. due to thread interrupt signal received");
					break;
				}
				if (stopflag){
					stdout.println(getName()+" exited. due to stop flag is true");
					break;
				}

				String host = domainQueue.take();
				int leftTaskNum = domainQueue.size();

				stdout.print(String.format("%s tasks left ",leftTaskNum));
				TempLineEntry tmpLine = new TempLineEntry(host);
				Set<LineEntry> resultSet  = tmpLine.getFinalLineEntry();
				//根据请求有效性分类处理
				Iterator<LineEntry> it = resultSet.iterator();
				while (it.hasNext()) {
					LineEntry item = it.next();
					String url = item.getUrl();
					if (item.getEntryType().equals(LineEntry.EntryType_Web)){
						LineEntry linefound = findHistory(url);
						if (null != linefound) {
							linefound.removeComment(LineEntry.NotTargetBaseOnCertInfo);
							linefound.removeComment(LineEntry.NotTargetBaseOnBlackList);
							item.addComment(linefound.getComment());
							item.setAssetType(linefound.getAssetType());
							try {
								//长度的判断不准确，不再使用，就记录以前的状态！时间就记录上传完成渗透的时间
								if (url.equalsIgnoreCase(linefound.getUrl())) {
									item.setCheckStatus(linefound.getCheckStatus());
									item.setTime(linefound.getTime());
								}
							}catch(Exception err) {
								err.printStackTrace(stderr);
							}
						}
					}

					TitlePanel.getTitleTableModel().addNewLineEntry(item);

					//stdout.println(new LineEntry(messageinfo,true).ToJson());

					stdout.println(String.format("+++ [%s] +++ get title done",url));
				}
			} catch (Exception error) {
				error.printStackTrace(stderr);
				continue;//unnecessary
				//java.lang.RuntimeException can't been catched, why?
			}
		}
	}

	public static LineEntry findHistory(String url) {
		IndexedLinkedHashMap<String,LineEntry> HistoryLines = GUIMain.getTitlePanel().getBackupLineEntries();
		if (HistoryLines == null) return null;
		LineEntry found = HistoryLines.get(url);
		if (found != null) {
			HistoryLines.replace(url,null);//不使用remove和put操作，避免//ConcurrentModificationException问题
			//因为这2个操作都会让map的长度发生变化，从而导致问题
			return found;
		}

		//根据host进行查找的逻辑，不会导致手动保存的条目被替换为null，因为手动保存的条目IP列表为空
		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		for (LineEntry line:HistoryLines.values()) {
			if (line== null) {
				continue;
			}
			line.setHelpers(helpers);
			try{//根据host查找
				String host = new URL(url).getHost();//可能是域名、也可能是IP

				Set<String> lineHost = line.fetchIPSet();//解析得到的IP集合
				lineHost.add(line.getHost());
				if (lineHost.contains(host)) {
					//HistoryLines.remove(line.getUrl());//如果有相同URL的记录，就删除这个记录。//ConcurrentModificationException
					HistoryLines.replace(url,null);
					return line;
				}
			}catch (Exception e){
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return null;
	}

	public static void main(String[] args) {
		int i= 0;
		while(true) {
			if (i >= 10) {
				System.out.println("exited.");
				break;
			}
			i++;
		}
		System.out.println("1111");
	}
}