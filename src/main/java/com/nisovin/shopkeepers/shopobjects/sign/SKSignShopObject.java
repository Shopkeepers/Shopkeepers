package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.BlockFaceUtils;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	protected final SignShops signShops;
	private TreeSpecies signType = TreeSpecies.GENERIC; // default oak
	private boolean wallSign = true;
	private BlockFace signFacing = BlockFace.SOUTH; // not null
	// update the sign content at least once after plugin start, in case some settings have changed which affect the
	// sign content:
	private boolean updateSign = true;
	private long lastFailedRespawnAttempt = 0;

	protected SKSignShopObject(SignShops signShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.signShops = signShops;
		if (creationData != null) {
			BlockFace targetedBlockFace = creationData.getTargetedBlockFace();
			if (targetedBlockFace == BlockFace.UP) {
				this.wallSign = false;
				this.signFacing = BlockFaceUtils.getSignPostFacing(creationData.getSpawnLocation().getYaw());
			} else if (BlockFaceUtils.isWallSignFace(targetedBlockFace)) {
				this.signFacing = targetedBlockFace;
			}
		}
	}

	@Override
	public SKSignShopObjectType getType() {
		return signShops.getSignShopObjectType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		// sign wood type:
		String signTypeName = configSection.getString("signType");
		try {
			signType = TreeSpecies.valueOf(signTypeName);
		} catch (Exception e) {
			// fallback to default:
			Log.warning("Missing or invalid sign type '" + signTypeName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + TreeSpecies.GENERIC + "' now.");
			this.signType = TreeSpecies.GENERIC;
			shopkeeper.markDirty();
		}

		// wall sign vs sign post:
		if (!configSection.isBoolean("wallSign")) {
			// missing value:
			shopkeeper.markDirty();
		}
		wallSign = configSection.getBoolean("wallSign", true);

		// sign facing:
		signFacing = BlockFace.SOUTH; // default
		String signFacingName = configSection.getString("signFacing");
		if (signFacingName == null) {
			Log.warning("Missing sign facing for shopkeeper " + shopkeeper.getId());
			shopkeeper.markDirty();
		} else {
			try {
				signFacing = BlockFace.valueOf(signFacingName);
			} catch (IllegalArgumentException e) {
				Log.warning("Could not parse sign facing for shopkeeper " + shopkeeper.getId() + ": " + signFacingName);
				shopkeeper.markDirty();
			}
			if (wallSign ? !BlockFaceUtils.isWallSignFace(signFacing) : !BlockFaceUtils.isSignPostFacing(signFacing)) {
				Log.warning("Invalid sign facing for shopkeeper " + shopkeeper.getId() + ": " + signFacingName);
				signFacing = BlockFace.SOUTH; // fallback to default
				shopkeeper.markDirty();
			}
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);

		// sign type:
		configSection.set("signType", signType.name());

		// wall sign vs sign post:
		configSection.set("wallSign", wallSign);

		// sign facing:
		configSection.set("signFacing", signFacing.name());
	}

	public boolean isWallSign() {
		return wallSign;
	}

	public BlockFace getSignFacing() {
		return signFacing;
	}

	// ACTIVATION

	@Override
	public Block getBlock() {
		Location signLocation = this.getLocation();
		if (signLocation == null) return null; // world not loaded
		if (!shopkeeper.getChunkCoords().isChunkLoaded()) return null; // chunk not loaded
		Block signBlock = signLocation.getBlock();
		if (!ItemUtils.isSign(signBlock.getType())) return null; // not a sign
		return signBlock;
	}

	public Sign getSign() {
		Block signBlock = this.getBlock();
		if (signBlock == null) return null;
		assert ItemUtils.isSign(signBlock.getType());
		return (Sign) signBlock.getState();
	}

	@Override
	public boolean isActive() {
		return (this.getBlock() != null);
	}

	@Override
	public String getId() {
		return this.getType().createObjectId(this.getBlock());
	}

	@Override
	public boolean needsSpawning() {
		return true; // despawn signs on chunk unload, and spawn them again on chunk load
	}

	@Override
	public boolean spawn() {
		Location signLocation = this.getLocation();
		if (signLocation == null) {
			return false;
		}

		// if re-spawning fails due to the sign dropping for some reason (ex. attached block missing) this could be
		// abused (sign drop farming), therefore we limit the number of spawn attempts:
		if (System.currentTimeMillis() - lastFailedRespawnAttempt < 3 * 60 * 1000L) {
			Log.debug(() -> "Shopkeeper sign at " + shopkeeper.getPositionString() + " is on spawn cooldown.");
			return false;
		}

		// place sign:
		Block signBlock = signLocation.getBlock();
		BlockData signData = this.createBlockData();
		assert signData != null;

		// cancel block physics for this placed sign if needed:
		signShops.cancelNextBlockPhysics(signBlock);
		signBlock.setBlockData(signData, false); // skip physics update
		// cleanup state if no block physics were triggered:
		signShops.cancelNextBlockPhysics(null);

		// in case sign placement has failed for some reason:
		if (!ItemUtils.isSign(signBlock.getType())) {
			lastFailedRespawnAttempt = System.currentTimeMillis();
			return false;
		}

		// init sign content:
		updateSign = false;
		this.updateSign();

		return true;
	}

	private BlockData createBlockData() {
		Material signMaterial = getMaterial(signType, wallSign);
		assert ItemUtils.isSign(signMaterial);
		BlockData signData = null;
		if (wallSign) {
			// wall sign:
			WallSign wallSignData = (WallSign) Bukkit.createBlockData(signMaterial);
			wallSignData.setFacing(signFacing);
			signData = wallSignData;
		} else {
			// sign post:
			org.bukkit.block.data.type.Sign signPostData = (org.bukkit.block.data.type.Sign) Bukkit.createBlockData(signMaterial);
			signPostData.setRotation(signFacing);
			signData = signPostData;
		}
		return signData;
	}

	private static Material getMaterial(TreeSpecies treeSpecies, boolean wallSign) {
		if (treeSpecies == null) {
			treeSpecies = TreeSpecies.GENERIC; // default
		}
		switch (treeSpecies) {
		case ACACIA:
			return wallSign ? Material.ACACIA_WALL_SIGN : Material.ACACIA_SIGN;
		case BIRCH:
			return wallSign ? Material.BIRCH_WALL_SIGN : Material.BIRCH_SIGN;
		case DARK_OAK:
			return wallSign ? Material.DARK_OAK_WALL_SIGN : Material.DARK_OAK_SIGN;
		case JUNGLE:
			return wallSign ? Material.JUNGLE_WALL_SIGN : Material.JUNGLE_SIGN;
		case REDWOOD: // spruce
			return wallSign ? Material.SPRUCE_WALL_SIGN : Material.SPRUCE_SIGN;
		case GENERIC: // oak
		default:
			return wallSign ? Material.OAK_WALL_SIGN : Material.OAK_SIGN;
		}
	}

	@Override
	public void despawn() {
		Block signBlock = this.getBlock();
		if (signBlock != null) {
			assert ItemUtils.isSign(signBlock.getType());
			// remove sign:
			signBlock.setType(Material.AIR, false);
		}
	}

	@Override
	public Location getLocation() {
		return shopkeeper.getLocation();
	}

	public void updateSign() {
		Sign sign = this.getSign();
		if (sign == null) {
			updateSign = true; // request update, once the sign is available again
			return;
		}

		// line 0: header
		sign.setLine(0, Settings.signShopFirstLine);

		// line 1: shop name
		String name = shopkeeper.getName(); // can be empty
		name = this.prepareName(name);
		String line1 = name;
		sign.setLine(1, line1);

		// line 2: owner name
		String line2 = "";
		if (shopkeeper instanceof PlayerShopkeeper) {
			PlayerShopkeeper playerShopkeeper = (PlayerShopkeeper) shopkeeper;
			User owner = playerShopkeeper.getOwner();
			line2 = owner.getName();
		}
		sign.setLine(2, line2);

		// line 3: empty
		sign.setLine(3, "");

		// apply sign changes:
		sign.update(false, false);
	}

	@Override
	public boolean check() {
		if (!shopkeeper.getChunkCoords().isChunkLoaded()) {
			// only verify sign, if the chunk is currently loaded:
			return false;
		}

		Block signBlock = this.getBlock();
		if (signBlock == null) {
			Log.debug(() -> "Shopkeeper sign at " + shopkeeper.getPositionString() + " is no longer existing! Attempting respawn now.");
			if (!this.spawn()) {
				Log.warning("Shopkeeper sign at " + shopkeeper.getPositionString() + " could not be spawned!");
			}
			return true;
		}

		// update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}

		return false;
	}

	@Override
	public void onShopkeeperOwnerChanged() {
		// Update the sign:
		this.updateSign();
	}

	// NAMING

	@Override
	public void setName(String name) {
		// the name gets set during sign update, which always uses the name of the shopkeeper:
		// TODO allow changing only the name? Currently this restriction allows to not have to store
		// custom names inside this sign shop object
		this.updateSign();
	}

	@Override
	public String getName() {
		Sign sign = this.getSign();
		if (sign == null) return null;
		return sign.getLine(1);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<EditorHandler.Button>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getSignTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleSignType(backwards);
				return true;
			}
		});
		return editorButtons;
	}

	// SIGN TYPE

	public void setSignType(TreeSpecies signType) {
		Validate.notNull(signType, "Sign type is null!");
		this.signType = signType;
		shopkeeper.markDirty();
		this.applySignType();
	}

	protected void applySignType() {
		Sign sign = this.getSign();
		if (sign != null) {
			BlockData signData = this.createBlockData();
			sign.setBlockData(signData); // keeps sign data (text) the same
			sign.update(true, false); // force: material has changed, skip physics update
		}
	}

	public void cycleSignType(boolean backwards) {
		this.setSignType(EnumUtils.cycleEnumConstant(TreeSpecies.class, signType, backwards));
	}

	protected ItemStack getSignTypeEditorItem() {
		ItemStack iconItem = new ItemStack(getMaterial(signType, false));
		return ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonSignVariant, Settings.msgButtonSignVariantLore);
	}
}
