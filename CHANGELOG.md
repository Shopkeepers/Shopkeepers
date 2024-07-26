# Changelog
Date format: (YYYY-MM-DD)  

## v2.22.3 (TBA)
### Supported MC versions: 1.21, 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

* Fix: Compatibility with the latest versions of Spigot 1.21 (Thanks @DerMistkaefer). Spigot 1.21 builds from before 2024-07-07 are not supported.

## v2.22.2 (2024-07-20)
### Supported MC versions: (1.21,) 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

**Update: Some change in Spigot 1.21 broke this version of the Shopkeepers plugin. This version only works on Spigot versions from before 2024-07-06.**

* Add: Equipment editor for shopkeeper mobs and normal villagers.
  * The equipment editor can be opened from the shopkeeper editor, as well as from the editor for normal villagers. There is no equipment editor for Citizens NPC shopkeepers yet.
  * Players can also use placeholder items to setup the equipment items.
  * As before, to reduce the amount of entity combust events that we need to handle, we automatically apply a small (invisible) default head item for mobs that usually burn in sunlight. If a custom head item is configured inside the editor, the item from the editor is used instead. If the configured item is destructible, it is automatically made unbreakable to not break when exposed to the sunlight.
  * By default, the equipment can only be edited for mobs and slots that are known, at least for certain items, to visually affect the mob.
    * If the mob is not affected by any equipment, the editor option is omitted.
    * If a mob already provides dedicated editor buttons for certain equipment slots (e.g. horse armor, llama carpet), we omit these slots from the equipment editor.
    * Vindicators currently don't support the mainhand slot, because they only render the item when chasing a target, which is not the case for shopkeeper mobs.
  * Config: Add setting `enable-all-equipment-editor-slots` (default: `false`) that enables the equipment editor for all mobs and all equipment slots, regardless of which equipment slots the mob actually supports.
    * This can for example be useful for testing purposes.
    * If the mob provides dedicated editor options for certain equipment slots, any non-empty equipment configured in the equipment editor takes precedence.
  * Command: The `/shopkeeper replaceAllWithVanillaVillagers` command ignores the custom equipment for now, because vanilla villagers can end up dropping the equipped items, e.g. on death, which might not be intended.
  * Debug: Extend the `/shopkeeper debugCreateShops` command to accept an argument `testEquipment`: When specified, we spawn a shopkeeper for each enabled mob type and apply a default equipment. This can be useful to quickly test which equipment slots the individual mobs support.
  * API: Add `LivingShopObject#getEquipment()` that provides access to the shop object's equipment. Via the API, one can specify arbitrary equipment, regardless of what the mob actually supports or what can be edited by players in the editor.
  * API: Add `LivingShopObject#openEquipmentEditor` to open the equipment editor for a player.
  * API: Add `DefaultUITypes#getEquipmentEditorUIType`.
* Add: Configure the carried block of endermans by assigning an item to their mainhand in the equipment editor.
  * Only items with a corresponding block type result in a block to be carried by the enderman.
  * Players can also use placeholder items to specify the block type. This also allows players to use block types for which there doesn't exist a corresponding item (e.g. potted flowers).
  * Any block state data contained by the item (`BlockStateTag` / `block_state` component) is applied to the carried block. If the item is a placeholder item, any contained block state data is applied to the substituted block type.
  * Note: Some blocks do not properly render (e.g. player heads, signs, etc.).
* Debug: The command `/shopkeeper debugCreateShops` spawns the shopkeepers now in the direction the player is facing.
* Minor changes related to the trading shop editor. Players can now click a non-empty slot in their inventory to swap the item on the cursor.
* API: Remove deprecated `LivingShopObjectTypes#getAliases(EntityType)`.
* Build: Fix issue when building against the latest Spigot version. We now build against a specific Spigot 1.21 version.
* Internal: Add the option to add default potion effects to living entity shops, for example if certain effects are required for a mob to properly function as a shopkeeper (not yet used).
* Internal: When opening the UI for a player, we validate now that the player and the involved shopkeeper are still valid.

**Message changes:**  
* Added `button-equipment`.
* Added `button-equipment-lore`.
* Added `equipment-editor-title`.
* Added `equipment-slot-lore`.
* Added `equipment-slot-mainhand`.
* Added `equipment-slot-offhand`.
* Added `equipment-slot-feet`.
* Added `equipment-slot-legs`.
* Added `equipment-slot-chest`.
* Added `equipment-slot-head`.
* Added `equipment-slot-body`.

## v2.22.1 (2024-06-30)
### Supported MC versions: (1.21,) 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

**Update: Some change in Spigot 1.21 broke this version of the Shopkeepers plugin. This version only works on Spigot versions from before 2024-07-06.**

* Fix: Since Spigot 1.20.5, snowman and mushroom cow shopkeepers failed to load because these mobs have been renamed to their Minecraft names ('SNOW_GOLEM' and 'MOOSHROOM').
  * When running on MC 1.20.5 or above, we automatically migrate these shop object types to their new names so that they load again.
  * Config: When updating to MC 1.20.5 or above, the old mob names in the `enabled-living-shops` setting might still work for now, but it is recommended to update them to their new names. We log a warning now should the old mob names no longer be recognized in the future.
  * Config: The default config was updated to use the new mob names. When running on an older server version, this results in a warning now, similar to the warnings that are logged for any default enabled mob types that are not supported by the server's Minecraft version.
  * Adjusted the shop object type aliases so that the `/shopkeeper` command recognizes both the old and new mob names.
* Fix: In Shopkeepers v2.22.0, after a server upgrade, some shopkeepers failed to load with an error `Invalid trade offer 1: Failed to load property 'resultItem': Data is not of type ItemStack, but com.nisovin.shopkeepers.util.inventory.SKUnmodifiableItemStack!`.

## v2.22.0 (2024-06-22)
### Supported MC versions: (1.21,) 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

**Update: Some change in Spigot 1.21 broke this version of the Shopkeepers plugin. This version only works on Spigot versions from before 2024-07-06.**

* Fix: Avoid extending Bukkit's ItemStack in our implementation of UnmodifiableItemStack. This also resolves a compatibility issue with Paper servers for MC 1.21.
* API: Add PlayerShopkeeper#setForHire(UnmodifiableItemStack). This might break some add-on plugins, because when passing null, the call is now ambiguous and requires a cast to either ItemStack or UnmodifiableItemStack.
* Build: Update Maven repository URL.

## v2.21.0 (2024-06-18)
### Supported MC versions: (1.21,) 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

**Update: Some change in Spigot 1.21 broke this version of the Shopkeepers plugin. This version only works on Spigot versions from before 2024-07-06.**

* Update for MC 1.21. Add bogged and breeze to the by default enabled mob types.
* Debug: Add additional debug output if one of the item related server assumption tests fails.

## v2.20.1 (2024-05-26)
### Supported MC versions: 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

* Fix: Avoid adding empty item stacks to merchant recipes. This resolves a crash on Paper 1.20.6 servers.
* Fix: The default config version is `6`. We previously applied a redundant migration from version 5 to 6 that did nothing.

## v2.20.0 (2024-05-25)
### Supported MC versions: 1.20.6, 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.18.2, 1.17.1, 1.16.5

* Update for MC 1.20.6:
  * MC 1.20.5 was replaced and is not supported.
  * Add wolf variants.
  * Placeholder items: Some potion and enchantments have been renamed in Bukkit to now match their Minecraft counterparts. If you used the Bukkit names in placeholder items, they might no longer get recognized and you need to update those items to use the Minecraft names instead. The "empty"/"uncraftable" potion is also no longer supported in placeholder items.
  * Build: The build requires JDK 21 now.
    * Use Sdkman instead of Jabba, because Jabba does not provide JDK 21. However, we still use Jabba for JDK 16, because it is missing in Sdkman for certain platforms.
    * Updated Gradle and some libraries accordingly.
    * Disabled the checkerframework checks for now, because on the current versions we get a lot of weird casting errors.
* Drop support for MC 1.19, 1.19.2, and 1.19.3 to speed up build times. MC 1.19.4 is still supported and can be updated to without issues.
* Add settings to identify the shop creation item by a custom NBT tag.
  * Previously, we identified shop creation items by matching their item data with the data specified in the `shop-creation-item` setting. Adding and identifying the shop creation item by a custom NBT tag instead has several benefits, such as being able to change the `shop-creation-item` in the future without breaking any existing shop creation items in the world (e.g. in chests, player inventories, trades, third-party plugin data, etc.). Also, in the past, we occasionally ran into issues when the server implementation made changes to how it creates the item based on the data specified inside the config.
  * These new settings are enabled by default for new configurations, but disabled when migrating from a previous Shopkeepers version in order to not break backwards compatibility for existing items.
  * Setting `add-shop-creation-item-tag` (default: `true`): Whether to add the tag to newly created shop creation items, e.g. when created via the `/shopkeeper give` command.
  * Setting `identify-shop-creation-item-by-tag` (default: `true`): Whether to identify the shop creation item by the tag.
    * This is a separate setting in order to help server owners with the migration process: Server owners can enable `add-shop-creation-item-tag` very early to already add the tag to all newly created shop creation items, but separately enable `identify-shop-creation-item-by-tag` later, once they expect or verified that the old shop creation item is no longer in use.
    * Unfortunately, the Shopkeepers plugin provides no built-in solution to automatically migrate all occurrences of the old shop creation item in the world or plugin data.
* Change the default `shop-creation-item` display name to use json-based text.
  * Since somewhere around MC 1.20.4, display names with `&`-based color codes were no longer converted to a json text representation, but to legacy color codes. This caused some issues with existing shop creation items no longer getting recognized or stacking correctly. On some servers, these legacy color codes are in some circumstances later converted to json-based text, which causes similar issues with these shop-creation items suddenly no longer working after these conversions.
  * The default display name was changed to the compact `'{"text":"Shopkeeper","italic":false,"color":"green"}'`. This change is not automatically applied to existing configurations, because it could break existing shop creation items.
  * If you want to use a json-based display name that matches the previous representation on Spigot servers prior to MC 1.20.4, you can change the display name in the config to `display-name: '{"extra":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"color":"green","text":"Shopkeeper"}],"text":""}'`.
* Fix: In v2.19.0, we added a workaround for a change in the Paper server to now force teleports of shopkeeper entities. However, on Spigot servers, we need to manually reset the forced teleport request again after the entity teleport, because we don't receive an EntityTeleportEvent there.
* Fix: The angry state of wolves would previously get reset after some time (when the random AngerTime runs out). We now periodically re-apply it.
* Debug: Add debug option `text-components` to log additional debug output whenever component-based text is sent.
* Prepare for Paper's future removal of CraftBukkit's package relocation: Adjust the fallback compatibility mode to no longer try to parse the CraftBukkit version from the package name.
* Fix: The entity argument for command `editVillager` no longer accepts non-villagers, shopkeepers, or Citizens NPCs.

**Message changes:**  
* Added `command-entity-argument-no-villager`.
* Added `button-wolf-variant`.
* Added `button-wolf-variant-lore`.

## v2.19.0 (2023-12-26)
### Supported MC versions: 1.20.4, 1.20.2, 1.20.1, 1.19.4, 1.19.3, 1.19.2, 1.19, 1.18.2, 1.17.1, 1.16.5

* Add support for MC 1.20.4. MC 1.20.3, which is mostly the same, is not officially supported. No new features or mobs. Experimental MC 1.21 mobs are not yet supported.
* Add setting `citizen-npc-fluid-pushable` (default: `false`) to make all shopkeeper Citizens NPCs pushable by fluids (`true`), unpushable by fluids (`false`), or not modify their current behavior (`"undefined"`).
  * When set to `"undefined"`, the Citizens NPCs are not modified but retain their default or previously set fluid pushable behavior.
  * Unfortunately, the Citizens plugin has no in-game command yet to toggle the fluid pushable state of individual NPCs. But this is likely to be added in the future.
* Shulker shopkeepers peek at nearby players now.
  * This behavior can be disabled with the setting `shulker-peek-if-player-nearby` (default: `true`).
  * The setting `shulker-peek-height` (default `0.3`) defines how much the shulker opens when it peeks.
* Add: Support for setting up items that execute a command when being traded (either sold or bought).
  * Add: Command `/shopkeeper setTradedCommand <command>` (permission `shopkeeper.settradedcommand`, default `op`): Sets the command to execute when the held item is traded.
  * The command to execute is stored inside the item's data under the Bukkit key `shopkeepers:traded_command`.
  * When an item that has a traded command assigned is traded, the item is destroyed and the assigned command is executed as often as there are items in the item stack.
  * This feature is not meant to replace the requirement for custom third-party plugins or scripts in order to implement complex or custom behaviors. In order to reduce implementation and maintenance effort, only a single command can be assigned to an item and only a very limited set of placeholders is supported: `{player_name}`, `{player_uuid}`, `{player_displayname}`, `{shop_uuid}`.
    * Simple command sequences can also be defined via command aliases in Bukkit's "commands.yml" (see https://bukkit.fandom.com/wiki/Commands.yml).
    * If additional context information is required, e.g. about the shopkeeper's location or shop owner, a custom plugin that listens for the `ShopkeeperTradeEvent` might be better suited to implement the intended behavior.
* Add: Utility command "/shopkeeper replaceAllWithVanillaVillagers".
  * This command deletes all shopkeepers and replaces them with corresponding vanilla villagers without AI that are configured very similar to villager shopkeepers.
  * This might for example be useful when migrating a world to vanilla Minecraft, e.g. when a server closes a world but wants to provide it as download to its players with all the shopkeepers included.
  * This command requires all of the following permissions: `shopkeeper.debug`, `shopkeeper.remove-all.player`, `shopkeeper.remove-all.admin`.
* Add: "No shops were found" message to the "removeAll" command.
* Fix: Moving shopkeepers did not update their location in the AI system, breaking gravity and AI activations when being moved out of their original chunk.
* Fix: Verify that the Citizens API is still available before we try to use it. This guards against cases in which the Citizens plugin reports as "enabled", but the Citizens API is not in a properly initialized state. Reloading the Citizens plugin via PlugMan also seems to leave the Citizens API in an unusable state.
* Fix: The check whether the cursor can hold the traded item was off by one, unnecessarily preventing trades in some cases.
* Fix: Shopkeepers could no longer be moved after a recent change in the Paper server (PR 9937) to now also call the EntityTeleportEvent for plugin invoked teleports: All plugins that previously cancelled or modified entity teleports in certain cases (including the Shopkeepers plugin itself) can now accidentally cancel or modify plugin invoked entity teleports that were not supposed to be cancelled or modified previously. For now, we restore the previous behavior for all of our plugin triggered shopkeeper entity teleports by forcing these teleports, bypassing any cancellation and modification attempts of plugins.
* API (breaking): Changes to the `ShopkeeperTradeEvent`.
  * The result item received by the trading player and the items received by the shopkeeper can now be altered via the `ShopkeeperTradeEvent` (`#get/setReceivedItem1/2`, `#get/setResultItem`).
    * Breaking: Plugins that use this event can no longer assume that the received items equal those of the trading recipe.
    * It is also possible to clear the received items. This might for example be useful when implementing items that apply alternative effects when traded (e.g. command items, or Vault integration).
    * Trade logging and trade notifications still log the original items of the trading recipe.
    * Some shopkeepers ignore changes to the received items in certain cases. For example, when removing items from a shop container or when adding or removing currency items to or from shop containers, the default shopkeepers will ignore any changes to the received items.
  * Since the received items can now be modified by plugins during the trade event, the event is called earlier now, prior to certain inventory related checks.
    * Breaking: Plugins can no longer assume that the trade will actually take place even if the trade event remains uncancelled.
  * Add a non-cancellable `ShopkeeperTradeCompletedEvent` that is called if the trade actually took place, after all trade effects have been applied.
  * Add `ShopkeeperTradeEvent#getTradeEffects` that allows custom `TradeEffect` instances to be added that are invoked when the trade is either aborted or completed.
    * This can be useful when implementing custom trade effects that are meant to only be executed once the trade is actually applied.
    * This might also be used for some of the built-in default trade effects in the future.
* Internal: Add support for float values inside the config.
* Internal: Shop objects are notified now whenever their AI is ticked.
* Internal: Minor refactors related to trade merging.
* Internal: Refactors related to the creature force spawner to be able to reuse it outside of shopkeeper spawning.
* Internal: Add support for Trilean type in config (`true`, `false`, `"undefined"`).

**Message changes:**  
* Added `traded-command-set`.
* Added `traded-command-removed`.
* Added `traded-command-view`.
* Added `traded-command-view-unset`.
* Added `command-description-settradedcommand`.
* Added `no-shops-found`.
* Added `confirm-replace-all-shops-with-vanilla-villagers`.
* Added `all-shops-replaced-with-vanilla-villagers`.
* Added `command-description-replace-all-with-vanilla-villagers`.

## v2.18.0 (2023-10-02)
### Supported MC versions: 1.20.2, 1.20.1, 1.19.4, 1.19.3, 1.19.2, 1.19, 1.18.2, 1.17.1, 1.16.5

* Add support for 1.20.2.

## v2.17.2 (2023-09-17)
### Supported MC versions: 1.20.1, 1.19.4, 1.19.3, 1.19.2, 1.19, 1.18.2, 1.17.1, 1.16.5

* Fix: Incompatibility with Paper build 185: Item migrations failing due to new ItemStack#isEmpty method.

## v2.17.1 (2023-06-28)
### Supported MC versions: 1.20.1, 1.19.4, 1.19.3, 1.19.2, 1.19, 1.18.2, 1.17.1, 1.16.5

* Change the icons of the sign type editor button of hanging sign shop objects from sign to hanging sign items.
* Fix: Plugin not loading on server versions without MC 1.20 hanging sign materials: "The default value for property 'signType' is invalid: Unsupported hanging sign type: 'OAK'."
  * On those server versions, there is no valid default hanging sign type.
  * Disable the sign type validation on those server versions and adapt all code that previously expected the sign type to always provide valid hanging sign materials.
  * The hanging sign object type is now always disabled on server versions without MC 1.20 features.
* Fix: Error "Invalid sign block face: DOWN" when trying to move a hanging sign shop object to the bottom of a block.

## v2.17.0 (2023-06-25)
### Supported MC versions: 1.20.1, 1.19.4, 1.19.3, 1.19.2, 1.19, 1.18.2, 1.17.1, 1.16.5

* Add support for 1.20.1. MC 1.20 is not supported!
  * Add camel and sniffer to the by default enabled mob types.
  * Add bamboo and cherry sign types.
  * Add hanging sign shops:
    * This is a new shop object type similar to sign shops.
    * Required permission: `shopkeeper.hanging-sign` (default: `true`).
    * Config option: `enable-hanging-sign-shops` (default: `true`).
    * Limitation: Ceiling hanging signs always use the 'attached' state for now, instead of choosing this state dynamically based on the shape of the block above.
  * The sign text is now applied to both sides of shop signs, including sign posts, wall signs, hanging signs, and wall hanging signs.
* Drop support for MC 1.18.1 and 1.19.1. These versions are only used by a few servers. Removing their support speeds up build times. MC 1.18.2 and 1.19.4 are still supported and can be updated to without issues.

**API changes:**  
* Add `ShopkeeperRegistry#getShopkeeperByBlock(String worldName, int x, int y, int z)`.

**Internal changes:**  
* Use the optimized shared block shop object id for block shop lookups in the ShopkeeperRegistry.
* Various refactors to allow the sign shop spawning logic to be reused for other types of block shop objects, such as the new hanging sign shops.

**Message changes:**  
* Added `shop-object-type-hanging-sign`.

## v2.16.5 (2023-03-20)
### Supported MC versions: 1.19.4, 1.19.3, 1.19.2, 1.19.1, 1.19, 1.18.2, 1.18.1, 1.17.1, 1.16.5

* Update for MC 1.19.4.
* Fix: When using `bypass-spawn-blocking`, we now compare spawn locations fuzzily.

## v2.16.4 (2023-01-15)
### Supported MC versions: 1.19.3, 1.19.2, 1.19.1, 1.19, 1.18.2, 1.18.1, 1.17.1, 1.16.5

* Fix: Immediately end the UI session if the GUI is not successfully opened (e.g. if another plugin cancels the corresponding InventoryOpenEvent).
* Fix: Item duplication related to shopkeeper GUIs being opened while another GUI is already open.
* Fix: Update for the latest breaking changes in the Citizens API related to metadata strings. This version of Shopkeepers is only compatible with the latest development version of Citizens!
* Fix: The villager editor did not correctly translate hex color codes of the form `&#555555Test`.

**API:**  
* Fix: `Shopkeeper#setName` did not correctly translate hex color codes of the form `&#555555Test`. The result used the alternative `&` color code instead of Minecraft's `§` code.

## v2.16.3 (2022-12-8)
### Supported MC versions: 1.19.3, 1.19.2, 1.19.1, 1.19, 1.18.2, 1.18.1, 1.17.1, 1.16.5

* Update for MC 1.19.3. Experimental MC 1.20 features are not yet supported.
* Drop support for MC 1.17 and MC 1.18. These versions are only used by a few servers. Removing their support speeds up build times by several minutes. MC 1.17.1, 1.18.2, and 1.18.1 are still supported and can be updated to without issues.
* Increase the maximum name length limit from 128 to 256 characters.
  * This is especially useful for names that contain many hex color codes.
  * This does not affect the default name length limit defined inside the default config.
  * Also increase the name length limit of Citizens shopkeepers: The latest Citizens versions support names up to 256 characters long, regardless of the mob type.

**Internal changes:**  
* Build: Update Citizens repository and bump dependency to v2.0.30.
* Build: Update VaultAPI dependency to v1.7.1 and fix retrieval from JitPack.
* Build: Exclude transitive Citizens dependencies.
* Build: Update GitHub actions.

## v2.16.2 (2022-8-10)
### Supported MC versions: 1.19.2, 1.19.1, 1.19, 1.18.2, 1.18.1, 1.18, 1.17.1, 1.17, 1.16.5

* Update for MC 1.19.2.

## v2.16.1 (2022-7-31)
### Supported MC versions: 1.19.1, 1.19, 1.18.2, 1.18.1, 1.18, 1.17.1, 1.17, 1.16.5

* Update for MC 1.19.1.
* Fix #809: Deleting a Citizens shopkeeper via the shopkeeper editor menu resulted in an error.
* We log a warning now if another plugin deletes the shopkeeper while the ShopkeeperRemoveEvent is handled.
* Config: Add setting `cancel-citizen-npc-interactions` (default `true`).  
  By default, we will now cancel interactions with Citizen shopkeeper NPCs, similar to how it is already the case for all the other types of shopkeeper objects. Not canceling the interaction with shopkeeper NPCs can result in some unintentional behaviors to take place (e.g. villager NPCs will increment the player's 'talked-to-villager' statistic).  
  However, there may be cases in which server admins don't mind these additional interaction effects and actually want the normal NPC interaction behavior to take place in addition to the shopkeeper specific behavior, for example to still trigger attached NPC commands. These server admins have the option to revert to the previous behavior by disabling this setting.
* Improve support for hex colors in translated messages and shopkeeper names.
  * We support both the Bukkit hex syntax ('&x&a&a&b&b&c&c') and the more compact hex syntax ('&#aabbcc') now. The Bukkit syntax already worked before for shopkeeper names and some texts, but is supported for more messages now.
  * When a player renames a shopkeeper using the compact hex syntax, the player's input is expanded to the Bukkit syntax due to compatibility reasons (Spigot only supports the expanded hex syntax in certain contexts). Validations like the shopkeeper name length limit are applied to the expanded syntax.
  * Note: In order for hex colored shopkeeper names to properly work in the trading UI, you need to use the latest Spigot 1.19 version or newer.

**Internal changes:**  
* Build: Compilation and running of tests also use UTF-8 as file encoding now.

## v2.16.0 (2022-06-20)
### Supported MC versions: 1.19, 1.18.2, 1.18.1, 1.18, 1.17.1, 1.17, 1.16.5

* Update for MC 1.19.
  * Added the new MC 1.19 mobs to the by default enabled mobs. If you are updating, you will have to manually enable these mobs inside your config.
  * Added an editor option to switch between different frog variants.
  * Added editor options to toggle the left and right horns of goats.
  * Added the mangrove sign variant.
* Fixed: When a currency item is changed via the "/shopkeeper setCurrency" command, we close all currently open player shopkeeper UIs now so that the change is immediately in effect.
* We no longer forcefully spawn Citizens shopkeeper NPCs when we reload their NPC data (e.g. when we apply a shopkeeper snapshot). Instead, the NPC is spawned based on its stored spawn state and location.
* Shulkers are now oriented according to the block face they are placed against. However, when the block they are attached to is broken, or they are attached to a non-full block face, or a block in the opposite direction would prevent them from opening, they can dynamically attach to another adjacent block.
* Shulkers are no longer affected by gravity. However, if they are oriented to stand on top of a block, we still adjust their spawn location downwards by up to 1 block, as we do for any other mob even if gravity is disabled.
* Added a new editor option to move shopkeepers.
  * Config: Added `move-item` (default: `ENDER_PEARL`).
  * Config: Moving of player shops can be disabled via the new `enable-moving-of-player-shops` setting (default: `true`).
  * Some default messages related to the validation of spawn locations were slightly changed to better fit when we move shopkeepers.
  * When right clicking a new shopkeeper location, we ignore interactions with certain types of blocks that are relevant for navigation, such as doors, trap doors, fence doors, buttons, levers, etc. Other types of interactable blocks (work benches, chests, signs, etc.) are not ignored, because we want shopkeepers to be placeable against those (even if some plugins may use them for navigation purposes, e.g. command signs).
  * It is also possible to move sign shopkeepers between their sign post and wall sign variant, or to change the orientation of shulker shopkeepers. However, it is not yet possible to change the sign or shulker orientation 'in place', i.e. without first moving the shopkeeper to another location.
  * Limitation: Moving Citizens shopkeepers silently fails if the Citizens plugin is not enabled currently.
* Data: Removed various old data migrations:
  * Removed the importing from the old book offer data format (changed in late MC 1.14.4).
  * Removed the importing of pre MC 1.14 cat typed ocelot shopkeepers.
  * Removed the importing of pre MC 1.14 villager profession data.
  * Removed the 'unknown' owner name validation (no longer used since late 1.14.4). This conflicted with any player actually named 'unknown'.
* Fixed: Striders no longer randomly spawn with a saddle.
* The shopkeeper and villager editors try to restore the previously open page now when the deletion confirmation is cancelled.
* Config: We no longer log a warning about invalid living entity types when we internally initialize the default config settings and are running on an older but supported MC version.

**Debugging changes:**  
* We log now when we unexpectedly encounter invalid currency items in the editors of the selling, buying, or book shopkeepers.
* Added an additional note to the error message about server downgrades not being supported when the save file cannot be loaded.

**Internal changes:**  
* Refactors related to chat input handling.
* Refactors related to shop creation, container selection, and shopkeeper placement.
* Added InteractionInput for keeping track of interaction requests.
* Added AbstractShopkeeper#teleport for moving a shopkeeper.
* Added AbstractShopObject#move which moves the shop object to its intended spawn location based on its associated shopkeeper.
* Fixed: The Citizens shopkeeper trait reference was not properly cleaned up. This also resulted in a failed assertion on plugin reloads.
* Removed no longer needed compatibility code for versions below 1.16.5:
  * Removed the no longer needed compatibility code related to raiders, piglins, zoglins, and the despawn delay of wandering traders.
  * Removed the MC 1.16 features guard. These features are always available now, so the guard is no longer required.
  * Use the EntityType and Material enums instead of String-based names for any entity types and materials available in Bukkit 1.16.
  * Removed the restriction of not being able to interact with shopkeeper mobs while holding a written book. The underlying Minecraft issue (MC-141494) has been fixed by now.
  * Just in case, we still disallow the double clicking of items in the trading UI inventory if they match the trade result item. But this check should no longer be required and no longer results in a restriction that is not also present in vanilla Minecraft: MC-129515 has been fixed by now by disabling all double clicking of items in the trading UI (MC-148867).
  * Test cases related to written books have been enabled again. They were broken in Bukkit versions before 1.16.

**Message changes:**  
* Changed `mob-cannot-spawn-on-peaceful-difficulty`.
* Changed `restricted-area`.
* Added `button-move`.
* Added `button-move-lore`.
* Added `click-new-shop-location`.
* Added `shopkeeper-moved`.
* Added `shopkeeper-move-aborted`.
* Added `button-frog-variant`.
* Added `button-frog-variant-lore`.
* Added `button-goat-left-horn`.
* Added `button-goat-left-horn-lore`.
* Added `button-goat-right-horn`.
* Added `button-goat-right-horn-lore`.

## v2.15.1 (2022-03-27)
### Supported MC versions: 1.18.2, 1.18.1, 1.18, 1.17.1, 1.17, 1.16.5

* Fixed: The refactors in version 2.15.0 broke the deletion of shopkeepers of inactive players.

## v2.15.0 (2022-03-27)
### Supported MC versions: 1.18.2, 1.18.1, 1.18, 1.17.1, 1.17, 1.16.5

* Added support for MC 1.18.2. Make sure that you are using the very latest build of Spigot 1.18.2, because initial builds of 1.18.2 had some chunk loading related issue that might affect the activation (spawning, ticking, etc.) of shopkeepers.
* Dropped support for all Bukkit versions below 1.16.5 (1.14.4, 1.15.2, 1.16.1-1.16.4).
* Bumped the Towny dependency to version 0.98.1.0 and using the glaremasters repository for it now (Jitpack seems to be having issues with Towny currently).
* Config: The translations repository has been renamed and is now located at `https://github.com/Shopkeepers/Language-Files`.
* Config: Added config option `placeholder-item` that allows changing the placeholder item (Thanks to oltdaniel, PR #762). It is also possible to disable placeholder items now by setting the item to AIR.
* Config: Changed the default placeholder item type from `NAME_TAG`, which is difficult to obtain in vanilla Minecraft, to `PAPER`.
* Config: Similar to Mohist servers, we also automatically enable the `disable-inventory-verification` setting now when we detect that we run on a Magma server.
* Added an editor option to toggle the saddle of horse shopkeepers on and off.
* Data: The shopkeeper data version has changed from `1` to `2`.
* Reduced the range at which shopkeeper mobs look at players from 12 to 6 blocks.
* Command: The "/shopkeeper [shop-type] [object-type]" command no longer suggests completions for disabled shop and object types, or shop and object types for which the executing player does not have the usage permission.
* Improvements related to the deletion of shopkeepers that are owned by inactive players:
  * We now log how many days ago the players were last seen when we delete their shops.
  * Fixed: If the plugin is reloaded, and the still pending asynchronous task that checks for inactive players is somehow taking longer than 10 seconds to complete, and the `player-shopkeeper-inactive-days` setting is changed to `0` (i.e. disabled) during the reload, there was a chance for shop owners to be incorrectly considered 'inactive' and their shopkeepers deleted.
  * Related to that, this asynchronous task also aborts itself now when it detects that it has been cancelled, which happens when the plugin is disabled.
  * Fixed: When we delete the shops of an inactive player, we now ignore any shops that have already been removed for other reasons in the meantime.
* We log a warning now whenever a shopkeeper mob has been removed due to the world's difficulty being set to peaceful. This warning is only logged once per affected shopkeeper and then skipped until the difficulty has changed.
* It is no longer possible to create mob shopkeepers when they would not be able to spawn due to the difficulty being set to peaceful.
* Improved the feedback messages that are sent when a shopkeeper cannot be created at a specific location: The previously used generic `shop-create-fail` message was replaced with dedicated messages for the different reasons for why the shopkeeper cannot be created.
* Config: Added setting `invert-shop-type-and-object-type-selection` (default: `false`), which allows to invert how the shop type and the shop object type are selected with the shop creation item. This might for example be useful for servers that disable all shop types but one.
* Config: Added setting `ignore-failed-server-assumption-tests` (default: `false`), which allows to enable the plugin anyway, even when a server incompatibility has been detected during the initial server assumption tests.
* Fixed: Citizens shopkeepers are able to move, teleport, and change their location while the Shopkeepers plugin is not running, or while the shopkeeper's chunk is not active currently. Previously, we only updated the shopkeeper's location during shopkeeper ticking, i.e. when the shopkeeper's chunk has been activated. However, if the NPC was moved to a different chunk (or even world), and the shopkeeper's previous chunk has never been activated since then, the spawned NPC might no longer have been recognized as a shopkeeper. One noticeable effect of this was that the NPC could no longer be interacted with. This has been fixed by also updating the shopkeeper's location in various other circumstances now.
* Fixed: A related but more minor issue has been that Citizens NPCs can already be spawned while their chunk is still pending to be activated by the Shopkeepers plugin. During this short time period (roughly one second after chunk loads), the Citizens NPCs were not yet recognized as shopkeepers. This has been fixed by separating the registration of ticking (i.e. active) shopkeepers from the registration of spawned shop objects: Citizens shopkeepers now register their NPC entity already before the chunk is activated.
* Fixed: We no longer attempt to save the data of Citizens shopkeeper NPCs when the corresponding NPC has not yet been created (i.e. when no NPC has been attached to the shopkeeper yet).
* Fixed: Updating a shopkeeper's location also updates the shopkeeper's activation state now. Previously, it was possible for a shopkeeper's new chunk to not get activated until the chunk is reloaded.
* Command: Added command "/shopkeeper setCurrency ['base'|'high']", which allows you to change the currency item(s) from in-game.
* Permission: Added permission `shopkeeper.setcurrency` (default: `op`) which provides access to the new set-currency command.
* Fixed: When setting the zero currency item to AIR, the selling and book shopkeepers were not able to derive the price of a trade from the editor when only the high currency price is specified.
* Added placeholder items for empty trade slots inside the editors of player shops.
  * Config: Added various settings to specify the editor placeholder items for empty slots of partially set up trades (default: barriers, as it has been the case before), as well as for completely empty trade columns (default: gray stained glass panes).
  * When you disable the high currency inside the config (by setting it to AIR), you will now need to manually adjust the corresponding empty placeholder items for the affected high currency slots (for example by setting them to AIR). This is a compromise that allows these slots to be set to arbitrary items, even if the high currency is disabled.
  * Added display names and lore to these placeholder items that explain their purpose and usage inside the editor.
  * Config: The settings for the zero currency items are no longer used (they have been replaced with new settings) and are automatically removed during a config migration.
  * The "checkItem" command no longer compares the held items with the zero currency / editor placeholder items.
* Config: Bumped the config version to 5.
* Command: The "give" and "giveCurrency" commands no longer allow item amounts greater than 1024. Previously, these commands silently truncated any amounts greater than 1024.
* Command: The "giveCurrency" and "setCurrency" commands refer to the low currency as 'base' currency now.
* Command: Different types of command executors (e.g. command blocks and the console) no longer share their pending confirmation state.
* Command: Confirmations account for proxied command senders now: Since the original caller of the command receives the command's feedback messages, we also require confirmation from the original caller.
* Fixed: When removing or translating color codes in text we also account for Bukkit's hex color code now (x).

**Debugging changes:**  
* We still clear all shopkeeper registry collections during plugin shutdown, just in case something went wrong earlier and prevented elements from being properly removed. But we also log a warning in this case now.
* Added additional debug output when chunks are activated and deactivated. Also, the debug output when shopkeepers are spawned due to chunk activations has slightly changed.
* The debug output for entity interactions is no longer limited to living entities, and also mentions the entity type now.
* Converted various Bukkit scheduler tasks from lambda expressions and anonymous classes to dedicated classes, so that they can be better identified in timing reports.
* Added additional log output when we fail to re-register a listener when we reorder the event handlers of some event. In order to resolve certain plugin incompatibility issues, we reorder the event handlers for some types of events. However, as it turns out, some plugins modify the internals of the Bukkit event system. If these plugins have flaws, they can break our event handler reordering. This additional log output should make it easier to identify the culprit plugin in those cases.
* We log now whenever we save the config.

**API changes:**  
* The ShopkeeperRemoveEvent is now called earlier during the shopkeeper removal, before the shopkeeper is deactivated (i.e. despawned, UIs closed, ticking stopped, etc.).
* Added nullness annotations.
* Some methods are more strict now and no longer accept null as input.
* For consistency, the return types of various methods that return unmodifiable collections have been changed to use wildcards to indicate that the returned collections are unmodifiable.
* Various minor javadoc changes, for example to clarify behavior related to shop object spawning.

**Internal changes:**  
* Refactors related to the deletion of shopkeepers that are owned by inactive players.
* Replaced ShopObjectType#isValidSpawnLocation with AbstractShopObjectType#validateSpawnLocation, which additionally sends feedback to the player who is trying to create the shopkeeper.
* Various internal changes to Citizens shop objects, mostly related to how they register their spawned NPC entities, and how they update the location of their associated shopkeepers.
* Various refactors related to the shopkeeper registry, shopkeeper activation, ticking, and spawning.
  * Fixed: We better account for dynamic shopkeeper registry, location, activation, and spawn state changes during various internal shopkeeper and shop object callbacks, such as when a shopkeeper starts/stops ticking, or a shop object is spawned/despawned. This has become necessary because Citizens shop objects might update their shopkeeper's location during ticking stop now.
  * Updating the location of a shopkeeper from one chunk to another will no longer remove and subsequently recreate the world data if this shopkeeper is the only shopkeeper within the world.
  * Fixed: When removing or moving a shopkeeper in the chunk map, we no longer use the shopkeeper's current world, but the world it was last stored by.
  * Internal API: Changes to block and entity object ids, and to how active shopkeeper blocks and entities are registered and tracked.
  * Internal API: Extended the implementation notes in AbstractShopObject with regard to the registration of spawned shop objects, and shop objects that can change their location.
  * Various constants related to shopkeeper ticking have been moved from AbstractShopkeeper into ShopkeeperTicker.
  * Each ticking group stores its respective shopkeepers separately now. We therefore no longer need to iterate and filter all active shopkeepers, but can directly iterate the shopkeepers of the currently active ticking group.
  * Internal API: Chunk activations immediately start the ticking of activated shopkeepers now, even if these shopkeepers are not immediately spawned. Shop objects can use `#isSpawningScheduled` to check whether their spawning is scheduled, and then skip any shop object checks and spawning attempts in the meantime. This 'spawning-scheduled' state is also active while shopkeepers are pending to be respawned during world saves.
  * Internal API: `#onChunkActivation/Deactivation` methods in AbstractShopkeeper and AbstractShopObject have been renamed to `#onStart/StopTicking`.
  * Internal API: AbstractShopkeeper is responsible now for invoking the tick callbacks of its shop object. AbstractShopkeeper keeps track now whether it is currently ticking. This flag is used to skip ticking the shop object if the shopkeeper aborts its own ticking while it is being ticked.
  * Internal API: AbstractShopkeeper and AbstractShopObject have additional callbacks now that are run at the beginning and at the end of a shopkeeper tick.
  * Internal API: Minor changes to the tick visualization of shopkeepers. The shopkeeper is itself responsible for invoking the tick visualization at the end of a tick now.
  * We no longer clear the spawn queue right away now during plugin disable (i.e. when unloading all shopkeepers): This optimization is not expected to provide much benefit anyway, as the spawn queue is intentionally not used in situations in which it could fill up a lot.
  * We also track now if a world is currently being saved even if the world does not contain any shopkeepers yet, because shopkeepers might be added to the world while it is being saved. The world data of the ShopkeeperSpawner is cleared again once the world is no longer loaded and contains no shopkeepers anymore.
  * The world save respawn task is now started and assigned before the world's shopkeepers are despawned. This ensures that the world is already marked as 'currently-saving' when the shopkeepers are despawned.
  * Internal API: Some callback functions have been renamed and had changes to their visibility. Some externally called callback functions are final now and delegate to other internal callback functions.
  * Internal API: Some calls to shopkeeper callback functions are better guarded against unexpected exceptions now. But this does not yet apply to all callback functions.
  * Internal API: Various other internal changes that might also affect the internal API of shopkeepers and shop objects.
* Internal API: Added the ability to attach external 'components' to shopkeepers, which can provide additional state and/or functionality related to a particular shopkeeper.
* Refactors and preparations to support more than two currency items.
* Reduced the frequency with which we check for remaining async tasks during plugin shutdown.
* Added nullness annotations and the Checker Framework.
  * In order to also satisfy the Eclipse null annotation checker all packages have been annotated with a custom NonNullByDefault annotation, and the use of some Checker Framework specific features has been avoided.
  * Several non-null variants of existing methods have been added.
  * Added a utility class 'Unsafe' with various utility methods to perform unchecked casts or suppress false-positive nullness related warnings.
  * Various methods are more strict now and no longer accept null as input.
  * Fixed: When we verify that a given collection does not contain null, we now account for non-null collections that may throw a NPE for this containment check.
  * CommandContext#get can no longer be used for nullable arguments. Instead, a new #getOrNull method has been added.
  * Property validators no longer have access to the property. This resolved some nullness related type checking difficulty. But ideally this access should not be required anyway.
  * Some compound properties (e.g. the container property of player shopkeepers) better account for null values now.
  * Added SKUser#EMPTY, which is for example used as the temporary initial value for the owner of player shops.
  * Fixed a nullness related issue in DataMatcher#Result: The left and right objects can each be null, but they cannot both be null.
  * DataAccessor has been split into two separate interfaces: DataSaver and DataLoader. This allows the use of different type parameter bounds for each interface.
  * There are some cases in which DataSerializer#serialize can return null, so we need to account for that. The internal documentation on that has been updated.
* The ItemData serializer can now be configured to serialize the display name, lore, and loc name in the plain text format with color codes instead of Json. This is primarily used in test cases in which we need to compare the serializer output with item text data in the plain format.
* Message keys inside the language files may now use dots to indicate structure.
* Minor command library refactors.
* Various other internal refactors and documentation changes.
* Various code formatting changes.

**Message changes:**  
* Slightly changed the default `must-target-container` message.
* Changed `invalid-snapshot-id`.
* Changed `invalid-snapshot-name`.
* Changed `currency-items-given`.
* Changed `currency-items-received`.
* Removed `shop-create-fail`.
* Removed `high-currency-items-given`.
* Removed `high-currency-items-received`.
* Removed `high-currency-disabled`.
* Added `button-horse-saddle`.
* Added `button-horse-saddle-lore`.
* Added `must-target-block`.
* Added `missing-spawn-location`.
* Added `spawn-block-not-empty`.
* Added `invalid-spawn-block-face`.
* Added `mob-cannot-spawn-on-peaceful-difficulty`.
* Added `restricted-area`.
* Added `location-already-in-use`.
* Added `must-hold-item-in-main-hand`.
* Added `currency-item-set-to-main-hand-item`.
* Added `command-description-set-currency`.
* Added `selling-shop.empty-trade.result-item`.
* Added `selling-shop.empty-trade.result-item-lore`.
* Added `selling-shop.empty-trade.item1`.
* Added `selling-shop.empty-trade.item1-lore`.
* Added `selling-shop.empty-trade.item2`.
* Added `selling-shop.empty-trade.item2-lore`.
* Added `selling-shop.empty-item1`.
* Added `selling-shop.empty-item1-lore`.
* Added `selling-shop.empty-item2`.
* Added `selling-shop.empty-item2-lore`.
* Added `buying-shop.empty-trade.result-item`.
* Added `buying-shop.empty-trade.result-item-lore`.
* Added `buying-shop.empty-trade.item1`.
* Added `buying-shop.empty-trade.item1-lore`.
* Added `buying-shop.empty-result-item`.
* Added `buying-shop.empty-result-item-lore`.
* Added `trading-shop.empty-trade.result-item`.
* Added `trading-shop.empty-trade.result-item-lore`.
* Added `trading-shop.empty-trade.item1`.
* Added `trading-shop.empty-trade.item1-lore`.
* Added `trading-shop.empty-trade.item2`.
* Added `trading-shop.empty-trade.item2-lore`.
* Added `trading-shop.empty-result-item`.
* Added `trading-shop.empty-result-item-lore`.
* Added `trading-shop.empty-item1`.
* Added `trading-shop.empty-item1-lore`.
* Added `trading-shop.empty-item2`.
* Added `trading-shop.empty-item2-lore`.
* Added `book-shop.empty-trade.result-item`.
* Added `book-shop.empty-trade.result-item-lore`.
* Added `book-shop.empty-trade.item1`.
* Added `book-shop.empty-trade.item1-lore`.
* Added `book-shop.empty-trade.item2`.
* Added `book-shop.empty-trade.item2-lore`.
* Added `book-shop.empty-item1`.
* Added `book-shop.empty-item1-lore`.
* Added `book-shop.empty-item2`.
* Added `book-shop.empty-item2-lore`.
* Added `unknown-currency`.

## v2.14.0 (2021-12-17)
### Supported MC versions: 1.18.1, 1.18, 1.17.1, 1.17, 1.16.5, 1.15.2, 1.14.4

* Updated for MC 1.18 and MC 1.18.1.
* Updated the 'compatibility mode' implementation, which broke with the Miecraft 1.18 update. However, due to some server internal mapping changes, it is likely that this compatibility mode will break again in future updates.
* Added the option for admins to create and restore snapshots of shopkeepers.
  * Each snapshot captures the shopkeeper's dynamic state (i.e. its trades, shop object configuration, etc.), a timestamp, and is associated with a unique name that is provided when the snapshot is created.
  * The name can currently be at most 64 characters long and not contain any color codes.
  * This feature is currently meant to only be used by admins and third-party plugins. Shop owners are not yet able to create and restore snapshots of their own shops.
  * The shopkeeper data of snapshots is automatically migrated during plugin startup, just like the shopkeeper's normal data.
  * Shopkeeper snapshots are currently expected to be used sparsely. In order to guard against the unnoticed creation of excessive amounts of snapshots, we log a warning whenever a shopkeeper has more than 10 snapshots.
  * When formatting the timestamp of a snapshot for display, we currently use the server's default time zone. There is no option in Shopkeepers yet to use a different timezone. If you want to use a different timezone for formatting the timestamps, you can change your server's default timezone via the JVM command line argument `-Duser.timezone=America/New_York` when you start your server.
  * Command: Added commands `/shopkeeper snapshot [list|create|remove|restore]` to manage shopkeeper snapshots.
  * Permission: Added permission `shopkeeper.snapshot` (default: `op`) that provides access to the new snapshot commands.
  * Experimental: It is also possible to capture and restore the NPC state of Citizens NPC shopkeepers.
    * Config: Added setting `snapshots-save-citizen-npc-data` (default: `true`) that controls whether shopkeeper snapshots capture and restore Citizens NPC data.
    * When this setting is disabled, the Shopkeepers plugin automatically deletes all previously saved NPC data again.
    * Capturing the data of a Citizens NPC will not work if the NPC is not available at the time the snapshot is created (e.g. if the Citizens plugin is not running currently).
    * If the Citizens NPC is not available when a snapshot is restored, the shopkeeper remembers the NPC data until the NPC becomes available again and the stored NPC state can be applied. If another snapshot is restored in the meantime that does not store any NPC data, we retain the previously restored but not yet applied NPC state.
* Added editor options to change the puff state of puffer fish, as well as the pattern and colors of tropical fish.
* Added sound effects when a trade succeeds or fails.
  * Config: These sound effects can be changed or disabled (by setting their volume to zero) via the new config settings `trade-succeeded-sound` and `trade-failed-sound`.
* Added settings to simulate the trading sounds of vanilla villagers and wandering traders.
  * Config: Added setting `simulate-villager-trading-sounds` (default: `true`).
  * Config: Added setting `simulate-villager-ambient-sounds` (default: `false`).
  * Config: Added setting `simulate-wandering-trader-trading-sounds` (default: `true`).
  * Config: Added setting `simulate-wandering-trader-ambient-sounds` (default: `false`).
  * Config: Added setting `simulate-villager-trading-sounds-only-for-the-trading-player` (default: `true`).
  * By default, we now simulate the trading sounds of villagers and wandering traders for the trading player. These sounds are only played if the trading player is in the vicinity of the shopkeeper. Trading remotely will not play these sounds.
  * Debug: Debug output for these simulated villager sounds can be enabled via the existing debug option `regular-tick-activities`.
* Shopkeepers store the yaw angle now with which they are initially spawned.
  * When placing a shopkeeper on top of a block, the yaw angle is chosen so that the shopkeeper faces towards the player who is creating the shopkeeper. When placing a shopkeeper against the side of a block, the shopkeeper is rotated according to the direction of the targeted block side.
  * Existing shopkeepers will have a yaw of 0, i.e. they face south just like before. In the future, it will be possible to reposition shopkeepers and thereby also adjust the yaw of already existing shopkeepers. But this is not yet available.
  * Data: Sign shopkeepers no longer store their sign facing. Instead, this facing is now derived from the shopkeeper's yaw. Existing sign shops will automatically migrate their currently stored sign facing to the shopkeeper's yaw.
  * Shopkeeper mobs will rotate back to their initial direction now when there is no player to look at. However, this requires a player to still be somewhat nearby, since we only tick shopkeeper mobs when players are nearby.
* Config: Added setting `set-citizen-npc-owner-of-player-shops` (default: `false`).
  * When enabled, we set the Citizens NPC owner of player-owned NPC shopkeepers. When disabled, we automatically remove any previously set Citizens NPC owners from those player-owned NPC shopkeepers again.
  * By enabling this setting, and configuring the Citizens command permissions for your players accordingly, you can allow shop owners to use the commands of the Citizens plugin to edit the Citizens NPCs of their NPC shopkeepers.
* Fixed: Wandering traders and trader llama shopkeepers despawned roughly every 40 minutes due to Minecraft's despawning timer still being active for these mobs, even when their AI is disabled. However, this issue has been rather minor, because the affected shopkeepers automatically respawned a few seconds afterwards. This issue has now been fixed by setting the `DespawnDelay` of wandering traders to `0` and marking trader llamas as 'tamed', which disables Minecraft's despawning timer for these mobs.
* Fixed: We were not able to detect item stack deserialization issues, because Bukkit only logs an error in this case instead of throwing an exception. This also resulted in the loss of shopkeeper data when someone tried to revert from one server version to an older one, because the loading of item stacks that were saved on a newer server version is not supported by Bukkit. We are now able to detect these deserialization issues and then shut down the plugin before any data is lost.
* Fixed: Shopkeepers could lose some of their trades during item migrations. When an item was migrated, but the subsequent trades did not require any item migrations, these subsequent trades were lost. However, it was relatively unlikely to encounter this issue in practice. An exception to this is in combination with the following Paper-specific issue that only affects trades with certain enchanted book items.
* Fixed: On Paper servers, the comparisons of enchanted book items with multiple stored enchantments were not working as expected. The issue was caused by the server reordering the stored enchantments in some cases, as well as Paper automatically converting any deserialized Bukkit item stacks to CraftItemStacks, which behave differently when being compared to other CraftItemStacks. One known effect of this was that the above issue of trades potentially being lost due to item migrations would be encountered more likely on Paper servers, because the reordering of these stored enchantments would be detected as an 'item migration' by the Shopkeepers plugin. It also caused an unnecessary save of all shopkeeper data after every plugin reload.
* Fixed: The "/shopkeeper removeAll" command always tried to remove all player shops, instead of only the shops of the executing player.
* Data: Various kinds of invalid shopkeeper data and failed data migrations that have previously been ignored or only triggered a warning are now checked for and will prevent the shopkeeper from loading. Most notably, this includes the data of shopkeeper offers, and the hire cost item of player shops. Previously, this invalid data would be cleared. Now, it is retained for a server admin to investigate the issue.
* Data: It is invalid now to mix legacy and non-legacy book offer data (changed during late MC 1.14.4).
* When shopkeepers of inactive players are removed, we now trigger an immediate save, even if the `save-instantly` setting is disabled.
* Versioning: Snapshot builds will now include the Git hash in their plugin version.
* Debug: The 'shopkeeper-activation' debug option will now log additional details about every single shopkeeper activation and deactivation, instead of only logging a summary about how many shopkeepers were activated per chunk.
* Various minor changes to error and debug log messages:
  * Some error and debug log messages have slightly changed.
  * Log messages involving a specific shopkeeper are more consistently formatted now.
  * Some groups of error messages are now logged as a single message.
  * In a few remaining cases, stack traces are no longer printed directly to the server log, but through the plugin logger.
* During the initial plugin startup, we now test if the server meets certain basic assumptions about its API implementation. In order to prevent damage due to unexpected server and plugin behavior, the plugin shuts itself down if any of these assumptions turn out to be incorrect. These tests are meant to be lightweight, but may be expanded in the future.
* Debug: Added a debug log message whenever a player tries to set an invalid shop name.
* Debug: Added a debug log message when the creation of a Citizens NPC fails.
* Data: Removed various 3 year old data migrations:
  * We no longer check for and remove entity uuids from the data of living entity shopkeepers.
  * The object data of shopkeepers is expected to be located in its own dedicated 'object' section.
  * The stored object type identifiers of shopkeepers are expected to perfectly match the registered shop object types. They are no longer normalized and fuzzy matched.
* Data: We no longer generate a new unique id if the shopkeeper data is missing the shopkeeper's unique id. Instead, the loading of the shopkeeper fails now in this case.
* Data: All item stacks are now (shallow) copied before they are saved to the shopkeeper data. This prevents SnakeYaml from representing the item stacks using anchors and aliases (a Yaml feature) inside the shopkeepers save file if the same item stack instances would otherwise be saved multiple times in different contexts.
* Command: For consistency among commands, and to avoid certain ambiguous command parsing cases, the "edit", "remote", and "remove" commands no longer merge trailing arguments to derive the target shopkeeper name. It is still possible to target shopkeepers with names that consist of multiple words by using a dash as word separator instead of a space.
* Command: Shopkeeper and entity command arguments now propose the ids of targeted shopkeepers and entities.
* Fixed: The "testSpawn" debug command would respawn the shopkeepers in the executing player's chunk, but would not immediately update their object id registrations. The shop objects will now automatically and immediately inform the shopkeeper registry about those object id changes.
* Fixed: When changing the container location of a player shop via the API, the protection for the previous container was not properly disabled.
* Fixed: Any code that retrieves the container block of a player shopkeeper accounts now for the fact that the block might be null. This was only really an issue when another plugin invokes internal operations that require the container block to be available in a situation in which it might not be available currently.
* Fixed: The order of the enabled living shop object types was not updated on plugin reloads based on their order inside the config.
* Debug: Removed the debug option 'capabilities' again. We already always log whether server version specific features are enabled. This also resolves an internal issue related to whether the config has already been loaded at the time this debug option is checked.
* Fixed: In order to resolve compatibility issues with plugins that modify chat messages at lowest event priority, we now enforce that our chat input event handler always executes first. This should resolve compatibility issues with shopkeeper names not being considered valid, because some other plugin injected color codes into the player's chat message before we were able to process it.
* Fixed: Since Bukkit 1.16.5, the data version was no longer guaranteed to be the first entry of the save file, and the log message for the number of loaded shopkeepers has been off by one if the save file did not contain any data version yet.
* Fixed: We now check if the UI session is still valid before handling an inventory event at the HIGH event priority. Previously, the UI session was simply expected to still be valid. But this assumption can be violated by plugins that (incorrectly per API documentation) close the inventory during the handling of an inventory event.
* Fixed: Sound effects played to players would sometimes appear to be played slightly to the left or the right of the player's head.
* Fixed: When a NPC player shopkeeper moved to a different world, its container location (which is currently expected to always be located in the same world as the shopkeeper) and the protection of the container were not getting updated.
* Data: Under certain circumstances, one of the publicly posted snapshots of this update accidentally saved shopkeeper ids as normal attributes into the save file. The save file data version was bumped from 2 to 3, so that any previously saved shopkeeper ids are automatically removed again.
* Data: Improved the detection of server and plugin downgrades.
  * We now use the data version that is stored inside the Shopkeepers save file to detect Minecraft server and Shopkeepers plugin downgrades. When a downgrade is detected, we log an error and disable the plugin to prevent any potential loss of data.
  * There is one minor issue with this: After server downgrades, Spigot fails to load item stacks that are stored inside the save file. This causes the loading of the save file to fail before we are able to extract the data version. Our own Minecraft server downgrade check will therefore usually not actually be reached. However, the plugin will disable itself in this situation anyway. But the error message will then be different and more verbose. It should, however, also mention that the server downgrade is the issue.
  * In order to be able to more reliably detect downgrades of the Shopkeepers plugin that might affect the saved shopkeeper data, we added a new 'shopkeeper data version' component to the data version that is stored inside the save file. The previous 'shopkeeper storage version' was, and still is, used for changes that require a save of all shopkeepers, such as for example when the storage format has changed. The new shopkeeper data version, however, is meant to be incremented more frequently, namely after every change to the data format of shopkeepers and shop objects. Usually, these changes only affect a subset of the shopkeepers and shop objects, so no full save of all shopkeepers is required when this data version component has changed.
  * These shopkeeper and shop object data format changes also include the addition of new shopkeeper or shop object attributes: Since older plugin versions are not aware of these new attributes, the data for these attributes can get lost when an older plugin version would be allowed to load and later save shopkeepers that previously contained data for these attributes.

**API changes:**  
* Additions to manage the snapshots of shopkeepers.
* Added PlayerInactiveEvent that can be used to react to inactive players being detected, or alter which of their shopkeepers are deleted.
* Added User interface to represent players that the plugin knows about. However, this is not yet used throughout the API.
* Added Shopkeeper#getYaw().
* Added Shopkeeper#getLogPrefix(), #getUniqueIdLogPrefix(), and #getLocatedLogPrefix().
* Shopkeeper#getLocation() will include the shopkeeper's yaw now.
* ShopCreationData#getTargetedBlockFace() no longer determines the facing of sign shops. Instead, sign shops use the yaw of the spawn location now to derive their facing.
* Added UnmodifiableItemStack#shallowCopy().
* Various Javadoc additions, improvements, and fixes, and added additional validations for some constructor and method arguments.
* Deprecated DefaultShopTypes#getAdminShopType() and #ADMIN(), and added #getRegularAdminShopType() and #ADMIN_REGULAR() as alternatives.
* Deprecated the previous constructors and factory methods of AdminShopCreationData and PlayerShopCreationData, and added corresponding alternatives that directly require an AdminShopType or PlayerShopType respectively.
* Deprecated the superfluous LivingShopObjectTypes#getAliases(EntityType).
* Removed various previously deprecated methods in UIRegistry, Shopkeeper, and PlayerShopkeeper.
* PlayerShopkeeper#setForHire only resets the shopkeeper name now if the shopkeeper was previously for hire.
* Various internal methods that are not meant to be called by API users have been moved from public API classes into separate 'ApiInternals' and 'InternalShopkeepersAPI' classes.
* Removed the recently added but immediately deprecated factory method for unmodifiable item stacks from ShopkeepersPlugin again.
* Clarified that the block returned by PlayerShopkeeper#getContainer() can be null if the container's world is not loaded currently.

**Various internal build changes:**  
* We use JDK 17 now to build Shopkeepers. However, for compatibility with older and still supported MC versions, we still only use Java 8 compliant features.
* Switched from Maven to Gradle.
* Refactored the project structure to more closely align with Maven's recommended layout and resolve some IDE issues.
* The Maven publication of the Shopkeepers API also includes the API Javadocs now.
* We also publish a 'ShopkeepersMain' artifact now that contains the internal plugin code, but omits all NMS modules. Any plugins that rely on these internals, but don't require the NMS modules, can now depend on this artifact as an alternative to depending on the complete plugin jar.
* Removed the unused jenkins and release build profiles from Maven.
* All external Maven repositories are accessed via https now.
* Bumped the Vault dependency to version 1.7 and updated its repository.
* Reduced the Citizens dependency to the 'citizens-main' portion of Citizens.
* Bumped the Citizens dependency to version 2.0.29.
* Bumped the WorldGuard dependency to version 7.0.0, updated the repository, and removed the no longer needed Paper repository.
* Moved all auxiliary build scripts into a separate 'scripts' folder and made them less reliant on the directory they are called from.
* The primary 'build' script automatically invokes the 'installSpigotDependencies' script now.

**Internal API changes:**  
* Various internal UI, editor UI, and trading UI related refactors.
  * Changed how the trading UI handler represents the trade that is currently being processed.
  * It is now possible to register TradingListeners with the TradingHandler that are informed on various events during the trade handling.
  * Moved various editor internal classes into their own files.
* ProtectedContainers requires a BlockLocation now when adding or removing a container protection.
* Various changes to the loading, saving, and migration of shop offers. They use the new internal data serialization and property APIs now to save and load their data.
* Removed various no longer needed utility methods related to the saving and loading of item stacks.
* CitizensShopkeeperTrait#getShopkeeper() returns an AbstractShopkeeper now.
* Added SKSignShopObject#getSignType().
* It is now the responsibility of the shop object to inform about object id changes, even when the shop object is being spawned, despawned, or ticked.
* Added AbstractShopObject#respawn().
* Various internal fields of AbstractPlayerShopkeeper and AbstractAdminShopkeeper are no longer directly accessible by subclasses, but need to be accessed via their respective getters and setters now.
* Added the possibility to save and load a shopkeeper's dynamic state.
* Various changes to the internal representation and handling of shopkeeper data, shop object data, and data migrations.
* Instead of using Bukkit's ConfigurationSection interface, we now use our own DataContainer interface to represent data in memory.
  * Most uses of ConfigurationSection have been replaced with DataContainer, such as for representing config, language file, shopkeeper, and shop object data.
  * A ConfigData interface extends DataContainer to provide access to default config values.
  * ShopkeeperData and ShopObjectData are DataContainer wrappers for the data of shopkeepers and shop objects. In the future, they may contain additional methods specific to the handling of shopkeeper and shop object data.
* Various refactors related to shop object properties and the internal properties API. Shop objects also take into account now that their property values might get dynamically reloaded at runtime.
* Various uses of ShopkeeperCreateException have been replaced with the better fitting InvalidDataException.
* Minor changes to the shop and shop object type constructors.
* Various changes to how shopkeepers are constructed, initialized, and loaded. The shopkeeper id is passed around as part of the shopkeeper data now.

**Other internal changes:**  
* Various refactors and internal documentation improvements to the DataVersion class, and to how the 'missing data version' state is represented.
* When we are not able to retrieve the server's Minecraft data version, we now abort the initialization of the plugin with an error. Previously, we would have continued with a dummy data version value in this case, which would have bypassed our new version checks.
* Fixed: The arguments of translatable text components were not getting correctly converted to arguments of the corresponding Spigot-based text components. However, since we don't use any translatable texts with arguments yet, this fix has no noticeable impact.
* Data: The world, coordinates, and yaw are now omitted from the save data of virtual shopkeepers. However, this has no effect yet, since virtual shopkeepers are not yet properly supported.
* Shopkeepers are now deactivated before they are despawned. This avoids having to process the object id change of the shop object being despawned when the shopkeeper is going to be deactivated anyway.
* Fixed: The ShopkeeperByName command argument did not take the 'joinRemainingArgs' argument into account.
* Fixed: The parsing of command arguments failed in cases in which command arguments depend on the context provided by fallbacks of earlier arguments.
* Refactors related to the removal of shopkeepers of inactive players.
* We now enforce for all of our event handlers at lowest priority that they are executed first.
* APIMirrorTest no longer uses Hamcrest matchers. This also resolves some JUnit deprecations.
* The save operations of the shopkeeper storage and the CSV trade logger will now restore any caught Thread interruption status for anyone interested in it. Other than that, these operations still ignore Thread interruptions, because we prefer to keep trying to still save the data to disk after all.
* Admin shopkeepers normalize empty trade permissions in the save data to null now, similar to AdminShopkeeper#setTradePermission.
* Removed an old update mechanism for sign shops: If the sign block is not available at the time it is meant to be updated, this would update the sign later, once it is available again. This mechanism should no longer be required: The sign is already updated whenever it is spawned, and we dynamically spawn and despawn the sign with chunk loads, and dynamically respawn it if it is detected to be missing.
* ItemData caches the meta type of items now instead of always determining it freshly from a newly serialized ItemMeta instance.
* The shop object types for the various mob shop objects use composition now instead of deriving their own shop object type from a base class.
* Fixed: Timer was not correctly updating its state when being stopped, breaking an assertion when the timer was started again. The Timer class more strictly checks its expected states now and logs an error with stack trace whenever one of its operations is called in an unexpected state for the first time.
* Spigot plans to remove the Apache Commons Lang library from Bukkit. We therefore replaced all uses of the Apache Commons Lang Validate class with Guava's Preconditions class (this applies primarily to the API module).
* Changes to account for potential future changes to how Bukkit implements the representation of configuration sections for Yaml configurations: There are plans in Spigot to change how Yaml configurations save their sections. These changes move the representation of configuration sections from Bukkit's YamlRepresenter into YamlConfiguration. However, since we reuse Bukkit's YamlRepresenter for our own Yaml serialization purposes, we need it to still be able to represent configuration sections in the future. We now account for this potential future change and ensure that our Bukkit-based Yaml representer will still be able to represent configuration sections.
* Various other minor internal refactors and Javadoc improvements.

**Message changes:**  
* Slightly changed the default messages of `type-new-name`, `name-set`, and `name-invalid`. These messages, as well as `name-has-not-changed`, can now access the new name via the argument `{name}`.
* Added `button-tropical-fish-pattern`.
* Added `button-tropical-fish-pattern-lore`.
* Added `button-tropical-fish-body-color`.
* Added `button-tropical-fish-body-color-lore`.
* Added `button-tropical-fish-pattern-color`.
* Added `button-tropical-fish-pattern-color-lore`.
* Added `button-puffer-fish-puff-state`.
* Added `button-puffer-fish-puff-state-lore`.
* Added `date-time-format`.
* Added `shop-no-longer-exists`.
* Added `snapshot-list-header`.
* Added `snapshot-list-entry`.
* Added `invalid-snapshot-id`.
* Added `invalid-snapshot-name`.
* Added `snapshot-name-too-long`.
* Added `snapshot-name-invalid`.
* Added `snapshot-name-already-exists`.
* Added `snapshot-created`.
* Added `confirm-remove-snapshot`.
* Added `confirm-remove-all-snapshots`.
* Added `action-aborted-snapshots-changed`.
* Added `snapshot-removed`.
* Added `snapshot-removed-all`.
* Added `snapshot-restore-failed`.
* Added `snapshot-restored`.
* Added `command-description-snapshot-list`.
* Added `command-description-snapshot-create`.
* Added `command-description-snapshot-remove`.
* Added `command-description-snapshot-restore`.

## v2.13.3 (2021-07-08)
### Supported MC versions: 1.17.1, 1.17, 1.16.5, 1.15.2, 1.14.4

* Fixed: The 'style' property of horse shopkeepers was not getting saved anymore since v2.13.0 and caused migration warnings whenever the plugin attempted to load or save it. If you already upgraded, and don't have a backup of your save.yml file available, or don't want to revert to the backup's state, you may have to freshly edit the 'style' of your horse shopkeepers again.
* Fixed: There is an open Spigot 1.17 issue that sometimes causes the plugin to assume that it failed to spawn shopkeepers during chunk loads. In v2.13.1, we already turned a warning message related to this into a debug message in order to not spam the server log with warnings. But another message was missed and has now been turned into a debug message as well.

## v2.13.2 (2021-07-08)
### Supported MC versions: 1.17.1, 1.17, 1.16.5, 1.15.2, 1.14.4

* Updated for MC 1.17.1
* Building: Bumped the CraftBukkit dependencies for the 1_16_R2 and 1_16_R3 versions of Shopkeepers from 1.16.2-R0.1 and 1.16.4-R0.1 to 1.16.3-R0.1 and 1.16.5-R0.1 respectively. This has no effect on the server versions the plugin works on, but simplifies testing.
* Internal: We now use the server's mappings version to check if the plugin is compatible.
  * Previously, we used CraftBukkit's 'Minecraft Version' to determine compatibility (e.g. '1_16_R3' for 1.16.5). However, Spigot will occasionally update its mappings without bumping the CraftBukkit version or this 'Minecraft Version'. With the new remapping of NMS code since 1.17, these mappings changes will forcefully break our compiled NMS code and then require an update (or at least a rebuild) of the Shopkeepers plugin.
  * Previously, these mappings changes would result in errors during plugin startup, instead of being detected as an incompatible server version. We now keep track of all supported mappings versions and use the server's mappings version to determine whether we can support it and which NMS module we need to load.
  * For some of the 1.16 versions of the plugin, multiple mappings versions are mapped to the same NMS module. However, this should not be a problem, because these modules do not make use of the new remapping build feature, and are therefore less likely to actually break on mappings changes (there are no known server incompatibility issues for these versions).
  * Another effect of the mappings versions being changed without there necessarily being a CraftBukkit version bump: Since our NMS modules build against specific CraftBukkit versions instead of mappings versions, we can only support the latest mappings version for every CraftBukkit version.
  * Building: Added test cases to ensure that our NMS modules build against CraftBukkit versions with the expected mappings versions.
  * Also, our NMS module versions no longer have to match CraftBukkit's 'Minecraft Version'. In order to be able to support different mappings versions even if Spigot does not bump its 'Minecraft Version' (as it was the case in the 1.17 to 1.17.1 update), we will now derive a new NMS module version by incrementing the revision number ourselves.
* Fixed: The compatibility mode when running on an unsupported server version was no longer working.
* If the compatibility mode fails to enable, we now log an error that should provide additional information about the issue.
* Fixed: When the compatibility mode failed to enable, we would encounter an error during plugin disable due to the TradeLoggers component not having been enabled yet.
* Bumped Citizens dependency to v2.0.28.
* Config: Added setting `save-citizen-npcs-instantly` (disabled by default), which triggers a save of all Citizens NPC data whenever the Shopkeepers plugin modifies a Citizens NPC. Saving the Citizens NPCs is quite a heavy operation. Since the Citizens API does not yet provide an API to trigger an asynchronous save, we trigger the asynchronous save by invoking the `/citizens save -a` command in the console. As a side effect, this will print command feedback in the console whenever the NPCs are saved this way.
* Fixed: Since v2.13.0, when not using the `always-show-nameplates` setting (i.e. the default case), the nameplates of Citizens player NPC shopkeepers were limited to a length of 16 and would otherwise not be displayed. The issue is that the Citizens 'hover' nameplate option does not work well for player NPCs. If you are upgrading and have issues with nameplates of previously created NPCs not showing, either use the shopkeeper editor to rename the NPC, or use the `/npc name` command to toggle the nameplate visibility.
* We no longer open the trading interface if the shopkeeper has no offers. If the player can edit the shopkeeper, we print instructions on how to open the editor. This may also resolve an incompatibility with Bedrock players when using GeyserMC.
* Fixed: Writing files (such as when saving the data of shopkeepers) failed on some servers due to the SecurityManager implementation blindly denying the execute permission without differentiating between files and directories. We check the execute (i.e. access) permission for directories in which we want to write files in order to provide more meaningful error messages in cases in which missing directory permissions prevent us from writing files. However, this intervention of the SecurityManager should have no effect on whether we can actually write to a directory, so we can safely ignore it.
* API: Added Shopkeeper#hasTradingRecipes(Player).
* API: The type parameters of TypeRegistry#registerAll and #getRegisteredTypes were changed to be less specific.
* Debug: When we print ItemStacks in YAML representation to the server console, we omit the trailing newline now. However, the `/shopkeeper yaml` command still sends additional empty lines in chat to make the output more readable.
* Debug: Added debug option `empty-trades` that logs item information for the selected trading recipe and the input items whenever a player clicks an empty trading result slot.
* Internal: We avoid copying item stacks in a few more cases.
* Internal: Renamed MerchantUtils#getSelectedTradingRecipe() to #getActiveTradingRecipe() and adjusted a few related debug messages.
* Internal: Various refactors related to the default UIs, time unit conversions, and utilities in general.

Added messages:  
* `cannot-trade-no-offers`
* `no-offers-open-editor-description`

## v2.13.1 (2021-06-26)
### Supported MC versions: 1.17, 1.16.5, 1.15.2, 1.14.4

* Fixed: Added a temporary workaround for an open server issue that caused shopkeeper entities to duplicate on MC 1.17. The warning that was printed about the spawning of shopkeeper entities apparently failing has been turned into a debug message for now. Shopkeeper entities may sometimes take a few second to spawn currently.
* Config: We automatically enable the `disable-inventory-verification` setting now when we detect that we run on a Mohist server.

## v2.13.0 (2021-06-20)
### Supported MC versions: 1.17, 1.16.5, 1.15.2, 1.14.4

**Update for MC 1.17:**  
* Added axolotls, glow squids, and goats to the by default enabled mob types. If you are upgrading, you will have to manually enable these new mob types.
* Added new editor features that are only available when using MC 1.17:
  * Changing the axolotl variant.
  * Toggling between a glowing and a dark glow squid.
  * Toggling between a normal and a screaming goat. This editor option is only available if `silence-living-shop-entities` is disabled.
  * Toggle glowing text for sign shops.
* Known issues related to MC 1.17:
  * Squids and glow squids can be pushed around. This issue might already have existed for some time. See https://bugs.mojang.com/browse/MC-89883
  * The loading of entities is deferred from chunk loading in Minecraft now. I am not yet sure if this severely impacts the Shopkeepers plugin, but it certainly partially breaks the `lock-villager-spawns` and `block-wandering-trader-spawns` settings currently. It is not yet clear how Spigot will update to account for this in order to retain backwards compatibility for plugins.

**Migration notes:**  
* The folder structure has changed:
  * The save file is stored inside a new `data` folder now. If no save file exists at the new location and a previous save file is found at the old location, it is automatically moved.
  * Language files are located inside a new `lang` folder now. Existing custom language files need to be manually moved!
  * Trading logs (if enabled) are stored inside the folder `trade-logs` now. Existing logs are not automatically moved to the new location!
* Removed the 1.16 `PIG_ZOMBIE` migration. We no longer automatically remove this mob type from the config, but only log a warning and then ignore it.
* Removed the migration from Citizens shopkeeper NPC ids to NPC unique ids (originally added in v2.4.0).
* The setting `enable-spawn-verifier` is no longer used and is automatically removed from existing configs during the update.
* The data format of the CSV trade logs has changed. If you automatically process these CSV trade logs, you may have to update your programs to account for the new format.
* The permissions for the shopkeeper remove (all) command have been renamed. You may have to update your permissions setup.
  * `shopkeeper.remove.own` -> `shopkeeper.remove-all.own`
  * `shopkeeper.remove.others` -> `shopkeeper.remove-all.others`
  * `shopkeeper.remove.all` -> `shopkeeper.remove-all.player`
  * `shopkeeper.remove.admin` -> `shopkeeper.remove-all.admin`

**Language file changes:**  
* The default messages are no longer stored inside the config. Instead, we always generate an `en-default` language file. This file acts as a template for custom language files. It is not meant to be modified and is replaced with an up-to-date version on every plugin startup. Message keys no longer start with the `msg` prefix.
* Config: Changed the default language from `en` to `en-default`. Existing configurations are automatically migrated.
* Some texts that were previously settings in the config are now regular messages.
* Added warnings when the language file is missing messages or contains unexpected messages.
* Fixed: Color codes for default messages were not getting replaced. This has an effect when some messages of the specified language file cannot be loaded.
* When splitting messages into multiple lines, we take all Unicode line break characters into account now.

**New feature: Placeholder items for player shops.**  
* When players set up the trades of their shopkeepers, they can now use renamed nametag items as substitutes for items that they don't have. The nametag's display name has to match the name of the substituted item type. Display names that don't match any english item type name won't work. Other properties of the substituted item cannot be specified.
* The parsing is lenient to some extent: The Minecraft namespace prefix (`minecraft:`) is optional (some item type names are even too long to be used in anvils if the prefix is included). The upper and lower case of characters is not important. Leading and trailing whitespace is ignored. Spaces and dashes are converted to underscores. Any color codes are also ignored.
* Placeholder items are immediately converted to their substituted item inside the shopkeeper editor. If what appears to be a placeholder item is not converted, its display name probably does not match a known item type. It is not possible to set up player shopkeeper trades that buy or sell the placeholder items themselves. But normal nametags, and nametags whose names do not match any valid item type, are treated like normal items and can be traded as usual.
* Placeholder items are supported by the selling, buying, and trading player shopkeeper. The admin shopkeeper treats them like normal items.
* Placeholder items can not only be used for items that are bought, but also for items that are sold. This allows players to set up trades before they have the items required to fulfill these trades.
* It is also possible to specify basic enchanted books via placeholder items:
  * The used naming format is `<enchantment> <level>`.
  * The parsing is similar to that of item type names: The normalized display name has to match the id of the intended enchantment.
  * Aliases have been defined for some of the enchantments, so that for example `curse of binding` is correctly mapped to the enchantment with id `binding_curse`.
  * Only a single enchantment can be specified.
  * The level can be freely specified, even outside the range of enchantment levels that can usually be obtained in vanilla Minecraft. Levels outside the range of shorts are truncated to the nearest valid short number. If no level is specified, or if the level cannot be parsed, the enchantment's minimum level is used (usually `1`).
  * Up to level ten, the level can also be specified via roman numerals (`I` to `X`). It is also possible to specify the levels `min` or `max`, which results in the normal minimum or maximum level of the specified enchantment to be used.
* It is also possible to specify basic potions, splash potions, lingering potions, and tipped arrows via placeholder items:
  * The used naming formats are:
    * Potion: `[long] [strong] [potion] [of] [long] [strong] <potion type> [2|ii] [potion] [2|ii]`
    * Splash potion: `[long] [strong] <splash> [potion] [of] [long] [strong] <potion type> [2|ii] <splash> [potion] [2|ii]`
    * Lingering potion: `[long] [strong] <lingering> [potion] [of] [long] [strong] <potion type> [2|ii] <lingering> [potion] [2|ii]`
    * Tipped arrow: `[long] [strong] [tipped] [potion] <arrow> [of] [long] [strong] <potion type> [2|ii] [tipped] [potion] <arrow> [2|ii]`
  * The parsing of the potion type is similar to that of enchantments: The normalized `potion type` has to match the id of the intended potion type. These potion types cover the potions that are found inside the creative inventory. Custom potion items with arbitrary effects and properties are not supported.
  * The keywords `splash`, `lingering`, and `arrow` are used to identify the different item types. If none of these keywords is found, a normal potion item is assumed.
  * The keywords `long`, `strong`, `2`, and `ii` specify long or strong variants of the specified type of potion. There are currently no potion variants that are both long and strong at the same time. Consequently, only one of these keywords is allowed to be used at the same time. However, we currently ignore any additional occurrences of the respectively other keywords. If the specified potion type does not support the selected variant, the keyword is currently ignored as well. But this might change and potentially become stricter in a future release.
  * Each keyword can occur at most once, but there may be multiple valid locations at which it can occur (which is why the above formats mention some keywords multiple times). However, for simplicity, the parsing does not actually take the order or dependencies of words into account currently, but only checks for the presence of the various keywords. But this might change and potentially become stricter in a future release. Any other words in the above formats that were not mentioned as keywords are optional.

**New feature: Trade notifications.**  
* Config: Added settings `notify-players-about-trades` (default: disabled) and `notify-shop-owners-about-trades` (default: enabled), which enable trade notifications for all players with certain permissions, or for shop owners about trades that take place in their own shops.
* Permissions: Added permissions `shopkeeper.trade-notifications.admin` and `shopkeeper.trade-notifications.player` (both default to `false`). If trade notifications are enabled (setting `notify-players-about-trades`), players with these permissions will receive trade notifications for all admin or all player shops respectively.
* There are different sets of trade notification messages for different trade variations and contexts. The default messages do not make use of all of these variations, but this should provide a lot of flexibility when adjusting the messages.
* In order to avoid notification spam, the notifications for equal trades that take place in quick succession are merged into a single notification. However, this does not completely prevent players from potentially causing spam by trading. If this is a concern to you, you may have to disable the trade notifications for now, until more sophisticated countermeasures are available to guard against this kind of spam.
* A new editor option allows shop owners to disable trade notifications for individual shopkeepers. This only affects the trade notifications that are sent to the shop owner, not the general trade notifications that are sent to players with the respective permission.
* Added command `/shopkeeper notify trades`, which allows players to disable trade notifications. This command requires the new permission `shopkeeper.notify.trades` (default: `true`).
  * Since we do not keep track of any user data yet, the trade notifications are only disabled for the current game session (until the player reconnects).
  * The first trade notification received during the current game session notifies the player that they can disable trade notifications via this command. This hint message is clickable and will automatically insert the command to execute into the player's chat box. This hint is only sent to players that have the permission to use the command.
* Config: Added settings `trade-notification-sound` (disabled by default) and `shop-owner-trade-notification-sound`. These sound effects are played whenever a player (or shop owner) receives a trade notification. They can be disabled by setting them to an empty String inside the config.

**Improvements related to the saving and loading of shopkeepers:**  
* Fixed: Improved the error checking for both the serialization and the writing of shopkeeper data. This should also resolve an issue with the save data being lost when the disk is full.
* We provide more detailed error and debug messages now when a save fails. For example, we provide better error messages if failures are caused by missing directory access permissions.
* Replaced the old Java IO based implementation of saving and loading shopkeeper data with a new NIO based implementation. This should also contribute to more accurate error messages in case of failures.
* Fixed/Debug: The logged number of shopkeepers that have been deleted since the last successful save might not have matched the actual number of deleted shopkeepers. This number did not take into account any shopkeepers that could not be deleted during previously failed save attempts.
* Fixed: We explicitly wait for the new shopkeeper data to be written to disk now before we replace the old save data. This should provide additional protection against data loss and corruption in cases of severe system failures, such as crashes, power losses, etc.
* We log a warning now if we are not able to atomically replace the old and new save data. There are several non-atomic fallback solutions for the case that the save file cannot be atomically renamed. However, these may result in data loss or corruption in case of severe system failures (hence the warning).
* During plugin shutdown, we explicitly verify now that there really is no unsaved shopkeeper data, and that there are no saves pending or still in progress. Otherwise, we log a warning. This warning may for example be encountered if the final save during plugin shutdown fails.
* Fixed: When a save fails, we also trigger a delayed save now even if there are no dirty shopkeepers. This is for example required if there has been an explicit save request, or if shopkeepers have been deleted. These changes would not be reliably persisted before.
* When a save fails, we no longer serialize the data of the affected shopkeepers again during the subsequent save attempts if this data is still up-to-date. However, for debugging purposes, the storage still keeps track of the shopkeepers whose data changes could not be persisted to disk yet.
* The name of the temporary save file was slightly changed (`save.temp` -> `save.yml.tmp`).
* We log a warning now whenever a shopkeeper is loaded or created that uses a shop type or shop object type that is disabled. We still load the shopkeeper, so that in the case of a player shop the container is still protected. But we no longer attempt to spawn the shopkeeper. Even though these shopkeepers might in some cases still seem to work as normal, there is no guarantee for this. Admins are advised to either delete these shopkeepers, or change their shop (object) types to something else.

**Changes to the CSV trade logging:**  
* The CSV data format has changed. If you automatically process the CSV trade logs, you may have to update your programs to account for the new format.
* Config: Renamed `enable-purchase-logging` to `log-trades-to-csv`. The previous config is automatically migrated.
* Config: Added setting `log-item-metadata` (disabled by default). This enables the logging of item metadata.
* In order to represent the logged trades more compactly, we merge equivalent trades that are triggered in quick succession over a certain period of time. By default, we merge successive equal trades for up to 15 seconds, but only if there is no gap larger than 5 seconds between the individual trades. The new settings `trade-log-merge-duration-ticks` and `trade-log-next-merge-timeout-ticks` control these parameters. They also allow the trade merging to be disabled.
* Performance: The logging is performed asynchronously now, and in batches. When a trade takes place, we wait 30 seconds before we log the trade and any other trades that may have taken place during this duration.
* Debug: Improved the error handling and debug output related to the CSV trade logging.

**Several performance improvements:**  
* Various performance improvements related to sign shops and the processing of physics events. Also slightly improved the performance of checking for protected sign shops when blocks are broken or explode.
* For load balancing purposes, the activities of shopkeepers are distributed over multiple Minecraft ticks now. However, processing the shopkeepers every tick, even if only a portion of them needs to do any actual work, also comes with a certain overhead. We therefore do not distribute their activities over all Minecraft ticks, but instead process them in groups only every 5 ticks.
* Performance improvements related to the shopkeeper mob AI:
  * Config: Added the setting `mob-behavior-tick-period` with a default value of `3`.
    * This controls the rate at which we update the gravity and AI of shopkeeper mobs. Previously, these behaviors were updated every tick, as it is also the case in vanilla Minecraft. Values above 1 indicate a reduced tick rate, which result in a less smooth, less reactive, and possibly slower behavior in comparison to the behavior of mobs in vanilla Minecraft.
    * In order to compensate for a reduced tick rate, some activities are scaled accordingly. This ensures, for example, that mobs still rotate their head at the same speed towards nearby players, or that mobs still fall at the same speed when being affected by gravity. Consequently, a reduced tick rate is less performance-intensive in total, but may be slightly more performance-intensive per individual behavior update.
    * In my testing, the value 3 offered a large overall performance benefit with a relatively small additional performance impact per individual behavior update, while still providing an acceptable smooth mob behavior. Values above 3 are clearly noticeable and offer little additional benefit.
  * Reduced the rate at which the shopkeeper mob AI activations are updated for online players from once every 20 ticks to once every 30 ticks. This seems to be sufficient to still fluently activate nearby shopkeeper mobs even for players that fly around in creative mode. We also immediately activate the shopkeeper mob AI in nearby chunks now whenever a player joins the server or teleports.
  * Shopkeeper mobs no longer tick Minecraft's float behavior. This has no effect since they use the NoAI flag.
  * The shopkeeper mob AI task is no longer dynamically started and stopped, but instead keeps running even if there are no entities to process currently. Frequently stopping and restarting the task is itself associated with a certain overhead.
  * Various other small optimizations to how the shopkeeper AI task:
    * The shopkeeper mob AI task no longer checks if the entity is still valid, but only if it is still alive. This avoids having to check if the entity's chunk is still loaded, which is relatively costly compared to other operations. This check shouldn't be required since we expect the shopkeeper mobs to be stationary, and we already stop their AI on chunk unloads.
    * We also no longer remove dead entities from our mob AI processing right away, but simply ignore them. They are cleaned up once the shop objects recognize that the entities are no longer alive.
    * Shopkeeper mobs with AI are now stored by chunk. Instead of iterating over all mobs and checking if their AI or gravity is active, we now iterate over the chunks first and skip those with neither active AI nor gravity. Additionally, we skip the processing of mobs altogether if we know that there are currently no chunks with neither active AI nor gravity.
    * The shopkeeper mob AI no longer stores and queries chunk data by the chunks themselves, but via their coordinates. This avoids having to retrieve and check if the chunks around players are currently loaded during AI activation updates.
  * On some Paper versions, maybe due to their async chunk loading, the player's current chunk may sometimes not be loaded yet. We now avoid accessing (and thereby loading) the chunk when activating the AI and gravity behavior of nearby shopkeeper entities.
* Increased the chunk activation delay on chunk loads from 2 to 20 ticks. This further limits the rate at which shopkeepers may get spawned and despawned, for instance, when players frequently cross chunk boundaries back and forth. When a player joins the server or teleports, we immediately activate the chunks in a 2 chunk radius.
* Added a spawn queue for shopkeepers spawned on chunk loads.
  * Spawning lots of shopkeepers at the same time can be quite costly performance-wise. This queue distributes the spawning of shopkeepers over multiple ticks to avoid performance drops.
  * We spawn at most 6 shopkeepers every 3 ticks (roughly 40 shopkeepers per second): These numbers are a balance between keeping the number of shopkeepers spawned per cycle low, while avoiding the general performance overhead associated with higher spawn rates.
  * In order to avoid players having to wait for shopkeepers to spawn, there are some situations in which we spawn the shopkeepers immediately instead of adding them to the queue. This includes: When a shopkeeper is newly created, when a shopkeeper is loaded (i.e. on plugin reloads), and after world saves. In the latter two cases, a potentially large number of shopkeepers is expected to be spawned at the same time. Due to its limited throughput, the queue would not be able to deal with this sudden peak appropriately. However, since these are situations that are associated with a certain performance impact anyway, we prefer to spawn all the affected shopkeepers immediately, instead of causing confusion among players by having them wait for the shopkeepers to respawn.
* The check for whether the shopkeeper mob moved and needs to be teleported back reuses the previous spawn location now. Previously, this would freshly calculate the spawn location each time, which also involves a ray trace to find the exact location for the mob to stand on.
  * This change also ensures that shopkeeper mobs no longer start to fall if gravity is disabled and the block below them is broken. However, to account for shopkeepers that have previously been placed above passable or non-full blocks, which might have been broken since then, we still spawn shopkeeper mobs up to one block below their location even if gravity is disabled (e.g. when the chunk or the plugin are reloaded).
  * Another side effect of this change is that if gravity is disabled, it is no longer as easily possible to force the shopkeeper mob to move back to its original location after the block it was previously standing on is broken. The shopkeeper will only check for a new spawn location now when it is being respawned. If the mob is supposed to dynamically move when the block below it is broken, gravity needs to be enabled.
  * This reuse of the spawn location is limited to the movement check itself. If this detects that the shopkeeper actually moved and needs to be teleported back, a new spawn location is calculated as before. So if the shopkeeper previously spawned slightly below its actual spawn location (due to there missing some block), and gravity is enabled, players are still able to reset the shopkeeper's location by letting it fall due to gravity and then placing a block below its actual spawn location for the shopkeeper to stand on.
* There have been various internal changes that allow us to avoid copying and comparing item stacks in many situations.
  * For example, when we require read-only access to the data of a Minecraft item stack, as it is the case when we compare the provided and required items of a trade, we try to avoid copying the item stack and instead access its underlying data directly.
  * Also, with the change of using unmodifiable item stacks, internally and throughout the API, we can now avoid copying item stacks in many situations.
* Improvements related to checking and deleting shopkeepers of inactive players.
* Various other minor internal improvements.

**Command changes:**  
* The "/shopkeeper remove" command has been renamed to "/shopkeeper removeAll". All related permission nodes and some related messages have changed. The `all` argument, which removes the player shops of all players, has been renamed to `player`.
* The "removeAll" command prints the number of shopkeepers now that have been skipped because they have either already been removed while the command was waiting for confirmation, or because their removal has been cancelled by a plugin.
* Added command "/shopkeeper remove [shop]". This replaces the previous "remove" command and allows the removal of a single specific shopkeeper.
  * Added permission `shopkeeper.remove.own` (default: `op`): Allows the removal of own shops via command.
  * Added permission `shopkeeper.remove.others` (default: `op`): Allows the removal of shops of other players via command.
  * Added permission `shopkeeper.remove.admin` (default: `op`): Allows the removal of admin shops via command.
* Improved the feedback messages when a player tries to create a shopkeeper via command.
  * If the player targets a container, we assume that they are trying to create a player shop, regardless of whether the creation of player shops via command is enabled. However, unlike before this only affects the default shop type that is chosen if the player does not explicitly specify a shop type himself.
  * It is now possible to create admin shops via command even when targeting a container. However, the admin shop type has to be explicitly specified as command argument.
  * When a player shop type is selected, we send appropriate feedback messages depending on whether player shop creation via command is enabled, whether a container is targeted, and whether it is a supported type of container.
  * When not specifying a shop object type, we pick the first shop object type that can be used by the player. This is consistent for the creation of player and admin shops now.
* There have been a few minor fixes related to command arguments that should result in more detailed error messages when they cannot be parsed.
  * Fixed: When the parsing of a command argument failed, the error message was using the argument name instead of the argument format. However, this was mostly an issue for the few commands with required arguments whose format does not contain the argument name, for example because their format is constructed from their child arguments.

**Changes related to Citizens shopkeepers:**  
* Fixed: Previously, if the location of a Citizens shopkeeper changed due to the NPC moving around, it was not guaranteed whether that location change would be persisted. Now, we always trigger a save if at least one shopkeeper is marked as dirty during ticking.
* Similar to non-Citizens shopkeepers, we also periodically respawn Citizens shopkeepers now if they have previously been spawned (i.e. if they have an associated entity) but their entity has been removed, for example by another plugin. If the NPC has intentionally been despawned, it is not automatically respawned.
* Changes to Citizens shopkeeper NPC names:
  * NPCs are initially created with an empty name now. Only if the NPC is of type player, we subsequently assign it a name, because this determines its initial skin. If the shopkeeper is a player shop, we use the shop owner's name as before. Otherwise, we try to use the name of the player who created the shopkeeper. If this is not available, we fall back to an empty name.
  * We no longer add the nameplate prefix if the NPC is of type player, because this messes with the NPC's skin.
  * When we set the NPC's name, we now also set the nameplate visibility: If the name is empty, the nameplate is hidden. Otherwise, the `always-show-nameplates` setting determines whether the nameplate is always shown, or whether it is only shown when a player directly looks at the entity (this only works for non-player NPCs though).
* Config: Added setting `default-citizen-npc-type` (default: `PLAYER`) which controls the entity type used by newly created Citizens shopkeeper NPCs.
  * Admin shopkeepers also use player NPCs by default now. Using villager NPCs for them has caused some confusion in the past.
  * This also resolves an issue for offline mode servers: On recent versions of Citizens, the creation of player NPCs fails if they share the same name as regular players on the server, due to some conflict with their UUIDs being the same.
* We no longer automatically delete invalid Citizens shopkeepers by default.
  * This has caused some issue in the past due to Citizens shopkeepers being incorrectly classified as invalid in some situations. By default, we only log warnings now about any detected invalid Citizens shopkeepers, and instructions on how these shopkeepers can be deleted in order to get rid of these warnings.
  * Config: Added setting `delete-invalid-citizen-shopkeepers` (default: `false`). This setting can be used to enable the previous behavior of automatically deleting invalid Citizens shopkeepers again.
  * Alternatively, the new command "/shopkeepers cleanupCitizenShopkeepers" can be used to manually check for and delete invalid Citizens shopkeepers. This command requires the new permission `shopkeeper.cleanup-citizen-shopkeepers` (default: `op`).
  * If a NPC is deleted and there are multiple shopkeepers associated with it, all of these shopkeepers are deleted now.
* There have been several other internal refactors related to Citizens shopkeepers.

**Debugging improvements:**  
* Added command "/shopkeeper testDamage [damage] [times-per-tick] [duration-ticks]", which can be used to debug the performance of handling lots of damage events.
* Added debug command "/shopkeeper testSpawn [repetitions]", which measures the time it takes to respawn the active shopkeepers within the current chunk.
* Minor improvements and additions to the "/shopkeeper checkitem" command: The command checks now whether the main and off hand items match the shop creation item, and whether they match each other according to Minecraft's own stricter item matching rules. The output is also more compact now.
* Various small fixes and additions related to the "/shopkeeper check" command.
  * For example, this also shows chunk activation timings now, statistics on the current and maximum number of pending shopkeeper spawns, and shopkeeper mob behavior tick rate.
  * We also no longer reset the shopkeeper mob AI and gravity statistics when there are no more active entities.
* The command "/shopkeeper debug [option]" can toggle debug options now.
* It should be easier now to identify tasks inside timing reports, because they now use named classes instead of lambdas.
* Added debug option `visualize-shopkeeper-ticks`, which enables an in-game visualization of shopkeeper and shop object ticking activities by using particles.
* Added debug option `regular-tick-activities`. The debug output for a few non-exceptional ticking activities, which could sometimes be considered spam if one is not specifically interested in them, such as shopkeepers teleporting back into place, or mobile Citizens shopkeepers updating their shopkeeper's location, are disabled by default now. This new debug option enables them again.
* Added debug information about the number of shopkeepers that actually had to be spawned or despawned during the spawning or despawning of a chunk's shopkeepers.
* When the result item of a trade does not match the expected result item, or when a strict item comparison fails, we additionally log the serialized Yaml representation of the involved items now.
* The output of the "/shopkeeper yaml" command is logged as a single multi-line message now. This should make it easier to copy it from the server log and paste it into the config (depends on the logging configuration though). Also, the output includes the ItemStack's serialized type key now, and the used keys are more context specific.
* When we debug failed command executions, we log the stack trace of the internal command exception now.
* When the shopkeeper editor is closed, we log the number of shopkeeper offers that have changed.

**Other changes:**  
* Players in creative mode are no longer ignored when they use the shop creation item.
* Fixed: The shop creation item can no longer be renamed in anvils if the `prevent-shop-creation-item-regular-usage` setting is enabled.
* Changes to the maximum shops limit:
  * Config: A value of `0` for the `max-shops-per-player` setting no longer indicates no limit, but can be used to disable the creation and hiring of player shops. 'No limit' is indicated by a value of `-1` now. Any previous limit of `0` is automatically migrated.
  * Permission: Added permission `shopkeeper.maxshops.unlimited` (default: `op`), which disables the maximum shops limit for a player.
  * The permissions specified inside the config get cached and checked in decreasing order now. We abort checking permissions if they cannot further increase the player's current shops limit. An effect of this is that it is only possible to increase the default limit, not decrease it.
* When the plugin runs on an unsupported server version, we no longer add a movement speed attribute modifier to mobs in order to make them stationary. This should no longer be required, because on all recent and supported server versions we use the NoAI flag to make the mobs stationary.
* There have been some changes to how shopkeepers and shop objects are ticked.
  * Similar to shopkeepers, shop objects are also ticked once every second now. Also, we not only tick the shopkeepers with active shop objects, but all shopkeepers in active chunks. This ensures that we periodically check if we can respawn missing shop objects.
  * This replaces the task that previously periodically checked whether the shop object is still active or needs to be respawned. These checks still happen only once every 10 seconds for each shopkeeper, and are distributes over multiple Minecraft ticks for different shopkeepers.
  * The setting `enable-spawn-verifier` has been removed: The plugin always periodically checks now if the mobs are still there or if they need to be respawned, regardless of this setting.
  * If a shop object cannot be respawned a few times in a row, the rate at which it reattempts the spawning is throttled now. Previously, it would permanently abort all respawn attempts in this case. When the shop object was able to successfully spawn again, its tick rate and respawn counter are reset.
  * We no longer rely on random numbers when we offset the ticking activities of shopkeepers and shop objects, but instead use simple cyclic counters. This should produce a more even and deterministic load distribution.
  * When a shopkeeper is marked as dirty during ticking, we trigger a delayed save instead of an immediate save now.
* Fixed: Previously, after the Shopkeepers plugin has been dynamically reloaded, GriefPrevention prevented players from using the shop creation item or interact with shopkeepers in protected regions. This issue has been resolved by dynamically reordering the registered event handlers so that our event handlers are always executed first.
* We only print the shop creation item usage message if the player is still holding the item after a short delay now. This avoids message spam when a player quickly scrolls through the items on the hotbar via the mouse wheel.
* Fixed: We sometimes receive an additional left-click air interaction event when a player right-clicks air while looking at a block slightly outside the interaction range. This led to the shop creation item switching between two selections back and forth for a single right-click. We ignore this additional left-click air interaction now by ignoring any subsequent interactions that occur within a brief time span of the last handled interaction. The ignored interactions are still cancelled to prevent any vanilla behaviors or side effects of other event handlers, but they no longer change the player's selection.
* Fixed: Closing the shopkeeper editor did not immediately trigger a save, but relied on the periodic shopkeeper tick task to trigger a save. Also, we check now which trades have actually changed and only trigger a save if the shopkeeper was actually modified.
* Added an editor option to rename regular villagers. Unlike the renaming via nametags, this allows the use of color codes.
* Added an editor option to toggle the pumpkin head of snowman shopkeepers.
* Added an editor option to change the color of shulker shopkeepers.
* Added an editor option to toggle the invulnerability of regular non-shopkeeper villagers.
* Added sounds to the various buttons inside the editor menu.
* Players have to confirm now when they delete a shopkeeper via the editor. This also applies to the deletion of the villager via the editor for regular villagers.
  * The confirmation is done by pressing a button in an inventory menu that opens. Canceling the action returns the player back to the editor. When the confirmation inventory is closed by some other mean, an abort message is printed.
  * If the shopkeeper has already been removed by other means in the meantime, we send the `shop-already-removed` message.
* We also send the `shop-removed` message now when a shopkeeper is deleted via the editor menu. Similarly, deleting a regular villager via the villager editor will also print a message now.
* When using the villager editor, we check more frequently now whether the villager still exist, and close the editor if it does not.
* Fixed: Striders, magma cubes, and blazes are now able to stand on top of lava. This only applies if they are not completely submerged by lava (in which case they don't float to the top as they do in vanilla Minecraft, but sink to the ground as before).
* When creating a shopkeeper via command, it is now possible to place it on top of liquids by targeting a water or lava block. If the player is underwater (or inside lava), the shopkeeper is placed at the targeted block as before. Since we spawn shopkeepers up to one block below their location, this still allows placing shopkeepers on the ground in shallow liquids, without them being continuously teleported back if they are not able to stand on top of the liquid.
* Buying shopkeepers can also buy written books and enchanted items now. Being able to buy written books is probably not really useful though, because any book item being bought has to nearly perfectly match the requested book item.
* Config: Added setting `disable-inventory-verification` (default: `false`). Even though modded servers (Cauldron, Mohist, etc.) are not officially supported, this setting may help to resolve a particular known incompatibility with these types of servers.
* When the deletion of shopkeepers of inactive players is enabled, we not only check for inactive players during plugin startup, but also periodically now (roughly every 4 hours). This accounts for servers that keep running for long durations. We also log a message now whenever we check for shopkeepers of inactive players.
* Fixed: Editing and removing trades from a shopkeeper while another player is trading with it could result in an exception due to an issue with the insertion of empty dummy trades. These empty dummy trades are required because the Minecraft client cannot deal with the list of trades dynamically shrinking in size. However, since the insertion of empty dummy trades is rather confusing for players, we also try now to heuristically guess the trades that were removed and then instead insert blocked dummy trades that correspond to these trades.
* Fixed: Forge clients seem to send additional off-hand interactions when interacting with villagers. This breaks our villager editor, because it immediately closes the villager editor again and instead opens the regular villager trading interface. In an attempt to resolve this incompatibility, we now cancel all off-hand interactions with regular villagers if the player already has some inventory open.
* Fixed: Shopkeeper entities are now marked as invulnerable, so that other entities ignore them in various additional situations. For example, villagers are no longer panicked by nearby hostile mob shopkeepers.
* Added more player feedback messages for cases in which the trading may fail for some reason.
* Fixed: When handling a shopkeeper hire attempt, we first check now if the shopkeeper is still for hire.
* Fixed: Piglin brute mobs do not support a baby variant. Their shopkeeper type will therefore also no longer show the baby option in the editor.
* Fixed: Book shops logged warnings when they were loaded. The importer for legacy data attempted to read non-legacy data and then failed to read the book prices.
* Fixed: The shopkeeper metadata is also removed from an entity again now if the spawning failed.
* Metrics: The shopkeepers count chart groups its results into slightly more detailed categories now.

**Other config changes:**  
* The settings `edit-regular-villagers` and `edit-regular-wandering-traders` are now disabled by default. This feature seems to cause confusion for people who are not aware of it. Regular villagers can still be edited by default via the "editVillager" command.
* Added piglin brute to the by default enabled mob types.
* Added shulker to the by default enabled mob types. However, note that this mob will currently stay in its closed form due to its disabled AI.
* The default enabled shopkeeper mobs are now alphabetically sorted. This only applies to newly generated default configs.
* Fixed: The living shop object types were not registered in the order specified inside the config.
* The `enabled-living-shops` setting is slightly more lenient now when parsing the specified mob types. Previously, the mob type names had to perfectly match the server's entity type names.
* Fixed: If the material of the configured shop creation item was invalid, the used default shop creation item used a display name with untranslated color codes.
* Fixed: Specifying metadata for items of type AIR (which do not support metadata) no longer results in an exception.
* Updated the description of some settings, and improved the validation and error feedback.
  * Updated the description of the `file-encoding` setting: On all recent versions of Bukkit (since around 2016), we were actually using UTF-8 if this setting is left empty.
  * Added validation for the 'file-encoding' setting: If it is empty or invalid, we print a warning and use UTF-8.
  * Minor additions to the default comment of the 'prevent-item-movement' setting.
  * Legacy item types, and item types that are not actually items, can no longer be specified inside the config. We log a warning when we encounter such an item type.
  * List settings verify now that they don't contain null elements when being loaded from the config.

**API changes:**  
* Breaking: Added UnmodifiableItemStack, which is a read-only wrapper around an item stacks.
  * All methods that previously returned copies of internal item stacks will now return unmodifiable item stacks.
  * If you actually require a Bukkit ItemStack, you can now decide to either create a copy of the item stack via UnmodifiableItemStack#copy(), or get an unmodifiable ItemStack view via UnmodifiableItemStack#asItemStack(). Note, however, that the latter has certain limitations and is quite unsafe to use. See its documentation for more information.
  * Added various method variants that use UnmodifiableItemStack instead of ItemStack as parameter types.
  * Some methods that require an item stack as input will now use the unmodifiability of the passed item stack as an indicator that the passed item stack can be assumed to be immutable and therefore does not need to be copied before it is stored. This behavior is clarified in the documentation of these methods.
* Some methods and interfaces have been renamed.
  * `TradingOffer` is now called `TradeOffer`.
  * Fixed: Renamed AdminShopkeeper#getTradePremission to #getTradePermission.
  * A few methods of ChunkCoords have been renamed for consistency with the rest of the code base.
* Added factory methods to the TradeOffer, PriceOffer, and BookOffer interfaces. These are now preferred over the factory methods found in ShopkeepersAPI and ShopkeepersPlugin. The latter have been deprecated and may be removed in the future.
* Removed the factory methods for trading recipes from the API. Only the Shopkeepers plugin itself, and plugins using the internal API should be required to create trading recipes.
* Shopkeeper#getTradingRecipes, as well as the various shopkeeper type specific methods to get, set, and add offers are less specific now about the element types of the involved lists. However, they still validate that any added offers are of the expected implementation type, i.e. have been created via the provided factory methods.
* PlayerCreatePlayerShopkeeperEvent and PlayerShopkeeperHireEvent: The meaning of the maximum shops limit has changed. A value of 0 or less no longer indicates 'no limit'.
* Fixed: Event handlers for the specific PlayerCreateShopkeeperEvent were not actually getting invoked.
* Fixed: We now call the ShopkeeperEditedEvent when the player closes the editor and at least one trade offer has changed.
* Added: ShopkeeperTradeEvent#hasOfferedItem2(), TradeOffer#hasItem2(), and TradingRecipe#hasItem2() as shortcuts for checking if a trade involves two input items.
* There have been some changes related to object ids. Object ids are no longer exposed in the API.
* There have been some changes around the meaning of 'active shopkeepers' and 'active shop objects', and the behavior of related methods.
  * The behavior of ShopkeeperRegistry#getActiveShopkeepers() has changed and has been clarified: It returns all shopkeepers in active chunks now, even if their shop objects are not actually active currently (i.e. if they could not be spawned). For consistency, the method #getShopkeepersInActiveChunks(String) has been renamed to #getActiveShopkeepers(String).
  * The meaning of ShopObject#isActive() is more consistent now across different types of shop objects: It checks if the shop object is currently present in the world. Consequently, virtual shops always return false instead of true now. And the Citizens shop object's #getEntity() and #isActive() methods check if the entity exists and is still alive (instead of checking for the NPC's existence).
  * Several ShopObject methods were slightly optimized to no longer check if the shop object is currently active, but only if it has been spawned, which is sufficient most of the time. ShopObject#isSpawned has been added as a more lightweight variant of ShopObject#isActive.
* Various aspects and methods related to shop object spawning are no longer exposed in the API.
  * Removed ShopObject#spawn() and #despawn().
  * Removed ShopObject#needsSpawning() and the internal AbstractShopObject#despawnDuringWorldSaves(). These have been replaced with corresponding internal methods in AbstractShopObjectType.
* We skip spawning and activating shopkeepers now if they are immediately removed again during the ShopkeeperAddEvent.
* Removed ShopkeeperRegistry#loadShopkeeper from the API. This isn't properly supported by the implementation currently: The implementation expects all shopkeepers to be loaded from the built-in storage currently in order for certain operations (such as checking if a certain shopkeeper id is already in use) to work as expected.
* Removed Shopkeeper#isDirty() from the API. Internally, this flag has a different meaning now and should only be of use to the shopkeeper storage.
* Added ShopkeeperStorage#saveIfDirty and #saveIfDirtyAndAwaitCompletion.
* Added methods to BlockShopObjectType and EntityShopObjectType to query and check for shopkeepers of that specific type. This is less performance intensive compared to checking all shop object types when querying the ShopkeeperRegistry. Internally, sign shops make use of these new querying methods.
* Added: PlayerShopkeeper#isNotifyOnTrades and PlayerShopkeeper#setNotifyOnTrades.
* The blocks of sign shopkeepers are also marked with the shopkeeper metadata now.
* Sign shop objects no longer return a name. We previously returned the sign's second line. However, this isn't really useful anymore because the exact sign contents are configurable now.
* CitizensShopObject#getName returns the NPC name instead of its 'full name' now (i.e. the name that more closely corresponds to the name that has been set via #setName).
* Optimization: Various lazily populated views provided by the shopkeeper registry return an empty iterator now when they are known to be empty.
* Several Javadoc improvements and clarifications.

**Internal API:**  
* Added methods to retrieve the values of the various mob shopkeeper properties.
* For consistency reasons, a few methods related to the slime size, magma cube size, and parrot variant have been renamed.
* The dirty flag of shopkeepers only indicates whether the storage is aware of the shopkeeper's latest data changes. It is no longer an indicator for whether these changes have actually been persisted to disk yet.
* Several methods related to the shopkeeper's dirty flag are final now.
* Renamed a few methods of the ShopkeeperStorage.
* EditorHandler#addRecipe no longer receives the editing player as argument: None of the other related methods (#getTradingRecipes and #clearRecipes) receive the player either.
* Shop objects are now informed when the owner of their associated player shopkeeper changes.
* AbstractShopObject#spawn() no longer respawns the shop object if it is already spawned but no longer active. Instead, this method simply skips the spawning if it has already been spawned (this no longer checks if the shop object is still active). In order to respawn the shop object, it has to be explicitly despawned first.
* Renamed AbstractShopObject#getEditorButtons to #createEditorButtons to make it more clear that is usually only invoked once. Also, this returns a new modifiable list now. Subtypes therefore no longer need to create their own copies of that list.
* Added AbstractShopObject#onShopkeeperAdded.
* Added AbstractShopObject#getShopkeeper.
* Shopkeepers can now provide a lazily evaluated map of possible message arguments. However, this is not yet used everywhere yet.
* UIHandler#getUIType is final now.
* The conversion between the editor's trading recipe drafts and the merchant's own representation of trade offers has been moved behind the new interface TradingRecipesAdapter in AbstractEditorHandler.
* PlayerShopEditorHandler#createTradingRecipeDraft and #getPrice are no longer instance methods.
* ItemData#withType may return the same ItemData instance now if the type has not changed.
* Several internal references to the trade offer and trading recipe implementation classes have been replaced with references to the corresponding API interfaces. The internal return types of some shopkeeper methods for accessing the offers have changed. AbstractShopkeeper#getTradingRecipes no longer requires returning SKTradingRecipes.
* When loading a shopkeeper from a config section, any stored data elements (except sub-sections) are assumed to be immutable and the shopkeeper is allowed to directly store these elements without copying them first.
* Various workarounds for avoiding copying internal item stacks when it is not required, such as methods that directly return internally stored item stacks, have been removed, since they are no longer required.
* AbstractPlayerShopkeeper#createSellingRecipe and #createBuyingRecipe are final now.
* Small refactors of TradingRecipeDraft: Added TradingRecipeDraft#asItem2(). #toTradingRecipe has been moved into SKTradeOffer. Some methods are final now.

**Internal:**  
* Building:
  * We build against the Mojang-mapped CraftBukkit version now and remap the result to the mapping used by Spigot.
  * Building requires JDK 16 now. The source code is still Java 8 compatible, but that might change in the future.
  * Installing the Spigot dependencies via the `installSpigotDependencies` script requires both JDK 8 (to build older Spigot versions) and JDK 16 (to build versions 1.17 and above). We use Jabba to dynamically install and switch between these required JDK versions.
* Added callbacks to apply version-specific preparations and setup when spawning shopkeeper entities. But these are not yet used for anything.
* Refactor: Moved some general utility methods from 'MC_1_16Utils' to 'CompatUtils'.
* Changes to object ids:
  * Performance: Object ids can be of any type now (not only String). This avoids having to construct new Strings whenever a shop object is queried. There are usually already objects present that can be used as identifiers, without having to construct them first. Living entity shops use the entity UUID as identifier now, Citizens shops use their unique NPC id, and sign shops use their block location.
  * Performance: By using a shared mutable block location object, the performance of checking if a block at a certain coordinate is a sign shop has been further improved (around 70% in my testing!). This is especially relevant when handling block physics events, since these can occur quite frequently.
  * The object id can be a non-null value even if the entity is not active currently (e.g. for Citizens shopkeepers, which use the unique NPC id for this). The description of the #getId() method and of some affected API methods in the ShopkeeperRegistry have been slightly adapted to account for that.
  * Virtual shop objects always return null as id now, since they are never present in the world.
  * The Citizens shopkeeper trait no longer keeps track of the shop object id. There is no need for that as the shopkeeper can be determined directly by the NPC.
* The spawning and despawning of shopkeepers is handled more consistently now. For instance, if shopkeepers are marked as dirty during the spawning of their shop objects we would previously trigger a delayed save. We do the same when they are despawned now.
* Performance: To slightly speed up iterations, we use LinkedHashMap instead of HashMap and Collection#forEach instead of iterators at a few places now.
* Debug: A few debug messages are now printed before their denoted action takes place (e.g. before the shop object is teleported or respawned).
* Fixed: Optional arguments delegate to the wrapped argument for more detailed error messages now.
* Debug/Fixed: Average timings would previously sometimes be incorrect since they took into account unset values of the timing history.
* Debug: The shopkeeper mob AI timers are stopped before the AI task is stopped now. This might, in rare cases, affect the correctness of some of the timing outputs of the 'check' command.
* Metrics: We capture the actually used 'gravity-chunk-range' now.
* Metrics: The value of the new 'mob-behavior-tick-period' setting is captured by the features chart.
* Decreased the distance shopkeeper mobs are allowed to move before they are teleported back from slightly above 0.6 to slightly below 0.5. Since shopkeeper mobs are spawned at the center of their block, this ensures that we teleport them back into place whenever they change their block.
* Added StringProperty.
* The config key pattern is cached now.
* Major refactoring related to how the config and language files are loaded.
* Refactoring related to how shopkeeper data is saved and loaded:
  * Shopkeepers are saved after they are unloaded now. This allows them to still modify their data during despawning or when handling the unload.
  * When a new immediate save is triggered (for example during plugin shutdown), we no longer abort any currently scheduled but not yet started save task. Instead, we finish it and then execute the save task again. This may trigger more saves than necessary in a few cases, but ensures that frequent requests to save the shopkeepers won't repeatedly abort any previous saving attempts.
  * Reloading the shopkeeper data will now wait for any current and pending saves to complete. However, this has not really been an issue before since we only reload the shopkeeper data during plugin startup currently.
  * The storage no longer checks for config defaults when checking if the save data contains data for a specific shopkeeper.
  * Shopkeepers are no longer marked as dirty when they are deleted. The storage keeps track of the deleted shopkeepers separately, so there is no need for that.
  * The code for informing the shopkeeper storage about shopkeepers that have been marked as dirty during their loading or due to being newly created has been moved from the shopkeeper registry into the shopkeeper itself, after the shopkeeper has been marked as valid.
  * The code for informing the shopkeeper storage about used up shopkeeper ids has been moved to SKShopkeeperRegistry#addShopkeeper.
  * We no longer check the shopkeeper registry's loaded shopkeepers when we check if a given shopkeeper id is unused. Instead, the storage checks the available shopkeeper data (this also includes unloaded shopkeepers, and shopkeepers that could not be loaded for some reason), as well as the shopkeepers that are dirty (this also includes newly created shopkeepers that have not yet been persisted) or that are pending deletion from the storage. Ids of deleted shopkeepers can no longer be reused until their deletion has been persisted. If a deleted shopkeeper has never been saved before, its id becomes available for reuse right away. However, this is rarely relevant in practice, because we still prefer to use ascending unused ids when they are available.
* Minor cleanup related to the AI and gravity processing of shopkeeper entities.
* Removed a few redundant checks regarding whether an entity is still alive and its chunk is still loaded. These additional checks have been required in some previous versions of Spigot, but that should no longer apply to late Spigot 1.14.1 and above.
* Added AbstractShopObject#onIdChanged that can be used to update the shopkeeper's entry in the shopkeeper registry when a shop object's id dynamically changes.
* A new RateLimiter class is used for all shopkeeper and shop object ticking activities that do not need to happen with every tick.
* Added AbstractShopkeeper#onChunkActivation() and #onChunkDeactivation().
* Instead of passing the chunk coordinates around whenever a shopkeeper is registered, unregistered, or moved, each shopkeeper remembers the chunk now by which we previously stored it.
* ShopkeeperRegistry#onShopkeeperMove has been renamed to #onShopkeeperMoved.
* When ticking sign shopkeepers, we no longer check twice if the chunk is loaded in the regular case, but only if the sign block could not be retrieved (i.e. only in the exceptional case).
* The FixedValuesArgument is less restrictive with the types of Map values it accepts.
* Timer uses Java's TimeUnit for conversions now.
* Timer and Timings use a long counter now.
* Added a mutable variant of ChunkCoords.
* Minor refactoring related to shop object properties.
* By keeping track of the largest already used shopkeeper id, we no longer have to check all currently loaded shopkeepers when an id turns out to already be in use.
* Sign shops no longer check if their chunk is still loaded when accessing their block or checking if the block is still a sign. Since shopkeepers are despawned on chunk unloads, and sign shops cannot change their location, this is not required.
* Moved the handling of shopkeeper metadata from ShopkeeperUtils into ShopkeeperMetadata.
* When checking if a shopkeeper entity moved, we reuse a single Location object now.
* Added test cases that compare the values of the default config and language file with their internal counterparts.
* We explicitly set the mob type of Citizen shopkeeper NPCs now when we create the NPC, similar to Citizens' own NPC creation command.
* Slightly changed how we retrieve the entity type of Citizen shopkeeper NPCs.
* Several minor optimizations and refactors related to the various types of shopkeepers. Items are copied and compared less often now.
* Citizens shopkeepers set up their NPC after the shopkeeper has been successfully added to the shopkeeper registry now.
* We now keep track of the mapping between Citizens NPCs and their corresponding Citizen shopkeepers independently of the activation states of these shopkeepers.
* Refactors related to the various player shops related functionality, such as the updating of shop owner names, the checking and deleting of shopkeepers of inactive players, and the max shops permissions.
* The dynamically registered max shops permissions have a description now.
* We also register the dynamic mob type specific shop object type permissions now.
* Since the `shopkeeper.entity.*` permission is given to all players by default, we check it first now, prior to checking the mob type specific permission.
* Slightly optimized the lookup of protected containers.
* Added classes to represent snapshot data of trades, players, and shops.
* Added a CsvFormatter utility with various options.
* The CSV trade logger uses new file and date-time APIs now.
* Minor refactor to the utility functions that remove items from inventories, and to how the book shopkeeper removes writable books from the shop container.
* Added a utility class TradeMerger for merging equivalent trades.
* Changes to how message arguments are represented.
* Fixed: The default 'missing argument' error message was not being used. However, this issue probably remained unnoticed until now, because most of the commands either use more specific missing argument error messages, or provide fallbacks when no arguments are provided.
* Minor improvements to normalizing and matching enum names (used by command arguments, config settings, shopkeeper data, etc.).
* Added a SoundEffect class to represent sound effects inside the config.
* Started to use annotations to mark which methods modify their input arguments. These annotations are not yet checked in any form, but are merely used for documentation purposes. More methods will be annotated as the time goes on.
* Added a document that collects some of the assumptions made about the server implementation that the plugin relies on in order to work correctly.
* Testing: Added a minimal mock for the Shopkeepers plugin. This is required, because some tests need to be able to create unmodifiable item stacks, which requires the corresponding factory method to be bound to the Shopkeepers API.
* Added utility methods to more compactly create ItemData instances based on another ItemData, but with different display name and lore.
* Various other internal code refactoring.

**Messages:**  
* All message keys were changed to no longer start with the 'msg' prefix.
* Renamed 'cant-hire' -> 'cannot-hire'.
* Renamed 'cant-hire-shop-type' -> 'cannot-hire-shop-type'.
* Renamed 'cant-trade-with-own-shop' -> 'cannot-trade-with-own-shop'.
* Renamed 'cant-trade-while-owner-online' -> 'cannot-trade-while-owner-online'.
* Renamed 'cant-trade-with-shop-missing-container' -> 'cannot-trade-with-shop-missing-container'.
* Added 'must-target-container'.
* Added 'no-player-shops-via-command'.
* Removed 'no-admin-shop-type-selected'.
* Removed 'no-player-shop-type-selected'.
* Moved various messages from the config into the language files:
  * 'editor-title'
  * 'for-hire-title'
  * 'nameplate-prefix'. Also changed the color to a dark green.
  * 'sign-shop-first-line'. This has been replaced with 8 new messages for the sign lines of different types of shops:
    * 'admin-sign-shop-line1/2/3/4'
    * 'player-sign-shop-line1/2/3/4'
    * The appearance of sign shops has changed. They are slightly more colorful now.
* Changed the default color of 'villager-editor-title' to be less bright.
* Changed the 'too-many-shops' message to be more general.
* Added 'button-name-villager'.
* Added 'button-name-villager-lore'.
* Added 'type-new-villager-name'.
* Added 'villager-name-set'.
* Added 'villager-name-invalid'.
* Added 'button-snowman-pumpkin-head'.
* Added 'button-snowman-pumpkin-head-lore'.
* Added 'button-shulker-color'.
* Added 'button-shulker-color-lore'.
* Added 'button-invulnerability'.
* Added 'button-invulnerability-lore'.
* Changed 'creation-item-selected' to clarify that one has to not aim at any block in order to select the shop or object type.
* Renamed and slightly changed 'command-description-remove' -> 'command-description-remove-all'.
* Slightly changed the default 'command-description-list' message.
* Renamed and slightly changed 'removed-admin-shops' -> 'admin-shops-removed'.
* Renamed and slightly changed 'removed-shops-of-player' -> 'shops-of-player-removed'.
* Renamed and slightly changed 'removed-player-shops' -> 'shops-of-player-removed'.
* Changed 'owner-set'.
* Changed 'set-for-hire'.
* Small fix in 'confirm-remove-all-admin-shops'.
* Small fix in 'confirm-remove-all-own-shops'.
* Small fix in 'confirm-remove-all-shops-of-player'.
* Small fix in 'confirm-remove-all-player-shops'.
* Added 'shops-already-removed'.
* Added 'shop-removals-cancelled'.
* Added 'shop-removed'.
* Added 'shop-already-removed'.
* Added 'shop-removal-cancelled'.
* Added 'confirm-remove-shop'.
* Added 'command-description-remove'.
* Added 'cannot-trade-unexpected-trade'.
* Added 'cannot-trade-items-not-strictly-matching'.
* Added 'cannot-trade-insufficient-storage-space'.
* Added 'cannot-trade-insufficient-currency'.
* Added 'cannot-trade-insufficient-stock'.
* Added 'cannot-trade-insufficient-writable-books'.
* Added 'trade-notification-one-item'.
* Added 'trade-notification-two-items'.
* Added 'buy-notification-one-item'.
* Added 'buy-notification-two-items'.
* Added 'trade-notification-player-shop'.
* Added 'trade-notification-named-player-shop'.
* Added 'trade-notification-admin-shop'.
* Added 'trade-notification-named-admin-shop'.
* Added 'trade-notification-trade-count'.
* Added 'owner-trade-notification-one-item'.
* Added 'owner-trade-notification-two-items'.
* Added 'owner-buy-notification-one-item'.
* Added 'owner-buy-notification-two-items'.
* Added 'owner-trade-notification-shop'.
* Added 'owner-trade-notification-named-shop'.
* Added 'owner-buy-notification-shop'.
* Added 'owner-buy-notification-named-shop'.
* Added 'owner-trade-notification-trade-count'.
* Added 'state-enabled'.
* Added 'state-disabled'.
* Added 'button-trade-notifications'.
* Added 'button-trade-notifications-lore'.
* Added 'confirmation-ui-delete-shop-title'.
* Added 'confirmation-ui-delete-shop-confirm-lore'
* Added 'confirmation-ui-confirm'.
* Added 'confirmation-ui-cancel'.
* Added 'confirmation-ui-cancel-lore'.
* Added 'confirmation-ui-aborted'.
* Added 'villager-no-longer-exists'.
* Added 'confirmation-ui-delete-villager-title'.
* Added 'confirmation-ui-delete-villager-confirm-lore'.
* Added 'villager-removed'.
* Added 'button-sign-glowing-text'.
* Added 'button-sign-glowing-text-lore'.
* Added 'button-axolotl-variant'.
* Added 'button-axolotl-variant-lore'.
* Added 'button-glow-squid-dark'.
* Added 'button-glow-squid-dark-lore'.
* Added 'button-goat-screaming'.
* Added 'button-goat-screaming-lore'.
* Fixed: The german translation was missing translations for 'confirmation-expired' and 'nothing-to-confirm'.
* Minor changes to the german translation.

You will have to manually update your custom language files to adapt to these changes.

## v2.12.0 (2020-11-04)
### Supported MC versions: 1.16.4, 1.16.3, 1.16.2, 1.16.1, 1.15.2, 1.14.4

* Updated for MC 1.16.4.
* Bumped Towny dependency to 0.96.2.0 and using Jitpack repository for it now.
* Fixed: Creating shopkeepers via Citizens trait failed previously if the block at the spawn location is not passable (e.g. when the Citizens NPC stands on a non-full block such as carpet).
* Minor changes to handling failures when trying to create a shopkeeper via Citizens trait. We always inform the player (if there is one), log a warning and delete the trait again now.
* Config: Added some more examples for the 'name-regex' setting to the default config.
* Fixed: Some messages would print an empty line when set to an empty text, instead of being disabled.
* Changed: Players without the permission to edit regular villagers will silently access the regular trading interface now when they sneak and right-click a regular villager.
* Fixed: The Towny integration could run into a NPE when checking if a location is within a commercial area.

API:  
* PlayerOpenUIEvent indicates whether the UI request has been silent now.

Internal:  
* Moved the installation of Spigot dependencies into a separate script.
* Added support for building with Jitpack. This uses a Maven wrapper with a fixed version, because Jitpack uses a buggy Maven version currently (3.6.1).
* Updated building instructions in readme.
* Internal API: UIHandler#canOpen is public and has an additional 'silent' flag now.
* Internal API: UI requests can be silent now.
* Editor UIs are set up lazily now, only when required for the first time.

## v2.11.0 (2020-08-13)
### Supported MC versions: 1.16.3, 1.16.2, 1.16.1, 1.15.2, 1.14.4

Update for MC 1.16.2:  
* Config: Added piglin brute to the by default enabled mob types. If you are updating, you will have to manually enable this yourself.

Other changes:  
* Added: New command `/shopkeeper givecurrency [player] ['low'|'high'] [amount]`.
  * This command can be used to create and give the currency items which have been specified inside the config.
  * Added corresponding permission node `shopkeeper.givecurrency` (default: op).
* Added: When giving items via command to another player we now also inform the target player that he has received items. These newly added messages are also used now when giving items to yourself.
* Added: New command `/shopkeeper convertItems [player] ['all']`.
  * This command can be used to convert the held (or all) items to conform to Spigot's internal data format. I.e. this runs the items through Spigot's item serialization and deserialization in the same way as it would happen when these items are used inside shopkeeper trades and the plugin gets reloaded.
  * Added corresponding permission node `shopkeeper.convertitems.own` (default: op). This allows converting own items.
  * Added corresponding permission node `shopkeeper.convertitems.others` (default: op). This allows converting items of other players.
* Added config options for the automatic conversion of items inside the inventories of players and shop chests to Spigot's internal data format whenever a player is about to open a shopkeeper UI (e.g. trading, editor, hiring, etc.).
  * Added config option `convert-player-items` (default: false). This enables and disables the automatic item conversion.
  * Added config options `convert-all-player-items` (default: true) and 'convert-player-items-exceptions' (default: []). These two settings allow limiting which items are affected or ignored by the automatic conversion.
  * Note: Enabling this setting comes with a performance impact. You should generally try to avoid having to use this setting and instead search for alternative solutions. For more information, see the notes on this setting inside the default config.
* Debug: Added debug option 'item-conversions' which logs whenever we explicitly convert items to Spigot's data format. Note that this does not log when items get implicitly converted, which may happen under various circumstances.
* Config: Added option 'max-trades-pages' (default: 5, min: 1, max: 10) which allows changing the number of pages that can be filled with trading options. This limit applies to all shopkeepers (there are no different settings for different types of shops or different permission levels so far). Note: The scroll bar rendered by the Minecraft client will only work nicely for up to 64 trades.
* Added: It is now possible to change the size of slimes and magma cubes. Their default size is 1 (tiny). Even though Minecraft theoretically allows sizes up to 256, we limit the max size to 10. This avoids running into issues such as related to rendering, performance and not being able to interact with the slime or magma cube. If you have market areas where players can create their own shops, and they are able to create slime or magma cube shopkeepers, you might want to take this maximum slime/magma cube size into account when assigning shop areas to players.
* Fixed: Slimes and magma cubes no longer spawn with random size whenever they are respawned.
* Fixed: Various optional (context dependent) command arguments were shown to be required inside the Shopkeepers command help.
* Fixed: We no longer attempt to spawn Citizens NPCs when creating or loading Citizens shopkeepers if the spawn location's world is not loaded currently.
* Fixed: Some versions of Citizens would produce an error when we try to teleport a NPC which has no location and is therefore not spawned currently. The teleport attempt has been replaced with an attempt to spawn the NPC.
* Fixed: The `shopkeeper.*` permission was missing some child permissions.
* Removed: The legacy permissions `shopkeeper.player.normal`, `shopkeeper.villager`, `shopkeeper.witch` and `shopkeeper.creeper` have been removed. Use the corresponding replacement permissions instead.
* Changed: All players have access to all mob types (permission `shopkeeper.entity.*`) by default now.
* Added: The new 'all' argument for the `/shopkeeper list` command will list all shops now (admin and player shops). (Thanks to Mippy, PR 669)
* Added: It is possible to use barrels and shulker boxes as containers for player shops now.
* Added a message (msg-unsupported-container) when a player tries to select a type of container which is not supported by shopkeepers (i.e. hopper, dropper, dispenser, brewing stand, ender chest, or a type of furnace).
* API: Deprecated PlayerShopkeeper#getChestX/Y/Z, #get/setChest, #getCurrencyInChest, #openChestWindow and PlayerShopCreationData#getShopChest and added corresponding replacements methods with more general names.
* Various UI related changes:
  * API: It is now possible to create and open UIs that are not associated with any shopkeeper. UISession#getShopkeeper may return null now.
  * API: Added PlayerOpenUIEvent.
  * API: The ShopkeeperOpenUIEvent no longer extends ShopkeeperEvent, but the newly added PlayerOpenUIEvent instead.
  * API: Removed Shopkeeper#isUIActive, #deactivateUI, #activateUI. This is now part of the UISession.
  * API: Added UISession#close(), #closeDelayed(), #closeDelayedAndRunTask(Runnable), #abort(), #abortDelayed(), #abortDelayedAndRunTask(Runnable).
  * API: Added Shopkeeper#getUISessions() and #getUISessions(UIType).
  * API: Deprecated Shopkeeper#closeAllOpenWindows() and replaced it with #abortUISessionsDelayed().
  * API: Added UIRegistry#getUISessions(), #getUISessions(Shopkeeper), #getUISessions(Shopkeeper, UIType), #getUISessions(UIType).
  * API: Deprecated UIRegistry#getSession(Player) and added replacement #getUISession(Player).
  * API: Deprecated UIRegistry#getOpenUIType(Player).
  * API: Deprecated UIRegistry#closeAll(), #closeAll(Shopkeeper) and #closeAllDelayed(Shopkeeper) and added replacements #abortUISessions(), #abortUISessions(Shopkeeper) and #abortUISessionsDelayed(Shopkeeper).
* Minor fix: We check if the shopkeeper is still valid before attempting to open its container now.
* Minor fix: If the second buy item of a trading recipe is empty, the corresponding created merchant recipe stores that as an empty second ingredient now. This should help when checking if the existing merchant recipes still match the newly created merchant recipes and thereby cause less recipe updates that are not actually required.
* Fixed: Monster shopkeepers no longer burn when standing in sunlight.
  * Monsters usually get set on fire quite frequently when standing in sunlight. We therefore give zombies and skeletons a stone button as helmet now. This prevents them from getting set on fire in sunlight, by which we have to process less EntityCombustEvents. The stone button is also small enough to not be visible inside their head.
  * Entities standing in lava or fire are still set on fire as before.

Added editor for regular villagers and wandering traders:  
* Any villagers and wandering traders which are not Citizens NPC or shopkeepers are considered 'regular'.
* The editor supports editing the villager trades, similar to how editing works for admin shopkeepers. Note that trades created or edited via the editor will have infinite uses, no xp rewards, no price multipliers and the current uses counter gets reset to 0 (there are currently no options to edit or persist these attributes).
* To not accidentally edit all original trades whenever the editor is opened and closed (and thereby change the above-mentioned trade attributes), we compare the trades from the editor with the villager's current trades before applying the trades from the editor: If the items of a trade are still the same, we keep the original trade without changes. A message indicates how many trades have been modified.
* Since villagers may change their trades whenever they change their profession, we set the villager's xp to at least 1 whenever the villager's trades or profession have been modified via the editor.
* If the villager is killed or gets unloaded while editing, any changes in the editor will have no effect.
* Other supported villager editor options are:
  * Deleting the villager entity.
  * Opening a copy of the villager's inventory. Note that any changes to the opened inventory are not reflected in the villager's inventory currently (i.e. you can view, but not modify the villager inventory with this).
  * Changing the villager profession. Changing the profession via the editor will keep the current trades.
  * Changing the villager type (i.e. the biome specific outfit).
  * Changing the villager level (i.e. the badge color). This also affects which level is displayed and whether the villager's xp is shown within the villager's trading UI.
  * Toggling the villager's AI on and off. This is useful to make the villager stationary while editing it. Otherwise, it may wander away.
* Permissions: Added `shopkeeper.edit-villagers` and `shopkeeper.edit-wandering-traders` (default: `op`). These are required to edit regular villagers or wandering traders respectively.
* Added command `/shopkeeper editVillager [villager]`. This opens an editor to edit the specified villager / wandering trader. The villager / wandering trader can either be specified by uuid or by looking at it.
* Config: Added settings `edit-regular-villagers` and `edit-regular-wandering-traders` (default: `true`). With these settings enabled the villager editor can be opened by simply sneaking and right-clicking a regular villager (similar to how editing works for shopkeepers).

Internal changes:  
* Slightly changed how we cycle through the villager levels (badge colors).
* Added support for Lists of ItemData inside the config.
* We throw an exception now when we encounter an unexpected / not yet handled config setting type.
* We load all plugin classes up front now. This should avoid issues when the plugin jar gets replaced during runtime (e.g. for hot reloads).
* Various internal renaming related to shop containers.
* Replaced EditorHandler#closeEditorAndRunTask with UIHandler#closeDelayedAndRunTask.
* Moved UIHandler#getShopkeeper into a separate ShopkeeperUIHandler class. The existing editor, trading and hiring UIs extend from that now.
* Various internal formatting of code comments.
* Removed UIHandler#closeDelayedAndRunTask and replaced it with using the new methods inside UISession.
* Various minor refactoring inside SKUIRegistry.
* Delayed closing of UIs uses the SchedulerUtils now, which guards against issues during plugin shutdown.
* The created villager trading recipes use a 'max-uses' limit of the maximum integer value now (instead of 10000). If the trade is 'out-of-stock' both the 'max-uses' and the 'uses' are set to 0. This change should probably not affect anyone though.
* Added new command arguments to specify an entity by uuid.
* Added new command arguments to select a targeted entity.
* Moved and added some merchant and trading recipe related utilities into MerchantUtils.
* Minor changes to the comparison of merchant recipes.
* Moved the shopkeeper metadata key constant into ShopkeeperUtils.
* Added ItemUtils#getOrEmpty(ItemStack).
* ShopkeeperUIHandler is an interface now. This allows for more flexibility in the class hierarchy of UI handlers. Added a basic implementation 'AbstractShopkeeperUIHandler'.
* Removed the unused SKDefaultUITypes#register() method.
* Various refactoring related to the editor UI. There is now a separate base class for the shared implementation of the shopkeeper editor and the new villager editor UI. Any shopkeeper references had to be removed from the base class. All existing shopkeeper editor buttons had to be slightly adapted to this change.
* Minor refactoring related to the handling of wandering traders not supporting the baby state.
* Minor: All type registries (shop types, shop object types, UI types, etc.) remember the order of their registered types now. This should have no noticeable effect, other than maybe on the order of command argument completion suggestions.
* Most block and entity shop object types share the same object ids now. The idea is that this might allow for optimization when doing shopkeeper lookups.
* Minor refactoring related to the handling of chat inputs when naming shopkeepers.

Config changes:  
* The default value of the `prevent-shop-creation-item-regular-usage` setting was changed to `true`.
* The default value of the `shop-creation-item` setting was changed to a villager spawn egg with display name `&aShopkeeper`. You can give yourself this item in game via the `/shopkeeper give` command.
* The `use-legacy-mob-behavior` setting was broken since MC 1.14 and has been removed now. All shopkeeper entities always use the NoAI flag now.
* The default value of the `enable-citizen-shops` setting was changed to `true`.
* Changed a few comments inside the default config related to the shop container changes.
* Bumped config version to '3'. A few settings were renamed which get automatically migrated:
  * `require-chest-recently-placed` (now `require-container-recently-placed`)
  * `max-chest-distance` (now `max-container-distance`)
  * `protect-chests` (now `protect-containers`)
  * `delete-shopkeeper-on-break-chest` (now `delete-shopkeeper-on-break-container`)
  * `enable-chest-option-on-player-shop` (now `enable-container-option-on-player-shop`)
  * `chest-item` (now `container-item`)

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
* msg-unsupported-container
* msg-missing-edit-villagers-perm
* msg-missing-edit-wandering-traders-perm
* msg-must-target-entity
* msg-must-target-villager
* msg-target-entity-is-no-villager
* msg-villager-editor-title
* msg-villager-editor-description-header
* msg-villager-editor-description
* msg-button-delete-villager
* msg-button-delete-villager-lore
* msg-button-villager-inventory
* msg-button-villager-inventory-lore
* msg-button-mob-ai
* msg-button-mob-ai-lore
* msg-villager-inventory-title
* msg-set-villager-xp
* msg-no-villager-trades-changed
* msg-villager-trades-changed
* msg-command-description-edit-villager

Changed messages:  
* Some message settings were renamed. If you don't use a custom / separate language file, they get automatically migrated as part of the config migration to version 3. However, most of these messages also had changes to their default contents which need to be applied manually.
  * msg-button-chest (now msg-button-container)
  * msg-button-chest-lore (now msg-button-container-lore)
  * msg-selected-chest (now msg-container-selected)
  * msg-must-select-chest (now msg-must-select-container)
  * msg-no-chest-selected (now msg-invalid-container)
  * msg-chest-too-far (now msg-container-too-far-away)
  * msg-chest-not-placed (now msg-container-not-placed)
  * msg-chest-already-in-use (now msg-container-already-in-use)
  * msg-no-chest-access (now msg-no-container-access)
  * msg-unused-chest (now msg-unused-container)
  * msg-cant-trade-with-shop-missing-chest (now msg-cant-trade-with-shop-missing-container)
* msg-creation-item-selected
* msg-shop-setup-desc-selling
* msg-shop-setup-desc-buying
* msg-shop-setup-desc-trading
* msg-shop-setup-desc-book
* msg-trade-setup-desc-selling
* msg-trade-setup-desc-buying
* msg-trade-setup-desc-book

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
  * Any items stored inside the shopkeepers (e.g. for their trades or hire cost items) are automatically migrated.
* Note on Minecraft's new RGB color codes and other new text related features: I have not yet looked into supporting those in the texts and messages of the Shopkeepers plugin.

**Other migration notes:**  
* Removed: We no longer migrate items inside the config from legacy (pre MC 1.13) item types, data values and spawn eggs to corresponding item types in MC 1.13. Instead, any unknown item types get migrated to their default now.

**Other changes:**  
* Changed/Improved: We use a combination of our own 'Shopkeepers data version' (which has been bumped to 2) and Minecraft's data version for the data version stored inside the save.yml now. Minecraft's data version is incremented on every Minecraft release (including minor updates) and may indicate that new item migrations have been added. So whenever you update your server, we automatically trigger a full migration of all your shopkeeper data to ensure that your save.yml is always up-to-date.
* Fixed: Some mobs randomly spawn with passengers. We remove these passengers now.
* Fixed: When a mob randomly spawns with a passenger, this would previously interfere with our 'bypass-spawn-blocking' feature and print a warning inside the log about an unexpected entity (the passenger) being spawned.
* Fixed: The random equipment of certain mobs gets properly cleared now. For instance, this resolves the issue of foxes randomly carrying items inside their mouth.
* Fixed: When a Citizens NPC, created without the 'shopkeeper' trait, is deleted, we immediately delete any corresponding shopkeeper now. Previously, the corresponding shopkeeper would not get deleted right away, but only during the next plugin startup (when checking whether the corresponding NPC still exists). Any chest used by the shopkeeper would remain locked until then.
* Improved: In order to determine the player who is setting up a shopkeeper via the 'shopkeeper' trait, we previously only took players into account which are adding the trait via the Citizens trait command (NPCTraitCommandAttachEvent). However, players are also able to add traits during NPC creation. We now also react to players creating NPCs (PlayerCreateNPCEvent) and then (heuristically) assume that any directly following trait additions for the same NPC within one tick are caused by this player. This player will then be able to receive feedback messages about the shopkeeper creation.
* Improved: For any trait additions not directly associated with a player, we previously waited 5 ticks before the corresponding shopkeeper got created. One (minor) side effect of the above change is that we react to all trait additions within 1 tick now.
* Added: Added a link inside the config which points to the translations repository.
* Added: Added feedback messages when a player cannot trade with a shop due to trading with own shops being disabled or if the shop's chest is missing.
* Changed: The check for whether the player is able to bypass the restriction of not being able to trade with own shops was previously checking if the player is an operator. Instead, we now check if the player has the bypass permission.
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
* Removed the note about left and right-clicking items to adjust amounts from the 'msg-trade-setup-desc-admin-regular' message, since this doesn't actually apply to admin shops.

## v2.9.3 (2020-04-12)
### Supported MC versions: 1.15.2, 1.14.4

* Fixed: If a trade required two matching item stacks of the same type but different sizes, it was possible to trade for fewer items when offering the items in reverse order.
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
  * Additionally, we only update those inventory slots that were actually changed by inventory manipulations. This also has the benefit of sending fewer inventory slot updates.

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
  * This will typically occur with every (even minor) Minecraft updates.
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
* Config/Fixed: Some settings would not load correctly depending on the used locale. Also made all text comparisons locale independent.
* Config/Fixed: In case the name-regex setting cannot be parsed, we now print a warning and revert to the default (instead of throwing an error).
* API/Fixed: NPE when accessing a non-existing second offered item from the ShopkeeperTradeEvent.
* API/Fixed: The offered items inside the ShopkeeperTradeEvent are copies now and their stack sizes match those of the trading recipe.
* Messages/Fixed: The internal default for message 'msg-list-shops-entry' (that gets used if the message is missing in the config) was not matching the message in the default config.
* Internal/Fixed: Improved thread-safety for asynchronous logging operations and settings access.
* Changed: Villager shopkeepers get their experience set to 1 now. I wasn't able to reproduce this myself yet, but according to some reports, villager shopkeepers would sometimes lose their profession. Setting their experience to something above 0 is an attempt to resolve this.
* Changed: Instead of using a fallback name ("unknown"), player shops are required to always provide a valid owner name now.
* Changed: Explicitly checking for missing world names when loading shopkeepers.
* Changed: Added validation that the unique id of loaded or freshly created shopkeepers is not yet used.
* Changed: Added more information to the message that gets logged when a shopkeeper gets removed for owner inactivity.
* Changed: The errors about a potentially incompatible server version and trying to run in compatibility mode are warnings now.
* Messages/Changed: The msg-shop-creation-items-given message was using the player's display name. This was changed to use the player's regular name to be consistent with the rest of the plugin.
* Config/Changed: The plugin will now shutdown in case a severe issue prevents loading the config. This includes the case that the config version is invalid. Previously, it would treat invalid and missing config versions the same and apply config migrations nevertheless.
* Config/Changed: The always-show-nameplates setting seems to be working again (since MC 1.9 already). The corresponding comment in the default config was updated.
* Config/Changed: Changed/Added a few information/warning messages related to config and language file loading.
* Config/Changed: Only printing the 'Config already loaded' message during startup if the debug mode is enabled.
* Debug: Debug option 'owner-name-updates' enables additional output whenever stored shop owner names get updated.
* Various changes to the shopkeeper registry and shopkeeper activation:
  * We keep track now which chunks have been activated. This avoids a few checks whether chunks are loaded.
  * The delayed chunk activation tasks get cancelled now if the chunk gets unloaded again before it got activated. This resolves a few inconsistencies such as duplicate or out-of-order chunk activation and deactivation handling when chunks get loaded and unloaded very frequently.
  * Similarly, the respawn task on world saves gets cancelled now if the world gets unloaded.
  * Shopkeeper spawning is skipped if there is a respawn pending due to a world save.
  * Shopkeepers are now stored by world internally. This might speed up a few tasks which only affect the shopkeepers of a specific world.
  * Debug: Added debug option 'shopkeeper-activation'. Various debug output related to chunk/world loading/unloading/saving and spawning/despawning of shopkeepers was moved into this debug category.
  * Debug: The "/shopkeepers check" command now outputs some additional information about active chunks.
  * Internal: Moved all logic from WorldListener into SKShopkeeperRegistry. Various internally used methods are now hidden.

**API changes:**  
* Added: PlayerShopkeeper#getChestX/getChestY/getChestZ
* Added: ShopkeepersStartupEvent which can be used by plugins to make registrations during Shopkeepers' startup process (e.g. to register custom shop types, object types, etc.). This event is marked as deprecated because custom shop types, object types, etc. are not yet officially supported as part of the API. Also, the event is called so early that the plugin (and thereby the API) are not yet fully set up and ready to be used, so this event is only of use for plugins which know what they are doing.
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
  * Previously, arguments were parsed one after the other. In the presence of optional arguments this can lead to ambiguities. For example, the command "/shopkeeper list [player] 2" with no player specified is supposed to fall back to the executing player and display his second page of shopkeepers. Instead, the argument '2' was previously interpreted as the player name and the command therefore failed presenting the intended information.
  * A new mechanism was added for these kinds of fallbacks: It first continues parsing to check if the current argument can be interpreted by the following command arguments, before jumping back and then either providing a fallback value or presenting a likely more relevant error message.
  * Most optional arguments, default values and fallbacks were updated to use this new fallback mechanism, which should provide more relevant error messages in a few edge cases.
* Fixed: Commands would sometimes not correctly recognize the targeted shopkeeper entity. This is caused by SPIGOT-5228 keeping dead invisible entities around, which get ignored by the commands now.
* Added: The "give", "transfer", "list" and "remove" commands show the player's uuid as hover text now and allow it to be copied into the chat input via shift clicking.
* The "list", "remove", "transfer" and "setTradePerm" commands can be used from console now. Command confirmations work for the console as well now (any command sender that is not a player is considered to be the 'console' for this purpose).
* The "setForHire" and "transfer" commands allow specifying the shopkeeper via argument now. Also: When targeting a chest that happens to be used by multiple shopkeepers (e.g. due to manually modified save data), it picks the first one now (instead of applying the command to all shops). In the future this will likely print an error message instead.
* The "debugCreateShops" command limits the shop count per command invocation to 1000 and prints the number of actually created shops now.
* Added debug option 'commands' and added various debug output when parsing and executing commands.
* All argument suggestions are limited to 20 entries by default now.
* Player arguments suggest matching uuids now. To avoid spamming the suggestions with uuids for the first few characters of input, suggestions are only provided after at least 3 characters of matching input.
* Some commands (e.g. "list") provide suggestions for names of online players now.
* The "list" and "remove" commands accept player uuids now and ignore the case when comparing player names.
* The "list" and "remove" commands handle ambiguous player names now: If there are shops of different players matching the given player name, an error message is shown and the player needs to be specified by uuid instead. The player names and uuids can be copied to the chat input via shift clicking. If a player with matching name is online, that player is used for the command (regardless of if the given player name is ambiguous).
* The shops affected by the "remove" command are now determined before asking for the user's confirmation. This allows detecting ambiguous player names and missing player information before prompting the command executor for confirmation. A minor side effect of this is that any shops created after the command invocation are no longer affected by the remove command once it gets confirmed.
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
* Internal: Minor changes to handling errors during command handling. In addition to the stack trace, the plugin also logs the command context (parsed arguments) now.
* Internal: CommandArgument#parse now also returns the parsed value. This is useful when there is a chain of wrapped arguments and the parent needs to handle the value parsed by the child argument in some way.
* Internal/Fixed: Chains of FirstOfArguments should now be able to properly store all the values parsed by child arguments along the chain. Previously, this only worked for a chain depth of 1.
* Internal: Added validation that no arguments with the same name get added to the same command.
* Internal: Moved default shopkeeper argument filters into ShopkeeperFilter class/namespace.
* Internal: Added map view, toString and copy to CommandContext and made constructor CommandContext(otherContext) protected.
* Internal: CommandArgs no longer makes a copy of the passed arguments.
* Internal: Added marker interface for the CommandArgs state. When resetting CommandArgs to a previous state they ensure now that the state got created from the same CommandArgs instance.
* Internal: Added CommandArgs#copy. Since the underlying arguments are expected to never change, they are not actually copied (only the parsing state is copied). Any captured CommandArgs states are applicable to the copy as well.
* Internal: Replaced CommandArgs with ArgumentsReader: The arguments are now stored inside the CommandInput and the ArgumentsReader only references them from there.
* Internal: CommandContext is now an interface (with SimpleCommandContext as implementation). A new class CommandContextView was added that gets used anywhere where accessing the context is allowed, while modifying is not.
* Internal: Command and tab completion handling was moved from BaseCommand into Command.
* Internal: Resetting of the ArgumentsReader if parsing of a command argument failed was moved from CommandArgument#parse into Command.
* Internal: Added TypedFirstOfArgument, a variant of FirstOfArgument that preserves the result type of its child arguments.
* Internal: Added NamedArgument, which can be useful if there would otherwise be conflicts / ambiguities between arguments.
* Internal: ArgumentParseException provides the command argument that created it now. This is especially useful for debugging purposes.
* Internal: The command library was updated to use a new text representation everywhere now, and thereby support text features such as hover events, click events, etc.

Other internal changes:  
* Internal: The save task was using a copy of the save data to protect against concurrent modifications while an async save is in progress. However, since the actual save data did not get modified during ongoing saves anyway, this copy is redundant and was therefore removed.
* Internal: Made various changes in order to support Minecraft's text features such as hover events, click events, insertions, etc. Most texts are now stored in a new format internally.
* Internal: Various changes to TextUtils and argument replacement.
  * Arguments can be arbitrary objects now and Suppliers can be used to dynamically look up argument values.
  * Argument replacement uses a Map-based argument lookup now. Placeholder keys are specified without the surrounding braces now. Argument replacements for both plain Strings and the new text format should be faster now.
* Internal: Moved newline pattern and splitting into StringUtils.
* Internal: The regex pattern used to validate shopkeeper names gets precompiled now.
* Internal: bstats gets shaded into the package '[...].libs.bstats' now.
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
* Added: Changed how items are getting defined in the config. Internally this new format uses Bukkit's item serialization for parsing the item data, which allows it to support the specification of arbitrary item data and hopefully not require any major maintenance for future Minecraft versions. At the same time it tries to stay (slightly) more user-friendly than Bukkit's item serialization by omitting any data that can be restored by the plugin, by avoiding one level of nesting between the item type and item data, by translating ampersand ('&') color codes in display name and lore, and by offering a compact representation for specifying an item only by its type.
  * This change also allows a more detailed specification of some of the editor button items. However, many editor buttons still miss corresponding config settings. Also, keep in mind that the display name and lore for these button items get specified via corresponding message settings, so any specified item display name and lore will get replaced by that.
  * When checking if an in-game item matches the item data specified in the config, only the specified data gets compared. So this does not check for item data equality, but instead the checked item is able to contain additional data but still get matched (like before, but previously this was limited to checking display name and lore).
  * The previous item data gets automatically migrated to the new format (config version 2).
* Renamed the setting 'high-zero-currency-item' to 'zero-high-currency-item'. This gets automatically updated during the migration.
* Changed: All priorities and ignoring of cancelled events were reconsidered.
  * Event handlers potentially modifying or canceling the event and which don't depend on other plugins' event handling are called early (LOW or LOWEST), so that other plugins can react to / ignore those modified or canceled events.
    * All event handlers which simply cancel some event use the LOW priority now and more consistently ignore the event if already cancelled. Previously, they mostly used NORMAL priority.
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
* Changed: Some entity attributes are set up prior to entity spawning now (such as metadata, non-persist flag and name (if it has/uses one)). This should help other plugins to identify Shopkeeper entities during spawning.
* Changed: Added setting 'increment-villager-statistics' (default: false) which controls whether opening the trading menu and trading with shopkeepers increment Minecraft's 'talked-to-villager' and 'traded-with-villager' statistics. Previously, the talked-to-villager statistics would always get incremented and the traded-with-villager statistic was not used.
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
* Internal: Avoiding ItemStack#hasItemMeta calls before getting an item's ItemMeta, since this might be heavier than simply getting the ItemMeta directly and performing only the relevant checks on that. Internally ItemStack#hasItemMeta checks emptiness for all item attributes and might (for CraftItemStacks) even first copy all the item's data into a new ItemMeta object. And even if the item actually has no data (Bukkit ItemStack with null ItemMeta), ItemStack#getItemMeta will simply create a new empty ItemMeta object without having to copy any data, so this is still a similarly lightweight operation anyway.
* Internal: Made all priorities and ignoring of cancelled events explicit.
* Internal: Moved code for checking chest access into util package.
* Internal: Moved config migrations into a separate package.
* Internal: Moved some functions into ConfigUtils.
* Internal: Slightly changed how the plugin checks whether the high currency is enabled.
* Internal: Metrics will also report now whether the settings 'check-shop-interaction-result', 'bypass-spawn-blocking', 'enable-spawn-verifier' and 'increment-villager-statistics' are used.
* Internal: Skipping shopkeeper spawning requests for unloaded worlds (should usually not be the case, but we guard against this anyway now).
* Internal: Spigot is stopping the conversion of zombie villagers on its own now if the corresponding transform event gets cancelled.
* Internal: Added a test to ensure consistency between ShopkeepersPlugin and ShopkeepersAPI.
* Internal: Added ItemData tests. This requires CraftBukkit as new test dependency due to relying on item serialization.

Debugging:  
* Debugging: Added new debug command "/shopkeepers yaml", which prints Bukkit's yaml serialization and the item data representation used in the config for the currently held item.
* Debugging: Added entity, invalid entity and dead entity count to shopkeeper's "check" command. Also restructured the output slightly to make it more compact and clearer.
* Debugging: Minor changes to the "checkitem" command. It now compares the items in main and off hand and also checks if the item in the main hand matches the item in the off-hand.
* Debugging: Small changes and additions to some debug messages, especially related to shopkeeper interactions and shopkeeper spawning.
* Debugging: Added setting 'debug-options', which can be used to enable additional debugging tools.
  * Option 'log-all-events': Logs all events. Subsequent calls of the same event get combined into a single logging entry to slightly reduce spam.
  * Option 'print-listeners': Prints the registered listeners for the first call of each event.

## v2.7.2 (2019-07-02)
### Supported MC versions: 1.14.3

* Bumped Bukkit dependency to 1.14.3.
* Changed: With MC 1.14.3 custom merchants will no longer display the 'Villagers restock up to two times per day' message when hovering over the out-of-stock icon.
* Changed: Spigot is hiding the unused xp bar from custom merchant inventories now. The dynamic updating of trades (out-of-stock icon) was adapted accordingly.
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
* Made shop and object type matching stricter. This uses a fixed list of internal aliases now.
* Removed the generic 'sub-type' editor option in favor of letting each shop object supply a list of editor options. This allows living shopkeepers to provide multiple editor options now.
  * API: Removed getSubTypeItem, cycleSubType and equipItem from ShopObject. Editor options are internal API for now, and mob equipment hasn't properly worked already before due to not getting persisted.
* Added new mob attribute editor options:
  * All ageable mobs (except the wandering trader and parrots) and all zombies (zombie, husk, drowned, pig zombie, zombie villager): Baby variant. Previously, this options was only available for zombie and pig zombie shopkeepers. The editor item for this option is a regular chicken egg now.
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
* Added the ability to cycle the editor options back and forth via left and right-clicking.
* Updated for the latest WorldGuard changes. You will have to update WorldGuard for the WorldGuard integration to work.
  * The 'allow-shop' flag has been removed from WorldGuard and is now left to other plugins to register themselves. Shopkeepers will now attempt to register this flag if no other plugin has registered it yet (one such other plugin is for example ChestShop). Since WorldGuard only allows registering flags before it got enabled, but we are loading the config at a later point, we will always attempt to register the flag, even if the WorldGuard integration is disabled in the config.
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
  * The admin and trading shop editors were updated to reflect those changes. This change might initially confuse anyone who is used to the previous admin trade setup (with the result item being at the bottom), but should ultimately be easier to remember due to being consistent among all shopkeeper types.
* It is now possible to switch between up to 5 pages inside the editor, allowing for a total of 45 trades to be set up per shopkeeper.
  * Added settings for the button items used to switch between pages.
* The editor buttons were moved to the bottom row. In the future the additional space may be used for more editor options.
  * The chest button option for player shops will no longer replace the naming button, but they will be both available side by side now.
* Improved the setup of the trading player shopkeepers:
  * Inside the editor, the player picks up items in their inventory and can then freely place copies of those items in the trades section of the editor to specify the traded items. The picked up items will appear on the player's cursor, making the setup more apparent.
  * To make the inventory interaction more fluent, item dragging that only involves a single slot gets interpreted as click.
  * It is now also possible to set up multiple trades for the same result item.
* Visualizing trades that are out-of-stock:
  * When a trade runs out of stock it gets deactivated, but is still visualized by limiting the trade's remaining uses.
  * The trades get updated for the trading player dynamically after every trade attempt. This will not work in compatibility mode in which case it will behave as before, with trading simply getting cancelled without visual feedback for the user.
  * The player shopkeepers will also keep displaying their set up trades in the editor, even if the corresponding items are no longer present in the chest.
  * Trades of the book shopkeeper for books that are no longer present in the chest will be displayed in the form of dummy books with unknown author and 'tattered' generation.
* An item inside the editor briefly explains the trade setup for the specific type of shopkeeper.
  * Added setting 'trade-setup-item'.
  * Changed the shop type display names to upper case and with 'shop' prefix, since this is now also used for the trade setup item.
  * Slightly changed the admin and book shopkeeper description messages.

Other changes:  
* Changed: Clicking the naming or chest editor buttons will no longer trigger two consecutive saves.
* Changed: The book shopkeeper's behavior more closely matches Minecraft's behavior now: Only original and copies of original books can be copied. When copied, the book's generation is increased.
* Changed: The book shopkeeper editor will filter different books with the same title and only consider the first one it finds in the chest.
* Changed: The transfer command will now transfer all shops that use a chest that are owned by the player. Previously, it would report 'no permission' if there was a single non-owned shop that was using the same chest. If the player has the bypass permission, all shops will get transferred (like before), regardless of the owner.
* Added: The transfer, remote, setForHire and setTradePerm commands allow targeting the shopkeeper directly now, instead of having to target the corresponding chest.
* Changed: The shopkeeper remote command can now also be used to open player shopkeepers. Also added the alias 'open' for this command.
* Added: 'edit' command to remotely edit shops. Permission 'shopkeeper.remoteedit' (default: op). This works with both admin and player shops. Shops can be specified via argument (id, name, etc.) or by targeting a shop or shop chest.
* Added: 'give' command that can be used to give players shop creation items. Permission 'shopkeeper.give' (default: op).
* Added: Allow backwards cycling through shop and shop object types with the shop creation item in hand by left-clicking.
* Changed: Improved handling of shopkeeper names with colors and whitespace in commands.
* Changed: Using the shop type and object type display names for the command argument suggestions.
* Fixed: The list command's max page is now at least 1 (even if there are no shops to list).
* Fixed: Missing material settings couldn't be automatically inserted from the defaults.
* Fixed: List messages wouldn't be loaded from alternative language files.

Internal changes:  
* Various refactoring related to the editor code common to all shopkeeper types.
* Small changes related to handling start and end of UI sessions of players.
* As precaution, the trading player shop verifies now that there actually still is a corresponding offer set up for the currently traded recipe.
* API: Removed UIRegistry#onInventoryClose(Player)
* API: Calling ShopkeeperEditedEvent on regular editor inventory closes as well now.

This update includes many message changes. It is therefore suggested letting Shopkeepers freshly generate all default messages.  

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
* Reworked chest protection to be slightly less strict:
  * Players with bypass permission can now bypass block placement restrictions when placing blocks next to protected shop chests.
  * Chests next to a shop's chest are now only protected / prevented from being placed if they are actually connected to the shop's chest.
  * Placement of adjacent hoppers is now only prevented if they would be able to extract or inject items into the chest.
  * Added: Also restricting placement of droppers in a similar way now.
* Changed: If the chest protection is disabled in the config, chest access will not be prevented either now. (It doesn't seem to make much sense to prevent chest access but allow the chest to be broken/destroyed)
* Changed: If 'delete-shopkeeper-on-break-chest' is enabled, the shopkeeper will now no longer be removed if the half of a double chest gets broken that is not directly used by the shopkeeper.
* Changed: If 'delete-shopkeeper-on-break-chest' is enabled, the shopkeepers will now also be removed if the chest gets destroyed by an explosion. And if 'deleting-player-shop-returns-creation-item' is enabled, shop creations items will be dropped for those shopkeepers as well.

**Improved shopkeeper placement:**  
* Fixed: Treating other air variants as empty as well when trying to place a shopkeeper.
* Slightly changed how the spawn block is determined to more closely match the behavior of vanilla Minecraft: If the clicked / targeted block is passable (i.e. tall grass, etc.) this block gets used as spawn location. Only otherwise the spawn location is shifted according to the clicked / targeted block face.
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
* Increased the built-in maximum shopkeeper name length from 32 to 128. The actual name length limit is still limited by the default config settings, and some types of shopkeeper objects will not be able to actually display names of larger lengths.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if you are just updating to MC 1.13.x.

## v2.4.1 Beta (2018-09-11)
### Supported MC versions: 1.13.1
* Added: A (very simple) minimum version check.
* Fixed #522: CME during reload when closing shopkeeper UIs.
* Fixed #521: Error when adding a shopkeeper to an existing Citizens NPC via trait.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if you are just updating to MC 1.13.x.

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
  * Changed: Signs that could not be respawned will no longer cause the shopkeeper to be deleted. Instead, a 3-minute spawning delay was added to prevent potential abuse (in case the spawned signs drop for some reason/bug).
  * Added a warning if the facing direction of a sign shopkeeper could not be determined or is missing. Since the signs are getting removed now, it will no longer attempt to determine the sign facing from the world now. Instead, it will fall back to using some arbitrary default facing direction then.
* Changed: Blocks to which shopkeeper signs are attached to are now protected as well.
* API/Internal: Spawn locations passed to the ShopCreationData are now in the center of the spawn block and contain yaw and pitch to face the creating player. Shop objects can now use this new information if they wish.
* Added: Sign shopkeepers now support sign posts (if the creating player targets the top of a block). The sign is rotated to face towards the creating player.
* Added: The setting 'enable-sign-post-shops' (default true) can be used to disable creation of sign post shops.

**Experimental change related to handling shopkeeper entities:**  
* Using Bukkit's new (experimental) non-persistent entities feature: This should make sure that shopkeeper entities don't get saved to the chunk data during world saves and by that also prevent any kind of duplicate entities issues (assuming it works correctly).
* As consequence, all existing fallback code was removed:
  * No longer keeping track of the uuid of the last spawned entity (this should also reduce the need of periodic saves of shopkeeper data) and no longer searching for old entities before spawning an entity.
  * No longer handling silent / unnoticed chunk unloads and no longer handling entities that got pushed out of their chunk (it's assumed that the non-persistent entities get automatically removed).
  * No longer temporarily despawning all shopkeeper entities during world saves (less performance impact and less visual disturbance for players).
However, in case something doesn't work as expected, this change has the potential to cause entity related issues (thus 'experimental').

**Other changes:**  
* Changed: Citizens shopkeepers are now identified by the Citizens NPC's unique ids. Conversion should happen automatically once Citizens is detected to be running.
* Fixed: No longer deleting the Citizens NPC when a shopkeeper is deleted due to another shopkeeper using the same NPC.
* Changed (internal): Removal of invalid Citizens shopkeepers was moved and gets run now everytime Citizens gets enabled.
* Fixed: There might have been some trading issue related to undamaged damageable items not being considered matching since 1.13.
* Debugging/Changed: Durability is no longer displayed as part of the recipe items debugging information.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if you are just updating to MC 1.13.x.

## v2.3.4 Alpha (2018-08-18)
### Supported MC versions: 1.13
* Fixed: Default config values for primitives were not handled correctly.
* Fixed: Sign facing wasn't applied correctly since the update to 1.13.
* Fixed: The sign physics protection broke somewhere in MC 1.13.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if you are just updating to MC 1.13.

## v2.3.3 Alpha (2018-08-01)
### Supported MC versions: 1.13
* Fixed: Defaults values for missing config values not getting properly added.
* Fixed: Not decolorizing default values which are not meant to be colorized.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if you are just updating to MC 1.13.

## v2.3.2 Alpha (2018-08-01) [broken]
See 2.3.3 instead.

## v2.3.1 Alpha (2018-08-01)
### Supported MC versions: 1.13
* This version relies on the very latest version of CraftBukkit / Spigot. Make sure to **update your server** to the latest Spigot build before running this version!
* Updated link to project website.
* Fixed: Chicken shopkeepers should no longer lay eggs.
* Fixed: Shopkeeper entities should no longer gain potion effects (for any reason).
* Fixed: Player shop type migration broke in v2.3.0.

**Make sure you have read the changelog and notices of v2.3.0 before installing this version!** Especially if you are just updating to MC 1.13.

## v2.3.0 Alpha (2018-07-18)
### Supported MC versions: 1.13
**This update brings support for MC 1.13:**  
**Some important notices to begin with:**  
* Support for versions below 1.13 has been dropped. It is only compatible with the latest builds of Spigot 1.13-pre7.
* This update is **experimental**! Don't use it for live servers yet, until any upcoming issue have been fixed.
* Before installing: **Back up your existing shopkeeper data!** This update will make irreversible changes and might not even be able to import all the previous data.
* **Updating is only supported from version v2.2.1 and MC 1.12.2!** Updating from older Minecraft or Shopkeepers versions hasn't been tested and might not properly work, because some old migration code has been removed with this update as well. So if you are updating from an older version of Shopkeepers OR Minecraft, first update to MC 1.12.2 and Shopkeepers v2.2.1.

**Migration procedure:**  
Item data values have been removed and various material (item/block) names have changed to be more in-line with vanilla Minecraft names. So this update requires a migration of existing configs and shopkeeper data.
* If you use any item ids inside your config (if your config is very old): Those are no longer supported at all, and you will have to manually replace them with the corresponding material names prior to running this update.
* Config migration: When being run for the first time (if there is no 'config-version' present in the existing config), this update attempts to convert previous materials and data values, and the shop-creation-item from the config. However, there is no guarantee for this to work for all materials. It will log every migration it performs and might fall back to using default materials. So check the log and the resulting config and make sure everything went fine.
* Shopkeeper data migration: Shopkeeper trades will get converted by Bukkit/Spigot itself. So it's up to them to make sure that this works correctly.
* The plugin will trigger a save of all shopkeeper data, so that any legacy materials that got converted by Bukkit during loading of the shopkeepers will also end up being saved with their updated materials to the save data. However, any shopkeepers that cannot be loaded for some reason will skip this procedure. So make sure that all your existing shopkeepers load fine prior to installing the update, or you might have to update the data of those shopkeepers manually in the future.
* If some materials cannot be converted, the result might be some kind of fallback: Empty spawn eggs for example will be converted to pig spawn eggs by Bukkit. There is no existing documentation available so far regarding which fallbacks exist.

**Updating is only supported from version v2.2.1 and MC 1.12.2:**  
* Removed support for loading very old (MC 1.8) serialized item attributes.
* Removed MC 1.10 villager profession id migrations.
* Removed MC 1.11 entity type migrations.
* Removed support for importing very old shopkeeper trades (changed somewhere in 2015).

**Other changes related to the update:**  
* Removed the `skip-custom-head-saving` setting: Previously, this was meant to work around some issue with saving custom head items (head items with custom texture and no player name) causing corrupted save data. The data corruption / crashing should be fixed by Bukkit by now (by completely dropping 'support' for those custom head items though).
* Added: All 1.13 mobs have been added to the default config. There is no support for them beyond that yet (no support to switch to baby variants, etc.).
* Improvement: The chest and sign protection takes block explosions into account.
* Internal: Reduced the amount of version specific code, since for many things there are Bukkit alternatives available by now.
* Internal: The save data now contains a 'data-version'. This can be used to determine required migrations (not used for that currently), or force a save of all loaded shopkeeper data.

**Other changes:**  
* Internal: Sheep colors were previous saved by using the old wool data values. They are now converted and saved by their color names.
* Improvement: Logging a message when importing old book offers (changed during late 1.12.2) and old sheep colors (changed with this update).
* Improvement: Added a warning message when not able to load a cat type or sheep color and falling back to the default in that case.
* Improvement: When testing if a player can access a chest, it now clears the off hand as well during the check (making the check more accurate).
* Improvement: When the plugin is reloaded and the config is missing values, it should now use the actual default values instead of the pre-reload values. (There is still a fallback of using the previous values)
* Internal: There have been some formatting changes to the permissions section of the plugin.yml. This gets copied into the wiki now.
* Internal: Using player ids instead of player names for temporary data at a few places now.
* Internal: Minor changes to the internal maven project layout.

## v2.2.1 Release (2018-07-02)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Fixed: Some internal shop object ids have slightly changed in the save data. This update is able to load and convert those old ids.
* Added config validation for enabled living shop types and improved entity type matching.
* Changed: Using the normalized entity type name in the 'selected shop object type' messages.
* Debugging: Added some storage debugging output to the 'check' command.

If you are updating, please read through the changelogs of the previous few versions! They also contain updating hints (e.g. regarding changed messages).

## v2.2.0 Release (2018-06-29)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Various changes (and minor internal fixes) related to commands.
  * Displayed command names and aliases don't get formatted into lower case anymore (matching still uses the lower case version).
* API: Minor renaming of a few permission names (only affects the API, the actual permissions are still the same).
* API: Various changes and additions related to shop object types.
  * It is now possible to differentiate between entity and block shop object types, and to get the entity that is currently representing a shopkeeper.
  * Renamed ShopObject#getObjectType() to ShopObject#getType().
  * A few internal shop object ids have changes to be slightly more consistent.

Since there have been no reported issues with the previous beta versions, I mark this version as 'release' to get a few more people to use this new version. If you are updating, please read through the changelogs of the previous beta versions! They also contain updating hints (e.g. regarding changed messages). The easiest way to update your config and messages is to remove it and let the plugin regenerate it, and then re-apply you custom changes.

## v2.1.0 Beta (2018-06-18)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
**Major internal changes to the way shopkeepers get created and their data gets saved:**  
* Changes to shopkeeper ids: 'Session id' is now named only 'id' , and persists across server restarts / plugin reloads.
  * API: Renamed getShopkeeper-methods to ShopkeeperRegistry#getShopkeeperById and ShopkeeperRegistry#getShopkeeperByUniqueId
* All shopkeeper data gets kept in memory now and during saves, only the data of dirty shopkeepers gets updated.
  * This should come with a large performance improvement if there are many shopkeepers loaded, but only few of them change between saves.
  * Internal/API: Shopkeepers are automatically marked as dirty when some of their data has changed.
  * When legacy shopkeeper or shop object data gets found and imported, the shopkeeper gets marked as dirty.
  * Dirty shopkeeper data gets saved back to file right after plugin start (e.g. any imported legacy data).
  * Internal: Various changes to the way the sync saves get handled (e.g. during plugin disable).
  * Change: We no longer shut down the plugin when a shopkeeper cannot be loaded. The invalid shopkeeper data should get saved back to file again now, without any information being lost.
  * Change: We no longer use the default object type if a shopkeeper with invalid object type is loaded. Instead, the shopkeeper is skipped during loading.
  * Change: We no longer default player shops of unknown type to 'normal'. Instead, the shopkeeper is skipped during loading.
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
* Debugging: Added various debugging output related to Citizens shopkeepers.
* Debugging: Saving debug messages show the number of dirty and number of deleted shopkeepers that were saved now.
* Internal: Saving aborts any delayed saving task now (fewer unneeded saves).
* Various internal project restructuring:
  * This should also make it easier for other plugins to depend on Shopkeepers or ShopkeepersAPI.
  * Various classes were moved around again: If your plugin depends on Shopkeepers' API, you may have to update.
* Various other internal refactors.

Due to the various message related changes, you will have to update your messages. The easiest way to do this, it to remove all messages from the config and let the plugin generate the defaults on the next server start.

## v2.0 Beta (2018-06-05)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
**Major change to Shopkeepers' mob behavior:**  
* Shopkeeper mobs by default use Minecraft's NoAI flag now:
  * This disables various internal AI behavior that would otherwise still be active in the background (villagers for example would periodically search for nearby villages and even cause chunk loads by that).
  * This makes shop entities unpushable (at least on certain MC versions, see below).
* However:
  * In order to keep the look-at-nearby-players behavior, the plugin now manually triggers the Minecraft logic responsible for that.
  * In order for the entities to still be affected by gravity, the plugin now manually periodically checks for block collisions below mobs and then teleports them downwards. This doesn't look as smooth as Minecraft's gravity motion, but it should suffice (especially since the shopkeeper mobs are usually not falling far from their initial spawn position anyway).
  
**Impact on performance:**  
* Shopkeepers only runs the AI and the gravity for mobs that are in range of players (AI: 1 chunk around players, gravity: 4 chunks around players by default).
* You can experiment with the gravity chunk range by tuning the config setting 'gravity-chunk-range'. Setting it lower than your server's entity tracking range might however result in players being able to see mobs floating above the ground until they get closer.
* Internal: The active AI and gravity chunks are currently determined only once every 20 ticks, and falling conditions of mobs are checked only once every 10 ticks (with a random initial offset to distribute the falling checks over all ticks).
* Internal: The shopkeeper mobs get their internal 'onGround' flag set, so that the mobs are properly recognized by Spigot's entity activation range and not getting ticked when far away (see SPIGOT-3947).
  * However, during our simulated 'falling' the flag gets disabled and enabled again at the end of the fall in order to work around some visual glitch that might otherwise affect players near the entity tracking range (see MC-130725 and SPIGOT-3948).
* Please note: Since at least some AI stuff is now run or triggered by the Shopkeepers plugin, your timings reports will show that Shopkeepers is using quite a bit more of your server's ticking time now. Don't be irritated by that though: I tried to optimize this quite a bit, so hopefully if you compare the performance of your server overall before and after the update, it should in summary even be a small improvement, since lots of unneeded AI and movement logic is no longer getting run.

**Other related changes:**  
* Shopkeeper mobs get the 'collidable' flag set on MC 1.9 and above now. Unfortunately this alone will not prevent them from getting pushed around, however it might be beneficial regardless, due them being fully ignored by minecarts, boats and projectiles now.
* You can disable the new mob behavior with the setting 'use-legacy-mob-behavior' (default: false). AI and gravity will then be handled by Minecraft directly again. By the way, please use this setting and compare the overall performance of your server with and without the new mob behavior and let me know of your findings!
  * Side note: Spigot added a new setting 'tick-inactive-villagers' (default: true). If you have areas with lots of villagers, and you are using the old (legacy) mob behavior, consider disabling this setting. Otherwise, those villagers might cause lots of unneeded additional chunk loads due to their search for nearby villages.
* You are now able to fully disable gravity of shopkeeper mobs via the setting 'disable-gravity' (default: false).
  * This only works on MC 1.10 and later.
  * If you are using the legacy-mob-behavior: Some mob types will always get their AI disabled, and will therefore not be affected by gravity regardless of that setting.
  * With gravity enabled, mobs spawn 0.5 blocks above their spawning position (like before).
  * With gravity disabled, mobs will be spawned exactly at the position they are meant to be. Note however, that if your shopkeeper's spawn location is already above the ground (for example if they were placed on top of grass or other collision-less blocks), those shopkeepers will end up floating above the ground.

**Note on MC version differences:**  
* On MC 1.9 and MC 1.10 the NoAI tag does not disable Minecraft's gravity and collision handling. Mobs will therefore by default be pushable on those versions, even with the new mob behavior.
* However, if gravity gets disabled on MC 1.10 and above, we are also able to disable collisions/pushing of mobs (by using Minecraft's internal noclip flag of entities).

**Various improvements when running on a not yet supported version of Minecraft:**  
* Fixed: Freezing of mobs via a slowness potion effect didn't properly work. Instead, we now set the movement speed of mobs to zero via a custom attribute modifier.
* Fixed: The fallback handler supports silencing of entities now.
* Fixed: The fallback handler wasn't able to handle trading recipes with empty second item.
* Fixed: The fallback handler was updated to now support setting the NoAI flag of mobs.
* Note on the new mob behavior: The new AI and gravity handling is not supported when running on unsupported Minecraft versions: All entity AI will be prevented (due to the NoAI flag), but no AI replacement will be active, causing the mobs to not be affected by gravity and not look at nearby players.
  * As a workaround, you may enable the legacy mob behavior in this case then: Minecraft will handle AI and gravity again then. However, this might be unperformant due to lots of unneeded internal AI logic being active then.
  * When using the legacy mob behavior and also disabling gravity, the mobs will additionally not be affected by gravity, and they can no longer be pushed around by players.

**Other changes:**  
* Changed: The default help page header message now includes the plugin's version. If you are updating, you will have to manually update the message or let it regenerate.
* Added: A new message 'msg-name-has-not-changed' gets sent when a player attempts to change the name of a shopkeeper and the new name matches the old one.
* Fixed: If renaming via item is enabled and the new shopkeeper name matches the old one, no item is removed from the player.
* Added blaze and silverfish to the by default enabled mob types. They seem to work fine with NoAI.
* Minor reordering of the default shop types: 'Buying' is placed between 'normal' and 'trading' now.
* Added bStats metrics: This reports anonymous usage statistics to bstats.org.
  * In addition to the general server and plugin information, this also collects Shopkeepers-specific information about: Usage of supported third-party plugins, used Vault economy, usage of various features, total shopkeeper count and the number of worlds containing shopkeepers.
  * All collected information can be publicly viewed here: https://bstats.org/plugin/bukkit/Shopkeepers/
  * You can disable this globally for all plugins on your server by editing 'plugins/bStats/config.yml'. Or you can alternatively also disable this for Shopkeepers only via the setting 'enable-metrics'.
  * Consider keeping this enabled: Having this information available allows me to determine when it is safe to drop support for older Minecraft versions, and on which features I should focus development and optimization efforts.
* Documentation: The full changelog of the plugin can now also be found in the repository: https://github.com/Shopkeepers/Shopkeepers/blob/master/CHANGELOG.md
* Debugging: Improved the output of debugging command '/shopkeepers check': It prints information about loaded chunks, and it lists various AI and gravity related timing statistics now. With the arguments 'chunks' and 'active' you can let it print additional information. Some information, that may not fit into the player's chat, may only get printed if the command is run from the console.
* Debugging: Added world name to chunk load and unload debug messages.
* Fixed/Internal: The license file gets properly included inside the plugin jar now.
* Internal: Added a few plugins as soft dependencies (Vault, WorldGuard, Towny, Gringotts). They will now get reliably loaded before Shopkeepers.

**Major internal restructuring (affects the API) and version bump to 2.0:**  
* All functions related to the public API of the plugin have been moved into a separate package, and are mostly behind interfaces now. This will break every plugin currently depending on Shopkeepers, sorry! However, this should make it far easier to differentiate between the public API and the internal API and implementation. This is only the first step towards providing a more stable and therefore more useful public API in the future. The functionality of the API hasn't changed much yet, and things will still keep changing quite a lot, until I have refined all the currently existing functions.
* Various classes were split into abstract base classes (internal API) and interfaces (public API).
* Everything related to shop type and shop object type was moved into the corresponding packages.
* Renamed 'UIManager' to 'UIRegistry' and added a (read-only) interface for it to the API. UIHandler is no longer exposed in the API for now.
* Added interfaces for accessing the default shop types, shop object types and UI types.
* Various functionality was moved outside the plugin's main class into separate classes: ShopkeeperStorage, ShopkeeperRegistry.
* ShopCreationData construction is now hidden behind a factory method.
* Renamed a few primary classes: ShopkeepersPlugin is an interface now, ShopkeepersAPI provides static access to the methods of ShopkeepersPlugin, the actual main plugin class is called SKShopkeepersPlugin now.
* Some restructuring and cleanup related to the internal project structure and how the plugin gets built. The API part gets built separately and can be depended on separately now from other projects.

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

Previously, trading with player shopkeepers only allowed simple left-clicks of the result slot. We predicted the trading outcome (rather simple when only allowing left-clicking) and then let Minecraft handle the click like normal. Any other inventory actions (e.g. shift-clicks) were prevented, because so far, we weren't able to predict the outcome of those.  
I finally took the time to dive into Minecraft's and CraftBukkit's internals to figure out how it processes the various clicking types and resulting inventory actions, how those can trigger trades, and how Minecraft processes those trades.

Supporting more advanced inventory actions for trading requires at minimum being able to reliably predict the outcome of those inventory actions, and by that already requires re-implementing of large parts of the vanilla logic (which is one of the reasons why this wasn't done so far). The step to also apply those predicted inventory changes ourselves then isn't that much of an additional effort on top, but has a few advantages:  
For one, we can be quite sure that the predicted outcome of a trade actually matches what gets applied in the end. This should reduce the risk of inconsistencies between Minecraft's behaviour and our predictions resulting in bugs (also with future Minecraft updates in mind).  
Secondly, when relying on Minecraft's implementation we are only able to allow or cancel the inventory action as a whole. The shift-clicking inventory action however is able to trigger multiple successive trades (possibly even using different trading recipes). By implementing this ourselves, we are able to apply as many trades as possible in terms of available stock and chest capacity.

Implementing the trading ourselves has a few advantages, but also comes with caveats:

**Caveats:**  
* Increased risk for bugs which we need to fix ourselves. Increased maintenance and testing effort.
* Increased possibility for inconsistencies between vanilla inventory/trading behaviour and our own implementation.
* Minecraft performs some map initialization when certain map items are created by crafting or via trades: This seems to be responsible for updating the scaling (after crafting a larger map) and enabling tracking (this doesn't seem to be used anywhere inside Minecraft though). Since this is only the case if the map item has some special internal nbt data (which shouldn't be obtainable in vanilla Minecraft), we currently simply ignore this inside Shopkeepers.

**Neutral:**  
* The usage count of the used trading recipes doesn't get increased anymore. But since these are only temporarily existing and aren't used for anything so far anyway, this shouldn't be an issue.
* The player's item crafting statistic doesn't get increased anymore for items received via shopkeeper trades.
* The player's traded-with-villager statistic doesn't get increased anymore for shopkeeper trades.

I could manually replicate some of these, but since these are custom trades anyway I don't see the need for it right now. But if there is a justified interest in this, let me know, and I might add a config option for it.

**Advantages:**  
* Support for more advanced inventory actions when trading with shopkeepers:
  * Currently supported are: Shift-clicking, and moving traded items to a hotbar slot. (Fixes #437)
  * Not (yet) supported are:
    * Dropping the traded item: Exactly reproducing the vanilla behaviour for dropping the traded item is actually rather tricky / impossible with pure Bukkit API so far.
    * Trading by double-clicking an item in the player inventory: The behaviour of this inventory action is rather arbitrary in vanilla already (trades zero, one, or two times depending on the other items in the inventory), and it is currently affected by a bug (https://bugs.mojang.com/browse/MC-129515)
* Simpler API: A single ShopkeeperTradeEvent is called for each trade (might be multiple per click depending on the used inventory action, e.g. shift-clicking). The ShopkeeperTradeCompletedEvent was removed. (API breakage, sorry.)
* Fixed: The logging of purchases should now actually be accurate for shift-clicks. It logs each trade individually. (Fixes #360)
* Eventually this might allow for more options for customization of the trading behaviour in the future.

**Potential issue (that has existed before already):**  
* If Minecraft adds new inventory actions, those are initially not supported by shopkeepers: If those inventory actions involve clicking on the result slot, they will simply be prevented. If they however allow players to trigger trades by clicking on items inside the player's inventory (similar to the existing collect-to-cursor action by double-clicking), they might introduce bugs which players might be able to exploit.

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
* Internal: Removed 'forHire' boolean value from shopkeepers. Instead, a shopkeeper's hiring state is now solely determined by whether a non-empty hire cost item has been specified.
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
* Internal: Replaced Scanner with InputStreamReader and letting Bukkit load the save file contents using that reader.
* Internal: Fully wrapped loading procedure into try-catch-block to catch any types of unexpected issues there, and then disable the plugin. (This hopefully helps to figure out the issue behind #485)
* Minor improvement: Logging to console how many shops are about to get loaded.

## v1.84 Release (2018-01-09)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Improvements to saving and loading: Saving first writes to a temporary save file before replacing the old save file. This update improves on that by considering this temporary save file during saving and loading, if no actual save file is found. Previously, any existing temporary save file would simply have been ignored and removed during saving and loading, possibly deleting the last valid backup of shop data, if a previous saving attempt already deleted the save.yml file but wasn't then able to rename the temporary save file for some reason.
* Added: An error message being printed to online admins (throttled to at most 1 message every 4 minutes) when saving failed. So that these severe issues do hopefully get noticed and looked into quicker when they occur.
* Minor change: Warning instead of debug message in case a shopkeeper cannot be spawned after creation or loading.
* Improved: Catching any type of issue during shopkeeper loading, and logging them with severe instead of warning priority.
* Minor changes to multi-line warning log messages.
* Various improvements related to async tasks, saving, reloads, disabling:
* Improved: Catching a rare race-condition when trying to register a task with the Bukkit scheduler from within an async task while the plugin is currently getting disabled.
* Improved: Waiting (up to 10 seconds) for all currently running async tasks to finish, before continuing to disable.
* Improved: Using synchronization to make sure that only one thread at once attempts to write the save files. Previously, this might have caused issues if an async and a sync save were going on at the same time (due to the plugin being disabled).
* API / Internal: Added a method for requesting a delayed save. Refactored (replaced) the chunkLoadSave task to use this instead now.
* Improved: If saving fails, it will now attempt another save after a short delay.
* Improved: The saving debug output is now slightly more detailed.
* Fixed: Invalid saving debug timings were logged in case a sync save gets triggered (e.g. during plugin disable) while an async save is already going on.
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
* Improved: The findOldEntity routine was slightly improved to also find entities in neighboring (and loaded) chunks. (This is only useful for situations in which chunk-unload events are not properly called, or world-save events were not properly called and the server crashes. However, it only improves the handling of those cases, it doesn't fully solve them.)

## v1.83 Release (2017-10-10)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* Changed: Explicitly built with java 8 from now on.
* Fixed / Reverted: If the display name / lore is empty in the config, the checked item's display name / lore is ignored again. Before it was required to be empty as well.
* Fixed for MC 1.8.x: Custom pathfinder goals of shopkeepers being cleared instead of target goals.
* Fixed: Skipping duplicate trades for the same item when loading shop.
* Changed: Book shopkeepers now store their trades in a section called 'offers' (previous 'costs'), to be consistent with all the other shop types.
* Minor internal refactoring related to shop offers handling for all shop types.
* Fixed: Default config missing by-default enabled mob types for MC 1.12 (illusioner and parrot).
* Fixed: Ejecting shopkeeper mobs right after spawning. Some entities have a random chance to mount nearby entities when spawned, e.g. baby zombies on chickens.
* Minor internal refactor related to applying mob sub-types for various living entity shops.
* Added: Support for baby pigman shops. Previously, pigman shops would randomly spawn as baby variant. Now, this can be explicitly specified in the editor menu of the shop.
* Fixed / Re-Added: Ability to specify the required spawn egg type for the shop creation item in the config (default: villager). Can be set empty to accept any spawn egg type. Can be set to something invalid (e.g. white space ' ') to only accept the spawn egg without any entity type assigned. This only works for the latest versions of Bukkit 1.11 upwards! On MC 1.9 and 1.10 any spawn egg type will be accepted. On MC 1.8 the spawn egg type is specified via the data value.

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
* Major change: Using new Bukkit API for opening virtual merchant inventories and getting the used trading recipe. This is only available in the very latest version of Bukkit for MC 1.11, so **if you are already using Spigot 1.11, you need to update to the most recent version**.
* Changes/Fixes: All empty itemstacks related to trading recipes should now be replaced with null everywhere (similar to the pre-1.11 behavior). This also means that trading recipes with empty second item should no longer save this as an item of type AIR, but instead omit this entry all together.
* Changes: More consistent handling of empty itemstacks everywhere in the code.
* Minor code cleanup at various places.

## v1.79 Release (2016-11-18)
### Supported MC versions: 1.11, 1.10, 1.9, 1.8
**Update for MC 1.11:**  
* Minecraft represents the different variants of skeletons (stray, wither_skeleton) and zombies (zombie_villager) now in the form of different entity types. Shopkeepers using these variants get automatically converted to use the new mob types instead.  
  This however means that you cannot switch back to previous Minecraft versions without loosing those converted shopkeepers.  
  And you will have to give all your players, which were previously able to create zombie or skeleton shopkeepers the required permission nodes for stray, wither_skeleton and zombie_villager, otherwise they won't be able to access/edit their shops after this conversion.  
  Also, if you are running on a Minecraft version below MC 1.11, you can no longer cycle through the different zombie variants and skeleton variants and existing shopkeepers using those will fall back to normal zombies and skeletons.
* Added the new mobs from MC 1.11 (evoker, vex, vindicator). Llamas, sadly, act weirdly currently when clicked (same goes for all horse variants) and are therefore not included in the default list of enabled mob types. In case you are updating, you will have to let the plugin recreate this config entry (by removing the entry or the whole config) or manually add those new mobs in order to enable them.
* Added: Support for the green 'Nitwit' villager variant added in MC 1.11.
* Some minor changes related to handling empty inventory slots and itemstacks, especially when creating the trading recipes, were required.

**Other changes:**  
* Added a bunch of old mobs, which seem to mostly work fine, to the by default enabled living shop types. Also reordered this list a bit (roughly: friendly mobs first, hostile and new mobs at the end).
* Added: Instead of cycling between normal and villager variant, zombie shopkeepers can now change to baby variant by clicking the spawn egg in the editor window.
* Change: No longer disabling other, normal villagers by default.
* Fix: Ignoring double left-clicks in editor window when adjusting item amounts. Previously, this would cause two fast left-clicks to be counted as three left-clicks.
* Fix: Also ignoring double clicks when cycle shopkeeper object variants in editor window.
* Fixed: The 'compatibility mode' fallback, which is run if no supported Minecraft version is found, wasn't able to open the trading window for players.

**Known Caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older Bukkit versions is untested. If you encounter any problems, let me know, and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.
* In the latest MC 1.8.x versions default Minecraft trading logic has slightly changed (and by that those of shopkeepers as well): If a trade requires an item with special data (like a custom name, etc.) Minecraft is now only allowing this trade if the offered item is perfectly matching, including all special item data and attributes.
* MC 1.9 has changed how the different spawn eggs are differentiated internally. All spawn eggs now have the data value 0 and there is no API in Bukkit yet which would allow me to check which type of spawn egg a player is holding. In order to get the shop creation via item working for now, you will have to either change the data value of the shop creation item to 0 (which will however let ALL types of spawn eggs act like the creation item), or you change the creation item type all together.
* Shopkeepers using skeleton or zombie variants after updating to MC 1.11 or above can not be loaded again (will be lost) when switching back to a previous Minecraft version.
* If you are running on a Minecraft version below MC 1.11, you can no longer cycle through the different zombie variants and skeleton variants. Existing shopkeepers using those will fall back to normal zombies and skeletons.

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
* Changed: No longer removing monsters which are accidentally attacking a shopkeeper entity (e.g. wither explosions).
* Changed: When setting item amount with hotbar button, use the actual number key value instead of key value - 1.
* Some small internal refactoring at various places. Among others, related to handling of updating item amount when clicking items in the editor view.

## v1.77 Release (2016-07-14)
### Supported MC versions: 1.10, 1.9, 1.8
* Fix: If 'protect-chests' is disabled, it should no longer prevent breaking of shop chests. Opening of shop chests will still be prevented though.
* Added 'use-strict-item-comparison' setting back in (default: false). It seems that even with the trading changes in MC 1.8 there are still some cases in which Minecraft ignores certain item attributes.
* Reimplemented chest protection: It considers all shopkeepers now, even currently inactive ones. This might be of use in the edge case of player shopkeepers being in a different and somehow unloaded chunk than the corresponding chest. This should also fix the major issue that chest are temporary unprotected after world saves due to the temporarily deactivation of shops. And this might bring slight performance improvements if you have lots of shopkeepers in loaded chunks.
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
* Due to some villager profession changes in Bukkit, data about villager professions gets converted to a new storage format (stored by name instead of id) (not backwards compatible!).
* Using Bukkit's new API for setting entities silent on MC 1.10.
* Added POLAR_BEAR to default config.
* Added support for the new Stray skeleton type. It is represented by MHF_Ghast player head currently, which doesn't work perfectly, but is better than nothing for now.
* Minor changes to the compatibility mode, which didn't work for the past few Minecraft updates.
* Removed support for MC 1.7.10.
* Removed 'use-strict-item-comparison' setting (Minecraft is performing a strict item comparison already itself since MC 1.8).
* Removed uuid conversion related code: PlayerShops are expected to have a valid owner uuid by now.
* Slightly refactored the handling of issues during shop creation and loading, so that shopkeepers with invalid data (e.g. missing owner uuids) can now communicate to the loading procedure that the loading failed (which then prints a warning and continues). Shops which failed to load might get overwritten the next time shopkeepers saves its data.
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
* Fixed an issue related to the shopkeeper Citizens trait when either Shopkeepers or Citizens are not running: We now unregister the shopkeeper Citizens trait whenever Shopkeepers or Citizens get disabled. Also added an additional check to ignore shopkeeper Citizens trait attachments if Shopkeepers isn't currently running (just in case).
* Removed the restrictions of not being able to open shops while holding a villager egg in the hand by slightly changing the way the villager trading menu gets opened for past Minecraft versions.
* Removed the no longer used 'msg-cant-open-shop-with-spawn-egg' message.
* Fix: The remote command should now work again.
* Added: Preventing shopkeepers from entering vehicles.
* Improved filtering of faulty trading offers in the saved data.
* Fixed: The shop creation item, the hire item and the currency item get now automatically set to their defaults if they are invalidly set to AIR in the config. The (high) zero currency items should now even work in the player shop editors if being of type AIR.

## v1.71 Release (2016-04-07)
### Supported MC versions: 1.9, 1.8, 1.7.10
**This update contains a few changes, which might require you to update/reset your config and language file.**  
* In MC 1.9 all spawn-eggs use the data value 0 now. The default shop creation item data value was changed to 0 for now. This will cause all spawn-eggs to function as shop creation item by default. There is no new option yet to limit the shop creation item to the villager egg.
* Replaced 'disable-living-shops' with 'enabled-living-shops'. You might have to update your config due to this, however it has the advantage that whenever new mob types, which are disabled by default, are added to shopkeepers in the future, they don't automatically get enabled on servers which already run shopkeepers (which already have an old config which doesn't list those new mob types as disabled yet).
* Did some internal refactoring, mostly related to living shop types: Shopkeepers is now automatically 'supporting' all living entity types available for your server version for being used as shop entities. However, not all entity types will properly work: If you want to try out on your own risk if a living entity type works as shop entity, you will have to manually enable it in the config.
* Added RABBIT to the by default enabled living shop entity types.
* Slightly changed the default shopkeeper list entry message, to display the shopkeeper's session id instead of the list index. Also, the colors where slightly changed to make it better readable.
* Added some german translations for some command argument names to the included default german translation.
* Added setTradePerm command: This allows setting of a custom permission for individual admin shopkeepers, which is then required for being allowed to trade with the affected shopkeeper. Also added a few new messages related to this.
* Also added a new message which is sent to players that don't have the normal trade-permission. By default, the message text is the same as the message being sent if the player is missing the newly added custom trading permission, though those can be changed in the config independently.
* The 'remote' command now also accepts a shop's unique id or session id (instead of the shop name).
* The default 'unknown-shopkeeper'-message was changed to include the 'unknown id' fact.
* No longer manually saving item attributes for servers running on MC 1.9 upwards. Since somewhere in late 1.8 Bukkit started saving attribute data as part of their 'internal' data. Attribute loading is left in for now, in case someone updates from a Bukkit version which didn't save attributes yet. This should also fix the issue of the new Slot attribute added in MC 1.9 being now properly saved.
* Slightly changed the saving procedure which will hopefully make it less likely that all your save data gets lost if something goes wrong during saving: The data gets now first saved to a different, temporary file (in the Shopkeepers plugin folder). Only if saving was successful, the old save file gets removed and replaced by the new one. Also added a bit more debugging information to the related messages.
* Added a possible workaround for the 'duplicating villagers' issue some people still seem to have sometimes:
* My current guess on the problem is, that: The shopkeeper entities get saved to disk when the world is saved. Shopkeepers removes a shop entity, respawns the entity (this replaces the old 'last-entity-uuid'). This does not necessarily have to happen at the same time or for all entities, but it does for example occur when the Shopkeepers plugin is getting reloaded. Now, if the server closes (e.g. due to a crash) without another world save taking place, the old shop entity is still saved in the chunk data of the world, but we cannot identify it as shop entity on the next server start, because we remembered the entity-uuid of the last entity we spawned, which is not the same as the last entity which was saved to disk.
* This workaround: Whenever the world gets saved (at least whenever WorldSaveEvent gets triggered) all shopkeepers get temporarily unloaded (and by that removed from the world) and loaded (respawned) again shortly after the world-save. With this the shop entities will hopefully no longer get stored to the chunk data. You will notice a short flicker of shop entities, but trading etc. should keep working, even during world-saves. If you have debug-mode enabled in the config, you will get a rather large block of debugging output everytime all your worlds get saved.

## v1.70 Release (2016-03-08)
### Supported MC versions: 1.9, 1.8, 1.7.10
* Ignoring off-hand interaction events at various places. The shop creation item will only act as such if it is held in the main hand. In the off-hand it will act like the normal item (and regular usage is prevented like normal via the 'prevent-regular-usage' setting).

## v1.69 Release (2016-03-01)
### Supported MC versions: 1.9, 1.8, 1.7.10
* Updated for MC 1.9.
* Removed support for nearly all versions of MC 1.7. Only v1.7.10 is still supported.
* Added setting 'skip-custom-head-saving' (default: true), which allows turning of the skipping of custom player head items (player heads with custom texture property and no valid owner) during saving of shopkeeper trades. Note: Best you keep this enabled for now, because disabling this on a server which does not properly support saving of custom player head items, can cause all your saved shopkeepers to get wiped. On newer versions of Spigot, the wiping of saved shopkeepers might be fixed, however custom player head items are still not properly supported by Spigot, so they will basically get saved like if they were normal player head items.
* Fixed: No longer considering normal player heads without any owner as 'custom' player heads
* Added: Shopkeepers now get assigned a 'session id', which is much shorter than the unique id, but is only valid until the next server restart or the next time the shopkeepers get reloaded from the save file. Those ids are currently unused, but might get used in the future to let players (admins) specifiy a shopkeeper in commands via these shorter ids.

**Notice about an issue with MC 1.9:**  
MC 1.9 has changed how the different spawn eggs are differentiated internally. All spawn eggs now have the data value 0 and there is no API in Bukkit yet which would allow me to check which type of spawn egg a player is holding.  
In order to get the shop creation via item working for now, you will have to either change the data value of the shop creation item to 0 (which will however let ALL types of spawn eggs act like the creation item), or you change the creation item type all together.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older Bukkit versions is untested. If you encounter any problems, let me know, and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.
* In the latest MC 1.8.x versions default Minecraft trading logic has slightly changed (and by that those of shopkeepers as well): if a trade requires an item with special data (like a custom name, etc.) Minecraft is now only allowing this trade, if the offered item is perfectly matching, including all special item data and attributes.
* MC 1.9 has changed how the different spawn eggs are differentiated internally. All spawn eggs now have the data value 0 and there is no API in Bukkit yet which would allow me to check which type of spawn egg a player is holding. In order to get the shop creation via item working for now, you will have to either change the data value of the shop creation item to 0 (which will however let ALL types of spawn eggs act like the creation item), or you change the creation item type all together.

## v1.68 Release (2015-12-21)
### Supported MC versions: 1.8, 1.7
* Small change to the logging format: It now includes the shopkeeper id and the amounts of currency 1 and currency 2.
* Small improvement: We now spawn Citizens NPCs in the center of the block, similar to the other living entity shops.

## v1.67 Release (2015-11-28)
### Supported MC versions: 1.8, 1.7
* Fix: No longer consider normal mob heads to be 'custom' heads. So they should no longer get excluded from saving due to the changes of the last version.

## v1.66 Release (2015-11-25)
### Supported MC versions: 1.8, 1.7
* Fix: Added a temporary check to skip saving of trades which involve a custom head item, which would otherwise currently cause a wipe of the complete save file due to a bug in Bukkit's/Spigot's item serialization.

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
* Now really fixed: Citizens NPCs with the shopkeeper trait should now keep working after a reload via '/citizens reload'.
* Improvement: We now react to Citizens getting enabled or disabled dynamically (in case this is possible, via plugin managers or similar).
* Tiny internal changes.

## v1.62 Release (2015-07-17)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Improvement: If an error is catched during loading of shopkeeper data, the plugin gets disabled without triggering a save. This hopefully prevents that the complete shopkeeper data gets lost if for example some item cannot be properly loaded.
* Change: We also disable the plugin without a save now if a shopkeeper cannot be loaded (for example because of invalid save data). Previously, that shopkeeper would have been skipped, resulting in that shopkeeper being completely removed. Now, you have to manually fix your save data in this case, but with the advantage of being able to fix the affected shopkeeper without losing for example all the set up trades.
* ~~Fixed: Citizens NPCs with the shopkeeper trait should now keep working after a reload via '/citizens reload'.~~ (Not actually fixed, check out the next version.)
* Added: Automatic removal of invalid Citizens shopkeepers, which either have no backing Citizens NPC, or are using the same NPC as some other shopkeeper for some reason.
* A few minor other internal improvements related to Citizens related code (like checking if Citizens is currently still enabled before accessing or creating NPCs).
* Tiny improvement: Not triggering a save during removal of inactive player shopkeepers, if no shopkeepers got actually removed.
* Improvement: Hopefully better handling shopkeepers with somehow invalid or duplicate object id during activation and deactivation (preventing that the wrong shopkeeper gets disabled for example), and printing some warnings if a shopkeeper is detected during activation with a somehow invalid object id. This mostly has debugging purposes currently.
* A few tiny other internal changes, which hopefully didn't break anything.

## v1.61 Release (2015-06-24)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Slight changes to handling saving: If there is a problem during the saving, we wait a bit and then retry it several times. This might help if for some reason the save file can only temporary not be written to (for example because of some other application, like Dropbox, currently locking the file). We also checks for other possible causes of issues now, like whether the parent directory is missing (if it is, we try to create it), and whether we can currently write to the save file. These checks are done before we remove the old save file. These changes hopefully prevent that certain issues cause the save data to go completely lost. We also print more information to the log when we detect some issue.
* Fixed: A bug in Minecraft's trading allowed players to exploit certain trade setups (only very few trade setups are affected though). Shopkeepers is now detecting and preventing those affected trades.
* Added (API): The ShopkeeperTradeEvent now holds information about the used trading recipe, and the ShopkeeperTradeCompletedEvent holds a reference to the original ShopkeeperTradeEvent.
* Added (API): A bunch of javadoc was added or updated for the available events.

## v1.60 Release (2015-06-12)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Allways allow editing of own shopkeepers, even if a player no longer has the permission to create shopkeepers of that type.
* Fixed: Skipping empty recipes for admin shopkeepers during loading.
* Changed: If a player cannot create any shopkeeper at all because of missing permissions, the no-permission message is printed instead of the creation-failed message. We also skip the chest selection, and the cycling of the shop object type, and print no message when the player selects the shop creation item in the hotbar.
* Change/Possible fix: Triggering a save if the teleporter task had to respawn a shopkeeper object (like an entity). This should make sure that the latest entity uuid gets saved.
* Change: No need to update a sign shop another time if it had to be respawned.
* Internal change: Moved a few more permission nodes into ShopkeepersAPI.
* Added: API method for checking if a player can create any shopkeeper at all.

## v1.59 Release (2015-06-08)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Updated to the latest version of Citizens.

## v1.58 Release (2015-06-06)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Some internal cleanup, moving around of things and other minor changes. Hopefully all is still working properly.
* Possible fixed: Some minor cleanup did not take place if a shopkeeper entity was respawned, or when activating a shopkeeper on chunk load.
* Changed/Fixed: The permission node for the help command was added to the plugin.yml and now defaults to 'true' instead of 'false'.
* Changed: The default of the `shopkeeper.remote` permission node was changed from `false´ to `op`.
* Added: `shopkeeper.*` permission node, which includes all other permission nodes.
* Added: Debug information whenever a permission node is checked for a player. Might be a bit spammy though.
* Changed: Handling of world unloads was slightly changed to reduce the amount of duplicate code. It now triggers the chunk unload code for all currently loaded chunks. Hopyfully without causing issues.
* Changed: We now check the hire item name and lore earlier already. If they don't match, the player is informed that the villager can be hired (just like when clicking a villager with any other item), instead of telling them that hiring failed (like when the player does not have the needed amount of hiring items).
* Fixed: The code which checks for chest access when creating a player shopkeeper via command does no longer trigger chest selection if the player is holding a shop creation item in hand.
* Fixed: Previously, sign shops weren't properly created if created via command. The initial sign wasn't placed and the shopkeeper was stored at the wrong location.
* Changed: Shop creation via command will fail now if a block face is targeted at which either a non-air block is, or if no wall sign can be placed there.
* Added/Fixed: Canceling block physics for shop signs, so they should no longer break in various situations. Signs might end up floating in mid-air though.
* Added/Fixed: Preventing entity explosions from destroying shop signs.
* Added/Fixed: Preventing entity explosions from destroying shop chests.
* Added/Fixed: In case we detect that a shop sign went missing, we attempt to respawn it. If the sign respawning fails for some reason, we completely remove the shopkeeper and print a warning to console. Previously, the sign shopkeepers would no longer be usable nor accessible from inside Minecraft, but still exist in the save data.
* Added: Sign shops should now store their initial sign facing, which is used for sign-respawning. For already existing sign shops it is attempted to get the facing from the current sign in the world.
* Fixed: Shop signs are now updated at least once when their chunk gets loaded, so that config changes which affect the sign content are applied to the already existing signs.
* Fixed: We also request a sign update whenever the owner or owner name changes, so that the owner name on the sign gets updated.
* Added: Whenever a player switches to the shop creation item in the hotbar, a message explaining the usage of it is now printed.

## v1.57 Alpha (2015-05-25)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Internal change: Removed our own trading-recipe-finding code and instead we now get the recipe used in the current trade from Minecraft directly. With this change, it should be guaranteed that we really get the same recipe that Minecraft is using. We also have less work for keeping this code updated.
* Internal change: The code which adds and removes an item stack to/from a chest was changed. Adding an item stack should now try to first fill up existing item stacks before starting to create new ones.
* Fixed: The trading and buying player shopkeepers are now adding those items to the chest, which were actually used by the trading player. Depending on item comparison of Minecraft and shopkeeper settings, those don't have to perfectly match the required items from the used recipe, but are still accepted.
* Fixed: The owner name for player shopkeepers was initially (on creation) getting stored in lower case for some reason in the past. This probably caused issues at some places when we were comparing the owner name to the name of a certain player, for example when using the remove command.
* Tiny change/fix (not sure if this was actually an issue): Skipping NPC players now when doing some initialization with the online player when the plugin gets enabled.

See also the changelog of the previous version!

## v1.56 Alpha (2015-05-23)
### Supported MC versions: 1.8, 1.7, 1.6.2
This version is marked as ALPHA, because it is including a bunch of internal changes which weren't properly tested yet and which might especially affect trading and saving of shopkeeper data. This version is currently only meant for testing purposes and finding potential issues.

Please create a backup of your shopkeeper save.yml file before using this version.  
You might especially want to check if all shopkeeper trades get properly loaded and saved, and if the setup of shopkeepers, trading and item removal and disposal in/from player-shop-chests is working properly.  
If you discover any issues please let me know by creating tickets for those.

* Removed custom item comparison, which was used in the past to only allow trades if the required and the offered items perfectly match, excluding internal attribute uuids. This is now useless, because of changes in Minecraft's 1.8.x trading logic.
* Unfortunately detailed debug messages, telling exactly why certain items are not similar, are no longer possible with this change.
* Updated to Minecraft's new 1.8.x trading logic:  
  Previously, Minecraft did unlock a trade if the type of the offered items matches the required item. Now, in MC 1.8.x, if a trade requires an item with custom data, such as custom name, lore or attributes, Minecraft only unlocks the trade if the offered item perfectly matches the required one (all the item's internal data and attributes have to be the same).  
    If the required item has no special data, then any item of the same type, even items with special data, can be used for this trade.  
    Also, Minecraft sometimes unlocks a non-selected trade, if the required items of a different trade is matching the ones which are currently inserted.

Shopkeepers is now trying to allow most of those trades as well. So if a trade requires an item without special data, every item of that type is accepted. If an item with special data is required, only perfectly matching items are accepted.

* Added setting `use-strict-item-comparison` (default: `true`). When this setting is active, the trading behavior is nearly the same as before: An additional and stricter item comparison is performed before a trade is allowed, which ensures that the offered items perfectly match those of the used trading recipe. So even if the required item has no special data, only items of the same type that also don't have any special data are accepted. Otherwise, the trade is blocked.  
  This setting's default is currently set to true, to prevent potential issues with already set up shops, which expect the old/current trading behavior. Also, servers running on MC versions below 1.8.x might depend on this setting to be active.  
  For servers that use the latest server versions, I recommend disabling this setting, so that the trading logic better matches the one of Minecraft.
* Changed: Resetting the player's selected chest after successfully creating a player shopkeeper. This seems to be more intuitive.
* Fixed: The default shop-created message for the buying shopkeeper did say that one of each item which is being 'sold' has to be entered into the chest. Instead, it was meant to say 'one of each item being bought'.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older Bukkit versions is untested. If you encounter any problems, let me know, and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.
* In the latest MC 1.8.x versions default Minecraft trading logic has slightly changed (and by that those of shopkeepers as well): If a trade requires an item with special data (like a custom name, etc.), Minecraft is now only allowing this trade if the offered item is perfectly matching, including all special item data and attributes.

## v1.55 Release (2015-05-18)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Actually include the update for 1.8.4.

## v1.54 Release (2015-05-18)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Color codes should now actually get converted.

## v1.53 Release (2015-05-17)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: Code which requires searching for nearby entities wasn't properly searching in negative z direction previously.
* Updated to the latest Spigot build: now comparing the stored block state of items (untested)
* Fixed: Missed applying the creation item lore from the config to the creation item.
* Fixed: Missed applying color code conversion to the shop creation item name and lore.
* Internal change: Color code conversion of text is now done once when the config is loaded, instead of everytime the colored text is needed. If you encounter any issues with color codes not being properly converted at certain places, let me know.
* Updated to support Spigot's 1.8.4 build.

## v1.52 Release (2015-03-15)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny cosmetic fix: Print 'yes' instead of 'null' for the checkItems debug command, if the compared items are similar.
* Fixed: Banner meta comparison now compares base colors as well.
* Fixed: Banner meta comparison should now actually work. Before it reported that the patterns of two banners are similar, when they weren't, and that they are different, when they were actually similar.

## v1.51 Release (2015-03-13)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Improved/Fixed: sometimes villagers were still able to turn into witches by nearby lightning strikes. The radius in which lightning strikes around villager shopkeepers get prevented was increased from 4 to 7 blocks.
* Fixed (workaround): Occasionally, an error did pop up for some users during thunderstorms due to entities reporting to be in a different world than we are expecting them to be.
* Fixed: Player shops should now identify shop owners solely by name again if running on versions which don't properly support player uuids yet (below 1.7.10, I think). Untested though.
* Changed: We no longer remove all already stored owner uuids when switching from a Bukkit version which supported player uuids to an older versions which doesn't. The owner uuids are not used on that older Bukkit version, but we still keep them for the case that we switch back to a newer version of Bukkit.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older Bukkit versions is untested. If you encounter any problems, let me know, and I will look into it.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.

## v1.50 Release (2015-03-07)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Added support for CraftBukkit/Spigot 1.8.3.
* Experimental: Unlocked a bunch of remaining living entity types for use as shopkeeper objects: bat, blaze, cave_spider, spider, ender_dragon, wither, enderman, ghast, giant, horse, magma_cube, slime, pig_zombie, silverfish, squid (no MC 1.8 entities yet) Those are highly experimental! They are disabled by default in the configuration: you might need to remove the 'disabled-living-shops' section in your config in order to let it regenerate with the new default values.  
Some of those entities (currently: bat, ender_dragon, wither, enderman, silverfish) get the new NoAI entity tag set in order to prevent the biggest issues with them: As this new tag is only available on MC 1.8, you should only enable those mobs if you are running MC 1.8.x.

Known issues with those new entity types:  
* Chicken: Still lays eggs.
* Bat: Without the NoAI tag it flies around. If NoAI tag is available (MC 1.8) the bat is sleeping, but starts the flying animation if being hit by something.
* Blaze: Without the NoAI tag it randomly starts flying upwards.
* Enderdragon: Without NoAI tag, it flies around, towards players, and pushes entities around. Shows boss bar.
* Wither: Without NoAI tag, it makes noise. Shows boss bar.
* Enderman: Teleports away / goes invisible when being hit by a projectile. Starts starring animation.
* Silverfish: Without NoAI tag it is rotating wierdly depending on where the player is standing (possibly because it tries to rotate towards the player). Is constantly showing the movement animation.
There might be more issues. Use on your own risk.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older Bukkit versions is untested. If you encounter any problems, let me know, and I will look into it.
* When switching from a Bukkit version which support mojang uuids to a Bukkit version which does not support mojang uuids yet all stored owner uuids of player shops are lost.
* A bunch of entity types are only meant for experimental usage. They might cause all kinds of issues if used. See changelog of v1.50.

## v1.49 Release (2015-03-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny internal change: The shopkeepers are kept ordered in the internal storage. This might fix potential issues of the newly added list command, which requires that the internal storage order does not change while the player is selecting another page for displaying.
* Fixed/Added: Entity shopkeepers are now unaffected by splash potion effects.

## v1.48 Release (2015-03-04)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Changed: We are now less strict with the argument count of commands: If the first argument matches an existing command, we handle this command, even if the argument count doesn't match the required amount of arguments. Previously, it attempted to spawn a shopkeeper via command in some situations if it didn't find a command that exactly matched.
* Changed: Ignoring the player's naming chat event, if the affected shopkeeper is no longer existing.
* Changed (API): Deprecated getting all shopkeepers by chunks.
* Changed (API): All API methods now return unmodifiable collections.
* Changed (API): The permission nodes were moved into ShopkeepersAPI.
* Added command: "/shopkeeper list [player|admin] [page]" - Lists the players own shops (if no player/'admin' is specified), the shops of a specified player or all admin shops. The shops are divided into pages.
* Added permission node: `shopkeeper.list.own` (default: `true`) - Allows listing the own shops via command.
* Added permission node: `shopkeeper.list.others` (default: `op`) - Allows listing the shops of other players via command.
* Added permission node: `shopkeeper.list.admin` (default: `op`) - Allows listing all admin shops via command.
* Added command: "/shopkeeper remove [player|all|admin]" - Removes the players own shops (if no player/'admin' is specified), the shops of a specified player, all player shops, or all admin shops. The command needs to be confirmed by the player via '/shopkeeper confirm'
* Added permission node: `shopkeeper.remove.own` (default: `op`) - Allows removing the own shops via command.
* Added permission node: `shopkeeper.remove.others` (default: `op`) - Allows removing the shops of other players via command.
* Added permission node: `shopkeeper.remove.all` (default: `op`) - Allows removing the shops of all players at once via command.
* Added permission node: `shopkeeper.remove.admin` (default: `op`) - Allows removing all admin shops via command.

Create a backup of your save.yml file and make sure that the permissions are set up correctly and actually work correctly before using.

## v1.47 Release (2015-03-01)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Update to the latest Spigot version: Item flags should now get compared as well.
* Fixed (workaround): In order to prevent villager shopkeeper to turn into witches, we prevent all lightning strikes in a 5 block radius around villager shopkeepers.
* Added (API): Method for getting a shopkeeper by its uuid.
* Fixed (API): Whenever Shopkeeper#setLocation is called, the shopkeeper's location is now updated in the chunk map as well.

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
* Fixed: Left and right-clicking the delete button at the same time no longer gives two shop creation items (eggs) back.

## v1.43 Release (2015-01-10)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Fixed: If the cost for an item of a normal player shopkeeper are between high-currency-min and high-currency-value the first slot of the recipe remained empty (and by that the trade became unavailable). We now move the low cost into the first slot in this case.
* Slightly improved handling of invalid costs (this can happen if somebody manually modifies the save file) for trading player shopkeepers: If the first item is empty for some reason, we now insert the second item into the first slot to make the trade still usable.

## v1.42 Release (2015-01-07)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Added: each shopkeeper now additionally stores a unique id which should not change over the shopkeeper's lifetime.
* Attempt to fix potential issue with protection plugins, which cancle the PlayerInteractEvent because they want to deny usage of the shop creation item, not because denying access to the clicked chest: We now call another PlayerInteractEvent with the player not holding any items in hand, if that events gets cancelled as well we can be sure that the player has no access to the clicked chest.

## v1.41 Release (2015-01-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* No longer storing owner uuids of player shops if running on a Bukkit version which does not support mojang uuids yet. This helps if you have already stored those 'invalid'/non-mojang player uuids by running v1.40, however it has the disadvantage that you lose all stored owner uuids if you switch back to an older Bukkit version after you have already run a newer Bukkit version which supports mojang uuids!
* Ignoring the owner uuid now when finding inactive shops if not running on a Bukkit versions which supports mojang uuids yet.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.
* Compatibility with older Bukkit versions is untested.
* When switching from a Bukkit version which support mojang uuids to a Bukkit version which does not support mojang uuids yet all stored owner uuids of player shops are lost.

## v1.40 Release (2015-01-05)
### Supported MC versions: 1.8, 1.7, 1.6.2
* Tiny change to the (hidden) debugging command checkitem: The currently selected item now gets compared to the item in the next hotbar slot, instead of the item on the cursor (which made no sense, because the player never has an item on the cursor when executing a command).
* Attempt of making shopkeepers backwards compatible again down to Bukkit 1.6.2 R1.0. Completely untested though.

## v1.39 Release (2014-12-30)
### Supported MC versions: 1.8.1, 1.8
* Fixed a duplication bug when players shift-click very fast (at least 2 times in the same tick) an item in a shopkeeper inventory which gets closed one tick later (e.g. hire inventory).
* Added ShopkeeperTradeEvent (meant for canceling trades because of additional conditions) and ShopkeeperTradeCompletedEvent (meant for monitoring the actual outcome of a trade).

## v1.38 Release (2014-12-29)
### Supported MC versions: 1.8.1, 1.7.4, 1.7.2
* Fixed (untested though): concurrent modification exception when removing player shopkeepers due to inactivity.

## v1.37 Release (2014-12-17)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Fixed error which was introduced in v1.36: The error about 'plugin registering a task during disable' should be gone now. During disable the plugin now triggers a save which runs synchronous instead again.

## v1.36 Release (2014-12-17)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Changed: the shopkeeper data now gets saved to file in a separate thread, hopefully reducing load on the main thread if your save file is very large.

## v1.35 Release (2014-12-17)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Readded: removing inactive player shops every time the plugin gets enabled. Untested though.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Server crashes and improper shutdowns might cause living non-Citizens shopkeeper entities to duplicate sometimes.
* The 'always-show-nameplates' setting is no longer working on MC 1.8.

## v1.34 Release (2014-12-16)
### Supported MC versions: 1.8.1, 1.8, 1.7.9
* Fixed: hiring of shopkeepers should now work again.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
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
* Added optional name and lore settings for the hire item. Those will be ignored if they are kept empty.
* The settings 'zero-item' and 'high-zero-item' were renamed to 'zero-currency-item' and 'high-zero-currency-item'. You will have to update your config if you made changes to those settings in the past!
* Added optional item name and lore settings for currency and high currency item (if not set (by default) those will be ignored).
* Added optional item data, name and lore for zero currency and high zero currency items as well (if not set (by default) those will be ignored).
* Removed 5th villager profession (green) from editor menu in case the plugin is running on MC 1.8. Mc 1.8 doesn't seem to have the green version anymore.
* Fixed: the shopkeeper command should now work again with the optional shop object type parameter. [Ticket-269]
* Added /shopkeepers help command and help pages which displays the available commands. Commands for which a player doesn't have the needed permission get filtered out.
* Added a few more messages to the command output for the cases that a command fails (for example due to missing permissions or invalid command parameters). And we also stop the command execution now instead of continuing with default command parameters.
* Added `shopkeepers.debug` permission node (default: `op`). Previously, we manually checked if the command executor is op before giving access to the debug command.

## v1.30 Release (2014-12-02)
### Supported MC versions: 1.8, 1.7.9
* Added the setting 'silence-living-shop-entities' which lets you define whether living shopkeeper entities create sounds or not (default: true/silenced). This feature only work on MC 1.8.

Please also see the changelog of the previous versions!

## v1.29 Beta (2014-11-29)
### Supported MC versions: 1.8, 1.7.9
* Updated for CraftBukkit/Spigot 1.8

Please also see the changelog of the previous versions!

## v1.28 Alpha (2014-11-21)
### Supported MC versions: 1.7.9
* Reordered the shop types so the normal player shop is by default selected instead of the book shop.
* The selection of shop type and shop object (mobs, sign) are no longer reset after shop creation, but are kept stored until the player quits or the plugin is reloaded.
* Added a debug message to inventory clicks.
* Fixed: The previous version did not proplery block shift-click buying on player shops anymore. This should now be fixed again.

Please also see the changelog of the previous versions!

## v1.27 Alpha (2014-11-21)
### Supported MC versions: 1.7.9
* Fixed: The first (top-left) slot in a player's inventory was not properly working in editor mode of player shopkeepers.
* Fixed: The custom item comparison in the last version did skip item meta comparison.
* Improved the debug output when trading fails: it now displays the reason why the invovled items are not considered similar.
* Fixed: When the trading slot was clicked and the cursor would not be able to hold the result of the trade (because the cursor already hold an item of different type or the result would exceed the max stack size) Minecraft cancles the trade. Shopkeepers however wasn't aware of this and removed and added the invovled items to and from a player's shop chest regardlessly. We now skip our trade-handling as well in this situation: You cannot trade if the cursor cannot hold the resulting items.

Please also see the changelog of the previous versions!

## v1.26 Alpha (2014-11-20)
### Supported MC versions: 1.7.9
* Changed how item comparison is done: We no longer use Bukkit's built-in comparison method. This has the disadvantage that we have to write and update the comparison of all item related data ourselves and probably don't work with modded items that well anymore (I might add a setting in the future which toggles between the new and the old comparison if there is the need for it). However doing the comparison ourselves gives us more control over what aspects of items actually define them as being 'similar': this change allows us to compare item attributes and skull data ourselves, hopefully resolving the issues we had with this in the past.

Please also see the changelog of the previous versions!

This is an alpha version. Things might not properly work yet.  
**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now.

I suggest you to backup your old save file, just in case.

## v1.25 Alpha (2014-08-14)
### Supported MC versions: 1.7.9
* Fixed: Citizens falsely being detected as disabled.

Please also see the changelog of the previous versions!

## v1.24 Alpha (2014-08-04)
### Supported MC versions: 1.7.9
* Fixed: Some chest checks not working for the transfer and setForHire commands.
* Fixed: It should no longer be possible to create a player shopkeeper with a chest which is already in use by some other shopkeeper.
* Added setting 'prevent trading while owner is online' (default false): With this setting enabled, player shopkeepers don't trade (nor open the trading window) while the owning player is online. This might be useful for role-playing servers, which wish to force players to trade with each other directly while being online.

Please also see the changelog of the previous versions!

## v1.23 Alpha (2014-07-27)
### Supported MC versions: 1.7.9
* Fixed: Missing save when editor window is closed.

Please also see the changelog of the previous versions!

## v1.22 Alpha (2014-07-26)
### Supported MC versions: 1.7.9
* Fixed: Trading with player shopkeepers not working.
* Fixed: Shopkeeper names not being colored in the trade menu or on signs.

Also please see the changelog of the previous versions!

## v1.21 Alpha (2014-07-12)
### Supported MC versions: 1.7.9
* Fixed: Normal players not being able to trade with admin shops.
* Added: Permission `shopkeeper.trade` (default `true`), which determines whether a player can trade with shopkeepers.
* Added: Sheep shopkeepers can now change their colors via the editor menu.

Please also see the changelog of the previous version!

## v1.20 Alpha (2014-07-10)
### Supported MC versions: 1.7.9
This is an alpha version:  
While this version seems to work quite well already, there might be some new bugs being introduced due to some internal changes.
Also, there are some issues that still need to be resolved (hopefully no breaking issues though) and some new features to test out.

* Updated to CB 1.7.10. Not much tested yet.
* Internal rewrites. (Though I am not completly statisfied with the current result yet)
* Added: Shopkeeper entities are now tagged with metadata 'shopkeeper' which can be used to identify shopkeeper entities in other plugins.
* Added new setting 'bypassShopInteractionBlocking': can be turned on if you experience issues with other plugins bocking users from opening the shopkeeper interfaces (for example due to protected regions).
* Renamed setting from 'deletingPlayerShopReturnsEgg' to 'deletingPlayerShopReturnsCreationItem'
* Added support for more living entity shopkeeper types: chicken, cow, iron_golem, mushroom_cow, ocelot, pig, sheep, skeleton, snowman, wolf, zombie. Other entity types weren't included yet due to issues regarding replacement of their default behavior. The newly added entitiy types seem to work fine. However, note that chicken shopkeepers still lay eggs.
* Changed the permission nodes for the living entity shopkeepers to: `shopkeeper.entity.<lowercase_mobname>` and `shopkeeper.entity.*` for all.
* Old permission nodes for creepers, villagers and witches should still work, but might be removed in a future update.
* Trapped Chests can now be used as shop chests as well.
* Added: Experimental support for Citizens based shopkeeper entities (Thanks to elMakers for that): You can either create a player NPC shopkeeper via the creation item, which will default to the owner's name/skin, or an admin NPC shopkeeper via the command '/shopkeeper npc'. Also, there was a special ShopkeeperTrait added which can be assigned to a NPC to assign shopkeeper behavior to it. However, it is currently recommended to create the Citizens NPCs through the shopkeeper command instead. The permission node for creation is `shopkeeper.citizen`.
* Note that there are very likely still some issues left, mainly regarding cleanup of Citizens NPCs or shopkeeper data, especially in situations where either Citizens or Shopkeepers is not running.
* Added a setting to disallow renaming of Citizens player shopkeepers.
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now due to differences between the different currently supported Bukkit versions. This will probably be added at a later point again.

This is an alpha version. Things might not properly work yet.  
**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* Items with attributes might not properly work.
* The 'remove shopkeeper after certain time of inactivity of player' feature has been removed for now.

I suggest you to backup your old save file, just in case

## v1.18 Release (2014-04-14)
### Supported MC versions: 1.7.9, 1.7.4, 1.7.2
* Updated to CB 1.7.8
* Fixed a serious item duping bug [Ticket 228]: double clicking in the bottom inventory while having the trading gui opened gets blocked now
* We now update the player's inventory shortly after a trade failed to reduce the chance for fake items temporarily popping up in the player's inventory.
* If debug mode is enabled, we now print a slightly more detailed debug message for failing trades.
* Removed unused stuff from the compatibility mode, which probably was the cause that this time the compatibility mode didn't work when you tried to use the old Shopkeepers version on 1.7.8 servers.
* Updated to use player UUIDs. Some notes on that:
  * Make sure that you run one of the newest MC/Bukkit versions, because old versions might not support player account UUIDs correctly. I suggest that you use at least one of the 1.7.+ versions.
  * Make sure that your server runs in the correct online mode: UUIDs of players in online and offline mode don't match. So if you switch your server's online mode, the stored player UUIDs won't be 'working' anymore.
  * The uuids for owners of player shops get stored as soon as the player logs in. The playername gets used for display purposes and as identifier for the shop owner of those player shops for which we don't have a uuid yet.

**Known caveats:**  
* If the trade for a written book fails players can sometimes still open and read the book if they close the shop and click the temporary fake book in their inventory fast enough. There is not much I can do about this.
* If you use the 'remove shopkeeper after certain time of inactivity of player' feature, then you might experience some server freezing when the plugin loads: to find out when the owner of a shop was online for the last time we use a method which might have to look up a player's uuid or name via mojangs services. However, this is currently considered a minor 'issue' as those lookups can only happen once per plugin load (and reload), and the server probably also caches the results from those lookups, so the next plugin loads probably won't have to look up this data again.
* If you have shops with items in it with attributes, those might disappear or crash players. I am not completly sure if this is an issue with Shopkeepers or a general thing that the client cannot cope with certain modified items and crashes then. I also experienced this only for the first time I tested the new 1.7.8 Bukkit version, afterwards those items went lost somehow and stopped crashing the client. However, I wasn't able yet to test the new Shopkeepers version with items with custom attributes / edited nbt data, as I wasn't able yet to find some updated/working plugin which creates items with attributes.

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
* Probably fixed: Shopkeeper entities (villagers) duplicating sometimes. This was caused by plugins which manually unload chunks without calling an event (without letting us know). We now work around this by loading the chunk whenever we want to remove the entity again. Edit: Please let me know if this is still an issue.
* Probably fixed: Chunks kept loaded due to the same reason above: plugins not letting us know that they have unloaded the chunk. We now load the chunk only once and request an ordinary ChunkUnload which will then remove and deactivate the Shopkeeper when the chunk gets unloaded by that request. (Edit: seems to still be not fixed -> CraftBukkit seems to not call an ChunkUnloadEvent if saving is disabled :( )

## v1.16 Release (2014-02-08)
### Supported MC versions: 1.7.4, 1.7.2, 1.6.4
* Fixed: Preventing of the regular usage of the shop creation item did not work properly
* Fixed: Players could lose their spawn eggs when clicking a living shopkeeper (clicking with spawn eggs in hand is now blocked)
* Fixed: Some NPE when sign shops had no name
* Fixed: Name plate prefix / coloring going lost under certain conditions. Also, the entity name / name plate are now removed if name plates are disabled in the config. Previously, they were simply left the way they are.
* Changed: Empty messages are now skipped (quick solution to disable certainmessages)
* Added: Item data settings for the delete, hire, and name item.
* Added: Buttons can now have configurable item lore
* There are probably a few new messages in the config

See also the changes of the previous 1.16 beta versions!

## v1.16-beta3 Beta (2014-01-09)
### Supported MC versions: 1.7.4, 1.7.2, 1.6.4
* Added new command to remotly open admin shops via "/shopkeeper remote <shopname>" which needs the new `shopkeeper.remote` permission node.
* Fixed: shop entity-types are now correctly being checked, if they are enabled in the config, before they are created
* Fixed: an empty save.yml file should no longer cause an error
* Citizens2 NPC's are now ignored when being clicked
* Added a few new settings to allow naturally spawned villagers to be "hired" (a certain amount of shop-hire items can be traded for the shop creation item). This is turned off by default.
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
* Added `prevent-trading-with-own-shop` option.
* Added `tax-rate` and `tax-round-up` options.
* Added `prevent-shop-creation-item-regular-usage` option.
* Added transfer command ("/shopkeeper transfer <playername>" while looking at a shopkeeper chest) and `shopkeeper.transfer` permission node.
* Added setforhire command ("/shopkeeper setforhire" while looking at a shopkeeper chest and holding the cost in hand) and `shopkeeper.setforhire` and `shopkeeper.hire` permission nodes.
* Added `max-shops-perm-options` option and associated `shopkeeper.maxshops.<X>` permission node.
* Added `name-regex` and msg-name-invalid options.
* Added creeper shops (options: `enable-creeper-shops` and msg-selected-creeper-shop; permission: `shopkeeper.creeper`).
* Added support for 1.6.2.

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
* Added support for all items with meta information (fireworks, enchanted books, custom potions, etc.). Renamed items and items with lore are also supported.
* Added tooltips for the editor window action buttons.
* Removed the "Save" action button (just close the editor window to save).
* Added the "Set Shop Name" action button, which allows you to set a shop's name. This sets both the nameplate of the villager and the trade window text.
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
* Sign shopkeepers added: Right-click a chest with an emerald in your hand, then right-click a block to place the sign shop.
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
