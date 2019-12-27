package com.nisovin.shopkeepers.storage.sql.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.sql.SQL.Column;
import com.nisovin.shopkeepers.storage.sql.SQL.ForeignKey;
import com.nisovin.shopkeepers.storage.sql.SQL.Index;
import com.nisovin.shopkeepers.storage.sql.SQL.Table;
import com.nisovin.shopkeepers.storage.sql.SQLConnector;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SQLiteConnector extends SQLConnector {

	// TODO use BEGIN IMMEDIATE for read-then-write transactions? reduces the risk for in-transaction busy errors that
	// cause rollbacks and retries for already partially processed transactions
	// TODO when creating tables or adding new entries: in case of failure, check if entry has been added externally in
	// the meantime before retrying
	// TODO disable constraints/foreign keys during migrations
	// https://www.sqlite.org/pragma.html#pragma_defer_foreign_keys and PRAGMA foreign_keys = false;

	private final File databaseFile;

	public SQLiteConnector(String logPrefix, File databaseFile) {
		super(logPrefix, SQLite.INSTANCE);
		Validate.notNull(databaseFile, "Database file is null!");
		this.databaseFile = databaseFile;
	}

	@Override
	protected boolean supportsJDBC4() {
		return true;
	}

	@Override
	protected String getJDBCDriverClassName() {
		return "org.sqlite.JDBC";
	}

	@Override
	protected String getConnectionUrl() {
		return "jdbc:sqlite:" + databaseFile.getAbsolutePath();
	}

	@Override
	protected Properties getConnectionProperties() {
		Properties connectionProperties = super.getConnectionProperties();
		// driver-specific pragmas and settings; these override user-defined properties:
		// store DateTime/timestamps as human-readable text:
		connectionProperties.put("date_class", "TEXT"); // default is "integer"
		// default date_precision (milliseconds) and date_string_format ( "yyyy-MM-dd HH:mm:ss.SSS") are fine
		// TODO timestamps don't get stored as UTC yet.. -> should be fixed by now?
		return connectionProperties;
	}

	@Override
	protected void preConnect() throws Exception {
		super.preConnect();

		// create database file if it doesn't exist yet:
		if (!databaseFile.exists()) {
			try {
				databaseFile.getParentFile().mkdirs();
				databaseFile.createNewFile();
			} catch (IOException e) {
				throw new StorageException("Error during creation of database file!", e);
			}
		}
	}

	@Override
	protected void postConnect(Connection connection) throws Exception {
		super.postConnect(connection);

		try (Statement statement = connection.createStatement()) {
			// busy waiting time (ms):
			// not too large since queries are typically retried several times anyways
			statement.executeUpdate("PRAGMA busy_timeout = 1000;");
			// enable foreign keys:
			statement.executeUpdate("PRAGMA foreign_keys = ON;");
			// disable recursive triggers:
			statement.executeUpdate("PRAGMA recursive_triggers = OFF;");
		}
	}

	@Override
	public List<String> getTableNames(String tableNamePrefix) throws StorageException {
		String tablesSQL = "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE '" + tableNamePrefix + "%';";
		List<String> tables = new ArrayList<>();
		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(tablesSQL)) {
					while (resultSet.next()) {
						String tableName = resultSet.getString("name");
						if (tableName == null) continue; // just in case
						tables.add(tableName);
					}
				}
			}
			return tables;
		} catch (Exception e) {
			throw new StorageException("Could not get table names!", e);
		}
	}

	@Override
	public Table getTable(String tableName) throws StorageException {
		// | type | name | tbl_name | rootpage | sql |
		String autoIncrementSQL = "SELECT \"is-autoincrement\" FROM sqlite_master WHERE tbl_name=\""
				+ tableName + "\" AND sql LIKE \"%" + sql.autoIncrement() + "%\";";

		// | cid | name | type | notnull | dflt_value | pk |
		String tableInfoSQL = "PRAGMA table_info(" + sql.quoteId(tableName) + ");";

		// | id | seq | table | from | to | on_update | on_delete | match |
		String foreignKeysSQL = "PRAGMA foreign_key_list(" + sql.quoteId(tableName) + ");";

		Table table = sql.table(tableName);

		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				// check if there is an auto increment column:
				boolean hasAutoIncrement = false;
				try (ResultSet resultSet = statement.executeQuery(autoIncrementSQL)) {
					if (resultSet.next()) {
						hasAutoIncrement = true;
					}
				}

				// columns:
				boolean hasColumns = false;
				try (ResultSet resultSet = statement.executeQuery(tableInfoSQL)) {
					while (resultSet.next()) {
						String columnName = resultSet.getString("name");
						String type = resultSet.getString("type");
						boolean notNull = resultSet.getBoolean("notnull");
						String defaultValue = resultSet.getString("dflt_value");
						boolean primaryKey = resultSet.getBoolean("pk");
						// only the primary key can be auto increment:
						boolean autoIncrement = primaryKey && hasAutoIncrement;

						Column column = table.column(columnName).type(type).defaultValue(defaultValue);
						if (primaryKey) column.primaryKey();
						if (autoIncrement) column.autoIncrement();
						if (notNull) column.notNull();
						hasColumns = true;
					}
				}
				if (!hasColumns) {
					return null; // no columns: table doesn't exist
				}

				// foreign keys:
				try (ResultSet resultSet = statement.executeQuery(foreignKeysSQL)) {
					while (resultSet.next()) {
						String referencedTable = resultSet.getString("table");
						String column = resultSet.getString("from");
						String referencedColumn = resultSet.getString("to");
						boolean cascadeDelete = "CASCADE".equals(resultSet.getString("on_delete"));

						ForeignKey foreignKey = table.foreignKey().column(column).referencedTable(referencedTable).referencedColumn(referencedColumn);
						if (cascadeDelete) foreignKey.cascadeDelete();
					}
				}
			}
			return table;
		} catch (Exception e) {
			throw new StorageException("Could not get table information!", e);
		}
	}

	@Override
	public List<Index> getIndices(String tableName) throws StorageException {
		// | seq | name | unique | origin | partial |
		String indexListSQL = "PRAGMA index_list(" + sql.quoteId(tableName) + ");";

		// | seqno | cid | name |
		String indexInfoSQLTemplate = "PRAGMA index_info({index});";

		List<Index> indices = new ArrayList<>();

		try {
			Connection connection = this.getConnection();
			try (	Statement statement = connection.createStatement();
					Statement columnsStmt = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(indexListSQL)) {
					while (resultSet.next()) {
						// 'c' indicates created via "CREATE INDEX"
						boolean explicitlyCreated = "c".equals(resultSet.getString("origin"));
						if (!explicitlyCreated) continue; // skip

						Index index = sql.index().table(tableName);

						String indexName = resultSet.getString("name");
						index.name(indexName);

						boolean unique = resultSet.getBoolean("unique");
						if (unique) index.unique();

						// columns:
						boolean hasColumns = false;
						String indexInfoSQL = StringUtils.replaceFirst(indexInfoSQLTemplate, "{index}", sql.quoteId(indexName));
						try (ResultSet columnsRS = columnsStmt.executeQuery(indexInfoSQL)) {
							while (columnsRS.next()) {
								String columnName = columnsRS.getString("name");
								index.column(columnName);
								hasColumns = true;
							}
						}
						if (!hasColumns) {
							// index without columns? should not happen..
							continue;
						}
						indices.add(index);
					}
				}
			}
			return indices;
		} catch (Exception e) {
			throw new StorageException("Could not get indices information!", e);
		}
	}
}
