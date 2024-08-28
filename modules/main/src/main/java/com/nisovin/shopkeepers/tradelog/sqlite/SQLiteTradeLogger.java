package com.nisovin.shopkeepers.tradelog.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;

import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.tradelog.TradeLogStorageType;
import com.nisovin.shopkeepers.tradelog.base.AbstractFileTradeLogger;
import com.nisovin.shopkeepers.tradelog.data.PlayerRecord;
import com.nisovin.shopkeepers.tradelog.data.ShopRecord;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Logs trades to an SQLite database.
 */
public class SQLiteTradeLogger extends AbstractFileTradeLogger {

	private static final String FILE_NAME = "trades.db";
	private static final String TABLE_NAME = "trade";
	// Note: SQLite does not have rigid data types, but storage classes and type affinity. The data
	// types specified here are not enforced by SQLite or us, but only used to document the expected
	// structure of the data.
	private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
			+ "timestamp VARCHAR(30) NOT NULL, " // ISO 8601 UTC date time with fractional seconds
			+ "player_uuid CHARACTER(36) NOT NULL, "
			+ "player_name VARCHAR(16) NOT NULL, "
			+ "shop_uuid CHARACTER(36) NOT NULL, "
			+ "shop_type VARCHAR(32) NOT NULL, "
			+ "shop_world VARCHAR(32), " // Null for virtual shops
			+ "shop_x INTEGER NOT NULL, " // 0 for virtual shops
			+ "shop_y INTEGER NOT NULL, "
			+ "shop_z INTEGER NOT NULL, "
			+ "shop_owner_uuid CHARACTER(36), " // Shop owner, null for admin shops
			+ "shop_owner_name VARCHAR(16), "
			+ "item_1_type VARCHAR(64) NOT NULL, "
			+ "item_1_amount TINYINT UNSIGNED NOT NULL, "
			+ "item_1_metadata TEXT NOT NULL, " // Empty string if the item has no metadata
			+ "item_2_type VARCHAR(64), " // Second item is optional and can thus be null
			+ "item_2_amount TINYINT UNSIGNED, "
			+ "item_2_metadata TEXT, "
			+ "result_item_type VARCHAR(64) NOT NULL, "
			+ "result_item_amount TINYINT UNSIGNED NOT NULL, "
			+ "result_item_metadata TEXT NOT NULL, "
			+ "trade_count SMALLINT UNSIGNED NOT NULL"
			+ ");";
	private static final String INSERT_TRADE = "INSERT INTO " + TABLE_NAME
			+ "(timestamp, "
			+ "player_uuid, player_name, "
			+ "shop_uuid, shop_type, shop_world, shop_x, shop_y, shop_z, "
			+ "shop_owner_uuid, shop_owner_name, "
			+ "item_1_type, item_1_amount, item_1_metadata, "
			+ "item_2_type, item_2_amount, item_2_metadata, "
			+ "result_item_type, result_item_amount, result_item_metadata, "
			+ "trade_count) "
			+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private final String connectionURL;

	private volatile @Nullable String setupFailureReason = null;

	public SQLiteTradeLogger(Plugin plugin) {
		super(plugin, TradeLogStorageType.SQLITE);

		this.connectionURL = "jdbc:sqlite:" + tradeLogsFolder.resolve(FILE_NAME);

		this.createTable();
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(connectionURL);
	}

	@Override
	protected void asyncSetup() {
		super.asyncSetup();

		this.createTable();
	}

	@Override
	protected void postSetup() {
		var setupFailureReason = this.setupFailureReason;
		if (setupFailureReason != null) {
			this.disable(setupFailureReason);
		}
	}

	private void createTable() {
		try (	Connection connection = getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute(CREATE_TABLE);
		} catch (SQLException e) {
			setupFailureReason = "Could not create table '" + TABLE_NAME + "'.";
			Log.severe(logPrefix + setupFailureReason, e);
		}
	}

	@Override
	protected void writeTrades(SaveContext saveContext) throws Exception {
		TradeRecord trade = saveContext.getNextUnsavedTrade();
		if (trade == null) return; // There are no unsaved trades

		boolean done = false;
		// TODO Keep the connection open? Cache the PreparedStatement?
		try (	Connection connection = this.getConnection();
				PreparedStatement insertStatement = connection.prepareStatement(INSERT_TRADE)) {
			do {
				this.insertTrade(insertStatement, trade);

				// Trade successfully saved:
				saveContext.onTradeSuccessfullySaved();

				// Get the next trade to insert:
				trade = saveContext.getNextUnsavedTrade();
				if (trade == null) break; // There are no more trades to save
			} while (true);

			// We are about to close the connection:
			done = true;
		} catch (SQLException e) {
			if (!done) {
				throw e;
			} else {
				// Since all inserts completed successfully, we assume that the trades have been
				// successfully saved. We therefore ignore any exceptions raised during the closing
				// of the connection: The exception is still logged, but it does not trigger a retry
				// of the trade log attempt.
				Log.severe("Failed to close the database connection!", e);
			}
		}
	}

	private void insertTrade(PreparedStatement insertStatement, TradeRecord trade)
			throws SQLException {
		Instant timestamp = trade.getTimestamp();
		PlayerRecord player = trade.getPlayer();

		ShopRecord shop = trade.getShop();
		PlayerRecord shopOwner = shop.getOwner();
		@Nullable String shopOwnerId = null;
		@Nullable String shopOwnerName = null;
		if (shopOwner != null) {
			shopOwnerId = shopOwner.getUniqueId().toString();
			shopOwnerName = shopOwner.getName();
		}

		UnmodifiableItemStack resultItem = trade.getResultItem();
		UnmodifiableItemStack item1 = trade.getItem1();
		UnmodifiableItemStack item2 = trade.getItem2(); // Can be null
		@Nullable String item2Type = null;
		@Nullable Integer item2Amount = null;
		@Nullable String item2Metadata = null;
		if (item2 != null) {
			item2Type = item2.getType().name();
			item2Amount = item2.getAmount();
			item2Metadata = this.getItemMetadata(item2);
		}

		insertStatement.setString(1, timestamp.toString()); // timestamp as ISO UTC

		insertStatement.setString(2, player.getUniqueId().toString()); // player_uuid
		insertStatement.setString(3, player.getName()); // player_name

		insertStatement.setString(4, shop.getUniqueId().toString()); // shop_uuid
		insertStatement.setString(5, shop.getTypeId()); // shop_type
		insertStatement.setString(6, shop.getWorldName()); // shop_world
		insertStatement.setInt(7, shop.getX()); // shop_x
		insertStatement.setInt(8, shop.getY()); // shop_y
		insertStatement.setInt(9, shop.getZ()); // shop_z

		insertStatement.setString(10, shopOwnerId); // shop_owner_uuid
		insertStatement.setString(11, shopOwnerName); // shop_owner_name

		insertStatement.setString(12, item1.getType().name()); // item_1_type
		insertStatement.setInt(13, item1.getAmount()); // item_1_amount
		insertStatement.setString(14, this.getItemMetadata(item1)); // item_1_metadata

		insertStatement.setString(15, item2Type); // item_2_type
		insertStatement.setObject(16, item2Amount, Types.TINYINT); // item_2_amount
		insertStatement.setString(17, item2Metadata); // item_2_metadata

		insertStatement.setString(18, resultItem.getType().name()); // result_item_type
		insertStatement.setInt(19, resultItem.getAmount()); // result_item_amount
		insertStatement.setString(20, this.getItemMetadata(resultItem)); // result_item_metadata

		insertStatement.setInt(21, trade.getTradeCount()); // trade_count

		insertStatement.executeUpdate();
	}
}
