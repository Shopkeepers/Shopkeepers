package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.logging.Log;

public final class SellingPlayerShopType extends AbstractPlayerShopType<SKSellingPlayerShopkeeper> {

	static {
		// Register shopkeeper data migrations:
		// TODO Remove this again. This was added in v2.1.0 (2018).
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"shop-type-player-to-sell",
				MigrationPhase.EARLY
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				String shopTypeId = shopkeeperData.getOrNullIfMissing(AbstractShopkeeper.SHOP_TYPE_ID);
				if (shopTypeId == null) {
					return false; // Shop type is missing -> Skip migration.
				}

				boolean migrated = false;
				if (shopTypeId.equalsIgnoreCase("player")) {
					Log.info(logPrefix + "Migrating shop type from 'player' to 'sell'.");
					shopkeeperData.set(AbstractShopkeeper.SHOP_TYPE_ID, "sell");
					migrated = true;
				}

				return migrated;
			}
		});

	}

	public SellingPlayerShopType() {
		super(
				"sell",
				Arrays.asList("selling", "normal", "player"),
				ShopkeepersPlugin.PLAYER_SELL_PERMISSION,
				SKSellingPlayerShopkeeper.class
		);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeSelling;
	}

	@Override
	public Text getDescriptionText() {
		return Messages.shopTypeDescSelling;
	}

	@Override
	public Text getSetupDescriptionText() {
		return Messages.shopSetupDescSelling;
	}

	@Override
	public List<? extends String> getTradeSetupDescription() {
		return Messages.tradeSetupDescSelling;
	}

	@Override
	protected SKSellingPlayerShopkeeper createNewShopkeeper() {
		return new SKSellingPlayerShopkeeper();
	}
}
