package com.nisovin.shopkeepers.config;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.config.migration.ConfigMigrations;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.playershops.MaxShopsPermission;
import com.nisovin.shopkeepers.playershops.PlayerShopsLimit;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class Settings extends Config {

	/*
	 * General Settings
	 */
	public static int configVersion = 4;
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
	public static String fileEncoding = "UTF-8";
	public static boolean saveInstantly = true;

	public static boolean convertPlayerItems = false;
	public static boolean convertAllPlayerItems = true;
	public static List<ItemData> convertPlayerItemsExceptions = new ArrayList<>();

	public static ItemData placeholderItem = new ItemData(Material.NAME_TAG);

	/*
	 * Plugin Compatibility
	 */
	public static boolean bypassSpawnBlocking = true;
	public static boolean checkShopInteractionResult = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean registerWorldGuardAllowShopFlag = true;

	public static boolean enableTownyRestrictions = false;

	public static boolean disableInventoryVerification = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static ItemData shopCreationItem = new ItemData(Material.VILLAGER_SPAWN_EGG, c("&aShopkeeper"), null);
	public static boolean preventShopCreationItemRegularUsage = true;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean createPlayerShopWithCommand = false;

	public static boolean requireContainerRecentlyPlaced = true;
	public static int maxContainerDistance = 15;
	public static int maxShopsPerPlayer = -1;
	public static String maxShopsPermOptions = "5,15,25";

	public static boolean protectContainers = true;
	public static boolean preventItemMovement = true;
	public static boolean deleteShopkeeperOnBreakContainer = false;

	public static int playerShopkeeperInactiveDays = 0;

	/*
	 * Shop (Object) Types
	 */
	// Villager is the default and therefore first. The other entity types are alphabetically sorted.
	public static List<String> enabledLivingShops = CollectionUtils.addAll(
			new ArrayList<>(Arrays.asList(EntityType.VILLAGER.name())),
			CollectionUtils.sort(Arrays.asList(
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
					"BEE", // MC 1.15
					"ZOMBIFIED_PIGLIN", // MC 1.16, replaced PIG_ZOMBIE
					"PIGLIN", // MC 1.16
					"HOGLIN", // MC 1.16
					"ZOGLIN", // MC 1.16
					"STRIDER", // MC 1.16
					"PIGLIN_BRUTE", // MC 1.16.2
					"AXOLOTL", // MC 1.17
					"GOAT", // MC 1.17
					"GLOW_SQUID" // MC 1.17
			), String::compareTo)
	);

	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;

	// A tick period of 4 and higher is clearly noticeable, especially when entities are affected by gravity.
	// The total performance benefits of higher tick periods also become increasingly smaller and instead result in
	// higher performance impacts per individual behavior update.
	// The gravity updates at a tick period of 2 actually appear less smooth in my testing than at a period of 3 (maybe
	// due to some interpolation artifact by the client).
	public static int mobBehaviorTickPeriod = 3;

	public static boolean silenceLivingShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;

	public static boolean enableCitizenShops = true;
	public static EntityType defaultCitizenNpcType = EntityType.PLAYER;
	public static boolean saveCitizenNpcsInstantly = false;
	public static boolean deleteInvalidCitizenShopkeepers = false;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,25}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static int maxTradesPages = 5;

	public static ItemData previousPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData nextPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData currentPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData tradeSetupItem = new ItemData(Material.PAPER);

	public static ItemData nameItem = new ItemData(Material.NAME_TAG);

	public static boolean enableContainerOptionOnPlayerShop = true;
	public static ItemData containerItem = new ItemData(Material.CHEST);

	public static ItemData tradeNotificationsItem = new ItemData(Material.BELL);
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

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Trade Notifications
	 */
	public static boolean notifyPlayersAboutTrades = false;
	public static SoundEffect tradeNotificationSound = SoundEffect.EMPTY;

	public static boolean notifyShopOwnersAboutTrades = true;
	public static SoundEffect shopOwnerTradeNotificationSound = new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP).withVolume(0.25f);

	/*
	 * Trade Log
	 */
	public static int tradeLogMergeDurationTicks = 300; // 15 seconds
	public static int tradeLogNextMergeTimeoutTicks = 100; // 5 seconds

	public static boolean logTradesToCsv = false;

	public static boolean logItemMetadata = false;

	/*
	 * Currencies
	 */
	public static ItemData currencyItem = new ItemData(Material.EMERALD);
	public static ItemData zeroCurrencyItem = new ItemData(Material.BARRIER);
	public static ItemData highCurrencyItem = new ItemData(Material.EMERALD_BLOCK);
	public static ItemData zeroHighCurrencyItem = new ItemData(Material.BARRIER);

	// Note: This can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	///// DERIVED SETTINGS

	// Stores derived settings which get setup after loading the config.
	public static class DerivedSettings {

		public static Charset fileCharset;

		public static ItemData namingItemData;

		public static ItemData placeholderItem;

		// Button items:
		public static ItemData nameButtonItem;
		public static ItemData containerButtonItem;
		public static ItemData deleteButtonItem;
		public static ItemData hireButtonItem;

		public static ItemData deleteVillagerButtonItem;
		public static ItemData nameVillagerButtonItem;
		public static ItemData villagerInventoryButtonItem;

		public static Pattern shopNamePattern;

		// Sorted in descending order:
		public static final List<MaxShopsPermission> maxShopsPermissions = new ArrayList<>();

		public static final Set<EntityType> enabledLivingShops = new LinkedHashSet<>();

		static {
			// Initial setup of default values:
			setup();
		}

		// Gets called after setting values have changed (eg. after the config has been loaded):
		private static void setup() {
			// Charset derived from specified file encoding:
			if (StringUtils.isEmpty(fileEncoding)) {
				Log.warning(INSTANCE.getLogPrefix() + "'file-encoding' is empty. Using default 'UTF-8'.");
				fileEncoding = "UTF-8";
			}
			try {
				fileCharset = Charset.forName(fileEncoding);
			} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
				Log.warning(INSTANCE.getLogPrefix() + "Invalid or unsupported 'file-encoding' ('" + fileEncoding + "'). Using default 'UTF-8'.");
				fileEncoding = "UTF-8";
				fileCharset = StandardCharsets.UTF_8;
			}

			// Ignore (clear) the display name, which is used to specify the new shopkeeper name, but keep the lore:
			namingItemData = new ItemData(UnmodifiableItemStack.of(ItemUtils.setDisplayName(nameItem.createItemStack(), null)));
			placeholderItem = Settings.placeholderItem;

			// Button items:
			nameButtonItem = new ItemData(nameItem, Messages.buttonName, Messages.buttonNameLore);
			containerButtonItem = new ItemData(containerItem, Messages.buttonContainer, Messages.buttonContainerLore);
			deleteButtonItem = new ItemData(deleteItem, Messages.buttonDelete, Messages.buttonDeleteLore);
			hireButtonItem = new ItemData(hireItem, Messages.buttonHire, Messages.buttonHireLore);

			// Note: These use the same item types as the corresponding shopkeeper buttons.
			deleteVillagerButtonItem = new ItemData(deleteItem, Messages.buttonDeleteVillager, Messages.buttonDeleteVillagerLore);
			nameVillagerButtonItem = new ItemData(nameItem, Messages.buttonNameVillager, Messages.buttonNameVillagerLore);
			villagerInventoryButtonItem = new ItemData(containerItem, Messages.buttonVillagerInventory, Messages.buttonVillagerInventoryLore);

			// Shop name pattern:
			try {
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			} catch (PatternSyntaxException e) {
				Log.warning(INSTANCE.getLogPrefix() + "'name-regex' is not a valid regular expression ('" + Settings.nameRegex + "'). Reverting to default.");
				Settings.nameRegex = "[A-Za-z0-9 ]{3,25}";
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			}

			// Maximum shops permissions:
			PlayerShopsLimit.updateMaxShopsPermissions(invalidPermissionOption -> {
				Log.warning(INSTANCE.getLogPrefix() + "Ignoring invalid entry in 'max-shops-perm-options': " + invalidPermissionOption);
			});

			// Enabled living shop types:
			enabledLivingShops.clear();
			boolean foundInvalidEntityType = false;
			for (String entityTypeId : Settings.enabledLivingShops) {
				EntityType entityType = EntityUtils.matchEntityType(entityTypeId);
				if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
					foundInvalidEntityType = true;
					if ("PIG_ZOMBIE".equals(entityTypeId)) {
						// Migration note for MC 1.16 TODO Remove this again at some point?
						Log.warning(INSTANCE.getLogPrefix() + "Ignoring mob type 'PIG_ZOMBIE' in setting 'enabled-living-shops'. This mob no longer exist since MC 1.16. Consider replacing it with 'ZOMBIFIED_PIGLIN'.");
					} else {
						Log.warning(INSTANCE.getLogPrefix() + "Invalid living entity type name in 'enabled-living-shops': " + entityTypeId);
					}
				} else {
					enabledLivingShops.add(entityType);
				}
			}
			if (foundInvalidEntityType) {
				Log.warning(INSTANCE.getLogPrefix() + "All existing entity type names can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
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
		public final List<String> debugOptions;
		public final Charset fileCharset;

		private AsyncSettings() {
			this.debug = Settings.debug;
			this.debugOptions = Collections.unmodifiableList(new ArrayList<>(Settings.debugOptions));
			this.fileCharset = DerivedSettings.fileCharset;
		}
	}

	public static AsyncSettings async() {
		return AsyncSettings.INSTANCE;
	}

	// Can also be called externally, when settings are changed externally.
	public static void onSettingsChanged() {
		// Update derived settings:
		DerivedSettings.setup();

		// Refresh async settings cache:
		AsyncSettings.refresh();
	}

	// ITEMS

	// Shop creation item:
	public static ItemStack createShopCreationItem() {
		return shopCreationItem.createItemStack();
	}

	public static boolean isShopCreationItem(ItemStack item) {
		return shopCreationItem.matches(item);
	}

	// Naming item:
	public static boolean isNamingItem(ItemStack item) {
		return DerivedSettings.namingItemData.matches(item);
	}

	public static ItemStack createNameButtonItem() {
		return DerivedSettings.nameButtonItem.createItemStack();
	}

	// Container button:
	public static ItemStack createContainerButtonItem() {
		return DerivedSettings.containerButtonItem.createItemStack();
	}

	// Delete button:
	public static ItemStack createDeleteButtonItem() {
		return DerivedSettings.deleteButtonItem.createItemStack();
	}

	// Hire item:
	public static ItemStack createHireButtonItem() {
		return DerivedSettings.hireButtonItem.createItemStack();
	}

	public static boolean isHireItem(ItemStack item) {
		return hireItem.matches(item);
	}

	// CURRENCY

	private static final Predicate<@ReadOnly ItemStack> ANY_CURRENCY_ITEMS = Settings::isAnyCurrencyItem;

	public static Predicate<@ReadOnly ItemStack> anyCurrencyItems() {
		return ANY_CURRENCY_ITEMS;
	}

	public static boolean isAnyCurrencyItem(@ReadOnly ItemStack itemStack) {
		return isCurrencyItem(itemStack) || isHighCurrencyItem(itemStack);
	}

	public static boolean isAnyCurrencyItem(UnmodifiableItemStack itemStack) {
		return isAnyCurrencyItem(ItemUtils.asItemStackOrNull(itemStack));
	}

	// Currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return currencyItem.createItemStack(amount);
	}

	public static boolean isCurrencyItem(@ReadOnly ItemStack item) {
		return currencyItem.matches(item);
	}

	public static boolean isCurrencyItem(UnmodifiableItemStack item) {
		return isCurrencyItem(ItemUtils.asItemStackOrNull(item));
	}

	// High currency item:
	public static boolean isHighCurrencyEnabled() {
		return (highCurrencyValue > 0 && highCurrencyItem.getType() != Material.AIR);
	}

	public static ItemStack createHighCurrencyItem(int amount) {
		if (!isHighCurrencyEnabled()) return null;
		return highCurrencyItem.createItemStack(amount);
	}

	public static boolean isHighCurrencyItem(@ReadOnly ItemStack item) {
		if (!isHighCurrencyEnabled()) return false;
		return highCurrencyItem.matches(item);
	}

	public static boolean isHighCurrencyItem(UnmodifiableItemStack item) {
		return isHighCurrencyItem(ItemUtils.asItemStackOrNull(item));
	}

	// Zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (zeroCurrencyItem.getType() == Material.AIR) return null;
		return zeroCurrencyItem.createItemStack();
	}

	public static boolean isZeroCurrencyItem(@ReadOnly ItemStack item) {
		if (zeroCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroCurrencyItem.matches(item);
	}

	// Zero high currency item:
	public static ItemStack createZeroHighCurrencyItem() {
		if (zeroHighCurrencyItem.getType() == Material.AIR) return null;
		return zeroHighCurrencyItem.createItemStack();
	}

	public static boolean isZeroHighCurrencyItem(@ReadOnly ItemStack item) {
		if (zeroHighCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroHighCurrencyItem.matches(item);
	}

	///// LOADING

	// Returns null on success, otherwise a severe issue prevented loading the config.
	public static ConfigLoadException loadConfig(Plugin plugin) {
		Log.info("Loading config.");

		// Save default config in case the config file does not exist:
		plugin.saveDefaultConfig();

		// Load config:
		plugin.reloadConfig();
		Configuration config = plugin.getConfig();

		// Load settings from config:
		boolean configChanged = false;
		try {
			configChanged = loadConfig(config);
		} catch (ConfigLoadException e) {
			// Config loading failed with a severe issue:
			return e;
		}

		if (configChanged) {
			// If the config was modified (migrations, adding missing settings, ..), save it:
			// TODO Persist comments somehow.
			plugin.saveConfig();
		}
		return null; // Config loaded successfully
	}

	// Returns true, if the config has changed and needs to be saved.
	private static boolean loadConfig(Configuration config) throws ConfigLoadException {
		boolean configChanged = false;

		// Perform config migrations:
		boolean migrated = ConfigMigrations.applyMigrations(config);
		if (migrated) {
			configChanged = true;
		}

		// Insert default values for settings missing inside the config:
		boolean insertedDefaults = Settings.INSTANCE.insertMissingDefaultValues(config);
		if (insertedDefaults) {
			configChanged = true;
		}

		// Load and validate settings:
		Settings.INSTANCE.load(config);

		onSettingsChanged();
		return configChanged;
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
		if (mobBehaviorTickPeriod <= 0) {
			Log.warning(this.getLogPrefix() + "'mob-behavior-tick-period' has to be positive.");
			mobBehaviorTickPeriod = 1;
		}
		// Certain items cannot be of type AIR:
		if (placeholderItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'placeholder-item' can not be AIR.");
			placeholderItem = placeholderItem.withType(Material.NAME_TAG);
		}
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
				Log.warning(this.getLogPrefix() + "'name-item' can not be AIR if naming-of-player-shops-via-item is enabled!");
				nameItem = nameItem.withType(Material.NAME_TAG);
			}
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
		// Note: If tradeLogNextMergeTimeoutTicks is greater than or equal to tradeLogMergeDurationTicks, it has no
		// effect. However, we do not print a warning in this case to allow tradeLogMergeDurationTicks to be easily
		// adjusted inside the config without having to keep tradeLogNextMergeTimeoutTicks consistent.

		// Temporary workaround for Mohist servers. See https://github.com/Shopkeepers/Shopkeepers/issues/738
		// TODO This is supposed to be removed again once the underlying issue has been fixed by Mohist.
		if (!disableInventoryVerification && Bukkit.getServer().getName().contains("Mohist")) {
			Log.warning(this.getLogPrefix() + "Forcefully enabled 'disable-inventory-verification' to resolve a known incompatibility with Mohist servers.");
			disableInventoryVerification = true;
		}
	}
}
