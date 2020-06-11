package com.nisovin.shopkeepers.shopkeeper;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.PlayerCreateShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractShopType<T extends AbstractShopkeeper> extends AbstractSelectableType implements ShopType<T> {

	protected AbstractShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractShopType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	@Override
	protected void onSelect(Player player) {
		TextUtils.sendMessage(player, Settings.msgSelectedShopType,
				"type", this.getDisplayName(),
				"description", this.getDescription()
		);
	}

	protected Text getCreatedMessage() {
		Text text = Settings.msgShopkeeperCreated;
		TextUtils.setPlaceholderArguments(text,
				"type", this.getDisplayName(),
				"description", this.getDescription(),
				"setupDesc", this.getSetupDescription()
		);
		return text;
	}

	/**
	 * Recreates a shopkeeper of this type by loading its previously saved data from the given config section.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param configSection
	 *            the config section
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created (ex. due to invalid or missing data)
	 */
	public abstract T loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException;

	/**
	 * Creates a new shopkeeper of this type by using the data from the given {@link ShopCreationData}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created (ex. due to invalid or missing data)
	 */
	public abstract T createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException;

	// handles of shopkeeper creation by players
	// return null in case of failure
	public T handleShopkeeperCreation(ShopCreationData shopCreationData) {
		this.validateCreationData(shopCreationData);
		SKShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();

		// the creator, can not be null when creating a shopkeeper via this method:
		Player creator = shopCreationData.getCreator();
		Validate.notNull(creator, "Creator player cannot be null!");

		ShopType<?> shopType = shopCreationData.getShopType();
		ShopObjectType<?> shopObjectType = shopCreationData.getShopObjectType();

		// can the selected shop type be used?
		if (!shopType.hasPermission(creator)) {
			TextUtils.sendMessage(creator, Settings.msgNoPermission);
			return null;
		}
		if (!shopType.isEnabled()) {
			TextUtils.sendMessage(creator, Settings.msgShopTypeDisabled, "type", shopType.getIdentifier());
			return null;
		}

		// can the selected shop object type be used?
		if (!shopObjectType.hasPermission(creator)) {
			TextUtils.sendMessage(creator, Settings.msgNoPermission);
			return null;
		}
		if (!shopObjectType.isEnabled()) {
			TextUtils.sendMessage(creator, Settings.msgShopObjectTypeDisabled, "type", shopObjectType.getIdentifier());
			return null;
		}

		Location spawnLocation = shopCreationData.getSpawnLocation(); // can be null for virtual shops
		BlockFace targetedBlockFace = shopCreationData.getTargetedBlockFace(); // can be null

		// check if the shop can be placed there (enough space, etc.):
		if (!shopObjectType.isValidSpawnLocation(spawnLocation, targetedBlockFace)) {
			// invalid spawn location or targeted block face:
			TextUtils.sendMessage(creator, Settings.msgShopCreateFail);
			return null;
		}

		if (spawnLocation != null && !shopkeeperRegistry.getShopkeepersAtLocation(spawnLocation).isEmpty()) {
			// there is already a shopkeeper at that location:
			TextUtils.sendMessage(creator, Settings.msgShopCreateFail);
			return null;
		}

		try {
			// shop type specific handling:
			if (!this.handleSpecificShopkeeperCreation(shopCreationData)) {
				return null;
			}

			// create and spawn the shopkeeper:
			@SuppressWarnings("unchecked")
			T shopkeeper = (T) shopkeeperRegistry.createShopkeeper(shopCreationData);
			assert shopkeeper != null;

			// send creation message to creator:
			TextUtils.sendMessage(creator, this.getCreatedMessage());

			// save:
			shopkeeper.save();

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			// some issue identified during shopkeeper creation (possibly hinting to a bug):
			// TODO translation?
			TextUtils.sendMessage(creator, ChatColor.RED + "Shopkeeper creation failed: " + e.getMessage());
			return null;
		}
	}

	// shop type specific handling of shopkeeper creation by players
	// return null in case of failure
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData creationData) {
		// call event:
		PlayerCreateShopkeeperEvent createEvent = new PlayerCreateShopkeeperEvent(creationData);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("ShopkeeperCreateEvent was cancelled!");
			return false;
		}
		return true;
	}

	// common functions that might be useful for sub-classes:

	protected void validateCreationData(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "ShopCreationData is null!");
		ShopType<?> shopType = shopCreationData.getShopType();
		Validate.isTrue(this == shopType, "Expecting shop type " + this.getClass().getName()
				+ ", but got " + shopType.getClass().getName());
	}

	protected void validateConfigSection(ConfigurationSection configSection) {
		Validate.notNull(configSection, "Config section is null!");
	}
}
