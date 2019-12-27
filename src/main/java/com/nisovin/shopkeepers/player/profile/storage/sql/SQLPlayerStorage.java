package com.nisovin.shopkeepers.player.profile.storage.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.storage.SKStorage.PlayerStorageComponent;
import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.sql.SQL;
import com.nisovin.shopkeepers.storage.sql.SQL.Column;
import com.nisovin.shopkeepers.storage.sql.SQL.Table;
import com.nisovin.shopkeepers.storage.sql.SQLConnector;
import com.nisovin.shopkeepers.storage.sql.SQLStorageComponent;
import com.nisovin.shopkeepers.util.TimeUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.VoidCallable;

public class SQLPlayerStorage extends SQLStorageComponent implements PlayerStorageComponent {

	/*
	 * table: {tablePrefix}players
	 * | id [INT, primary] | uuid [CHAR(36), unique index] | name [VARCHAR(32), index]
	 * | first_seen [DateTime] | last_seen [DateTime] |
	 * 
	 * player names java edition vs bedrock: 16 vs 32 characters -> using 32 to be future proof
	 */
	public static final class PlayersTable {

		private final SQL sql;

		private final String table_name = Settings.databaseTablePrefix + "players";
		private final String col_id = "id";
		private final String col_uuid = "uuid";
		private final String col_name = "name";
		private final String col_first_seen = "first_seen";
		private final String col_last_seen = "last_seen";

		public final Table table;
		public final Column id;
		public final Column uuid;
		public final Column name;
		public final Column first_seen;
		public final Column last_seen;

		protected PlayersTable(SQL sql) {
			this.sql = sql;

			this.table = sql.table(table_name);
			this.id = table.column(col_id).type("INTEGER").primaryKey().autoIncrement().notNull().build();
			this.uuid = table.column(col_uuid).type("CHAR(36)").notNull().build();
			this.name = table.column(col_name).type("VARCHAR(32)").notNull().build();
			this.first_seen = table.column(col_first_seen).type(sql.dateTimeType()).notNull().build();
			this.last_seen = table.column(col_last_seen).type(sql.dateTimeType()).notNull().build();

			// used by MySQL: Case sensitive comparison of utf8 text
			table.extra("ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, DEFAULT COLLATE = utf8mb4_bin");
			table.build();
		}

		protected void setup(SQLConnector connector) throws StorageException {
			// create table:
			connector.createTable(table);

			// create indices:
			connector.createIndex(sql.index().table(table).column(uuid).unique());
			connector.createIndex(sql.index().table(table).column(name));
		}
	}

	protected final PlayersTable table_players;

	// not final to allow for overriding in subclasses:
	protected String GET_PLAYER_BY_ID_SQL;
	protected String GET_PLAYER_ID_BY_UUID_SQL;
	protected String GET_PLAYER_BY_UUID_SQL;
	protected String GET_PLAYERS_BY_NAME_SQL;
	protected String ADD_PLAYER_SQL;
	protected String UPDATE_PLAYER_SQL;
	protected String GET_PLAYER_COUNT_SQL;
	protected String REMOVE_PLAYER_SQL;

	public SQLPlayerStorage(SQLConnector connector) {
		super("player-storage", connector);
		this.table_players = new PlayersTable(sql);

		// prepare SQL statements:
		GET_PLAYER_BY_ID_SQL = "SELECT *"
				+ " FROM " + table_players.table.qn()
				+ " WHERE " + table_players.id.qn() + "=?"
				+ " LIMIT 1;";
		GET_PLAYER_ID_BY_UUID_SQL = "SELECT " + table_players.id.qn()
				+ " FROM " + table_players.table.qn()
				+ " WHERE " + table_players.uuid.qn() + "=?"
				+ " LIMIT 1;";
		GET_PLAYER_BY_UUID_SQL = "SELECT *"
				+ " FROM " + table_players.table.qn()
				+ " WHERE " + table_players.uuid.qn() + "=?"
				+ " LIMIT 1;";
		GET_PLAYERS_BY_NAME_SQL = "SELECT *"
				+ " FROM " + table_players.table.qn()
				+ " WHERE " + table_players.name.qn() + "=?"
				+ " ORDER BY " + table_players.last_seen.qn() + " DESC;";
		ADD_PLAYER_SQL = "INSERT " + sql.ignore()
				+ " INTO " + table_players.table.qn()
				+ " (" + table_players.uuid.qn()
				+ "," + table_players.name.qn()
				+ "," + table_players.first_seen.qn()
				+ "," + table_players.last_seen.qn()
				+ ")" + " VALUES(?,?,?,?);";
		UPDATE_PLAYER_SQL = "UPDATE " + table_players.table.qn()
				+ " SET " + table_players.name.qn() + "=?"
				+ "," + table_players.first_seen.qn() + "=?"
				+ "," + table_players.last_seen.qn() + "=?"
				+ " WHERE " + table_players.uuid.qn() + "=?"
				+ " AND " + table_players.last_seen.qn() + "<?;";
		GET_PLAYER_COUNT_SQL = "SELECT COUNT(*) FROM " + table_players.table.qn() + ";";
		REMOVE_PLAYER_SQL = "DELETE FROM " + table_players.table.qn()
				+ " WHERE " + table_players.uuid.qn() + "=?;";
	}

	public PlayersTable getPlayersTable() {
		return table_players;
	}

	@Override
	protected void onSetup() throws StorageException {
		super.onSetup();
		// create table and indices:
		table_players.setup(connector);
	}

	// returns the player_id, or throws an exception
	public int getOrInsertProfile(PlayerProfile profile) throws StorageException {
		assert !this.isShutdown();
		assert profile != null;

		String uuid = profile.getUniqueId().toString();
		String playerName = profile.getName();
		Timestamp firstSeen = Timestamp.from(profile.getFirstSeen());
		Timestamp lastSeen = Timestamp.from(profile.getLastSeen());
		Calendar utcCalendar = TimeUtils.UTC_CALENDAR.get();

		return connector.getOrInsertObject("player profile", profile, table_players.id,
				() -> {
					PreparedStatement stmt = connector.prepareStatement(GET_PLAYER_ID_BY_UUID_SQL);
					stmt.setString(1, uuid);
					return stmt;
				},
				() -> {
					PreparedStatement stmt = connector.prepareStatement(ADD_PLAYER_SQL, true);
					stmt.setString(1, uuid);
					stmt.setString(2, playerName);
					stmt.setTimestamp(3, firstSeen, utcCalendar);
					stmt.setTimestamp(4, lastSeen, utcCalendar);
					return stmt;
				}, Settings.isDebugging(Settings.DebugOptions.database)); // log new insert if debugging
	}

	public SQLPlayerProfile parseProfile(ResultSet resultSet) throws SQLException {
		return this.parseProfile("", resultSet);
	}

	// columnPrefix can be empty
	public SQLPlayerProfile parseProfile(String columnPrefix, ResultSet resultSet) throws SQLException {
		assert columnPrefix != null && resultSet != null;
		int playerId = resultSet.getInt(columnPrefix + table_players.id.name());
		String uuidString = resultSet.getString(columnPrefix + table_players.uuid.name());
		// when embedded in another table (eg. combined view), null uuid indicates 'no player'
		if (uuidString == null) return null;
		UUID playerUUID = UUID.fromString(uuidString);
		String playerName = resultSet.getString(columnPrefix + table_players.name.name());
		Calendar utcCalendar = TimeUtils.UTC_CALENDAR.get();
		Instant firstSeen = resultSet.getTimestamp(columnPrefix + table_players.first_seen.name(), utcCalendar).toInstant();
		Instant lastSeen = resultSet.getTimestamp(columnPrefix + table_players.last_seen.name(), utcCalendar).toInstant();
		return new SQLPlayerProfile(playerId, playerUUID, playerName, firstSeen, lastSeen);
	}

	@Override
	public SQLPlayerProfile getProfile(UUID playerUUID) throws StorageException {
		this.validateNotShutdown();
		try {
			return connector.execute(() -> {
				return this._getProfile(playerUUID);
			});
		} catch (Exception e) {
			throw new StorageException("Could not fetch player profile for uuid: " + playerUUID, e);
		}
	}

	/**
	 * @see #getProfile(UUID)
	 */
	protected SQLPlayerProfile _getProfile(UUID playerUUID) throws Exception {
		assert !this.isShutdown();
		PreparedStatement stmt = null;
		try {
			stmt = connector.prepareStatement(GET_PLAYER_BY_UUID_SQL);
			stmt.setString(1, playerUUID.toString());
			try (ResultSet resultSet = stmt.executeQuery()) {
				if (resultSet.next()) {
					return this.parseProfile(resultSet);
				} else {
					// no player profile found:
					return null;
				}
			}
		} finally {
			connector.clearParameters(stmt);
		}
	}

	@Override
	public List<PlayerProfile> getProfiles(String playerName) throws StorageException {
		this.validateNotShutdown();
		try {
			return connector.execute(() -> {
				PreparedStatement stmt = null;
				try {
					stmt = connector.prepareStatement(GET_PLAYERS_BY_NAME_SQL);
					List<PlayerProfile> profiles = new ArrayList<>();
					stmt.setString(1, playerName);
					try (ResultSet resultSet = stmt.executeQuery()) {
						while (resultSet.next()) {
							profiles.add(this.parseProfile(resultSet));
						}
					}
					return profiles;
				} finally {
					connector.clearParameters(stmt);
				}
			});
		} catch (Exception e) {
			throw new StorageException("Could not fetch player profiles for name: " + playerName, e);
		}
	}

	@Override
	public void updateProfile(PlayerProfile profile) throws StorageException {
		this.validateNotShutdown();
		Validate.notNull(profile);

		// insert if no data exists yet, otherwise update:
		String uuid = profile.getUniqueId().toString();
		String playerName = profile.getName();
		Timestamp firstSeen = Timestamp.from(profile.getFirstSeen());
		Timestamp lastSeen = Timestamp.from(profile.getLastSeen());
		Calendar utcCalendar = TimeUtils.UTC_CALENDAR.get();

		try {
			connector.execute(() -> {
				Connection connection = connector.getConnection();
				return connector.performTransaction(connection, () -> {
					// attempt update before insert, because failed inserts use up auto-incremented ids, and update is
					// also more likely to succeed here:
					boolean profileUpdated = false;
					PreparedStatement stmt = null;
					try {
						stmt = connector.prepareStatement(UPDATE_PLAYER_SQL);
						stmt.setString(1, playerName);
						stmt.setTimestamp(2, firstSeen, utcCalendar);
						stmt.setTimestamp(3, lastSeen, utcCalendar);
						stmt.setString(4, uuid);
						stmt.setTimestamp(5, lastSeen, utcCalendar);
						int updatedRows = stmt.executeUpdate();
						if (updatedRows > 0) {
							profileUpdated = true;
						}
					} finally {
						connector.clearParameters(stmt);
					}
					if (!profileUpdated) {
						// The stored profile is either missing, or the locale profile is outdated.
						// Check if the profile exists, otherwise add it:
						this.getOrInsertProfile(profile);
					}
					return null;
				});
			});
		} catch (Exception e) {
			throw new StorageException("Could not update profile for player '" + playerName + "' (" + uuid + ")!", e);
		}
	}

	@Override
	public void removeProfile(UUID playerUUID) throws StorageException {
		this.validateNotShutdown();
		try {
			connector.execute((VoidCallable) () -> {
				PreparedStatement stmt = null;
				try {
					stmt = connector.prepareStatement(REMOVE_PLAYER_SQL);
					stmt.setString(1, playerUUID.toString());
					int affectedRows = stmt.executeUpdate();
					if (affectedRows == 0) {
						throw new StorageException("Player not found: " + playerUUID);
					}
				} finally {
					connector.clearParameters(stmt);
				}
			});
		} catch (Exception e) {
			throw new StorageException("Could not remove player profile for uuid '" + playerUUID + "'!", e);
		}
	}

	@Override
	public int getPlayerCount() throws StorageException {
		this.validateNotShutdown();
		try {
			return connector.execute(() -> {
				PreparedStatement stmt = connector.prepareStatement(GET_PLAYER_COUNT_SQL);
				try (ResultSet resultSet = stmt.executeQuery()) {
					int playerCount = 0;
					if (resultSet.next()) {
						playerCount = resultSet.getInt(1);
					}
					return playerCount;
				}
			});
		} catch (Exception e) {
			throw new StorageException("Could not fetch player count!", e);
		}
	}
}
