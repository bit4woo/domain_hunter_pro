package InternetSearch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private File logFile;
    private long maxFileSize; // 最大文件大小，单位为字节

    public Logger(String filePath, long maxFileSize) {
        this.logFile = new File(filePath);
        this.maxFileSize = maxFileSize;
    }

    public File getLogFile() {
		return logFile;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public void log(String message) {
        // 检查文件大小是否超过限制
        if (logFile.length() >= maxFileSize) {
            clearLogFile(); // 清空文件内容
        }

        // 将日志信息写入文件
        try (FileWriter writer = new FileWriter(logFile, true)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            writer.write(timestamp + " " + message + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearLogFile() {
        // 清空文件内容
        try (FileWriter writer = new FileWriter(logFile)) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 创建日志记录器，设置最大文件大小为10MB
        Logger logger = new Logger("log.txt", 10 * 1024 * 1024);

        // 写入日志信息
        for (int i = 0; i < 10000; i++) {
            logger.log("Log message " + i);
        }
    }
}
