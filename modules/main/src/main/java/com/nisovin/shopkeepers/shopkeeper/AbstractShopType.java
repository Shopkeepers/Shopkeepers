package com.nisovin.shopkeepers.shopkeeper;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerCreateShopkeeperEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopcreation.ShopkeeperPlacement;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractShopType<T extends AbstractShopkeeper>
		extends AbstractSelectableType implements ShopType<T> {

	private final Class<T> shopkeeperClass;

	protected AbstractShopType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission,
			Class<T> shopkeeperClass
	) {
		super(identifier, aliases, permission);
		Validate.notNull(shopkeeperClass, "shopkeeperClass is null");
		this.shopkeeperClass = shopkeeperClass;
	}

	/**
	 * Gets the concrete and most specific class of the shopkeepers that are created by this
	 * {@link ShopType}.
	 * 
	 * @return the concrete shopkeeper class, not <code>null</code>
	 */
	public final Class<T> getShopkeeperClass() {
		return shopkeeperClass;
	}

	// Internal Text variants of the description methods, to support the advanced text
	// features without exposing Text in the public API.
	public abstract Text getDescriptionText();

	public abstract Text getSetupDescriptionText();

	@Override
	public final String getDescription() {
		return this.getDescriptionText().toPlainText();
	}

	@Override
	public final String getSetupDescription() {
		return this.getSetupDescriptionText().toPlainText();
	}

	@Override
	protected void onSelect(Player player) {
		TextUtils.sendMessage(player, Messages.selectedShopType,
				"type", this.getDisplayName(),
				"description", this.getDescriptionText()
		);
	}

	protected Text getCreatedMessage() {
		Text text = Messages.shopkeeperCreated;
		text.setPlaceholderArguments(
				"type", this.getDisplayName(),
				"description", this.getDescriptionText(),
				"setupDesc", this.getSetupDescriptionText()
		);
		return text;
	}

	/**
	 * Creates a new and not yet {@link AbstractShopkeeper#isInitialized() initialized} shopkeeper
	 * of this type.
	 * 
	 * @return the new shopkeeper, not <code>null</code>
	 */
	private final @NonNull T createShopkeeper() {
		T shopkeeper = this.createNewShopkeeper();

		// Validate the created shopkeeper:
		if (Unsafe.cast(shopkeeper) == null) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created null shopkeeper!");
		}
		if (shopkeeper.getType() != this) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created a shopkeeper of a different type (expected: "
					+ this.getIdentifier() + ", got: "
					+ shopkeeper.getType().getIdentifier() + ")!");
		}
		if (shopkeeper.getClass() != this.getShopkeeperClass()) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created a shopkeeper of unexpected class (expected: "
					+ this.getShopkeeperClass().getName() + ", got: "
					+ shopkeeper.getClass().getName() + ")!");
		}
		if (shopkeeper.isInitialized()) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created an already initialized shopkeeper!");
		}

		return shopkeeper;
	}

	/**
	 * Creates a new and not yet {@link AbstractShopkeeper#isInitialized() initialized} shopkeeper
	 * of this type.
	 * 
	 * @return the new shopkeeper, not <code>null</code>
	 * @see #createShopkeeper()
	 */
	protected abstract @NonNull T createNewShopkeeper();

	/**
	 * Creates a new shopkeeper of this type based on the given {@link ShopCreationData}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data, not <code>null</code>
	 * @return the created shopkeeper, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be created (e.g. due to invalid or missing data)
	 */
	public final @NonNull T createShopkeeper(
			int id,
			ShopCreationData shopCreationData
	) throws ShopkeeperCreateException {
		T shopkeeper = this.createShopkeeper();
		assert shopkeeper != null;
		shopkeeper.initOnCreation(id, shopCreationData);
		return shopkeeper;
	}

	/**
	 * Recreates a shopkeeper of this type by loading its previously saved state from the given
	 * {@link ShopkeeperData}.
	 * <p>
	 * The data is expected to already have been {@link ShopkeeperData#migrate(String) migrated}.
	 * <p>
	 * This operation does not modify the given {@link ShopkeeperData}. Any stored data elements
	 * (such as for example item stacks, etc.) and collections of data elements are assumed to not
	 * be modified, neither by the loaded shopkeeper, nor in contexts outside the loaded shopkeeper.
	 * If the loaded shopkeeper can guarantee not to modify these data elements, it is allowed to
	 * directly store them without copying them first.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @return the loaded shopkeeper, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the shopkeeper cannot be loaded (e.g. due to invalid or missing data)
	 */
	public final @NonNull T loadShopkeeper(
			ShopkeeperData shopkeeperData
	) throws InvalidDataException {
		T shopkeeper = this.createShopkeeper();
		assert shopkeeper != null;
		shopkeeper.initOnLoad(shopkeeperData);
		return shopkeeper;
	}

	/**
	 * Checks if the given {@link ShopCreationData} is compatible with this shop type.
	 * <p>
	 * This checks the common basic pre-requirements, such as whether the given shop creation data
	 * is of the expected type. This may not necessarily check if the given shop creation data
	 * contains all the information required to create a valid shopkeeper.
	 * 
	 * @param shopCreationData
	 *            the shop creation data
	 */
	protected void validateCreationData(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "shopCreationData is null");
		ShopType<?> shopType = shopCreationData.getShopType();
		Validate.isTrue(this == shopType, () -> "ShopType of shopCreationData is not of type "
				+ this.getClass().getName() + ", but: " + shopType.getClass().getName());

		ShopObjectType<?> shopObjectType = shopCreationData.getShopObjectType();
		Validate.isTrue(shopObjectType instanceof AbstractShopObjectType,
				() -> "ShopObjectType of shopCreationData is not of type "
						+ AbstractShopObjectType.class.getSimpleName()
						+ ", but: " + shopType.getClass().getName());
	}

	// Handles the shopkeeper creation by players.
	// Returns null in case of failure.
	public @Nullable T handleShopkeeperCreation(ShopCreationData shopCreationData) {
		this.validateCreationData(shopCreationData);
		SKShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();

		// The creator, can not be null when creating a shopkeeper via this method:
		Player creator = shopCreationData.getCreator();
		creator = Validate.notNull(creator, "Creator of shopCreationData is null");

		AbstractShopType<?> shopType = this;
		AbstractShopObjectType<?> shopObjectType = (AbstractShopObjectType<?>) shopCreationData.getShopObjectType();

		// Can the selected shop type be used?
		if (!shopType.hasPermission(creator)) {
			TextUtils.sendMessage(creator, Messages.noPermission);
			return null;
		}

		if (!shopType.isEnabled()) {
			TextUtils.sendMessage(creator, Messages.shopTypeDisabled,
					"type", shopType.getIdentifier()
			);
			return null;
		}

		// Can the selected shop object type be used?
		if (!shopObjectType.hasPermission(creator)) {
			TextUtils.sendMessage(creator, Messages.noPermission);
			return null;
		}

		if (!shopObjectType.isEnabled()) {
			TextUtils.sendMessage(creator, Messages.shopObjectTypeDisabled,
					"type", shopObjectType.getIdentifier()
			);
			return null;
		}

		// Can be null for virtual shops:
		Location spawnLocation = shopCreationData.getSpawnLocation();
		BlockFace targetedBlockFace = shopCreationData.getTargetedBlockFace(); // Can be null

		// Validate the spawn location:
		// This is expected to send feedback to the shop creator if necessary.
		ShopkeeperPlacement shopkeeperPlacement = SKShopkeepersPlugin.getInstance()
				.getShopkeeperCreation()
				.getShopkeeperPlacement();
		boolean isSpawnLocationValid = shopkeeperPlacement.validateSpawnLocation(
				creator,
				shopType,
				shopObjectType,
				spawnLocation,
				targetedBlockFace,
				shopCreationData,
				null
		);
		if (!isSpawnLocationValid) {
			return null;
		}

		try {
			// Shop type specific handling:
			if (!this.handleSpecificShopkeeperCreation(shopCreationData)) {
				return null;
			}

			// Create and spawn the shopkeeper:
			T shopkeeper = Unsafe.castNonNull(shopkeeperRegistry.createShopkeeper(shopCreationData));

			// Verify the shopkeeper was spawned successfully (if not virtual):
			// Since the shopkeeper is created by a player here, we assume that the shopkeeper is
			// meant to be immediately spawned, even when spawning is not handled by us (e.g.
			// Citizens NPC shopkeepers), i.e. the chunk is active and no world saving is currently
			// in progress.
			// Note: Citizens NPC seem to bypass region protection plugins like WorldGuard, since
			// they don't call the usual Bukkit entity spawn event.
			var shopObject = shopkeeper.getShopObject();
			if (!shopkeeper.isVirtual() && !shopObject.isActive()) {
				Log.debug(shopkeeper.getLogPrefix() + "New shopkeeper failed to spawn -> Deleting.");
				// Immediately remove the shopkeeper again:
				// Note: Once the deletion has been persisted, the storage also tries to reuse the
				// last used shopkeeper ids.
				// Intentionally not passing the creator here to not give back the shop creation
				// item for player shops.
				shopkeeper.delete(null);
				// Save if dirty: Usually, the shopkeeper was not persisted yet, so there is no need
				// to actually save.
				SKShopkeepersPlugin.getInstance().getShopkeeperStorage().saveIfDirty();

				TextUtils.sendMessage(creator, Messages.cannotSpawn);
				return null;
			}

			// Send creation message to creator:
			TextUtils.sendMessage(creator, this.getCreatedMessage());

			// Save:
			shopkeeper.save();

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			// Some issue identified during shopkeeper creation (possibly hinting to a bug):
			// TODO Translation?
			TextUtils.sendMessage(creator, ChatColor.RED + "Shopkeeper creation failed: "
					+ e.getMessage());
			return null;
		}
	}

	// Shop type specific handling of the shopkeeper creation by players (except any spawn location
	// validation).
	// Returns null in case of failure.
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData creationData) {
		// Call event:
		PlayerCreateShopkeeperEvent createEvent = new PlayerCreateShopkeeperEvent(creationData);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("ShopkeeperCreateEvent was cancelled!");
			return false;
		}
		return true;
	}

	/**
	 * Validates the given spawn location according to any shop-type specific validation rules.
	 * <p>
	 * If a player is specified, this sends feedback about failed validation rules to the player.
	 * <p>
	 * Only the {@code shopCreationData} or the {@code shopkeeper} can be specified, but not both.
	 * 
	 * @param player
	 *            the player who is trying to place the shop, or <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops, has to provide a
	 *            loaded world if not <code>null</code>
	 * @param blockFace
	 *            the block face, can be <code>null</code> for virtual shops or if not available
	 * @param shopCreationData
	 *            the {@link ShopCreationData} for which the spawn location is validated, or
	 *            <code>null</code> if not available
	 * @param shopkeeper
	 *            the shopkeeper for which the spawn location is validated, or <code>null</code> if
	 *            not available
	 * @return <code>true</code> if the spawn location is valid
	 */
	public boolean validateSpawnLocation(
			@Nullable Player player,
			@Nullable Location spawnLocation,
			@Nullable BlockFace blockFace,
			@Nullable ShopCreationData shopCreationData,
			@Nullable AbstractShopkeeper shopkeeper
	) {
		// Common argument validation:
		if (shopCreationData != null) {
			Validate.isTrue(shopkeeper == null,
					"shopCreationData and shopkeeper cannot both be specified");
			this.validateCreationData(shopCreationData);
		} else if (shopkeeper != null) {
			Validate.isTrue(shopkeeper.getType() == this, "shopkeeper is of a different type");
		}

		// There are no default shop-type specific validation rules.
		return true;
	}
}
