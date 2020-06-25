package com.nisovin.shopkeepers.text2;

/**
 * An element within a {@link Text} or {@link SubText}.
 * <p>
 * {@link TextElement}s are required to be immutable so that they can be easily reused when copying Texts.
 */
public interface TextElement {

	/**
	 * Gets the next {@link TextElement}.
	 * <p>
	 * The next {@link TextElement} inherits the formatting and interaction features of the previous
	 * {@link TextElement}s.
	 * 
	 * @return the next TextElement, or <code>null</code>
	 */
	public TextElement getNext();

	// PLAIN TEXT

	/**
	 * Checks whether this {@link TextElement} uses any non-plain text features (such as hover events, etc.).
	 * 
	 * @param container
	 *            the {@link Text} containing this element (provides access to placeholder arguments)
	 * @return <code>true</code> if this TextElement uses only plain text features
	 */
	public boolean isPlainText(Text container);

	/**
	 * Appends the plain text representation of this {@link TextElement} to the given {@link StringBuilder}.
	 * 
	 * @param builder
	 *            the StringBuilder
	 * @param container
	 *            the {@link Text} containing this element (provides access to placeholder arguments)
	 * @param includeFormatting
	 *            <code>true</code> to include formatting and color codes
	 * @param replacePlaceholders
	 *            <code>true</code> to replace placeholders with their arguments, <code>false</code> to output their
	 *            {@link Placeholder#getFormattedKey() formatted placeholder keys} instead
	 */
	public void appendPlainText(StringBuilder builder, Text container, boolean includeFormatting, boolean replacePlaceholders);

	// JAVA OBJECT

	/**
	 * A detailed String representation of this {@link TextElement}'s internals.
	 * 
	 * @return a detailed String representation
	 */
	@Override
	public String toString();

	/**
	 * This does not take the {@link TextElement}'s {@link #getNext() next} reference into account, so elements with
	 * equal contents but contained in different lists will be considered equal.
	 * 
	 * @param o
	 *            the other object
	 * @return <code>true</code> if equal
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object o);
}
