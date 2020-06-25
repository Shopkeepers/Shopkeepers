package com.nisovin.shopkeepers.api;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopTypesRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.user.UserManager;

public final class ShopkeepersAPI {

	private ShopkeepersAPI() {
	}

	private static ShopkeepersPlugin plugin = null;

	public static void enable(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "Plugin is null!");
		if (ShopkeepersAPI.plugin != null) {
			throw new IllegalStateException("API is already enabled!");
		}
		ShopkeepersAPI.plugin = plugin;
	}

	public static void disable() {
		if (ShopkeepersAPI.plugin == null) {
			throw new IllegalStateException("API is already disabled!");
		}
		ShopkeepersAPI.plugin = null;
	}

	/**
	 * Checks whether the API has already been enabled.
	 * <p>
	 * If this is called early during plugin startup (eg. during the {@link Plugin#onLoad() loading phase} of plugins,
	 * or while the Shopkeepers plugin itself is still getting enabled), the API may not yet be safe for use even if
	 * this returns <code>true</code>.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public static boolean isEnabled() {
		return (plugin != null);
	}

	public static ShopkeepersPlugin getPlugin() {
		if (plugin == null) {
			throw new IllegalStateException("API is not enabled!");
		}
		return plugin;
	}

	// PERMISSIONS

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if he cannot create shops at all, <code>true</code> otherwise
	 */
	public static boolean hasCreatePermission(Player player) {
		return getPlugin().hasCreatePermission(player);
	}

	// USER MANAGER

	public static UserManager getUserManager() {
		return getPlugin().getUserManager();
	}

	// SHOP TYPES

	public static ShopTypesRegistry<?> getShopTypeRegistry() {
		return getPlugin().getShopTypeRegistry();
	}

	public static DefaultShopTypes getDefaultShopTypes() {
		return getPlugin().getDefaultShopTypes();
	}

	// SHOP OBJECT TYPES

	public static ShopObjectTypesRegistry<?> getShopObjectTypeRegistry() {
		return getPlugin().getShopObjectTypeRegistry();
	}

	public static DefaultShopObjectTypes getDefaultShopObjectTypes() {
		return getPlugin().getDefaultShopObjectTypes();
	}
	// UI

	public static UIRegistry<?> getUIRegistry() {
		return getPlugin().getUIRegistry();
	}

	public static DefaultUITypes getDefaultUITypes() {
		return getPlugin().getDefaultUITypes();
	}

	// SHOPKEEPER REGISTRY

	public static ShopkeeperRegistry getShopkeeperRegistry() {
		return getPlugin().getShopkeeperRegistry();
	}

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public static ShopkeeperStorage getShopkeeperStorage() {
		return getPlugin().getShopkeeperStorage();
	}

	//

	/**
	 * Creates and spawns a new shopkeeper in the same way a player would create it.
	 * <p>
	 * This takes any limitations into account that might affect the creator or owner of the shopkeeper, and this sends
	 * the creator messages if the shopkeeper creation fails for some reason.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, etc.) for
	 *            creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some reason
	 */
	public static Shopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData) {
		return getPlugin().handleShopkeeperCreation(shopCreationData);
	}

	// FACTORY

	public static TradingRecipe createTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return getPlugin().createTradingRecipe(resultItem, item1, item2);
	}

	public static TradingRecipe createTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2, boolean outOfStock) {
		return getPlugin().createTradingRecipe(resultItem, item1, item2, outOfStock);
	}

	// OFFERS

	public static PriceOffer createPriceOffer(ItemStack item, int price) {
		return getPlugin().createPriceOffer(item, price);
	}

	public static TradingOffer createTradingOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return getPlugin().createTradingOffer(resultItem, item1, item2);
	}

	public static BookOffer createBookOffer(String bookTitle, int price) {
		return getPlugin().createBookOffer(bookTitle, price);
	}
}
