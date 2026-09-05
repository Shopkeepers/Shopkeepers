package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;

/**
 * This event is called whenever a {@link PlayerShopkeeper} is about to
 * {@link PlayerShopkeeper#expire() expire}.
 * <p>
 * Depending on whether the shopkeeper has a {@link PlayerShopkeeper#getHireCost() hire cost item},
 * expiring it either deletes it, or restores it to its {@link PlayerShopkeeper#isForHire() for
 * hire} state.
 * <p>
 * If this event is cancelled, the shopkeeper does not expire and its expiration is automatically
 * {@link PlayerShopkeeper#resetExpiration() reset} after the event handling, i.e.
 * {@link PlayerShopkeeper#getOwnedSince() owned-since} is reset and the configured expiration
 * duration starts fresh.
 */
public class PlayerShopkeeperExpireEvent extends ShopkeeperEvent implements Cancellable {

	private boolean cancelled = false;

	/**
	 * Creates a new {@link PlayerShopkeeperExpireEvent}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper that is about to expire, not <code>null</code>
	 */
	public PlayerShopkeeperExpireEvent(PlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	/**
	 * Gets the shopkeeper that is about to expire.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	@Override
	public PlayerShopkeeper getShopkeeper() {
		return (PlayerShopkeeper) super.getShopkeeper();
	}

	/**
	 * If cancelled, the shopkeeper does not expire and the expiration is
	 * {@link PlayerShopkeeper#resetExpiration() reset}.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled, the shopkeeper does not expire and the expiration is
	 * {@link PlayerShopkeeper#resetExpiration() reset}.
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
