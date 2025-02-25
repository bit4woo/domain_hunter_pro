package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class TableUpdater {

	// 通用的字段数据迁移方法
	public static String genMigrateDataSql(String oldTableName, String newTableName, Map<String, String> columnMappings) {
		// 生成插入数据的 SQL
		StringBuilder insertSql = new StringBuilder("INSERT OR IGNORE INTO " + newTableName + " (");
		StringBuilder selectSql = new StringBuilder("SELECT ");

		// 构造字段映射和数据插入 SQL
		for (Map.Entry<String, String> entry : columnMappings.entrySet()) {
			insertSql.append(entry.getKey()).append(", ");
			selectSql.append(entry.getValue()).append(", ");
		}

		// 移除最后多余的逗号
		insertSql.setLength(insertSql.length() - 2);
		selectSql.setLength(selectSql.length() - 2);

		// 完成 SQL
		insertSql.append(") ").append(selectSql).append(" FROM " + oldTableName);
		System.out.println("Generated Data Transfer SQL: " + insertSql.toString());
		
		return insertSql.toString();
	}

	public static boolean tableExists(JdbcTemplate jdbcTemplate,String tableName) {
		try {
			String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = '" + tableName + "'";
			SqlRowSet result = jdbcTemplate.queryForRowSet(sql);
			if (result.next() && result.getInt(1) > 0) {
				return true;
			} else {
				return false;
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 检查列是否存在
	private static boolean columnExists(JdbcTemplate jdbcTemplate,String tableName, String columnName) {
		List<String> columns = getTableColumns(jdbcTemplate,tableName);
		return columns.contains(columnName);
	}

	// 获取当前表的所有列
	public static List<String> getTableColumns(JdbcTemplate jdbcTemplate,String tableName) {
		String query = "PRAGMA table_info(" + tableName + ")";
		List<String> columns = new ArrayList<>();

		try {
			columns = jdbcTemplate.query(query, new RowMapper<String>() {
				@Override
				public String mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getString("name"); // Extract column name
				}
			});
		} catch (DataAccessException e) {
			e.printStackTrace();
		}

		return columns;
	}

	// 提取字段名的函数
	public static List<String> extractColumnNames(String createTableSql) {
		List<String> columnNames = new ArrayList<>();

		// 去掉 CREATE TABLE 和表名部分（找到第一个 '(' 出现的位置）
		int startIndex = createTableSql.indexOf('(');
		int endIndex = createTableSql.indexOf(')');

		if (startIndex != -1 && endIndex != -1) {
			// 提取括号内的列定义部分
			String columnsPart = createTableSql.substring(startIndex + 1, endIndex).trim();

			// 按逗号分割每个列定义
			String[] columnDefinitions = columnsPart.split(",");

			for (String columnDefinition : columnDefinitions) {
				columnDefinition = columnDefinition.trim(); // 去掉左右空白字符

				// 跳过包含约束的列定义（例如 PRIMARY KEY, FOREIGN KEY）
				if (columnDefinition.toUpperCase().contains("PRIMARY KEY") || columnDefinition.toUpperCase().contains("FOREIGN KEY")) {
					continue;
				}

				// 提取列名，列名通常是定义的第一个部分
				String[] parts = columnDefinition.split("\\s+");
				if (parts.length > 0) {
					columnNames.add(parts[0]);
				}
			}
		}

		return columnNames;
	}


	public static List<String> getAddedColumnNames(List<String> oldTableColumnNames, List<String> newTableColumnNames) {

		List<String> addedColumnNames = new ArrayList<String>();
		for (String field : newTableColumnNames) {
			if (!oldTableColumnNames.contains(field)) {
				addedColumnNames.add(field);
			}
		}
		return addedColumnNames;
	}

	// 创建表结构和字段迁移。各个表各自实现.这是一个样例
	public Map<String, String> buildColumnMappings(List<String> oldTableColumnNames, List<String> newTableColumnNames) {

		// 构建字段映射
		Map<String, String> columnMappings = new HashMap<>();

		for (String columnNameOfNewTable : newTableColumnNames) {
			if (oldTableColumnNames.contains(columnNameOfNewTable)) {
				columnMappings.put(columnNameOfNewTable, columnNameOfNewTable);
			} else {

				// 第一次修改：删除isBlack字段，新增trustLevel字段的逻辑。需要确保旧表有isBlack字段。
				if (columnNameOfNewTable.equals("trustLevel")) {
					if (oldTableColumnNames.contains(columnNameOfNewTable)) {
						// 特殊处理 "isBlack" 字段 -> 映射到 "trustLevel"
						columnMappings.put("trustLevel",
								"CASE WHEN isBlack = 1 THEN 'NonTarget' ELSE 'Maybe' END as trustLevel");
					} else {

						columnMappings.put("trustLevel", "'Maybe' as trustLevel");
					}

				} else if (columnNameOfNewTable.equals("subDomainCount")) {

					columnMappings.put("subDomainCount", "0 as subDomainCount");

				} else if (columnNameOfNewTable.equals("digDone")) {

					columnMappings.put("digDone", "false as digDone");

				} else {
					System.out.println(columnNameOfNewTable + " mapping logic missed!!!");
				}
			}
		}

		return columnMappings;

	}

	public static void main(String[] args) {
		String create_sql = TargetDao.genCreateTableSql("TargetTable_new");
		List<String> fieldNames = extractColumnNames(create_sql);

		DataSource dataSource = DBUtils.getSqliteDataSource("test.db");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		List<String> fieldNames_current = TableUpdater.getTableColumns(jdbcTemplate,"TargetTable");

		List<String> added = new ArrayList<String>();
		for (String field : fieldNames) {
			if (!fieldNames_current.contains(field)) {
				added.add(field);
			}
		}
	}

}
