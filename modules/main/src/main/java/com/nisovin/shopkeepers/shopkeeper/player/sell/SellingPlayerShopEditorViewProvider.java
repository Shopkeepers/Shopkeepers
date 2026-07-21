package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorViewProvider;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class SellingPlayerShopEditorViewProvider extends PlayerShopEditorViewProvider {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<PriceOffer> {

		private final SKSellingPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKSellingPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends PriceOffer> offers = shopkeeper.getOffers();
			// With heuristic initial capacity:
			List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8);
			offers.forEach(offer -> {
				ItemStack tradedItem = ItemUtils.asItemStack(offer.getItem());
				TradingRecipeDraft recipe = createTradingRecipeDraft(tradedItem, offer.getPrice());
				recipes.add(recipe);
			});

			// Add new empty recipe drafts for items from the stock containers without existing
			// offer:
			// We only add one recipe per similar item:
			List<ItemStack> newRecipes = new ArrayList<>();
			// Empty if no stock containers are found:
			@Nullable ItemStack[] stockContainerContents = shopkeeper.getStockContainerContents();
			for (ItemStack containerItem : stockContainerContents) {
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

				// Add new empty recipe:
				containerItem = ItemUtils.copySingleItem(containerItem);
				TradingRecipeDraft recipe = createTradingRecipeDraft(containerItem, 0);
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
			int price = getPrice(shopkeeper, recipe);
			if (price <= 0) {
				// Unexpected.
				return null; // Ignore invalid recipe
			}

			// We can reuse the trading recipe draft's items without copying them first.
			// Not null because the recipe is valid:
			UnmodifiableItemStack resultItem = Unsafe.assertNonNull(recipe.getResultItem());
			// Replace placeholder item, if this is one:
			// Note: We also replace placeholder items in selling shopkeepers, because this allows
			// the setup of trades before the player has all the required items.
			resultItem = PlaceholderItems.replaceNonNull(resultItem);

			return PriceOffer.create(resultItem, price);
		}
	}

	protected SellingPlayerShopEditorViewProvider(SKSellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeper() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new SellingPlayerShopEditorView(this, player, uiState);
	}
}
