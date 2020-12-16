package test;

import java.util.ArrayList;

public class threadExampleForceStop extends Thread{
	public static void main(String args[]) throws Exception{
		threadExampleForceStop xxx = new threadExampleForceStop();
		xxx.start();
		Thread.sleep(1*60);
		xxx.interrupt();
	}

	@Override
	public void run() {
		ArrayList<Producertest> plist = new ArrayList<Producertest>();

		for (int i=0;i<=5;i++) {
			Producertest p = new Producertest(i);
			p.setDaemon(true);//将子线程设置为守护线程，会随着主线程的结束而立即结束
			p.start();
			plist.add(p);
		}

		/*方法1：
		for (Producertest p:plist) {
			try {
				p.join();
				//让主线程等待各个子线程执行完成，才会结束。
				//https://www.cnblogs.com/zheaven/p/12054044.html
			} catch (InterruptedException e) {
				System.out.println("force stop received");
				//e.printStackTrace();
				break;//必须跳出循环，否则只是不再等待其中的一个线程，还会继续等待其他线程
			}
		}*/

		try {
			for (Producertest p:plist) {
				p.join();
				//让主线程等待各个子线程执行完成，才会结束。
				//https://www.cnblogs.com/zheaven/p/12054044.html
			}
		} catch (InterruptedException e) {
			System.out.println("force stop received");
			//e.printStackTrace();
		}
		System.out.println("main thread exit");
		return;
	}
}

class Producertest extends Thread {
	private int threadNo;
	public Producertest(int threadNo) {
		this.threadNo = threadNo;
	}
	@Override
	public void run() {
		while (true) {
			try {
				System.out.println("Produced thread:"+ threadNo+" is alive");
				Thread.sleep(1*60);
				System.out.println("Produced thread:"+ threadNo+" is alive");
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}
}
