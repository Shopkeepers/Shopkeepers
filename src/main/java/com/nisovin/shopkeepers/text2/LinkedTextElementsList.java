package com.nisovin.shopkeepers.text2;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.nisovin.shopkeepers.util.IteratorUtils;

/**
 * Implementation of {@link TextElementsList}.
 */
class LinkedTextElementsList implements TextElementsList {

	public static TextElementsList EMPTY_LIST = new LinkedTextElementsList(null);

	/**
	 * Returns a {@link TextElementsList} for the list of {@link TextElement}s starting with the given head element.
	 * <p>
	 * This assume that the list of {@link TextElement}s is immutable (i.e. that the links between the
	 * {@link TextElement}s does not change). If the given head element is <code>null</code>, the {@link #EMPTY_LIST
	 * empty List} is returned.
	 * 
	 * @param head
	 *            the head element, or <code>null</code> if the list is empty
	 * @return the list
	 */
	public static TextElementsList of(TextElement head) {
		if (head == null) return EMPTY_LIST;
		else return new LinkedTextElementsList(head);
	}

	private final TextElement head; // null if the list is empty

	// Use the static factory method to get an instance of this.
	private LinkedTextElementsList(TextElement head) {
		this.head = head;
	}

	@Override
	public TextElement getHead() {
		return head;
	}

	@Override
	public boolean isEmpty() {
		return (head == null);
	}

	@Override
	public Iterator<TextElement> iterator() {
		return new ElementIterator(head);
	}

	private static class ElementIterator implements Iterator<TextElement> {

		private TextElement next; // can be null

		private ElementIterator(TextElement start) {
			this.next = start;
		}

		@Override
		public boolean hasNext() {
			return (next != null);
		}

		@Override
		public TextElement next() {
			if (next == null) {
				throw new NoSuchElementException();
			}
			TextElement oldNext = next;
			next = next.getNext();
			return oldNext;
		}
	}

	// JAVA OBJECT

	@Override
	public String toString() {
		return IteratorUtils.toString(this.iterator());
	}

	@Override
	public int hashCode() {
		return IteratorUtils.hashCode(this.iterator());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TextElementsList)) return false;
		TextElementsList other = (TextElementsList) obj;
		if (!IteratorUtils.equals(this.iterator(), other.iterator())) return false;
		return true;
	}
}
