package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.trade.TradingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradeOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKTradingPlayerShopkeeper
		extends AbstractPlayerShopkeeper implements TradingPlayerShopkeeper {

	// There can be multiple different offers for the same kind of item:
	private final List<TradeOffer> offers = new ArrayList<>();
	private final List<? extends TradeOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a new and not yet initialized {@link SKTradingPlayerShopkeeper}.
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 */
	protected SKTradingPlayerShopkeeper() {
	}

	@Override
	protected void setup() {
		this.registerViewProviderIfMissing(DefaultUITypes.EDITOR(), () -> {
			return new TradingPlayerShopEditorViewProvider(this);
		});
		this.registerViewProviderIfMissing(DefaultUITypes.TRADING(), () -> {
			return new TradingPlayerShopTradingViewProvider(this);
		});
		super.setup();
	}

	@Override
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		super.loadDynamicState(shopkeeperData);
		this.loadOffers(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ShopkeeperData shopkeeperData, boolean saveAll) {
		super.saveDynamicState(shopkeeperData, saveAll);
		this.saveOffers(shopkeeperData);
	}

	// ITEM UPDATES

	@Override
	protected int updateItems(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		int updatedItems = super.updateItems(logPrefix, shopkeeperData);
		updatedItems += updateOfferItems(logPrefix, shopkeeperData);
		return updatedItems;
	}

	private static int updateOfferItems(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		try {
			var updatedOffers = new ArrayList<TradeOffer>(shopkeeperData.get(OFFERS));
			var updatedItems = SKTradeOffer.updateItems(updatedOffers, logPrefix);
			if (updatedItems > 0) {
				shopkeeperData.set(OFFERS, updatedOffers);
				return updatedItems;
			}
		} catch (InvalidDataException e) {
			Log.warning(logPrefix + "Failed to load '" + OFFERS.getName() + "'!", e);
		}
		return 0;
	}

	//

	@Override
	public TradingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_TRADING();
	}

	@Override
	public boolean hasTradingRecipes(@Nullable Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends TradingRecipe> getTradingRecipes(@Nullable Player player) {
		// Empty if no stock containers are found:
		@Nullable ItemStack[] stockContainerContents = this.getStockContainerContents();
		List<? extends TradeOffer> offers = this.getOffers();
		List<TradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(offer -> {
			UnmodifiableItemStack resultItem = offer.getResultItem();
			boolean outOfStock = !InventoryUtils.containsAtLeast(
					stockContainerContents,
					resultItem,
					resultItem.getAmount()
			);
			TradingRecipe recipe = SKTradeOffer.toTradingRecipe(offer, outOfStock);
			recipes.add(recipe);
		});
		return Collections.unmodifiableList(recipes);
	}

	// OFFERS

	private static final String DATA_KEY_OFFERS = "offers";
	public static final Property<List<? extends TradeOffer>> OFFERS = new BasicProperty<List<? extends TradeOffer>>()
			.dataKeyAccessor(DATA_KEY_OFFERS, SKTradeOffer.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"trading-offers",
				MigrationPhase.ofShopkeeperClass(SKTradingPlayerShopkeeper.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				return SKTradeOffer.migrateOffers(
						shopkeeperData.getDataValue(DATA_KEY_OFFERS),
						logPrefix
				);
			}
		});
	}

	private void loadOffers(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setOffers(shopkeeperData.get(OFFERS));
	}

	private void saveOffers(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(OFFERS, this.getOffers());
	}

	@Override
	public List<? extends TradeOffer> getOffers() {
		return offersView;
	}

	public boolean hasOffer(ItemStack resultItem) {
		Validate.notNull(resultItem, "resultItem is null");
		for (TradeOffer offer : this.getOffers()) {
			if (offer.getResultItem().isSimilar(resultItem)) {
				return true;
			}
		}
		return false;
	}

	public @Nullable TradeOffer getOffer(TradingRecipe tradingRecipe) {
		for (TradeOffer offer : this.getOffers()) {
			if (offer.areItemsEqual(tradingRecipe)) {
				return offer;
			}
		}
		return null;
	}

	@Override
	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

	private void _clearOffers() {
		offers.clear();
	}

	@Override
	public void setOffers(List<? extends TradeOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(List<? extends TradeOffer> offers) {
		assert offers != null && !CollectionUtils.containsNull(offers);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(TradeOffer offer) {
		Validate.notNull(offer, "offer is null");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(TradeOffer offer) {
		assert offer != null;
		Validate.isTrue(offer instanceof SKTradeOffer, "offer is not of type SKTradeOffer");
		SKTradeOffer skOffer = (SKTradeOffer) offer;

		// Add the new offer:
		offers.add(skOffer);
	}

	@Override
	public void addOffers(List<? extends TradeOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends TradeOffer> offers) {
		assert offers != null && !CollectionUtils.containsNull(offers);
		offers.forEach(this::_addOffer);
	}
}
