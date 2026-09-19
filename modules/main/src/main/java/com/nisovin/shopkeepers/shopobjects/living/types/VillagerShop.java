package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.types.villager.VillagerEditorItems;
import com.nisovin.shopkeepers.shopobjects.living.types.villager.VillagerSounds;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.ui.trading.TradingViewProvider;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.KeyedSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;

public class VillagerShop extends BabyableShop<Villager> {

	public static final Property<Profession> PROFESSION = new BasicProperty<Profession>()
			.dataKeyAccessor("profession", KeyedSerializers.forRegistry(Profession.class))
			.defaultValue(Profession.NONE)
			.build();

	public static final Property<Villager.Type> VILLAGER_TYPE = new BasicProperty<Villager.Type>()
			.dataKeyAccessor("villagerType", KeyedSerializers.forRegistry(Villager.Type.class))
			.defaultValue(Villager.Type.PLAINS)
			.build();

	public static final int MIN_VILLAGER_LEVEL = 1;
	public static final int MAX_VILLAGER_LEVEL = 5;
	public static final Property<Integer> VILLAGER_LEVEL = new BasicProperty<Integer>()
			.dataKeyAccessor("villagerLevel", NumberSerializers.INTEGER)
			.validator(IntegerValidators.bounded(MIN_VILLAGER_LEVEL, MAX_VILLAGER_LEVEL))
			.defaultValue(1)
			.build();

	private final PropertyValue<Profession> professionProperty = new PropertyValue<>(PROFESSION)
			.onValueChanged(Unsafe.initialized(this)::applyProfession)
			.build(properties);
	private final PropertyValue<Villager.Type> villagerTypeProperty = new PropertyValue<>(VILLAGER_TYPE)
			.onValueChanged(Unsafe.initialized(this)::applyVillagerType)
			.build(properties);
	private final PropertyValue<Integer> villagerLevelProperty = new PropertyValue<>(VILLAGER_LEVEL)
			.onValueChanged(Unsafe.initialized(this)::applyVillagerLevel)
			.build(properties);

	private final VillagerSounds villagerSounds;

	public VillagerShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<VillagerShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
		villagerSounds = new VillagerSounds(Unsafe.initialized(this));
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		professionProperty.load(shopObjectData);
		villagerTypeProperty.load(shopObjectData);
		villagerLevelProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		professionProperty.save(shopObjectData);
		villagerTypeProperty.save(shopObjectData);
		villagerLevelProperty.save(shopObjectData);
	}

	@Override
	public void setup() {
		super.setup();

		if (Settings.simulateVillagerTradingSounds) {
			var viewProvider = shopkeeper.getViewProvider(DefaultUITypes.TRADING());
			if (viewProvider instanceof TradingViewProvider tradingViewProvider) {
				tradingViewProvider.addListener(villagerSounds);
			}
		}
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		// TODO I wasn't able to reproduce this myself yet, but according to some reports villager
		// shopkeepers would sometimes lose their profession. Setting their experience to something
		// above 0 is an attempt to resolve this.
		// Related (but shouldn't apply here since we use NoAI mobs):
		// https://hub.spigotmc.org/jira/browse/SPIGOT-4776
		Villager entity = Unsafe.assertNonNull(this.getEntity());
		entity.setVillagerExperience(1);

		// Disable the vanilla ambient sounds if we simulate the ambient and/or trading sounds
		// ourselves:
		if (Settings.simulateVillagerTradingSounds || Settings.simulateVillagerAmbientSounds) {
			entity.setSilent(true);
		}

		this.applyProfession();
		this.applyVillagerType();
		this.applyVillagerLevel();
	}

	@Override
	public void onTick() {
		super.onTick();

		// Ambient sounds:
		if (Settings.simulateVillagerAmbientSounds) {
			villagerSounds.tick();
		}
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getProfessionEditorButton());
		editorButtons.add(this.getVillagerTypeEditorButton());
		editorButtons.add(this.getVillagerLevelEditorButton());
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
		Villager entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setProfession(this.getProfession());
	}

	private ItemStack getProfessionEditorItem() {
		var iconItem = VillagerEditorItems.getProfessionEditorItem(this.getProfession());
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonVillagerProfession,
				Messages.buttonVillagerProfessionLore
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

	// VILLAGER TYPE

	public Villager.Type getVillagerType() {
		return villagerTypeProperty.getValue();
	}

	public void setVillagerType(Villager.Type villagerType) {
		villagerTypeProperty.setValue(villagerType);
	}

	public void cycleVillagerType(boolean backwards) {
		this.setVillagerType(RegistryUtils.cycleKeyed(
				Villager.Type.class,
				this.getVillagerType(),
				backwards
		));
	}

	private void applyVillagerType() {
		Villager entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVillagerType(this.getVillagerType());
	}

	private ItemStack getVillagerTypeEditorItem() {
		var iconItem = VillagerEditorItems.getVillagerTypeEditorItem(this.getVillagerType());
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonVillagerVariant,
				Messages.buttonVillagerVariantLore
		);
		return iconItem;
	}

	private Button getVillagerTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getVillagerTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleVillagerType(backwards);
				return true;
			}
		};
	}

	// VILLAGER LEVEL

	public int getVillagerLevel() {
		return villagerLevelProperty.getValue();
	}

	public void setVillagerLevel(int villagerLevel) {
		villagerLevelProperty.setValue(villagerLevel);
	}

	public void cycleVillagerLevel(boolean backwards) {
		int villagerLevel = this.getVillagerLevel();
		int nextLevel;
		if (backwards) {
			nextLevel = villagerLevel - 1;
		} else {
			nextLevel = villagerLevel + 1;
		}
		nextLevel = MathUtils.rangeModulo(nextLevel, MIN_VILLAGER_LEVEL, MAX_VILLAGER_LEVEL);
		this.setVillagerLevel(nextLevel);
	}

	private void applyVillagerLevel() {
		Villager entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVillagerLevel(this.getVillagerLevel());
	}

	private ItemStack getVillagerLevelEditorItem() {
		ItemStack iconItem;
		switch (this.getVillagerLevel()) {
		default:
		case 1:
			iconItem = new ItemStack(Material.COBBLESTONE);
			break;
		case 2:
			iconItem = new ItemStack(Material.IRON_INGOT);
			break;
		case 3:
			iconItem = new ItemStack(Material.GOLD_INGOT);
			break;
		case 4:
			iconItem = new ItemStack(Material.EMERALD);
			break;
		case 5:
			iconItem = new ItemStack(Material.DIAMOND);
			break;
		}
		assert iconItem != null;
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonVillagerLevel,
				Messages.buttonVillagerLevelLore
		);
		return iconItem;
	}

	private Button getVillagerLevelEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getVillagerLevelEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleVillagerLevel(backwards);
				return true;
			}
		};
	}
}
