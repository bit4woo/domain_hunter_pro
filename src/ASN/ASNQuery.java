package ASN;

import com.alibaba.fastjson.JSON;
import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.io.FileUtils;
import title.IndexedLinkedHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ASNQuery {
    public static final String localdir =
            System.getProperty("user.home")+File.separator+".domainhunter";
    public static final String ASNFileName = "ASNInfo.json";
    public static final File localFile = new File(localdir+File.separator+ ASNFileName);
    static IndexedLinkedHashMap<String,ASNEntry> entries = new IndexedLinkedHashMap<>();
    /**
     *
     */
    public static void queryFromApi(String IP){
        String queryByIp = "https://api.shadowserver.org/net/asn?origin="+IP;
        String resp = HttpRequest.get(queryByIp).body();
        List<ASNEntry> tmpEntries = JSON.parseArray(resp,ASNEntry.class);
        for (ASNEntry entry:tmpEntries){
            entries.put(entry.getAsn(),entry);
            System.out.println(entry.toString());
        }
        saveToFile();

        //https://www.peeringdb.com/api/net/3554
        //https://api.asrank.caida.org/v2/restful/asns/714
        //https://api.ipdata.co/17.253.144.10/asn?api-key=test
    }

    static boolean saveToFile() {
        try {
            String tmp = JSON.toJSONString(entries);
            FileUtils.writeStringToFile(localFile, tmp,StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    boolean loadFromFile(){
        try {
            String tmp = FileUtils.readFileToString(localFile, StandardCharsets.UTF_8);
            entries = (IndexedLinkedHashMap<String, ASNEntry>) JSON.parse(tmp);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void queryFromLocal(){
        String tmp = JSON.toJSONString(entries);
        loadFromFile();//TODO
        System.out.println(tmp);
        JSON.parse(tmp);
        System.out.println(tmp);
    }

    //
    public static void batchQueryFromApi(Set<String> ipSet){
        if (ipSet.size()>=1000){
            System.out.println("too many IP address to query! should less than 1000");
        }else {
            String tmpStr = String.join(",",ipSet);
            queryFromApi(tmpStr);
        }
    }

    public static void main(String[] args) {
        queryFromApi("8.8.8.8,143.92.69.133");
        queryFromLocal();
    }
}