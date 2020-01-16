package com.nisovin.shopkeepers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.events.ShopkeepersStartupEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.chestprotection.ProtectedChests;
import com.nisovin.shopkeepers.commands.Commands;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.config.ConfigLoadException;
import com.nisovin.shopkeepers.history.TradingHistory;
import com.nisovin.shopkeepers.metrics.CitizensChart;
import com.nisovin.shopkeepers.metrics.FeaturesChart;
import com.nisovin.shopkeepers.metrics.GringottsChart;
import com.nisovin.shopkeepers.metrics.PlayerShopsChart;
import com.nisovin.shopkeepers.metrics.ShopkeepersCountChart;
import com.nisovin.shopkeepers.metrics.TownyChart;
import com.nisovin.shopkeepers.metrics.VaultEconomyChart;
import com.nisovin.shopkeepers.metrics.WorldGuardChart;
import com.nisovin.shopkeepers.metrics.WorldsChart;
import com.nisovin.shopkeepers.naming.ShopkeeperNaming;
import com.nisovin.shopkeepers.player.PlayerIdCache;
import com.nisovin.shopkeepers.player.profile.PlayerProfiles;
import com.nisovin.shopkeepers.player.profile.SKPlayerProfiles;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;
import com.nisovin.shopkeepers.shopcreation.ShopkeeperCreation;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.SKShopTypesRegistry;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.offers.SKBookOffer;
import com.nisovin.shopkeepers.shopkeeper.offers.SKPriceOffer;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradingOffer;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SKShopObjectTypesRegistry;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShops;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.sign.SignShops;
import com.nisovin.shopkeepers.spigot.SpigotFeatures;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.storage.SKStorage;
import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.StorageTypes;
import com.nisovin.shopkeepers.ui.SKUIRegistry;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.util.DebugListener;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.TradingCountListener;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.villagers.BlockVillagerSpawnListener;
import com.nisovin.shopkeepers.villagers.BlockZombieVillagerCuringListener;
import com.nisovin.shopkeepers.villagers.VillagerInteractionListener;

public class SKShopkeepersPlugin extends JavaPlugin implements ShopkeepersPlugin {

	private static final int ASYNC_TASKS_TIMEOUT_SECONDS = 60;

	private static SKShopkeepersPlugin plugin;

	public static SKShopkeepersPlugin getInstance() {
		return plugin;
	}

	// utilities:
	private final Executor syncExecutor = SchedulerUtils.createSyncExecutor(this);
	private final Executor asyncExecutor = SchedulerUtils.createAsyncExecutor(this);

	// stores the uuids and names of all online players, as well as all known shop owners among the loaded shopkeepers:
	private final PlayerIdCache playerIdCache = new PlayerIdCache();

	// database storage:
	private final StorageTypes storageTypes = new StorageTypes();

	private SKStorage storage = null;
	private final SKPlayerProfiles playerProfiles = new SKPlayerProfiles(this);
	private final TradingHistory tradingHistory = new TradingHistory(this);

	// shop types and shop object types registry:
	private final SKShopTypesRegistry shopTypesRegistry = new SKShopTypesRegistry();
	private final SKShopObjectTypesRegistry shopObjectTypesRegistry = new SKShopObjectTypesRegistry();

	// default shop and shop object types:
	private final SKDefaultShopTypes defaultShopTypes = new SKDefaultShopTypes();
	private final SKDefaultShopObjectTypes defaultShopObjectTypes = new SKDefaultShopObjectTypes(this);

	// ui registry:
	private final SKUIRegistry uiRegistry = new SKUIRegistry(this);
	private final SKDefaultUITypes defaultUITypes = new SKDefaultUITypes();

	// shopkeeper registry:
	private final SKShopkeeperRegistry shopkeeperRegistry = new SKShopkeeperRegistry(this);

	// shopkeeper storage:
	private final SKShopkeeperStorage shopkeeperStorage = new SKShopkeeperStorage(this);

	private final Commands commands = new Commands(this);
	private final ShopkeeperNaming shopkeeperNaming = new ShopkeeperNaming(this);
	private final ShopkeeperCreation shopkeeperCreation = new ShopkeeperCreation(this);

	private final ProtectedChests protectedChests = new ProtectedChests(this);
	private final LivingShops livingShops = new LivingShops(this);
	private final SignShops signShops = new SignShops(this);
	private final CitizensShops citizensShops = new CitizensShops(this);

	private boolean outdatedServer = false;
	private boolean incompatibleServer = false;
	private ConfigLoadException configLoadError = null; // null on success

	private void loadRequiredClasses() {
		// making sure that certain classes, that are needed during shutdown, are loaded:
		// this helps for hot reloads (when the plugin gets disabled, but the original jar got replaced and is therefore
		// no longer available)
		// TODO pre-load all classes?
		try {
			Class.forName(SchedulerUtils.class.getName());
			Class.forName(ShopkeeperRemoveEvent.class.getName());
			Class.forName(ShopkeeperRemoveEvent.Cause.class.getName());
			Class.forName("com.nisovin.shopkeepers.storage.SKShopkeeperStorage$SaveResult$State");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// returns true if server is outdated
	private boolean isOutdatedServerVersion() {
		// validate that this server is running a minimum required version:
		// TODO add proper version parsing
		/*String cbVersion = Utils.getServerCBVersion(); // eg. 1_13_R2
		String bukkitVersion = Bukkit.getBukkitVersion(); // eg. 1.13.1-R0.1-SNAPSHOT*/
		try {
			// this has been added with the recent changes to PlayerBedEnterEvent: TODO outdated
			Class.forName("org.bukkit.event.player.PlayerBedEnterEvent$BedEnterResult");
			return false;
		} catch (ClassNotFoundException e) {
			return true;
		}
	}

	// returns false if no compatible NMS version, nor the fallback handler could be setup
	private boolean setupNMS() {
		NMSManager.load(this);
		return (NMSManager.getProvider() != null);
	}

	// returns null on success, otherwise a severe issue prevented loading the config
	private ConfigLoadException loadConfig() {
		Log.info("Loading config.");

		// save default config in case the config file doesn't exist
		this.saveDefaultConfig();

		// load config:
		this.reloadConfig();
		Configuration config = this.getConfig();

		// load settings from config:
		boolean configChanged;
		try {
			configChanged = Settings.loadConfiguration(config);
		} catch (ConfigLoadException e) {
			// config loading failed with a severe issue:
			return e;
		}

		if (configChanged) {
			// if the config was modified (migrations, adding missing settings, ..), save it:
			// TODO persist comments somehow
			this.saveConfig();
		}
		return null; // config loaded successfully
	}

	private void loadLanguageFile() {
		// load language config:
		String lang = Settings.language;
		String langFileName = "language-" + lang + ".yml";
		File langFile = new File(this.getDataFolder(), langFileName);
		if (!langFile.exists() && this.getResource(langFileName) != null) {
			this.saveResource(langFileName, false);
		}

		if (!langFile.exists()) {
			if (!lang.equals("en")) { // if not default // TODO don't hardcode
				Log.warning("Could not find language file '" + langFile.getPath() + "'!");
			} // else: ignore
		} else {
			Log.info("Loading language file: " + langFileName);
			try {
				YamlConfiguration langConfig = new YamlConfiguration();
				langConfig.load(langFile);
				Settings.loadLanguageConfiguration(langConfig);
			} catch (Exception e) {
				Log.warning("Could not load language file '" + langFile.getPath() + "'!", e);
			}
		}
	}

	private void registerDefaults() {
		Log.info("Registering defaults.");
		storageTypes.registerDefaults();
		uiRegistry.registerAll(defaultUITypes.getAllUITypes());
		shopTypesRegistry.registerAll(defaultShopTypes.getAll());
		shopObjectTypesRegistry.registerAll(defaultShopObjectTypes.getAll());
	}

	public SKShopkeepersPlugin() {
		super();
	}

	@Override
	public void onLoad() {
		Log.setLogger(this.getLogger()); // setup logger early
		// setting plugin reference early, so it is also available for any code running here:
		plugin = this;
		ShopkeepersAPI.enable(this);

		// making sure that certain classes, that are needed during shutdown, are loaded:
		// this helps for hot reloads (when the plugin gets disabled, but the original jar got replaced and is therefore
		// no longer available)
		this.loadRequiredClasses();

		// validate that this server is running a minimum required version:
		this.outdatedServer = this.isOutdatedServerVersion();
		if (this.outdatedServer) {
			return;
		}

		// try to load suitable NMS (or fallback) code:
		this.incompatibleServer = !this.setupNMS();
		if (this.incompatibleServer) {
			return;
		}

		// load config:
		this.configLoadError = this.loadConfig();
		if (this.configLoadError != null) {
			return;
		}

		// load language file:
		this.loadLanguageFile();

		// WorldGuard only allows registering flags before it gets enabled.
		// Note: Changing the config setting has no effect until the next server restart or server reload.
		if (Settings.registerWorldGuardAllowShopFlag) {
			WorldGuardHandler.registerAllowShopFlag();
		}

		// register defaults:
		this.registerDefaults();
	}

	@Override
	public void onEnable() {
		assert Log.getLogger() != null; // log should already have been setup
		// plugin instance and API might already have been set during onLoad:
		boolean alreadySetup = true;
		if (plugin == null) {
			alreadySetup = false;
			plugin = this;
			ShopkeepersAPI.enable(this);
		}

		// validate that this server is running a minimum required version:
		if (this.outdatedServer) {
			Log.severe("Outdated server version (" + Bukkit.getVersion() + "): Shopkeepers cannot be enabled. Please update your server!");
			this.setEnabled(false); // also calls onDisable
			return;
		}

		// check if the server version is incompatible:
		if (this.incompatibleServer) {
			Log.severe("Incompatible server version: Shopkeepers cannot be enabled.");
			this.setEnabled(false); // also calls onDisable
			return;
		}

		// load config (if not already loaded during onLoad):
		if (!alreadySetup) {
			this.configLoadError = this.loadConfig();
		} else {
			Log.debug("Config already loaded.");
		}
		if (this.configLoadError != null) {
			Log.severe("Could not load the config!", configLoadError);
			this.setEnabled(false); // also calls onDisable
			return;
		}

		// load language file (if not already loaded during onLoad):
		if (!alreadySetup) {
			this.loadLanguageFile();
		} else {
			Log.debug("Language file already loaded.");
		}

		// process additional permissions
		String[] perms = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : perms) {
			if (Bukkit.getPluginManager().getPermission("shopkeeper.maxshops." + perm) == null) {
				Bukkit.getPluginManager().addPermission(new Permission("shopkeeper.maxshops." + perm, PermissionDefault.FALSE));
			}
		}

		// inform about Spigot exclusive features:
		if (SpigotFeatures.isSpigotAvailable()) {
			Log.debug("Spigot-based server found: Enabling Spigot exclusive features.");
		} else {
			Log.info("No Spigot-based server found: Disabling Spigot exclusive features!");
		}

		// register defaults (if not already setup during onLoad):
		if (!alreadySetup) {
			this.registerDefaults();
		} else {
			Log.debug("Defaults already registered.");
		}

		// setup PlayerIdCache for all already online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			playerIdCache.addPlayerId(player);
		}

		// call startup event so other plugins can make their registrations:
		Bukkit.getPluginManager().callEvent(new ShopkeepersStartupEvent());

		// setup storage and components (but only if trading history is enabled):
		if (Settings.enableTradingHistory) {
			try {
				storage = SKStorage.setup(this);
			} catch (StorageException e) {
				Log.severe("Error during storage setup!", e);
				this.setEnabled(false); // also calls onDisable
				return;
			}

			playerProfiles.onEnable();
			tradingHistory.onEnable();
		}

		// inform ui registry (registers ui event handlers):
		uiRegistry.onEnable();

		// enable ProtectedChests:
		protectedChests.enable();

		// register events:
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PlayerJoinQuitListener(this), this);
		pm.registerEvents(new TradingCountListener(this), this);

		// DEFAULT SHOP OBJECT TYPES

		// enable living entity shops:
		livingShops.onEnable();

		// enable sign shops:
		signShops.onEnable();

		// enable citizens shops:
		citizensShops.onEnable();

		//

		// handling of regular villagers:
		pm.registerEvents(new VillagerInteractionListener(this), this);
		if (Settings.blockVillagerSpawns || Settings.blockWanderingTraderSpawns) {
			pm.registerEvents(new BlockVillagerSpawnListener(), this);
		}
		if (Settings.disableZombieVillagerCuring) {
			pm.registerEvents(new BlockZombieVillagerCuringListener(), this);
		}

		// enable commands:
		commands.onEnable();

		// enable shopkeeper naming:
		shopkeeperNaming.onEnable();

		// enable shopkeeper creation:
		shopkeeperCreation.onEnable();

		// enable shopkeeper storage:
		shopkeeperStorage.onEnable();

		// enable shopkeeper registry:
		shopkeeperRegistry.onEnable();

		// load shopkeepers from saved data:
		boolean loadingSuccessful = shopkeeperStorage.reload();
		if (!loadingSuccessful) {
			// detected an issue during loading
			// disabling the plugin without saving, to prevent loss of shopkeeper data:
			Log.severe("Detected an issue during the loading of the shopkeepers data! Disabling the plugin!");
			shopkeeperStorage.disableSaving();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		shopkeeperRegistry.activateShopkeepersInAllWorlds();

		Bukkit.getScheduler().runTaskLater(this, () -> {
			// remove inactive player shopkeepers:
			this.removeInactivePlayerShops();
		}, 5L);

		// let's update the shopkeepers for all already online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (CitizensHandler.isNPC(player)) continue;
			this.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
		}

		// write back all updated data:
		if (shopkeeperStorage.isDirty()) {
			shopkeeperStorage.saveNow();
		}

		// setup metrics:
		if (Settings.enableMetrics) {
			this.setupMetrics();
		}

		// debugging tools:
		if (Settings.debug) {
			// register debug listener:
			// run delayed to also catch events / event listeners of other plugins:
			Bukkit.getScheduler().runTaskLater(this, () -> {
				boolean logAllEvent = Settings.debugOptions.contains(Settings.DebugOptions.logAllEvents);
				boolean printListeners = Settings.debugOptions.contains(Settings.DebugOptions.printListeners);
				if (logAllEvent || printListeners) {
					DebugListener.register(logAllEvent, printListeners);
				}
			}, 10L);
		}
	}

	@Override
	public void onDisable() {
		// wait for async tasks to complete:
		SchedulerUtils.awaitAsyncTasksCompletion(this, ASYNC_TASKS_TIMEOUT_SECONDS, this.getLogger());

		// shutdown storage and components:
		if (storage != null) {
			tradingHistory.onDisable();
			playerProfiles.onDisable();
			storage.shutdown();
			storage = null;
		}

		// inform ui registry about disable:
		uiRegistry.onDisable();

		// despawn all shopkeepers (prior to saving shopkeepers data and before unloading all shopkeepers):
		shopkeeperRegistry.deactivateShopkeepersInAllWorlds();

		// disable living entity shops:
		livingShops.onDisable();

		// disable sign shops:
		signShops.onDisable();

		// disable citizens shops:
		citizensShops.onDisable();

		// save shopkeepers:
		shopkeeperStorage.saveImmediateIfDirty();

		// disable protected chests:
		protectedChests.disable();

		// disable shopkeeper registry: unloads all shopkeepers
		shopkeeperRegistry.onDisable();

		// disable storage:
		shopkeeperStorage.onDisable();

		shopTypesRegistry.clearAllSelections();
		shopObjectTypesRegistry.clearAllSelections();

		// disable commands:
		commands.onDisable();

		shopkeeperNaming.onDisable();
		shopkeeperCreation.onDisable();

		// clear all types of registers:
		shopTypesRegistry.clearAll();
		shopObjectTypesRegistry.clearAll();
		uiRegistry.clearAll();
		storageTypes.clearAll();
		playerIdCache.clear();

		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);

		ShopkeepersAPI.disable();
		plugin = null;
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	// UTILITIES

	public Executor getSyncExecutor() {
		return syncExecutor;
	}

	public Executor getAsyncExecutor() {
		return asyncExecutor;
	}

	// METRICS

	private void setupMetrics() {
		Metrics metrics = new Metrics(this);
		metrics.addCustomChart(new CitizensChart());
		metrics.addCustomChart(new WorldGuardChart());
		metrics.addCustomChart(new TownyChart());
		metrics.addCustomChart(new VaultEconomyChart());
		metrics.addCustomChart(new GringottsChart());
		metrics.addCustomChart(new ShopkeepersCountChart(shopkeeperRegistry));
		metrics.addCustomChart(new PlayerShopsChart(shopkeeperRegistry));
		metrics.addCustomChart(new FeaturesChart());
		metrics.addCustomChart(new WorldsChart(shopkeeperRegistry));
		// TODO add chart with number of virtual shops?
		// TODO add chart with the server variant used (CraftBukkit, Spigot, Paper, other..)
	}

	// PLAYER JOINING AND QUITTING

	void onPlayerJoin(Player player) {
		playerIdCache.addPlayerId(player);
		this.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
	}

	void onPlayerQuit(Player player) {
		// player cleanup:
		shopTypesRegistry.clearSelection(player);
		shopObjectTypesRegistry.clearSelection(player);
		uiRegistry.onPlayerQuit(player);

		shopkeeperNaming.onPlayerQuit(player);
		shopkeeperCreation.onPlayerQuit(player);
		commands.onPlayerQuit(player);
		playerIdCache.removePlayerId(player);
	}

	// STORAGE:

	// null if trading history is disabled
	public SKStorage getStorage() {
		return storage;
	}

	// PLAYER PROFILES

	public PlayerIdCache getPlayerIdCache() {
		return playerIdCache;
	}

	public PlayerProfiles getPlayerProfiles() {
		return playerProfiles;
	}

	// TRADING HISTORY

	public StorageTypes getStorageTypes() {
		return storageTypes;
	}

	public TradingHistory getTradingHistory() {
		return tradingHistory;
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

	// COMMANDS

	public Commands getCommands() {
		return commands;
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

	// PROTECTED CHESTS:

	public ProtectedChests getProtectedChests() {
		return protectedChests;
	}

	// LIVING ENTITY SHOPS

	public LivingShops getLivingShops() {
		return livingShops;
	}

	// SIGN SHOPS

	public SignShops getSignShops() {
		return signShops;
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

	/**
	 * Gets the default shop object type.
	 * 
	 * <p>
	 * Usually this will be the villager entity shop object type. However, there are no guarantees that this might not
	 * get changed or be configurable in the future.
	 * 
	 * @return the default shop object type
	 */
	public AbstractShopObjectType<?> getDefaultShopObjectType() {
		// default: villager entity shop object type:
		return this.getDefaultShopObjectTypes().getLivingShopObjectTypes().get(EntityType.VILLAGER);
	}

	// SHOPKEEPER NAMING

	public ShopkeeperNaming getShopkeeperNaming() {
		return shopkeeperNaming;
	}

	// SHOPKEEPER CREATION:

	public ShopkeeperCreation getShopkeeperCreation() {
		return shopkeeperCreation;
	}

	@Override
	public boolean hasCreatePermission(Player player) {
		if (player == null) return false;
		return (shopTypesRegistry.getSelection(player) != null) && (shopObjectTypesRegistry.getSelection(player) != null);
	}

	@Override
	public AbstractShopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "CreationData is null!");
		ShopType<?> rawShopType = shopCreationData.getShopType();
		Validate.isTrue(rawShopType instanceof AbstractShopType,
				"Expecting an AbstractShopType, got " + rawShopType.getClass().getName());
		AbstractShopType<?> shopType = (AbstractShopType<?>) rawShopType;
		// forward to shop type:
		return shopType.handleShopkeeperCreation(shopCreationData);
	}

	// INACTIVE SHOPS

	private void removeInactivePlayerShops() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return;

		Set<UUID> playerUUIDs = new HashSet<>();
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				playerUUIDs.add(playerShop.getOwnerUUID());
			}
		}
		if (playerUUIDs.isEmpty()) {
			// no player shops found:
			return;
		}

		// fetch OfflinePlayers async:
		int playerShopkeeperInactiveDays = Settings.playerShopkeeperInactiveDays;
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			List<OfflinePlayer> inactivePlayers = new ArrayList<>();
			long now = System.currentTimeMillis();
			for (UUID uuid : playerUUIDs) {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				if (!offlinePlayer.hasPlayedBefore()) continue;

				long lastPlayed = offlinePlayer.getLastPlayed();
				if ((lastPlayed > 0) && ((now - lastPlayed) / 86400000 > playerShopkeeperInactiveDays)) {
					inactivePlayers.add(offlinePlayer);
				}
			}

			if (inactivePlayers.isEmpty()) {
				// no inactive players found:
				return;
			}

			// continue in main thread:
			SchedulerUtils.runTaskOrOmit(SKShopkeepersPlugin.this, () -> {
				List<PlayerShopkeeper> forRemoval = new ArrayList<>();
				for (OfflinePlayer inactivePlayer : inactivePlayers) {
					// remove all shops of this inactive player:
					UUID playerUUID = inactivePlayer.getUniqueId();

					for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
						if (shopkeeper instanceof PlayerShopkeeper) {
							PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
							UUID ownerUUID = playerShop.getOwnerUUID();
							if (ownerUUID.equals(playerUUID)) {
								forRemoval.add(playerShop);
							}
						}
					}
				}

				// remove those shopkeepers:
				if (!forRemoval.isEmpty()) {
					for (PlayerShopkeeper shopkeeper : forRemoval) {
						shopkeeper.delete();
						Log.info("Shopkeeper " + shopkeeper.getIdString() + " at " + shopkeeper.getPositionString()
								+ " owned by " + shopkeeper.getOwnerString() + " has been removed for owner inactivity.");
					}

					// save:
					shopkeeperStorage.save();
				}
			});
		});
	}

	// HANDLING PLAYER NAME CHANGES:

	// updates owner names for the shopkeepers of the specified player:
	private void updateShopkeepersForPlayer(UUID playerUUID, String playerName) {
		Log.debug(Settings.DebugOptions.ownerNameUpdates,
				() -> "Updating shopkeepers for: " + TextUtils.getPlayerString(playerName, playerUUID)
		);
		boolean dirty = false;
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				UUID ownerUUID = playerShop.getOwnerUUID();
				String ownerName = playerShop.getOwnerName();

				if (ownerUUID.equals(playerUUID)) {
					if (!ownerName.equals(playerName)) {
						// update the stored name, because the player must have changed it:
						Log.debug(Settings.DebugOptions.ownerNameUpdates,
								() -> "  Updating owner name ('" + ownerName + "') of shopkeeper " + shopkeeper.getId() + "."
						);
						playerShop.setOwner(playerUUID, playerName);
						dirty = true;
					} else {
						// The stored owner name matches the player's current name.
						// Assumption: The stored owner names among all shops are consistent.
						// We can therefore abort checking the other shops here.
						Log.debug(Settings.DebugOptions.ownerNameUpdates,
								() -> "  The stored owner name of shopkeeper " + shopkeeper.getId()
										+ " matches the current player name. Skipping checking of further shops."
						);
						return;
					}
				}
			}
		}

		if (dirty) {
			shopkeeperStorage.save();
		}
	}

	// FACTORY

	@Override
	public TradingRecipe createTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return new SKTradingRecipe(resultItem, item1, item2);
	}

	@Override
	public TradingRecipe createTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2, boolean outOfStock) {
		return new SKTradingRecipe(resultItem, item1, item2, outOfStock);
	}

	// OFFERS

	@Override
	public PriceOffer createPriceOffer(ItemStack item, int price) {
		return new SKPriceOffer(item, price);
	}

	@Override
	public TradingOffer createTradingOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return new SKTradingOffer(resultItem, item1, item2);
	}

	@Override
	public BookOffer createBookOffer(String bookTitle, int price) {
		return new SKBookOffer(bookTitle, price);
	}
}
