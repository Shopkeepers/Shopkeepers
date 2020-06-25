package com.nisovin.shopkeepers.commands.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class Command {

	public static final String COMMAND_PREFIX = "/";
	public static final String ARGUMENTS_SEPARATOR = " ";

	private static final Text DEFAULT_HELP_TITLE_FORMAT = Text
			.color(ChatColor.AQUA).text("-------[ ")
			.color(ChatColor.DARK_GREEN).text("Command Help: ")
			.color(ChatColor.GOLD).formatting(ChatColor.ITALIC)
			.placeholder("command")
			.color(ChatColor.AQUA).text(" ]-------")
			.buildRoot();
	private static final Text DEFAULT_HELP_USAGE_FORMAT = Text
			.color(ChatColor.YELLOW).placeholder("usage")
			.buildRoot();
	private static final Text DEFAULT_HELP_DESC_FORMAT = Text
			.color(ChatColor.DARK_GRAY).text(" - ")
			.color(ChatColor.DARK_AQUA).placeholder("description")
			.buildRoot();
	private static final Text HELP_ENTRY_FORMAT = Text
			.placeholder("usage") // the usage format
			.placeholder("description") // the description format
			.buildRoot();

	private final String name;
	private final List<String> aliases; // unmodifiable
	private Text description = Text.EMPTY;
	// null if no permission is required:
	private String permission = null;
	private final List<CommandArgument<?>> arguments = new ArrayList<>();
	private Command parent = null;
	private final CommandRegistry childCommands = new CommandRegistry(this);

	// hides this command from the help page:
	private boolean hiddenInParentHelp = false;
	private boolean hiddenInOwnHelp = false;
	// makes the parent help content display this command's child commands:
	private boolean includeChildsInParentHelp = false;

	// common message arguments:
	private final Map<String, Object> commonMessageArgs;
	{
		// dynamically evaluated:
		Map<String, Object> commonMessageArgs = new HashMap<>();
		commonMessageArgs.put("name", (Supplier<?>) () -> this.getName());
		commonMessageArgs.put("description", (Supplier<?>) () -> this.getDescription());
		commonMessageArgs.put("command", (Supplier<?>) () -> this.getCommandFormat());
		commonMessageArgs.put("usage", (Supplier<?>) () -> this.getUsageFormat());
		commonMessageArgs.put("arguments", (Supplier<?>) () -> this.getArgumentsFormat());
		this.commonMessageArgs = Collections.unmodifiableMap(commonMessageArgs);
	}

	// formatting (null results in the parent's formatting to be used):
	private Text helpTitleFormat = null;
	private Text helpUsageFormat = null;
	private Text helpDescFormat = null;
	private Text helpChildUsageFormat = null;
	private Text helpChildDescFormat = null;

	public Command(String name) {
		this(name, null);
	}

	public Command(String name, List<String> aliases) {
		Validate.notEmpty(name, "Command name is empty!");
		this.name = name;

		// validate and copy aliases:
		if (aliases == null || aliases.isEmpty()) {
			this.aliases = Collections.emptyList();
		} else {
			List<String> aliasesCopy = new ArrayList<>(aliases);
			// validate aliases:
			for (String alias : aliasesCopy) {
				Validate.notEmpty(alias, "Command contains empty alias!");
				Validate.isTrue(!StringUtils.containsWhitespace(alias), "Command contains alias with whitespace!");
			}
			this.aliases = Collections.unmodifiableList(aliasesCopy);
		}
	}

	/**
	 * Gets the name of this command.
	 * <p>
	 * For {@link BaseCommand base commands} this name is supposed to be unique among the commands of the same plugin.
	 * There might conflicts with the commands of other plugins though.
	 * <p>
	 * For child commands this name is supposed to be unique among the child commands of the same parent command.
	 * 
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Gets all the aliases of this command.
	 * <p>
	 * Depending on the names and aliases of other commands, not all aliases might actually be active for this command.
	 * 
	 * @return an unmodifiable view on the aliases, might be empty (but not <code>null</code>)
	 */
	public final List<String> getAliases() {
		return aliases;
	}

	/**
	 * A short one-line description of what this command does.
	 * <p>
	 * Might be used in command listings and the command help.
	 * 
	 * @return the short description, might be {@link Text#isPlainTextEmpty() empty} to indicate that no description is
	 *         available
	 */
	public final Text getDescription() {
		return description;
	}

	/**
	 * Sets the short description for this command.
	 * 
	 * @param description
	 *            the description
	 * @see #getDescription()
	 */
	protected final void setDescription(Text description) {
		this.description = (description == null) ? Text.EMPTY : description;
	}

	/**
	 * Gets the permission which is required for executing this command.
	 * <p>
	 * This only affects execution of this specific command, and not those of the child commands. Each child command has
	 * to specify its requirements and behavior separately.
	 * <p>
	 * This permission is for example used by {@link #testPermission(CommandSender)}.
	 * 
	 * @return the required permission, <code>null</code> if no required permission is set
	 */
	public final String getPermission() {
		return permission;
	}

	/**
	 * Sets the required permission for being able to execute this command.
	 * 
	 * @param permission
	 *            the permission
	 * @see #getPermission()
	 */
	protected final void setPermission(String permission) {
		this.permission = permission;
	}

	/**
	 * Checks whether the given {@link CommandSender} is potentially allowed to execute this command.
	 * <p>
	 * This only affects execution of this specific command, and not those of the child commands. Each child command has
	 * to specify its requirements and behavior separately.
	 * <p>
	 * By default the implementation checks the permission returned by {@link #getPermission()}, but it may be
	 * overridden to check for additional {@link CommandSender} specific conditions. If implementations are unsure if
	 * the {@link CommandSender} can execute this command, <code>true</code> should be returned.
	 * <p>
	 * The result of this method may be used to determine whether this command is listed in command listings for the
	 * given {@link CommandSender}.
	 * 
	 * @param sender
	 *            the sender
	 * @return <code>true</code> if the given {@link CommandSender} might be allowed to execute this command
	 */
	public boolean testPermission(CommandSender sender) {
		Validate.notNull(sender);
		return permission == null || PermissionUtils.hasPermission(sender, permission);
	}

	/**
	 * Similar to {@link Command#testPermission(CommandSender)}, but throws an exception with feedback message in case
	 * the given {@link CommandSender} is not allowed to execute this command.
	 * 
	 * @param sender
	 *            the sender
	 * @throws NoPermissionException
	 *             if the sender is not allowed to execute this command
	 */
	public void checkPermission(CommandSender sender) throws NoPermissionException {
		Validate.notNull(sender);
		if (!this.testPermission(sender)) {
			throw this.noPermissionException();
		}
	}

	/**
	 * Checks if the given {@link CommandSender} has the specified permission, and throws a
	 * {@link NoPermissionException} with corresponding error message if not.
	 * 
	 * @param sender
	 *            the sender
	 * @param permission
	 *            the permission, can be <code>null</code>
	 * @throws NoPermissionException
	 *             if the sender does not have the permission
	 */
	public void checkPermission(CommandSender sender, String permission) throws NoPermissionException {
		Validate.notNull(sender);
		if (permission != null && !PermissionUtils.hasPermission(sender, permission)) {
			throw this.noPermissionException();
		}
	}

	protected NoPermissionException noPermissionException() {
		return new NoPermissionException(Settings.msgNoPermission);
	}

	/**
	 * Checks whether the given type of {@link CommandSender} is accepted to execute this command.
	 * <p>
	 * This only affects execution of this specific command, and not those of the child commands. Each child command has
	 * to specify its requirements and behavior separately.
	 * <p>
	 * If overridden, it is recommended to also override {@link #checkCommandSource(CommandSender)} in order to use a
	 * more specific error message there in case the command sender cannot use the command.
	 * 
	 * @param sender
	 *            the command sender
	 * @return <code>true</code> of the command sender is accepted
	 */
	public boolean isAccepted(CommandSender sender) {
		Validate.notNull(sender);
		// by default all command senders are accepted:
		return true;
	}

	private static final Text MSG_COMMAND_SOURCE_DENIED = Text.of("You cannot execute this command here!");

	/**
	 * Checks whether the given type of {@link CommandSender} is accepted to execute this command.
	 * <p>
	 * If the {@link CommandSender} is not accepted the thrown {@link CommandSourceRejectedException} contains a
	 * user-friendly rejection message.
	 * 
	 * @param sender
	 *            the sender, receives feedback
	 * @throws CommandSourceRejectedException
	 *             if the given type of command sender is not accepted to execute this command
	 */
	public void checkCommandSource(CommandSender sender) throws CommandSourceRejectedException {
		Validate.notNull(sender);
		if (!this.isAccepted(sender)) {
			throw new CommandSourceRejectedException(MSG_COMMAND_SOURCE_DENIED);
		}
	}

	/**
	 * Gets the command format of this command.
	 * <p>
	 * The command format depends on the chain of parent commands and does not include this command's arguments.<br>
	 * Example: For a command {@code '/mail send <player> <message>'} this would be {@code '/mail send'}.
	 * 
	 * @return the command format
	 */
	public final String getCommandFormat() {
		if (parent == null) {
			// this is a base command:
			return COMMAND_PREFIX + this.getName();
		} else {
			// append primary alias to the format of the parent:
			return parent.getCommandFormat() + ARGUMENTS_SEPARATOR + this.getName();
		}
	}

	/**
	 * Gets the arguments of this command.
	 * 
	 * @return an unmodifiable view on the arguments of this command
	 */
	public final List<CommandArgument<?>> getArguments() {
		return Collections.unmodifiableList(arguments);
	}

	/**
	 * Gets the argument with the specified name.
	 * 
	 * @param name
	 *            the argument name
	 * @return the argument, or <code>null</code> if there is no such argument
	 */
	public final CommandArgument<?> getArgument(String name) {
		for (CommandArgument<?> argument : arguments) {
			if (argument.getName().equals(name)) {
				return argument;
			}
		}
		return null;
	}

	/**
	 * Adds an {@link CommandArgument} to this command.
	 * 
	 * @param argument
	 *            the argument
	 */
	protected final void addArgument(CommandArgument<?> argument) {
		Validate.notNull(argument, "Argument is null!");
		Validate.isTrue(this.getArgument(argument.getName()) == null, "There is already another argument with that name: " + argument.getName());
		Validate.isTrue(argument.getParent() == null, "Cannot add argument with parent!");
		// make sure that no parent can be set once the argument has been added:
		argument.setParent(null); // parent can only be set once
		arguments.add(argument);
	}

	/**
	 * Gets the arguments format of this command.
	 * <p>
	 * Example: For a command {@code '/message <player> <message>'}, this would be {@code '<player> <message>'}.<br>
	 * If this command is a child-command, the argument format is meant to only contain the arguments for this
	 * child-command.
	 * <p>
	 * The returned format is empty if this command does not use any arguments, if all arguments are hidden, or if this
	 * command only acts as parent for other commands.
	 * 
	 * @return the arguments format, possibly empty
	 */
	public final String getArgumentsFormat() {
		if (arguments.isEmpty()) return "";
		StringBuilder argumentsFormat = new StringBuilder();
		for (CommandArgument<?> argument : arguments) {
			String argumentFormat = argument.getFormat();
			if (!argumentFormat.isEmpty()) {
				argumentsFormat.append(argumentFormat).append(ARGUMENTS_SEPARATOR);
			}
		}
		if (argumentsFormat.length() == 0) {
			return "";
		} else {
			return argumentsFormat.substring(0, argumentsFormat.length() - ARGUMENTS_SEPARATOR.length());
		}
	}

	/**
	 * Gets the usage format of this command.
	 * <p>
	 * This is basically the command format appended with the arguments format.
	 * 
	 * @return the usage format
	 */
	public final String getUsageFormat() {
		String usageFormat = this.getCommandFormat();
		// append arguments:
		String argsFormat = this.getArgumentsFormat();
		if (!argsFormat.isEmpty()) {
			usageFormat += ARGUMENTS_SEPARATOR + argsFormat;
		}
		return usageFormat;
	}

	/**
	 * Gets the common message arguments for this {@link Command}.
	 * <p>
	 * This includes:
	 * <ul>
	 * <li>{name}: The command's {@link #getName() name}.
	 * <li>{description}: The command's {@link #getDescription() description}.
	 * <li>{command}: The command's {@link #getCommandFormat() format}.
	 * <li>{usage}: The command's {@link #getUsageFormat() usage format}.
	 * <li>{arguments}: The command's {@link #getArgumentsFormat() arguments format}.
	 * </ul>
	 * 
	 * @return the common message arguments
	 */
	public final Map<String, Object> getCommonMessageArgs() {
		return commonMessageArgs;
	}

	public final Command getParent() {
		return parent;
	}

	// gets set by the parent command during registration of this command as child-command:
	final void setParent(Command parent) {
		this.parent = parent;
	}

	/**
	 * Gets the root command by following the chain of parent commands.
	 * 
	 * @return the root command
	 */
	public final Command getRootCommand() {
		if (parent != null) {
			return parent.getRootCommand();
		} else {
			return this;
		}
	}

	public final CommandRegistry getChildCommands() {
		return childCommands;
	}

	/**
	 * Checks if the arguments reader's next argument matches a child command.
	 * <p>
	 * If a matching child command is found, the arguments reader's cursor gets moved forward.
	 * 
	 * @param argsReader
	 *            the arguments reader
	 * @return the child command, or <code>null</code> if none was found
	 */
	protected Command getChildCommand(ArgumentsReader argsReader) {
		String childCommandAlias = argsReader.peekIfPresent();
		if (childCommandAlias != null) {
			Command childcommand = this.getChildCommands().getCommand(childCommandAlias);
			if (childcommand != null) {
				// move cursor forward for the successfully used-up argument:
				argsReader.next();
				return childcommand;
			}
		}
		// no matching child-command command was found:
		return null;
	}

	/**
	 * Processes the given inputs and then executes this command.
	 * <p>
	 * Unlike {@link #processCommand(CommandInput)} this includes handling of errors that occur during command
	 * processing.
	 * 
	 * @param input
	 *            the command input
	 */
	public void handleCommand(CommandInput input) {
		Validate.notNull(input, "Input is null!");
		Validate.isTrue(input.getCommand() == this.getRootCommand(), "Input is meant for a different command!");

		CommandSender sender = input.getSender();
		CommandContext context = new SimpleCommandContext();
		ArgumentsReader argsReader = new ArgumentsReader(input);
		try {
			this.processCommand(input, context, argsReader);
			Log.debug(Settings.DebugOptions.commands, () -> "Command succeeded. Context: " + context.toString());
		} catch (CommandException e) {
			TextUtils.sendMessage(sender, e.getMessageText());

			Log.debug(Settings.DebugOptions.commands, () -> {
				StringBuilder sb = new StringBuilder();
				sb.append("Command failed. Argument chain: ");
				CommandArgument<?> argument = null;
				if (e instanceof ArgumentParseException) {
					argument = ((ArgumentParseException) e).getArgument();
				}
				sb.append(this.getArgumentChain(argument));
				return sb.toString();
			});
			Log.debug(Settings.DebugOptions.commands, () -> "Context: " + context.toString());
			Log.debug(Settings.DebugOptions.commands, () -> "Arguments reader: " + argsReader.toString());
		} catch (Exception e) {
			// an unexpected exception was caught:
			TextUtils.sendMessage(sender, Text.color(ChatColor.RED).text("An error occurred during command handling! Check the console log."));
			Log.severe("An error occurred during command handling!", e);
			Log.severe("Context: " + context.toString());
		}
	}

	private String getArgumentChain(CommandArgument<?> argument) {
		if (argument == null) return "-";
		String delimiter = " < ";
		StringBuilder sb = new StringBuilder();
		CommandArgument<?> currentArgument = argument;
		while (currentArgument != null) {
			sb.append(currentArgument.getClass().getName());
			sb.append(" (");
			sb.append(currentArgument.getName());
			sb.append(")");
			sb.append(delimiter);
			currentArgument = currentArgument.getParent();
		}
		return sb.substring(0, sb.length() - delimiter.length());
	}

	/**
	 * Processes the given inputs and then executes this command.
	 * 
	 * @param input
	 *            the command input
	 * @throws CommandException
	 *             if command execution failed
	 */
	public void processCommand(CommandInput input) throws CommandException {
		Validate.notNull(input, "Input is null!");
		Validate.isTrue(input.getCommand() == this.getRootCommand(), "Input is meant for a different command!");

		CommandContext context = new SimpleCommandContext();
		ArgumentsReader argsReader = new ArgumentsReader(input);
		this.processCommand(input, context, argsReader);
	}

	/**
	 * Processes the given inputs and then executes this command.
	 * <p>
	 * This handles a few common things before the command gets actually executed, like:
	 * <ul>
	 * <li>Passing the command handling to a matching child-command, otherwise:
	 * <li>Checking if the {@link CommandSender} is accepted.
	 * <li>Checking the command permission for the {@link CommandSender}.
	 * <li>Parsing the command arguments.
	 * <li>And finally executing this command via {@link #execute(CommandInput, CommandContext)}.
	 * </ul>
	 * 
	 * @param input
	 *            the command input
	 * @throws CommandException
	 *             if command execution failed
	 */
	protected void processCommand(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws CommandException {
		assert input != null && context != null && argsReader != null;
		assert (input.getCommand() == this.getRootCommand()); // input is meant for this command
		assert (argsReader.getArgs() != input.getArguments()); // arguments reader is consistent with input

		// search for matching child-command:
		Command childCommand = this.getChildCommand(argsReader);
		if (childCommand != null) {
			// delegate to child-command:
			childCommand.processCommand(input, context, argsReader);
		} else { // no applicable child-command was found
			CommandSender sender = input.getSender();
			// check if this type of command sender supported:
			this.checkCommandSource(sender);

			// check if the command sender has the required permission to proceed:
			this.checkPermission(sender);

			// parse arguments:
			this.parseArguments(input, context, argsReader);

			// execute this command:
			this.execute(input, context.getView());
		}
	}

	// PARSING

	/**
	 * Used internally to keep track of the parsing state.
	 */
	protected static class ParsingContext {

		public final CommandInput input;
		public CommandContext context; // the currently active context
		public final ArgumentsReader argsReader;
		public final int argumentsCount;

		public int currentArgumentIndex = 0;
		public ArgumentParseException currentParseException = null;

		// linked list:
		public Fallback pendingFallback = null;

		protected ParsingContext(CommandInput input, CommandContext context, ArgumentsReader argsReader, int argumentsCount) {
			assert input != null && context != null && argsReader != null && argumentsCount >= 0;
			this.input = input;
			this.context = context;
			this.argsReader = argsReader;
			this.argumentsCount = argumentsCount;
		}

		public boolean hasPendingFallback() {
			return (pendingFallback != null);
		}

		public Fallback getPendingFallback() {
			return pendingFallback;
		}

		public boolean hasUnparsedCommandArguments() {
			return (currentArgumentIndex < (argumentsCount - 1));
		}
	}

	protected static class Fallback {

		protected final Fallback previousPendingFallback; // can be null
		protected final int argumentIndex;

		private final FallbackArgumentException exception;
		private final BufferedCommandContext bufferedContext;
		private final ArgumentsReader originalArgsReader; // snapshot from before the fallback

		protected Fallback(	Fallback previousPendingFallback, int argumentIndex, FallbackArgumentException exception,
							BufferedCommandContext bufferedContext, ArgumentsReader originalArgsReader) {
			assert exception != null && bufferedContext != null && originalArgsReader != null;
			this.previousPendingFallback = previousPendingFallback;
			this.argumentIndex = argumentIndex;
			this.exception = exception;
			this.bufferedContext = bufferedContext;
			this.originalArgsReader = originalArgsReader;
		}

		/**
		 * Gets the {@link FallbackArgumentException} that initiated this fallback.
		 * 
		 * @return the fallback argument exception
		 */
		public FallbackArgumentException getException() {
			return exception;
		}

		/**
		 * Gets the {@link FallbackArgument} that created the exception and may be able to provide a fallback.
		 * 
		 * @return the fallback argument
		 */
		public FallbackArgument<?> getFallbackArgument() {
			return exception.getArgument();
		}

		/**
		 * Gets the {@link BufferedCommandContext} that tracks context changes that happen while the fallback is
		 * pending.
		 * 
		 * @return the buffered command context
		 */
		public BufferedCommandContext getBufferedContext() {
			return bufferedContext;
		}

		/**
		 * Gets the original {@link CommandContext} from before the fallback.
		 * <p>
		 * This uses the {@link #getBufferedContext() buffered context's}
		 * {@link BufferedCommandContext#getParentContext() parent context} and therefore only matches the original
		 * state as long as the buffered context's buffer has not yet been applied.
		 * 
		 * @return the original context
		 */
		public CommandContext getOriginalContext() {
			return bufferedContext.getParentContext();
		}

		/**
		 * Gets the {@link ArgumentsReader} at the state from before the fallback.
		 * 
		 * @return the arguments reader at the state from before the fallback
		 */
		public ArgumentsReader getOriginalArgsReader() {
			return originalArgsReader;
		}
	}

	/**
	 * Parses all command arguments for this command.
	 * 
	 * @param input
	 *            the input
	 * @param context
	 *            the command context to store the parsed values in
	 * @param argsReader
	 *            the arguments reader
	 * @throws ArgumentParseException
	 *             if a required argument cannot be parsed or there are unparsed remaining arguments
	 */
	protected void parseArguments(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws ArgumentParseException {
		// setup parsing context:
		int argumentsCount = arguments.size();
		ParsingContext parsingContext = new ParsingContext(input, context, argsReader, argumentsCount);

		// parse all arguments:
		for (; parsingContext.currentArgumentIndex < argumentsCount; ++parsingContext.currentArgumentIndex) {
			CommandArgument<?> argument = arguments.get(parsingContext.currentArgumentIndex);

			// parse argument:
			this.parseArgument(argument, parsingContext);

			// handle fallback(s) (if any):
			this.handleFallbacks(parsingContext);
		}

		// handle unparsed arguments (if any):
		this.handleUnparsedArguments(parsingContext);

		// parsing succeeded
	}

	protected void parseArgument(CommandArgument<?> argument, ParsingContext parsingContext) {
		CommandContext context = parsingContext.context;
		ArgumentsReader argsReader = parsingContext.argsReader;

		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		ArgumentParseException parseException = null;
		try {
			// on success this stores any parsed values inside the context:
			argument.parse(parsingContext.input, context, argsReader);
		} catch (FallbackArgumentException e) {
			Log.debug(Settings.DebugOptions.commands, () -> "Fallback for argument '" + argument.getName() + "': " + e.getMessage());
			argsReader.setState(argsReaderState); // restore previous args reader state

			// keep track of context changes while continuing with the pending fallback:
			BufferedCommandContext bufferedContext = new BufferedCommandContext(context);
			parsingContext.context = bufferedContext;

			// set pending fallback (also remembers the previous pending fallback):
			Fallback fallback = new Fallback(parsingContext.pendingFallback, parsingContext.currentArgumentIndex, e, bufferedContext, argsReaderState);
			parsingContext.pendingFallback = fallback;
		} catch (ArgumentParseException e) { // parsing failed
			argsReader.setState(argsReaderState); // restore previous args reader state
			parseException = e;
		}
		assert !(parseException instanceof FallbackArgumentException);
		parsingContext.currentParseException = parseException; // resets to null on success or fallback
	}

	protected void handleFallbacks(ParsingContext parsingContext) throws ArgumentParseException {
		ArgumentParseException parseException = parsingContext.currentParseException;
		assert !(parseException instanceof FallbackArgumentException);

		Fallback fallback = parsingContext.getPendingFallback();
		if (fallback == null) { // no pending fallback(s)
			if (parseException != null) {
				throw parseException; // parsing failed at the current argument
			} else {
				return; // parsing successful, continue with the next argument (if any)
			}
		}

		// Note: We attempt to fully parse all comamnd arguments after the fallback before evaluating the fallback.
		// Evaluating the fallback right after the first subsequent command argument either failed or was able to parse
		// arguments is not sufficient.
		// Consider for example the command "/list [player] [page]" with inputs "/list 2" and "/list 123 2":
		// If the first argument in the input doesn't match any online player, a fallback is used to check if the
		// argument matches a page number instead (first case) and then the executing player is used as convenient
		// fallback value.
		// However, this heuristic fails in the second case, because numbers can (in the unlikely case) also be valid
		// player names. In order to keep using the heuristic fallback mechanism (which works fine most of the time),
		// but still be able to correctly parse the second example, where both arguments are specified explicitly, we
		// need to continue parsing past the page argument and detect the 'unexpected argument'. We can then use a
		// fallback that accepts any input for the player name.

		// Evaluate the pending fallback if parsing of the current command argument failed (without providing a fallback
		// itself), or if there are no more command arguments to parse. Otherwise continue parsing:
		boolean currentParsingFailed = (parseException != null);
		boolean hasUnparsedCommandArguments = parsingContext.hasUnparsedCommandArguments();
		if (!currentParsingFailed && hasUnparsedCommandArguments) {
			return; // continue parsing the next argument
		} // else: continue here, evaluating the pending fallback
		assert currentParsingFailed || !hasUnparsedCommandArguments;

		// parsing past the fallback failed if either the current command argument failed, or if there are unparsed
		// remaining arguments:
		ArgumentsReader argsReader = parsingContext.argsReader;
		boolean parsingFailed = (currentParsingFailed || argsReader.hasNext());

		// update pending fallback (null if there are no more pending fallbacks):
		parsingContext.pendingFallback = fallback.previousPendingFallback;

		// reset context to state before fallback:
		parsingContext.context = fallback.getOriginalContext();

		// Note: If some command argument after the fallback was able to parse something, the args reader might no
		// longer match the state from before the fallback. If parsing past the fallback failed, reset the args reader
		// to the state from before the fallback:
		ArgumentsReader prevArgsState = argsReader.createSnapshot(); // capture current args reader state
		if (parsingFailed) {
			argsReader.setState(fallback.getOriginalArgsReader());
		} else {
			// parsing past the fallback succeeded -> there are no remaining arguments for the fallback argument to
			// consume
			assert !currentParsingFailed && !hasUnparsedCommandArguments && !argsReader.hasNext();
		}

		// parse fallback:
		// TODO Maybe invoke the parseFallback function of the top-most parent argument which extends FallbackArgument.
		// Currently parent arguments often need to wrap fallback exceptions in order to get informed when one of their
		// child arguments wants to parse a fallback.
		FallbackArgument<?> fallbackArgument = fallback.getFallbackArgument();
		ArgumentParseException fallbackError = null;
		// hasRemainingArgs: only the case if args got reset and there were remaining args originally as well
		boolean hasRemainingArgs = argsReader.hasNext();
		ArgumentsReader argsReaderState = argsReader.createSnapshot();
		try {
			// on success this stores any parsed values inside the context:
			fallbackArgument.parseFallback(parsingContext.input, parsingContext.context, argsReader, fallback.exception, parsingFailed);
		} catch (FallbackArgumentException e) { // fallback is not allowed to throw another fallback exception here
			Validate.State.error("Argument '" + fallbackArgument.getName() + "' threw another FallbackArgumentException while parsing fallback: " + e);
		} catch (ArgumentParseException e) { // fallback failed
			argsReader.setState(argsReaderState); // restore previous args reader state
			fallbackError = e;
		}
		boolean fallbackConsumedArgs = (argsReaderState.getCursor() != argsReader.getCursor());
		// assumption: args state got restored (no args consumed) if parsing failed
		assert (fallbackError == null) || !fallbackConsumedArgs;

		if (hasRemainingArgs && !fallbackConsumedArgs) {
			// There are arguments remaining but the fallback did not consume any of them.
			// Continuing would either use the fallback error (if the fallback failed), or the parsing error that lead
			// to the evaluation of the current fallback (otherwise there would be no remaining args).
			// In most cases however we will want to use the original (root) parsing error of the current fallback
			// argument instead. Consider for example the command "/list [player, default:self] [page, default:1]" with
			// input "/list a 2", 'a' being an invalid player name: The player name fallback got evaluated as result of
			// an 'invalid page argument' error, and the fallback will produce 'self' as the default value, which in the
			// end leads to the 'invalid page argument' error to propagate. By using the original parsing exception, we
			// instead get the expected 'invalid player' error in this case.

			// Regardless of whether or not the parsing of the fallback succeeded, we consider the parsing to have
			// failed at this fallback argument with the original root exception:
			parsingContext.currentArgumentIndex = fallback.argumentIndex;
			parsingContext.currentParseException = fallback.exception.getRootException();
			this.handleFallbacks(parsingContext);
			return;
		}
		assert !hasRemainingArgs || fallbackConsumedArgs;

		if (fallbackError != null) {
			// fallback argument failed:
			parsingContext.currentArgumentIndex = fallback.argumentIndex;
			parsingContext.currentParseException = fallbackError;
			// If there is another pending fallback, evaluate it (otherwise parsing fails with this fallback error):
			this.handleFallbacks(parsingContext);
			return;
		} else {
			if (fallbackConsumedArgs) {
				// The fallback did consume arguments: Freshly restart parsing from the next argument after the fallback
				parsingContext.currentArgumentIndex = fallback.argumentIndex;
				// Note: Any buffered context changes that happened since the fallback get disregarded
				return;
			} else {
				// Fallback succeeded, but no arguments were consumed by it, resulting in the same situation as before
				// Apply buffered context changes that happened after the fallback argument:
				fallback.getBufferedContext().applyBuffer();
				// Restore the previous args state (from before the evaluation of this fallback):
				argsReader.setState(prevArgsState);

				// If there is another pending fallback, evaluate it:
				// If there is no other pending fallback, this will either lead to a parsing error (if parsing of the
				// current argument failed), or to parsing success
				this.handleFallbacks(parsingContext);
				return;
			}
		}
	}

	private void handleUnparsedArguments(ParsingContext parsingContext) throws ArgumentParseException {
		ArgumentsReader argsReader = parsingContext.argsReader;
		if (argsReader.getRemainingSize() == 0) return; // no unparsed arguments

		// remaining unexpected/unparsed arguments:
		String firstUnparsedArg = argsReader.peek();
		if (!this.getChildCommands().getCommands().isEmpty()) {
			// has child commands: throw an 'unknown command' exception
			// TODO only use this exception if no arguments got parsed by this command?
			throw new ArgumentParseException(null, this.getUnknownCommandMessage(firstUnparsedArg));
		} else {
			// throw an 'invalid argument' exception for the first unparsed argument after the last parsed argument:
			CommandContext context = parsingContext.context;
			CommandArgument<?> firstUnparsedArgument = null;
			ListIterator<CommandArgument<?>> argumentsIter = arguments.listIterator(arguments.size());
			while (argumentsIter.hasPrevious()) {
				CommandArgument<?> argument = argumentsIter.previous();
				if (!context.has(argument.getName())) {
					firstUnparsedArgument = argument;
				} else {
					break; // stop at the first (from behind) found parsed argument
				}
			}
			if (firstUnparsedArgument != null) {
				throw firstUnparsedArgument.invalidArgumentError(firstUnparsedArg);
			} else {
				// throw an 'unexpected argument' exception:
				Text errorMsg = Settings.msgCommandArgumentUnexpected;
				errorMsg.setPlaceholderArguments(Collections.singletonMap("argument", firstUnparsedArg));
				throw new ArgumentParseException(null, errorMsg);
			}
		}
	}

	protected Text getUnknownCommandMessage(String command) {
		Text text = Settings.msgCommandUnknown;
		text.setPlaceholderArguments(Collections.singletonMap("command", command));
		return text;
	}

	//

	/**
	 * Executes this specific command.
	 * <p>
	 * By default this simply calls {@link #sendHelp(CommandSender)} when executed. Override this method for custom
	 * command behavior.
	 * 
	 * @param input
	 *            the command input
	 * @param context
	 *            the context containing the parsed argument values
	 * @throws CommandException
	 *             if command execution failed
	 */
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		// default command behavior: print command help information
		this.sendHelp(input.getSender());
	}

	/**
	 * Gets tab completion suggestions for the last (possibly partial or empty) argument of the given input.
	 * 
	 * @param input
	 *            the command input
	 * @return the suggestions for the final argument, or an empty list to indicate 'no suggestions' (not
	 *         <code>null</code> and not containing <code>null</code>)
	 */
	public List<String> handleTabCompletion(CommandInput input) {
		Validate.notNull(input, "Input is null!");
		Validate.isTrue(input.getCommand() == this.getRootCommand(), "Input is meant for a different command!");

		CommandContext commandContext = new SimpleCommandContext();
		ArgumentsReader argsReader = new ArgumentsReader(input);
		return this.handleTabCompletion(input, commandContext, argsReader);
	}

	/**
	 * Gets tab completion suggestions for the last (possibly partial or empty) argument of the given input.
	 * 
	 * @param input
	 *            the command input
	 * @param context
	 *            the context
	 * @param argsReader
	 *            the arguments reader
	 * @return the suggestions for the final argument, or an empty list to indicate 'no suggestions' (not
	 *         <code>null</code> and not containing <code>null</code>)
	 */
	protected List<String> handleTabCompletion(CommandInput input, CommandContext context, ArgumentsReader argsReader) {
		assert input != null && context != null && argsReader != null;
		assert (input.getCommand() == this.getRootCommand()); // input is meant for this command
		assert (argsReader.getArgs() != input.getArguments()); // arguments reader is consistent with input

		// search for matching child-command:
		Command childCommand = this.getChildCommand(argsReader);
		if (childCommand != null) {
			// delegate to child-command:
			return childCommand.handleTabCompletion(input, context, argsReader);
		}

		// no applicable child-command was found:
		CommandSender sender = input.getSender();
		// check if this type of command sender supported:
		if (!this.isAccepted(sender)) {
			// not supported type of command sender:
			return Collections.emptyList();
		}

		// check if this command sender has the required permission to proceed:
		if (!this.testPermission(sender)) {
			// command sender not allowed:
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		if (argsReader.getRemainingSize() == 1) {
			String finalArgument = CommandUtils.normalize(argsReader.peek());
			// include matching child-command aliases (max one per command):
			// asserts that the aliases-map provides all aliases for the same command in succession
			Command lastMatchingCommand = null;
			for (Entry<String, Command> aliasEntry : this.getChildCommands().getAliasesMap().entrySet()) {
				String alias = aliasEntry.getKey(); // normalized
				Command aliasCommand = aliasEntry.getValue();
				if (lastMatchingCommand != null && lastMatchingCommand == aliasCommand) {
					// we have already included a suggestion for this child command, skip:
					continue;
				}
				// we have reached an alias for a new child command:
				lastMatchingCommand = null;

				// does the alias match the input?
				if (alias.startsWith(finalArgument)) {
					// exclude further aliases for this command:
					lastMatchingCommand = aliasCommand;

					// check if recipient even has required permission for this command:
					if (!aliasCommand.testPermission(sender)) {
						// missing permission for this command, skip:
						continue;
					}

					// add this alias to the suggestions:
					// TODO maybe use the original alias here, and not the normalized one?
					suggestions.add(alias);
				}
			}
		}

		// parse and complete arguments:
		CommandContextView contextView = context.getView();
		for (CommandArgument<?> argument : arguments) {
			int remainingArgs = argsReader.getRemainingSize();
			if (remainingArgs == 0) {
				// no argument left which could be completed:
				break;
			}
			ArgumentsReader argsReaderState = argsReader.createSnapshot();
			try {
				argument.parse(input, context, argsReader);
				// successfully parsed:
				if (!argsReader.hasNext()) {
					// this consumed the last argument:
					// reset args reader and provide alternative completions for the last argument instead:
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, contextView, argsReader));
					break;
				} else if (argsReader.getRemainingSize() == remainingArgs) {
					// no error during parsing, but none of the remaining args used up:
					// -> this was an optional argument which got skipped
					// include suggestions (if it has any), but continue:
					suggestions.addAll(argument.complete(input, contextView, argsReader));

					// reset args reader and then let the following arguments also try to complete the same arg(s):
					argsReader.setState(argsReaderState);
					continue;
				}
			} catch (FallbackArgumentException e) {
				// parsing failed, but registered a fallback:
				// check for completions, but continue parsing
				argsReader.setState(argsReaderState);
				suggestions.addAll(argument.complete(input, contextView, argsReader));
				argsReader.setState(argsReaderState);
				continue;
			} catch (ArgumentParseException e) {
				if (argument.getReducedFormat().isEmpty()) {
					// argument is hidden, check for completions but continue parsing:
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, contextView, argsReader));
					argsReader.setState(argsReaderState);
					continue;
				} else {
					// parsing might have failed because of an invalid partial last argument
					// -> check for and include suggestions
					argsReader.setState(argsReaderState);
					suggestions.addAll(argument.complete(input, contextView, argsReader));
					// parsing might also have failed because of an invalid argument inside the sequence of arguments
					// -> skip later arguments (current argument will not provide suggestions in that case, because
					// it isn't using up the last argument)
					break;
				}
			}
		}

		return Collections.unmodifiableList(suggestions);
	}

	// help page related:

	/**
	 * Checks whether the child-commands of this {@link Command} are included in the parent's help content.
	 * <p>
	 * By default only direct child commands are included in the help content.
	 * 
	 * @return <code>true</code> if child-commands are included in the parent's help content
	 */
	public final boolean isIncludeChildsInParentHelp() {
		return includeChildsInParentHelp;
	}

	/**
	 * Sets whether the child-commands of this {@link Command} are included in the parent's help content.
	 * <p>
	 * By default only direct child commands are included in the help content.
	 * 
	 * @param includeChilds
	 *            <code>true</code> to include the child commands of this command in the parent's help content
	 */
	protected final void setIncludeChildsInParentHelp(boolean includeChilds) {
		this.includeChildsInParentHelp = includeChilds;
	}

	/**
	 * Checks whether this {@link Command} is hidden in the parent's help contents.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @return <code>true</code> if this command is hidden in the parent's help contents
	 */
	public final boolean isHiddenInParentHelp() {
		return hiddenInParentHelp;
	}

	/**
	 * Sets whether this {@link Command} is hidden in the parent's help contents.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @param hiddenInParentHelp
	 *            <code>true</code> to exclude this command in the parent's help contents
	 */
	protected final void setHiddenInParentHelp(boolean hiddenInParentHelp) {
		this.hiddenInParentHelp = hiddenInParentHelp;
	}

	/**
	 * Checks whether this {@link Command} is hidden in its own help page.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @return <code>true</code> if this command is hidden in its own help page
	 */
	public final boolean isHiddenInOwnHelp() {
		return hiddenInOwnHelp;
	}

	/**
	 * Sets whether this {@link Command} is hidden in its own help page.
	 * <p>
	 * Hidden commands won't show up on the help pages. Tab completion however is not affected by this.
	 * 
	 * @param hiddenInOwnHelp
	 *            <code>true</code> to exclude this command in its own help contents
	 */
	protected final void setHiddenInOwnHelp(boolean hiddenInOwnHelp) {
		this.hiddenInOwnHelp = hiddenInOwnHelp;
	}

	// help page formatting:

	/**
	 * Sets the format to use for the title when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * See {@link #getCommonMessageArgs()} for the available placeholders.
	 * <p>
	 * If the format is {@link Text#isPlainTextEmpty() empty}, no title will be used in the help pages. If the format is
	 * <code>null</code>, the format of the parent command gets used. If no parent is available or the parent
	 * format is {@link Text#isPlainTextEmpty() empty}, a default format gets used.
	 * 
	 * @param helpTitleFormat
	 *            the format
	 */
	protected void setHelpTitleFormat(Text helpTitleFormat) {
		this.helpTitleFormat = helpTitleFormat;
	}

	/**
	 * Sets the format to use for this command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * See {@link #getCommonMessageArgs()} for the available placeholders.
	 * <p>
	 * If the format is {@link Text#isPlainTextEmpty() empty}, no command usage will be printed in the help pages. If
	 * the format is <code>null</code>, the format of the parent command gets used. If no parent is available or the
	 * parent format is {@link Text#isPlainTextEmpty() empty}, a default format gets used.
	 * 
	 * @param helpUsageFormat
	 *            the format
	 */
	protected void setHelpUsageFormat(Text helpUsageFormat) {
		this.helpUsageFormat = helpUsageFormat;
	}

	/**
	 * Sets the format to use for this command's description when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * See {@link #getCommonMessageArgs()} for the available placeholders.
	 * <p>
	 * If the format is {@link Text#isPlainTextEmpty() empty}, no command description will be printed in the help pages.
	 * If the format is <code>null</code>, the format of the parent command gets used. If no parent is available or the
	 * parent format is {@link Text#isPlainTextEmpty() empty}, a default format gets used.
	 * 
	 * @param helpDescFormat
	 *            the format
	 */
	protected void setHelpDescFormat(Text helpDescFormat) {
		this.helpDescFormat = helpDescFormat;
	}

	/**
	 * Sets the format to use for a child-command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * <p>
	 * See {@link #getCommonMessageArgs()} for the available placeholders.
	 * <p>
	 * If the format is {@link Text#isPlainTextEmpty() empty}, no child commands will be included in the help pages. If
	 * the format is <code>null</code>, the format of the parent command gets used. If no parent is available or the
	 * parent format is {@link Text#isPlainTextEmpty() empty}, a default format gets used.
	 * 
	 * @param helpChildUsageFormat
	 *            the format
	 */
	protected void setHelpChildUsageFormat(Text helpChildUsageFormat) {
		this.helpChildUsageFormat = helpChildUsageFormat;
	}

	/**
	 * Sets the format to use for a child-command's description when sending the help via
	 * {@link #sendHelp(CommandSender)}.
	 * <p>
	 * See {@link #getCommonMessageArgs()} for the available placeholders.
	 * <p>
	 * If the format is {@link Text#isPlainTextEmpty() empty}, no child command descriptions will be included in the
	 * help pages. If the format is <code>null</code>, the format of the parent command gets used. If no parent is
	 * available or the parent format is {@link Text#isPlainTextEmpty() empty}, a default format gets used.
	 * 
	 * @param helpChildDescFormat
	 *            the format
	 */
	protected void setHelpChildDescFormat(Text helpChildDescFormat) {
		this.helpChildDescFormat = helpChildDescFormat;
	}

	/**
	 * Gets the format to use for the title when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpTitleFormat(Text)
	 */
	protected final Text getHelpTitleFormat() {
		Text format = this.helpTitleFormat;
		if (format == null) {
			Text parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpTitleFormat()).isPlainTextEmpty()) {
				format = parentFormat;
			} else {
				// default:
				format = DEFAULT_HELP_TITLE_FORMAT;
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for this command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpUsageFormat(Text)
	 */
	protected final Text getHelpUsageFormat() {
		Text format = this.helpUsageFormat;
		if (format == null) {
			Text parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpUsageFormat()).isPlainTextEmpty()) {
				format = parentFormat;
			} else {
				// default:
				format = DEFAULT_HELP_USAGE_FORMAT;
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for this command's description when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpDescFormat(Text)
	 */
	protected final Text getHelpDescFormat() {
		Text format = this.helpDescFormat;
		if (format == null) {
			Text parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpDescFormat()).isPlainTextEmpty()) {
				format = parentFormat;
			} else {
				// default:
				format = DEFAULT_HELP_DESC_FORMAT;
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for a child-command's usage when sending the help via {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpChildUsageFormat(Text)
	 */
	protected final Text getHelpChildUsageFormat() {
		Text format = this.helpChildUsageFormat;
		if (format == null) {
			Text parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpChildUsageFormat()).isPlainTextEmpty()) {
				format = parentFormat;
			} else {
				// default: use this command's help usage format
				format = this.getHelpUsageFormat();
			}

		}
		assert format != null;
		return format;
	}

	/**
	 * Gets the format to use for a child-command's description when sending the help via
	 * {@link #sendHelp(CommandSender)}.
	 * 
	 * @return the format, not <code>null</code>
	 * @see #setHelpChildDescFormat(Text)
	 */
	protected final Text getHelpChildDescFormat() {
		Text format = this.helpChildDescFormat;
		if (format == null) {
			Text parentFormat;
			if (parent != null && !(parentFormat = parent.getHelpChildDescFormat()).isPlainTextEmpty()) {
				format = parentFormat;
			} else {
				// default: use this command's help description format
				format = this.getHelpDescFormat();
			}
		}
		assert format != null;
		return format;
	}

	/**
	 * Checks if the recipient has the permission to view the help of this command.
	 * <p>
	 * This checks if the recipient has the permission to execute this command or any of the child commands.
	 * 
	 * @param recipient
	 *            the recipient
	 * @return <code>true</code> if the recipient is allowed to view the help
	 */
	protected final boolean hasHelpPermission(CommandSender recipient) {
		if (this.testPermission(recipient)) return true;
		for (Command childCommand : this.getChildCommands().getCommands()) {
			if (childCommand.hasHelpPermission(recipient)) return true;
		}
		return false;
	}

	/**
	 * Sends usage information about this command and its child-commands to the given {@link CommandSender}.
	 * 
	 * @param recipient
	 *            the recipient
	 * @throws NoPermissionException
	 *             if the sender is not allowed to view the help of this command
	 */
	public void sendHelp(CommandSender recipient) throws NoPermissionException {
		Validate.notNull(recipient);

		// make sure the recipient has the required permission:
		if (!this.hasHelpPermission(recipient)) {
			throw this.noPermissionException();
		}

		Map<String, Object> commonMsgArgs = this.getCommonMessageArgs();

		// title:
		Text titleFormat = this.getHelpTitleFormat();
		assert titleFormat != null;
		if (!titleFormat.isPlainTextEmpty()) {
			TextUtils.sendMessage(recipient, titleFormat, commonMsgArgs);
		}

		// skip info about the command if it is hidden or the recipient does not have the required permission:
		if (!this.isHiddenInOwnHelp() && this.testPermission(recipient)) {
			// command usage:
			Text usageFormat = this.getHelpUsageFormat();
			assert usageFormat != null;
			usageFormat.setPlaceholderArguments(commonMsgArgs);

			// command description:
			Text descriptionFormat;
			Text description = this.getDescription();
			if (description.isPlainTextEmpty()) {
				descriptionFormat = Text.EMPTY;
			} else {
				descriptionFormat = this.getHelpDescFormat();
				assert descriptionFormat != null;
				descriptionFormat.setPlaceholderArguments(commonMsgArgs);
			}

			Text helpEntryFormat = HELP_ENTRY_FORMAT;
			helpEntryFormat.setPlaceholderArguments(
					"usage", usageFormat,
					"description", descriptionFormat
			);

			// skip if both usage and description formats are empty:
			if (!helpEntryFormat.isPlainTextEmpty()) {
				TextUtils.sendMessage(recipient, helpEntryFormat);
			}
		}

		// include child-commands help:
		Text childUsageFormat = this.getHelpChildUsageFormat();
		Text childDescFormat = this.getHelpChildDescFormat();
		this.sendChildCommandsHelp(recipient, childUsageFormat, childDescFormat, this);
	}

	protected void sendChildCommandsHelp(CommandSender recipient, Text childUsageFormat, Text childDescFormat, Command command) {
		Validate.notNull(recipient);
		if (childUsageFormat == null || childUsageFormat.isPlainTextEmpty()) {
			// not including child commands at all:
			return;
		}
		boolean childDescFormatEmpty = (childDescFormat == null || childDescFormat.isPlainTextEmpty());

		// print usage and description of child-commands:
		for (Command childCommand : command.getChildCommands().getCommands()) {
			// skip info about the command if it is hidden or the recipient does not have the required permission:
			if (!childCommand.isHiddenInParentHelp() && childCommand.testPermission(recipient)) {
				Map<String, Object> childCommonMsgArgs = childCommand.getCommonMessageArgs();

				// command usage:
				childUsageFormat.setPlaceholderArguments(childCommonMsgArgs);

				// command description:
				Text childDescriptionFormat = childDescFormat;
				Text childDescription = childCommand.getDescription();
				if (childDescFormatEmpty || childDescription.isPlainTextEmpty()) {
					childDescriptionFormat = Text.EMPTY;
				} else {
					childDescriptionFormat.setPlaceholderArguments(childCommonMsgArgs);
				}

				Text helpEntryFormat = HELP_ENTRY_FORMAT;
				helpEntryFormat.setPlaceholderArguments(
						"usage", childUsageFormat,
						"description", childDescriptionFormat
				);

				TextUtils.sendMessage(recipient, helpEntryFormat);
			}

			// optionally include the child-command's child-commands in help content:
			if (childCommand.isIncludeChildsInParentHelp()) {
				this.sendChildCommandsHelp(recipient, childUsageFormat, childDescFormat, childCommand);
			}
		}
	}
}
