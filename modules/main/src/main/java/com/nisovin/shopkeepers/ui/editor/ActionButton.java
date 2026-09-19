package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A {@link Button} for simple one-click actions.
 */
public abstract class ActionButton extends Button {

	public ActionButton() {
		super();
	}

	public ActionButton(boolean placeAtEnd) {
		super(placeAtEnd);
	}

	protected void playButtonClickSound(boolean actionSuccess) {
		DEFAULT_BUTTON_CLICK_SOUND.play(this.getEditorView().getPlayer());
	}

	@Override
	protected final void onClick(InventoryClickEvent clickEvent) {
		if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return; // Ignore double clicks

		// Run action:
		boolean success = this.runAction(clickEvent);
		if (!success) return;

		// Post-processing:
		this.onActionSuccess(clickEvent);

		// Play sound:
		this.playButtonClickSound(success);

		// Icon might have changed:
		if (this.isUpdateIconOnActionSuccess()) {
			this.updateIcon();
		}
	}

	// Returns true on success:
	protected abstract boolean runAction(InventoryClickEvent clickEvent);

	protected void onActionSuccess(InventoryClickEvent clickEvent) {
		// Nothing by default.
	}

	// Whether the icon of this button is automatically updated after a successful action.
	// Actions that already update the affected icons themselves can disable this.
	protected boolean isUpdateIconOnActionSuccess() {
		return true;
	}
}
