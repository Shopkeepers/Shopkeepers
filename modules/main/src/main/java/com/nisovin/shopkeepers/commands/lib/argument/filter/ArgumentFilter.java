package com.nisovin.shopkeepers.commands.lib.argument.filter;

import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.text.Text;

/**
 * A filter that tests parsed arguments.
 *
 * @param <T>
 *            the type of the filtered parsed arguments
 */
public abstract class ArgumentFilter<T> implements Predicate<T> {

	private static final ArgumentFilter<@Nullable Object> ACCEPT_ANY = new ArgumentFilter<@Nullable Object>() {
		@Override
		public boolean test(@Nullable Object value) {
			return true;
		}
	};

	/**
	 * Gets a {@link ArgumentFilter} that accepts any values.
	 * 
	 * @param <T>
	 *            the type of the filtered parsed arguments
	 * @return the argument filter, not <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static <T> ArgumentFilter<T> acceptAny() {
		return (ArgumentFilter<T>) ACCEPT_ANY;
	}

	/**
	 * Gets the 'invalid argument' error message for the given parsed but declined value.
	 * <p>
	 * When overriding this method, consider using {@link CommandArgument#getDefaultErrorMsgArgs()}
	 * for the common message arguments.
	 * <p>
	 * Consider using an {@link ArgumentRejectedException} when using the returned message for an
	 * exception.
	 * 
	 * @param argument
	 *            the argument using this filter, not <code>null</code>
	 * @param argumentInput
	 *            the argument input, not <code>null</code>
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the error message
	 */
	public Text getInvalidArgumentErrorMsg(
			CommandArgument<?> argument,
			String argumentInput,
			T value
	) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	/**
	 * Gets a {@link ArgumentRejectedException} that uses this filter's
	 * {@link #getInvalidArgumentErrorMsg(CommandArgument, String, Object) invalid argument error
	 * message}.
	 * 
	 * @param argument
	 *            the argument using this filter, not <code>null</code>
	 * @param argumentInput
	 *            the argument input, not <code>null</code>
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the {@link ArgumentRejectedException}
	 */
	public ArgumentRejectedException rejectedArgumentException(
			CommandArgument<?> argument,
			String argumentInput,
			T value
	) {
		return new ArgumentRejectedException(
				argument,
				this.getInvalidArgumentErrorMsg(argument, argumentInput, value)
		);
	}
}
