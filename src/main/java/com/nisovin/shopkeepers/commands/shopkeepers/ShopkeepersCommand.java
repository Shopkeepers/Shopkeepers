package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Collections;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.arguments.ShopObjectTypeArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopTypeArgument;
import com.nisovin.shopkeepers.commands.lib.BaseCommand;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.CommandRegistry;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class ShopkeepersCommand extends BaseCommand {

	private static final String ARGUMENT_SHOP_TYPE = "shop-type";
	private static final String ARGUMENT_OBJECT_TYPE = "object-type";

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;

	public ShopkeepersCommand(SKShopkeepersPlugin plugin, PluginCommand pluginCommand, Confirmations confirmations) {
		super(pluginCommand);
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();

		// permission gets checked by testPermission and during execution

		// description:
		this.setDescription(Settings.msgCommandDescriptionShopkeeper);

		// formatting:
		this.setHelpTitleFormat(Settings.msgCommandHelpTitle.setPlaceholderArguments(
				Collections.singletonMap("version", plugin.getDescription().getVersion())
		));
		this.setHelpUsageFormat(Settings.msgCommandHelpUsageFormat);
		this.setHelpDescFormat(Settings.msgCommandHelpDescriptionFormat);

		// arguments for shopkeeper creation:
		this.addArgument(new OptionalArgument<>(new ShopTypeArgument(ARGUMENT_SHOP_TYPE)));
		this.addArgument(new OptionalArgument<>(new ShopObjectTypeArgument(ARGUMENT_OBJECT_TYPE)));

		// register child commands:
		CommandRegistry childCommands = this.getChildCommands();
		childCommands.register(new CommandHelp(this));
		childCommands.register(new CommandReload(plugin));
		childCommands.register(new CommandDebug());
		childCommands.register(new CommandList(shopkeeperRegistry));
		childCommands.register(new CommandHistory(plugin.getTradingHistory()));
		childCommands.register(new CommandRemove(plugin, shopkeeperRegistry, confirmations));
		childCommands.register(new CommandGive());
		childCommands.register(new CommandRemote());
		childCommands.register(new CommandEdit());
		childCommands.register(new CommandTransfer());
		childCommands.register(new CommandSetTradePerm());
		childCommands.register(new CommandSetForHire());
		// hidden commands:
		childCommands.register(new CommandConfirm(confirmations));
		// hidden debugging commands:
		childCommands.register(new CommandCheck(plugin));
		childCommands.register(new CommandCheckItem());
		childCommands.register(new CommandYaml());
		childCommands.register(new CommandDebugCreateShops(plugin));
	}

	// also responsible for hiding the command from the help page, if shop creation via command is disabled
	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return Settings.createPlayerShopWithCommand || PermissionUtils.hasPermission(sender, ShopkeepersPlugin.ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		if (!(input.getSender() instanceof Player)) {
			throw PlayerCommand.createCommandSourceRejectedException(sender);
		}
		Player player = (Player) sender;

		// creating new shopkeeper:

		// get targeted block information:
		RayTraceResult targetBlockInfo = player.rayTraceBlocks(10.0D, FluidCollisionMode.NEVER);

		// check for valid targeted block:
		if (targetBlockInfo == null) {
			TextUtils.sendMessage(player, Settings.msgShopCreateFail);
			return;
		}
		Block targetBlock = targetBlockInfo.getHitBlock();
		BlockFace targetBlockFace = targetBlockInfo.getHitBlockFace();
		assert targetBlock != null && !targetBlock.isEmpty();
		assert targetBlockFace != null;

		ShopType<?> shopType = context.get(ARGUMENT_SHOP_TYPE);
		ShopObjectType<?> shopObjType = context.get(ARGUMENT_OBJECT_TYPE);

		boolean createPlayerShop = (Settings.createPlayerShopWithCommand && ItemUtils.isChest(targetBlock.getType()));
		if (createPlayerShop) {
			// create player shopkeeper:

			// default shop type and shop object type: first use-able player shop type and shop object type
			if (shopType == null) {
				shopType = plugin.getShopTypeRegistry().getDefaultSelection(player);
			}
			if (shopObjType == null) {
				shopObjType = plugin.getShopObjectTypeRegistry().getDefaultSelection(player);
			}

			if (shopType == null || shopObjType == null) {
				// the player cannot create shops at all:
				TextUtils.sendMessage(player, Settings.msgNoPermission);
				return;
			}

			// validate the selected shop type:
			if (!(shopType instanceof PlayerShopType)) {
				// only player shop types are allowed here:
				TextUtils.sendMessage(player, Settings.msgNoPlayerShopTypeSelected);
				return;
			}
		} else {
			// create admin shopkeeper:

			// check permission:
			if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.ADMIN_PERMISSION)) {
				TextUtils.sendMessage(sender, Settings.msgNoPermission);
				return;
			}

			// default shop type and shop object type:
			if (shopType == null) {
				shopType = DefaultShopTypes.ADMIN();
			}
			if (shopObjType == null) {
				shopObjType = plugin.getDefaultShopObjectType();
			}
			assert shopType != null && shopObjType != null;

			// validate the selected shop type:
			if (!(shopType instanceof AdminShopType)) {
				// only admin shop types are allowed here:
				TextUtils.sendMessage(player, Settings.msgNoAdminShopTypeSelected);
				return;
			}
		}
		assert shopType != null && shopObjType != null;

		// determine spawn location:
		Location spawnLocation = plugin.getShopkeeperCreation().determineSpawnLocation(player, targetBlock, targetBlockFace);

		// shop creation data:
		ShopCreationData shopCreationData;
		if (createPlayerShop) {
			// create player shopkeeper:
			shopCreationData = PlayerShopCreationData.create(player, shopType, shopObjType, spawnLocation, targetBlockFace, targetBlock);
		} else {
			// create admin shopkeeper:
			shopCreationData = AdminShopCreationData.create(player, shopType, shopObjType, spawnLocation, targetBlockFace);
		}
		assert shopCreationData != null;

		// handle shopkeeper creation:
		plugin.handleShopkeeperCreation(shopCreationData);
	}
}
