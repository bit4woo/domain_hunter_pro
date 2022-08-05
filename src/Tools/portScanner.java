package Tools;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;

/**
 * 实现Masscan+Nmap的端口扫描
 *
 */
public class portScanner {
	
	public static String masscan(String ip) {
		String resultFile = resultFile(ip);
		String command = String.format("/usr/local/bin/masscan -p 1-65535 --rate 10000 -oJ %s %s",resultFile,ip.trim());
		System.out.println(command);
		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();//等待执行完成
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultFile;
	}
	
	/**
	 * 
	 * @param ipList
	 * @return 扫描结果文件
	 */
	public static String masscan(List<String> ipList) {
		String targetFile = targetsToFile(ipList);
		String resultFile = resultFile(targetFile);
		String command = String.format("sudo masscan -p 1-65535 --rate 10000 -iL {} -oJ {}",targetFile,resultFile);
		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();//等待执行完成
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultFile;
	}
	
	public static void nmap(String masscanJsonFile) {
//		String command = "sudo masscan -p 1-65535 --rate 10000 -oJ {}.masscan.json".format(ip);
	}
	
	
	public static void parseMasscanJsonFile(String masscanJsonFile) {
	}
	
	public static String resultFile(String targetIPOrFile) {
		String basedir = (String) System.getProperties().get("java.io.tmpdir");
		String resultFile = targetIPOrFile+".masscan.json";
		if (targetIPOrFile.startsWith(basedir)){
			return resultFile;
		}else {
			File resultFile1 = new File(basedir,resultFile);
			return resultFile1.getAbsolutePath();
		}
	}
	
	public static String targetsToFile(List<String> ipList) {
		try {
			String basedir = (String) System.getProperties().get("java.io.tmpdir");
			File targetFile = new File(basedir,"masscan-temp-target.txt");
			targetFile.deleteOnExit();
			targetFile.createNewFile();
			
			String targetsConent = String.join(System.lineSeparator(), ipList);
			FileUtils.writeByteArrayToFile(targetFile, targetsConent.getBytes());

			return targetFile.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
			return null;
		}
	}
	
	public static String findAbsolutePath(String cmd) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);//TODO
			process.waitFor();//等待执行完成
			OutputStream aa = process.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String[] args) {
		System.out.println(masscan("43.132.80.21"));
	}
}
