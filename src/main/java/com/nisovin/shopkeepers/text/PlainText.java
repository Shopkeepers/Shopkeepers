package com.nisovin.shopkeepers.text;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Text} containing plain text.
 * <p>
 * Even though this is not enforced currently, this type of {@link Text} should ideally not contain legacy color codes.
 * Using legacy color codes inside a {@link PlainText} can cause issues in certain edge cases, such as issues related to
 * the inheritance of text formatting, texts spanning multiple lines, or when converting the Text to plain unformatted
 * text (which is not supposed to contain formatting codes).
 */
public class PlainText extends TextBuilder {

	private final String text; // not null, can be empty

	PlainText(String text) {
		Validate.notNull(text, "text is null");
		this.text = text;
	}

	// TEXT

	/**
	 * Gets the text.
	 * 
	 * @return the text, not <code>null</code>, can be empty
	 */
	public String getText() {
		return text;
	}

	// PLAIN TEXT

	@Override
	protected void appendPlainText(StringBuilder builder, boolean formatText) {
		builder.append(this.getText());
		super.appendPlainText(builder, formatText);
	}

	@Override
	public boolean isPlainTextEmpty() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		PlainText copy = new PlainText(text);
		copy.copy(this, true);
		return copy.build();
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", text=");
		builder.append(this.getText());
		super.appendToStringFeatures(builder);
	}
}
