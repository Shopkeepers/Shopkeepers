package com.nisovin.shopkeepers.lang;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.ConfigData;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.config.lib.annotation.WithDefaultValueType;
import com.nisovin.shopkeepers.config.lib.annotation.WithValueTypeProvider;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.types.ColoredStringListValue;
import com.nisovin.shopkeepers.config.lib.value.types.ColoredStringValue;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.data.persistence.DataStore;
import com.nisovin.shopkeepers.util.data.persistence.bukkit.BukkitConfigDataStore;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

@WithDefaultValueType(fieldType = String.class, valueType = ColoredStringValue.class)
@WithValueTypeProvider(ColoredStringListValue.Provider.class)
public class Messages extends Config {

	// TODO Replace all with Text? Will require converting back to String,
	// especially for texts used
	// by items.
	public static String dateTimeFormat = c("yyyy-MM-dd HH:mm:ss");

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

	public static String shopObjectTypeEntity = c("{type}");
	public static String shopObjectTypeSign = c("sign");
	public static String shopObjectTypeHangingSign = c("hanging sign");
	public static String shopObjectTypeNpc = c("npc");

	public static Text selectedShopType = Text.parse("&aSelected shop type: &6{type} &7({description})");
	public static Text selectedShopObjectType = Text.parse("&aSelected object type: &6{type}");

	public static Text creationItemSelected = Text.parse("&aShop creation:\n"
			+ "&e  Do not aim at any block. Then:{shopTypeSelection}{objectTypeSelection}\n"
			+ "&e  Right-click a container to select it.\n"
			+ "&e  Then right-click a block to place the shopkeeper.");

	public static Text creationItemShopTypeSelection = Text.parse("\n&e  Left/Right-click to select the shop type.");
	public static Text creationItemObjectTypeSelection = Text
			.parse("\n&e  Sneak + left/right-click to select the object type.");

	public static String stateEnabled = c("&2Enabled");
	public static String stateDisabled = c("&4Disabled");

	public static String editorTitle = c("Shopkeeper Editor");

	public static String buttonPreviousPage = c("&6<- Previous page ({prev_page} of {max_page})");
	public static List<String> buttonPreviousPageLore = c(Arrays.asList());
	public static String buttonNextPage = c("&6Next page ({next_page} of {max_page}) ->");
	public static List<String> buttonNextPageLore = c(Arrays.asList());
	public static String buttonCurrentPage = c("&6Page {page} of {max_page}");
	public static List<String> buttonCurrentPageLore = c(Arrays.asList());

	public static String buttonShopOpen = c("&aClose shop");
	public static List<String> buttonShopOpenLore = c(Arrays.asList(
			"Temporarily closes the shop.",
			"Currently: &2Open",
			"&2Players can trade."));
	public static String buttonShopClosed = c("&aOpen shop");
	public static List<String> buttonShopClosedLore = c(Arrays.asList(
			"Re-opens the shop.",
			"Currently: &4Closed",
			"&4Players cannot trade."));
	public static String buttonName = c("&aSet shop name");
	public static List<String> buttonNameLore = c(Arrays.asList(
			"Lets you rename",
			"your shopkeeper"));
	public static String buttonEquipment = c("&aEquipment");
	public static List<String> buttonEquipmentLore = c(Arrays.asList(
			"Assign equipment.",
			"Note: Not all mobs support",
			"the same equipment slots."));
	public static String buttonMove = c("&aMove shopkeeper");
	public static List<String> buttonMoveLore = c(Arrays.asList(
			"Lets you move",
			"your shopkeeper"));
	public static String buttonContainer = c("&aView shop inventory");
	public static List<String> buttonContainerLore = c(Arrays.asList(
			"Lets you view the inventory",
			"your shopkeeper is using"));
	public static String buttonMembers = c("&aManage Members");
	public static List<String> buttonMembersLore = c(Arrays.asList(
			"Add or remove members",
			"who can help manage",
			"this shop"));
	public static String buttonTradeNotifications = c("&aTrade Notifications");
	public static List<String> buttonTradeNotificationsLore = c(Arrays.asList(
			"Toggles trade notifications",
			"for this shopkeeper on/off.",
			"Currently: {state}"));
	public static String buttonDelete = c("&4Delete");
	public static List<String> buttonDeleteLore = c(Arrays.asList(
			"Closes and removes",
			"this shopkeeper"));

	public static String buttonSignVariant = c("&aChoose sign variant");
	public static List<String> buttonSignVariantLore = c(Arrays.asList(
			"Changes the sign's",
			"wood type"));
	public static String buttonSignGlowingText = c("&aToggle glowing text");
	public static List<String> buttonSignGlowingTextLore = c(Arrays.asList(
			"Toggles glowing text",
			"on and off"));
	public static String buttonBaby = c("&aToggle baby variant");
	public static List<String> buttonBabyLore = c(Arrays.asList(
			"Toggles between the mob's",
			"baby and adult variant"));
	public static String buttonSitting = c("&aToggle sitting pose");
	public static List<String> buttonSittingLore = c(Arrays.asList(
			"Toggles the mob's",
			"sitting pose"));
	public static String buttonCatVariant = c("&aChoose cat variant");
	public static List<String> buttonCatVariantLore = c(Arrays.asList(
			"Changes the cat's look"));
	public static String buttonRabbitVariant = c("&aChoose rabbit variant");
	public static List<String> buttonRabbitVariantLore = c(Arrays.asList(
			"Changes the rabbit's look"));
	public static String buttonCollarColor = c("&aChoose collar color");
	public static List<String> buttonCollarColorLore = c(Arrays.asList(
			"Changes the mob's",
			"collar color"));
	public static String buttonWolfAngry = c("&aToggle angry wolf");
	public static List<String> buttonWolfAngryLore = c(Arrays.asList(
			"Toggles the wolf's",
			"angry state"));
	public static String buttonCarryingChest = c("&aToggle carrying chest");
	public static List<String> buttonCarryingChestLore = c(Arrays.asList(
			"Toggles whether the mob",
			"is carrying a chest"));
	public static String buttonHorseColor = c("&aChoose horse color");
	public static List<String> buttonHorseColorLore = c(Arrays.asList(
			"Changes the color",
			"of the horse"));
	public static String buttonHorseStyle = c("&aChoose horse style");
	public static List<String> buttonHorseStyleLore = c(Arrays.asList(
			"Changes the coat pattern",
			"of the horse"));
	public static String buttonHorseArmor = c("&aChoose horse armor");
	public static List<String> buttonHorseArmorLore = c(Arrays.asList(
			"Changes the armor",
			"of the horse"));
	public static String buttonLlamaVariant = c("&aChoose llama variant");
	public static List<String> buttonLlamaVariantLore = c(Arrays.asList(
			"Changes the llama's look"));
	public static String buttonLlamaCarpetColor = c("&aLlama carpet color");
	public static List<String> buttonLlamaCarpetColorLore = c(Arrays.asList(
			"Changes the llama's",
			"carpet color"));
	public static String buttonCreeperCharged = c("&aToggle charged creeper");
	public static List<String> buttonCreeperChargedLore = c(Arrays.asList(
			"Toggles the creeper's",
			"charged state"));
	public static String buttonFoxVariant = c("&aChoose fox variant");
	public static List<String> buttonFoxVariantLore = c(Arrays.asList(
			"Changes the fox's look"));
	public static String buttonFoxCrouching = c("&aToggle crouching pose");
	public static List<String> buttonFoxCrouchingLore = c(Arrays.asList(
			"Toggles the fox's",
			"crouching pose"));
	public static String buttonFoxSleeping = c("&aToggle sleeping pose");
	public static List<String> buttonFoxSleepingLore = c(Arrays.asList(
			"Toggles the fox's",
			"sleeping pose"));
	public static String buttonCowVariant = c("&aChoose cow variant");
	public static List<String> buttonCowVariantLore = c(Arrays.asList(
			"Changes the cow's look"));
	public static String buttonMooshroomVariant = c("&aChoose mooshroom variant");
	public static List<String> buttonMooshroomVariantLore = c(Arrays.asList(
			"Changes the look",
			"of the mooshroom"));
	public static String buttonPandaVariant = c("&aChoose panda variant");
	public static List<String> buttonPandaVariantLore = c(Arrays.asList(
			"Changes the panda's look"));
	public static String buttonParrotVariant = c("&aChoose parrot variant");
	public static List<String> buttonParrotVariantLore = c(Arrays.asList(
			"Changes the parrot's look"));
	public static String buttonPigVariant = c("&aChoose pig variant");
	public static List<String> buttonPigVariantLore = c(Arrays.asList(
			"Changes the pig's look"));
	public static String buttonChickenVariant = c("&aChoose chicken variant");
	public static List<String> buttonChickenVariantLore = c(Arrays.asList(
			"Changes the chicken's look"));
	public static String buttonSheepColor = c("&aChoose sheep color");
	public static List<String> buttonSheepColorLore = c(Arrays.asList(
			"Changes the sheep's",
			"wool color"));
	public static String buttonSheepSheared = c("&aToggle sheared sheep");
	public static List<String> buttonSheepShearedLore = c(Arrays.asList(
			"Toggles the sheep's",
			"sheared state"));
	public static String buttonVillagerProfession = c("&aChoose villager profession");
	public static List<String> buttonVillagerProfessionLore = c(Arrays.asList(
			"Changes the profession",
			"of the villager"));
	public static String buttonVillagerVariant = c("&aChoose villager variant");
	public static List<String> buttonVillagerVariantLore = c(Arrays.asList(
			"Changes the look",
			"of the villager"));
	public static String buttonVillagerLevel = c("&aChoose villager badge color");
	public static List<String> buttonVillagerLevelLore = c(Arrays.asList(
			"Changes the badge color",
			"of the villager"));
	public static String buttonZombieVillagerProfession = c("&aChoose villager profession");
	public static List<String> buttonZombieVillagerProfessionLore = c(Arrays.asList(
			"Changes the profession",
			"of the zombie villager"));
	public static String buttonSlimeSize = c("&aChoose slime size");
	public static List<String> buttonSlimeSizeLore = c(Arrays.asList(
			"Cycles the slime's size.",
			"Current size: &e{size}"));
	public static String buttonMagmaCubeSize = c("&aChoose magma cube size");
	public static List<String> buttonMagmaCubeSizeLore = c(Arrays.asList(
			"Cycles the magma cube's size.",
			"Current size: &e{size}"));
	public static String buttonSnowmanPumpkinHead = c("&aToggle pumpkin head");
	public static List<String> buttonSnowmanPumpkinHeadLore = c(Arrays.asList(
			"Toggles the snowman's",
			"pumpkin head"));
	public static String buttonShulkerColor = c("&aChoose shulker color");
	public static List<String> buttonShulkerColorLore = c(Arrays.asList(
			"Changes the color",
			"of the shulker"));
	public static String buttonAxolotlVariant = c("&aChoose axolotl variant");
	public static List<String> buttonAxolotlVariantLore = c(Arrays.asList(
			"Changes the axolotl's look"));
	public static String buttonGlowSquidDark = c("&aToggle glow");
	public static List<String> buttonGlowSquidDarkLore = c(Arrays.asList(
			"Toggles the glow squid's",
			"glow on and off"));
	public static String buttonGoatScreaming = c("&aToggle screaming goat");
	public static List<String> buttonGoatScreamingLore = c(Arrays.asList(
			"Toggles between a normal",
			"and a screaming goat"));
	public static String buttonGoatLeftHorn = c("&aToggle left horn");
	public static List<String> buttonGoatLeftHornLore = c(Arrays.asList(
			"Toggles the goat's",
			"left horn"));
	public static String buttonGoatRightHorn = c("&aToggle right horn");
	public static List<String> buttonGoatRightHornLore = c(Arrays.asList(
			"Toggles the goat's",
			"right horn"));
	public static String buttonTropicalFishPattern = c("&aChoose variant");
	public static List<String> buttonTropicalFishPatternLore = c(Arrays.asList(
			"Changes the shape and pattern",
			"of the tropical fish.",
			"Currently: &e{pattern}"));
	public static String buttonTropicalFishBodyColor = c("&aChoose body color");
	public static List<String> buttonTropicalFishBodyColorLore = c(Arrays.asList(
			"Changes the body color",
			"of the tropical fish"));
	public static String buttonTropicalFishPatternColor = c("&aChoose pattern color");
	public static List<String> buttonTropicalFishPatternColorLore = c(Arrays.asList(
			"Changes the pattern color",
			"of the tropical fish"));
	public static String buttonPufferFishPuffState = c("&aChoose puff state");
	public static List<String> buttonPufferFishPuffStateLore = c(Arrays.asList(
			"Changes the puff state",
			"of the puffer fish.",
			"Currently: &e{puffState}"));
	public static String buttonSalmonVariant = c("&aChoose variant");
	public static List<String> buttonSalmonVariantLore = c(Arrays.asList(
			"Changes the size",
			"of the salmon."));
	public static String buttonFrogVariant = c("&aChoose frog variant");
	public static List<String> buttonFrogVariantLore = c(Arrays.asList(
			"Changes the frog's look"));
	public static String buttonWolfVariant = c("&aChoose wolf variant");
	public static List<String> buttonWolfVariantLore = c(Arrays.asList(
			"Changes the wolf's look"));
	public static String buttonSaddle = c("&aToggle saddle");
	public static List<String> buttonSaddleLore = c(Arrays.asList(
			"Toggles the mob's saddle"));
	public static String buttonCopperGolemWeatherState = c("&aChoose oxidation level");
	public static List<String> buttonCopperGolemWeatherStateLore = c(Arrays.asList(
			"Changes the copper golem's",
			"oxidation level"));
	public static String buttonArmorStandBasePlate = c("&aToggle base plate");
	public static List<String> buttonArmorStandBasePlateLore = c(Arrays.asList(
			"Toggles the base plate",
			"of the armor stand"));
	public static String buttonArmorStandShowArms = c("&aToggle arms");
	public static List<String> buttonArmorStandShowArmsLore = c(Arrays.asList(
			"Toggles the arms",
			"of the armor stand"));
	public static String buttonArmorStandSmall = c("&aToggle size");
	public static List<String> buttonArmorStandSmallLore = c(Arrays.asList(
			"Toggles the size",
			"of the armor stand"));
	public static String buttonMannequinMainHand = c("&aToggle main hand");
	public static List<String> buttonMannequinMainHandLore = c(Arrays.asList(
			"Toggles the main hand",
			"of the mannequin"));
	public static String buttonMannequinPose = c("&aToggle pose");
	public static List<String> buttonMannequinPoseLore = c(Arrays.asList(
			"Toggles the pose",
			"of the mannequin"));
	public static String buttonMannequinProfile = c("&aChange skin");
	public static List<String> buttonMannequinProfileLore = c(Arrays.asList(
			"Changes the skin",
			"of the mannequin"));
	public static String buttonNautilusArmor = c("&aChoose armor");
	public static List<String> buttonNautilusArmorLore = c(Arrays.asList(
			"Changes the armor",
			"of the nautilus"));
	public static String buttonZombieNautilusVariant = c("&aChoose variant");
	public static List<String> buttonZombieNautilusVariantLore = c(Arrays.asList(
			"Changes the look of",
			"the zombie nautilus."));
	public static String buttonEndCrystalBottomSlab = c("&aToggle bottom slab");
	public static List<String> buttonEndCrystalBottomSlabLore = c(Arrays.asList(
			"Toggles the bottom slab",
			"of the end crystal"));

	public static Text mannequinEnterProfile = Text.parse(
			"&aPlease enter in chat the name or id of the player whose profile you want to apply to"
					+ " the mannequin.\n"
					+ "&a  Enter a dash (-) to clear the current profile.\n"
					+ "&a  Enter an exclamation mark (!) to keep the current profile.");
	public static Text mannequinEnterProfileCanceled = Text.parse("&7Canceled: The mannequin's profile was not changed.");
	public static Text mannequinProfileSet = Text.parse("&aThe mannequin's profile has been set to '&e{profileName}&a'!");
	public static Text mannequinProfileCleared = Text.parse("&aThe mannequin's profile has been cleared!");
	public static Text mannequinProfileInvalid = Text.parse("&cFailed to fetch the profile for '&e{input}&c'!");

	public static String equipmentEditorTitle = c("Equipment Editor");
	public static List<String> equipmentSlotLore = c(Arrays.asList(
			"Place an item to equip it.",
			"Right-click to clear",
			"the equipment slot."));

	public static String equipmentSlotMainhand = c("&aMainhand");
	public static String equipmentSlotOffhand = c("&aOffhand");
	public static String equipmentSlotFeet = c("&aFeet");
	public static String equipmentSlotLegs = c("&aLegs");
	public static String equipmentSlotChest = c("&aChest");
	public static String equipmentSlotHead = c("&aHead");
	public static String equipmentSlotBody = c("&aBody");
	public static String equipmentSlotSaddle = c("&aSaddle");

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
	public static List<String> buttonHireLore = c(Arrays.asList(
			"Buy this shopkeeper"));

	public static String tradingTitlePrefix = c("&2");
	public static String tradingTitleDefault = c("Shopkeeper");

	public static Text mustTargetBlock = Text.parse("&7You must look at a block to place the shopkeeper.");
	public static Text missingSpawnLocation = Text.parse("&7You must specify a spawn location for this type of shop.");
	public static Text spawnBlockNotEmpty = Text.parse("&7The spawn location must be empty.");
	public static Text cannotSpawnMidair = Text.parse("&7The selected shop object cannot be placed in midair.");
	public static Text invalidSpawnBlockFace = Text.parse("&7The shopkeeper cannot be placed on this side of the block.");
	public static Text mobCannotSpawnOnPeacefulDifficulty = Text
			.parse("&7The selected mob type cannot spawn here on peaceful difficulty.");
	public static Text restrictedArea = Text.parse("&7You cannot place a shopkeeper in this area.");
	public static Text locationAlreadyInUse = Text.parse("&7This location is already used by another shopkeeper.");
	public static Text endCrystalDisabledInTheEnd = Text.parse("&7End crystal shops cannot be placed in the end.");
	public static Text cannotSpawn = Text.parse("&7The shopkeeper cannot spawn here.");

	public static Text containerSelected = Text
			.parse("&aContainer selected! Right-click a block to place your shopkeeper.");
	public static Text unsupportedContainer = Text.parse("&7This type of container cannot be used for shops.");
	public static Text mustSelectContainer = Text
			.parse("&7You must right-click a container before placing your shopkeeper.");
	public static Text mustTargetContainer = Text.parse("&7You must look at a container to place this type of shop.");
	public static Text invalidContainer = Text.parse("&7The selected block is not a valid container!");
	public static Text containerTooFarAway = Text.parse("&7The shopkeeper's container is too far away!");
	public static Text containerNotPlaced = Text.parse("&7You must select a container you have recently placed!");
	public static Text containerAlreadyInUse = Text
			.parse("&7Another shopkeeper is already using the selected container!");
	public static Text noContainerAccess = Text.parse("&7You cannot access the selected container!");
	public static Text tooManyShops = Text.parse("&7You have already reached the limit of how many shops you can own!");

	public static Text typeNewName = Text.parse("&aPlease enter the shop's new name in chat.\n"
			+ "  &aEnter a dash (-) to remove the current name.");
	public static Text nameSet = Text.parse("&aThe shop's name has been set to '&e{name}&a'!");
	public static Text nameHasNotChanged = Text.parse("&aThe shop's name has not changed.");
	public static Text nameInvalid = Text.parse("&cInvalid shop name: '&e{name}&c'");
	public static String nameplatePrefix = c("&2");

	// Members:
	public static Text typeMemberName = Text.parse("&aPlease enter the player name to add as a member in chat.\n"
			+ "  &aEnter a dash (-) to cancel.");
	public static Text memberAdded = Text.parse("&a'&e{member}&a' has been added as a member of this shop!");
	public static Text memberAlreadyAdded = Text.parse("&7'&e{member}&7' is already a member of this shop.");
	public static Text memberIsOwner = Text.parse("&7The owner cannot be added as a member.");
	public static Text memberRemoved = Text.parse("&a'&e{member}&a' has been removed from this shop's members.");
	public static Text memberNotFound = Text.parse("&7'&e{member}&7' is not a member of this shop.");
	public static Text memberLimitReached = Text.parse("&7This shop has reached the maximum number of members!");
	public static Text memberPlayerNotFound = Text.parse("&7Could not find player '&e{player}&7'.");
	public static Text membersListHeader = Text.parse("&6Members of this shop:");
	public static Text membersListEntry = Text.parse("&e  - {member}");
	public static Text membersListEmpty = Text.parse("&7This shop has no members.");
	public static Text typeRemoveMemberName = Text
			.parse("&aPlease enter the player name to remove from members in chat.\n"
					+ "  &aEnter a dash (-) to cancel.");

	public static Text clickNewShopLocation = Text.parse("&aPlease right-click the shop's new location.\n"
			+ "  &aLeft-click to abort.");
	public static Text shopkeeperMoved = Text.parse("&aThe shopkeeper has been moved!");
	public static Text shopkeeperMoveAborted = Text.parse("&7Shopkeeper move aborted.");

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
	public static Text shopCreationItemsGiven = Text
			.parse("&aPlayer &e{player}&a has received &e{amount}&a shop creation item(s)!");
	public static Text shopCreationItemsReceived = Text.parse("&aYou have received &e{amount}&a shop creation item(s)!");
	public static Text unknownCurrency = Text.parse("&cUnknown currency: '&e{currency}&c'");
	public static Text currencyItemsGiven = Text
			.parse("&aPlayer &e{player}&a has received &6{amount}x&a currency item '&e{currency}&a'!");
	public static Text currencyItemsReceived = Text
			.parse("&aYou have received &6{amount}x&a currency item '&e{currency}&a'!");
	public static Text mustHoldItemInMainHand = Text.parse("&7You must hold an item in your main hand.");
	public static Text currencyItemSetToMainHandItem = Text
			.parse("&aThe currency item '&e{currencyId}&a' has been set to the &eitem in your main hand&a!");
	public static Text itemsUpdated = Text.parse("&aUpdated &e{count}&a item(s)!");
	public static String unknownBookAuthor = c("Unknown");

	public static Text tradePermSet = Text.parse("&aThe shop's trading permission has been set to '&e{perm}&a'!");
	public static Text tradePermRemoved = Text.parse("&aThe shop's trading permission '&e{perm}&a' has been removed!");
	public static Text tradePermView = Text.parse("&aThe shop's current trading permission is '&e{perm}&a'.");

	public static Text tradedCommandSet = Text.parse("&aThe item's traded command has been set to '&e{command}&a'!");
	public static Text tradedCommandRemoved = Text.parse("&aThe item's traded command has been removed!");
	public static Text tradedCommandView = Text.parse("&aThe item's current traded command is '&e{command}&a'.");
	public static Text tradedCommandViewUnset = Text.parse("&aThe item has no traded command assigned.");

	public static Text zombieVillagerCuringDisabled = Text.parse("&7Curing of zombie villagers is disabled.");
	public static Text mustHoldHireItem = Text.parse("&7You have to hold the required hire item in your hand.");
	public static Text setForHire = Text.parse("&aThis shopkeeper can now be hired.");
	public static Text hired = Text.parse("&aYou have hired this shopkeeper!");
	public static Text missingHirePerm = Text.parse("&7You do not have the permission to hire shopkeepers.");
	public static Text cannotHire = Text.parse("&7You cannot afford to hire this shopkeeper.");
	public static Text cannotHireShopType = Text
			.parse("&7You do not have the permission to hire this type of shopkeeper.");
	// Placeholders: {costs}, {hire-item}
	public static Text villagerForHire = Text
			.parse("&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.");

	public static Text missingTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text missingCustomTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text shopCurrentlyClosed = Text.parse("&7This shop is currently closed. Check again later!");
	public static Text cannotTradeNoOffers = Text.parse("&7This shop currently has no offers. Check again later!");
	public static String noOffersOpenEditorDescription = c(
			"&eYou can edit this shop by right clicking it while sneaking.");
	public static Text cannotTradeWithOwnShop = Text.parse("&7You cannot trade with your own shop.");
	public static Text cannotTradeWhileOwnerOnline = Text
			.parse("&7You cannot trade while the owner of this shop ('&e{owner}&7') is online.");
	public static Text cannotTradeWithShopMissingContainer = Text
			.parse("&7You cannot trade with this shop, because its container is missing.");
	public static Text cannotTradeUnexpectedTrade = Text
			.parse("&7Trade aborted: The traded items do not match what this shopkeeper expected.");
	public static Text cannotTradeItemsNotStrictlyMatching = Text
			.parse("&7Trade aborted: The offered items do not exactly match the required items.");
	public static Text cannotTradeInsufficientStorageSpace = Text
			.parse("&7Trade aborted: This shop does not have enough storage space.");
	public static Text cannotTradeInsufficientCurrency = Text
			.parse("&7Trade aborted: This shop does not have enough currency.");
	public static Text cannotTradeInsufficientStock = Text
			.parse("&7Trade aborted: This shop does not have enough of the traded item.");
	public static Text cannotTradeInsufficientWritableBooks = Text
			.parse("&7Trade aborted: This book shop lacks writable books to copy the book with.");

	// Trade placeholders: {player}, {playerId}, {resultItem}, {resultItemAmount},
	// {item1},
	// {item1Amount}, {item2}, {item2Amount}, {shop} (replaced by admin or player
	// shop text
	// respectively), {trade_count} (replaced by the trade count message, or empty
	// if the trade
	// count is one).
	// Shop placeholders: {shop_id}, {shop_uuid}, {shop_name}, {shop_world},
	// {shop_x}, {shop_y},
	// {shop_z}, {shop_location} ('world,x,y,z' or '[virtual]'), {shop_type},
	// {shop_object_type},
	// {shop_owner_name}, {shop_owner_uuid}. World name and coordinates are empty
	// for virtual shops.
	public static Text tradeNotificationOneItem = Text.parse(
			"&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text tradeNotificationTwoItems = Text.parse(
			"&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] [&6{item2Amount}x &a{item2}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text buyNotificationOneItem = Text.parse(
			"&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text buyNotificationTwoItems = Text.parse(
			"&7Trade: &e{player}&7 [&6{item1Amount}x &a{item1}&7] [&6{item2Amount}x &a{item2}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count}");
	public static Text tradeNotificationPlayerShop = Text.parse("&e{shop_owner_name}");
	public static Text tradeNotificationNamedPlayerShop = Text.parse("&e{shop_owner_name}");
	public static Text tradeNotificationAdminShop = Text.parse("&eAdmin Shop");
	public static Text tradeNotificationNamedAdminShop = Text.parse("&e\"{shop_name}\"");
	public static Text tradeNotificationTradeCount = Text.parse("&7 (&6{count}x&7)");

	// Placeholders: Same as the general trade notification messages.
	public static Text ownerTradeNotificationOneItem = Text
			.parse("&e{player}&7 bought &6{resultItemAmount}x &a{resultItem}&7 from {shop}{trade_count}");
	public static Text ownerTradeNotificationTwoItems = Text
			.parse("&e{player}&7 bought &6{resultItemAmount}x &a{resultItem}&7 from {shop}{trade_count}");
	public static Text ownerBuyNotificationOneItem = Text
			.parse("&e{player}&7 sold &6{item1Amount}x &a{item1}&7 to {shop}{trade_count}");
	public static Text ownerBuyNotificationTwoItems = Text
			.parse("&e{player}&7 sold &6{item1Amount}x &a{item1}&7 and &6{item2Amount}x &a{item2}&7 to {shop}{trade_count}");
	public static Text ownerTradeNotificationShop = Text.parse("one of your shops");
	public static Text ownerTradeNotificationNamedShop = Text.parse("your shop &e\"{shop_name}\"");
	public static Text ownerBuyNotificationShop = Text.parse("one of your shops");
	public static Text ownerBuyNotificationNamedShop = Text.parse("your shop &e\"{shop_name}\"");
	public static Text ownerTradeNotificationTradeCount = Text.parse("&7 (&6{count}x&7)");

	public static Text disableTradeNotificationsHint = Text
			.parse("&7You can disable these trade notifications with the command &e{command}");
	public static Text disableTradeNotificationsHintCommand = Text.parse("/shopkeeper notify trades");
	public static Text tradeNotificationsDisabled = Text
			.parse("&aYou will no longer receive trade notifications during this game session.");
	public static Text tradeNotificationsEnabled = Text.parse("&aYou will now receive trade notifications again.");

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
	public static List<String> tradeSetupDescAdminRegular = c(Arrays.asList(
			"Has unlimited stock.",
			"Insert items from your inventory.",
			"Top row: Result items",
			"Bottom rows: Cost items"));
	public static List<String> tradeSetupDescSelling = c(Arrays.asList(
			"Sells items to players.",
			"Insert items to sell into the container.",
			"Left/Right click to adjust amounts.",
			"Top row: Items being sold",
			"Bottom rows: Cost items"));
	public static List<String> tradeSetupDescBuying = c(Arrays.asList(
			"Buys items from players.",
			"Insert one of each item you want to",
			"buy and plenty of currency items",
			"into the container.",
			"Left/Right click to adjust amounts.",
			"Top row: Cost items",
			"Bottom row: Items being bought"));
	public static List<String> tradeSetupDescTrading = c(Arrays.asList(
			"Trades items with players.",
			"Pickup an item from your inventory",
			"and then click a slot to place it.",
			"Left/Right click to adjust amounts.",
			"Top row: Result items",
			"Bottom rows: Cost items"));
	public static List<String> tradeSetupDescBook = c(Arrays.asList(
			"Sells book copies.",
			"Insert written and blank books",
			"into the container.",
			"Left/Right click to adjust costs.",
			"Top row: Books being sold",
			"Bottom rows: Cost items"));

	public static String sellingShop_emptyTrade_resultItem = c("&dSell Item");
	public static List<String> sellingShop_emptyTrade_resultItemLore = c(Arrays.asList(
			"The item you want to sell.",
			"Add items to the shop container.",
			"Left/Right click to adjust the amount."));
	public static String sellingShop_emptyTrade_item1 = c("&dBuy Item");
	public static List<String> sellingShop_emptyTrade_item1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Left/Right click to adjust the amount."));
	public static String sellingShop_emptyTrade_item2 = c("&dBuy Item 2");
	public static List<String> sellingShop_emptyTrade_item2Lore = c(Arrays.asList(
			"The second item you want to buy.",
			"Left/Right click to adjust the amount."));
	public static String sellingShop_emptyItem1 = c("&dBuy Item");
	public static List<String> sellingShop_emptyItem1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Left/Right click to adjust the amount."));
	public static String sellingShop_emptyItem2 = c("&dBuy Item 2");
	public static List<String> sellingShop_emptyItem2Lore = c(Arrays.asList(
			"The second item you want to buy.",
			"Left/Right click to adjust the amount."));

	public static String buyingShop_emptyTrade_resultItem = c("&dSell Item");
	public static List<String> buyingShop_emptyTrade_resultItemLore = c(Arrays.asList(
			"The item you want to sell.",
			"Left/Right click to adjust the amount."));
	public static String buyingShop_emptyTrade_item1 = c("&dBuy Item");
	public static List<String> buyingShop_emptyTrade_item1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Add items to the shop container.",
			"Left/Right click to adjust the amount."));
	public static String buyingShop_emptyResultItem = c("&dSell Item");
	public static List<String> buyingShop_emptyResultItemLore = c(Arrays.asList(
			"The item you want to sell.",
			"Left/Right click to adjust the amount."));

	public static String tradingShop_emptyTrade_resultItem = c("&dSell Item");
	public static List<String> tradingShop_emptyTrade_resultItemLore = c(Arrays.asList(
			"The item you want to sell.",
			"Add items to the shop container,",
			"or place an item from your inventory.",
			"Left/Right click to adjust the amount."));
	public static String tradingShop_emptyTrade_item1 = c("&dBuy Item");
	public static List<String> tradingShop_emptyTrade_item1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Place an item from your inventory.",
			"Left/Right click to adjust the amount."));
	public static String tradingShop_emptyTrade_item2 = c("&dBuy Item 2");
	public static List<String> tradingShop_emptyTrade_item2Lore = c(Arrays.asList(
			"The second item you want to buy.",
			"Place an item from your inventory.",
			"Left/Right click to adjust the amount."));
	public static String tradingShop_emptyResultItem = c("&dSell Item");
	public static List<String> tradingShop_emptyResultItemLore = c(Arrays.asList(
			"The item you want to sell.",
			"Place an item from your inventory.",
			"Left/Right click to adjust the amount."));
	public static String tradingShop_emptyItem1 = c("&dBuy Item");
	public static List<String> tradingShop_emptyItem1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Place an item from your inventory.",
			"Left/Right click to adjust the amount."));
	public static String tradingShop_emptyItem2 = c("&dBuy Item 2");
	public static List<String> tradingShop_emptyItem2Lore = c(Arrays.asList(
			"The second item you want to buy.",
			"Place an item from your inventory.",
			"Left/Right click to adjust the amount."));

	public static String bookShop_emptyTrade_resultItem = c("&dSell Item");
	public static List<String> bookShop_emptyTrade_resultItemLore = c(Arrays.asList(
			"The item you want to sell.",
			"Add written books to the shop container."));
	public static String bookShop_emptyTrade_item1 = c("&dBuy Item");
	public static List<String> bookShop_emptyTrade_item1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Left/Right click to adjust the amount."));
	public static String bookShop_emptyTrade_item2 = c("&dBuy Item 2");
	public static List<String> bookShop_emptyTrade_item2Lore = c(Arrays.asList(
			"The second item you want to buy.",
			"Left/Right click to adjust the amount."));
	public static String bookShop_emptyItem1 = c("&dBuy Item");
	public static List<String> bookShop_emptyItem1Lore = c(Arrays.asList(
			"The item you want to buy.",
			"Left/Right click to adjust the amount."));
	public static String bookShop_emptyItem2 = c("&dBuy Item 2");
	public static List<String> bookShop_emptyItem2Lore = c(Arrays.asList(
			"The second item you want to buy.",
			"Left/Right click to adjust the amount."));

	public static String shopInformationHeader = c("&6Details");
	public static List<String> shopInformation = c(Arrays.asList(
			"Id: &e{shop_id}",
			"Unique id: &e{shop_uuid}",
			"Name: &e{shop_name}",
			"Type: &e{shop_type}",
			"Object: &e{shop_object_type}",
			"Location: &e{shop_location}"));
	public static List<String> playerShopInformation = c(Arrays.asList(
			"Owner: &e{shop_owner_name} &8({shop_owner_uuid})"));

	public static Text missingEditVillagersPerm = Text.parse("&7You do not have the permission to edit villagers.");
	public static Text missingEditWanderingTradersPerm = Text
			.parse("&7You do not have the permission to edit wandering traders.");
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
			"uses and no XP rewards."));

	public static String villagerEditorInformationHeader = c("&6Details");
	public static List<String> villagerEditorInformation = c(Arrays.asList(
			"Id: &e{entity_id}",
			"Unique id: &e{entity_uuid}",
			"Name: &e{entity_name}",
			"Location: &e{entity_location}"));

	public static String buttonDeleteVillager = c("&4Delete");
	public static List<String> buttonDeleteVillagerLore = c(Arrays.asList(
			"Deletes the villager"));
	public static String buttonNameVillager = c("&aSet villager name");
	public static List<String> buttonNameVillagerLore = c(Arrays.asList(
			"Lets you rename",
			"the villager"));
	public static String buttonVillagerInventory = c("&aView villager inventory");
	public static List<String> buttonVillagerInventoryLore = c(Arrays.asList(
			"Lets you view a copy of",
			"the villager's inventory"));
	public static String buttonMobAi = c("&aToggle mob AI");
	public static List<String> buttonMobAiLore = c(Arrays.asList(
			"Toggles the mob's AI"));
	public static String buttonInvulnerability = c("&aToggle invulnerability");
	public static List<String> buttonInvulnerabilityLore = c(Arrays.asList(
			"Toggles the mob's",
			"invulnerability.",
			"Players in creative mode",
			"can still damage the mob."));

	public static String villagerInventoryTitle = c("Villager inventory (copy)");
	public static String setVillagerXp = c("&aSet the villager's XP to &e{xp}");
	public static String noVillagerTradesChanged = c("&aNo trades have been changed.");
	public static String villagerTradesChanged = c("&e{changedTrades}&a trades have been changed.");

	public static Text typeNewVillagerName = Text.parse("&aPlease type the villager's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.");
	public static Text villagerNameSet = Text.parse("&aThe villager's name has been set!");
	public static Text villagerNameInvalid = Text.parse("&aThat name is not valid!");

	public static Text noShopsFound = Text.parse("&7No shops were found.");

	public static Text teleportVirtualShopkeeper = Text.parse("&7Cannot teleport to a virtual shopkeeper!");
	public static Text teleportShopkeeperWorldNotLoaded = Text
			.parse("&7Cannot teleport to a shopkeeper in an unloaded world!");
	public static Text teleportNoSafeLocationFound = Text.parse("&7Could not find a safe location to teleport to!");
	public static Text teleportFailed = Text.parse("&7The teleport failed!");
	public static Text teleportSuccess = Text.parse("&aPlayer '&e{player}&a' was teleported to shopkeeper '&e{shop}&a'!");

	public static Text listAdminShopsHeader = Text
			.parse("&9There are &e{shopsCount} &9admin shops: &e(Page {page} of {maxPage})");
	public static Text listAllShopsHeader = Text
			.parse("&9There are &e{shopsCount} &9shops in total: &e(Page {page} of {maxPage})");
	public static Text listPlayerShopsHeader = Text
			.parse("&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page} of {maxPage})");
	public static Text listShopsEntry = Text
			.parse("  &e{shopId}) &7{shopName}&r&8at &7({location})&8, type: &7{shopType}&8, object: &7{objectType}");

	public static Text shopRemoved = Text.parse("&aThe shopkeeper has been removed.");
	public static Text shopAlreadyRemoved = Text.parse("&7The shopkeeper has already been removed.");
	public static Text shopNoLongerExists = Text.parse("&7The shopkeeper no longer exists.");
	public static Text shopRemovalCancelled = Text.parse("&cA plugin has prevented the removal of the shopkeeper.");

	public static Text shopsAlreadyRemoved = Text.parse("&e{shopsCount}&7 of the shops have already been removed.");
	public static Text shopRemovalsCancelled = Text
			.parse("&cPlugins have prevented the removal of &e{shopsCount}&c of the shops.");
	public static Text adminShopsRemoved = Text.parse("&e{shopsCount} &aadmin shops have been removed.");
	public static Text shopsOfPlayerRemoved = Text
			.parse("&e{shopsCount} &ashops of player '&e{player}&a' have been removed.");
	public static Text playerShopsRemoved = Text.parse("&e{shopsCount} &aplayer shops have been removed.");

	public static Text confirmRemoveShop = Text.parse("&cYou are about to irrevocably remove the specified shopkeeper!");
	public static Text confirmRemoveAllAdminShops = Text
			.parse("&cYou are about to irrevocably remove all admin shops (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllOwnShops = Text
			.parse("&cYou are about to irrevocably remove all your shops (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllShopsOfPlayer = Text
			.parse("&cYou are about to irrevocably remove all shops of player &6{player}&c (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllPlayerShops = Text
			.parse("&cYou are about to irrevocably remove all player shops of all players (&6{shopsCount}&c)!");

	public static Text confirmReplaceAllShopsWithVanillaVillagers = Text.parse(
			"&cYou are about to irrevocably delete all shops (&6{shopsCount}&c) and replace them with vanilla villagers without AI!\n"
					+ "Consider creating a backup of your world and shopkeeper data before you proceed.");
	public static Text allShopsReplacedWithVanillaVillagers = Text.parse(
			"&e{adminShopsCount}&a admin shops and &e{playerShopsCount}&a player shops have been replaced with vanilla villagers!\n"
					+ "&e{skippedShopsCount}&a shops have been skipped.");

	public static Text confirmationRequired = Text.parse("&7Please confirm this action by typing &6/shopkeepers confirm");
	public static Text confirmationExpired = Text.parse("&cConfirmation expired.");
	public static Text nothingToConfirm = Text.parse("&cThere is nothing to confirm currently.");

	public static String confirmationUiDeleteShopTitle = c("&cReally delete this shop?");
	public static List<String> confirmationUiDeleteShopConfirmLore = c(Arrays.asList(
			"This will irrevocably",
			"remove this shop!"));

	public static String confirmationUiDeleteVillagerTitle = c("&cReally delete this villager?");
	public static List<String> confirmationUiDeleteVillagerConfirmLore = c(Arrays.asList(
			"This will irrevocably",
			"remove this villager!"));
	public static Text villagerRemoved = Text.parse("&aThe villager has been removed.");

	public static String confirmationUiConfirm = c("&2Confirm");
	public static String confirmationUiCancel = c("&4Cancel");
	public static List<String> confirmationUiCancelLore = c(Arrays.asList(
			"This will abort the",
			"current action."));
	public static Text confirmationUiAborted = Text.parse("&7Confirmation aborted.");

	public static Text snapshotListHeader = Text
			.parse("&9Shop &e{shop_id} &9has &e{snapshotsCount} &9snapshots: &e(Page {page} of {maxPage})");
	public static Text snapshotListEntry = Text.parse("  &e{id}) &2{name}&8 (&7{timestamp}&8)");
	public static Text invalidSnapshotId = Text.parse("&cInvalid snapshot id: &e{argument}");
	public static Text invalidSnapshotName = Text.parse("&cNo snapshot found with name '&e{argument}&c'.");
	public static Text snapshotNameTooLong = Text
			.parse("&cThe snapshot name can be a maximum of &e{maxLength}&c characters long: &e{name}");
	public static Text snapshotNameInvalid = Text.parse("&cInvalid snapshot name: &e{name}");
	public static Text snapshotNameAlreadyExists = Text
			.parse("&cThere already exists another snapshot with this name: &e{name}");
	public static Text snapshotCreated = Text.parse("&aNew snapshot created: &e({id}) &2{name} &8(&7{timestamp}&8)");
	public static Text confirmRemoveSnapshot = Text
			.parse("&cYou are about to irrevocably delete the specified snapshot!");
	public static Text confirmRemoveAllSnapshots = Text
			.parse("&cYou are about to irrevocably delete all snapshots of the specified shopkeeper (&6{snapshotsCount}&c)!");
	public static Text actionAbortedSnapshotsChanged = Text
			.parse("&cAction aborted! The snapshots have changed in the meantime. Try again.");
	public static Text snapshotRemoved = Text.parse("&aSnapshot deleted: &e({id}) &2{name} &8(&7{timestamp}&8)");
	public static Text snapshotRemovedAll = Text
			.parse("&aAll &e{snapshotsCount} &asnapshots of shop &e{shop_id}&a have been deleted.");
	public static Text snapshotRestoreFailed = Text
			.parse("&cFailed to restore snapshot: &e({id}) &2{name} &8(&7{timestamp}&8)");
	public static Text snapshotRestored = Text.parse("&aSnapshot restored: &e({id}) &2{name} &8(&7{timestamp}&8)");

	public static Text historyHeader = Text
			.parse("&9Trades of {players}&9 with {shops}&9: &e{tradesCount} &e(Page {page} of {maxPage})");
	public static Text historyHeaderAllPlayers = Text.parse("&eall players");
	public static Text historyHeaderSpecificPlayer = Text.parse("&e{player}");
	public static Text historyHeaderAllShops = Text.parse("&eall shops");
	public static Text historyHeaderAdminShops = Text.parse("all &eadmin shops");
	public static Text historyHeaderPlayerShops = Text.parse("all &eplayer shops");
	public static Text historyHeaderAllOwnedShops = Text.parse("shops owned by &e{owner}");
	public static Text historyHeaderSpecificShop = Text.parse("shop &e\"{shop}\"");
	public static Text historyHeaderSpecificOwnedShop = Text.parse("shop &e\"{shop}\"&9 owned by &e{owner}");

	public static Text historyDisabled = Text.parse("&7The trading history is disabled.");
	public static Text historyNoTradesFound = Text.parse("&7No trades found.");

	public static Text historyEntryOneItem = Text.parse(
			"  &f{index}) &e{player}&7 [&6{item1Amount}x &a{item1}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count} &7(&f{timeAgo} ago&7)");
	public static Text historyEntryTwoItems = Text.parse(
			"  &f{index}) &e{player}&7 [&6{item1Amount}x &a{item1}&7] [&6{item2Amount}x &a{item2}&7] \u279e [&6{resultItemAmount}x &a{resultItem}&7] {shop}{trade_count} &7(&f{timeAgo} ago&7)");
	public static Text historyEntryPlayerShop = Text.parse("&e{owner}");
	public static Text historyEntryAdminShop = Text.parse("&eAdmin Shop");
	public static Text historyEntryTradeCount = Text.parse("&7 (&6{count}x&7)");

	public static Text noPermission = Text.parse("&cYou don't have the permission to do that.");
	public static Text commandCreateNoPermission = Text.parse("&cYou don't have the permission to do that.\n"
			+ "&eIf you are trying to create a player shop, use the shop creation item instead.");

	public static Text commandUnknown = Text.parse("&cUnknown command '&e{command}&c'!");
	public static Text commandArgumentUnexpected = Text.parse("&cUnexpected argument '&e{argument}&c'.");
	public static Text commandArgumentRequiresPlayer = Text
			.parse("&cArgument '&e{argumentFormat}&c' requires a player to execute the command.");
	public static Text commandArgumentMissing = Text.parse("&cMissing argument '&e{argumentFormat}&c'.");
	public static Text commandArgumentInvalid = Text.parse("&cInvalid argument '&e{argument}&c'.");
	public static Text commandPlayerArgumentMissing = Text.parse("&cNo player specified for '&e{argumentFormat}&c'.");
	public static Text commandPlayerArgumentInvalid = Text.parse("&cNo player found for '&e{argument}&c'.");
	public static Text commandShopTypeArgumentInvalid = Text.parse("&cUnknown shop type '&e{argument}&c'.");
	public static Text commandShopTypeArgumentNoAdminShop = Text.parse("&cShop type '&e{argument}&c' is no admin shop.");
	public static Text commandShopTypeArgumentNoPlayerShop = Text
			.parse("&cShop type '&e{argument}&c' is no player shop.");
	public static Text commandShopObjectTypeArgumentInvalid = Text.parse("&cUnknown shop object type '&e{argument}&c'.");
	public static Text commandShopkeeperArgumentInvalid = Text.parse("&cNo shopkeeper found for '&e{argument}&c'.");
	public static Text commandShopkeeperArgumentNoAdminShop = Text
			.parse("&cShopkeeper '&e{argument}&c' is no admin shopkeeper.");
	public static Text commandShopkeeperArgumentNoPlayerShop = Text
			.parse("&cShopkeeper '&e{argument}&c' is no player shopkeeper.");
	public static Text commandShopkeeperArgumentNoAccess = Text
			.parse("&cYou do not have access to shopkeeper '&e{argument}&c'.");
	public static Text commandEntityArgumentNoVillager = Text.parse("&cEntity '&e{argument}&c' is no regular villager.");

	public static Text ambiguousPlayerName = Text.parse("&cThere are multiple players for the name '&e{name}&c'!");
	public static Text ambiguousPlayerNameEntry = Text.parse("&c  - '&e{name}&r&c' (&6{uuid}&c)");
	public static Text ambiguousPlayerNameMore = Text.parse("&c  - ....");

	public static Text ambiguousShopkeeperName = Text
			.parse("&cThere are multiple shopkeepers for the name '&e{name}&c'!");
	public static Text ambiguousShopkeeperNameEntry = Text.parse("&c  - &e{id})&c '&e{name}&r&c' (&6{uuid}&c)");
	public static Text ambiguousShopkeeperNameMore = Text.parse("&c  - ....");

	public static Text ambiguousTargetShopkeeper = Text.parse("&cThere are multiple shopkeepers being targeted!");
	public static Text ambiguousTargetShopkeeperEntry = Text.parse("&c  - &e{id})&c '&e{name}&r&c' (&6{uuid}&c)");
	public static Text ambiguousTargetShopkeeperMore = Text.parse("&c  - ....");

	public static Text commandHelpTitle = Text.parse("&9***** &8[&6Shopkeepers v{version}&8] &9*****");
	public static Text commandHelpUsageFormat = Text.parse("&e{usage}");
	public static Text commandHelpDescriptionFormat = Text.parse("&8 - &3{description}");

	public static Text commandDescriptionShopkeeper = Text.parse("Creates a shopkeeper.");
	public static Text commandDescriptionHelp = Text.parse("Shows this help page.");
	public static Text commandDescriptionReload = Text.parse("Reloads this plugin.");
	public static Text commandDescriptionDebug = Text.parse("Toggles debug mode on and off.");
	public static Text commandDescriptionNotify = Text.parse("Turns trade notifications for you on or off.");
	public static Text commandDescriptionList = Text.parse("Lists all shops of a specific player, or all admin shops.");
	public static Text commandDescriptionHistory = Text.parse("Shows the trading history.");
	public static Text commandDescriptionRemove = Text.parse("Removes a specific shop.");
	public static Text commandDescriptionRemoveAll = Text
			.parse("Removes all shops of a specific player, all players, or all admin shops.");
	public static Text commandDescriptionGive = Text.parse("Gives shop creation item(s) to the specified player.");
	public static Text commandDescriptionGiveCurrency = Text.parse("Gives currency item(s) to the specified player.");
	public static Text commandDescriptionSetCurrency = Text.parse("Changes the currency item to the item held in hand.");
	public static Text commandDescriptionUpdateItems = Text.parse("Updates third-party items (if they support it).");
	public static Text commandDescriptionRemote = Text.parse("Remotely opens a shop (Optionally: For another player).");
	public static Text commandDescriptionRemoteEdit = Text.parse("Remotely edits a shop.");
	public static Text commandDescriptionTransfer = Text.parse("Transfers the ownership of a shop.");
	public static Text commandDescriptionTeleport = Text.parse("Teleports to a shop.");
	public static Text commandDescriptionSettradeperm = Text
			.parse("Sets, removes (-) or displays (?) the trading permission.");
	public static Text commandDescriptionSettradedcommand = Text
			.parse("Sets, removes (-) or displays (?) the traded command of the held item.");
	public static Text commandDescriptionSetforhire = Text.parse("Sets one of your shops for sale.");
	public static Text commandDescriptionSnapshotList = Text.parse("Lists the snapshots of a shop.");
	public static Text commandDescriptionSnapshotCreate = Text.parse("Creates a new shop snapshot.");
	public static Text commandDescriptionSnapshotRemove = Text.parse("Removes a specific or all snapshots of a shop.");
	public static Text commandDescriptionSnapshotRestore = Text.parse("Restores a specific shop snapshot.");
	public static Text commandDescriptionEditVillager = Text.parse("Opens the editor for the target villager.");
	public static Text commandDescriptionReplaceAllWithVanillaVillagers = Text
			.parse("Replaces all shopkeepers with vanilla villagers without AI.");

	/////

	private static final String LANG_FOLDER = "lang";
	public static final String DEFAULT_LANGUAGE = "en-default";

	public static File getLangFolder() {
		return new File(SKShopkeepersPlugin.getInstance().getDataFolder(), LANG_FOLDER);
	}

	private static String getLanguageFileName(String language) {
		return "language-" + language + ".yml";
	}

	// Relative to the plugin's data folder:
	public static String getLanguageFilePath(String language) {
		return LANG_FOLDER + "/" + getLanguageFileName(language);
	}

	public static String getDefaultLanguageFilePath() {
		return getLanguageFilePath(DEFAULT_LANGUAGE);
	}

	private static File getLanguageFile(String language) {
		String languageFilePath = getLanguageFilePath(language);
		return new File(SKShopkeepersPlugin.getInstance().getDataFolder(), languageFilePath);
	}

	/**
	 * The default language file is freshly written on every startup and overwrites
	 * the already
	 * existing default language file.
	 */
	private static void saveDefaultLanguageFile() {
		String languageFilePath = getDefaultLanguageFilePath();
		SKShopkeepersPlugin plugin = SKShopkeepersPlugin.getInstance();
		assert plugin.getResource(languageFilePath) != null;
		plugin.saveResource(languageFilePath, true);
	}

	private static @Nullable ConfigData loadLanguageFile(String language) {
		SKShopkeepersPlugin plugin = SKShopkeepersPlugin.getInstance();

		// Create language file if it is missing and there exists a default:
		String languageFilePath = getLanguageFilePath(language);
		File languageFile = getLanguageFile(language);
		if (!languageFile.exists() && plugin.getResource(languageFilePath) != null) {
			plugin.saveResource(languageFilePath, false);
		}

		// Load language file:
		if (!languageFile.exists()) {
			Log.warning("Could not find language file '" + languageFile.getName() + "'!");
			return null;
		}

		Log.info("Loading language file: " + languageFile.getName());
		try {
			// Load the language config:
			DataStore languageConfig = BukkitConfigDataStore.ofNewYamlConfig();
			languageConfig.load(languageFile);
			return ConfigData.of(languageConfig);
		} catch (Exception e) {
			Log.warning("Could not load language file '" + languageFile.getName() + "'!", e);
			return null;
		}
	}

	private static @Nullable ConfigData loadDefaultLanguageFile() {
		return loadLanguageFile(DEFAULT_LANGUAGE);
	}

	public static void loadLanguageFile() {
		// Create the default language file, updating any existing one:
		saveDefaultLanguageFile();

		// Load the default language file (used for defaults when values are missing):
		var defaultLanguageConfigData = loadDefaultLanguageFile();
		if (defaultLanguageConfigData == null) {
			// Failed to load the default language file (already logged elsewhere):
			return;
		}

		String language = Settings.language;

		ConfigData languageConfigData = defaultLanguageConfigData;
		if (!language.equals(DEFAULT_LANGUAGE)) {
			// Else: No need to load the default language file again.
			languageConfigData = loadLanguageFile(language);
			if (languageConfigData == null) {
				// Failed to load the language file (already logged elsewhere):
				return;
			}

			// Use the default language file as defaults:
			languageConfigData.setDefaults(defaultLanguageConfigData);
		}

		// Load messages:
		try {
			INSTANCE.load(languageConfigData);

			// Also update the derived settings:
			Settings.onSettingsChanged();
		} catch (Exception e) {
			Log.warning("Could not load language file for language '" + language + "'!", e);
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
	public String getLogPrefix() {
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
		return this.getLogPrefix() + "Could not load message '" + configKey + "': "
				+ e.getMessage();
	}

	@Override
	protected String msgDefaultValueLoadException(String configKey, ValueLoadException e) {
		return this.getLogPrefix() + "Could not load default value for message '" + configKey
				+ "': " + e.getMessage();
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
	public void load(ConfigData configData) throws ConfigLoadException {
		Validate.notNull(configData, "configData is null");
		// Check for unexpected (possibly no longer existing) message keys:
		Set<? extends String> configKeys = configData.getKeys();
		for (String configKey : configKeys) {
			if (this.getSetting(configKey) == null) {
				Log.warning(this.getLogPrefix() + "Unknown message: " + configKey);
			}
		}

		// Load the config:
		super.load(configData);
	}
}
