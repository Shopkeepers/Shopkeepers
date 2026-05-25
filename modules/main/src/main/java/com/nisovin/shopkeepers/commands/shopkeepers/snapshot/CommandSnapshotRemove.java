package com.nisovin.shopkeepers.commands.shopkeepers.snapshot;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.arguments.snapshot.ShopkeeperSnapshotIndexArgument;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

class CommandSnapshotRemove extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_SNAPSHOT = "snapshot";
	private static final String ARGUMENT_ALL = "all";

	private final Confirmations confirmations;

	CommandSnapshotRemove(Confirmations confirmations) {
		super("remove", Arrays.asList("delete"));
		Validate.notNull(confirmations, "confirmations is null");
		this.confirmations = confirmations;

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SNAPSHOT_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSnapshotRemove);

		// Arguments:
		CommandArgument<Shopkeeper> shopkeeperArgument = new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.withAccess(DefaultUITypes.EDITOR())),
				TargetShopkeeperFilter.ANY
		);
		this.addArgument(shopkeeperArgument);
		this.addArgument(new FirstOfArgument("snapshot:firstOf", Arrays.asList(
				new LiteralArgument(ARGUMENT_ALL),
				new ShopkeeperSnapshotIndexArgument(ARGUMENT_SNAPSHOT, shopkeeperArgument)
		)));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		AbstractShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);

		// Check access:
		if (shopkeeper instanceof PlayerShopkeeper) {
			if (!((AbstractPlayerShopkeeper) shopkeeper).checkAccess(sender, DefaultPlayerShopAccessLevels.FULL(), false)) {
				return;
			}
		} else if (!shopkeeper.canEdit(sender, false)) {
			return;
		}

		List<? extends ShopkeeperSnapshot> snapshots = shopkeeper.getSnapshots();

		if (context.has(ARGUMENT_ALL)) {
			// Remove all snapshots:
			int snapshotsCount = shopkeeper.getSnapshots().size();
			confirmations.awaitConfirmation(sender, () -> {
				if (!shopkeeper.isValid()) {
					TextUtils.sendMessage(sender, Messages.shopNoLongerExists);
					return;
				}

				if (!shopkeeper.getSnapshots().equals(snapshots)) {
					TextUtils.sendMessage(sender, Messages.actionAbortedSnapshotsChanged);
					return;
				}

				shopkeeper.removeAllSnapshots();
				shopkeeper.save();

				Messages.snapshotRemovedAll.setPlaceholderArguments(
						shopkeeper.getMessageArguments("shop_")
				);
				TextUtils.sendMessage(sender, Messages.snapshotRemovedAll,
						"snapshotsCount", snapshotsCount
				);
			});

			Messages.confirmRemoveAllSnapshots.setPlaceholderArguments(
					shopkeeper.getMessageArguments("shop_")
			);
			TextUtils.sendMessage(sender, Messages.confirmRemoveAllSnapshots,
					"snapshotsCount", snapshotsCount
			);
			TextUtils.sendMessage(sender, Messages.confirmationRequired);
			return;
		}

		// Remove the specified snapshot:
		assert context.has(ARGUMENT_SNAPSHOT);
		int snapshotIndex = context.get(ARGUMENT_SNAPSHOT);
		assert snapshotIndex >= 0 && snapshotIndex < shopkeeper.getSnapshots().size();
		int snapshotId = snapshotIndex + 1;
		ShopkeeperSnapshot snapshot = shopkeeper.getSnapshot(snapshotIndex);

		confirmations.awaitConfirmation(sender, () -> {
			if (!shopkeeper.isValid()) {
				TextUtils.sendMessage(sender, Messages.shopNoLongerExists);
				return;
			}

			if (!shopkeeper.getSnapshots().equals(snapshots)) {
				TextUtils.sendMessage(sender, Messages.actionAbortedSnapshotsChanged);
				return;
			}

			assert snapshotIndex >= 0 && snapshotIndex < shopkeeper.getSnapshots().size();
			ShopkeeperSnapshot removedSnapshot = shopkeeper.removeSnapshot(snapshotIndex);
			assert removedSnapshot == snapshot;
			shopkeeper.save();

			TextUtils.sendMessage(sender, Messages.snapshotRemoved,
					"id", snapshotId,
					"name", snapshot.getName(),
					"timestamp", (Supplier<?>) () -> DerivedSettings.dateTimeFormatter.format(
							snapshot.getTimestamp()
					)
			);
		});

		TextUtils.sendMessage(sender, Messages.confirmRemoveSnapshot,
				"id", snapshotId,
				"name", snapshot.getName(),
				"timestamp", (Supplier<?>) () -> DerivedSettings.dateTimeFormatter.format(
						snapshot.getTimestamp()
				)
		);
		TextUtils.sendMessage(sender, Messages.confirmationRequired);
	}
}
