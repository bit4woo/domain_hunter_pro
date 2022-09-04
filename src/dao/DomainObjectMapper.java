package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArraySet;

import com.alibaba.fastjson.JSON;
import org.springframework.jdbc.core.RowMapper;

public class DomainObjectMapper implements RowMapper<CopyOnWriteArraySet<String>> {

	public CopyOnWriteArraySet<String> mapRow(ResultSet resultSet, int i) throws SQLException {
		return JSON.parseObject(resultSet.getString("Content"), CopyOnWriteArraySet.class);
		//return DomainManager.FromJson(resultSet.getString("Content"));
		//return JSON.parseObject(resultSet.getString("Content"), DomainManager.class);
	}
}