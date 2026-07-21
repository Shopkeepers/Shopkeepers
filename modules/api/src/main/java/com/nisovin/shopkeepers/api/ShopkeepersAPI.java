package com.nisovin.shopkeepers.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopTypesRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.container.DefaultShopContainerTypes;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerTypeRegistry;
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
 * Convenient static accessors to the methods of {@link ShopkeepersPlugin}.
 * <p>
 * These accessors can only be used while the API is enabled.
 */
public final class ShopkeepersAPI {

	private ShopkeepersAPI() {
	}

	/**
	 * Checks whether the API has already been enabled.
	 * <p>
	 * If this is called early during plugin startup (e.g. during the {@link Plugin#onLoad() loading
	 * phase} of plugins, or while the Shopkeepers plugin itself is still getting enabled), the API
	 * may not yet be safe for use even if this returns <code>true</code>.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public static boolean isEnabled() {
		return InternalShopkeepersAPI.isEnabled();
	}

	/**
	 * Gets the {@link ShopkeepersPlugin} instance.
	 * 
	 * @return the plugin instance, not <code>null</code>
	 * @throws IllegalStateException
	 *             if the API is not enabled currently, e.g. because the plugin is not enabled
	 *             currently
	 */
	public static ShopkeepersPlugin getPlugin() {
		return InternalShopkeepersAPI.getPlugin();
	}

	// PERMISSIONS

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if they cannot create shops at all, <code>true</code> otherwise
	 * @see ShopkeepersPlugin#hasCreatePermission(Player)
	 */
	public static boolean hasCreatePermission(Player player) {
		return getPlugin().hasCreatePermission(player);
	}

	// SHOP TYPES

	/**
	 * Gets the {@link ShopTypesRegistry}.
	 * 
	 * @return the shop types registry
	 * @see ShopkeepersPlugin#getShopTypeRegistry()
	 */
	public static ShopTypesRegistry<?> getShopTypeRegistry() {
		return getPlugin().getShopTypeRegistry();
	}

	/**
	 * Gets the {@link DefaultShopTypes}.
	 * 
	 * @return the default shop types
	 * @see ShopkeepersPlugin#getDefaultShopTypes()
	 */
	public static DefaultShopTypes getDefaultShopTypes() {
		return getPlugin().getDefaultShopTypes();
	}

	// SHOP OBJECT TYPES

	/**
	 * Gets the {@link ShopObjectTypesRegistry}.
	 * 
	 * @return the shop object types registry
	 * @see ShopkeepersPlugin#getShopObjectTypeRegistry()
	 */
	public static ShopObjectTypesRegistry<?> getShopObjectTypeRegistry() {
		return getPlugin().getShopObjectTypeRegistry();
	}

	/**
	 * Gets the {@link DefaultShopObjectTypes}.
	 * 
	 * @return the default shop object types
	 * @see ShopkeepersPlugin#getDefaultShopObjectTypes()
	 */
	public static DefaultShopObjectTypes getDefaultShopObjectTypes() {
		return getPlugin().getDefaultShopObjectTypes();
	}

	// UI

	/**
	 * Gets the {@link UIRegistry}.
	 * 
	 * @return the UI registry
	 * @see ShopkeepersPlugin#getUIRegistry()
	 */
	public static UIRegistry<?> getUIRegistry() {
		return getPlugin().getUIRegistry();
	}

	/**
	 * Gets the {@link DefaultUITypes}.
	 * 
	 * @return the default UI types
	 * @see ShopkeepersPlugin#getDefaultUITypes()
	 */
	public static DefaultUITypes getDefaultUITypes() {
		return getPlugin().getDefaultUITypes();
	}

	// PLAYER SHOP ACCESS LEVELS

	/**
	 * Gets the {@link PlayerShopAccessLevelRegistry}.
	 * 
	 * @return the player shop access level registry
	 */
	public static PlayerShopAccessLevelRegistry<?> getPlayerShopAccessLevelRegistry() {
		return getPlugin().getPlayerShopAccessLevelRegistry();
	}

	/**
	 * Gets the {@link DefaultPlayerShopAccessLevels}.
	 * 
	 * @return the default player shop access levels
	 */
	public static DefaultPlayerShopAccessLevels getDefaultPlayerShopAccessLevels() {
		return getPlugin().getDefaultPlayerShopAccessLevels();
	}

	// SHOP CONTAINER TYPES

	/**
	 * Gets the {@link ShopContainerTypeRegistry}.
	 * 
	 * @return the shop container type registry
	 */
	public static ShopContainerTypeRegistry<?> getShopContainerTypeRegistry() {
		return getPlugin().getShopContainerTypeRegistry();
	}

	/**
	 * Gets the {@link DefaultShopContainerTypes}.
	 * 
	 * @return the default shop container types
	 */
	public static DefaultShopContainerTypes getDefaultShopContainerTypes() {
		return getPlugin().getDefaultShopContainerTypes();
	}

	// SHOPKEEPER REGISTRY

	/**
	 * Gets the {@link ShopkeeperRegistry}.
	 * 
	 * @return the shopkeeper registry
	 * @see ShopkeepersPlugin#getShopkeeperRegistry()
	 */
	public static ShopkeeperRegistry getShopkeeperRegistry() {
		return getPlugin().getShopkeeperRegistry();
	}

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 * @see ShopkeepersPlugin#getShopkeeperStorage()
	 */
	public static ShopkeeperStorage getShopkeeperStorage() {
		return getPlugin().getShopkeeperStorage();
	}

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
	 * @see ShopkeepersPlugin#updateItems()
	 */
	public static int updateItems() {
		return getPlugin().updateItems();
	}

	//

	/**
	 * Creates and spawns a new shopkeeper in the same way a player would create it.
	 * <p>
	 * This takes any limitations into account that might affect the creator or owner of the
	 * shopkeeper, and this sends the creator messages if the shopkeeper creation fails for some
	 * reason.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments (spawn location, object
	 *            type, etc.) for creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some
	 *         reason
	 * @see ShopkeepersPlugin#handleShopkeeperCreation(ShopCreationData)
	 */
	public static @Nullable Shopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData) {
		return getPlugin().handleShopkeeperCreation(shopCreationData);
	}

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
	public static PriceOffer createPriceOffer(ItemStack item, int price) {
		return getPlugin().createPriceOffer(item, price);
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
	public static PriceOffer createPriceOffer(UnmodifiableItemStack item, int price) {
		return getPlugin().createPriceOffer(item, price);
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
	public static TradeOffer createTradeOffer(
			ItemStack resultItem,
			ItemStack item1,
			ItemStack item2
	) {
		return getPlugin().createTradeOffer(resultItem, item1, item2);
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
	public static TradeOffer createTradeOffer(
			UnmodifiableItemStack resultItem,
			UnmodifiableItemStack item1,
			UnmodifiableItemStack item2
	) {
		return getPlugin().createTradeOffer(resultItem, item1, item2);
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
	public static BookOffer createBookOffer(String bookTitle, int price) {
		return getPlugin().createBookOffer(bookTitle, price);
	}
}
