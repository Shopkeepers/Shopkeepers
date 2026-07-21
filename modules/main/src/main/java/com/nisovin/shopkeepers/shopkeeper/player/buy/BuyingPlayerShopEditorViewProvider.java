package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorViewProvider;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class BuyingPlayerShopEditorViewProvider extends PlayerShopEditorViewProvider {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<PriceOffer> {

		private final SKBuyingPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKBuyingPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends PriceOffer> offers = shopkeeper.getOffers();
			// With heuristic initial capacity:
			List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8);
			Currency baseCurrency = Currencies.getBase();
			offers.forEach(offer -> {
				UnmodifiableItemStack tradedItem = offer.getItem();
				UnmodifiableItemStack currencyItem = baseCurrency.getItemData().createUnmodifiableItemStack(offer.getPrice());
				TradingRecipeDraft recipe = new TradingRecipeDraft(currencyItem, tradedItem, null);
				recipes.add(recipe);
			});

			// Add new empty recipe drafts for items from the earnings containers without existing
			// offer:
			// We only add one recipe per similar item:
			List<ItemStack> newRecipes = new ArrayList<>();
			// Empty if no containers are found:
			@Nullable ItemStack[] earningsContainerContents = shopkeeper.getEarningsContainerContents();
			for (ItemStack containerItem : earningsContainerContents) {
				// Ignore empty ItemStacks:
				if (containerItem == null) continue;
				if (ItemUtils.isEmpty(containerItem)) continue;

				// Replace placeholder item, if this is one:
				containerItem = PlaceholderItems.replaceNonNull(containerItem);

				// Ignore currency items:
				if (Currencies.matchesAny(containerItem)) {
					continue;
				}

				if (shopkeeper.getOffer(containerItem) != null) {
					// There is already a recipe for this item:
					continue;
				}

				if (InventoryUtils.contains(newRecipes, containerItem)) {
					// We already added a new recipe for this item:
					continue;
				}

				// Add a new empty recipe:
				containerItem = ItemUtils.copySingleItem(containerItem);
				TradingRecipeDraft recipe = new TradingRecipeDraft(null, containerItem, null);
				recipes.add(recipe);
				newRecipes.add(containerItem);
			}
			return recipes;
		}

		@Override
		protected List<? extends PriceOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<? extends PriceOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected @Nullable PriceOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			assert recipe.getItem2() == null; // Cannot be set via the editor

			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack priceItem = Unsafe.assertNonNull(recipe.getResultItem());
			// Make sure that the item is actually currency, just in case:
			if (!Currencies.getBase().getItemData().matches(priceItem)) {
				// Unexpected.
				Log.debug(shopkeeper.getLogPrefix()
						+ "Price item does not match the base currency!");
				return null; // Ignore invalid recipe
			}
			assert priceItem.getAmount() > 0;
			int price = priceItem.getAmount();

			UnmodifiableItemStack tradedItem = Unsafe.assertNonNull(recipe.getItem1());
			// Replace placeholder item, if this is one:
			tradedItem = PlaceholderItems.replaceNonNull(tradedItem);

			return PriceOffer.create(tradedItem, price);
		}
	}

	protected BuyingPlayerShopEditorViewProvider(SKBuyingPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKBuyingPlayerShopkeeper getShopkeeper() {
		return (SKBuyingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new BuyingPlayerShopEditorView(this, player, uiState);
	}
}
