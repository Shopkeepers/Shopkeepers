package com.nisovin.shopkeepers.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link Text} that gets translated on the client.
 * <p>
 * In case translatable texts are not supported or when converting to {@link #toPlainText() plain} or
 * {@link #toPlainFormatText() plain format} text, its {@link #getChild() child} is used as fallback. Otherwise the
 * child is ignored.
 */
public class TranslatableText extends TextBuilder {

	private final String translationKey; // not null or empty
	// only contains already built Texts:
	private List<Text> translationArguments = Collections.emptyList(); // not null, can be empty, unmodifiable view

	TranslatableText(String translationKey) {
		Validate.notEmpty(translationKey, "Translation key is empty!");
		this.translationKey = translationKey;
	}

	// TRANSLATABLE

	/**
	 * Gets the translation key.
	 * 
	 * @return the translationKey, not <code>null</code>
	 */
	public String getTranslationKey() {
		return translationKey;
	}

	/**
	 * Gets the translation arguments.
	 * 
	 * @return an unmodifiable view on the translation arguments, not <code>null</code>, can be empty
	 */
	public List<Text> getTranslationArguments() {
		return translationArguments;
	}

	/**
	 * Sets the translation arguments.
	 * <p>
	 * Any non-{@link Text} argument gets first converted to a corresponding Text by using its {@link Object#toString()
	 * String representation}. If the argument is a {@link Supplier} it will be invoked to obtain the actual argument.
	 * <p>
	 * Any {@link Text} translation arguments that are not yet {@link AbstractTextBuilder#isBuilt() built} may get built
	 * by this method.
	 * 
	 * @param translationArguments
	 *            the translation arguments, or <code>null</code> or empty to unset
	 * @return this Text
	 */
	public TranslatableText setTranslationArguments(List<?> translationArguments) {
		if (translationArguments == null || translationArguments.isEmpty()) {
			this.translationArguments = Collections.emptyList(); // resetting is always allowed
		} else {
			List<Text> translationTextArguments = new ArrayList<>(translationArguments.size());
			for (Object argument : translationArguments) {
				Validate.notNull(argument, "One of the translation arguments is null!");
				Validate.isTrue(argument != this, "Cannot set self as translation argument!");

				Text argumentText = Text.of(argument);
				Validate.isTrue(argumentText.getParent() == null, "Cannot use non-root Text as translation argument!");
				translationTextArguments.add(argumentText);
			}

			// build all unbuilt arguments:
			for (Text argument : translationTextArguments) {
				buildIfRequired(argument);
			}

			this.translationArguments = Collections.unmodifiableList(translationTextArguments);
		}
		return this;
	}

	// PLAIN TEXT

	@Override
	public boolean isPlainText() {
		return false;
	}

	// COPY

	@Override
	public Text copy() {
		TranslatableText copy = new TranslatableText(translationKey);
		copy.copy(this, true);
		return copy.build();
	}

	/**
	 * This will not copy the translation key. Any currently set translation arguments get (deeply) copied.
	 */
	@Override
	public TranslatableText copy(Text sourceText, boolean copyChilds) {
		super.copy(sourceText, copyChilds);
		this.copyTranslationArguments(sourceText);
		return this;
	}

	protected void copyTranslationArguments(Text sourceText) {
		if (sourceText instanceof TranslatableText) {
			TranslatableText translatableSourceText = (TranslatableText) sourceText;
			this.setTranslationArguments(copyAll(translatableSourceText.getTranslationArguments()));
		}
	}

	private static List<Text> copyAll(Collection<? extends Text> toCopy) {
		if (toCopy == null) return null;
		if (toCopy.isEmpty()) return Collections.emptyList();
		List<Text> copies = new ArrayList<>(toCopy.size());
		for (Text text : toCopy) {
			copies.add(text.copy());
		}
		return copies;
	}

	// JAVA OBJECT

	@Override
	protected void appendToStringFeatures(StringBuilder builder) {
		builder.append(", translationKey=");
		builder.append(translationKey);
		builder.append(", translationArguments=");
		builder.append(translationArguments);
		super.appendToStringFeatures(builder);
	}
}
