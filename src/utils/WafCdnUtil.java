package utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WafCdnUtil {

	private static final String RESOURCE_SERVER = "WAF-CDN-Server.txt";
	private static final String RESOURCE_CNAME = "WAF-CDN-CNAME.txt";
	private static final String RESOURCE_HEADER = "WAF-CDN-Header.txt";

	/**
	 * 通过Server响应头判断是否是WAF或CDN。忽略大小写，按前缀匹配。
	 */
	public static boolean isWafCdnByServer(String server) {
		if (server == null) {
			return false;
		}
		String lower = server.toLowerCase().trim();
		for (String line : readLines(RESOURCE_SERVER)) {
			if (lower.startsWith(line.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 通过CNAME目标域名后缀判断是否是CDN。CNAME指向CDN厂商专属节点域名时命中。
	 */
	public static boolean isWafCdnByCName(Collection<String> cnames) {
		if (cnames == null || cnames.isEmpty()) {
			return false;
		}
		for (String cname : cnames) {
			if (cname == null) {
				continue;
			}
			String lower = cname.toLowerCase().trim();
			if (lower.endsWith(".")) {
				lower = lower.substring(0, lower.length() - 1);
			}
			for (String line : readLines(RESOURCE_CNAME)) {
				if (lower.endsWith(line.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 通过响应头/Set-Cookie指纹判断是否是WAF或CDN。只扫描头部区域（\r\n\r\n 之前），避免正文误报。
	 */
	public static boolean isWafCdnByResponse(byte[] response) {
		if (response == null || response.length == 0) {
			return false;
		}
		String headerBlock = extractHeaderBlock(response);
		if (headerBlock.isEmpty()) {
			return false;
		}
		String lower = headerBlock.toLowerCase();
		for (String line : readLines(RESOURCE_HEADER)) {
			if (lower.contains(line.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 汇总入口：Server头、CNAME、响应指纹任一命中即判定为WAF或CDN。
	 */
	public static boolean isWafCdn(String server, Collection<String> cnames, byte[] response) {
		if (isWafCdnByServer(server)) {
			return true;
		}
		if (isWafCdnByCName(cnames)) {
			return true;
		}
		if (isWafCdnByResponse(response)) {
			return true;
		}
		return false;
	}

	/**
	 * 从原始HTTP响应字节中截取头部区域（状态行+响应头），即 \r\n\r\n 之前的部分。
	 * 找不到标准分隔符时退化为 \n\n，仍找不到则只取前4KB（无body的响应）。
	 */
	private static String extractHeaderBlock(byte[] response) {
		int end = -1;
		for (int i = 0; i + 3 < response.length; i++) {
			if (response[i] == '\r' && response[i + 1] == '\n' && response[i + 2] == '\r' && response[i + 3] == '\n') {
				end = i;
				break;
			}
		}
		if (end < 0) {
			for (int i = 0; i + 1 < response.length; i++) {
				if (response[i] == '\n' && response[i + 1] == '\n') {
					end = i;
					break;
				}
			}
		}
		if (end < 0) {
			end = Math.min(response.length, 4096);
		}
		try {
			// 用ISO-8859-1保证非ASCII字节不丢失，指纹均为ASCII
			return new String(response, 0, end, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	/**
	 * 读取资源文件并过滤空行与#注释行。
	 */
	private static List<String> readLines(String resource) {
		List<String> result = new ArrayList<>();
		for (String line : ResourcesUtil.readFileLines(resource)) {
			if (line == null) {
				continue;
			}
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			result.add(trimmed);
		}
		return result;
	}
}
