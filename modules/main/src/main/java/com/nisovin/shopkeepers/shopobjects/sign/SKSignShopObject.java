package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShopObject;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShops;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.SignUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKSignShopObject extends BaseBlockShopObject implements SignShopObject {

	private static final String DATA_KEY_SIGN_TYPE = "signType";
	public static final Property<SignType> SIGN_TYPE = new BasicProperty<SignType>()
			.dataKeyAccessor(DATA_KEY_SIGN_TYPE, EnumSerializers.lenient(SignType.class))
			.validator(value -> {
				Validate.isTrue(value.isSupported(),
						() -> "Unsupported sign type: '" + value.name() + "'.");
			})
			.defaultValue(SignType.OAK)
			.build();

	public static final Property<Boolean> WALL_SIGN = new BasicProperty<Boolean>()
			.dataKeyAccessor("wallSign", BooleanSerializers.LENIENT)
			.defaultValue(true)
			.build();

	public static final Property<Boolean> GLOWING_TEXT = new BasicProperty<Boolean>()
			.dataKeyAccessor("glowingText", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	static {
		// Register shopkeeper data migrations:

		// Migration from TreeSpecies to SignType.
		// TODO Remove this again at some point. Added in v2.10.0.
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"sign-type",
				MigrationPhase.ofShopObjectClass(SKSignShopObject.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				boolean migrated = false;
				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				String signTypeName = shopObjectData.getString(DATA_KEY_SIGN_TYPE);
				if ("GENERIC".equals(signTypeName)) {
					Log.warning(logPrefix + "Migrating sign type from '" + signTypeName + "' to '"
							+ SignType.OAK + "'.");
					shopObjectData.set(SIGN_TYPE, SignType.OAK);
					migrated = true;
				} else if ("REDWOOD".equals(signTypeName)) {
					Log.warning(logPrefix + "Migrating sign type from '" + signTypeName + "' to '"
							+ SignType.SPRUCE + "'.");
					shopObjectData.set(SIGN_TYPE, SignType.SPRUCE);
					migrated = true;
				}
				return migrated;
			}
		});

		// Migration from sign facing to shopkeeper yaw (pre v2.13.4):
		// TODO Remove this migration again at some point.
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"sign-facing-to-yaw",
				MigrationPhase.ofShopObjectClass(SKSignShopObject.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				boolean migrated = false;
				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				String signFacingName = shopObjectData.getString("signFacing");
				if (signFacingName != null) {
					BlockFace signFacing = BlockFace.SOUTH;
					try {
						signFacing = BlockFace.valueOf(signFacingName);
					} catch (IllegalArgumentException e) {
						Log.warning(logPrefix + "Could not parse sign facing '" + signFacingName
								+ "'. Falling back to SOUTH.");
					}

					// Validate the sign facing:
					if (!this.isValidSignFacing(shopObjectData, signFacing)) {
						Log.warning(logPrefix + "Invalid sign facing '" + signFacingName
								+ "'. Falling back to SOUTH.");
						signFacing = BlockFace.SOUTH;
					}

					float yaw = BlockFaceUtils.getYaw(signFacing);
					Log.warning(logPrefix + "Migrating sign facing '" + signFacing + "' to yaw "
							+ TextUtils.format(yaw));
					shopkeeperData.set(AbstractShopkeeper.YAW, yaw);
					migrated = true;
				}
				return migrated;
			}

			private boolean isValidSignFacing(
					ShopObjectData shopObjectData,
					BlockFace signFacing
			) throws InvalidDataException {
				Boolean wallSign = shopObjectData.getOrNullIfMissing(WALL_SIGN); // Can be null
				if (wallSign == null) return true; // Skip the validation
				if (wallSign) {
					return BlockFaceUtils.isWallSignFacing(signFacing);
				} else {
					return BlockFaceUtils.isSignPostFacing(signFacing);
				}
			}
		});
	}

	private final PropertyValue<SignType> signTypeProperty = new PropertyValue<>(SIGN_TYPE)
			.onValueChanged(Unsafe.initialized(this)::applySignType)
			.build(properties);
	private final PropertyValue<Boolean> wallSignProperty = new PropertyValue<>(WALL_SIGN)
			.onValueChanged(() -> Unsafe.initialized(this).respawn().exceptionally(e -> {
				// optional logging/handling
				return false;
			}))
			.build(properties);
	private final PropertyValue<Boolean> glowingTextProperty = new PropertyValue<>(GLOWING_TEXT)
			.onValueChanged(Unsafe.initialized(this)::applyGlowingText)
			.build(properties);

	protected SKSignShopObject(
			BaseBlockShops blockShops,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(blockShops, shopkeeper, creationData);

		if (creationData != null) {
			BlockFace targetedBlockFace = creationData.getTargetedBlockFace();
			if (targetedBlockFace == BlockFace.UP) {
				// Sign post:
				wallSignProperty.setValue(false, Collections.emptySet()); // Not marking dirty
			} // Else: Wall sign (default).
		}
	}

	@Override
	public SKSignShopObjectType getType() {
		return SKDefaultShopObjectTypes.SIGN();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		signTypeProperty.load(shopObjectData);
		wallSignProperty.load(shopObjectData);
		glowingTextProperty.load(shopObjectData);

		// Disable glowing text based on the current configuration:
		this.setGlowingText(this.isGlowingText());
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		signTypeProperty.save(shopObjectData);
		wallSignProperty.save(shopObjectData);
		glowingTextProperty.save(shopObjectData);
		// Note: The sign facing is not saved, but instead derived from the shopkeeper's yaw.
	}

	// ACTIVATION

	@Override
	protected boolean isValidBlockType(Material blockType) {
		return ItemUtils.isSign(blockType);
	}

	public @Nullable Sign getSign() {
		if (!this.isActive()) return null;
		Block block = Unsafe.assertNonNull(this.getBlock());
		assert this.isValidBlockType(block.getType());
		return (Sign) block.getState();
	}

	@Override
	protected @Nullable BlockData createBlockData() {
		SignType signType = this.getSignType();
		assert signType.isSupported();
		boolean wallSign = this.isWallSign();
		Material blockMaterial = Unsafe.assertNonNull(signType.getSignMaterial(wallSign));
		assert this.isValidBlockType(blockMaterial);
		BlockData blockData;
		if (wallSign) {
			// Wall sign:
			WallSign wallSignData = (WallSign) Bukkit.createBlockData(blockMaterial);
			wallSignData.setFacing(this.getSignFacing());
			blockData = wallSignData;
		} else {
			// Sign post:
			org.bukkit.block.data.type.Sign signPostData = Unsafe.castNonNull(
					Bukkit.createBlockData(blockMaterial)
			);
			signPostData.setRotation(this.getSignFacing());
			blockData = signPostData;
		}
		return blockData;
	}

	@Override
	protected void updateBlock() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		// Common sign setup (e.g. sign content):
		SignShops.updateShopSign(sign, shopkeeper);

		// Glowing text:
		this.applyGlowingText(sign);

		// Apply sign changes:
		sign.update(false, false);
	}

	// TICKING

	@Override
	public @Nullable Location getTickVisualizationParticleLocation() {
		Location location = this.getLocation();
		if (location == null) return null;
		if (this.isWallSign()) {
			// Location at the block center:
			return location.add(0.5D, 0.5D, 0.5D);
		} else {
			// Location above the sign post:
			return location.add(0.5D, 1.3D, 0.5D);
		}
	}

	// EDITOR ACTIONS

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSignTypeEditorButton());
		if (Settings.enableGlowingSignText) {
			editorButtons.add(this.getGlowingTextEditorButton());
		}
		return editorButtons;
	}

	// WALL SIGN (vs sign post)

	// Can be edited by moving the shopkeeper.
	@Override
	public void setAttachedBlockFace(BlockFace attachedBlockFace) {
		super.setAttachedBlockFace(attachedBlockFace);
		Validate.isTrue(attachedBlockFace != BlockFace.DOWN, "Invalid sign block face: DOWN.");
		if (attachedBlockFace == BlockFace.UP) {
			wallSignProperty.setValue(false);
		} else {
			// Update the yaw accordingly:
			shopkeeper.setYaw(BlockFaceUtils.getYaw(attachedBlockFace));
			wallSignProperty.setValue(true);
		}
	}

	@Override
	public @Nullable BlockFace getAttachedBlockFace() {
		return this.isWallSign() ? this.getSignFacing() : BlockFace.UP;
	}

	public boolean isWallSign() {
		return wallSignProperty.getValue();
	}

	// SIGN FACING

	public BlockFace getSignFacing() {
		if (this.isWallSign()) {
			return BlockFaceUtils.getWallSignFacings().fromYaw(shopkeeper.getYaw());
		} else {
			return BlockFaceUtils.getSignPostFacings().fromYaw(shopkeeper.getYaw());
		}
	}

	// SIGN TYPE

	public SignType getSignType() {
		return signTypeProperty.getValue();
	}

	public void setSignType(SignType signType) {
		signTypeProperty.setValue(signType);
	}

	protected void applySignType() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		// Note: The different sign types are different materials. We need to capture the sign state
		// (e.g. sign contents), because they would otherwise be removed when changing the block's
		// type.
		BlockData blockData = Unsafe.assertNonNull(this.createBlockData());
		sign.setBlockData(blockData); // Keeps sign data (e.g. text) the same
		sign.update(true, false); // Force: Material has changed, skip physics update.
	}

	public void cycleSignType(boolean backwards) {
		this.setSignType(
				EnumUtils.cycleEnumConstant(
						SignType.class,
						this.getSignType(),
						backwards,
						SignType.IS_SUPPORTED
				)
		);
	}

	private ItemStack getSignTypeEditorItem() {
		Material signMaterial = Unsafe.assertNonNull(this.getSignType().getSignMaterial());
		ItemStack iconItem = new ItemStack(signMaterial);
		return ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonSignVariant,
				Messages.buttonSignVariantLore
		);
	}

	private Button getSignTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getSignTypeEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleSignType(backwards);
				return true;
			}
		};
	}

	// GLOWING TEXT

	public boolean isGlowingText() {
		return glowingTextProperty.getValue();
	}

	public void setGlowingText(boolean glowing) {
		if (glowing && !Settings.enableGlowingSignText) {
			Log.warning(shopkeeper.getLogPrefix() + "Disabling glowing sign text.");
			glowing = false;
		}
		glowingTextProperty.setValue(glowing);
	}

	protected void applyGlowingText() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		this.applyGlowingText(sign);

		// Sign block type is still the same (no force required), and we want to skip physics:
		sign.update(false, false);
	}

	private void applyGlowingText(Sign sign) {
		// Apply the glowing text to both sign sides:
		SignUtils.setBothSidesGlowingText(sign, this.isGlowingText());
	}

	public void cycleGlowingText(boolean backwards) {
		this.setGlowingText(!this.isGlowingText());
	}

	private ItemStack getGlowingTextEditorItem() {
		ItemStack iconItem;
		if (this.isGlowingText()) {
			iconItem = new ItemStack(Material.GLOW_INK_SAC);
		} else {
			iconItem = new ItemStack(Material.INK_SAC);
		}
		return ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonSignGlowingText,
				Messages.buttonSignGlowingTextLore
		);
	}

	private Button getGlowingTextEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getGlowingTextEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleGlowingText(backwards);
				return true;
			}
		};
	}
}
