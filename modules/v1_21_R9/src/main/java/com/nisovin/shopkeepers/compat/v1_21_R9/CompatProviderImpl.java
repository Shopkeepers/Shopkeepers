package com.nisovin.shopkeepers.compat.v1_21_R9;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.v1_21_R7.CraftRegistry;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftMob;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftMerchant;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieNautilus;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.view.builder.InventoryViewBuilder;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.compat.CompatProvider;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopobjects.entity.base.EntityAI;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.inventory.ItemStackComponentsData;
import com.nisovin.shopkeepers.util.inventory.ItemStackMetaTag;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.item.trading.MerchantOffers;

public final class CompatProviderImpl implements CompatProvider {

	// Public static for access in tests.
	public static final String VERSION_ID = "1_21_R9";

	private final CopperChestProtectionListener copperChestProtectionListener;

	private final TagParser<Tag> tagParser = Unsafe.castNonNull(TagParser.create(NbtOps.INSTANCE));

	private final Field craftItemStackHandleField;

	public CompatProviderImpl() throws Exception {
		copperChestProtectionListener = new CopperChestProtectionListener(SKShopkeepersPlugin.getInstance());
		craftItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
		craftItemStackHandleField.setAccessible(true);
	}

	@Override
	public String getVersionId() {
		return VERSION_ID;
	}

	@Override
	public void onEnable() {
		var plugin = SKShopkeepersPlugin.getInstance();
		// 1.21.11-R0.1 did not yet have the event.
		if (Settings.protectContainers
				&& Settings.preventCopperGolemAccess
				&& ClassUtils.getClassOrNull("org.bukkit.event.entity.EntityTargetBlockEvent") != null) {
			Bukkit.getPluginManager().registerEvents(copperChestProtectionListener, plugin);
		}
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(copperChestProtectionListener);
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		// Example: Armor stands are living, but not insentient/Mob.
		if (!(entity instanceof Mob)) return;
		try {
			net.minecraft.world.entity.Mob mcMob = ((CraftMob) entity).getHandle();

			// Overwrite the goal selector:
			GoalSelector goalSelector = mcMob.goalSelector;

			// Clear the old goals: Removes all goals from the "availableGoals". During the next
			// tick, the "lockedFlags" (active goals) are updated as well.
			goalSelector.removeAllGoals(goal -> true);

			// Add new goals:
			goalSelector.addGoal(
					0,
					new LookAtPlayerGoal(
							mcMob,
							net.minecraft.world.entity.player.Player.class,
							EntityAI.LOOK_RANGE,
							1.0F
					)
			);

			// Overwrite the target selector:
			GoalSelector targetSelector = mcMob.targetSelector;

			// Clear old target goals:
			targetSelector.removeAllGoals(goal -> true);
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
		return Unsafe.assertNonNull(CraftItemStack.asNMSCopy(itemStack));
	}

	private CompoundTag getItemStackTag(net.minecraft.world.item.ItemStack nmsItem) {
		var itemTag = (CompoundTag) net.minecraft.world.item.ItemStack.CODEC.encodeStart(
				CraftRegistry.getMinecraftRegistry().createSerializationContext(NbtOps.INSTANCE),
				nmsItem
		).getOrThrow();
		assert itemTag != null;
		return itemTag;
	}

	@Override
	public boolean matches(@Nullable ItemStack provided, @Nullable ItemStack required) {
		if (provided == required) return true;
		// If the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		assert required != null && provided != null;
		if (provided.getType() != required.getType()) return false;
		net.minecraft.world.item.ItemStack nmsProvided = this.asNMSItemStack(provided);
		net.minecraft.world.item.ItemStack nmsRequired = this.asNMSItemStack(required);
		DataComponentMap requiredComponents = PatchedDataComponentMap.fromPatch(
				DataComponentMap.EMPTY,
				nmsRequired.getComponentsPatch()
		);
		// Compare the components according to Minecraft's matching rules (imprecise):
		return DataComponentExactPredicate.allOf(requiredComponents).test(nmsProvided);
	}

	@Override
	public void setInventoryViewTitle(InventoryViewBuilder<?> builder, String title) {
		builder.title(title);
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
	public ItemStackMetaTag getItemStackMetaTag(@ReadOnly @Nullable ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) {
			return new ItemStackMetaTag(null);
		}
		assert itemStack != null;

		var nmsItem = this.asNMSItemStack(itemStack);
		var itemTag = this.getItemStackTag(nmsItem);
		var componentsTag = (CompoundTag) itemTag.get("components");
		return new ItemStackMetaTag(componentsTag);
	}

	@Override
	public boolean matches(ItemStackMetaTag provided, ItemStackMetaTag required, boolean matchPartialLists) {
		Validate.notNull(provided, "provided is null");
		Validate.notNull(required, "required is null");
		var providedTag = (Tag) provided.getNmsTag();
		var requiredTag = (Tag) required.getNmsTag();
		return NbtUtils.compareNbt(requiredTag, providedTag, matchPartialLists);
	}

	@Override
	public @Nullable ItemStackComponentsData getItemStackComponentsData(@ReadOnly ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null!");
		if (ItemUtils.isEmpty(itemStack)) {
			return null;
		}

		var nmsItem = this.asNMSItemStack(itemStack);
		var itemTag = this.getItemStackTag(nmsItem);

		var componentsTag = (CompoundTag) itemTag.get("components");
		if (componentsTag == null) {
			return null;
		}

		var componentsData = ItemStackComponentsData.ofNonNull(DataContainer.create());
		componentsTag.forEach((componentKey, componentValue) -> {
			assert componentKey != null;
			// Serialized as SNBT:
			componentsData.set(componentKey, componentValue.toString());
		});
		return componentsData;
	}

	@Override
	public ItemStack deserializeItemStack(
			int dataVersion,
			NamespacedKey id,
			int count,
			@Nullable ItemStackComponentsData componentsData
	) {
		Validate.notNull(id, "id is null!");
		var itemTag = new CompoundTag();
		itemTag.putString("id", id.toString());
		itemTag.putInt("count", count);

		var componentValues = componentsData != null ? componentsData.getValues() : null;
		if (componentValues != null && !componentValues.isEmpty()) {
			var componentsTag = new CompoundTag();
			componentValues.forEach((componentKey, componentValue) -> {
				assert componentKey != null;
				assert componentValue != null;
				var componentSnbt = componentValue.toString();

				Tag componentTag;
				try {
					componentTag = Unsafe.assertNonNull(tagParser.parseFully(componentSnbt));
				} catch (CommandSyntaxException e) {
					throw new IllegalArgumentException(
							"Error parsing item stack component: '" + componentSnbt + "'",
							e
					);
				}
				componentsTag.put(componentKey.toString(), componentTag);
			});
			itemTag.put("components", componentsTag);
		}

		var currentDataVersion = ServerUtils.getDataVersion();
		var convertedItemTag = (CompoundTag) DataFixers.getDataFixer().update(
				References.ITEM_STACK,
				new Dynamic<>(Unsafe.castNonNull(NbtOps.INSTANCE), itemTag),
				dataVersion,
				currentDataVersion
		).getValue();

		if (convertedItemTag.getStringOr("id", "minecraft:air").equals("minecraft:air")) {
			return new ItemStack(Material.AIR);
		}

		var nmsItem = net.minecraft.world.item.ItemStack.CODEC.parse(
				CraftRegistry.getMinecraftRegistry().createSerializationContext(NbtOps.INSTANCE),
				convertedItemTag
		).getOrThrow();
		return Unsafe.assertNonNull(CraftItemStack.asCraftMirror(nmsItem));
	}

	@Override
	public <T extends Keyed> Registry<T> getRegistry(Class<T> clazz) {
		// Some API-only registries might not be supported by Bukkit.getRegistry(Class):
		if (clazz == EntityType.class) {
			return Unsafe.castNonNull(Registry.ENTITY_TYPE);
		} else if (clazz == Particle.class) {
			return Unsafe.castNonNull(Registry.PARTICLE_TYPE);
		} else if (clazz == PotionType.class) {
			return Unsafe.castNonNull(Registry.POTION);
		} else if (clazz == MemoryKey.class) {
			return Unsafe.castNonNull(Registry.MEMORY_MODULE_TYPE);
		} else {
			// Non-null: Expected to only be used with known registry types.
			return Unsafe.assertNonNull(Bukkit.getRegistry(clazz));
		}
	}

	// MC 1.21.9+ TODO Can be removed once we only support Bukkit 1.21.9+

	public void setCopperGolemWeatherState(Golem golem, String weatherState) {
		CopperGolem.CopperWeatherState weatherStateValue = EnumUtils.valueOf(CopperGolem.CopperWeatherState.class, weatherState);
		if (weatherStateValue == null) {
			weatherStateValue = CopperGolem.CopperWeatherState.UNAFFECTED; // Default
		}
		((CopperGolem) golem).setWeatherState(weatherStateValue);
	}

	public void setCopperGolemNextWeatheringTick(Golem golem, int tick) {
		((CopperGolem) golem).setNextWeatheringTick(tick);
	}

	public void setMannequinHideDescription(LivingEntity mannequin, boolean hideDescription) {
		((Mannequin) mannequin).setHideDescription(hideDescription);
	}

	public void setMannequinDescription(LivingEntity mannequin, @Nullable String description) {
		((Mannequin) mannequin).setDescription(description);
	}

	public void setMannequinMainHand(LivingEntity mannequin, MainHand mainHand) {
		((Mannequin) mannequin).setMainHand(mainHand);
	}

	public void setMannequinPose(LivingEntity mannequin, Pose pose) {
		((Mannequin) mannequin).setPose(pose);
	}

	public void setMannequinProfile(LivingEntity mannequin, @Nullable PlayerProfile profile) {
		((Mannequin) mannequin).setPlayerProfile(profile);
	}

	// MC 1.21.11+ TODO Can maybe be removed once we only support Bukkit 1.21.11+

	@Override
	public void setZombieNautilusVariant(LivingEntity zombieNautilus, NamespacedKey variant) {
		var registry = this.getRegistry(ZombieNautilus.Variant.class);
		ZombieNautilus.Variant variantValue = registry.get(variant);
		if (variantValue == null) {
			variantValue = ZombieNautilus.Variant.TEMPERATE; // Default
		}
		assert variantValue != null;
		((ZombieNautilus) zombieNautilus).setVariant(variantValue);
	}

	@Override
	public NamespacedKey cycleZombieNautilusVariant(NamespacedKey variant, boolean backwards) {
		var registry = this.getRegistry(ZombieNautilus.Variant.class);
		ZombieNautilus.Variant variantValue = registry.get(variant);
		if (variantValue == null) {
			variantValue = ZombieNautilus.Variant.TEMPERATE; // Default
		}
		assert variantValue != null;

		return RegistryUtils.cycleKeyed(ZombieNautilus.Variant.class, variantValue, backwards)
				.getKeyOrThrow();
	}
}
