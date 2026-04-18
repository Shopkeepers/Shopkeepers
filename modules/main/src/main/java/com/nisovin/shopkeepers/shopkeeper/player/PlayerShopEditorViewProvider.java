package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.MemberPermission;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperEditorLayout;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperEditorViewProvider;
import com.nisovin.shopkeepers.ui.editor.TradingRecipesAdapter;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class PlayerShopEditorViewProvider extends ShopkeeperEditorViewProvider {

	// Note: In the editor item1 is representing the low cost item and item2 the
	// high cost item, but
	// in the corresponding trading recipe they will be swapped if they are both
	// present.

	protected PlayerShopEditorViewProvider(
			AbstractPlayerShopkeeper shopkeeper,
			TradingRecipesAdapter tradingRecipesAdapter) {
		super(SKDefaultUITypes.EDITOR(), shopkeeper, tradingRecipesAdapter);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canAccess(Player player, boolean silent) {
		if (!super.canAccess(player, silent))
			return false;

		AbstractPlayerShopkeeper shopkeeper = this.getShopkeeper();
		// Check the owner or member:
		if (!shopkeeper.isOwner(player)
				&& !this.canMemberAccess(player)
				&& !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Player is not owning this shop.");
				TextUtils.sendMessage(player, Messages.notOwner);
			}
			return false;
		}
		return true;
	}

	private boolean canMemberAccess(Player player) {
		AbstractPlayerShopkeeper shopkeeper = this.getShopkeeper();
		if (!shopkeeper.isMember(player))
			return false;
		MemberPermission level = Settings.DerivedSettings.memberPermissionLevel;
		return level == MemberPermission.CHEST_AND_TRADES || level == MemberPermission.FULL;
	}

	@Override
	protected ShopkeeperEditorLayout createLayout() {
		return new PlayerShopEditorLayout(this.getShopkeeper());
	}

	// Note: In case the cost is too large to represent, it sets the cost to zero.
	// (So opening and closing the editor window will remove the offer, instead of
	// setting the costs
	// to a lower value than what was previously somehow specified)
	protected static TradingRecipeDraft createTradingRecipeDraft(
			@ReadOnly ItemStack resultItem,
			int cost) {
		ItemStack highCostItem = null;
		ItemStack lowCostItem = null;

		int remainingCost = cost;
		if (Currencies.isHighCurrencyEnabled()) {
			Currency highCurrency = Currencies.getHigh();
			int highCost = 0;
			if (remainingCost > Settings.highCurrencyMinCost) {
				highCost = Math.min(
						(remainingCost / highCurrency.getValue()),
						highCurrency.getMaxStackSize());
			}
			if (highCost > 0) {
				remainingCost -= (highCost * highCurrency.getValue());
				highCostItem = Currencies.getHigh().getItemData().createItemStack(highCost);
			}
		}
		if (remainingCost > 0) {
			Currency baseCurrency = Currencies.getBase();
			if (remainingCost <= baseCurrency.getMaxStackSize()) {
				lowCostItem = Currencies.getBase().getItemData().createItemStack(remainingCost);
			} else {
				// Cost is too large to represent: Reset cost to zero.
				assert lowCostItem == null;
				highCostItem = null;
			}
		}

		return new TradingRecipeDraft(resultItem, lowCostItem, highCostItem);
	}

	protected static int getPrice(Shopkeeper shopkeeper, TradingRecipeDraft recipe) {
		Validate.notNull(recipe, "recipe is null");
		int price = 0;

		UnmodifiableItemStack item1 = recipe.getItem1();
		Currency currency1 = Currencies.match(item1);
		if (currency1 != null) {
			assert item1 != null;
			price += (currency1.getValue() * item1.getAmount());
		} else if (!ItemUtils.isEmpty(item1)) {
			// Unexpected.
			Log.debug(shopkeeper.getLogPrefix() + "Price item 1 does not match any currency!");
		}

		UnmodifiableItemStack item2 = recipe.getItem2();
		Currency currency2 = Currencies.match(item2);
		if (currency2 != null) {
			assert item2 != null;
			price += (currency2.getValue() * item2.getAmount());
		} else if (!ItemUtils.isEmpty(item2)) {
			// Unexpected.
			Log.debug(shopkeeper.getLogPrefix() + "Price item 2 does not match any currency!");
		}
		return price;
	}
}
