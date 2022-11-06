package ASN;

//[{"prefix":"8.8.8.0/24","geo":"US","ip":"8.8.8.8","asname_short":"AS15169","asn":"15169","asname_long":"GOOGLE LLC"}]

import burp.IPAddressUtils;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;

import java.util.List;

/**
 *存储
 */
public class ASNEntry {

    String asn = "";//自治系统编号
    String asname_long = "";
    String asname_short = "";
    String prefix = "";//网段信息
    String geo = "";
    String alias = "";

    /**
     * 用于fastjson反序列化
     */
    ASNEntry(){}


    /**
     * 从TSV文件加载数据
     *      * range_start range_end AS_number country_code AS_description
     * @param lineFromTSV
     */
    public ASNEntry(String lineFromTSV){
        try {
            String[] items = lineFromTSV.split("\t");
            if (items.length !=5){
                throw new IllegalArgumentException("data illegal: "+lineFromTSV);
            }
            prefix = items[0]+"-"+items[1];
            asn = items[2];
            geo = items[3];
            asname_long = items[4];
            if (asname_long.equals("Not routed")){
                throw new IllegalArgumentException("Not routed ASN");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public String getAsname_long() {
        return asname_long;
    }

    public void setAsname_long(String asname_long) {
        this.asname_long = asname_long;
    }

    public String getAsname_short() {
        return asname_short;
    }

    public void setAsname_short(String asname_short) {
        this.asname_short = asname_short;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * ASN描述，主要用于用户查看
     * @return
     * get开头的函数会被序列化过程调用
     */
    public String fetchASNDescription() {
        if (!getAlias().equals("")){
            return getAlias();
        }else{
            return getAsname_long();
        }
    }

    public boolean contains(String IP){
        if (prefix.contains("/")){
            List<String> IPSet = IPAddressUtils.toIPList(prefix);
            return IPSet.contains(IP);
        }

        if (prefix.contains("-")){//这个速度更快，减少了查找时间
            try {
                String start = prefix.split("-")[0];
                String end = prefix.split("-")[1];
                return IPAddressUtils.checkIPIsInGivenRange(IP,start,end);//
            } catch (AddressStringException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    /**
     * get开头的函数会被序列化过程调用
     */
    public int FetchumberOfIP(){
        String start = prefix.split("-")[0];
        String end = prefix.split("-")[1];
        IPAddress startIPAddress = new IPAddressString(start).getAddress();
        IPAddress endIPAddress = new IPAddressString(end).getAddress();
        IPAddressSeqRange ipRange = startIPAddress.toSequentialRange(endIPAddress);
        return ipRange.getCount().intValue();
    }

    /**
     * 对比是否相同的asn，对比编号即可
     * @param entry
     * @return
     */
    public boolean equals(ASNEntry entry){
        return this.toString().equals(entry.toString());
    }

    @Override
    public String toString() {
        return "ASNEntry{" +
                "asn='" + asn + '\'' +
                ", asname_long='" + asname_long + '\'' +
                ", asname_short='" + asname_short + '\'' +
                ", prefix='" + prefix + '\'' +
                ", geo='" + geo + '\'' +
                '}';
    }
}
