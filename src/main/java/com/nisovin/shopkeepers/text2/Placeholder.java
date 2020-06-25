package com.nisovin.shopkeepers.text2;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link TextElement} that acts as placeholder for another, dynamically supplied {@link Text} argument.
 * <p>
 * To ensure the immutability of {@link Placeholder} elements, placeholder arguments do not get assigned to
 * {@link Placeholder}s directly, but instead get {@link Text#setPlaceholder(String, Object) specified} at the
 * {@link Text} containing the {@link Placeholder}.
 * <p>
 * If no argument is specified for a {@link Placeholder}, the {@link Placeholder} behaves like a regular plain Text
 * which simply consists of its {@link #getFormattedPlaceholderKey() formatted placeholder key}.
 * <p>
 * The argument Text which substitutes this {@link Placeholder} behaves similar to a {@link SubText} element in how
 * formatting and interaction features are inherited from previous elements and to subsequent elements of the parent
 * Text. See {@link Text}.
 */
public class Placeholder extends AbstractTextElement {

	public static final char PLACEHOLDER_PREFIX_CHAR = '{';
	public static final char PLACEHOLDER_SUFFIX_CHAR = '}';

	private final String key; // not null or empty
	private final String formattedKey; // not null or empty

	Placeholder(String key) {
		Validate.notEmpty(key, "key is null or empty");
		this.key = key;
		this.formattedKey = (PLACEHOLDER_PREFIX_CHAR + key + PLACEHOLDER_SUFFIX_CHAR);
	}

	/**
	 * Gets the placeholder key.
	 * 
	 * @return the placeholder key, not <code>null</code> or empty
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Gets the {@link #getKey() placeholder key} in the format <code>{key}</code>.
	 * 
	 * @return the formatted placeholder key, not <code>null</code> or empty
	 */
	public String getFormattedKey() {
		return formattedKey;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText(Text container) {
		Text argument = container.getPlaceholder(key);
		if (argument != null) {
			return argument.isPlainText();
		} else {
			return true;
		}
	}

	@Override
	public void appendPlainText(StringBuilder builder, Text container, boolean includeFormatting, boolean replacePlaceholders) {
		if (replacePlaceholders) {
			Text argument = container.getPlaceholder(key);
			if (argument != null) {
				for (TextElement element : argument.getElements()) {
					element.appendPlainText(builder, argument, includeFormatting, replacePlaceholders);
				}
				return;
			} // Else: Continue and append formatted placeholder key
		}
		builder.append(formattedKey);
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Placeholder [key=");
		builder.append(key);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Placeholder)) return false;
		Placeholder other = (Placeholder) obj;
		if (!key.equals(other.key)) return false;
		return true;
	}
}
