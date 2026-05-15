package com.nisovin.shopkeepers.tradenotifications;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeCompletedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.text.ClickEventText.Action;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Lazy;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.text.MessageArguments;
import com.nisovin.shopkeepers.util.trading.MergedTrades;
import com.nisovin.shopkeepers.util.trading.TradeMerger;
import com.nisovin.shopkeepers.util.trading.TradeMerger.MergeMode;

/**
 * Informs certain players and/or shop owners/members about trades that take place.
 */
public class TradeNotifications implements Listener {

	private static class TradeContext {

		private final MergedTrades mergedTrades;
		// Note: The TradeContext is not kept around, but created and processed immediately when we
		// handle a completed set of merged trades. Changes to the shopkeeper's state or the
		// settings (e.g. the currency items) are therefore not expected to occur while this
		// TradeContext is in use, and can therefore not affect the outcome of these lazily
		// calculated properties.
		private final Lazy<MessageArguments> shopMessageArguments;
		private final Lazy<Map<String, Object>> tradeMessageArguments;
		private final Lazy<Boolean> isResultItemCurrency;

		TradeContext(MergedTrades mergedTrades) {
			this.mergedTrades = mergedTrades;
			shopMessageArguments = new Lazy<>(() -> {
				return ((AbstractShopkeeper) Unsafe.initialized(this).getShopkeeper())
						.getMessageArguments("shop_");
			});
			tradeMessageArguments = new Lazy<>(() -> {
				return createTradeMessageArguments(Unsafe.initialized(this));
			});
			isResultItemCurrency = new Lazy<>(() -> {
				return Currencies.getBase().getItemData()
						.matches(Unsafe.initialized(this).getResultItem());
			});
		}

		public Player getTradingPlayer() {
			return mergedTrades.getInitialTrade().getPlayer();
		}

		public Shopkeeper getShopkeeper() {
			return mergedTrades.getInitialTrade().getShopkeeper();
		}

		/**
		 * Gets the result item of the trades. See {@link ShopkeeperTradeEvent#getTradingRecipe()}
		 * and {@link TradingRecipe#getResultItem()}.
		 * 
		 * @return an unmodifiable view on the result item, not <code>null</code> or empty
		 */
		public UnmodifiableItemStack getResultItem() {
			return mergedTrades.getResultItem();
		}

		public boolean isResultItemCurrency() {
			return isResultItemCurrency.get();
		}

		/**
		 * Gets the first offered item of the trades. See
		 * {@link ShopkeeperTradeEvent#getOfferedItem1()}.
		 * 
		 * @return an unmodifiable view on the first offered item, not <code>null</code> or empty
		 */
		public UnmodifiableItemStack getOfferedItem1() {
			return mergedTrades.getOfferedItem1();
		}

		/**
		 * Gets the second offered item of the trades. See
		 * {@link ShopkeeperTradeEvent#getOfferedItem2()}.
		 * 
		 * @return an unmodifiable view on the second offered item, or <code>null</code>
		 */
		public @Nullable UnmodifiableItemStack getOfferedItem2() {
			return mergedTrades.getOfferedItem2();
		}

		private boolean hasOfferedItem2() {
			return mergedTrades.getInitialTrade().hasOfferedItem2();
		}

		public int getTradeCount() {
			return mergedTrades.getTradeCount();
		}

		public MessageArguments getShopMessageArguments() {
			return shopMessageArguments.get();
		}

		// Returns a modifiable map.
		public Map<String, Object> getTradeMessageArguments() {
			return tradeMessageArguments.get();
		}
	}

	private static Map<String, Object> createTradeMessageArguments(
			TradeContext tradeContext
	) {
		Player player = tradeContext.getTradingPlayer();
		Map<String, Object> msgArgs = new HashMap<>();
		msgArgs.put("player", Unsafe.assertNonNull(player.getName()));
		msgArgs.put(
				"playerId",
				(Supplier<Object>) () -> player.getUniqueId().toString()
		);
		msgArgs.put(
				"resultItem",
				(Supplier<Object>) () -> TextUtils.getItemText(tradeContext.getResultItem())
		);
		msgArgs.put(
				"resultItemAmount",
				(Supplier<Object>) () -> tradeContext.getResultItem().getAmount()
		);
		msgArgs.put(
				"item1",
				(Supplier<Object>) () -> {
					return TextUtils.getItemText(tradeContext.getOfferedItem1());
				}
		);
		msgArgs.put(
				"item1Amount",
				(Supplier<Object>) () -> tradeContext.getOfferedItem1().getAmount()
		);
		msgArgs.put(
				"item2",
				(Supplier<Object>) () -> TextUtils.getItemText(tradeContext.getOfferedItem2())
		);
		msgArgs.put(
				"item2Amount",
				(Supplier<Object>) () -> {
					return ItemUtils.getItemStackAmount(tradeContext.getOfferedItem2());
				}
		);
		return msgArgs;
	}

	// TODO Make these configurable
	private static final long TRADE_MERGE_DURATION_TICKS = 300L; // 15 seconds
	private static final long NEXT_MERGE_TIMEOUT_TICKS = 100L; // 5 seconds

	private final Plugin plugin;
	private final NotificationUserPreferences userPreferences;
	private final TradeMerger tradeMerger;

	private boolean enabled;

	public TradeNotifications(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.userPreferences = new NotificationUserPreferences(plugin);
		this.tradeMerger = new TradeMerger(
				plugin,
				MergeMode.DURATION,
				Unsafe.initialized(this)::onTradesCompleted
		).withMergeDurations(TRADE_MERGE_DURATION_TICKS, NEXT_MERGE_TIMEOUT_TICKS);
	}

	public void onEnable() {
		this.enabled = (Settings.notifyPlayersAboutTrades || Settings.notifyShopMembersAboutTrades);
		if (!enabled) return;

		Bukkit.getPluginManager().registerEvents(this, plugin);
		userPreferences.onEnable();
		tradeMerger.onEnable();
	}

	public void onDisable() {
		if (!enabled) return;
		enabled = false;

		tradeMerger.onDisable();
		userPreferences.onDisable();
		HandlerList.unregisterAll(this);
	}

	public NotificationUserPreferences getUserPreferences() {
		return userPreferences;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTradeCompleted(ShopkeeperTradeCompletedEvent event) {
		tradeMerger.mergeTrade(event.getCompletedTrade());
	}

	private void onTradesCompleted(MergedTrades mergedTrades) {
		TradeContext tradeContext = new TradeContext(mergedTrades);
		this.sendTradeNotifications(tradeContext);
		this.sendShopMemberTradeNotifications(tradeContext);
	}

	private void sendTradeNotifications(TradeContext tradeContext) {
		assert tradeContext != null;
		if (!Settings.notifyPlayersAboutTrades) return;

		var shopkeeper = tradeContext.getShopkeeper();

		String tradeNotificationPermission = ShopkeepersPlugin.TRADE_NOTIFICATIONS_ADMIN;
		if (shopkeeper instanceof PlayerShopkeeper) {
			tradeNotificationPermission = ShopkeepersPlugin.TRADE_NOTIFICATIONS_PLAYER;
		}

		Lazy<Text> tradeNotification = new Lazy<>(
				() -> this.getTradeNotificationMessage(tradeContext)
		);
		for (Player player : Bukkit.getOnlinePlayers()) {
			assert player != null;
			// Avoid notifying the shop owner twice.
			// Note that the shop owner may have deactivated the trade notification for this
			// particular shopkeeper. In this case, they will not receive either type of trade
			// notification.
			if (Settings.notifyShopMembersAboutTrades && this.isShopMember(shopkeeper, player)) {
				continue;
			}
			if (!userPreferences.isNotifyOnTrades(player)) continue;
			if (!PermissionUtils.hasPermission(player, tradeNotificationPermission)) continue;

			// Note: We also send trade notifications for own trades (i.e. when the trading player
			// matches the recipient of the notification).
			TextUtils.sendMessage(player, tradeNotification.get());
			Settings.tradeNotificationSound.play(player);
			this.sendDisableTradeNotificationsHint(player);
		}
	}

	private boolean isShopMember(Shopkeeper shopkeeper, Player player) {
		return shopkeeper instanceof PlayerShopkeeper playerShop
				&& playerShop.isMember(player);
	}

	private Text getTradeNotificationMessage(TradeContext tradeContext) {
		assert tradeContext != null;
		Shopkeeper shopkeeper = tradeContext.getShopkeeper();

		Text message;
		// We avoid checking for specific shop types (e.g. buying shop) and instead check if the
		// result item is currency:
		if (tradeContext.isResultItemCurrency()) {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.buyNotificationTwoItems;
			} else {
				message = Messages.buyNotificationOneItem;
			}
		} else {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.tradeNotificationTwoItems;
			} else {
				message = Messages.tradeNotificationOneItem;
			}
		}

		Text shopText;
		if (shopkeeper.getName().isEmpty()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				shopText = Messages.tradeNotificationPlayerShop;
			} else {
				shopText = Messages.tradeNotificationAdminShop;
			}
		} else {
			if (shopkeeper instanceof PlayerShopkeeper) {
				shopText = Messages.tradeNotificationNamedPlayerShop;
			} else {
				shopText = Messages.tradeNotificationNamedAdminShop;
			}
		}

		Text tradeCountText = Text.EMPTY;
		if (tradeContext.getTradeCount() > 1) {
			tradeCountText = Messages.tradeNotificationTradeCount;
		}

		return this.getTradeNotificationMessage(tradeContext, message, shopText, tradeCountText);
	}

	private Text getTradeNotificationMessage(
			TradeContext tradeContext,
			Text message,
			Text shopText,
			Text tradeCountText
	) {
		MessageArguments shopMsgArgs = tradeContext.getShopMessageArguments();
		Map<String, Object> tradeMsgArgs = tradeContext.getTradeMessageArguments();

		shopText.setPlaceholderArguments(shopMsgArgs);
		// TODO Display more shop information as hover text? Add a click event or insertion text to
		// automatically copy the shop coordinates or id, or insert a teleport command to teleport
		// to the shop?
		tradeMsgArgs.put("shop", shopText);

		tradeCountText.setPlaceholderArguments("count", tradeContext.getTradeCount());
		tradeMsgArgs.put("trade_count", tradeCountText);

		message.setPlaceholderArguments(tradeMsgArgs);
		message.setPlaceholderArguments(shopMsgArgs);
		return message;
	}

	private void sendShopMemberTradeNotifications(TradeContext tradeContext) {
		assert tradeContext != null;
		if (!Settings.notifyShopMembersAboutTrades) return;
		if (!(tradeContext.getShopkeeper() instanceof PlayerShopkeeper playerShop)) return;
		if (!playerShop.isNotifyOnTrades()) return;

		@Nullable Text message = null;

		@Nullable Player owner = playerShop.getOwner();
		if (owner != null && userPreferences.isNotifyOnTrades(owner)) {
			message = this.getOwnerTradeNotificationMessage(tradeContext);
			this.sendShopMemberTradeNotification(owner, message);
		}

		for (var shopMember : playerShop.getMembers()) {
			var memberPlayer = shopMember.getUser().getPlayer();
			if (memberPlayer != null && userPreferences.isNotifyOnTrades(memberPlayer)) {
				if (message == null) {
					message = this.getOwnerTradeNotificationMessage(tradeContext);
				}

				this.sendShopMemberTradeNotification(memberPlayer, message);
			}
		}
	}

	private void sendShopMemberTradeNotification(Player shopMember, Text message) {
		// Note: We also send trade notifications for own trades (i.e. when the trading player
		// matches the recipient of the notification).
		TextUtils.sendMessage(shopMember, message);
		Settings.shopMemberTradeNotificationSound.play(shopMember);
		this.sendDisableTradeNotificationsHint(shopMember);
	}

	private Text getOwnerTradeNotificationMessage(TradeContext tradeContext) {
		assert tradeContext != null;
		Shopkeeper shopkeeper = tradeContext.getShopkeeper();

		// We avoid checking for specific shop types (e.g. buying shop) and instead check if the
		// result item is currency:
		boolean isBuy = tradeContext.isResultItemCurrency();
		Text message;
		if (isBuy) {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.ownerBuyNotificationTwoItems;
			} else {
				message = Messages.ownerBuyNotificationOneItem;
			}
		} else {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.ownerTradeNotificationTwoItems;
			} else {
				message = Messages.ownerTradeNotificationOneItem;
			}
		}

		Text shopText;
		if (isBuy) {
			if (shopkeeper.getName().isEmpty()) {
				shopText = Messages.ownerBuyNotificationShop;
			} else {
				shopText = Messages.ownerBuyNotificationNamedShop;
			}
		} else {
			if (shopkeeper.getName().isEmpty()) {
				shopText = Messages.ownerTradeNotificationShop;
			} else {
				shopText = Messages.ownerTradeNotificationNamedShop;
			}
		}

		Text tradeCountText = Text.EMPTY;
		if (tradeContext.getTradeCount() > 1) {
			tradeCountText = Messages.ownerTradeNotificationTradeCount;
		}

		return this.getTradeNotificationMessage(tradeContext, message, shopText, tradeCountText);
	}

	private void sendDisableTradeNotificationsHint(Player player) {
		if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.NOTIFY_TRADES_PERMISSION)) {
			return;
		}

		// We only send this once per session:
		if (userPreferences.hasReceivedDisableTradeNotificationsHint(player)) return;
		userPreferences.setReceivedDisableTradeNotificationsHint(player, true);

		Text command = Messages.disableTradeNotificationsHintCommand.copy(); // TODO Avoid copy
		Text commandText = Text.clickEvent(Action.SUGGEST_COMMAND, command.toPlainText())
				.next(command).getRoot();
		TextUtils.sendMessage(player, Messages.disableTradeNotificationsHint,
				"command", commandText
		);
	}
}
