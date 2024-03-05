package title;

import javax.swing.JOptionPane;

public class GetTitleTempConfig {

	boolean handlePriavte = true;
	String cookie = "";
	int  threadNumber = 50;


	public boolean isHandlePriavte() {
		return handlePriavte;
	}

	public void setHandlePriavte(boolean handlePriavte) {
		this.handlePriavte = handlePriavte;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public int getThreadNumber() {
		return threadNumber;
	}

	public void setThreadNumber(int threadNumber) {
		this.threadNumber = threadNumber;
	}
	
	GetTitleTempConfig(int domainNum){
		handlePriavte = WetherHandlePrivate();
		cookie = inputCookie();
		threadNumber = threadNumberShouldUse(domainNum);
		//threadNumber = inputThreadNumber(num);
	}

	private static boolean WetherHandlePrivate() {
		int user_input = JOptionPane.showConfirmDialog(null, "Do you want request [PRIVATE] ip addresses?","Chose work model",JOptionPane.YES_NO_OPTION);
		if (JOptionPane.YES_OPTION == user_input) {
			return true;
		}else {
			return false;
		}
	}

	//cookie used at burp.Commons.buildCookieRequest(IExtensionHelpers, String, byte[])
	//burp.Producer.doRequest(URL)
	public static String inputCookie() {
		String cookie = JOptionPane.showInputDialog("Input cookie OR Leave it blank", null).trim();
		return cookie;
	}

	@Deprecated
	public static int inputThreadNumber(int defaultThreadNum) {
		int times = 3;
		while (times > 0) {
			times--;
			try {
				String threads = JOptionPane.showInputDialog("How many threads do you want to use", defaultThreadNum).trim();
				int number = Integer.parseInt(threads);
				return number;
			}catch (Exception e) {
				continue;
			}
		}
		return -1;
	}
	
	/**
	 * 根据已有的域名梳理，预估应该使用的线程数
	 * 假设1个任务需要1秒钟。线程数在1-100之间，如何选择线程数使用最小的时间？
	 * @param domains
	 * @return
	 */
	public static int threadNumberShouldUse(int domainNum) {

		int tmp = (int) Math.sqrt(domainNum);
		if (tmp <=1) {
			return 1;
		}else if(tmp>=50) {
			return 50;
		}else {
			return tmp;
		}
	}
	
	public static void main(String[] args) {
		System.out.println(threadNumberShouldUse(100));
		System.out.println(threadNumberShouldUse(50));
		System.out.println(threadNumberShouldUse(1));
		System.out.println(threadNumberShouldUse(1000));
		System.out.println(threadNumberShouldUse(2395));
		System.out.println(threadNumberShouldUse(2395000));
	}
}
