package com.nisovin.shopkeepers.text2;

import com.nisovin.shopkeepers.util.Validate;

public class HoverEvent extends AbstractNonPlainTextElement {

	public enum Action {
		/**
		 * Displays the hover text.
		 * <p>
		 * The text can be multi-line by using the newline character {@code \n}.
		 */
		SHOW_TEXT,
		/**
		 * Displays an item.
		 * <p>
		 * Requires the hover text to be the item's stringified NBT data.
		 */
		SHOW_ITEM,
		/**
		 * Displays an entity.
		 * <p>
		 * Requires the hover text to be the entity's stringified NBT data.
		 */
		SHOW_ENTITY;
	}

	private final Action action; // not null
	private final Text text; // not null, can be empty, unmodifiable and expected to be immutable

	/**
	 * Constructs a new {@link HoverEvent}.
	 * <p>
	 * To ensure that this {@link HoverEvent} is immutable, this creates a {@link Text#copy(boolean) copy} of the passed
	 * {@link Text} and only provides an unmodifiable view on the stored Text.
	 * 
	 * @param action
	 *            the action
	 * @param text
	 *            the text
	 */
	HoverEvent(Action action, Text text) {
		Validate.notNull(action, "action is null");
		Validate.notNull(text, "text is null");
		this.action = action;
		this.text = text.copy(true).getView();
	}

	/**
	 * Gets the {@link HoverEvent.Action}.
	 * 
	 * @return the hover event action, not <code>null</code>
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * Gets the hover event text.
	 * 
	 * @return the (unmodifiable) hover event text, not <code>null</code>
	 */
	public Text getText() {
		return text;
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HoverEvent [action=");
		builder.append(action);
		builder.append(", text=");
		builder.append(text);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + action.hashCode();
		result = prime * result + text.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof HoverEvent)) return false;
		HoverEvent other = (HoverEvent) obj;
		if (action != other.action) return false;
		if (!text.equals(other.text)) return false;
		return true;
	}
}
