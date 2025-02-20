package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;

import org.springframework.jdbc.core.RowMapper;

import base.SetAndStr;
import domain.target.AssetTrustLevel;
import domain.target.TargetEntry;

public class TargetMapper implements RowMapper<TargetEntry> {

	@Override
	/**
	 * 	target,type,keyword,ZoneTransfer,isBlack,comment,useTLD
	 */
	public TargetEntry mapRow(ResultSet rs, int rowNum) throws SQLException {

		TargetEntry entry = new TargetEntry();

		//entry.setID(rs.getInt("ID"));
		entry.setTarget(rs.getString("target"));
		entry.setType(rs.getString("type"));
		entry.setKeyword(rs.getString("keyword"));
		entry.setZoneTransfer(rs.getBoolean("ZoneTransfer"));
		//entry.setBlack(rs.getBoolean("isBlack"));//反序列化时不再处理该字段。
		if (rs.getString("trustLevel")!=null) {
			entry.setTrustLevel(rs.getString("trustLevel"));
		}else {
			//兼容旧版本数据库
			entry.setTrustLevel(AssetTrustLevel.Maybe);
		}
		
		if (rs.getString("comment").startsWith("[") && rs.getString("comment").endsWith("]")) {
			//新的代码，setToStr的存储格式
			entry.setComments(SetAndStr.toSet(rs.getString("comment")));
		}else {
			//兼容以前的string格式存储，即单纯逗号分割
			entry.setComments(new HashSet<String>(Arrays.asList(rs.getString("comment").split(","))));
		}
		
		entry.setUseTLD(rs.getBoolean("useTLD"));
		entry.setSubdomainCount(rs.getInt("subDomainCount"));
		return entry;
	}
}