package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorViewProvider;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class TradingPlayerShopEditorViewProvider extends PlayerShopEditorViewProvider {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<TradeOffer> {

		private final SKTradingPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKTradingPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends TradeOffer> offers = shopkeeper.getOffers();
			// With heuristic initial capacity:
			List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8);
			offers.forEach(offer -> {
				// The offer returns copies of its items:
				TradingRecipeDraft recipe = new TradingRecipeDraft(
						offer.getResultItem(),
						offer.getItem1(),
						offer.getItem2()
				);
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

				if (shopkeeper.hasOffer(containerItem)) {
					// There is already a recipe for this item:
					continue;
				}

				if (InventoryUtils.contains(newRecipes, containerItem)) {
					// We already added a new recipe for this item:
					continue;
				}

				// Add new empty recipe:
				containerItem = ItemUtils.copySingleItem(containerItem);
				TradingRecipeDraft recipe = new TradingRecipeDraft(containerItem, null, null);
				recipes.add(recipe);
				newRecipes.add(containerItem);
			}

			return recipes;
		}

		@Override
		protected List<? extends TradeOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<? extends TradeOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected @Nullable TradeOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack resultItem = Unsafe.assertNonNull(recipe.getResultItem());
			UnmodifiableItemStack item1 = Unsafe.assertNonNull(recipe.getRecipeItem1());
			UnmodifiableItemStack item2 = recipe.getRecipeItem2();

			// Replace placeholder items, if any:
			// Note: We also replace placeholder items in the buy items, because this allows the
			// setup of trades before the player has all the required items.
			resultItem = PlaceholderItems.replaceNonNull(resultItem);
			item1 = PlaceholderItems.replaceNonNull(item1);
			item2 = PlaceholderItems.replace(item2);

			return TradeOffer.create(resultItem, item1, item2);
		}
	}

	protected TradingPlayerShopEditorViewProvider(SKTradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeper() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new TradingPlayerShopEditorView(this, player, uiState);
	}
}
