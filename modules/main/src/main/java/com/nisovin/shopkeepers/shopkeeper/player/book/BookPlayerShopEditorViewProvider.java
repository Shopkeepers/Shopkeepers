package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorViewProvider;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.inventory.BookItems;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class BookPlayerShopEditorViewProvider extends PlayerShopEditorViewProvider {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<BookOffer> {

		private final SKBookPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKBookPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// We only add one recipe per book title:
			Set<String> bookTitles = new HashSet<>();

			// Add the shopkeeper's offers:
			Map<? extends String, ? extends ItemStack> stockContainerBooksByTitle
					= shopkeeper.getCopyableBooksFromStockContainers();
			List<? extends BookOffer> offers = shopkeeper.getOffers();
			List<TradingRecipeDraft> recipes = new ArrayList<>(Math.max(
					offers.size(),
					stockContainerBooksByTitle.size()
			));
			offers.forEach(bookOffer -> {
				String bookTitle = bookOffer.getBookTitle();
				bookTitles.add(bookTitle);
				ItemStack bookItem = stockContainerBooksByTitle.get(bookTitle);
				if (bookItem == null) {
					bookItem = shopkeeper.createDummyBook(bookTitle);
				} else {
					bookItem = ItemUtils.copySingleItem(bookItem); // Also ensures a stack size of 1
				}
				TradingRecipeDraft recipe = createTradingRecipeDraft(bookItem, bookOffer.getPrice());
				recipes.add(recipe);
			});

			// Add new empty recipe drafts for book items from the stock containers without existing offer:
			stockContainerBooksByTitle.forEach((bookTitle, bookItem) -> {
				assert bookTitle != null;
				if (!bookTitles.add(bookTitle)) {
					// We already added a recipe for a book with this title.
					return;
				}

				// Add new empty recipe:
				ItemStack bookItemCopy = ItemUtils.copySingleItem(bookItem);
				TradingRecipeDraft recipe = createTradingRecipeDraft(bookItemCopy, 0);
				recipes.add(recipe);
			});
			return recipes;
		}

		@Override
		protected List<? extends BookOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<? extends BookOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected @Nullable BookOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack bookItem = recipe.getResultItem();
			BookMeta bookMeta = BookItems.getBookMeta(bookItem);
			if (bookMeta == null) return null; // Invalid recipe (not a written book, unexpected)
			if (!SKBookPlayerShopkeeper.isDummyBook(bookMeta) && !BookItems.isCopyable(bookMeta)) {
				return null; // Invalid recipe
			}

			// Note: The dummy books provide the original book title as well.
			String bookTitle = BookItems.getTitle(bookMeta);
			if (bookTitle == null) return null; // Invalid recipe

			int price = getPrice(shopkeeper, recipe);
			if (price <= 0) {
				// Unexpected.
				return null; // Ignore invalid recipe
			}

			return BookOffer.create(bookTitle, price);
		}
	}

	protected BookPlayerShopEditorViewProvider(SKBookPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKBookPlayerShopkeeper getShopkeeper() {
		return (SKBookPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new BookPlayerShopEditorView(this, player, uiState);
	}
}
