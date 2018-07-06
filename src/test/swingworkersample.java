package test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public class swingworkersample extends SwingWorker<Integer,Integer>{

    private JFrame window;
    private JTextField text;

    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                new swingworkersample().execute();
            }
        });
    }

    swingworkersample(){
        window = new JFrame();
        window.setVisible(true);
        window.setSize(150, 150);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        text = new JTextField();
        window.add(text);
    }

    @Override
    protected Integer doInBackground() throws Exception {
        if(SwingUtilities.isEventDispatchThread())
            System.out.println("doInBackground()在EDT");
        int i=0;
        while(i!=100){
            Thread.sleep(100);
            publish(i++);
        }
        return i;
    }

    @Override
    protected void done() {
        if(SwingUtilities.isEventDispatchThread())
            System.out.println("done()在EDT");
        try {
            System.out.println("任务结束了，done（），结果为"+get());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void process(List<Integer> chunks) {
        if(SwingUtilities.isEventDispatchThread())
            System.out.println("process()在EDT");
        for(int i:chunks){
            text.setText(String.valueOf(i));
        }
    }

}