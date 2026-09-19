package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class Button {

	// Volume 0.25 matches Minecraft's default button click volume.
	protected static final SoundEffect DEFAULT_BUTTON_CLICK_SOUND = new SoundEffect(Sound.UI_BUTTON_CLICK)
			.withVolume(0.25f);

	static final int NO_SLOT = -1;

	private final boolean placeAtEnd;

	private @Nullable EditorLayout editorLayout;
	private int slot = NO_SLOT;

	public Button() {
		this(false);
	}

	public Button(boolean placeAtEnd) {
		this.placeAtEnd = placeAtEnd;
	}

	void setEditorLayout(EditorLayout editorLayout) {
		Validate.State.isTrue(
				this.editorLayout == null,
				"Button was already added to some editor layout!"
		);
		Validate.isTrue(
				this.isApplicable(editorLayout),
				"Button is not applicable to this layout!"
		);

		this.editorLayout = editorLayout;
	}

	boolean isPlaceAtEnd() {
		return placeAtEnd;
	}

	int getSlot() {
		return slot;
	}

	void setSlot(int slot) {
		this.slot = slot;
	}

	protected boolean isApplicable(EditorLayout editorLayout) {
		return true;
	}

	protected @Nullable EditorLayout getEditorLayout() {
		return editorLayout;
	}

	/**
	 * Gets the {@link EditorView} this button has been created for.
	 * 
	 * @return the editor view, not <code>null</code>
	 */
	protected final EditorView getEditorView() {
		var editorLayout = Validate.State.notNull(
				this.editorLayout,
				"Button was not yet added to any editor layout!"
		);
		return editorLayout.getEditorView();
	}

	/**
	 * Gets an object that identifies this specific button type.
	 * <p>
	 * This is for example used to update the button icon across all {@link EditorView}s for the
	 * same context.
	 * <p>
	 * By default, this uses the (often anonymous) button class. Button types that share their class
	 * with other buttons have to override this.
	 * 
	 * @return the button identity, not <code>null</code>
	 */
	protected Object getIdentity() {
		return this.getClass();
	}

	public abstract @Nullable ItemStack getIcon();

	// Updates the icon in all editor views.
	protected final void updateIcon() {
		if (editorLayout == null) return;
		assert editorLayout != null;

		editorLayout.getEditorView().updateButtonInAllViews(this.getIdentity());
	}

	// Updates all button icons in all editor views.
	protected final void updateAllIcons() {
		if (editorLayout == null) return;
		assert editorLayout != null;

		editorLayout.getEditorView().updateButtonsInAllViews();
	}

	protected abstract void onClick(InventoryClickEvent clickEvent);
}
