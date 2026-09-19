package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class ShopkeeperEditorView extends EditorView {

	protected ShopkeeperEditorView(
			ShopkeeperEditorViewProvider viewProvider,
			Player player,
			UIState uiState
	) {
		super(viewProvider, player, uiState);
	}

	@Override
	protected ShopkeeperEditorLayout createLayout() {
		return new ShopkeeperEditorLayout(this);
	}

	@Override
	protected String getTitle() {
		return Messages.editorTitle;
	}

	@Override
	protected void saveRecipes() {
		var player = this.getPlayer();
		var shopkeeper = this.getShopkeeperNonNull();

		// UI sessions are aborted (i.e. not saved) when the shopkeeper is removed:
		assert shopkeeper.isValid();

		int changedOffers = this.getTradingRecipesAdapter().updateTradingRecipes(
				player,
				this.getRecipes()
		);
		if (changedOffers == 0) {
			Log.debug(() -> this.getContext().getLogPrefix() + "No offers have changed.");
		} else {
			Log.debug(() -> this.getContext().getLogPrefix() + changedOffers
					+ " offers have changed.");

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// TODO Close all other UI sessions for the shopkeeper (e.g. trading players)? Also send
			// a message to them.
		}

		// Even if no trades have changed, the shopkeeper might have been marked as dirty due to
		// other editor options. If this is the case, we trigger a save here. Otherwise, we omit the
		// save.
		if (shopkeeper.isDirty()) {
			shopkeeper.save();
		}
	}
}
