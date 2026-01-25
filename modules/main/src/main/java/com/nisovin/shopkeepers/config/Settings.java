package com.nisovin.shopkeepers.config;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.ConfigData;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.config.lib.setting.Setting;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.types.ItemDataValue;
import com.nisovin.shopkeepers.config.lib.value.types.ListValue;
import com.nisovin.shopkeepers.config.migration.ConfigMigrations;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.items.ItemUpdates;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.playershops.MaxShopsPermission;
import com.nisovin.shopkeepers.playershops.PlayerShopsLimit;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopobjects.living.types.MagmaCubeShop;
import com.nisovin.shopkeepers.shopobjects.living.types.SlimeShop;
import com.nisovin.shopkeepers.tradelog.TradeLogStorageType;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.Trilean;
import com.nisovin.shopkeepers.util.logging.Log;

public class Settings extends Config {

	/*
	 * General Settings
	 */
	public static int configVersion = 10;
	// Initial value: Lowest supported Minecraft data version: MC 1.21.5
	public static int dataVersion = 4325;
	public static boolean debug = false;
	// See DebugOptions for all available options.
	public static List<String> debugOptions = new ArrayList<>(0);
	public static boolean enableMetrics = true;

	/*
	 * Messages
	 */
	public static String language = "en-default";

	/*
	 * Shopkeeper Data
	 */
	public static boolean saveInstantly = true;

	/*
	 * Plugin Compatibility
	 */
	public static boolean ignoreFailedServerAssumptionTests = false;

	public static boolean bypassSpawnBlocking = true;
	public static boolean deleteUnspawnableShopkeepers = false;
	public static boolean checkShopInteractionResult = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean registerWorldGuardAllowShopFlag = true;

	public static boolean enableTownyRestrictions = false;

	public static boolean checkSpawnLocationInteractionResult = false;

	public static boolean disableInventoryVerification = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static ItemData shopCreationItem = new ItemData(
			Material.VILLAGER_SPAWN_EGG,
			"{\"text\":\"Shopkeeper\",\"italic\":false,\"color\":\"green\"}",
			null
	);

	public static boolean addShopCreationItemTag = true;
	public static boolean identifyShopCreationItemByTag = true;

	public static boolean preventShopCreationItemRegularUsage = true;
	public static boolean invertShopTypeAndObjectTypeSelection = false;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean requireContainerRecentlyPlaced = true;
	public static int maxContainerDistance = 15;
	public static int maxShopsPerPlayer = -1;
	public static String maxShopsPermOptions = "5,15,25";

	public static boolean protectContainers = true;
	public static boolean preventItemMovement = true;
	public static boolean preventCopperGolemAccess = true;
	public static boolean deleteShopkeeperOnBreakContainer = false;

	public static int playerShopkeeperInactiveDays = 0;

	/*
	 * Shop (Object) Types
	 */
	// Villager is the default and therefore first. The other entity types are alphabetically
	// sorted.
	// TODO Generate the default enabled mobs list based on the server's version.
	public static List<String> enabledLivingShops = CollectionUtils.addAll(
			new ArrayList<>(Arrays.asList(EntityType.VILLAGER.name())),
			CollectionUtils.sort(Arrays.asList(
					EntityType.COW.name(),
					EntityType.MOOSHROOM.name(),
					EntityType.SHEEP.name(),
					EntityType.PIG.name(),
					EntityType.CHICKEN.name(),
					EntityType.OCELOT.name(),
					EntityType.RABBIT.name(),
					EntityType.WOLF.name(),
					EntityType.SNOW_GOLEM.name(),
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
					EntityType.SHULKER.name(),
					EntityType.ARMOR_STAND.name(),
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
					EntityType.BEE.name(), // MC 1.15
					EntityType.ZOMBIFIED_PIGLIN.name(), // MC 1.16, replaced PIG_ZOMBIE
					EntityType.PIGLIN.name(), // MC 1.16
					EntityType.HOGLIN.name(), // MC 1.16
					EntityType.ZOGLIN.name(), // MC 1.16
					EntityType.STRIDER.name(), // MC 1.16
					EntityType.PIGLIN_BRUTE.name(), // MC 1.16.2
					EntityType.AXOLOTL.name(), // MC 1.17
					EntityType.GOAT.name(), // MC 1.17
					EntityType.GLOW_SQUID.name(), // MC 1.17
					EntityType.ALLAY.name(), // MC 1.19
					EntityType.FROG.name(), // MC 1.19
					EntityType.TADPOLE.name(), // MC 1.19
					EntityType.WARDEN.name(), // MC 1.19
					EntityType.CAMEL.name(), // MC 1.20
					EntityType.SNIFFER.name(), // MC 1.20
					EntityType.ARMADILLO.name(), // MC 1.20.5
					EntityType.BOGGED.name(), // MC 1.21
					EntityType.BREEZE.name(), // MC 1.21
					EntityType.CREAKING.name(), // MC 1.21.4
					"HAPPY_GHAST", // MC 1.21.6
					"COPPER_GOLEM", // MC 1.21.9
					"MANNEQUIN", // MC 1.21.9
					"CAMEL_HUSK", // MC 1.21.11
					"NAUTILUS", // MC 1.21.11
					"PARCHED", // MC 1.21.11
					"ZOMBIE_NAUTILUS" // MC 1.21.11
			), String::compareTo)
	);

	public static boolean enableEndCrystalShops = true;
	public static boolean allowEndCrystalShopsInTheEnd = false;

	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;

	// A tick period of 4 and higher is clearly noticeable, especially when entities are affected by
	// gravity.
	// The total performance benefits of higher tick periods also become increasingly smaller and
	// instead result in higher performance impacts per individual behavior update.
	// The gravity updates at a tick period of 2 actually appear less smooth in my testing than at a
	// period of 3 (maybe due to some interpolation artifact by the client).
	public static int entityBehaviorTickPeriod = 3;

	public static boolean shulkerPeekIfPlayerNearby = true;
	public static float shulkerPeekHeight = 0.3F;

	public static int slimeMaxSize = 5;
	public static int magmaCubeMaxSize = 5;

	public static boolean silenceShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;

	public static boolean enableCitizenShops = true;
	public static EntityType defaultCitizenNpcType = EntityType.PLAYER;
	public static boolean setCitizenNpcOwnerOfPlayerShops = false;
	public static Trilean citizenNpcFluidPushable = Trilean.FALSE;
	public static boolean cancelCitizenNpcInteractions = true;
	public static boolean saveCitizenNpcsInstantly = false;
	public static boolean snapshotsSaveCitizenNpcData = true;
	public static boolean deleteInvalidCitizenShopkeepers = false;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;
	public static boolean enableHangingSignShops = true;
	public static boolean enableGlowingSignText = true;

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,25}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static ItemData sellingEmptyTradeResultItem = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData sellingEmptyTradeItem1 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData sellingEmptyTradeItem2 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData sellingEmptyItem1 = new ItemData(Material.BARRIER);
	public static ItemData sellingEmptyItem2 = new ItemData(Material.BARRIER);

	public static ItemData buyingEmptyTradeResultItem = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData buyingEmptyTradeItem1 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData buyingEmptyTradeItem2 = new ItemData(Material.AIR);
	public static ItemData buyingEmptyResultItem = new ItemData(Material.BARRIER);
	public static ItemData buyingEmptyItem2 = new ItemData(Material.AIR);

	public static ItemData tradingEmptyTradeResultItem = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData tradingEmptyTradeItem1 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData tradingEmptyTradeItem2 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData tradingEmptyResultItem = new ItemData(Material.BARRIER);
	public static ItemData tradingEmptyItem1 = new ItemData(Material.BARRIER);
	public static ItemData tradingEmptyItem2 = new ItemData(Material.BARRIER);

	public static ItemData bookEmptyTradeResultItem = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData bookEmptyTradeItem1 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData bookEmptyTradeItem2 = new ItemData(Material.GRAY_STAINED_GLASS_PANE);
	public static ItemData bookEmptyItem1 = new ItemData(Material.BARRIER);
	public static ItemData bookEmptyItem2 = new ItemData(Material.BARRIER);

	public static int maxTradesPages = 5;

	public static ItemData previousPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData nextPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData currentPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData tradeSetupItem = new ItemData(Material.PAPER);
	public static ItemData shopInformationItem = new ItemData(Material.PAPER);

	public static ItemData placeholderItem = new ItemData(Material.PAPER);

	public static ItemData shopOpenItem = new ItemData(Material.GREEN_BANNER);
	public static ItemData shopClosedItem = new ItemData(Material.RED_BANNER);
	public static ItemData nameItem = new ItemData(Material.NAME_TAG);

	public static boolean enableAllEquipmentEditorSlots = false;

	public static boolean enableMovingOfPlayerShops = true;
	public static ItemData moveItem = new ItemData(Material.ENDER_PEARL);

	public static boolean enableContainerOptionOnPlayerShop = true;
	public static ItemData containerItem = new ItemData(Material.CHEST);

	public static ItemData tradeNotificationsItem = new ItemData(Material.BELL);
	public static ItemData deleteItem = new ItemData(Material.BONE);

	/*
	 * Non-shopkeeper villagers
	 */
	public static boolean disableOtherVillagers = false;
	public static List<String> disableOtherVillagersWorlds = new ArrayList<>();
	public static boolean blockVillagerSpawns = false;
	public static List<String> blockVillagerSpawnsWorlds = new ArrayList<>();
	public static boolean disableZombieVillagerCuring = false;
	public static List<String> disableZombieVillagerCuringWorlds = new ArrayList<>();
	public static boolean hireOtherVillagers = false;
	public static List<String> hireOtherVillagersWorlds = new ArrayList<>();

	public static boolean disableWanderingTraders = false;
	public static List<String> disableWanderingTradersWorlds = new ArrayList<>();
	public static boolean blockWanderingTraderSpawns = false;
	public static List<String> blockWanderingTraderSpawnsWorlds = new ArrayList<>();
	public static boolean hireWanderingTraders = false;
	public static List<String> hireWanderingTradersWorlds = new ArrayList<>();

	public static boolean editRegularVillagers = false;
	public static boolean editRegularWanderingTraders = false;

	/*
	 * Hiring
	 */
	public static ItemData hireItem = new ItemData(Material.EMERALD);
	public static int hireOtherVillagersCosts = 1;
	public static boolean hireRequireCreationPermission = true;

	/*
	 * Trading
	 */
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;

	public static boolean incrementVillagerStatistics = false;
	public static boolean simulateVillagerTradingSounds = true;
	public static boolean simulateVillagerAmbientSounds = false;
	public static boolean simulateWanderingTraderTradingSounds = true;
	public static boolean simulateWanderingTraderAmbientSounds = false;
	public static boolean simulateTradingSoundsOnlyForTheTradingPlayer = true;

	public static SoundEffect tradeSucceededSound = new SoundEffect(Sound.UI_BUTTON_CLICK)
			.withPitch(2.0f)
			.withVolume(0.3f);
	public static SoundEffect tradeFailedSound = new SoundEffect(Sound.BLOCK_BARREL_CLOSE)
			.withPitch(2.0f)
			.withVolume(0.5f);

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Trade Notifications
	 */
	public static boolean notifyPlayersAboutTrades = false;
	public static SoundEffect tradeNotificationSound = SoundEffect.EMPTY;

	public static boolean notifyShopOwnersAboutTrades = true;
	public static SoundEffect shopOwnerTradeNotificationSound = new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
			.withVolume(0.25f);

	/*
	 * Trade Log
	 */
	public static TradeLogStorageType tradeLogStorage = TradeLogStorageType.DISABLED;

	public static int tradeLogMergeDurationTicks = 300; // 15 seconds
	public static int tradeLogNextMergeTimeoutTicks = 100; // 5 seconds

	public static boolean logItemMetadata = false;

	/*
	 * Currencies
	 */
	public static ItemData currencyItem = new ItemData(Material.EMERALD);
	public static ItemData highCurrencyItem = new ItemData(Material.EMERALD_BLOCK);

	// Note: This can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	///// DERIVED SETTINGS

	// Stores derived settings which get setup after loading the config.
	public static class DerivedSettings {

		private static boolean initialSetup = true;

		public static DateTimeFormatter dateTimeFormatter = Unsafe.uncheckedNull();

		public static TradingRecipeDraft sellingEmptyTrade = Unsafe.uncheckedNull();
		public static TradingRecipeDraft sellingEmptyTradeSlotItems = Unsafe.uncheckedNull();
		public static TradingRecipeDraft buyingEmptyTrade = Unsafe.uncheckedNull();
		public static TradingRecipeDraft buyingEmptyTradeSlotItems = Unsafe.uncheckedNull();
		public static TradingRecipeDraft tradingEmptyTrade = Unsafe.uncheckedNull();
		public static TradingRecipeDraft tradingEmptyTradeSlotItems = Unsafe.uncheckedNull();
		public static TradingRecipeDraft bookEmptyTrade = Unsafe.uncheckedNull();
		public static TradingRecipeDraft bookEmptyTradeSlotItems = Unsafe.uncheckedNull();

		public static ItemData shopCreationItemData = Unsafe.uncheckedNull();
		public static ItemData placeholderItemData = Unsafe.uncheckedNull();
		public static ItemData namingItemData = Unsafe.uncheckedNull();

		// Button items:
		public static ItemData shopOpenButtonItem = Unsafe.uncheckedNull();
		public static ItemData shopClosedButtonItem = Unsafe.uncheckedNull();
		public static ItemData nameButtonItem = Unsafe.uncheckedNull();
		public static ItemData moveButtonItem = Unsafe.uncheckedNull();
		public static ItemData containerButtonItem = Unsafe.uncheckedNull();
		public static ItemData deleteButtonItem = Unsafe.uncheckedNull();
		public static ItemData hireButtonItem = Unsafe.uncheckedNull();

		public static ItemData deleteVillagerButtonItem = Unsafe.uncheckedNull();
		public static ItemData nameVillagerButtonItem = Unsafe.uncheckedNull();
		public static ItemData villagerInventoryButtonItem = Unsafe.uncheckedNull();

		public static Pattern shopNamePattern = Unsafe.uncheckedNull();

		// Sorted in descending order:
		public static final List<MaxShopsPermission> maxShopsPermissions = new ArrayList<>();

		public static final Set<EntityType> enabledLivingShops = new LinkedHashSet<>();

		static {
			// Initial setup of default values:
			setup();
			initialSetup = false;
		}

		// Gets called after setting values have changed (e.g. after the config has been loaded):
		private static void setup() {
			// TODO This formatter uses the server's default time zone. Allow configuring the time
			// zone?
			try {
				dateTimeFormatter = DateTimeFormatter.ofPattern(Messages.dateTimeFormat)
						.withZone(Unsafe.assertNonNull(ZoneId.systemDefault()));
			} catch (IllegalArgumentException e) {
				Log.warning(Messages.getInstance().getLogPrefix()
						+ "'date-time-format' is not a valid format pattern ('"
						+ Messages.dateTimeFormat + "'). Reverting to default.");
				Messages.dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
				dateTimeFormatter = DateTimeFormatter.ofPattern(Messages.dateTimeFormat)
						.withZone(Unsafe.assertNonNull(ZoneId.systemDefault()));
			}

			sellingEmptyTrade = new TradingRecipeDraft(
					ItemUtils.setDisplayNameAndLore(
							sellingEmptyTradeResultItem.createItemStack(),
							Messages.sellingShop_emptyTrade_resultItem,
							Messages.sellingShop_emptyTrade_resultItemLore
					),
					ItemUtils.setDisplayNameAndLore(
							sellingEmptyTradeItem1.createItemStack(),
							Messages.sellingShop_emptyTrade_item1,
							Messages.sellingShop_emptyTrade_item1Lore
					),
					// The editor item can be configured, even if the high currency is disabled:
					Currencies.isHighCurrencyEnabled() ? ItemUtils.setDisplayNameAndLore(
							sellingEmptyTradeItem2.createItemStack(),
							Messages.sellingShop_emptyTrade_item2,
							Messages.sellingShop_emptyTrade_item2Lore
					) : sellingEmptyTradeItem2.createItemStack()
			);
			sellingEmptyTradeSlotItems = new TradingRecipeDraft(
					// This item is never used, because the slot is never empty for a non-empty
					// trade:
					null,
					ItemUtils.setDisplayNameAndLore(
							sellingEmptyItem1.createItemStack(),
							Messages.sellingShop_emptyItem1,
							Messages.sellingShop_emptyItem1Lore
					),
					// The editor item can be configured, even if the high currency is disabled:
					Currencies.isHighCurrencyEnabled() ? ItemUtils.setDisplayNameAndLore(
							sellingEmptyItem2.createItemStack(),
							Messages.sellingShop_emptyItem2,
							Messages.sellingShop_emptyItem2Lore
					) : sellingEmptyItem2.createItemStack()
			);
			buyingEmptyTrade = new TradingRecipeDraft(
					ItemUtils.setDisplayNameAndLore(
							buyingEmptyTradeResultItem.createItemStack(),
							Messages.buyingShop_emptyTrade_resultItem,
							Messages.buyingShop_emptyTrade_resultItemLore
					),
					ItemUtils.setDisplayNameAndLore(
							buyingEmptyTradeItem1.createItemStack(),
							Messages.buyingShop_emptyTrade_item1,
							Messages.buyingShop_emptyTrade_item1Lore
					),
					// The editor item can be configured, even though this slot is not used for
					// anything:
					buyingEmptyTradeItem2.createItemStack()
			);
			buyingEmptyTradeSlotItems = new TradingRecipeDraft(
					ItemUtils.setDisplayNameAndLore(
							buyingEmptyResultItem.createItemStack(),
							Messages.buyingShop_emptyResultItem,
							Messages.buyingShop_emptyResultItemLore
					),
					// This item is never used, because the slot is never empty for a non-empty
					// trade:
					null,
					// The editor item can be configured, even though this slot is not used for
					// anything:
					buyingEmptyItem2.createItemStack()
			);
			tradingEmptyTrade = new TradingRecipeDraft(
					ItemUtils.setDisplayNameAndLore(
							tradingEmptyTradeResultItem.createItemStack(),
							Messages.tradingShop_emptyTrade_resultItem,
							Messages.tradingShop_emptyTrade_resultItemLore
					),
					ItemUtils.setDisplayNameAndLore(
							tradingEmptyTradeItem1.createItemStack(),
							Messages.tradingShop_emptyTrade_item1,
							Messages.tradingShop_emptyTrade_item1Lore
					),
					ItemUtils.setDisplayNameAndLore(
							tradingEmptyTradeItem2.createItemStack(),
							Messages.tradingShop_emptyTrade_item2,
							Messages.tradingShop_emptyTrade_item2Lore
					)
			);
			tradingEmptyTradeSlotItems = new TradingRecipeDraft(
					ItemUtils.setDisplayNameAndLore(
							tradingEmptyResultItem.createItemStack(),
							Messages.tradingShop_emptyResultItem,
							Messages.tradingShop_emptyResultItemLore
					),
					ItemUtils.setDisplayNameAndLore(
							tradingEmptyItem1.createItemStack(),
							Messages.tradingShop_emptyItem1,
							Messages.tradingShop_emptyItem1Lore
					),
					ItemUtils.setDisplayNameAndLore(
							tradingEmptyItem2.createItemStack(),
							Messages.tradingShop_emptyItem2,
							Messages.tradingShop_emptyItem2Lore
					)
			);
			bookEmptyTrade = new TradingRecipeDraft(
					ItemUtils.setDisplayNameAndLore(
							bookEmptyTradeResultItem.createItemStack(),
							Messages.bookShop_emptyTrade_resultItem,
							Messages.bookShop_emptyTrade_resultItemLore
					),
					ItemUtils.setDisplayNameAndLore(
							bookEmptyTradeItem1.createItemStack(),
							Messages.bookShop_emptyTrade_item1,
							Messages.bookShop_emptyTrade_item1Lore
					),
					// The editor item can be configured, even if the high currency is disabled:
					Currencies.isHighCurrencyEnabled() ? ItemUtils.setDisplayNameAndLore(
							bookEmptyTradeItem2.createItemStack(),
							Messages.bookShop_emptyTrade_item2,
							Messages.bookShop_emptyTrade_item2Lore
					) : bookEmptyTradeItem2.createItemStack()
			);
			bookEmptyTradeSlotItems = new TradingRecipeDraft(
					// This item is never used, because the slot is never empty for a non-empty
					// trade:
					null,
					ItemUtils.setDisplayNameAndLore(
							bookEmptyItem1.createItemStack(),
							Messages.bookShop_emptyItem1,
							Messages.bookShop_emptyItem1Lore
					),
					// The editor item can be configured, even if the high currency is disabled:
					Currencies.isHighCurrencyEnabled() ? ItemUtils.setDisplayNameAndLore(
							bookEmptyItem2.createItemStack(),
							Messages.bookShop_emptyItem2,
							Messages.bookShop_emptyItem2Lore
					) : bookEmptyItem2.createItemStack()
			);

			// If enabled, add the shop creation item tag:
			if (addShopCreationItemTag) {
				ItemStack shopCreationItemStack = shopCreationItem.createItemStack();
				var shopCreationItemHelper = new ShopCreationItem(shopCreationItemStack);
				shopCreationItemHelper.addTag();
				shopCreationItemHelper.applyItemMeta();
				shopCreationItemData = new ItemData(UnmodifiableItemStack.ofNonNull(
						shopCreationItemStack
				));
			} else {
				shopCreationItemData = shopCreationItem;
			}

			// Ignore (clear) the display name that is used to specify the substituted item type:
			placeholderItemData = new ItemData(UnmodifiableItemStack.ofNonNull(
					ItemUtils.setDisplayName(placeholderItem.createItemStack(), null)
			));

			// Ignore (clear) the display name that is used to specify the new shopkeeper name, but
			// keep the lore:
			namingItemData = new ItemData(UnmodifiableItemStack.ofNonNull(
					ItemUtils.setDisplayName(nameItem.createItemStack(), null)
			));

			// Button items:
			shopOpenButtonItem = new ItemData(
					shopOpenItem,
					Messages.buttonShopOpen,
					Messages.buttonShopOpenLore
			);
			shopClosedButtonItem = new ItemData(
					shopClosedItem,
					Messages.buttonShopClosed,
					Messages.buttonShopClosedLore
			);
			nameButtonItem = new ItemData(
					nameItem,
					Messages.buttonName,
					Messages.buttonNameLore
			);
			moveButtonItem = new ItemData(
					moveItem,
					Messages.buttonMove,
					Messages.buttonMoveLore
			);
			containerButtonItem = new ItemData(
					containerItem,
					Messages.buttonContainer,
					Messages.buttonContainerLore
			);
			deleteButtonItem = new ItemData(
					deleteItem,
					Messages.buttonDelete,
					Messages.buttonDeleteLore
			);
			hireButtonItem = new ItemData(
					hireItem,
					Messages.buttonHire,
					Messages.buttonHireLore
			);

			// Note: These use the same item types as the corresponding shopkeeper buttons.
			deleteVillagerButtonItem = new ItemData(
					deleteItem,
					Messages.buttonDeleteVillager,
					Messages.buttonDeleteVillagerLore
			);
			nameVillagerButtonItem = new ItemData(
					nameItem,
					Messages.buttonNameVillager,
					Messages.buttonNameVillagerLore
			);
			villagerInventoryButtonItem = new ItemData(
					containerItem,
					Messages.buttonVillagerInventory,
					Messages.buttonVillagerInventoryLore
			);

			// Shop name pattern:
			try {
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			} catch (PatternSyntaxException e) {
				Log.warning(INSTANCE.getLogPrefix()
						+ "'name-regex' is not a valid regular expression ('" + Settings.nameRegex
						+ "'). Reverting to default.");
				Settings.nameRegex = "[A-Za-z0-9 ]{3,25}";
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			}

			// Maximum shops permissions:
			PlayerShopsLimit.updateMaxShopsPermissions(invalidPermissionOption -> {
				Log.warning(INSTANCE.getLogPrefix()
						+ "Ignoring invalid entry in 'max-shops-perm-options': "
						+ invalidPermissionOption);
			});

			// Enabled living shop types:
			enabledLivingShops.clear();
			boolean foundInvalidEntityType = false;
			for (String entityTypeId : Settings.enabledLivingShops) {
				EntityType entityType = EntityUtils.parseEntityType(entityTypeId);
				if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
					// We omit the warning when we initialize the derived settings for the default
					// settings, because we might run on an older but supported MC version.
					if (initialSetup) {
						continue;
					}

					foundInvalidEntityType = true;
					if ("PIG_ZOMBIE".equals(entityTypeId)) {
						// Migration note for MC 1.16 TODO Remove this again at some point.
						Log.warning(INSTANCE.getLogPrefix()
								+ "Ignoring mob type 'PIG_ZOMBIE' in setting 'enabled-living-shops'."
								+ " This mob no longer exists since MC 1.16."
								+ " Consider replacing it with 'ZOMBIFIED_PIGLIN'.");
					} else if ("MUSHROOM_COW".equals(entityTypeId)) {
						// Migration note for MC 1.20.5 TODO Remove this again at some point.
						// Note: Not actually triggered currently, because Spigot's backwards
						// compatibility measures ensure that we can still find the mob type by its
						// old name currently.
						Log.warning(INSTANCE.getLogPrefix()
								+ "Ignoring mob type 'MUSHROOM_COW' in setting 'enabled-living-shops'."
								+ " This mob was renamed in MC 1.20.5: Consider replacing it with 'MOOSHROOM'.");
					} else if ("SNOWMAN".equals(entityTypeId)) {
						// Migration note for MC 1.20.5 TODO Remove this again at some point.
						// Note: Not actually triggered currently, because Spigot's backwards
						// compatibility measures ensure that we can still find the mob type by its
						// old name currently.
						Log.warning(INSTANCE.getLogPrefix()
								+ "Ignoring mob type 'SNOWMAN' in setting 'enabled-living-shops'."
								+ " This mob was renamed in MC 1.20.5: Consider replacing it with 'SNOW_GOLEM'.");
					} else {
						Log.warning(INSTANCE.getLogPrefix()
								+ "Invalid living entity type name in 'enabled-living-shops': "
								+ entityTypeId);
					}
				} else {
					enabledLivingShops.add(entityType);
				}
			}
			if (foundInvalidEntityType) {
				Log.warning(INSTANCE.getLogPrefix()
						+ "All existing entity type names can be found here: "
						+ "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
			}
		}

		private DerivedSettings() {
		}
	}

	// Cached values for settings which are used asynchronously.
	public static final class AsyncSettings {

		private static volatile AsyncSettings INSTANCE = new AsyncSettings();

		private static void refresh() {
			INSTANCE = new AsyncSettings();
		}

		public final boolean debug;
		public final List<? extends String> debugOptions;

		private AsyncSettings() {
			this.debug = Settings.debug;
			this.debugOptions = Collections.unmodifiableList(new ArrayList<>(Settings.debugOptions));
		}
	}

	public static AsyncSettings async() {
		return AsyncSettings.INSTANCE;
	}

	// Can also be called externally, when settings are changed externally.
	public static void onSettingsChanged() {
		// Update derived settings:
		Currencies.load();
		DerivedSettings.setup();

		// Refresh async settings cache:
		AsyncSettings.refresh();
	}

	///// PERSISTENCE

	private static ConfigData getPluginConfigData() {
		Plugin plugin = SKShopkeepersPlugin.getInstance();
		var pluginConfig = plugin.getConfig();
		// The default dot path separator can cause issues.
		// For example, since Bukkit 1.20.6, item attribute modifiers are now serialized using the
		// attribute namespaced keys as section keys, which can contain dots. When saving ItemData
		// with attribute modifiers, we filter the serialized item meta data (to produce a more
		// minimal and user-friendly output) and then apply all preserved key-value pairs to this
		// config-backed DataContainer. Setting values with a key containing dots would by default
		// result in sub-sections to be created, which fails to properly deserialize later.
		ConfigUtils.disablePathSeparator(pluginConfig);
		// This is a wrapper around the Bukkit config. Config comments are preserved by the
		// underlying Bukkit config.
		return ConfigData.of(pluginConfig);
	}

	// Returns null on success, otherwise a severe issue prevented loading the config.
	public static @Nullable ConfigLoadException loadConfig() {
		Log.info("Loading config.");
		Plugin plugin = SKShopkeepersPlugin.getInstance();

		// Save default config in case the config file does not exist:
		plugin.saveDefaultConfig();

		// Load config:
		plugin.reloadConfig();

		ConfigData configData = getPluginConfigData();

		// The default config is not empty. If the loaded config data is empty, the config file is
		// either empty (unusual) or the config failed to load. We abort the config loading with an
		// error here instead of inserting the default settings, because doing so might overwrite
		// the user's current config file contents.
		if (configData.isEmpty()) {
			return new ConfigLoadException("The config file is empty or could not be loaded."
					+ " Please check the log and config file for errors, or delete the file to have"
					+ " it reset to the default config during the next restart.");
		}

		// Load settings from config:
		boolean configChanged;
		try {
			configChanged = loadConfig(configData);
		} catch (ConfigLoadException e) {
			// Config loading failed with a severe issue:
			return e;
		}

		if (configChanged) {
			// If the config was modified (migrations, adding missing settings, ..), save it:
			saveConfig();
		}
		return null; // Config loaded successfully
	}

	// Returns true if the config data has changed and needs to be saved.
	private static boolean loadConfig(ConfigData configData) throws ConfigLoadException {
		boolean configChanged = false;

		// Perform config migrations:
		boolean migrated = ConfigMigrations.applyMigrations(configData);
		if (migrated) {
			configChanged = true;
		}

		// Insert default values for settings missing inside the config:
		boolean insertedDefaults = Settings.INSTANCE.insertMissingDefaultValues(configData);
		if (insertedDefaults) {
			configChanged = true;
		}

		// Load and validate settings:
		// Load the data version setting first so that it is available for the loading of ItemData
		// settings:
		var dataVersionSetting = Settings.INSTANCE.getSetting("data-version");
		assert dataVersionSetting != null;
		Settings.INSTANCE.loadSetting(configData, dataVersionSetting);
		ItemData.setSerializerDataVersion(dataVersion);

		Settings.INSTANCE.load(configData);

		// Update the data version:
		// Note: This is done after inserting default values, since the default value might itself
		// be outdated (we support a range of Minecraft versions).
		var dataVersionChanged = Settings.INSTANCE.updateDataVersion();
		if (dataVersionChanged) {
			configChanged = true;
		}

		onSettingsChanged();
		return configChanged;
	}

	/**
	 * Applies the values of this config to the config of the {@link ShopkeepersPlugin} and
	 * {@link Plugin#saveConfig() saves it}.
	 */
	public static void saveConfig() {
		Log.info("Saving config.");
		Plugin plugin = SKShopkeepersPlugin.getInstance();
		ConfigData configData = getPluginConfigData();
		Settings.getInstance().save(configData);
		plugin.saveConfig();
	}

	/////

	private static final Settings INSTANCE = new Settings();

	public static Settings getInstance() {
		return INSTANCE;
	}

	private Settings() {
	}

	@Override
	protected void validateSettings() {
		if (maxContainerDistance > 50) {
			Log.warning(this.getLogPrefix() + "'max-container-distance' can be at most 50.");
			maxContainerDistance = 50;
		}
		if (gravityChunkRange < 0) {
			Log.warning(this.getLogPrefix() + "'gravity-chunk-range' cannot be negative.");
			gravityChunkRange = 0;
		}
		if (entityBehaviorTickPeriod <= 0) {
			Log.warning(this.getLogPrefix() + "'entity-behavior-tick-period' has to be positive.");
			entityBehaviorTickPeriod = 1;
		}
		if (shulkerPeekHeight < 0 || shulkerPeekHeight > 1) {
			Log.warning(this.getLogPrefix() + "'shulker-peek-height' must be between 0.0 and 1.0.");
			shulkerPeekHeight = (shulkerPeekHeight < 0 ? 0 : 1);
		}
		if (slimeMaxSize < SlimeShop.MIN_SIZE || slimeMaxSize > SlimeShop.MAX_SIZE) {
			Log.warning(this.getLogPrefix() + "'slime-max-size' must be between "
					+ SlimeShop.MIN_SIZE + " and " + SlimeShop.MAX_SIZE + ".");
			slimeMaxSize = MathUtils.clamp(slimeMaxSize, SlimeShop.MIN_SIZE, SlimeShop.MAX_SIZE);
		}
		if (magmaCubeMaxSize < MagmaCubeShop.MIN_SIZE || magmaCubeMaxSize > MagmaCubeShop.MAX_SIZE) {
			Log.warning(this.getLogPrefix() + "'magma-cube-max-size' must be between "
					+ MagmaCubeShop.MIN_SIZE + " and " + MagmaCubeShop.MAX_SIZE + ".");
			magmaCubeMaxSize = MathUtils.clamp(magmaCubeMaxSize, MagmaCubeShop.MIN_SIZE, MagmaCubeShop.MAX_SIZE);
		}

		// Certain items cannot be of type AIR:
		if (shopCreationItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'shop-creation-item' can not be AIR.");
			shopCreationItem = shopCreationItem.withType(Material.VILLAGER_SPAWN_EGG);
		}
		if (hireItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'hire-item' can not be AIR.");
			hireItem = hireItem.withType(Material.EMERALD);
		}
		if (currencyItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'currency-item' can not be AIR.");
			currencyItem = currencyItem.withType(Material.EMERALD);
		}
		if (namingOfPlayerShopsViaItem) {
			if (nameItem.getType() == Material.AIR) {
				Log.warning(this.getLogPrefix() + "'name-item' can not be AIR if "
						+ "'naming-of-player-shops-via-item' is enabled!");
				nameItem = nameItem.withType(Material.NAME_TAG);
			}
		}

		// Warn about potential configuration mistakes regarding the shop creation item tag:
		if (identifyShopCreationItemByTag && !addShopCreationItemTag) {
			Log.warning(this.getLogPrefix() + "'identify-shop-creation-item-by-tag' enabled, "
					+ "but 'add-shop-creation-item-tag' is disabled! Intended?");
		}

		if (maxTradesPages < 1) {
			Log.warning(this.getLogPrefix() + "'max-trades-pages' can not be less than 1!");
			maxTradesPages = 1;
		} else if (maxTradesPages > 10) {
			Log.warning(this.getLogPrefix() + "'max-trades-pages' can not be greater than 10!");
			maxTradesPages = 10;
		}
		if (taxRate < 0) {
			Log.warning(this.getLogPrefix() + "'tax-rate' can not be less than 0!");
			taxRate = 0;
		} else if (taxRate > 100) {
			Log.warning(this.getLogPrefix() + "'tax-rate' can not be larger than 100!");
			taxRate = 100;
		}

		if (tradeLogMergeDurationTicks < 0) {
			Log.warning(this.getLogPrefix() + "'trade-log-merge-duration-ticks' cannot be negative.");
			tradeLogMergeDurationTicks = 0;
		}
		if (tradeLogNextMergeTimeoutTicks < 0) {
			Log.warning(this.getLogPrefix() + "'trade-log-next-merge-timeout-ticks' cannot be negative.");
			tradeLogNextMergeTimeoutTicks = 0;
		}
		// Note: If tradeLogNextMergeTimeoutTicks is greater than or equal to
		// tradeLogMergeDurationTicks, it has no effect. However, we do not print a warning in this
		// case to allow tradeLogMergeDurationTicks to be easily adjusted inside the config without
		// having to keep tradeLogNextMergeTimeoutTicks consistent.

		// Temporary workaround for Mohist and Magma servers.
		// See https://github.com/Shopkeepers/Shopkeepers/issues/738
		// TODO This is supposed to be removed again once the underlying issue has been fixed by
		// Mohist/Magma.
		if (!disableInventoryVerification) {
			String serverName = Bukkit.getServer().getName();
			boolean forceDisableInventoryVerification = false;
			String serverDisplayName = "";
			if (serverName.contains("Mohist")) {
				forceDisableInventoryVerification = true;
				serverDisplayName = "Mohist";
			} else if (serverName.contains("Magma")) {
				forceDisableInventoryVerification = true;
				serverDisplayName = "Magma";
			}
			if (forceDisableInventoryVerification) {
				Log.warning(this.getLogPrefix() + "Forcefully enabled " +
						"'disable-inventory-verification' to resolve a known incompatibility with "
						+ serverDisplayName + " servers.");
				disableInventoryVerification = true;
			}
		}
	}

	// Returns true if the data version has changed.
	private boolean updateDataVersion() {
		var currentDataVersion = ServerUtils.getDataVersion();
		if (dataVersion == currentDataVersion) {
			return false;
		}

		Log.info(this.getLogPrefix() + "'data-version' updated from " + dataVersion + " to "
				+ currentDataVersion + ".");
		dataVersion = currentDataVersion;
		// Note: Any item data is automatically migrated during loading. This data version check and
		// updating ensures that we save back the migrated item data whenever the data version has
		// changed.

		return true;
	}

	/**
	 * Calls an {@link UpdateItemEvent} for each item stored by the config.
	 * 
	 * @return the number of updated items
	 */
	public int updateItems() {
		int updatedItems = 0;

		for (var setting : this.getSettings()) {
			var valueType = setting.getValueType();

			if (valueType instanceof ItemDataValue) {
				if (this.updateItemDataSetting(Unsafe.cast(setting))) {
					updatedItems += 1;
				}
			} else if (valueType instanceof ListValue<?> listValueType) {
				if (listValueType.getElementValueType() instanceof ItemDataValue) {
					updatedItems += this.updateItemDataListSetting(Unsafe.cast(setting));
				}
			}

			// No event is called for Material settings: There is no item data based on which
			// plugins can reasonable decide whether or not to change the material without not also
			// affecting all items of the same type in other contexts. Plugins that want to update
			// specific Material settings need to do so explicitly.
		}

		if (updatedItems > 0) {
			this.validateSettings();
			onSettingsChanged();
			saveConfig();
		}

		return updatedItems;
	}

	private boolean updateItemDataSetting(Setting<ItemData> setting) {
		var value = setting.getValue(); // Can be null
		var newItemData = ItemUpdates.updateItemData(value);
		if (newItemData == value) return false; // Not changed
		assert newItemData != null;

		String configKey = setting.getConfigKey();
		try {
			setting.setValue(newItemData);
			Log.debug(DebugOptions.itemUpdates, this.getLogPrefix() + "Updated item for setting '"
					+ configKey + "'.");
			return true;
		} catch (ValueLoadException e) {
			Log.warning(this.getLogPrefix() + "Could not update item for setting '" + configKey
					+ "': " + e.getMessage());
			for (String extraMessage : e.getExtraMessages()) {
				Log.warning(this.getLogPrefix() + extraMessage);
			}
			return false;
		}
	}

	private int updateItemDataListSetting(Setting<List<ItemData>> setting) {
		var value = setting.getValue();
		if (value == null) return 0; // Nothing to update

		int updatedItems = 0;

		int index = -1;
		var iterator = value.listIterator();
		while (iterator.hasNext()) {
			index += 1;
			@Nullable ItemData itemData = iterator.next(); // Can be null
			var newItemData = ItemUpdates.updateItemData(itemData);
			if (newItemData == itemData) continue; // Not changed
			assert newItemData != null;

			String configKey = setting.getConfigKey();
			Log.debug(DebugOptions.itemUpdates, this.getLogPrefix() + "Updated item " + index
					+ " for setting '" + configKey + "'.");

			updatedItems += 1;
			iterator.set(newItemData);
		}

		return updatedItems;
	}
}
