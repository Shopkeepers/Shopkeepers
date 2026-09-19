package com.nisovin.shopkeepers.ui.villager.editor;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * An editor for regular villagers and wandering traders.
 */
public final class VillagerEditorView extends EditorView {

	protected VillagerEditorView(
			VillagerEditorViewProvider viewProvider,
			Player player,
			UIState uiState
	) {
		super(viewProvider, player, uiState);
	}

	/**
	 * Gets the villager that is being edited.
	 * 
	 * @return the villager, not <code>null</code>
	 */
	public AbstractVillager getVillager() {
		return (AbstractVillager) this.getContext().getObject();
	}

	@Override
	protected VillagerEditorLayout createLayout() {
		return new VillagerEditorLayout(this);
	}

	@Override
	protected String getTitle() {
		var villager = this.getVillager();
		String villagerName = villager.getName(); // Not null
		return StringUtils.replaceArguments(Messages.villagerEditorTitle,
				"villagerName", villagerName
		);
	}

	// TRADING RECIPES

	@Override
	protected void saveRecipes() {
		var player = this.getPlayer();
		var villager = this.getVillager();

		// The villager might have been unloaded in the meantime. Our changes won't have any effect
		// then:
		if (this.abortIfContextInvalid()) {
			return;
		}

		int changedTrades = this.getTradingRecipesAdapter().updateTradingRecipes(
				player,
				this.getRecipes()
		);
		if (changedTrades == 0) {
			// No changes:
			TextUtils.sendMessage(player, Messages.noVillagerTradesChanged);
			return;
		} else {
			TextUtils.sendMessage(player, Messages.villagerTradesChanged,
					"changedTrades", changedTrades
			);
		}

		if (villager instanceof Villager) {
			Villager regularVillager = (Villager) villager;
			// We set the villager experience to at least 1, so that the villager no longer
			// automatically changes its profession (and thereby its trades):
			if (regularVillager.getVillagerExperience() == 0) {
				regularVillager.setVillagerExperience(1);
				TextUtils.sendMessage(player, Messages.setVillagerXp, "xp", 1);
			}
		}
	}
}
