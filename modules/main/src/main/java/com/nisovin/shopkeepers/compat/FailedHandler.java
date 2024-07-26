package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public final class FailedHandler implements NMSCallProvider {

	// Minecraft
	private final Class<?> nmsEntityClass;
	private final Method nmsEntitySetOnGroundMethod;

	private final Class<?> nmsItemStackClass;
	private final Method nmsGetTagMethod;

	private final Class<?> nmsGameProfileSerializerClass;
	private final Class<?> nmsNBTBaseClass;
	private final Method nmsAreNBTMatchingMethod;

	// CraftBukkit
	private final Class<?> obcCraftItemStackClass;
	private final Method obcAsNMSCopyMethod;

	private final Class<?> obcCraftEntityClass;
	private final Method obcGetHandleMethod;

	// Bukkit
	private final @Nullable Registry<@NonNull ?> catTypeRegistry;

	public FailedHandler() throws Exception {
		String cbPackage = ServerUtils.getCraftBukkitPackage();

		// Minecraft
		nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
		nmsGetTagMethod = nmsItemStackClass.getDeclaredMethod("s"); // getTag

		nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
		nmsEntitySetOnGroundMethod = nmsEntityClass.getDeclaredMethod(
				"c", // setOnGround
				boolean.class
		);

		nmsGameProfileSerializerClass = Class.forName("net.minecraft.nbt.GameProfileSerializer");
		nmsNBTBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
		nmsAreNBTMatchingMethod = nmsGameProfileSerializerClass.getDeclaredMethod(
				"a",
				nmsNBTBaseClass,
				nmsNBTBaseClass,
				boolean.class
		);

		// CraftBukkit
		obcCraftItemStackClass = Class.forName(cbPackage + ".inventory.CraftItemStack");
		obcAsNMSCopyMethod = obcCraftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);

		obcCraftEntityClass = Class.forName(cbPackage + ".entity.CraftEntity");
		obcGetHandleMethod = obcCraftEntityClass.getDeclaredMethod("getHandle");

		// Cat type:
		// Registry was added in 1.20.4.
		@Nullable Registry<@NonNull ?> catTypeRegistry = null;
		try {
			Field catVariantField = Registry.class.getField("CAT_VARIANT");
			Object catVariantRegistry = catVariantField.get(null);
			catTypeRegistry = (Registry<@NonNull ?>) catVariantRegistry;
		} catch (Exception ex) {
			// We still continue to enable the plugin, but applying and cycling the cat type is not
			// supported:
			Log.warning("Failed to initialize cat type compatibility mode. Cat type is not supported!");
		}
		this.catTypeRegistry = catTypeRegistry;
	}

	@Override
	public String getVersionId() {
		return "FailedHandler";
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
	}

	@Override
	public boolean supportsCustomMobAI() {
		// Not supported. Mobs will be stationary and not react towards nearby players due to the
		// NoAI flag.
		return false;
	}

	@Override
	public void tickAI(LivingEntity entity, int ticks) {
		// Not supported.
	}

	@Override
	public void setOnGround(Entity entity, boolean onGround) {
		try {
			Object mcEntity = Unsafe.assertNonNull(obcGetHandleMethod.invoke(entity));
			nmsEntitySetOnGroundMethod.invoke(mcEntity, onGround);
		} catch (Exception e) {
			// Ignoring, since this is not that important if it doesn't work.
		}
	}

	@Override
	public void setNoclip(Entity entity) {
		// Not supported, but also not necessarily required (just provides a small performance
		// benefit).
	}

	@Override
	public boolean matches(@Nullable ItemStack provided, @Nullable ItemStack required) {
		if (provided == required) return true;
		// If the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		assert required != null && provided != null;
		if (provided.getType() != required.getType()) return false;
		try {
			Object nmsProvided = Unsafe.assertNonNull(
					obcAsNMSCopyMethod.invoke(Unsafe.uncheckedNull(), provided)
			);
			Object nmsRequired = Unsafe.assertNonNull(
					obcAsNMSCopyMethod.invoke(Unsafe.uncheckedNull(), required)
			);
			Object providedTag = nmsGetTagMethod.invoke(nmsProvided);
			Object requiredTag = nmsGetTagMethod.invoke(nmsRequired);
			return Unsafe.castNonNull(nmsAreNBTMatchingMethod.invoke(
					Unsafe.uncheckedNull(),
					Unsafe.nullableAsNonNull(requiredTag),
					Unsafe.nullableAsNonNull(providedTag),
					false
			));
		} catch (Exception e) {
			// Fallback: Check for metadata equality. In this case the behavior of this method is no
			// longer equivalent to Minecraft's item comparison behavior!
			// The direction of this check is important, because the required item stack might be an
			// UnmodifiableItemStack.
			return required.isSimilar(provided);
		}
	}

	@Override
	public void updateTrades(Player player) {
		// Not supported.
	}

	@Override
	public @Nullable String getItemSNBT(ItemStack itemStack) {
		return null; // Not supported.
	}

	@Override
	public @Nullable String getItemTypeTranslationKey(Material material) {
		return null; // Not supported.
	}

	// MC 1.20.4 specific features

	@Override
	public Cat.@Nullable Type getCatType(String typeName) {
		@Nullable Registry<@NonNull ?> catTypeRegistry = this.catTypeRegistry;
		if (catTypeRegistry == null) return null; // Not supported

		@Nullable NamespacedKey key = NamespacedKeyUtils.parse(typeName);
		if (key == null) return null; // Not recognized

		return Unsafe.cast(catTypeRegistry.get(key)); // Null if not found
	}

	@Override
	public String cycleCatType(String typeName, boolean backwards) {
		@Nullable Registry<@NonNull ?> catTypeRegistry = this.catTypeRegistry;
		if (catTypeRegistry == null) return typeName; // Not supported

		Cat.@Nullable Type catType = this.getCatType(typeName);
		if (catType == null) return typeName; // Current value not found

		Keyed next = Unsafe.castNonNull(RegistryUtils.cycleKeyed(
				catTypeRegistry,
				Unsafe.castNonNull(catType),
				backwards
		));
		return next.getKey().toString();
	}
}
