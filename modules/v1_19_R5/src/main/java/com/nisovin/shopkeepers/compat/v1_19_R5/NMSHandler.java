package com.nisovin.shopkeepers.compat.v1_19_R5;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftMob;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.GlowSquid;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityAI;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils; // GameProfileSerializer
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.item.trading.MerchantOffers;

public final class NMSHandler implements NMSCallProvider {

	private final Field craftItemStackHandleField;

	public NMSHandler() throws Exception {
		craftItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
		craftItemStackHandleField.setAccessible(true);
	}

	@Override
	public String getVersionId() {
		return "1_19_R5";
	}

	public Class<?> getCraftMagicNumbersClass() {
		return CraftMagicNumbers.class;
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		// Example: Armor stands are living, but not insentient/Mob.
		if (!(entity instanceof Mob)) return;
		try {
			net.minecraft.world.entity.Mob mcMob = ((CraftMob) entity).getHandle();

			// Make the goal selector items accessible:
			// lockedFlags (Active goals)
			Field activeGoalsField = GoalSelector.class.getDeclaredField("c");
			activeGoalsField.setAccessible(true);

			// Overwrite the goal selector: Clear old goals
			GoalSelector goalSelector = mcMob.goalSelector;

			// Clear old goals:
			Map<?, ?> activeGoals = Unsafe.castNonNull(activeGoalsField.get(goalSelector));
			activeGoals.clear();
			goalSelector.getAvailableGoals().clear();

			// Add new goals:
			goalSelector.addGoal(
					0,
					new LookAtPlayerGoal(
							mcMob,
							net.minecraft.world.entity.player.Player.class,
							LivingEntityAI.LOOK_RANGE,
							1.0F
					)
			);

			// Overwrite the target selector:
			GoalSelector targetSelector = mcMob.targetSelector;

			// Clear old target goals:
			Map<?, ?> targetSelectorActiveGoals = Unsafe.castNonNull(activeGoalsField.get(targetSelector));
			targetSelectorActiveGoals.clear();
			targetSelector.getAvailableGoals().clear();
		} catch (Exception e) {
			Log.severe("Failed to override mob AI!", e);
		}
	}

	@Override
	public void tickAI(LivingEntity entity, int ticks) {
		net.minecraft.world.entity.LivingEntity mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
		// Example: Armor stands are living, but not insentient/Mob.
		if (!(mcLivingEntity instanceof net.minecraft.world.entity.Mob)) return;
		net.minecraft.world.entity.Mob mcMob = (net.minecraft.world.entity.Mob) mcLivingEntity;

		// Clear the sensing cache. This sensing cache is reused for the individual ticks.
		mcMob.getSensing().tick();
		for (int i = 0; i < ticks; ++i) {
			mcMob.goalSelector.tick();
			if (!mcMob.getLookControl().isLookingAtTarget()) {
				// If there is no target to look at, the entity rotates towards its current body
				// rotation.
				// We reset the entity's body rotation here to the initial yaw it was spawned with,
				// causing it to rotate back towards this initial direction whenever it has no
				// target to look at anymore.
				// This rotating back towards its initial orientation only works if the entity is
				// still ticked: Since we only tick shopkeeper mobs near players, the entity may
				// remain in its previous rotation whenever the last nearby player teleports away,
				// until the ticking resumes when a player comes close again.

				// Setting the body rotation also ensures that it initially matches the entity's
				// intended yaw, because CraftBukkit itself does not automatically set the body
				// rotation when spawning the entity (only its yRot and head rotation are set).
				// Omitting this would therefore cause the entity to initially rotate towards some
				// random direction if it is being ticked and has no target to look at.
				mcMob.setYBodyRot(mcMob.getYRot());
			}
			// Tick the look controller:
			// This makes the entity's head (and indirectly also its body) rotate towards the
			// current target.
			mcMob.getLookControl().tick();
		}
		mcMob.getSensing().tick(); // Clear the sensing cache
	}

	@Override
	public void setOnGround(Entity entity, boolean onGround) {
		net.minecraft.world.entity.Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.setOnGround(onGround);
	}

	@Override
	public boolean isNoAIDisablingGravity() {
		return true;
	}

	@Override
	public void setNoclip(Entity entity) {
		net.minecraft.world.entity.Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.noPhysics = true;
	}

	// For CraftItemStacks, this first tries to retrieve the underlying NMS item stack without
	// making a copy of it. Otherwise, this falls back to using CraftItemStack#asNMSCopy.
	private net.minecraft.world.item.ItemStack asNMSItemStack(ItemStack itemStack) {
		assert itemStack != null;
		if (itemStack instanceof CraftItemStack) {
			try {
				return Unsafe.castNonNull(craftItemStackHandleField.get(itemStack));
			} catch (Exception e) {
				Log.severe("Failed to retrieve the underlying Minecraft ItemStack!", e);
			}
		}
		return CraftItemStack.asNMSCopy(itemStack);
	}

	@Override
	public boolean matches(@Nullable ItemStack provided, @Nullable ItemStack required) {
		if (provided == required) return true;
		// If the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		assert required != null && provided != null;
		if (provided.getType() != required.getType()) return false;
		net.minecraft.world.item.ItemStack nmsProvided = asNMSItemStack(provided);
		net.minecraft.world.item.ItemStack nmsRequired = asNMSItemStack(required);
		CompoundTag providedTag = nmsProvided.getTag();
		CompoundTag requiredTag = nmsRequired.getTag();
		// Compare the tags according to Minecraft's matching rules (imprecise):
		return NbtUtils.compareNbt(requiredTag, providedTag, false);
	}

	@Override
	public void updateTrades(Player player) {
		Inventory openInventory = player.getOpenInventory().getTopInventory();
		if (!(openInventory instanceof MerchantInventory)) {
			return;
		}
		MerchantInventory merchantInventory = (MerchantInventory) openInventory;

		// Update the merchant inventory on the server (updates the result item, etc.):
		merchantInventory.setItem(0, merchantInventory.getItem(0));

		Merchant merchant = merchantInventory.getMerchant();
		net.minecraft.world.item.trading.Merchant nmsMerchant;
		boolean regularVillager = false;
		boolean canRestock = false;
		// Note: When using the 'is-regular-villager'-flag, using level 0 allows hiding the level
		// name suffix.
		int merchantLevel = 1;
		int merchantExperience = 0;
		if (merchant instanceof Villager) {
			nmsMerchant = ((CraftVillager) merchant).getHandle();
			Villager villager = (Villager) merchant;
			regularVillager = true;
			canRestock = true;
			merchantLevel = villager.getVillagerLevel();
			merchantExperience = villager.getVillagerExperience();
		} else if (merchant instanceof AbstractVillager) {
			nmsMerchant = ((CraftAbstractVillager) merchant).getHandle();
		} else {
			nmsMerchant = ((CraftMerchant) merchant).getMerchant();
			merchantLevel = 0; // Hide name suffix
		}
		MerchantOffers merchantRecipeList = nmsMerchant.getOffers();
		if (merchantRecipeList == null) {
			// Just in case:
			merchantRecipeList = new MerchantOffers();
		}

		// Send PacketPlayOutOpenWindowMerchant packet: window id, recipe list, merchant level (1:
		// Novice, .., 5: Master), merchant total experience, is-regular-villager flag (false: hides
		// some gui elements), can-restock flag (false: hides restock message if out of stock)
		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		nmsPlayer.sendMerchantOffers(
				nmsPlayer.containerMenu.containerId,
				merchantRecipeList,
				merchantLevel,
				merchantExperience,
				regularVillager,
				canRestock
		);
	}

	@Override
	public @Nullable String getItemSNBT(@Nullable ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null");
		net.minecraft.world.item.ItemStack nmsItem = asNMSItemStack(itemStack);
		CompoundTag itemNBT = nmsItem.save(new CompoundTag());
		return itemNBT.toString();
	}

	@Override
	public @Nullable String getItemTypeTranslationKey(Material material) {
		Validate.notNull(material, "material is null");
		net.minecraft.world.item.Item nmsItem = CraftMagicNumbers.getItem(material);
		if (nmsItem == null) return null;
		return nmsItem.getDescriptionId();
	}

	// Default implementation for MC 1.16+:

	@Override
	public Cat.@Nullable Type getCatType(String typeName) {
		return EnumUtils.valueOf(Cat.Type.class, typeName);
	}

	@Override
	public String cycleCatType(String typeName, boolean backwards) {
		Cat.@Nullable Type catType = this.getCatType(typeName);
		if (catType == null) return typeName; // Current value not found

		return EnumUtils.cycleEnumConstant(Cat.Type.class, catType, backwards).name();
	}

	// MC 1.17 specific features

	@Override
	public void setAxolotlVariant(LivingEntity axolotl, String variantName) {
		((Axolotl) axolotl).setVariant(Axolotl.Variant.valueOf(variantName));
	}

	@Override
	public String cycleAxolotlVariant(String variantName, boolean backwards) {
		return EnumUtils.cycleEnumConstant(
				Axolotl.Variant.class,
				Axolotl.Variant.valueOf(variantName),
				backwards
		).name();
	}

	@Override
	public void setGlowSquidDark(LivingEntity glowSquid, boolean dark) {
		// Integer.MAX_VALUE should be sufficiently long to not require periodic refreshes.
		((GlowSquid) glowSquid).setDarkTicksRemaining(dark ? Integer.MAX_VALUE : 0);
	}

	@Override
	public void setScreamingGoat(LivingEntity goat, boolean screaming) {
		((Goat) goat).setScreaming(screaming);
	}

	@Override
	public void setGlowingText(Sign sign, boolean glowingText) {
		sign.setGlowingText(glowingText);
	}

	// MC 1.19 specific features

	@Override
	public void setFrogVariant(LivingEntity frog, String variantName) {
		((Frog) frog).setVariant(Frog.Variant.valueOf(variantName));
	}

	@Override
	public String cycleFrogVariant(String variantName, boolean backwards) {
		return EnumUtils.cycleEnumConstant(
				Frog.Variant.class,
				Frog.Variant.valueOf(variantName),
				backwards
		).name();
	}

	@Override
	public void setGoatLeftHorn(LivingEntity goat, boolean hasLeftHorn) {
		((Goat) goat).setLeftHorn(hasLeftHorn);
	}

	@Override
	public void setGoatRightHorn(LivingEntity goat, boolean hasRightHorn) {
		((Goat) goat).setRightHorn(hasRightHorn);
	}
}
