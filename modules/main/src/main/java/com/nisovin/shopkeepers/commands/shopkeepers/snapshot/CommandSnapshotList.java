package com.nisovin.shopkeepers.commands.shopkeepers.snapshot;

import java.util.List;
import java.util.function.Supplier;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandSnapshotList extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_PAGE = "page";

	private static final int ENTRIES_PER_PAGE = 8;

	CommandSnapshotList() {
		super("list");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SNAPSHOT_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSnapshotList);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.withAccess(DefaultUITypes.EDITOR())),
				TargetShopkeeperFilter.ANY
		));
		this.addArgument(new PositiveIntegerArgument(ARGUMENT_PAGE).orDefaultValue(1));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		AbstractShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		int page = context.get(ARGUMENT_PAGE);

		// Check access:
		if (shopkeeper instanceof PlayerShopkeeper) {
			if (!((AbstractPlayerShopkeeper) shopkeeper).checkAccess(sender, DefaultPlayerShopAccessLevels.FULL(), false)) {
				return;
			}
		} else if (!shopkeeper.canEdit(sender, false)) {
			return;
		}

		List<? extends ShopkeeperSnapshot> snapshots = shopkeeper.getSnapshots();
		int snapshotsCount = snapshots.size();
		int maxPage = Math.max(1, (int) Math.ceil((double) snapshotsCount / ENTRIES_PER_PAGE));
		page = Math.max(1, Math.min(page, maxPage));

		Messages.snapshotListHeader.setPlaceholderArguments(shopkeeper.getMessageArguments("shop_"));
		TextUtils.sendMessage(sender, Messages.snapshotListHeader,
				"snapshotsCount", snapshotsCount,
				"page", page,
				"maxPage", maxPage
		);

		int startIndex = (page - 1) * ENTRIES_PER_PAGE;
		int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, snapshotsCount);
		for (int index = startIndex; index < endIndex; index++) {
			ShopkeeperSnapshot snapshot = snapshots.get(index);
			TextUtils.sendMessage(sender, Messages.snapshotListEntry,
					"id", (index + 1),
					"name", snapshot.getName(),
					"timestamp", (Supplier<?>) () -> DerivedSettings.dateTimeFormatter.format(
							snapshot.getTimestamp()
					)
			);
		}
	}
}
