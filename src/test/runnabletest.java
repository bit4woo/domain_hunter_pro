package test;
public class runnabletest implements Runnable{
 
    private int j;
    private int tickets;//资源，对所有线程来说是共享的。
    public runnabletest(int ticketNum) {
    	tickets = ticketNum;
    }
    @Override
    public void run() {
      for (int i=0;i<=20;i++) {//一般根据   【资源数量%线程数量+1】 来确定，保证资源得到处理
        
    	//如果线程类实现 runnable 接口获取当前的线程，只能用 Thread.currentThread() 获取当前的线程名
        Thread.currentThread().getName();
        
        if(tickets>0){
        	System.out.println(Thread.currentThread().getName()+"--卖出票：" + tickets--+"号票");
        	System.out.println(j++);
        }
      }
    }
    
    public static void main(String[] args){
     
        runnabletest xxx = new runnabletest(10);
     
        //通过new Thread(target,name)创建新的线程
        new Thread(xxx,"买票人1").start();
        new Thread(xxx,"买票人2").start();
        new Thread(xxx,"买票人3").start();
        
        System.out.println(xxx.j);
      }
}
