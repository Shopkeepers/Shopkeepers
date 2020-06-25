package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.config.ConfigLoadException;
import com.nisovin.shopkeepers.config.migration.ConfigMigrations;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

public class Settings {

	public static final class DebugOptions {
		private DebugOptions() {
		}

		// Logs all events (spams!). Starts slightly delayed. Subsequent calls of the same event get combined into a
		// single logging entry to slightly reduce spam.
		public static final String logAllEvents = "log-all-events";
		// Prints the registered listeners for the first call of each event.
		public static final String printListeners = "print-listeners";
		// Enables debugging output related to shopkeeper activation.
		public static final String shopkeeperActivation = "shopkeeper-activation";
		// Enables additional commands related debugging output.
		public static final String commands = "commands";
		// Logs information when updating stored shop owner names.
		public static final String ownerNameUpdates = "owner-name-updates";
		// Logs whenever a shopkeeper performs item migrations (eg. for trading offers).
		public static final String itemMigrations = "item-migrations";
		// Enables debug output related to the user cache.
		public static final String userCache = "user-cache";
	}

	public static boolean isDebugging() {
		return isDebugging(null);
	}

	public static boolean isDebugging(String option) {
		if (Bukkit.isPrimaryThread()) {
			return Settings.debug && (option == null || Settings.debugOptions.contains(option));
		} else {
			AsyncSettings async = Settings.async();
			return async.debug && (option == null || async.debugOptions.contains(option));
		}
	}

	// cached values for settings used asynchronously
	public static final class AsyncSettings {

		private static volatile AsyncSettings INSTANCE = new AsyncSettings();

		private static void refresh() {
			INSTANCE = new AsyncSettings();
		}

		public final boolean debug;
		public final List<String> debugOptions;
		public final String fileEncoding;

		private AsyncSettings() {
			this.debug = Settings.debug;
			this.debugOptions = new ArrayList<String>(Settings.debugOptions);
			this.fileEncoding = Settings.fileEncoding;
		}
	}

	public static AsyncSettings async() {
		return AsyncSettings.INSTANCE;
	}

	/*
	 * General Settings
	 */
	public static int configVersion = 2;
	public static boolean debug = false;
	// See DebugOptions for all available options.
	public static List<String> debugOptions = new ArrayList<>(0);
	public static boolean enableMetrics = true;

	/*
	 * Shopkeeper Data
	 */
	public static String fileEncoding = "UTF-8";
	public static boolean saveInstantly = true;

	/*
	 * Plugin Compatibility
	 */
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean checkShopInteractionResult = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean registerWorldGuardAllowShopFlag = true;

	public static boolean enableTownyRestrictions = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static ItemData shopCreationItem = new ItemData(Material.VILLAGER_SPAWN_EGG);
	public static boolean preventShopCreationItemRegularUsage = false;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean createPlayerShopWithCommand = false;

	public static boolean requireChestRecentlyPlaced = true;
	public static int maxChestDistance = 15;
	public static int maxShopsPerPlayer = 0;
	public static String maxShopsPermOptions = "10,15,25";

	public static boolean protectChests = true;
	public static boolean preventItemMovement = true;
	public static boolean deleteShopkeeperOnBreakChest = false;

	public static int playerShopkeeperInactiveDays = 0;

	/*
	 * Shop (Object) Types
	 */
	public static List<String> enabledLivingShops = Arrays.asList(
			EntityType.VILLAGER.name(),
			EntityType.COW.name(),
			EntityType.MUSHROOM_COW.name(),
			EntityType.SHEEP.name(),
			EntityType.PIG.name(),
			EntityType.CHICKEN.name(),
			EntityType.OCELOT.name(),
			EntityType.RABBIT.name(),
			EntityType.WOLF.name(),
			EntityType.SNOWMAN.name(),
			EntityType.IRON_GOLEM.name(),
			EntityType.BLAZE.name(),
			EntityType.SILVERFISH.name(),
			EntityType.POLAR_BEAR.name(), // MC 1.10
			EntityType.SKELETON.name(),
			EntityType.STRAY.name(), // MC 1.11
			EntityType.WITHER_SKELETON.name(), // MC 1.11
			EntityType.SPIDER.name(),
			EntityType.CAVE_SPIDER.name(),
			EntityType.CREEPER.name(),
			EntityType.WITCH.name(),
			EntityType.ENDERMAN.name(),
			EntityType.ZOMBIE.name(),
			EntityType.ZOMBIE_VILLAGER.name(), // MC 1.11
			EntityType.PIG_ZOMBIE.name(),
			EntityType.HUSK.name(), // MC 1.11
			EntityType.GIANT.name(),
			EntityType.GHAST.name(),
			EntityType.SLIME.name(),
			EntityType.MAGMA_CUBE.name(),
			EntityType.SQUID.name(),
			EntityType.HORSE.name(),
			EntityType.MULE.name(),
			EntityType.DONKEY.name(),
			EntityType.SKELETON_HORSE.name(),
			EntityType.ZOMBIE_HORSE.name(),
			EntityType.EVOKER.name(), // MC 1.11
			EntityType.VEX.name(), // MC 1.11
			EntityType.VINDICATOR.name(), // MC 1.11
			EntityType.ILLUSIONER.name(), // MC 1.12
			EntityType.PARROT.name(), // MC 1.12
			EntityType.TURTLE.name(), // MC 1.13
			EntityType.PHANTOM.name(), // MC 1.13
			EntityType.COD.name(), // MC 1.13
			EntityType.SALMON.name(), // MC 1.13
			EntityType.PUFFERFISH.name(), // MC 1.13
			EntityType.TROPICAL_FISH.name(), // MC 1.13
			EntityType.DROWNED.name(), // MC 1.13
			EntityType.DOLPHIN.name(), // MC 1.13
			EntityType.CAT.name(), // MC 1.14
			EntityType.PANDA.name(), // MC 1.14
			EntityType.PILLAGER.name(), // MC 1.14
			EntityType.RAVAGER.name(), // MC 1.14
			EntityType.LLAMA.name(), // MC 1.11
			EntityType.TRADER_LLAMA.name(), // MC 1.14
			EntityType.WANDERING_TRADER.name(), // MC 1.14
			EntityType.FOX.name(), // MC 1.14
			"BEE" // MC 1.15
	);

	public static boolean useLegacyMobBehavior = false;
	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;
	public static boolean silenceLivingShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";

	public static boolean enableCitizenShops = false;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;
	public static String signShopFirstLine = "[SHOP]";

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static String editorTitle = "Shopkeeper Editor";

	public static ItemData previousPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData nextPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData currentPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData tradeSetupItem = new ItemData(Material.PAPER);

	public static ItemData nameItem = new ItemData(Material.NAME_TAG);

	public static boolean enableChestOptionOnPlayerShop = true;
	public static ItemData chestItem = new ItemData(Material.CHEST);

	public static ItemData deleteItem = new ItemData(Material.BONE);

	/*
	 * Non-shopkeeper villagers
	 */
	public static boolean disableOtherVillagers = false;
	public static boolean blockVillagerSpawns = false;
	public static boolean disableZombieVillagerCuring = false;
	public static boolean hireOtherVillagers = false;

	public static boolean disableWanderingTraders = false;
	public static boolean blockWanderingTraderSpawns = false;
	public static boolean hireWanderingTraders = false;

	/*
	 * Hiring
	 */
	public static ItemData hireItem = new ItemData(Material.EMERALD);
	public static int hireOtherVillagersCosts = 1;
	public static String forHireTitle = "For Hire";
	public static boolean hireRequireCreationPermission = true;

	/*
	 * Trading
	 */
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;
	public static boolean enablePurchaseLogging = false;
	public static boolean incrementVillagerStatistics = false;

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Currencies
	 */
	public static ItemData currencyItem = new ItemData(Material.EMERALD);
	public static ItemData zeroCurrencyItem = new ItemData(Material.BARRIER);
	public static ItemData highCurrencyItem = new ItemData(Material.EMERALD_BLOCK);
	public static ItemData zeroHighCurrencyItem = new ItemData(Material.BARRIER);

	// note: this can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	/*
	 * Messages
	 */
	public static String language = "en";

	// TODO replace all with Text? will require converting back to String, especially for texts used by items
	public static String msgShopTypeAdminRegular = "Admin shop";
	public static String msgShopTypeSelling = "Selling shop";
	public static String msgShopTypeBuying = "Buying shop";
	public static String msgShopTypeTrading = "Trading shop";
	public static String msgShopTypeBook = "Book shop";

	public static String msgShopTypeDescAdminRegular = "has unlimited stock";
	public static String msgShopTypeDescSelling = "sells items to players";
	public static String msgShopTypeDescBuying = "buys items from players";
	public static String msgShopTypeDescTrading = "trades items with players";
	public static String msgShopTypeDescBook = "sells book copies";

	public static String msgShopObjectTypeLiving = "{type}";
	public static String msgShopObjectTypeSign = "sign";
	public static String msgShopObjectTypeNpc = "npc";

	public static Text msgSelectedShopType = Text.parse("&aSelected shop type: &6{type} &7({description})");
	public static Text msgSelectedShopObjectType = Text.parse("&aSelected object type: &6{type}");

	public static Text msgCreationItemSelected = Text.parse("&aShop creation:\n"
			+ "&e  Left/Right-click to select the shop type.\n"
			+ "&e  Sneak + left/right-click to select the object type.\n"
			+ "&e  Right-click a chest to select it.\n"
			+ "&e  Then right-click a block to place the shopkeeper.");

	public static String msgButtonPreviousPage = "&6<- Previous page ({prev_page} of {max_page})";
	public static List<String> msgButtonPreviousPageLore = Arrays.asList();
	public static String msgButtonNextPage = "&6Next page ({next_page} of {max_page}) ->";
	public static List<String> msgButtonNextPageLore = Arrays.asList();
	public static String msgButtonCurrentPage = "&6Page {page} of {max_page}";
	public static List<String> msgButtonCurrentPageLore = Arrays.asList();

	public static String msgButtonName = "&aSet shop name";
	public static List<String> msgButtonNameLore = Arrays.asList("Lets you rename", "your shopkeeper");
	public static String msgButtonChest = "&aView chest inventory";
	public static List<String> msgButtonChestLore = Arrays.asList("Lets you view the inventory", " your shopkeeper is using");
	public static String msgButtonDelete = "&4Delete";
	public static List<String> msgButtonDeleteLore = Arrays.asList("Closes and removes", "this shopkeeper");
	public static String msgButtonHire = "&aHire";
	public static List<String> msgButtonHireLore = Arrays.asList("Buy this shop");

	public static String msgButtonSignVariant = "&aChoose sign variant";
	public static List<String> msgButtonSignVariantLore = Arrays.asList("Changes the sign's", "wood type");
	public static String msgButtonBaby = "&aToggle baby variant";
	public static List<String> msgButtonBabyLore = Arrays.asList("Toggles between the mob's", "baby and adult variant");
	public static String msgButtonSitting = "&aToggle sitting pose";
	public static List<String> msgButtonSittingLore = Arrays.asList("Toggles the mob's", "sitting pose");
	public static String msgButtonCatVariant = "&aChoose cat variant";
	public static List<String> msgButtonCatVariantLore = Arrays.asList("Changes the cat's look");
	public static String msgButtonCollarColor = "&aChoose collar color";
	public static List<String> msgButtonCollarColorLore = Arrays.asList("Changes the mob's", "collar color");
	public static String msgButtonWolfAngry = "&aToggle angry wolf";
	public static List<String> msgButtonWolfAngryLore = Arrays.asList("Toggles the wolf's", "angry state");
	public static String msgButtonCarryingChest = "&aToggle carrying chest";
	public static List<String> msgButtonCarryingChestLore = Arrays.asList("Toggles whether the mob", "is carrying a chest");
	public static String msgButtonHorseColor = "&aChoose horse color";
	public static List<String> msgButtonHorseColorLore = Arrays.asList("Changes the color", "of the horse");
	public static String msgButtonHorseStyle = "&aChoose horse style";
	public static List<String> msgButtonHorseStyleLore = Arrays.asList("Changes the coat pattern", "of the horse");
	public static String msgButtonHorseArmor = "&aChoose horse armor";
	public static List<String> msgButtonHorseArmorLore = Arrays.asList("Changes the armor", "of the horse");
	public static String msgButtonLlamaVariant = "&aChoose llama variant";
	public static List<String> msgButtonLlamaVariantLore = Arrays.asList("Changes the llama's look");
	public static String msgButtonLlamaCarpetColor = "&aLlama carpet color";
	public static List<String> msgButtonLlamaCarpetColorLore = Arrays.asList("Changes the llama's", "carpet color");
	public static String msgButtonCreeperCharged = "&aToggle charged creeper";
	public static List<String> msgButtonCreeperChargedLore = Arrays.asList("Toggles the creeper's", "charged state");
	public static String msgButtonFoxVariant = "&aChoose fox variant";
	public static List<String> msgButtonFoxVariantLore = Arrays.asList("Changes the fox's look");
	public static String msgButtonFoxCrouching = "&aToggle crouching pose";
	public static List<String> msgButtonFoxCrouchingLore = Arrays.asList("Toggles the fox's", "crouching pose");
	public static String msgButtonFoxSleeping = "&aToggle sleeping pose";
	public static List<String> msgButtonFoxSleepingLore = Arrays.asList("Toggles the fox's", "sleeping pose");
	public static String msgButtonMooshroomVariant = "&aChoose mooshroom variant";
	public static List<String> msgButtonMooshroomVariantLore = Arrays.asList("Changes the look", "of the mooshroom");
	public static String msgButtonPandaVariant = "&aChoose panda variant";
	public static List<String> msgButtonPandaVariantLore = Arrays.asList("Changes the panda's look");
	public static String msgButtonParrotVariant = "&aChoose parrot variant";
	public static List<String> msgButtonParrotVariantLore = Arrays.asList("Changes the parrot's look");
	public static String msgButtonPigSaddle = "&aToggle pig saddle";
	public static List<String> msgButtonPigSaddleLore = Arrays.asList("Toggles the pig's saddle");
	public static String msgButtonSheepColor = "&aChoose sheep color";
	public static List<String> msgButtonSheepColorLore = Arrays.asList("Changes the sheep's", "wool color");
	public static String msgButtonSheepSheared = "&aToggle sheared sheep";
	public static List<String> msgButtonSheepShearedLore = Arrays.asList("Toggles the sheep's", "sheared state");
	public static String msgButtonVillagerProfession = "&aChoose villager profession";
	public static List<String> msgButtonVillagerProfessionLore = Arrays.asList("Changes the profession", "of the villager");
	public static String msgButtonVillagerVariant = "&aChoose villager variant";
	public static List<String> msgButtonVillagerVariantLore = Arrays.asList("Changes the look", "of the villager");
	public static String msgButtonVillagerLevel = "&aChoose villager level";
	public static List<String> msgButtonVillagerLevelLore = Arrays.asList("Changes the level", "of the villager");
	public static String msgButtonZombieVillagerProfession = "&aChoose villager profession";
	public static List<String> msgButtonZombieVillagerProfessionLore = Arrays.asList("Changes the profession", "of the zombie villager");

	public static String msgTradingTitlePrefix = "&2";
	public static String msgTradingTitleDefault = "Shopkeeper";

	public static Text msgSelectedChest = Text.parse("&aChest selected! Right-click a block to place your shopkeeper.");
	public static Text msgMustSelectChest = Text.parse("&7You must right-click a chest before placing your shopkeeper.");
	public static Text msgNoChestSelected = Text.parse("&7The selected block is not a chest!");
	public static Text msgChestTooFar = Text.parse("&7The shopkeeper's chest is too far away!");
	public static Text msgChestNotPlaced = Text.parse("&7You must select a chest you have recently placed!");
	public static Text msgChestAlreadyInUse = Text.parse("&7Another shopkeeper is already using the selected chest!");
	public static Text msgNoChestAccess = Text.parse("&7You cannot access the selected chest!");
	public static Text msgTooManyShops = Text.parse("&7You have too many shops!");
	public static Text msgNoAdminShopTypeSelected = Text.parse("&7You have to select an admin shop type!");
	public static Text msgNoPlayerShopTypeSelected = Text.parse("&7You have to select a player shop type!");
	public static Text msgShopCreateFail = Text.parse("&7You cannot create a shopkeeper there.");

	public static Text msgTypeNewName = Text.parse("&aPlease type the shop's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.");
	public static Text msgNameSet = Text.parse("&aThe shop's name has been set!");
	public static Text msgNameHasNotChanged = Text.parse("&aThe shop's name has not changed.");
	public static Text msgNameInvalid = Text.parse("&aThat name is not valid!");

	public static Text msgShopTypeDisabled = Text.parse("&7The shop type '&6{type}&7' is disabled.");
	public static Text msgShopObjectTypeDisabled = Text.parse("&7The shop object type '&6{type}&7' is disabled.");

	public static Text msgMustTargetShop = Text.parse("&7You have to target a shopkeeper.");
	public static Text msgMustTargetAdminShop = Text.parse("&7You have to target an admin shopkeeper.");
	public static Text msgMustTargetPlayerShop = Text.parse("&7You have to target a player shopkeeper.");
	public static Text msgTargetEntityIsNoShop = Text.parse("&7The targeted entity is no shopkeeper.");
	public static Text msgTargetShopIsNoAdminShop = Text.parse("&7The targeted shopkeeper is no admin shopkeeper.");
	public static Text msgTargetShopIsNoPlayerShop = Text.parse("&7The targeted shopkeeper is no player shopkeeper.");
	public static Text msgUnusedChest = Text.parse("&7No shopkeeper is using this chest.");
	public static Text msgNotOwner = Text.parse("&7You are not the owner of this shopkeeper.");
	// placeholders: {owner} -> new owners name
	public static Text msgOwnerSet = Text.parse("&aNew owner was set to &e{owner}");
	public static Text msgShopCreationItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a shop creation item(s)!");
	public static String msgUnknownBookAuthor = "Unknown";

	public static Text msgTradePermSet = Text.parse("&aThe shop's trading permission has been set to '&e{perm}&a'!");
	public static Text msgTradePermRemoved = Text.parse("&aThe shop's trading permission '&e{perm}&a' has been removed!");
	public static Text msgTradePermView = Text.parse("&aThe shop's current trading permission is '&e{perm}&a'.");

	public static Text msgZombieVillagerCuringDisabled = Text.parse("&7Curing of zombie villagers is disabled.");
	public static Text msgMustHoldHireItem = Text.parse("&7You have to hold the required hire item in your hand.");
	public static Text msgSetForHire = Text.parse("&aThe Shopkeeper was set for hire.");
	public static Text msgHired = Text.parse("&aYou have hired this shopkeeper!");
	public static Text msgMissingHirePerm = Text.parse("&7You do not have the permission to hire shopkeepers.");
	public static Text msgCantHire = Text.parse("&aYou cannot afford to hire this shopkeeper.");
	public static Text msgCantHireShopType = Text.parse("&7You do not have the permission to hire this type of shopkeeper.");
	// placeholders: {costs}, {hire-item}
	public static Text msgVillagerForHire = Text.parse("&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.");

	public static Text msgMissingTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text msgMissingCustomTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text msgCantTradeWhileOwnerOnline = Text.parse("&7You cannot trade while the owner of this shop ('&e{owner}&7') is online.");

	public static Text msgShopkeeperCreated = Text.parse("&aShopkeeper created: &6{type} &7({description})\n{setupDesc}");

	public static String msgShopSetupDescSelling = "&e  Add items you want to sell to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescBuying = "&e  Add one of each item you want to buy to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescTrading = "&e  Add items you want to sell to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescBook = "&e  Add written books and blank books to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescAdminRegular = "&e  Right-click the shop while sneaking to modify trades.";

	public static String msgTradeSetupDescHeader = "&6{shopType}";
	public static List<String> msgTradeSetupDescAdminRegular = Arrays.asList("Has unlimited stock.", "Insert items from your inventory.", "Left/Right click to adjust amounts.", "Top row: Result items", "Bottom rows: Cost items");
	public static List<String> msgTradeSetupDescSelling = Arrays.asList("Sells items.", "Insert items to sell into the chest.", "Left/Right click to adjust amounts.", "Top row: Items being sold", "Bottom rows: Cost items");
	public static List<String> msgTradeSetupDescBuying = Arrays.asList("Buys items.", "Insert one of each item you want to", "buy and plenty of currency items", "into the chest.", "Left/Right click to adjust amounts.", "Top row: Cost items", "Bottom row: Items being bought");
	public static List<String> msgTradeSetupDescTrading = Arrays.asList("Trades items.", "Pickup an item from your inventory", "and then click a slot to place it.", "Left/Right click to adjust amounts.", "Top row: Result items", "Bottom rows: Cost items");
	public static List<String> msgTradeSetupDescBook = Arrays.asList("Sells book copies.", "Insert written and blank books", "into the chest.", "Left/Right click to adjust costs.", "Top row: Books being sold", "Bottom rows: Cost items");

	public static Text msgListAdminShopsHeader = Text.parse("&9There are &e{shopsCount} &9admin shops: &e(Page {page} of {maxPage})");
	public static Text msgListPlayerShopsHeader = Text.parse("&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page} of {maxPage})");
	public static Text msgListShopsEntry = Text.parse("  &e{shopId}) &7{shopName}&r&8at &7({location})&8, type: &7{shopType}&8, object: &7{objectType}");

	public static Text msgRemovedAdminShops = Text.parse("&e{shopsCount} &aadmin shops were removed.");
	public static Text msgRemovedShopsOfPlayer = Text.parse("&e{shopsCount} &ashops of player '&e{player}&a' were removed.");
	public static Text msgRemovedPlayerShops = Text.parse("&e{shopsCount} &aplayer shops were removed.");

	public static Text msgConfirmRemoveAllAdminShops = Text.parse("&cYou are about to irrevocable remove all admin shops (&6{shopsCount}&c)!");
	public static Text msgConfirmRemoveAllOwnShops = Text.parse("&cYou are about to irrevocable remove all your shops (&6{shopsCount}&c)!");
	public static Text msgConfirmRemoveAllShopsOfPlayer = Text.parse("&cYou are about to irrevocable remove all shops of player &6{player}&c (&6{shopsCount}&c)!");
	public static Text msgConfirmRemoveAllPlayerShops = Text.parse("&cYou are about to irrevocable remove all player shops of all players (&6{shopsCount}&c)!");

	public static Text msgConfirmationRequired = Text.parse("&7Please confirm this action by typing &6/shopkeepers confirm");
	public static Text msgConfirmationExpired = Text.parse("&cConfirmation expired.");
	public static Text msgNothingToConfirm = Text.parse("&cThere is nothing to confirm currently.");

	public static Text msgNoPermission = Text.parse("&cYou don't have the permission to do that.");

	public static Text msgCommandUnknown = Text.parse("&cUnknown command '&e{command}&c'!");
	public static Text msgCommandArgumentUnexpected = Text.parse("&cUnexpected argument '&e{argument}&c'.");
	public static Text msgCommandArgumentRequiresPlayer = Text.parse("&cArgument '&e{argumentFormat}&c' requires a player to execute the command.");
	public static Text msgCommandArgumentMissing = Text.parse("&cMissing argument '&e{argumentFormat}&c'.");
	public static Text msgCommandArgumentInvalid = Text.parse("&cInvalid argument '&e{argument}&c'.");
	public static Text msgCommandPlayerArgumentMissing = Text.parse("&cNo player specified for '&e{argumentFormat}&c'.");
	public static Text msgCommandPlayerArgumentInvalid = Text.parse("&cNo player found for '&e{argument}&c'.");
	public static Text msgCommandShopTypeArgumentInvalid = Text.parse("&cUnknown shop type '&e{argument}&c'.");
	public static Text msgCommandShopObjectTypeArgumentInvalid = Text.parse("&cUnknown shop object type '&e{argument}&c'.");
	public static Text msgCommandShopkeeperArgumentInvalid = Text.parse("&cNo shopkeeper found for '&e{argument}&c'.");
	public static Text msgCommandShopkeeperArgumentNoAdminShop = Text.parse("&cShopkeeper '&e{argument}&c' is no admin shopkeeper.");
	public static Text msgCommandShopkeeperArgumentNoPlayerShop = Text.parse("&cShopkeeper '&e{argument}&c' is no player shopkeeper.");

	public static Text msgAmbiguousPlayerName = Text.parse("&cThere are multiple matches for the name '&e{name}&c'!");
	public static Text msgAmbiguousPlayerNameEntry = Text.parse("&c  - '&e{name}&r&c' (&6{uuid}&c)");
	public static Text msgAmbiguousPlayerNameMore = Text.parse("&c  ....");

	public static Text msgAmbiguousShopkeeperName = Text.parse("&cThere are multiple matches for the name '&e{name}&c'!");
	public static Text msgAmbiguousShopkeeperNameEntry = Text.parse("&c  - &e{id})&c '&e{name}&r&c' (&6{uuid}&c)");
	public static Text msgAmbiguousShopkeeperNameMore = Text.parse("&c  - ....");

	public static Text msgAmbiguousTargetShopkeeper = Text.parse("&cThere are multiple shopkeepers being targeted!");
	public static Text msgAmbiguousTargetShopkeeperEntry = Text.parse("&c  - &e{id})&c '&e{name}&r&c' (&6{uuid}&c)");
	public static Text msgAmbiguousTargetShopkeeperMore = Text.parse("&c  - ....");

	public static Text msgCommandHelpTitle = Text.parse("&9***** &8[&6Shopkeepers v{version}&8] &9*****");
	public static Text msgCommandHelpUsageFormat = Text.parse("&e{usage}");
	public static Text msgCommandHelpDescriptionFormat = Text.parse("&8 - &3{description}");

	public static Text msgCommandDescriptionShopkeeper = Text.parse("Creates a shopkeeper.");
	public static Text msgCommandDescriptionHelp = Text.parse("Shows this help page.");
	public static Text msgCommandDescriptionReload = Text.parse("Reloads this plugin.");
	public static Text msgCommandDescriptionDebug = Text.parse("Toggles debug mode on and off.");
	public static Text msgCommandDescriptionList = Text.parse("Lists all shops for the specified player, or all admin shops.");
	public static Text msgCommandDescriptionRemove = Text.parse("Removes all shops for the specified player, all players, or all admin shops.");
	public static Text msgCommandDescriptionGive = Text.parse("Gives shop creation item(s) to the specified player.");
	public static Text msgCommandDescriptionRemote = Text.parse("Remotely opens a shop.");
	public static Text msgCommandDescriptionRemoteEdit = Text.parse("Remotely edits a shop.");
	public static Text msgCommandDescriptionTransfer = Text.parse("Transfers the ownership of a shop.");
	public static Text msgCommandDescriptionSettradeperm = Text.parse("Sets, removes (-) or displays (?) the trading permission.");
	public static Text msgCommandDescriptionSetforhire = Text.parse("Sets one of your shops for sale.");

	// /////

	private static String toConfigKey(String fieldName) {
		return fieldName.replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase(Locale.ROOT);
	}

	// returns true, if the config misses values which need to be saved
	public static boolean loadConfiguration(Configuration config) throws ConfigLoadException {
		boolean configChanged = false;

		// perform config migrations:
		boolean migrated = ConfigMigrations.applyMigrations(config);
		if (migrated) {
			configChanged = true;
		}

		// exempt a few string / string list settings from color conversion:
		List<String> noColorConversionKeys = Arrays.asList(
				toConfigKey("debugOptions"), toConfigKey("fileEncoding"), toConfigKey("shopCreationItemSpawnEggEntityType"),
				toConfigKey("maxShopsPermOptions"), toConfigKey("enabledLivingShops"), toConfigKey("nameRegex"),
				toConfigKey("language"));
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.isSynthetic()) continue;
				Class<?> typeClass = field.getType();
				Class<?> genericType = null;
				if (typeClass == List.class) {
					genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				String configKey = toConfigKey(field.getName());

				// initialize the setting with the default value, if it is missing in the config
				if (!config.isSet(configKey)) {
					Log.warning("Config: Inserting default value for missing config entry: " + configKey);

					// determine default value:
					Configuration defaults = config.getDefaults();
					Object defaultValue = loadConfigValue(defaults, configKey, noColorConversionKeys, typeClass, genericType);

					// validate default value:
					if (defaultValue == null) {
						Log.warning("Config: Missing default value for missing config entry: " + configKey);
						continue;
					} else if (!Utils.isAssignableFrom(typeClass, defaultValue.getClass())) {
						Log.warning("Config: Default value for missing config entry '" + configKey + "' is of wrong type: "
								+ "Got " + defaultValue.getClass().getName() + ", expecting " + typeClass.getName());
						continue;
					}

					// set default value:
					setConfigValue(config, configKey, noColorConversionKeys, typeClass, genericType, defaultValue);
					configChanged = true;
				}

				// load value:
				Object value = loadConfigValue(config, configKey, noColorConversionKeys, typeClass, genericType);
				field.set(null, value);
			}
		} catch (Exception e) {
			throw new ConfigLoadException("Error while loading config values!", e);
		}

		// validation:

		boolean foundInvalidEntityType = false;
		for (String entityTypeId : enabledLivingShops) {
			EntityType entityType = matchEntityType(entityTypeId);
			if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
				foundInvalidEntityType = true;
				Log.warning("Config: Invalid living entity type name in 'enabled-living-shops': " + entityTypeId);
			}
		}
		if (foundInvalidEntityType) {
			Log.warning("Config: All existing entity type names can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
		}

		if (maxChestDistance > 50) {
			Log.warning("Config: 'max-chest-distance' can be at most 50.");
			maxChestDistance = 50;
		}
		if (gravityChunkRange < 0) {
			Log.warning("Config: 'gravity-chunk-range' cannot be negative.");
			gravityChunkRange = 0;
		}
		// certain items cannot be of type AIR:
		if (shopCreationItem.getType() == Material.AIR) {
			Log.warning("Config: 'shop-creation-item' can not be AIR.");
			shopCreationItem = shopCreationItem.withType(Material.VILLAGER_SPAWN_EGG);
		}
		if (hireItem.getType() == Material.AIR) {
			Log.warning("Config: 'hire-item' can not be AIR.");
			hireItem = hireItem.withType(Material.EMERALD);
		}
		if (currencyItem.getType() == Material.AIR) {
			Log.warning("Config: 'currency-item' can not be AIR.");
			currencyItem = currencyItem.withType(Material.EMERALD);
		}
		if (namingOfPlayerShopsViaItem) {
			if (nameItem.getType() == Material.AIR) {
				Log.warning("Config: 'name-item' can not be AIR if naming-of-player-shops-via-item is enabled!");
				nameItem = nameItem.withType(Material.NAME_TAG);
			}
		}
		if (taxRate < 0) {
			Log.warning("Config: 'tax-rate' can not be less than 0!");
			taxRate = 0;
		} else if (taxRate > 100) {
			Log.warning("Config: 'tax-rate' can not be larger than 100!");
			taxRate = 100;
		}

		onSettingsChanged();
		return configChanged;
	}

	private static Object loadConfigValue(Configuration config, String configKey, List<String> noColorConversionKeys, Class<?> typeClass, Class<?> genericType) {
		if (typeClass == String.class || typeClass == Text.class) {
			String string = config.getString(configKey);
			// colorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				string = TextUtils.colorize(string);
			}
			if (typeClass == Text.class) {
				return Text.parse(string);
			} else {
				return string;
			}
		} else if (typeClass == int.class) {
			return config.getInt(configKey);
		} else if (typeClass == short.class) {
			return (short) config.getInt(configKey);
		} else if (typeClass == boolean.class) {
			return config.getBoolean(configKey);
		} else if (typeClass == Material.class) {
			// this assumes that legacy item conversion has already been performed
			Material material = ConfigUtils.loadMaterial(config, configKey);
			if (material == null) {
				Log.warning("Config: Unknown material for config entry '" + configKey + "': " + config.get(configKey));
				Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
			}
			return material;
		} else if (typeClass == ItemData.class) {
			ItemData itemData = ItemData.deserialize(config.get(configKey), (warning) -> {
				Log.warning("Config: Couldn't load item data for config entry '" + configKey + "': " + warning);
				if (warning.contains("Unknown item type")) { // TODO this is ugly
					Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
				}
			});
			// normalize to not null:
			if (itemData == null) {
				itemData = new ItemData(Material.AIR);
			}
			return itemData;
		} else if (typeClass == List.class) {
			if (genericType == String.class || genericType == Text.class) {
				List<String> stringList = config.getStringList(configKey);
				// colorize, if not exempted:
				if (!noColorConversionKeys.contains(configKey)) {
					stringList = TextUtils.colorize(stringList);
				}
				if (genericType == Text.class) {
					return Text.parse(stringList);
				} else {
					return stringList;
				}
			} else {
				return null; // not supported currently
			}
		}
		return null;
	}

	private static void setConfigValue(Configuration config, String configKey, List<String> noColorConversionKeys, Class<?> typeClass, Class<?> genericType, Object value) {
		if (value == null) {
			// remove value:
			config.set(configKey, null);
			return;
		}

		if (typeClass == Material.class) {
			config.set(configKey, ((Material) value).name());
		} else if (typeClass == String.class || typeClass == Text.class) {
			String stringValue;
			if (typeClass == Text.class) {
				stringValue = ((Text) value).toPlainFormatText();
			} else {
				stringValue = (String) value;
			}
			// decolorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				value = TextUtils.decolorize(stringValue);
			}
			config.set(configKey, value);
		} else if (typeClass == List.class && (genericType == String.class || genericType == Text.class)) {
			List<String> stringList;
			if (genericType == Text.class) {
				stringList = ((List<Text>) value).stream().map(Text::toPlainFormatText).collect(Collectors.toList());
			} else {
				stringList = (List<String>) value;
			}

			// decolorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				value = TextUtils.decolorize(stringList);
			}
			config.set(configKey, value);
		} else if (typeClass == ItemData.class) {
			config.set(configKey, ((ItemData) value).serialize());
		} else {
			config.set(configKey, value);
		}
	}

	public static void loadLanguageConfiguration(Configuration config) throws ConfigLoadException {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (!field.getName().startsWith("msg")) continue;
				Class<?> typeClass = field.getType();
				Class<?> genericType = null;
				if (typeClass == List.class) {
					genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				String configKey = toConfigKey(field.getName());
				if (!config.isSet(configKey)) {
					continue; // skip, keeps default
				}

				Object value = loadConfigValue(config, configKey, Collections.emptyList(), typeClass, genericType);
				if (value == null) {
					continue; // skip, keeps default
				}
				field.set(null, value);
			}
		} catch (Exception e) {
			throw new ConfigLoadException("Error while loading messages from language file!", e);
		}

		onSettingsChanged();
	}

	public static void onSettingsChanged() {
		// prepare derived settings:
		DerivedSettings.setup();

		// refresh async settings cache:
		AsyncSettings.refresh();
	}

	// item utilities:

	// stores derived settings that get setup after loading the config
	public static class DerivedSettings {

		public static ItemData namingItemData = new ItemData(Material.AIR);

		// button items:
		public static ItemData nameButtonItem = new ItemData(Material.AIR);
		public static ItemData chestButtonItem = new ItemData(Material.AIR);
		public static ItemData deleteButtonItem = new ItemData(Material.AIR);
		public static ItemData hireButtonItem = new ItemData(Material.AIR);

		public static Pattern shopNamePattern = Pattern.compile("^[A-Za-z0-9 ]{3,32}$");

		// gets called after the config has been loaded:
		private static void setup() {
			// ignore display name (which is used for specifying the new shopkeeper name):
			namingItemData = new ItemData(ItemUtils.setItemStackName(nameItem.createItemStack(), null));

			// button items:
			nameButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(nameItem.createItemStack(), msgButtonName, msgButtonNameLore));
			chestButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(chestItem.createItemStack(), msgButtonChest, msgButtonChestLore));
			deleteButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(deleteItem.createItemStack(), msgButtonDelete, msgButtonDeleteLore));
			hireButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(hireItem.createItemStack(), msgButtonHire, msgButtonHireLore));

			try {
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			} catch (PatternSyntaxException e) {
				Log.warning("Config: 'name-regex' is not a valid regular expression ('" + Settings.nameRegex + "'). Reverting to default.");
				Settings.nameRegex = "[A-Za-z0-9 ]{3,32}";
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			}
		}
	}

	// creation item:
	public static ItemStack createShopCreationItem() {
		return shopCreationItem.createItemStack();
	}

	public static boolean isShopCreationItem(ItemStack item) {
		return shopCreationItem.matches(item);
	}

	// naming item:
	public static boolean isNamingItem(ItemStack item) {
		return DerivedSettings.namingItemData.matches(item);
	}

	public static ItemStack createNameButtonItem() {
		return DerivedSettings.nameButtonItem.createItemStack();
	}

	// chest button:
	public static ItemStack createChestButtonItem() {
		return DerivedSettings.chestButtonItem.createItemStack();
	}

	// delete button:
	public static ItemStack createDeleteButtonItem() {
		return DerivedSettings.deleteButtonItem.createItemStack();
	}

	// hire item:
	public static ItemStack createHireButtonItem() {
		return DerivedSettings.hireButtonItem.createItemStack();
	}

	public static boolean isHireItem(ItemStack item) {
		return hireItem.matches(item);
	}

	// CURRENCY

	// currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return currencyItem.createItemStack(amount);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return currencyItem.matches(item);
	}

	// high currency item:
	public static boolean isHighCurrencyEnabled() {
		return (highCurrencyValue > 0 && highCurrencyItem.getType() != Material.AIR);
	}

	public static ItemStack createHighCurrencyItem(int amount) {
		if (!isHighCurrencyEnabled()) return null;
		return highCurrencyItem.createItemStack(amount);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		if (!isHighCurrencyEnabled()) return false;
		return highCurrencyItem.matches(item);
	}

	// zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (zeroCurrencyItem.getType() == Material.AIR) return null;
		return zeroCurrencyItem.createItemStack();
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (zeroCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroCurrencyItem.matches(item);
	}

	// high zero currency item:
	public static ItemStack createZeroHighCurrencyItem() {
		if (zeroHighCurrencyItem.getType() == Material.AIR) return null;
		return zeroHighCurrencyItem.createItemStack();
	}

	public static boolean isZeroHighCurrencyItem(ItemStack item) {
		if (zeroHighCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroHighCurrencyItem.matches(item);
	}

	//

	public static int getMaxShops(Player player) {
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (PermissionUtils.hasPermission(player, "shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		return maxShops;
	}

	public static EntityType matchEntityType(String entityTypeId) {
		if (StringUtils.isEmpty(entityTypeId)) return null;
		// get by bukkit id:
		String normalizedEntityTypeId = entityTypeId.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		try {
			return EntityType.valueOf(normalizedEntityTypeId);
		} catch (IllegalArgumentException e) {
			// unknown entity type:
			return null;
		}
	}
}
