package domain;

import burp.BurpExtender;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;

//负责将收集的域名信息写入数据库
public class DomainConsumer extends Thread {

	private int threadNo;
	private volatile boolean stopflag;

	public DomainConsumer(int threadNo) {
		stopflag= false;
	}

	public void stopThread() {
		stopflag = true;
	}

	@Override
	public void run() {
		while(true){
			if (stopflag) {
				break;
			}
			try {
				QueueToResult();

				int min=3;
				int max=5;
				Random random = new Random();
				int minute = random.nextInt(max-min+1) + min;
				sleep(minute*60*1000);
			} catch (Exception error) {
				error.printStackTrace(BurpExtender.getStderr());
			}
		}
	}

	/*
	使用这种方法从Queue中取数据，一来避免了主动clear的操作，二来避免在使用数据后，clear操作之前加进来的数据的丢失。
	 */
	public static void moveQueueToSet(BlockingQueue<String> queue, Set<String> resultSet){
		while (!queue.isEmpty()){
			try {
				String item = queue.take();
				resultSet.add(item);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void QueueToResult() {
		//HashSet<String> oldSubdomains = new HashSet<String>();
		CopyOnWriteArraySet<String> oldSubdomains = new CopyOnWriteArraySet<String>();
		//java.util.ConcurrentModificationException 可能同时有其他线程在向subDomainSet中写数据，导致的这个错误。
		//http://ifeve.com/java-copy-on-write/
		//https://www.jianshu.com/p/c5b52927a61a
		DomainManager result= DomainPanel.getDomainResult();
		if (result != null) {
			oldSubdomains.addAll(result.getSubDomainSet());

			moveQueueToSet(BurpExtender.subDomainQueue,result.getSubDomainSet());//所有子域名还是都存在里面的，至少新发现的又单独存了一份，所以SubDomainSet一直都是最全的。
			moveQueueToSet(BurpExtender.similarDomainQueue,result.getSimilarDomainSet());
			moveQueueToSet(BurpExtender.relatedDomainQueue,result.getRelatedDomainSet());
			moveQueueToSet(BurpExtender.emailQueue,result.getEmailSet());
			moveQueueToSet(BurpExtender.packageNameQueue,result.getPackageNameSet());
			moveQueueToSet(BurpExtender.TLDDomainQueue,result.getSubDomainSet());
			
			HashSet<String> tmpTLDDomains = new HashSet<String>();
			moveQueueToSet(BurpExtender.TLDDomainQueue,tmpTLDDomains);
			for (String domain:tmpTLDDomains) {
				DomainPanel.domainResult.addToRootDomainAndSubDomain(domain, false);
			}

			HashSet<String> newSubdomains = new HashSet<String>();
			newSubdomains.addAll(result.getSubDomainSet());

			newSubdomains.removeAll(oldSubdomains);
			result.getNewAndNotGetTitleDomainSet().addAll(newSubdomains);

			if (newSubdomains.size()>0){
				BurpExtender.getStdout().println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~",newSubdomains.size()));
				BurpExtender.getStdout().println(String.join(System.lineSeparator(), newSubdomains));
			}
            DomainPanel.autoSave();
		}
	}

	public static void main(String[] args) {

	}
}