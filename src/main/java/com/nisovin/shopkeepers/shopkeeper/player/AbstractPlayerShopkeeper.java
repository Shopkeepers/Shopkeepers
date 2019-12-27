package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.citizens.SKCitizensShopObject;
import com.nisovin.shopkeepers.shopobjects.sign.SKSignShopObject;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractPlayerShopkeeper extends AbstractShopkeeper implements PlayerShopkeeper {

	protected UUID ownerUUID; // not null after successful initialization
	protected String ownerName; // not null after successful initialization
	// TODO store chest world separately? currently it uses the shopkeeper world
	// this would allow the chest and shopkeeper to be located in different worlds, and virtual player shops
	protected int chestX;
	protected int chestY;
	protected int chestZ;
	protected ItemStack hireCost = null; // null if not for hire

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
		Player owner = playerShopCreationData.getCreator();
		Block chest = playerShopCreationData.getShopChest();
		assert owner != null;
		assert chest != null;

		this.ownerUUID = owner.getUniqueId();
		this.ownerName = owner.getName();
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
		try {
			ownerUUID = UUID.fromString(configSection.getString("owner uuid"));
		} catch (Exception e) {
			// uuid invalid or non-existent:
			throw new ShopkeeperCreateException("Missing owner uuid!");
		}
		ownerName = configSection.getString("owner");
		// TODO no longer using fallback name (since late 1.14.4); remove the "unknown"-check again in the future
		// (as soon as possible, because it conflicts with any player actually named 'unknown')
		if (ownerName == null || ownerName.isEmpty() || ownerName.equals("unknown")) {
			throw new ShopkeeperCreateException("Missing owner name!");
		}

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
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("owner uuid", ownerUUID.toString());
		configSection.set("owner", ownerName);
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

		// cache owner identification information:
		SKShopkeepersPlugin.getInstance().getPlayerIdCache().addPlayerId(ownerUUID, ownerName, false);

		// register protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().addChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		super.onRemoval(cause);

		// unregister previously protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().removeChest(this.getWorldName(), chestX, chestY, chestZ, this);

		// cleanup cached owner identification information:
		SKShopkeepersPlugin.getInstance().getPlayerIdCache().removePlayerId(ownerUUID, ownerName);
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
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), player.getName());
	}

	@Override
	public void setOwner(UUID ownerUUID, String ownerName) {
		Validate.notNull(ownerUUID, "Owner uuid is null!");
		Validate.notEmpty(ownerName, "Owner name is empty!");
		this.markDirty();

		if (this.isValid()) {
			// cleanup cached owner identification information:
			SKShopkeepersPlugin.getInstance().getPlayerIdCache().removePlayerId(this.ownerUUID, this.ownerName);

			// cache new owner's identification information:
			SKShopkeepersPlugin.getInstance().getPlayerIdCache().addPlayerId(ownerUUID, ownerName, false);
		}

		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		// TODO do this in a more abstract way
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
			// update the npc's name:
			((SKCitizensShopObject) this.getShopObject()).setName(ownerName);
		} else if (this.getShopObject().getType() == DefaultShopObjectTypes.SIGN()) {
			// update sign:
			((SKSignShopObject) this.getShopObject()).updateSign();
		}
	}

	@Override
	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	@Override
	public String getOwnerName() {
		return ownerName;
	}

	@Override
	public String getOwnerString() {
		return TextUtils.getPlayerString(ownerName, ownerUUID);
	}

	@Override
	public boolean isOwner(Player player) {
		return player.getUniqueId().equals(ownerUUID);
	}

	@Override
	public Player getOwner() {
		return Bukkit.getPlayer(ownerUUID);
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
}
