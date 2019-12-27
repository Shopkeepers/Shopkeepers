package com.nisovin.shopkeepers.storage.sql.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.sql.SQL.Column;
import com.nisovin.shopkeepers.storage.sql.SQL.CreatableDBObject;
import com.nisovin.shopkeepers.storage.sql.SQL.ForeignKey;
import com.nisovin.shopkeepers.storage.sql.SQL.Index;
import com.nisovin.shopkeepers.storage.sql.SQL.Table;
import com.nisovin.shopkeepers.storage.sql.SQL.Trigger;
import com.nisovin.shopkeepers.storage.sql.SQLConnector;
import com.nisovin.shopkeepers.util.VoidCallable;

public class MySQLConnector extends SQLConnector {

	private final String host;
	private final String port;
	private final String database;
	private final String user;
	private final String password;

	public MySQLConnector(String logPrefix, String host, String port, String database, String user, String password) {
		super(logPrefix, MySQL.INSTANCE);
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
	}

	@Override
	protected boolean supportsJDBC4() {
		return true;
	}

	@Override
	protected String getJDBCDriverClassName() {
		return "com.mysql.jdbc.Driver";
	}

	@Override
	protected String getConnectionUrl() {
		return "jdbc:mysql://" + host + ":" + port + "/" + database;
	}

	@Override
	protected Properties getConnectionProperties() {
		// TODO test this..
		// character_set_server=utf8mb4&collation-server=utf8mb4_bin
		// Use "SET NAMES 'utf-8'" and "SET CHARACTER SET 'utf-8'"? Or is this handled by the driver already?
		// Use autoReconnect=true ?
		Properties connectionProperties = super.getConnectionProperties();
		// these override user-defined properties:
		connectionProperties.put("user", user);
		connectionProperties.put("password", password);
		connectionProperties.put("useUnicode", true);
		connectionProperties.put("characterEncoding", "UTF-8");
		connectionProperties.put("connectionCollation", "utf8mb4_bin");
		return connectionProperties;
	}

	@Override
	protected void postConnect(Connection connection) throws Exception {
		super.postConnect(connection);

		try (Statement statement = connection.createStatement()) {
			// ensure safe-mode is disabled (limits certain queries):
			statement.executeUpdate("SET SQL_SAFE_UPDATES = 0;");
		}
	}

	// existsSQL creates a boolean column "exists"
	protected void _createDBObjectCheckExists(String objectTypeName, CreatableDBObject dbObject, String existsSQL) throws StorageException {
		assert dbObject != null && dbObject.isValid();
		String createSQL = dbObject.toCreateSQL();
		try {
			this.execute((VoidCallable) () -> {
				Connection connection = this.getConnection();
				this.performTransaction(connection, (VoidCallable) () -> {
					// check if object already exists:
					try (	Statement statement = connection.createStatement();
							ResultSet resultSet = statement.executeQuery(existsSQL)) {
						if (resultSet.getBoolean("exists")) {
							return; // exists
						}
						// create object:
						statement.executeUpdate(createSQL);
					}
				});
			});
		} catch (Exception e) {
			throw new StorageException("Error during creation of database " + objectTypeName + ": " + dbObject.name(), e);
		}
	}

	// existsSQL creates a boolean column "exists"
	protected void _dropDBObjectCheckExists(String objectTypeName, CreatableDBObject dbObject, String existsSQL) throws StorageException {
		assert dbObject != null && dbObject.isValid();
		String dropSQL = dbObject.toDropSQL();
		try {
			this.execute((VoidCallable) () -> {
				Connection connection = this.getConnection();
				this.performTransaction(connection, (VoidCallable) () -> {
					// check if object exists:
					try (	Statement statement = connection.createStatement();
							ResultSet resultSet = statement.executeQuery(existsSQL)) {
						if (resultSet.getBoolean("exists")) {
							return; // exists
						}
						// drop object:
						statement.executeUpdate(dropSQL);
					}
				});
			});
		} catch (Exception e) {
			throw new StorageException("Error during deletion of database " + objectTypeName + ": " + dbObject.name(), e);
		}
	}

	// MySQL doesn't support 'IF NOT EXISTS' for indices
	protected String _indexExistsSQL(Index index) {
		return "SELECT COUNT(1) AS " + sql.quoteId("exists") + " FROM INFORMATION_SCHEMA.STATISTICS "
				+ "WHERE TABLE_SCHEMA=DATABASE() " + "AND TABLE_NAME='" + index.table() + "' AND INDEX_NAME='" + index.name() + "';";
	}

	@Override
	protected void _createIndex(Index index) throws StorageException {
		this._createDBObjectCheckExists("index", index, this._indexExistsSQL(index));
	}

	@Override
	protected void _dropIndex(Index index) throws StorageException {
		this._dropDBObjectCheckExists("index", index, this._indexExistsSQL(index));
	}

	// MySQL doesn't support 'IF NOT EXISTS' for triggers
	protected String _triggerExistsSQL(Trigger trigger) {
		return "SELECT COUNT(1) AS " + sql.quoteId("exists") + " FROM INFORMATION_SCHEMA.TRIGGERS "
				+ "WHERE TRIGGER_SCHEMA=DATABASE() " + "AND TRIGGER_NAME='" + trigger.name() + "';";
	}

	@Override
	protected void _createTrigger(Trigger trigger) throws StorageException {
		this._createDBObjectCheckExists("trigger", trigger, this._triggerExistsSQL(trigger));
	}

	@Override
	protected void _dropTrigger(Trigger trigger) throws StorageException {
		this._dropDBObjectCheckExists("trigger", trigger, this._triggerExistsSQL(trigger));
	}

	//

	@Override
	public List<String> getTableNames(String tableNamePrefix) throws StorageException {
		String tablesSQL = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE '" + tableNamePrefix + "%';";
		List<String> tables = new ArrayList<>();

		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(tablesSQL)) {
					while (resultSet.next()) {
						String tableName = resultSet.getString("TABLE_NAME");
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
		// | Field | Type | Null | Key | Default | Extra |
		String showColumnsSQL = "SHOW COLUMNS FROM " + sql.quoteId(tableName) + ";";

		// https://dev.mysql.com/doc/refman/8.0/en/key-column-usage-table.html
		// https://dev.mysql.com/doc/refman/8.0/en/referential-constraints-table.html
		String foreignKeysSQL = "SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS kcu"
				+ " INNER JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS AS rc"
				+ " ON (kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME"
				+ " AND kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA"
				+ " AND kcu.TABLE_NAME=rc.TABLE_NAME)"
				+ " WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='" + tableName + "';";

		Table table = sql.table(tableName);

		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				// columns:
				boolean hasColumns = false;
				try (ResultSet resultSet = statement.executeQuery(showColumnsSQL)) {
					while (resultSet.next()) {
						String columnName = resultSet.getString("Field");
						String type = resultSet.getString("Type");
						boolean notNull = "NO".equals(resultSet.getString("Null"));
						String defaultValue = resultSet.getString("Default");
						boolean primaryKey = "PRI".equals(resultSet.getString("Key"));
						String extra = resultSet.getString("Extra");
						boolean autoIncrement = (extra != null && extra.contains("auto_increment"));

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
						String referencedTable = resultSet.getString("REFERENCED_TABLE_NAME");
						String column = resultSet.getString("COLUMN_NAME");
						String referencedColumn = resultSet.getString("REFERENCED_COLUMN_NAME");
						boolean cascadeDelete = "CASCADE".equals(resultSet.getString("DELETE_RULE"));

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
		// index name "PRIMARY" is reserved for primary keys
		String indicesSQL = "SELECT *, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS " + sql.quoteId("Columns")
				+ " FROM INFORMATION_SCHEMA.STATISTICS"
				+ " WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + tableName + "' AND INDEX_NAME!='PRIMARY'"
				+ " GROUP BY INDEX_NAME;";
		List<Index> indices = new ArrayList<>();

		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(indicesSQL)) {
					while (resultSet.next()) {
						String indexName = resultSet.getString("INDEX_NAME");
						boolean unique = !resultSet.getBoolean("NON_UNIQUE");
						String columnsConcat = resultSet.getString("Columns");
						String[] columns = columnsConcat == null ? new String[0] : columnsConcat.split(",");
						if (columns.length == 0) {
							// index without columns? should not happen..
							continue;
						}
						Index index = sql.index().table(tableName).name(indexName);
						if (unique) index.unique();
						for (String column : columns) {
							index.column(column);
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
