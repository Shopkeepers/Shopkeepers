package com.nisovin.shopkeepers.player.profile.storage.sql;

import java.time.Instant;
import java.util.UUID;

import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Additionally stores the player's database id.
 */
public class SQLPlayerProfile extends PlayerProfile {

	private final int playerId;

	public SQLPlayerProfile(int playerId, UUID playerUUID, String playerName, Instant firstSeen, Instant lastSeen) {
		super(playerUUID, playerName, firstSeen, lastSeen);
		Validate.isTrue(playerId >= 0, "PlayerId cannot be negative!");
		this.playerId = playerId;
	}

	/**
	 * Gets the player's unique database id.
	 * 
	 * @return the player's unique database id
	 */
	public int getPlayerId() {
		return playerId;
	}
}
