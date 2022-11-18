package dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

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
		entry.setBlack(rs.getBoolean("isBlack"));
		entry.setComment(rs.getString("comment"));
		entry.setUseTLD(rs.getBoolean("useTLD"));
		return entry;
	}
}