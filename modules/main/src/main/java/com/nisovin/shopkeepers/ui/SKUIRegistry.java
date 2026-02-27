package com.nisovin.shopkeepers.ui;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;
import com.nisovin.shopkeepers.ui.lib.AbstractUIType;
import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.java.Validate;

public class SKUIRegistry extends AbstractTypeRegistry<AbstractUIType>
		implements UIRegistry<AbstractUIType> {

	public SKUIRegistry() {
	}

	@Override
	protected String getTypeName() {
		return "UI type";
	}

	@Override
	public Collection<? extends View> getUISessions() {
		return UISessionManager.getInstance().getUISessions();
	}

	@Override
	public Collection<? extends View> getUISessions(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		return UISessionManager.getInstance().getUISessionsForContext(shopkeeper);
	}

	@Override
	public Collection<? extends View> getUISessions(Shopkeeper shopkeeper, UIType uiType) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		return UISessionManager.getInstance().getUISessionsForContext(shopkeeper, uiType);
	}

	@Override
	public Collection<? extends View> getUISessions(UIType uiType) {
		return UISessionManager.getInstance().getUISessions(uiType);
	}

	@Override
	public @Nullable View getUISession(Player player) {
		return UISessionManager.getInstance().getUISession(player);
	}

	@Override
	public void abortUISessions() {
		UISessionManager.getInstance().abortUISessions();
	}

	@Override
	public void abortUISessions(Shopkeeper shopkeeper) {
		UISessionManager.getInstance().abortUISessionsForContext(shopkeeper);
	}

	@Override
	public void abortUISessionsDelayed(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		UISessionManager.getInstance().abortUISessionsForContextDelayed(shopkeeper.getLocation(), shopkeeper);
	}
}
