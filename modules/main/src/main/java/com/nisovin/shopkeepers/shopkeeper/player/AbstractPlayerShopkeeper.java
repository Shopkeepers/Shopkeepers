package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
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
import com.nisovin.shopkeepers.api.shopkeeper.container.DefaultShopContainerTypes;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopMember;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.container.SKShopContainer;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.currency.CurrencyInventoryUtils;
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
import com.nisovin.shopkeepers.shopkeeper.player.members.SKPlayerShopMember;
import com.nisovin.shopkeepers.ui.containers.PlayerShopContainersEditorViewProvider;
import com.nisovin.shopkeepers.ui.members.PlayerShopMembersEditorViewProvider;
import com.nisovin.shopkeepers.user.SKUser;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
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
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractPlayerShopkeeper
		extends AbstractShopkeeper implements PlayerShopkeeper {

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

		// Migrate the legacy single-container storage (chestx/y/z) to the new container list:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"container-list",
				MigrationPhase.ofShopkeeperClass(AbstractPlayerShopkeeper.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				// Read the legacy container location (null if missing or already migrated):
				BlockLocation legacyContainer = shopkeeperData.getOrNullIfMissing(CONTAINER);
				if (legacyContainer == null) return false; // Nothing to migrate

				// Apply the shopkeeper's world to the container: The legacy storage did not store
				// the container's world, but derived it from the shopkeeper's world.
				// If the shopkeeper has no world (e.g. missing or a virtual shopkeeper): Flagged as
				// data error so the shopkeeper is not loaded and can be inspected and migrated
				// manually.
				String worldName = shopkeeperData.getOrNullIfMissing(AbstractShopkeeper.WORLD_NAME);
				if (worldName == null) {
					throw new InvalidDataException(
							"Container list migration failed: The shopkeeper has no world!"
					);
				}

				var location = new BlockLocation(
						worldName,
						legacyContainer.getX(),
						legacyContainer.getY(),
						legacyContainer.getZ()
				);

				// Convert to the new container list format:
				var container = new SKShopContainer(
						location,
						DefaultShopContainerTypes.STOCK_AND_EARNINGS()
				);
				shopkeeperData.set(CONTAINERS, Collections.singletonList(container));

				// Remove the legacy container data:
				shopkeeperData.remove(CONTAINER.getName());

				Log.debug(() -> logPrefix + "Migrated container to the container list format.");
				return true;
			}
		});
	}

	private static final int CHECK_CONTAINER_PERIOD_SECONDS = 5;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(
			1,
			CHECK_CONTAINER_PERIOD_SECONDS + 1
	);

	// Valid after successful initialization:
	private PlayerShopMember owner = SKPlayerShopMember.EMPTY;
	private final List<SKPlayerShopMember> members = new ArrayList<SKPlayerShopMember>();
	private final List<? extends PlayerShopMember> membersView = Collections.unmodifiableList(members);
	// Each container stores its own world, which is usually, but not necessarily, the shopkeeper's
	// world:
	private final List<SKShopContainer> containers = new ArrayList<SKShopContainer>();
	private final List<? extends SKShopContainer> containersView = Collections.unmodifiableList(containers);
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
		Block containerBlock = playerShopCreationData.getShopContainer();

		this._setOwner(owner.getUniqueId(), Unsafe.assertNonNull(owner.getName()));

		SKShopContainer container = new SKShopContainer(
				BlockLocation.of(containerBlock),
				DefaultShopContainerTypes.STOCK_AND_EARNINGS()
		);
		this._setContainers(Collections.singletonList(container));
	}

	@Override
	protected void setup() {
		this.registerViewProviderIfMissing(DefaultUITypes.SHOP_MEMBERS_EDITOR(), () -> {
			return new PlayerShopMembersEditorViewProvider(this);
		});
		this.registerViewProviderIfMissing(DefaultUITypes.SHOP_CONTAINERS_EDITOR(), () -> {
			return new PlayerShopContainersEditorViewProvider(this);
		});
		this.registerViewProviderIfMissing(DefaultUITypes.HIRING(), () -> {
			return new PlayerShopHiringViewProvider(this);
		});
		super.setup();
	}

	@Override
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		super.loadDynamicState(shopkeeperData);
		this.loadOwner(shopkeeperData);
		this.loadMembers(shopkeeperData);
		this.loadContainers(shopkeeperData);
		this.loadNotifyOnTrades(shopkeeperData);
		this.loadForHire(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ShopkeeperData shopkeeperData, boolean saveAll) {
		super.saveDynamicState(shopkeeperData, saveAll);
		this.saveOwner(shopkeeperData);
		this.saveMembers(shopkeeperData);
		this.saveContainers(shopkeeperData);
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
		this.protectContainers();
	}

	@Override
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		super.onRemoval(cause);

		// Disable the container protection:
		this.unprotectContainers();
	}

	@Override
	public void delete(@Nullable Player player) {
		// Return the shop creation item:
		if (Settings.deletingPlayerShopReturnsCreationItem
				&& player != null
				&& this.hasAccessLevel(player, DefaultPlayerShopAccessLevels.FULL())) {
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
					Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
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
		shopkeeperData.set(OWNER, owner.getUser());
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
		this.owner = new SKPlayerShopMember(owner, true, DefaultPlayerShopAccessLevels.FULL());

		// Inform the shop object:
		this.getShopObject().onShopOwnerChanged();
	}

	public User getOwnerUser() {
		return owner.getUser();
	}

	@Override
	public UUID getOwnerUUID() {
		return owner.getUser().getUniqueId();
	}

	@Override
	public String getOwnerName() {
		return owner.getUser().getLastKnownName();
	}

	@Override
	public String getOwnerString() {
		return TextUtils.getPlayerString(owner.getUser());
	}

	@Override
	public boolean isOwner(Player player) {
		return this.isOwner(player.getUniqueId());
	}

	@Override
	public boolean isOwner(UUID playerUUID) {
		return this.getOwnerUUID().equals(playerUUID);
	}

	@Override
	public @Nullable Player getOwner() {
		return Bukkit.getPlayer(this.getOwnerUUID());
	}

	// MEMBERS

	public static final Property<List<? extends PlayerShopMember>> MEMBERS = new BasicProperty<List<? extends PlayerShopMember>>()
			.dataKeyAccessor("members", SKPlayerShopMember.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	private void loadMembers(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setMembers(shopkeeperData.get(MEMBERS));
	}

	private void saveMembers(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(MEMBERS, membersView);
	}

	@Override
	public Collection<? extends PlayerShopMember> getMembers() {
		if (Settings.maxMembersPerShop == 0) {
			// Shop members feature is disabled:
			return Collections.emptyList();
		}

		return membersView;
	}

	public @Nullable Player getFirstOnlineMember() {
		var owner = this.getOwner();
		if (owner != null) {
			return owner;
		}

		for (var member : this.getMembers()) {
			var memberPlayer = member.getUser().getPlayer();
			if (memberPlayer != null) {
				return memberPlayer;
			}
		}

		return null;
	}

	private void _setMembers(List<? extends PlayerShopMember> members) {
		assert members != null && !CollectionUtils.containsNull(members);
		this.members.clear();
		members.forEach(this::_addMember);
	}

	private void _addMember(PlayerShopMember member) {
		Validate.notNull(member, "member is null");
		Validate.isTrue(member instanceof SKPlayerShopMember,
				"Member is not of type SKPlayerShopMember!");

		var skMember = (SKPlayerShopMember) member;
		Validate.isTrue(!skMember.isOwner(), "Cannot add shop owner as member!");

		var playerUUID = member.getUser().getUniqueId();
		Validate.isTrue(!this.isOwner(playerUUID), "Cannot add shop owner as member!");
		Validate.isTrue(this._getMember(playerUUID) == null, "Already a member!");
		Validate.isTrue(member.getAccessLevel() != DefaultPlayerShopAccessLevels.NONE(),
				"Invalid member access level: " + DefaultPlayerShopAccessLevels.NONE().getIdentifier());

		members.add((SKPlayerShopMember) member);
	}

	@Override
	public void addMember(UUID playerUUID, String playerName, PlayerShopAccessLevel accessLevel) {
		if (Settings.maxMembersPerShop == 0) {
			// Shop members feature is disabled:
			return;
		}

		var user = SKUser.of(playerUUID, playerName);
		var newMember = new SKPlayerShopMember(user, false, accessLevel);
		// Validate the new member:
		this._addMember(newMember);
		this.markDirty();
	}

	@Override
	public void removeMember(UUID playerUUID) {
		Validate.isTrue(!this.isOwner(playerUUID), "Cannot remove shop owner from members!");
		if (members.removeIf(x -> x.getUser().getUniqueId().equals(playerUUID))) {
			this.markDirty();
		}
	}

	/**
	 * Updates the specified shop member, i.e. their name and/or access level.
	 * <p>
	 * Unlike removing and re-adding the member, this preserves the members position in the member
	 * list.
	 * 
	 * @param playerUUID
	 *            the member's uuid, not <code>null</code>
	 * @param playerName
	 *            the member's name, or <code>null</code> to preserve the current name
	 * @param accessLevel
	 *            the {@link PlayerShopAccessLevel}, not
	 *            {@link DefaultPlayerShopAccessLevels#getNone()}, or <code>null</code> to preserve
	 *            the current access level
	 * @throws IllegalArgumentException
	 *             if the specified player is the {@link #isOwner(UUID) owner} or not a
	 *             {@link #isMember(UUID) member}
	 */
	public void updateMember(
			UUID playerUUID,
			@Nullable String playerName,
			@Nullable PlayerShopAccessLevel accessLevel
	) {
		Validate.isTrue(!this.isOwner(playerUUID), "The specified player is the shop owner!");
		Validate.isTrue(accessLevel != DefaultPlayerShopAccessLevels.NONE(),
				"Invalid member access level: " + DefaultPlayerShopAccessLevels.NONE().getIdentifier());

		var member = this.getMember(playerUUID);
		if (member == null) {
			throw new IllegalArgumentException("The specified player is not a shop member!");
		}

		// Update the member, preserving its position within the members list:

		var index = members.indexOf(member);
		assert index >= 0 && index < members.size();

		var newMemberName = playerName == null ? member.getUser().getName() : playerName;
		var newUser = SKUser.of(playerUUID, newMemberName);
		var newAccessLevel = accessLevel == null ? member.getAccessLevel() : accessLevel;

		var newMember = new SKPlayerShopMember(newUser, false, newAccessLevel);
		members.set(index, newMember);
		this.markDirty();
	}

	@Override
	public @Nullable PlayerShopMember getMember(UUID playerUUID) {
		if (this.isOwner(playerUUID)) {
			return owner;
		}

		// Shop members feature is disabled:
		if (Settings.maxMembersPerShop == 0) {
			return null;
		}

		return this._getMember(playerUUID);
	}

	// No owner or setting check:
	private @Nullable PlayerShopMember _getMember(UUID playerUUID) {
		for (var member : membersView) {
			if (member.getUser().getUniqueId().equals(playerUUID)) {
				return member;
			}
		}

		return null;
	}

	@Override
	public boolean isMember(Player player) {
		return this.isMember(player.getUniqueId());
	}

	@Override
	public boolean isMember(UUID playerUUID) {
		return this.getMember(playerUUID) != null;
	}

	@Override
	public PlayerShopAccessLevel getAccessLevel(UUID playerUUID) {
		// Includes the owner:
		var member = this.getMember(playerUUID);
		if (member == null) {
			return DefaultPlayerShopAccessLevels.NONE();
		}

		return member.getAccessLevel();
	}

	@Override
	public void setAccessLevel(UUID playerUUID, PlayerShopAccessLevel accessLevel) {
		// Update the member, preserving its name and its position within the members list:
		this.updateMember(playerUUID, null, accessLevel);
	}

	@Override
	public boolean hasAccessLevel(Player player, PlayerShopAccessLevel accessLevel) {
		return this.hasAccessLevel(player.getUniqueId(), accessLevel);
	}

	@Override
	public boolean hasAccessLevel(UUID playerUUID, PlayerShopAccessLevel accessLevel) {
		var memberAccessLevel = this.getAccessLevel(playerUUID);
		return memberAccessLevel.includes(accessLevel);
	}

	/**
	 * Checks if the given {@link CommandSender} has either the specified
	 * {@link PlayerShopAccessLevel} or the bypass permission.
	 * <p>
	 * For non-player {@link CommandSender}s, this only checks the bypass permission.
	 * 
	 * @param sender
	 *            the {@link CommandSender}, not <code>null</code>
	 * @param accessLevel
	 *            the {@link PlayerShopAccessLevel}, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to omit any feedback that might otherwise be sent to the sender
	 * @return <code>true</code> if the sender has the specified access level for this shopkeeper
	 */
	public boolean checkAccess(CommandSender sender, PlayerShopAccessLevel accessLevel, boolean silent) {
		Validate.notNull(sender, "sender is null");
		Validate.notNull(accessLevel, "accessLevel is null");

		if (sender instanceof Player player) {
			if (!this.hasAccessLevel(player, accessLevel)
					&& !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				if (!silent) {
					TextUtils.sendMessage(player, Messages.missingAccessLevel);
				}
				return false;
			}

			return true;
		} else {
			// Check if the command sender has the bypass permission (e.g. the case for the console
			// and block command senders, but might not be the case for other unexpected types of
			// command senders):
			return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.BYPASS_PERMISSION);
		}
	}

	public boolean canEditMembers(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		return this.canAccess(player, DefaultUITypes.SHOP_MEMBERS_EDITOR(), silent);
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

	// CONTAINERS

	// TODO Legacy single-container storage keys (chestx/y/z): Remove once no longer needed for the
	// migration.
	private static final Property<Integer> CONTAINER_X = new BasicProperty<Integer>()
			.dataKeyAccessor("chestx", NumberSerializers.INTEGER)
			.build();
	private static final Property<Integer> CONTAINER_Y = new BasicProperty<Integer>()
			.dataKeyAccessor("chesty", NumberSerializers.INTEGER)
			.build();
	private static final Property<Integer> CONTAINER_Z = new BasicProperty<Integer>()
			.dataKeyAccessor("chestz", NumberSerializers.INTEGER)
			.build();
	private static final Property<BlockLocation> CONTAINER = new BasicProperty<BlockLocation>()
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

	public static final Property<List<? extends ShopContainer>> CONTAINERS = new BasicProperty<List<? extends ShopContainer>>()
			.dataKeyAccessor("containers", SKShopContainer.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	private void loadContainers(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setContainers(shopkeeperData.get(CONTAINERS));
	}

	private void saveContainers(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(CONTAINERS, containersView);
	}

	@Override
	public Collection<? extends SKShopContainer> getContainers() {
		return containersView;
	}

	// Returns null if this shopkeeper has no container at the given location.
	private @Nullable SKShopContainer getContainer(BlockLocation location) {
		Validate.notNull(location, "location is null");
		for (SKShopContainer container : containers) {
			if (container.getLocation().equals(location)) {
				return container;
			}
		}
		return null;
	}

	private void protectContainers() {
		var protectedContainers = SKShopkeepersPlugin.getInstance().getProtectedContainers();
		for (SKShopContainer container : containers) {
			protectedContainers.addContainer(container.getLocation(), this);
		}
	}

	private void unprotectContainers() {
		var protectedContainers = SKShopkeepersPlugin.getInstance().getProtectedContainers();
		for (SKShopContainer container : containers) {
			protectedContainers.removeContainer(container.getLocation(), this);
		}
	}

	protected void _setContainers(List<? extends ShopContainer> containers) {
		Validate.notNull(containers, "containers is null");
		for (ShopContainer container : containers) {
			Validate.notNull(container, "container is null");
			Validate.isTrue(container instanceof SKShopContainer,
					"Container is not of type SKShopContainer!");
		}

		// Disable the protection for the previous containers:
		if (this.isValid()) {
			this.unprotectContainers();
		}

		this.containers.clear();
		for (ShopContainer container : containers) {
			this.containers.add((SKShopContainer) container);
		}

		// Enable the protection for the new containers:
		if (this.isValid()) {
			this.protectContainers();
		}
	}

	@Override
	public ShopContainer addContainer(
			String worldName,
			int containerX,
			int containerY,
			int containerZ,
			ShopContainerType type
	) {
		Validate.notEmpty(worldName, "worldName is null or empty");
		Validate.notNull(type, "type is null");
		var location = new BlockLocation(worldName, containerX, containerY, containerZ);
		Validate.isTrue(this.getContainer(location) == null,
				"There is already a container at this location!");

		List<SKShopContainer> newContainers = new ArrayList<>(containers);
		var container = new SKShopContainer(location, type);
		newContainers.add(container);
		this._setContainers(newContainers);
		this.markDirty();

		return container;
	}

	@Override
	public void removeContainer(
			String worldName,
			int containerX,
			int containerY,
			int containerZ
	) {
		Validate.notEmpty(worldName, "worldName is null or empty");
		var location = new BlockLocation(worldName, containerX, containerY, containerZ);
		var container = this.getContainer(location);
		if (container == null) return;

		List<SKShopContainer> newContainers = new ArrayList<>(containers);
		newContainers.remove(container);
		this._setContainers(newContainers);
		this.markDirty();
	}

	/**
	 * Updates the container at the given location, i.e. its {@link ShopContainerType type}.
	 * <p>
	 * Unlike removing and re-adding the container, this preserves the container's position in the
	 * container list.
	 * 
	 * @param location
	 *            the container's location, not <code>null</code>
	 * @param type
	 *            the {@link ShopContainerType}, or <code>null</code> to preserve the current type
	 * @throws IllegalArgumentException
	 *             if there is no container at the given location
	 */
	public void updateContainer(BlockLocation location, @Nullable ShopContainerType type) {
		Validate.notNull(location, "location is null");
		var container = this.getContainer(location);
		if (container == null) {
			throw new IllegalArgumentException("There is no container at the given location!");
		}

		// Note: The container location stays the same, so there is no need to update the container
		// protections.

		// Update the container, preserving its position within the container list:
		var index = containers.indexOf(container);
		assert index >= 0 && index < containers.size();
		var newType = (type == null) ? container.getType() : type;
		containers.set(index, container.withType(newType));
		this.markDirty();
	}

	@Deprecated
	@Override
	public int getContainerX() {
		return this.getFirstContainerLocation().getX();
	}

	@Deprecated
	@Override
	public int getContainerY() {
		return this.getFirstContainerLocation().getY();
	}

	@Deprecated
	@Override
	public int getContainerZ() {
		return this.getFirstContainerLocation().getZ();
	}

	// Returns null if this shopkeeper currently has no containers.
	private @Nullable SKShopContainer getFirstContainer() {
		return CollectionUtils.getFirstOrNull(containers);
	}

	// Returns the first container's location, or BlockLocation#EMPTY if there are no containers.
	private BlockLocation getFirstContainerLocation() {
		SKShopContainer firstContainer = this.getFirstContainer();
		return firstContainer != null ? firstContainer.getLocation() : BlockLocation.EMPTY;
	}

	@Deprecated
	@Override
	public void setContainer(int containerX, int containerY, int containerZ) {
		SKShopContainer firstContainer = this.getFirstContainer();
		if (firstContainer == null) {
			// The shop has no containers yet: Add a new container at the given coordinates, using
			// the shopkeeper's world.
			// Error if the shopkeeper has no world (e.g. virtual shops).
			var worldName = this.getWorldName();
			Validate.notNull(worldName, "Cannot add container without a world!");
			assert worldName != null;
			this.addContainer(
					worldName,
					containerX,
					containerY,
					containerZ,
					DefaultShopContainerTypes.STOCK_AND_EARNINGS()
			);
			return;
		}

		// Update the coordinates of the first container:
		var location = new BlockLocation(firstContainer.getWorldName(), containerX, containerY, containerZ);
		List<SKShopContainer> newContainers = new ArrayList<>(containers);
		newContainers.set(0, new SKShopContainer(location, firstContainer.getType()));
		this._setContainers(newContainers);
		this.markDirty();
	}

	@Deprecated
	@Override
	public @Nullable Block getContainer() {
		return this.getFirstContainerLocation().getBlock();
	}

	// Returns the combined contents of all stock containers.
	// Returns an empty array if no stock containers could be found.
	public @Nullable ItemStack[] getStockContainerContents() {
		return this.getContainerContents(true);
	}

	// Returns the combined contents of all earnings containers.
	// Returns an empty array if no earnings containers could be found.
	public @Nullable ItemStack[] getEarningsContainerContents() {
		return this.getContainerContents(false);
	}

	// Returns the combined contents of all stock or earnings containers.
	// The returned array is only meant to be read (e.g. to search or count items), not written
	// back. It may (or may not) skip empty item stacks.
	private @Nullable ItemStack[] getContainerContents(boolean stock) {
		if (containers.isEmpty()) {
			return InventoryUtils.emptyItemStackArray();
		}

		// Fast path for a single container: Return its contents directly (avoids array copy).
		if (containers.size() == 1) {
			ShopContainer container = containers.get(0);
			var type = container.getType();
			if (stock ? type.isStock() : type.isEarnings()) {
				Inventory containerInventory = container.getInventory();
				if (containerInventory != null) {
					return Unsafe.cast(containerInventory.getContents());
				}
			}

			return InventoryUtils.emptyItemStackArray();
		}

		// Combine the contents of all matching containers, skipping empty item stacks to keep the
		// returned array (and the downstream iteration) smaller:
		List<@Nullable ItemStack> contents = new ArrayList<>();
		for (ShopContainer container : containers) {
			var type = container.getType();
			if (stock ? !type.isStock() : !type.isEarnings()) continue;

			Inventory containerInventory = container.getInventory();
			if (containerInventory == null) continue;

			for (ItemStack itemStack : containerInventory.getContents()) {
				if (ItemUtils.isEmpty(itemStack)) continue;

				contents.add(itemStack);
			}
		}

		return contents.toArray(new @Nullable ItemStack[0]);
	}

	@Override
	public int getCurrencyInContainer() {
		// Empty if no stock containers are found:
		@Nullable ItemStack[] contents = this.getStockContainerContents();
		return CurrencyInventoryUtils.countCurrency(contents);
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

	@Deprecated
	@Override
	public boolean openContainerWindow(Player player) {
		var firstContainer = this.getFirstContainer();
		if (firstContainer == null) {
			Log.debug(() -> "Cannot open container inventory for player '" + player.getName()
					+ "': The shop has no container!");
			return false;
		}

		return firstContainer.openInventoryView(player);
	}

	@Override
	public boolean openMembersEditorWindow(Player player) {
		return this.openWindow(DefaultUITypes.SHOP_MEMBERS_EDITOR(), player);
	}

	@Override
	public boolean openContainersEditorWindow(Player player) {
		return this.openWindow(DefaultUITypes.SHOP_CONTAINERS_EDITOR(), player);
	}

	@Override
	public boolean closeView(Player player) {
		if (super.closeView(player)) {
			return true;
		}

		// Close any views for the shop containers:
		var openInventoryView = player.getOpenInventory();
		var inventoryBlock = InventoryUtils.getBlock(openInventoryView.getTopInventory());
		if (inventoryBlock != null) {
			var shopkeepersUsingBlock = SKShopkeepersPlugin.getInstance()
					.getProtectedContainers()
					.getShopkeepersUsingContainer(inventoryBlock);
			if (shopkeepersUsingBlock.contains(this)) {
				openInventoryView.close();
				return true;
			}
		}

		return false;
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

	// Deletes the shopkeeper if none of its containers are present anymore (e.g. if they got
	// removed externally by another plugin, such as WorldEdit, etc.):
	// While there is still at least one container remaining, broken containers are kept in the
	// container list (they show up as "missing" in the editor).
	private void onTickCheckDeleteIfContainerBroken() {
		if (!Settings.deleteShopkeeperOnBreakContainer) return;
		if (!checkContainerLimiter.request()) {
			return;
		}

		// Delete the shopkeeper if none of its containers are valid anymore (all of its containers
		// are broken, or it has no containers at all, e.g. after they were removed via the API).
		// Note: If this shopkeeper is deleted here, we will trigger a delayed save after the
		// ticking of the shopkeepers.
		SKShopkeepersPlugin.getInstance().getRemoveShopOnContainerBreak()
				.deleteShopIfContainersBroken(this);
	}
}
