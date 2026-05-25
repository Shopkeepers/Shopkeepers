package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class ShopkeeperFilter {

	public static final ArgumentFilter<Shopkeeper> ANY = ArgumentFilter.acceptAny();

	public static final ArgumentFilter<@Nullable Shopkeeper> ADMIN = new ArgumentFilter<@Nullable Shopkeeper>() {
		@Override
		public boolean test(
				CommandInput input,
				CommandContextView context,
				@Nullable Shopkeeper shopkeeper
		) {
			return (shopkeeper instanceof AdminShopkeeper);
		}

		@Override
		public Text getInvalidArgumentErrorMsg(
				CommandArgument<?> argument,
				String argumentInput,
				@Nullable Shopkeeper value
		) {
			Validate.notNull(argumentInput, "argumentInput is null");
			Text text = Messages.commandShopkeeperArgumentNoAdminShop;
			text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
			text.setPlaceholderArguments("argument", argumentInput);
			return text;
		}
	};

	public static final ArgumentFilter<@Nullable Shopkeeper> PLAYER = new ArgumentFilter<@Nullable Shopkeeper>() {
		@Override
		public boolean test(
				CommandInput input,
				CommandContextView context,
				@Nullable Shopkeeper shopkeeper
		) {
			return (shopkeeper instanceof PlayerShopkeeper);
		}

		@Override
		public Text getInvalidArgumentErrorMsg(
				CommandArgument<?> argument,
				String argumentInput,
				@Nullable Shopkeeper value
		) {
			Validate.notNull(argumentInput, "argumentInput is null");
			Text text = Messages.commandShopkeeperArgumentNoPlayerShop;
			text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
			text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
			return text;
		}
	};

	public static ArgumentFilter<@Nullable Shopkeeper> withAccess(UIType uiType) {
		return new ArgumentFilter<@Nullable Shopkeeper>() {
			@Override
			public boolean test(
					CommandInput input,
					CommandContextView context,
					@Nullable Shopkeeper shopkeeper
			) {
				if (shopkeeper == null) return false;

				// Avoid spamming the log with permission check notifications in debug mode:
				return PermissionUtils.runWithoutPermissionCheckLogging(() -> {
					// Takes non-player command senders into account: If the sender is not a player,
					// it requires the bypass permission (usually the case for the console and block
					// command senders).
					return ((AbstractShopkeeper) shopkeeper).canAccess(input.getSender(), uiType, true);
				});
			}

			@Override
			public Text getInvalidArgumentErrorMsg(
					CommandArgument<?> argument,
					String argumentInput,
					@Nullable Shopkeeper value
			) {
				Validate.notNull(argumentInput, "argumentInput is null");
				Text text = Messages.commandShopkeeperArgumentNoAccess;
				text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
				text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
				return text;
			}
		};
	}

	// Only includes PlayerShopkeepers:
	public static ArgumentFilter<@Nullable Shopkeeper> withAccessLevel(PlayerShopAccessLevel accessLevel) {
		return new ArgumentFilter<@Nullable Shopkeeper>() {
			@Override
			public boolean test(
					CommandInput input,
					CommandContextView context,
					@Nullable Shopkeeper shopkeeper
			) {
				if (!(shopkeeper instanceof PlayerShopkeeper)) {
					return false;
				}

				// Avoid spamming the log with permission check notifications in debug mode:
				return PermissionUtils.runWithoutPermissionCheckLogging(() -> {
					// Takes non-player command senders into account: If the sender is not a player,
					// it requires the bypass permission (usually the case for the console and block
					// command senders).
					return ((AbstractPlayerShopkeeper) shopkeeper).checkAccess(input.getSender(), accessLevel, true);
				});
			}

			@Override
			public Text getInvalidArgumentErrorMsg(
					CommandArgument<?> argument,
					String argumentInput,
					@Nullable Shopkeeper value
			) {
				Validate.notNull(argumentInput, "argumentInput is null");
				Text text = Messages.commandShopkeeperArgumentNoAccess;
				text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
				text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
				return text;
			}
		};
	}

	private ShopkeeperFilter() {
	}
}
