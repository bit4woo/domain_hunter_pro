package Config;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import burp.BurpExtender;
import burp.LineEntry;
import burp.TitlePanel;

public class LineConfig {
	private static int MaximumEntries = 1000;//控制显示的条目数，减少内存占用
	
	//跑title时根据各字段过滤某些条目
	//private static Set<String> blacklistHostSet = new HashSet<String>(); //其实不需要
	private static Set<String> blacklistStatusCodeSet = new HashSet<String>(); 
	private static Set<String> blacklistIPSet = new HashSet<String>(); 
	private static Set<String> blacklistCDNSet = new HashSet<String>(); 
	private static Set<String> blacklistWebContainerSet = new HashSet<String>(); 
	private static boolean ignoreHttpIfHttpsOK =true;
	
	
	
	public static int getMaximumEntries() {
		return MaximumEntries;
	}

	public void setMaximumEntries(int maximumEntries) {
		MaximumEntries = maximumEntries;
	}
	
//	public static Set<String> getBlacklistHostSet() {
//		return blacklistHostSet;
//	}
//
//	public static void setBlacklistHostSet(Set<String> blacklistHostSet) {
//		LineConfig.blacklistHostSet = blacklistHostSet;
//	}

	public static Set<String> getBlacklistStatusCodeSet() {
		return blacklistStatusCodeSet;
	}

	public static void setBlacklistStatusCodeSet(Set<String> blacklistStatusCodeSet) {
		LineConfig.blacklistStatusCodeSet = blacklistStatusCodeSet;
	}

	public static Set<String> getBlacklistIPSet() {
		return blacklistIPSet;
	}

	public static void setBlacklistIPSet(Set<String> blacklistIPSet) {
		LineConfig.blacklistIPSet = blacklistIPSet;
	}

	public static Set<String> getBlacklistCDNSet() {
		return blacklistCDNSet;
	}

	public static void setBlacklistCDNSet(Set<String> blacklistCDNSet) {
		LineConfig.blacklistCDNSet = blacklistCDNSet;
	}

	public static Set<String> getBlacklistWebContainerSet() {
		return blacklistWebContainerSet;
	}

	public static void setBlacklistWebContainerSet(Set<String> blacklistWebContainerSet) {
		LineConfig.blacklistWebContainerSet = blacklistWebContainerSet;
	}

	public static boolean isIgnoreHttpIfHttpsOK() {
		return ignoreHttpIfHttpsOK;
	}

	public static void setIgnoreHttpIfHttpsOK(boolean ignoreHttpIfHttpsOK) {
		LineConfig.ignoreHttpIfHttpsOK = ignoreHttpIfHttpsOK;
	}

	/*
	 * 能通过过滤器返回true，否则返回false。
	 */
	public static boolean doFilter(LineEntry entry) {
		
		PrintWriter stdout = BurpExtender.getStdout();
		PrintWriter stderr = BurpExtender.getStderr();
		
		//default requirement
		if (entry.getStatuscode() <=0 || entry.getStatuscode() >=500) {
			stdout.println(String.format("--- [%s] --- status code >= 500",entry.getUrl()));
			TitlePanel.getTitleTableModel().addNewNoResponseDomain(entry.getHost(), entry.getIP());
			return false;
		}
		
		if (null != blacklistStatusCodeSet && blacklistStatusCodeSet.size()>0) {
			if (blacklistStatusCodeSet.contains(Integer.toString(entry.getStatuscode()))) {
				stdout.println(String.format("--- [%s] --- due to status code black list",entry.getUrl()));
				return false;
			}
		}
		
		if (null != blacklistIPSet && blacklistIPSet.size()>0) {
			for (String IP:entry.getIP().split(",")) {
				if (blacklistIPSet.contains(IP.trim())) {
					stdout.println(String.format("--- [%s] --- due to IP black list",entry.getUrl()));
					return false;
				}
			}
		}
		
		if (null != blacklistCDNSet && blacklistCDNSet.size()>0) {
			String cdn = entry.getCDN();
			if (null != cdn && !cdn.trim().equals("")) {
				for (String cdnitem:cdn.split(",")) {
					if (blacklistCDNSet.contains(cdnitem.trim())) {
						stdout.println(String.format("--- [%s] --- due to CDN black list",entry.getUrl()));
						return false;
					}
				}
			}
		}		
		
		if (null != blacklistWebContainerSet && blacklistWebContainerSet.size()>0) {
			if (blacklistWebContainerSet.contains(entry.getWebcontainer())) {
				stdout.println(String.format("--- [%s] --- due to web container black list",entry.getUrl()));
				return false;
			}
		}
		
		//放到最后，其他匹配项可能更常用
//		if (null != blacklistHostSet && blacklistHostSet.size()>0) {
//			if (blacklistHostSet.contains(entry.getHost())) {
//				return false;
//			}
//		}
		
		return true;
	}
	
}
