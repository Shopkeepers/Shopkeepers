package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Sittable;
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
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// Using Babyable as common super type of all sittable mobs for now.
public class SittableShop<E extends Ageable & Sittable> extends BabyableShop<E> {

	public static final Property<Boolean> SITTING = new BasicProperty<Boolean>()
			.dataKeyAccessor("sitting", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> sittingProperty = new PropertyValue<>(SITTING)
			.onValueChanged(Unsafe.initialized(this)::applySitting)
			.build(properties);

	public SittableShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<? extends SittableShop<E>> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		sittingProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		sittingProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applySitting();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSittingEditorButton());
		return editorButtons;
	}

	// SITTING

	public boolean isSitting() {
		return sittingProperty.getValue();
	}

	public void setSitting(boolean sitting) {
		sittingProperty.setValue(sitting);
	}

	public void cycleSitting() {
		this.setSitting(!this.isSitting());
	}

	private void applySitting() {
		Sittable entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setSitting(this.isSitting());
	}

	private ItemStack getSittingEditorItem() {
		ItemStack iconItem = new ItemStack(Material.IRON_HORSE_ARMOR);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonSitting,
				Messages.buttonSittingLore
		);
		return iconItem;
	}

	private Button getSittingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getSittingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleSitting();
				return true;
			}
		};
	}
}
