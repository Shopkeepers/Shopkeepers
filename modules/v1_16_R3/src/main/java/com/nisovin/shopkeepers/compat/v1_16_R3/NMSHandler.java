package com.nisovin.shopkeepers.compat.v1_16_R3;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Cat;
import org.bukkit.entity.LivingEntity;
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

import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.GameProfileSerializer;
import net.minecraft.server.v1_16_R3.IMerchant;
import net.minecraft.server.v1_16_R3.MerchantRecipeList;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_16_R3.PathfinderGoalSelector;

public final class NMSHandler implements NMSCallProvider {

	private final Field craftItemStackHandleField;

	public NMSHandler() throws Exception {
		craftItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
		craftItemStackHandleField.setAccessible(true);
	}

	@Override
	public String getVersionId() {
		return "1_16_R3";
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
			// Example: Armor stands are living, but not insentient.
			if (!(mcLivingEntity instanceof EntityInsentient)) return;
			EntityInsentient mcInsentientEntity = (EntityInsentient) mcLivingEntity;

			// Make the goal selector items accessible:
			Field cField = PathfinderGoalSelector.class.getDeclaredField("c"); // Active goals
			cField.setAccessible(true);
			Field dField = PathfinderGoalSelector.class.getDeclaredField("d"); // Registered goals
			dField.setAccessible(true);

			// Overwrite the goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = Unsafe.castNonNull(goalsField.get(mcLivingEntity));

			// Clear old goals:
			Map<?, ?> goals_c = Unsafe.castNonNull(cField.get(goals));
			goals_c.clear();
			Set<?> goals_d = Unsafe.castNonNull(dField.get(goals));
			goals_d.clear();

			// Add new goals:
			goals.a(
					0,
					new PathfinderGoalLookAtPlayer(
							mcInsentientEntity,
							EntityHuman.class,
							LivingEntityAI.LOOK_RANGE,
							1.0F
					)
			);

			// Overwrite the target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = Unsafe.castNonNull(targetsField.get(mcLivingEntity));

			// Clear old target goals:
			Map<?, ?> targets_c = Unsafe.castNonNull(cField.get(targets));
			targets_c.clear();
			Set<?> targets_d = Unsafe.castNonNull(dField.get(targets));
			targets_d.clear();
		} catch (Exception e) {
			Log.severe("Failed to override mob AI!", e);
		}
	}

	@Override
	public void tickAI(LivingEntity entity, int ticks) {
		EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
		// Example: Armor stands are living, but not insentient.
		if (!(mcLivingEntity instanceof EntityInsentient)) return;
		EntityInsentient mcMob = (EntityInsentient) mcLivingEntity;

		// Clear the sensing cache. This sensing cache is reused for the individual ticks.
		mcMob.getEntitySenses().a();
		for (int i = 0; i < ticks; ++i) {
			mcMob.goalSelector.doTick();
			if (!mcMob.getControllerLook().c()) { // isHasWanted
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
				mcMob.n(mcMob.yaw); // setYBodyRot
			}
			// Tick the look controller:
			// This makes the entity's head (and indirectly also its body) rotate towards the
			// current target.
			mcMob.getControllerLook().a();
		}
		mcMob.getEntitySenses().a(); // Clear the sensing cache
	}

	@Override
	public void setOnGround(org.bukkit.entity.Entity entity, boolean onGround) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.setOnGround(onGround);
	}

	@Override
	public boolean isNoAIDisablingGravity() {
		return true;
	}

	@Override
	public void setNoclip(org.bukkit.entity.Entity entity) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.noclip = true;
	}

	// For CraftItemStacks, this first tries to retrieve the underlying NMS item stack without
	// making a copy of it. Otherwise, this falls back to using CraftItemStack#asNMSCopy.
	private net.minecraft.server.v1_16_R3.ItemStack asNMSItemStack(ItemStack itemStack) {
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
		net.minecraft.server.v1_16_R3.ItemStack nmsProvided = asNMSItemStack(provided);
		net.minecraft.server.v1_16_R3.ItemStack nmsRequired = asNMSItemStack(required);
		NBTTagCompound providedTag = nmsProvided.getTag();
		NBTTagCompound requiredTag = nmsRequired.getTag();
		// Compare the tags according to Minecraft's matching rules (imprecise):
		return GameProfileSerializer.a(requiredTag, providedTag, false);
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
		IMerchant nmsMerchant;
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
		MerchantRecipeList merchantRecipeList = nmsMerchant.getOffers();
		if (merchantRecipeList == null) {
			// Just in case:
			merchantRecipeList = new MerchantRecipeList();
		}

		// Send PacketPlayOutOpenWindowMerchant packet: window id, recipe list, merchant level (1:
		// Novice, .., 5: Master), merchant total experience, is-regular-villager flag (false: hides
		// some gui elements), can-restock flag (false: hides restock message if out of stock)
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		nmsPlayer.openTrade(
				nmsPlayer.activeContainer.windowId,
				merchantRecipeList,
				merchantLevel,
				merchantExperience,
				regularVillager,
				canRestock
		);
	}

	@Override
	public @Nullable String getItemSNBT(ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null");
		net.minecraft.server.v1_16_R3.ItemStack nmsItem = asNMSItemStack(itemStack);
		NBTTagCompound itemNBT = nmsItem.save(new NBTTagCompound());
		return itemNBT.toString();
	}

	@Override
	public @Nullable String getItemTypeTranslationKey(Material material) {
		Validate.notNull(material, "material is null");
		net.minecraft.server.v1_16_R3.Item nmsItem = CraftMagicNumbers.getItem(material);
		if (nmsItem == null) return null;
		return nmsItem.getName();
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
}
