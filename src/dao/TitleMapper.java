package dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import org.springframework.jdbc.core.RowMapper;

import com.alibaba.fastjson.JSON;

import title.LineEntry;

public class TitleMapper implements RowMapper<LineEntry> {

	/**
	 * 	需要主动赋值的属性
	 * 	IP,CDN,icon_hash,ASNInfo
	 * 	CheckStatus,AssetType,EntryType,comment,isManualSaved
	 */
	@Override
	public LineEntry mapRow(ResultSet rs, int rowNum) throws SQLException {

		LineEntry entry = null;
		String type = rs.getString("EntryType"); //DNS or Web
		if (type.equalsIgnoreCase(LineEntry.EntryType_DNS)) {
			String host = rs.getString("host");
			String IPStr = rs.getString("IP");
			entry = new LineEntry(host,JSON.parseObject(IPStr,HashSet.class));//Set和String之间的互相转换
		}else {//EntryType_Web or Manual_Saved
			try {
				URL url = new URL(rs.getString("url"));
				byte[] request = rs.getBytes("request");
				byte[] response = rs.getBytes("request");
				entry = new LineEntry(url,request,response);
				
				entry.setIPSet(JSON.parseObject(rs.getString("IP"),HashSet.class));
				entry.setCNAMESet(JSON.parseObject(rs.getString("CNAME"),HashSet.class));
				entry.setIcon_hash(rs.getString("icon_hash"));
				entry.setASNInfo(rs.getString("ASNInfo"));
				entry.setCheckStatus(rs.getString("CheckStatus"));
				entry.setAssetType(rs.getString("AssetType"));
				entry.setEntryType(rs.getString("EntryType"));
				entry.setComments(JSON.parseObject(rs.getString("comment"),HashSet.class));
				//TODO 补齐字段
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return entry;
	}
}