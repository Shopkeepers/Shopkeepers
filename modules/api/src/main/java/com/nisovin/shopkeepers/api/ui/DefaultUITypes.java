package com.nisovin.shopkeepers.api.ui;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

/**
 * Provides access to the default {@link UIType UITypes}.
 */
public interface DefaultUITypes {

	/**
	 * Gets all default {@link UIType UITypes}.
	 * 
	 * @return all default UI types
	 */
	public List<? extends UIType> getAllUITypes();

	/**
	 * Gets the default editor {@link UIType}.
	 * 
	 * @return the default editor {@link UIType}
	 */
	public UIType getEditorUIType();

	/**
	 * Gets the default equipment editor {@link UIType}.
	 * 
	 * @return the default equipment editor {@link UIType}
	 */
	public UIType getEquipmentEditorUIType();

	/**
	 * Gets the default shop members editor {@link UIType}.
	 * 
	 * @return the default shop members editor {@link UIType}
	 */
	public UIType getShopMembersEditorUIType();

	/**
	 * Gets the default trading {@link UIType}.
	 * 
	 * @return the default trading {@link UIType}
	 */
	public UIType getTradingUIType();

	/**
	 * Gets the default hiring {@link UIType}.
	 * 
	 * @return the default hiring {@link UIType}
	 */
	public UIType getHiringUIType();

	// STATIC ACCESSORS (for convenience)

	/**
	 * Gets the {@link DefaultUITypes} instance.
	 * 
	 * @return the instance
	 */
	public static DefaultUITypes getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultUITypes();
	}

	/**
	 * Gets the default editor {@link UIType}.
	 * 
	 * @return the default editor {@link UIType}
	 * @see #getEditorUIType()
	 */
	public static UIType EDITOR() {
		return getInstance().getEditorUIType();
	}

	/**
	 * Gets the default equipment editor {@link UIType}.
	 * 
	 * @return the default equipment editor {@link UIType}
	 * @see #getEquipmentEditorUIType()
	 */
	public static UIType EQUIPMENT_EDITOR() {
		return getInstance().getTradingUIType();
	}

	/**
	 * Gets the default shop members editor {@link UIType}.
	 * 
	 * @return the default shop members editor {@link UIType}
	 * @see #getShopMembersEditorUIType()
	 */
	public static UIType SHOP_MEMBERS_EDITOR() {
		return getInstance().getShopMembersEditorUIType();
	}

	/**
	 * Gets the default trading {@link UIType}.
	 * 
	 * @return the default trading {@link UIType}
	 * @see #getTradingUIType()
	 */
	public static UIType TRADING() {
		return getInstance().getTradingUIType();
	}

	/**
	 * Gets the default hiring {@link UIType}.
	 * 
	 * @return the default hiring {@link UIType}
	 * @see #getHiringUIType()
	 */
	public static UIType HIRING() {
		return getInstance().getHiringUIType();
	}
}
