package ASN;

//[{"prefix":"8.8.8.0/24","geo":"US","ip":"8.8.8.8","asname_short":"AS15169","asn":"15169","asname_long":"GOOGLE LLC"}]

import burp.IPAddressUtils;
import com.alibaba.fastjson.JSON;

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

    public boolean contains(String IP){
        List<String> IPSet = IPAddressUtils.toIPList(prefix);
        return IPSet.contains(IP);
    }

    /**
     * 对比是否相同的asn，对比编号即可
     * @param entry
     * @return
     */
    public boolean equals(ASNEntry entry){
        return asn.equals(entry.getAsn());
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
