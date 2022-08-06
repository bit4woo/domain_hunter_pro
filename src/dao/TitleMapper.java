package dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import title.LineEntry;

public class TitleMapper implements RowMapper<LineEntry> {

	@Override
	/**
	 * 	需要主动赋值的属性
	 * 	IP,CDN,icon_hash,ASNInfo
	 * 	CheckStatus,AssetType,EntryType,comment,isManualSaved
	 */
	public LineEntry mapRow(ResultSet rs, int rowNum) throws SQLException {

		LineEntry entry = null;
		String type = rs.getString("EntryType"); //DNS or Web
		if (type.equalsIgnoreCase(LineEntry.EntryType_DNS)) {
			String host = rs.getString("host");
			String IPStr = rs.getString("IP");
			entry = new LineEntry(host,IPStr);
		}else {//EntryType_Web or Manual_Saved
			try {
				URL url = new URL(rs.getString("url"));
				byte[] request = rs.getBytes("request");
				byte[] response = rs.getBytes("request");
				entry = new LineEntry(url,request,response);
				
				entry.setIP(rs.getString("IP"));
				entry.setCDN(rs.getString("CDN"));
				entry.setIcon_hash(rs.getString("icon_hash"));
				entry.setASNInfo(rs.getString("ASNInfo"));
				entry.setCheckStatus(rs.getString("CheckStatus"));
				entry.setAssetType(rs.getString("AssetType"));
				entry.setEntryType(rs.getString("EntryType"));
				entry.setComment(rs.getString("comment"));
				entry.setManualSaved(rs.getBoolean("isManualSaved"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return entry;
	}
}