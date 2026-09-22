package thread;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.bit4woo.utilbox.utils.DomainUtils;

import GUI.GUIMain;
import base.IndexedHashMap;
import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import config.ConfigManager;
import config.ConfigName;
import title.LineEntry;

/**
 * 执行web请求，获取title的线程
 *
 */

public class Producer extends Thread {// Producer do
	private final BlockingQueue<Map.Entry<String, String>> domainQueue;// use to store domains

	private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();// 静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
	public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
	public IExtensionHelpers helpers = callbacks.getHelpers();
	private GUIMain guiMain;

	public Producer(GUIMain guiMain, BlockingQueue<Map.Entry<String, String>> domainQueue, int threadNo) {
		this.guiMain = guiMain;
		this.domainQueue = domainQueue;
		this.setName(this.getClass().getName() + threadNo);
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (Thread.currentThread().isInterrupted()) {
					stdout.println(getName() + " interrupted");
					break;
				}

				Map.Entry<String, String> entry = domainQueue.poll(1, TimeUnit.SECONDS);
				if (entry == null) {
					stdout.println(getName() + " queue empty, exit");
					break;
				}

				String host = entry.getKey();
				String type = entry.getValue();
				Set<URL> urls = new HashSet<>(DomainUtils.toURLs(host));

				List<LineEntry> tempEntries = new ArrayList<LineEntry>();
				for (URL Url : urls) {
					LineEntry item = new LineEntry(Url).firstRequest(guiMain.getTitlePanel().getTempConfig());

					ConfigManager.doFilter(item);
					if (ConfigManager.getBooleanConfigByKey(ConfigName.removeItemIfIgnored)
							&& item.getCheckStatus().equals(LineEntry.CheckStatus_Checked)) {
						continue;
					}
					item.setEntrySource(type);

					String url = item.getUrl();
					if (item.getEntryType().equals(LineEntry.EntryType_Web)) {
						LineEntry linefound = findHistory(url.toString());
						if (null != linefound) {
							// 只继承旧记录的人工状态（comments/assetType/checkStatus/time）；
							// 不再去改 linefound 的 EntryTags，因为 linefound 可能是已脱离的快照（getAllTitle）
							// 或是当前表格里的另一条活跃记录（增量场景），修改它都属于无意义或有副作用的操作。
							item.getComments().addAll(linefound.getComments());
							item.setAssetType(linefound.getAssetType());
							try {
								// 长度的判断不准确，不再使用，就记录以前的状态！时间就记录上次完成渗透的时间
								if (url.equalsIgnoreCase(linefound.getUrl())) {
									item.setCheckStatus(linefound.getCheckStatus());
									item.setTime(linefound.getTime());
								}
							} catch (Exception err) {
								err.printStackTrace(stderr);
							}
						}
					}
					tempEntries.add(item);
				}

				tempEntries = ConfigManager.doSameHostFilter(tempEntries);

				for (LineEntry item : tempEntries) {
					if (ConfigManager.getBooleanConfigByKey(ConfigName.removeItemIfIgnored)
							&& item.getCheckStatus().equals(LineEntry.CheckStatus_Checked)) {
						continue;
					}
					guiMain.getTitlePanel().getTitleTable().getLineTableModel().addNewLineEntry(item);
					// stdout.println(new LineEntry(messageinfo,true).ToJson());
					int leftTaskNum = domainQueue.size();
					stdout.println(
							String.format("+++ [%s] +++ get title done %s tasks left", item.getUrl(), leftTaskNum));
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			stdout.println(getName() + " interrupted during execution");
		} catch (Exception error) {
			error.printStackTrace(stderr);
		}

	}

	public LineEntry findHistory(String url) {
		IndexedHashMap<String, LineEntry> HistoryLines = guiMain.getTitlePanel().getBackupLineEntries();
		if (HistoryLines == null)
			return null;

		// 1) 优先按完整URL精确查找（O(1)），URL格式统一包含默认端口，故重跑场景通常能精确命中。
		LineEntry found = HistoryLines.get(url);
		if (found != null) {
			return found;
		}

		// 2) 按host/IP回退查找：用于“不同域名/URL解析到同一IP”的host碰撞场景，把旧记录的状态继承过来。
		// 注意：URL的host解析与循环无关，提前解析一次，避免在循环内反复 new URL。
		String host;
		try {
			host = new URL(url).getHost();// 可能是域名、也可能是IP
		} catch (Exception e) {
			return null;// url非法，无法回退查找
		}

		for (LineEntry line : HistoryLines.values()) {
			if (line == null) {
				continue;
			}
			try {// 根据host查找
				Set<String> ipset = line.getIPSet();// 解析得到的IP集合
				if (ipset == null) {
					continue;// 手动保存等记录可能没有IP，跳过
				}
				Set<String> lineHost = new HashSet<>(ipset);
				lineHost.add(line.getHost());
				if (lineHost.contains(host)) {
					return line;
				}
			} catch (Exception e) {
				e.printStackTrace(BurpExtender.getStderr());
			}
		}
		return null;
	}

	public static void main(String[] args) {
		int i = 0;
		while (true) {
			if (i >= 10) {
				System.out.println("exited.");
				break;
			}
			i++;
		}
		System.out.println("1111");
	}
}