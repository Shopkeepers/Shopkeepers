package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An {@link ActionButton} for simple one-click shopkeeper editing actions.
 * <p>
 * Successful actions trigger a {@link ShopkeeperEditedEvent} and a save of the shopkeeper.
 */
public abstract class ShopkeeperActionButton extends ActionButton {

	public ShopkeeperActionButton() {
		this(false);
	}

	public ShopkeeperActionButton(boolean placeAtEnd) {
		super(placeAtEnd);
	}

	@Override
	protected boolean isApplicable(EditorLayout editorLayout) {
		return super.isApplicable(editorLayout)
				&& editorLayout instanceof ShopkeeperEditorLayout;
	}

	protected Shopkeeper getShopkeeper() {
		var layout = this.getEditorLayout();
		Validate.State.notNull(layout, "Button was not yet added to any editor layout!");
		assert layout instanceof ShopkeeperEditorLayout; // Checked by isApplicable
		return ((ShopkeeperEditorLayout) layout).getShopkeeper();
	}

	@Override
	protected void onActionSuccess(InventoryClickEvent clickEvent) {
		Shopkeeper shopkeeper = this.getShopkeeper();

		// Call shopkeeper edited event:
		Player player = this.getEditorView().getPlayer();
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();
	}
}
