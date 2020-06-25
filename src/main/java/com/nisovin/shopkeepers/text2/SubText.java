package com.nisovin.shopkeepers.text2;

public class SubText extends AbstractTextElement {

	private final TextElementsList elements; // not null, unmodifiable, can be empty

	SubText(TextElementsList elements) {
		assert elements != null;
		this.elements = elements;
	}

	/**
	 * Gets the {@link TextElement}s contained by this {@link SubText}.
	 * 
	 * @return the immutable list of TextElements, not <code>null</code>
	 */
	public TextElementsList getElements() {
		return elements;
	}

	/**
	 * Checks if this {@link SubText} empty, i.e. contains no elements.
	 * <p>
	 * This is a shortcut for checking if the SubText's {@link #getElements() elements} is empty.
	 * 
	 * @return <code>true</code> if this SubText is empty
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText(Text container) {
		for (TextElement element : elements) {
			if (!element.isPlainText(container)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void appendPlainText(StringBuilder builder, Text text, boolean includeFormatting, boolean replacePlaceholders) {
		for (TextElement element : elements) {
			element.appendPlainText(builder, text, includeFormatting, replacePlaceholders);
		}
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SubText [elements=");
		builder.append(elements);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SubText)) return false;
		SubText other = (SubText) obj;
		if (!elements.equals(other.elements)) return false;
		return true;
	}
}
