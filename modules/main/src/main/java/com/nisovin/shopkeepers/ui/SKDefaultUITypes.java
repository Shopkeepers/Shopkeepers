package com.nisovin.shopkeepers.ui;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIType;
import com.nisovin.shopkeepers.ui.containers.ShopContainersEditorUIType;
import com.nisovin.shopkeepers.ui.editor.EditorUIType;
import com.nisovin.shopkeepers.ui.equipmentEditor.EquipmentEditorUIType;
import com.nisovin.shopkeepers.ui.hiring.HiringUIType;
import com.nisovin.shopkeepers.ui.lib.AbstractUIType;
import com.nisovin.shopkeepers.ui.members.ShopMembersEditorUIType;
import com.nisovin.shopkeepers.ui.trading.TradingUIType;
import com.nisovin.shopkeepers.ui.villager.editor.VillagerEditorUIType;
import com.nisovin.shopkeepers.ui.villager.equipmentEditor.VillagerEquipmentEditorUIType;

public final class SKDefaultUITypes implements DefaultUITypes {

	private final EditorUIType editorUIType = EditorUIType.INSTANCE;
	private final EquipmentEditorUIType equipmentEditorUIType = EquipmentEditorUIType.INSTANCE;
	private final ShopMembersEditorUIType shopMembersEditorUIType = ShopMembersEditorUIType.INSTANCE;
	private final ShopContainersEditorUIType shopContainersEditorUIType = ShopContainersEditorUIType.INSTANCE;
	private final TradingUIType tradingUIType = TradingUIType.INSTANCE;
	private final HiringUIType hiringUIType = HiringUIType.INSTANCE;
	private final VillagerEditorUIType villagerEditorUIType = VillagerEditorUIType.INSTANCE;
	private final VillagerEquipmentEditorUIType villagerEquipmentEditorUIType = VillagerEquipmentEditorUIType.INSTANCE;
	private final ConfirmationUIType confirmationUIType = ConfirmationUIType.INSTANCE;

	public SKDefaultUITypes() {
	}

	@Override
	public List<? extends AbstractUIType> getAllUITypes() {
		List<AbstractUIType> defaults = new ArrayList<>();
		defaults.add(editorUIType);
		defaults.add(equipmentEditorUIType);
		defaults.add(shopMembersEditorUIType);
		defaults.add(shopContainersEditorUIType);
		defaults.add(tradingUIType);
		defaults.add(hiringUIType);
		defaults.add(villagerEditorUIType);
		defaults.add(villagerEquipmentEditorUIType);
		defaults.add(confirmationUIType);
		return defaults;
	}

	@Override
	public EditorUIType getEditorUIType() {
		return editorUIType;
	}

	@Override
	public EquipmentEditorUIType getEquipmentEditorUIType() {
		return equipmentEditorUIType;
	}

	@Override
	public ShopMembersEditorUIType getShopMembersEditorUIType() {
		return shopMembersEditorUIType;
	}

	@Override
	public ShopContainersEditorUIType getShopContainersEditorUIType() {
		return shopContainersEditorUIType;
	}

	@Override
	public TradingUIType getTradingUIType() {
		return tradingUIType;
	}

	@Override
	public HiringUIType getHiringUIType() {
		return hiringUIType;
	}

	// Internal.
	public VillagerEditorUIType getVillagerEditorUIType() {
		return villagerEditorUIType;
	}

	// Internal.
	public VillagerEquipmentEditorUIType getVillagerEquipmentEditorUIType() {
		return villagerEquipmentEditorUIType;
	}

	// Internal.
	public ConfirmationUIType getConfirmationUIType() {
		return confirmationUIType;
	}

	// STATICS (for convenience):

	public static SKDefaultUITypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultUITypes();
	}

	public static EditorUIType EDITOR() {
		return getInstance().getEditorUIType();
	}

	public static EquipmentEditorUIType EQUIPMENT_EDITOR() {
		return getInstance().getEquipmentEditorUIType();
	}

	public static ShopMembersEditorUIType SHOP_MEMBERS_EDITOR() {
		return getInstance().getShopMembersEditorUIType();
	}

	public static ShopContainersEditorUIType SHOP_CONTAINERS_EDITOR() {
		return getInstance().getShopContainersEditorUIType();
	}

	public static TradingUIType TRADING() {
		return getInstance().getTradingUIType();
	}

	public static HiringUIType HIRING() {
		return getInstance().getHiringUIType();
	}

	public static VillagerEditorUIType VILLAGER_EDITOR() {
		return getInstance().getVillagerEditorUIType();
	}

	public static VillagerEquipmentEditorUIType VILLAGER_EQUIPMENT_EDITOR() {
		return getInstance().getVillagerEquipmentEditorUIType();
	}

	public static ConfirmationUIType CONFIRMATION() {
		return getInstance().getConfirmationUIType();
	}
}
