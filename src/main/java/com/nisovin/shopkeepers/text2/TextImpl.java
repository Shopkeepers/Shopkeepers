package com.nisovin.shopkeepers.text2;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Text} implementation.
 */
public final class TextImpl implements Text {

	private final TextElementsList elements; // not null, unmodifiable, can be empty
	// Texts are likely to be reused with new placeholder arguments. Since the Text's structure does not change, the
	// contained placeholders will always stay the same. We can avoid recreating the map entry objects by storing null
	// instead of removing the entries when clearing placeholder arguments. The map may therefore also contain entries
	// with a value of null for unassigned placeholders.
	// The stored Texts are unmodifiable views.
	// TODO lazily pre-populate the map for all Placeholders contained inside this Text
	// TODO use

	private Map<String, Text> placeholders = Collections.emptyMap(); // lazily instantiated when needed
	private Map<String, Text> placeholdersView = Collections.emptyMap(); // lazily instantiated when needed
	private Text view = null; // lazily instantiated when actually needed

	private Boolean isPlainText = null; // lazily initialized
	private String plainFormatString = null; // lazily initialized

	/**
	 * Constructs a new {@link Text} which uses the given list of elements.
	 * <p>
	 * Texts are supposed to be created by the {@link TextBuilder}, so this is intended to only be invoked by
	 * it. The
	 * given list is directly used (without copying) and is supposed to not change.
	 * 
	 * @param elements
	 *            the elements to use, not <code>null</code>
	 */
	TextImpl(TextElementsList elements) {
		this.elements = elements;
	}

	/**
	 * Constructs a new {@link Text} which copies the {@link TextElement}s of the given {@link Text}.
	 * <p>
	 * Any dynamic state (such as placeholder arguments) is not getting copied and needs to be applied manually if
	 * intended.
	 * 
	 * @param textToCopy
	 *            the Text to copy, not <code>null</code>
	 */
	TextImpl(Text textToCopy) {
		assert textToCopy != null;
		// The lists and the contained TextElements are asserted to be immutable, so the lists can be shared:
		this.elements = textToCopy.getElements();
	}

	@Override
	public TextElementsList getElements() {
		return elements;
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	// PLACEHOLDERS

	@Override
	public Map<String, Text> getPlaceholders() {
		return placeholdersView;
	}

	@Override
	public Text getPlaceholder(String key) {
		return placeholdersView.get(key);
	}

	@Override
	public Text setPlaceholder(String key, Object argument) {
		Validate.notEmpty(key, "key is null or empty");
		if (argument == null) {
			// Replacing the Entry's value with null instead of removing it avoids having the recreate the Entry object
			// when assigning a different placeholder argument later:
			placeholders.replace(key, null);
		} else {
			// We only store an unmodifiable view of the given Text:
			placeholders.put(key, Text.of(argument).getView());
		}
		return this;
	}

	@Override
	public Text setPlaceholders(Map<String, ?> arguments) {
		Validate.notNull(arguments, "arguments is null");
		for (Entry<String, ?> entry : arguments.entrySet()) {
			String key = entry.getKey();
			Object argument = entry.getValue();
			this.setPlaceholder(key, argument);
		}
		return this;
	}

	@Override
	public Text setPlaceholders(Object... keyArgumentPairs) {
		Validate.notNull(keyArgumentPairs, "keyArgumentPairs is null");
		Validate.isTrue(keyArgumentPairs.length % 2 == 0, "keyArgumentPairs.length is not a multiple of 2");
		int keyLimit = keyArgumentPairs.length - 1;
		for (int i = 0; i < keyLimit; i += 2) {
			Object keyObject = keyArgumentPairs[i];
			String key = (keyObject == null) ? null : keyObject.toString();
			Object argument = keyArgumentPairs[i + 1];
			this.setPlaceholder(key, argument); // throws an exception if the key is null
		}
		return this;
	}

	@Override
	public Text clearPlaceholders() {
		for (Entry<String, Text> entry : placeholders.entrySet()) {
			// Replacing the Entry's value with null instead of removing it avoids having the recreate the Entry object
			// when assigning a different placeholder argument later:
			entry.setValue(null);
		}
		return this;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText() {
		if (isPlainText == null) {
			isPlainText = this.calculateIsPlainText();
		}
		return isPlainText;
	}

	private boolean calculateIsPlainText() {
		for (TextElement element : elements) {
			if (!element.isPlainText(this)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toPlainText() {
		// This cannot be cached because it relies on the current state of the placeholder arguments (which might even
		// change internally without this Text being able to notice it).
		StringBuilder builder = new StringBuilder();
		for (TextElement element : elements) {
			element.appendPlainText(builder, this, true, true);
		}
		return builder.toString();
	}

	@Override
	public String toPlainFormatText() {
		if (plainFormatString == null) {
			plainFormatString = this.calculatePlainFormatText();
		}
		return plainFormatString;
	}

	private String calculatePlainFormatText() {
		StringBuilder builder = new StringBuilder();
		for (TextElement element : elements) {
			element.appendPlainText(builder, this, true, false);
		}
		return builder.toString();
	}

	@Override
	public String toUnformattedText() {
		// This cannot be cached because it relies on the current state of the placeholder arguments (which might even
		// change internally without this Text being able to notice it).
		StringBuilder builder = new StringBuilder();
		for (TextElement element : elements) {
			element.appendPlainText(builder, this, false, true);
		}
		return builder.toString();
	}

	// COPY

	@Override
	public Text copy(boolean copyPlaceholders) {
		Text copy = new TextImpl(this);
		if (copyPlaceholders) {
			for (Entry<String, Text> entry : placeholders.entrySet()) {
				String key = entry.getKey();
				Text argument = entry.getValue();
				if (argument != null) {
					argument = argument.copy(true);
				}
				this.setPlaceholder(key, argument);
			}
		} // Else: Not copying placeholders.
		return copy;
	}

	// VIEW

	@Override
	public Text getView() {
		if (view == null) {
			view = new TextView(this);
		}
		return view;
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Text [elements=");
		builder.append(elements);
		builder.append(", placeholders=");
		builder.append(placeholders);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + elements.hashCode();
		result = prime * result + placeholders.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		// Note: This Text can also be equal to other types of Text, such as TextViews.
		if (!(obj instanceof Text)) return false;
		Text other = (Text) obj;
		if (!elements.equals(other.getElements())) return false;
		if (!placeholders.equals(other.getPlaceholders())) return false;
		return true;
	}
}
