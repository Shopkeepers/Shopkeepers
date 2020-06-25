package com.nisovin.shopkeepers.text2;

/**
 * Base class for all {@link TextElement} implementations.
 */
abstract class AbstractTextElement implements TextElement {

	private TextElement next = null;

	AbstractTextElement() {
	}

	// Only used by the TextBuilder while constructing a new Text.
	void setNext(TextElement next) {
		this.next = next;
	}

	@Override
	public TextElement getNext() {
		return next;
	}

	// JAVA OBJECT
	// Every implementation should provide implementations for toString, hashCode and equals.

	@Override
	public abstract String toString();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object o);
}
