package com.nisovin.shopkeepers.shopobjects.endcrystal;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.endcrystal.EndCrystalShopObject;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObject;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// Note: Disabled in the end by default, because there might be unexpected effects. For example,
// they set blocks on fire (disabled via an event handler) and heal nearby ender dragons.
public class SKEndCrystalShop extends BaseEntityShopObject<EnderCrystal>
		implements EndCrystalShopObject {

	public static final Property<Boolean> SHOW_BOTTOM = new BasicProperty<Boolean>()
			.dataKeyAccessor("showBottom", BooleanSerializers.STRICT)
			.useDefaultIfMissing()
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> showBottomProperty = new PropertyValue<>(SHOW_BOTTOM)
			.onValueChanged(Unsafe.initialized(this)::applyShowBottom)
			.build(properties);

	public SKEndCrystalShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<SKEndCrystalShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		showBottomProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		showBottomProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyShowBottom();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getShowBottomEditorButton());
		return editorButtons;
	}

	// SHOW BOTTOM

	public boolean isShowBottom() {
		return showBottomProperty.getValue();
	}

	public void setShowBottom(boolean showBottom) {
		showBottomProperty.setValue(showBottom);
	}

	public void cycleShowBottom(boolean backwards) {
		this.setShowBottom(!this.isShowBottom());
	}

	private void applyShowBottom() {
		EnderCrystal entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setShowingBottom(this.isShowBottom());
	}

	private ItemStack getShowBottomEditorItem() {
		ItemStack iconItem = new ItemStack(this.isShowBottom()
				? Material.HEAVY_WEIGHTED_PRESSURE_PLATE
				: Material.STONE_PRESSURE_PLATE
		);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonEndCrystalBottomSlab,
				Messages.buttonEndCrystalBottomSlabLore
		);
		return iconItem;
	}

	private Button getShowBottomEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getShowBottomEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleShowBottom(backwards);
				return true;
			}
		};
	}
}
