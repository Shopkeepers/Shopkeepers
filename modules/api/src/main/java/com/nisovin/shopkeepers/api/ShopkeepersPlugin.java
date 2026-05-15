package com.nisovin.shopkeepers.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopTypesRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevelRegistry;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * The Shopkeepers plugin.
 * <p>
 * This is the main entry point to the Shopkeepers plugin API. See {@link ShopkeepersAPI} for
 * convenient to use static accessors to the runtime instance and methods of this interface that can
 * be used while the API is enabled, i.e. while the plugin is enabled.
 */
public interface ShopkeepersPlugin extends Plugin {

	/**
	 * Gets the {@link ShopkeepersPlugin} instance.
	 * 
	 * @return the plugin instance, not <code>null</code>
	 * @throws IllegalStateException
	 *             if the API is not enabled currently, e.g. because the plugin is not enabled
	 *             currently
	 */
	public static ShopkeepersPlugin getInstance() {
		return ShopkeepersAPI.getPlugin();
	}

	// PERMISSIONS

	/**
	 * The <code>shopkeeper.help</code> permission.
	 */
	public static final String HELP_PERMISSION = "shopkeeper.help";
	/**
	 * The <code>shopkeeper.trade</code> permission.
	 */
	public static final String TRADE_PERMISSION = "shopkeeper.trade";
	/**
	 * The <code>shopkeeper.create</code> permission.
	 */
	public static final String CREATE_PERMISSION = "shopkeeper.create";
	/**
	 * The <code>shopkeeper.reload</code> permission.
	 */
	public static final String RELOAD_PERMISSION = "shopkeeper.reload";
	/**
	 * The <code>shopkeeper.debug</code> permission.
	 */
	public static final String DEBUG_PERMISSION = "shopkeeper.debug";
	/**
	 * The <code>shopkeeper.delete-unspawnable-shopkeepers</code> permission.
	 */
	public static final String DELETE_UNSPAWNABLE_SHOPKEEPERS = "shopkeeper.delete-unspawnable-shopkeepers";
	/**
	 * The <code>shopkeeper.cleanup-citizen-shopkeepers</code> permission.
	 */
	public static final String CLEANUP_CITIZEN_SHOPKEEPERS = "shopkeeper.cleanup-citizen-shopkeepers";

	/**
	 * The <code>shopkeeper.list.own</code> permission.
	 */
	public static final String LIST_OWN_PERMISSION = "shopkeeper.list.own";
	/**
	 * The <code>shopkeeper.list.others</code> permission.
	 */
	public static final String LIST_OTHERS_PERMISSION = "shopkeeper.list.others";
	/**
	 * The <code>shopkeeper.list.admin</code> permission.
	 */
	public static final String LIST_ADMIN_PERMISSION = "shopkeeper.list.admin";

	/**
	 * The <code>shopkeeper.history.own</code> permission.
	 */
	public static final String HISTORY_OWN_PERMISSION = "shopkeeper.history.own";
	/**
	 * The <code>shopkeeper.history.admin</code> permission.
	 */
	public static final String HISTORY_ADMIN_PERMISSION = "shopkeeper.history.admin";

	/**
	 * The <code>shopkeeper.remove.own</code> permission.
	 */
	public static final String REMOVE_OWN_PERMISSION = "shopkeeper.remove.own";
	/**
	 * The <code>shopkeeper.remove.others</code> permission.
	 */
	public static final String REMOVE_OTHERS_PERMISSION = "shopkeeper.remove.others";
	/**
	 * The <code>shopkeeper.remove.admin</code> permission.
	 */
	public static final String REMOVE_ADMIN_PERMISSION = "shopkeeper.remove.admin";

	/**
	 * The <code>shopkeeper.remove-all.own</code> permission.
	 */
	public static final String REMOVE_ALL_OWN_PERMISSION = "shopkeeper.remove-all.own";
	/**
	 * The <code>shopkeeper.remove-all.others</code> permission.
	 */
	public static final String REMOVE_ALL_OTHERS_PERMISSION = "shopkeeper.remove-all.others";
	/**
	 * The <code>shopkeeper.remove-all.player</code> permission.
	 */
	public static final String REMOVE_ALL_PLAYER_PERMISSION = "shopkeeper.remove-all.player";
	/**
	 * The <code>shopkeeper.remove-all.admin</code> permission.
	 */
	public static final String REMOVE_ALL_ADMIN_PERMISSION = "shopkeeper.remove-all.admin";

	/**
	 * The <code>shopkeeper.notify.trades</code> permission.
	 */
	public static final String NOTIFY_TRADES_PERMISSION = "shopkeeper.notify.trades";
	/**
	 * The <code>shopkeeper.give</code> permission.
	 */
	public static final String GIVE_PERMISSION = "shopkeeper.give";
	/**
	 * The <code>shopkeeper.givecurrency</code> permission.
	 */
	public static final String GIVE_CURRENCY_PERMISSION = "shopkeeper.givecurrency";
	/**
	 * The <code>shopkeeper.setcurrency</code> permission.
	 */
	public static final String SET_CURRENCY_PERMISSION = "shopkeeper.setcurrency";
	/**
	 * The <code>shopkeeper.updateitems</code> permission.
	 */
	public static final String UPDATE_ITEMS_PERMISSION = "shopkeeper.updateitems";
	/**
	 * The <code>shopkeeper.remote</code> permission.
	 */
	public static final String REMOTE_PERMISSION = "shopkeeper.remote";
	/**
	 * The <code>shopkeeper.remote.otherplayers</code> permission.
	 */
	public static final String REMOTE_OTHER_PLAYERS_PERMISSION = "shopkeeper.remote.otherplayers";
	/**
	 * The <code>shopkeeper.remoteedit</code> permission.
	 */
	public static final String REMOTE_EDIT_PERMISSION = "shopkeeper.remoteedit";
	/**
	 * The <code>shopkeeper.transfer</code> permission.
	 */
	public static final String TRANSFER_PERMISSION = "shopkeeper.transfer";
	/**
	 * The <code>shopkeeper.teleport</code> permission.
	 */
	public static final String TELEPORT_PERMISSION = "shopkeeper.teleport";
	/**
	 * The <code>shopkeeper.teleport.others</code> permission.
	 */
	public static final String TELEPORT_OTHERS_PERMISSION = "shopkeeper.teleport.others";
	/**
	 * The <code>shopkeeper.settradeperm</code> permission.
	 */
	public static final String SET_TRADE_PERM_PERMISSION = "shopkeeper.settradeperm";
	/**
	 * The <code>shopkeeper.settradedcommand</code> permission.
	 */
	public static final String SET_TRADED_COMMAND_PERMISSION = "shopkeeper.settradedcommand";
	/**
	 * The <code>shopkeeper.setforhire</code> permission.
	 */
	public static final String SET_FOR_HIRE_PERMISSION = "shopkeeper.setforhire";
	/**
	 * The <code>shopkeeper.hire</code> permission.
	 */
	public static final String HIRE_PERMISSION = "shopkeeper.hire";
	/**
	 * The <code>shopkeeper.snapshot</code> permission.
	 */
	public static final String SNAPSHOT_PERMISSION = "shopkeeper.snapshot";
	/**
	 * The <code>shopkeeper.edit-villagers</code> permission.
	 */
	public static final String EDIT_VILLAGERS_PERMISSION = "shopkeeper.edit-villagers";
	/**
	 * The <code>shopkeeper.edit-wandering-traders</code> permission.
	 */
	public static final String EDIT_WANDERING_TRADERS_PERMISSION = "shopkeeper.edit-wandering-traders";
	/**
	 * The <code>shopkeeper.bypass</code> permission.
	 */
	public static final String BYPASS_PERMISSION = "shopkeeper.bypass";
	/**
	 * The <code>shopkeeper.maxshops.unlimited</code> permission.
	 */
	public static final String MAXSHOPS_UNLIMITED_PERMISSION = "shopkeeper.maxshops.unlimited";

	/**
	 * The <code>shopkeeper.trade-notifications.admin</code> permission.
	 */
	public static final String TRADE_NOTIFICATIONS_ADMIN = "shopkeeper.trade-notifications.admin";
	/**
	 * The <code>shopkeeper.trade-notifications.player</code> permission.
	 */
	public static final String TRADE_NOTIFICATIONS_PLAYER = "shopkeeper.trade-notifications.player";

	/**
	 * The <code>shopkeeper.admin</code> permission.
	 */
	public static final String ADMIN_PERMISSION = "shopkeeper.admin";
	/**
	 * The <code>shopkeeper.player.sell</code> permission.
	 */
	public static final String PLAYER_SELL_PERMISSION = "shopkeeper.player.sell";
	/**
	 * The <code>shopkeeper.player.buy</code> permission.
	 */
	public static final String PLAYER_BUY_PERMISSION = "shopkeeper.player.buy";
	/**
	 * The <code>shopkeeper.player.trade</code> permission.
	 */
	public static final String PLAYER_TRADE_PERMISSION = "shopkeeper.player.trade";
	/**
	 * The <code>shopkeeper.player.book</code> permission.
	 */
	public static final String PLAYER_BOOK_PERMISSION = "shopkeeper.player.book";

	/**
	 * Checks if the given player has the permission to create any kind of shopkeeper.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>false</code> if the player cannot create any kind of shop, <code>true</code>
	 *         otherwise
	 */
	public boolean hasCreatePermission(Player player);

	// SHOP TYPES

	/**
	 * Gets the {@link ShopTypesRegistry}.
	 * 
	 * @return the shop types registry
	 */
	public ShopTypesRegistry<?> getShopTypeRegistry();

	/**
	 * Gets the {@link DefaultShopTypes}.
	 * 
	 * @return the default shop types
	 */
	public DefaultShopTypes getDefaultShopTypes();

	// SHOP OBJECT TYPES

	/**
	 * Gets the {@link ShopObjectTypesRegistry}.
	 * 
	 * @return the shop object types registry
	 */
	public ShopObjectTypesRegistry<?> getShopObjectTypeRegistry();

	/**
	 * Gets the {@link DefaultShopObjectTypes}.
	 * 
	 * @return the default shop object types
	 */
	public DefaultShopObjectTypes getDefaultShopObjectTypes();

	// UI

	/**
	 * Gets the {@link UIRegistry}.
	 * 
	 * @return the UI registry
	 */
	public UIRegistry<?> getUIRegistry();

	/**
	 * Gets the {@link DefaultUITypes}.
	 * 
	 * @return the default UI types
	 */
	public DefaultUITypes getDefaultUITypes();

	// PLAYER SHOP ACCESS LEVELS

	/**
	 * Gets the {@link PlayerShopAccessLevelRegistry}.
	 * 
	 * @return the player shop access level registry
	 */
	public PlayerShopAccessLevelRegistry<?> getPlayerShopAccessLevelRegistry();

	/**
	 * Gets the {@link DefaultPlayerShopAccessLevels}.
	 * 
	 * @return the default player shop access levels
	 */
	public DefaultPlayerShopAccessLevels getDefaultPlayerShopAccessLevels();

	// SHOPKEEPER REGISTRY

	/**
	 * Gets the {@link ShopkeeperRegistry}.
	 * 
	 * @return the shopkeeper registry
	 */
	public ShopkeeperRegistry getShopkeeperRegistry();

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public ShopkeeperStorage getShopkeeperStorage();

	// ITEM UPDATES

	/**
	 * Calls an {@link UpdateItemEvent} for each item stored by the Shopkeepers plugin.
	 * <p>
	 * This includes calling {@link Shopkeeper#updateItems()} for each loaded shopkeeper, but also
	 * calls the event for items stored in other contexts, such as inside the configuration.
	 * <p>
	 * This does not call an {@link UpdateItemEvent} for items stored in the trading history. See
	 * also {@link Shopkeeper#updateItems()} for other examples of items for which no event is being
	 * called.
	 * <p>
	 * This may {@link UIRegistry#abortUISessions() abort} any currently open UI sessions (trading,
	 * editor, etc.), because they might be affected by changes to the item data.
	 * <p>
	 * This automatically saves the config and triggers a {@link ShopkeeperStorage#save()} if any
	 * items were updated.
	 * 
	 * @return the number of updated items
	 */
	public int updateItems();

	//

	/**
	 * Creates and spawns a new shopkeeper in the same way a player would create it.
	 * <p>
	 * This takes any limitations into account that might affect the creator of the shopkeeper, and
	 * this sends the creator messages if the shopkeeper creation fails for some reason.
	 * <p>
	 * This requires a {@link ShopCreationData} with non-<code>null</code> creator.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments for creating this
	 *            shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation was not successful for some
	 *         reason
	 */
	public @Nullable Shopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData);

	// FACTORIES

	/**
	 * Creates a new {@link PriceOffer}.
	 * <p>
	 * The given item stack is copied before it is stored by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 * @deprecated Use {@link PriceOffer#create(ItemStack, int)}
	 */
	@Deprecated
	public default PriceOffer createPriceOffer(ItemStack item, int price) {
		return PriceOffer.create(item, price);
	}

	/**
	 * Creates a new {@link PriceOffer}.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored
	 * by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 * @deprecated Use {@link PriceOffer#create(UnmodifiableItemStack, int)}
	 */
	@Deprecated
	public default PriceOffer createPriceOffer(UnmodifiableItemStack item, int price) {
		return PriceOffer.create(item, price);
	}

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * The given item stacks are copied before they are stored by the trade offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 * @deprecated Use {@link TradeOffer#create(ItemStack, ItemStack, ItemStack)}
	 */
	@Deprecated
	public default TradeOffer createTradeOffer(
			ItemStack resultItem,
			ItemStack item1,
			@Nullable ItemStack item2
	) {
		return TradeOffer.create(resultItem, item1, item2);
	}

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are
	 * stored by the trade offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 * @deprecated Use
	 *             {@link TradeOffer#create(UnmodifiableItemStack, UnmodifiableItemStack, UnmodifiableItemStack)}
	 */
	@Deprecated
	public default TradeOffer createTradeOffer(
			UnmodifiableItemStack resultItem,
			UnmodifiableItemStack item1,
			@Nullable UnmodifiableItemStack item2
	) {
		return TradeOffer.create(resultItem, item1, item2);
	}

	/**
	 * Creates a new {@link BookOffer}.
	 * 
	 * @param bookTitle
	 *            the book title, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 * @deprecated Use {@link BookOffer#create(String, int)}
	 */
	@Deprecated
	public default BookOffer createBookOffer(String bookTitle, int price) {
		return BookOffer.create(bookTitle, price);
	}
}
