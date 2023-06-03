package base;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DictReader extends Thread{
	private static final Logger log = LogManager.getLogger(DictFileReader.class);
	String filePath;
	private boolean stopflag;
	private BlockingQueue<String> pathDict;
	
	public DictReader(String filePath,BlockingQueue<String> pathDict) {
		this.filePath = filePath;
		this.pathDict = pathDict;
	}
	
	public void stopThread() {
		stopflag = true;
	}
	
	@Override
	public void run() {
		DictFileReader readline = new DictFileReader(filePath);
		while(true){
			try {
				if (stopflag) {
					break;
				}
				List<String> tmp = readline.next(10000,"");
				if (tmp.size() == 0) {
					log.info("read dict file: "+filePath+" done");
					break;
				}else {
					for (String item:tmp) {
						pathDict.put(item);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
