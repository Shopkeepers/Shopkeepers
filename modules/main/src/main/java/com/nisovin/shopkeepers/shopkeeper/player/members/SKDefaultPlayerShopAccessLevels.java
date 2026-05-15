package com.nisovin.shopkeepers.shopkeeper.player.members;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.lang.Messages;

/**
 * The {@link DefaultPlayerShopAccessLevels}.
 */
public class SKDefaultPlayerShopAccessLevels implements DefaultPlayerShopAccessLevels {

	private final SKPlayerShopAccessLevel none = new SKPlayerShopAccessLevel(
			"none",
			0,
			() -> Messages.playerShopAccessLevelNone,
			() -> Messages.playerShopAccessLevelDescriptionNone
	);
	private final SKPlayerShopAccessLevel container = new SKPlayerShopAccessLevel(
			"container",
			10,
			() -> Messages.playerShopAccessLevelContainer,
			() -> Messages.playerShopAccessLevelDescriptionContainer
	);
	private final SKPlayerShopAccessLevel edit = new SKPlayerShopAccessLevel(
			"edit",
			20,
			() -> Messages.playerShopAccessLevelEdit,
			() -> Messages.playerShopAccessLevelDescriptionEdit
	);
	private final SKPlayerShopAccessLevel full = new SKPlayerShopAccessLevel(
			"full",
			99,
			() -> Messages.playerShopAccessLevelFull,
			() -> Messages.playerShopAccessLevelDescriptionFull
	);

	public SKDefaultPlayerShopAccessLevels() {
	}

	@Override
	public List<? extends SKPlayerShopAccessLevel> getAllAccessLevels() {
		List<SKPlayerShopAccessLevel> defaults = new ArrayList<>();
		defaults.add(none);
		defaults.add(container);
		defaults.add(edit);
		defaults.add(full);
		return defaults;
	}

	@Override
	public SKPlayerShopAccessLevel getNone() {
		return none;
	}

	@Override
	public SKPlayerShopAccessLevel getContainer() {
		return container;
	}

	@Override
	public SKPlayerShopAccessLevel getEdit() {
		return edit;
	}

	@Override
	public SKPlayerShopAccessLevel getFull() {
		return full;
	}

	// STATICS (for convenience):

	public static SKDefaultPlayerShopAccessLevels getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultPlayerShopAccessLevels();
	}

	public static SKPlayerShopAccessLevel NONE() {
		return getInstance().getNone();
	}

	public static SKPlayerShopAccessLevel CONTAINER() {
		return getInstance().getContainer();
	}

	public static SKPlayerShopAccessLevel EDIT() {
		return getInstance().getEdit();
	}

	public static SKPlayerShopAccessLevel FULL() {
		return getInstance().getFull();
	}
}
