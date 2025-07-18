package utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ASN.ASNEntry;
import burp.BurpExtender;

public class WafCdnUtil {

	public static boolean isWafCdnByServer(String server){
		List<String> lines = readFile("WAF-CDN-Server.txt");
		for (String line:lines){
			if (server.startsWith(line)) {
				return true;
			}
		}
		return false;
	}
	
	
	private static List<String> readFile(String filename) {
		try {
			URL url = BurpExtender.class.getClassLoader().getResource(filename);
			File copyFile = new File(FileUtils.getTempDirectory()+File.pathSeparator+"."+filename);
			copyFile.deleteOnExit();
			if (copyFile.exists()) {
				copyFile.delete();
			}
			FileUtils.copyURLToFile(url,copyFile);
			List<String> dictList = FileUtils.readLines(copyFile, StandardCharsets.UTF_8);
			return dictList;
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}
}
