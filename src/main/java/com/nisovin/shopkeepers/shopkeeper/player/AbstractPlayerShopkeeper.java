package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractPlayerShopkeeper extends AbstractShopkeeper implements PlayerShopkeeper {

	private static final int CHECK_CHEST_PERIOD_SECONDS = 5;

	protected User owner; // not null after successful initialization
	// TODO store chest world separately? currently it uses the shopkeeper world
	// this would allow the chest and shopkeeper to be located in different worlds, and virtual player shops
	protected int chestX;
	protected int chestY;
	protected int chestZ;
	protected ItemStack hireCost = null; // null if not for hire

	// random shopkeeper-specific starting offset between [1, CHECK_CHEST_PERIOD_SECONDS]
	private int remainingCheckChestSeconds = (int) (Math.random() * CHECK_CHEST_PERIOD_SECONDS) + 1;

	/**
	 * Creates a not yet initialized {@link AbstractPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected AbstractPlayerShopkeeper(int id) {
		super(id);
	}

	/**
	 * Expects a {@link PlayerShopCreationData}.
	 */
	@Override
	protected void loadFromCreationData(ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super.loadFromCreationData(shopCreationData);
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;
		User creatorUser = playerShopCreationData.getCreatorUser();
		Block chest = playerShopCreationData.getShopChest();
		assert creatorUser != null;
		assert chest != null;

		this.owner = creatorUser;
		this._setChest(chest.getX(), chest.getY(), chest.getZ());
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.HIRING()) == null) {
			this.registerUIHandler(new PlayerShopHiringHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		UUID ownerUUID;
		try {
			ownerUUID = UUID.fromString(configSection.getString("owner"));
		} catch (Exception e) {
			// Owner uuid invalid or non-existent.
			// Try loading from 'owner uuid' (removed in late MC 1.15.x):
			// TODO Remove loading from legacy data again at some point.
			// Since late MC 1.15.x we store the owner uuid in 'owner' instead of 'owner uuid'. 'owner' previously
			// stored the owner's name, which has been removed from the save data completely. Previously stored owner
			// names should however not parse as valid uuid, so reusing that data key to store the uuid now should not
			// be an issue.
			try {
				ownerUUID = UUID.fromString(configSection.getString("owner uuid"));
			} catch (Exception e2) {
				throw new ShopkeeperCreateException("Missing or invalid owner uuid!");
			}
		}
		assert ownerUUID != null;
		// We load the user immediately here. Since we usually load shopkeepers only during plugin startup and on plugin
		// reloads, this should not be an issue.
		this.owner = ShopkeepersAPI.getUserManager().getOrCreateUserImmediately(ownerUUID);
		assert this.owner != null;

		if (!configSection.isInt("chestx") || !configSection.isInt("chesty") || !configSection.isInt("chestz")) {
			throw new ShopkeeperCreateException("Missing chest coordinate(s)");
		}

		// update chest:
		this._setChest(configSection.getInt("chestx"), configSection.getInt("chesty"), configSection.getInt("chestz"));

		hireCost = configSection.getItemStack("hirecost");
		// hire cost itemstack is not null, but empty -> normalize to null:
		if (hireCost != null && ItemUtils.isEmpty(hireCost)) {
			Log.warning("Invalid (empty) hire cost! Disabling 'for hire' for shopkeeper at " + this.getPositionString());
			hireCost = null;
			this.markDirty();
		}
		ItemStack migratedHireCost = ItemUtils.migrateItemStack(hireCost);
		if (!ItemUtils.isSimilar(hireCost, migratedHireCost)) {
			if (ItemUtils.isEmpty(migratedHireCost) && !ItemUtils.isEmpty(hireCost)) {
				// migration failed:
				Log.warning("Shopkeeper " + this.getId() + ": Hire cost item migration failed: " + hireCost.toString());
				hireCost = null;
			} else {
				hireCost = migratedHireCost;
				Log.debug(Settings.DebugOptions.itemMigrations,
						() -> "Shopkeeper " + this.getId() + ": Migrated hire cost item."
				);
			}
			this.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("owner", owner.getUniqueId().toString());
		configSection.set("chestx", chestX);
		configSection.set("chesty", chestY);
		configSection.set("chestz", chestZ);
		if (hireCost != null) {
			configSection.set("hirecost", hireCost);
		}
	}

	@Override
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onAdded(cause);

		// register protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().addChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		super.onRemoval(cause);

		// unregister previously protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().removeChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	public void delete(Player player) {
		// Return the shop creation item:
		if (Settings.deletingPlayerShopReturnsCreationItem && player != null && this.isOwner(player)) {
			ItemStack shopCreationItem = Settings.createShopCreationItem();
			Map<Integer, ItemStack> remaining = player.getInventory().addItem(shopCreationItem);
			if (!remaining.isEmpty()) {
				// Inventory is full, drop the item instead:
				Location playerLocation = player.getEyeLocation();
				Location shopLocation = this.getShopObject().getLocation();
				// If within a certain range, drop the item at the shop's location, else drop at player's location:
				Location dropLocation;
				if (shopLocation != null && Utils.getDistanceSquared(shopLocation, playerLocation) <= 100) {
					dropLocation = shopLocation;
				} else {
					dropLocation = playerLocation;
				}
				dropLocation.getWorld().dropItem(dropLocation, shopCreationItem);
			}
		}
		super.delete(player);
	}

	@Override
	public void onPlayerInteraction(Player player) {
		// naming via item:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		if (Settings.namingOfPlayerShopsViaItem && Settings.isNamingItem(itemInMainHand)) {
			// check if player can edit this shopkeeper:
			PlayerShopEditorHandler editorHandler = (PlayerShopEditorHandler) this.getUIHandler(DefaultUITypes.EDITOR());
			if (editorHandler.canOpen(player)) {
				// rename with the player's item in hand:
				ItemMeta itemMeta = itemInMainHand.getItemMeta(); // can be null
				String newName = (itemMeta != null && itemMeta.hasDisplayName()) ? itemMeta.getDisplayName() : "";
				assert newName != null; // ItemMeta#getDisplayName returns non-null in all cases

				// handled name changing:
				if (SKShopkeepersPlugin.getInstance().getShopkeeperNaming().requestNameChange(player, this, newName)) {
					// manually remove rename item from player's hand after this event is processed:
					Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
						ItemStack newItemInMainHand = ItemUtils.descreaseItemAmount(itemInMainHand, 1);
						playerInventory.setItemInMainHand(newItemInMainHand);
					});
				}
				return;
			}
		}

		if (!player.isSneaking() && this.isForHire()) {
			// open hiring window:
			this.openHireWindow(player);
		} else {
			// open editor or trading window:
			super.onPlayerInteraction(player);
		}
	}

	@Override
	public User getOwner() {
		return owner;
	}

	@Override
	public void setOwner(User newOwner) {
		Validate.notNull(newOwner, "newOwner is null");
		Validate.isTrue(newOwner.isValid(), "newOwner is invalid");
		this.markDirty();
		this.owner = newOwner;

		// Inform shop object:
		this.getShopObject().onShopkeeperOwnerChanged();
	}

	@Override
	public boolean isOwner(UUID playerId) {
		Validate.notNull(playerId, "playerId is null");
		return owner.getUniqueId().equals(playerId);
	}

	@Override
	public boolean isOwner(User user) {
		Validate.notNull(user, "user is null");
		return this.isOwner(user.getUniqueId());
	}
	
	@Override
	public boolean isOwner(OfflinePlayer player) {
		Validate.notNull(player, "player is null");
		return this.isOwner(player.getUniqueId());
	}

	@Override
	public String getOwnerString() {
		return owner.toPrettyString();
	}

	@Override
	public boolean isForHire() {
		return (hireCost != null);
	}

	@Override
	public void setForHire(ItemStack hireCost) {
		this.markDirty();
		if (ItemUtils.isEmpty(hireCost)) {
			// disable hiring:
			this.hireCost = null;
			this.setName("");
		} else {
			// set for hire:
			this.hireCost = hireCost.clone();
			this.setName(Settings.forHireTitle);
		}
	}

	@Override
	public ItemStack getHireCost() {
		return (this.isForHire() ? hireCost.clone() : null);
	}

	protected void _setChest(int chestX, int chestY, int chestZ) {
		if (this.isValid()) {
			// unregister previously protected chest:
			SKShopkeepersPlugin.getInstance().getProtectedChests().removeChest(this.getWorldName(), chestX, chestY, chestZ, this);
		}

		// update chest:
		this.chestX = chestX;
		this.chestY = chestY;
		this.chestZ = chestZ;

		if (this.isValid()) {
			// register new protected chest:
			SKShopkeepersPlugin.getInstance().getProtectedChests().addChest(this.getWorldName(), chestX, chestY, chestZ, this);
		}
	}

	@Override
	public int getChestX() {
		return chestX;
	}

	@Override
	public int getChestY() {
		return chestY;
	}

	@Override
	public int getChestZ() {
		return chestZ;
	}

	@Override
	public void setChest(int chestX, int chestY, int chestZ) {
		this._setChest(chestX, chestY, chestZ);
		this.markDirty();
	}

	@Override
	public Block getChest() {
		return Bukkit.getWorld(this.getWorldName()).getBlockAt(chestX, chestY, chestZ);
	}

	// returns null (and logs a warning) if the price cannot be represented correctly by currency items
	protected TradingRecipe createSellingRecipe(ItemStack itemBeingSold, int price, boolean outOfStock) {
		int remainingPrice = price;

		ItemStack item1 = null;
		ItemStack item2 = null;

		if (Settings.isHighCurrencyEnabled() && price > Settings.highCurrencyMinCost) {
			int highCurrencyAmount = Math.min(price / Settings.highCurrencyValue, Settings.highCurrencyItem.getType().getMaxStackSize());
			if (highCurrencyAmount > 0) {
				remainingPrice -= (highCurrencyAmount * Settings.highCurrencyValue);
				ItemStack highCurrencyItem = Settings.createHighCurrencyItem(highCurrencyAmount);
				item1 = highCurrencyItem; // using the first slot
			}
		}

		if (remainingPrice > 0) {
			if (remainingPrice > Settings.currencyItem.getType().getMaxStackSize()) {
				// cannot represent this price with the used currency items:
				Log.warning("Shopkeeper " + this.getIdString() + " at " + this.getPositionString()
						+ " owned by " + this.getOwnerString() + " has an invalid cost!");
				return null;
			}

			ItemStack currencyItem = Settings.createCurrencyItem(remainingPrice);
			if (item1 == null) {
				item1 = currencyItem;
			} else {
				// the first item of the trading recipe is already used by the high currency item:
				item2 = currencyItem;
			}
		}
		return ShopkeepersAPI.createTradingRecipe(itemBeingSold, item1, item2, outOfStock);
	}

	// returns null (and logs a warning) if the price cannot be represented correctly by currency items
	protected TradingRecipe createBuyingRecipe(ItemStack itemBeingBought, int price, boolean outOfStock) {
		if (price > Settings.currencyItem.getType().getMaxStackSize()) {
			// cannot represent this price with the used currency items:
			Log.warning("Shopkeeper " + this.getIdString() + " at " + this.getPositionString()
					+ " owned by " + this.getOwnerString() + " has an invalid cost!");
			return null;
		}
		ItemStack currencyItem = Settings.createCurrencyItem(price);
		return ShopkeepersAPI.createTradingRecipe(currencyItem, itemBeingBought, null, outOfStock);
	}

	@Override
	public int getCurrencyInChest() {
		Block chest = this.getChest();
		if (!ItemUtils.isChest(chest.getType())) return 0;

		int totalCurrency = 0;
		Inventory chestInventory = ((Chest) chest.getState()).getInventory();
		ItemStack[] chestContents = chestInventory.getContents();
		for (ItemStack itemStack : chestContents) {
			if (Settings.isCurrencyItem(itemStack)) {
				totalCurrency += itemStack.getAmount();
			} else if (Settings.isHighCurrencyItem(itemStack)) {
				totalCurrency += (itemStack.getAmount() * Settings.highCurrencyValue);
			}
		}
		return totalCurrency;
	}

	protected List<ItemCount> getItemsFromChest(Filter<ItemStack> filter) {
		ItemStack[] chestContents = null;
		Block chest = this.getChest();
		if (ItemUtils.isChest(chest.getType())) {
			Inventory chestInventory = ((Chest) chest.getState()).getInventory();
			chestContents = chestInventory.getContents();
		}
		// returns an empty list if the chest couldn't be found:
		return ItemUtils.countItems(chestContents, filter);
	}

	// SHOPKEEPER UIs - shortcuts for common UI types:

	@Override
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUITypes.HIRING(), player);
	}

	@Override
	public boolean openChestWindow(Player player) {
		// make sure the chest still exists
		Block chest = this.getChest();
		if (!ItemUtils.isChest(chest.getType())) {
			Log.debug(() -> "Cannot open chest inventory for player '" + player.getName() + "': The block is no longer a chest!");
			return false;
		}

		Log.debug(() -> "Opening chest inventory for player '" + player.getName() + "'.");
		// open the chest directly as the player (no need for a custom UI)
		Inventory inv = ((Chest) chest.getState()).getInventory();
		player.openInventory(inv);
		return true;
	}

	// TICKING

	@Override
	public void tick() {
		// delete the shopkeeper if the chest is no longer present (eg. if it got removed externally by another plugin,
		// such as WorldEdit, etc.):
		if (Settings.deleteShopkeeperOnBreakChest) {
			remainingCheckChestSeconds--;
			if (remainingCheckChestSeconds <= 0) {
				remainingCheckChestSeconds = CHECK_CHEST_PERIOD_SECONDS;
				// this checks if the block is still a chest:
				Block chestBlock = this.getChest();
				if (!ItemUtils.isChest(chestBlock.getType())) {
					SKShopkeepersPlugin.getInstance().getRemoveShopOnChestBreak().handleBlockBreakage(chestBlock);
				}
			}
		}
	}
}
