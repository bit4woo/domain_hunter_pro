package utils;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/*
 * 从插件的jar包中读取文件列表和文件内容
 */
public class ResourcesUtil {

	/**
	 * @return
	 */
	public static List<String> getResourceFileList() {
		List<String> fileList = new ArrayList<>();

		try {
			// 获取当前 JAR 包的 URL，比如 file:/Users/user/project/dist/mytool.jar
			URL jarUrl = ResourcesUtil.class.getProtectionDomain().getCodeSource().getLocation();

			String jarFilePath = URLDecoder.decode(jarUrl.getPath(), "UTF-8"); // 解码路径
			JarFile jarFile = new JarFile(jarFilePath); // 用真实路径

			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {

				JarEntry entry = entries.nextElement();

				if (!entry.isDirectory() && entry.getName().endsWith(".txt")
						&& !entry.getName().startsWith("META-INF/")) {
					String fileName = entry.getName();
					fileList.add(fileName);
				}
			}
			jarFile.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(fileList);

		return fileList;
	}

	/**
	 * 从 JAR 包中读取指定文件的内容
	 */
	public static List<String> readFileLines(String filePath) {
		try {
			URL jarUrl = ResourcesUtil.class.getProtectionDomain().getCodeSource().getLocation();
			String jarFilePath = URLDecoder.decode(jarUrl.getPath(), "UTF-8");

			JarFile jarFile = new JarFile(jarFilePath);
			JarEntry jarEntry = jarFile.getJarEntry(filePath);
			if (jarEntry != null) {
				try (InputStream inputStream = jarFile.getInputStream(jarEntry);
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
					return reader.lines().collect(Collectors.toList());
				}
			} else {
				System.err.println("File not found in JAR: " + filePath);
			}
			jarFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	public static void main(final String[] args) throws IOException {

	}
}