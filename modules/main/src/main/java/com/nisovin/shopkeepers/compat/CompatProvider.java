package com.nisovin.shopkeepers.compat;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.view.builder.InventoryViewBuilder;
import org.bukkit.profile.PlayerProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.inventory.ItemStackComponentsData;
import com.nisovin.shopkeepers.util.inventory.ItemStackMetaTag;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * The Shopkeepers plugin relies on functionality that is specific to the server version, e.g. to
 * support multiple Minecraft versions, and specific to the server implementation, such as internal
 * aspects of NMS and CraftBukkit that are not exposed via the Bukkit API. Additionally, there may
 * be differences between the Spigot and Paper server implementations and APIs.
 * <p>
 * {@link CompatProvider} hides these version and server implementation specific aspects behind an
 * interface that must be implemented for every supported combination of server implementation and
 * version.
 * <p>
 * Every implementation, except the fallback implementation, must reside in the package
 * {@code com.nisovin.shopkeepers.compat.v<version>}, be named {@code CompatProviderImpl}, and
 * provide a default constructor without parameters.
 */
public interface CompatProvider {

	/**
	 * This is invoked when the plugin is enabled, after the config and language file have been
	 * loaded.
	 */
	public default void onEnable() {
	}

	/**
	 * This is invoked when the plugin is disabled.
	 */
	public default void onDisable() {

	}

	// The compat version string.
	public String getVersionId();

	// This does not return null.
	public default CompatVersion getCompatVersion() {
		return getCompatVersion(this.getVersionId());
	}

	public static CompatVersion getCompatVersion(String versionId) {
		CompatVersion compatVersion = Compat.getCompatVersion(versionId);
		// Not finding the compat version indicates a bug.
		return Validate.State.notNull(compatVersion, "Could not find CompatVersion for '"
				+ versionId + "'!");
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

	// Note: Different API on Paper (Component instead of String).
	// Note: We cannot add the Spigot implementation as the default implementation here, since this
	// would break our Paper-API compilation check.
	public void setInventoryViewTitle(InventoryViewBuilder<?> builder, String title);

	// Note: It is not safe to reduce the number of trading recipes! Reducing the size below the
	// selected index can crash the client. It's left to the caller to ensure that the number of
	// recipes does not get reduced, for example by inserting dummy entries.
	public void updateTrades(Player player);

	public ItemStackMetaTag getItemStackMetaTag(@ReadOnly @Nullable ItemStack itemStack);

	public boolean matches(ItemStackMetaTag provided, ItemStackMetaTag required, boolean matchPartialLists);

	public @Nullable ItemStackComponentsData getItemStackComponentsData(@ReadOnly ItemStack itemStack);

	public ItemStack deserializeItemStack(
			int dataVersion,
			NamespacedKey id,
			int count,
			@Nullable ItemStackComponentsData componentsData
	);

	// Note: Different implementation on Paper.
	public <T extends Keyed> Registry<T> getRegistry(Class<T> clazz);

	// MC 1.21.9+ TODO Can be removed once we only support Bukkit 1.21.9+

	public default void setCopperGolemWeatherState(Golem golem, String weatherState) {
		// Not supported by default.
	}

	// -2 to disable weathering state changes (waxed).
	public default void setCopperGolemNextWeatheringTick(Golem golem, int tick) {
		// Not supported by default.
	}

	public default void setMannequinHideDescription(LivingEntity mannequin, boolean hideDescription) {
		// Not supported by default.
	}

	public default void setMannequinDescription(LivingEntity mannequin, @Nullable String description) {
		// Not supported by default.
	}

	public default void setMannequinMainHand(LivingEntity mannequin, MainHand mainHand) {
		// Not supported by default.
	}

	public default void setMannequinPose(LivingEntity mannequin, Pose pose) {
		// Not supported by default.
	}

	public default void setMannequinProfile(LivingEntity mannequin, @Nullable PlayerProfile profile) {
		// Not supported by default.
	}

	// MC 1.21.11+ TODO Can maybe be removed once we only support Bukkit 1.21.11+

	public default void setZombieNautilusVariant(LivingEntity zombieNautilus, NamespacedKey variant) {
		// Not supported by default.
	}

	public default NamespacedKey cycleZombieNautilusVariant(NamespacedKey variant, boolean backwards) {
		// Not supported by default.
		return variant;
	}
}
