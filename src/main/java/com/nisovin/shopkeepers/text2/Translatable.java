package com.nisovin.shopkeepers.text2;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link TextElement} that gets translated on the client.
 * <p>
 * TODO arguments use their own placeholder arguments, or shared with aprent Text?
 * 
 * <p>
 * In case translatable texts are not supported or when converting to
 * {@link #appendPlainText(Text, StringBuilder, boolean, boolean) plain text}, its {@link #getFallbackText() fallback
 * Text} is used instead (if available).
 */
public class Translatable extends AbstractTextElement {

	private final String translationKey; // not null or empty
	private final List<Text> arguments; // not null, can be empty, unmodifiable, does not contain null
	private final Text fallbackText; // can be null, unmodifiable view

	Translatable(String translationKey) {
		this(translationKey, Collections.emptyList());
	}

	Translatable(String translationKey, List<Text> arguments) {
		this(translationKey, arguments, null);
	}

	Translatable(String translationKey, List<Text> arguments, Text fallbackText) {
		Validate.notEmpty(translationKey, "translationKey is null or empty");
		Validate.notNull(arguments, "arguments is null");
		Validate.isTrue(!arguments.contains(null), "arguments contains null");
		this.translationKey = translationKey;
		// TODO create a deep copy of the passed Texts? This would also make the below check redundant
		// Only wrap the arguments in a new unmodifiable list if required:
		if (arguments == Collections.EMPTY_LIST) {
			this.arguments = arguments;
		} else {
			this.arguments = Collections.unmodifiableList(arguments);
		}
		this.fallbackText = (fallbackText != null) ? fallbackText.copy(true).getView() : null;
	}

	// TRANSLATABLE

	/**
	 * Gets the translation key.
	 * 
	 * @return the translation key, not <code>null</code>
	 */
	public String getTranslationKey() {
		return translationKey;
	}

	/**
	 * Gets the translation arguments.
	 * 
	 * @return the unmodifiable list of translation arguments, not <code>null</code>, can be empty
	 */
	public List<Text> getArguments() {
		return arguments;
	}

	/**
	 * Gets the {@link Text} that is used when converting this element to a
	 * {@link #appendPlainText(Text, StringBuilder, boolean, boolean) plain text}.
	 * 
	 * @return the fallback Text, can be <code>null</code>
	 */
	public Text getFallbackText() {
		return fallbackText;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText(Text container) {
		return false;
	}

	@Override
	public void appendPlainText(StringBuilder builder, Text container, boolean includeFormatting, boolean replacePlaceholders) {
		if (fallbackText != null) {
			for (TextElement element : fallbackText.getElements()) {
				element.appendPlainText(builder, fallbackText, includeFormatting, replacePlaceholders);
			}
		}
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Translatable [translationKey=");
		builder.append(translationKey);
		builder.append(", arguments=");
		builder.append(arguments);
		builder.append(", fallbackText=");
		builder.append(fallbackText);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + translationKey.hashCode();
		result = prime * result + arguments.hashCode();
		result = prime * result + Objects.hashCode(fallbackText);

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Translatable)) return false;
		Translatable other = (Translatable) obj;
		if (!translationKey.equals(other.translationKey)) return false;
		if (!arguments.equals(other.arguments)) return false;
		if (!Objects.equals(fallbackText, other.fallbackText)) return false;
		return true;
	}
}
