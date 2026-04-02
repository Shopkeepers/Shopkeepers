package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShops;
import com.nisovin.shopkeepers.shopobjects.citizens.SKCitizensShopObjectType;
import com.nisovin.shopkeepers.shopobjects.fancynpcs.SKFancyNpcShopObjectType;
import com.nisovin.shopkeepers.shopobjects.endcrystal.SKEndCrystalShopObjectType;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.sign.SKHangingSignShopObjectType;
import com.nisovin.shopkeepers.shopobjects.sign.SKSignShopObjectType;

public final class SKDefaultShopObjectTypes implements DefaultShopObjectTypes {

	// TODO Maybe change object type permissions to 'shopkeeper.object.<type>'?

	private final SKShopkeepersPlugin plugin;

	private final SKEndCrystalShopObjectType endCrystalShopObjectType;
	private final SKSignShopObjectType signShopObjectType;
	private final SKHangingSignShopObjectType hangingSignShopObjectType;

	public SKDefaultShopObjectTypes(
			SKShopkeepersPlugin plugin,
			BaseBlockShops blockShops,
			BaseEntityShops entityShops
	) {
		this.plugin = plugin;
		this.endCrystalShopObjectType = new SKEndCrystalShopObjectType(entityShops);
		this.signShopObjectType = new SKSignShopObjectType(blockShops);
		this.hangingSignShopObjectType = new SKHangingSignShopObjectType(blockShops);
	}

	public void onRegisterDefaults() {
		this.getLivingShopObjectTypes().onRegisterDefaults();
		endCrystalShopObjectType.registerPermission();
	}

	@Override
	public List<? extends AbstractShopObjectType<?>> getAll() {
		List<AbstractShopObjectType<?>> shopObjectTypes = new ArrayList<>();
		shopObjectTypes.addAll(this.getLivingShopObjectTypes().getAll());
		shopObjectTypes.add(this.getEndCrystalShopObjectType());
		shopObjectTypes.add(this.getSignShopObjectType());
		shopObjectTypes.add(this.getHangingSignShopObjectType());
		shopObjectTypes.add(this.getCitizensShopObjectType());
		shopObjectTypes.add(this.getFancyNpcShopObjectType());
		return shopObjectTypes;
	}

	@Override
	public SKLivingShopObjectTypes getLivingShopObjectTypes() {
		return plugin.getLivingShops().getLivingShopObjectTypes();
	}

	@Override
	public SKEndCrystalShopObjectType getEndCrystalShopObjectType() {
		return endCrystalShopObjectType;
	}

	@Override
	public SKSignShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	@Override
	public SKHangingSignShopObjectType getHangingSignShopObjectType() {
		return hangingSignShopObjectType;
	}

	@Override
	public SKCitizensShopObjectType getCitizensShopObjectType() {
		return plugin.getCitizensShops().getCitizensShopObjectType();
	}

	public SKFancyNpcShopObjectType getFancyNpcShopObjectType() {
		return plugin.getFancyNpcsShops().getFancyNpcShopObjectType();
	}

	// STATICS (for convenience):

	public static SKDefaultShopObjectTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopObjectTypes();
	}

	public static SKLivingShopObjectTypes LIVING() {
		return getInstance().getLivingShopObjectTypes();
	}

	public static SKEndCrystalShopObjectType END_CRYSTAL() {
		return getInstance().getEndCrystalShopObjectType();
	}

	public static SKSignShopObjectType SIGN() {
		return getInstance().getSignShopObjectType();
	}

	public static SKHangingSignShopObjectType HANGING_SIGN() {
		return getInstance().getHangingSignShopObjectType();
	}

	public static SKCitizensShopObjectType CITIZEN() {
		return getInstance().getCitizensShopObjectType();
	}

	public static SKFancyNpcShopObjectType FANCY_NPC() {
		return getInstance().getFancyNpcShopObjectType();
	}
}
