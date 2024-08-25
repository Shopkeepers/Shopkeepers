package com.nisovin.shopkeepers.tradelog.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.tradelog.TradeLogUtils;
import com.nisovin.shopkeepers.tradelog.TradeLogger;
import com.nisovin.shopkeepers.tradelog.data.PlayerRecord;
import com.nisovin.shopkeepers.tradelog.data.ShopRecord;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Logs trades to an SQLite database.
 */
public class SQLiteTradeLogger implements TradeLogger {

	private static final String TRADE_LOGS_FOLDER = "trade-logs";
	private static final String FILE_NAME = "trades.db";
	private static final String TABLE_NAME = "trade";
	// TODO SQlite has no data types (only storage classes). Also, we should probably not make
	// assumptions here, e.g. about the max world name length.
	private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
			+ "time DATETIME NOT NULL, "
			+ "player_uuid CHARACTER(36) NOT NULL, "
			+ "player_name VARCHAR(16) NOT NULL, "
			+ "shop_uuid VARCHAR(36) NOT NULL, "
			+ "shop_type VARCHAR(32) NOT NULL, "
			+ "shop_world VARCHAR(30), " // Null for virtual shops
			+ "shop_x INTEGER NOT NULL, "
			+ "shop_y INTEGER NOT NULL, "
			+ "shop_z INTEGER NOT NULL, "
			+ "shop_owner_uuid CHARACTER(36), " // Shop owner can be null for admin shops
			+ "shop_owner_name VARCHAR(16), "
			+ "item_1_type VARCHAR(64) NOT NULL, "
			+ "item_1_amount TINYINT UNSIGNED NOT NULL, "
			+ "item_1_metadata TEXT NOT NULL, "
			+ "item_2_type VARCHAR(64), " // Second item is optional and thus can be null
			+ "item_2_amount TINYINT UNSIGNED, "
			+ "item_2_metadata TEXT, "
			+ "result_item_type VARCHAR(64) NOT NULL, "
			+ "result_item_amount TINYINT UNSIGNED NOT NULL, "
			+ "result_item_metadata TEXT NOT NULL, "
			+ "trade_count SMALLINT UNSIGNED NOT NULL"
			+ ");";
	private static final String INSERT_TRADE = "INSERT INTO " + TABLE_NAME
			+ "(time, "
			+ "player_uuid, player_name, "
			+ "shop_uuid, shop_type, shop_world, shop_x, shop_y, shop_z, "
			+ "shop_owner_uuid, shop_owner_name, "
			+ "item_1_type, item_1_amount, item_1_metadata, "
			+ "item_2_type, item_2_amount, item_2_metadata, "
			+ "result_item_type, result_item_amount, result_item_metadata, "
			+ "trade_count) "
			+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private final Plugin plugin;
	private final String connectionURL;

	private final AtomicBoolean tableCreated = new AtomicBoolean(false);

	public SQLiteTradeLogger(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		Path tradeLogsFolder = plugin.getDataFolder().toPath().resolve(TRADE_LOGS_FOLDER);
		this.connectionURL = "jdbc:sqlite:" + tradeLogsFolder.resolve(FILE_NAME);

		this.createTable();
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(connectionURL);
	}

	private void createTable() {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try (	Connection connection = getConnection();
					Statement statement = connection.createStatement()) {
				statement.execute(CREATE_TABLE);
				tableCreated.set(true);
			} catch (SQLException e) {
				Log.severe("Could not create trades table. Trades won't be logged.", e);
			}
		});
	}

	@Override
	public void logTrade(TradeRecord trade) {
		// TODO Batch the incoming trades and commit them once the setup is done.
		if (!tableCreated.get()) {
			return;
		}

		// TODO Keep the connection open? Cache the PreparedStatement?
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try (	Connection connection = this.getConnection();
					PreparedStatement statement = connection.prepareStatement(INSERT_TRADE)) {
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

				// TODO Store as UTC timestamp instead of as OffsetDateTime
				statement.setObject(1, timestamp.atOffset(ZoneOffset.UTC)); // Time in UTC

				statement.setString(2, player.getUniqueId().toString()); // player_uuid
				statement.setString(3, player.getName()); // player_name

				statement.setString(4, shop.getUniqueId().toString()); // shop_uuid
				statement.setString(5, shop.getTypeId()); // shop_type
				statement.setString(6, shop.getWorldName()); // shop_world
				statement.setInt(7, shop.getX()); // shop_x
				statement.setInt(8, shop.getY()); // shop_y
				statement.setInt(9, shop.getZ()); // shop_z

				statement.setString(10, shopOwnerId); // shop_owner_uuid
				statement.setString(11, shopOwnerName); // shop_owner_name

				statement.setString(12, item1.getType().name()); // item_1_type
				statement.setInt(13, item1.getAmount()); // item_1_amount
				statement.setString(14, this.getItemMetadata(item1)); // item_1_metadata

				statement.setString(15, item2Type); // item_2_type
				statement.setObject(16, item2Amount, Types.TINYINT); // item_2_amount
				statement.setString(17, item2Metadata); // item_2_metadata

				statement.setString(18, resultItem.getType().name()); // result_item_type
				statement.setInt(19, resultItem.getAmount()); // result_item_amount
				statement.setString(20, this.getItemMetadata(resultItem)); // result_item_metadata

				statement.setInt(21, trade.getTradeCount()); // trade_count
				statement.executeUpdate();
			} catch (SQLException e) {
				Log.severe("Could not not log trade.", e);
			}
		});
	}

	@Override
	public void flush() {
		// Not needed: We commit all trades immediately.
		// TODO Actually: Await any currently in-progress async tasks.
	}

	private String getItemMetadata(UnmodifiableItemStack itemStack) {
		assert itemStack != null;
		if (Settings.logItemMetadata) return ""; // Disabled

		return TradeLogUtils.getItemMetadata(itemStack);
	}
}
