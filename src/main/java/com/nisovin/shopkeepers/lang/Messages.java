package com.nisovin.shopkeepers.lang;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.config.lib.annotation.WithDefaultValueType;
import com.nisovin.shopkeepers.config.lib.annotation.WithValueTypeProvider;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.types.ColoredStringListValue;
import com.nisovin.shopkeepers.config.lib.value.types.ColoredStringValue;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

@WithDefaultValueType(fieldType = String.class, valueType = ColoredStringValue.class)
@WithValueTypeProvider(ColoredStringListValue.Provider.class)
public class Messages extends Config {

	// TODO Replace all with Text? Will require converting back to String, especially for texts used by items.
	public static String shopTypeAdminRegular = c("Admin shop");
	public static String shopTypeSelling = c("Selling shop");
	public static String shopTypeBuying = c("Buying shop");
	public static String shopTypeTrading = c("Trading shop");
	public static String shopTypeBook = c("Book shop");

	public static String shopTypeDescAdminRegular = c("has unlimited stock");
	public static String shopTypeDescSelling = c("sells items to players");
	public static String shopTypeDescBuying = c("buys items from players");
	public static String shopTypeDescTrading = c("trades items with players");
	public static String shopTypeDescBook = c("sells book copies");

	public static String shopObjectTypeLiving = c("{type}");
	public static String shopObjectTypeSign = c("sign");
	public static String shopObjectTypeNpc = c("npc");

	public static Text selectedShopType = Text.parse("&aSelected shop type: &6{type} &7({description})");
	public static Text selectedShopObjectType = Text.parse("&aSelected object type: &6{type}");

	public static Text creationItemSelected = Text.parse("&aShop creation:\n"
			+ "&e  Do not aim at any block. Then:\n"
			+ "&e  Left/Right-click to select the shop type.\n"
			+ "&e  Sneak + left/right-click to select the object type.\n"
			+ "&e  Right-click a container to select it.\n"
			+ "&e  Then right-click a block to place the shopkeeper.");

	public static String stateEnabled = c("&2Enabled");
	public static String stateDisabled = c("&4Disabled");

	public static String editorTitle = c("Shopkeeper Editor");

	public static String buttonPreviousPage = c("&6<- Previous page ({prev_page} of {max_page})");
	public static List<String> buttonPreviousPageLore = c(Arrays.asList());
	public static String buttonNextPage = c("&6Next page ({next_page} of {max_page}) ->");
	public static List<String> buttonNextPageLore = c(Arrays.asList());
	public static String buttonCurrentPage = c("&6Page {page} of {max_page}");
	public static List<String> buttonCurrentPageLore = c(Arrays.asList());

	public static String buttonName = c("&aSet shop name");
	public static List<String> buttonNameLore = c(Arrays.asList("Lets you rename", "your shopkeeper"));
	public static String buttonContainer = c("&aView shop inventory");
	public static List<String> buttonContainerLore = c(Arrays.asList("Lets you view the inventory", "your shopkeeper is using"));
	public static String buttonTradeNotifications = c("&aTrade Notifications");
	public static List<String> buttonTradeNotificationsLore = c(Arrays.asList("Toggles trade notifications", "for this shopkeeper on/off.", "Currently: {state}"));
	public static String buttonDelete = c("&4Delete");
	public static List<String> buttonDeleteLore = c(Arrays.asList("Closes and removes", "this shopkeeper"));

	public static String buttonSignVariant = c("&aChoose sign variant");
	public static List<String> buttonSignVariantLore = c(Arrays.asList("Changes the sign's", "wood type"));
	public static String buttonBaby = c("&aToggle baby variant");
	public static List<String> buttonBabyLore = c(Arrays.asList("Toggles between the mob's", "baby and adult variant"));
	public static String buttonSitting = c("&aToggle sitting pose");
	public static List<String> buttonSittingLore = c(Arrays.asList("Toggles the mob's", "sitting pose"));
	public static String buttonCatVariant = c("&aChoose cat variant");
	public static List<String> buttonCatVariantLore = c(Arrays.asList("Changes the cat's look"));
	public static String buttonRabbitVariant = c("&aChoose rabbit variant");
	public static List<String> buttonRabbitVariantLore = c(Arrays.asList("Changes the rabbit's look"));
	public static String buttonCollarColor = c("&aChoose collar color");
	public static List<String> buttonCollarColorLore = c(Arrays.asList("Changes the mob's", "collar color"));
	public static String buttonWolfAngry = c("&aToggle angry wolf");
	public static List<String> buttonWolfAngryLore = c(Arrays.asList("Toggles the wolf's", "angry state"));
	public static String buttonCarryingChest = c("&aToggle carrying chest");
	public static List<String> buttonCarryingChestLore = c(Arrays.asList("Toggles whether the mob", "is carrying a chest"));
	public static String buttonHorseColor = c("&aChoose horse color");
	public static List<String> buttonHorseColorLore = c(Arrays.asList("Changes the color", "of the horse"));
	public static String buttonHorseStyle = c("&aChoose horse style");
	public static List<String> buttonHorseStyleLore = c(Arrays.asList("Changes the coat pattern", "of the horse"));
	public static String buttonHorseArmor = c("&aChoose horse armor");
	public static List<String> buttonHorseArmorLore = c(Arrays.asList("Changes the armor", "of the horse"));
	public static String buttonLlamaVariant = c("&aChoose llama variant");
	public static List<String> buttonLlamaVariantLore = c(Arrays.asList("Changes the llama's look"));
	public static String buttonLlamaCarpetColor = c("&aLlama carpet color");
	public static List<String> buttonLlamaCarpetColorLore = c(Arrays.asList("Changes the llama's", "carpet color"));
	public static String buttonCreeperCharged = c("&aToggle charged creeper");
	public static List<String> buttonCreeperChargedLore = c(Arrays.asList("Toggles the creeper's", "charged state"));
	public static String buttonFoxVariant = c("&aChoose fox variant");
	public static List<String> buttonFoxVariantLore = c(Arrays.asList("Changes the fox's look"));
	public static String buttonFoxCrouching = c("&aToggle crouching pose");
	public static List<String> buttonFoxCrouchingLore = c(Arrays.asList("Toggles the fox's", "crouching pose"));
	public static String buttonFoxSleeping = c("&aToggle sleeping pose");
	public static List<String> buttonFoxSleepingLore = c(Arrays.asList("Toggles the fox's", "sleeping pose"));
	public static String buttonMooshroomVariant = c("&aChoose mooshroom variant");
	public static List<String> buttonMooshroomVariantLore = c(Arrays.asList("Changes the look", "of the mooshroom"));
	public static String buttonPandaVariant = c("&aChoose panda variant");
	public static List<String> buttonPandaVariantLore = c(Arrays.asList("Changes the panda's look"));
	public static String buttonParrotVariant = c("&aChoose parrot variant");
	public static List<String> buttonParrotVariantLore = c(Arrays.asList("Changes the parrot's look"));
	public static String buttonPigSaddle = c("&aToggle pig saddle");
	public static List<String> buttonPigSaddleLore = c(Arrays.asList("Toggles the pig's saddle"));
	public static String buttonSheepColor = c("&aChoose sheep color");
	public static List<String> buttonSheepColorLore = c(Arrays.asList("Changes the sheep's", "wool color"));
	public static String buttonSheepSheared = c("&aToggle sheared sheep");
	public static List<String> buttonSheepShearedLore = c(Arrays.asList("Toggles the sheep's", "sheared state"));
	public static String buttonVillagerProfession = c("&aChoose villager profession");
	public static List<String> buttonVillagerProfessionLore = c(Arrays.asList("Changes the profession", "of the villager"));
	public static String buttonVillagerVariant = c("&aChoose villager variant");
	public static List<String> buttonVillagerVariantLore = c(Arrays.asList("Changes the look", "of the villager"));
	public static String buttonVillagerLevel = c("&aChoose villager badge color");
	public static List<String> buttonVillagerLevelLore = c(Arrays.asList("Changes the badge color", "of the villager"));
	public static String buttonZombieVillagerProfession = c("&aChoose villager profession");
	public static List<String> buttonZombieVillagerProfessionLore = c(Arrays.asList("Changes the profession", "of the zombie villager"));
	public static String buttonSlimeSize = c("&aChoose slime size");
	public static List<String> buttonSlimeSizeLore = c(Arrays.asList("Cycles the slime's size.", "Current size: &e{size}"));
	public static String buttonMagmaCubeSize = c("&aChoose magma cube size");
	public static List<String> buttonMagmaCubeSizeLore = c(Arrays.asList("Cycles the magma cube's size.", "Current size: &e{size}"));
	public static String buttonSnowmanPumpkinHead = c("&aToggle pumpkin head");
	public static List<String> buttonSnowmanPumpkinHeadLore = c(Arrays.asList("Toggles the snowman's", "pumpkin head"));
	public static String buttonShulkerColor = c("&aChoose shulker color");
	public static List<String> buttonShulkerColorLore = c(Arrays.asList("Changes the color", "of the shulker"));

	public static String adminSignShopLine1 = c("&2[SHOP]");
	public static String adminSignShopLine2 = c("&7{shopName}");
	public static String adminSignShopLine3 = c("");
	public static String adminSignShopLine4 = c("&eRight click!");

	public static String playerSignShopLine1 = c("&2[SHOP]");
	public static String playerSignShopLine2 = c("&7{shopName}");
	public static String playerSignShopLine3 = c("&7{owner}");
	public static String playerSignShopLine4 = c("&eRight click!");

	public static String forHireTitle = c("For Hire");
	public static String buttonHire = c("&aHire");
	public static List<String> buttonHireLore = c(Arrays.asList("Buy this shopkeeper"));

	public static String tradingTitlePrefix = c("&2");
	public static String tradingTitleDefault = c("Shopkeeper");

	public static Text containerSelected = Text.parse("&aContainer selected! Right-click a block to place your shopkeeper.");
	public static Text unsupportedContainer = Text.parse("&7This type of container cannot be used for shops.");
	public static Text mustSelectContainer = Text.parse("&7You must right-click a container before placing your shopkeeper.");
	public static Text mustTargetContainer = Text.parse("&7You must target a container to place this type of shop.");
	public static Text invalidContainer = Text.parse("&7The selected block is not a valid container!");
	public static Text containerTooFarAway = Text.parse("&7The shopkeeper's container is too far away!");
	public static Text containerNotPlaced = Text.parse("&7You must select a container you have recently placed!");
	public static Text containerAlreadyInUse = Text.parse("&7Another shopkeeper is already using the selected container!");
	public static Text noContainerAccess = Text.parse("&7You cannot access the selected container!");
	public static Text tooManyShops = Text.parse("&7You have already reached the limit of how many shops you can own!");
	public static Text noPlayerShopsViaCommand = Text.parse("&7Player shops can only be created via the shop creation item!");
	public static Text shopCreateFail = Text.parse("&7You cannot create a shopkeeper there.");

	public static Text typeNewName = Text.parse("&aPlease type the shop's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.");
	public static Text nameSet = Text.parse("&aThe shop's name has been set!");
	public static Text nameHasNotChanged = Text.parse("&aThe shop's name has not changed.");
	public static Text nameInvalid = Text.parse("&aThat name is not valid!");
	public static String nameplatePrefix = c("&2");

	public static Text shopTypeDisabled = Text.parse("&7The shop type '&6{type}&7' is disabled.");
	public static Text shopObjectTypeDisabled = Text.parse("&7The shop object type '&6{type}&7' is disabled.");

	public static Text mustTargetShop = Text.parse("&7You have to target a shopkeeper.");
	public static Text mustTargetAdminShop = Text.parse("&7You have to target an admin shopkeeper.");
	public static Text mustTargetPlayerShop = Text.parse("&7You have to target a player shopkeeper.");
	public static Text targetEntityIsNoShop = Text.parse("&7The targeted entity is no shopkeeper.");
	public static Text targetShopIsNoAdminShop = Text.parse("&7The targeted shopkeeper is no admin shopkeeper.");
	public static Text targetShopIsNoPlayerShop = Text.parse("&7The targeted shopkeeper is no player shopkeeper.");
	public static Text unusedContainer = Text.parse("&7No shopkeeper is using this container.");
	public static Text notOwner = Text.parse("&7You are not the owner of this shopkeeper.");
	// Placeholders: {owner} -> new owners name
	public static Text ownerSet = Text.parse("&aThe new owner is now &e{owner}");
	public static Text shopCreationItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a shop creation item(s)!");
	public static Text shopCreationItemsReceived = Text.parse("&aYou have received &e{amount}&a shop creation item(s)!");
	public static Text currencyItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a currency item(s)!");
	public static Text currencyItemsReceived = Text.parse("&aYou have received &e{amount}&a currency item(s)!");
	public static Text highCurrencyItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a high currency item(s)!");
	public static Text highCurrencyItemsReceived = Text.parse("&aYou have received &e{amount}&a high currency item(s)!");
	public static Text highCurrencyDisabled = Text.parse("&cThe high currency is disabled!");
	public static Text itemsConverted = Text.parse("&aConverted &e{count}&a item stack(s)!");
	public static String unknownBookAuthor = c("Unknown");

	public static Text tradePermSet = Text.parse("&aThe shop's trading permission has been set to '&e{perm}&a'!");
	public static Text tradePermRemoved = Text.parse("&aThe shop's trading permission '&e{perm}&a' has been removed!");
	public static Text tradePermView = Text.parse("&aThe shop's current trading permission is '&e{perm}&a'.");

	public static Text zombieVillagerCuringDisabled = Text.parse("&7Curing of zombie villagers is disabled.");
	public static Text mustHoldHireItem = Text.parse("&7You have to hold the required hire item in your hand.");
	public static Text setForHire = Text.parse("&aThis shopkeeper can now be hired.");
	public static Text hired = Text.parse("&aYou have hired this shopkeeper!");
	public static Text missingHirePerm = Text.parse("&7You do not have the permission to hire shopkeepers.");
	public static Text cannotHire = Text.parse("&7You cannot afford to hire this shopkeeper.");
	public static Text cannotHireShopType = Text.parse("&7You do not have the permission to hire this type of shopkeeper.");
	// Placeholders: {costs}, {hire-item}
	public static Text villagerForHire = Text.parse("&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.");

	public static Text missingTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text missingCustomTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text cannotTradeWithOwnShop = Text.parse("&7You cannot trade with your own shop.");
	public static Text cannotTradeWhileOwnerOnline = Text.parse("&7You cannot trade while the owner of this shop ('&e{owner}&7') is online.");
	public static Text cannotTradeWithShopMissingContainer = Text.parse("&7You cannot trade with this shop, because its container is missing.");
	public static Text cannotTradeUnexpectedTrade = Text.parse("&7Trade aborted: The traded items do not match what this shopkeeper expected.");
	public static Text cannotTradeItemsNotStrictlyMatching = Text.parse("&7Trade aborted: The offered items do not exactly match the required items.");
	public static Text cannotTradeInsufficientStorageSpace = Text.parse("&7Trade aborted: This shop does not have enough storage space.");
	public static Text cannotTradeInsufficientCurrency = Text.parse("&7Trade aborted: This shop does not have enough currency.");
	public static Text cannotTradeInsufficientStock = Text.parse("&7Trade aborted: This shop does not have enough of the traded item.");
	public static Text cannotTradeInsufficientWritableBooks = Text.parse("&7Trade aborted: This book shop lacks writable books to copy the book with.");

	// Trade placeholders: {player}, {playerId}, {resultItem}, {resultItemAmount}, {item1}, {item1Amount}, {item2},
	// {item2Amount}, {shop} (replaced by admin or player shop text respectively), {trade_count} (replaced by the trade
	// count message, or empty if the trade count is one).
	// Shop placeholders: {shop_id}, {shop_uuid}, {shop_name}, {shop_world}, {shop_x}, {shop_y}, {shop_z},
	// {shop_location} ('world,x,y,z' or '[virtual]'), {shop_type}, {shop_object_type}, {shop_owner_name},
	// {shop_owner_uuid}. World name and coordinates are empty for virtual shops.
	public static Text tradeNotificationOneItem = Text.parse("&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text tradeNotificationTwoItems = Text.parse("&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] [&6{item2Amount}x &a{item2}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text buyNotificationOneItem = Text.parse("&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text buyNotificationTwoItems = Text.parse("&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] [&6{item2Amount}x &a{item2}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text tradeNotificationPlayerShop = Text.parse("&e{shop_owner_name}");
	public static Text tradeNotificationNamedPlayerShop = Text.parse("&e{shop_owner_name}");
	public static Text tradeNotificationAdminShop = Text.parse("&eAdmin Shop");
	public static Text tradeNotificationNamedAdminShop = Text.parse("&e\"{shop_name}\"");
	public static Text tradeNotificationTradeCount = Text.parse("&7 (&6{count}x&7)");

	// Placeholders: Same as the general trade notification messages.
	public static Text ownerTradeNotificationOneItem = Text.parse("&e{player}&7 bought &6{resultItemAmount}x &a{resultItem}&7 from {shop}{trade_count}");
	public static Text ownerTradeNotificationTwoItems = Text.parse("&e{player}&7 bought &6{resultItemAmount}x &a{resultItem}&7 from {shop}{trade_count}");
	public static Text ownerBuyNotificationOneItem = Text.parse("&e{player}&7 sold &6{item1Amount}x &a{item1}&7 to {shop}{trade_count}");
	public static Text ownerBuyNotificationTwoItems = Text.parse("&e{player}&7 sold &6{item1Amount}x &a{item1}&7 and &6{item2Amount}x &a{item2}&7 to {shop}{trade_count}");
	public static Text ownerTradeNotificationShop = Text.parse("one of your shops");
	public static Text ownerTradeNotificationNamedShop = Text.parse("your shop &e\"{shop_name}\"");
	public static Text ownerBuyNotificationShop = Text.parse("one of your shops");
	public static Text ownerBuyNotificationNamedShop = Text.parse("your shop &e\"{shop_name}\"");
	public static Text ownerTradeNotificationTradeCount = Text.parse("&7 (&6{count}x&7)");

	public static Text disableTradeNotificationsHint = Text.parse("&7You can disable these trade notifications with the command &e{command}");
	public static Text disableTradeNotificationsHintCommand = Text.parse("/shopkeeper notify trades");
	public static Text tradeNotificationsDisabled = Text.parse("&aYou will no longer receive trade notifications during this game session.");
	public static Text tradeNotificationsEnabled = Text.parse("&aYou will now receive trade notifications again.");

	// Placeholders: General shop placeholders (see above).
	// Additional placeholders: {missing_item}
	// public static Text ownerNotificationInsufficientStock = Text.parse("&7Your shop &e{shop}&7 is short of
	// &6{missing_item}&7!");
	// public static Text ownerNotificationInsufficientStorage = Text.parse("&7Your shop &e{shop}&7 has insufficient
	// storage space!");

	public static Text shopkeeperCreated = Text.parse("&aShopkeeper created: &6{type} &7({description})\n{setupDesc}");

	public static String shopSetupDescSelling = c("&e  Add items you want to sell to your container, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.");
	public static String shopSetupDescBuying = c("&e  Add one of each item you want to buy to your container,\n"
			+ "&e  then right-click the shop while sneaking to modify costs.");
	public static String shopSetupDescTrading = c("&e  Add items you want to sell to your container, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.");
	public static String shopSetupDescBook = c("&e  Add written and blank books to your container, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.");
	public static String shopSetupDescAdminRegular = c("&e  Right-click the shop while sneaking to modify trades.");

	public static String tradeSetupDescHeader = c("&6{shopType}");
	public static List<String> tradeSetupDescAdminRegular = c(Arrays.asList("Has unlimited stock.", "Insert items from your inventory.", "Top row: Result items", "Bottom rows: Cost items"));
	public static List<String> tradeSetupDescSelling = c(Arrays.asList("Sells items to players.", "Insert items to sell into the container.", "Left/Right click to adjust amounts.", "Top row: Items being sold", "Bottom rows: Cost items"));
	public static List<String> tradeSetupDescBuying = c(Arrays.asList("Buys items from players.", "Insert one of each item you want to", "buy and plenty of currency items", "into the container.", "Left/Right click to adjust amounts.", "Top row: Cost items", "Bottom row: Items being bought"));
	public static List<String> tradeSetupDescTrading = c(Arrays.asList("Trades items with players.", "Pickup an item from your inventory", "and then click a slot to place it.", "Left/Right click to adjust amounts.", "Top row: Result items", "Bottom rows: Cost items"));
	public static List<String> tradeSetupDescBook = c(Arrays.asList("Sells book copies.", "Insert written and blank books", "into the container.", "Left/Right click to adjust costs.", "Top row: Books being sold", "Bottom rows: Cost items"));

	public static Text missingEditVillagersPerm = Text.parse("&7You do not have the permission to edit villagers.");
	public static Text missingEditWanderingTradersPerm = Text.parse("&7You do not have the permission to edit wandering traders.");
	public static Text mustTargetEntity = Text.parse("&7You have to target an entity.");
	public static Text mustTargetVillager = Text.parse("&7You have to target a villager.");
	public static Text targetEntityIsNoVillager = Text.parse("&7The targeted entity is no regular villager.");
	public static Text villagerNoLongerExists = Text.parse("&7The villager no longer exists.");

	public static String villagerEditorTitle = c("&2Villager Editor: &e{villagerName}");
	public static String villagerEditorDescriptionHeader = c("&6Villager Editor");
	public static List<String> villagerEditorDescription = c(Arrays.asList(
			"Top row: Result items",
			"Bottom rows: Cost items",
			"Edited trades have infinite",
			"uses and no XP rewards."
	));

	public static String buttonDeleteVillager = c("&4Delete");
	public static List<String> buttonDeleteVillagerLore = c(Arrays.asList("Deletes the villager"));
	public static String buttonNameVillager = c("&aSet villager name");
	public static List<String> buttonNameVillagerLore = c(Arrays.asList("Lets you rename", "the villager"));
	public static String buttonVillagerInventory = c("&aView villager inventory");
	public static List<String> buttonVillagerInventoryLore = c(Arrays.asList(
			"Lets you view a copy of",
			"the villager's inventory"
	));
	public static String buttonMobAi = c("&aToggle mob AI");
	public static List<String> buttonMobAiLore = c(Arrays.asList("Toggles the mob's AI"));
	public static String buttonInvulnerability = c("&aToggle invulnerability");
	public static List<String> buttonInvulnerabilityLore = c(Arrays.asList("Toggles the mob's", "invulnerability.", "Players in creative mode", "can still damage the mob."));

	public static String villagerInventoryTitle = c("Villager inventory (copy)");
	public static String setVillagerXp = c("&aSet the villager's XP to &e{xp}");
	public static String noVillagerTradesChanged = c("&aNo trades have been changed.");
	public static String villagerTradesChanged = c("&e{changedTrades}&a trades have been changed.");

	public static Text typeNewVillagerName = Text.parse("&aPlease type the villager's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.");
	public static Text villagerNameSet = Text.parse("&aThe villager's name has been set!");
	public static Text villagerNameInvalid = Text.parse("&aThat name is not valid!");

	public static Text listAdminShopsHeader = Text.parse("&9There are &e{shopsCount} &9admin shops: &e(Page {page} of {maxPage})");
	public static Text listAllShopsHeader = Text.parse("&9There are &e{shopsCount} &9shops in total: &e(Page {page} of {maxPage})");
	public static Text listPlayerShopsHeader = Text.parse("&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page} of {maxPage})");
	public static Text listShopsEntry = Text.parse("  &e{shopId}) &7{shopName}&r&8at &7({location})&8, type: &7{shopType}&8, object: &7{objectType}");

	public static Text shopRemoved = Text.parse("&aThe shopkeeper has been removed.");
	public static Text shopAlreadyRemoved = Text.parse("&7The shopkeeper has already been removed.");
	public static Text shopRemovalCancelled = Text.parse("&cA plugin has prevented the removal of the shopkeeper.");

	public static Text shopsAlreadyRemoved = Text.parse("&e{shopsCount}&7 of the shops have already been removed.");
	public static Text shopRemovalsCancelled = Text.parse("&cPlugins have prevented the removal of &e{shopsCount}&c of the shops.");
	public static Text adminShopsRemoved = Text.parse("&e{shopsCount} &aadmin shops have been removed.");
	public static Text shopsOfPlayerRemoved = Text.parse("&e{shopsCount} &ashops of player '&e{player}&a' have been removed.");
	public static Text playerShopsRemoved = Text.parse("&e{shopsCount} &aplayer shops have been removed.");

	public static Text confirmRemoveShop = Text.parse("&cYou are about to irrevocably remove the specified shopkeeper!");
	public static Text confirmRemoveAllAdminShops = Text.parse("&cYou are about to irrevocably remove all admin shops (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllOwnShops = Text.parse("&cYou are about to irrevocably remove all your shops (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllShopsOfPlayer = Text.parse("&cYou are about to irrevocably remove all shops of player &6{player}&c (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllPlayerShops = Text.parse("&cYou are about to irrevocably remove all player shops of all players (&6{shopsCount}&c)!");

	public static Text confirmationRequired = Text.parse("&7Please confirm this action by typing &6/shopkeepers confirm");
	public static Text confirmationExpired = Text.parse("&cConfirmation expired.");
	public static Text nothingToConfirm = Text.parse("&cThere is nothing to confirm currently.");

	public static String confirmationUiDeleteShopTitle = c("&cReally delete this shop?");
	public static List<String> confirmationUiDeleteShopConfirmLore = c(Arrays.asList("This will irrevocably", "remove this shop!"));

	public static String confirmationUiDeleteVillagerTitle = c("&cReally delete this villager?");
	public static List<String> confirmationUiDeleteVillagerConfirmLore = c(Arrays.asList("This will irrevocably", "remove this villager!"));
	public static Text villagerRemoved = Text.parse("&aThe villager has been removed.");

	public static String confirmationUiConfirm = c("&2Confirm");
	public static String confirmationUiCancel = c("&4Cancel");
	public static List<String> confirmationUiCancelLore = c(Arrays.asList("This will abort the", "current action."));
	public static Text confirmationUiAborted = Text.parse("&7Confirmation aborted.");

	public static Text noPermission = Text.parse("&cYou don't have the permission to do that.");

	public static Text commandUnknown = Text.parse("&cUnknown command '&e{command}&c'!");
	public static Text commandArgumentUnexpected = Text.parse("&cUnexpected argument '&e{argument}&c'.");
	public static Text commandArgumentRequiresPlayer = Text.parse("&cArgument '&e{argumentFormat}&c' requires a player to execute the command.");
	public static Text commandArgumentMissing = Text.parse("&cMissing argument '&e{argumentFormat}&c'.");
	public static Text commandArgumentInvalid = Text.parse("&cInvalid argument '&e{argument}&c'.");
	public static Text commandPlayerArgumentMissing = Text.parse("&cNo player specified for '&e{argumentFormat}&c'.");
	public static Text commandPlayerArgumentInvalid = Text.parse("&cNo player found for '&e{argument}&c'.");
	public static Text commandShopTypeArgumentInvalid = Text.parse("&cUnknown shop type '&e{argument}&c'.");
	public static Text commandShopObjectTypeArgumentInvalid = Text.parse("&cUnknown shop object type '&e{argument}&c'.");
	public static Text commandShopkeeperArgumentInvalid = Text.parse("&cNo shopkeeper found for '&e{argument}&c'.");
	public static Text commandShopkeeperArgumentNoAdminShop = Text.parse("&cShopkeeper '&e{argument}&c' is no admin shopkeeper.");
	public static Text commandShopkeeperArgumentNoPlayerShop = Text.parse("&cShopkeeper '&e{argument}&c' is no player shopkeeper.");

	public static Text ambiguousPlayerName = Text.parse("&cThere are multiple matches for the name '&e{name}&c'!");
	public static Text ambiguousPlayerNameEntry = Text.parse("&c  - '&e{name}&r&c' (&6{uuid}&c)");
	public static Text ambiguousPlayerNameMore = Text.parse("&c  - ....");

	public static Text commandHelpTitle = Text.parse("&9***** &8[&6Shopkeepers v{version}&8] &9*****");
	public static Text commandHelpUsageFormat = Text.parse("&e{usage}");
	public static Text commandHelpDescriptionFormat = Text.parse("&8 - &3{description}");

	public static Text commandDescriptionShopkeeper = Text.parse("Creates a shopkeeper.");
	public static Text commandDescriptionHelp = Text.parse("Shows this help page.");
	public static Text commandDescriptionReload = Text.parse("Reloads this plugin.");
	public static Text commandDescriptionDebug = Text.parse("Toggles debug mode on and off.");
	public static Text commandDescriptionNotify = Text.parse("Turns trade notifications for you on or off.");
	public static Text commandDescriptionList = Text.parse("Lists all shops of a specific player, or all admin shops.");
	public static Text commandDescriptionRemove = Text.parse("Removes a specific shop.");
	public static Text commandDescriptionRemoveAll = Text.parse("Removes all shops of a specific player, all players, or all admin shops.");
	public static Text commandDescriptionGive = Text.parse("Gives shop creation item(s) to the specified player.");
	public static Text commandDescriptionGiveCurrency = Text.parse("Gives currency item(s) to the specified player.");
	public static Text commandDescriptionConvertItems = Text.parse("Converts the held (or all) items to conform to Spigot's data format.");
	public static Text commandDescriptionRemote = Text.parse("Remotely opens a shop (Optionally: For another player).");
	public static Text commandDescriptionRemoteEdit = Text.parse("Remotely edits a shop.");
	public static Text commandDescriptionTransfer = Text.parse("Transfers the ownership of a shop.");
	public static Text commandDescriptionSettradeperm = Text.parse("Sets, removes (-) or displays (?) the trading permission.");
	public static Text commandDescriptionSetforhire = Text.parse("Sets one of your shops for sale.");
	public static Text commandDescriptionEditVillager = Text.parse("Opens the editor for the target villager.");

	/////

	private static final String LANG_FOLDER = "lang";
	private static final String DEFAULT_LANGUAGE = "en-default";

	public static File getLangFolder() {
		return new File(SKShopkeepersPlugin.getInstance().getDataFolder(), LANG_FOLDER);
	}

	private static String getLanguageFileName(String language) {
		return "language-" + language + ".yml";
	}

	// Relative to the plugin's data folder:
	private static String getLanguageFilePath(String language) {
		return LANG_FOLDER + "/" + getLanguageFileName(language);
	}

	private static File getLanguageFile(String language) {
		String languageFilePath = getLanguageFilePath(language);
		return new File(SKShopkeepersPlugin.getInstance().getDataFolder(), languageFilePath);
	}

	/**
	 * The default language file is freshly written on every startup and overwrites the already existing default
	 * language file.
	 */
	private static void saveDefaultLanguageFile() {
		String languageFilePath = getLanguageFilePath(DEFAULT_LANGUAGE);
		SKShopkeepersPlugin plugin = SKShopkeepersPlugin.getInstance();
		assert plugin.getResource(languageFilePath) != null;
		plugin.saveResource(languageFilePath, true);
	}

	public static void loadLanguageFile() {
		// Create default language file:
		saveDefaultLanguageFile();

		SKShopkeepersPlugin plugin = SKShopkeepersPlugin.getInstance();
		String language = Settings.language;

		// Create language file if it is missing and there exists a default:
		String languageFilePath = getLanguageFilePath(language);
		File languageFile = getLanguageFile(language);
		if (!languageFile.exists() && plugin.getResource(languageFilePath) != null) {
			plugin.saveResource(languageFilePath, false);
		}

		// Load messages from language config:
		if (!languageFile.exists()) {
			Log.warning("Could not find language file '" + languageFile.getName() + "'!");
		} else {
			Log.info("Loading language file: " + languageFile.getName());
			try {
				// Load language config:
				YamlConfiguration languageConfig = new YamlConfiguration();
				languageConfig.load(languageFile);

				// Load messages:
				INSTANCE.load(languageConfig);
			} catch (Exception e) {
				Log.warning("Could not load language file '" + languageFile.getName() + "'!", e);
			}

			// Also update the derived settings and call a config change event:
			Settings.onSettingsChanged();
		}
	}

	/////

	private static final Messages INSTANCE = new Messages();

	public static Messages getInstance() {
		return INSTANCE;
	}

	private Messages() {
	}

	@Override
	protected String getLogPrefix() {
		return "Language: ";
	}

	@Override
	protected String msgMissingValue(String configKey) {
		return this.getLogPrefix() + "Missing message: " + configKey;
	}

	@Override
	protected String msgUsingDefaultForMissingValue(String configKey, Object defaultValue) {
		return this.getLogPrefix() + "Using default value for missing message: " + configKey;
	}

	@Override
	protected String msgValueLoadException(String configKey, ValueLoadException e) {
		return this.getLogPrefix() + "Could not load message '" + configKey + "': " + e.getMessage();
	}

	@Override
	protected String msgDefaultValueLoadException(String configKey, ValueLoadException e) {
		return this.getLogPrefix() + "Could not load default value for message '" + configKey + "': " + e.getMessage();
	}

	@Override
	protected String msgInsertingDefault(String configKey) {
		return this.getLogPrefix() + "Inserting default value for missing message: " + configKey;
	}

	@Override
	protected String msgMissingDefault(String configKey) {
		return this.getLogPrefix() + "Missing default value for message: " + configKey;
	}

	@Override
	public void load(ConfigurationSection config) throws ConfigLoadException {
		Validate.notNull(config, "config is null");
		// Check for unexpected (possibly no longer existing) message keys:
		Set<String> configKeys = config.getKeys(true);
		for (String configKey : configKeys) {
			if (this.getSetting(configKey) == null) {
				Log.warning(this.getLogPrefix() + "Unknown message: " + configKey);
			}
		}

		// Load the config:
		super.load(config);
	}
}
