package com.nisovin.shopkeepers.text2;

/**
 * Abstract base class for (usually interactive) {@link TextElement}s which, usually, span across a sequence of other
 * {@link TextElement}s and don't have a plain text representation of their own.
 */
abstract class AbstractNonPlainTextElement extends AbstractTextElement {

	// PLAIN TEXT

	@Override
	public final boolean isPlainText(Text container) {
		return false;
	}

	@Override
	public final void appendPlainText(StringBuilder builder, Text container, boolean includeFormatting, boolean replacePlaceholders) {
		// Nothing to append.
	}
}
