package com.nisovin.shopkeepers.commands.shopkeepers;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.BoundedDoubleArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.bukkit.Ticks;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Produces lots of damage events (with a damage of <code>0</code>) in quick succession.
 * <p>
 * This can for example be used to test the performance of the involved event handlers.
 * <p>
 * This may conflict with anti-cheat detection plugins.
 */
class CommandTestDamage extends PlayerCommand {

	private static final String ARGUMENT_DAMAGE = "damage";
	private static final String ARGUMENT_TIMES_PER_TICK = "times-per-tick";
	private static final String ARGUMENT_DURATION_TICKS = "duration-ticks";

	private final SKShopkeepersPlugin plugin;

	CommandTestDamage(SKShopkeepersPlugin plugin) {
		super("testDamage");
		this.plugin = plugin;

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Produces damage events for the targeted entity."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);

		// Arguments:
		this.addArgument(
				new BoundedDoubleArgument(ARGUMENT_DAMAGE, 0.0D, Double.MAX_VALUE)
						.orDefaultValue(0.0D)
		);
		this.addArgument(
				new PositiveIntegerArgument(ARGUMENT_TIMES_PER_TICK)
						.orDefaultValue(1)
		);
		this.addArgument(
				new PositiveIntegerArgument(ARGUMENT_DURATION_TICKS)
						.orDefaultValue((int) Ticks.fromSeconds(10))
		);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		Player player = (Player) input.getSender();
		double damage = context.get(ARGUMENT_DAMAGE);
		assert damage >= 0.0D;
		int timesPerTick = context.get(ARGUMENT_TIMES_PER_TICK);
		assert timesPerTick >= 1;
		int durationTicks = context.get(ARGUMENT_DURATION_TICKS);
		assert durationTicks >= 1;

		LivingEntity target = (LivingEntity) EntityUtils.getTargetedEntity(
				player,
				(entity) -> entity instanceof LivingEntity
		);
		if (target == null) {
			player.sendMessage(ChatColor.RED + "No living entity targeted!");
			return;
		}

		// Start damage task:
		player.sendMessage(TextUtils.colorize("&aStarting damage task: Target: &e"
				+ target.getName() + "&a, Damage: &e" + TextUtils.format(damage)
				+ "&a, Per tick: &e" + timesPerTick + "&a, Duration &e" + durationTicks
				+ " ticks &a..."));

		AtomicInteger tickCounter = new AtomicInteger();

		SchedulerUtils.runTaskTimerOrOmit(player, task -> {
			boolean playerValid = player.isValid();
			if (tickCounter.get() >= durationTicks || !playerValid || !target.isValid()) {
				// We are done:
				if (playerValid) {
					player.sendMessage(ChatColor.GREEN + "... Done");
				}
				task.cancel();
				return;
			}

			// Apply damage:
			for (int i = 0; i < timesPerTick; ++i) {
				// Reset damage cooldown:
				target.setNoDamageTicks(0);
				target.setLastDamage(0.0D);

				// Damage:
				target.damage(damage, player);

				// Abort if the entity died:
				if (target.isDead()) {
					break;
				}
			}

			tickCounter.addAndGet(1);

			// Periodic progress feedback:
			if ((tickCounter.get() % 20) == 0) {
				player.sendMessage(ChatColor.GRAY + "... (" + ChatColor.YELLOW + tickCounter.get()
						+ ChatColor.GRAY + " / " + ChatColor.YELLOW + durationTicks
						+ ChatColor.GRAY + ")");
			}
		}, 1L, 1L);
	}
}
