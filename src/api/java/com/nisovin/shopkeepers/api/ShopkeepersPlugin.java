package com.nisovin.shopkeepers.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.registry.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.api.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shoptypes.ShopTypesRegistry;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.util.TradingRecipe;

public interface ShopkeepersPlugin extends Plugin {

	public static ShopkeepersPlugin getInstance() {
		return ShopkeepersAPI.getPlugin();
	}

	// PERMISSIONS

	public static final String HELP_PERMISSION = "shopkeeper.help";
	public static final String TRADE_PERMISSION = "shopkeeper.trade";
	public static final String RELOAD_PERMISSION = "shopkeeper.reload";
	public static final String DEBUG_PERMISSION = "shopkeeper.debug";
	public static final String LIST_OWN_PERMISSION = "shopkeeper.list.own";
	public static final String LIST_OTHERS_PERMISSION = "shopkeeper.list.others";
	public static final String LIST_ADMIN_PERMISSION = "shopkeeper.list.admin";
	public static final String REMOVE_OWN_PERMISSION = "shopkeeper.remove.own";
	public static final String REMOVE_OTHERS_PERMISSION = "shopkeeper.remove.others";
	public static final String REMOVE_ALL_PERMISSION = "shopkeeper.remove.all";
	public static final String REMOVE_ADMIN_PERMISSION = "shopkeeper.remove.admin";
	public static final String REMOTE_PERMISSION = "shopkeeper.remote";
	public static final String TRANSFER_PERMISSION = "shopkeeper.transfer";
	public static final String SETTRADEPERM_PERMISSION = "shopkeeper.settradeperm";
	public static final String SETFORHIRE_PERMISSION = "shopkeeper.setforhire";
	public static final String HIRE_PERMISSION = "shopkeeper.hire";
	public static final String BYPASS_PERMISSION = "shopkeeper.bypass";
	public static final String ADMIN_PERMISSION = "shopkeeper.admin";
	public static final String PLAYER_NORMAL_PERMISSION = "shopkeeper.player.normal";
	public static final String PLAYER_BUY_PERMISSION = "shopkeeper.player.buy";
	public static final String PLAYER_TRADE_PERMISSION = "shopkeeper.player.trade";
	public static final String PLAYER_BOOK_PERMISSION = "shopkeeper.player.book";

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if he cannot create shops at all, <code>true</code> otherwise
	 */
	public boolean hasCreatePermission(Player player);

	// SHOP TYPES

	public ShopTypesRegistry<?> getShopTypeRegistry();

	public DefaultShopTypes getDefaultShopTypes();

	// SHOP OBJECT TYPES

	public ShopObjectTypesRegistry<?> getShopObjectTypeRegistry();

	public DefaultShopObjectTypes getDefaultShopObjectTypes();

	// UI

	public UIRegistry<?> getUIRegistry();

	public DefaultUITypes getDefaultUITypes();

	// SHOPKEEPER REGISTRY

	public ShopkeeperRegistry getShopkeeperRegistry();

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public ShopkeeperStorage getShopkeeperStorage();

	//

	/**
	 * Creates a new shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, etc.) for
	 *            creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some reason
	 */
	public Shopkeeper createShopkeeper(ShopCreationData shopCreationData);

	public TradingRecipe createTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2);
}