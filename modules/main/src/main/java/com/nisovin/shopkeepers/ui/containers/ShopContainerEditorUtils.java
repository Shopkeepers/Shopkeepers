package com.nisovin.shopkeepers.ui.containers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.SKShopContainerType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * Editor helpers related to the shop containers.
 */
public final class ShopContainerEditorUtils {

	/**
	 * Checks whether the given shopkeeper's containers are represented via the containers editor
	 * (multiple containers are allowed or already present), rather than as a single container
	 * directly.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @return <code>true</code> if the containers editor is used
	 */
	public static boolean usesContainersEditor(PlayerShopkeeper shopkeeper) {
		return Settings.maxContainersPerPlayerShop > 1 || shopkeeper.getContainers().size() > 1;
	}

	/**
	 * Checks whether the given shopkeeper's containers can currently be removed via the editor.
	 * <p>
	 * We currently prevent users from removing their last shop container. This make it more
	 * complicated to move shopkeepers over long distances (nothing we really supported previously
	 * either), but avoids issues when "delete-shopkeeper-on-break-container" is enabled.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @return <code>true</code> if containers can be removed
	 */
	public static boolean isRemovalSupported(PlayerShopkeeper shopkeeper) {
		return shopkeeper.getContainers().size() > 1;
	}

	/**
	 * Creates the editor item representing the given shop container.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param container
	 *            the container, not <code>null</code>
	 * @param singleContainerButton
	 *            <code>true</code> if the item represents the shop's single container directly (in
	 *            the main editor), rather than an entry in the containers editor
	 * @return the editor item
	 */
	public static ItemStack createContainerItem(
			PlayerShopkeeper shopkeeper,
			ShopContainer container,
			boolean singleContainerButton
	) {
		return ItemUtils.setDisplayNameAndLore(
				Settings.containerItem.createItemStack(),
				Messages.shopContainerTitle,
				createContainerLore(shopkeeper, container, singleContainerButton)
		);
	}

	private static List<? extends String> createContainerLore(
			PlayerShopkeeper shopkeeper,
			ShopContainer container,
			boolean singleContainerButton
	) {
		List<? extends String> loreTemplate = singleContainerButton
				? Messages.singleShopContainerLore
				: Messages.shopContainerLore;

		SKShopContainerType containerType = (SKShopContainerType) container.getType();

		// Empty to not include the state lore line:
		String containerState = getContainerState(container);

		// The container type is only editable if the shop can use multiple containers. Otherwise,
		// every container always serves for both stock and earnings.
		String changeTypeAction = Settings.maxContainersPerPlayerShop > 1
				? Messages.shopContainerActionChangeType
				: "";

		// The single container button is opened with a plain click, whereas containers inside the
		// containers editor are opened via shift-right-click.
		String openAction = "";
		if (Settings.enablePlayerShopOpenContainer) {
			openAction = singleContainerButton
					? Messages.singleShopContainerActionOpen
					: Messages.shopContainerActionOpen;
		}

		String removeAction = isRemovalSupported(shopkeeper)
				? Messages.shopContainerActionRemove
				: "";

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("container_world", container.getWorldName());
		arguments.put("container_x", container.getX());
		arguments.put("container_y", container.getY());
		arguments.put("container_z", container.getZ());
		arguments.put("container_state", containerState);
		arguments.put("container_type", containerType.getDisplayName());
		arguments.put("container_type_description", containerType.getDescription());
		arguments.put("container_action_change_type", changeTypeAction);
		arguments.put("container_action_open", openAction);
		arguments.put("container_action_remove", removeAction);

		List<String> lore = StringUtils.flattenLines(
				StringUtils.replaceArguments(loreTemplate, arguments),
				true
		);

		// Drop lines whose arguments resolved to empty (e.g. the state or action lines):
		lore.removeIf(String::isEmpty);

		return lore;
	}

	// Returns a display string for the current state of the container block, or empty to not
	// display any state.
	private static String getContainerState(ShopContainer container) {
		Inventory containerInventory = container.getInventory();
		if (containerInventory == null) {
			return Messages.shopContainerStateMissing;
		} else if (containerInventory instanceof DoubleChestInventory) {
			return Messages.shopContainerStateDoubleChest;
		} else {
			return "";
		}
	}

	public static void openContainerContents(
			View editorView,
			PlayerShopkeeper shopkeeper,
			Player player,
			ShopContainer container
	) {
		// Keep the editor open if the container is missing:
		if (container.getInventory() == null) {
			TextUtils.sendMessage(player, Messages.shopContainerMissing);
			return;
		}

		editorView.closeDelayedAndRunTask(() -> {
			if (!player.isValid() || !shopkeeper.isValid()) return;

			if (!container.openInventoryView(player)) {
				TextUtils.sendMessage(player, Messages.shopContainerMissing);
			}
		});
	}

	private ShopContainerEditorUtils() {
	}
}
