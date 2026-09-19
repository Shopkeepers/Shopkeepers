package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Golem;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.compat.MC_1_21_9;
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
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;

// TODO Use the actual entity type once we only support 1.21.9+
public class CopperGolemShop extends SKLivingShopObject<Golem> {

	// TODO Use the actual enum type once we only support 1.21.9+
	public static final Property<String> WEATHER_STATE = new BasicProperty<String>()
			.dataKeyAccessor("weatherState", StringSerializers.STRICT_NON_EMPTY)
			.useDefaultIfMissing()
			.defaultValue("UNAFFECTED")
			.build();

	private final PropertyValue<String> weatherStateProperty = new PropertyValue<>(WEATHER_STATE)
			.onValueChanged(Unsafe.initialized(this)::applyWeatherState)
			.build(properties);

	public CopperGolemShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<CopperGolemShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		weatherStateProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		weatherStateProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyWeatherState();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getWeatherStateEditorButton());
		return editorButtons;
	}

	// WEATHER STATE

	public String getWeatherState() {
		return weatherStateProperty.getValue();
	}

	public void setWeatherState(String weatherState) {
		weatherStateProperty.setValue(weatherState);
	}

	public void cycleWeatherState(boolean backwards) {
		this.setWeatherState(CollectionUtils.cycleValue(
				MC_1_21_9.COPPER_GOLEM_WEATHER_STATES,
				this.getWeatherState(),
				backwards
		));
	}

	private void applyWeatherState() {
		Golem entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setCopperGolemWeatherState(entity, this.getWeatherState());

		// Disabled weathering state changes (waxed):
		Compat.getProvider().setCopperGolemNextWeatheringTick(entity, -2);
	}

	private ItemStack getWeatherStateEditorItem() {
		String weatherState = this.getWeatherState();
		ItemStack iconItem;
		switch (weatherState) {
		case "EXPOSED":
			iconItem = new ItemStack(Material.EXPOSED_COPPER);
			break;
		case "WEATHERED":
			iconItem = new ItemStack(Material.WEATHERED_COPPER);
			break;
		case "OXIDIZED":
			iconItem = new ItemStack(Material.OXIDIZED_COPPER);
			break;
		case "UNAFFECTED":
		default:
			iconItem = new ItemStack(Material.COPPER_BLOCK);
			break;
		}
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonCopperGolemWeatherState,
				Messages.buttonCopperGolemWeatherStateLore
		);
		return iconItem;
	}

	private Button getWeatherStateEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getWeatherStateEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleWeatherState(backwards);
				return true;
			}
		};
	}
}
