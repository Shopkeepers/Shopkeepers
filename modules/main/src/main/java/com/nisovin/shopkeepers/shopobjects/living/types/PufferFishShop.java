package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.PufferFish;
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
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class PufferFishShop extends SKLivingShopObject<PufferFish> {

	public static final int MIN_PUFF_STATE = 0;
	public static final int MAX_PUFF_STATE = 2;
	public static final Property<Integer> PUFF_STATE = new BasicProperty<Integer>()
			.dataKeyAccessor("puffState", NumberSerializers.INTEGER)
			.validator(IntegerValidators.bounded(MIN_PUFF_STATE, MAX_PUFF_STATE))
			.defaultValue(0)
			.build();

	private final PropertyValue<Integer> puffStateProperty = new PropertyValue<>(PUFF_STATE)
			.onValueChanged(Unsafe.initialized(this)::applyPuffState)
			.build(properties);

	public PufferFishShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<PufferFishShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		puffStateProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		puffStateProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyPuffState();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getPuffStateEditorButton());
		return editorButtons;
	}

	// PUFF STATE

	public int getPuffState() {
		return puffStateProperty.getValue();
	}

	public void setPuffState(int puffState) {
		puffStateProperty.setValue(puffState);
	}

	public void cyclePuffState(boolean backwards) {
		int newPuffState = this.getPuffState() + (backwards ? -1 : 1);
		newPuffState = MathUtils.rangeModulo(newPuffState, MIN_PUFF_STATE, MAX_PUFF_STATE);
		this.setPuffState(newPuffState);
	}

	private void applyPuffState() {
		PufferFish entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setPuffState(this.getPuffState());
	}

	private ItemStack getPuffStateEditorItem() {
		ItemStack iconItem = new ItemStack(Material.PUFFERFISH);
		String puffState;
		switch (this.getPuffState()) {
		case 0:
			puffState = "\u25A1\u25A1";
			break;
		case 1:
			puffState = "\u25A0\u25A1";
			break;
		case 2:
		default:
			puffState = "\u25A0\u25A0";
			break;
		}
		String displayName = StringUtils.replaceArguments(
				Messages.buttonPufferFishPuffState,
				"puffState", puffState
		);
		List<String> lore = StringUtils.replaceArguments(
				Messages.buttonPufferFishPuffStateLore,
				"puffState", puffState
		);
		ItemUtils.setDisplayNameAndLore(iconItem, displayName, lore);
		return iconItem;
	}

	private Button getPuffStateEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getPuffStateEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cyclePuffState(backwards);
				return true;
			}
		};
	}
}
