package com.nisovin.shopkeepers.player.profile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.player.profile.storage.PlayerStorage;
import com.nisovin.shopkeepers.storage.SKStorage;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.VoidCallable;

public class SKPlayerProfiles implements PlayerProfiles {

	private final SKShopkeepersPlugin plugin;
	private final PlayerJoinQuitListener playerJoinQuitListener = new PlayerJoinQuitListener(this);

	private SKStorage storage;
	private PlayerStorage playerStorage = null;

	public SKPlayerProfiles(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public boolean isEnabled() {
		return (storage != null);
	}

	private void validateEnabled() {
		Validate.State.isTrue(this.isEnabled(), "Trading history (and thereby player profiles) are disabled!");
	}

	public void onEnable() {
		// get storage:
		this.storage = plugin.getStorage();
		Validate.State.notNull(storage, "Cannot enable PlayerProfiles if storage is disabled!");
		this.playerStorage = storage.getPlayerStorage();

		// register listener:
		Bukkit.getPluginManager().registerEvents(playerJoinQuitListener, plugin);

		// update profiles for online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.updateProfile(player);
		}
	}

	public void onDisable() {
		this.validateEnabled();
		// update profiles for online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.updateProfile(player);
		}

		// unregister listener:
		HandlerList.unregisterAll(playerJoinQuitListener);

		this.storage = null;
		this.playerStorage = null;
	}

	void onPlayerJoin(Player player) {
		this.updateProfile(player);
	}

	void onPlayerQuit(Player player) {
		this.updateProfile(player);
	}

	private CompletableFuture<Void> updateProfile(Player player) {
		assert player != null;
		PlayerProfile profile = PlayerProfile.newProfile(player); // current profile
		return this.updateProfile(profile);
	}

	@Override
	public CompletableFuture<PlayerProfile> getProfile(UUID playerUUID) {
		this.validateEnabled();
		Validate.notNull(playerUUID, "Player uuid is null!");
		return storage.addTask(() -> playerStorage.getProfile(playerUUID));
	}

	@Override
	public CompletableFuture<List<PlayerProfile>> getProfiles(String playerName) {
		this.validateEnabled();
		Validate.notEmpty(playerName, "Player name is empty!");
		return storage.addTask(() -> playerStorage.getProfiles(playerName));
	}

	@Override
	public CompletableFuture<Void> updateProfile(PlayerProfile profile) {
		this.validateEnabled();
		Validate.notNull(profile, "Profile is null!");
		return storage.addTask((VoidCallable) () -> playerStorage.updateProfile(profile));
	}

	@Override
	public CompletableFuture<Void> removeProfile(UUID playerUUID) {
		this.validateEnabled();
		Validate.notNull(playerUUID, "Player uuid is null!");
		return storage.addTask((VoidCallable) () -> playerStorage.removeProfile(playerUUID));
	}

	@Override
	public CompletableFuture<Integer> getPlayerCount() {
		this.validateEnabled();
		return storage.addTask(() -> playerStorage.getPlayerCount());
	}
}
