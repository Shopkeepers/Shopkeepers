package com.nisovin.shopkeepers.paper;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;

public class PaperCompatProviderImpl implements PaperCompatProvider {

	private final SKShopkeepersPlugin plugin;
	private final PaperEntityZapListener entityZapListener;
	private final PaperPlayerLeftClickEntityListener playerLeftClickEntityListener;

	public PaperCompatProviderImpl() {
		plugin = SKShopkeepersPlugin.getInstance();
		entityZapListener = new PaperEntityZapListener(plugin);
		playerLeftClickEntityListener = new PaperPlayerLeftClickEntityListener(plugin);
	}

	@Override
	public void onEnable() {
		entityZapListener.onEnable();
		playerLeftClickEntityListener.onEnable();
	}

	@Override
	public void onDisable() {
		entityZapListener.onDisable();
		playerLeftClickEntityListener.onDisable();
	}
}
