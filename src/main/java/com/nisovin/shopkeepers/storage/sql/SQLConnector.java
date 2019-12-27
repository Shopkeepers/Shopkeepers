package com.nisovin.shopkeepers.storage.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.sql.SQL.Column;
import com.nisovin.shopkeepers.storage.sql.SQL.CombinedView;
import com.nisovin.shopkeepers.storage.sql.SQL.CreatableDBObject;
import com.nisovin.shopkeepers.storage.sql.SQL.Index;
import com.nisovin.shopkeepers.storage.sql.SQL.Table;
import com.nisovin.shopkeepers.storage.sql.SQL.Trigger;
import com.nisovin.shopkeepers.storage.sql.SQL.View;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;
import com.nisovin.shopkeepers.util.Utils.RetryHandler;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.VoidCallable;

public abstract class SQLConnector {

	private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 5;
	private static final int CONNECTION_MAX_ATTEMPTS = 3;
	private static final long CONNECTION_RETRY_SLEEP_MILLIS = 500;

	private static final int RETRY_MAX_ATTEMPTS = 10;
	private static final int RETRY_SLEEP_AFTER_ATTEMPTS = 3;
	private static final long RETRY_SLEEP_MILLIS = 200;

	protected final String logPrefix;
	protected final SQL sql;
	private boolean shutdown = false;

	// single connection: TODO use thread pool (HikariCP) (only for mysql?)? TODO check synchronization of storage tasks
	// allows for easy reasoning about order of executed tasks and db updates
	private Connection connection = null;

	protected static class CachedPreparedStatement {

		public final PreparedStatement preparedStatement;
		public final boolean returnGeneratedKeys;

		public CachedPreparedStatement(PreparedStatement preparedStatement, boolean returnGeneratedKeys) {
			this.preparedStatement = preparedStatement;
			this.returnGeneratedKeys = returnGeneratedKeys;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + preparedStatement.hashCode();
			result = prime * result + (returnGeneratedKeys ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof CachedPreparedStatement)) return false;
			CachedPreparedStatement other = (CachedPreparedStatement) obj;
			if (!preparedStatement.equals(other.preparedStatement)) return false;
			if (returnGeneratedKeys != other.returnGeneratedKeys) return false;
			return true;
		}
	}

	// PreparedStatement cache:
	// This only works if there is only a single connection being used.
	// SQL query -> prepared statement
	private final Map<String, CachedPreparedStatement> preparedStatementCache = new HashMap<>();

	public SQLConnector(String logPrefix, SQL sql) {
		this.logPrefix = logPrefix;
		this.sql = sql;
	}

	protected SQL getSQL() {
		return sql;
	}

	// shutdown:

	// once shut down, this connector can not be used anymore
	public synchronized void shutdown() {
		Validate.State.isTrue(!shutdown, "Database already shut down!");

		// disconnect:
		this.disconnect();

		shutdown = true;
	}

	protected synchronized final boolean isShutdown() {
		return shutdown;
	}

	// connection:

	protected abstract String getJDBCDriverClassName();

	protected abstract String getConnectionUrl();

	protected Properties getConnectionProperties() {
		Properties connectionProperties = new Properties();
		connectionProperties.putAll(Settings.async().databaseConnectionProperties);
		return connectionProperties;
	}

	protected void preConnect() throws Exception {
		// nothing by default
	}

	protected void postConnect(Connection connection) throws Exception {
		// strict transaction separation:
		connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
	}

	/**
	 * Connects to the database.
	 * 
	 * @throws StorageException
	 *             in case the connection couldn't be established
	 */
	protected final synchronized void connect() throws StorageException {
		Validate.State.isTrue(!this.isShutdown(), "Cannot connect to database after shutdown!");
		Validate.State.isTrue(connection == null, "There is already a connection setup!");
		try {
			// pre-connect:
			this.preConnect();

			// load driver:
			try {
				Class.forName(this.getJDBCDriverClassName());
			} catch (ClassNotFoundException e) {
				throw new StorageException("Could not find database driver: " + this.getJDBCDriverClassName(), e);
			}

			// open connection:
			// TODO setLoginTimeout?
			this.connection = DriverManager.getConnection(this.getConnectionUrl(), this.getConnectionProperties());
			assert connection != null;

			// post-connect:
			this.postConnect(connection);

			Log.debug(logPrefix + "Connection established.");
		} catch (Exception e) {
			throw new StorageException("Failed to establish database connection!", e);
		}
	}

	// does nothing if already disconnected
	protected final synchronized void disconnect() {
		if (connection == null) return; // already disconnected

		// cleanup prepared statement cache:
		for (CachedPreparedStatement cachedStmt : preparedStatementCache.values()) {
			this.closeStatement(cachedStmt.preparedStatement);
		}
		preparedStatementCache.clear();

		try {
			// close connection:
			if (!connection.isClosed()) {
				// Note on potential race condition: The connection might no longer be valid, and isClosed might already
				// be outdated at this point if the connection got closed remotely. This will result in a (harmless)
				// logged error.
				try {
					// rollback any incomplete transactions:
					if (!connection.getAutoCommit()) {
						connection.rollback();
					}
				} catch (SQLException e) {
					Log.severe(logPrefix + "Error during disconnect rollback!", e);
					// continue with closing of database connection
				}
				connection.close();
			}
		} catch (Exception e) {
			Log.severe(logPrefix + "Error during disconnect!", e);
		}

		connection = null;
	}

	public synchronized boolean isConnected(boolean checkAlive) {
		if (checkAlive) {
			return this.isConnectionAlive(connection); // also checks for null connection
		} else {
			return (connection != null);
		}
	}

	protected abstract boolean supportsJDBC4();

	// only used if JDBC4 is not supported
	protected String getConnectionTestQuery() {
		return "SELECT 1;";
	}

	protected synchronized boolean isConnectionAlive(Connection connection) {
		if (connection == null) return false;
		try {
			if (this.supportsJDBC4()) {
				// TODO sqlite might ignore the timeout parameter
				return connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
			} else {
				// execute test query:
				try (Statement statement = connection.createStatement()) {
					statement.setQueryTimeout(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
					statement.execute(this.getConnectionTestQuery());
					return true;
				} catch (SQLException e) {
					return false; // connection is not alive
				}
			}
		} catch (SQLException e) {
			Log.severe(logPrefix + "Failed to validate connection!", e);
			return false;
		}
	}

	/**
	 * Gets the database connection.
	 * <p>
	 * If not yet done, this will first establish the database connection. See {@link #ensureConnection(boolean)
	 * ensureConnection(false)}.
	 * 
	 * @return the database connection, not <code>null</code>
	 * @throws StorageException
	 *             in case the connection couldn't be established
	 */
	public synchronized Connection getConnection() throws StorageException {
		// ensure that the connection is established:
		this.ensureConnection(false);
		assert connection != null;
		return connection;
	}

	/**
	 * Checks if there is a connection established and optionally validates if it is still alive, and otherwise tries to
	 * establish a new connection.
	 * <p>
	 * When not connected, this will start up to {@link #MAX_CONNECTION_ATTEMPTS} connection attempts before giving up.
	 * 
	 * @param checkAlive
	 *            <code>true</code> to check if the connection is still alive
	 * @throws StorageException
	 *             in case the connection couldn't be established
	 */
	public synchronized void ensureConnection(boolean checkAlive) throws StorageException {
		// validate and refresh connection if necessary:
		if (!this.isConnected(checkAlive)) {
			// properly disconnect:
			this.disconnect();

			// freshly connect:
			Utils.callAndRethrow(() -> {
				Utils.retry(() -> {
					this.connect();
					return null;
				}, new RetryHandler() {
					@Override
					public void onRetry(int currentAttempt, Exception lastException) throws Exception {
						// sleep before next attempt:
						try {
							Thread.sleep(CONNECTION_RETRY_SLEEP_MILLIS);
						} catch (InterruptedException e) {
							// ignore, but reset interrupt flag:
							Thread.currentThread().interrupt();
						}
					}
				}, CONNECTION_MAX_ATTEMPTS);
				return null;
			});
			assert connection != null; // either connected, or exception thrown
		}
	}

	// PreparedStatement cache:

	public synchronized PreparedStatement prepareStatement(String sql) throws StorageException {
		return this.prepareStatement(sql, false);
	}

	public synchronized PreparedStatement prepareStatement(String sql, boolean returnGeneratedKeys) throws StorageException {
		try {
			CachedPreparedStatement cachedStmt = preparedStatementCache.get(sql);
			if (cachedStmt != null) {
				if (!returnGeneratedKeys || returnGeneratedKeys == cachedStmt.returnGeneratedKeys) {
					if (!cachedStmt.preparedStatement.isClosed()) {
						return cachedStmt.preparedStatement; // use cached statement
					} // else: statement already closed
				} else {
					// close cached statement: this statement is going to get replaced by a new one
					if (!cachedStmt.preparedStatement.isClosed()) {
						this.closeStatement(cachedStmt.preparedStatement);
					}
				}
			}

			int autoGeneratedKeys = (returnGeneratedKeys) ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
			PreparedStatement preparedStatement = this.getConnection().prepareStatement(sql, autoGeneratedKeys);
			preparedStatementCache.put(sql, new CachedPreparedStatement(preparedStatement, returnGeneratedKeys));
			return preparedStatement;
		} catch (Exception e) {
			throw new StorageException("Could not prepare statement: " + sql + " (return-generated-keys: " + returnGeneratedKeys + ")", e);
		}
	}

	// Utilities for running statements:

	private final RetryHandler defaultRetryHander = new RetryHandler() {
		@Override
		public void onRetry(int currentAttempt, Exception lastException) throws Exception {
			if (currentAttempt > RETRY_SLEEP_AFTER_ATTEMPTS) {
				// sleep before next attempt:
				try {
					Thread.sleep(RETRY_SLEEP_MILLIS);
				} catch (InterruptedException e) {
					// ignore, but reset interrupt flag:
					Thread.currentThread().interrupt();
				}

				// check if the connection is still alive:
				// If we cannot connect to the database (even after retrying several times): Forward the exception and
				// abort the retrying
				ensureConnection(true);
			}
		}
	};

	public RetryHandler getDefaultRetryHandler() {
		return defaultRetryHander;
	}

	/**
	 * Attempts to run the given {@link Callable} up to {@link #RETRY_MAX_ATTEMPTS} times and returns its return value
	 * in case of success.
	 * <p>
	 * This uses the {@link #getDefaultRetryHandler() default retry handler} that periodically sleeps and validates the
	 * connection.
	 * <p>
	 * In case of failure the exception thrown by the callable during the final attempt gets forwarded.
	 * 
	 * @param callable
	 *            the callable to execute
	 * @return the return value of the callable in case of successful execution
	 * @throws Exception
	 *             any exception thrown by the callable during the last attempt gets forwarded
	 * @see Utils#retry(Callable, int)
	 */
	public <T> T retry(Callable<T> callable) throws Exception {
		return this.retry(callable, this.getDefaultRetryHandler());
	}

	/**
	 * Attempts to run the given {@link Callable} up to {@link #MAX_RETRIES} times and returns its return value in case
	 * of success.
	 * <p>
	 * In case of failure the exception thrown by the callable during the final attempt gets forwarded.
	 * 
	 * @param callable
	 *            the callable to execute
	 * @param retryHandler
	 *            retry handler, optional
	 * @return the return value of the callable in case of successful execution
	 * @throws Exception
	 *             any exception thrown by the callable or retry-handler during the last attempt gets forwarded
	 * @see Utils#retry(Callable, RetryHandler, int)
	 */
	public <T> T retry(Callable<T> callable, RetryHandler retryHandler) throws Exception {
		return Utils.retry(callable, retryHandler, RETRY_MAX_ATTEMPTS);
	}

	/**
	 * Runs the given code while holding the connection lock.
	 * <p>
	 * If the code throws an exception, it gets retried up to {@link #RETRY_MAX_ATTEMPTS} times using the
	 * {@link #getDefaultRetryHandler() default RetryHandler}.
	 * 
	 * @param <T>
	 * @param callable
	 * @return
	 * @throws Exception
	 */
	public <T> T execute(Callable<T> callable) throws Exception {
		synchronized (this) { // connection lock
			return this.retry(callable); // retry using the default RetryHandler
		}
	}

	// wrap common try-catch blocks:

	/**
	 * Closes the given statement if it isn't <code>null</code> and catches and logs any possibly thrown
	 * {@link SQLException}.
	 * 
	 * @param <T>
	 *            the statement type
	 * @param statement
	 *            the statement
	 * @return <code>null</code> for compact clearing of statement references
	 */
	public <T extends Statement> T closeStatement(T statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				Log.severe(logPrefix + "Could not close statement!", e);
			}
		}
		return null;
	}

	// TODO use this everywhere where possible?
	public void setParameters(PreparedStatement preparedStmt, Object... params) throws SQLException {
		this.setParametersOffset(preparedStmt, 0, params);
	}

	public void setParametersOffset(PreparedStatement preparedStmt, int parameterIndexOffset, Object... params) throws SQLException {
		Validate.isTrue(parameterIndexOffset >= 0);
		if (preparedStmt == null || params == null) return;
		int index = 1 + parameterIndexOffset;
		for (Object param : params) {
			preparedStmt.setObject(index, param);
			++index;
		}
	}

	/**
	 * Clears the prepared statement's parameters if it isn't <code>null</code> and catches and logs the
	 * {@link SQLException} possibly arising by that.
	 * 
	 * @param preparedStatement
	 *            the prepared statement
	 * @see PreparedStatement#clearParameters()
	 */
	public void clearParameters(PreparedStatement preparedStatement) {
		if (preparedStatement == null) return;
		try {
			preparedStatement.clearParameters();
		} catch (SQLException e) {
			Log.severe(logPrefix + "Could not clear statement parameters!", e);
		}
	}

	public void rollbackOrIgnore(Connection connection) {
		try {
			connection.rollback();
		} catch (SQLException e) {
			// ignore
		}
	}

	public void setAutoCommitOrIgnore(Connection connection, boolean autoCommit) {
		try {
			connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			// ignore
		}
	}

	public <T> T performTransaction(Connection connection, Callable<T> callable) throws Exception {
		Boolean autoCommit = null;
		try {
			autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			T result = callable.call();
			connection.commit();
			return result;
		} catch (Throwable e) {
			// rollback, if transaction was started:
			if (autoCommit != null) {
				this.rollbackOrIgnore(connection);
			}
			throw e; // forward exception
		} finally {
			// reset auto-commit:
			if (autoCommit != null) {
				this.setAutoCommitOrIgnore(connection, autoCommit);
			}
		}
	}

	// accepts a Column instead of column name
	public <T> int getOrInsertObject(	String objectTypeName, T object, Column idColumn,
										Callable<PreparedStatement> preparedGetStmt,
										Callable<PreparedStatement> preparedInsertStmt,
										boolean logNewInserts) throws StorageException {
		return this.getOrInsertObject(objectTypeName, object, idColumn.name(), preparedGetStmt, preparedInsertStmt, logNewInserts);
	}

	// This is supposed to get called inside a transaction!
	// returns the object id (using the specified id column), or throws an exception
	// the insert statement is expected to return the generated id
	// objectTypeName and object are used for error messages
	public <T> int getOrInsertObject(	String objectTypeName, T object, String idColumn,
										Callable<PreparedStatement> prepareQueryStmt,
										Callable<PreparedStatement> prepareInsertStmt,
										boolean logNewInserts) throws StorageException {
		assert !this.isShutdown();

		int objectId = 0;
		boolean hasObject = false;
		Exception exception = null;
		try {
			// get object if it exists:
			PreparedStatement queryStmt = null;
			try {
				queryStmt = prepareQueryStmt.call();
				try (ResultSet resultSet = queryStmt.executeQuery()) {
					if (resultSet.next()) {
						objectId = resultSet.getInt(idColumn);
						hasObject = true;
					}
				}
			} finally {
				this.clearParameters(queryStmt);
			}

			// otherwise insert object:
			if (!hasObject) {
				PreparedStatement insertStmt = null;
				try {
					insertStmt = prepareInsertStmt.call();
					insertStmt.executeUpdate();
					try (ResultSet resultSet = insertStmt.getGeneratedKeys()) {
						if (resultSet.next()) {
							objectId = resultSet.getInt(1);
							hasObject = true;
							if (logNewInserts) {
								Log.info(logPrefix + "Added new " + objectTypeName + ": " + object);
							}
						}
					}
				} finally {
					this.clearParameters(insertStmt);
				}
			}
		} catch (Exception e) {
			// wrap for more specific error message:
			exception = e;
			hasObject = false;
		}

		if (!hasObject) {
			// exception can be null
			throw new StorageException("Could not find or insert " + objectTypeName + ": " + object, exception);
		}
		return objectId;
	}

	// other utility:

	public abstract List<String> getTableNames(String tableNamePrefix) throws StorageException;

	public abstract Table getTable(String tableName) throws StorageException;

	public abstract List<Index> getIndices(String tableName) throws StorageException;

	// creation and deletion of common database objects:

	// callable result is ignored; returns true on success, otherwise logs the error
	protected boolean runSafely(Callable<?> callable) {
		try {
			callable.call();
			return true;
		} catch (Throwable e) {
			// expects the exception to already have an expressive error message:
			Log.severe(e.getMessage(), e);
			return false;
		}
	}

	// creation:

	protected boolean createDBObjectSafely(String objectTypeName, CreatableDBObject dbObject) {
		Validate.notNull(dbObject, objectTypeName + " is null!");
		Validate.isTrue(dbObject.isValid(), "Invalid " + objectTypeName + "!");
		return this.runSafely(() -> {
			this.createDBObject(objectTypeName, dbObject);
			return null; // ignored
		});
	}

	protected void createDBObject(String objectTypeName, CreatableDBObject dbObject) throws StorageException {
		Validate.notNull(dbObject, objectTypeName + " is null!");
		Validate.isTrue(dbObject.isValid(), "Invalid " + objectTypeName + "!");
		// anything other than StorageException is unexpected here:
		Utils.callAndRethrow((VoidCallable) () -> this.execute((VoidCallable) () -> this._createDBObject(objectTypeName, dbObject)));
	}

	protected void _createDBObject(String objectTypeName, CreatableDBObject dbObject) throws StorageException {
		assert dbObject != null && dbObject.isValid();
		String createSQL = dbObject.toCreateSQL();
		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate(createSQL);
			}
		} catch (SQLException e) {
			throw new StorageException("Error during creation of database " + objectTypeName + ": " + dbObject.name(), e);
		}
	}

	// deletion:

	protected boolean dropDBObjectSafely(String objectTypeName, CreatableDBObject dbObject) {
		Validate.notNull(dbObject, objectTypeName + " is null!");
		Validate.isTrue(dbObject.isValid(), "Invalid " + objectTypeName + "!");
		return this.runSafely(() -> {
			this.dropDBObject(objectTypeName, dbObject);
			return null; // ignored
		});
	}

	protected void dropDBObject(String objectTypeName, CreatableDBObject dbObject) throws StorageException {
		Validate.notNull(dbObject, objectTypeName + " is null!");
		Validate.isTrue(dbObject.isValid(), "Invalid " + objectTypeName + "!");
		// anything other than StorageException is unexpected here:
		Utils.callAndRethrow((VoidCallable) () -> this.execute((VoidCallable) () -> this._dropDBObject(objectTypeName, dbObject)));
	}

	protected void _dropDBObject(String objectTypeName, CreatableDBObject dbObject) throws StorageException {
		assert dbObject != null && dbObject.isValid();
		String dropSQL = dbObject.toDropSQL();
		try {
			Connection connection = this.getConnection();
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate(dropSQL);
			}
		} catch (SQLException e) {
			throw new StorageException("Error during deletion of database " + objectTypeName + ": " + dbObject.name(), e);
		}
	}

	// separate methods for the different object type to allow for specific overrides in sub-classes

	// create table:

	public boolean createTableSafely(Table table) {
		return this.createDBObjectSafely("table", table);
	}

	public void createTable(Table table) throws StorageException {
		this.createDBObject("table", table);
	}

	protected void _createTable(Table table) throws StorageException {
		this._createDBObject("table", table);
	}

	// drop table:

	public boolean dropTableSafely(Table table) {
		return this.dropDBObjectSafely("table", table);
	}

	public void dropTable(Table table) throws StorageException {
		this.dropDBObject("table", table);
	}

	protected void _dropTable(Table table) throws StorageException {
		this._dropDBObject("table", table);
	}

	// create (combined) view:

	public boolean createViewSafely(View view) {
		return this.createDBObjectSafely("view", view);
	}

	public void createView(View view) throws StorageException {
		this.createDBObject("view", view);
	}

	protected void _createView(View view) throws StorageException {
		this._createDBObject("view", view);
	}

	public boolean createViewSafely(CombinedView view) {
		return this.createViewSafely(view.getView());
	}

	public void createView(CombinedView view) throws StorageException {
		this.createView(view.getView());
	}

	protected void _createView(CombinedView view) throws StorageException {
		this._createView(view.getView());
	}

	// drop (combined) view:

	public boolean dropViewSafely(View view) {
		return this.dropDBObjectSafely("view", view);
	}

	public void dropView(View view) throws StorageException {
		this.dropDBObject("view", view);
	}

	protected void _dropView(View view) throws StorageException {
		this._dropDBObject("view", view);
	}

	public boolean dropViewSafely(CombinedView view) {
		return this.dropViewSafely(view.getView());
	}

	public void dropView(CombinedView view) throws StorageException {
		this.dropView(view.getView());
	}

	protected void _dropView(CombinedView view) throws StorageException {
		this._dropView(view.getView());
	}

	// create index:

	public boolean createIndexSafely(Index index) {
		return this.createDBObjectSafely("index", index);
	}

	public void createIndex(Index index) throws StorageException {
		this.createDBObject("index", index);
	}

	protected void _createIndex(Index index) throws StorageException {
		this._createDBObject("index", index);
	}

	// drop index:

	public boolean dropIndexSafely(Index index) {
		return this.dropDBObjectSafely("index", index);
	}

	public void dropIndex(Index index) throws StorageException {
		this.dropDBObject("index", index);
	}

	protected void _dropIndex(Index index) throws StorageException {
		this._dropDBObject("index", index);
	}

	// create trigger:

	public boolean createTriggerSafely(Trigger trigger) {
		return this.createDBObjectSafely("trigger", trigger);
	}

	public void createTrigger(Trigger trigger) throws StorageException {
		this.createDBObject("trigger", trigger);
	}

	protected void _createTrigger(Trigger trigger) throws StorageException {
		this._createDBObject("trigger", trigger);
	}

	// drop trigger:

	public boolean dropTriggerSafely(Trigger trigger) {
		return this.dropDBObjectSafely("trigger", trigger);
	}

	public void dropTrigger(Trigger trigger) throws StorageException {
		this.dropDBObject("trigger", trigger);
	}

	protected void _dropTrigger(Trigger trigger) throws StorageException {
		this._dropDBObject("trigger", trigger);
	}
}
