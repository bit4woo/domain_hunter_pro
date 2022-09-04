package Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;

import burp.BurpExtender;
import burp.Commons;

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
		String command = String.format("/usr/local/bin/masscan -p 1-65535 --rate 10000 -iL {} -oJ {}",targetFile,resultFile);
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
	
	private static String getSystemCharSet() {
		return Charset.defaultCharset().toString();
	}
	
	//TODO有的命令可以，有的不行，奇怪
	public static void findAbsolutePath(String cmd) throws Exception {
		//windows where
		//linux which
		//mac which where
		
		if (Commons.isWindows()) {
			cmd = "where "+cmd;
		}else {
			cmd = "which "+cmd;
		}
		
		final Process p = Runtime.getRuntime().exec(cmd);

		new Thread(new Runnable() {
		    public void run() {
		        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		        String line = null;
		        try {
		            while ((line = input.readLine()) != null)
		                System.out.println(line);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		}).start();

		p.waitFor();
	}
	
	
	public static void main(String[] args) throws Exception {
//		System.out.println(masscan("43.132.80.21"));
//		findAbsolutePath("masscan");
//		findAbsolutePath("python");
//		System.out.println(findAbsolutePath("masscan"));
//		System.out.println(findAbsolutePath("python"));

	}
}
