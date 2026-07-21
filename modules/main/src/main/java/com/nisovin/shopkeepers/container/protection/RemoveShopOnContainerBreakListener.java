package com.nisovin.shopkeepers.container.protection;

import java.util.List;

import org.bukkit.ExplosionResult;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

class RemoveShopOnContainerBreakListener implements Listener {

	private final RemoveShopOnContainerBreak removeShopOnContainerBreak;

	RemoveShopOnContainerBreakListener(RemoveShopOnContainerBreak removeShopOnContainerBreak) {
		assert removeShopOnContainerBreak != null;
		this.removeShopOnContainerBreak = removeShopOnContainerBreak;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		removeShopOnContainerBreak.handleBlockBreakage(block);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		if (!isDestroyingBlocks(event.getExplosionResult())) {
			return;
		}

		List<Block> blockList = event.blockList();
		removeShopOnContainerBreak.handleBlocksBreakage(blockList);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		if (!isDestroyingBlocks(event.getExplosionResult())) {
			return;
		}

		List<Block> blockList = event.blockList();
		removeShopOnContainerBreak.handleBlocksBreakage(blockList);
	}

	private static boolean isDestroyingBlocks(ExplosionResult explosionResult) {
		return explosionResult == ExplosionResult.DESTROY
				|| explosionResult == ExplosionResult.DESTROY_WITH_DECAY;
	}
}
