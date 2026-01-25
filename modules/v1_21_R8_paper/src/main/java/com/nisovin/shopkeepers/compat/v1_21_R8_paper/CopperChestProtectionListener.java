package com.nisovin.shopkeepers.compat.v1_21_R8_paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.util.java.Validate;

import io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent;

public class CopperChestProtectionListener implements Listener {

	private final ProtectedContainers protectedContainers;

	public CopperChestProtectionListener(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin");
		this.protectedContainers = plugin.getProtectedContainers();
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onItemTransportingEntityValidateTargetEvent(ItemTransportingEntityValidateTargetEvent e) {
		// Already disallowed:
		if (!e.isAllowed()) {
			return;
		}

		// The container is not a shop container:
		if (!protectedContainers.isProtectedContainer(Unsafe.assertNonNull(e.getBlock()))) {
			return;
		}

		e.setAllowed(false);
	}
}
