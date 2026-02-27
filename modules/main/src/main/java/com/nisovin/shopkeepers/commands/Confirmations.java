package com.nisovin.shopkeepers.commands;

import java.util.HashMap;
import java.util.Map;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class Confirmations {

	private static class PendingConfirmation {

		private final Runnable action;
		private final WrappedTask task;

		public PendingConfirmation(Runnable action, WrappedTask task) {
			this.task = task;
			this.action = action;
		}

		public WrappedTask getTask() {
			return task;
		}

		public Runnable getAction() {
			return action;
		}
	}

	public static final int DEFAULT_CONFIRMATION_TICKS = 25 * 20; // 25 seconds

	private final Plugin plugin;
	// The type of key that is used to track pending confirmations depends on the type of
	// CommandSender.
	private final Map<Object, PendingConfirmation> pendingConfirmations = new HashMap<>();

	public Confirmations(Plugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
	}

	public void onDisable() {
		pendingConfirmations.clear();
	}

	private Object getSenderKey(CommandSender sender) {
		// Note: We cannot use the CommandSender instance itself as key, because for some types of
		// command senders we might get a new instance for each invoked command.
		if (sender instanceof Player) {
			return ((Player) sender).getUniqueId();
		} else if (sender instanceof ProxiedCommandSender) {
			// Messages and permission checks use the caller, so we also use the caller for
			// confirmations.
			return this.getSenderKey(((ProxiedCommandSender) sender).getCaller());
		} else {
			// Any other type of command sender (console, rcon, command blocks, etc.).
			// Using the CommandSender's class as key allows us to track separate pending
			// confirmations for different types of command senders (e.g. the console and command
			// blocks don't share the same pending confirmation state).
			return sender.getClass();
		}
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		this.endConfirmation(player);
	}

	public void awaitConfirmation(CommandSender sender, Runnable action) {
		this.awaitConfirmation(sender, action, DEFAULT_CONFIRMATION_TICKS);
	}

	public void awaitConfirmation(CommandSender sender, Runnable action, int timeoutTicks) {
		Validate.notNull(sender, "sender is null");
		Validate.notNull(action, "action is null");
		Validate.isTrue(timeoutTicks > 0, "timeoutTicks has to be positive");

		WrappedTask task = SchedulerUtils.runAsyncTaskLaterOrOmit(() -> {
			this.endConfirmation(sender);
			TextUtils.sendMessage(sender, Messages.confirmationExpired);
		}, timeoutTicks);

		if (task == null) {
			// I guess we just return early. TODO: Should throw here?
			return;
		}

		PendingConfirmation previousPendingConfirmation = pendingConfirmations.put(
				this.getSenderKey(sender),
				new PendingConfirmation(action, task)
		);
		if (previousPendingConfirmation != null) {
			// Cancel the previous pending confirmation task:
			previousPendingConfirmation.getTask().cancel();
		}
	}

	// Returns the action that was awaiting confirmation.
	public @Nullable Runnable endConfirmation(CommandSender sender) {
		Validate.notNull(sender, "sender is null");
		PendingConfirmation pendingConfirmation = pendingConfirmations.remove(this.getSenderKey(sender));
		if (pendingConfirmation != null) {
			// End confirmation task:
			pendingConfirmation.getTask().cancel();

			// Return action:
			return pendingConfirmation.getAction();
		}
		return null;
	}

	public void handleConfirmation(CommandSender sender) {
		Validate.notNull(sender, "sender is null");
		Runnable action = this.endConfirmation(sender);
		if (action != null) {
			// Execute confirmed action:
			action.run();
		} else {
			TextUtils.sendMessage(sender, Messages.nothingToConfirm);
		}
	}
}
