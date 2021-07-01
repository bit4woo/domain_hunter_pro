package title;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import GUI.GUI;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;

/** 
 * @author bit4woo
 * @github https://github.com/bit4woo 
 * @version CreateTime：Jun 25, 2020 2:35:31 PM 
 */

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

public class Producer extends Thread {//Producer do
	private final BlockingQueue<String> domainQueue;//use to store domains
	private int threadNo;
	private volatile boolean stopflag = false;

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();

	public Producer(BlockingQueue<String> domainQueue,int threadNo) {
		this.threadNo = threadNo;
		this.domainQueue = domainQueue;
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			try {
				if (domainQueue.isEmpty() || stopflag) {
					//stdout.println(threadNo+" Producer exited");
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
					LineEntry linefound = findHistory(url);
					if (null != linefound) {
						linefound.removeComment(LineEntry.NotTargetBaseOnCertInfo);
						linefound.removeComment(LineEntry.NotTargetBaseOnBlackList);
						item.addComment(linefound.getComment());
						item.setAssetType(linefound.getAssetType());
						try {
							if (url.equalsIgnoreCase(linefound.getUrl()) && item.getBodyText().length() == linefound.getBodyText().length()) {
								item.setCheckStatus(linefound.getCheckStatus());
								item.setTime(linefound.getTime());
							}
						}catch(Exception err) {
							err.printStackTrace(stderr);
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
		IndexedLinkedHashMap<String,LineEntry> HistoryLines = GUI.getTitlePanel().getBackupLineEntries();
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
				String host = new URL(url).getHost();

				List<String> lineHost = new ArrayList<>(Arrays.asList(line.getIP().trim().split(",")));
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
}