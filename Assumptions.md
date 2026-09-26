# Assumptions

This document collects some of the assumptions and implementation details about other software components that this plugin interacts with. This may include details about the underlying server and Bukkit/Spigot API implementation, but also about the Minecraft client, or the behavior of other plugins. If these assumptions are not actually fulfilled at runtime, the plugin can behave unexpectedly and is likely to cause problems.  
This document is in a continuous work-in-progress state: It will be updated and expanded as new assumptions are identified.

## Server and Bukkit API implementation

* `Inventory#setItem(int, ItemStack)` is expected to create a copy of the passed item stack. For instance, the implementation in CraftBukkit creates a new Minecraft item stack that is then stored in an underlying Minecraft inventory. When setting item stacks in an inventory, the Shopkeepers plugin can therefore avoid creating additional copies of the inserted item stacks. It is also possible for the plugin to set the same item stack instance to multiple slots, without changes to the item stack in any of these slots affecting the original item stack, or the item stacks in any of the other slots.
* `Inventory#getItem(int)` returns a live wrapper around the underlying Minecraft item stack. Changes to the returned item stack, such as changing its stack size, directly affect the underlying Minecraft item stack. It is not required to set the modified item stack back into the slot in order to apply the change. The returned item stack has to be copied before it is suited for long term storage, since it may otherwise be modified externally by Minecraft or other plugins.
* The same assumptions as above apply to operations that get or set the item stack on the cursor of a player inside an inventory view.
* Item stacks deserialized from configurations are Bukkit `ItemStack`s, and not `CraftItemStack`s.
  * Usually, this detail should not be relevant to us. This may only be relevant when:
    * Comparing the deserialized item stacks to other CraftItemStacks: The comparison between two CraftItemStacks uses a different implementation than comparisons that involve at least one Bukkit ItemStack. In the past, there have been cases in which these two implementations behaved differently. However, ideally, any identified differences should be fixed in the server's Bukkit API implementation, and then no longer be a concern of ours.
  * The Paper server is known to break this assumption: It automatically converts any deserialized Bukkit ItemStacks to CraftItemStacks.
