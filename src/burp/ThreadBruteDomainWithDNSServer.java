package burp;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//////////////////ThreadGetTitle block/////////////
//no need to pass BurpExtender object to these class, IBurpExtenderCallbacks object is enough 
public class ThreadBruteDomainWithDNSServer{
    private Set<String> rootDomains = new HashSet<>();
    private List<DomainBruteProducerWithDNSServer> plist;

    private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

    public ThreadBruteDomainWithDNSServer(Set<String> rootDomains) {
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

        Set<RootDomainForBrute> rds = new HashSet<>();
        for (String rootDomain: rootDomains){
            RootDomainForBrute rd= new RootDomainForBrute(rootDomain);
            rds.add(rd);
        }



        stdout.println(rootDomains);

        if (rootDomains.size() <=0) return;

        BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();//use to store rootDomains
        BlockingQueue<String> dictQueue = new LinkedBlockingQueue<String>();//store dict

        dictQueue.addAll(readDictFile());

        plist = new ArrayList<DomainBruteProducerWithDNSServer>();

        for (int i=0;i<=10000;i++) {
            DomainBruteProducerWithDNSServer p = new DomainBruteProducerWithDNSServer(rds,dictQueue,outputQueue,i);
            p.start();
            plist.add(p);
        }

        int i =1;
        while(true) {//to wait all threads exit.
//            if (DomainBruteProducer.originSize-dictQueue.size() >= 10000*i && DomainBruteProducer.originSize-dictQueue.size() <= 10000*i+5){
//                i++;
//                DomainPanel.getDomainResult().getSubDomainSet().addAll(outputQueue);
//            }//尝试阶段性保存

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
        for (DomainBruteProducerWithDNSServer p:plist) {
            if(p.isAlive()) {
                return false;
            }
        }
        return true;
    }

    public void stopThreads() {
        for (DomainBruteProducerWithDNSServer p:plist) {
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

class DomainBruteProducerWithDNSServer extends Thread {//Producer do
    private Set<RootDomainForBrute> rootDomainForBrutes;
    private BlockingQueue<String> dictQueue;
    private BlockingQueue<String> outputQueue;

    private int threadNo;
    private boolean stopflag = false;

    private static IBurpExtenderCallbacks callbacks = BurpExtender.getCallbacks();//静态变量，burp插件的逻辑中，是可以保证它被初始化的。;
    public PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
    public PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
    public IExtensionHelpers helpers = callbacks.getHelpers();

    public DomainBruteProducerWithDNSServer(Set<RootDomainForBrute> rootDomainForBrutes,
                                            BlockingQueue<String> dictQueue,
                                            BlockingQueue<String> outputQueue,
                                            int threadNo) {
        this.threadNo = threadNo;
        this.dictQueue = dictQueue;
        this.outputQueue = outputQueue;
        this.rootDomainForBrutes = rootDomainForBrutes;
        stopflag= false;

    }

    public void stopThread() {
        stopflag = true;
    }

    @Override
    public void run() {
        while(true) try {
            if (dictQueue.isEmpty() || stopflag) {
                //stdout.println("Producer break");
                break;
            }
            String subdomainWord = dictQueue.take().trim().toLowerCase();

            for (RootDomainForBrute item : rootDomainForBrutes) {
                String tmpDomain = subdomainWord + "." + item.rootDomain;
                //stdout.println(tmpDomain);
                HashMap<String, Set<String>> result = item.query(tmpDomain);
                Set<String> CDNSet = result.get("CDN");
                Set<String> IPset = result.get("IP");
                boolean successfulflag = false;

                //根据CDN进行判断是否为泛解析域名
                if (CDNSet != null && CDNSet.size() != 0 && !CDNSet.toString().equalsIgnoreCase(item.wildCDNSet.toString())) {
                    successfulflag = true;
                } else if (IPset != null && IPset.size() != 0) {//根据IP集合进行判断是否为泛解析域名
                    Set<String> tmpSet = new HashSet<>();
                    tmpSet.addAll(IPset);
                    tmpSet.retainAll(item.wildIPset);
                    if (tmpSet.size() != IPset.size()) {
                        successfulflag = true;
                    }
                }

                if (successfulflag){
                    outputQueue.add(tmpDomain);
                    DecimalFormat df=(DecimalFormat) DecimalFormat.getInstance();
                    df.setGroupingSize(4);
                    stdout.println("domain found by brute force [" + df.format(dictQueue.size()) + " left] "
                            + tmpDomain + " " + IPset.toString() + " " + CDNSet.toString());
                }
            }
        } catch (Exception error) {
            error.printStackTrace(stderr);
        }
    }

    //简单的查询方法，只获取IP
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
//        System.out.println(Commons.dnsquery("xxxsdfdsfasf.jr.jd.com","223.5.5.5"));
//        System.out.println(query("xxxsdfdsfasf.jr.jd.com"));
    }
}

