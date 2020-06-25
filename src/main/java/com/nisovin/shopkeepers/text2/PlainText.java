package com.nisovin.shopkeepers.text2;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link TextElement} containing plain text.
 * <p>
 * Even though this is not enforced currently, this type of {@link TextElement} should ideally not contain legacy color
 * codes, because those can cause issues in certain edge cases (eg. with inheritance of text formatting, texts spanning
 * multiple lines, or when converting the TextElement to {@link #appendPlainText(StringBuilder, Text boolean, boolean)
 * plain unformatted text} (which is not supposed to contain formatting codes).
 */
public class PlainText extends AbstractTextElement {

	private final String text; // not null, can be empty

	PlainText(String text) {
		Validate.notNull(text, "text is null");
		this.text = text;
	}

	/**
	 * Gets the text.
	 * 
	 * @return the text, not <code>null</code>, can be empty
	 */
	public String getText() {
		return text;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText(Text container) {
		return true;
	}

	@Override
	public void appendPlainText(StringBuilder builder, Text container, boolean includeFormatting, boolean replacePlaceholders) {
		builder.append(text);
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlainText [text=");
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
		if (!(obj instanceof PlainText)) return false;
		PlainText other = (PlainText) obj;
		if (!text.equals(other.text)) return false;
		return true;
	}
}
