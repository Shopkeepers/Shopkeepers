package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.text.Text;

/**
 * Utility functions related to shopkeepers and trading.
 */
public class ShopkeeperUtils {

	private ShopkeeperUtils() {
	}

	private static final int SHOPKEEPER_TARGET_RANGE = 10;

	public static TradingRecipe getSelectedTradingRecipe(MerchantInventory merchantInventory) {
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		return createTradingRecipe(merchantRecipe);
	}

	public static TradingRecipe createTradingRecipe(MerchantRecipe merchantRecipe) {
		if (merchantRecipe == null) return null;
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		ItemStack item1 = ingredients.get(0);
		ItemStack item2 = null;
		if (ingredients.size() > 1) {
			ItemStack buyItem2 = ingredients.get(1);
			if (!ItemUtils.isEmpty(buyItem2)) {
				item2 = buyItem2;
			}
		}
		ItemStack resultItem = merchantRecipe.getResult();
		return ShopkeepersAPI.createTradingRecipe(resultItem, item1, item2);
	}

	// note: this method considers the recipes equal even if their uses and max uses don't match
	public static boolean areMerchantRecipesEqual(MerchantRecipe recipe1, MerchantRecipe recipe2) {
		if (recipe1 == recipe2) return true;
		if (recipe1 == null) return false;
		if (recipe2 == null) return false;

		if (!recipe1.getResult().equals(recipe2.getResult())) return false;
		boolean outOfStock1 = (recipe1.getUses() >= recipe1.getMaxUses());
		boolean outOfStock2 = (recipe2.getUses() >= recipe2.getMaxUses());
		if (outOfStock1 != outOfStock2) return false;
		if (recipe1.hasExperienceReward() != recipe2.hasExperienceReward()) return false;
		if (!recipe1.getIngredients().equals(recipe2.getIngredients())) return false;
		return true;
	}

	// note: this method considers the recipes equal even if their uses and max uses don't match
	public static boolean areMerchantRecipesEqual(List<MerchantRecipe> recipes1, List<MerchantRecipe> recipes2) {
		if (recipes1 == recipes2) return true;
		if (recipes1 == null) return false;
		if (recipes2 == null) return false;

		if (recipes1.size() != recipes2.size()) return false;
		for (int i = 0; i < recipes1.size(); ++i) {
			MerchantRecipe recipe1 = recipes1.get(i);
			MerchantRecipe recipe2 = recipes2.get(i);
			if (!areMerchantRecipesEqual(recipe1, recipe2)) {
				return false;
			}
		}
		return true;
	}

	public static final class TargetShopkeepersResult {

		private final List<Shopkeeper> shopkeepers;
		private final Text errorMessage;
		// assert: !shopkeepers.isEmpty || errorMessage != null

		private TargetShopkeepersResult(List<Shopkeeper> shopkeepers) {
			Validate.isTrue(shopkeepers != null && !shopkeepers.isEmpty());
			this.shopkeepers = shopkeepers;
			this.errorMessage = null;
		}

		private TargetShopkeepersResult(Text errorMessage) {
			Validate.notNull(errorMessage);
			Validate.isTrue(!errorMessage.isPlainTextEmpty());
			this.errorMessage = errorMessage;
			this.shopkeepers = Collections.emptyList();
		}

		public boolean isSuccess() {
			return (errorMessage == null);
		}

		public List<Shopkeeper> getShopkeepers() {
			return shopkeepers;
		}

		public Text getErrorMessage() {
			return errorMessage;
		}
	}

	public interface TargetShopkeeperFilter extends Predicate<Shopkeeper> {

		public static final TargetShopkeeperFilter ANY = new TargetShopkeeperFilter() {
			@Override
			public boolean test(Shopkeeper shopkeeper) {
				return true;
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Settings.msgMustTargetShop;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper) {
				return Text.EMPTY; // not used
			}
		};

		public static final TargetShopkeeperFilter ADMIN = new TargetShopkeeperFilter() {
			@Override
			public boolean test(Shopkeeper shopkeeper) {
				return (shopkeeper instanceof AdminShopkeeper);
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Settings.msgMustTargetAdminShop;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper) {
				return Settings.msgTargetShopIsNoAdminShop;
			}
		};

		public static final TargetShopkeeperFilter PLAYER = new TargetShopkeeperFilter() {
			@Override
			public boolean test(Shopkeeper shopkeeper) {
				return (shopkeeper instanceof PlayerShopkeeper);
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Settings.msgMustTargetPlayerShop;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper) {
				return Settings.msgTargetShopIsNoPlayerShop;
			}
		};

		public abstract Text getNoTargetErrorMsg();

		public abstract Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper);
	}

	// type is null to allow any shopkeeper type to be returned
	public static TargetShopkeepersResult getTargetedShopkeepers(Player player, TargetShopkeeperFilter shopkeeperFilter) {
		if (shopkeeperFilter == null) shopkeeperFilter = TargetShopkeeperFilter.ANY;
		Location playerLoc = player.getEyeLocation();
		World world = playerLoc.getWorld();
		Vector viewDirection = playerLoc.getDirection();

		// ray trace to check for the closest block and entity collision:
		// not ignoring passable blocks, in case some type of shopkeeper object makes use of them
		RayTraceResult rayTraceResult = world.rayTrace(playerLoc, viewDirection, SHOPKEEPER_TARGET_RANGE, FluidCollisionMode.NEVER, false, 0.0D, (entity) -> {
			return !entity.isDead() && !entity.equals(player); // TODO SPIGOT-5228: filtering dead entities
		});

		// determine targeted shopkeeper, and return context dependent failure messages:
		if (rayTraceResult != null) {
			Shopkeeper shopkeeper = null;
			Block targetBlock = rayTraceResult.getHitBlock();
			if (targetBlock != null) {
				// get shopkeeper by targeted block:
				shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByBlock(targetBlock);
				if (shopkeeper == null) {
					// get player shopkeepers by targeted chest:
					if (ItemUtils.isChest(targetBlock.getType())) {
						List<PlayerShopkeeper> shopsUsingChest = SKShopkeepersPlugin.getInstance().getProtectedChests().getShopkeepersUsingChest(targetBlock);
						if (shopsUsingChest.isEmpty()) {
							return new TargetShopkeepersResult(Settings.msgUnusedChest);
						} else {
							// filter shops:
							List<Shopkeeper> acceptedShops = new ArrayList<>();
							for (Shopkeeper shopUsingChest : shopsUsingChest) {
								if (shopkeeperFilter.test(shopUsingChest)) {
									acceptedShops.add(shopUsingChest);
								}
							}
							if (acceptedShops.isEmpty()) {
								// use the first shopkeeper using the chest for the error message:
								return new TargetShopkeepersResult(shopkeeperFilter.getInvalidTargetErrorMsg(shopsUsingChest.get(0)));
							} else {
								return new TargetShopkeepersResult(acceptedShops);
							}
						}
					}
				}
			} else {
				Entity targetEntity = rayTraceResult.getHitEntity();
				assert targetEntity != null;
				shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByEntity(targetEntity);
				if (shopkeeper == null) {
					return new TargetShopkeepersResult(Settings.msgTargetEntityIsNoShop);
				}
			}

			// check if found shopkeeper is accepted:
			if (shopkeeper != null) {
				if (shopkeeperFilter.test(shopkeeper)) {
					return new TargetShopkeepersResult(Arrays.asList(shopkeeper)); // accepted
				} else {
					return new TargetShopkeepersResult(shopkeeperFilter.getInvalidTargetErrorMsg(shopkeeper));
				}
			}
		}

		// no targeted shopkeeper found:
		return new TargetShopkeepersResult(shopkeeperFilter.getNoTargetErrorMsg());
	}

	public static class OwnedPlayerShopsResult {

		private final UUID playerUUID; // can be null
		private final String playerName; // can be null
		// Stores all found shop owners which match the given target player name. If this contains more than one entry
		// then the player name is ambiguous.
		private final Set<User> matchingShopOwners; // not null, can be empty
		private final List<? extends PlayerShopkeeper> shops; // not null, can be empty

		public OwnedPlayerShopsResult(UUID playerUUID, String playerName, Set<User> matchingShopOwners, List<? extends PlayerShopkeeper> shops) {
			Validate.isTrue(playerUUID != null || playerName != null, "The player uuid and name are both null!");
			Validate.notNull(matchingShopOwners, "Matching shop owners is null!");
			this.playerUUID = playerUUID;
			this.playerName = playerName;
			this.matchingShopOwners = matchingShopOwners;
			this.shops = shops;
		}

		public UUID getPlayerUUID() {
			return playerUUID;
		}

		// can return null if the player name is unknown:
		public String getPlayerName() {
			return playerName;
		}

		public Set<User> getMatchingShopOwners() {
			return matchingShopOwners;
		}

		public List<? extends PlayerShopkeeper> getShops() {
			return shops;
		}
	}

	// Searches for shops owned by the player specified by either uuid or name.
	// If at least one matching shop is found, it is used to complete the available information about the target player
	// (eg. missing uuid or name). The found player name may also differ in case.
	// If shops are searched via target player name, a map of matching shop owners is returned, which stores the player
	// uuids and names of all shop owners found that match the given target player name. If this contains more than one
	// entry then the target player name is ambiguous. The result contains the shops of all those matching players then.
	// TODO remove this and let callers handle this in 2 parts: Matching of Users, dealing with ambiguous matches, and then looking up shopkeepers by user. 
	public static OwnedPlayerShopsResult getOwnedPlayerShops(UUID targetPlayerUUID, String targetPlayerName) {
		Validate.isTrue(targetPlayerUUID != null || targetPlayerName != null, "The target player uuid and name are both null!");

		// keep track if there are multiple shop owners with matching name:
		Map<UUID, String> matchingShopOwners = new LinkedHashMap<>();

		// search for shops owned by the specified player:
		List<PlayerShopkeeper> shops = new ArrayList<>();
		for (Shopkeeper shopkeeper : ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				User shopOwner = playerShop.getOwner();
				UUID shopOwnerUUID = shopOwner.getUniqueId(); // not null
				// String shopOwnerName = shopOwner.getName(); // can be null // TODO owner name was previously assumed to never be null
				if (targetPlayerUUID != null) {
					// we search for shops with matching owner uuid:
					if (targetPlayerUUID.equals(shopOwnerUUID)) {
						shops.add(playerShop);

						// The input target player name may be missing or differ in case.
						// Keep track of the owner's actual name:
						targetPlayerName = shopOwnerName;
					}
				} else {
					assert targetPlayerName != null;
					// check for matching name:
					if (targetPlayerName.equalsIgnoreCase(shopOwnerName)) {
						// Note: If there exist multiple players which match the given name, the result will include the
						// shops of all of them.
						shops.add(playerShop);

						// The input target player name may differ in case.
						// Keep track of the owner's actual name:
						targetPlayerName = shopOwnerName;

						// keep track of players with matching name:
						matchingShopOwners.putIfAbsent(shopOwnerUUID, shopOwnerName);
					}
				}
			}
		}
		return new OwnedPlayerShopsResult(targetPlayerUUID, targetPlayerName, matchingShopOwners, shops);
	}

	public static class ShopkeeperNameMatchers {

		private ShopkeeperNameMatchers() {
		}

		public static final ObjectMatcher<Shopkeeper> DEFAULT = new ObjectMatcher<Shopkeeper>() {
			@Override
			public Stream<? extends Shopkeeper> match(String input) {
				if (StringUtils.isEmpty(input)) return Stream.empty();
				return ShopkeepersAPI.getShopkeeperRegistry().getShopkeepersByName(input);
			}
		};
	}
}
