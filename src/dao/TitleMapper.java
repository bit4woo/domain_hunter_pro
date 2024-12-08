package dao;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import base.SetAndStr;
import title.LineEntry;

public class TitleMapper implements RowMapper<LineEntry> {

	/**
	 * 	需要主动赋值的属性
	 * 	IP,CDN,icon_hash,ASNInfo
	 * 	CheckStatus,AssetType,EntryType,comment,isManualSaved
	 *
	 *
	 */
	@Override
	public LineEntry mapRow(ResultSet rs, int rowNum) throws SQLException {

		LineEntry entry = null;
		String type = rs.getString("EntryType"); //DNS or Web
		if (type.equalsIgnoreCase(LineEntry.EntryType_DNS)) {
			String host = rs.getString("host");
			String IPStr = rs.getString("IPSet");
			entry = new LineEntry(host,SetAndStr.toSet(IPStr));//Set和String之间的互相转换
		}else {
			try {
				URL url = new URL(rs.getString("url"));
				byte[] request = rs.getBytes("request");
				byte[] response = rs.getBytes("response");
				entry = new LineEntry(url,request,response);
				
				entry.setIPSet(SetAndStr.toSet(rs.getString("IPSet")));
				entry.setCNAMESet(SetAndStr.toSet(rs.getString("CNAMESet")));
				entry.setCertDomainSet(SetAndStr.toSet(rs.getString("CertDomainSet")));
				entry.setIcon_hash(rs.getString("icon_hash"));
				entry.setASNInfo(rs.getString("ASNInfo"));
				entry.setCheckStatus(rs.getString("CheckStatus"));
				entry.setAssetType(rs.getString("AssetType"));
				entry.setEntryType(rs.getString("EntryType"));
				entry.setEntrySource(rs.getString("EntrySource"));
				entry.setComments(SetAndStr.toSet(rs.getString("comments")));
				entry.setTime(rs.getString("time"));
				entry.setEntryTags(SetAndStr.toSet(rs.getString("EntryTags")));
				entry.setIcon_bytes(rs.getBytes("icon_bytes"));
				entry.setIcon_url(rs.getString("icon_url"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return entry;
	}
}