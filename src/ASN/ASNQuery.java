package ASN;

import burp.IPAddressUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class ASNQuery {
    public static final String localdir =
            System.getProperty("user.home")+File.separator+".domainhunter";
    public static final String ASNFileName = "ASNInfo.json";
    public static final File localFile = new File(localdir+File.separator+ ASNFileName);

    static HashMap<String,ASNEntry> entries = new HashMap<>();
    private static boolean localDataChanged = true;

    public static HashMap<String, ASNEntry> getEntries() {
        return entries;
    }

    public static void setEntries(HashMap<String, ASNEntry> entries) {
        ASNQuery.entries = entries;
    }

    /**
     *
     */
    private static ASNEntry queryFromApi(String IP){
        String queryByIp = "https://api.shadowserver.org/net/asn?origin="+IP;
        String resp = HttpRequest.get(queryByIp).body();
        List<ASNEntry> tmpEntries = JSON.parseArray(resp,ASNEntry.class);
        //List的大小应该是固定的1，可能不能大于一。因为一个IP不可能属于多个ASN。
        for (ASNEntry entry:tmpEntries){
            entries.put(entry.getAsn(),entry);
            //System.out.println(entry.toString());
        }
        saveToFile();
        return tmpEntries.get(0);
        //https://www.peeringdb.com/api/net/3554
        //https://api.asrank.caida.org/v2/restful/asns/714
        //https://api.ipdata.co/17.253.144.10/asn?api-key=test
    }

    private static boolean saveToFile() {
        try {
            String tmp = JSON.toJSONString(entries);
            FileUtils.writeStringToFile(localFile, tmp,StandardCharsets.UTF_8);
            localDataChanged = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean loadFromFile(){
        try {
            String tmp = FileUtils.readFileToString(localFile, StandardCharsets.UTF_8);
            entries = JSON.parseObject(tmp, new TypeReference<HashMap<String,ASNEntry>>(){});
            localDataChanged = false;//内存中和文件中内容一致了，重置变量标志
            return true;
        }catch (FileNotFoundException e){
            //
            return true;
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 单独查询
     * @param singleIP
     */
    private static ASNEntry queryFromLocal(String singleIP){
        if (localDataChanged){
            loadFromFile();
        }
        for (Object tmp:entries.values()){
            ASNEntry entry = (ASNEntry) tmp;
            if (entry.contains(singleIP)){
                return entry;
            }
        }
        return null;
    }

    /**
     * 可以用于批量更新本地数据库
     * @param ipSet
     */
    public static void batchQueryFromApi(List<String> ipSet){
        if (ipSet.size()>=1000){
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
            ASNEntry result = queryFromLocal(singleIP);
            if (null == result){
                result = queryFromApi(singleIP);
            }
            return result;
        }else {
            throw new IllegalArgumentException();
        }
    }

    public static void main(String[] args) {
        System.out.println(query("2.2.2.2"));
    }
}