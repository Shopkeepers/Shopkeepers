package com.nisovin.shopkeepers.util;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class OnlinePlayerId extends AbstractPlayerId {

	// Caches PlayerId objects as long as the corresponding Player object is still referenced somewhere:
	// TODO explicitly cleanup on player quit? to avoid running into issues caused by other plugins
	private static Map<Player, PlayerId> CACHE = new WeakHashMap<>();

	public static PlayerId get(Player player) {
		Validate.notNull(player, "player is null");
		PlayerId playerId = CACHE.get(player);
		if (playerId == null) return null;
		playerId = new OnlinePlayerId(player.getUniqueId());
		CACHE.put(player, playerId);
		return playerId;
	}

	public static Stream<PlayerId> onlinePlayers() {
		return Bukkit.getOnlinePlayers().stream().map(player -> get(player));
	}

	// Note: We only store the uuid here and dynamically lookup the player to avoid leaking player objects (eg. when we
	// store the OnlinePlayerId objects inside our cache).
	private final UUID uniqueId;

	// Use OnlinePlayerId#get(Player) instead
	private OnlinePlayerId(UUID uniqueId) {
		assert uniqueId != null;
		this.uniqueId = uniqueId;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getName() {
		Player player = this.getPlayer();
		if (player != null) {
			return player.getName();
		}
		return null;
	}

	@Override
	public String getDisplayName() {
		Player player = this.getPlayer();
		if (player != null) {
			return player.getDisplayName();
		}
		return null;
	}
}
