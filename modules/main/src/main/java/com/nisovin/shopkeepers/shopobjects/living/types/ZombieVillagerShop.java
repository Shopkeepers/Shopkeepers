package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.ZombieVillager;
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
import com.nisovin.shopkeepers.shopobjects.living.types.villager.VillagerEditorItems;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.KeyedSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ZombieVillagerShop extends ZombieShop<ZombieVillager> {

	public static final Property<Profession> PROFESSION = new BasicProperty<Profession>()
			.dataKeyAccessor("profession", KeyedSerializers.forRegistry(Profession.class))
			.defaultValue(Profession.NONE)
			.build();

	private final PropertyValue<Profession> professionProperty = new PropertyValue<>(PROFESSION)
			.onValueChanged(Unsafe.initialized(this)::applyProfession)
			.build(properties);

	public ZombieVillagerShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<ZombieVillagerShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		professionProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		professionProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyProfession();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getProfessionEditorButton());
		return editorButtons;
	}

	// PROFESSION

	public Profession getProfession() {
		return professionProperty.getValue();
	}

	public void setProfession(Profession profession) {
		professionProperty.setValue(profession);
	}

	public void cycleProfession(boolean backwards) {
		this.setProfession(RegistryUtils.cycleKeyed(
				Villager.Profession.class,
				this.getProfession(),
				backwards
		));
	}

	private void applyProfession() {
		ZombieVillager entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVillagerProfession(this.getProfession());
	}

	private ItemStack getProfessionEditorItem() {
		var iconItem = VillagerEditorItems.getProfessionEditorItem(this.getProfession());
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonZombieVillagerProfession,
				Messages.buttonZombieVillagerProfessionLore
		);
		return iconItem;
	}

	private Button getProfessionEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getProfessionEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleProfession(backwards);
				return true;
			}
		};
	}
}
