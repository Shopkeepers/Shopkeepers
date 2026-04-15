package com.nisovin.shopkeepers;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.bukkit.WrappedExecutor;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.util.FoliaLibOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeepersStartupEvent;
import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.commands.Commands;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.compat.MC_1_21_11;
import com.nisovin.shopkeepers.compat.MC_1_21_9;
import com.nisovin.shopkeepers.compat.ServerAssumptionsTest;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.container.protection.RemoveShopOnContainerBreak;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.debug.events.EventDebugger;
import com.nisovin.shopkeepers.debug.trades.TradingCountListener;
import com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency;
import com.nisovin.shopkeepers.input.chat.ChatInput;
import com.nisovin.shopkeepers.input.interaction.InteractionInput;
import com.nisovin.shopkeepers.internals.SKApiInternals;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.metrics.PluginMetrics;
import com.nisovin.shopkeepers.moving.ShopkeeperMoving;
import com.nisovin.shopkeepers.naming.ShopkeeperNaming;
import com.nisovin.shopkeepers.playershops.PlayerShops;
import com.nisovin.shopkeepers.shopcreation.ShopkeeperCreation;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.SKShopTypesRegistry;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SKShopObjectTypesRegistry;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShops;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShops;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShops;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.spigot.SpigotFeatures;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.tradelog.TradeLoggers;
import com.nisovin.shopkeepers.tradelog.history.TradingHistoryProvider;
import com.nisovin.shopkeepers.tradenotifications.TradeNotifications;
import com.nisovin.shopkeepers.trading.commandtrading.CommandTrading;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.SKUIRegistry;
import com.nisovin.shopkeepers.ui.SKUISystem;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;
import com.nisovin.shopkeepers.villagers.RegularVillagers;
import com.nisovin.shopkeepers.world.ForcingEntitySpawner;
import com.nisovin.shopkeepers.world.ForcingEntityTeleporter;

public class SKShopkeepersPlugin extends JavaPlugin implements InternalShopkeepersPlugin {

	private static final FoliaLibOptions FOLIA_LIB_OPTIONS = new FoliaLibOptions();

	private static final Set<? extends String> SKIP_PRELOADING_CLASSES = Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList(
					// Skip classes that interact with optional dependencies:
					"com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency$Internal",
					"com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils$Internal",
					"com.nisovin.shopkeepers.shopobjects.citizens.CitizensShopkeeperTrait",
					"com.nisovin.shopkeepers.spigot.text.SpigotText$Internal"
			))
	);

	private static final int ASYNC_TASKS_TIMEOUT_SECONDS = 10;

	private static @Nullable SKShopkeepersPlugin plugin;

	public static boolean isPluginEnabled() {
		return (plugin != null);
	}

	public static SKShopkeepersPlugin getInstance() {
		return Validate.State.notNull(plugin, "Plugin is not enabled!");
	}

	// Utilities:
	private final FoliaLib foliaLib = new FoliaLib(Unsafe.initialized(this), FOLIA_LIB_OPTIONS);
	private final WrappedExecutor syncExecutor = SchedulerUtils.createSyncExecutor();
	private final Executor asyncExecutor = SchedulerUtils.createAsyncExecutor();

	private final ForcingEntitySpawner forcingEntitySpawner = new ForcingEntitySpawner(Unsafe.initialized(this));
	private final ForcingEntityTeleporter forcingEntityTeleporter = new ForcingEntityTeleporter(Unsafe.initialized(this));

	private final ApiInternals apiInternals = new SKApiInternals();

	// Shop types and shop object types registry:
	private final SKShopTypesRegistry shopTypesRegistry = new SKShopTypesRegistry();
	private final SKShopObjectTypesRegistry shopObjectTypesRegistry = new SKShopObjectTypesRegistry();

	// UI system:
	private final SKUISystem uiSystem = new SKUISystem(Unsafe.initialized(this));
	private final SKUIRegistry uiRegistry = new SKUIRegistry();
	private final SKDefaultUITypes defaultUITypes = new SKDefaultUITypes();

	// Shopkeeper registry:
	private final SKShopkeeperRegistry shopkeeperRegistry = new SKShopkeeperRegistry(
			Unsafe.initialized(this)
	);

	// Shopkeeper storage:
	private final SKShopkeeperStorage shopkeeperStorage = new SKShopkeeperStorage(
			Unsafe.initialized(this)
	);

	private final Commands commands = new Commands(Unsafe.initialized(this));
	private final ChatInput chatInput = new ChatInput(Unsafe.initialized(this));
	private final InteractionInput interactionInput = new InteractionInput(Unsafe.initialized(this));

	private final CommandTrading commandTrading = new CommandTrading(Unsafe.initialized(this));
	private final TradeLoggers tradeLoggers = new TradeLoggers(Unsafe.initialized(this));
	private final TradeNotifications tradeNotifications = new TradeNotifications(
			Unsafe.initialized(this)
	);
	private final EventDebugger eventDebugger = new EventDebugger(Unsafe.initialized(this));

	private final PlayerShops playerShops = new PlayerShops(Unsafe.initialized(this));

	private final ProtectedContainers protectedContainers = new ProtectedContainers(
			Unsafe.initialized(this)
	);
	private final ShopkeeperCreation shopkeeperCreation = new ShopkeeperCreation(
			Unsafe.initialized(this),
			shopkeeperRegistry,
			protectedContainers
	);
	private final ShopkeeperNaming shopkeeperNaming = new ShopkeeperNaming(chatInput);
	private final ShopkeeperMoving shopkeeperMoving = new ShopkeeperMoving(
			interactionInput,
			shopkeeperCreation.getShopkeeperPlacement()
	);
	private final RemoveShopOnContainerBreak removeShopOnContainerBreak = new RemoveShopOnContainerBreak(
			Unsafe.initialized(this),
			protectedContainers
	);

	private final BaseBlockShops blockShops = new BaseBlockShops(Unsafe.initialized(this));
	private final BaseEntityShops entityShops = new BaseEntityShops(Unsafe.initialized(this));
	private final LivingShops livingShops = new LivingShops(Unsafe.initialized(this), entityShops);
	private final CitizensShops citizensShops = new CitizensShops(Unsafe.initialized(this));

	private final RegularVillagers regularVillagers = new RegularVillagers(Unsafe.initialized(this));

	// Default shop and shop object types:
	private final SKDefaultShopTypes defaultShopTypes = new SKDefaultShopTypes();
	private final SKDefaultShopObjectTypes defaultShopObjectTypes = new SKDefaultShopObjectTypes(
			Unsafe.initialized(this),
			blockShops,
			entityShops
	);

	private final PluginMetrics pluginMetrics = new PluginMetrics(Unsafe.initialized(this));

	private boolean outdatedServer = false;
	private boolean incompatibleServer = false;
	private @Nullable ConfigLoadException configLoadError = null; // Null on success

	private void loadAllPluginClasses() {
		File pluginJarFile = this.getFile();
		long startNanos = System.nanoTime();
		boolean success = ClassUtils.loadAllClassesFromJar(pluginJarFile, className -> {
			// Skip version dependent classes:
			if (className.startsWith("com.nisovin.shopkeepers.compat.")) {
				return false;
			}
			if (SKIP_PRELOADING_CLASSES.contains(className)) {
				return false;
			}
			return true;
		}, this.getLogger());
		if (success) {
			long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
			Log.info("Loaded all plugin classes (" + durationMillis + " ms).");
		}
	}

	// Returns true if server is outdated.
	private boolean isOutdatedServerVersion() {
		// Validate that this server is running a minimum required version:
		// Changed in late Bukkit 1.20.6 from 'false' -> 'true':
		// On the CraftBukkit side, there have been further bug fixes related to item serialization
		// after that.
		return !EntityType.ITEM.isSpawnable();
		/*try {
			// Changed during MC 1.21 from enum to interface:
			Class<?> clazz = Class.forName("org.bukkit.entity.Villager$Type");
			return clazz.isEnum();
		} catch (ClassNotFoundException e) {
			return true;
		}*/
	}

	private void registerDefaults() {
		Log.info("Registering defaults.");
		defaultShopObjectTypes.onRegisterDefaults();
		uiRegistry.registerAll(defaultUITypes.getAllUITypes());
		shopTypesRegistry.registerAll(defaultShopTypes.getAll());
		shopObjectTypesRegistry.registerAll(defaultShopObjectTypes.getAll());
	}

	public SKShopkeepersPlugin() {
		super();
	}

	@Override
	public void onLoad() {
		Log.setLogger(this.getLogger()); // Set up logger early
		// Setting plugin reference early, so it is also available for any code running here:
		plugin = this;
		InternalShopkeepersAPI.enable(this);

		// Validate that this server is running a minimum required version:
		this.outdatedServer = this.isOutdatedServerVersion();
		if (this.outdatedServer) {
			return;
		}

		// Try to load the compat module: Returns false if neither a compatible compat provider nor
		// the fallback provider could be loaded.
		this.incompatibleServer = !Compat.load(this);
		if (this.incompatibleServer) {
			return;
		}

		// Load config:
		// Note: The config loading can already depend on Compat functionality (e.g. for item
		// loading), so Compat must be initialized first.
		this.configLoadError = Settings.loadConfig();
		if (this.configLoadError != null) {
			return;
		}

		// Load language file:
		Messages.loadLanguageFile();

		// WorldGuard only allows registering flags before it gets enabled.
		// Note: Changing the config setting has no effect until the next server restart or server
		// reload.
		if (Settings.registerWorldGuardAllowShopFlag) {
			WorldGuardDependency.registerAllowShopFlag();
		}

		// Register defaults:
		this.registerDefaults();

		// Load all plugin classes up front to ensure that we don't run into missing classes
		// (usually during shutdown) when the plugin jar gets dynamically replaced during runtime
		// (e.g. for hot reloads):
		// This must be done after the compatibility provider has been initialized, because some
		// class initialization logic may already depend on it (e.g. to retrieve Bukkit registries).
		this.loadAllPluginClasses();
	}

	@Override
	public void onEnable() {
		assert Log.getLogger() != null; // Log should already have been set up
		// Plugin instance and API might already have been set during onLoad:
		boolean alreadySetUp = true;
		if (plugin == null) {
			alreadySetUp = false;
			plugin = this;
			InternalShopkeepersAPI.enable(this);
		}

		// Validate that this server is running a minimum required version:
		if (this.outdatedServer) {
			Log.severe("Outdated server version (" + Bukkit.getVersion()
					+ "): Shopkeepers cannot be enabled. Please update your server!");
			this.setEnabled(false); // Also calls onDisable
			return;
		}

		// Check if the server version is incompatible:
		if (this.incompatibleServer) {
			Log.severe("Incompatible server version: Shopkeepers cannot be enabled.");
			this.setEnabled(false); // Also calls onDisable
			return;
		}

		// Load config (if not already loaded during onLoad):
		if (!alreadySetUp) {
			this.configLoadError = Settings.loadConfig();
		} else {
			Log.debug("Config already loaded.");
		}
		if (this.configLoadError != null) {
			Log.severe("Could not load the config!", configLoadError);
			this.setEnabled(false); // Also calls onDisable
			return;
		}

		// Load language file (if not already loaded during onLoad):
		if (!alreadySetUp) {
			Messages.loadLanguageFile();
		} else {
			Log.debug("Language file already loaded.");
		}

		// Check for and initialize version dependent utilities:
		// Example: MC_1_20_6.init();
		MC_1_21_9.init();
		MC_1_21_11.init();

		// Compat module:
		Compat.getProvider().onEnable();

		// Inform about Spigot exclusive features:
		if (SpigotFeatures.isSpigotAvailable()) {
			Log.debug("Spigot-based server found: Enabling Spigot exclusive features.");
		} else {
			Log.info("No Spigot-based server found: Disabling Spigot exclusive features!");
		}

		// Test server assumptions:
		if (!ServerAssumptionsTest.run()) {
			if (Settings.ignoreFailedServerAssumptionTests) {
				Log.severe("Server incompatibility detected! But we continue to enable the plugin "
						+ "anyway, because setting 'ignore-failed-server-assumption-tests' is "
						+ "enabled. Runnning the plugin in this mode is unsupported!");
			} else {
				Log.severe("Server incompatibility detected! Disabling the plugin!");
				this.setEnabled(false); // Also calls onDisable
				return;
			}
		}

		// Register defaults (if not already set up during onLoad):
		if (!alreadySetUp) {
			this.registerDefaults();
		} else {
			Log.debug("Defaults already registered.");
		}

		// Call startup event so that other plugins can make their registrations:
		// TODO This event doesn't make much sense, because dependent plugins are enabled after us,
		// so they were not yet able to register their event handlers.
		// An option could be to enable the Shopkeepers plugin 1 tick after all other plugins have
		// been enabled. But then any performance intensive startup tasks (loading shops, ..) would
		// potentially be interpreted as lag by the server.
		// Another option is for these plugins to perform their setup during onLoad (similar to how
		// we register default shop types, etc., during onLoad).
		Bukkit.getPluginManager().callEvent(new ShopkeepersStartupEvent());

		forcingEntitySpawner.onEnable();
		forcingEntityTeleporter.onEnable();

		// Enable UI system:
		uiSystem.onEnable();

		// Enable container protection:
		protectedContainers.enable();
		removeShopOnContainerBreak.onEnable();

		// Register events:
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PlayerJoinQuitListener(this), this);
		new TradingCountListener(this).onEnable();

		// DEFAULT SHOP OBJECT TYPES

		// Enable block shops:
		// Note: This has to be enabled before the shop creation listener, so that interactions with
		// block shops take precedence over interactions with the shop creation item.
		blockShops.onEnable();

		// Enable living entity shops:
		entityShops.onEnable();
		livingShops.onEnable();

		// Enable citizens shops:
		citizensShops.onEnable();

		// -----

		// Features related to regular villagers:
		regularVillagers.onEnable();

		// Enable commands:
		commands.onEnable();

		// Input:
		chatInput.onEnable();
		interactionInput.onEnable();

		// Enable shopkeeper creation:
		shopkeeperCreation.onEnable();

		// Enable shopkeeper naming and moving:
		shopkeeperNaming.onEnable();
		shopkeeperMoving.onEnable();

		// Enable shopkeeper storage:
		shopkeeperStorage.onEnable();

		// Enable shopkeeper registry:
		shopkeeperRegistry.onEnable();

		// Debug log the registered shopkeeper data migrations:
		if (Debug.isDebugging()) {
			ShopkeeperDataMigrator.logRegisteredMigrations();
		}

		// Load shopkeepers from saved data:
		boolean loadingSuccessful = shopkeeperStorage.reload();
		if (!loadingSuccessful) {
			// Detected an issue during loading.
			// Disabling the plugin without saving, to prevent loss of shopkeeper data:
			Log.severe("Detected an issue during the loading of the saved shopkeepers data! "
					+ "Disabling the plugin!");
			shopkeeperStorage.disableSaving();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// Activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		shopkeeperRegistry.getChunkActivator().activateShopkeepersInAllWorlds();

		// Player shops:
		playerShops.onEnable();

		commandTrading.onEnable();

		// Trade loggers:
		tradeLoggers.onEnable();

		// Trade notifications:
		tradeNotifications.onEnable();

		// Save all updated shopkeeper data (e.g. after data migrations):
		shopkeeperStorage.saveIfDirty();

		// Plugin metrics:
		pluginMetrics.onEnable();

		// Event debugger:
		eventDebugger.onEnable();
	}

	@Override
	public void onDisable() {
		if (!foliaLib.isFolia()) {
			// Wait for async tasks to complete:
			SchedulerUtils.awaitAsyncTasksCompletion(
					this,
					ASYNC_TASKS_TIMEOUT_SECONDS,
					this.getLogger()
			);
		}

		// Disable UI system:
		uiSystem.onDisable();

		// Deactivate (despawn) all shopkeepers (prior to saving shopkeepers data and before
		// unloading all shopkeepers):
		shopkeeperRegistry.getChunkActivator().deactivateShopkeepersInAllWorlds();

		// Disable block shops:
		blockShops.onDisable();

		// Disable living entity shops:
		livingShops.onDisable();
		entityShops.onDisable();

		// Disable citizens shops:
		citizensShops.onDisable();

		// Disable protected containers:
		protectedContainers.disable();
		removeShopOnContainerBreak.onDisable();

		// Disable shopkeeper registry: Unloads all shopkeepers.
		shopkeeperRegistry.onDisable();

		// Shutdown shopkeeper storage (saves shopkeepers):
		shopkeeperStorage.onDisable();

		shopTypesRegistry.clearAllSelections();
		shopObjectTypesRegistry.clearAllSelections();

		// Disable commands:
		commands.onDisable();

		// Input:
		chatInput.onDisable();
		interactionInput.onDisable();

		// Regular villagers:
		regularVillagers.onDisable();

		shopkeeperNaming.onDisable();
		shopkeeperMoving.onDisable();

		shopkeeperCreation.onDisable();

		// Player shops:
		playerShops.onDisable();

		commandTrading.onDisable();

		// Trade loggers:
		tradeLoggers.onDisable();

		// Trade notifications:
		tradeNotifications.onDisable();

		// Clear all types of registers:
		shopTypesRegistry.clearAll();
		shopObjectTypesRegistry.clearAll();
		uiRegistry.clearAll();

		forcingEntityTeleporter.onDisable();
		forcingEntitySpawner.onDisable();

		// Plugin metrics:
		pluginMetrics.onDisable();

		// Event debugger:
		eventDebugger.onDisable();
		// Compat module:
		if (Compat.hasProvider()) {
			Compat.getProvider().onDisable();
		}

		HandlerList.unregisterAll(this);
		foliaLib.getScheduler().cancelAllTasks();

		InternalShopkeepersAPI.disable();

		if (foliaLib.isFolia()) {
			// Wait for async tasks to complete:
			SchedulerUtils.awaitAsyncTasksCompletion(
					this,
					ASYNC_TASKS_TIMEOUT_SECONDS,
					this.getLogger()
			);
		}

		plugin = null;
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	// PLAYER JOINING AND QUITTING

	void onPlayerJoin(Player player) {
	}

	void onPlayerQuit(Player player) {
		// Player cleanup:
		shopTypesRegistry.clearSelection(player);
		shopObjectTypesRegistry.clearSelection(player);

		shopkeeperCreation.onPlayerQuit(player);
		commands.onPlayerQuit(player);
	}

	@Override
	public ApiInternals getApiInternals() {
		return apiInternals;
	}

	// UTILITIES


	public FoliaLib getFoliaLib() {
		return foliaLib;
	}

	public WrappedExecutor getSyncExecutor() {
		return syncExecutor;
	}

	public Executor getAsyncExecutor() {
		return asyncExecutor;
	}

	public ForcingEntitySpawner getForcingEntitySpawner() {
		return forcingEntitySpawner;
	}

	public ForcingEntityTeleporter getForcingEntityTeleporter() {
		return forcingEntityTeleporter;
	}

	// SHOPKEEPER REGISTRY

	@Override
	public SKShopkeeperRegistry getShopkeeperRegistry() {
		return shopkeeperRegistry;
	}

	// SHOPKEEPER STORAGE

	@Override
	public SKShopkeeperStorage getShopkeeperStorage() {
		return shopkeeperStorage;
	}

	// ITEM UPDATES

	@Override
	public int updateItems() {
		Log.debug(DebugOptions.itemUpdates, "Updating all items.");

		// Note: Not safe to be called from inside inventory events!
		uiRegistry.abortUISessions();

		int updatedItems = Settings.getInstance().updateItems();

		int shopkeeperUpdatedItems = 0;
		for (AbstractShopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			shopkeeperUpdatedItems += shopkeeper.updateItems();
		}
		if (shopkeeperUpdatedItems > 0) {
			updatedItems += shopkeeperUpdatedItems;
			shopkeeperStorage.save();
		}

		return updatedItems;
	}

	// COMMANDS

	public Commands getCommands() {
		return commands;
	}

	// INPUT

	public ChatInput getChatInput() {
		return chatInput;
	}

	public InteractionInput getInteractionInput() {
		return interactionInput;
	}

	// UI

	@Override
	public SKUIRegistry getUIRegistry() {
		return uiRegistry;
	}

	@Override
	public SKDefaultUITypes getDefaultUITypes() {
		return defaultUITypes;
	}

	// PROTECTED CONTAINERS

	public ProtectedContainers getProtectedContainers() {
		return protectedContainers;
	}

	// SHOPKEEPER REMOVAL ON CONTAINER BREAKING

	public RemoveShopOnContainerBreak getRemoveShopOnContainerBreak() {
		return removeShopOnContainerBreak;
	}

	// ENTITY SHOPS

	public BaseEntityShops getEntityShops() {
		return entityShops;
	}

	public LivingShops getLivingShops() {
		return livingShops;
	}

	// BLOCK SHOPS

	public BaseBlockShops getBlockShops() {
		return blockShops;
	}

	// CITIZENS SHOPS

	public CitizensShops getCitizensShops() {
		return citizensShops;
	}

	// SHOP TYPES

	@Override
	public SKShopTypesRegistry getShopTypeRegistry() {
		return shopTypesRegistry;
	}

	@Override
	public SKDefaultShopTypes getDefaultShopTypes() {
		return defaultShopTypes;
	}

	// SHOP OBJECT TYPES

	@Override
	public SKShopObjectTypesRegistry getShopObjectTypeRegistry() {
		return shopObjectTypesRegistry;
	}

	@Override
	public SKDefaultShopObjectTypes getDefaultShopObjectTypes() {
		return defaultShopObjectTypes;
	}

	// SHOPKEEPER NAMING

	public ShopkeeperNaming getShopkeeperNaming() {
		return shopkeeperNaming;
	}

	// SHOPKEEPER MOVING

	public ShopkeeperMoving getShopkeeperMoving() {
		return shopkeeperMoving;
	}

	// REGULAR VILLAGERS

	public RegularVillagers getRegularVillagers() {
		return regularVillagers;
	}

	// SHOPKEEPER CREATION

	public ShopkeeperCreation getShopkeeperCreation() {
		return shopkeeperCreation;
	}

	@Override
	public boolean hasCreatePermission(Player player) {
		Validate.notNull(player, "player is null");
		assert player != null;
		return (shopTypesRegistry.getSelection(player) != null)
				&& (shopObjectTypesRegistry.getSelection(player) != null);
	}

	@Override
	public @Nullable AbstractShopkeeper handleShopkeeperCreation(
			ShopCreationData shopCreationData
	) {
		Validate.notNull(shopCreationData, "shopCreationData is null");
		ShopType<?> rawShopType = shopCreationData.getShopType();
		Validate.isTrue(rawShopType instanceof AbstractShopType,
				"ShopType of shopCreationData is not of type AbstractShopType, but: "
						+ rawShopType.getClass().getName());
		AbstractShopType<?> shopType = (AbstractShopType<?>) rawShopType;
		// Forward to shop type:
		return shopType.handleShopkeeperCreation(shopCreationData);
	}

	// PLAYER SHOPS

	public PlayerShops getPlayerShops() {
		return playerShops;
	}

	// TRADE NOTIFICATIONS

	public TradeNotifications getTradeNotifications() {
		return tradeNotifications;
	}

	// TRADING HISTORY

	public @Nullable TradingHistoryProvider getTradingHistoryProvider() {
		return tradeLoggers.getTradingHistoryProvider();
	}


	static {
		FOLIA_LIB_OPTIONS.disableNotifications();
	}
}
