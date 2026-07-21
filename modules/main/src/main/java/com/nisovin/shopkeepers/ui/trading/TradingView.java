package com.nisovin.shopkeepers.ui.trading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeCompletedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.trading.TradeEffect;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.MerchantUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Lazy;
import com.nisovin.shopkeepers.util.logging.Log;

public class TradingView extends View {

	private static final Set<? extends Class<? extends InventoryEvent>> ADDITIONAL_INVENTORY_EVENTS
			= Collections.singleton(TradeSelectEvent.class);

	// Those slot ids match both raw slot ids and regular slot ids for the merchant inventory view
	// with the merchant inventory at the top:
	protected static final int BUY_ITEM_1_SLOT_ID = 0;
	protected static final int BUY_ITEM_2_SLOT_ID = 1;
	protected static final int RESULT_ITEM_SLOT_ID = 2;

	public TradingView(TradingViewProvider provider, Player player, UIState uiState) {
		super(provider, player, uiState);
	}

	protected TradingViewProvider getTradingViewProvider() {
		return (TradingViewProvider) this.getProvider();
	}

	private final List<TradingListener> getTradingListeners() {
		return this.getTradingViewProvider().getTradingListeners();
	}

	@Override
	protected @Nullable InventoryView openInventoryView() {
		// Create and open the trading window:
		Player player = this.getPlayer();
		Shopkeeper shopkeeper = this.getShopkeeperNonNull();
		String title = this.getInventoryTitle();
		List<? extends TradingRecipe> recipes = shopkeeper.getTradingRecipes(player);
		if (recipes.isEmpty()) {
			// Unexpected: Already checked by the view provider.
			this.debugNotOpeningUI(player, "Shopkeeper has no offers.");
			TextUtils.sendMessage(player, Messages.cannotTradeNoOffers);
			return null;
		}
		return this.openTradeWindow(title, recipes);
	}

	protected @Nullable InventoryView openTradeWindow(
			String title,
			List<? extends TradingRecipe> recipes
	) {
		var player = this.getPlayer();

		// Set up merchant:
		Merchant merchant = this.setupMerchant(recipes);

		// Increment 'talked-to-villager' statistic when opening trading menu:
		if (Settings.incrementVillagerStatistics) {
			player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);
		}

		// Open merchant:
		var menuBuilder = MenuType.MERCHANT.builder();
		Compat.getProvider().setInventoryViewTitle(menuBuilder, title);
		var inventoryView = menuBuilder.merchant(merchant).build(player);
		player.openInventory(inventoryView);
		return inventoryView;
	}

	protected Merchant setupMerchant(List<? extends TradingRecipe> recipes) {
		Merchant merchant = Bukkit.createMerchant();
		this.setupMerchantRecipes(merchant, recipes);
		return merchant;
	}

	protected void setupMerchantRecipes(Merchant merchant, List<? extends TradingRecipe> recipes) {
		// Create list of merchant recipes:
		List<MerchantRecipe> merchantRecipes = this.createMerchantRecipes(recipes);
		// Set merchant's recipes:
		merchant.setRecipes(merchantRecipes);
	}

	protected List<MerchantRecipe> createMerchantRecipes(List<? extends TradingRecipe> recipes) {
		List<MerchantRecipe> merchantRecipes = new ArrayList<>();
		for (TradingRecipe recipe : recipes) {
			merchantRecipes.add(this.createMerchantRecipe(recipe));
		}
		return merchantRecipes;
	}

	protected MerchantRecipe createMerchantRecipe(TradingRecipe recipe) {
		return MerchantUtils.createMerchantRecipe(recipe); // Default
	}

	protected String getInventoryTitle() {
		String title = this.getShopkeeperNonNull().getName(); // Can be empty
		if (title.isEmpty()) {
			title = Messages.tradingTitleDefault;
		}
		return Messages.tradingTitlePrefix + title;
	}

	@Override
	public void updateInventory() {
		this.updateTrades();
	}

	protected void updateTrades() {
		// Check if the currently open inventory still corresponds to this UI:
		if (!this.isOpen()) return;

		Player player = this.getPlayer();
		InventoryView openInventory = player.getOpenInventory();
		assert openInventory.getType() == InventoryType.MERCHANT;
		MerchantInventory merchantInventory = (MerchantInventory) openInventory.getTopInventory();
		Merchant merchant = merchantInventory.getMerchant();
		List<MerchantRecipe> oldMerchantRecipes = merchant.getRecipes();

		Shopkeeper shopkeeper = this.getShopkeeperNonNull();
		List<? extends TradingRecipe> recipes = shopkeeper.getTradingRecipes(player);
		List<MerchantRecipe> newMerchantRecipes = this.createMerchantRecipes(recipes);
		if (MerchantUtils.MERCHANT_RECIPES_IGNORE_USES_EXCEPT_BLOCKED.equals(
				oldMerchantRecipes,
				newMerchantRecipes
		)) {
			Log.debug(() -> this.getContext().getLogPrefix()
					+ "Trades are still up-to-date for player " + player.getName());
			return; // Recipes did not change
		}
		Log.debug(() -> this.getContext().getLogPrefix() + "Updating trades for player "
				+ player.getName());

		// It is not safe to reduce the number of trading recipes for the player, so we may need to
		// add dummy recipes:
		this.ensureNoFewerRecipes(oldMerchantRecipes, newMerchantRecipes);

		// Set merchant's recipes:
		merchant.setRecipes(newMerchantRecipes);

		// Update recipes for the client:
		Compat.getProvider().updateTrades(player);
	}

	// Dynamically modifying trades (e.g. their blocked state, or properties such as their items),
	// or adding trades, is fine. But reducing the number of trades is not safe, because the index
	// of the currently selected recipe can end up being out of bounds on the client. There is no
	// way for us to remotely update it into valid bounds.
	// TODO Check if this still applies in MC 1.14+
	// We therefore insert blocked dummy trades to retain the previous recipe count. We could insert
	// empty dummy trades at the end of the recipe list, but that might confuse players since empty
	// trades are rather unusual. Instead we try to (heuristically) determine the recipes that were
	// removed, and then insert blocked variants of these recipes.
	private void ensureNoFewerRecipes(
			List<? extends MerchantRecipe> oldMerchantRecipes,
			List<MerchantRecipe> newMerchantRecipes
	) {
		int oldRecipeCount = oldMerchantRecipes.size();
		int newRecipeCount = newMerchantRecipes.size();
		if (newRecipeCount >= oldRecipeCount) {
			// The new recipe list already contains no fewer recipes than the previous recipe list:
			return;
		}

		// Try to identify the removed recipes in order to insert blocked dummy recipes that likely
		// make sense:
		// In order to keep the computational effort low, this heuristic simply walks through both
		// recipe lists at the same time and matches recipes based on their index and their items:
		// If the items of the recipes at the same index are the same, it is assumed that these
		// recipes correspond to each other.
		// If the items of a recipe changed, or recipes were inserted at positions other than at the
		// end of the list, this heuristic may insert sub-optimal dummy recipes. However, it still
		// ensures that the recipe list does not shrink in size.
		for (int i = 0; i < oldRecipeCount; ++i) {
			MerchantRecipe oldRecipe = oldMerchantRecipes.get(i);
			MerchantRecipe newRecipe;
			if (i < newRecipeCount) {
				newRecipe = newMerchantRecipes.get(i);
			} else {
				newRecipe = null;
			}
			if (!MerchantUtils.MERCHANT_RECIPES_EQUAL_ITEMS.equals(oldRecipe, newRecipe)) {
				// The recipes at this index differ: Insert the old recipe into the new recipe list,
				// but set its max uses to 0 so that it cannot be used.
				oldRecipe.setMaxUses(0); // Block the trade
				newMerchantRecipes.add(i, oldRecipe);
				newRecipeCount++;
				if (newRecipeCount == oldRecipeCount) {
					// Abort the insertion of dummy recipes if we reached our goal of ensuring that
					// the new recipe list contains no fewer recipes than the old recipe list:
					break;
				}
			}
		}
		assert newRecipeCount == oldRecipeCount;
	}

	@Override
	protected Set<? extends Class<? extends InventoryEvent>> getAdditionalInventoryEvents() {
		return ADDITIONAL_INVENTORY_EVENTS;
	}

	@Override
	protected void onInventoryClose(@Nullable InventoryCloseEvent closeEvent) {
		// Callback for subclasses.
	}

	// TRADE PROCESSING

	// TODO This doesn't work because the client will automatically update the result slot item
	// whenever a slot is changed.
	/*@Override
	protected void onInventoryClickEarly(InventoryClickEvent clickEvent, Player player) {
		// Clear the result item slot if we use strict item comparison and there is no valid trade:
		// TODO We also need to do this when the player selects a trading recipe, because that will
		// automatically insert the matching items into the trading view.
		if (!Settings.useStrictItemComparison) return;
		if (clickEvent.isCancelled()) return;
		// This needs to happen after the event has been handled, because Minecraft will set the
		// result slot afterwards:
		SKUISession uiSession = SKShopkeepersPlugin.getInstance().getUIRegistry().getSession(player);
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			if (!uiSession.isValid()) return;
			if (clickEvent.isCancelled()) return;
			// Logs if it encounters items that are not strictly matching and then clears the result
			// slot:
			this.checkForTrade(clickEvent, true, false, false);
		});
	}*/

	@Override
	protected void onInventoryEventEarly(InventoryEvent event) {
		if (event instanceof TradeSelectEvent tradeSelectEvent) {
			// Inform listeners:
			this.getTradingListeners().forEach(listener -> {
				listener.onTradeSelect(this, tradeSelectEvent);
			});
		}
	}

	private boolean canSlotHoldItemStack(
			@ReadOnly @Nullable ItemStack slotItem,
			@ReadOnly ItemStack itemStack
	) {
		if (ItemUtils.isEmpty(slotItem)) return true;
		assert slotItem != null;
		if (!itemStack.isSimilar(slotItem)) return false;
		return slotItem.getAmount() + itemStack.getAmount() <= slotItem.getMaxStackSize();
	}

	// Late processing, so that other plugins can cancel the trading without having to rely on
	// Shopkeepers' API.
	@Override
	protected void onInventoryClickLate(InventoryClickEvent clickEvent) {
		// Inform listeners:
		this.getTradingListeners().forEach(listener -> listener.onInventoryClick(this, clickEvent));

		Player player = this.getPlayer();
		Shopkeeper shopkeeper = this.getShopkeeperNonNull();

		if (clickEvent.isCancelled()) {
			Log.debug(() -> this.getContext().getLogPrefix()
					+ "Some plugin has cancelled the trading UI click of player "
					+ player.getName());
			return;
		}

		int rawSlot = clickEvent.getRawSlot();
		InventoryAction action = clickEvent.getAction();

		MerchantInventory merchantInventory = (MerchantInventory) clickEvent.getInventory();
		UnmodifiableItemStack resultSlotItem = UnmodifiableItemStack.of(
				merchantInventory.getItem(RESULT_ITEM_SLOT_ID)
		);
		ItemStack cursor = clickEvent.getCursor();

		// Prevent unsupported types of special clicks:
		if (action == InventoryAction.COLLECT_TO_CURSOR
				&& ItemUtils.isSimilar(resultSlotItem, cursor)) {
			// MC-129515: In the past, the behavior of this inventory action was rather weird and
			// buggy if the clicked item matches the trade result item. We therefore cancel and
			// ignore it if the cursor item matches the result item.
			// MC-148867: Since MC 1.14, Mojang fully disabled this inventory action inside the
			// trading UI, so this case should no longer be reached. We still explicitly cancel it,
			// just in case.
			Log.debug(() -> this.getContext().getLogPrefix()
					+ "Prevented unsupported type of trading UI click by player " + player.getName()
					+ ": " + action);
			clickEvent.setCancelled(true);
			InventoryUtils.updateInventoryLater(player);
			return;
		}

		// All currently supported inventory actions that might trigger trades involve a click of
		// the result slot:
		if (rawSlot != RESULT_ITEM_SLOT_ID) {
			// Not canceling the event to allow regular inventory interaction inside the player's
			// inventory.
			return;
		}

		// Some clicks on the result slot don't trigger trades:
		if (action == InventoryAction.CLONE_STACK) {
			return;
		}

		// We are handling all types of clicks which might trigger a trade ourselves:
		clickEvent.setCancelled(true);
		InventoryUtils.updateInventoryLater(player);

		// Set up a new TradingContext:
		TradingContext tradingContext = new TradingContext(shopkeeper, clickEvent);
		this.setupTradingContext(tradingContext);

		// Check for a trade:
		Trade trade = this.checkForTrade(tradingContext, false);
		if (trade == null) {
			// No trade available.
			return;
		}
		assert resultSlotItem != null;
		assert trade.getTradingRecipe().getResultItem().isSimilar(resultSlotItem);

		PlayerInventory playerInventory = player.getInventory();
		boolean isCursorEmpty = ItemUtils.isEmpty(cursor);

		// Handle trade depending on used inventory action:
		// Note: There is no need for us to add a custom "quick trade" action, since Minecraft
		// already allows players to trade very quickly: Players can shift click the result slot to
		// trade as often as possible with the current input items, and then press the spacebar to
		// refill the input slots for the selected trade with items from the inventory.
		// Note: In creative mode, players can also middle click the result slot to copy a stack of
		// the result item (action CLONE_STACK), but this does not trigger a trade.
		// Left click and right click: Trade once.
		if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF) {
			if (this.handleTrade(trade)) {
				UnmodifiableItemStack resultItem = trade.getTradeEvent().getResultItem();
				boolean resultItemEmpty = ItemUtils.isEmpty(resultItem);

				if (!resultItemEmpty) {
					assert resultItem != null;

					if (!this.canSlotHoldItemStack(cursor, ItemUtils.asItemStack(resultItem))) {
						Log.debug(() -> this.getContext().getLogPrefix()
								+ "Not handling trade: The cursor cannot hold the result items.");
						this.onTradeAborted(tradingContext, false);
						return;
					}
				}

				if (!this.finalTradePreparation(trade)) {
					return;
				}

				// We are going to apply the trade now:
				this.preApplyTrade(trade);

				if (!resultItemEmpty) {
					assert resultItem != null;

					// Add the result items to the cursor:
					ItemStack resultCursor;
					if (isCursorEmpty) {
						// No item copy required here: setItemOnCursor copies the item.
						resultCursor = ItemUtils.asItemStack(resultItem);
					} else {
						resultCursor = ItemUtils.increaseItemAmount(cursor, resultItem.getAmount());
					}
					player.setItemOnCursor(resultCursor);
				}

				// Common apply trade:
				this.commonApplyTrade(trade);
			}

			this.updateTrades();
		} else if (action == InventoryAction.DROP_ONE_SLOT || action == InventoryAction.DROP_ALL_SLOT) {
			// Not supported for now, since this might be tricky to accurately reproduce.
			// dropItemNaturally is not equivalent to the player themselves dropping the item and
			// inventoryView.setItem(-999, item) doesn't set the item's thrower (and there is no API
			// to set that, nor does the inventoryView return a reference to the dropped item).
			/*if (isCursorEmpty) {
				if (this.handleTrade(trade)) {
					// Drop result items:
					ItemStack droppedItem = resultItem.clone(); // todo Copy required?
					// todo Call drop event first
					player.getWorld().dropItemNaturally(player.getEyeLocation(), droppedItem);
			
					// Common apply trade:
					this.commonApplyTrade(trade);
				}
			}*/
		} else if (action == InventoryAction.HOTBAR_SWAP) {
			int hotbarButton = clickEvent.getHotbarButton();
			if (hotbarButton >= 0 && hotbarButton <= 8) {
				if (this.handleTrade(trade)) {
					UnmodifiableItemStack resultItem = trade.getTradeEvent().getResultItem();
					boolean resultItemEmpty = ItemUtils.isEmpty(resultItem);

					if (!resultItemEmpty) {
						assert resultItem != null;

						if (!ItemUtils.isEmpty(playerInventory.getItem(hotbarButton))) {
							Log.debug(() -> this.getContext().getLogPrefix()
									+ "Not handling trade: The hotbar slot is not empty.");
							this.onTradeAborted(tradingContext, false);
							return;
						}
					}

					if (!this.finalTradePreparation(trade)) {
						return;
					}

					// We are going to apply the trade now:
					this.preApplyTrade(trade);

					if (!resultItemEmpty) {
						assert resultItem != null;

						// Set the result items to the hotbar slot:
						// No item copy required here.
						playerInventory.setItem(hotbarButton, ItemUtils.asItemStack(resultItem));
					}

					// Common apply trade:
					this.commonApplyTrade(trade);
				}

				this.updateTrades();
			}
		} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			// Shift left or right click: Trades as often as possible (depending on offered items
			// and inventory space) for the current result item:
			// If the current trading recipe is no longer fulfilled, and the currently selected
			// recipe index is 0, it will switch to the next applicable trading recipe, and continue
			// the trading if the new result item is equal to the previous result item.
			// TODO Handling each trade individually, e.g. 64 times one item for one other item, can
			// result in the trade to fail if the chest of a player shop is full, even though it
			// would in principle be possible to trade one time 64 items for 64 items (because
			// removing 64 items will clear a slot of the chest, whereas removing only one item at a
			// time may not). However, determining up front how often the trade can be applied would
			// be tricky, especially since the used trading recipe may change mid trading (at least
			// in vanilla Minecraft). Also, usually the situation may dynamically change in-between
			// the individual trades (especially if plugins or the shopkeepers themselves react to
			// the individual trades), and each trade may have other side effects. So trading one
			// time 64 for 64 items may not be equivalent to trading 64 times one item for one item.
			while (true) {
				if (!this.handleTrade(trade)) {
					// Trade was aborted:
					break;
				}

				UnmodifiableItemStack resultItem = trade.getTradeEvent().getResultItem();
				boolean resultItemEmpty = ItemUtils.isEmpty(resultItem);

				ItemStack[] newPlayerContents = null;

				if (!resultItemEmpty) {
					assert resultItem != null;

					// Check if there is enough space in the player's inventory:
					newPlayerContents = playerInventory.getStorageContents();

					// Minecraft is adding items in reverse container order (starting with hotbar
					// slot 9), so we reverse the player contents accordingly before adding items:
					// Changes write through to the original array.
					List<ItemStack> listView = Arrays.asList(newPlayerContents);
					List<ItemStack> hotbarView = listView.subList(0, 9);
					List<ItemStack> contentsView = listView.subList(9, 36);
					Collections.reverse(hotbarView);
					Collections.reverse(contentsView);

					// No item copy required here:
					if (InventoryUtils.addItems(newPlayerContents, resultItem) != 0) {
						Log.debug(() -> this.getContext().getLogPrefix()
								+ "Not handling trade: Not enough inventory space.");
						this.onTradeAborted(tradingContext, false);
						break;
					}

					// Revert the previous reverse:
					Collections.reverse(hotbarView);
					Collections.reverse(contentsView);
				}

				if (!this.finalTradePreparation(trade)) {
					return;
				}

				// We are going to apply the trade now:
				this.preApplyTrade(trade);

				if (!resultItemEmpty) {
					assert newPlayerContents != null;

					// Apply player inventory changes:
					InventoryUtils.setStorageContents(playerInventory, newPlayerContents);
				}

				// Common apply trade:
				this.commonApplyTrade(trade);

				// Check if we can continue trading:
				var previousTrade = trade;
				trade = this.checkForTrade(tradingContext, true); // Silent
				if (trade == null) {
					// No trade available:
					break;
				}

				// Abort the trading if the active trading recipe has changed:
				// Mimics Minecraft behavior: Minecraft aborts the trading if the result item has
				// changed. We compare the full trading recipe instead, so that players don't
				// accidentally continue trading the same result item but for different costs.
				if (!trade.getTradingRecipe().equals(previousTrade.getTradingRecipe())) {
					break; // The active trade has changed: Abort
				}
			}

			this.updateTrades();
		} else {
			// The inventory action involves the result slot, but does not usually trigger a trade,
			// or is not supported yet.
		}
	}

	private void clearResultSlotForInvalidTrade(MerchantInventory merchantInventory) {
		// TODO This is not working currently. The client updates the result slot contents whenever
		// it receives a slot update from the server.
		/*merchantInventory.setItem(RESULT_ITEM_SLOT_ID, null);
		ItemUtils.updateInventoryLater(merchantInventory);
		Log.debug("Result slot cleared due to invalid trade.");*/
	}

	private @Nullable Trade checkForTrade(TradingContext tradingContext, boolean silent) {
		return this.checkForTrade(tradingContext, silent, silent, true);
	}

	// Checks for an available trade and does some preparation in case a trade is found.
	// Returns null if no trade could be prepared for some reason.
	private @Nullable Trade checkForTrade(
			TradingContext tradingContext,
			boolean silent,
			boolean slientStrictItemComparison,
			boolean isInTradingContext
	) {
		// Start the processing of a new trade attempt:
		tradingContext.startNewTrade();

		Player tradingPlayer = tradingContext.getTradingPlayer();
		MerchantInventory merchantInventory = tradingContext.getMerchantInventory();

		// Use null here instead of air for consistent behavior with previous versions:
		ItemStack offeredItem1 = ItemUtils.getNullIfEmpty(
				merchantInventory.getItem(BUY_ITEM_1_SLOT_ID)
		);
		ItemStack offeredItem2 = ItemUtils.getNullIfEmpty(
				merchantInventory.getItem(BUY_ITEM_2_SLOT_ID)
		);

		// Check for a result item:
		ItemStack resultItem = merchantInventory.getItem(RESULT_ITEM_SLOT_ID);
		if (ItemUtils.isEmpty(resultItem)) {
			if (!silent) {
				Log.debug(() -> this.getContext().getLogPrefix() + "Not handling trade: "
						+ "There is no item in the clicked result slot (no trade available).");
				if (Debug.isDebugging(DebugOptions.emptyTrades)) {
					int selectedRecipeIndex = merchantInventory.getSelectedRecipeIndex();
					Log.debug("Selected trading recipe index: " + selectedRecipeIndex);
					TradingRecipe selectedTradingRecipe = MerchantUtils.getSelectedTradingRecipe(
							merchantInventory
					);
					if (selectedTradingRecipe == null) {
						// Can be null if the merchant has no trades at all.
						Log.debug("No trading recipe selected (merchant has no trades).");
					} else {
						debugLogItemStack("recipeItem1", selectedTradingRecipe.getItem1());
						debugLogItemStack("recipeItem2", selectedTradingRecipe.getItem2());
						debugLogItemStack("recipeResultItem", selectedTradingRecipe.getResultItem());
					}
					debugLogItemStack("offeredItem1", offeredItem1);
					debugLogItemStack("offeredItem2", offeredItem2);
				}
			}
			return null; // No trade available
		}

		// Find (and validate) the recipe Minecraft is using for the trade:
		TradingRecipe tradingRecipe = MerchantUtils.getActiveTradingRecipe(merchantInventory);
		if (tradingRecipe == null) {
			// Unexpected, since there is an item inside the result slot.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				Log.debug(() -> this.getContext().getLogPrefix()
						+ "Not handling trade: Could not find the active trading recipe!");
			}
			this.onTradeAborted(tradingContext, silent);
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}

		// As a safe-guard, check that the result item of the selected recipe actually matches the
		// result item expected by the player:
		UnmodifiableItemStack recipeResultItem = tradingRecipe.getResultItem();
		if (!recipeResultItem.equals(resultItem)) {
			// Unexpected, but may happen if some other plugin modifies the involved trades or
			// items.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				if (Debug.isDebugging()) {
					Log.debug(this.getContext().getLogPrefix() + "Not handling trade: "
							+ "The trade result item does not match the expected item of the "
							+ "active trading recipe!");
					debugLogItemStack("recipeResultItem", recipeResultItem);
					debugLogItemStack("resultItem", resultItem);
				}
			}
			this.onTradeAborted(tradingContext, silent);
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}

		UnmodifiableItemStack requiredItem1 = tradingRecipe.getItem1();
		UnmodifiableItemStack requiredItem2 = tradingRecipe.getItem2();
		assert !ItemUtils.isEmpty(requiredItem1);

		// Minecraft checks both combinations (item1, item2) and (item2, item1) when determining if
		// a trading recipe matches, so we need to determine the used item order for the currently
		// active trading recipe:
		boolean swappedItemOrder = false;
		if (this.matches(offeredItem1, offeredItem2, requiredItem1, requiredItem2)) {
			// Order is as-is.
		} else if (this.matches(offeredItem1, offeredItem2, requiredItem2, requiredItem1)) {
			// Swapped order:
			swappedItemOrder = true;
			ItemStack temp = offeredItem1;
			offeredItem1 = offeredItem2;
			offeredItem2 = temp;
		} else {
			// The used item order could not be determined.
			// This should not happen. But this might for example occur if the
			// FallbackCompatProvider#matches implementation falls back to using the stricter
			// isSimilar for the item comparison and the involved items are not strictly similar.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				Log.debug(() -> this.getContext().getLogPrefix() + "Not handling trade: "
						+ "Could not match the offered items to the active trading recipe!");
			}
			this.onTradeAborted(tradingContext, silent);
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}
		assert offeredItem1 != null;

		if (Settings.useStrictItemComparison) {
			// Verify that the recipe items are perfectly matching (they can still be swapped
			// though):
			boolean item1Similar = ItemUtils.isSimilar(requiredItem1, offeredItem1);
			ItemStack offeredItem2Final = offeredItem2;
			Lazy<Boolean> item2Similar = new Lazy<>(
					() -> ItemUtils.isSimilar(requiredItem2, offeredItem2Final)
			);
			if (!item1Similar || !item2Similar.get()) {
				if (!slientStrictItemComparison) {
					// Feedback message:
					TextUtils.sendMessage(
							tradingPlayer,
							Messages.cannotTradeItemsNotStrictlyMatching
					);

					// Additional debug output:
					if (Debug.isDebugging()) {
						String errorMsg = "The offered items do not strictly match the required items.";
						if (isInTradingContext) {
							this.debugPreventedTrade(errorMsg);
						} else {
							Log.debug(this.getContext().getLogPrefix() + errorMsg);
						}

						Log.debug("Active trading recipe: "
								+ ItemUtils.getSimpleRecipeInfo(tradingRecipe));
						if (!item1Similar) {
							debugLogItemStack("requiredItem1", requiredItem1);
							debugLogItemStack("offeredItem1", offeredItem1);
						}
						if (!item2Similar.get()) {
							debugLogItemStack("requiredItem2", requiredItem2);
							debugLogItemStack("offeredItem2", offeredItem2);
						}
					}
				}
				this.onTradeAborted(tradingContext, slientStrictItemComparison);
				this.clearResultSlotForInvalidTrade(merchantInventory);
				return null;
			}
		}

		// Create and set up a new Trade:
		Trade trade = new Trade(
				tradingContext,
				tradingContext.getTradeCount(),
				tradingRecipe,
				offeredItem1,
				offeredItem2,
				swappedItemOrder
		);
		this.setupTrade(trade);
		tradingContext.setCurrentTrade(trade);
		return trade;
	}

	private boolean matches(
			@Nullable ItemStack offeredItem1,
			@Nullable ItemStack offeredItem2,
			@Nullable UnmodifiableItemStack requiredItem1,
			@Nullable UnmodifiableItemStack requiredItem2
	) {
		int offeredItem1Amount = ItemUtils.getItemStackAmount(offeredItem1);
		int offeredItem2Amount = ItemUtils.getItemStackAmount(offeredItem2);
		int requiredItem1Amount = ItemUtils.getItemStackAmount(requiredItem1);
		int requiredItem2Amount = ItemUtils.getItemStackAmount(requiredItem2);
		return (offeredItem1Amount >= requiredItem1Amount
				&& offeredItem2Amount >= requiredItem2Amount
				&& Compat.getProvider().matches(offeredItem1, requiredItem1)
				&& Compat.getProvider().matches(offeredItem2, requiredItem2));
	}

	protected final void debugPreventedTrade(String reason) {
		Log.debug(() -> this.getContext().getLogPrefix() + "Prevented trade by "
				+ this.getPlayer().getName() + ": " + reason);
	}

	/**
	 * This is called for every newly created {@link TradingContext} and can be used by sub-classes
	 * to set up additional metadata that is relevant for processing any subsequent trades.
	 * 
	 * @param tradingContext
	 *            the trading context, not <code>null</code>
	 */
	protected void setupTradingContext(TradingContext tradingContext) {
		// Callback for subclasses.
	}

	/**
	 * This is called for every newly created {@link Trade} and can be used by sub-classes to set up
	 * additional metadata that is relevant for processing the trade.
	 * 
	 * @param trade
	 *            the trade, not <code>null</code>
	 */
	protected void setupTrade(Trade trade) {
		// Callback for subclasses.
	}

	// Returns false if the trade was aborted.
	private boolean handleTrade(Trade trade) {
		assert trade != null;
		// Shopkeeper-specific checks and preparation:
		if (!this.prepareTrade(trade)) {
			// The trade got cancelled for some shopkeeper-specific reason:
			this.onTradeAborted(trade.getTradingContext(), false);
			return false;
		}

		Player tradingPlayer = trade.getTradingPlayer();

		// Call the trade event:
		ShopkeeperTradeEvent tradeEvent = trade.callTradeEvent();
		this.onPostTradeEvent(trade);

		if (tradeEvent.isCancelled()) {
			Log.debug(() -> this.getContext().getLogPrefix() + "Some plugin cancelled the trade event of "
					+ "player " + tradingPlayer.getName());
			this.onTradeAborted(trade.getTradingContext(), false);
			return false;
		}

		// Assert: The click event and the affected inventories should not get modified during the
		// event!

		// Making sure that the click event is still cancelled:
		InventoryClickEvent clickEvent = trade.getInventoryClickEvent();
		if (!clickEvent.isCancelled()) {
			Log.warning(this.getContext().getLogPrefix()
					+ "Some plugin tried to uncancel the inventory click event of the trade event!");
			clickEvent.setCancelled(true);
		}

		if (tradeEvent.isResultItemAltered()) {
			Log.debug(() -> this.getContext().getLogPrefix()
					+ "Some plugin altered the result item.");
		}
		if (tradeEvent.isReceivedItem1Altered()) {
			Log.debug(() -> this.getContext().getLogPrefix()
					+ "Some plugin altered the first received item.");
		}
		if (tradeEvent.isReceivedItem2Altered()) {
			Log.debug(() -> this.getContext().getLogPrefix()
					+ "Some plugin altered the second received item.");
		}

		return true;
	}

	private void commonApplyTrade(Trade trade) {
		// Update merchant inventory contents:
		MerchantInventory merchantInventory = trade.getMerchantInventory();
		merchantInventory.setItem(RESULT_ITEM_SLOT_ID, null); // Clear result slot, just in case

		TradingRecipe tradingRecipe = trade.getTradingRecipe();
		ItemStack newOfferedItem1 = ItemUtils.decreaseItemAmount(
				trade.getOfferedItem1(),
				ItemUtils.getItemStackAmount(tradingRecipe.getItem1())
		);
		ItemStack newOfferedItem2 = ItemUtils.decreaseItemAmount(
				trade.getOfferedItem2(),
				ItemUtils.getItemStackAmount(tradingRecipe.getItem2())
		);
		// Inform the merchant inventory about the change (updates the active trading recipe and
		// result item):
		boolean itemOrderSwapped = trade.isItemOrderSwapped();
		merchantInventory.setItem(
				itemOrderSwapped ? BUY_ITEM_2_SLOT_ID : BUY_ITEM_1_SLOT_ID, newOfferedItem1
		);
		merchantInventory.setItem(
				itemOrderSwapped ? BUY_ITEM_1_SLOT_ID : BUY_ITEM_2_SLOT_ID, newOfferedItem2
		);

		// TODO Increase uses of corresponding MerchantRecipe?
		// TODO Add support for exp-rewards?
		// TODO Support modifications to the MerchantRecipe's maxUses?

		Player player = trade.getTradingPlayer();

		// Increment 'traded-with-villager' statistic for every trade:
		if (Settings.incrementVillagerStatistics) {
			player.incrementStatistic(Statistic.TRADED_WITH_VILLAGER);
		}

		// Shopkeeper-specific application of the trade:
		this.onTradeApplied(trade);

		// Apply additional trade effects:
		ShopkeeperTradeEvent tradeEvent = trade.getTradeEvent();
		tradeEvent.getTradeEffects().forEach(tradeEffect -> tradeEffect.onTradeApplied(tradeEvent));

		// Call trade completed event:
		ShopkeeperTradeCompletedEvent tradeCompletedEvent = new ShopkeeperTradeCompletedEvent(tradeEvent);
		Bukkit.getPluginManager().callEvent(tradeCompletedEvent);

		// Play a sound effect if this is the first trade triggered by the inventory click:
		boolean silent = (trade.getTradeNumber() > 1);
		if (!silent) {
			Settings.tradeSucceededSound.play(player);
		}

		// Inform listeners:
		this.getTradingListeners().forEach(listener -> listener.onTradeCompleted(trade, silent));

		// Log trade:
		Log.debug(() -> trade.getShopkeeper().getLogPrefix() + "Trade (#" + trade.getTradeNumber()
				+ ") by " + player.getName() + ": " + ItemUtils.getSimpleRecipeInfo(tradingRecipe));

		this.onTradeCompleted(trade);
		this.onTradeOver(trade.getTradingContext());
	}

	/**
	 * Checks whether the given trade can take place and makes any necessary preparations for
	 * applying the trade if it is not cancelled.
	 * <p>
	 * This is called for every trade attempt that a player triggers through a merchant inventory
	 * action. Depending on the inventory action, multiple successive trades (even using different
	 * trading recipes) can be triggered by the same inventory action.
	 * <p>
	 * The corresponding {@link InventoryClickEvent} and the involved inventories (player,
	 * container, etc.) are expected to not be modified between this phase of the trade handling and
	 * the actual application of the trade.
	 * <p>
	 * This is called prior to the {@link ShopkeeperTradeEvent}. If the trade is aborted at this
	 * stage, no trade event is called. If the trade is not aborted, the trade can still get
	 * cancelled or modified during the trade event.
	 * 
	 * @param trade
	 *            the trade, not <code>null</code>
	 * @return <code>true</code> to continue with the given trade, or <code>false</code> to cancel
	 *         the given trade and any successive trades that would be triggered by the same
	 *         inventory click
	 */
	protected boolean prepareTrade(Trade trade) {
		return true;
	}

	/**
	 * This is called after the {@link ShopkeeperTradeEvent} has been called, including for
	 * cancelled trade events, before the outcome of the trade event is handled.
	 * <p>
	 * The trade event can still be modified at this stage.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void onPostTradeEvent(Trade trade) {
	}

	/**
	 * This is called right before a trade is about to be applied.
	 * <p>
	 * This can be used for any final shopkeeper-specific trade preparations or checks that could
	 * still cancel the trade. The {@link ShopkeeperTradeEvent} has already been called at this
	 * stage and its result might already have been taken into account by others checks or
	 * preparations. The trade can still be cancelled at this stage, but the
	 * {@link ShopkeeperTradeEvent} must not be modified anymore. Use
	 * {@link #onPostTradeEvent(Trade)} if you need to modify the trade event.
	 * 
	 * @param trade
	 *            the trade
	 * @return <code>true</code> to continue with the trade, <code>false</code> to cancel the trade
	 */
	protected boolean finalTradePreparation(Trade trade) {
		return true;
	}

	/**
	 * This is called whenever a trade attempt has been cancelled for some reason.
	 * <p>
	 * This is not called for cancelled {@link InventoryClickEvent}s, or inventory actions that are
	 * ignored because they would not result in a trade in vanilla Minecraft either.
	 * <p>
	 * If available, the corresponding {@link Trade} instance can be retrieved via
	 * {@link TradingContext#getCurrentTrade()}. However, trade attempts can also be aborted before
	 * a corresponding valid {@link Trade} instance could be created.
	 * {@link TradingContext#getCurrentTrade()} will then return <code>null</code>.
	 * {@link TradingContext#getTradeCount()} will always reflect the aborted trade attempt.
	 * <p>
	 * This is also called for trades that were aborted by {@link #prepareTrade(Trade)} and can be
	 * used to perform any necessary cleanup.
	 * <p>
	 * When a trade has been cancelled, no further trades will be processed for the same
	 * {@link TradingContext}.
	 * 
	 * @param tradingContext
	 *            the trading context, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to skip any actions that might be noticeable by players on the
	 *            server
	 */
	protected void onTradeAborted(TradingContext tradingContext, boolean silent) {
		Trade trade = tradingContext.getCurrentTrade();
		if (trade != null && trade.isTradeEventCalled()) {
			// Inform custom trade effects of the trade event:
			ShopkeeperTradeEvent tradeEvent = trade.getTradeEvent();
			tradeEvent.getTradeEffects().forEach(tradeEffect -> tradeEffect.onTradeAborted(tradeEvent));
		}

		// Inform listeners:
		this.getTradingListeners().forEach(listener -> listener.onTradeAborted(tradingContext, silent));

		// Play a sound effect, but only if this has been the first trade attempt triggered by the
		// inventory click:
		if (!silent && tradingContext.getTradeCount() == 1) {
			Settings.tradeFailedSound.play(tradingContext.getTradingPlayer());
		}

		this.onTradeOver(tradingContext);
	}

	/**
	 * This is called at the very beginning of applying a trade.
	 * <p>
	 * This can be used to perform any kind of pre-processing or trade application that needs to
	 * happen first.
	 * <p>
	 * At this phase of the trade handling, the trade can no longer be cancelled. Any conditions
	 * that could prevent a trade from getting successfully applied must be checked during
	 * {@link #prepareTrade(Trade)}, the {@link ShopkeeperTradeEvent},
	 * {@link #onPostTradeEvent(Trade)}, or {@link #finalTradePreparation(Trade)} instead.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void preApplyTrade(Trade trade) {
		// Callback for subclasses.
	}

	/**
	 * This is called when a trade is being applied, after any common trade application logic, but
	 * before any {@link TradeEffect#onTradeApplied(ShopkeeperTradeEvent)} calls and before the
	 * {@link ShopkeeperTradeCompletedEvent} is called.
	 * <p>
	 * This can be used to apply any shopkeeper-specific trading behavior.
	 * <p>
	 * At this phase of the trade handling, the trade can no longer be cancelled. Any conditions
	 * that could prevent a trade from getting successfully applied must be checked during
	 * {@link #prepareTrade(Trade)}, the {@link ShopkeeperTradeEvent},
	 * {@link #onPostTradeEvent(Trade)}, or {@link #finalTradePreparation(Trade)} instead.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void onTradeApplied(Trade trade) {
		// Callback for subclasses.
	}

	/**
	 * This is called after a trade has been applied, after the corresponding
	 * {@link ShopkeeperTradeCompletedEvent} is called.
	 * <p>
	 * This can be used to perform any kind of post-processing which needs to happen last.
	 * <p>
	 * At this phase of the trade handling, the trade can no longer be cancelled. Any conditions
	 * that could prevent a trade from getting successfully applied must be checked during
	 * {@link #prepareTrade(Trade)}, the {@link ShopkeeperTradeEvent},
	 * {@link #onPostTradeEvent(Trade)}, or {@link #finalTradePreparation(Trade)} instead.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void onTradeCompleted(Trade trade) {
		// Callback for subclasses.
	}

	/**
	 * Called after a trade attempt has been either aborted or completed.
	 * <p>
	 * This is called very late and can be used to cleanup or reset any state related to the last
	 * processed trade.
	 * <p>
	 * If available, the corresponding {@link Trade} instance can be retrieved via
	 * {@link TradingContext#getCurrentTrade()}. However, trade attempts can also be aborted before
	 * a corresponding valid {@link Trade} instance could be created.
	 * {@link TradingContext#getCurrentTrade()} will then return <code>null</code>.
	 * {@link TradingContext#getTradeCount()} will always reflect the aborted trade attempt.
	 * 
	 * @param tradingContext
	 *            the trading context, not <code>null</code>
	 */
	protected void onTradeOver(TradingContext tradingContext) {
		// Callback for subclasses.
	}

	private static void debugLogItemStack(
			String itemStackName,
			@Nullable UnmodifiableItemStack itemStack
	) {
		debugLogItemStack(itemStackName, ItemUtils.asItemStackOrNull(itemStack));
	}

	private static void debugLogItemStack(
			String itemStackName,
			@ReadOnly @Nullable ItemStack itemStack
	) {
		Object itemStackData = (itemStack != null) ? itemStack : "<empty>";
		Log.debug(ConfigUtils.toConfigYamlWithoutTrailingNewline(itemStackName, itemStackData));
	}
}
