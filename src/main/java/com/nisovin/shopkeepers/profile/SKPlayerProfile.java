package com.nisovin.shopkeepers.profile;

import java.time.Instant;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.nisovin.shopkeepers.api.profile.PlayerProfile;
import com.nisovin.shopkeepers.util.Validate;

public class SKPlayerProfile implements PlayerProfile {

	/**
	 * Creates a new {@link PlayerProfile} for the given player.
	 * 
	 * @param player
	 *            the player
	 * @return the player profile
	 */
	public static PlayerProfile newProfile(OfflinePlayer player) {
		Validate.notNull(player, "Player is null!");
		return newUpdatedProfile(player.getUniqueId(), player.getName(), Instant.EPOCH, Instant.EPOCH, player);
	}

	/**
	 * Creates a new {@link PlayerProfile} for the specified player.
	 * <p>
	 * This takes into account the player information currently available on this server.
	 * 
	 * @param playerUUID
	 *            the player's unique id, not <code>null</code>
	 * @param playerName
	 *            the player's last known name, can be <code>null</code>
	 * @return the player profile
	 */
	public static PlayerProfile newProfile(UUID playerUUID, String playerName) {
		Validate.notNull(playerUUID, "Player UUID is null!");
		Validate.notEmpty(playerName, "Player name is null or empty!");
		return newUpdatedProfile(playerUUID, playerName, Instant.EPOCH, Instant.EPOCH);
	}

	/**
	 * Creates a new {@link PlayerProfile} that merges the given player information with the information currently
	 * available on this server.
	 * 
	 * @param playerUUID
	 *            the player's unique id, not <code>null</code>
	 * @param playerName
	 *            the player's last known name, can be <code>null</code>
	 * @param firstSeen
	 *            the timestamp of when the player was first seen on the server, not <code>null</code>
	 * @param lastSeen
	 *            the timestamp of when the player was last seen on the server, not <code>null</code>
	 * @return the player profile
	 */
	private static PlayerProfile newUpdatedProfile(UUID playerUUID, String playerName, Instant firstSeen, Instant lastSeen) {
		assert playerUUID != null && playerName != null && firstSeen != null && lastSeen != null;
		assert !firstSeen.isAfter(lastSeen); // firstSeen <= lastSeen

		// This first checks for an online player:
		OfflinePlayer localPlayer = Bukkit.getOfflinePlayer(playerUUID); // non-blocking (no name lookup), not null
		assert localPlayer != null;
		return newUpdatedProfile(playerUUID, playerName, firstSeen, lastSeen, localPlayer);
	}

	/**
	 * Creates a new {@link PlayerProfile} that merges the given player information with the information available from
	 * the given player.
	 * 
	 * @param playerUUID
	 *            the player's unique id, not <code>null</code>
	 * @param playerName
	 *            the player's last known name, can be <code>null</code>
	 * @param firstSeen
	 *            the timestamp of when the player was first seen on the server, not <code>null</code>
	 * @param lastSeen
	 *            the timestamp of when the player was last seen on the server, not <code>null</code>
	 * @param localPlayer
	 *            the player whose information gets merged
	 * @return the player profile
	 */
	private static PlayerProfile newUpdatedProfile(UUID playerUUID, String playerName, Instant firstSeen, Instant lastSeen, OfflinePlayer localPlayer) {
		assert playerUUID != null && playerName != null && firstSeen != null && lastSeen != null && localPlayer != null;
		assert !firstSeen.isAfter(lastSeen); // firstSeen <= lastSeen

		long localFirstSeenMillis = localPlayer.getFirstPlayed(); // 0 (epoch) if unknown
		if (localFirstSeenMillis != 0) {
			Instant localFirstSeen = Instant.ofEpochMilli(localFirstSeenMillis);
			if (localFirstSeen.isBefore(firstSeen)) {
				firstSeen = localFirstSeen;
			}
		}

		if (localPlayer.isOnline()) {
			Instant now = Instant.now();
			assert !firstSeen.isAfter(now); // firstSeen <= now
			assert !lastSeen.isAfter(now); // lastSeen <= now

			lastSeen = now;
			String localPlayerName = localPlayer.getName();
			assert localPlayerName != null;
			playerName = localPlayerName;
		} else {
			long localLastSeenMillis = localPlayer.getLastPlayed(); // 0 (epoch) if unknown
			if (localLastSeenMillis != 0) {
				Instant localLastSeen = Instant.ofEpochMilli(localLastSeenMillis);
				if (localLastSeen.isAfter(lastSeen)) {
					lastSeen = localLastSeen;

					String localPlayerName = localPlayer.getName(); // null if unknown
					if (localPlayerName != null) {
						playerName = localPlayerName;
					}
				}
			}
		}

		return new SKPlayerProfile(playerUUID, playerName, firstSeen, lastSeen);
	}

	private final UUID uniqueId; // not null
	private final String name; // can be null
	private final Instant firstSeen; // not null, epoch (1970-01-01) if unknown
	private final Instant lastSeen; // not null, epoch (1970-01-01) if unknown

	/**
	 * Creates a new {@link PlayerProfile}.
	 * 
	 * @param playerUUID
	 *            the player's unique id, not <code>null</code>
	 * @param playerName
	 *            the player's last known name, can be <code>null</code>
	 * @param firstSeen
	 *            the timestamp of when the player was first seen on the server, not <code>null</code>
	 * @param lastSeen
	 *            the timestamp of when the player was last seen on the server, not <code>null</code>
	 */
	public SKPlayerProfile(UUID playerUUID, String playerName, Instant firstSeen, Instant lastSeen) {
		Validate.notNull(playerUUID, "Player UUID is null!");
		Validate.notNull(firstSeen, "FirstSeen is null!");
		Validate.notNull(lastSeen, "LastSeen is null!");
		Validate.isTrue(!firstSeen.isAfter(lastSeen), "FirstSeen cannot be after lastSeen!");

		this.uniqueId = playerUUID;
		this.name = playerName;
		this.firstSeen = firstSeen;
		this.lastSeen = lastSeen;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Instant getFirstSeen() {
		return firstSeen;
	}

	@Override
	public Instant getLastSeen() {
		return lastSeen;
	}

	@Override
	public PlayerProfile update() {
		return newUpdatedProfile(uniqueId, name, firstSeen, lastSeen);
	}

	// TODO toSimpleString (containing uuid and name, user-friendly representation)

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerProfile [uniqueId=");
		builder.append(uniqueId);
		builder.append(", name=");
		builder.append(name);
		builder.append(", firstSeen=");
		builder.append(firstSeen);
		builder.append(", lastSeen=");
		builder.append(lastSeen);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstSeen.hashCode();
		result = prime * result + lastSeen.hashCode();
		result = prime * result + name.hashCode();
		result = prime * result + uniqueId.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SKPlayerProfile)) return false;
		SKPlayerProfile other = (SKPlayerProfile) obj;
		if (!uniqueId.equals(other.uniqueId)) return false;
		if (!firstSeen.equals(other.firstSeen)) return false;
		if (!lastSeen.equals(other.lastSeen)) return false;
		if (!name.equals(other.name)) return false;
		return true;
	}
}
