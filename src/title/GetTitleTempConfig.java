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

	GetTitleTempConfig(){
		handlePriavte = WetherHandlePrivate();
		cookie = inputCookie();
		threadNumber = inputThreadNumber();
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

	public static int inputThreadNumber() {
		while (true) {
			try {
				String threads = JOptionPane.showInputDialog("How many threads do you want to use", "50").trim();
				int number = Integer.parseInt(threads);
				return number;
			}catch (Exception e) {
				continue;
			}
		}
	}
}
