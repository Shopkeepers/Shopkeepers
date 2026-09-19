package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.GlowSquid;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class GlowSquidShop extends SKLivingShopObject<GlowSquid> {

	public static final Property<Boolean> DARK = new BasicProperty<Boolean>()
			.dataKeyAccessor("darkGlowSquid", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> darkGlowSquidProperty = new PropertyValue<>(DARK)
			.onValueChanged(Unsafe.initialized(this)::applyDark)
			.build(properties);

	public GlowSquidShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<GlowSquidShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		darkGlowSquidProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		darkGlowSquidProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyDark();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getDarkEditorButton());
		return editorButtons;
	}

	// DARK

	public boolean isDark() {
		return darkGlowSquidProperty.getValue();
	}

	public void setDark(boolean dark) {
		darkGlowSquidProperty.setValue(dark);
	}

	public void cycleDark(boolean backwards) {
		this.setDark(!this.isDark());
	}

	private void applyDark() {
		GlowSquid entity = this.getEntity();
		if (entity == null) return; // Not spawned

		// Integer.MAX_VALUE should be sufficiently long to not require periodic refreshes.
		entity.setDarkTicksRemaining(this.isDark() ? Integer.MAX_VALUE : 0);
	}

	private ItemStack getDarkEditorItem() {
		ItemStack iconItem;
		if (this.isDark()) {
			iconItem = new ItemStack(Material.INK_SAC);
		} else {
			iconItem = new ItemStack(Material.GLOW_INK_SAC);
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonGlowSquidDark,
				Messages.buttonGlowSquidDarkLore
		);
		return iconItem;
	}

	private Button getDarkEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getDarkEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleDark(backwards);
				return true;
			}
		};
	}
}
