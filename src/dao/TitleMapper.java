package dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import burp.SetAndStr;
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
			entry = new LineEntry(host,SetAndStr.toSet(IPStr));//Set和String之间的互相转换
		}else {
			try {
				URL url = new URL(rs.getString("url"));
				byte[] request = rs.getBytes("request");
				byte[] response = rs.getBytes("request");
				entry = new LineEntry(url,request,response);
				
				entry.setIPSet(SetAndStr.toSet(rs.getString("IP")));
				entry.setCNAMESet(SetAndStr.toSet(rs.getString("CNAME")));
				entry.setIcon_hash(rs.getString("icon_hash"));
				entry.setASNInfo(rs.getString("ASNInfo"));
				entry.setCheckStatus(rs.getString("CheckStatus"));
				entry.setAssetType(rs.getString("AssetType"));
				entry.setEntryType(rs.getString("EntryType"));
				entry.setComments(SetAndStr.toSet(rs.getString("comment")));
				//TODO 补齐字段
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return entry;
	}
}