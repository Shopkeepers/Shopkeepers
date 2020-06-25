package com.nisovin.shopkeepers.text2;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.util.Validate;

public class Formatting extends AbstractTextElement {

	private final ChatColor chatColor; // not null

	Formatting(ChatColor chatColor) {
		Validate.notNull(chatColor, "chatColor is null");
		this.chatColor = chatColor;
	}

	/**
	 * Gets the {@link Formatting}'s {@link ChatColor}.
	 * <p>
	 * This can be a {@link ChatColor#isColor() color}, a {@link ChatColor#isFormat() format}, or
	 * {@link ChatColor#RESET}.
	 * 
	 * @return the chatColor, not <code>null</code>
	 */
	public ChatColor getChatColor() {
		return chatColor;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText(Text container) {
		return true;
	}

	@Override
	public void appendPlainText(StringBuilder builder, Text container, boolean includeFormatting, boolean replacePlaceholders) {
		if (includeFormatting) {
			builder.append(chatColor.toString());
		}
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Formatting [chatColor=");
		builder.append(chatColor);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return chatColor.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Formatting)) return false;
		Formatting other = (Formatting) obj;
		if (chatColor != other.chatColor) return false;
		return true;
	}
}
