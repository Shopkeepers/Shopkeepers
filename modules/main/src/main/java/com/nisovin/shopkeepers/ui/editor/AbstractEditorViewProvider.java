package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.ui.lib.AbstractUIType;
import com.nisovin.shopkeepers.ui.lib.ViewContext;
import com.nisovin.shopkeepers.ui.lib.ViewProvider;
import com.nisovin.shopkeepers.ui.villager.editor.VillagerEditorViewProvider;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for editor view providers that support editing multiple pages of trades and offer
 * various editor buttons.
 * <p>
 * For example used by {@link ShopkeeperEditorViewProvider} and {@link VillagerEditorViewProvider}.
 */
public abstract class AbstractEditorViewProvider extends ViewProvider {

	protected final TradingRecipesAdapter tradingRecipesAdapter;

	protected AbstractEditorViewProvider(
			AbstractUIType uiType,
			ViewContext viewContext,
			TradingRecipesAdapter tradingRecipesAdapter
	) {
		super(uiType, viewContext);
		Validate.notNull(tradingRecipesAdapter, "tradingRecipesAdapter is null");
		this.tradingRecipesAdapter = tradingRecipesAdapter;
	}

	@Override
	public boolean canAccess(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		// Permission for the type of shopkeeper is checked in the AdminShopkeeper specific
		// ViewProvider.
		// Owner is checked in the PlayerShopkeeper specific ViewProvider.
		return true;
	}
}
