package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.sign.HangingSignShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShopObject;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShops;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.SignUtils;
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

public class SKHangingSignShopObject extends BaseBlockShopObject implements HangingSignShopObject {

	private static final String DATA_KEY_SIGN_TYPE = "signType";
	public static final Property<SignType> SIGN_TYPE = new BasicProperty<SignType>()
			.dataKeyAccessor(DATA_KEY_SIGN_TYPE, EnumSerializers.lenient(SignType.class))
			.validator(value -> Validate.isTrue(value.isHangingSupported(),
					() -> "Unsupported hanging sign type: '" + value.name() + "'."))
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

	protected SKHangingSignShopObject(
			BaseBlockShops blockShops,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(blockShops, shopkeeper, creationData);

		if (creationData != null) {
			BlockFace targetedBlockFace = creationData.getTargetedBlockFace();
			if (targetedBlockFace == BlockFace.DOWN) {
				// Hanging sign:
				wallSignProperty.setValue(false, Collections.emptySet()); // Not marking dirty
			} // Else: Wall hanging sign (default).
		}
	}

	@Override
	public SKHangingSignShopObjectType getType() {
		return SKDefaultShopObjectTypes.HANGING_SIGN();
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
	protected boolean isValidBlockType(Material type) {
		return ItemUtils.isHangingSign(type);
	}

	// HangingSign extends Sign.
	public @Nullable Sign getSign() {
		if (!this.isActive()) return null;
		Block block = Unsafe.assertNonNull(this.getBlock());
		assert this.isValidBlockType(block.getType());
		return (Sign) block.getState();
	}

	@Override
	protected @Nullable BlockData createBlockData() {
		SignType signType = this.getSignType();
		if (!signType.isHangingSupported()) return null;

		boolean wallSign = this.isWallSign();
		Material blockMaterial = Unsafe.assertNonNull(signType.getHangingSignMaterial(wallSign));
		assert this.isValidBlockType(blockMaterial);
		BlockData blockData;
		if (wallSign) {
			// Wall hanging sign:
			// TODO Use the actual type once we only support MC 1.20 and above.
			Directional wallHangingSignData = (Directional) Bukkit.createBlockData(blockMaterial);
			wallHangingSignData.setFacing(this.getSignFacing());
			blockData = wallHangingSignData;
		} else {
			// Hanging sign:
			// TODO Use the actual type once we only support MC 1.20 and above.
			Rotatable hangingSignData = (Rotatable) Bukkit.createBlockData(blockMaterial);
			hangingSignData.setRotation(this.getSignFacing());

			// We always set the 'attached' flag for now, which is usually only used if the block
			// above has a non-full bottom collision face:
			// TODO Set the 'attached' flag dynamically based on the block above?
			Attachable attachable = (Attachable) hangingSignData;
			attachable.setAttached(true);

			blockData = hangingSignData;
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

		// Location below the hanging sign:
		return location.add(0.5D, -0.3D, 0.5D);
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

	// WALL SIGN

	// Can be edited by moving the shopkeeper.
	@Override
	public void setAttachedBlockFace(BlockFace attachedBlockFace) {
		super.setAttachedBlockFace(attachedBlockFace);
		Validate.isTrue(attachedBlockFace != BlockFace.UP, "Invalid hanging sign block face: UP.");
		if (attachedBlockFace == BlockFace.DOWN) {
			wallSignProperty.setValue(false);
		} else {
			// Update the yaw accordingly:
			shopkeeper.setYaw(BlockFaceUtils.getYaw(attachedBlockFace));
			wallSignProperty.setValue(true);
		}
	}

	@Override
	public @Nullable BlockFace getAttachedBlockFace() {
		if (this.isWallSign()) {
			return BlockFaceUtils.getWallSignFacings().fromYaw(shopkeeper.getYaw());
		} else {
			return BlockFace.DOWN;
		}
	}

	public boolean isWallSign() {
		return wallSignProperty.getValue();
	}

	// SIGN FACING

	public BlockFace getSignFacing() {
		if (this.isWallSign()) {
			// The wall hanging sign facing is the attached block face rotated by 90 degree left:
			return BlockFaceUtils.getWallSignFacings().fromYaw(shopkeeper.getYaw() - 90);
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
		BlockData blockData = this.createBlockData();
		if (blockData == null) return;

		sign.setBlockData(blockData); // Keeps sign data (e.g. text) the same
		sign.update(true, false); // Force: Material has changed, skip physics update.
	}

	public void cycleSignType(boolean backwards) {
		this.setSignType(
				EnumUtils.cycleEnumConstant(
						SignType.class,
						this.getSignType(),
						backwards,
						SignType.IS_HANGING_SUPPORTED
				)
		);
	}

	private ItemStack getSignTypeEditorItem() {
		Material signMaterial = Material.OAK_SIGN; // Default
		SignType signType = this.getSignType();
		if (signType.isHangingSupported()) {
			signMaterial = Unsafe.assertNonNull(signType.getHangingSignMaterial());
		}
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
