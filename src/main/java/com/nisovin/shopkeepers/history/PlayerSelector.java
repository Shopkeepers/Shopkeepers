package com.nisovin.shopkeepers.history;

import java.util.UUID;

import com.nisovin.shopkeepers.util.Validate;

public interface PlayerSelector {

	public String getPlayerIdentifier();

	public static final PlayerSelector ALL = new PlayerSelector() {

		@Override
		public String getPlayerIdentifier() {
			return "all";
		}

		@Override
		public String toString() {
			return "PlayerSelector.ALL";
		}
	};

	public static class ByUUID implements PlayerSelector {

		private final UUID playerUUID;

		public ByUUID(UUID playerUUID) {
			Validate.notNull(playerUUID);
			this.playerUUID = playerUUID;
		}

		public UUID getPlayerUUID() {
			return playerUUID;
		}

		@Override
		public String getPlayerIdentifier() {
			return playerUUID.toString();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PlayerSelector.ByUUID [playerUUID=");
			builder.append(playerUUID);
			builder.append("]");
			return builder.toString();
		}
	}

	public static class ByName implements PlayerSelector {

		private final String playerName;

		public ByName(String playerName) {
			Validate.notEmpty(playerName);
			this.playerName = playerName;
		}

		public String getPlayerName() {
			return playerName;
		}

		@Override
		public String getPlayerIdentifier() {
			return playerName;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("PlayerSelector.ByName [playerName=");
			builder.append(playerName);
			builder.append("]");
			return builder.toString();
		}
	}
}
