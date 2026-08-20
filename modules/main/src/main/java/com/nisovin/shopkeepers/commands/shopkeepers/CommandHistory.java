package com.nisovin.shopkeepers.commands.shopkeepers;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperUUIDArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperArgument;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.CommandSourceRejectedException;
import com.nisovin.shopkeepers.commands.lib.arguments.AnyFallbackArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.AnyStringFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.NamedArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerByNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.UserArgumentUtils;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.tradelog.data.PlayerRecord;
import com.nisovin.shopkeepers.tradelog.data.ShopRecord;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.tradelog.history.PlayerSelector;
import com.nisovin.shopkeepers.tradelog.history.ShopSelector;
import com.nisovin.shopkeepers.tradelog.history.ShopSelector.ByOwnerUUID;
import com.nisovin.shopkeepers.tradelog.history.ShopSelector.ByShopIdentifier;
import com.nisovin.shopkeepers.tradelog.history.TradingHistoryRequest;
import com.nisovin.shopkeepers.tradelog.history.TradingHistoryResult;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Range;
import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

class CommandHistory extends Command {

	private static final String ARGUMENT_TARGET_PLAYERS = "target-players";
	private static final String ARGUMENT_ALL_PLAYERS = "all-players";
	// Conflicts with all-shops arg alias
	private static final String ARGUMENT_ALL_PLAYERS_ALIAS = "all";
	private static final String ARGUMENT_SELF = "self";
	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_PLAYER_UUID = ARGUMENT_PLAYER + ":uuid";
	private static final String ARGUMENT_PLAYER_NAME = ARGUMENT_PLAYER + ":name";

	private static final String ARGUMENT_TARGET_SHOPS = "target-shops";
	private static final String ARGUMENT_OWN = "own";
	private static final String ARGUMENT_ALL_SHOPS = "all-shops";
	// Conflicts with all-players arg alias
	private static final String ARGUMENT_ALL_SHOPS_ALIAS = "all";
	private static final String ARGUMENT_ADMIN_SHOPS = "admin";
	private static final String ARGUMENT_PLAYER_SHOPS = "player-shops";
	// Conflicts with player arg
	private static final String ARGUMENT_PLAYER_SHOPS_ALIAS = "player";
	private static final String ARGUMENT_SHOP = "shop";
	private static final String ARGUMENT_SHOP_EXISTING = ARGUMENT_SHOP + ":shopkeeper";
	private static final String ARGUMENT_SHOP_UUID = ARGUMENT_SHOP + ":uuid";
	private static final String ARGUMENT_OWNER = "owner";
	private static final String ARGUMENT_OWNER_UUID = ARGUMENT_OWNER + ":uuid";
	private static final String ARGUMENT_OWNER_NAME = ARGUMENT_OWNER + ":name";

	private static final String ARGUMENT_PAGE = "page";

	private static final int ENTRIES_PER_PAGE = 10;

	private final SKShopkeepersPlugin plugin;

	CommandHistory(SKShopkeepersPlugin plugin) {
		super("history");
		this.plugin = plugin;

		// Permission gets checked by testPermission and during execution.

		// Set description:
		this.setDescription(Messages.commandDescriptionHistory);

		// Arguments:
		this.addArgument(new FirstOfArgument(ARGUMENT_TARGET_PLAYERS, Arrays.asList(
				new LiteralArgument(ARGUMENT_SELF),
				new LiteralArgument(ARGUMENT_ALL_PLAYERS, Arrays.asList(ARGUMENT_ALL_PLAYERS_ALIAS))
						.setDisplayName(ARGUMENT_ALL_PLAYERS_ALIAS),
				new FirstOfArgument(ARGUMENT_PLAYER, Arrays.asList(
						new PlayerUUIDArgument(ARGUMENT_PLAYER_UUID), // Accepts any uuid
						// Only accepts names of online players initially, but falls back to any
						// given name (using a fallback to give the following arguments a chance to
						// parse the input first).
						// This ensures that we can also lookup the history of offline players.
						new AnyStringFallback(
								new PlayerByNameArgument(ARGUMENT_PLAYER_NAME)
										.transformed(player -> player.getName())
						)
				), false) // Don't join formats
		), true, true).optional()); // Join formats and reverse

		this.addArgument(new FirstOfArgument(ARGUMENT_TARGET_SHOPS, Arrays.asList(
				// Store as 'all-shops' (to not conflict with ALL_PLAYERS argument), but display as
				// 'all':
				new LiteralArgument(ARGUMENT_ALL_SHOPS, Arrays.asList(ARGUMENT_ALL_SHOPS_ALIAS))
						.setDisplayName(ARGUMENT_ALL_SHOPS_ALIAS),
				new LiteralArgument(ARGUMENT_ADMIN_SHOPS),
				// Avoid conflict with ARGUMENT_PLAYER but still display as 'player':
				new LiteralArgument(ARGUMENT_PLAYER_SHOPS, Arrays.asList(ARGUMENT_PLAYER_SHOPS_ALIAS))
						.setDisplayName(ARGUMENT_PLAYER_SHOPS_ALIAS),
				new LiteralArgument(ARGUMENT_OWN),

				// Note: We allow any uuid and name as fallback to also be able to lookup the
				// history of offline shop owners.
				// Using named arguments because owner uuid/name conflicts with shop uuid/name:
				new NamedArgument<>(new FirstOfArgument(ARGUMENT_OWNER, Arrays.asList(
						new PlayerUUIDArgument(ARGUMENT_OWNER_UUID), // Accepts any uuid
						new PlayerNameArgument(ARGUMENT_OWNER_NAME)), // Accepts any name
						false)
				), // Don't join formats

				// Note: We allow any uuid as fallback to also be able to lookup the history of no
				// longer existing shops. However, lookup by shop name is only supported for
				// existing shops (allowing for proper ambiguity handling without having to query
				// the database).
				new AnyFallbackArgument(
						new NamedArgument<>(new FirstOfArgument(ARGUMENT_SHOP, Arrays.asList(
								new ShopkeeperArgument(ARGUMENT_SHOP_EXISTING),
								// Fallback to any uuid:
								new ShopkeeperUUIDArgument(ARGUMENT_SHOP_UUID)
						))),
						// Fallback to targeted shop:
						new TargetShopkeeperArgument(ARGUMENT_SHOP_EXISTING)
				)
		), true, true).optional()); // Join formats and reverse

		this.addArgument(new PositiveIntegerArgument(ARGUMENT_PAGE).orDefaultValue(1));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;

		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		var tradingHistoryProvider = plugin.getTradingHistoryProvider();
		if (tradingHistoryProvider == null) {
			TextUtils.sendMessage(sender, Messages.historyDisabled);
			return;
		}

		@Nullable Player executingPlayer = (sender instanceof Player) ? (Player) sender : null;
		@Nullable Boolean hasAllPerm = null; // Null if not yet checked

		// Target players:
		boolean hasTargetPlayers = context.has(ARGUMENT_TARGET_PLAYERS);
		boolean allPlayers = context.has(ARGUMENT_ALL_PLAYERS);
		boolean self = context.has(ARGUMENT_SELF);
		@Nullable UUID playerUUID = context.getOrNull(ARGUMENT_PLAYER_UUID);
		@Nullable String playerName = context.getOrNull(ARGUMENT_PLAYER_NAME);

		// Target shops:
		boolean hasTargetShops = context.has(ARGUMENT_TARGET_SHOPS);
		boolean allShops = context.has(ARGUMENT_ALL_SHOPS);
		boolean adminShops = context.has(ARGUMENT_ADMIN_SHOPS);
		boolean playerShops = context.has(ARGUMENT_PLAYER_SHOPS);
		boolean ownShops = context.has(ARGUMENT_OWN);
		@Nullable Shopkeeper existingShop = context.getOrNull(ARGUMENT_SHOP_EXISTING);
		@Nullable UUID shopUUID = context.getOrNull(ARGUMENT_SHOP_UUID);
		@Nullable UUID ownerUUID = context.getOrNull(ARGUMENT_OWNER_UUID);
		@Nullable String ownerName = context.getOrNull(ARGUMENT_OWNER_NAME);

		int page = context.get(ARGUMENT_PAGE);

		// Fill in missing arguments with defaults:
		if (!hasTargetPlayers) {
			// history _ x -> history all x
			allPlayers = true;
			hasTargetPlayers = true;
			if (!hasTargetShops && executingPlayer != null) {
				// If executed by a player: history _ _ -> history all own (instead of history all
				// all)
				ownShops = true;
				hasTargetShops = true;
			}
		}
		assert hasTargetPlayers;

		if (!hasTargetShops) {
			// history x _ -> history x all
			allShops = true;
			hasTargetShops = true;
		}
		assert hasTargetShops;

		// Map to selectors:
		boolean ownHistory = false;

		PlayerSelector playerSelector;
		if (allPlayers) {
			playerSelector = PlayerSelector.ALL;
		} else if (self) {
			if (executingPlayer == null) {
				// Not executed by a player:
				throw new CommandSourceRejectedException(
						Text.of("You must be a player in order to use the argument 'self'!")
				);
			}

			playerSelector = new PlayerSelector.ByUUID(
					executingPlayer.getUniqueId(),
					executingPlayer.getName()
			);
			ownHistory = true;
		} else if (playerUUID != null) {
			@Nullable String selectorPlayerName = null;
			if (executingPlayer != null && playerUUID.equals(executingPlayer.getUniqueId())) {
				ownHistory = true;
				selectorPlayerName = executingPlayer.getName();
			}
			playerSelector = new PlayerSelector.ByUUID(playerUUID, selectorPlayerName);
		} else {
			assert playerName != null;
			var playerUser = UserArgumentUtils.resolveUserByName(sender, playerName);
			if (playerUser == null) {
				// Abort. Sender feedback was already handled.
				return;
			}

			playerSelector = new PlayerSelector.ByUUID(playerUser.getUniqueId(), playerUser.getName());

			if (executingPlayer != null
					&& playerUser.getUniqueId().equals(executingPlayer.getUniqueId())) {
				ownHistory = true;
			}
		}

		ShopSelector shopSelector = null;
		if (allShops) {
			shopSelector = ShopSelector.ALL;
		} else if (adminShops) {
			shopSelector = ShopSelector.ADMIN_SHOPS;
		} else if (playerShops) {
			shopSelector = ShopSelector.PLAYER_SHOPS;
		} else if (ownShops) {
			if (executingPlayer == null) {
				// Not executed by a player:
				throw new CommandSourceRejectedException(
						Text.of("You must be a player in order to use the argument 'own'!")
				);
			}

			shopSelector = new ShopSelector.ByOwnerUUID(
					executingPlayer.getUniqueId(),
					executingPlayer.getName()
			);
			ownHistory = true;
		} else if (existingShop != null) {
			if (PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ADMIN_PERMISSION)) {
				hasAllPerm = true;
				shopSelector = new ShopSelector.ByExistingShop(existingShop);
			} else {
				if (existingShop instanceof PlayerShopkeeper) {
					// Even if the shop is currently owned by the executing player, it might have
					// been owned a different player earlier. Likewise: If the shop is currently
					// owned by a different player, it might have been owned by the executing player
					// in the past.
					// If the executing player does not have the permission to view the full
					// history, filter by owner:
					if (executingPlayer != null) {
						shopSelector = new ShopSelector.ByExistingShop(
								existingShop,
								executingPlayer.getUniqueId(),
								executingPlayer.getName()
						);
						ownHistory = true;
					} else {
						// 'all'-permission is required for non-player executor:
						throw this.noPermissionException();
					}
				} else {
					// If the target shop is not a player shop, the 'all'-permission is required in
					// all cases:
					throw this.noPermissionException();
				}
			}
		} else if (shopUUID != null) {
			// Note: We don't known if the target shop is an admin or player shop. If the executing
			// player does not have the permission to view the full history, we filter by owner.
			// This will not find any trades if the shop was never owned by the executing player.
			if (PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ADMIN_PERMISSION)) {
				hasAllPerm = true;
				shopSelector = new ShopSelector.ByShopUUID(shopUUID);
			} else {
				if (executingPlayer != null) {
					shopSelector = new ShopSelector.ByShopUUID(
							shopUUID,
							executingPlayer.getUniqueId(),
							executingPlayer.getName()
					);
					ownHistory = true;
				} else {
					// 'all'-permission is required for non-player executor:
					throw this.noPermissionException();
				}
			}
		} else if (ownerUUID != null) {
			@Nullable String selectorOwnerName = null;
			if (executingPlayer != null && ownerUUID.equals(executingPlayer.getUniqueId())) {
				ownHistory = true;
				selectorOwnerName = executingPlayer.getName();
			}

			shopSelector = new ShopSelector.ByOwnerUUID(ownerUUID, selectorOwnerName);
		} else if (ownerName != null) {
			var ownerUser = UserArgumentUtils.resolveUserByName(sender, ownerName);
			if (ownerUser == null) {
				// Abort. Sender feedback was already handled.
				return;
			}

			shopSelector = new ShopSelector.ByOwnerUUID(ownerUser.getUniqueId(), ownerUser.getName());

			if (executingPlayer != null
					&& ownerUser.getUniqueId().equals(executingPlayer.getUniqueId())) {
				ownHistory = true;
			}
		} else {
			throw Validate.State.error("Missing shop selector!");
		}
		assert shopSelector != null;

		// Check permission:
		if (hasAllPerm == null || !hasAllPerm) {
			if (ownHistory) {
				assert executingPlayer != null;
				this.checkPermission(sender, ShopkeepersPlugin.HISTORY_OWN_PERMISSION);
			} else {
				if (hasAllPerm == null) {
					this.checkPermission(sender, ShopkeepersPlugin.HISTORY_ADMIN_PERMISSION);
				} else {
					// We already checked the permission and know that it is false:
					assert !hasAllPerm;
					throw this.noPermissionException();
				}
			}
		} // Else: We already know that the sender has the 'all'-permission, continue.

		// Create request and retrieve history:
		Range range = new Range.PageRange(page, ENTRIES_PER_PAGE);
		TradingHistoryRequest historyRequest = new TradingHistoryRequest(playerSelector, shopSelector, range);

		final long historyFetchStart = System.nanoTime();
		tradingHistoryProvider.getTradingHistory(historyRequest)
				.thenAcceptAsync(historyResult -> {
					Validate.State.notNull(historyRequest, "historyResult is null!");
					assert historyResult != null;

					// Send history result:
					final long historyPrintStart = System.nanoTime();
					sendTradingHistory(sender, historyRequest, historyResult);

					if (Debug.isDebugging(DebugOptions.commands)) {
						final long end = System.nanoTime();
						final long fetchDuration = historyPrintStart - historyFetchStart;
						final long printDuration = end - historyPrintStart;
						sender.sendMessage("Fetch: " + TimeUnit.NANOSECONDS.toMillis(fetchDuration) + " ms"
								+ " | Print: " + TimeUnit.NANOSECONDS.toMillis(printDuration) + " ms");
					}
				}, SKShopkeepersPlugin.getInstance().getSyncExecutor())
				.exceptionally(exception -> {
					// Error case:
					// TODO Localize?
					TextUtils.sendMessage(sender, Text.parse("&cError: Could not retrieve the trading history!"));
					Log.severe("Error while retrieving trading history!", exception);
					return null;
				});
	}

	private void sendTradingHistory(CommandSender sender, TradingHistoryRequest historyRequest, TradingHistoryResult historyResult) {
		assert sender != null && historyRequest != null && historyResult != null;
		PlayerSelector playerSelector = historyRequest.playerSelector;
		ShopSelector shopSelector = historyRequest.shopSelector;
		int totalTrades = historyResult.getTotalTradesCount();
		int startIndex = historyRequest.range.getStartIndex(totalTrades);
		int page = (startIndex / ENTRIES_PER_PAGE) + 1;
		int maxPage = Math.max(1, (int) Math.ceil((double) totalTrades / ENTRIES_PER_PAGE));

		// Header:
		// Prepare message arguments:
		Map<String, Object> headerArgs = new HashMap<>();
		headerArgs.put("page", page);
		headerArgs.put("maxPage", maxPage);
		headerArgs.put("tradesCount", totalTrades);

		Text headerPlayers;
		if (playerSelector == PlayerSelector.ALL) {
			headerPlayers = Messages.historyHeaderAllPlayers;
		} else if (playerSelector instanceof PlayerSelector.ByUUID playerByUuidSelector) {
			var playerName = playerByUuidSelector.getPlayerName();
			var playerUuid = playerByUuidSelector.getPlayerUUID();

			headerPlayers = Messages.historyHeaderSpecificPlayer;
			headerPlayers.setPlaceholderArguments("player", TextUtils.getPlayerText(playerName, playerUuid));
		} else {
			throw new IllegalStateException("Unexpected player selector type: "
					+ playerSelector.getClass().getName());
		}
		headerArgs.put("players", headerPlayers);

		Text headerShops;
		if (shopSelector == ShopSelector.ALL) {
			headerShops = Messages.historyHeaderAllShops;
		} else if (shopSelector == ShopSelector.ADMIN_SHOPS) {
			headerShops = Messages.historyHeaderAdminShops;
		} else if (shopSelector == ShopSelector.PLAYER_SHOPS) {
			headerShops = Messages.historyHeaderPlayerShops;
		} else if (shopSelector instanceof ByOwnerUUID byOwnerSelector) {
			var ownerUuid = byOwnerSelector.getOwnerUUID();
			var ownerName = byOwnerSelector.getOwnerName();

			headerShops = Messages.historyHeaderAllOwnedShops;
			headerShops.setPlaceholderArguments("owner", TextUtils.getPlayerText(ownerName, ownerUuid));
		} else if (shopSelector instanceof ByShopIdentifier byShopSelector) {
			// Specific shop:
			Text shopIdentifier = byShopSelector.getShopIdentifier();

			var ownerUuid = byShopSelector.getOwnerUUID();
			if (ownerUuid == null) {
				headerShops = Messages.historyHeaderSpecificShop;
				headerShops.setPlaceholderArguments("shop", shopIdentifier);
			} else {
				// Also filtered by owner:
				var ownerName = byShopSelector.getOwnerName();

				headerShops = Messages.historyHeaderSpecificOwnedShop;
				headerShops.setPlaceholderArguments("shop", shopIdentifier);
				headerShops.setPlaceholderArguments("owner", TextUtils.getPlayerText(ownerName, ownerUuid));
			}
		} else {
			throw new IllegalStateException("Unexpected shop selector type: "
					+ shopSelector.getClass().getName());
		}

		headerArgs.put("shops", headerShops);

		TextUtils.sendMessage(sender, Messages.historyHeader, headerArgs);

		// Print logged trade entries:
		if (totalTrades == 0) {
			TextUtils.sendMessage(sender, Messages.historyNoTradesFound);
		} else {
			Map<String, Object> entryArgs = new HashMap<>();
			int index = startIndex;
			for (TradeRecord trade : historyResult.getTrades()) {
				entryArgs.clear();

				PlayerRecord player = trade.getPlayer();
				Instant timestamp = trade.getTimestamp();
				ShopRecord shop = trade.getShop();
				@Nullable PlayerRecord shopOwner = shop.getOwner();
				UnmodifiableItemStack item1 = trade.getItem1();
				@Nullable UnmodifiableItemStack item2 = trade.getItem2();
				UnmodifiableItemStack resultItem = trade.getResultItem();

				Text entryMsg = item2 == null ? Messages.historyEntryOneItem
						: Messages.historyEntryTwoItems;

				// Prepare message arguments:
				entryArgs.put("index", index + 1);
				entryArgs.put("player", TextUtils.getPlayerText(player));
				entryArgs.put("item1Amount", item1.getAmount());
				entryArgs.put("item1", TextUtils.getItemText(item1));
				entryArgs.put("resultItemAmount", resultItem.getAmount());
				entryArgs.put("resultItem", TextUtils.getItemText(resultItem));
				var formattedTimestamp = DerivedSettings.dateTimeFormatter.format(timestamp);
				entryArgs.put("timeAgo", Text.hoverEvent(Text.of(formattedTimestamp))
						.childText(TimeUtils.getTimeAgoString(timestamp)).buildRoot());
				if (item2 != null) {
					entryArgs.put("item2Amount", item2.getAmount());
					entryArgs.put("item2", TextUtils.getItemText(item2));
				}

				Text tradeCountText = Text.EMPTY;
				if (trade.getTradeCount() > 1) {
					tradeCountText = Messages.historyEntryTradeCount;
					tradeCountText.setPlaceholderArguments("count", trade.getTradeCount());
				}

				entryArgs.put("trade_count", tradeCountText);

				// Note: We don't have access to the shop name here, only the type, owner and uuid.
				// TODO However, for still existing shops, we could retrieve additional information.
				Text entryShopDisplayText;
				if (shopOwner == null) {
					// Trade with admin shop:
					entryShopDisplayText = Messages.historyEntryAdminShop;
				} else {
					// Trade with player shop:
					entryShopDisplayText = Messages.historyEntryPlayerShop;
					entryShopDisplayText.setPlaceholderArguments("owner", TextUtils.getPlayerText(shopOwner));
				}

				entryArgs.put("shop", TextUtils.getShopText(entryShopDisplayText, shop.getUniqueId()));

				TextUtils.sendMessage(sender, entryMsg, entryArgs);
				++index;
			}
		}

		// TODO Next/prev page buttons (if SpigotFeatures is available)
	}
}
