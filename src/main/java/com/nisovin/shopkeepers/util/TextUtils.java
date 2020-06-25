package com.nisovin.shopkeepers.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.spigot.text.SpigotText;
import com.nisovin.shopkeepers.text.Text;

/**
 * Text and messaging utilities.
 * <p>
 * In contrast to {@link StringUtils}, this contains utilities that are more minecraft and messaging specific.
 */
public class TextUtils {

	private TextUtils() {
	}

	/*
	 * PLAIN TEXT
	 */

	// FORMATTING AND CONVERSION

	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
	static {
		DECIMAL_FORMAT.setGroupingUsed(false);
	}

	public static String getLocationString(Location location) {
		return getLocationString(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
	}

	public static String getLocationString(Block block) {
		return getLocationString(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	// more performant variant if coordinates are integers:
	public static String getLocationString(String worldName, int x, int y, int z) {
		return worldName + "," + x + "," + y + "," + z;
	}

	public static String getLocationString(String worldName, double x, double y, double z) {
		return worldName + "," + DECIMAL_FORMAT.format(x) + "," + DECIMAL_FORMAT.format(y) + "," + DECIMAL_FORMAT.format(z);
	}

	public static String getChunkString(ChunkCoords chunk) {
		return getChunkString(chunk.getWorldName(), chunk.getChunkX(), chunk.getChunkZ());
	}

	public static String getChunkString(Chunk chunk) {
		return getChunkString(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	public static String getChunkString(String worldName, int cx, int cz) {
		return worldName + "," + cx + "," + cz;
	}

	private static final String UNKNOWN_PLAYER = "[unknown]";
	
	public static String getPlayerString(OfflinePlayer player) {
		return getPlayerString(player.getName(), player.getUniqueId());
	}

	public static String getPlayerString(String playerName, UUID playerUUID) {
		// either of them might be null
		if (playerName != null) {
			return playerName + (playerUUID == null ? "" : " (" + playerUUID.toString() + ")");
		} else if (playerUUID != null) {
			return playerUUID.toString();
		} else {
			return UNKNOWN_PLAYER;
		}
	}

	public static String getPlayerNameOrUUID(String playerName, UUID playerUUID) {
		// either of them might be null
		// prefer name, else use uuid
		if (playerName != null) {
			return playerName;
		} else if (playerUUID != null) {
			return playerUUID.toString();
		} else {
			return UNKNOWN_PLAYER;
		}
	}

	public static String getPlayerNameOrUnknown(String playerName) {
		if (playerName != null) {
			return playerName;
		} else {
			return UNKNOWN_PLAYER;
		}
	}

	public static final char COLOR_CHAR_ALTERNATIVE = '&';
	private static final Pattern STRIP_COLOR_ALTERNATIVE_PATTERN = Pattern.compile("(?i)" + String.valueOf(COLOR_CHAR_ALTERNATIVE) + "[0-9A-FK-OR]");

	// reverse of ChatColor#translateAlternateColorCodes
	public static String translateColorCodesToAlternative(char altColorChar, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
		for (int i = 0; i < b.length - 1; i++) {
			if (b[i] == ChatColor.COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
				b[i] = altColorChar;
				b[i + 1] = Character.toLowerCase(b[i + 1]);
			}
		}
		return new String(b);
	}

	public static String stripColor(String colored) {
		if (colored == null || colored.isEmpty()) return colored;
		String uncolored = ChatColor.stripColor(colored);
		uncolored = STRIP_COLOR_ALTERNATIVE_PATTERN.matcher(uncolored).replaceAll("");
		return uncolored;
	}

	public static String decolorize(String colored) {
		if (colored == null || colored.isEmpty()) return colored;
		return translateColorCodesToAlternative(COLOR_CHAR_ALTERNATIVE, colored);
	}

	public static List<String> decolorize(List<String> colored) {
		if (colored == null) return null;
		List<String> decolored = new ArrayList<>(colored.size());
		for (String string : colored) {
			decolored.add(translateColorCodesToAlternative(COLOR_CHAR_ALTERNATIVE, string));
		}
		return decolored;
	}

	// decolorizes string entries, otherwise adopts them as they are
	public static List<Object> decolorizeUnknown(List<?> colored) {
		if (colored == null) return null;
		List<Object> decolored = new ArrayList<>(colored.size());
		for (Object entry : colored) {
			Object decolorizedEntry = entry;
			if (entry instanceof String) {
				decolorizedEntry = translateColorCodesToAlternative(COLOR_CHAR_ALTERNATIVE, (String) entry);
			}
			decolored.add(decolorizedEntry);
		}
		return decolored;
	}

	public static String colorize(String message) {
		if (message == null || message.isEmpty()) return message;
		return ChatColor.translateAlternateColorCodes(COLOR_CHAR_ALTERNATIVE, message);
	}

	public static List<String> colorize(List<String> messages) {
		if (messages == null) return messages;
		List<String> colored = new ArrayList<>(messages.size());
		for (String message : messages) {
			colored.add(colorize(message));
		}
		return colored;
	}

	// colorizes string entries, otherwise adopts them as they are
	public static List<Object> colorizeUnknown(List<?> uncolored) {
		if (uncolored == null) return null;
		List<Object> colored = new ArrayList<>(uncolored.size());
		for (Object entry : uncolored) {
			Object colorizedEntry = entry;
			if (entry instanceof String) {
				colorizedEntry = colorize((String) entry);
			}
			colored.add(colorizedEntry);
		}
		return colored;
	}

	// ARGUMENTS

	private static final Map<String, Object> TEMP_ARGUMENTS_MAP = new HashMap<>();

	// Arguments format: [key1, value1, key2, value2, ...]
	// The keys are expected to be of type String.
	public static <T> void addArgumentsToMap(Map<String, Object> argumentsMap, Object... argumentPairs) {
		Validate.notNull(argumentsMap, "argumentsMap is null");
		Validate.notNull(argumentPairs, "argumentPairs is null");
		Validate.isTrue(argumentPairs.length % 2 == 0, "argumentPairs.length is not a multiple of 2");
		int argumentsKeyLimit = argumentPairs.length - 1;
		for (int i = 0; i < argumentsKeyLimit; i += 2) {
			String key = (String) argumentPairs[i];
			Object value = argumentPairs[i + 1];
			argumentsMap.put(key, value);
		}
	}

	public static String replaceArguments(String message, Object... argumentPairs) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return replaceArguments(message, TEMP_ARGUMENTS_MAP);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // reset
		}
	}

	public static String replaceArguments(String message, Map<String, Object> arguments) {
		Validate.notNull(message, "Message is null!");
		// uses the default key format: {key}
		return StringUtils.replaceArguments(message, arguments); // checks arguments
	}

	// creates and returns a new List of messages
	public static List<String> replaceArguments(Collection<String> messages, Map<String, Object> arguments) {
		Validate.notNull(messages, "Messages is null!");
		List<String> replaced = new ArrayList<>(messages.size());
		for (String message : messages) {
			replaced.add(replaceArguments(message, arguments)); // checks message and arguments
		}
		return replaced;
	}

	public static List<String> replaceArguments(Collection<String> messages, Object... argumentPairs) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return replaceArguments(messages, TEMP_ARGUMENTS_MAP);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // reset
		}
	}

	// SENDING

	public static void sendMessage(CommandSender recipient, String message) {
		Validate.notNull(recipient, "Recipient is null!");
		Validate.notNull(message, "Message is null!");
		// skip sending if message is empty: allows disabling of messages
		if (message.isEmpty()) return;

		// send (potentially multiline) message:
		for (String line : StringUtils.splitLines(message)) {
			recipient.sendMessage(line);
		}
	}

	public static void sendMessage(CommandSender recipient, String message, Map<String, Object> arguments) {
		// replace message arguments and then send:
		sendMessage(recipient, replaceArguments(message, arguments));
	}

	public static void sendMessage(CommandSender recipient, String message, Object... argumentPairs) {
		// replace message arguments and then send:
		sendMessage(recipient, replaceArguments(message, argumentPairs));
	}

	/*
	 * TEXT COMPONENTS
	 */

	public static Text getPlayerText(Player player) {
		assert player != null;
		return getPlayerText(player.getName(), player.getUniqueId());
	}

	public static Text getPlayerText(User user) {
		assert user != null;
		return getPlayerText(user.getName(), user.getUniqueId());
	}

	public static Text getPlayerText(String playerName, UUID playerUUID) {
		// either of them might be null
		// prefer name, else use uuid
		if (playerName != null) {
			if (playerUUID != null) {
				String playerUUIDString = playerUUID.toString();
				return Text.hoverEvent(Text.of(playerUUIDString))
						.childInsertion(playerUUIDString)
						.childText(playerName)
						.buildRoot();
			} else {
				return Text.of(playerName);
			}
		} else if (playerUUID != null) {
			return Text.of(playerUUID.toString());
		} else {
			return Text.of("[unknown]");
		}
	}

	// SENDING

	public static void sendMessage(CommandSender recipient, Text message) {
		SpigotText.sendMessage(recipient, message);
	}

	public static void sendMessage(CommandSender recipient, Text message, Map<String, Object> arguments) {
		Validate.notNull(recipient, "Recipient is null!");
		Validate.notNull(message, "Message is null!");
		// assign arguments and then send:
		message.setPlaceholderArguments(arguments);
		sendMessage(recipient, message);
	}

	public static void sendMessage(CommandSender recipient, Text message, Object... argumentPairs) {
		Validate.notNull(recipient, "Recipient is null!");
		Validate.notNull(message, "Message is null!");
		// assign arguments and then send:
		message.setPlaceholderArguments(argumentPairs);
		sendMessage(recipient, message);
	}
}
