package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.logging.Log;

// TODO Replace with the actual mob type once we support 26.2+
public class SulfurCubeShop extends SKLivingShopObject<LivingEntity> {

	// Note: Minecraft actually allows sulfur cubes with sizes up to 256 (internally stored as 0 -
	// 255). However, at these sizes the sulfur cube is not properly rendered anymore, cannot be
	// interacted with, and it becomes laggy.
	// We limit it to 10 since this seems to be a more reasonable limit.
	// Additionally, the maximum size can be further limited in the config.
	public static final int MIN_SIZE = 1;
	public static final int MAX_SIZE = 10;
	public static final Property<Integer> SIZE = new BasicProperty<Integer>()
			.dataKeyAccessor("sulfurCubeSize", NumberSerializers.INTEGER)
			.validator(IntegerValidators.bounded(MIN_SIZE, MAX_SIZE))
			.defaultValue(1)
			.build();

	private final PropertyValue<Integer> sizeProperty = new PropertyValue<>(SIZE)
			.onValueChanged(Unsafe.initialized(this)::applySize)
			.build(properties);

	public SulfurCubeShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<SulfurCubeShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		sizeProperty.load(shopObjectData);

		// Trim the size into valid bounds according to the current configuration:
		this.setSize(this.getSize());
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		sizeProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applySize();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSizeEditorButton());
		return editorButtons;
	}

	// SIZE

	public int getSize() {
		return sizeProperty.getValue();
	}

	public void setSize(int size) {
		int clampedSize = this.clampSize(size);
		if (clampedSize != size) {
			Log.warning(shopkeeper.getLogPrefix() + "Sulfur cube size clamped to valid bounds: "
					+ size + " -> " + clampedSize);
		}
		sizeProperty.setValue(clampedSize);
	}

	private int clampSize(int size) {
		return MathUtils.clamp(size, MIN_SIZE, Settings.sulfurCubeMaxSize);
	}

	public void cycleSize(boolean backwards) {
		int size = this.getSize();
		int nextSize;
		if (backwards) {
			nextSize = size - 1;
		} else {
			nextSize = size + 1;
		}
		nextSize = MathUtils.rangeModulo(nextSize, MIN_SIZE, Settings.sulfurCubeMaxSize);
		this.setSize(nextSize);
	}

	private void applySize() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setSulfurCubeSize(entity, this.getSize());
	}

	private ItemStack getSizeEditorItem() {
		int size = this.getSize();
		ItemStack iconItem = new ItemStack(Material.SLIME_BLOCK);
		String displayName = StringUtils.replaceArguments(Messages.buttonSulfurCubeSize,
				"size", size
		);
		List<String> lore = StringUtils.replaceArguments(Messages.buttonSulfurCubeSizeLore,
				"size", size
		);
		ItemUtils.setDisplayNameAndLore(iconItem, displayName, lore);
		return iconItem;
	}

	private Button getSizeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getSizeEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleSize(backwards);
				return true;
			}
		};
	}
}
