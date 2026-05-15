package com.nisovin.shopkeepers.api.shopkeeper.player.members;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

/**
 * The default built-in {@link PlayerShopAccessLevel}.
 */
public interface DefaultPlayerShopAccessLevels {

	/**
	 * Gets all default {@link PlayerShopAccessLevel}.
	 * 
	 * @return all default access levels
	 */
	public List<? extends PlayerShopAccessLevel> getAllAccessLevels();

	/**
	 * Gets the "none" access level: The player is not a member of the shop. They cannot edit the
	 * shop or access its containers.
	 * 
	 * @return the "none" access level
	 */
	public PlayerShopAccessLevel getNone();

	/**
	 * Gets the "container" access level: The player can access the shop containers.
	 * 
	 * @return the "container" access level
	 */
	public PlayerShopAccessLevel getContainer();

	/**
	 * Gets the "edit" access level: The player can access the shop containers and edit the
	 * shopkeeper (e.g. the trades, the shop object, etc.), but they cannot delete the shop or edit
	 * the shop members.
	 * 
	 * @return the "edit" access level
	 */
	public PlayerShopAccessLevel getEdit();

	/**
	 * Gets the "full" access level: The player has full editing rights, just like the shop owner:
	 * They can access the shop containers and edit the shopkeeper, including deleting it or editing
	 * the shop members.
	 * 
	 * @return the "full" access level
	 */
	public PlayerShopAccessLevel getFull();

	// STATIC ACCESSORS (for convenience)

	/**
	 * Gets the {@link DefaultPlayerShopAccessLevels} instance.
	 * 
	 * @return the instance
	 */
	public static DefaultPlayerShopAccessLevels getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultPlayerShopAccessLevels();
	}

	/**
	 * Gets the "none" {@link PlayerShopAccessLevel}.
	 * 
	 * @return the "none" {@link PlayerShopAccessLevel}
	 * @see #getNone()
	 */
	public static PlayerShopAccessLevel NONE() {
		return getInstance().getNone();
	}

	/**
	 * Gets the "container" {@link PlayerShopAccessLevel}.
	 * 
	 * @return the "container" {@link PlayerShopAccessLevel}
	 * @see #getContainer()
	 */
	public static PlayerShopAccessLevel CONTAINER() {
		return getInstance().getNone();
	}

	/**
	 * Gets the "edit" {@link PlayerShopAccessLevel}.
	 * 
	 * @return the "edit" {@link PlayerShopAccessLevel}
	 * @see #getEdit()
	 */
	public static PlayerShopAccessLevel EDIT() {
		return getInstance().getNone();
	}

	/**
	 * Gets the "full" {@link PlayerShopAccessLevel}.
	 * 
	 * @return the "full" {@link PlayerShopAccessLevel}
	 * @see #getFull()
	 */
	public static PlayerShopAccessLevel FULL() {
		return getInstance().getNone();
	}
}
