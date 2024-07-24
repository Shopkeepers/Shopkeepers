package com.nisovin.shopkeepers.compat.api;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.compat.CompatVersion;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.KeyedSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

import java.lang.reflect.Field;

public interface NMSCallProvider {

	// The compat version string.
	public String getVersionId();

	// This does not return null.
	public default CompatVersion getCompatVersion() {
		CompatVersion compatVersion = NMSManager.getCompatVersion(this.getVersionId());
		// Not finding the compat version indicates a bug.
		return Validate.State.notNull(compatVersion, "Could not find CompatVersion for '"
				+ this.getVersionId() + "'!");
	}

	public void overwriteLivingEntityAI(LivingEntity entity);

	// Whether tickAI and getCollisionDistance are supported.
	public default boolean supportsCustomMobAI() {
		return true;
	}

	public void tickAI(LivingEntity entity, int ticks);

	public void setOnGround(Entity entity, boolean onGround);

	// On some MC versions (e.g. MC 1.9, 1.10) NoAI only disables AI.
	public default boolean isNoAIDisablingGravity() {
		return true;
	}

	public void setNoclip(Entity entity);

	// Performs any version-specific setup that needs to happen before the entity is spawned. The
	// available operations may be limited during this phase of the entity spawning.
	public default void prepareEntity(Entity entity) {
	}

	// Performs any version-specific setup of the entity that needs to happen right after the entity
	// was spawned.
	public default void setupSpawnedEntity(Entity entity) {
	}

	public default boolean matches(
			@ReadOnly @Nullable ItemStack provided,
			@Nullable UnmodifiableItemStack required
	) {
		return this.matches(provided, ItemUtils.asItemStackOrNull(required));
	}

	/**
	 * Checks if the <code>provided</code> item stack fulfills the requirements of a trading recipe
	 * requiring the given <code>required</code> item stack.
	 * <p>
	 * This mimics Minecraft's item comparison: This checks if the item stacks are either both
	 * empty, or of same type and the provided item stack's metadata contains all the contents of
	 * the required item stack's metadata (with any list metadata being equal).
	 *
	 * @param provided
	 *            the provided item stack
	 * @param required
	 *            the required item stack, this may be an unmodifiable item stack
	 * @return <code>true</code> if the provided item stack matches the required item stack
	 */
	public boolean matches(
			@ReadOnly @Nullable ItemStack provided,
			@ReadOnly @Nullable ItemStack required
	);

	// Note: It is not safe to reduce the number of trading recipes! Reducing the size below the
	// selected index can crash the client. It's left to the caller to ensure that the number of
	// recipes does not get reduced, for example by inserting dummy entries.
	public void updateTrades(Player player);

	// For use in chat hover messages, null if not supported.
	public @Nullable String getItemSNBT(@ReadOnly ItemStack itemStack);

	// For use in translatable item type names, null if not supported.
	// Note: This might not necessarily match the name that is usually displayed for an ItemStack,
	// but rather the translated item type name (for example for items such as different types of
	// potions, skulls, etc.).
	public @Nullable String getItemTypeTranslationKey(Material material);

	// MC 1.17 specific features
	// TODO Remove this once we only support MC 1.17 and above.

	public default void setAxolotlVariant(LivingEntity axolotl, String variantName) {
		// Not supported by default.
	}

	public default String cycleAxolotlVariant(String variantName, boolean backwards) {
		// Not supported by default.
		return variantName;
	}

	public default void setGlowSquidDark(LivingEntity glowSquid, boolean dark) {
		// Not supported by default.
	}

	public default void setScreamingGoat(LivingEntity goat, boolean screaming) {
		// Not supported by default.
	}

	public default void setGlowingText(Sign sign, boolean glowingText) {
		// Not supported by default.
	}

	// MC 1.19 specific features
	// TODO Remove this once we only support MC 1.19 and above.

	public default void setFrogVariant(LivingEntity frog, String variantName) {
		// Not supported by default.
	}

	public default String cycleFrogVariant(String variantName, boolean backwards) {
		// Not supported by default.
		return variantName;
	}

	public default void setGoatLeftHorn(LivingEntity goat, boolean hasLeftHorn) {
		// Not supported by default.
	}

	public default void setGoatRightHorn(LivingEntity goat, boolean hasRightHorn) {
		// Not supported by default.
	}

	// MC 1.20 specific features
	// TODO Remove this once we only support MC 1.20 and above.

	public default void setSignBackLines(Sign sign, @NonNull String[] lines) {
		// Not supported by default.
	}

	public default void setSignBackGlowingText(Sign sign, boolean glowingText) {
		// Not supported by default.
	}

	// MC 1.20.3 specific features
	// TODO Remove this once we only support MC 1.20.3 and above.

	public default DataSerializer<Cat.@NonNull Type> getCatTypeSerializer() {
		try {
			// This is only supported on MC 1.20.3 and above. (for "compatibility mode")
			Class<?> keyedType = Class.forName(Cat.Type.class.getName());
			Class<?> registryClass = Class.forName(Registry.class.getName());
			Field catVariantField = registryClass.getField("CAT_VARIANT");
			Object catVariantRegistry = catVariantField.get(null);
			return (DataSerializer<Cat.Type>) (Object) KeyedSerializers.forRegistry((Class<Keyed>)keyedType, (Registry<Keyed>) catVariantRegistry);
		} catch (Throwable ex) {
			// Not supported by default.
			return null;
		}
	}

	public default Cat.Type cycleCatType(Cat.Type type, boolean backwards) {
		// Not supported by default.
		return type;
	}

	// MC 1.20.5 specific features
	// TODO Remove this once we only support MC 1.20.5 and above.

	public default void setMaxStackSize(@ReadWrite ItemMeta itemMeta, @Nullable Integer maxStackSize) {
		// Not supported by default.
	}

	public default NamespacedKey cycleWolfVariant(NamespacedKey variantKey, boolean backwards) {
		// Not supported by default.
		return variantKey;
	}

	public default void setWolfVariant(Wolf wolf, NamespacedKey variantKey) {
		// Not supported by default.
	}
}
