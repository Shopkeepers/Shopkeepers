package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.BoundedIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.TimeUtils;

/**
 * Measures the time it takes to despawn and respawn the active shopkeepers within the current
 * chunk.
 */
class CommandTestSpawn extends PlayerCommand {

	private static final String ARGUMENT_REPETITIONS = "repetitions";

	private final SKShopkeepersPlugin plugin;

	CommandTestSpawn(SKShopkeepersPlugin plugin) {
		super("testSpawn");
		this.plugin = plugin;

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Measures the time it takes to respawn the active shopkeepers "
				+ "within the current chunk."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);

		// Arguments:
		this.addArgument(
				new BoundedIntegerArgument(ARGUMENT_REPETITIONS, 1, 1000)
						.orDefaultValue(10)
		);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		Player player = (Player) input.getSender();
		int repetitions = context.get(ARGUMENT_REPETITIONS);
		assert repetitions >= 1;

		// Get the shopkeepers of the current chunk:
		ChunkCoords chunkCoords = new ChunkCoords(player.getLocation());
		Collection<? extends AbstractShopkeeper> chunkShopkeepers = plugin.getShopkeeperRegistry().getShopkeepersInChunk(chunkCoords);
		if (chunkShopkeepers.isEmpty()) {
			player.sendMessage(ChatColor.RED + "There are no shopkeepers in this chunk ("
					+ chunkCoords.getChunkX() + "," + chunkCoords.getChunkZ() + ")!");
			return;
		}

		// We only despawn and spawn the shopkeepers that are currently spawned and active:
		List<AbstractShopkeeper> activeShopkeepers = new ArrayList<>(chunkShopkeepers.size());
		chunkShopkeepers.forEach(shopkeeper -> {
			if (shopkeeper.getShopObject().isActive()) {
				activeShopkeepers.add(shopkeeper);
			}
		});

		// Check if we have any active shopkeepers:
		if (activeShopkeepers.isEmpty()) {
			player.sendMessage(ChatColor.RED + "There are no active shopkeepers in this chunk ("
					+ chunkCoords.getChunkX() + "," + chunkCoords.getChunkZ() + ")!");
			return;
		}

		player.sendMessage(ChatColor.GREEN + "Measuring the time it takes to respawn the active "
				+ "shopkeepers within this chunk ...");

		long startTimeNanos = System.nanoTime();

		long[] despawnTimesNanos = new long[repetitions];
		long[] spawnTimesNanos = new long[repetitions];
		int failedToSpawn = 0;
		for (int i = 0; i < repetitions; ++i) {
			Result result = testSpawn(activeShopkeepers);
			despawnTimesNanos[i] = result.despawnTimeNanos;
			spawnTimesNanos[i] = result.spawnTimeNanos;
			failedToSpawn += result.failedToSpawn;
		}

		double avgDespawnTimeMillis = TimeUtils.convert(
				MathUtils.average(despawnTimesNanos),
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		double avgDespawnTimePerShopkeeperMillis = avgDespawnTimeMillis / activeShopkeepers.size();

		double maxDespawnTimeMillis = TimeUtils.convert(
				MathUtils.max(despawnTimesNanos),
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		double maxDespawnTimePerShopkeeperMillis = maxDespawnTimeMillis / activeShopkeepers.size();

		double avgSpawnTimeMillis = TimeUtils.convert(
				MathUtils.average(spawnTimesNanos),
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		double avgSpawnTimePerShopkeeperMillis = avgSpawnTimeMillis / activeShopkeepers.size();

		double maxSpawnTimeMillis = TimeUtils.convert(
				MathUtils.max(spawnTimesNanos),
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		double maxSpawnTimePerShopkeeperMillis = maxSpawnTimeMillis / activeShopkeepers.size();

		double totalDurationMillis = TimeUtils.convert(
				System.nanoTime() - startTimeNanos,
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);

		// Result summary:
		player.sendMessage(ChatColor.GREEN + "Shopkeepers: " + ChatColor.YELLOW + activeShopkeepers.size()
				+ (chunkShopkeepers.size() > activeShopkeepers.size() ? " / " + chunkShopkeepers.size() : "")
				+ ChatColor.GREEN + "   Repetitions: " + ChatColor.YELLOW + repetitions
				+ ChatColor.GREEN + "   Total duration: " + ChatColor.YELLOW
				+ TextUtils.format(totalDurationMillis) + " ms");

		if (failedToSpawn > 0) {
			player.sendMessage(ChatColor.RED + "  Failed to respawn " + ChatColor.YELLOW + failedToSpawn
					+ ChatColor.RED + " shopkeepers. The results might be inaccurate.");
		}

		player.sendMessage(ChatColor.GRAY + "  Despawn times (avg | avg per | max | max per): "
				+ ChatColor.WHITE + TextUtils.format(avgDespawnTimeMillis) + " ms"
				+ ChatColor.GRAY + " | " + ChatColor.WHITE
				+ TextUtils.formatPrecise(avgDespawnTimePerShopkeeperMillis) + " ms"
				+ ChatColor.GRAY + " | " + ChatColor.WHITE
				+ TextUtils.format(maxDespawnTimeMillis) + " ms"
				+ ChatColor.GRAY + " | " + ChatColor.WHITE
				+ TextUtils.formatPrecise(maxDespawnTimePerShopkeeperMillis) + " ms");
		player.sendMessage(ChatColor.GRAY + "  Spawn times (avg | avg per | max | max per): "
				+ ChatColor.WHITE + TextUtils.format(avgSpawnTimeMillis) + " ms"
				+ ChatColor.GRAY + " | " + ChatColor.WHITE
				+ TextUtils.formatPrecise(avgSpawnTimePerShopkeeperMillis) + " ms"
				+ ChatColor.GRAY + " | " + ChatColor.WHITE
				+ TextUtils.format(maxSpawnTimeMillis) + " ms"
				+ ChatColor.GRAY + " | " + ChatColor.WHITE
				+ TextUtils.formatPrecise(maxSpawnTimePerShopkeeperMillis) + " ms");
	}

	private static class Result {
		long despawnTimeNanos;
		long spawnTimeNanos;
		int failedToSpawn = 0;
	}

	private static Result testSpawn(Collection<? extends AbstractShopkeeper> shopkeepers) {
		Result result = new Result();

		// Despawn the shopkeepers:
		long despawnStartNanos = System.nanoTime();
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			shopkeeper.getShopObject().despawn();
		}
		result.despawnTimeNanos = System.nanoTime() - despawnStartNanos;

		// Respawn the shopkeepers:
		long spawnStartNanos = System.nanoTime();
		AtomicInteger failedToSpawn = new AtomicInteger();
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			Location location = shopkeeper.getLocation();
			if (location != null) {
				SchedulerUtils.runTaskOrOmit(location, () -> {
					boolean success = shopkeeper.getShopObject().spawn();
					if (!success) {
						failedToSpawn.getAndIncrement();
					}
				});
			} else {
				boolean success = shopkeeper.getShopObject().spawn();
				if (!success) {
					failedToSpawn.getAndIncrement();
				}
			}
		}
		result.spawnTimeNanos = System.nanoTime() - spawnStartNanos;
		result.failedToSpawn = failedToSpawn.get();
		return result;
	}
}
