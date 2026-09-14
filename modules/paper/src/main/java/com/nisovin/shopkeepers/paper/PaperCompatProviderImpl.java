package com.nisovin.shopkeepers.paper;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;

public class PaperCompatProviderImpl implements PaperCompatProvider {

	private final SKShopkeepersPlugin plugin;
	private final PaperEntityZapListener entityZapListener;

	public PaperCompatProviderImpl() {
		plugin = SKShopkeepersPlugin.getInstance();
		entityZapListener = new PaperEntityZapListener(plugin);
	}

	@Override
	public void onEnable() {
		entityZapListener.onEnable();
	}

	@Override
	public void onDisable() {
		entityZapListener.onDisable();
	}
}
