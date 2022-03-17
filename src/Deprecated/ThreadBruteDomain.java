package Deprecated;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import burp.BurpExtender;
import burp.Commons;
import burp.DomainNameUtils;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import domain.DomainPanel;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
@Deprecated
class ThreadBruteDomain{
    private Set<String> rootDomains = new HashSet<>();
    private List<DomainBruteProducer> plist;

    private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

    public static Map<String ,Set<String>> badRecords = new HashMap<>();

    public ThreadBruteDomain(Set<String> rootDomains) {
        this.rootDomains.addAll(rootDomains);
        //this.rootDomains = rootDomains; 是对象地址的传递，删除其中元素，将导致原始数据被删除
    }

    public void Do(){
        stdout.println("~~~~~~~~~~~~~Start Brute Domain~~~~~~~~~~~~~");

        //清除泛解析的域名
/*        //TODO 不应该全删除，应当记录泛解析的IP。
        Iterator<String> it = rootDomains.iterator();
        while (it.hasNext()){
            String rootDomain = it.next();
            String badDomain = "domain-hunter-pro-test."+rootDomain;
            String ip = DomainBruteProducer.query(badDomain);
            if (ip != null){
                it.remove();
                stdout.println(rootDomain+ " removed, due to all subdomains point to same IP");
            }
        }*/

        stdout.println("checking wildcard DNS record");
        for (String rootDomain: rootDomains){
            String badDomain = "domain-hunter-pro-test."+rootDomain;
            Set<String> ipset = DomainNameUtils.dnsquery(badDomain).get("IP");
            badRecords.put(rootDomain,ipset);
        }

        stdout.println(rootDomains);

        if (rootDomains.size() <=0) return;

        BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();//use to store rootDomains
        BlockingQueue<String> dictQueue = new LinkedBlockingQueue<String>();//store dict

        dictQueue.addAll(readDictFile());
        plist = new ArrayList<DomainBruteProducer>();

        for (int i=0;i<=10000;i++) {//layer中是10万
            DomainBruteProducer p = new DomainBruteProducer(rootDomains,dictQueue,outputQueue,i);
            p.start();
            plist.add(p);
        }

        while(true) {//to wait all threads exit.
            if (dictQueue.isEmpty() && isAllProductorFinished()) {
                stdout.println("~~~~~~~~~~~~~Brute Domain Done~~~~~~~~~~~~~");
                break;
            }else {
                try {
                    Thread.sleep(1*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
        }

        HashSet<String> oldSubdomains = new HashSet<String>();
        oldSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());
        DomainPanel.getDomainResult().getSubDomainSet().addAll(outputQueue);

        HashSet<String> newSubdomains = new HashSet<String>();
        newSubdomains.addAll(DomainPanel.getDomainResult().getSubDomainSet());

        newSubdomains.removeAll(oldSubdomains);

        stdout.println(String.format("~~~~~~~~~~~~~%s subdomains added!~~~~~~~~~~~~~",newSubdomains.size()));
        stdout.println(String.join(System.lineSeparator(), newSubdomains));

        return;
    }

    boolean isAllProductorFinished(){
        for (DomainBruteProducer p:plist) {
            if(p.isAlive()) {
                return false;
            }
        }
        return true;
    }

    public void stopThreads() {
        for (DomainBruteProducer p:plist) {
            p.stopThread();
        }
        stdout.println("brute threads stopped!");
    }


    public Set<String> readDictFile(){
        //https://www.cnblogs.com/macwhirr/p/8116583.html
        Set<String> dicts = new HashSet<>();
        // 返回当前类字节码所在路径,即 target/classes/类的包路径
        //String path = this.getClass().getResource("").toString();
//        String path = this.getClass().getResource("/dict.txt").toString();
//        stdout.println(path);
//        if (new File(path).exists()){
//            stdout.println("exists");
//        }else {
//            stdout.println("false");
//        }
        InputStream aaa = this.getClass().getResourceAsStream("/dict.txt");

        InputStream is = null;
        Reader reader = null;
        BufferedReader bufferedReader = null;
        try {
            reader = new InputStreamReader(aaa);
            bufferedReader = new BufferedReader(reader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                dicts.add(line);
                //System.out.println(line);
            }
            return dicts;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufferedReader)
                    bufferedReader.close();
                if (null != reader)
                    reader.close();
                if (null != is)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dicts;
    }

    public static void main(String args[]) {//test
        //System.out.println(DomainProducer.grepDomain("http://www.jd.com/usr/www.baidu.com/xss.jd.com"));
    }
}

/*
 * do request use method of burp
 * return IResponseInfo object Set
 *
 */

class DomainBruteProducer extends Thread {//Producer do
    private Set<String> rootDomains;
    private BlockingQueue<String> dictQueue;
    private BlockingQueue<String> outputQueue;

    private int threadNo;
    private boolean stopflag = false;

    private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

    public DomainBruteProducer(Set<String> rootDomains,
                               BlockingQueue<String> dictQueue,
                               BlockingQueue<String> outputQueue,
                               int threadNo) {
        this.threadNo = threadNo;
        this.dictQueue = dictQueue;
        this.outputQueue = outputQueue;
        this.rootDomains = rootDomains;
        stopflag= false;
    }

    public void stopThread() {
        stopflag = true;
    }

    @Override
    public void run() {
        while(true){
            try {
                if (dictQueue.isEmpty() || stopflag) {
                    //stdout.println("Producer break");
                    break;
                }
                String subdomainWord = dictQueue.take().trim().toLowerCase();

                for (String rootDomain: rootDomains){
                    String tmpDomain = subdomainWord+"."+rootDomain;
                    String ip = query(tmpDomain);
                    if(ip != null && !ThreadBruteDomain.badRecords.get(rootDomain).contains(ip)){
                        outputQueue.add(tmpDomain);
                        stdout.println("domain found by brute force ["+dictQueue.size()+" left] "+tmpDomain+" "+ip);
                    }
                }
            } catch (Exception error) {
                error.printStackTrace(stderr);
            }
        }
    }

    //simple query ,just get one ip
    public static String query(String domain){
        try {
            InetAddress inetAddress = InetAddress.getByName(domain);
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String args[]){
        System.out.println(query("xxx.jd.com"));
    }
}
