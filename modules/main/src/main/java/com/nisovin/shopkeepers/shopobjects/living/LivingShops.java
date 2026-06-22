package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShops;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.logging.Log;

public class LivingShops {

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"living-shop-object-types",
				MigrationPhase.EARLY
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				ShopObjectData shopObjectData = shopkeeperData.getOrNullIfMissing(
						AbstractShopkeeper.SHOP_OBJECT_DATA
				);
				if (shopObjectData == null) return false;

				String objectTypeId = shopObjectData.getOrNullIfMissing(
						AbstractShopObject.SHOP_OBJECT_TYPE_ID
				);
				if (objectTypeId == null) {
					return false; // Shop object type is missing. -> Skip migration.
				}

				boolean migrated = false;

				// TODO Remove these migrations again at some point
				// MC 1.16:
				// Convert 'pig-zombie' to 'zombified-piglin':
				if (objectTypeId.equals("pig-zombie")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "zombified-piglin");
					Log.warning(logPrefix + "Migrated object type 'pig-zombie' to 'zombified-piglin'.");
					migrated = true;
				}

				// MC 1.20.5:
				// Convert 'mushroom-cow' to 'mooshroom':
				if (objectTypeId.equals("mushroom-cow")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "mooshroom");
					Log.warning(logPrefix + "Migrated object type 'mushroom-cow' to 'mooshroom'.");
					migrated = true;
				}
				// Convert 'snowman' to 'snow-golem':
				if (objectTypeId.equals("snowman")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "snow-golem");
					Log.warning(logPrefix + "Migrated object type 'snowman' to 'snow-golem'.");
					migrated = true;
				}

				return migrated;
			}
		});
	}

	private final SKShopkeepersPlugin plugin;
	private final SKLivingShopObjectTypes livingShopObjectTypes;
	private final LivingEntityShopListener livingEntityShopListener;
	// The PigZapListener class is not loaded if the compat module is handling the event:
	private @Nullable Listener pigZapListener;

	public LivingShops(SKShopkeepersPlugin plugin, BaseEntityShops baseEntityShops) {
		this.plugin = plugin;
		livingShopObjectTypes = new SKLivingShopObjectTypes(baseEntityShops, this);
		livingEntityShopListener = new LivingEntityShopListener(plugin);
	}

	public void onEnable() {
		livingEntityShopListener.onEnable();

		if (Compat.getProvider().isHandlePigZapEvent() && pigZapListener == null) {
			pigZapListener = new PigZapListener(plugin);
		}

		if (pigZapListener != null) {
			((PigZapListener) pigZapListener).onEnable();
		}
	}

	public void onDisable() {
		livingEntityShopListener.onDisable();

		if (pigZapListener != null) {
			((PigZapListener) pigZapListener).onDisable();
		}
	}

	public SKLivingShopObjectTypes getLivingShopObjectTypes() {
		return livingShopObjectTypes;
	}
}
