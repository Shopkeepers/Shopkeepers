package com.nisovin.shopkeepers.text2;

import java.util.Map;

import com.nisovin.shopkeepers.util.Validate;

/**
 * An immutable view on a {@link Text}.
 * <p>
 * The returned view does not allow {@link Text#setPlaceholder(String, Object) setting} or
 * {@link Text#clearPlaceholders() clearing} the placeholder arguments of the underlying Text. However, the underlying
 * Text's placeholder arguments might still get changed through a direct reference to it. These changes are then also
 * reflected by the returned view.
 */
public class TextView implements Text {

	private final Text text;

	TextView(Text text) {
		Validate.notNull(text, "text is null");
		this.text = text;
	}

	private UnsupportedOperationException noModificationException() {
		return new UnsupportedOperationException("This Text is a view and cannot be modified!");
	}

	@Override
	public TextElementsList getElements() {
		return text.getElements();
	}

	@Override
	public boolean isEmpty() {
		return text.isEmpty();
	}

	// PLACEHOLDERS

	@Override
	public Map<String, Text> getPlaceholders() {
		// TODO Return a Map which recursively returns views of the assigned placeholders?
		return text.getPlaceholders();
	}

	@Override
	public Text getPlaceholder(String key) {
		return text.getPlaceholder(key);
	}

	@Override
	public Text setPlaceholder(String key, Object argument) {
		throw this.noModificationException();
	}

	@Override
	public Text setPlaceholders(Map<String, ?> arguments) {
		throw this.noModificationException();
	}

	@Override
	public Text setPlaceholders(Object... keyArgumentPairs) {
		throw this.noModificationException();
	}

	@Override
	public Text clearPlaceholders() {
		throw this.noModificationException();
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText() {
		return text.isPlainText();
	}

	@Override
	public String toPlainText() {
		return text.toPlainText();
	}

	@Override
	public String toPlainFormatText() {
		return text.toPlainFormatText();
	}

	@Override
	public String toUnformattedText() {
		return text.toUnformattedText();
	}

	// COPY

	@Override
	public Text copy(boolean copyPlaceholders) {
		// Note: Does not return a new TextView, but a regular modifiable Text.
		return text.copy(copyPlaceholders);
	}

	// VIEW

	@Override
	public Text getView() {
		return this; // already a view
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TextView [text=");
		builder.append(text);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return text.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		// Note: This TextView can also be equal to regular types of Text, not just other TextViews.
		return text.equals(obj);
	}
}
