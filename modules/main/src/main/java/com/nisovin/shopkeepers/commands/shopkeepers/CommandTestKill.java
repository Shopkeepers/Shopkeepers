package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;

/**
 * Kills the targeted entity by setting its health to <code>0</code>.
 * <p>
 * Unlike the vanilla kill command, this bypasses any damage events. This can be used to test the
 * death behavior of shopkeeper entities, such as their item drops, when they are killed directly,
 * for example by another plugin.
 */
class CommandTestKill extends PlayerCommand {

	CommandTestKill() {
		super("testKill");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Kills the targeted entity."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		Player player = (Player) input.getSender();

		LivingEntity target = (LivingEntity) EntityUtils.getTargetedEntity(
				player,
				(entity) -> entity instanceof LivingEntity
		);
		if (target == null) {
			player.sendMessage(ChatColor.RED + "No living entity targeted!");
			return;
		}

		target.setHealth(0.0D);

		player.sendMessage(ChatColor.GREEN + "Killed entity: " + ChatColor.YELLOW + target.getName()
				+ ChatColor.GREEN + " (" + ChatColor.YELLOW + target.getUniqueId() + ChatColor.GREEN
				+ "). Dead=" + ChatColor.YELLOW + target.isDead());
	}
}
