package com.nisovin.shopkeepers.commands.lib;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.Validate;

/**
 * This exception is thrown during handling of a command when an error occurs or execution fails. The detail message of
 * the exception might get sent to the {@link CommandSender} which triggered the command.
 */
public class CommandException extends Exception {

	private static final long serialVersionUID = 3021047528891246476L;

	private final Text messageText;

	public CommandException(Text message) {
		this(message, null);
	}

	private static String validateMessage(Text message) {
		Validate.notNull(message, "Message is null!");
		String plainMessage = message.toPlainText();
		Validate.notEmpty(plainMessage, "Message is empty!");
		return plainMessage;
	}

	public CommandException(Text message, Throwable cause) {
		super(validateMessage(message), cause);
		// TODO can this copy be avoided?
		// required since placeholder and translation arguments may dynamically change while the exception is kept
		// around
		// TODO Add an immutable Text variant, a TextView or a flag which also prevents changing the placeholder
		// arguments and then use that when returning the Text?
		this.messageText = message.copy();
	}

	/**
	 * Gets the exception's detail message {@link Text}.
	 * 
	 * @return the exception's detail message Text
	 */
	public final Text getMessageText() {
		return messageText;
	}
}
