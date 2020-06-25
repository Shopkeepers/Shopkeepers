package com.nisovin.shopkeepers.text2;

import java.util.Iterator;

/**
 * An unmodifiable list of linked {@link TextElement}s.
 */
public interface TextElementsList extends Iterable<TextElement> {

	/**
	 * Gets the first {@link TextElement}.
	 * 
	 * @return the first element, or <code>null</code> if the list of TextElements is empty
	 */
	public TextElement getHead();

	/**
	 * Checks if there are any contained {@link TextElement}s.
	 * 
	 * @return <code>true</code> if the list of TextElements is empty
	 */
	public boolean isEmpty();

	/**
	 * Gets an {@link Iterator} over the contained {@link TextElement}s.
	 * <p>
	 * The returned {@link Iterator} does not iterate over the elements of contained {@link SubText}s.
	 */
	// Note: We don't provide an Iterator which recursively traverses the elements of contained SubTexts. Such an
	// Iterator would not be of much use in most cases, because there would be no indication when the Iterator jumps
	// from iterating over the elements of an inner SubText back to continue iterating the remaining elements of the
	// parent Text.
	@Override
	public Iterator<TextElement> iterator();

	// JAVA OBJECT

	/**
	 * Checks if the given object is a {@link TextElementsList} as well and that their contents are equal.
	 * <p>
	 * Note that {@link TextElement}s don't take their {@link #getNext() next} references into account when being
	 * compared, so elements with equal contents but contained in different lists will be considered equal.
	 * 
	 * @param o
	 *            the other object
	 * @return <code>true</code> if equal
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object o);
}
