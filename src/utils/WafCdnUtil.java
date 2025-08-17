package utils;

import java.util.List;

public class WafCdnUtil {

	public static boolean isWafCdnByServer(String server) {
		List<String> lines = ResourcesUtil.readFileLines("WAF-CDN-Server.txt");
		for (String line : lines) {
			if (server.startsWith(line)) {
				return true;
			}
		}
		return false;
	}
}
