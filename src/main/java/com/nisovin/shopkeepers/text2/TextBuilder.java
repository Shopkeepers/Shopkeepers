package com.nisovin.shopkeepers.text2;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.text.AbstractText;
import com.nisovin.shopkeepers.text.ClickEventText;
import com.nisovin.shopkeepers.text.HoverEventText;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A builder for the (fluent) construction of a {@link Text}.
 * <p>
 * An instance of {@link TextBuilder} can only be used to build a single {@link Text}. Once {@link #build()} has been
 * called, this builder can no longer be used.
 * <p>
 * Use {@link Text#builder()} to create an instance.
 */
public class TextBuilder {

	private AbstractTextElement head = null;
	private AbstractTextElement tail = null;
	private boolean built = false;

	TextBuilder() {
	}

	// BUILDER

	/**
	 * Checks whether this builder has already been used to build a {@link Text}.
	 * 
	 * @return <code>true</code> if built
	 */
	public boolean isBuilt() {
		return built;
	}

	protected void validateNotYetBuilt() {
		Validate.State.isTrue(!built, "The Text of this TextBuilder has already been built!");
	}

	private final void internalBuild() {
		this.validateNotYetBuilt();
		built = true;
	}

	/**
	 * Creates a new {@link Text} with this builder's {@link TextElement}s and marks this builder as {@link #isBuilt()
	 * built} to prevent any further modifications.
	 * 
	 * @return the built Text
	 */
	public Text build() {
		this.internalBuild();
		return new TextImpl(LinkedTextElementsList.of(head));
	}

	// Only used internally, invoked by other TextBuilders.
	/**
	 * Creates a new {@link SubText} with this builder's {@link TextElement}s and marks this builder as
	 * {@link #isBuilt() built} to prevent any further modifications.
	 * 
	 * @return the new SubText
	 */
	private SubText buildSubText() {
		this.internalBuild();
		return new SubText(LinkedTextElementsList.of(head));
	}

	// ELEMENTS

	// Internally used only.
	private TextBuilder append(AbstractTextElement element) {
		assert element != null;
		// assert: element is not yet linked to or by any other TextElement.
		this.validateNotYetBuilt();
		if (tail == null) {
			// First element:
			assert head == null;
			head = element;
			tail = element;
		} else {
			// Append to tail:
			tail.setNext(element);
			tail = element;
		}
		return this;
	}

	// Note: There is no #append(Text) method. Each Text has its own set of placeholder arguments. Implicitly importing
	// these placeholder arguments into the newly built Text is prone to clash with the placeholders of the Text being
	// built and may thereby accidentally lead to unintended effects. Instead, clients can either manually append the
	// Text's elements themselves and explicitly handle the importing of placeholder arguments, or they can
	// (recommended) add a new placeholder element and set the Text as its argument.
	// TODO Requires a TextElement#copy and the ability to append externally provided TextElements (which is potentially
	// unsafe).

	/**
	 * Appends a {@link SubText} element with the contents of the given {@link TextBuilder}.
	 * 
	 * @param subTextBuilder
	 *            the builder for the SubText
	 * @return this TextBuilder
	 */
	public TextBuilder subText(TextBuilder subTextBuilder) {
		Validate.notNull(subTextBuilder, "subTextBuilder is null");
		Validate.isTrue(!subTextBuilder.isBuilt(), "subTextBuilder has already been built");
		SubText subText = subTextBuilder.buildSubText();
		return this.append(subText);
	}

	/**
	 * Appends a {@link PlainText} element the given textual contents.
	 * 
	 * @param text
	 *            the textual contents
	 * @return this TextBuilder
	 */
	public TextBuilder text(String text) {
		return this.append(new PlainText(text));
	}

	/**
	 * Appends a {@link PlainText} element which uses the given object's {@link Object#toString() String representation}
	 * as its text.
	 * <p>
	 * If the given object is a {@link Supplier}, it gets invoked to obtain the actual object. If the object is
	 * <code>null</code>, the String <code>"null"</code> is used.
	 * <p>
	 * The object being appended cannot be a {@link Text} or {@link TextBuilder} itself.
	 * 
	 * @param object
	 *            the object
	 * @return this TextBuilder
	 */
	public TextBuilder text(Object object) {
		if (object instanceof Supplier) {
			object = ((Supplier<?>) object).get();
		}
		Validate.isTrue(!(object instanceof Text), "The object being appended cannot be a Text!");
		Validate.isTrue(!(object instanceof TextBuilder), "The object being appended cannot be a TextBuilder!");
		return this.text(String.valueOf(object));
	}

	/**
	 * Appends a {@link PlainText} element with the newline symbol as text.
	 * 
	 * @return this TextBuilder
	 */
	public TextBuilder newline() {
		return this.text("\n");
	}

	/**
	 * Appends a {@link Formatting} element with the given {@link ChatColor}.
	 * <p>
	 * The chat color can be a {@link ChatColor#isColor() color}, a {@link ChatColor#isFormat() format}, or
	 * {@link ChatColor#RESET}.
	 * 
	 * @param chatColor
	 *            the chat color
	 * @return this TextBuilder
	 */
	public TextBuilder formatting(ChatColor chatColor) {
		return this.append(new Formatting(chatColor));
	}

	/**
	 * Appends a {@link Formatting} element with the given {@link ChatColor}.
	 * <p>
	 * This is simply an alias for {@link #formatting(ChatColor)} and accepts any type of {@link ChatColor}.
	 * 
	 * @param chatColor
	 *            the chat color
	 * @return this TextBuilder
	 */
	public TextBuilder color(ChatColor chatColor) {
		return this.formatting(chatColor);
	}

	/**
	 * Appends a {@link Formatting} element which resets any previous formatting.
	 * <p>
	 * This is shortcut for calling {@link #formatting(ChatColor)} with {@link ChatColor#RESET}.
	 * 
	 * @return this TextBuilder
	 */
	public TextBuilder reset() {
		return this.formatting(ChatColor.RESET);
	}

	/**
	 * Appends a {@link Translatable} element.
	 * 
	 * @param translationKey
	 *            the translation key
	 * @param
	 * @return
	 */
	public TextBuilder translatable(String translationKey, List<Text> arguments, Text fallbackText) {
		return this.append(new Translatable(translationKey, arguments, fallbackText));
	}

	public TextBuilder translatable(String translationKey, List<Text> arguments) {
		return this.translatable(translationKey, arguments, null);
	}

	public TextBuilder translatable(String translationKey) {
		return this.translatable(translationKey, Collections.emptyList(), null);
	}

	// FLUENT NEXT BUILDER

	/**
	 * Creates a new placeholder {@link TextBuilder} and sets it as {@link #next(Text) next} Text.
	 * 
	 * @param placeholderKey
	 *            the placeholder key
	 * @return the new {@link TextBuilder}
	 */
	public TextBuilder placeholder(String placeholderKey) {
		return this.next(Text.placeholder(placeholderKey));
	}

	/**
	 * Creates a new {@link TextBuilder} with the specified hover event and sets it as {@link #next(Text) next} Text.
	 * 
	 * @param action
	 *            the hover event action
	 * @param value
	 *            the hover event value
	 * @return the new {@link TextBuilder}
	 */
	public TextBuilder hoverEvent(HoverEventText.Action action, Text value) {
		return this.next(Text.hoverEvent(action, value));
	}

	/**
	 * Creates a new {@link TextBuilder} with the specified hover text and sets it as {@link #next(Text) next} Text.
	 * <p>
	 * This is a shortcut for the corresponding {@link #hoverEvent(HoverEventText.Action, Text)}.
	 * 
	 * @param hoverText
	 *            the hover text
	 * @return the new {@link TextBuilder}
	 */
	public TextBuilder hoverEvent(Text hoverText) {
		return this.next(Text.hoverEvent(hoverText));
	}

	/**
	 * Creates a new {@link TextBuilder} with the specified click event and sets it as {@link #next(Text) next} Text.
	 * 
	 * @param action
	 *            the click event action
	 * @param value
	 *            the click event value
	 * @return the new {@link TextBuilder}
	 */
	public TextBuilder clickEvent(ClickEventText.Action action, String value) {
		return this.next(Text.clickEvent(action, value));
	}

	/**
	 * Creates a new {@link TextBuilder} with the given insertion text and sets it as {@link #next(Text) next} Text.
	 * 
	 * @param insertion
	 *            the insertion text
	 * @return the new {@link TextBuilder}
	 */
	public TextBuilder insertion(String insertion) {
		return this.next(Text.insertion(insertion));
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		// child:
		Text child = this.getChild();
		if (child != null) {
			((AbstractText) child).appendPlainText(builder, formatText);
		}

		// next:
		Text next = this.getNext();
		if (next != null) {
			((AbstractText) next).appendPlainText(builder, formatText);
		}
	}

	@Override
	public boolean isPlainText() {
		// child:
		Text child = this.getChild();
		if (child != null && !child.isPlainText()) {
			return false;
		}

		// next:
		Text next = this.getNext();
		if (next != null && !next.isPlainText()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isPlainTextEmpty() {
		// child:
		Text child = this.getChild();
		if (child != null && !child.isPlainTextEmpty()) {
			return false;
		}

		// next:
		Text next = this.getNext();
		if (next != null && !next.isPlainTextEmpty()) {
			return false;
		}
		return true;
	}

	// COPY

	@Override
	public abstract Text copy();

	/**
	 * Copies the properties of the given source Text.
	 * <p>
	 * Copied child and subsequent Texts will be unmodifiable already.
	 * 
	 * @param sourceText
	 *            the source Text
	 * @param copyChilds
	 *            <code>true</code> to also (deeply) copy the child and subsequent Texts, <code>false</code> to omit
	 *            them and keep any currently set child and subsequent Texts
	 * @return this
	 */
	public TextBuilder copy(Text sourceText, boolean copyChilds) {
		Validate.notNull(sourceText, "The given source Text is null!");
		if (copyChilds) {
			this.copyChild(sourceText);
			this.copyNext(sourceText);
		}
		return this;
	}

	// implemented in separate methods to allow for individual overriding:

	protected void copyChild(Text sourceText) {
		Text sourceChild = sourceText.getChild();
		if (sourceChild != null) {
			this.child(sourceChild.copy());
		}
	}

	protected void copyNext(Text sourceText) {
		Text sourceNext = sourceText.getNext();
		if (sourceNext != null) {
			this.next(sourceNext.copy());
		}
	}

	// JAVA OBJECT

	// allows for easier extension in subclasses
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", child=");
		builder.append(this.getChild());
		builder.append(", next=");
		builder.append(this.getNext());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName());
		builder.append(" [");
		// built property is always appended first (and without the comma):
		builder.append("built=");
		builder.append(this.isBuilt());
		this.appendToStringFeatures(builder);
		builder.append("]");
		return builder.toString();
	}

	//////////

	public static class SubTextBuilder extends TextBuilder {

	}
}
