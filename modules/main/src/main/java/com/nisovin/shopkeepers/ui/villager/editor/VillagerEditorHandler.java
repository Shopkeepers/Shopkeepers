package com.nisovin.shopkeepers.ui.villager.editor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIConfig;
import com.nisovin.shopkeepers.ui.editor.AbstractEditorHandler;
import com.nisovin.shopkeepers.ui.editor.ActionButton;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.state.UIState;
import com.nisovin.shopkeepers.ui.villager.VillagerUIHelper;
import com.nisovin.shopkeepers.ui.villager.equipmentEditor.VillagerEquipmentEditorUI;
import com.nisovin.shopkeepers.util.bukkit.MerchantUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * An editor for regular villagers and wandering traders.
 */
public final class VillagerEditorHandler extends AbstractEditorHandler {

	private static final Registry<Villager.@NonNull Profession> REGISTRY_VILLAGER_PROFESSION
			= Unsafe.castNonNull(Registry.VILLAGER_PROFESSION);
	private static final Registry<Villager.@NonNull Type> REGISTRY_VILLAGER_TYPE
			= Unsafe.castNonNull(Registry.VILLAGER_TYPE);

	// We compare the recipes from the editor with the original recipes and keep the original
	// recipes with their original internal data if the items have not changed.
	// TODO Somehow support changing/persisting: max-uses, uses, exp reward, villager xp reward,
	// price multiplier?
	// TODO The trades may change during the editor session, in which case the comparison between
	// new and old recipes no longer works (trades may get reverted to the editor state).
	private static class TradingRecipesAdapter
			extends DefaultTradingRecipesAdapter<@NonNull MerchantRecipe> {

		private final AbstractVillager villager;

		private TradingRecipesAdapter(AbstractVillager villager) {
			assert villager != null;
			this.villager = villager;
		}

		@Override
		public List<@NonNull TradingRecipeDraft> getTradingRecipes() {
			assert villager.isValid();
			@NonNull List<@NonNull MerchantRecipe> merchantRecipes = Unsafe.cast(villager.getRecipes());
			List<@NonNull TradingRecipeDraft> recipes = MerchantUtils.createTradingRecipeDrafts(
					merchantRecipes
			);
			return recipes;
		}

		@Override
		protected List<? extends @NonNull MerchantRecipe> getOffers() {
			assert villager.isValid();
			return Unsafe.cast(villager.getRecipes());
		}

		@Override
		protected void setOffers(List<? extends @NonNull MerchantRecipe> newOffers) {
			assert villager.isValid();

			// Stop any current trading with the villager:
			HumanEntity trader = villager.getTrader();
			if (trader != null) {
				trader.closeInventory();
				// TODO Send a message to the player explaining that the villager's trades have
				// changed?
			}

			// Apply the new trading recipes:
			villager.setRecipes(Unsafe.cast(newOffers));
		}

		@Override
		protected @Nullable MerchantRecipe createOffer(TradingRecipeDraft recipe) {
			return MerchantUtils.createMerchantRecipe(recipe);
		}

		@Override
		protected boolean areOffersEqual(MerchantRecipe oldOffer, MerchantRecipe newOffer) {
			// Keep the old recipe (including all of its other internal data) if the items are still
			// the same:
			return MerchantUtils.MERCHANT_RECIPES_EQUAL_ITEMS.equals(oldOffer, newOffer);
		}
	}

	private static final ConfirmationUIConfig CONFIRMATION_UI_CONFIG_DELETE_VILLAGER = new ConfirmationUIConfig() {

		@Override
		public String getTitle() {
			return Messages.confirmationUiDeleteVillagerTitle;
		}

		@Override
		public @Nullable List<? extends @NonNull String> getConfirmationLore() {
			return Messages.confirmationUiDeleteVillagerConfirmLore;
		}
	};

	private final AbstractVillager villager;
	private final String title; // Determined once during construction

	public VillagerEditorHandler(AbstractVillager villager) {
		super(SKDefaultUITypes.VILLAGER_EDITOR(), new TradingRecipesAdapter(villager));
		Validate.notNull(villager, "villager is null");
		this.villager = villager;
		String villagerName = villager.getName(); // Not null
		this.title = StringUtils.replaceArguments(Messages.villagerEditorTitle,
				"villagerName", villagerName
		);
	}

	public AbstractVillager getVillager() {
		return villager;
	}

	@Override
	protected String getEditorTitle() {
		return title;
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		// Check permission:
		if (!this.checkEditPermission(player, silent)) {
			return false;
		}
		return true;
	}

	private boolean checkEditPermission(Player player, boolean silent) {
		if (villager instanceof WanderingTrader) {
			return this.checkEditWanderingTraderPermission(player, silent);
		} else { // Regular villager
			return this.checkEditVillagerPermission(player, silent);
		}
	}

	private boolean checkEditWanderingTraderPermission(Player player, boolean silent) {
		return this.checkEditPermission(
				player,
				silent,
				ShopkeepersPlugin.EDIT_WANDERING_TRADERS_PERMISSION,
				Messages.missingEditWanderingTradersPerm
		);
	}

	private boolean checkEditVillagerPermission(Player player, boolean silent) {
		return this.checkEditPermission(
				player,
				silent,
				ShopkeepersPlugin.EDIT_VILLAGERS_PERMISSION,
				Messages.missingEditVillagersPerm
		);
	}

	private boolean checkEditPermission(
			Player player,
			boolean silent,
			String permission,
			Text missingPermissionMessage
	) {
		if (PermissionUtils.hasPermission(player, permission)) {
			return true;
		}

		if (!silent) {
			this.debugNotOpeningUI(player, "Player is missing the required edit permission.");
			TextUtils.sendMessage(player, missingPermissionMessage);
		}
		return false;
	}

	@Override
	public boolean openWindow(UISession uiSession, UIState uiState) {
		return super.openWindow(uiSession, uiState);
	}

	@Override
	protected void onInventoryClose(UISession uiSession, @Nullable InventoryCloseEvent closeEvent) {
		super.onInventoryClose(uiSession, closeEvent);
	}

	// BUTTONS

	@Override
	protected void setupButtons() {
		super.setupButtons();
		this.addButtonOrIgnore(this.createDeleteButton());
		// Note: Players can also use nametags to rename the villager like normal. However, this
		// option allows to set up colored names more conveniently.
		this.addButtonOrIgnore(this.createNamingButton());
		this.addButtonOrIgnore(this.createEquipmentButton());
		// Note: Wandering traders have an inventory as well, but it is usually always empty.
		this.addButtonOrIgnore(this.createContainerButton());
		if (villager instanceof Villager) {
			// The wandering trader does not support the baby state (even though it is ageable).
			this.addButtonOrIgnore(this.getBabyEditorButton());
		}
		this.addButtonOrIgnore(this.getProfessionEditorButton());
		this.addButtonOrIgnore(this.getVillagerTypeEditorButton());
		this.addButtonOrIgnore(this.getVillagerLevelEditorButton());
		this.addButtonOrIgnore(this.getAIButton());
		this.addButtonOrIgnore(this.getInvulnerabilityButton());
		// TODO Option to generate random vanilla trades? Maybe when changing the profession and/or
		// level and there are no trades currently?
	}

	protected Button createDeleteButton() {
		return new ActionButton(true) {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return DerivedSettings.deleteVillagerButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				UIState capturedUIState = captureState(editorSession.getUISession());
				editorSession.getUISession().closeDelayedAndRunTask(() -> {
					requestConfirmationDeleteVillager(editorSession, capturedUIState);
				});
				return true;
			}
		};
	}

	private void requestConfirmationDeleteVillager(EditorSession editorSession, UIState previousUIState) {
		Player player = editorSession.getPlayer();
		ConfirmationUI.requestConfirmation(player, CONFIRMATION_UI_CONFIG_DELETE_VILLAGER, () -> {
			// Delete confirmed.
			if (!player.isValid()) return;
			if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
				return;
			}

			villager.remove();
			TextUtils.sendMessage(player, Messages.villagerRemoved);
		}, () -> {
			// Delete cancelled.
			if (!player.isValid()) return;
			if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
				return;
			}

			// Try to open the editor again:
			SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(
					this,
					player,
					previousUIState
			);
		});
	}

	protected Button createNamingButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return DerivedSettings.nameVillagerButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				editorSession.getUISession().closeDelayedAndRunTask(() -> {
					Player player = editorSession.getPlayer();
					if (!player.isValid()) return;
					if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
						return;
					}

					// Start naming:
					SKShopkeepersPlugin.getInstance().getChatInput().request(player, message -> {
						renameVillager(editorSession, message);
					});
					TextUtils.sendMessage(player, Messages.typeNewVillagerName);
				});
				return true;
			}
		};
	}

	private void renameVillager(EditorSession editorSession, String newName) {
		assert editorSession != null && newName != null;
		if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
			return;
		}

		Player player = editorSession.getPlayer();

		// Prepare the new name:
		String preparedName = newName.trim();
		preparedName = TextUtils.convertHexColorsToBukkit(preparedName);
		preparedName = TextUtils.colorize(preparedName);

		if (preparedName.isEmpty() || preparedName.equals("-")) {
			// Remove name:
			preparedName = "";
		} else {
			// Validate name:
			if (!this.isValidName(preparedName)) {
				TextUtils.sendMessage(player, Messages.villagerNameInvalid);
				return;
			}
		}

		// Apply new name:
		if (preparedName.isEmpty()) {
			villager.setCustomName(null);
		} else {
			// Further preparation:
			villager.setCustomName(preparedName);
		}

		// Inform player:
		TextUtils.sendMessage(player, Messages.villagerNameSet);
	}

	private static final int MAX_NAME_LENGTH = 256;

	private boolean isValidName(String name) {
		assert name != null;
		return name.length() <= MAX_NAME_LENGTH;
	}

	protected Button createEquipmentButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return ItemUtils.setDisplayNameAndLore(
						new ItemStack(Material.ARMOR_STAND),
						Messages.buttonEquipment,
						Messages.buttonEquipmentLore
				);
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				editorSession.getUISession().closeDelayedAndRunTask(() -> {
					Player player = editorSession.getPlayer();
					if (!player.isValid()) return;
					if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
						return;
					}

					VillagerEquipmentEditorUI.request(villager, player);
				});
				return true;
			}
		};
	}

	protected Button createContainerButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return DerivedSettings.villagerInventoryButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				editorSession.getUISession().closeDelayedAndRunTask(() -> {
					Player player = editorSession.getPlayer();
					if (!player.isValid()) return;
					if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
						return;
					}

					// We cannot open the villagers inventory directly. Instead, we create custom
					// inventory with its contents. However, any changes in the inventory are not
					// reflected in the villager.
					// TODO Apply inventory changes? The inventory may change during the editor
					// session ...
					Inventory villagerInventory = villager.getInventory();
					int inventorySize = (int) Math.ceil(villagerInventory.getSize() / 9.0D) * 9;

					String villagerName = villager.getName(); // Not null
					String inventoryTitle = StringUtils.replaceArguments(
							Messages.villagerInventoryTitle,
							"villagerName", villagerName
					);
					Inventory customInventory = Bukkit.createInventory(
							null,
							inventorySize,
							inventoryTitle
					);

					// Copy storage contents:
					customInventory.setStorageContents(villagerInventory.getStorageContents());
					player.openInventory(customInventory);
				});
				return true;
			}
		};
	}

	protected Button getBabyEditorButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				ItemStack iconItem = new ItemStack(Material.EGG);
				ItemUtils.setDisplayNameAndLore(
						iconItem,
						Messages.buttonBaby,
						Messages.buttonBabyLore
				);
				return iconItem;
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				if (villager.isAdult()) {
					villager.setBaby();
				} else {
					// TODO: MC-9568: Growing up mobs get moved.
					Location location = villager.getLocation();
					villager.setAdult();
					SKShopkeepersPlugin.getInstance().getForcingEntityTeleporter().teleport(villager, location);
				}
				return true;
			}
		};
	}

	protected @Nullable Button getProfessionEditorButton() {
		if (!(villager instanceof Villager)) return null;
		Villager regularVillager = (Villager) villager;
		return new ActionButton() {

			private Profession profession = regularVillager.getProfession();

			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				ItemStack iconItem;
				switch (profession) {
				case ARMORER:
					iconItem = new ItemStack(Material.BLAST_FURNACE);
					break;
				case BUTCHER:
					iconItem = new ItemStack(Material.SMOKER);
					break;
				case CARTOGRAPHER:
					iconItem = new ItemStack(Material.CARTOGRAPHY_TABLE);
					break;
				case CLERIC:
					iconItem = new ItemStack(Material.BREWING_STAND);
					break;
				case FARMER:
					iconItem = new ItemStack(Material.WHEAT); // Instead of COMPOSTER
					break;
				case FISHERMAN:
					iconItem = new ItemStack(Material.FISHING_ROD); // Instead of BARREL
					break;
				case FLETCHER:
					iconItem = new ItemStack(Material.FLETCHING_TABLE);
					break;
				case LEATHERWORKER:
					iconItem = new ItemStack(Material.LEATHER); // Instead of CAULDRON
					break;
				case LIBRARIAN:
					iconItem = new ItemStack(Material.LECTERN);
					break;
				case MASON:
					iconItem = new ItemStack(Material.STONECUTTER);
					break;
				case SHEPHERD:
					iconItem = new ItemStack(Material.LOOM);
					break;
				case TOOLSMITH:
					iconItem = new ItemStack(Material.SMITHING_TABLE);
					break;
				case WEAPONSMITH:
					iconItem = new ItemStack(Material.GRINDSTONE);
					break;
				case NITWIT:
					iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
					ItemUtils.setLeatherColor(iconItem, Color.GREEN);
					break;
				case NONE:
				default:
					iconItem = new ItemStack(Material.BARRIER);
					break;
				}
				assert iconItem != null;
				ItemUtils.setDisplayNameAndLore(iconItem,
						Messages.buttonVillagerProfession,
						Messages.buttonVillagerProfessionLore
				);
				return iconItem;
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				boolean backwards = clickEvent.isRightClick();
				// Changing the profession will change the trades. Closing the editor view will
				// replace the new trades with the old ones from the editor. But we try to preserve
				// the old trades with their original data:
				List<MerchantRecipe> previousRecipes = villager.getRecipes();
				profession = RegistryUtils.cycleKeyed(
						REGISTRY_VILLAGER_PROFESSION,
						profession,
						backwards
				);
				regularVillager.setProfession(profession);
				// Restore previous trades with their original data:
				villager.setRecipes(previousRecipes);

				// We set the villager experience to at least 1, so that the villager does no longer
				// automatically change its profession:
				if (regularVillager.getVillagerExperience() == 0) {
					regularVillager.setVillagerExperience(1);
					Player player = editorSession.getPlayer();
					TextUtils.sendMessage(player, Messages.setVillagerXp, "xp", 1);
				}
				return true;
			}
		};
	}

	protected @Nullable Button getVillagerTypeEditorButton() {
		if (!(villager instanceof Villager)) return null;
		Villager regularVillager = (Villager) villager;
		return new ActionButton() {

			private Villager.Type villagerType = regularVillager.getVillagerType();

			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
				switch (villagerType) {
				default:
				case PLAINS:
					// Default brown color:
					break;
				case DESERT:
					ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
					break;
				case JUNGLE:
					ItemUtils.setLeatherColor(iconItem, Color.YELLOW.mixColors(Color.ORANGE));
					break;
				case SAVANNA:
					ItemUtils.setLeatherColor(iconItem, Color.RED);
					break;
				case SNOW:
					ItemUtils.setLeatherColor(iconItem, DyeColor.CYAN.getColor());
					break;
				case SWAMP:
					ItemUtils.setLeatherColor(iconItem, DyeColor.PURPLE.getColor());
					break;
				case TAIGA:
					ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.BROWN));
					break;
				}
				ItemUtils.setDisplayNameAndLore(iconItem,
						Messages.buttonVillagerVariant,
						Messages.buttonVillagerVariantLore
				);
				return iconItem;
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				boolean backwards = clickEvent.isRightClick();
				villagerType = RegistryUtils.cycleKeyed(
						REGISTRY_VILLAGER_TYPE,
						villagerType,
						backwards
				);
				regularVillager.setVillagerType(villagerType);
				return true;
			}
		};
	}

	protected @Nullable Button getVillagerLevelEditorButton() {
		if (!(villager instanceof Villager)) return null;
		Villager regularVillager = (Villager) villager;
		return new ActionButton() {

			private int villagerLevel = regularVillager.getVillagerLevel();

			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				ItemStack iconItem;
				switch (regularVillager.getVillagerLevel()) {
				default:
				case 1:
					iconItem = new ItemStack(Material.COBBLESTONE);
					break;
				case 2:
					iconItem = new ItemStack(Material.IRON_INGOT);
					break;
				case 3:
					iconItem = new ItemStack(Material.GOLD_INGOT);
					break;
				case 4:
					iconItem = new ItemStack(Material.EMERALD);
					break;
				case 5:
					iconItem = new ItemStack(Material.DIAMOND);
					break;
				}
				assert iconItem != null;
				// TODO Change the default message back to mention the villager level, instead of
				// just the badge color?
				ItemUtils.setDisplayNameAndLore(iconItem,
						Messages.buttonVillagerLevel,
						Messages.buttonVillagerLevelLore
				);
				return iconItem;
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				boolean backwards = clickEvent.isRightClick();
				if (backwards) {
					villagerLevel -= 1;
				} else {
					villagerLevel += 1;
				}
				villagerLevel = MathUtils.rangeModulo(villagerLevel, 1, 5);
				regularVillager.setVillagerLevel(villagerLevel);
				return true;
			}
		};
	}

	protected Button getAIButton() {
		return new ActionButton() {

			private boolean hasAI = villager.hasAI();

			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				ItemStack iconItem;
				if (hasAI) {
					iconItem = new ItemStack(Material.JACK_O_LANTERN);
				} else {
					iconItem = new ItemStack(Material.CARVED_PUMPKIN);
				}
				assert iconItem != null;
				ItemUtils.setDisplayNameAndLore(iconItem,
						Messages.buttonMobAi,
						Messages.buttonMobAiLore
				);
				return iconItem;
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				hasAI = !hasAI;
				villager.setAI(hasAI);
				return true;
			}
		};
	}

	protected Button getInvulnerabilityButton() {
		return new ActionButton() {

			private boolean invulnerable = villager.isInvulnerable();

			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				ItemStack iconItem;
				if (invulnerable) {
					iconItem = new ItemStack(Material.POTION);
					PotionMeta potionMeta = Unsafe.castNonNull(iconItem.getItemMeta());
					potionMeta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL));
					iconItem.setItemMeta(potionMeta);
				} else {
					iconItem = new ItemStack(Material.GLASS_BOTTLE);
				}
				assert iconItem != null;
				ItemUtils.setDisplayNameAndLore(iconItem,
						Messages.buttonInvulnerability,
						Messages.buttonInvulnerabilityLore
				);
				return iconItem;
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
					return false;
				}

				invulnerable = !invulnerable;
				villager.setInvulnerable(invulnerable);
				return true;
			}
		};
	}

	// TRADING RECIPES

	@Override
	protected ItemStack createTradeSetupIcon() {
		String villagerName = villager.getName(); // Not null
		String itemName = StringUtils.replaceArguments(Messages.villagerEditorDescriptionHeader,
				"villagerName", villagerName
		);
		List<? extends @NonNull String> itemLore = Messages.villagerEditorDescription;
		return ItemUtils.setDisplayNameAndLore(
				Settings.tradeSetupItem.createItemStack(),
				itemName,
				itemLore
		);
	}

	@Override
	protected void saveRecipes(EditorSession editorSession) {
		Player player = editorSession.getPlayer();
		// The villager might have been unloaded in the meantime. Our changes won't have any effect
		// then:
		if (!VillagerUIHelper.checkVillagerValid(villager, editorSession.getUISession())) {
			Log.debug("The villager edited by " + player.getName()
					+ " no longer exists or has been unloaded.");
			return;
		}

		int changedTrades = tradingRecipesAdapter.updateTradingRecipes(
				player,
				editorSession.getRecipes()
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
