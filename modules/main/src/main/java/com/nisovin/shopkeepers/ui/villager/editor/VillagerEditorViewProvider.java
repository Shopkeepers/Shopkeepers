package com.nisovin.shopkeepers.ui.villager.editor;

import java.util.List;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.MerchantRecipe;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.editor.AbstractEditorViewProvider;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.ui.villager.VillagerViewContext;
import com.nisovin.shopkeepers.util.bukkit.MerchantUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Provides an editor for regular villagers and wandering traders.
 */
public final class VillagerEditorViewProvider extends AbstractEditorViewProvider {

	// We compare the recipes from the editor with the original recipes and keep the original
	// recipes with their original internal data if the items have not changed.
	// TODO Somehow support changing/persisting: max-uses, uses, exp reward, villager xp reward,
	// price multiplier?
	// TODO The trades may change during the editor session, in which case the comparison between
	// new and old recipes no longer works (trades may get reverted to the editor state).
	private static class TradingRecipesAdapter
			extends DefaultTradingRecipesAdapter<MerchantRecipe> {

		private final AbstractVillager villager;

		private TradingRecipesAdapter(AbstractVillager villager) {
			assert villager != null;
			this.villager = villager;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			assert villager.isValid();
			List<MerchantRecipe> merchantRecipes = villager.getRecipes();
			var recipes = MerchantUtils.createTradingRecipeDrafts(merchantRecipes);
			return recipes;
		}

		@Override
		protected List<? extends MerchantRecipe> getOffers() {
			assert villager.isValid();
			return villager.getRecipes();
		}

		@Override
		protected void setOffers(List<? extends MerchantRecipe> newOffers) {
			assert villager.isValid();

			// Stop any current trading with the villager:
			HumanEntity trader = villager.getTrader();
			if (trader != null) {
				trader.closeInventory();
				// TODO Send a message to the player explaining that the villager's trades have
				// changed?
			}

			// Apply the new trading recipes:
			villager.setRecipes(Unsafe.castNonNull(newOffers));
		}

		@Override
		protected @Nullable MerchantRecipe createOffer(TradingRecipeDraft recipe) {
			return MerchantUtils.createMerchantRecipe(recipe);
		}

		@Override
		protected boolean areOffersEqual(MerchantRecipe oldOffer, MerchantRecipe newOffer) {
			// Keep the old recipe (including all of its other internal data) if the items are still
			// the same:
			return MerchantUtils.MERCHANT_RECIPES_EQUAL_ITEMS.equals(oldOffer, newOffer);
		}
	}

	public VillagerEditorViewProvider(AbstractVillager villager) {
		super(
				SKDefaultUITypes.VILLAGER_EDITOR(),
				new VillagerViewContext(villager),
				new TradingRecipesAdapter(villager)
		);
	}

	@Override
	public VillagerViewContext getContext() {
		return (VillagerViewContext) super.getContext();
	}

	public AbstractVillager getVillager() {
		return this.getContext().getObject();
	}

	@Override
	public boolean canAccess(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		// Check permission:
		if (!this.checkEditPermission(player, silent)) {
			return false;
		}
		return true;
	}

	private boolean checkEditPermission(Player player, boolean silent) {
		if (this.getVillager() instanceof WanderingTrader) {
			return this.checkEditWanderingTraderPermission(player, silent);
		} else { // Regular villager
			return this.checkEditVillagerPermission(player, silent);
		}
	}

	private boolean checkEditWanderingTraderPermission(Player player, boolean silent) {
		return this.checkEditPermission(
				player,
				silent,
				ShopkeepersPlugin.EDIT_WANDERING_TRADERS_PERMISSION,
				Messages.missingEditWanderingTradersPerm
		);
	}

	private boolean checkEditVillagerPermission(Player player, boolean silent) {
		return this.checkEditPermission(
				player,
				silent,
				ShopkeepersPlugin.EDIT_VILLAGERS_PERMISSION,
				Messages.missingEditVillagersPerm
		);
	}

	private boolean checkEditPermission(
			Player player,
			boolean silent,
			String permission,
			Text missingPermissionMessage
	) {
		if (PermissionUtils.hasPermission(player, permission)) {
			return true;
		}

		if (!silent) {
			this.debugNotOpeningUI(player, "Player is missing the required edit permission.");
			TextUtils.sendMessage(player, missingPermissionMessage);
		}
		return false;
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new VillagerEditorView(this, player, uiState);
	}
}
