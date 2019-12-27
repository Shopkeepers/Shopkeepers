package com.nisovin.shopkeepers.history.storage.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.history.HistoryRequest;
import com.nisovin.shopkeepers.history.HistoryRequest.Range;
import com.nisovin.shopkeepers.history.HistoryResult;
import com.nisovin.shopkeepers.history.ItemInfo;
import com.nisovin.shopkeepers.history.LoggedTrade;
import com.nisovin.shopkeepers.history.PlayerSelector;
import com.nisovin.shopkeepers.history.ShopInfo;
import com.nisovin.shopkeepers.history.ShopSelector;
import com.nisovin.shopkeepers.history.WorldInfo;
import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.player.profile.storage.sql.SQLPlayerProfile;
import com.nisovin.shopkeepers.player.profile.storage.sql.SQLPlayerStorage;
import com.nisovin.shopkeepers.player.profile.storage.sql.SQLPlayerStorage.PlayersTable;
import com.nisovin.shopkeepers.storage.SKStorage.HistoryStorageComponent;
import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.sql.SQL;
import com.nisovin.shopkeepers.storage.sql.SQL.Column;
import com.nisovin.shopkeepers.storage.sql.SQL.CombinedView;
import com.nisovin.shopkeepers.storage.sql.SQL.ForeignKey;
import com.nisovin.shopkeepers.storage.sql.SQL.ForeignKeyJoin;
import com.nisovin.shopkeepers.storage.sql.SQL.Table;
import com.nisovin.shopkeepers.storage.sql.SQL.ViewColumn;
import com.nisovin.shopkeepers.storage.sql.SQLConnector;
import com.nisovin.shopkeepers.storage.sql.SQLStorageComponent;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TimeUtils;
import com.nisovin.shopkeepers.util.Utils;
import com.nisovin.shopkeepers.util.Validate;

public class SQLHistoryStorage extends SQLStorageComponent implements HistoryStorageComponent {

	/*
	 * table: {tablePrefix}worlds
	 * | id [INT, primary] | server_id [VARCHAR(36)] | name [VARCHAR(128)] |
	 * unique(name, server_id) (in this order to also use it as name index)
	 * 
	 * Note: Not storing world uuid, since we are storing server_id instead anyways and world names are unique per server.
	 * name: Null-world (eg. for virtual shopkeepers) is stored as empty String instead of NULL.
	 */
	public static final class WorldsTable {

		private final SQL sql;

		private final String table_name = Settings.databaseTablePrefix + "worlds";
		private final String col_id = "id";
		private final String col_server_id = "server_id";
		private final String col_name = "name";

		public final Table table;
		public final Column id;
		public final Column server_id;
		public final Column name;

		protected WorldsTable(SQL sql) {
			this.sql = sql;

			this.table = sql.table(table_name);
			this.id = table.column(col_id).type("INTEGER").primaryKey().autoIncrement().notNull().build();
			this.server_id = table.column(col_server_id).type("VARCHAR(36)").notNull().build();
			// no world=empty string
			this.name = table.column(col_name).type("VARCHAR(128)").notNull().build();

			// used by MySQL: Case sensitive comparison of utf8 text
			table.extra("ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, DEFAULT COLLATE = utf8mb4_bin");
			table.build();
		}

		protected void setup(SQLConnector connector) throws StorageException {
			// create table:
			connector.createTable(table);

			// create index:
			connector.createIndex(sql.index().unique().table(table).column(name).column(server_id));
		}
	}

	/*
	 * table: {tablePrefix}items
	 * | id [INT, primary] | type [VARCHAR(128), index] | data [VARCHAR(16384)] | hash [INT, 32-bit CRC, index] |
	 * 
	 * Note: Indexes on large BLOB, TEXT or VARCHAR columns might not be supported (eg. in MySQL). Instead, we
	 * additionally store a 32-bit hash (CRC32 stored as signed 32bit integer) over the item's type and data
	 * and use that to quickly find potentially matching rows.
	 * When creating the hash we use no delimiter between the type and data, so that the hash matches the type's hash if there is
	 * no data (or if we don't store the data).
	 * The hash index will likely only be of use if there are many items of the same type but with differing data.
	 * 
	 * Index on item type allows for searching for trades involving a specific type of item.
	 * data: 16384 is max character count for utf8mb4. We expect ~64kB (- size of other columns) to be more than enough for
	 * all commonly used serialized item data. Using VARCHAR instead of TEXT, since they support roughly the same size (VARCHAR
	 * is limited by total row size though) and TEXT might get off-loaded / perform worse.
	 * data is not null. An item with no data is stored as empty String.
	 * TODO store the data version somewhere and update stored items?
	 */
	public static final class ItemsTable {

		private final SQL sql;

		private final String table_name = Settings.databaseTablePrefix + "items";
		private final String col_id = "id";
		private final String col_type = "type";
		private final String col_data = "data";
		private final String col_hash = "hash";

		public final Table table;
		public final Column id;
		public final Column type;
		public final Column data;
		public final Column hash;

		protected ItemsTable(SQL sql) {
			this.sql = sql;

			this.table = sql.table(table_name);
			this.id = table.column(col_id).type("INTEGER").primaryKey().autoIncrement().notNull().build();
			this.type = table.column(col_type).type("VARCHAR(128)").notNull().build();
			// no data=empty string
			this.data = table.column(col_data).type("VARCHAR(16384)").notNull().build();
			this.hash = table.column(col_hash).type("INTEGER").notNull().build();

			// used by MySQL: Case sensitive comparison of utf8 text
			table.extra("ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, DEFAULT COLLATE = utf8mb4_bin");
			table.build();
		}

		protected void setup(SQLConnector connector) throws StorageException {
			// create table:
			connector.createTable(table);

			// create indices:
			connector.createIndex(sql.index().table(table).column(type));
			connector.createIndex(sql.index().table(table).column(hash));
		}
	}

	/*
	 * table: {tablePrefix}shops
	 * | id [INT, primary] | uuid [CHAR(36), index] | type [VARCHAR(128), index] | owner_id [INT, FK, nullable, index] |
	 * | name [VARCHAR(128), index] | world_id [INT, FK, index] | x [INT] | y [INT] | z [INT] | hash [INT, 32-bit CRC] |
	 * 
	 * owner_id is null for admin shops.
	 * Not using an index on 'type', since checking whether the shop has an owner is sufficient for differentiating between admin
	 * and player shops.
	 * Note: This is the data required for the history, not an alternative shopkeepers storage. The shopkeeper might not actually
	 * be existing anymore, and there can be multiple entries for the same shopkeeper if some of its attributes (owner, location,
	 * type, name, ..) have changed.
	 * Citizens shopkeepers can move around and constantly change their location. And shopkeepers might principally change their
	 * name often. This is not considered an issue regarding duplicate data (it's still better than saving all data in-line for
	 * every trade). However, finding an already existing entry gets more difficulty as the number of shops with the same uuid
	 * increases. -> Using a hash column (CRC32, stored as signed 32 bit) to quickly find fully matching rows.
	 */
	public static final class ShopsTable {

		private final SQL sql;

		private final String table_name = Settings.databaseTablePrefix + "shops";

		private final String role_owner = "owner";
		private final String role_world = "world";

		private final String col_id = "id";
		private final String col_uuid = "uuid";
		private final String col_type = "type";
		private final String col_owner_id = role_owner + "_id";
		private final String col_name = "name";
		private final String col_world_id = role_world + "_id";
		private final String col_x = "x";
		private final String col_y = "y";
		private final String col_z = "z";
		private final String col_hash = "hash";

		public final Table table;
		public final Column id;
		public final Column uuid;
		public final Column type;
		public final Column owner_id;
		public final Column name;
		public final Column world_id;
		public final Column x;
		public final Column y;
		public final Column z;
		public final Column hash;

		private final String combined_view_name = table_name + "_combined_view";
		private final CombinedView combined_view;

		protected ShopsTable(SQL sql, PlayersTable table_players, WorldsTable table_worlds) {
			this.sql = sql;

			this.table = sql.table(table_name);
			this.id = table.column(col_id).type("INTEGER").primaryKey().autoIncrement().notNull().build();
			this.uuid = table.column(col_uuid).type("CHAR(36)").notNull().build();
			this.type = table.column(col_type).type("VARCHAR(128)").notNull().build();
			// can be null
			this.owner_id = table.column(col_owner_id).type("INTEGER").build();
			// can be empty
			this.name = table.column(col_name).type("VARCHAR(128)").notNull().build();
			this.world_id = table.column(col_world_id).type("INTEGER").notNull().build();
			this.x = table.column(col_x).type("INTEGER").notNull().build();
			this.y = table.column(col_y).type("INTEGER").notNull().build();
			this.z = table.column(col_z).type("INTEGER").notNull().build();
			this.hash = table.column(col_hash).type("INTEGER").notNull().build();

			ForeignKey fk_owner = table.foreignKey();
			fk_owner.column(owner_id).referencedTable(table_players.table).referencedColumn(table_players.id);

			ForeignKey fk_world = table.foreignKey();
			fk_world.column(world_id).referencedTable(table_worlds.table).referencedColumn(table_worlds.id);

			// used by MySQL: Case sensitive comparison of utf8 text
			table.extra("ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, DEFAULT COLLATE = utf8mb4_bin");
			table.build();

			// combined view:
			this.combined_view = sql.combinedView(combined_view_name)
					.table(table)
					.join(new ForeignKeyJoin(table, table_players.table, role_owner, fk_owner))
					.join(new ForeignKeyJoin(table, table_worlds.table, role_world, fk_world))
					.omitReferencedColumns(false) // include referenced id columns
					.roleDelimiter(SQL.DEFAULT_COLUMN_ROLE_DELIMITER)
					.build();
		}

		protected void setup(SQLConnector connector) throws StorageException {
			// create table:
			connector.createTable(table);
			// create indices:
			connector.createIndex(sql.index().table(table).column(uuid));
			connector.createIndex(sql.index().table(table).column(owner_id));
			connector.createIndex(sql.index().table(table).column(name));
			connector.createIndex(sql.index().table(table).column(world_id));
			connector.createIndex(sql.index().table(table).column(hash));

			// create combined view:
			// first drop previous view if it exists (avoids checking/tracking if the view is still up-to-date):
			connector.dropView(combined_view.getView());
			connector.createView(combined_view.getView());
		}
	}

	/*
	 * table: {tablePrefix}trades
	 * | id [INT, primary] | timestamp [DateTime, index] | player_id [INT, FK, index] | shop_id [INT, FK, index] |
	 * | item1_id [INT, FK, index] | item1_amount [INT] | item2_id [INT, FK, nullable, index] | item2_amount [INT]
	 * | result_item_id [INT, FK, index] | result_item_amount [INT] |
	 */
	public static final class TradesTable {

		private final SQL sql;

		private final String table_name = Settings.databaseTablePrefix + "trades";

		private final String role_player = "player";
		private final String role_shop = "shop";
		private final String role_item1 = "item1";
		private final String role_item2 = "item2";
		private final String role_result_item = "result_item";

		private final String col_id = "id";
		private final String col_timestamp = "timestamp";
		private final String col_player_id = role_player + "_id";
		private final String col_shop_id = role_shop + "_id";
		private final String col_item1_id = role_item1 + "_id";
		private final String col_item1_amount = role_item1 + "_amount";
		private final String col_item2_id = role_item2 + "_id";
		private final String col_item2_amount = role_item2 + "_amount";
		private final String col_result_item_id = role_result_item + "_id";
		private final String col_result_item_amount = role_result_item + "_amount";

		public final Table table;
		public final Column id;
		public final Column timestamp;
		public final Column player_id;
		public final Column shop_id;
		public final Column item1_id;
		public final Column item1_amount;
		public final Column item2_id;
		public final Column item2_amount;
		public final Column result_item_id;
		public final Column result_item_amount;

		private final String combined_view_name = table_name + "_combined_view";
		private final TradesCombinedView combined_view;

		protected TradesTable(SQL sql, PlayersTable table_players, ItemsTable table_items, ShopsTable table_shops, WorldsTable table_worlds) {
			this.sql = sql;

			this.table = sql.table(table_name);
			this.id = table.column(col_id).type("INTEGER").primaryKey().autoIncrement().notNull().build();
			this.timestamp = table.column(col_timestamp).type(sql.dateTimeType()).notNull().build();
			this.player_id = table.column(col_player_id).type("INTEGER").notNull().build();
			this.shop_id = table.column(col_shop_id).type("INTEGER").notNull().build();
			this.item1_id = table.column(col_item1_id).type("INTEGER").notNull().build();
			this.item1_amount = table.column(col_item1_amount).type("INTEGER").notNull().build();
			// can be null
			this.item2_id = table.column(col_item2_id).type("INTEGER").build();
			this.item2_amount = table.column(col_item2_amount).type("INTEGER").notNull().build();
			this.result_item_id = table.column(col_result_item_id).type("INTEGER").notNull().build();
			this.result_item_amount = table.column(col_result_item_amount).type("INTEGER").notNull().build();

			ForeignKey fk_player = table.foreignKey();
			fk_player.column(player_id).referencedTable(table_players.table).referencedColumn(table_players.id);

			ForeignKey fk_shop = table.foreignKey();
			fk_shop.column(shop_id).referencedTable(table_shops.table).referencedColumn(table_shops.id);

			ForeignKey fk_item1 = table.foreignKey();
			fk_item1.column(item1_id).referencedTable(table_items.table).referencedColumn(table_items.id);

			ForeignKey fk_item2 = table.foreignKey();
			fk_item2.column(item2_id).referencedTable(table_items.table).referencedColumn(table_items.id);

			ForeignKey fk_result_item = table.foreignKey();
			fk_result_item.column(result_item_id).referencedTable(table_items.table).referencedColumn(table_items.id);

			// used by MySQL: Case sensitive comparison of utf8 text
			table.extra("ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, DEFAULT COLLATE = utf8mb4_bin");
			table.build();

			// combined view:
			ForeignKey fk_owner = table_shops.table.getForeignKey(table_shops.owner_id.name());
			ForeignKey fk_world = table_shops.table.getForeignKey(table_shops.world_id.name());
			this.combined_view = new TradesCombinedView(this, table_shops, sql.combinedView(combined_view_name)
					.table(table)
					.join(new ForeignKeyJoin(table, table_players.table, role_player, fk_player))
					.join(new ForeignKeyJoin(table, table_shops.table, role_shop, fk_shop))
					.join(new ForeignKeyJoin(table_shops.table, role_shop, table_players.table, table_shops.role_owner, fk_owner))
					.join(new ForeignKeyJoin(table_shops.table, role_shop, table_worlds.table, table_shops.role_world, fk_world))
					.join(new ForeignKeyJoin(table, table_items.table, role_item1, fk_item1))
					.join(new ForeignKeyJoin(table, table_items.table, role_item2, fk_item2))
					.join(new ForeignKeyJoin(table, table_items.table, role_result_item, fk_result_item))
					.omitReferencedColumns(false) // include referenced id columns
					.roleDelimiter(SQL.DEFAULT_COLUMN_ROLE_DELIMITER)
					.build());
		}

		protected void setup(SQLConnector connector) throws StorageException {
			// create table:
			connector.createTable(table);

			// create indices:
			connector.createIndex(sql.index().table(table).column(timestamp));
			connector.createIndex(sql.index().table(table).column(player_id));
			connector.createIndex(sql.index().table(table).column(shop_id));
			connector.createIndex(sql.index().table(table).column(item1_id));
			connector.createIndex(sql.index().table(table).column(item2_id));
			connector.createIndex(sql.index().table(table).column(result_item_id));

			// create combined view:
			// first drop previous view if it exists (avoids checking/tracking if the view is still up-to-date):
			connector.dropView(combined_view.view);
			connector.createView(combined_view.view);
		}
	}

	public static final class TradesCombinedView {

		private final TradesTable table_trades;
		private final ShopsTable table_shops;
		public final CombinedView view;

		public TradesCombinedView(TradesTable table_trades, ShopsTable table_shops, CombinedView view) {
			this.table_trades = table_trades;
			this.table_shops = table_shops;
			this.view = view;
		}

		public String name() {
			return view.name();
		}

		// quoted name
		public String qn() {
			return view.qn();
		}

		// columns

		public ViewColumn trade(Column column) {
			return view.getColumn(column.name());
		}

		public ViewColumn player(Column column) {
			return view.getColumn(column.name(), table_trades.role_player);
		}

		public ViewColumn shop(Column column) {
			return view.getColumn(column.name(), table_trades.role_shop);
		}

		public ViewColumn shopOwner(Column column) {
			return view.getColumn(column.name(), table_trades.role_shop, table_shops.role_owner);
		}

		public ViewColumn shopWorld(Column column) {
			return view.getColumn(column.name(), table_trades.role_shop, table_shops.role_world);
		}

		public ViewColumn item1(Column column) {
			return view.getColumn(column.name(), table_trades.role_item1);
		}

		public ViewColumn item2(Column column) {
			return view.getColumn(column.name(), table_trades.role_item2);
		}

		public ViewColumn resultItem(Column column) {
			return view.getColumn(column.name(), table_trades.role_result_item);
		}
	}

	protected final SQLPlayerStorage playerStorage;
	protected final PlayersTable table_players;
	protected final WorldsTable table_worlds;
	protected final ItemsTable table_items;
	protected final ShopsTable table_shops;
	protected final TradesTable table_trades;

	// not final to allow for overriding in subclasses:
	protected String GET_WORLD_BY_ID_SQL;
	protected String GET_WORLD_ID_SQL;
	protected String ADD_WORLD_SQL;

	protected String GET_ITEM_BY_ID_SQL;
	protected String GET_ITEM_ID_SQL;
	protected String ADD_ITEM_SQL;

	protected String GET_SHOP_BY_ID_SQL;
	protected String GET_SHOP_ID_SQL;
	protected String ADD_SHOP_SQL;

	protected String GET_TRADE_BY_ID_SQL;
	protected String ADD_TRADE_SQL;

	protected String GET_ALL_TRADES_SQL;
	protected String GET_TRADES_WITH_ADMIN_SHOPS_SQL;
	protected String GET_TRADES_WITH_PLAYER_SHOPS_SQL;
	protected String GET_TRADES_WITH_OWNER_SQL;
	protected String GET_TRADES_WITH_SHOP_SQL;
	protected String GET_TRADES_WITH_SHOP_BY_NAME_SQL;
	protected String GET_TRADES_WITH_OWNED_SHOP_SQL;
	protected String GET_TRADES_WITH_OWNED_SHOP_BY_NAME_SQL;

	protected String GET_ALL_PLAYER_TRADES_SQL;
	protected String GET_PLAYER_TRADES_WITH_ADMIN_SHOPS_SQL;
	protected String GET_PLAYER_TRADES_WITH_PLAYER_SHOPS_SQL;
	protected String GET_PLAYER_TRADES_WITH_OWNER_SQL;
	protected String GET_PLAYER_TRADES_WITH_SHOP_SQL;
	protected String GET_PLAYER_TRADES_WITH_SHOP_BY_NAME_SQL;
	protected String GET_PLAYER_TRADES_WITH_OWNED_SHOP_SQL;
	protected String GET_PLAYER_TRADES_WITH_OWNED_SHOP_BY_NAME_SQL;

	public SQLHistoryStorage(SQLConnector connector, SQLPlayerStorage playerStorage) {
		super("trading-history-storage", connector);
		this.playerStorage = playerStorage;
		this.table_players = playerStorage.getPlayersTable();
		this.table_worlds = new WorldsTable(sql);
		this.table_items = new ItemsTable(sql);
		this.table_shops = new ShopsTable(sql, table_players, table_worlds);
		this.table_trades = new TradesTable(sql, table_players, table_items, table_shops, table_worlds);
	}

	@Override
	protected void onSetup() throws StorageException {
		super.onSetup();
		// tables, views and indices:
		table_worlds.setup(connector);
		table_items.setup(connector);
		table_shops.setup(connector);
		table_trades.setup(connector);

		// prepare SQL statements:
		GET_WORLD_BY_ID_SQL = "SELECT *"
				+ " FROM " + table_worlds.table.qn()
				+ " WHERE " + table_worlds.id.qn() + "=?"
				+ " LIMIT 1;";
		GET_WORLD_ID_SQL = "SELECT " + table_worlds.id.qn()
				+ " FROM " + table_worlds.table.qn()
				+ " WHERE " + table_worlds.server_id.qn() + "=?"
				+ " AND " + table_worlds.name.qn() + "=?"
				+ " LIMIT 1;";
		ADD_WORLD_SQL = "INSERT " + sql.ignore()
				+ " INTO " + table_worlds.table.qn()
				+ " (" + table_worlds.server_id.qn()
				+ "," + table_worlds.name.qn()
				+ ")" + " VALUES(?,?);";

		GET_ITEM_BY_ID_SQL = "SELECT *"
				+ " FROM " + table_items.table.qn()
				+ " WHERE " + table_items.id.qn() + "=?"
				+ " LIMIT 1;";
		GET_ITEM_ID_SQL = "SELECT " + table_items.id.qn()
				+ " FROM " + table_items.table.qn()
				+ " WHERE " + table_items.hash.qn() + "=?"
				+ " AND " + table_items.type.qn() + "=?"
				+ " AND " + table_items.data.qn() + "=?"
				+ " LIMIT 1;";
		ADD_ITEM_SQL = "INSERT " + sql.ignore()
				+ " INTO " + table_items.table.qn()
				+ " (" + table_items.type.qn()
				+ "," + table_items.data.qn()
				+ "," + table_items.hash.qn()
				+ ")" + " VALUES(?,?,?);";

		GET_SHOP_BY_ID_SQL = "SELECT *"
				+ " FROM " + table_shops.table.qn()
				+ " WHERE " + table_shops.id.qn() + "=?"
				+ " LIMIT 1;";
		GET_SHOP_ID_SQL = "SELECT " + table_shops.id.qn()
				+ " FROM " + table_shops.table.qn()
				+ " WHERE " + table_shops.hash.qn() + "=?"
				+ " AND " + table_shops.uuid.qn() + "=?"
				+ " AND " + table_shops.type.qn() + "=?"
				+ " AND " + table_shops.owner_id.qn() + "=? OR (" + table_shops.owner_id.qn() + " IS NULL AND ? IS NULL)"
				+ " AND " + table_shops.name.qn() + "=?"
				+ " AND " + table_shops.world_id.qn() + "=?"
				+ " AND " + table_shops.x.qn() + "=?"
				+ " AND " + table_shops.y.qn() + "=?"
				+ " AND " + table_shops.z.qn() + "=?"
				+ " LIMIT 1;";
		ADD_SHOP_SQL = "INSERT " + sql.ignore()
				+ " INTO " + table_shops.table.qn()
				+ " (" + table_shops.uuid.qn()
				+ "," + table_shops.type.qn()
				+ "," + table_shops.owner_id.qn()
				+ "," + table_shops.name.qn()
				+ "," + table_shops.world_id.qn()
				+ "," + table_shops.x.qn()
				+ "," + table_shops.y.qn()
				+ "," + table_shops.z.qn()
				+ "," + table_shops.hash.qn()
				+ ")" + " VALUES(?,?,?,?,?,?,?,?,?);";

		GET_TRADE_BY_ID_SQL = "SELECT *"
				+ " FROM " + table_trades.table.qn()
				+ " WHERE " + table_trades.id.qn() + "=?"
				+ " LIMIT 1;";
		ADD_TRADE_SQL = "INSERT " + sql.ignore()
				+ " INTO " + table_trades.table.qn()
				+ " (" + table_trades.timestamp.qn()
				+ "," + table_trades.player_id.qn()
				+ "," + table_trades.shop_id.qn()
				+ "," + table_trades.item1_id.qn()
				+ "," + table_trades.item1_amount.qn()
				+ "," + table_trades.item2_id.qn()
				+ "," + table_trades.item2_amount.qn()
				+ "," + table_trades.result_item_id.qn()
				+ "," + table_trades.result_item_amount.qn()
				+ ")" + " VALUES(?,?,?,?,?,?,?,?,?);";

		// get trades of all players
		GET_ALL_TRADES_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		// assumption: all player shops always have an owner and all admin shops always have no owner
		GET_TRADES_WITH_ADMIN_SHOPS_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shopOwner(table_players.id).qn() + " IS NULL"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_TRADES_WITH_PLAYER_SHOPS_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shopOwner(table_players.id).qn() + " IS NOT NULL"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_TRADES_WITH_OWNER_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shopOwner(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_TRADES_WITH_SHOP_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shop(table_shops.uuid).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_TRADES_WITH_SHOP_BY_NAME_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shop(table_shops.name).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_TRADES_WITH_OWNED_SHOP_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shop(table_shops.uuid).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_TRADES_WITH_OWNED_SHOP_BY_NAME_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.shop(table_shops.name).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";

		// get trades of specific player
		GET_ALL_PLAYER_TRADES_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_ADMIN_SHOPS_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + " IS NULL"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_PLAYER_SHOPS_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + " IS NOT NULL"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_OWNER_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_SHOP_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shop(table_shops.uuid).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_SHOP_BY_NAME_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shop(table_shops.name).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_OWNED_SHOP_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shop(table_shops.uuid).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
		GET_PLAYER_TRADES_WITH_OWNED_SHOP_BY_NAME_SQL = "SELECT *"
				+ " FROM " + table_trades.combined_view.qn()
				+ " WHERE " + table_trades.combined_view.player(table_players.id).qn() + "=?"
				+ " AND " + table_trades.combined_view.shop(table_shops.name).qn() + "=?"
				+ " AND " + table_trades.combined_view.shopOwner(table_players.id).qn() + "=?"
				+ " ORDER BY " + table_trades.timestamp.qn() + " DESC"
				+ " LIMIT ? OFFSET ?;";
	}

	private String toTradesCountSQL(String getTradesSQL) {
		String tradesCountSQL = getTradesSQL;
		tradesCountSQL = StringUtils.replaceFirst(tradesCountSQL, "SELECT *", "SELECT COUNT(*)");
		tradesCountSQL = StringUtils.replaceFirst(tradesCountSQL, " ORDER BY " + table_trades.timestamp.qn() + " DESC", "");
		tradesCountSQL = StringUtils.replaceFirst(tradesCountSQL, " LIMIT ? OFFSET ?", "");
		return tradesCountSQL;
	}

	// HISTORY

	@Override
	public void purgeTradesOlderThan(Duration duration) throws StorageException {
		this.validateNotShutdown();
		// TODO
	}

	// removes no longer referenced data
	@Override
	public void performCleanup() throws StorageException {
		this.validateNotShutdown();
		// TODO
	}

	// combines various startup cleanup and migration tasks
	@Override
	public void onStartup() throws StorageException {
		this.validateNotShutdown();
		// TODO
		// remove no longer needed data (data that isn't referenced anymore):
		// purge outdated data if configured to do so automatically:
		// update stored items:
	}

	// WORLD INFO

	// returns the world_id, or throws an exception
	public int getOrInsertWorld(WorldInfo world) throws StorageException {
		assert !this.isShutdown();
		assert world != null;

		String serverId = world.getServerId();
		// null world name is stored as empty String
		String worldName = StringUtils.getNotNull(world.getWorldName());

		return connector.getOrInsertObject("world info", world, table_worlds.id,
				() -> {
					PreparedStatement stmt = connector.prepareStatement(GET_WORLD_ID_SQL);
					stmt.setString(1, serverId);
					stmt.setString(2, worldName);
					return stmt;
				},
				() -> {
					PreparedStatement stmt = connector.prepareStatement(ADD_WORLD_SQL, true);
					stmt.setString(1, serverId);
					stmt.setString(2, worldName);
					return stmt;
				}, Settings.isDebugging(Settings.DebugOptions.database)); // log new insert if debugging
	}

	public WorldInfo parseWorldInfo(String columnPrefix, ResultSet resultSet) throws SQLException {
		assert columnPrefix != null && resultSet != null;
		String serverId = resultSet.getString(columnPrefix + table_worlds.server_id.name());
		// convert empty world name to null:
		String worldName = StringUtils.getNotEmpty(resultSet.getString(columnPrefix + table_worlds.name.name()));
		return new WorldInfo(serverId, worldName);
	}

	// ITEM INFO

	// returns the item_id, or throws an exception
	// item stack amount does not get stored here
	public int getOrInsertItem(ItemInfo item) throws StorageException {
		assert !this.isShutdown();
		assert item != null;

		String itemType = item.getItemType();
		// null item data is stored as empty String
		// this may serialize the ItemStack's data asynchronously
		String itemData = StringUtils.getNotNull(item.getItemData());
		// no delimiter, so that the hash matches CRC32(type) in case that the item data is omitted
		int hash = Utils.calculateCRC32("", itemType, itemData);

		return connector.getOrInsertObject("item info", item, table_items.id,
				() -> {
					PreparedStatement stmt = connector.prepareStatement(GET_ITEM_ID_SQL);
					stmt.setInt(1, hash);
					stmt.setString(2, itemType);
					stmt.setString(3, itemData);
					return stmt;
				},
				() -> {
					PreparedStatement stmt = connector.prepareStatement(ADD_ITEM_SQL, true);
					stmt.setString(1, itemType);
					stmt.setString(2, itemData);
					stmt.setInt(3, hash);
					return stmt;
				}, Settings.isDebugging(Settings.DebugOptions.database)); // log new insert if debugging
	}

	// amount gets stored separately
	public ItemInfo tradesCombinedView_parseItemInfo(String itemRole, ResultSet resultSet, int amount) throws SQLException {
		assert itemRole != null && resultSet != null;
		CombinedView view = table_trades.combined_view.view;
		String itemPrefix = itemRole + view.roleDelimiter();

		String itemType = resultSet.getString(itemPrefix + table_items.type.name());
		if (itemType == null) return null;
		// convert empty item data to null:
		String itemData = StringUtils.getNotEmpty(resultSet.getString(itemPrefix + table_items.data.name()));
		ItemInfo itemInfo = new ItemInfo(itemType, itemData, amount);
		itemInfo.getItemStack(); // deserialize ItemStack asynchronously
		return itemInfo;
	}

	// SHOP INFO

	// returns the shop_id, or throws an exception
	public int getOrInsertShop(ShopInfo shop) throws StorageException {
		assert !this.isShutdown();
		assert shop != null;
		String uuid = shop.getUniqueId().toString();
		String shopType = shop.getTypeId();
		PlayerProfile owner = shop.getOwner(); // can be null
		String shopName = shop.getName(); // can be empty
		WorldInfo world = shop.getWorld();
		int x = shop.getX();
		int y = shop.getY();
		int z = shop.getZ();

		// get or insert owner:
		Integer ownerId = (owner == null) ? null : playerStorage.getOrInsertProfile(owner);

		// get or insert world:
		int worldId = this.getOrInsertWorld(world);

		// shop hash:
		int hash = Utils.calculateCRC32("|", uuid, shopType, ownerId, shopName, worldId, x, y, z);

		return connector.getOrInsertObject("shop info", shop, table_shops.id,
				() -> {
					PreparedStatement stmt = connector.prepareStatement(GET_SHOP_ID_SQL);
					stmt.setInt(1, hash);
					stmt.setString(2, uuid);
					stmt.setString(3, shopType);
					stmt.setObject(4, ownerId, Types.INTEGER); // can be null
					stmt.setObject(5, ownerId, Types.INTEGER); // can be null
					stmt.setString(6, shopName);
					stmt.setInt(7, worldId);
					stmt.setInt(8, x);
					stmt.setInt(9, y);
					stmt.setInt(10, z);
					return stmt;
				},
				() -> {
					PreparedStatement stmt = connector.prepareStatement(ADD_SHOP_SQL, true);
					stmt.setString(1, uuid);
					stmt.setString(2, shopType);
					stmt.setObject(3, ownerId, Types.INTEGER); // can be null
					stmt.setString(4, shopName);
					stmt.setInt(5, worldId);
					stmt.setInt(6, x);
					stmt.setInt(7, y);
					stmt.setInt(8, z);
					stmt.setInt(9, hash);
					return stmt;
				}, Settings.isDebugging(Settings.DebugOptions.database)); // log new insert if debugging
	}

	public ShopInfo tradesCombinedView_parseShopInfo(ResultSet resultSet) throws SQLException {
		assert resultSet != null;
		CombinedView view = table_trades.combined_view.view;
		String shopPrefix = table_trades.role_shop + view.roleDelimiter();
		String shopOwnerPrefix = shopPrefix + table_shops.role_owner + view.roleDelimiter();
		String shopWorldPrefix = shopPrefix + table_shops.role_world + view.roleDelimiter();

		UUID shopUUID = UUID.fromString(resultSet.getString(shopPrefix + table_shops.uuid.name()));
		String shopTypeId = resultSet.getString(shopPrefix + table_shops.type.name());
		PlayerProfile owner = playerStorage.parseProfile(shopOwnerPrefix, resultSet); // can be null
		String shopName = resultSet.getString(shopPrefix + table_shops.name.name());
		WorldInfo world = this.parseWorldInfo(shopWorldPrefix, resultSet);
		int x = resultSet.getInt(shopPrefix + table_shops.x.name());
		int y = resultSet.getInt(shopPrefix + table_shops.y.name());
		int z = resultSet.getInt(shopPrefix + table_shops.z.name());
		return new ShopInfo(shopUUID, shopTypeId, owner, shopName, world, x, y, z);
	}

	// TRADES

	@Override
	public void logTrade(LoggedTrade trade) throws StorageException {
		this.validateNotShutdown();
		Validate.notNull(trade);

		Timestamp timestamp = Timestamp.from(trade.getTimestamp());
		PlayerProfile player = trade.getPlayer();
		ShopInfo shop = trade.getShop();
		ItemInfo item1 = trade.getItem1();
		int item1Amount = item1.getAmount();
		ItemInfo item2 = trade.getItem2(); // can be null
		int item2Amount = (item2 == null) ? 0 : item2.getAmount();
		ItemInfo resultItem = trade.getResultItem();
		int resultItemAmount = resultItem.getAmount();
		Calendar utcCalendar = TimeUtils.UTC_CALENDAR.get();

		try {
			connector.execute(() -> {
				Connection connection = connector.getConnection();
				return connector.performTransaction(connection, () -> {

					// get or insert player:
					int playerId = playerStorage.getOrInsertProfile(player);

					// get or insert items:
					int item1Id = this.getOrInsertItem(item1);
					Integer item2Id = (item2 == null) ? null : this.getOrInsertItem(item2);
					int resultItemId = this.getOrInsertItem(resultItem);

					// get or insert shop:
					int shopId = this.getOrInsertShop(shop);

					// insert trade:
					PreparedStatement stmt = null;
					try {
						stmt = connector.prepareStatement(ADD_TRADE_SQL, true);
						stmt.setTimestamp(1, timestamp, utcCalendar);
						stmt.setInt(2, playerId);
						stmt.setInt(3, shopId);
						stmt.setInt(4, item1Id);
						stmt.setInt(5, item1Amount);
						stmt.setObject(6, item2Id, Types.INTEGER); // can be null
						stmt.setInt(7, item2Amount);
						stmt.setInt(8, resultItemId);
						stmt.setInt(9, resultItemAmount);
						stmt.executeUpdate();
					} finally {
						connector.clearParameters(stmt);
					}

					// log new inserts if debugging:
					Log.debug(Settings.DebugOptions.database, () -> logPrefix + "Trade logged: " + trade);
					return null;
				});
			});
		} catch (Exception e) {
			throw new StorageException("Could not log trade: " + trade, e);
		}
	}

	public PlayerProfile tradesCombinedView_parsePlayerProfile(ResultSet resultSet) throws SQLException {
		assert resultSet != null;
		CombinedView view = table_trades.combined_view.view;
		String playerPrefix = table_trades.role_player + view.roleDelimiter();
		return playerStorage.parseProfile(playerPrefix, resultSet);
	}

	protected LoggedTrade tradesCombinedView_parseTrade(ResultSet resultSet) throws SQLException {
		assert resultSet != null;
		TradesCombinedView combinedView = table_trades.combined_view;
		Calendar utcCalendar = TimeUtils.UTC_CALENDAR.get();
		Instant timestamp = resultSet.getTimestamp(combinedView.trade(table_trades.timestamp).name(), utcCalendar).toInstant();
		PlayerProfile player = this.tradesCombinedView_parsePlayerProfile(resultSet);
		ShopInfo shop = this.tradesCombinedView_parseShopInfo(resultSet);
		int item1Amount = resultSet.getInt(combinedView.trade(table_trades.item1_amount).name());
		int item2Amount = resultSet.getInt(combinedView.trade(table_trades.item2_amount).name());
		int resultItemAmount = resultSet.getInt(combinedView.trade(table_trades.result_item_amount).name());
		ItemInfo item1 = this.tradesCombinedView_parseItemInfo(table_trades.role_item1, resultSet, item1Amount);
		ItemInfo item2 = this.tradesCombinedView_parseItemInfo(table_trades.role_item2, resultSet, item2Amount);
		ItemInfo resultItem = this.tradesCombinedView_parseItemInfo(table_trades.role_result_item, resultSet, resultItemAmount);
		return new LoggedTrade(timestamp, player, shop, item1, item2, resultItem);
	}

	private static class RequestContext {

		public boolean allPlayers = false;
		// if not null, the player is looked up by either name or uuid:
		public String lookupPlayerName = null;
		public UUID lookupPlayerUUID = null;
		public Integer playerId = null;

		// if not null, the owner is looked up by either name or uuid:
		public String lookupOwnerName = null;
		public UUID lookupOwnerUUID = null;
		public Integer ownerId = null;

		public int totalTradesCount = 0; // gets set once it has been looked up
		public final Range range;

		private RequestContext(Range range) {
			assert range != null;
			this.range = range;
		}
	}

	protected class GetTradesStmts {

		private final String allPlayersQuery;
		private final String singlePlayerQuery;

		protected GetTradesStmts(String allPlayersQuery, String singlePlayerQuery) {
			this.allPlayersQuery = allPlayersQuery;
			this.singlePlayerQuery = singlePlayerQuery;
		}

		// These specific parameters get inserted in between the player id (first parameter, if used for the query) and
		// the limit and offset (last parameters, if used by the query)
		protected Object[] getSpecificParams(RequestContext context) {
			return null;
		}

		public PreparedStatement getPreparedGetTradesCountStmt(RequestContext context) throws Exception {
			String query;
			if (context.playerId == null) {
				query = allPlayersQuery;
			} else {
				query = singlePlayerQuery;
			}
			PreparedStatement stmt = connector.prepareStatement(toTradesCountSQL(query));

			// parameters:
			int parameterOffset = 0;
			if (context.playerId != null) {
				connector.setParameters(stmt, context.playerId);
				++parameterOffset;
			}

			Object[] specificParams = this.getSpecificParams(context);
			if (specificParams != null && specificParams.length > 0) {
				connector.setParametersOffset(stmt, parameterOffset, specificParams);
				parameterOffset += specificParams.length;
			}
			return stmt;
		}

		public PreparedStatement getPreparedGetTradesStmt(RequestContext context) throws Exception {
			String query;
			if (context.playerId == null) {
				query = allPlayersQuery;
			} else {
				query = singlePlayerQuery;
			}
			PreparedStatement stmt = connector.prepareStatement(query);

			// parameters:
			int parameterOffset = 0;
			if (context.playerId != null) {
				connector.setParameters(stmt, context.playerId);
				++parameterOffset;
			}

			Object[] specificParams = this.getSpecificParams(context);
			if (specificParams != null && specificParams.length > 0) {
				connector.setParametersOffset(stmt, parameterOffset, specificParams);
				parameterOffset += specificParams.length;
			}

			Range range = context.range;
			int startIndex = range.getStartIndex(context.totalTradesCount);
			int endIndex = range.getEndIndex(context.totalTradesCount);
			int offset = startIndex;
			int limit = (endIndex - startIndex);
			connector.setParametersOffset(stmt, parameterOffset, limit, offset);
			return stmt;
		}
	}

	@Override
	public HistoryResult getTradingHistory(HistoryRequest request) throws StorageException {
		this.validateNotShutdown();
		Validate.notNull(request, "History request is null!");
		PlayerSelector playerSelector = request.playerSelector;
		ShopSelector shopSelector = request.shopSelector;
		Range range = request.range;
		assert playerSelector != null && shopSelector != null && range != null;

		RequestContext context = new RequestContext(request.range);

		if (playerSelector == PlayerSelector.ALL) {
			context.allPlayers = true;
		} else if (playerSelector instanceof PlayerSelector.ByName) {
			context.lookupPlayerName = ((PlayerSelector.ByName) playerSelector).getPlayerName();
		} else if (playerSelector instanceof PlayerSelector.ByUUID) {
			context.lookupPlayerUUID = ((PlayerSelector.ByUUID) playerSelector).getPlayerUUID();
		} else {
			Validate.State.error("Unknown PlayerSelector type: " + playerSelector.getClass().getName());
		}
		assert context.allPlayers || context.lookupPlayerName != null || context.lookupPlayerUUID != null;

		GetTradesStmts getTradesStmts;

		if (shopSelector == ShopSelector.ALL) {
			getTradesStmts = new GetTradesStmts(GET_ALL_TRADES_SQL, GET_ALL_PLAYER_TRADES_SQL);
		} else if (shopSelector == ShopSelector.ADMIN_SHOPS) {
			getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_ADMIN_SHOPS_SQL, GET_PLAYER_TRADES_WITH_ADMIN_SHOPS_SQL);
		} else if (shopSelector == ShopSelector.PLAYER_SHOPS) {
			getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_PLAYER_SHOPS_SQL, GET_PLAYER_TRADES_WITH_PLAYER_SHOPS_SQL);
		} else if (shopSelector instanceof ShopSelector.ByOwner) {
			if (shopSelector instanceof ShopSelector.ByOwnerName) {
				context.lookupOwnerName = ((ShopSelector.ByOwnerName) shopSelector).getOwnerName();
			} else if (shopSelector instanceof ShopSelector.ByOwnerUUID) {
				context.lookupOwnerUUID = ((ShopSelector.ByOwnerUUID) shopSelector).getOwnerUUID();
			} else {
				Validate.State.error("Unknown ByOwner ShopSelector type: " + shopSelector.getClass().getName());
			}

			getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_OWNER_SQL, GET_PLAYER_TRADES_WITH_OWNER_SQL) {
				@Override
				protected Object[] getSpecificParams(RequestContext context) {
					assert context.ownerId != null;
					return new Object[] { context.ownerId };
				}
			};
		} else if (shopSelector instanceof ShopSelector.ByShop) {
			// specific shop:
			ShopSelector.ByShop byShopSelector = ((ShopSelector.ByShop) shopSelector);
			if (shopSelector instanceof ShopSelector.ByShopUUID) {
				String shopUUID = ((ShopSelector.ByShopUUID) shopSelector).getShopUUID().toString();
				if (byShopSelector.getOwnerUUID() == null) {
					getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_SHOP_SQL, GET_PLAYER_TRADES_WITH_SHOP_SQL) {
						@Override
						protected Object[] getSpecificParams(RequestContext context) {
							return new Object[] { shopUUID };
						}
					};
				} else {
					getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_OWNED_SHOP_SQL, GET_PLAYER_TRADES_WITH_OWNED_SHOP_SQL) {
						@Override
						protected Object[] getSpecificParams(RequestContext context) {
							assert context.ownerId != null;
							return new Object[] { shopUUID, context.ownerId };
						}
					};
				}
			} else if (shopSelector instanceof ShopSelector.ByShopName) {
				String shopName = ((ShopSelector.ByShopName) shopSelector).getShopName();
				if (byShopSelector.getOwnerUUID() == null) {
					getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_SHOP_BY_NAME_SQL, GET_PLAYER_TRADES_WITH_SHOP_BY_NAME_SQL) {
						@Override
						protected Object[] getSpecificParams(RequestContext context) {
							return new Object[] { shopName };
						}
					};
				} else {
					getTradesStmts = new GetTradesStmts(GET_TRADES_WITH_OWNED_SHOP_BY_NAME_SQL, GET_PLAYER_TRADES_WITH_OWNED_SHOP_BY_NAME_SQL) {
						@Override
						protected Object[] getSpecificParams(RequestContext context) {
							assert context.ownerId != null;
							return new Object[] { shopName, context.ownerId };
						}
					};
				}
			} else {
				getTradesStmts = null;
				Validate.State.error("Unknown ByShop ShopSelector type: " + shopSelector.getClass().getName());
			}
		} else {
			getTradesStmts = null;
			Validate.State.error("Unexpected shop selector type: " + shopSelector.getClass().getName());
		}

		try {
			return connector.execute(() -> {
				Connection connection = connector.getConnection();
				return connector.performTransaction(connection, () -> {
					boolean lookupPlayer = false;
					SQLPlayerProfile playerProfile = null;

					boolean lookupOwner = false;
					SQLPlayerProfile ownerProfile = null;

					List<LoggedTrade> trades = new ArrayList<>();
					context.totalTradesCount = 0;

					// lookup player profile:
					if (context.lookupPlayerName != null) {
						lookupPlayer = true;
						// TODO perform raw getProfiles here (without retries, error handling, ..)?
						// TODO maybe make all storage methods 'raw' and move retry and error handling into caller
						// class?
						List<PlayerProfile> profiles = playerStorage.getProfiles(context.lookupPlayerName);
						if (!profiles.isEmpty()) {
							playerProfile = (SQLPlayerProfile) profiles.get(0);
						}
					} else if (context.lookupPlayerUUID != null) {
						lookupPlayer = true;
						playerProfile = playerStorage.getProfile(context.lookupPlayerUUID); // can be null
					}
					if (playerProfile != null) {
						context.playerId = playerProfile.getPlayerId();
					}

					// lookup owner profile:
					if (context.lookupOwnerName != null) {
						lookupOwner = true;
						List<PlayerProfile> profiles = playerStorage.getProfiles(context.lookupOwnerName);
						if (!profiles.isEmpty()) {
							ownerProfile = (SQLPlayerProfile) profiles.get(0);
						}
					} else if (context.lookupOwnerUUID != null) {
						lookupOwner = true;
						ownerProfile = playerStorage.getProfile(context.lookupOwnerUUID); // can be null
					}
					if (ownerProfile != null) {
						context.ownerId = ownerProfile.getPlayerId();
					}

					if ((lookupPlayer && playerProfile == null) || (lookupOwner && ownerProfile == null)) {
						// player or owner not found: return empty result
						return new HistoryResult(playerProfile, ownerProfile, trades, context.totalTradesCount);
					}

					// total trades count:
					PreparedStatement getTradesCountStmt = null;
					try {
						getTradesCountStmt = getTradesStmts.getPreparedGetTradesCountStmt(context);
						try (ResultSet resultSet = getTradesCountStmt.executeQuery()) {
							if (resultSet.next()) {
								context.totalTradesCount = resultSet.getInt(1);
							}
						}
					} finally {
						connector.clearParameters(getTradesCountStmt);
					}
					if (context.totalTradesCount == 0) {
						// no trades found:
						return new HistoryResult(playerProfile, ownerProfile, trades, context.totalTradesCount);
					}

					// trades:
					PreparedStatement getTradesStmt = null;
					try {
						getTradesStmt = getTradesStmts.getPreparedGetTradesStmt(context);
						try (ResultSet resultSet = getTradesStmt.executeQuery()) {
							while (resultSet.next()) {
								trades.add(this.tradesCombinedView_parseTrade(resultSet));
							}
						}
					} finally {
						connector.clearParameters(getTradesStmt);
					}

					// return result:
					return new HistoryResult(playerProfile, ownerProfile, trades, context.totalTradesCount);
				});
			});
		} catch (Exception e) {
			throw new StorageException("Could not fetch trading history: " + request.toString(), e);
		}
	}
}
