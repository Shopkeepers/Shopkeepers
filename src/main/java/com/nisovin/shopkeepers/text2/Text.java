package com.nisovin.shopkeepers.text2;

import java.util.Map;
import java.util.function.Supplier;

import org.bukkit.ChatColor;

/**
 * A text representation with support for various interaction and formatting features.
 * <p>
 * A {@link Text} is the combination of the text's fixed structure, represented as an immutable list of immutable
 * {@link TextElement}s, and its dynamically assigned placeholder arguments.
 * <p>
 * Each {@link TextElement} represents one feature such as {@link PlainText text}, {@link Formatting formatting},
 * {@link Placeholder placeholders}, or interaction features such as {@link HoverEvent hover events}, {@link ClickEvent
 * click events} or {@link Insertion click insertions}.
 * <p>
 * Requiring the individual {@link TextElement}s to be immutable allows them to be easily reused when copying or
 * assembling new Texts. Any dynamic state (such as {@link #getPlaceholders() placeholder arguments}) is located inside
 * the {@link Text} object itself and not within its {@link TextElement}s.
 * <p>
 * Placeholder arguments may themselves be {@link Text}s with their own set of assigned placeholder arguments.
 * Placeholder arguments specified in one {@link Text} only bind to {@link Placeholder}s within the same {@link Text}
 * and not to {@link Placeholder}s within other placeholder arguments or {@link Text}s referenced by certain
 * {@link TextElement}s (such as the {@link HoverEvent#getText() Text of hover events}.
 * <p>
 * Formatting and interaction features are inherited to the following Text elements in the list. The formatting can be
 * reset by inserting a {@link Formatting} with {@link ChatColor#RESET}. An interaction feature of a certain type
 * replaces any previously active interaction feature of the same type for the following Text elements.
 * <p>
 * A {@link SubText} can be used to limit the scope of contained interaction features to only the subsequent elements
 * within the same {@link SubText}. The {@link SubText} inherits the formatting and interaction features of the parent
 * Text's previous elements, but any interaction features defined within the {@link SubText} do not get inherited to the
 * following elements of the parent Text. This allows for Texts with interaction features which only encompass a
 * subsection of the whole Text.
 * <p>
 * However, any formatting defined within the {@link SubText} <b>is inherited</b> to the following elements of the
 * parent Text as well. This inheritance behavior is closely oriented on regular text with legacy color codes and allows
 * us to implement {@link #toPlainText()} and {@link #toPlainFormatText()} without having to artificially insert
 * additional formatting resets at the end of every {@link SubText}. This ensures that {@link #toPlainText()} and
 * {@link #toPlainFormatText()} for Texts produced by {@link Text#parse(String)} will reproduce the original input text.
 * <p>
 * Placeholder argument Texts which substitute {@link Placeholder} elements behave similar to {@link SubText} elements,
 * with the difference that they use their own set of placeholder arguments. Formatting from previous elements of the
 * parent Text gets inherited to the placeholder argument Text and any formatting defined within the placeholder
 * argument Text gets inherited to the following elements of the parent. But any interaction features defined within the
 * placeholder argument Text do not get inherited to the following elements of the parent Text.
 */
public interface Text {

	// COMMON CONSTANTS

	/**
	 * An unmodifiable empty {@link Text}.
	 */
	public static final Text EMPTY = Text.builder().build().getView();

	/**
	 * An unmodifiable {@link Text} with the newline symbol as text.
	 */
	public static final Text NEWLINE = Text.builder().text("\n").build().getView();

	// FACTORY

	/**
	 * Returns a new {@link TextBuilder}.
	 * 
	 * @return the new TextBuilder
	 */
	public static TextBuilder builder() {
		return new TextBuilder();
	}

	/**
	 * Shortcut for creating a new {@link Text} with fixed contents.
	 * <p>
	 * Consider using {@link #parse(String)} instead if the given text may contain features such as formatting, color
	 * codes, or placeholders.
	 * <p>
	 * Consider using a {@link #builder() TextBuilder} if you intend to extend the newly created Text.
	 * 
	 * @param text
	 *            the textual contents
	 * @return the new Text
	 */
	public static Text of(String text) {
		return builder().text(text).build();
	}

	/**
	 * Shortcut for converting the given object into a {@link Text}.
	 * <p>
	 * If the given object is a {@link Supplier}, it gets invoked to obtain the actual object. If the object is already
	 * a {@link Text}, it is returned. Otherwise a new {@link Text} is created from the object's
	 * {@link Object#toString() String representation}. If the object is <code>null</code>, the String
	 * <code>"null"</code> is used.
	 * 
	 * @param object
	 *            the object to convert to a Text
	 * @return the new Text, not <code>null</code>
	 */
	public static Text of(Object object) {
		if (object instanceof Supplier) {
			object = ((Supplier<?>) object).get();
		}
		if (object instanceof Text) return (Text) object;
		return Text.of(String.valueOf(object));
	}

	// PARSING

	// TODO

	/////

	/**
	 * Gets the {@link TextElement}s contained by this {@link Text}.
	 * 
	 * @return the immutable list of TextElements, not <code>null</code>
	 */
	public TextElementsList getElements();

	/**
	 * Checks if this {@link Text} is equal to the {@link Text#EMPTY empty Text}, i.e. contains no elements.
	 * <p>
	 * This is a shortcut for checking if the Text's {@link #getElements() elements} is empty.
	 * 
	 * @return <code>true</code> if this Text is empty
	 */
	public boolean isEmpty();

	// PLACEHOLDERS

	/**
	 * Gets the currently assigned placeholder arguments.
	 * <p>
	 * The arguments themselves are unmodifiable views as well.
	 * 
	 * @return an unmodifiable view on the currently assigned placeholder arguments
	 */
	public Map<String, Text> getPlaceholders();

	/**
	 * Gets the placeholder argument for the specified placeholder key.
	 * 
	 * @param key
	 *            the placeholder key
	 * @return an unmodifiable view on the argument, or <code>null</code>
	 */
	public Text getPlaceholder(String key);

	/**
	 * Sets the placeholder argument for the specified key.
	 * <p>
	 * Any non-{@link Text} argument gets first converted to a corresponding Text via {@link Text#of(Object)}.
	 * 
	 * @param key
	 *            the placeholder key
	 * @param argument
	 *            the argument, or <code>null</code> to clear it
	 * @return this Text
	 */
	public Text setPlaceholder(String key, Object argument);

	/**
	 * Sets the placeholder arguments for the given key-value mappings.
	 * <p>
	 * This is a shortcut for calling {@link #setPlaceholder(String, Object)} for each mapping.
	 * <p>
	 * Any placeholders for which no corresponding argument is provided in the given Map will retain their currently
	 * assigned argument (if any).
	 * 
	 * @param arguments
	 *            a mapping between placeholder keys and their arguments
	 * @return this Text
	 * @see #setPlaceholder(String, Object)
	 */
	public Text setPlaceholders(Map<String, ?> arguments);

	/**
	 * Sets the placeholder arguments for the given key-argument pairs.
	 * <p>
	 * This is a shortcut for calling {@link #setPlaceholder(String, Object)} for each key-argument pair.
	 * <p>
	 * Any placeholders for which no corresponding argument is provided in the given key-argument pairs will retain
	 * their currently assigned argument (if any).
	 * 
	 * @param keyArgumentPairs
	 *            an array that pairwise contains placeholder keys and their arguments in the format
	 *            <code>[key1, argument1, key2, argument2, ...]</code>
	 * @return this Text
	 * @see #setPlaceholder(String, Object)
	 */
	public Text setPlaceholders(Object... keyArgumentPairs);

	/**
	 * Clears all placeholder arguments.
	 * 
	 * @return this Text
	 */
	public Text clearPlaceholders();

	// PLAIN TEXT

	/**
	 * Checks whether this {@link Text} uses any non-plain text features such as hover events, click events, insertions,
	 * etc.
	 * 
	 * @return <code>true</code> if this Text uses only plain text features
	 */
	public boolean isPlainText();

	/**
	 * Converts this {@link Text} to a plain String text.
	 * <p>
	 * This includes color and formatting codes, replaces {@link Placeholder}s with their arguments (if available), but
	 * omits any interaction features.
	 * 
	 * @return the plain text
	 */
	public String toPlainText();

	/**
	 * Converts this {@link Text} to a plain String text, but omits any assigned placeholder arguments and prints their
	 * {@link Placeholder#getFormattedKey() formatted placeholder key} instead.
	 * 
	 * @return the plain format text
	 */
	public String toPlainFormatText();

	/**
	 * Converts this {@link Text} to a {@link #toPlainText() plain String text}, but also omits any color and formatting
	 * codes.
	 * <p>
	 * Placeholders are replaced with their arguments (if available).
	 * 
	 * @return the plain text without formatting codes
	 */
	public String toUnformattedText();

	// COPY

	/**
	 * Creates a copy of this {@link Text}.
	 * <p>
	 * If this Text is a {@link TextView}, this does not return another {@link TextView} but a regular {@link Text}
	 * whose {@link Text#getPlaceholders() placeholder arguments} can be modified.
	 * 
	 * @param copyPlaceholders
	 *            <code>true</code> to create deep copies of all placeholder arguments, <code>false</code> to not copy
	 *            them
	 * @return the copy
	 */
	public Text copy(boolean copyPlaceholders);

	// VIEW

	/**
	 * Gets an unmodifiable view on this {@link Text}.
	 * <p>
	 * The returned view does not allow {@link #setPlaceholder(String, Object) setting} or {@link #clearPlaceholders()
	 * clearing} the placeholder arguments of this Text. However, the Text's placeholder arguments might still get
	 * changed through a direct reference to it. These changes are then also reflected by the returned view.
	 * 
	 * @return the unmodifiable view on this Text
	 */
	public Text getView();

	// JAVA OBJECT

	/**
	 * A detailed String representation of this {@link Text}'s internals.
	 * <p>
	 * Use {@link #toPlainText()} or {@link #toPlainFormatText()} to get a flat String representation of this
	 * {@link Text}'s contents.
	 * 
	 * @return a detailed String representation
	 */
	@Override
	public String toString();

	/**
	 * Checks if the given object is a {@link Text} as well and that their {@link #getElements() elements} and
	 * {@link #getPlaceholders() placeholders} are equal.
	 * <p>
	 * Note that {@link TextElement}s don't take their {@link #getNext() next} references into account when being
	 * compared, so elements with equal contents but contained in different lists will be considered equal.
	 * <p>
	 * This comparison does not take into account whether the given other {@link Text} object is a {@link TextView} or
	 * not.
	 * 
	 * @param o
	 *            the other object
	 * @return <code>true</code> if equal
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object o);
}
