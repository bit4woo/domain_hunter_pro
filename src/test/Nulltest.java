package test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Nulltest {
    public  static void main(String args[]){
        String a = null;
        if (a != null){
            System.out.println("xxx");
        }

        BlockingQueue<String> dictQueue = new LinkedBlockingQueue();
        dictQueue.add("a");
        dictQueue.add("b");
        dictQueue.add("c");
        try {
            System.out.println(dictQueue.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
