package com.nisovin.shopkeepers.commands.shopkeepers.snapshot;

import java.util.function.Supplier;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperLoadException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.arguments.snapshot.ShopkeeperSnapshotIndexArgument;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.logging.Log;

class CommandSnapshotRestore extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_SNAPSHOT = "snaphot";

	CommandSnapshotRestore() {
		super("restore");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SNAPSHOT_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSnapshotRestore);

		// Arguments:
		CommandArgument<Shopkeeper> shopkeeperArgument = new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.withAccess(DefaultUITypes.EDITOR())),
				TargetShopkeeperFilter.ANY
		);
		this.addArgument(shopkeeperArgument);
		this.addArgument(new ShopkeeperSnapshotIndexArgument(ARGUMENT_SNAPSHOT, shopkeeperArgument));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		AbstractShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		int snapshotIndex = context.get(ARGUMENT_SNAPSHOT);
		assert snapshotIndex >= 0 && snapshotIndex < shopkeeper.getSnapshots().size();
		int snapshotId = snapshotIndex + 1;

		// Check access:
		if (shopkeeper instanceof PlayerShopkeeper) {
			if (!((AbstractPlayerShopkeeper) shopkeeper).checkAccess(sender, DefaultPlayerShopAccessLevels.FULL(), false)) {
				return;
			}
		} else if (!shopkeeper.canEdit(sender, false)) {
			return;
		}

		ShopkeeperSnapshot snapshot = shopkeeper.getSnapshot(snapshotIndex);
		try {
			shopkeeper.applySnapshot(snapshot);
			shopkeeper.save();
		} catch (ShopkeeperLoadException e) {
			TextUtils.sendMessage(sender, Messages.snapshotRestoreFailed,
					"id", snapshotId,
					"name", snapshot.getName(),
					"timestamp", (Supplier<?>) () -> DerivedSettings.dateTimeFormatter.format(
							snapshot.getTimestamp()
					)
			);
			Log.warning(shopkeeper.getLogPrefix() + "Failed to restore snapshot " + snapshotId
					+ " ('" + snapshot.getName() + "')!", e);
			return;
		}
		TextUtils.sendMessage(sender, Messages.snapshotRestored,
				"id", snapshotId,
				"name", snapshot.getName(),
				"timestamp", (Supplier<?>) () -> DerivedSettings.dateTimeFormatter.format(
						snapshot.getTimestamp()
				)
		);
	}
}
