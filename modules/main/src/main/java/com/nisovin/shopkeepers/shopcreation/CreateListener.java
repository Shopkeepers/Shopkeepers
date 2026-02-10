package com.nisovin.shopkeepers.shopcreation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.interaction.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MutableLong;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Handles the usage of the shop creation item.
 */
class CreateListener implements Listener {

	// We sometimes receive two interact events (right click air and then left click air) at the
	// same time when a player right-clicks air while being in survival mode and targeting a block
	// that is slightly further away than the block interaction range allows.
	// By only handling one interaction event within a small time span, we ignore the additional
	// left click air event.
	private static final long INTERACTION_DELAY_MILLIS = 50L;

	private final SKShopkeepersPlugin plugin;
	private final ContainerSelection containerSelection;
	private final ShopkeeperPlacement shopkeeperPlacement;

	// By player UUID:
	private final Map<UUID, MutableLong> lastHandledPlayerInteractionsMillis = new HashMap<>();

	// Cached value whether the shop creation item selection message is enabled:
	private boolean shopCreationItemSelectedMessageEnabled;

	CreateListener(
			SKShopkeepersPlugin plugin,
			ContainerSelection containerSelection,
			ShopkeeperPlacement shopkeeperPlacement
	) {
		this.plugin = plugin;
		this.containerSelection = containerSelection;
		this.shopkeeperPlacement = shopkeeperPlacement;
	}

	void onEnable() {
		shopCreationItemSelectedMessageEnabled = !Messages.creationItemSelected.isPlainTextEmpty();
		Bukkit.getPluginManager().registerEvents(this, plugin);
		// Ensure that our interact event handler is always executed first, even after plugin
		// reloads:
		// In order to not change the order among the already registered event handlers of our own
		// plugin, we move them all together to the front of the handler list.
		EventUtils.enforceExecuteFirst(PlayerInteractEvent.class, EventPriority.LOWEST, plugin);
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
		lastHandledPlayerInteractionsMillis.clear();
		ShopCreationItemSelectionTask.onDisable();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID uniqueId = player.getUniqueId();

		// Cleanup:
		lastHandledPlayerInteractionsMillis.remove(uniqueId);
		ShopCreationItemSelectionTask.cleanupAndCancel(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onItemHeld(PlayerItemHeldEvent event) {
		// If the shop creation item selection message is disabled, we can skip all of this:
		if (!shopCreationItemSelectedMessageEnabled) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack newItemInHand = player.getInventory().getItem(event.getNewSlot());
		if (!ShopCreationItem.isShopCreationItem(newItemInHand)) {
			return;
		}

		if (!plugin.hasCreatePermission(player)) {
			// The player cannot create any shopkeepers at all.
			return;
		}

		// To avoid message spam, we only print the shop creation item selection message if the
		// player is still holding the item after a short delay. Any already active task is aborted.
		ShopCreationItemSelectionTask.start(plugin, player);
	}

	// See LivingEntityShopListener for a reasoning on why we handle this event the way we do:
	// We handle and cancel this event on LOWEST priority so that other plugins (e.g. protection
	// plugins) can ignore it.
	// This event handler might check container access by calling another dummy interaction event.
	// Handling and canceling this event as early as possible is also required so that other event
	// handlers (for example of protection plugins) can ignore the first event and don't handle the
	// interaction twice.
	// To resolve conflicts with other event handlers at LOWEST priority, we ignore the event if it
	// is already cancelled. This includes, for example, the interaction handler for sign shops
	// within this plugin, which takes precedence over this event handler.
	// To further reduce conflicts with other event handlers at LOWEST priority, including after
	// dynamic plugin reloads, we forcefully move our event handler(s) to the front of the handler
	// list.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// Ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// Ignore if the player isn't right-clicking, or left-clicking air:
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR
				&& action != Action.RIGHT_CLICK_BLOCK
				&& action != Action.LEFT_CLICK_AIR) {
			return;
		}

		// Make sure that the used item is the shop creation item:
		ItemStack itemInHand = event.getItem();
		var shopCreationItem = new ShopCreationItem(itemInHand);
		if (!shopCreationItem.isShopCreationItem()) {
			return;
		}

		Player player = event.getPlayer();
		Log.debug(() -> "Player " + player.getName()
				+ " is interacting with the shop creation item: action=" + action
				+ ", hand=" + event.getHand());

		// Capture event's cancellation state:
		Result useItemInHand = event.useItemInHand();

		// Prevent regular usage:
		// If we actually act on the interaction later, we always cancel the event. However, if we
		// don't cancel the event here, players are able to use the item like normal when holding it
		// in their off hand.
		// TODO Are there items which would require canceling the event for all left clicks or
		// physical interaction as well?
		if (Settings.preventShopCreationItemRegularUsage
				&& !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Log.debug("  Preventing normal shop creation item usage");
			event.setCancelled(true);
		}

		// Ignore off-hand interactions from this point on:
		// -> The item will only act as shop creation item if it is held in the main hand.
		if (event.getHand() != EquipmentSlot.HAND) {
			Log.debug("  Ignoring off-hand interaction");
			return;
		}

		// Ignore if already cancelled. This resolves conflicts with other event handlers that also
		// run at LOWEST priority, such as for example Shopkeeper's sign shop listener.
		if (useItemInHand == Result.DENY) {
			Log.debug("  Ignoring already cancelled item interaction");
			return;
		}

		// This is one of the interactions that we want to act on. We always cancel the event to
		// prevent any vanilla or subsequent actions of other event handlers:
		event.setCancelled(true);

		// Ignore the event if we already handled an interaction for the player recently:
		// This filters out duplicate left click air interaction events that we sometimes receive
		// alongside a right click air interaction event (TODO This assumes that we first receive
		// the intended right click event, before we receive the additional left click event).
		// Note: We do this after the above checks, so that we always cancel the interaction with
		// the shop creation item, even if we don't actually handle the interaction then. This also
		// avoids unrelated debug spam, since we only print debug output for interactions with the
		// shop creation item.
		UUID uniqueId = player.getUniqueId();
		MutableLong lastHandledInteractionMillis = lastHandledPlayerInteractionsMillis.computeIfAbsent(
				uniqueId,
				uuid -> new MutableLong()
		);
		assert lastHandledInteractionMillis != null;
		long nowMillis = System.currentTimeMillis();
		long millisSinceLastInteraction = (nowMillis - lastHandledInteractionMillis.getValue());
		if (millisSinceLastInteraction < INTERACTION_DELAY_MILLIS) {
			Log.debug(() -> "Ignoring interaction of player " + player.getName()
					+ ": Last handled interaction was " + millisSinceLastInteraction + " ms ago.");
			return;
		}
		// We only update the last interaction timestamp when we actually handle the event. This
		// prevents that delays between individual events accumulate and that we always handle the
		// event after the intended delay is over.
		lastHandledInteractionMillis.setValue(nowMillis);

		// Get shop type:
		ShopType<?> shopType = plugin.getShopTypeRegistry().getSelection(player);
		boolean isShopTypeFixed = false;
		// Get shop object type:
		ShopObjectType<?> shopObjectType = plugin.getShopObjectTypeRegistry().getSelection(player);
		boolean isShopObjectTypeFixed = false;

		if (shopType == null || shopObjectType == null) {
			// The player cannot create any shops at all:
			TextUtils.sendMessage(player, Messages.noPermission);
			return;
		}

		// Check if the item specifies a fixed shop and/or object type:
		var itemShopTypeId = shopCreationItem.getShopTypeId();
		if (itemShopTypeId != null) {
			var itemShopType = plugin.getShopTypeRegistry().get(itemShopTypeId);
			if (itemShopType == null) {
				TextUtils.sendMessage(player, Messages.commandShopTypeArgumentInvalid,
						"argument", itemShopTypeId
				);
				return;
			}

			// Only player shops can currently be created with the shop creation item:
			if (!(itemShopType instanceof PlayerShopType)) {
				TextUtils.sendMessage(player, Messages.commandShopTypeArgumentNoPlayerShop,
						"argument", itemShopTypeId
				);
				return;
			}

			// Note: Disabled types and permission checks are handled during subsequent shop
			// creation.

			shopType = itemShopType;
			isShopTypeFixed = true;
		}

		var itemShopObjectTypeId = shopCreationItem.getObjectTypeId();
		if (itemShopObjectTypeId != null) {
			var itemShopObjectType = plugin.getShopObjectTypeRegistry().get(itemShopObjectTypeId);
			if (itemShopObjectType == null) {
				TextUtils.sendMessage(player, Messages.commandShopObjectTypeArgumentInvalid,
						"argument", itemShopObjectTypeId
				);
				return;
			}

			// Note: Disabled types and permission checks are handled during subsequent shop
			// creation.

			shopObjectType = itemShopObjectType;
			isShopObjectTypeFixed = true;
		}

		// Check what the player is doing with the shop creation item in hand:
		if (action == Action.RIGHT_CLICK_AIR) {
			if (player.isSneaking() ^ Settings.invertShopTypeAndObjectTypeSelection) {
				// Cycle shop objects:
				if (!isShopObjectTypeFixed) {
					plugin.getShopObjectTypeRegistry().selectNext(player);
				}
			} else {
				// Cycle shopkeeper types:
				if (!isShopTypeFixed) {
					plugin.getShopTypeRegistry().selectNext(player);
				}
			}
		} else if (action == Action.LEFT_CLICK_AIR) {
			if (player.isSneaking() ^ Settings.invertShopTypeAndObjectTypeSelection) {
				// Cycle shop objects backwards:
				if (!isShopObjectTypeFixed) {
					plugin.getShopObjectTypeRegistry().selectPrevious(player);
				}
			} else {
				// Cycle shopkeeper types backwards:
				if (!isShopTypeFixed) {
					plugin.getShopTypeRegistry().selectPrevious(player);
				}
			}
		} else if (action == Action.RIGHT_CLICK_BLOCK) {
			Block clickedBlock = Unsafe.assertNonNull(event.getClickedBlock());

			Block selectedContainer = containerSelection.getSelectedContainer(player);
			// Validate old selected container:
			if (selectedContainer != null
					&& !ShopContainers.isSupportedContainer(selectedContainer.getType())) {
				containerSelection.selectContainer(player, null);
				selectedContainer = null;
			}

			// Handle container selection:
			boolean isContainerSelection = false;
			if (!clickedBlock.equals(selectedContainer)) {
				if (ShopContainers.isSupportedContainer(clickedBlock.getType())) {
					isContainerSelection = true;
					// Check if the container can be used for a shop:
					if (containerSelection.validateContainer(player, clickedBlock)) {
						// Select container:
						containerSelection.selectContainer(player, clickedBlock);
						TextUtils.sendMessage(player, Messages.containerSelected);
					}
				} else if (ItemUtils.isContainer(clickedBlock.getType())) {
					// Player clicked a type of container which cannot be used for shops:
					isContainerSelection = true;
					TextUtils.sendMessage(player, Messages.unsupportedContainer);
				}
			}

			if (!isContainerSelection) {
				// Player shop creation:
				if (selectedContainer == null) {
					// Clicked a location without having a container selected:
					TextUtils.sendMessage(player, Messages.mustSelectContainer);
					return;
				}
				// Checked above already:
				assert ShopContainers.isSupportedContainer(selectedContainer.getType());

				// Only player shops can be selected currently. TODO Change that?
				assert shopType instanceof PlayerShopType;

				// Determine spawn location:
				BlockFace clickedBlockFace = event.getBlockFace();
				Location spawnLocation = shopkeeperPlacement.determineSpawnLocation(
						player,
						clickedBlock,
						clickedBlockFace
				);

				// Create player shopkeeper:
				ShopCreationData creationData = PlayerShopCreationData.create(
						player,
						(PlayerShopType<?>) shopType,
						shopObjectType,
						spawnLocation,
						clickedBlockFace,
						selectedContainer
				);
				Shopkeeper shopkeeper = plugin.handleShopkeeperCreation(creationData);
				if (shopkeeper != null) {
					// Shopkeeper creation was successful:

					// Reset selected container:
					containerSelection.selectContainer(player, null);

					// Manually remove creation item from player's hand after this event is
					// processed:
					SchedulerUtils.runTaskOrOmit(player, () -> {
						ItemStack newItemInMainHand = ItemUtils.decreaseItemAmount(itemInHand, 1);
						player.getInventory().setItemInMainHand(newItemInMainHand);
					});
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockDispense(BlockDispenseEvent event) {
		if (Settings.preventShopCreationItemRegularUsage
				&& ShopCreationItem.isShopCreationItem(event.getItem())) {
			Log.debug(() -> "Preventing dispensing of shop creation item at "
					+ TextUtils.getLocationString(event.getBlock()));
			event.setCancelled(true);
			// TODO Drop item instead.
			// TODO Only prevent it for items that have a special dispense behavior.
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		this.handleEntityInteraction(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		this.handleEntityInteraction(event);
	}

	private void handleEntityInteraction(PlayerInteractEntityEvent event) {
		if (!Settings.preventShopCreationItemRegularUsage) {
			return;
		}

		Player player = event.getPlayer();
		// We check the permission first since this check is fast:
		if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			return;
		}

		ItemStack itemInHand = player.getInventory().getItem(event.getHand());
		if (!ShopCreationItem.isShopCreationItem(itemInHand)) {
			return;
		}

		// Prevent the entity interaction:
		// TODO Only prevent the entity interaction if the item actually has a special entity
		// interaction behavior.
		// The interaction result may also depend on the interacted entity. However, there is no
		// Bukkit API yet to check for this.
		Log.debug(() -> {
			if (event instanceof PlayerInteractAtEntityEvent) {
				return "Preventing interaction at entity with shop creation item for player "
						+ TextUtils.getPlayerString(player);
			} else {
				return "Preventing entity interaction with shop creation item for player "
						+ TextUtils.getPlayerString(player);
			}
		});
		event.setCancelled(true);
	}

	// Rate limiting of debug messages related to the PrepareAnvilEvent:
	private static final long ANVIL_DEBUG_MESSAGE_THROTTLE_MILLIS = TimeUnit.SECONDS.toMillis(5L);
	private @Nullable UUID lastAnvilDebugMessagePlayer = null;
	private long lastAnvilDebugMessageMillis = 0L;

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPrepareAnvilEvent(PrepareAnvilEvent event) {
		if (!Settings.preventShopCreationItemRegularUsage) return;
		if (ItemUtils.isEmpty(event.getResult())) {
			// The PrepareAnvilEvent is called multiple times. We only need to handle it when a
			// result item is set.
			return;
		}

		Inventory anvilInventory = event.getInventory();
		if (!ShopCreationItem.isShopCreationItem(anvilInventory.getItem(0))
				&& !ShopCreationItem.isShopCreationItem(anvilInventory.getItem(1))) {
			// No shop creation item involved.
			// Note: We only prevent the renaming of the shop creation item, not its creation. I.e.
			// we do not check the result item here. The shop creation item could intentionally be
			// set up to be an item that can be created with an anvil.
			return;
		}

		if (Settings.debug) {
			// Rate limiting of debug messages (this event is called for every text change):
			Player player = (Player) event.getView().getPlayer();
			UUID playerUniqueId = player.getUniqueId();
			long nowMillis = System.currentTimeMillis();
			if (!playerUniqueId.equals(lastAnvilDebugMessagePlayer)
					|| (nowMillis - lastAnvilDebugMessageMillis) > ANVIL_DEBUG_MESSAGE_THROTTLE_MILLIS) {
				lastAnvilDebugMessagePlayer = playerUniqueId;
				lastAnvilDebugMessageMillis = nowMillis;
				Log.debug(() -> "Preventing renaming of shop creation item by " + player.getName()
						+ " (debug output is throttled)");
			}
		}

		event.setResult(null);
		InventoryUtils.updateInventoryLater(anvilInventory);
		// TODO Inform the player? (This would require some per-player throttling)
	}
}
