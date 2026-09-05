package com.nisovin.shopkeepers.api.events;

import java.util.Collection;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;

/**
 * This event is called whenever a player is detected to be inactive and their
 * {@link PlayerShopkeeper shopkeepers} are about to be deleted or reverted to the for-hire state.
 * <p>
 * If the plugin is configured to expire hired player shopkeepers back to their for-hire state, this
 * event is not called for shop owners that only have shopkeepers remaining that are already for
 * hire, because those shopkeepers are not affected by the player inactivity handling.
 */
public class PlayerInactiveEvent extends Event implements Cancellable {

	private final User user;
	private final Collection<? extends PlayerShopkeeper> shopkeepers;
	private boolean cancelled = false;

	/**
	 * Creates a new {@link PlayerInactiveEvent}.
	 * 
	 * @param user
	 *            the inactive user, not <code>null</code>
	 * @param shopkeepers
	 *            the user's owned shopkeepers that are about to be deleted or reverted to the
	 *            for-hire state, not <code>null</code> but can be empty
	 */
	public PlayerInactiveEvent(User user, Collection<? extends PlayerShopkeeper> shopkeepers) {
		Preconditions.checkNotNull(user, "user is null");
		Preconditions.checkNotNull(shopkeepers, "shopkeepers is null");
		this.user = user;
		this.shopkeepers = shopkeepers;
	}

	/**
	 * Gets the inactive {@link User}.
	 * 
	 * @return the user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Gets the user's shopkeepers that are about to be deleted or reverted to the for-hire state.
	 * <p>
	 * This does not include shopkeepers that would expire back to the for-hire state but are
	 * already for hire.
	 * <p>
	 * The returned collection is modifiable: Removing shopkeepers from it will skip their deletion
	 * / reversion to the for-hire state. Adding shopkeepers to the list is not supported.
	 * 
	 * @return the shopkeepers that are about to be deleted or reverted to the for-hire state, not
	 *         <code>null</code> but may be empty
	 */
	public Collection<? extends PlayerShopkeeper> getShopkeepers() {
		return shopkeepers;
	}

	/**
	 * If cancelled, the player's shopkeepers won't be deleted or reverted to the for-hire state.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled, the player's shopkeepers won't be deleted or reverted to the for-hire state.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Gets the {@link HandlerList} of this event.
	 * 
	 * @return the handler list
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
