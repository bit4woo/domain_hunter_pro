package ASN;

import burp.BurpExtender;
import burp.IPAddressUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ASNQuery {
    public static final String localdir =
            System.getProperty("user.home")+File.separator+".domainhunter";
    public static final String ASNFileName = "ASNInfo.json";
    public static final File localFile = new File(localdir+File.separator+ ASNFileName);

    static ConcurrentHashMap<String,ASNEntry> entriesFromTSV = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String,ASNEntry> entriesRecentUsed = new ConcurrentHashMap<>();//当做缓存使用
    private static boolean recentDataChanged = true;

    public static ConcurrentHashMap<String, ASNEntry> getEntriesRecentUsed() {
        return entriesRecentUsed;
    }

    public static void setEntriesRecentUsed(ConcurrentHashMap<String, ASNEntry> entriesRecentUsed) {
        ASNQuery.entriesRecentUsed = entriesRecentUsed;
    }

    /**
     *
     */
    private static ASNEntry queryFromApi(String IP){
        try {
            //https://api.shadowserver.org/net/asn?origin=116.198.3.100/24
            //https://api.shadowserver.org/net/asn?origin=116.198.3.100,8.8.8.8

            String queryByIp = "https://api.shadowserver.org/net/asn?origin="+IP;
            String resp = HttpRequest.get(queryByIp).body();
            if (resp.contains("Request forbidden by administrative rules.")){
                return  null;
            }
            List<ASNEntry> tmpEntries = JSON.parseArray(resp,ASNEntry.class);
            //List的大小应该是固定的1，可能不能大于一。因为一个IP不可能属于多个ASN。
            for (ASNEntry entry:tmpEntries){
                entriesRecentUsed.put(entry.getAsn()+entry.getPrefix(),entry);
                //System.out.println(entry.toString());
            }
            saveRecentToFile();
            return tmpEntries.get(0);
        } catch (HttpRequest.HttpRequestException e) {
            e.printStackTrace();
            return null;
        }
        //https://www.peeringdb.com/api/net/3554
        //https://api.asrank.caida.org/v2/restful/asns/714
        //https://api.ipdata.co/17.253.144.10/asn?api-key=test
    }

    public static boolean saveRecentToFile() {
        try {
            String tmp = JSON.toJSONString(entriesRecentUsed);
            FileUtils.writeStringToFile(localFile, tmp,StandardCharsets.UTF_8);
            recentDataChanged = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     */
    private static boolean loadRecentFromFile(){
        if (recentDataChanged){
            try {
                String tmp = FileUtils.readFileToString(localFile, StandardCharsets.UTF_8);
                entriesRecentUsed = JSON.parseObject(tmp, new TypeReference<ConcurrentHashMap<String,ASNEntry>>(){});
                recentDataChanged = false;//内存中和文件中内容一致了，重置变量标志
                return true;
            }catch (FileNotFoundException e){
                //当第一次加载是，还没没有文件，使用默认值，依然返回true。
                return true;
            }catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 从TSV文件加载数据
     * range_start range_end AS_number country_code AS_description
     *
     * 注意：ASN变化不应该成为唯一ID值，因为一个ASN可能有多个网段！而数据库是以网段为主体的
     * @return
     */
    private static void loadTsvFile(){
        if (entriesFromTSV.size() == 0){//无需重复加载
            System.out.println("loading ip2asn-v4.tsv");
            List<String> lines = readFile("ip2asn-v4.tsv");
            for (String line:lines){
                try {
                    ASNEntry tmp = new ASNEntry(line);
                    /**
                    if (tmp.getNumberOfIP() <255){
                        System.out.println(tmp.toString());
                    }*/
                    entriesFromTSV.put(tmp.getAsn()+tmp.getPrefix(),tmp);//注意key值！
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("ip2asn-v4.tsv loaded");
        }
    }

    public static List<String> readFile(String filename) {
        try {
            URL url = BurpExtender.class.getClassLoader().getResource(filename);
            File copyFile = new File(FileUtils.getTempDirectory()+File.pathSeparator+"."+filename);
            copyFile.deleteOnExit();
            if (!copyFile.exists()){//TODO文件更新后需要删除这个
                FileUtils.copyURLToFile(url,copyFile);
            }
            List<String> dictList = FileUtils.readLines(copyFile, StandardCharsets.UTF_8);
            return dictList;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<String>();
        }
    }

    /**
     * 单独查询
     * @param singleIP
     */
    private static ASNEntry queryFromRecent(String singleIP){
        loadRecentFromFile();
        for (ASNEntry entry:entriesRecentUsed.values()){
            if (entry.contains(singleIP)){
                return entry;
            }
        }
        return null;
    }


    /**
     * https://iptoasn.com/
     * https://iptoasn.com/data/ip2asn-v4.tsv.gz
     * 查询来自以上链接的数据
     * @param singleIP
     */
    private static ASNEntry queryFromTsvFile(String singleIP){
        loadTsvFile();
        for (ASNEntry entry:entriesFromTSV.values()){
            if (entry.contains(singleIP)){
                entriesRecentUsed.put(entry.getAsn()+entry.getPrefix(),entry);
                saveRecentToFile();
                return entry;
            }
        }
        return null;
    }

    /**
     * 可以用于批量更新本地数据库
     * @param ipSet
     */
    @Deprecated
    private static void batchQueryFromApi(List<String> ipSet){
        if (ipSet.size()>1000){
            System.out.println("too many IP address to query! should less than 1000");
        }else {
            String tmpStr = String.join(",",ipSet);
            queryFromApi(tmpStr);
        }
    }

    /**
     * 1、先本地查询，如果没有记录。则转为网络查询
     * 2、网络查询，同时更新本地记录。
     * @return
     */
    public static ASNEntry query(String singleIP){
        if (IPAddressUtils.isValidIP(singleIP)){
            //1.从缓存查询
            ASNEntry result = queryFromRecent(singleIP);
            //2.从本地数据库文件查询
            if (null == result) {
                try {
                    result = queryFromTsvFile(singleIP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //3.从API接口查询
            if (null == result){
                try {
                    result = queryFromApi(singleIP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }else {
            throw new IllegalArgumentException(singleIP);
        }
    }

    public static void main(String[] args) {
//        System.out.println(query("2.2.2.2"));
//        System.out.println(queryFromApi("2.2.2.2"));
        loadTsvFile();
        //System.out.println(queryFromTsvFile("8.8.8.8"));
        //ASNEntry tmp = new ASNEntry("8.8.8.0\t8.8.8.255\t15169\tUS\tGOOGLE - Google LLC");
        //System.out.println(tmp.contains("8.8.8.8"));
    }
}