package com.nisovin.shopkeepers.compat.v1_21_R9;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetBlockEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.util.java.Validate;

public class CopperChestProtectionListener implements Listener {

	private final ProtectedContainers protectedContainers;

	public CopperChestProtectionListener(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin");
		this.protectedContainers = plugin.getProtectedContainers();
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityTargetBlockEvent(EntityTargetBlockEvent e) {
		var targetBlock = e.getTarget();
		if (targetBlock == null) {
			return;
		}

		// The container is not a shop container:
		if (!protectedContainers.isProtectedContainer(targetBlock)) {
			return;
		}

		// Set the target to null instead of canceling the event to allow the copper golem to
		// continue to target other chests:
		e.setTarget(null);
	}
}
