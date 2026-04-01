package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.items.ItemUpdates;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.naming.ShopkeeperNaming;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.user.SKUser;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.MutableBlockLocation;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.bukkit.ItemStackValidators;
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataAccessor;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.ItemStackSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractPlayerShopkeeper
		extends AbstractShopkeeper implements PlayerShopkeeper {

	private static final int CHECK_CONTAINER_PERIOD_SECONDS = 5;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(
			1,
			CHECK_CONTAINER_PERIOD_SECONDS + 1
	);

	private User owner = SKUser.EMPTY; // Valid after successful initialization
	// The world name of this BlockLocation matches the shopkeeper world name.
	// TODO Allow the container to be located in a world different to that of the shopkeeper? This
	// could also be useful for virtual player shops, which don't have a world themselves, but would
	// still need a container block in a world.
	// Immutable, valid after successful initialization:
	private BlockLocation container = BlockLocation.EMPTY;
	private boolean notifyOnTrades = NOTIFY_ON_TRADES.getDefaultValue();
	private @Nullable UnmodifiableItemStack hireCost = null; // Null if not for hire

	// Initial threshold between [1, CHECK_CONTAINER_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkContainerLimiter = new RateLimiter(
			CHECK_CONTAINER_PERIOD_SECONDS,
			nextCheckingOffset.getAndIncrement()
	);

	/**
	 * Creates a new and not yet initialized {@link AbstractPlayerShopkeeper}.
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 */
	protected AbstractPlayerShopkeeper() {
	}

	/**
	 * Expects a {@link PlayerShopCreationData}.
	 */
	@Override
	protected void loadFromCreationData(int id, ShopCreationData shopCreationData)
			throws ShopkeeperCreateException {
		super.loadFromCreationData(id, shopCreationData);
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;
		Player owner = Unsafe.assertNonNull(playerShopCreationData.getCreator());
		Block container = playerShopCreationData.getShopContainer();

		this._setOwner(owner.getUniqueId(), Unsafe.assertNonNull(owner.getName()));
		this._setContainer(container.getX(), container.getY(), container.getZ());
	}

	@Override
	protected void setup() {
		this.registerViewProviderIfMissing(DefaultUITypes.HIRING(), () -> {
			return new PlayerShopHiringViewProvider(this);
		});
		super.setup();
	}

	@Override
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		super.loadDynamicState(shopkeeperData);
		this.loadOwner(shopkeeperData);
		this.loadContainer(shopkeeperData);
		this.loadNotifyOnTrades(shopkeeperData);
		this.loadForHire(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ShopkeeperData shopkeeperData, boolean saveAll) {
		super.saveDynamicState(shopkeeperData, saveAll);
		this.saveOwner(shopkeeperData);
		this.saveContainer(shopkeeperData);
		this.saveNotifyOnTrades(shopkeeperData);
		this.saveForHire(shopkeeperData);
	}

	// ITEM UPDATES

	@Override
	protected int updateItems(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		int updatedItems = super.updateItems(logPrefix, shopkeeperData);
		updatedItems += updateHireCost(logPrefix, shopkeeperData);
		return updatedItems;
	}

	private static int updateHireCost(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		try {
			var hireCost = shopkeeperData.get(HIRE_COST_ITEM);
			// Does nothing if hireCost is null:
			var updatedHireCost = ItemUpdates.updateItem(hireCost);
			if (updatedHireCost != hireCost) {
				assert !ItemUtils.isEmpty(updatedHireCost);
				shopkeeperData.set(HIRE_COST_ITEM, updatedHireCost);

				Log.debug(DebugOptions.itemUpdates, logPrefix + "Updated hire cost item.");
				return 1;
			}
		} catch (InvalidDataException e) {
			Log.warning(logPrefix + "Failed to load '" + HIRE_COST_ITEM.getName() + "'!", e);
		}
		return 0;
	}

	//

	@Override
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onAdded(cause);

		// Enable the container protection:
		this.protectContainer();
	}

	@Override
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		super.onRemoval(cause);

		// Disable the container protection:
		this.unprotectContainer();
	}

	@Override
	public void delete(@Nullable Player player) {
		// Return the shop creation item:
		if (Settings.deletingPlayerShopReturnsCreationItem && player != null && this.isOwner(player)) {
			ItemStack shopCreationItem = ShopCreationItem.create();
			Map<Integer, ItemStack> remaining = player.getInventory().addItem(shopCreationItem);
			if (!remaining.isEmpty()) {
				// Inventory is full, drop the item instead:
				Location playerLocation = player.getEyeLocation();
				Location shopLocation = this.getShopObject().getLocation(); // Null if not spawned
				// If within a certain range, drop the item at the shop's location, else drop at
				// player's location:
				Location dropLocation;
				if (shopLocation != null
						&& LocationUtils.getDistanceSquared(shopLocation, playerLocation) <= 100) {
					dropLocation = shopLocation;
				} else {
					dropLocation = playerLocation;
				}
				World world = Unsafe.assertNonNull(dropLocation.getWorld());
				world.dropItem(dropLocation, shopCreationItem);
			}
		}

		super.delete(player);
	}

	@Override
	protected void populateMessageArguments(Map<String, Supplier<@NonNull ?>> messageArguments) {
		super.populateMessageArguments(messageArguments);
		messageArguments.put("owner_name", this::getOwnerName);
		messageArguments.put("owner_uuid", this::getOwnerUUID);
	}

	@Override
	protected void onShopkeeperMoved() {
		super.onShopkeeperMoved();

		// Update the container location and the registered container protection if the shopkeeper
		// has been moved to a different world:
		// The container is (currently) assumed to always be located in the same world as the
		// shopkeeper. If the shopkeeper is moved to a different world, the container is expected to
		// also have been moved. Otherwise, if the container cannot be found in the new world,
		// trading will not work.
		if (!Objects.equals(this.getWorldName(), container.getWorldName())) {
			// This updates the container's world based on the shopkeeper's current world:
			this._setContainer(container);
		}
	}

	@Override
	public void onPlayerInteraction(Player player) {
		Validate.notNull(player, "player is null");
		// Naming via item:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		if (Settings.namingOfPlayerShopsViaItem
				&& DerivedSettings.namingItemData.matches(itemInMainHand)) {
			// Check if player can edit this shopkeeper:
			if (this.canEdit(player, false)) {
				// Rename with the player's item in hand:
				String newName = ItemUtils.getDisplayNameOrEmpty(itemInMainHand);

				ShopkeeperNaming shopkeeperNaming = SKShopkeepersPlugin.getInstance().getShopkeeperNaming();
				if (shopkeeperNaming.requestNameChange(player, this, newName)) {
					// Manually remove rename item from player's hand after this event is processed:
					SchedulerUtils.runTaskOrOmit(player, () -> {
						ItemStack newItemInMainHand = ItemUtils.decreaseItemAmount(itemInMainHand, 1);
						playerInventory.setItemInMainHand(newItemInMainHand);
					});
				}
				return;
			}
		}

		if (!player.isSneaking() && this.isForHire()) {
			// Open hiring window:
			this.openHireWindow(player);
		} else {
			// Open editor or trading window:
			super.onPlayerInteraction(player);
		}
	}

	// OWNER

	public static final Property<UUID> OWNER_UNIQUE_ID = new BasicProperty<UUID>()
			.dataKeyAccessor("owner uuid", UUIDSerializers.LENIENT)
			.build();
	public static final Property<String> OWNER_NAME = new BasicProperty<String>()
			.dataKeyAccessor("owner", StringSerializers.SCALAR)
			.validator(StringValidators.NON_EMPTY)
			.build();
	public static final Property<User> OWNER = new BasicProperty<User>()
			.name("owner")
			.dataAccessor(new DataAccessor<User>() {
				@Override
				public void save(DataContainer dataContainer, @Nullable User value) {
					if (value != null) {
						dataContainer.set(OWNER_UNIQUE_ID, value.getUniqueId());
						dataContainer.set(OWNER_NAME, value.getLastKnownName());
					} else {
						dataContainer.set(OWNER_UNIQUE_ID, null);
						dataContainer.set(OWNER_NAME, null);
					}
				}

				@Override
				public User load(DataContainer dataContainer) throws InvalidDataException {
					UUID ownerUniqueId = dataContainer.get(OWNER_UNIQUE_ID);
					String ownerName = dataContainer.get(OWNER_NAME);
					return SKUser.of(ownerUniqueId, ownerName);
				}
			})
			.build();

	private void loadOwner(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setOwner(shopkeeperData.get(OWNER));
	}

	private void saveOwner(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(OWNER, owner);
	}

	@Override
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), Unsafe.assertNonNull(player.getName()));
	}

	// TODO Add to API
	public void setOwner(User owner) {
		this._setOwner(owner);
		this.markDirty();
	}

	@Override
	public void setOwner(UUID ownerUUID, String ownerName) {
		this._setOwner(ownerUUID, ownerName);
		this.markDirty();
	}

	private void _setOwner(UUID ownerUUID, String ownerName) {
		this._setOwner(SKUser.of(ownerUUID, ownerName));
	}

	private void _setOwner(User owner) {
		Validate.notNull(owner, "owner is null");
		this.owner = owner;

		// Inform the shop object:
		this.getShopObject().onShopOwnerChanged();
	}

	public User getOwnerUser() {
		return owner;
	}

	@Override
	public UUID getOwnerUUID() {
		return owner.getUniqueId();
	}

	@Override
	public String getOwnerName() {
		return owner.getLastKnownName();
	}

	@Override
	public String getOwnerString() {
		return TextUtils.getPlayerString(owner);
	}

	@Override
	public boolean isOwner(Player player) {
		return player.getUniqueId().equals(this.getOwnerUUID());
	}

	@Override
	public @Nullable Player getOwner() {
		return Bukkit.getPlayer(this.getOwnerUUID());
	}

	// TRADE NOTIFICATIONS

	public static final Property<Boolean> NOTIFY_ON_TRADES = new BasicProperty<Boolean>()
			.dataKeyAccessor("notifyOnTrades", BooleanSerializers.LENIENT)
			.defaultValue(true)
			.omitIfDefault()
			.build();

	private void loadNotifyOnTrades(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setNotifyOnTrades(shopkeeperData.get(NOTIFY_ON_TRADES));
	}

	private void saveNotifyOnTrades(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(NOTIFY_ON_TRADES, notifyOnTrades);
	}

	@Override
	public boolean isNotifyOnTrades() {
		return notifyOnTrades;
	}

	@Override
	public void setNotifyOnTrades(boolean notifyOnTrades) {
		if (this.notifyOnTrades == notifyOnTrades) return;
		this._setNotifyOnTrades(notifyOnTrades);
		this.markDirty();
	}

	private void _setNotifyOnTrades(boolean notifyOnTrades) {
		this.notifyOnTrades = notifyOnTrades;
	}

	// HIRING

	public static final Property<@Nullable UnmodifiableItemStack> HIRE_COST_ITEM = new BasicProperty<@Nullable UnmodifiableItemStack>()
			.dataKeyAccessor("hirecost", ItemStackSerializers.UNMODIFIABLE)
			.validator(ItemStackValidators.Unmodifiable.NON_EMPTY)
			.nullable() // Null if the shop is not for hire
			.defaultValue(null)
			.build();

	static {
		// Register shopkeeper data migrations:
		// TODO Can be removed once all servers are expected to have updated to our new item stack
		// serialization format.
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"hire-cost-item",
				MigrationPhase.ofShopkeeperClass(AbstractPlayerShopkeeper.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				@Nullable UnmodifiableItemStack hireCost = shopkeeperData.get(HIRE_COST_ITEM);
				if (hireCost == null) return false; // Nothing to migrate
				assert !ItemUtils.isEmpty(hireCost);

				boolean itemMigrated = false;
				@Nullable UnmodifiableItemStack migratedHireCost = ItemMigration.migrateItemStack(hireCost);
				if (!ItemUtils.isSimilar(hireCost, migratedHireCost)) {
					if (ItemUtils.isEmpty(migratedHireCost)) {
						throw new InvalidDataException("Hire cost item migration failed: " + hireCost);
					}

					hireCost = migratedHireCost;
					itemMigrated = true;
				}

				if (!itemMigrated) return false; // Nothing migrated.

				// Write back the migrated hire cost item:
				shopkeeperData.set(HIRE_COST_ITEM, hireCost);
				Log.debug(DebugOptions.itemMigrations, () -> logPrefix + "Migrated hire cost item.");
				return true;
			}
		});
	}

	private void loadForHire(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setForHire(shopkeeperData.get(HIRE_COST_ITEM));
	}

	private void saveForHire(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(HIRE_COST_ITEM, hireCost);
	}

	@Override
	public boolean isForHire() {
		return (hireCost != null);
	}

	@Override
	public void setForHire(@ReadOnly @Nullable ItemStack hireCost) {
		this.setForHire(ItemUtils.unmodifiableClone(hireCost));
	}

	@Override
	public void setForHire(@Nullable UnmodifiableItemStack hireCost) {
		this._setForHire(hireCost);
		this.markDirty();
	}

	private void _setForHire(@Nullable UnmodifiableItemStack hireCost) {
		boolean isForHire = this.isForHire();
		if (ItemUtils.isEmpty(hireCost)) {
			// Disable hiring:
			this.hireCost = null;

			// If the shopkeeper was previously for hire, reset its name:
			if (isForHire) {
				this.setName("");
			}
		} else {
			// Set for hire:
			this.hireCost = Unsafe.assertNonNull(hireCost);
			this.setName(Messages.forHireTitle);
		}
		// TODO Close any currently open hiring UIs for players.
	}

	@Override
	public @Nullable UnmodifiableItemStack getHireCost() {
		return hireCost;
	}

	// CONTAINER

	// TODO Rename the storage keys to containerx/y/z?
	// TODO Change to a list of containers?
	// TODO Store container world independently of shopkeeper world?
	public static final Property<Integer> CONTAINER_X = new BasicProperty<Integer>()
			.dataKeyAccessor("chestx", NumberSerializers.INTEGER)
			.build();
	public static final Property<Integer> CONTAINER_Y = new BasicProperty<Integer>()
			.dataKeyAccessor("chesty", NumberSerializers.INTEGER)
			.build();
	public static final Property<Integer> CONTAINER_Z = new BasicProperty<Integer>()
			.dataKeyAccessor("chestz", NumberSerializers.INTEGER)
			.build();
	public static final Property<BlockLocation> CONTAINER = new BasicProperty<BlockLocation>()
			.name("container")
			.dataAccessor(new DataAccessor<BlockLocation>() {
				@Override
				public void save(DataContainer dataContainer, @Nullable BlockLocation value) {
					if (value != null) {
						dataContainer.set(CONTAINER_X, value.getX());
						dataContainer.set(CONTAINER_Y, value.getY());
						dataContainer.set(CONTAINER_Z, value.getZ());
					} else {
						dataContainer.set(CONTAINER_X, null);
						dataContainer.set(CONTAINER_Y, null);
						dataContainer.set(CONTAINER_Z, null);
					}
				}

				@Override
				public BlockLocation load(DataContainer dataContainer) throws InvalidDataException {
					int containerX = dataContainer.get(CONTAINER_X);
					int containerY = dataContainer.get(CONTAINER_Y);
					int containerZ = dataContainer.get(CONTAINER_Z);
					return new BlockLocation(containerX, containerY, containerZ);
				}
			})
			.build();

	private void loadContainer(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setContainer(shopkeeperData.get(CONTAINER));
	}

	private void saveContainer(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(CONTAINER, container);
	}

	private void protectContainer() {
		ProtectedContainers protectedContainers = SKShopkeepersPlugin.getInstance().getProtectedContainers();
		protectedContainers.addContainer(container, this);
	}

	private void unprotectContainer() {
		ProtectedContainers protectedContainers = SKShopkeepersPlugin.getInstance().getProtectedContainers();
		protectedContainers.removeContainer(container, this);
	}

	protected void _setContainer(int containerX, int containerY, int containerZ) {
		this._setContainer(new BlockLocation(this.getWorldName(), containerX, containerY, containerZ));
	}

	protected void _setContainer(BlockLocation container) {
		Validate.notNull(container, "container is null");
		if (this.isValid()) {
			// Disable the protection for the previous container:
			this.unprotectContainer();
		}

		// Update the container:
		// Ensure that the container's world matches the shopkeeper world:
		BlockLocation newContainer = container;
		String shopkeeperWorldName = this.getWorldName(); // Can be null for virtual shopkeepers
		if (!Objects.equals(container.getWorldName(), shopkeeperWorldName)) {
			MutableBlockLocation containerCopy = container.mutableCopy();
			containerCopy.setWorldName(shopkeeperWorldName);
			newContainer = containerCopy;
		}

		// Ensure that we store an immutable BlockLocation:
		this.container = newContainer.immutable();

		if (this.isValid()) {
			// Enable the protection for the new container:
			this.protectContainer();
		}
	}

	@Override
	public int getContainerX() {
		return container.getX();
	}

	@Override
	public int getContainerY() {
		return container.getY();
	}

	@Override
	public int getContainerZ() {
		return container.getZ();
	}

	public BlockLocation getContainerLocation() {
		return container;
	}

	@Override
	public void setContainer(int containerX, int containerY, int containerZ) {
		this._setContainer(containerX, containerY, containerZ);
		this.markDirty();
	}

	@Override
	public @Nullable Block getContainer() {
		return container.getBlock();
	}

	// Returns null if the container could not be found.
	public @Nullable Inventory getContainerInventory() {
		Block container = this.getContainer();
		if (container != null && ShopContainers.isSupportedContainer(container.getType())) {
			return ShopContainers.getInventory(container); // Not null
		}
		return null;
	}

	// Returns an empty array if the container could not be found.
	public @Nullable ItemStack[] getContainerContents() {
		Inventory containerInventory = this.getContainerInventory();
		if (containerInventory == null) {
			// Container not found:
			return InventoryUtils.emptyItemStackArray();
		} else {
			return Unsafe.cast(containerInventory.getContents()); // Not null
		}
	}

	@Override
	public int getCurrencyInContainer() {
		int totalCurrency = 0;
		// Empty if the container is not found:
		@Nullable ItemStack[] contents = this.getContainerContents();
		for (ItemStack itemStack : contents) {
			if (itemStack == null) continue;
			Currency currency = Currencies.match(itemStack);
			if (currency != null) {
				totalCurrency += (itemStack.getAmount() * currency.getValue());
			}
		}
		return totalCurrency;
	}

	// Returns null (and logs a warning) if the price cannot be represented correctly by currency
	// items.
	protected final @Nullable TradingRecipe createSellingRecipe(
			UnmodifiableItemStack itemBeingSold,
			int price,
			boolean outOfStock
	) {
		Validate.notNull(itemBeingSold, "itemBeingSold is null");
		Validate.isTrue(price > 0, "price has to be positive");

		UnmodifiableItemStack item1 = null;
		UnmodifiableItemStack item2 = null;

		int remainingPrice = price;
		if (Currencies.isHighCurrencyEnabled() && price > Settings.highCurrencyMinCost) {
			Currency highCurrency = Currencies.getHigh();
			int highCurrencyAmount = Math.min(
					price / highCurrency.getValue(),
					highCurrency.getMaxStackSize()
			);
			if (highCurrencyAmount > 0) {
				remainingPrice -= (highCurrencyAmount * highCurrency.getValue());
				UnmodifiableItemStack highCurrencyItem = highCurrency.getItemData().createUnmodifiableItemStack(highCurrencyAmount);
				item1 = highCurrencyItem; // Using the first slot
			}
		}

		if (remainingPrice > 0) {
			Currency baseCurrency = Currencies.getBase();
			int maxStackSize = baseCurrency.getMaxStackSize();
			if (remainingPrice > maxStackSize) {
				// Cannot represent this price with the used currency items:
				// TODO Move this warning into the loading phase.
				int maxPrice = getMaximumSellingPrice();
				Log.warning(this.getLogPrefix() + "Skipping offer with invalid price (" + price
						+ "). Maximum price is " + maxPrice + ".");
				return null;
			}

			UnmodifiableItemStack currencyItem = baseCurrency.getItemData().createUnmodifiableItemStack(remainingPrice);
			if (item1 == null) {
				item1 = currencyItem;
			} else {
				// The first item of the trading recipe is already used by the high currency item:
				item2 = currencyItem;
			}
		}
		assert item1 != null;
		return new SKTradingRecipe(itemBeingSold, item1, item2, outOfStock);
	}

	// Returns null (and logs a warning) if the price cannot be represented correctly by currency
	// items.
	protected final @Nullable TradingRecipe createBuyingRecipe(
			UnmodifiableItemStack itemBeingBought,
			int price,
			boolean outOfStock
	) {
		Currency currency = Currencies.getBase();
		int maxPrice = currency.getStackValue();
		if (price > maxPrice) {
			// Cannot represent this price with the used currency items:
			// TODO Move this warning into the loading phase.
			Log.warning(this.getLogPrefix() + "Skipping offer with invalid price (" + price
					+ "). Maximum price is " + maxPrice + ".");
			return null;
		}
		UnmodifiableItemStack currencyItem = currency.getItemData().createUnmodifiableItemStack(price);
		return new SKTradingRecipe(currencyItem, itemBeingBought, null, outOfStock);
	}

	private static int getMaximumSellingPrice() {
		// Combined value of two stacks of the two highest valued currencies:
		// TODO In the future: Two stacks of the single highest valued currency.
		int maxPrice = 0;
		int currenciesCount = Currencies.getAll().size();
		Currency currency1 = Currencies.getAll().get(currenciesCount - 1);
		maxPrice += currency1.getStackValue();

		if (currenciesCount > 1) {
			Currency currency2 = Currencies.getAll().get(currenciesCount - 2);
			maxPrice += currency2.getStackValue();
		}
		return maxPrice;
	}

	// SHOPKEEPER UIs - Shortcuts for common UI types:

	@Override
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUITypes.HIRING(), player);
	}

	@Override
	public boolean openContainerWindow(Player player) {
		// Check if the container still exists:
		Inventory containerInventory = this.getContainerInventory();
		if (containerInventory == null) {
			Log.debug(() -> "Cannot open container inventory for player '" + player.getName()
					+ "': The block is no longer a valid container!");
			return false;
		}

		Log.debug(() -> "Opening container inventory for player '" + player.getName() + "'.");
		// Open the container directly for the player (no need for a custom UI):
		return player.openInventory(containerInventory) != null;
	}

	// EDITOR

	@Override
	public List<String> getInformation() {
		var information = super.getInformation();
		var messageArguments = this.getMessageArguments("shop_");
		information.addAll(StringUtils.replaceArguments(Messages.playerShopInformation, messageArguments));
		return information;
	}

	// TICKING

	@Override
	protected void onTick() {
		this.onTickCheckDeleteIfContainerBroken();
		super.onTick();
	}

	// Deletes the shopkeeper if the container is no longer present (e.g. if it got removed
	// externally by another plugin, such as WorldEdit, etc.):
	private void onTickCheckDeleteIfContainerBroken() {
		if (!Settings.deleteShopkeeperOnBreakContainer) return;
		if (!checkContainerLimiter.request()) {
			return;
		}

		// This checks if the block is still a valid container:
		Block containerBlock = this.getContainer();
		if (containerBlock != null && !ShopContainers.isSupportedContainer(containerBlock.getType())) {
			// Note: If this shopkeeper is deleted due to the chest having been broken, we will
			// trigger a delayed save after the ticking of the shopkeepers.
			SKShopkeepersPlugin.getInstance().getRemoveShopOnContainerBreak().handleBlockBreakage(containerBlock);
		}
	}
}
