package com.nisovin.shopkeepers.tradelog.sqlite;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.tradelog.TradeLogger;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;
import com.nisovin.shopkeepers.util.yaml.YamlUtils;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.sql.*;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logs trades to an SQLite database.
 */
public class SQLiteTradeLogger implements TradeLogger {

    private static final String TRADE_LOGS_FOLDER = "trade-logs";
    private static final String FILE_NAME = "trades.db";
    private static final String TABLE_NAME = "trade";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "time DATETIME NOT NULL, "
            + "player_uuid CHARACTER(36) NOT NULL, "
            + "player_name VARCHAR(16) NOT NULL, "
            + "shop_uuid VARCHAR(36) NOT NULL, "
            + "shop_type VARCHAR(32) NOT NULL, "
            + "shop_world VARCHAR(30) NOT NULL, "
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
            + "shop_uuid, shop_type, shop_world, shop_x, shop_y, shop_z, shop_owner_uuid, shop_owner_name, "
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

        createTable();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionURL);
    }

    private void createTable() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                statement.execute(CREATE_TABLE);
                tableCreated.set(true);
            } catch (SQLException e) {
                Log.severe("Could not create trades table. Trades won't be logged.", e);
            }
        });
    }

    @Override
    public void logTrade(TradeRecord trade) {
        if (!tableCreated.get()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(INSERT_TRADE)) {
                statement.setObject(1, trade.getTimestamp().atOffset(ZoneOffset.UTC)); // time (in UTC)

                statement.setString(2, trade.getPlayer().getUniqueId().toString()); // player_uuid
                statement.setString(3, trade.getPlayer().getName()); // player_name

                statement.setString(4, trade.getShop().getUniqueId().toString()); // shop_uuid
                statement.setString(5, trade.getShop().getTypeId()); // shop_type
                statement.setString(6, trade.getShop().getWorldName()); // shop_world
                statement.setInt(7, trade.getShop().getX()); // shop_x
                statement.setInt(8, trade.getShop().getY()); // shop_y
                statement.setInt(9, trade.getShop().getZ()); // shop_z

                if (trade.getShop().getOwner() != null) {
                    statement.setString(10, trade.getShop().getOwner().getUniqueId().toString()); // shop_owner_uuid
                    statement.setString(11, trade.getShop().getOwner().getName()); // shop_owner_name
                } else {
                    statement.setString(10, null); // shop_owner_uuid
                    statement.setString(11, null); // shop_owner_name
                }

                statement.setString(12, trade.getItem1().getType().name()); // item_1_type
                statement.setInt(13, trade.getItem1().getAmount()); // item_1_amount
                statement.setString(14, getItemMetadata(trade.getItem1())); // item_1_metadata

                if (trade.getItem2() != null) {
                    statement.setString(15, trade.getItem2().getType().name()); // item_2_type
                    statement.setInt(16, trade.getItem2().getAmount()); // item_2_amount
                    statement.setString(17, this.getItemMetadata(trade.getItem2())); // item_2_metadata
                } else {
                    statement.setString(15, null); // item_2_type
                    statement.setNull(16, Types.TINYINT); // item_2_amount
                    statement.setString(17, null); // item_2_metadata
                }

                statement.setString(18, trade.getResultItem().getType().name()); // result_item_type
                statement.setInt(19, trade.getResultItem().getAmount()); // result_item_amount
                statement.setString(20, getItemMetadata(trade.getResultItem())); // result_item_metadata

                statement.setInt(21, trade.getTradeCount()); // trade_count
                statement.execute();
            } catch (SQLException e) {
                Log.severe("Could not not log trade.", e);
            }
        });
    }

    @Override
    public void flush() {
        // We do not need this as each logTrade will commit
    }

    /**
     * See {@link com.nisovin.shopkeepers.tradelog.csv.CsvTradeLogger#getItemMetadata(UnmodifiableItemStack)}.
     */
    private String getItemMetadata(UnmodifiableItemStack itemStack) {
        assert itemStack != null;
        if (Settings.logItemMetadata) return "";

        Map<String, Object> itemData = itemStack.serialize();
        itemData.remove("type");
        itemData.remove("amount");
        return YamlUtils.toCompactYaml(itemData);
    }
}
