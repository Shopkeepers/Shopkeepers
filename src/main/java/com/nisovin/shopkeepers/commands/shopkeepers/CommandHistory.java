package com.nisovin.shopkeepers.commands.shopkeepers;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperNameArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperUUIDArgument;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperArgument;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.CommandSourceRejectedException;
import com.nisovin.shopkeepers.commands.lib.arguments.AnyFallbackArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.AnyStringFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.NamedArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerByNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TransformedArgument;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.history.HistoryRequest;
import com.nisovin.shopkeepers.history.HistoryResult;
import com.nisovin.shopkeepers.history.ItemInfo;
import com.nisovin.shopkeepers.history.LoggedTrade;
import com.nisovin.shopkeepers.history.PlayerSelector;
import com.nisovin.shopkeepers.history.ShopInfo;
import com.nisovin.shopkeepers.history.ShopSelector;
import com.nisovin.shopkeepers.history.ShopSelector.ByOwner;
import com.nisovin.shopkeepers.history.ShopSelector.ByShop;
import com.nisovin.shopkeepers.history.TradingHistory;
import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.text.HoverEventText;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.text.TextBuilder;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.MapUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.TimeUtils;
import com.nisovin.shopkeepers.util.Validate;

class CommandHistory extends Command {

	private static final String ARGUMENT_TARGET_PLAYERS = "target-players";
	private static final String ARGUMENT_ALL_PLAYERS = "all-players";
	private static final String ARGUMENT_ALL_PLAYERS_ALIAS = "all"; // conflicts with all-shops arg alias
	private static final String ARGUMENT_SELF = "self";
	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_PLAYER_UUID = ARGUMENT_PLAYER + ":uuid";
	private static final String ARGUMENT_PLAYER_NAME = ARGUMENT_PLAYER + ":name";

	private static final String ARGUMENT_TARGET_SHOPS = "target-shops";
	private static final String ARGUMENT_OWN = "own";
	private static final String ARGUMENT_ALL_SHOPS = "all-shops";
	private static final String ARGUMENT_ALL_SHOPS_ALIAS = "all"; // conflicts with all-players arg alias
	private static final String ARGUMENT_ADMIN_SHOPS = "admin";
	private static final String ARGUMENT_PLAYER_SHOPS = "player-shops";
	private static final String ARGUMENT_PLAYER_SHOPS_ALIAS = "player"; // conflicts with player arg
	private static final String ARGUMENT_SHOP = "shop";
	private static final String ARGUMENT_SHOP_UUID = ARGUMENT_SHOP + ":uuid";
	private static final String ARGUMENT_SHOP_NAME = ARGUMENT_SHOP + ":name";
	private static final String ARGUMENT_OWNER = "owner";
	private static final String ARGUMENT_OWNER_UUID = ARGUMENT_OWNER + ":uuid";
	private static final String ARGUMENT_OWNER_NAME = ARGUMENT_OWNER + ":name";

	private static final String ARGUMENT_PAGE = "page";

	private static final int ENTRIES_PER_PAGE = 10;

	private final TradingHistory tradingHistory;

	CommandHistory(TradingHistory tradingHistory) {
		super("history");
		this.tradingHistory = tradingHistory;

		// permission gets checked by testPermission and during execution

		// set description:
		this.setDescription(Settings.msgCommandDescriptionHistory);

		// arguments:
		this.addArgument(new OptionalArgument<>(new FirstOfArgument(ARGUMENT_TARGET_PLAYERS, Arrays.asList(
				new LiteralArgument(ARGUMENT_SELF),
				new LiteralArgument(ARGUMENT_ALL_PLAYERS, Arrays.asList(ARGUMENT_ALL_PLAYERS_ALIAS)).setDisplayName(ARGUMENT_ALL_PLAYERS_ALIAS),
				new FirstOfArgument(ARGUMENT_PLAYER, Arrays.asList(
						new PlayerUUIDArgument(ARGUMENT_PLAYER_UUID), // accepts any uuid
						// only accepts names of online players initially, but falls back to any given name (using a
						// fallback to give the following arguments a chance to parse the input first)
						new AnyStringFallback(new TransformedArgument<>(
								new PlayerByNameArgument(ARGUMENT_PLAYER_NAME),
								(player) -> player.getName()))),
						false) // don't join formats
		), true, true))); // join formats and reverse

		this.addArgument(new OptionalArgument<>(new FirstOfArgument(ARGUMENT_TARGET_SHOPS, Arrays.asList(
				// store as 'all-shops' (to not conflict with ALL_PLAYERS argument), but display as 'all':
				new LiteralArgument(ARGUMENT_ALL_SHOPS, Arrays.asList(ARGUMENT_ALL_SHOPS_ALIAS)).setDisplayName(ARGUMENT_ALL_SHOPS_ALIAS),
				new LiteralArgument(ARGUMENT_ADMIN_SHOPS),
				// avoid conflict with ARGUMENT_PLAYER but still display as 'player':
				new LiteralArgument(ARGUMENT_PLAYER_SHOPS, Arrays.asList(ARGUMENT_PLAYER_SHOPS_ALIAS)).setDisplayName(ARGUMENT_PLAYER_SHOPS_ALIAS),
				new LiteralArgument(ARGUMENT_OWN),

				// using named arguments because owner uuid/name conflicts with shop uuid/name:
				new NamedArgument<>(new FirstOfArgument(ARGUMENT_OWNER, Arrays.asList(
						new PlayerUUIDArgument(ARGUMENT_OWNER_UUID), // accepts any uuid
						new PlayerNameArgument(ARGUMENT_OWNER_NAME)), // accepts any name
						false)
				), // don't join formats

				new AnyFallbackArgument(
						new NamedArgument<>(new AnyFallbackArgument(new AnyFallbackArgument(
								new ShopkeeperArgument(ARGUMENT_SHOP),
								new ShopkeeperUUIDArgument(ARGUMENT_SHOP_UUID)),// fallback to any uuid
								new ShopkeeperNameArgument(ARGUMENT_SHOP_NAME))), // fallback to any name
						// fallback to targeted shop:
						new TargetShopkeeperArgument(ARGUMENT_SHOP, TargetShopkeeperFilter.ANY)
				)
		), true, true))); // join formats and reverse

		this.addArgument(new DefaultValueFallback<>(new PositiveIntegerArgument(ARGUMENT_PAGE), 1));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ALL_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		if (!tradingHistory.isEnabled()) {
			TextUtils.sendMessage(sender, Settings.msgHistoryDisabled);
			return;
		}

		Player executingPlayer = (sender instanceof Player) ? (Player) sender : null;
		Boolean hasAllPerm = null; // null if not yet checked

		// target players:
		boolean hasTargetPlayers = context.has(ARGUMENT_TARGET_PLAYERS);
		boolean allPlayers = context.has(ARGUMENT_ALL_PLAYERS);
		boolean self = context.has(ARGUMENT_SELF);
		UUID playerUUID = context.get(ARGUMENT_PLAYER_UUID);
		String playerName = context.get(ARGUMENT_PLAYER_NAME);

		// target shops:
		boolean hasTargetShops = context.has(ARGUMENT_TARGET_SHOPS);
		boolean allShops = context.has(ARGUMENT_ALL_SHOPS);
		boolean adminShops = context.has(ARGUMENT_ADMIN_SHOPS);
		boolean playerShops = context.has(ARGUMENT_PLAYER_SHOPS);
		boolean ownShops = context.has(ARGUMENT_OWN);
		Shopkeeper existingShop = context.get(ARGUMENT_SHOP);
		UUID shopUUID = context.get(ARGUMENT_SHOP_UUID);
		String shopName = context.get(ARGUMENT_SHOP_NAME);
		UUID ownerUUID = context.get(ARGUMENT_OWNER_UUID);
		String ownerName = context.get(ARGUMENT_OWNER_NAME);

		int page = context.get(ARGUMENT_PAGE);

		// fill in missing arguments with defaults:
		if (!hasTargetPlayers) {
			// history _ x -> history all x
			allPlayers = true;
			hasTargetPlayers = true;
			if (!hasTargetShops && executingPlayer != null) {
				// if executed by a player: history _ _ -> history all own (instead of history all all)
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

		// map to selectors:
		boolean ownHistory = false;

		PlayerSelector playerSelector;
		if (allPlayers) {
			playerSelector = PlayerSelector.ALL;
		} else if (self) {
			if (executingPlayer == null) {
				// not executed by a player:
				throw new CommandSourceRejectedException(Text.of("You must be a player in order to use the argument 'self'!"));
			}
			playerSelector = new PlayerSelector.ByUUID(executingPlayer.getUniqueId());
			ownHistory = true;
		} else if (playerUUID != null) {
			playerSelector = new PlayerSelector.ByUUID(playerUUID);
			if (executingPlayer != null && playerUUID.equals(executingPlayer.getUniqueId())) {
				ownHistory = true;
			}
		} else {
			assert playerName != null;
			Validate.State.isTrue(playerName != null, "Missing target player!");
			playerSelector = new PlayerSelector.ByName(playerName);
			if (executingPlayer != null && playerName.equals(executingPlayer.getName())) {
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
				// not executed by a player:
				throw new CommandSourceRejectedException(Text.of("You must be a player in order to use the argument 'own'!"));
			}
			shopSelector = new ShopSelector.ByOwnerUUID(executingPlayer.getUniqueId());
			ownHistory = true;
		} else if (existingShop != null) {
			if (PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ALL_PERMISSION)) {
				hasAllPerm = true;
				shopSelector = new ShopSelector.ByExistingShop(existingShop);
			} else {
				if (existingShop instanceof PlayerShopkeeper) {
					// Even if the shop is currently owned by the executing player, it might have been owned a different
					// player earlier. Likewise: If the shop is currently owned by a different player, it might have
					// been owned by the executing player is the past.
					// If the executing player does not have the permission to view the full history, filter by owner:
					if (executingPlayer != null) {
						shopSelector = new ShopSelector.ByExistingShop(existingShop, executingPlayer.getUniqueId());
						ownHistory = true;
					} else {
						// 'all'-permission is required for non-player executor:
						throw this.noPermissionException();
					}
				} else {
					// if the target shop is not a player shop, the 'all'-permission is required in all cases:
					throw this.noPermissionException();
				}
			}
		} else if (shopUUID != null) {
			// Note: We don't known if the target shop is an admin or player shop. If the executing player does not have
			// the permission to view the full history, we filter by owner. This will not find any trades if the shop
			// was never owned by the executing player.
			if (PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ALL_PERMISSION)) {
				hasAllPerm = true;
				shopSelector = new ShopSelector.ByShopUUID(shopUUID);
			} else {
				if (executingPlayer != null) {
					shopSelector = new ShopSelector.ByShopUUID(shopUUID, executingPlayer.getUniqueId());
					ownHistory = true;
				} else {
					// 'all'-permission is required for non-player executor:
					throw this.noPermissionException();
				}
			}
		} else if (shopName != null) {
			// Note: We don't known if the target shop is an admin or player shop. If the executing player does not have
			// the permission to view the full history, we filter by owner. This will not find any trades if the shop
			// was never owned by the executing player.
			if (PermissionUtils.hasPermission(sender, ShopkeepersPlugin.HISTORY_ALL_PERMISSION)) {
				hasAllPerm = true;
				shopSelector = new ShopSelector.ByShopName(shopName);
			} else {
				if (executingPlayer != null) {
					shopSelector = new ShopSelector.ByShopName(shopName, executingPlayer.getUniqueId());
					ownHistory = true;
				} else {
					// 'all'-permission is required for non-player executor:
					throw this.noPermissionException();
				}
			}
		} else if (ownerUUID != null) {
			shopSelector = new ShopSelector.ByOwnerUUID(ownerUUID);
			if (executingPlayer != null && ownerUUID.equals(executingPlayer.getUniqueId())) {
				ownHistory = true;
			}
		} else if (ownerName != null) {
			shopSelector = new ShopSelector.ByOwnerName(ownerName);
			if (executingPlayer != null && ownerName.equals(executingPlayer.getName())) {
				ownHistory = true;
			}
		} else {
			Validate.State.error("Missing shop selector!");
		}

		// check permission:
		if (hasAllPerm == null || !hasAllPerm) {
			if (ownHistory) {
				assert executingPlayer != null;
				this.checkPermission(sender, ShopkeepersPlugin.HISTORY_OWN_PERMISSION);
			} else {
				if (hasAllPerm == null) {
					this.checkPermission(sender, ShopkeepersPlugin.HISTORY_ALL_PERMISSION);
				} else {
					// we already checked the permission and know that it is false
					assert !hasAllPerm;
					throw this.noPermissionException();
				}
			}
		} // else: we already know that the sender has the 'all'-permission, continue

		// create request and retrieve history:
		HistoryRequest.Range range = new HistoryRequest.Range.PageRange(page, ENTRIES_PER_PAGE);
		HistoryRequest historyRequest = new HistoryRequest(playerSelector, shopSelector, range);

		final long historyFetchStart = System.nanoTime();
		CompletableFuture<HistoryResult> task = tradingHistory.getTradingHistory(historyRequest);
		task.whenComplete((historyResult, exception) -> SchedulerUtils.runOnMainThreadOrOmit(SKShopkeepersPlugin.getInstance(), new Runnable() {
			@Override
			public void run() {
				Throwable historyException = exception;
				if (historyException == null && historyResult == null) {
					// exception for the case that the returned history result is null:
					historyException = new Exception("Returned trading history is null!");
				}
				if (historyException != null) {
					// error case:
					TextUtils.sendMessage(sender, Text.color(ChatColor.RED).text("Error: Could not retrieve trading history!"));
					Log.severe("Error while retrieving trading history!", historyException);
					return;
				}
				assert historyResult != null;

				// send history result:
				final long historyPrintStart = System.nanoTime();
				sendTradingHistory(sender, historyRequest, historyResult);
				if (Settings.isDebugging(Settings.DebugOptions.commands)) {
					final long end = System.nanoTime();
					sender.sendMessage("Printing results: " + ((end - historyPrintStart) / 1000000.0D) + " ms");
					sender.sendMessage("Total (fetch + printing): " + ((end - historyFetchStart) / 1000000.0D) + " ms");
				}
			}
		}));
	}

	private void sendTradingHistory(CommandSender sender, HistoryRequest historyRequest, HistoryResult historyResult) {
		assert sender != null && historyRequest != null && historyResult != null;
		// TODO update all player profiles by taking the local information into account (first/last seen)? Or prefer
		// displaying the information as stored inside the database?
		PlayerSelector playerSelector = historyRequest.playerSelector;
		ShopSelector shopSelector = historyRequest.shopSelector;
		int totalTrades = historyResult.getTotalTradesCount();
		int startIndex = historyRequest.range.getStartIndex(totalTrades);
		int page = (startIndex / ENTRIES_PER_PAGE) + 1;
		int maxPage = Math.max(1, (int) Math.ceil((double) totalTrades / ENTRIES_PER_PAGE));
		PlayerProfile owner = historyResult.getOwner(); // can be null

		// header:
		// prepare message arguments:
		Map<String, Object> headerArgs = new HashMap<>();
		headerArgs.put("page", page);
		headerArgs.put("maxPage", maxPage);
		headerArgs.put("tradesCount", totalTrades);

		if (playerSelector != PlayerSelector.ALL) {
			// can be null if no profile has been found for the specified player:
			PlayerProfile tradingPlayer = historyResult.getTradingPlayer();
			String playerId = (tradingPlayer != null) ? tradingPlayer.getName() : playerSelector.getPlayerIdentifier();
			headerArgs.put("player", this.getPlayerHoverEvent(tradingPlayer).childText(playerId).buildRoot());
		}

		Text headerMsg;
		if (shopSelector == ShopSelector.ALL) {
			if (playerSelector == PlayerSelector.ALL) {
				headerMsg = Settings.msgHistoryHeaderAllShops;
			} else {
				headerMsg = Settings.msgHistoryHeaderPlayerWithAllShops;
			}
		} else if (shopSelector == ShopSelector.ADMIN_SHOPS) {
			if (playerSelector == PlayerSelector.ALL) {
				headerMsg = Settings.msgHistoryHeaderAdminShops;
			} else {
				headerMsg = Settings.msgHistoryHeaderPlayerWithAdminShops;
			}
		} else if (shopSelector == ShopSelector.PLAYER_SHOPS) {
			if (playerSelector == PlayerSelector.ALL) {
				headerMsg = Settings.msgHistoryHeaderPlayerShops;
			} else {
				headerMsg = Settings.msgHistoryHeaderPlayerWithPlayerShops;
			}
		} else if (shopSelector instanceof ByOwner) {
			// owner might be null if no matching profile was found:
			String ownerId = (owner != null) ? owner.getName() : ((ByOwner) shopSelector).getOwnerIdentifier();
			headerArgs.put("owner", this.getPlayerHoverEvent(owner).childText(ownerId).buildRoot());

			if (playerSelector == PlayerSelector.ALL) {
				headerMsg = Settings.msgHistoryHeaderOwnedShops;
			} else {
				headerMsg = Settings.msgHistoryHeaderPlayerWithOwnedShops;
			}
		} else if (shopSelector instanceof ByShop) {
			// specific shop:
			ByShop byShopSelector = ((ByShop) shopSelector);
			String shopIdentifier = byShopSelector.getShopIdentifier();
			headerArgs.put("shop", shopIdentifier); // TODO hover info

			if (byShopSelector.getOwnerUUID() == null) {
				if (playerSelector == PlayerSelector.ALL) {
					headerMsg = Settings.msgHistoryHeaderShop;
				} else {
					headerMsg = Settings.msgHistoryHeaderPlayerWithShop;
				}
			} else {
				// also filtered by owner:
				// owner might be null if no matching profile was found (if the profile for the the executing player
				// wasn't created yet or got removed for some reason):
				String ownerId = (owner != null) ? owner.getName() : byShopSelector.getOwnerUUID().toString();
				headerArgs.put("owner", this.getPlayerHoverEvent(owner).childText(ownerId).buildRoot());

				if (playerSelector == PlayerSelector.ALL) {
					headerMsg = Settings.msgHistoryHeaderOwnedShop;
				} else {
					headerMsg = Settings.msgHistoryHeaderPlayerWithOwnedShop;
				}
			}
		} else {
			throw new IllegalStateException("Unexpected shop selector type: " + shopSelector.getClass().getName());
		}
		TextUtils.sendMessage(sender, headerMsg, headerArgs);

		// TODO add shop hover information -> requires changing the messages!

		// print logged trade entries:
		if (totalTrades == 0) {
			TextUtils.sendMessage(sender, Settings.msgHistoryNoTradesFound);
		} else {
			Map<String, Object> entryArgs = new HashMap<>();
			int index = startIndex;
			for (LoggedTrade loggedTrade : historyResult.getLoggedTrades()) {
				PlayerProfile player = loggedTrade.getPlayer();
				Instant timestamp = loggedTrade.getTimestamp();
				ShopInfo shop = loggedTrade.getShop();
				PlayerProfile shopOwner = shop.getOwner(); // can be null
				ItemInfo item1 = loggedTrade.getItem1();
				ItemInfo item2 = loggedTrade.getItem2(); // can be null
				ItemInfo resultItem = loggedTrade.getResultItem();

				// prepare message arguments:
				entryArgs.put("index", (index + 1));
				entryArgs.put("player", this.getPlayerHoverEvent(player).childText(player.getName()).buildRoot());
				entryArgs.put("item1Amount", item1.getAmount());
				entryArgs.put("item1", this.getItemTextArgument(item1));
				entryArgs.put("resultItemAmount", resultItem.getAmount());
				entryArgs.put("resultItem", this.getItemTextArgument(resultItem));
				entryArgs.put("timeAgo", Text.hoverEvent(Text.of(Settings.formatTimestamp(timestamp)))
						.childText(TimeUtils.getTimeAgoString(timestamp)).buildRoot());
				if (item2 != null) {
					entryArgs.put("item2Amount", item2.getAmount());
					entryArgs.put("item2", this.getItemTextArgument(item2));
				}

				Text message;
				if (shopOwner == null) {
					// trade with admin shop:
					if (item2 == null) {
						message = Settings.msgHistoryEntryAdminShopOneItem;
					} else {
						message = Settings.msgHistoryEntryAdminShopTwoItems;
					}
				} else {
					// trade with player shop:
					entryArgs.put("owner", this.getPlayerHoverEvent(shopOwner).childText(shopOwner.getName()).buildRoot());
					if (item2 == null) {
						message = Settings.msgHistoryEntryPlayerShopOneItem;
					} else {
						message = Settings.msgHistoryEntryPlayerShopTwoItems;
					}
				}

				TextUtils.sendMessage(sender, message, entryArgs);
				++index;
			}
		}

		// TODO next/prev page buttons (if SpigotFeatures is available)
	}

	private TextBuilder getPlayerHoverEvent(PlayerProfile profile) {
		Text hoverText;
		if (profile == null) {
			hoverText = Settings.msgPlayerProfileNotFound;
		} else {
			hoverText = Settings.msgPlayerProfile;
			hoverText.setPlaceholderArguments(MapUtils.createMap(
					"name", profile.getName(),
					"uuid", profile.getUniqueId().toString(),
					"firstSeen", Settings.formatTimestamp(profile.getFirstSeen()),
					"lastSeen", Settings.formatTimestamp(profile.getLastSeen())
			));
		}
		return Text.hoverEvent(hoverText);
	}

	private Text getItemTextArgument(ItemInfo itemInfo) {
		assert itemInfo != null;
		// Note: Since ItemStack display names can be arbitrary, we prefer using the item type name for the text. The
		// display name can be retrieved from the message's item hover event if available.

		// This doesn't match the english item names perfectly, but it should be good enough for our purpose
		// TODO retrieve the english item names from the server-included language file?
		String itemTypeName = ItemUtils.getPrettyMaterialName(itemInfo.getItemType());

		// Note: Not using translatable item names, because those will likely not match the rest of the message, because
		// they depend on the player's language rather than the server's.
		// TODO add setting to use translatable item names?
		// TODO Since the language is currently set by the server (not per-user), maybe allow admins to provide
		// minecraft language files from which to read the translated item names (so they can match the rest of the
		// servers language)?
		TextBuilder hoverEvent = this.getItemHoverEvent(itemInfo); // can be null
		if (hoverEvent != null) {
			return hoverEvent.childText(itemTypeName).buildRoot();
		} else {
			return Text.text(itemTypeName);
		}
	}

	private TextBuilder getItemHoverEvent(ItemInfo itemInfo) {
		assert itemInfo != null;
		ItemStack itemStack = itemInfo.getItemStack(); // can be null
		String itemSNBT = NMSManager.getProvider().getItemSNBT(itemStack); // can be null
		if (itemSNBT == null) return null;
		return Text.hoverEvent(HoverEventText.Action.SHOW_ITEM, Text.of(itemSNBT));
	}
}
