package dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import domain.DomainManager;

public class DomainObjectMapper implements RowMapper<DomainManager> {

	public DomainManager mapRow(ResultSet resultSet, int i) throws SQLException {
		return DomainManager.FromJson(resultSet.getString("Content"));
		//return JSON.parseObject(resultSet.getString("Content"), DomainManager.class);
	}
}