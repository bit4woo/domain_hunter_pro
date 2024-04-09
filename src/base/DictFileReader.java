package base;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DictFileReader {
	static File sourceFile;
	private static final Logger log = LogManager.getLogger(DictFileReader.class);

	int nextReadLineIndex = 0;//从0开始计数，就像数组的index,标识下一个即将被读取的行

	FileReader fileReader = null;
	LineNumberReader lineNumberReader = null;
	InputStream dictInputStream = null; //用于读取内置字典
	InputStreamReader dictInputStreamReader = null; //用于读取内置字典
	int lineNumber;

	public DictFileReader(String filePath){
		sourceFile = new File(filePath.trim());
		if (!sourceFile.exists()) {
			log.warn("指定的文件不存在[%s]",sourceFile);
			System.exit(0);
		}else {
			lineNumber = getTotalLines();
			System.out.println("字典文件共有"+lineNumber+"行");
		}
	}

	public List<String> next(int NumberToRead,String suffix){
		//System.out.println("读取"+nextReadLineIndex+"---"+(nextReadLineIndex+NumberToRead));
		return readLinebyNumber(nextReadLineIndex,NumberToRead,suffix);
	}

	// 读取文件指定行。
	//fromNumber 从0开始计数，就像数组的index
	private List<String> readLinebyNumber(int beginIndex,int NumberToRead,String suffix){
		List<String> dicts = new ArrayList<>();

		//beginIndex需要读取的区间的开始index
		int endIndex = beginIndex+NumberToRead-1;//需要读取的区间的结束index
		int maxIndex = lineNumber-1;//最大index是总行数-1

		if (beginIndex >= 0 && beginIndex <= maxIndex){
			try {
				fileReader = new FileReader(sourceFile);
				lineNumberReader = new LineNumberReader(fileReader);
				//lineNumberReader.setLineNumber(fromNumber);//虽然设置了行号，还是会从第一行开始读取，并不会改变当前读取的位置！！！反而会影响判断
				//System.out.println("更改后行号为:" + lineNumberReader.getLineNumber());
				//long i = lineNumberReader.getLineNumber();
				//System.out.println("即将被读取的行的Index" + lineNumberReader.getLineNumber());

				while (true) {
					String lineStr = lineNumberReader.readLine();
					if (lineStr == null) {//文件结束
						break;
					}else {
						int currentIndex = lineNumberReader.getLineNumber()-1;//getLineNumber()获取到的是下一次需要读取的行的index
						nextReadLineIndex = lineNumberReader.getLineNumber();//下次开始读取的index，是当前行数的下一行
						//注意，这个lineNuber是在读取后自动加1的。
						if (currentIndex < beginIndex) {//当前读取到的值的Index < 开始行的Index
							continue;
						}else if (currentIndex >= beginIndex &&  currentIndex <= endIndex) {
							lineStr = lineStr.trim();
							if (StringUtils.isNotEmpty(lineStr)) {
								dicts.add(lineStr+suffix);
								//dicts.addAll(AltDomainGenerator.genKeywordDomain(Config.specialKeywords, lineStr, suffix));
							}
						}else {
							break;
						}
					}
				}
				//System.out.println("更改后行号为:" + reader.getLineNumber());//debug
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (null != lineNumberReader)
						lineNumberReader.close();
					if (null != fileReader)
						fileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return dicts;
	}

	// 文件内容的总行数。
	int getTotalLines(){
		int lines = -1;
		try {
			fileReader = new FileReader(sourceFile);
			lineNumberReader = new LineNumberReader(fileReader);

			String s = lineNumberReader.readLine();
			lines = 0;
			while (s != null) {
				lines++;
				s = lineNumberReader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				if (null != lineNumberReader)
					lineNumberReader.close();
				if (null != fileReader)
					fileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return lines;
	}

	public static void test() {
		DictFileReader xxx = new DictFileReader("D:\\dicttest.txt");
		List<String> aaa = xxx.next(10,"");
		System.out.println(aaa);
		List<String> bbb = xxx.next(10,"");
		System.out.println(bbb);
		List<String> ccc = xxx.next(10,"");
		System.out.println(ccc);
	}

	public static void test1() {
		DictFileReader readline = new DictFileReader("D:\\numdict.txt");
		while(true){
			List<String> tmp = readline.next(10,"xxx");
			System.out.println(tmp);
			if (tmp.size() == 0) {
				//System.out.println("没有更多元素，退出");
				break;
			}
		}
	}

	public static void test2() {
		DictFileReader readline = new DictFileReader("D:\\dicttest.txt");
		List<String> result = readline.readLinebyNumber(0,3,"");
		System.out.println(result);
		System.out.println(result.size());
		//		List<String> result1 = readline.readLinebyNumber(629,1000,"");
		//		System.out.println(result1);
		//		System.out.println(result1.size());
	}


	public static void test3() {
		DictFileReader readline = new DictFileReader("D:\\dicttest.txt");
		while(true){
			List<String> tmp = readline.next(100,"xxx");
			System.out.println(tmp);
		}
	}

	/** * 读取文件指定行。 */
	public static void main(String[] args) throws IOException {
		test1();
	}
}
