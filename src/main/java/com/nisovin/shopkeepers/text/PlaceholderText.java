package com.nisovin.shopkeepers.text;

import java.util.Map;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Text} that uses a dynamically set {@link #getPlaceholderArgument() placeholder argument} as its child.
 * <p>
 * If no placeholder argument is set this Text acts like a plain Text which simply consists of its
 * {@link #getFormattedPlaceholderKey() formatted placeholder key}.
 * <p>
 * The placeholder's {@link #getChild() child} cannot be set manually.
 */
public class PlaceholderText extends TextBuilder {

	public static final char PLACEHOLDER_PREFIX_CHAR = '{';
	public static final char PLACEHOLDER_SUFFIX_CHAR = '}';

	private final String placeholderKey; // not null or empty
	private final String formattedPlaceholderKey; // not null or empty
	private Text placeholderArgument = null; // can be null, not unbuilt

	// not intended for public use, use Text.placeholder(String) instead
	PlaceholderText(String placeholderKey) {
		Validate.notEmpty(placeholderKey, "Placeholder key is null or empty!");
		this.placeholderKey = placeholderKey;
		this.formattedPlaceholderKey = (PLACEHOLDER_PREFIX_CHAR + placeholderKey + PLACEHOLDER_SUFFIX_CHAR);
	}

	// PLACEHOLDER

	private UnsupportedOperationException unsupportedPlaceholderOperation() {
		return new UnsupportedOperationException("This operation is not supported for placeholder Texts!");
	}

	/**
	 * Gets the placeholder key.
	 * 
	 * @return the placeholder key, not <code>null</code> or empty
	 */
	public String getPlaceholderKey() {
		return placeholderKey;
	}

	/**
	 * Gets the {@link #getPlaceholderKey() placeholder key} in the format <code>{key}</code>.
	 * 
	 * @return the formatted placeholder key, not <code>null</code> or empty
	 */
	public String getFormattedPlaceholderKey() {
		return formattedPlaceholderKey;
	}

	/**
	 * Gets the argument {@link Text} that is currently assigned to this {@link PlaceholderText}.
	 * <p>
	 * This Text adopts all properties of the argument Text. If no placeholder argument is set this Text acts like a
	 * plain Text which simply returns the {@link #getFormattedPlaceholderKey() formatted placeholder key} for its
	 * {@link #getText() text}.
	 * 
	 * @return the argument Text, or <code>null</code> if no argument is set
	 */
	public Text getPlaceholderArgument() {
		return placeholderArgument;
	}

	/**
	 * Checks if this {@link PlaceholderText} has an argument assigned currently.
	 * 
	 * @return <code>true</code> if an argument is assigned
	 */
	public boolean hasPlaceholderArgument() {
		return (placeholderArgument != null);
	}

	/**
	 * Sets the argument {@link Text} that is currently assigned to this {@link PlaceholderText}.
	 * <p>
	 * Any non-{@link Text} argument gets first converted to a corresponding Text by using its {@link Object#toString()
	 * String representation}. If the argument is a {@link Supplier} it will be invoked to obtain the actual argument.
	 * <p>
	 * If the argument is a {@link Text} that has not yet been {@link AbstractTextBuilder#isBuilt() built}, this method
	 * may build it.
	 * 
	 * @param placeholderArgument
	 *            the argument Text, or <code>null</code> to unset the argument
	 * @return this Text
	 * @see #getPlaceholderArgument()
	 */
	public void setPlaceholderArgument(Object placeholderArgument) {
		Text placeholderArgumentText = null;
		if (placeholderArgument != null) {
			Validate.isTrue(placeholderArgument != this, "Cannot set self as placeholder argument!");
			placeholderArgumentText = Text.of(placeholderArgument);
			Validate.isTrue(placeholderArgumentText.getParent() == null, "Cannot use non-root Text as placeholder argument!");

			// build unbuilt argument:
			buildIfRequired(placeholderArgumentText);

			// TODO set parent?
			// not setting the parent allows reusing the same placeholder argument Text instances more easily
			// setting the parent would currently block the Text from being used as placeholder argument in more than
			// one Text at once
		}
		this.placeholderArgument = placeholderArgumentText; // can be null
	}

	// PLACEHOLDER ARGUMENTS

	@Override
	public Text setPlaceholderArguments(Map<String, ?> arguments) {
		// temporarily clear placeholder argument (if any) to not delegate to it via child delegation:
		Text prevArgument = this.getPlaceholderArgument();
		this.setPlaceholderArgument(null);

		try {
			// handles validation and delegation:
			super.setPlaceholderArguments(arguments);

			Object argument = arguments.get(placeholderKey); // can be null
			if (argument != null) {
				this.setPlaceholderArgument(argument);
				prevArgument = null; // don't restore previous argument
			} // else retain currently assigned argument if any
		} finally {
			// restore previous placeholder argument:
			if (prevArgument != null) {
				this.setPlaceholderArgument(prevArgument);
			}
		}
		return this;
	}

	@Override
	public Text clearPlaceholderArguments() {
		// clear first to not delegate to it via child delegation:
		this.setPlaceholderArgument(null);

		// handles delegation:
		super.clearPlaceholderArguments();
		return this;
	}

	// CHILD

	@Override
	public Text getChild() {
		return placeholderArgument; // can be null
	}

	@Override
	public <T extends Text> T child(T child) {
		throw unsupportedPlaceholderOperation();
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		if (formatText || !this.hasPlaceholderArgument()) {
			// temporarily clear placeholder argument (if any) to not append it via child delegation:
			Text prevArgument = this.getPlaceholderArgument();
			this.setPlaceholderArgument(null);

			// append formatted placeholder key:
			builder.append(this.getFormattedPlaceholderKey());

			try {
				super.appendPlainText(builder, formatText);
			} finally {
				// restore previous placeholder argument:
				if (prevArgument != null) {
					this.setPlaceholderArgument(prevArgument);
				}
			}
		} else {
			super.appendPlainText(builder, formatText);
		}
	}

	// COPY

	@Override
	public Text copy() {
		PlaceholderText copy = new PlaceholderText(placeholderKey);
		copy.copy(this, true);
		return copy.build();
	}

	/**
	 * This will not copy the placeholder key. Any currently set placeholder argument gets (deeply) copied.
	 */
	@Override
	public PlaceholderText copy(Text sourceText, boolean copyChilds) {
		super.copy(sourceText, copyChilds);
		this.copyPlaceholderArgument(sourceText);
		return this;
	}

	@Override
	protected void copyChild(Text sourceText) {
		return; // not copying child
	}

	protected void copyPlaceholderArgument(Text sourceText) {
		if (sourceText instanceof PlaceholderText) {
			PlaceholderText placeholderSourceText = (PlaceholderText) sourceText;
			Text placeholderArgument = placeholderSourceText.getPlaceholderArgument();
			this.setPlaceholderArgument(placeholderArgument != null ? placeholderArgument.copy() : null);
		}
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", placeholderKey=");
		builder.append(placeholderKey);
		// note: placeholder argument gets already appended as 'child'
		super.appendToStringFeatures(builder);
	}
}
