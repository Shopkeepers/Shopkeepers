package com.nisovin.shopkeepers.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;

/**
 * This event is called whenever the {@link Settings} or {@link Messages} of the Shopkeepers plugin have changed.
 * <p>
 * This event can for example be used by components to update any of their internally cached or derived configuration
 * values.
 * <p>
 * However, not all components may react to dynamic config changes: Some config changes may require a full plugin reload
 * in order to have an effect.
 */
public class ShopkeepersConfigChangedEvent extends Event {

	public ShopkeepersConfigChangedEvent() {
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
