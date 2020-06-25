package com.nisovin.shopkeepers.text2;

import com.nisovin.shopkeepers.util.Validate;

/**
 * When shift-clicked by the player, the insertion text gets inserted into his chat input.
 * <p>
 * Unlike {@link ClickEvent.Action#SUGGEST_COMMAND} this does not replace the already existing chat input.
 */
public class Insertion extends AbstractNonPlainTextElement {

	private final String text; // not null or empty

	Insertion(String text) {
		Validate.notEmpty(text, "text is null or empty");
		this.text = text;
	}

	/**
	 * Gets the insertion text.
	 * 
	 * @return the insertion text, not <code>null</code> or empty
	 */
	public String getText() {
		return text;
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Insertion [text=");
		builder.append(text);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + text.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Insertion)) return false;
		Insertion other = (Insertion) obj;
		if (!text.equals(other.text)) return false;
		return true;
	}
}
