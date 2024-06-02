package utils;

import java.util.List;
import java.util.Set;

import com.bit4woo.utilbox.utils.SystemUtils;

public class PortScanUtils {
	
	public static String genCmd(String ScannerCmd,List<String> targets) {
		
		//先判断masscan,因为可能存在masscan 和nmap联合使用的情况，比如用xargs？
		String text = "";
		if (ScannerCmd.contains("masscan")) {
			text = String.join(",", targets);
		}else if (ScannerCmd.contains("nmap")) {
			text = String.join(" ", targets);
		}else {
			text = String.join(" ", targets); //比如调用了masscan和nmap的python脚本
		}
		String command = ScannerCmd.replace("{host}", text.trim());
		return command;
	}
	
	
	public static String genCmd(String ScannerCmd,Set<String> targets) {
		
		//先判断masscan,因为可能存在masscan 和nmap联合使用的情况，比如用xargs？
		String text = "";
		if (ScannerCmd.contains("masscan")) {
			text = String.join(",", targets);
		}else if (ScannerCmd.contains("nmap")) {
			text = String.join(" ", targets);
		}else {
			text = String.join(" ", targets); //比如调用了masscan和nmap的python脚本
		}
		String command = ScannerCmd.replace("{host}", text.trim());
		return command;
	}
	
	
	public static void genCmdAndCopy(String ScannerCmd,List<String> targets) {
		SystemUtils.writeToClipboard(genCmd(ScannerCmd,targets));
	}
	
	public static void genCmdAndCopy(String ScannerCmd,Set<String> targets) {
		SystemUtils.writeToClipboard(genCmd(ScannerCmd,targets));
	}
}
