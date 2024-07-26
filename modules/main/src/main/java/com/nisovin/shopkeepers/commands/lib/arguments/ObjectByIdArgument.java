package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for arguments that accept some form of identifier (e.g. UUID or name) for specifying a
 * corresponding object.
 *
 * @param <I>
 *            the identifier type
 * @param <O>
 *            the object type
 */
public abstract class ObjectByIdArgument<@NonNull I, @NonNull O>
		extends CommandArgument<@NonNull O> {

	protected final ArgumentFilter<? super O> filter; // Not null
	protected final ObjectIdArgument<I> idArgument;

	public ObjectByIdArgument(
			String name,
			ArgumentFilter<? super O> filter,
			IdArgumentArgs idArgumentArgs
	) {
		super(name);
		Validate.notNull(filter, "filter is null");
		this.filter = filter;
		this.idArgument = this.createIdArgument(name + ":id", idArgumentArgs);
		this.idArgument.setParent(this);
	}

	protected static class IdArgumentArgs {

		public final int minimumCompletionInput;
		public final boolean joinRemainingArgs;

		public IdArgumentArgs(int minimumCompletionInput) {
			this(minimumCompletionInput, false);
		}

		public IdArgumentArgs(int minimumCompletionInput, boolean joinRemainingArgs) {
			this.minimumCompletionInput = minimumCompletionInput;
			this.joinRemainingArgs = joinRemainingArgs;
		}
	}

	// Implementation note: Usually, we don't use an id filter here. Instead, we filter directly
	// which objects are involved in generating the suggestions. To achieve that, the created
	// id-argument has to delegate its ObjectIdArgument#getCompletionSuggestions(String)
	// implementation to ObjectByIdArgument#getCompletionSuggestions(String), which should take this
	// argument's object filter into account.
	protected abstract ObjectIdArgument<I> createIdArgument(
			// TODO Eclipse (2024-06): Cannot add type annotation here (@UnknownInitialization)
			ObjectByIdArgument<I, O> this,
			String name,
			IdArgumentArgs args
	);

	@Override
	public Text getMissingArgumentErrorMsg() {
		return idArgument.getMissingArgumentErrorMsg();
	}

	// Implementation note: Consider overriding #getInvalidArgumentErrorMsg

	/**
	 * Gets the object that corresponds to the given id.
	 * <p>
	 * The given command input and context can be used to limit the scope of the considered objects.
	 * <p>
	 * Returning <code>null</code> indicates that no corresponding object could be found.
	 * <code>null</code> itself is never considered to be a valid object.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param id
	 *            the id, not <code>null</code>
	 * @return the corresponding object, or <code>null</code> if no such object is found
	 * @throws ArgumentParseException
	 *             if the id is ambiguous
	 */
	protected abstract @Nullable O getObject(
			CommandInput input,
			CommandContextView context,
			@NonNull I id
	) throws ArgumentParseException;

	@Override
	public @NonNull O parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// Parse id: This deals with invalid and missing input.
		I id = idArgument.parseValue(input, context, argsReader);
		@Nullable O object = this.getObject(input, context, id);
		if (object == null) {
			// No corresponding object found:
			throw this.invalidArgumentError(idArgument.toString(id));
		}
		if (!filter.test(object)) {
			// Rejected by the filter:
			throw filter.rejectedArgumentException(this, idArgument.toString(id), object);
		}
		return object;
	}

	/**
	 * Gets the completion suggestions for the given id prefix.
	 * <p>
	 * This should take this argument's object filter into account.
	 * <p>
	 * The given command input and context can be used to limit the scope of the considered objects.
	 * <p>
	 * The id-argument created by {@link #createIdArgument(String, IdArgumentArgs)} should delegate
	 * to this method.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected abstract Iterable<? extends @NonNull I> getCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String idPrefix
	);

	@Override
	public List<? extends @NonNull String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return idArgument.complete(input, context, argsReader);
	}
}
