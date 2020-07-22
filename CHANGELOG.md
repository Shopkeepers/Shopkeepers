# Changelog
Date format: (YYYY-MM-DD)  

## v2.11.0 (TBA)
### Supported MC versions: 1.16.1, 1.15.2, 1.14.4

* Added: New command `/shopkeeper givecurrency [player] ['low'|'high'] [amount]`.
  * This command can be used to create and give the currency items which have been specified inside the config.
  * Added corresponding permission node `shopkeeper.givecurrency` (default: op).
* Added: When giving items via command to another player we now also inform the target player that he has received items. These newly added messages are also used now when giving items to yourself.
* Added: New command `/shopkeeper convertItems [player] ['all']`.
  * This command can be used to convert the held (or all) items to conform to Spigot's internal data format. I.e. this runs the items through Spigot's item serialization and deserialization in the same way as it would happen when these items are used inside shopkeeper trades and the plugin gets reloaded.
  * Added corresponding permission node `shopkeeper.convertitems.own` (default: op). This allows converting own items.
  * Added corresponding permission node `shopkeeper.convertitems.others` (default: op). This allows converting items of other players.
* Added config options for the automatic conversion of items inside the inventories of players and shop chests to Spigot's internal data format whenever a player is about to open a shopkeeper UI (eg. trading, editor, hiring, etc.).
  * Added config option `convert-player-items` (default: false). This enables and disables the automatic item conversion.
  * Added config options `convert-all-player-items` (default: true) and 'convert-player-items-exceptions' (default: []). These two settings allow limiting which items are affected or ignored by the automatic conversion.
  * Note: Enabling this setting comes with a performance impact. You should generally try to avoid having to use this setting and instead search for alternative solutions. For more information, see the notes on this setting inside the default config.
* Debug: Added debug option 'item-conversions' which logs whenever we explicitly convert items to Spigot's data format. Note that this does not log when items get implicitly converted, which may happen under various circumstances.
* Config: Added option 'max-trades-pages' (default: 5, min: 1, max: 10) which allows changing the number of pages that can be filled with trading options. This limit applies to all shopkeepers (there are no different settings for different types of shops or different permission levels so far). Note: The scroll bar rendered by the Minecraft client will only work nicely for up to 64 trades.
* Added: It is now possible to change the size of slimes and magma cubes. Their default size is 1 (tiny). Even though Minecraft theoretically allows sizes up to 256, we limit the max size to 10. This avoids running into issues such as related to rendering, performance and not being able to interact with the slime or magma cube. If you have market areas where players can create their own shops and they are able to create slime or magma cube shopkeepers, you might want to take this maximum slime/magma cube size into account when assigning shop areas to players.
* Fixed: Slimes and magma cubes no longer spawn with random size whenever they are respawned.
* Fixed: Various optional (context dependent) command arguments were shown to be required inside the Shopkeepers command help.
* Fixed: We no longer attempt to spawn Citizens NPCs when creating or loading Citizens shopkeepers if the spawn location's world is not loaded currently.
* Fixed: Some versions of Citizens would produce an error when we try to teleport a NPC which has no location and is therefore not spawned currently. The teleport attempt has been replaced with an attempt to spawn the NPC.
* Fixed: The `shopkeeper.*` permission was missing some child permissions.
* Removed: The legacy permissions `shopkeeper.player.normal`, `shopkeeper.villager`, `shopkeeper.witch` and `shopkeeper.creeper` have been removed. Use the corresponding replacement permissions instead.
* Changed: All players have access to all mob types (permission `shopkeeper.entity.*`) by default now.
* Added: The new 'all' argument for the `/shopkeeper list` command will list all shops now (admin and player shops). (Thanks to Mippy, PR 669)

Internal changes:
* Slightly changed how we cycle through the villager levels (badge colors).
* Added support for Lists of ItemData inside the config.
* We throw an exception now when we encounter an unexpected / not yet handled config setting type.
* We load all plugin classes up front now. This should avoid issues when the plugin jar gets replaced during runtime (eg. for hot reloads).

Config changes:  
* The default value of the `prevent-shop-creation-item-regular-usage` setting was changed to `true`.
* The default value of the `shop-creation-item` setting was changed to a villager spawn egg with display name `&aShopkeeper`. You can give yourself this item in game via the `/shopkeeper give` command.
* The `use-legacy-mob-behavior` setting was broken since MC 1.14 and has been removed now. All shopkeeper entities always use the NoAI flag now.
* The default value of the `enable-citizen-shops` setting was changed to `true`.

Added messages:  
* msg-currency-items-given
* msg-high-currency-items-given
* msg-high-currency-disabled
* msg-shop-creation-items-received
* msg-currency-items-received
* msg-high-currency-items-received
* msg-command-description-give-currency
* msg-command-description-convert-items
* msg-items-converted
* msg-button-slime-size
* msg-button-slime-size-lore
* msg-button-magma-cube-size
* msg-button-magma-cube-size-lore

## v2.10.0 (2020-06-26)
### Supported MC versions: 1.16.1, 1.15.2, 1.14.4

**Update for MC 1.16.1:**
* Added zombified piglin, piglin, hoglin, zoglin and strider to the by default enabled mob types. If you are updating, you will have to manually add these to your config's 'enabled-living-shops' setting.
* During my quick initial testing I did not encounter any major issues with these new mobs, but there are some oddities you might want to be aware of:
  * We don't support changing the baby property of piglin and zoglin shopkeepers yet. However, we at least ensure that they always spawn as adult.
  * The zombified piglin, hoglin and strider already support changing the baby property.
  * The strider constantly shakes when being spawned outside the nether and randomly spawns with saddle.
* The pig zombie mob type has been removed from the by default enabled mob types. If you are updating to MC 1.16, it will get automatically removed from your config. To prevent your config from losing its comments and formatting during this small migration, consider manually removing this mob type before your update.
* If you are updating to MC 1.16, your pig zombie shopkeepers get automatically converted to zombified pigman shopkeepers.
* Internal: Any internal references to the pig zombie mob type have been removed to prevent any kind of binary problems to arise.
* Sign shops support the new crimson and warped sign variants.
* Internal data format changes: Sign shops of type 'GENERIC' and 'REDWOOD' are migrated to 'OAK' and 'SPRUCE' respectively.
* Note on the removal of item type 'ZOMBIE_PIGMAN_SPAWN_EGG' and its replacement with item type 'ZOMBIFIED_PIGLIN_SPAWN_EGG':
  * If you are updating and your config contains an item of type 'ZOMBIE_PIGMAN_SPAWN_EGG' you will have to manually migrate this item to a 'ZOMBIFIED_PIGLIN_SPAWN_EGG'.
  * Any items stored inside the shopkeepers (eg. for their trades or hire cost items) are automatically migrated.
* Note on Minecraft's new RGB color codes and other new text related features: I have not yet looked into supporting those in the texts and messages of the Shopkeepers plugin.

**Other migration notes:**
* Removed: We no longer migrate items inside the config from legacy (pre MC 1.13) item types, data values and spawn eggs to corresponding item types in MC 1.13. Instead any unknown item types get migrated to their default now.

**Other changes:**
* Changed/Improved: We use a combination of our own 'Shopkeepers data version' (which has been bumped to 2) and Minecraft's data version for the data version stored inside the save.yml now. Minecraft's data version is incremented on every Minecraft release (including minor updates) and may indicate that new item migrations have been added. So whenever you update your server, we automatically trigger a full migration of all your shopkeepers data to ensure that your save.yml is always up-to-date.
* Fixed: Some mobs randomly spawn with passengers. We remove these passengers now.
* Fixed: When a mob randomly spawns with a passenger, this would previously interfere with our 'bypass-spawn-blocking' feature and print a warning inside the log about an unexpected entity (the passenger) being spawned.
* Fixed: The random equipment of certain mobs gets properly cleared now. For instance, this resolves the issue of foxes randomly carrying items inside their mouth.
* Fixed: When a Citizens NPC, created without the 'shopkeeper' trait, is deleted, we immediately delete any corresponding shopkeeper now. Previously the corresponding shopkeeper would not get deleted right away, but only during the next plugin startup (when checking whether the corresponding NPC still exists). Any chest used by the shopkeeper would remain locked until then.
* Improved: In order to determine the player who is setting up a shopkeeper via the 'shopkeeper' trait, we previously only took players into account which are adding the trait via the Citizens trait command (NPCTraitCommandAttachEvent). However, players are also able to add traits during NPC creation. We now also react to players creating NPCs (PlayerCreateNPCEvent) and then (heuristically) assume that any directly following trait additions for the same NPC within one tick are caused by this player. This player will then be able to receive feedback messages about the shopkeeper creation.
* Improved: For any trait additions not directly associated with a player, we previously waited 5 ticks before the corresponding shopkeeper got created. One (minor) side effect of the above change is that we react to all trait additions within 1 tick now.
* Added: Added a link inside the config which points to the translations repository.
* Added: Added feedback messages when a player cannot trade with a shop due to trading with own shops being disabled or if the shop's chest is missing.
* Changed: The check for whether the player is able to bypass the restriction of not being able to trade with own shops was previously checking if the player is an operator. Instead we now check if the player has the bypass permission.
* Changed: When checking if the player is able to bypass the restriction of not being able to trade with shops while their owner is online, we only check for the bypass permission now after checking if the shop owner is actually online currently.
* Added: The variant of rabbit shopkeepers can be changed now. Any existing rabbit shopkeepers will use the brown variant (the default). This also resolves an issue with the rabbit type randomly changing whenever the shopkeeper is respawned.
* Added: Added a header comment to the top of the save.yml file mentioning the risk of manually editing this file while the server is still running or without making a backup first.
* Fixed: If regular item usage is disabled we also prevent any kind of entity interaction now while holding the shop creation item in hand. Players in creative mode or with the 'shopkeeper.bypass' permission are exempt from this restriction.
* Fixed: Checking the WorldGuard allow-shop flag now takes into account the player for whom the flag is being queried.
* Fixed: The returned shop creation item would get dropped twice under certain conditions.
* Fixed: The shop creation item is now also returned if a player deletes his own shop via command.
* Fixed/API: The PlayerDeleteShopkeeperEvent is now also called when a player deletes shops via command.
* Changed: The result message after deleting shops via command will now print the number of actually removed shops (which does not necessarily match the number of shops that were confirmed for removal).
* Changed: The item representing the black horse inside the editor is now slightly less black.
* Debug: Added the debug option 'capabilities', which logs additional details when the plugin checks for server version dependent capabilities.
* Debug: Added some more information to the debug message that gets logged when the PlayerDeleteShopkeeperEvent has been cancelled.
* Debug: Minor changes to some debug messages related to Citizens shopkeepers.
* API/Internal: Added Shopkeeper#delete(Player) which optionally passes the player responsible for the shopkeeper deletion. Note that the player is not passed if a player shop is deleted due to a player breaking the shop's chest.
* API: Added a note about the PlayerDeleteShopkeeperEvent not being called in all circumstances.

Internal changes:  
* Moved most of the code responsible for returning the shop creation item for deleted player shops into the new PlayerShopkeeper#delete(Player) method.
* Added ShopkeeperEventHelper class and moved the common code for calling and handling PlayerDeleteShopkeeperEvents there.
* Various minor refactorings related to the Text implementation.
* Various minor refactorings related to Citizens shopkeepers.
* Minor formatting changes. Not applied to the whole code base yet.

Added messages:  
* msg-button-rabbit-variant
* msg-button-rabbit-variant-lore
* msg-cant-trade-with-own-shop
* msg-cant-trade-with-shop-missing-chest

Changed messages (you will have to manually update those!):  
* Renamed 'msg-removed-player-shops' to 'msg-removed-shops-of-player'.
* Renamed 'msg-removed-all-player-shops' to 'msg-removed-player-shops'.
* Renamed 'msg-confirm-remove-admin-shops' to 'msg-confirm-remove-all-admin-shops'.
* Renamed 'msg-confirm-remove-own-shops' to 'msg-confirm-remove-all-own-shops'.
* Renamed 'msg-confirm-remove-player-shops' to 'msg-confirm-remove-all-shops-of-player'.
* The 'msg-removed-player-shops' message (previously 'msg-removed-all-player-shops') no longer mentions that 'all' shops got deleted (since this is not necessarily true).
* Changed the 'msg-button-villager-level' and 'msg-button-villager-level-lore' messages to clarify that this option only changes the visual appearance of the villager's badge color. The included german translation has been updated accordingly as well.
* Slightly changed the german translation of the 'msg-cant-trade-while-owner-online' message.
* Removed the note about left and right clicking items to adjust amounts from the 'msg-trade-setup-desc-admin-regular' message, since this doesn't actually apply to admin shops.

## v2.9.3 (2020-04-12)
### Supported MC versions: 1.15.2, 1.14.4

* Fixed: If a trade required two matching item stacks of the same type but different sizes, it was possible to trade for less items when offering the items in reverse order.
* Added: The `/shopkeeper remote` command can be used from console now and optionally accepts a player as argument. The shop will then be opened for the specified player.
  * Added permission: 'shopkeeper.remote.otherplayers' (default: op). This is required for opening shops for other players.
* Partially fixed: When a player shift double left-clicks inside the editor view, Minecraft triggers shift left-clicks on all slots containing matching items. This causes the prices of other trades to unintentionally change. We heuristically assume that any shift left-clicks occurring within 250 ms on a slot different to the previously clicked were triggered automatically and ignore those clicks now.
  * Unfortunately, we cannot use a lower time span, because the automatically triggered clicks may be received quite some time later (up to 150 ms on a local server and possibly more with network delay involved). This heuristic also does not work for automatic clicks on the same slot. Since the automatic clicks are received quite some time later we cannot differentiate them from regular fast clicking.
* Debug: Added the current time in milliseconds to the debug output of click events.
* API: UISession keeps track whether it is still valid.

Changed messages (you will have to manually update those!):  
* msg-command-description-remote: Adapted to the addition of the optional player argument.
* msg-command-description-give: Slightly changed the german translation.

## v2.9.2 (2020-03-04)
### Supported MC versions: 1.15.2, 1.14.4

* Added: If the setting 'delete-shopkeeper-on-break-chest' is enabled, player shopkeepers will now periodically (roughly once every 5 seconds) check if their chest is still present and otherwise delete themselves.
  * This allows them to detect when other plugins, such as WorldEdit, remove the shop chest.
  * The setting 'deleting-player-shop-returns-creation-item' applies to these checks as well and controls whether to drop a shop creation item for every removed shopkeeper.
* Fixed a potential CME when shopkeepers get removed after their shop chest got deleted.
* Fixed: The 'active shopkeepers' would not get properly cleaned up in some occasions (even on shopkeeper removal) if the shopkeeper mob got deleted or the shop object was no longer considered 'active' for some other reason. A side effect of this was that the shopkeeper entity would get respawned, even though it was not supposed to get spawned or even after the shopkeeper was already deleted.
* Fixed: The DerivedSettings use the default value for the name-regex setting during initialization now to properly catch user errors during the subsequent setup after the config has already been loaded.
* Fixed: The selling and book shops attempted to convert currency items into high currency items even if the high currency got disabled.
* Fixed: Trading via shift-clicking while the player is charging a trident would allow the player to duplicate the trident.
  * The issue is caused by the inventory getting updated while the trident is being charged (or any other usable item being used).
  * We now prevent item actions to even start while interacting with a shopkeeper. Note: The client might currently still display the item action animation, even though it has been successfully stopped on the server (see SPIGOT-5609).
  * Additionally, we only update those inventory slots that were actually changed by inventory manipulations. This also has the benefit of sending less inventory slot updates.

Internal changes:  
* Added AbstractShopkeeper#tick which gets invoked roughly once per second for all shopkeepers in currently active chunks.

## v2.9.1 (2020-01-22)
### Supported MC versions: 1.15.2, 1.14.4

**Update for MC 1.15.2:**  
* Bumped CraftBukkit dependency for MC 1.15 to 1.15.2.
* Changed: Replaced the NMS call for setting the CanJoinRaid property on MC 1.15.x with a corresponding Bukkit API call. This fixes compatibility with Spigot 1.15.2, but only works on the very latest versions of MC 1.15.1 and upwards.

**Other changes:**  
* Added: Warning messages when a trading offer cannot be loaded for some reason.
* Changed: Trading offer and hire cost items get automatically migrated to the current data version now.
  * This will typically occur with every (even minor) minecraft updates.
  * A warning is logged if an offer can not be migrated.
  * Minecraft may log 'Unable to resolve BlockEntity for ItemStack ...' messages during ItemStack migrations. You can safely ignore these debug messages.
  * Config/Debug: Added debug option 'item-migrations' to log whenever a shopkeeper performs item migrations.

## v2.9.0 (2019-12-28)
### Supported MC versions: 1.15.1, 1.14.4

**Update for MC 1.15.1:**
* Added bees to the by default enabled mob types. If you are migrating from a previous version, you will have to manually enable them in the config.

**Other changes:**  
* Fixed: Raider shopkeeper mobs were able to join and thereby interfere with nearby raids. This should no longer be the case.
* Fixed: The save file was missing the data-version when initially created. This caused subsequent reloads to always trigger a 'migration' / forced save without actually being required.
* Fixed: The book shopkeeper was ignoring books with missing generation tag. These are now treated as 'original' books, just like Minecraft does.
* Fixed: Spigot seems to (internally) support books with empty titles now. The book shopkeepers were updated to ignore them, since supporting them would require fundamental changes to how book prices are stored and how books are identified. Those books can't be created in vanilla Minecraft, so this shouldn't be a severe limitation.
* Fixed: Due to a Minecraft bug (MC-141494) interacting with a villager while holding a written book in the main or off hand results in weird glitches and tricks the plugin into thinking that the editor or trading UI got opened even though the book got opened instead. We therefore ignore any interactions with shopkeeper mobs for now when the interacting player is holding a written book.
* Fixed: The book shopkeeper would not correctly store offers for books that have dots in their name.
* Fixed: We would previously drop the shop-creation item returned on shop deletion at the shop's location, even if the shop got deleted via remote editing from far away (and is potentially not even loaded). If the player is further than 10 blocks away (or if the shop object is not loaded), it will drop the item at the player's location now.
* Fixed: The shop creation item can no longer be used from dispensers if regular use is disabled.
* Config/Fixed: Derived settings were not updated when loading messages from a separate language file.
* Config/Fixed: Some settings would not loaded correctly depending on the used locale. Also made all text comparisons locale independent.
* Config/Fixed: In case the name-regex setting cannot be parsed, we now print a warning and revert to the default (instead of throwing an error).
* API/Fixed: NPE when accessing a non-existing second offered item from the ShopkeeperTradeEvent.
* API/Fixed: The offered items inside the ShopkeeperTradeEvent are copies now and their stack sizes match those of the trading recipe.
* Messages/Fixed: The internal default for message 'msg-list-shops-entry' (that gets used if the message is missing in the config) was not matching the message in the default config.
* Internal/Fixed: Improved thread-safety for asynchronous logging operations and settings access.
* Changed: Villager shopkeepers get their experience set to 1 now. I wasn't able to reproduce this myself yet, but according to some reports villager shopkeepers would sometimes lose their profession. Setting their experience to something above 0 is an attempt to resolve this.
* Changed: Instead of using a fallback name ("unknown"), player shops are required to always provide a valid owner name now.
* Changed: Explicitly checking for missing world names when loading shopkeepers.
* Changed: Added validation that the unique id of loaded or freshly created shopkeepers is not yet used.
* Changed: Added more information to the message that gets logged when a shopkeeper gets removed for owner inactivity.
* Changed: The errors about a potentially incompatible server version and trying to run in compatibility mode are warnings now.
* Messages/Changed: The msg-shop-creation-items-given message was using the player's display name. This was changed to use the player's regular name to be consistent with the rest of the plugin.
* Config/Changed: The plugin will now shutdown in case a severe issue prevents loading the config. This includes the case that the config version is invalid. Previously it would treat invalid and missing config versions the same and apply config migrations nevertheless.
* Config/Changed: The always-show-nameplates setting seems to be working again (since MC 1.9 already). The corresponding comment in the default config was updated.
* Config/Changed: Changed/Added a few information/warning messages related to config and language file loading.
* Config/Changed: Only printing the 'Config already loaded' message during startup if the debug mode is enabled.
* Debug: Debug option 'owner-name-updates' enables additional output whenever stored shop owner names get updated.
* Various changes to the shopkeeper registry and shopkeeper activation:
  * We keep track now which chunks have been activated. This avoids a few checks whether chunks are loaded.
  * The delayed chunk activation tasks get cancelled now if the chunk gets unloaded again before it got activated. This resolves a few inconsistencies such as duplicate or out-of-order chunk activation and deactivation handling when chunks get loaded and unloaded very frequently.
  * Similarly the respawn task on world saves gets cancelled now if the world gets unloaded.
  * Shopkeeper spawning is skipped if there is a respawn pending due to a world save.
  * Shopkeepers are now stored by world internally. This might speed up a few tasks which only affect the shopkeepers of a specific world.
  * Debug: Added debug option 'shopkeeper-activation'. Various debug output related to chunk/world loading/unloading/saving and spawning/despawning of shopkeepers was moved into this debug category.
  * Debug: The "/shopkeepers check" command now outputs some additional information about active chunks.
  * Internal: Moved all logic from WorldListener into SKShopkeeperRegistry. Various internally used methods are now hidden.

**API changes:**  
* Added: PlayerShopkeeper#getChestX/getChestY/getChestZ
* Added: ShopkeepersStartupEvent which can be used by plugins to make registrations during Shopkeepers' startup process (eg. to register custom shop types, object types, etc.). This event is marked as deprecated because custom shop types, object types, etc. are not yet officially supported as part of the API. Also, the event is called so early that the plugin (and thereby the API) are not yet fully setup and ready to be used, so this event is only of use for plugins which know what they are doing.
* Added: Shopkeeper#getIdString.
* Removed: Various API methods from Shopkeeper which simply delegated to the corresponding shop object.
* Changed: Moved ShopObjectType#needsSpawning into ShopObject.
* Changed: Renamed PlayerShopkeeper#getOwnerAsString to #getOwnerString.
* Changed: Made some preparations to support virtual shopkeepers in the future (which are not located in any world). Various location related API methods may now return null.
* Various changes to ShopkeeperRegistry:
  * Changed: Methods inside ShopkeeperRegistry now return Collections instead of Lists.
  * Removed: ShopkeeperRegistry#getShopkeepersInWorld(world, onlyLoadedChunks)
  * Removed: ShopkeeperRegistry#getShopkeepersInChunk(chunk)
  * Removed: ShopkeeperRegistry#getAllShopkeepersByChunks
  * Removed: ShopkeeperRegistry#getShopkeeperByName
  * Added: ShopkeeperRegistry#getVirtualShopkeepers
  * Added: ShopkeeperRegistry#getShopkeepersByName
  * Added: ShopkeeperRegistry#getShopkeepersByNamePrefix
  * Added: ShopkeeperRegistry#getAllPlayerShopkeepers
  * Added: ShopkeeperRegistry#getPlayerShopkeepersByOwner(ownerUUID)
  * Added: ShopkeeperRegistry#getWorldsWithShopkeepers
  * Added: ShopkeeperRegistry#getShopkeepersInWorld(worldName)
  * Added: ShopkeeperRegistry#getShopkeepersByChunks(worldName)
  * Added: ShopkeeperRegistry#getActiveChunks(worldName)
  * Added: ShopkeeperRegistry#isChunkActive(chunkCoords)
  * Added: ShopkeeperRegistry#getShopkeepersInActiveChunks(worldName)

**Various (mostly internal) changes to commands and argument parsing:**  
* Fallback mechanism:
  * Previously arguments were parsed one after the other. In the presence of optional arguments this can lead to ambiguities. For example, the command "/shopkeeper list [player] 2" with no player specified is supposed to fallback to the executing player and display his second page of shopkeepers. Instead the argument '2' was previously interpreted as the player name and the command therefore failed presenting the intended information.
  * A new mechanism was added for these kinds of fallbacks: It first continues parsing to check if the current argument can be interpreted by the following command arguments, before jumping back and then either providing a fallback value or presenting a likely more relevant error message.
  * Most optional arguments, default values and fallbacks were updated to use this new fallback mechanism, which should provide more relevant error messages in a few edge cases.
* Fixed: Commands would sometimes not correctly recognize the targeted shopkeeper entity. This is caused by SPIGOT-5228 keeping dead invisible entities around, which get ignored by the commands now.
* Added: The "give", "transfer", "list" and "remove" commands show the player's uuid as hover text now and allow it to be copied into the chat input via shift clicking.
* The "list", "remove", "transfer" and "setTradePerm" commands can be used from console now. Command confirmations work for the console as well now (any command sender that is not a player is considered to be the 'console' for this purpose).
* The "setForHire" and "transfer" commands allow specifying the shopkeeper via argument now. Also: When targeting a chest that happens to be used by multiple shopkeepers (eg. due to manually modified save data..), it picks the first one now (instead of applying the command to all shops). In the future this will likely print an error message instead.
* The "debugCreateShops" command limits the shop count per command invocation to 1000 and prints the number of actually created shops now.
* Added debug option 'commands' and added various debug output when parsing and executing commands.
* All argument suggestions are limited to 20 entries by default now.
* Player arguments suggest matching uuids now. To avoid spamming the suggestions with uuids for the first few characters of input, suggestions are only provided after at least 3 characters of matching input.
* Some commands (eg. "list") provide suggestions for names of online players now.
* The "list" and "remove" commands accept player uuids now and ignore the case when comparing player names.
* The "list" and "remove" commands handle ambiguous player names now: If there are shops of different players matching the given player name, an error message is shown and the player needs to be specified by uuid instead. The player names and uuids can be copied to the chat input via shift clicking. If a player with matching name is online, that player is used for the command (regardless of if the given player name is ambiguous).
* The shops affected by the "remove" command are now determined before asking for the users confirmation. This allows detecting ambiguous player names and missing player information before prompting the command executor for confirmation. A minor side effect of this is that any shops created after the command invocation are no longer affected by the remove command once it gets confirmed.
* Internal: Refactored name, uuid and id based parsing (and matching) of players and shopkeepers to allow for more code reuse. Added ObjectByIdArgument which contains most of the shared logic now.
* Internal: Added ShopkeeperIdArgument.
* Internal: Added TransformedArgument which allows transforming of parsed arguments.
* Internal: Minor refactoring to the targeting of shopkeepers and changes to how targeting of shopkeepers is handled in case no shopkeeper can be parsed from the command input. This should result in more appropriate error messages when specifying an invalid shopkeeper.
* Internal: FirstOf-arguments reset the parsed arguments before every child argument's completion attempt, so that every child argument has a chance to provide completions.
* Internal: FirstOf-arguments now forward the exceptions of their child arguments (instead of using their own).
* Internal: Added the ability to define 'hidden' command arguments. These can for example be used to inject information into the command's execution context without requiring textual input from the user.
* Internal: CommandArgument#isOptional now only controls the formatting. The parsing behavior is left to the individual argument implementations.
* Internal: Added type parameter to CommandArgument.
* Internal: Added ArgumentRejectedException for when an argument got parsed, but rejected by a filter rule. This is used to provide more relevant error messages in FirstOf-arguments.
* Internal: Added MissingArgumentException and InvalidArgumentException (which ArgumentRejectedException extends) which allows specifically handling those types of exceptions.
* Internal: Renamed CommandArgument#missingArgument and #invalidArgument to #missingArgumentError and #invalidArgumentError.
* Internal: Added CommandArgument#requiresPlayerError with a corresponding default message (msg-command-argument-requires-player) for arguments that require a player as executor.
* Internal: Added more general BoundedIntegerArgument. PositiveIntegerArgument makes use of it.
* Internal: Moved ArgumentFilter into base commands lib package.
* Internal: Command arguments keep track of their parent argument now (if used internally by another argument) and use that for their error messages.
* Internal: Added a display name property to all command arguments that can be used to change the name that is used to represent the argument in the command format. This is especially useful for conflicting literal arguments. Literal arguments will omit their actual argument name from the argument completions if a different display name has been set.
* Internal: Minor changes to handling errors during command handling. Besides the stack trace, the plugin also logs the command context (parsed arguments) now.
* Internal: CommandArgument#parse now also returns the parsed value. This is useful when there is a chain of wrapped arguments and the parent needs to handle the value parsed by the child argument in some way.
* Internal/Fixed: Chains of FirstOfArguments should now be able to properly store all the values parsed by child arguments along the chain. Previously this only worked for a chain depth of 1.
* Internal: Added validation that no arguments with the same name get added to the same command.
* Internal: Moved default shopkeeper argument filters into ShopkeeperFilter class/namespace.
* Internal: Added map view, toString and copy to CommandContext and made constructor CommandContext(otherContext) protected.
* Internal: CommandArgs no longer makes a copy of the passed arguments.
* Internal: Added marker interface for the CommandArgs state. When resetting CommandArgs to a previous state they ensure now that the state got created from the same CommandArgs instance.
* Internal: Added CommandArgs#copy. Since the underlying arguments are expected to never change, they are not actually copied (only the parsing state is copied). Any captured CommandArgs states are applicable to the copy as well.
* Internal: Replaced CommandArgs with ArgumentsReader: The arguments are now stored inside the CommandInput and the ArgumentsReader only references them from there.
* Internal: CommandContext is now an interface (with SimpleCommandContext as implementation). A new class CommandContextView was added that gets used anywhere where accessing the context is allowed, while modifying is not.
* Internal: Command and tab completion handling was moved from BaseCommand into Command.
* Internal: Resetting of the arguments reader if parsing of a command argument failed was moved from CommandArgument#parse into Command.
* Internal: Added TypedFirstOfArgument, a variant of FirstOfArgument that preserves the result type of its child arguments.
* Internal: Added NamedArgument, which can be useful if there would otherwise be conflicts / ambiguities between arguments.
* Internal: ArgumentParseException provides the command argument that created it now. This is especially useful for debugging purposes.
* Internal: The commands library was updated to use a new text representation everywhere now, and thereby support text features such as hover events, click events, etc.

Other internal changes:  
* Internal: The save task was using a copy of the save data to protect against concurrent modifications while an async save is in progress. However, since the actual save data did not get modified during ongoing saves anyways, this copy is redundant and was therefore removed.
* Internal: Made various changes in order to support Minecraft's text features such as hover events, click events, insertions, etc. Most texts are now stored in a new format internally.
* Internal: Various changes to TextUtils and argument replacement.
  * Arguments can be arbitrary objects now and Suppliers can be used to dynamically lookup argument values.
  * Argument replacement uses a Map-based argument lookup now. Placeholder keys are specified without the surrounding braces now. Argument replacement for both plain Strings as well as the new text format should be faster now.
* Internal: Moved newline pattern and splitting into StringUtils.
* Internal: The regex pattern used to validate shopkeeper names gets precompiled now.
* Internal: bstats gets shaded into the package '[..].libs.bstats' now.
* Internal: Added Settings#isDebugging and Settings#isDebugging(option) to conveniently (and thread-safe) check for debugging options.
* Internal: Default shop, object and UI types are getting registered early during onLoad now.
* Internal: Separated config and language file loading.
* Internal: Slightly changed the text format that gets used at a few places to represent a player's name and uuid.
* Internal: Renamed CollectionUtils to MapUtils.
* Internal: All concatenated debug messages get lazily created only when actually required.

Migration notes:  
* Removed the importing of old book offers (from late MC 1.12.2, see v1.83). When updating from an older version of Shopkeepers, you will have to first update to a version in-between.

Save data format changes:  
* The storage of book shopkeeper offers has changed.
* The ids used for storing shopkeeper offers start at 1 now (instead of 0). This has no impact on the loading of save data (it still accepts any ids), but makes it nicer to read.

Changed messages (you will have to manually update those!):  
* msg-list-shops-entry: 'object type' changed to 'object', and the arguments '{shopSessionId}' and '{shopId}' changed to '{shopId}' and '{shopUUID}' respectively. Argument '{shopSessionId}' still works but will likely get removed in the future.
* msg-villager-for-hire: The german translation was slightly changed.
* Normalized the representation of various multi-line messages inside the default config:
  * msg-creation-item-selected
  * msg-shop-setup-desc-selling
  * msg-shop-setup-desc-buying
  * msg-shop-setup-desc-trading
  * msg-shop-setup-desc-book
  * msg-shop-setup-desc-admin-regular
* Added {shopsCount} argument to shop removal confirmation messages:
  * msg-confirm-remove-admin-shops
  * msg-confirm-remove-own-shops
  * msg-confirm-remove-player-shops
  * msg-confirm-remove-all-player-shops

Removed messages:  
* The default german translation contained a few no longer used messages: msg-button-type and msg-button-type-lore

New messages:  
* msg-command-argument-requires-player
* msg-ambiguous-player-name
* msg-ambiguous-player-name-entry
* msg-ambiguous-player-name-more

## v2.8.1 (2019-08-23)
### Supported MC versions: 1.14.4

* Fixed: An issue introduced in the previous update prevented players from using the shop creation item if the item's regular usage got disabled in the config.
* Config: Added a comment about the available debug options.
* Changed: Cancelling all EntityTransformEvents involving shopkeeper entities. This replaces the previously used workaround for preventing villagers struck by lightning turning into witches.

Internal:  
* Refactoring: Moved lots of utilities from Utils class into more specific utility classes.
* Refactoring: Using our own Validation class, since the one bundled in Bukkit is outdated and missing some features.

## v2.8.0 (2019-08-22)
### Supported MC versions: 1.14.4

* Bumped Bukkit dependency from 1.14.3 to 1.14.4.
* Added: Changed how items are getting defined in the config. Internally this new format uses Bukkit's item serialization for parsing the item data, which allows it to support the specification of arbitrary item data and hopefully not require any major maintenance for future minecraft versions. At the same time it tries to stay (slightly) more user-friendly than Bukkit's item serialization by omitting any data that can be restored by the plugin, by avoiding one level of nesting between the item type and item data, by translating ampersand ('&') color codes in display name and lore, and by offering a compact representation for specifying an item only by its type.
  * This change also allows a more detailed specification of some of the editor button items. However, many editor buttons still miss corresponding config settings. Also keep in mind that the display name and lore for these button items get specified via corresponding message settings, so any specified item display name and lore will get replaced by that.
  * When checking if an in-game item matches the item data specified in the config, only the specified data gets compared. So this does not check for item data equality, but instead the checked item is able to contain additional data but still get matched (like before, but previously this was limited to checking display name and lore).
  * The previous item data gets automatically migrated to the new format (config version 2).
* Renamed the setting 'high-zero-currency-item' to 'zero-high-currency-item'. This gets automatically updated during the migration.
* Changed: All priorities and ignoring of cancelled events were reconsidered.
  * Event handlers potentially modifying or canceling the event and which don't depend on other plugins' event handling are called early (LOW or LOWEST), so that other plugins can react to / ignore those modified or canceled events.
    * All event handlers which simply cancel some event use the LOW priority now and more consistently ignore the event if already cancelled. Previously they mostly used NORMAL priority.
    * Zombie villager curing is prevented (this includes sending the player a message) on LOW priority now. If some plugin wants to bypass Shopkeepers (for example to allow the curing of specific zombie villagers) it can still cancel the event on lowest priority, so that Shopkeepers ignores it, and then uncancel it afterwards.
    * Interaction with the shop creation item (chest selection, shop creation) is handled at LOWEST priority now, so that hopefully even protection plugins can ignore the cancelled event and don't handle it twice when we check chest access by calling another fake interact event. In order to resolve potential conflicts with other event handlers acting on LOWEST priority (such as Shopkeepers' sign interaction listener), we ignore the event if interaction with the item got already cancelled.
    * Interaction with shopkeepers (entities and signs) are handled at LOWEST priority now, so that other plugins more reliably ignore the cancelled event. This makes bypassing of other plugin's interaction blocking the new default behavior. Taking the interaction result of other plugins into account can be enabled by the new setting 'check-shop-interaction-result', which involves calling a fake interact event (similarly to how chest access is checked). Since this is usually not wanted and might in general cause side effects (depending on the other plugins active on the server) this is disabled by default. In order to resolve conflicts with other event handlers acting on LOWEST priority (such as Shopkeepers' shop creation item interaction handling), the interaction is ignored if the event got already cancelled.
    * Most UI clicking is handled at LOW priority (like before).
  * Event handlers purely reacting to an event (without modifying or canceling it) are called at the MONITOR stage.
    * The shop creation item usage gets sent on MONITOR priority now when switching the held item.
    * Handling of shopkeepers spawning and despawning on chunk/world loading/unloading and world saving happens on MONITOR priority to reliably ignore cancelled events. Loading of shopkeepers might even happen delayed, to ignore chunks which only get loaded temporarily (like before).
    * Handling of UI closing happens on MONITOR priority now.
  * Event handlers potentially modifying or canceling the event and which depend on other plugins' event handling are called late (HIGH or HIGHEST).
    * Clicks in the trading UI are handled at HIGH priority (like before), so that other plugins have a chance to cancel the trading by canceling the corresponding click event, while still allowing other plugins to react to / ignore the event after we have cancelled it.
    * Interactions with regular villagers (blocking, hiring) are handled at HIGH priority (like before), so that hiring can be skipped if some other plugin has cancelled the interaction.
* Changed: Replaced the 'bypass-shop-interaction-blocking' setting (default: false) with the new setting 'check-shop-interaction-result' (default: false).
* Changed: The new 'check-shop-interaction-result' setting also applies to sign shops now.
* Changed: Added a new 'loadbefore' entry for GriefPrevention as workaround to fix some compatibility issue caused by our changed event priorities and GriefPrevention reacting to entity interactions at LOWEST priority.
* Fixed: Also cancelling the PlayerInteractAtEntityEvent for shopkeeper entity interactions.
* Changed: When forcing an entity to spawn, the pitch and yaw of the expected and actual spawn location are ignored now. This avoids a warning message for some entity types (such as shulkers), which always spawn with fixed pitch and yaw.
* Changed: Some entity attributes are setup prior to entity spawning now (such as metadata, non-persist flag and name (if it has/uses one)). This should help other plugins to identify Shopkeeper entities during spawning.
* Changed: Added setting 'increment-villager-statistics' (default: false) which controls whether opening the trading menu and trading with shopkeepers increment minecraft's 'talked-to-villager' and 'traded-with-villager' statistics. Previously the talked-to-villager statistics would always get incremented and the traded-with-villager statistic was not used.
* Added: The previous, current and next page items inside the editor view will now use their stack size to visualize the previous, current and next page number. This even works for items which are usually not stackable.
* Added: Added the config setting 'register-world-guard-allow-shop-flag' (default: true) which can be used to disable the registration of the 'allow-shop' WorldGuard flag. Note that changing this setting has no effect until the next server restart or full server reload.

API:  
* API: Added interfaces for the different shopkeeper types and their offers to the API. They allow modifying the shopkeepers' trades. Factory methods for the different types of offers are provided via ShopkeepersPlugin and ShopkeepersAPI. The internal shopkeeper classes got renamed.
* API: Added a few utility methods to TradingRecipe for comparing the recipes with given items or other recipes.
* API: Added toString, hashCode and equals to TradingRecipe and the new offer types.
* API: Minor javadoc changes.
* API/Fixed: ShopkeepersAPI was missing getDefaultUITypes.

Internal:  
* Internal: Made some small change that resulted in major performance improvements for handling items moving between inventories (hoppers, droppers, etc.).
* Internal: Slightly improved performance for handling block physics events.
* Internal: Moved some initialization and config loading into the onLoad phase.
* Internal: Avoiding ItemStack#hasItemMeta calls before getting an item's ItemMeta, since this might be heavier than simply getting the ItemMeta directly and performing only the relevant checks on that. Internally ItemStack#hasItemMeta checks emptiness for all item attributes and might (for CraftItemStacks) even first copy all the item's data into a new ItemMeta object. And even if the item actually has no data (Bukkit ItemStack with null ItemMeta), ItemStack#getItemMeta will simply create a new empty ItemMeta object without having to copy any data, so this is still a similarly lightweight operation anyways.
* Internal: Made all priorities and ignoring of cancelled events explicit.
* Internal: Moved code for checking chest access into util package.
* Internal: Moved config migrations into a separate package.
* Internal: Moved some functions into ConfigUtils.
* Internal: Slightly changed how the plugin checks whether the high currency is enabled.
* Internal: Metrics will also report now whether the settings 'check-shop-interaction-result', 'bypass-spawn-blocking', 'enable-spawn-verifier' and 'increment-villager-statistics' are used.
* Internal: Skipping shopkeeper spawning requests for unloaded worlds (should usually not be the case, but we guard against this anyways now).
* Internal: Spigot is stopping the conversion of zombie villagers on its own now if the corresponding transform event gets cancelled.
* Internal: Added a test to ensure consistency between ShopkeepersPlugin and ShopkeepersAPI.
* Internal: Added ItemData tests. This requires CraftBukkit as new test dependency due to relying on item serialization.

Debugging:  
* Debugging: Added new debug command "/shopkeepers yaml", which prints Bukkit's yaml serialization and the item data representation used in the config for the currently held item.
* Debugging: Added entity, invalid entity and dead entity count to shopkeeper's "check" command. Also restructured the output slightly to make it more compact and clearer.
* Debugging: Minor changes to the "checkitem" command. It nows compares the items in main and off hand and also checks if the item in the main hand matches the item in the off-hand.
* Debugging: Small changes and additions to some debug messages, especially related to shopkeeper interactions and shopkeeper spawning.
* Debugging: Added setting 'debug-options', which can be used to enable additional debugging tools.
  * Option 'log-all-events': Logs all events. Subsequent calls of the same event get combined into a single logging entry to slightly reduce spam.
  * Option 'print-listeners': Prints the registered listeners for the first call of each event.

## v2.7.2 (2019-07-02)
### Supported MC versions: 1.14.3

* Bumped Bukkit dependency to 1.14.3.
* Changed: With MC 1.14.3 custom merchants will no longer display the 'Villagers restock up to two times per day' message when hovering over the out of stock icon.
* Changed: Spigot is hiding the unused xp bar from custom merchant inventories now. The dynamic updating of trades (out of stock icon) was adapted accordingly.
* Improved performance of world save handling.
* Slightly improved metrics performance.
* Added debug messages for mob spawn blocking and zombie villager curing.
* Internal: Updated bstats to version 1.5.
* Internal: Retrieving the merchant from the player's open inventory instead of manually keeping track of it.
* Internal: Changed the visibility of UIHandler#isWindow to protected and added UIHandler#isOpen.
* Internal / Plugin compatibility: Handling most custom inventory interactions early (event priority LOW), so that other plugins can ignore cancelled events.
* Internal / Plugin compatibility: Reduced the event priority for the handling of trades from HIGHEST to HIGH. This allows other plugins to still cancel the trading at NORMAL and below priorities, while giving other plugins which run late (like StackableItems) a chance to ignore the event if we are canceling it.

## v2.7.1 (2019-05-30)
### Supported MC versions: 1.14.2

Checkout the changelog of v2.7.0 regarding the update to 1.14.

* Fix: Shopkeeper mobs sensing cache is never cleared, resulting in them not looking towards nearby players sometimes.

## v2.7.0 (2019-05-29)
### Supported MC versions: 1.14.2

**Update for MC 1.14:**
* Dropped support for MC 1.13. This version only supports MC 1.14.2!
* If you are upgrading, make sure that you have successfully updated Shopkeepers to 1.13 first. Migration from older MC versions are not supported and might not work. Reverting to older versions isn't supported either.
  * Removed pre 1.13 sheep color migration.
* Villager shopkeepers:
  * Changed default profession from farmer to 'none'.
  * Priest villagers get converted to 'cleric' and blacksmiths become 'armorer'. Previous regular farmer villagers stay farmers (but they look differently now).
  * Changed the items representing the villager professions in the editor.
  * Note: Wandering traders are not a villager sub-variant, but a different mob type.
* Ocelot shopkeepers of cat types have been converted to corresponding cat shopkeepers.
  * You might have to adapt your players' permissions, since cat and ocelot shopkeepers require different permissions now. With this migration players can end up with cat shopkeepers even though they don't have the permission to freshly create those if they wanted.
  * The different cat types are represented by colored leather armor inside the editor.
* Added separate settings for disabling trading, spawning and allowing hiring of wandering traders.
  * When preventing wandering traders from spawning, trader llamas are prevented from spawning as well.
  * When a wandering trader is hired, its llamas will remain in the world (just like it is the case when the wandering trader dies).
* Internal: Spigot is no longer calling creature spawn events for entities that get spawned as part of chunk generation. If spawning of regular villagers or wandering traders is disabled, these get removed during the corresponding chunk load event now.
* Added: Sign shops can now switch between different wood types.
* Added all new 1.14 mobs to the by default enabled mobs. If you are updating you will have to manually enable them in the config.

Other changes:  
* Changed: When spawning of villagers or wandering traders is disabled, villagers and wandering traders spawned by other plugins, spawn eggs, mob spawners or due to curing zombie villagers are still allowed to spawn now.
* Added a separate setting 'disable-zombie-villager-curing' (default: false) that can be used to prevent curing of zombie villagers.
* Made shop and object type matching more strict. This uses a fixed list of internal aliases now.
* Removed the generic 'sub type' editor option in favor of letting each shop object supply a list of editor options. This allows living shopkeepers to provide multiple editor options now.
  * API: Removed getSubTypeItem, cycleSubType and equipItem from ShopObject. Editor options are internal API for now, and mob equipment hasn't properly worked already before due to not getting persisted.
* Added new mob attribute editor options:
  * All ageable mobs (except the wandering trader and parrots) and all zombies (zombie, husk, drowned, pig zombie, zombie villager): Baby variant. Previously this options was only available for zombie and pig zombie shopkeepers. The editor item for this option is a regular chicken egg now.
  * Zombie villager: Villager profession.
  * Sheep: Sheared state.
  * Cat: Collar color.
  * Villager: Biome type and level.
  * Fox: Variant, crouching and sleeping state.
  * Wolf: Collar color, angry state.
  * Parrot: Color and sitting state.
  * Chested horse (Donkey, Llama, Mule, TraderLlama): Carrying chest state.
  * Horse: Color, style and armor.
  * Llama: Color and carpet color.
  * Panda: Genes (appearance).
  * MushroomCow: Variant.
  * Pig: Saddled.
* Added the ability to cycle the editor options back and forth via left and right clicking.
* Updated for the latest WorldGuard changes. You will have to update WorldGuard for the WorldGuard integration to work.
  * The 'allow-shop' flag got removed from WorldGuard itself and it is left for other plugins to register themselves. Shopkeepers will now attempt to register this flag, if no other plugin has registered it yet (one such other plugin is for example ChestShop). Since WorldGuard only allows registering flags before it got enabled, but we are loading the config at a later point, we will always attempt to register the flag, even if the WorldGuard integration is disabled in the config.
  * Removed: We no longer check for the alternative 'enable-shop' flag, if the 'allow-shop' flag is not present.
* Fixed a class loading issue in case the WorldGuard integration is enabled but WorldGuard is not present.
* Fix: Sign shops no longer temporarily load the chunk when checking if they are active.

Internal changes:  
* Villagers store their profession under 'profession' now. Previous values under 'prof' get imported.
* Fixed a minor internal inconsistency with the updating of trades: Trades were updated on the client, but the server was left in the previous state (mostly affected debug messages).
* Removed special handling of item damage tags from item comparison. Spigot has made some changes that should make this obsolete.
* Moved common de/serialization and validation code of shop object properties into new Property classes.

Config changes (if you are updating, you have to manually apply those changes):  
* Enabled various mobs by default now, which previously had some issues but seem to work fine now: Horse, mule, donkey, skeleton horse, zombie horse, llama.
* Changed the default value for the setting enable-chest-option-on-player-shop to 'true'.

New messages:  
* msg-zombie-villager-curing-disabled
* msg-button-sign-variant
* msg-button-sign-variant-lore
* msg-button-baby
* msg-button-baby-lore
* msg-button-sitting
* msg-button-sitting-lore
* msg-button-cat-variant
* msg-button-cat-variant-lore
* msg-button-collar-color
* msg-button-collar-color-lore
* msg-button-wolf-angry
* msg-button-wolf-angry-lore
* msg-button-carrying-chest
* msg-button-carrying-chest-lore
* msg-button-horse-color
* msg-button-horse-color-lore
* msg-button-horse-style
* msg-button-horse-style-lore
* msg-button-horse-armor
* msg-button-horse-armor-lore
* msg-button-llama-variant
* msg-button-llama-variant-lore
* msg-button-llama-carpet-color
* msg-button-llama-carpet-color-lore
* msg-button-creeper-charged
* msg-button-creeper-charged-lore
* msg-button-fox-variant
* msg-button-fox-variant-lore
* msg-button-fox-crouching
* msg-button-fox-crouching-lore
* msg-button-fox-sleeping
* msg-button-fox-sleeping-lore
* msg-button-mushroom-cow-variant
* msg-button-mushroom-cow-variant-lore
* msg-button-panda-variant
* msg-button-panda-variant-lore
* msg-button-parrot-variant
* msg-button-parrot-variant-lore
* msg-button-pig-saddle
* msg-button-pig-saddle-lore
* msg-button-sheep-color
* msg-button-sheep-color-lore
* msg-button-sheep-sheared
* msg-button-sheep-sheared-lore
* msg-button-villager-profession
* msg-button-villager-profession-lore
* msg-button-villager-variant
* msg-button-villager-variant-lore
* msg-button-villager-level
* msg-button-villager-level-lore
* msg-button-zombie-villager-profession
* msg-button-zombie-villager-profession-lore

Removed messages:  
* msg-button-type
* msg-button-type-lore

Changed messages (if you are updating, you have to manually apply those changes):  
* msg-button-name (lower case words)
* msg-button-chest (lower case words)

## v2.6.0 (2019-03-04)
### Supported MC versions: 1.13.2

Since the changes of this update are manifold and prone to potentially exploitable issues, this update is initially marked as 'alpha' version to see if any issues come up.

**Completely changed the editor interface(s):**
* Unified the trade representation in the editor among all shopkeeper types to be more consistent:
  * The top item is now always representing the result item and the next two items represent from bottom to top the buy items 1 and 2.
  * The only exception to this are the selling and book shopkeeper: Here the high cost item will appear as buy item 2 inside the editor, but as first item inside the trading recipe (like before).
  * The admin and trading shop editors were updated to reflect those changes. This change might initially confuse anyone who is used to the previous admin trades setup (with the result item being at the bottom), but should ultimately be easier to remember due to being consistent among all shopkeeper types.
* It is now possible to switch between up to 5 pages inside the editor, allowing for a total of 45 trades to be setup per shopkeeper.
  * Added settings for the button items used to switch between pages.
* The editor buttons were moved to the bottom row. In the future the additional space may be used for more editor options.
  * The chest button option for player shops will no longer replace the naming button, but they will be both available side by side now.
* Improved the setup of the trading player shopkeepers:
  * Inside the editor, the player picks up items in their inventory and can then freely place copies of those items in the trades section of the editor to specify the traded items. The picked up items will appear on the player's cursor, making the setup more apparent.
  * To make the inventory interaction more fluent, item dragging that only involves a single slot gets interpreted as click.
  * It is now also possible to setup multiple trades for the same result item.
* Visualizing trades that are out of stock:
  * When a trade runs out of stock it gets deactivated, but is still visualized by limiting the trade's remaining uses.
  * The trades get updated for the trading player dynamically after every trade attempt. This will not work in compatibility mode in which case it will behave as before, with trading simply getting cancelled without visual feedback for the user.
  * The player shopkeepers will also keep displaying their setup trades in the editor, even if the corresponding items are no longer present in the chest.
  * Trades of the book shopkeeper for books that are no longer present in the chest will be displayed in the form of dummy books with unknown author and 'tattered' generation.
* An item inside the editor briefly explains the trades setup for the specific type of shopkeeper.
  * Added setting 'trade-setup-item'.
  * Changed the shop type display names to upper case and with 'shop' prefix, since this is now also used for the trades setup item.
  * Slightly changed the admin and book shopkeeper description messages.

Other changes:  
* Changed: Clicking the naming or chest editor buttons will no longer trigger two consecutive saves.
* Changed: The book shopkeeper's behavior more closely matches minecraft's behavior now: Only original and copies of original books can be copied. When copied, the book's generation is increased.
* Changed: The book shopkeeper editor will filter different books with the same title and only consider the first one it finds in the chest.
* Changed: The transfer command will now transfer all shops that use a chest that are owned by the player. Previously it would report 'no permission' if there was a single non-owned shop that was using the same chest. If the player has the bypass permission, all shops will get transferred (like before), regardless of the owner.
* Added: The transfer, remote, setForHire and setTradePerm commands allow targeting the shopkeeper directly now, instead of having to target the corresponding chest.
* Changed: The shopkeeper remote command can now also be used to open player shopkeepers. Also added the alias 'open' for this command.
* Added: 'edit' command to remotely edit shops. Permission 'shopkeeper.remoteedit' (default: op). This works with both admin and player shops. Shops can be specified via argument (id, name, ..) or by targeting a shop or shop chest.
* Added: 'give' command that can be used to give players shop creation items. Permission 'shopkeeper.give' (default: op).
* Added: Allow backwards cycling through shop and shop object types with the shop creation item in hand by left clicking.
* Changed: Improved handling of shopkeeper names with colors and whitespace in commands.
* Changed: Using the shop type and object type display names for the command argument suggestions.
* Fixed: The list command's max page is now at least 1 (even if there are no shops to list).
* Fixed: Missing material settings couldn't be automatically inserted from the defaults.
* Fixed: List messages wouldn't be loaded from alternative language files.

Internal changes:  
* Various refactoring related to the editor code common to all shopkeeper types.
* Small changes related to handling start and end of UI sessions of players.
* As precaution the trading player shop now verifies that there actually still is a corresponding offer setup for the currently traded recipe.
* API: Removed UIRegistry#onInventoryClose(Player)
* API: Calling ShopkeeperEditedEvent on regular editor inventory closes as well now.

This update includes many message changes. It is therefore suggested to let shopkeeper freshly generate all default messages.  

Changed messages:
* msg-creation-item-selected
* msg-shop-type-disabled
* msg-shop-object-type-disabled
* msg-cant-trade-while-owner-online
* msg-trade-perm-set
* msg-trade-perm-removed

Added messages:
* msg-must-target-shop
* msg-must-target-player-shop
* msg-target-entity-is-no-shop
* msg-target-shop-is-no-player-shop
* msg-command-description-remote-edit
* msg-shop-type-admin-regular
* msg-shop-type-selling
* msg-shop-type-buying
* msg-shop-type-trading
* msg-shop-type-book
* msg-shop-type-desc-admin-regular
* msg-shop-type-desc-selling
* msg-shop-type-desc-buying
* msg-shop-type-desc-trading
* msg-shop-type-desc-book
* msg-shop-object-type-living
* msg-shop-object-type-sign
* msg-shop-object-type-npc
* msg-selected-shop-type
* msg-selected-shop-object-type
* msg-must-target-admin-shop
* msg-target-shop-is-no-admin-shop
* msg-shopkeeper-created
* msg-shop-setup-desc-selling
* msg-shop-setup-desc-buying
* msg-shop-setup-desc-trading
* msg-shop-setup-desc-book
* msg-shop-setup-desc-admin-regular
* msg-command-shopkeeper-argument-no-admin-shop
* msg-command-shopkeeper-argument-no-player-shop
* msg-command-description-give
* msg-shop-creation-items-given
* msg-unknown-book-author
* msg-button-previous-page
* msg-button-previous-page-lore
* msg-button-next-page
* msg-button-next-page-lore
* msg-button-current-page
* msg-button-current-page-lore
* msg-trade-setup-desc-header
* msg-trade-setup-desc-admin-regular
* msg-trade-setup-desc-selling
* msg-trade-setup-desc-buying
* msg-trade-setup-desc-trading
* msg-trade-setup-desc-book

Removed messages:
* msg-must-target-chest
* msg-selected-sell-shop
* msg-selected-buy-shop
* msg-selected-trade-shop
* msg-selected-book-shop
* msg-selected-living-shop
* msg-selected-sign-shop
* msg-selected-citizen-shop
* msg-sell-shop-created
* msg-buy-shop-created
* msg-trade-shop-created
* msg-book-shop-created
* msg-admin-shop-created

## v2.5.0 (2019-02-16)
### Supported MC versions: 1.13.2
**Chest protection improvements:**  
* Fixed: Also preventing items being moved into a protected chest. Players might have previously abused this to fill a chest with useless items to prevent the shop from being able to trade due to being out of storage space.
* Fixed: Item movement protection for double chests might not have worked correctly.
* Added: The setting 'prevent-item-movement' (default: true) can now be used to allow item movement from/to the shop chest. This gives the shop owner more possibilities to manage the chest contents, but also opens possibilities for other players to maliciously inject or extract items if this is not properly prevented by other means. If chest protection is disabled, this setting will have no effect (item movement will always be allowed then).
* Reworked chest protection to by slightly less strict:
  * Players with bypass permission can now bypass block placement restrictions when placing blocks next to protected shop chests.
  * Chests next to a shop's chest are now only protected / prevented from being placed if they are actually connected to the shop's chest.
  * Placement of adjacent hoppers is now only prevented if they would able to extract or inject items into the chest.
  * Added: Also restricting placement of droppers in a similar way now.
* Changed: If chest protection is disabled in the config, chest access will not prevented either now. (It doesn't seem to make much sense to prevent chest access but allow the chest to be broken/destroyed)
* Changed: If 'delete-shopkeeper-on-break-chest' is enabled, the shopkeeper will now no longer be removed if the half of a double chest gets broken that is not directly used by the shopkeeper.
* Changed: If 'delete-shopkeeper-on-break-chest' is enabled, the shopkeepers will now also be removed if the chest gets destroyed by an explosion. And if 'deleting-player-shop-returns-creation-item' is enabled, shop creations items will be dropped for those shopkeepers as well.

**Improved shopkeeper placement:**
* Fixed: Treating other air variants as empty as well when trying to place a shopkeeper.
* Slightly changed how the spawn block is determined to match more closely to vanilla minecraft's behavior: If the clicked / targeted block is passable (i.e. tall grass, etc.) this block gets used as spawn location. Only otherwise the spawn location is shifted according to the clicked / targeted block face.
  * This now also allows placement of shopkeepers underwater (Update aquatic)! Signs can still only be placed at air blocks.
* Shopkeeper entities get now spawned at the exact location they would fall to due to gravity (within a range of 1 block below their spawn block, and even if gravity is disabled).
  * This also resolves the issue for shopkeepers periodically 'jumping' if their spawn location is 1 block above a passable or non-full block (such as grass, snow, carpet, etc.).

**Various:**  
* Bumped Bukkit dependency to the latest version of 1.13.2.
* Updated for WorldGuard 7.0.0.
* Pre-loading another class. This might fix an occasional issue during reloads when replacing the jar file.
* Fixed: Creeper shopkeepers' powered state not being applied immediately.
* Fixed: Nearby shopkeeper monsters will no longer prevent sleeping.
* Small performance improvements to related to sign shopkeepers.
* Fixed: No longer randomly spawning baby variants of shopkeeper mobs.
* Increased the built-in max shopkeeper name length from 32 to to 128. The actual name length limit is still further limited by the config settings by default and some shopkeeper object types will not be able to actually display names of those length.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if your are just updating to MC 1.13.x.

## v2.4.1 Beta (2018-09-11)
### Supported MC versions: 1.13.1
* Added: A (very simple) minimum version check.
* Fixed #522: CME during reload when closing shopkeeper UIs.
* Fixed #521: Error when adding a shopkeeper to an existing citizens npc via trait.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if your are just updating to MC 1.13.x.

## v2.2.2 Release (2018-09-11)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Fixed #522: CME during reload when closing shopkeeper UIs.

## v2.4.0 Alpha (2018-08-26)
### Supported MC versions: 1.13.1
**This update brings support for MC 1.13.1:**  
* Support for version 1.13 has been dropped. 1.13.1 is a bug fix release and there should be no reason not to update.

**Internal refactoring related to shopkeeper creation:**  
Shopkeeper creation via command and via item and via API (ShopkeepersPlugin#handleShopkeeperCreation) should be more consistent now.
* Changed/Fixed: Various chest related checks that were previously only run during chest selection are now run during shopkeeper creation again as well (and by that also when creating player shopkeepers via command).
* Changed: Shopkeeper creation via command now takes the targeted block face into account when determining the spawn location. You can now place shopkeepers on the sides of the targeted chest (previously it would always place player shopkeepers on top of the chest).
* Changed: Always preventing all regular shop creation item usage, even when shopkeeper creation fails for some reason, or when selecting shop / shop object types. One can use the shop creation item regularly (if not disabled in the config) by using it from the off hand.
* Removed: Removed the setting 'simulate-right-click-on-command'. Chest access is now always checked as part of shopkeeper creation.
* API: ShopkeepersPlugin#handleShopkeeperCreation now takes various restrictions into account that were previously handled as part of chest selection or as part of shopkeeper creation preparation. This includes chest validation, chest access checks, permissions and enabled-state of shop and shop object types, spawn location validation, and checking for other shopkeepers at the spawn location.
* API: Removed the separate owner from player shop creation data. The creator gets used as owner now. The creator can no longer be null when creating shopkeepers via ShopkeepersPlugin#handleShopkeeperCreation.
* API: Added AdminShopType and AdminShopkeeper interfaces. There may be different types of admin shopkeepers in the future.
* API: ShopCreationData is abstract now. For creating admin shopkeepers one has to use the new AdminShopCreationData instead.

Added and changed (mostly colors) a few messages:
* Added: msg-no-chest-selected
* Added: msg-chest-already-in-use
* Added: msg-no-chest-access
* Added: msg-no-admin-shop-type-selected
* Added: msg-no-player-shop-type-selected
* Changed: msg-selected-chest
* Changed: msg-must-select-chest
* Changed: msg-chest-too-far
* Changed: msg-chest-not-placed
* Changed: msg-too-many-shops
* Changed: msg-shop-create-fail

**Various changes to sign shopkeepers:**  
* Changed: Shopkeeper signs get despawned now with chunk unloads and during world saves.
  * To prevent potential abuse of this temporarily despawning during world saves, players are prevented from placing blocks at the location where the sign is supposed to be. And shopkeeper signs will now replace any block that is at the location the sign tries to spawn at.
  * Changed: Signs that could not be respawned will no longer cause the shopkeeper to be deleted. Instead a 3 minute spawning delay was added to prevent potential abuse (in case the spawned signs drop for some reason/bug).
  * Added a warning if the facing direction of a sign shopkeeper could not be determined or is missing. Since the signs are getting removed now, it will no longer attempt to determine the sign facing from the world now. Instead it will fallback to using some arbitrary default facing direction then.
* Changed: Blocks to which shopkeeper signs are attached to are now protected as well.
* API/Internal: Spawn locations passed to the ShopCreationData are now in the center of the spawn block and contain yaw and pitch to face the creating player. Shop objects can now use this new information if they wish.
* Added: Sign shopkeepers now support sign posts (if the creating player targets the top of a block). The sign is rotated to face towards the creating player.
* Added: The setting 'enable-sign-post-shops' (default true) can be used to disable creation of sign post shops.

**Experimental change related to handling shopkeeper entities:**  
* Using Bukkit's new (experimental) non-persistent entities feature: This should make sure that shopkeeper entities don't get saved to the chunk data during world saves and by that also prevent any kind of duplicate entities issues (assuming it works correctly..).
* As consequence all existing fallback code was removed:
  * No longer keeping track of the uuid of the last spawned entity (this should also reduce the need of periodic saves of shopkeeper data) and no longer searching for old entities before spawning an entity.
  * No longer handling silent / unnoticed chunk unloads and no longer handling entities that got pushed out of their chunk (it's assumed that the non-persistent entities get automatically removed).
  * No longer temporarily despawning all shopkeeper entities during world saves (less performance impact and less visual disturbance for players).
However, in case something doesn't work as expected, this change has the potential to cause entity related issues (thus 'experimental').

**Other changes:**  
* Changed: Citizens shopkeepers are now identified by the citizens NPC's unique ids. Conversion should happen automatically once Citizens is detected to be running.
* Fixed: No longer deleting the citizens NPC when a shopkeeper is deleted due to another shopkeeper using the same NPC.
* Changed (internal): Removal of invalid citizens shopkeepers was moved and gets run now everytime Citizens gets enabled.
* Fixed: There might have been some trading issue related to undamaged damageable items not being considered matching since 1.13.
* Debugging/Changed: Durability is no longer displayed as part of the recipe items debugging information.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if your are just updating to MC 1.13.x.

## v2.3.4 Alpha (2018-08-18)
### Supported MC versions: 1.13
* Fixed: Default config values for primitives were not handled correctly.
* Fixed: Sign facing wasn't applied correctly since the update to 1.13.
* Fixed: The sign physics protection broke somewhere in MC 1.13.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if your are just updating to MC 1.13.

## v2.3.3 Alpha (2018-08-01)
### Supported MC versions: 1.13
* Fixed: Defaults values for missing config values not getting properly added.
* Fixed: Not decolorizing default values which are not meant to be colorized.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if your are just updating to MC 1.13.

## v2.3.2 Alpha (2018-08-01) [broken]
See 2.3.3 instead.

## v2.3.1 Alpha (2018-08-01)
### Supported MC versions: 1.13
* This version relies on the very latest version of Craftbukkit / Spigot. Make sure to **update your server** to the latest Spigot build before running this version!
* Updated link to project website.
* Fixed: Chicken shopkeepers should no longer lay eggs.
* Fixed: Shopkeeper entities should no longer gain potion effects (for any reason).
* Fixed: Player shop type migration broke in v2.3.0.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if your are just updating to MC 1.13.

## v2.3.0 Alpha (2018-07-18)
### Supported MC versions: 1.13
**This update brings support for MC 1.13:**  
**Some important notices to begin with:**
* Support for versions below 1.13 has been dropped. It is only compatible with the latest builds of Spigot 1.13-pre7.
* This update is **experimental**! Don't use it for live servers yet, until any upcoming issue have been fixed.
* Before installing: **Backup your existing shopkeepers data!** This update will make irreversible changes and might not even be able to import all of the previous data.
* **Updating is only supported from version v2.2.1 and MC 1.12.2!** Updating from older minecraft or shopkeepers versions hasn't been tested and might not properly work, because some old migration code has been removed with this update as well. So if you are updating from an older version of shopkeepers OR minecraft, first update to MC 1.12.2 and Shopkeepers v2.2.1.

**Migration procedure:**  
Item data values have been removed and various material (item/block) names have changed to be more in-line with vanilla minecraft names. So this update require a migration of existing configs and shopkeepers data.
* If you use any item ids inside your config (if your config is very old): Those are no longer supported at all and you will have to manually replace them with the corresponding material names prior to running this update.
* Config migration: When being run for the first time (if there is no 'config-version' present in the existing config), this update attempts to convert previous materials and data values, and the shop-creation-item from the config. However, there is no guarantee for this to work for all materials. It will log every migration it performs and might fallback to using default materials. So check the log and the resulting config and make sure everything went fine.
* Shopkeeper data migration: Shopkeeper trades will get converted by Bukkit/Spigot itself. So it's up to them to make sure that this works correctly.
* The plugin will trigger a save of all shopkeepers data, so that any legacy materials that got converted by Bukkit during loading of the shopkeepers will also end up being saved with their updated materials to the save data. Any shopkeepers that cannot be loaded for some reason will skip this procedure however. So make sure that all your existing shopkeepers load fine prior to installing the update, or you might have to update the data of those shopkeepers manually in the future.
* If some materials cannot be converted, the result might be some kind of fallback: Empty spawn eggs for example will be converted to pig spawn eggs by Bukkit. There is no existing documentation available so far regarding which fallbacks there exist.

**Updating is only supported from version v2.2.1 and MC 1.12.2:**
* Removed support for loading very old (MC 1.8) serialized item attributes.
* Removed MC 1.10 villager profession id migrations. 
* Removed MC 1.11 entity type migrations.
* Removed support for importing very old shopkeeper trades (changed somewhere in 2015).

**Other changes related to the update:**
* Removed skip-custom-head-saving setting: Previously this was meant to workaround some issue with saving custom head items (head items with custom texture and no player name) causing corrupted save data. The data corruption / crashing should be fixed by Bukkit by now (by completely dropping 'support' for those custom head items though).
* Added: All 1.13 mobs have been added to the default config. There is no support for them beyond that yet (no support to switch to baby variants, etc.).
* Improvement: The chest and sign protection takes block explosions into account.
* Internal: Reduced the amount of version specific code, since for many things there are Bukkit alternatives available by now.
* Internal: The save data now contains a 'data-version'. This can be used to determine required migrations (not used for that currently), or force a save of all loaded shopkeepers data.

**Other changes:**
* Internal: Sheep colors were previous saved by using the old wool data values. They are now converted and saved by their color names.
* Improvement: Logging a message when importing old book offers (changed during late 1.12.2) and old sheep colors (changed with this update).
* Improvement: Added a warning message when not able to load a cat type or sheep color and falling back to the default in that case.
* Improvement: When testing if a player can access a chest, it now clears the off hand as well during the check (making the check more accurate).
* Improvement: When the plugin is reloaded and the config is missing values, it should now use the actual default values instead of the pre-reload values. (Fallback is still to use the previous values)
* Internal: There have been some formatting changes to the permissions section of the plugin.yml. This gets copied into the wiki now.
* Internal: Using player ids instead of player names for temporary data at a few places now.
* Internal: Minor changes to the internal maven project layout.

## v2.2.1 Release (2018-07-02)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Fixed: Some internal shop object ids have slightly changed in the save data. This update is able to load and convert those old ids.
* Added config validation for enabled living shop types and improved entity type matching.
* Changed: Using the normalized entity type name in the 'selected shop object type' messages.
* Debugging: Added some storage debugging output to the 'check' command.

If you are updating, please read through the changelogs of the previous few versions! They also contain updating hints (ex. regarding changed messages).

## v2.2.0 Release (2018-06-29)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Various changes (and minor internal fixes) related to commands.
  * Displayed command names and aliases don't get formatted into lower case anymore (matching still uses the lower case version).
* API: Minor renaming of a few permission names (only affects the API, the actual permissions are still the same).
* API: Various changes and additions related to shop object types.
  * It is now possible to differentiate between entity and block shop object types, and to get the entity that is currently representing a shopkeeper.
  * Renamed ShopObject#getObjectType() to ShopObject#getType().
  * A few internal shop object ids have changes to be slightly more consistent.

Since there have been no reported issues with the previous beta versions, I mark this version as 'release' to get a few more people to use this new version. If you are updating, please read through the changelogs of the previous beta versions! They also contain updating hints (ex. regarding changed messages). The easiest way to update your config and messages is to remove it and let the plugin regenerate it, and then re-apply you custom changes.

## v2.1.0 Beta (2018-06-18)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
**Major internal changes to the way shopkeepers get created and their data gets saved:**
* Changes to shopkeeper ids: 'Session id' is now named only 'id' , and persists across server restarts / plugin reloads.
  * API: Renamed getShopkeeper-methods to ShopkeeperRegistry#getShopkeeperById and ShopkeeperRegistry#getShopkeeperByUniqueId
* All shopkeeper data gets kept in memory now and during saves, only the data of dirty shopkeepers gets updated.
  * This should come with a large performance improvement if there are many shopkeepers loaded, but only few of them change between saves.
  * Internal/API: Shopkeepers should automatically get marked as dirty, when some of their data gets changed.
  * When legacy shopkeeper or shop object data gets found and imported, the shopkeeper gets marked as dirty.
  * Dirty shopkeeper data gets saved back to file right after plugin start (ex. any imported legacy data).
  * Internal: Various changes to the way the sync saves get handled (ex. during plugin disable).
  * Change: No longer shutting the plugin down if a shopkeeper cannot be loaded. The invalid shopkeeper data should get saved back to file again now, without any information getting lost.
  * Change: No longer using default object type if a shopkeeper with invalid object type is loaded. Instead the shopkeeper gets skipped during loading.
  * Change: No longer defaulting player shops of unknown type to 'normal'. Instead the shopkeeper gets skipped during loading.
* Internal/API: Improvements to the life cycle handling of shopkeepers:
  * Internal/API: The naming and calling of methods and events should be clearer and more consistent now.
  * API: Shopkeepers get properly unloaded now (with removal events) during shutdown and reloads.
  * API: Added ShopkeeperRegistry#createShopkeeper and ShopkeeperRegistry#loadShopkeeper.
  * API: Renamed ShopkeepersPlugin#createShopkeeper to ShopkeepersPlugin#handleShopkeeperCreation to make it more clear that this creates the shopkeeper with player limitations taken into account and the creator receiving messages about a failed shopkeeper creation.
  * Internal: Moved a few things around related to shopkeeper creation by players.
* API: Various changes to all shopkeeper events:
  * API: Various shopkeeper events got slightly renamed.
  * API: Added PlayerCreateShopkeeperEvent that gets called every time a player creates a shopkeeper (including admin shopkeepers).
  * API: Added ShopkeeperAddedEvent that gets called for the creation and loading of all shopkeepers.
  * API: Added ShopkeeperRemoveEvent that gets called for the deletion and unloading of all shopkeepers.
  * API: OpenTradeEvent was removed and replaced with a more generic ShopkeeperOpenUIEvent

**Major restructuring of command handling:**
* All commands come with fancy command completion now.
  * Shopkeeper completions are limited to 30 entries, will prefer shopkeeper names over ids and unique ids, and will suggest only shopkeeper names if an empty argument is given.
* Various command-related message have been changed, removed and added.
* Behavior-wise commands should work like before. But if you notice any issues (especially related to permissions, or argument handling), let me know.

**Other changes:**
* Changed: 'normal' player shopkeeper was renamed to 'selling' player shopkeeper everywhere.
  * The permission node has changed to 'shopkeeper.player.sell' ('shopkeeper.player.normal' keeps working though).
  * Messages related to this have changed.
  * Previous save data should get automatically converted.
* Fixed: Cat shop object not correctly persisting the cat type.
* Changed: Shop object data gets saved in its own config section now. Previous save data should get automatically converted.
* Changed: The id of sign shop objects was changed from 'block' to 'sign'. Previous save data should get automatically converted.
* Added: The max page number to the header of the list command output.
* Fixed: The max page number used by the list command would sometimes be off by one.
* Changed: 'shopkeepers-count' metric now returns '0' instead of '<10' if there are no shopkeepers at all.
* Changed: No longer allowing the creation of shopkeepers at places where a shopkeeper already exists.
* Changed: Minor change to how some of the default permissions are assigned inside the plugin.yml (should not have any noticeable impact).
* API: Added ShopkeeperRegistry#getShopkeepersAtLocation(Location).
* API: Removed Shopkeeper#setLocation from API.
* API: Added ShopkeeperStorage#isDirty().
* API: Renamed 'saveReal()' to 'saveNow()'.
* API: Added Shopkeeper#save() and Shopkeeper#saveDelayed() as shortcuts for marking a specific shopkeeper dirty and requesting a save of the ShopkeeperStorage.
* API: Added saveImmediate() to request a sync (blocking) save.
* Debugging: Added various debugging output related to citizens shopkeepers.
* Debugging: Saving debug messages show the number of dirty and number of deleted shopkeepers that were saved now.
* Internal: Saving aborts any delayed saving task now (less unneeded saves).
* Various internal project restructuring:
  * This should also make it easier for other plugins to depend on Shopkeepers or ShopkeepersAPI.
  * Various classes were moved around again: If your plugin depends on Shopkeepers' API, you may have to update.
* Various other internal refactors.

Due to the various message related changes, you will have to update your messages. The easiest way to do this, it to remove all messages from the config and let the plugin generate the defaults on the next server start.

## v2.0 Beta (2018-06-05)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
**Major change to Shopkeepers' mob behavior:**
* Shopkeeper mobs by default use minecraft's NoAI flag now:
  * This disables various internal AI behavior that would otherwise still be active in the background (villagers for example would periodically search for nearby villages and even cause chunk loads by that).
  * This makes shop entities unpushable (at least on certain MC versions, see below).
* However:
  * In order to keep the look-at-nearby-players behavior, the plugin now manually triggers the minecraft logic responsible for that.
  * In order for the entities to still be affected by gravity, the plugin now manually periodically checks for block collisions below mobs and then teleports them downwards. This doesn't look as smooth as minecraft's gravity motion, but it should suffice (especially since the shopkeeper mobs are usually not falling far from their initial spawn position anyways).
  
**Impact on performance:**
* Shopkeepers only runs the AI and the gravity for mobs that are in range of players (AI: 1 chunk around players, gravity: 4 chunks around players by default).
* You can experiment with the gravity chunk range by tuning the config setting 'gravity-chunk-range'. Setting it lower than your server's entity tracking range might however result in players being able to see mobs floating above the ground until they get closer.
* Internal: The active AI and gravity chunks are currently determined only once every 20 ticks, and falling conditions of mobs are checked only once every 10 ticks (with a random initial offset to distribute the falling checks over all ticks).
* Internal: The shopkeeper mobs get their internal 'onGround' flag set, so that the mobs are properly recognized by Spigot's entity activation range and not getting ticked when far away (see SPIGOT-3947).
  * However, during our simulated 'falling' the flag gets disabled and enabled again at the end of the fall in order to workaround some visual glitch that might otherwise affect players near the entity tracking range (see MC-130725 and SPIGOT-3948).
* Please note: Since at least some AI stuff is now run or triggered by the Shopkeepers plugin, your timings reports will show that Shopkeepers is using quite a bit more of your server's ticking time now. Don't be irritated by that though: I tried to optimize this quite a bit, so hopefully if you compare the performance of your server overall before and after the update, it should in summary even be a small improvement, since lots of unneeded AI and movement logic is no longer getting run.

**Other related changes:**
* Shopkeeper mobs get the 'collidable' flag set on MC 1.9 and above now. Unfortunately this alone will not prevent them from getting pushed around, however it might be beneficial regardless, due them being fully ignored by minecarts, boats and projectiles now.
* You can disable the new mob behavior with the setting 'use-legacy-mob-behavior' (default: false). AI and gravity will then be handled by minecraft directly again. By the way, please use this setting and compare the overall performance of your server with and without the new mob behavior and let me know of your findings!
  * Side note: Spigot added a new setting 'tick-inactive-villagers' (default: true). If you have areas with lots of villagers, and you are using the old (legacy) mob behavior, consider disabling this setting. Otherwise those villagers might cause lots of unneeded additional chunk loads due to their search for nearby villages.
* You are now able to fully disable gravity of shopkeeper mobs via the setting 'disable-gravity' (default: false).
  * This only works on MC 1.10 and later.
  * If you are using the legacy-mob-behavior: Some mob types will always get their AI disabled, and will therefore not be affected by gravity regardless of that setting.
  * With gravity enabled, mobs spawn 0.5 blocks above their spawning position (like before).
  * With gravity disabled, mobs will be spawned exactly at the position they are meant to be. Note however, that if your shopkeeper's spawn location is already above the ground (for example if they were placed on top of grass or other collision-less blocks), those shopkeepers will end up floating above the ground.

**Note on MC version differences:**
* On MC 1.9 and MC 1.10 the NoAI tag does not disable minecraft's gravity and collision handling. Mobs will therefore by default be pushable on those versions, even with the new mob behavior.
* However, if gravity gets disabled on MC 1.10 and above, we are also able to disable collisions/pushing of mobs (by using minecraft's internal noclip flag of entities).

**Various improvements when running on a not yet supported version of minecraft:**
* Fixed: Freezing of mobs via a slowness potion effect didn't properly work. Instead mobs get their movement speed set to zero now via a custom attribute modifier.
* Fixed: The fallback handler supports silencing of entities now.
* Fixed: The fallback handler wasn't able to handle trading recipes with empty second item.
* Fixed: The fallback handler was updated to now support setting the NoAI flag of mobs.
* Note on the new mob behavior: The new AI and gravity handling is not supported when running on unsupported minecraft versions: All entity AI will be prevented (due to the NoAI flag), but no AI replacement will be active, causing the mobs to not be affected by gravity and not look at nearby players.
  * As workaround you may enable the legacy mob behavior in this case then: Minecraft will handle AI and gravity again then. However, this might be unperformant due to lots of unneeded internal AI logic being active then.
  * When using the legacy mob behavior and also disabling gravity, the mobs will additionally not be affected by gravity and they can no longer be pushed around by players.

**Other changes:**
* Changed: The default help page header message now includes the plugin's version. If you are updating, you will have to manually update the message or let it regenerate.
* Added: A new message 'msg-name-has-not-changed' gets sent when a player attempts to change the name of a shopkeeper and the new name matches the old one.
* Fixed: If renaming via item is enabled and the new shopkeeper name matches the old one, no item is removed from the player.
* Added blaze and silverfish to the by default enabled mob types. They seem to work fine with NoAI.
* Minor reordering of the default shop types: 'Buying' is placed between 'normal' and 'trading' now.
* Added bStats metrics: This reports anonymous usage statistics to bstats.org.
  * Besides the general server and plugin information, this also collects Shopkeepers-specific information about: Usage of supported third-party plugins, used Vault economy, usage of various features, total shopkeeper count and the number of worlds containing shopkeepers.
  * All collected information can be publicly viewed here: https://bstats.org/plugin/bukkit/Shopkeepers/
  * You can disable this globally for all plugins on your server by editing 'plugins/bStats/config.yml'. Or you can alternatively also disable this for Shopkeepers only via the setting 'enable-metrics'.
  * Consider keeping this enabled: Having this information available allows me to determine when it is safe to drop support for older minecraft versions, and on which features I should focus development and optimization efforts.
* Documentation: The full changelog of the plugin can now also be found in the repository: https://github.com/Shopkeepers/Shopkeepers/blob/master/CHANGELOG.md
* Debugging: Improved the output of debugging command '/shopkeepers check': It prints information about loaded chunks, and it lists various AI and gravity related timing statistics now. With the arguments 'chunks' and 'active' you can let it print additional information. Some information, that may not fit into the player's chat, may only get printed if the command is run from the console. 
* Debugging: Added world name to chunk load and unload debug messages.
* Fixed/Internal: The license file gets properly included inside the plugin jar now.
* Internal: Added a few plugins as soft dependencies (Vault, WorldGuard, Towny, Gringotts). They will now get reliably loaded before Shopkeepers.

**Major internal restructuring (affects the API) and version bump to 2.0:**
* All functions related to the public API of the plugin have been moved into a separate package, and are mostly behind interfaces now. This will break every plugin currently depending on Shopkeepers, sorry! However, this should make it far easier to differentiate between the public API and the internal API and implementation. This is only the first step towards providing a more stable and therefore more useful public API in the future. The functionality of the API hasn't changed much yet, and things will still keep changing quite a lot, until I have refined all the currently existing functions.
* Various classes were split into abstract base classes (internal API) and interfaces (public API).
* Everything related to shop type and shop object type was moved into the corresponding packages.
* Renamed 'UIManager' to 'UIRegistry' and added an (read-only) interface for it to the API. UIHandler is no longer exposed in the API for now.
* Added interfaces for accessing the default shop types, shop object types and UI types.
* Various functionality was moved outside the plugin's main class into separate classes: ShopkeeperStorage, ShopkeeperRegistry.
* ShopCreationData construction is now hidden behind a factory method.
* Renamed a few primary classes: ShopkeepersPlugin is an interface now, ShopkeepersAPI provides static access to the methods of ShopkeepersPlugin, the actual main plugin class is called SKShopkeepersPlugin now.
* Some restructuring and cleanup related to the internal project structure an how the plugin gets built. The API part gets built separately and can be depended on separately now from other projects.

**Other API related changes:**
* CreatePlayerShopkeeperEvent has changed a bit. It also no longer supports changing the shop type.
* Removed createNewAdminShopkeeper and createNewPlayerShopkeeper and added a general createShopkeeper method instead.
* The getShopkeepersFromChunk method now returns an empty list instead of null, in case a chunk doesn't contain any shopkeepers.
* Added method to get all shopkeepers in a specific world (optionally only from loaded chunks).
* Fixed: The returned shopkeepers-by-chunk mapping is now actually unmodifiable.
* Various javadoc improvements.
* Internal: User interfaces now get requested via the UIType instead of a plain UI identifier.
* Internal: Refactored shop creation and shop types.
* Internal: ChunkCoords fields have to be accessed via getters now.
* Internal: The shop creation data is now guarded against unintended modification.
* Internal: Various other refactoring and cleanup across the project.

## v1.87 Beta (2018-05-17)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
**Major rewrite of the processing of trades:**

Shopkeepers is now implementing the inventory actions that might trigger trades itself.

Previously, trading with player shopkeepers only allowed simple left clicks of the result slot. We predicted the trading outcome (rather simple when only allowing left clicking..) and then let minecraft handle the click like normal. Any other inventory actions (ex. shift-clicks) were prevented, because so far we weren't able to predict the outcome of those.  
I finally took the time to dive into minecraft's and craftbukkit's internals to figure out, how it processes the various clicking types and resulting inventory actions, how those can trigger trades, and how minecraft processes those trades.

Supporting more advanced inventory actions for trading requires at minimum being able to reliably predict the outcome of those inventory actions, and by that already requires re-implementing of large parts of the vanilla logic (which is one of the reasons why this wasn't done so far). The step to also apply those predicted inventory changes ourselves then isn't that much of an additional effort on top, but has a few advantages:  
For one, we can be quite sure that the predicted outcome of a trade actually matches what gets applied in the end. This should reduce the risk of inconsistencies between minecraft's behaviour and our predictions resulting in bugs (also with future minecraft updates in mind).  
Secondly, when relying on minecraft's implementation we are only able to allow or cancel the inventory action as a whole. The shift-clicking inventory action however is able to trigger multiple successive trades (possibly even using different trading recipes). By implementing this ourselves, we are able to apply as many trades as are possible with regards to available stock and chest capacity.

Implementing the trading ourselves has a few advantages, but also comes with caveats:

**Caveats:** 
* Increased risk for bugs which we need to fix ourselves. Increased maintenance and testing effort.
* Increased possibility for inconsistencies between vanilla inventory/trading behaviour and our own implementation.
* Minecraft performs some map initialization when certain map items are created by crafting or via trades: This seems to be responsible for updating the scaling (after crafting a larger map) and enabling tracking (this doesn't seem to be used anywhere inside minecraft though). Since this is only the case if the map item has some special internal nbt data (which shouldn't be obtainable in vanilla minecraft), we currently simply ignore this inside Shopkeepers.

**Neutral:**  
* The usage count of the used trading recipes doesn't get increased anymore. But since these are only temporarily existing and aren't used for anything so far anyways, this shouldn't be an issue.
* The player's item crafting statistic doesn't get increased anymore for items received via shopkeeper trades.
* The player's traded-with-villager statistic doesn't get increased anymore for shopkeeper trades.

I could manually replicate some of these, but since these are custom trades anyways I don't see the need for it right now. But if there is a justified interest in this, let me know and I might add a config option for it.

**Advantages:**  
* Support for more advanced inventory actions when trading with shopkeepers:
  * Currently supported are: Shift-clicking, and moving traded items to a hotbar slot. (Fixes #437)
  * Not (yet) supported are:
    * Dropping the traded item: Exactly reproducing the vanilla behaviour for dropping the traded item is actually rather tricky / impossible with pure bukkit API so far. 
    * Trading by double-clicking an item in the player inventory: The behaviour of this inventory action is rather arbitrary in vanilla already (trades zero, one, or two times depending on the other items in the inventory), and it is currently affected by a bug (https://bugs.mojang.com/browse/MC-129515)
* Simpler API: A single ShopkeeperTradeEvent is called for each trade (might be multiple per click depending on the used inventory action, ex. shift-clicking). The ShopkeeperTradeCompletedEvent was removed. (API breakage, sorry.)
* Fixed: The logging of purchases should now actually be accurate for shift-clicks. It logs each trade individually. (Fixes #360)
* Eventually this might allow for more options for customization of the trading behaviour in the future.

**Potential issue (that has existed before already):**  
* If minecraft adds new inventory actions, those are initially not supported by shopkeepers: If those inventory actions involve clicking on the result slot, they will simply be prevented. If they however allow players to trigger trades by clicking on items inside the player's inventory (similar to the existing collect-to-cursor action by double-clicking), they might introduce bugs which players might be able to exploit.

**Other changes in this update:**  
* Fix: Player inventory changed even though hiring was not successful. (Fixes #493)
* Fix: All inventory operations are now limited to storage contents. This might fix issues with hiring cost items being removed from armor or extra slots.
* Fix: No longer applying color conversion to a few settings. This mostly affects the 'name-regex' setting: Previously, depending on where the '&' was added inside the regex, the regex would become invalid due to color conversion.
* Fix/Improvement: Added config validation for 'tax-rate' (it has to be between 0 and 100).
* Improvement: The collect-to-cursor inventory action is now allowed inside the trading window, as long as the result slot doesn't match the item on the cursor.
* Improvement: Normal player shopkeepers are now slightly smarter when figuring out whether the chest has enough space for the traded currency items, by allowing a variable ratio between high and low currency items.
* Improvement: Buying shopkeepers are now slightly smarter when removing currency items from the chest: It prioritizes low currency items over high currency items, partial stacks over full stacks, and is able to split more than one large currency item and return the excess change to the chest (previously it only converted at most 1 high currency item). This should help keeping the chest more compact.
* Improvement: The logged purchases contain the trading players' uuids now, in parentheses inside the 'PLAYER' column. The overall format of the columns should still be the same.
* Improvement: Decreased delay of inventory updates from 3 to 1 tick (used when canceling inventory clicks inside the trading window).
* Debugging: Minor improvements to debug messages for click events, closing of unexpected inventories and trading recipe information.
* Debugging: Added an experimental way to accurately detect individual non-shopkeeper trades for all types of inventory clicks (by listening for corresponding statistic changes). This can be useful when comparing vanilla trading behaviour with Shopkeeper's new trading behaviour.
* API: The ShopkeeperTradeEvent now directly provides access to the items offered by the player and the order in which they were matched to the used trading recipe.
* API: Providing access to the resulting player inventory contents to the PlayerShopkeeperHiredEvent.
* API: Various javadoc improvements.
* API/Internal: Added new immutable class for representing trading recipes. This also affects users of the ShopkeeperTradeEvent.
* API/Internal: The shopkeeper offers are now immutable. Changing prices requires replacing the offer now.
* API/Internal: Changed how the AdminShopkeeper is handling its offers/recipes. The saving and loading of offers was slightly refactored, but shouldn't impact the current saving format.
* Internal/Fix: Minor refactoring during updating to the new trade handling. This might even fix a few small potential bugs/inconsistencies related to handling of high-currency items.
* Internal/Fix: All general inventory operations now work on content arrays (inventory snapshots) instead of the inventories directly. This might even fix a few severe issues, which went unnoticed so far.
* Internal: Removed 'forHire' boolean value from shopkeepers. Instead a shopkeeper's hiring state is now solely determined by whether or not a non-empty hire cost item has been specified.
* Internal: Added Settings#isHighCurrencyEnabled() and using that now everywhere.
* Internal: Settings#isHighCurrencyItem() now returns false if high currency isn't enabled.
* Internal: Added a debug message for every trade processed by Shopkeepers.
* Internal: Moved trade logging into a separate class and package. It now reacts to ShopkeeperTradeEvents.
* Internal: Some refactoring related to setting up the editor window and handling of editor clicks.
* Internal: Various other small refactorings. Moved logging into util package, and moved item and inventory related utilities into a separate class.
* Internal: Updated the internal TODO list and added a few more ideas.
 
Since the impact of these changes is rather high, I marked this update as 'beta' for now. Please try it out and report back any issues and inconsistencies you encounter.  
Thanks.

## v1.86 Release (2018-01-10)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Fixed: The previous version (v1.85) didn't start. (fixes #490)

## v1.85 Release (2018-01-10)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Added: Debug command (debugCreateShops <count>) to spawn massive amounts of empty admin shopkeepers for testing purposes.
* Internal: Replaced Scanner with InputStreamReader and letting bukkit load the save file contents using that reader.
* Internal: Fully wrapped loading procedure into try-catch-block to catch any types of unexpected issues there, and then disable the plugin. (hopefully helps figuring out the issue behind #485)
* Minor improvement: Logging to console how many shops are about to get loaded.

## v1.84 Release (2018-01-09)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Improvements to saving and loading: Saving first writes to a temporary save file before replacing the old save file. This update improves on that by considering this temporary save file during saving and loading, if no actual save file is found. Previously any existing temporary save file would simple have been ignored and removed during saving and loading, possibly deleting the last valid backup of shop data, if a previous saving attempt already deleted the save.yml file but wasn't then able to rename the temporary save file for some reason.
* Added: An error message being printed to online admins (throttled to at most 1 message every 4 minutes) when saving failed. So that these severe issues do hopefully get noticed and looked into quicker when they occur.
* Minor change: Warning instead of debug message in case a shopkeeper cannot be spawned after creation or loading.
* Improved: Catching any type of issue during shopkeeper loading, and logging them with severe instead of warning priority.
* Minor changes to multi-line warning log messages.
* Various improvements related to async tasks, saving, reloads, disabling:
* Improved: Catching a rare race-condition when trying to register a task with the bukkit scheduler from within an async task while the plugin is currently getting disabled.
* Improved: Waiting (up to 10 seconds) for all currently running async tasks to finish, before continuing to disable.
* Improved: Using synchronization to make sure that only one thread at once attempts to write the save files. Previously this might have caused issues, if an async and a sync save were going on at the same time (due to the plugin being disabled).
* API / Internal: Added a method for requesting a delayed save. Refactored (replaced) the chunkLoadSave task to use this instead now.
* Improved: If saving fails, it will now attempt another save after a short delay.
* Improved: The saving debug output is now slightly more detailed.
* Fixed: Invalid saving debug timings were logged in case a sync save gets triggered (ex. during plugin disable) while an async save is already going on.
* Improved: A bunch of variables now get properly reset on reloads. Not sure though if this might have been an issue previously.
* Improved / Fixed hiring behavior:
* Changed: The hiring window can now always be open, regardless of if the player has the permission to hire this type of shopkeeper.
* Added: Message 'msg-missing-hire-perm' if the player doesn't have the permission to hire shopkeepers at all.
* Added: Message 'msg-cant-hire-shop-type' if the player cannot hire a certain type of shopkeeper (because he is missing the corresponding creation permissions).
* Changed: Message 'msg-cant-hire' now uses a different color.
* Added: Config setting 'hire-require-creation-permission' (default: true) to allow enabling of hiring of shops the player would usually not be able to create himself.
* Added some warning and debug messages when noticing something wrong with the config.
* Removed 'name-item-name' setting: It makes no sense to have a setting for a required display name for the naming item. Especially as the naming button item uses the corresponding translation message text instead.
* Formatted and added documentation comments (including a link to the wiki) to the default config. Those comments will however not be persisted (yet) if the config gets updated (if settings are missing and need to be added).
* Internal: Renamed ChunkData -> ChunkCoords and removed an unneeded API function (for which an alternative is available).
* Internal: Some refactoring related to utility functions.
* Fixed: During chunk unloads the plugin now checks and removes entities belonging to living entity shopkeepers located in other chunks. This fixes an entity duplication issue (#488).
* Improved: The findOldEntity routine was slightly improved to also find entities in neighboring (and loaded) chunks. (This is only useful for situations in which chunk-unload events are not properly called, or world-save events were not properly called and the server crashes. However, it only improves the handling of those cases, it doesn't fully solve them..)

## v1.83 Release (2017-10-10)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Changed: Explicitly built with java 8 from now on.
* Fixed / Reverted: If the display name / lore is empty in the config, the checked item's display name / lore is ignored again. Before it was required to be empty as well.
* Fixed for MC 1.8.x: Custom pathfinder goals of shopkeepers being cleared instead of target goals.
* Fixed: Skipping duplicate trades for the same item when loading shop.
* Changed: Book shopkeepers now store their trades in a section called 'offers' (previous 'costs'), to be consistent with all the other shop types.
* Minor internal refactoring related to shop offers handling for all shop types.
* Fixed: Default config missing by-default enabled mob types for MC 1.12 (illusioner and parrot).
* Fixed: Ejecting shopkeeper mobs right after spawning. Some entities have a random chance to mount nearby entities when spawned, ex. baby zombies on chickens.
* Minor internal refactor related to applying mob sub-types for various living entity shops.
* Added: Support for baby pigman shops. Previously pigman shops would randomly spawn as baby variant, now this can be explicitly specified in the editor menu of the shop.
* Fixed / Re-Added: Ability to specify the required spawn egg type for the shop creation item in the config (default: villager). Can be set empty to accept any spawn egg type. Can be set to something invalid (ex. white space ' ') to only accept the spawn egg without any entity type assigned. This only works for the latest versions of bukkit 1.11 upwards! On MC 1.9 and 1.10 any spawn egg type will be accepted. On MC 1.8 the spawn egg type is specified via the data value.

## v1.82 Release (2017-05-17)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Fixed: The default german translation was missing some messages.
* Changed: Using normalized ids when matching shop (object) types. This should make specifying shop (object) types easier.
* Added: Alias 'mooshroom' for mushroom cow.
* Fixed: Prevent shopkeepers from getting teleported through portals.
* Changed / Fixed: Also cancelling entity targeting if the source entity is a shopkeeper.
* Fixed: Updating player inventory when interacting with a shopkeeper entity. This might be needed if the entity is an animal which can be fed.

**Update for MC 1.12:**  
* The update is for MC 1.12-pre2, but will probably work as well for MC 1.12 once that is released.
* Parrots and illusioners seem to work fine and were added to the default enabled shop types.

## v1.81 Release (2016-11-24)
### Supported MC versions: 1.11, 1.10, 1.9, 1.8
* Fix: Error when trying to save trading recipe of trading shopkeeper.

## v1.80 Release (2016-11-22)
### Supported MC versions: 1.11, 1.10, 1.9, 1.8
* Added: Settings for prefix and default trading window title.
* Change: When getting a recipe from admin shop editor window with no first item, we try to use the second item as first item instead of completely disregarding the recipe. Similar to how the trading shopkeeper handles this.

**Internal refactoring and cleanup:**  
* Removed separate handling of villager AI. Their AI is now replaced in the same way as for any other living entity shop.
* Major change: Using new bukkit api for opening virtual merchant inventories and getting the used trading recipe. This is only available in the very latest version of bukkit for MC 1.11, so **if you are already using Spigot 1.11, you need to update to the most recent version**.
* Changes/Fixes: All empty itemstacks related to trading recipes should now be replaced with null everywhere (similar to the pre-1.11 behavior). This also means that trading recipes with empty second item should no longer save this as an item of type AIR, but instead omit this entry all together.
* Changes: More consistent handling of empty itemstacks everywhere in the code.
* Minor code cleanup at various places.

## v1.79 Release (2016-11-18)
### Supported MC versions: 1.11, 1.10, 1.9, 1.8
**Update for MC 1.11:** 
* Minecraft represents the different variants of skeletons (stray, wither_skeleton) and zombies (zombie_villager) now in the form of different entity types. Shopkeepers using these variants get automatically converted to use the new mob types instead.  
  This however means that you cannot switch back to previous minecraft versions without loosing those converted shopkeepers.  
  And you will have to give all your players, which were previously able to create zombie or skeleton shopkeepers the required permission nodes for stray, wither_skeleton and zombie_villager, otherwise they won't be able to access/edit their shops after this conversion.  
  Also, if you are running on a minecraft version below MC 1.11, you can no longer cycle through the different zombie variants and skeleton variants and existing shopkeepers using those will fallback to normal zombies and skeletons.
* Added the new mobs from MC 1.11 (evoker, vex, vindicator). Llamas, sadly, act weirdly currently when clicked (same goes for all horse variants) and are therefore not included in the default list of enabled mob types. In case you are updating, you will have to let the plugin recreate this config entry (by removing the entry or the whole config) or manually add those new mobs in order to enable them.
* Added: Support for the green 'Nitwit' villager variant added in MC 1.11.
* Some minor changes related to handling empty inventory slots and itemstacks, especially when creating the trading recipes, were required.

**Other changes:**  
* Added a bunch of old mobs, which seem to mostly work fine, to the by default enabled living shop types. Also reordered this list a bit (roughly: friendly mobs first, hostile and new mobs at the end).
* Added: Instead of cycling between normal and villager variant, zombie shopkeepers can now change to baby variant by clicking the spawn egg in the editor window.
* Change: No longer disabling other, normal villagers by default.
* Fix: Ignoring double left clicks in editor window when adjusting item amounts. Previously this would cause 2 fast left clicks to be counted as 3 left clicks.
* Fix: Also ignoring double clicks when cycle shopkeeper object variants in editor window.
* Fixed: The 'compatibility mode' fallback, which is run if no supported minecraft version is found, wasn't able to open the trading window for players.

**Known Caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older bukkit versions is untested. If you encounter any problems let me know and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.
* In the latest MC 1.8.x versions default minecraft trading logic has slightly changed (and by that those of shopkeepers as well): if a trade requires an item with special data (like a custom name, etc.) minecraft is now only allowing this trade, if the offered item is perfectly matching, including all special item data and attributes.
* MC 1.9 has changed how the different spawn eggs are differentiated internally. All spawn eggs now have the data value 0 and there is no API in bukkit yet which would allow me to check which type of spawn egg a player is holding. In order to get the shop creation via item working for now, you will have to either change the data value of the shop creation item to 0 (which will however let ALL types of spawn eggs act like the creation item), or you change the creation item type all together.
* Shopkeepers using skeleton or zombie variants after updating to MC 1.11 or above can not be loaded again (will be lost) when switching back to a previous minecraft version.
* If you are running on a minecraft version below MC 1.11, you can no longer cycle through the different zombie variants and skeleton variants. Existing shopkeepers using those will fallback to normal zombies and skeletons.

## v1.78 Release (2016-10-24)
### Supported MC versions: 1.10, 1.9, 1.8
* Readded 'use-strict-item-comparison' to default config as well.
* Fixed: Decolorization of string lists. This should also fix an issue with default string lists in the config not getting properly initialized if missing in the current config.
* Fixed: Shop type displayed in 'shop object type disabled' message, instead of shop object type.
* Added: Checking if player has permission to remove the villager before allowing hiring by calling a fake damage event and checking if any plugin on the server is cancelling it. (Contributed by Intangir)
* Added: Dropping shop creation item when 'deleting-player-shop-returns-creation-item' is enabled and a shopkeeper gets removed due to breaking of the shop chest. (Contributed by Intangir)
* Added: Option to allow opening of the player shop chest inventory from the shopkeeper's editor window (might be helpful on survival servers where the chest is hidden/protected). In order for this option to be available you have to enable 'enable-chest-option-on-player-shop' in the config. However, this option will replace the naming option in the editor view. (Contributed by Intangir)
* Added: Option to allow naming of shopkeepers by right-clicking them with the naming item. Setting: 'naming-of-player-shops-via-item'. This will disable the naming option in the editor view. (Contributed by Intangir)
* Changed the default naming item to name tag. (Contributed by Intangir)
* Changed the default zero currency items from slime balls to barrier items.
* Reordered the settings in the default config slightly.
* Changed: No longer removing monsters which are accidentally attacking a shopkeeper entity (ex. wither explosions).
* Changed: When setting item amount with hotbar button, use the actual number key value instead of key value - 1.
* Some small internal refactoring at various places. Among others, related to handling of updating item amount when clicking items in the editor view.

## v1.77 Release (2016-07-14)
### Supported MC versions: 1.10, 1.9, 1.8
* Fix: If 'protect-chests' is disabled, it should no longer prevent breaking of shop chests. Opening of shop chests will still be prevented though.
* Added 'use-strict-item-comparison' setting back in (default: false). It seems that even with the trading changes in MC 1.8 there are still some cases in which minecraft ignores certain item attributes.
* Reimplemented chest protection: Now it considers all shopkeepers, even currently inactive ones. This might be of use in the edge case of player shopkeepers being in a different and somehow unloaded chunk than the corresponding chest. This should also fix the major issue that chest are temporarily unprotected after world saves due to the temporarily deactivation of shops. And this might bring slight performance improvements if you have lots of shopkeepers in loaded chunks.
* Changed: Only allowing chest placement near to protected chests, if the chest can also be broken again.
* Changed: Chest protection in different situations should now be more consistent. Adjacent protected chests are now considered in all situations in which we check for chest protection.
* Decreased refresh delay on world saves from 2 ticks to 1. 

If you notice any issues, especially with the changes regarding chest protection, let me know.

## v1.76 Release (2016-06-11)
### Supported MC versions: 1.10, 1.9, 1.8
* Fixed: Default villager profession was missing in last update (villager shopkeeper creation should work again now).

## v1.75 Release (2016-06-10)
### Supported MC versions: 1.10, 1.9, 1.8
**Backup your save file. If you run this version you cannot switch back to previous versions due to changes in the way things get stored in the save file.**  
* Updated for MC 1.10.
* Due to some villager profession changes in bukkit, data about villager professions gets converted to a new storage format (stored by name instead of id) (not backwards compatible!).
* Using bukkit's new API for setting entities silent on MC 1.10.
* Added POLAR_BEAR to default config.
* Added support for the new Stray skeleton type (represented by MHF_Ghast player head currently.. which doesn't work perfectly, but better than nothing for now).
* Minor changes to the compatibility mode, which didn't work for the past few minecraft updates. 
* Removed support for MC 1.7.10.
* Removed 'use-strict-item-comparison' setting (minecraft is performing a strict item comparison already itself since MC 1.8).
* Removed uuid conversion related code: PlayerShops are expected to have a valid owner uuid by now.
* Slightly refactored handling of issues during shop creation and loading, so that shopkeepers with invalid data (ex. missing owner uuids) can now communicate to the loading procedure that the loading failed (which then prints a warning and continues). Shops which failed to load might get overwritten the next time shopkeepers saves it's data. 
* Fixed: Also replacing settradeperm to all lower-case in the wildcard permission.
* Fixed: The location strings for shopkeepers in debug messages should now be correct. 

If you run into any issues let me know.

## v1.74 Release (2016-05-15)
### Supported MC versions: 1.9, 1.8, 1.7.10
* Fixed: Chest contents should no longer get modified if the trade fails at a later stage.

## v1.73 Release (2016-05-10)
### Supported MC versions: 1.9, 1.8, 1.7.10
* Updated for MC 1.9.4

## v1.72 Release (2016-05-01)
### Supported MC versions: 1.9, 1.8, 1.7.10
**This update contains a change, which might require you to update your permissions.**  
* Changed the setTradePerm permissions to 'shopkeeper.settradeperm' (all lower-case), to match the other permissions and potentially cause less trouble with certain permissions plugins.
* Fixed a typo in the default config: 'msg-trade-perm-removed' instead of 'term'.
* Fixed an issue related to the shopkeeper citizens trait when either shopkeepers or citizens are not running: Unregistering shopkeeper citizens trait whenever shopkeepers or citizens get disabled. And added an additional check to ignore shopkeeper citizens trait attachments if shopkeepers isn't currently running (just in case).
* Removed the restrictions of not being able to open shops while holding a villager egg in the hand by slightly changing the way the villager trading menu gets opened for past minecraft versions.
* Removed the no longer used 'msg-cant-open-shop-with-spawn-egg' message.
* Fix: The remote command should now work again.
* Added: Preventing shopkeepers from entering vehicles.
* Improved filtering out of faulty trading offers in the saved data.
* Fixed: The shop creation item, the hire item and the currency item get now automatically set to their defaults if they are invalidly set to AIR in the config. The (high) zero currency items should now even work in the player shop editors if being of type AIR.

## v1.71 Release (2016-04-07)
### Supported MC versions: 1.9, 1.8, 1.7.10
**This update contains a few changes, which might require you to update/reset your config and language file.**  
* In MC 1.9 all spawn-eggs use the data value 0 now. The default shop creation item data value was changed to 0 for now. This will cause all spawn-eggs to function as shop creation item by default. There is no new option yet to limit the shop creation item to the villager egg.
* Replaced 'disable-living-shops' with 'enabled-living-shops'. You might have to update your config due to this, however it has the advantage that whenever new mob types, which are disabled by default, are added to shopkeepers in the future, they don't automatically get enabled on servers which already run shopkeepers (which already have an old config which doesn't list those new mob types as disabled yet).
* Did some internal refactoring, mostly related to living shop types: Shopkeepers is now automatically 'supporting' all living entity types available for your server version for being used as shop entities. However, not all entity types will properly work: If you want to try out on your own risk if a living entity type works as shop entity, you will have to manually enable it in the config.
* Added RABBIT to the by default enabled living shop entity types.
* Slightly changed the default shopkeeper list entry message, to display the shopkeeper's session id instead of the list index. Also the colors where slightly changed to make it better readable.
* Added some german translations for some command argument names to the included default german translation.
* Added setTradePerm command: This allows setting of a custom permission for individual admin shopkeepers, which is then required for being allowed to trade with the affected shopkeeper. Also added a few new messages related to this.
* Also added a new message which gets sent to players which don't have the regular trade-permission. By default the message text is the same as the message being sent if the player is missing the newly added custom trading permission, though those can be changed in the config independently.
* The 'remote' command now also accepts a shop's unique id or session id (instead of the shop name).
* The default 'unknown-shopkeeper'-message was changed to include the 'unknown id' fact.
* No longer manually saving item attributes for servers running on MC 1.9 upwards. Since somewhere in late 1.8 bukkit started saving attribute data as part of their 'internal' data. Attribute loading is left in for now, in case someone updates from a bukkit version which didn't save attributes yet. This should also fix the issue of the new Slot attribute added in MC 1.9 being now properly saved.
* Slightly changed the saving procedure which will hopefully make it less likely that all your save data gets lost if something goes wrong during saving: The data gets now first saved to a different, temporary file (in the shopkeepers plugin folder). Only if saving was successful, the old save file gets removed and replaced by the new one. Also added a bit more debugging information to the related messages.
* Added a possible workaround for the 'duplicating villagers' issue some people still seem to have sometimes:
* My current guess on the problem is, that: The shopkeeper entities get saved to disk when the world gets saved. Shopkeepers removes a shop entity, respawns the entity (this replaces the old 'last-entity-uuid'). This does not necessarily have to happen at the same time or for all entities, but it does occur when shopkeepers is for ex. getting reloaded. Now if the server closes (ex. due to a crash) without another world save taking place, the old shop entity is still saved in the chunk data of the world, but we cannot identify it as shop entity on the next server start, because we remembered the entity-uuid of the last entity we spawned, which is not the same as the last entity which was saved to disk.
* This workaround: Whenever the world gets saved (at least whenever WorldSaveEvent gets triggered) all shopkeepers get temporarily unloaded (and by that removed from the world) and loaded (respawned) again shortly after the world-save. With this the shop entities will hopefully no longer get stored to the chunk data. You will notice a short flicker of shop entities, but trading etc. should keep working, even during world-saves. If you have debug-mode enabled in the config, you will get a rather large block of debugging output everytime all your worlds get saved.

## v1.70 Release (2016-03-08)
### Supported MC versions: 1.9, 1.8, 1.7.10
* Ignoring off-hand interaction events at various places. The shop creation item will only act as such if it is held in the main hand. In the off-hand it will act like the normal item (and regular usage is prevented like normal via the 'prevent-regular-usage' setting).

## v1.69 Release (2016-03-01)
### Supported MC versions: 1.9, 1.8, 1.7.10
* Updated for MC 1.9.
* Removed support for nearly all versions of MC 1.7. Only v1.7.10 is still supported. 
* Added setting 'skip-custom-head-saving' (default: true), which allows turning of the skipping of custom player head items (player heads with custom texture property and no valid owner) during saving of shopkeeper trades. Note: Best you keep this enabled for now, because disabling this on a server which does not properly support saving of custom player head items, can cause all your saved shopkeepers to get wiped. On newer versions of spigot the wiping of saved shopkeepers might be fixed, however custom player head items are still not properly supported by spigot, so they will basically get saved like if they were normal player head items.
* Fixed: No longer considering normal player heads without any owner as 'custom' player heads
* Added: Shopkeepers now get assigned a 'session id', which is much shorter than the unique id, but is only valid until the next server restart or the next time the shopkeepers get reloaded from the save file. Those ids are currently unused, but might get used in the future to let players (admins) specifiy a shopkeeper in commands via these shorter ids. 

**Notice about an issue with MC 1.9:**  
MC 1.9 has changed how the different spawn eggs are differentiated internally. All spawn eggs now have the data value 0 and there is no API in bukkit yet which would allow me to check which type of spawn egg a player is holding.  
In order to get the shop creation via item working for now, you will have to either change the data value of the shop creation item to 0 (which will however let ALL types of spawn eggs act like the creation item), or you change the creation item type all together.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older bukkit versions is untested. If you encounter any problems let me know and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.
* In the latest MC 1.8.x versions default minecraft trading logic has slightly changed (and by that those of shopkeepers as well): if a trade requires an item with special data (like a custom name, etc.) minecraft is now only allowing this trade, if the offered item is perfectly matching, including all special item data and attributes.
* MC 1.9 has changed how the different spawn eggs are differentiated internally. All spawn eggs now have the data value 0 and there is no API in bukkit yet which would allow me to check which type of spawn egg a player is holding. In order to get the shop creation via item working for now, you will have to either change the data value of the shop creation item to 0 (which will however let ALL types of spawn eggs act like the creation item), or you change the creation item type all together.

## v1.68 Release (2015-12-21)
### Supported MC versions: 1.8, 1.7
* Small change to the logging format: Now including the shopkeeper id and the amounts of currency 1 and currency 2.
* Small improvement: Now spawning citizens npc's in the center of the block, similar to the other living entity shops.

## v1.67 Release (2015-11-28)
### Supported MC versions: 1.8, 1.7
* Fix: No longer consider normal mob heads to be 'custom' heads. So they should no longer get excluded from saving due to the changes of the last version.

## v1.66 Release (2015-11-25)
### Supported MC versions: 1.8, 1.7
* Fix: Added a temporary check to skip saving of trades which involve a custom head item, which would otherwise currently cause a wipe of the complete save file due to a bug in bukkit's/spigot's item serialization.

## v1.65 Release (2015-10-25)
### Supported MC versions: 1.8, 1.7
* Dropped support for MC versions 1.6.x.
* The WorldGuard-restrictions feature now requires WorldGuard 6.1+ and might therefore not work on certain older versions of MC 1.7.x.
* Fixed: Due to the last update the WorldGuard-restrictions didn't work if run on MC 1.8 with WorldGuard 6.1. It should work again now.
* Added require-world-guard-allow-shop-flag setting (default: false): With this setting AND WorldGuard restrictions enabled, player shops will be allowed ONLY in regions with WorldGuard's allow-shop flag set.

## v1.64 Release (2015-10-24)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny change: If an exception is catched while a shopkeeper gets saved, we now skip this shopkeeper and print a warning.
* Added some more debug output when processing inventory clicks.
* Improved the WorldGuard hook (untested though!): Players should now also be allowed to place a shopkeeper if the region they are in has WorldGuard's ALLOW_SHOP flag set (regardless if they can build in that region). However, in order for players to provide the supplies for their shopkeeper they will still require chest access. So make sure that your WorldGuard region also has the appropiate flags set to allow that.

## v1.63 Release (2015-07-18)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Now really fixed: Citizens npc's with the shopkeeper trait should now keep working after a reload via '/citizens reload'.
* Improvement: Now reacting to Citizens getting enabled or disabled dynamically (in case this is possible, via plugin managers or similar).
* Tiny internal changes. 

## v1.62 Release (2015-07-17)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Improvement: If an error is catched during loading of shopkeeper data, the plugin gets disabled without triggering a save. This hopefully prevents that the complete shopkeeper data gets lost if for example some item cannot be properly loaded.
* Change: Also disable without save if one shopkeeper cannot be loaded (for ex. because of invalid save data). Previously that shopkeeper would have been skipped, resulting in that shopkeeper being completely removed. Now you have to manually fix your save data in this case, but with the advantage of being able to fix the affected shopkeeper without losing for example all the setup trades.
* ~~Fixed: Citizens npc's with the shopkeeper trait should now keep working after a reload via '/citizens reload'.~~ (Not actually fixed, check out the next version..)
* Added: Automatically removing invalid citizens shopkeepers, which either have no backing citizens npc, or are using the same npc as some other shopkeeper for some reason.
* A few minor other internal improvements related to citizens related code (like checking if Citizens is currently still enabled before accessing or creating npcs).
* Tiny improvement: Not triggering a save during removal of inactive player shopkeepers, if no shopkeepers got actually removed.
* Improvement: Hopefully better handling shopkeepers with somehow invalid or duplicate object id during activation and deactivation (preventing that the wrong shopkeeper gets disabled for example), and printing some warnings if a shopkeeper is detected during activation with an somehow invalid object id. This mostly has debugging purposes currently.
* A few tiny other internal changes, which hopefully didn't break anything.

## v1.61 Release (2015-06-24)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Slight changes to handling saving: If there is a problem during the saving it waits a bit and then retries several times. This might help if for some reason the save file can only temporary not be written to (for example because of some other application, like Dropbox, currently locking the file). It also checks for some other possible causes of issues, like if the parent directory is missing (if it is, it attempts to create it). It also checks if it currently can write to the save file, before removing the old save file. These changes hopefully prevent that certain issues cause the save data to go completely lost. Also it prints some more information to the log if it detects some issue.
* Fixed: A bug in minecraft's trading allowed players to exploit certain trade setups (only very few trade setups are affected though). Shopkeepers is now detecting and preventing those affected trades.
* Added (API): The ShopkeeperTradeEvent now holds information about the used trading recipe, and the ShopkeeperTradeCompletedEvent holds a reference to the original ShopkeeperTradeEvent.
* Added (API): A bunch of javadoc was added or updated for the available events.

## v1.60 Release (2015-06-12)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Allways allow editing of own shopkeepers, even if a player no longer has the permission to create shopkeepers of that type.
* Fixed: Skipping empty recipes for admin shopkeepers during loading.
* Changed: If a player cannot create any shopkeeper at all because of missing permissions, the no-permission message is printed instead of the creation-failed message. Also chest selection and shop and object type cycling is skipped. And no message is printed when the player selects the shop creation item in the hotbar.
* Change/Possible fix: Triggering a save if the teleporter task had to respawn a shopkeeper object (like an entity). This should make sure that the latest entity uuid gets saved.
* Change: No need to update a sign shop another time if it had to be respawned.
* Internal change: Moved a few more permissions node into ShopkeepersAPI.
* Added: API method for checking if a player can create any shopkeeper at all.

## v1.59 Release (2015-06-08)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Updated to latest version of citizens.

## v1.58 Release (2015-06-06)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Some internal cleanup, moving around of things and other minor changes. Hopefully all is still working properly.
* Possible fixed: Some minor cleanup did not take place if a shopkeeper entity was respawned, or when activating a shopkeeper on chunk load.
* Changed/Fixed: The permissions node for the help command was added to the plugin.yml and now defaults to 'true' instead of 'false'.
* Changed: shopkeeper.remote permissions node default value was changed from 'false' to 'op'.
* Added: shopkeeper.* permissions node, which includes all other permissions nodes.
* Added: Debug information whenever a permissions node is checked for a player. Might be a bit spammy though.
* Changed: Handling of world unloads was slightly changed to reduce the amount of duplicate code. It now triggers the chunk unload code for all currently loaded chunks. Hopyfully without causing issues.
* Changed: Now checking the hire item name and lore earlier already. If they don't match the player is informed that the villager can be hired (just like when clicking a villager with any other item), instead of telling him that hiring failed (like when the player does not have the needed amount of hiring items).
* Fixed: The code which checks for chest access when creating a player shopkeeper via command does no longer trigger chest selection if the player is holding a shop creation item in hand.
* Fixed: Previously sign shops weren't properly created if created via command. The initial sign wasn't placed and the shopkeeper was stored at the wrong location.
* Changed: Shop creation via command will fail now if a block face is targeted at which either a non-air block is, or if no wall sign can be placed there.
* Added/Fixed: Canceling block physics for shop signs, so they should no longer break in various situations. Signs might end up floating in mid-air though.
* Added/Fixed: Preventing entity explosions from destroying shop signs.
* Added/Fixed: Preventing entity explosions from destroying shop chests.
* Added/Fixed: In case we detect that a shop sign went missing, we attempt to respawn it. If the sign respawning fails for some reason, we completely remove the shopkeeper and print a warning to console. Previously the sign shopkeepers would no longer be usable nor accessible from inside of minecraft, but still be secretly existing in the save data.
* Added: Sign shops should now store their initial sign facing, which is used for sign-respawning. For already existing sign shops it is attempted to get the facing from the current sign in the world.
* Fixed: Shop signs are now updated at least once when their chunk gets loaded, so that config changes which affect the sign content are applied to the already existing signs.
* Fixed: Also requesting a sign update whenever the owner or owner name changes, so that the owner name on the sign gets updated.
* Added: Whenever a player switches to the shop creation item in the hotbar, a message explaining the usage of it is now printed.

## v1.57 Alpha (2015-05-25)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Internal change: Removed own trading-recipe-finding code, and instead getting the reciped used in the current trade from minecraft directly. With this it should be guaranteed that we really get the same recipe as minecraft is actually using. Also we have less work for keeping it updated.
* Internal change: The code which adds and removes an item stack to/from a chest was changed. Adding an item stack should now try to first fill up existing item stacks before starting to create new ones.
* Fixed: The trading and buying player-shopkeepers are now adding those items to the chest, which were actually used by the trading player. Depending on item comparison of minecraft and shopkeeper settings those don't have to perfectly match the required items from the used recipe, but are still accepted.
* Fixed: The owner name for player shopkeepers was initially (on creation) getting stored in lower case for some reason in the past. This probably caused issues at some places when we were comparing the owner name to the name of a certain player, for example when using the remove command.
* Tiny change/fix (not sure if this was actually an issue): Skipping npc players now when doing some initialization with the online player when the plugin gets enabled.

See also the changelog of the previous version!

## v1.56 Alpha (2015-05-23)
### Supported MC versions: 1.8, 1.7, 1.6.2
This version is marked as ALPHA, because it is including a bunch of internal changes which weren't properly tested yet and which might especially affect trading and saving of shopkeeper data. This version is currently only meant for testing purposes and finding potential issues.

Please create a backup of your shopkeeper save.yml file before using this version.  
You might especially want to check if all shopkeeper trades get properly loaded and saved, and if the setup of shopkeepers, trading and item removal and disposal in/from player-shop-chests is working properly.  
If you discover any issues please let me know by creating tickets for those.

* Removed custom item comparison, which was used in the past to only allow trades if the required and the offered items perfectly match, excluding internal attribute uuids. This is now useless, because of changes in minecraft's 1.8.x trading logic.
* Unfortunately detailed debug messages, telling exactly why certain items are not similar, are no longer possible with this change.
* Updated to minecraft's new 1.8.x trading logic:  
  Previously minecraft did unlock a trade if the type of the offered items matches the required item. Now in MC 1.8.x if a trade requires an item with custom data, such as custom name, lore or attributes, minecraft only unlocks the trade if the offered item perfectly matches the required one (all the item's internal data and attributes have to be the same).  
    If the required item has no special data, then any item of the same type, even items with special data, can be used for this trade.  
    Also, minecraft sometimes unlocks a non-selected trade, if the required items of a different trade is matching the ones which are currently inserted.

Shopkeepers is now trying to allow most of those trades as well. So if a trade requires an item without special data, every item of that type is accepted. If an item with special data is required, only perfectly matching items are accepted.

* Added setting use-strict-item-comparison setting, which defaults to true. When this setting is active trading behavior is nearly the same as before: An additional and more strict item comparison is performed before a trade is allowed, which makes sure that the items offered perfectly match those of the used recipe. So even if the required item has no special data, only items of the same type which as well don't have special data are accepted. Otherwise the trade gets blocked.  
  This setting's default is currently set to true, to prevent potential issues with already setup shops, which expect the old/current trading behavior. Also servers running on MC versions below 1.8.x might depend on this setting to be active.  
  For servers using the latest server versions I recommend disabling this setting, so that the trading logic better matches the one of minecraft. 
* Changed: Resetting the player's selected chest after successfully creating a player shopkeeper. This seems to be more intuitive.
* Fixed: the default shop-created message for the buying shopkeeper did say that one of each item which is being 'sold' has to be entered into the chest. Instead it was meant to say 'one of each items being bought'.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older bukkit versions is untested. If you encounter any problems let me know and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.
* In the latest MC 1.8.x versions default minecraft trading logic has slightly changed (and by that those of shopkeepers as well): if a trade requires an item with special data (like a custom name, etc.) minecraft is now only allowing this trade, if the offered item is perfectly matching, including all special item data and attributes.

## v1.55 Release (2015-05-18)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Actually include the update for 1.8.4. 

## v1.54 Release (2015-05-18)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Color codes should now actually get converted.. 

## v1.53 Release (2015-05-17)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Code which requires searching for nearby entities wasn't properly searching in negative z direction previously.
* Updated to latest spigot build: now comparing the stored block state of items (untested)
* Fixed: Missed applying the creation item lore from the config to the creation item.
* Fixed: Missed applying color code conversion to the shop creation item name and lore.
* Internal change: Color code conversion of text is now done once when the config is loaded, instead of everytime the colored text is needed. If you encounter any issues with color codes not being properly converted at certain places, let me know.
* Updated to support spigot's 1.8.4 build. 

## v1.52 Release (2015-03-15)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny cosmetic fix: Print 'yes' instead of 'null' for the checkItems debug command, if the compared items are similar.
* Fixed: Banner meta comparison now compares base colors as well.
* Fixed: Banner meta comparison should now actually work. Before it reported that the patterns of two banners are similar, when they weren't, and that they are different, when they were actually similar. 

## v1.51 Release (2015-03-13)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Improved/Fixed: sometimes villagers were still able to turn into witches by nearby lightning strikes. The radius in which lightning strikes around villager shopkeepers get prevented was increased from 4 to 7 blocks.
* Fixed (workaround): occasionally an error did pop up for some users during thunder storms due to entities reporting to be in a different world than we are expecting them to be.
* Fixed: Player shops should now identify shop owners solely by name again if running on versions which don't properly support player uuids yet (below 1.7.10, I think). Untested though.
* Changed: We no longer remove all already stored owner uuids when switching from a bukkit version which supported player uuids to an older versions which doesn't. The owner uuids are not used on that older bukkit version, but we still keep them for the case that we switch back to a newer version of bukkit. 

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older bukkit versions is untested. If you encounter any problems let me know and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.

## v1.50 Release (2015-03-07)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Added support for craftbukkit/spigot 1.8.3.
* Experimental: Unlocked a bunch of remaining living entity types for use as shopkeeper objects: bat, blaze, cave_spider, spider, ender_dragon, wither, enderman, ghast, giant, horse, magma_cube, slime, pig_zombie, silverfish, squid (no MC 1.8 entities yet) Those are highly experimental! They are disabled by default in the configuration: you might need to remove the 'disabled-living-shops' section in your config in order to let it regenerate with the new default values.  
Some of those entities (currently: bat, ender_dragon, wither, enderman, silverfish) get the new NoAI entity tag set in order to prevent the biggest issues with them: As this new tag is only available on MC 1.8 you should only enabled those if you are running on MC 1.8.x. 

Currently known issues with those new entity types (there might be more issues.. usage on own risk):  
* chicken: Still lays eggs.
* bat: Without the NoAI tag it flies around. If NoAI tag is available (MC 1.8) the bat is sleeping, but starts the flying animation if being hit by something.
* blaze: Without the NoAI tag it randomly starts flying upwards.
* enderdragon: Without NoAI tag, it flies around, towards players, and pushes entities around. Shows boss bar.
* wither: Without NoAI tag, it makes noise. Shows boss bar.
* enderman: Teleports away / goes invisible when being hit by a projectile. Starts starring animation.
* silverfish: Without NoAI tag it is rotating wierdly depending on where the player is standing (possibly because it tries to rotate towards the player). Is constantly showing the movement animation.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older bukkit versions is untested. If you encounter any problems let me know and I will look into it.
* When switching from a bukkit version which support mojang uuids to a bukkit version which does not support mojang uuids yet all stored owner uuids of player shops are lost.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.

## v1.49 Release (2015-03-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny internal change: The shopkeepers are kept ordered in the internal storage. This might fix potential issues of the newly added list command, which requires that the internal storage order does not change while the player is selecting another page for displaying.
* Fixed/Added: Entity shopkeepers are now unaffected by splash potion effects.

## v1.48 Release (2015-03-04)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Changed: Being less strict with the argument count of commands now: If the first argument matches an exisitng command, we handle this command, even if the argument count doesn't match the required amount of arguments. Previously it attempted to spawn a shopkeeper via command in some situations, if it didn't find a command that exactly matched.
* Changed: Ignoring the player's naming chat event, if the affected shopkeeper is no longer existing.
* Changed (API): Deprecated getting all shopkeepers by chunks.
* Changed (API): All API methods now return unmodifiable collections.
* Changed (API): The permission nodes were moved into ShopkeepersAPI. 
* Added command: /shopkeeper list [player|admin] [page] - Lists the players own shops (if no player/'admin' is specified), the shops of a specified player or all admin shops. The shops are divided into pages.
* Added permissions node: shopkeeper.list.own (default: true) - Allows listing the own shops via command.
* Added permissions node: shopkeeper.list.others (default: op) - Allows listing the shops of other players via command.
* Added permissions node: shopkeeper.list.admin (default: op) - Allows listing all admin shops via command. 
* Added command: /shopkeeper remove [player|all|admin] - Removes the players own shops (if no player/'admin' is specified), the shops of a specified player, all player shops, or all admin shops. The command needs to be confirmed by the player via '/shopkeeper confirm'
* Added permissions node: shopkeeper.remove.own (default: op) - Allows removing the own shops via command.
* Added permissions node: shopkeeper.remove.others (default: op) - Allows removing the shops of other players via command.
* Added permissions node: shopkeeper.remove.all (default: op) - Allows removing the shops of all players at once via command.
* Added permissions node: shopkeeper.remove.admin (default: op) - Allows removing all admin shops via command. 

Create a backup of your save.yml file and make sure that the permissions are setup correctly and actually work correctly before using.

## v1.47 Release (2015-03-01)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Update to latest spigot version: Item flags should now get compared as well.
* Fixed (workaround): In order to prevent villager shopkeeper to turn into witches, we prevent all lightning strikes in a 5 block radius around villager shopkeepers.
* Added (API): method for getting a shopkeeper by it's uuid.
* Fixed (API) : whenever shopkeeper.setLocation(..) is called, the shopkeepers location is now updated in the chunk map as well

## v1.46 Release (2015-02-13)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Added: The skeleton shopkeepers can now be switched between normal and wither skeleton variant.
* Added: The ocelot shopkeepers can now be switched between different cat type variants.
* Added: The zombie shopkeepers can now be switched between normal and villager-zombie variant. 

I am open for recommendations regarding how the different mob variants are represented by items in the editor menu, especially regarding representing the different cat types.

## v1.45.1 Release (2015-02-06)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed bug from previous version: Endless loop when hiring a player shopkeeper.

## v1.45 Release (2015-02-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed/Changed: Hiring a player shopkeeper should now check the player's max shops limit as well.
* Added PlayerShopkeeperHiredEvent.

## v1.44 Release (2015-02-03)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Left and right clicking the delete button at the same time no longer gives two shop creation items (eggs) back. 

## v1.43 Release (2015-01-10)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: If the cost for an item of a normal player shopkeeper are between high-currency-min and high-currency-value the first slot of the recipe remained empty (and by that the trade became unavailable). We now move the low cost into the first slot in this case.
* Slightly improved handling of invalid costs (this can happen if somebody manually modifies the save file) for trading player shopkeepers: if the first item for some reason is empty we now insert the second item into the first slot in this case, for the same reasons: making the trade still usable. 

## v1.42 Release (2015-01-07)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Added: each shopkeeper now additionally stores an unique id which shouldn't change over the shopkeepers lifetime.
* Attempt to fix potential issue with protection plugins, which cancle the PlayerInteractEvent because they want to deny usage of the shop creation item, not because denying access to the clicked chest: We now call another PlayerInteractEvent with the player not holding any items in hand, if that events gets cancelled as well we can be sure that the player has no access to the clicked chest. 

## v1.41 Release (2015-01-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* No longer storing owner uuids of player shops if running on a bukkit version which does not support mojang uuids yet. This helps if you have already stored those 'invalid'/non-mojang player uuids by running v1.40, however it has the disadvantage that you loose all stored owner uuids if you switch back to an older bukkit version after you have already run a newer bukkit version which supports mojang uuids!
* Ignoring the owner uuid now when finding inactive shops if not running on a bukkit versions which supports mojang uuids yet. 

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older bukkit versions is untested.
* When switching from a bukkit version which support mojang uuids to a bukkit version which does not support mojang uuids yet all stored owner uuids of player shops are lost. 

## v1.40 Release (2015-01-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny change to the (hidden) debugging command checkitem: The currently selected item now gets compared to the item in the next hotbar slot, instead of the item on the cursor (which made no sense because the player never has an item on the cursor when executing a command..)
* Attempt of making shopkeepers backwards compatible again down to bukkit 1.6.2 R1.0. Completely untested though. 

## v1.39 Release (2014-12-30)
### Supported MC versions: 1.8.1, 1.8
* Fixed duplication bug when players shift-click very fast (at least 2 times in the same tick) an item in an shopkeeper inventory which gets closed one tick later (ex. hire inventory).
* Added ShopkeeperTradeEvent (meant for canceling trades because of additional conditions) and ShopkeeperTradeCompletedEvent (meant for monitoring the actual outcome of a trade). 

## v1.38 Release (2014-12-29)
### Supported MC versions: 1.8.1, 1.7.4, 1.7.2
* Fixed (untested though): concurrent modification exception when removing player shopkeepers due to inactivity.

## v1.37 Release (2014-12-17)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Fixed error which was introduced in v1.36: The error about 'plugin registering a task during disable' should be gone now. During disable the plugin now triggers a save which runs synchronous instead again.

## v1.36 Release (2014-12-17)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Changed: the shopkeeper data now gets saved to file in a separate thread, hopefully reducing load on the main thread if your save file is very (very very) large. 

## v1.35 Release (2014-12-17)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Readded: removing inactive player shops every time the plugin gets enabled. Untested though. 

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Server crashes and improper shutdowns might cause living non-citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8. 

## v1.34 Release (2014-12-16)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Fixed: hiring of shopkeepers should now work again.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now.
* Experimental 1.8 support.
* Server crashes and improper shutdowns might cause living shopkeeper entities to duplicate sometimes. 

## v1.33 Release (2014-12-15)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Fixed: villager shopkeeper profession resetting after each reload.

## v1.32 Release (2014-12-15)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Added: (hidden) debug command 'shopkeepers checkitem' (only usable with debug permission) which compares the item in hand and item on cursor with each other and with the currency items.
* Fixed: item comparison (especially comparison of display name and lore) for the currency items didn't properly work for certain items under certain circumstances. 

## v1.31 Release (2014-12-07)
### Supported MC versions: 1.8, 1.7.9
* Changed the default remove-shop-button item from FIRE to BONE, because FIRE doesn't seem to be properly displayed since MC 1.8.
* Updated the item comparison to take the new banner data into account (MC 1.8).
* Changed: All item data values in the config can now be set to -1 in order to indicate that the data value shall be ignored.
* Changed: the item ids in the default config were replaced with material names.
* Added optional item lore setting for the shop creation item (if none is set (default) the lore is ignored).
* Added optional name and lore settings for the hire item. Those will be ignore if they are kept empty.
* The settings 'zero-item' and 'high-zero-item' were renamed to 'zero-currency-item' and 'high-zero-currency-item'. You will have to update your config if you made changes to those settings in the past!
* Added optional item name and lore settings for currency and high currency item (if not set (by default) those will be ignored).
* Added optional item data, name and lore for zero currency and high zero currency items as well (if not set (by default) those will be ignored).
* Removed 5th villager profession (green) from editor menu in case the plugin is running on MC 1.8. Mc 1.8 doesn't seem to have the green version anymore.
* Fixed: the shopkeeper command should now work again with the optional shop object type parameter. [Ticket-269]
* Added /shopkeepers help command and help pages which displays the available commands. Commands for which a player doesn't have the needed permission get filtered out.
* Added a few more messages to the command output for the cases that a command fails (for example due to missing permissions or invalid command parameters). Also we now stop the command execution instead of continuing with default command parameters.
* Added 'shopkeepers.debug' permissions node (default: op). Previously we manually checked if the command executor is op before giving access to the debug command. 

## v1.30 Release (2014-12-02)
### Supported MC versions: 1.8, 1.7.9
* Added the setting 'silence-living-shop-entities' which let's you define whether or not living shopkeeper entities create sounds or not (default: true/silenced). This feature only work on MC 1.8. 

Also please see the changelog of the previous versions!

## v1.29 Beta (2014-11-29)
### Supported MC versions: 1.8, 1.7.9
* Updated for craftbukkit/spigot 1.8

Also please see the changelog of the previous versions!

## v1.28 Alpha (2014-11-21)
### Supported MC versions: 1.7.9
* Reordered the shop types so the normal player shop is by default selected instead of the book shop.
* The selection of shop type and shop object (mobs, sign) are no longer reset after shop creation, but are kept stored until the player quits or the plugin is reloaded.
* Added a debug message to inventory clicks.
* Fixed: The previous version did not proplery block shift-click buying on player shops anymore. This should now be fixed again. 

Also please see the changelog of the previous versions!

## v1.27 Alpha (2014-11-21)
### Supported MC versions: 1.7.9
* Fixed: The first (top-left) slot in a player's inventory was not properly working in editor mode of player shopkeepers.
* Fixed: The custom item comparison in the last version did skip item meta comparison.
* Improved the debug output when trading fails: it now displays the reason why the invovled items are not considered similar.
* Fixed: When the trading slot was clicked and the cursor would not be able to hold the result of the trade (because the cursor already hold an item of different type or the result would exceed the max stack size) minecraft cancles the trade. Shopkeepers however wasn't aware of this and removed and added the invovled items to and from a player's shop chest regardlessly. We now skip our trade-handling as well in this situation: you cannot trade if the cursor cannot hold the resulting items. 

Also please see the changelog of the previous versions!

## v1.26 Alpha (2014-11-20)
### Supported MC versions: 1.7.9
* Changed how item comparison is done: We no longer use bukkit's built-in comparison method. This has the disadvantage that we have to write and update the comparison of all item related data ourselves and probably don't work with modded items that well anymore (I might add a setting in the future which toggles between the new and the old comparison if there is the need for it). However doing the comparison ourselves gives us more control over what aspects of items actually define them as being 'similar': this change allows us to compare item attributes and skull data ourselves, hopefully resolving the issues we had with this in the past.

Also please see the changelog of the previous versions!

This is an alpha version. Things might not properly work yet.  
**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now. 

I suggest you to backup your old save file, just in case

## v1.25 Alpha (2014-08-14)
### Supported MC versions: 1.7.9
* Fixed: citizens falsely being detected as disabled 

Also please see the changelog of the previous versions!

## v1.24 Alpha (2014-08-04)
### Supported MC versions: 1.7.9
* Fixed: some chest checks not working for the transfer and setForHire commands
* Fixed: it should no longer be possible to create a player shopkeeper with a chest which is already in use by some other shopkeeper
* Added setting 'prevent trading while owner is online' (default false): with this setting enabled player shopkeepers don't trade (nor open the trading window) while the owning player is online. This might be useful for role playing servers, which wish to force players to trade with each other directly while being online. 

Also please see the changelog of the previous versions!

## v1.23 Alpha (2014-07-27)
### Supported MC versions: 1.7.9
* Fixed: missing save when editor window is closed 

Also please see the changelog of the previous versions!

## v1.22 Alpha (2014-07-26)
### Supported MC versions: 1.7.9
* Fixed: trading with player shopkeepers not working
* Fixed: shopkeeper names not being colored in the trade menu or on signs 

Also please see the changelog of the previous versions!

## v1.21 Alpha (2014-07-12)
### Supported MC versions: 1.7.9
* Fixed: normal players not being able to trade with admin shops.
* Added: permission 'shopkeeper.trade' (default true), which determines whether or not a player can trade with shopkeepers.
* Added: sheep shopkeepers can now change their colors via the editor menu. 

Also please see the changelog of the previous version!

## v1.20 Alpha (2014-07-10)
### Supported MC versions: 1.7.9
This is an alpha version:  
While this version seems to work quite well already, there might be some new bugs being introduced due to some internal changes.
Also there are some issues which still needs to be resolved (hopefully no breaking issues though..) and some new features to test out.

* Updated to CB 1.7.10. Not much tested yet.
* Internal rewrites. (Though I am not completly statisfied with the current result yet..)
* Added: Shopkeeper entities are now tagged with metadata 'shopkeeper' which can be used to identify shopkeeper entities in other plugins.
* Added new setting 'bypassShopInteractionBlocking': can be turned on if you experience issues with other plugins bocking users from opening the shopkeeper interfaces (for example due to protected regions).
* Renamed setting from 'deletingPlayerShopReturnsEgg' to 'deletingPlayerShopReturnsCreationItem'
* Added support for some more living entities shopkeeper types: chicken, cow, iron_golem, mushroom_cow, ocelot, pig, sheep, skeleton, snowman, wolf, zombie. Other entity types weren't included yet due to issues regarding replacement of their default behavior. The newly added entitiy types seem to work fine. However, note that chicken shopkeepers still lay eggs.
* Changed the permission nodes for the living entity shopkeepers to: shopkeeper.entity.<lowercase_mobname> and shopkeeper.entity.*for all.
* Old permission nodes for creepers, villagers and witches should still work, but might be removed in a future update.
* Trapped Chests can now be used as shop chests as well.
* Added: experimental support for Citizens based shopkeeper entities (Thanks to elMakers for that): You can either create a player npc shopkeeper via the creation item, which will default to the owner's name/skin, or an admin npc shopkeeper via the command '/shopkeeper npc'. Also there was a special ShopkeeperTrait added which can be assigned to a NPC to assign shopkeeper behavior to it. Though it is recommended currently to created the citizens npc's through the shopkeeper command instead. Permission node for creation is: shopkeeper.citizen
* Note that there are very likely still some issues left, mainly regarding cleanup of Citizens npc's or shopkeeper data, especially in situations where either Citizens or Shopkeepers is not running.
* Added a setting to disallow renaming of citizens player shopkeepers.
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now due to differences between the different currently supported bukkit versions. This will probably be added at a later point again. 

This is an alpha version. Things might not properly work yet.  
**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* Items with attributes might not properly work.
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now. 

I suggest you to backup your old save file, just in case

## v1.18 Release (2014-04-14)
### Supported MC versions: 1.7.9, 1.7.4, 1.7.2
* Updated to CB 1.7.8
* Fixed a serious item duping bug [Ticket 228]: double clicking in the bottom inventory while having the trading gui opened gets blocked now
* Now updating the players inventory shortly after a trade failed to reduce the chance for fake items popping up in the players inventory temporary.
* Now printing slightly more detailed debug message for failing trades, if debug mode is turned on.
* Removed unused stuff from the compatibility mode, which probably was the cause that this time the compatibility mode didn't work when you tried to use the old Shopkeepers version on 1.7.8 servers..
* Updated to use player uuids. Some notices on that: 
  * Make sure you run on some of the newest MC/Bukkit versions, because old versions might not support player account uuids correctly. I suggest you use at least some 1.7.+ version.
  * Make sure that your server runs in the correct online mode: uuids of player between online and offline mode don't match. So if you switch your server's online mode the stored player uuids won't be 'working' anymore..
  * The uuids for owners of player shops get stored as soon as the player logs in. The playername gets used for display purposes and as identifier for the shop owner of those player shops for which we don't have an uuid yet.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this..
* If you use the 'remove shopkeeper after certain time of inactivity of player' feature, then you might experience some server freezing when the plugin loads: to find out when the owner of a shop was online for the last time we use a method which might have to lookup a player's uuid or name via mojangs services. However, this is currently considered a minor 'issue' as those lookups can only happen once per plugin load (and reload), and the server probably also caches the results from those lookups, so the next plugin loads probably won't have to lookup those data again.
* If you have shops with items in it with attributes, those might disappear or crash players. I am not completly sure if this is an issue with shopkeepers or a general thing that the client can't cope with certain modified items and crashes then. I also experienced this only for the first time I tested the new 1.7.8 bukkit version, afterwards those items went lost somehow and stopped crashing the client. However I wasn't able yet to test the new shopkeepers version with items with custom attributes / edited nbt data, as I wasn't able yet to find some updated/working plugin which creates items with attributes.. 

I suggest you to backup your old save file, just in case

## v1.17.2 Release (2014-03-25)
### Supported MC versions: 1.7.4, 1.7.2, 1.6.4
* Another attempt to fix the remaining issue with duplicating villagers (now regarding duplication over restarts). Let me know if this is still a problem.
* Fixed: Sign shops disabling themselves on chunk unloads.
* Updated to CB 1.7.5

## v1.17.1 Release (2014-03-11)
### Supported MC versions: 1.7.4, 1.7.2, 1.6.4
* Tiny encoding changes of the included german translation file (let me know if this works for you if you use it)
* Fixed: the trade not being blocked if there is a similar trade existing (only the current trade gets checked now)
* Probably fixed: Shopkeeper entities (villagers) duplicating sometimes. This was caused by plugins which manually unload chunks without calling an event (without letting us know). We now work-around this by loading the chunk whenever we want to remove the entity again. Edit: Please let me know if this is still an issue.
* Probably fixed: Chunks kept loaded due to the same reason above: plugins not letting us know that they have unloaded the chunk. We now load the chunk only once and request an ordinary ChunkUnload which will then remove and deactivate the Shopkeeper when the chunk gets unloaded by that request. (Edit: seems to still be not fixed -> craftbukkit seems to not call an ChunkUnloadEvent if saving is disabled :( )

## v1.16 Release (2014-02-08)
### Supported MC versions: 1.7.4, 1.7.2, 1.6.4
* Fixed: preventing of the regular usage of the shop creation item did not work properly
* Fixed: players could loose their spawn eggs when clicking a living shopkeeper (clicking with spawn eggs in hand is now blocked)
* Fixed: some NPE when sign shops had no name
* Fixed: name plate prefix / coloring going lost under certain conditions. Also the entity name / name plate are now removed, if name plates are disabled in the config (before they were simply left they way they are)
* Changed: empty messages are now skipped (quick solution to disable certainmessages)
* Added: item data settings for the delete, hire and name item.
* Added: buttons can now have configurable item lore
* There are probably a few new messages in the config

See also the changes of the previous 1.16 beta versions!

## v1.16-beta3 Beta (2014-01-09)
### Supported MC versions: 1.7.4, 1.7.2, 1.6.4
* Added new command to remotly open admin shops via "/shopkeeper remote <shopname>" which needs the new "shopkeeper.remote" permissions node
* Fixed: shop entity-types are now correctly being checked, if they are enabled in the config, before they are created
* Fixed: an empty save.yml file should no longer cause an error
* Citizens2 NPC's are now ignored when being clicked
* Added a few new settings to allow naturally spawned villagers to be "hired" (a certain amount of shop-hire items can be traded for the shop creation item). By default turned off.
* Added new messages to be configureable now. Make sure to update your translations.
* Tiny change: the items in the trading slots now have to also match the required number of their corresponding slots. [Ticket 210]

## v1.16-beta2 Beta (2013-12-09)
### Supported MC versions: 1.7.2, 1.6.4
* Colors are now supported in shopkeeper names, but you will need to add the ampersand symbol to the name-regex option
* Added bypass-spawn-blocking option, which should bypass villager spawns being blocked by other plugins

## v1.16-beta Beta (2013-12-03)
### Supported MC versions: 1.7.2, 1.6.4
* Support for 1.6.4 and 1.7.2
* Added creeper admin shop option
* Added support for item attributes

In 1.7.2, an invalid trade will create a "ghost item". This seems to be a CraftBukkit bug. 

## v1.15.1 Release (2013-07-19)
### Supported MC versions: 1.6.2
* Added prevent-trading-with-own-shop option
* Added tax-rate and tax-round-up options
* Added prevent-shop-creation-item-regular-usage option
* Added transfer command (/shopkeeper transfer playername, while looking at a shopkeeper chest) and shopkeeper.transfer permission node
* Added setforhire command (/shopkeeper setforhire, while looking at a shopkeeper chest and holding the cost in hand) and shopkeeper.setforhire and shopkeeper.hire permission nodes
* Added max-shops-perm-options option and associated shopkeeper.maxshops.X permission node
* Added name-regex and msg-name-invalid options
* Added creeper shops (options: enable-creeper-shops and msg-selected-creeper-shop; permission: shopkeeper.creeper)
* Added support for 1.6.2

## v1.14.2 Beta (2013-07-05)
### Supported MC versions: 1.6.1
* Updated for 1.6.1
* Not compatible with previous Minecraft versions!
* Added always-show-nameplates option (actually added in 1.13.4)
* Can now use "/shopkeeper witch" to create an admin witch shop
* Bug fixes

## v1.14.1 Beta (2013-07-03) [broken]
### Supported MC versions: 1.6.1
* Updated for 1.6.1
* Not compatible with previous Minecraft versions!
* Added always-show-nameplates option (actually added in 1.13.4)
* Can now use "/shopkeeper witch" to create an admin witch shop

Doesn't work. Whoops!

## v1.13.4 Release (2013-06-25)
### Supported MC versions: 1.5.2
* Added always-show-nameplates config option (defaults false)
* Added some new API
* Bug fixes

## v1.13.3 Release (2013-06-11)
### Supported MC versions: 1.5.2
* Bug fix

## v1.13.2 Release (2013-05-03)
### Supported MC versions: 1.5.2, 1.5.1
* Updated for 1.5.2

## v1.13.1 Release (2013-04-18)
### Supported MC versions: 1.5.1
* Bug fix

## v1.13 Release (2013-04-09)
### Supported MC versions: 1.5.1
* Shopkeeper chests are protected from hoppers
* To set up costs for trading shopkeepers, first click an item in your inventory, then click the slot in the editor window (you no longer click and drag)
* Shift-click is allowed again for admin shopkeepers (not for player shopkeepers)
* Added player-shopkeeper-inactive-days option (removes shopkeepers if a player has not logged in for a specified number of days)
* A couple minor bug fixes
* This version is NOT compatible with 1.4.7

## v1.12 Release (2013-03-21)
### Supported MC versions: 1.5.1, 1.4.7
* Updated for Minecraft 1.5 (should still work with 1.4.7 as well).
* Several config options have been added, changed, or deleted. See Configuration Options for more information.
* Added support for all items with meta information (fireworks, enchanted books, custom potions, etc). Renamed items and items with lore are also supported.
* Added tooltips for the editor window action buttons.
* Removed the "Save" action button (just close the editor window to save).
* Added the "Set Shop Name" action button, which allows you to set a shop's name. This sets the nameplate for the villager as well as the trade window text.
* Added the witch as a shopkeeper type.
* Shopkeeper creating has been unified. Right-click in air while holding the creation item (villager egg by default) to select the type of shop (normal, buy, trade). Hold sneak while you right-click in air to choose the shopkeeper type (villager, sign, witch). Then right-click on a chest then on a block as usual to place the shop.

I highly recommend you back up your save.yml file in the Shopkeepers folder. I've made some changes to the save file format, and in the event that I made an error, it will be nice to have a backup.

## v1.11 Release (2013-01-20)
### Supported MC versions: 1.4.7
* Updated for 1.4.7

## v1.10 Release (2012-12-31)
### Supported MC versions: 1.4.6
* Updated for 1.4.6

## v1.9 Beta (2012-11-20)
### Supported MC versions: 1.4.5
* Updated for 1.4.5
* A couple bug fixes
* Added delete-shopkeeper-on-break-chest option (this option is incompatible with the protect-chests option)
* Item names and lore are supported in admin shops

## v1.8 Release (2012-10-25)
### Supported MC versions: 1.3.2
* Multiple players can trade with a shopkeeper at the same time
* Sign shopkeepers added: right click a chest with an emerald in your hand, then right click a block to place the sign shop
* New Configuration Options added for sign shops
* Players can no longer buy items from their own shops
* Added enable-purchase-logging config option
* Fixed some bugs with shopkeepers disappearing and otherwise misbehaving

## v1.7.1 Release (2012-09-27)
### Supported MC versions: 1.3.2
* Bug fix: buying shop currency change calculations

## v1.7 Release (2012-09-25)
### Supported MC versions: 1.3.2
* Player shopkeepers can now only be created with a chest the player has recently placed
* Bug fix: Shopkeeper created message will no longer show up when the max-shops-per-player cap is reached
* Bug fix: Items with data values will no longer get confused in a player shopkeeper chest

## v1.6.1 Release (2012-08-31)
### Supported MC versions: 1.3.2, 1.3.1
* Bug fixes

## v1.6 Release (2012-08-29) [broken]
### Supported MC versions: 1.3.2, 1.3.1
* Added new player shopkeeper type: trading shopkeeper
* Added new permissions: shopkeeper.player.normal, shopkeeper.player.book, shopkeeper.player.buy, shopkeeper.player.trade
* Removed allow-player-book-shop option (use permission node instead)
* Can now specify a shop type in the /shopkeeper command
* Zombies will no longer gather around shopkeepers
* Hopefully fixed some bugs

There's an odd error with this version, use 1.6.1 instead.

## v1.5 Release (2012-08-21)
### Supported MC versions: 1.3.1
* Added WorldGuard support (if enabled, player shopkeepers can only be placed where a player can also build)
* Added Towny support (if enabled, player shopkeepers can only be placed in areas designated as commercial)
* Added 'language' option (currently only supports 'en' and 'de', if you want to offer a transation, please submit a ticket)
* A few bug fixes

## v1.4 Release (2012-08-14)
### Supported MC versions: 1.3.1
* Player shops can now sell enchanted items
* Buying shops can now use the higher value currency when handling trades
* Added max-chest-distance and block-villager-spawns options

## v1.3 Release (2012-08-09)
### Supported MC versions: 1.3.1
* Player shopkeepers no longer have to stand on chests
* Player shopkeepers no longer have to have stacks set up in the chest, but can instead adjust the quantity from the edit screen
* New player shopkeeper type that can sell written books
* New player shopkeeper type that can buy items instead of sell them
* Deleting a player shopkeeper can return a villager egg
* Added a max shops per player option
* Added some API
* Various bug fixes

## v1.2 Release (2012-08-07)
### Supported MC versions: 1.3.1
* Fixed major bug with second currency
* Fixed maps (for admin shops)
* Trades and costs will save when closing the editor window, even without clicking the emerald block
* Unused items from the admin editor will be returned to the player when the editor is closed
* Added a debug mode

## v1.1 Release (2012-08-07) [broken]
### Supported MC versions: 1.3.1
* Added a second, higher value currency for player shops (can be disabled)
* A couple bug fixes and improvements

Do not use the higher currency option in this version! It is broken!

## v1.0 Release (2012-08-06)
### Supported MC versions: 1.3.1
* First release
