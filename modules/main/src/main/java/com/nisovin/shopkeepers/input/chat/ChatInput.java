package com.nisovin.shopkeepers.input.chat;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.input.InputManager;
import com.nisovin.shopkeepers.input.InputRequest;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;

/**
 * Manages requests for chat input from players.
 */
public class ChatInput extends InputManager<String> implements Listener {

	public ChatInput(Plugin plugin) {
		// Thread-safe: Chat events can occur asynchronously.
		super(plugin, true);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		Bukkit.getPluginManager().registerEvents(this, plugin);
		// Ensure that our chat event handler is always executed first:
		// Otherwise, we might run into conflicts with other chat plugins that also use the lowest
		// event priority and that might modify the chat message before we were able to process it
		// (for example by injecting color codes).
		EventUtils.enforceExecuteFirst(AsyncPlayerChatEvent.class, EventPriority.LOWEST, this);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		HandlerList.unregisterAll(this);
	}

	// Not ignoring cancelled chat: We process the chat event in all cases (even if the player has
	// been muted, etc.).
	// Priority LOWEST: We intend to cancel the event as early as possible, so that other plugins
	// can ignore it. We also want to retrieve the chat message without other plugins modifying it
	// first.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onChat(AsyncPlayerChatEvent event) {
		// Check if there is a request for the player:
		// If there is a request, it is removed and then subsequently processed.
		Player player = event.getPlayer();
		InputRequest<String> request = this.removeRequest(player);
		if (request == null) return; // There is no request for the player

		// Cancel the event so that other event handlers can ignore it:
		event.setCancelled(true);

		// Get the message:
		String message = event.getMessage();

		// Process the request on the server's main thread:
		SchedulerUtils.runTaskOrOmit(player, () -> request.onInput(message));
	}
}
