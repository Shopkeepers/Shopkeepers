package com.nisovin.shopkeepers.shopobjects.entity.base;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An {@link AbstractEntityShopObjectType} whose entity shops inherit the default behaviors of
 * {@link BaseEntityShopObject}.
 *
 * @param <T>
 *            the shop object type
 */
public abstract class BaseEntityShopObjectType<T extends BaseEntityShopObject<?>>
		extends AbstractEntityShopObjectType<T> {

	// IDENTIFIERS

	protected static String getIdentifier(EntityType entityType) {
		assert entityType != null;
		return StringUtils.normalize(entityType.name());
	}

	// ALIASES

	// Deeply unmodifiable:
	private static final Map<? extends EntityType, ? extends List<? extends String>> ALIASES;

	static {
		Map<EntityType, List<? extends String>> aliases = new HashMap<>();
		aliases.put(EntityType.MOOSHROOM, prepareAliases(Arrays.asList(
				"mooshroom",
				"mushroom-cow"
		)));
		aliases.put(EntityType.SNOW_GOLEM, prepareAliases(Arrays.asList(
				"snow-golem",
				"snowman"
		)));
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	private static List<? extends String> prepareAliases(List<? extends String> aliases) {
		return Collections.unmodifiableList(StringUtils.normalize(aliases));
	}

	protected static List<? extends String> getAliasesFor(EntityType entityType) {
		Validate.notNull(entityType, "entityType is null");
		List<? extends String> aliases = ALIASES.get(entityType);
		if (aliases != null) {
			return aliases;
		}

		return Collections.emptyList();
	}

	// PERMISSIONS

	private static final String PERMISSION_PREFIX = "shopkeeper.entity.";
	private static final String PERMISSION_ALL_ENTITY_TYPES = PERMISSION_PREFIX + "*";

	protected static String getPermission(EntityType entityType) {
		assert entityType != null;
		String typeName = entityType.name().toLowerCase(Locale.ROOT);
		String permission = PERMISSION_PREFIX + typeName;
		return permission;
	}

	@FunctionalInterface
	public static interface ShopObjectConstructor<T extends BaseEntityShopObject<?>> {

		public @NonNull T create(
				BaseEntityShopObjectCreationContext context,
				BaseEntityShopObjectType<T> shopObjectType,
				AbstractShopkeeper shopkeeper,
				@Nullable ShopCreationData creationData
		);
	}

	// ----

	protected final BaseEntityShopObjectCreationContext shopCreationContext;
	protected final EntityType entityType;
	private final ShopObjectConstructor<T> shopObjectConstructor;

	protected BaseEntityShopObjectType(
			BaseEntityShopObjectCreationContext shopCreationContext,
			EntityType entityType,
			Class<@NonNull T> shopObjectType,
			ShopObjectConstructor<T> shopObjectConstructor
	) {
		this(
				shopCreationContext,
				entityType,
				getIdentifier(entityType),
				getAliasesFor(entityType),
				getPermission(entityType),
				shopObjectType,
				shopObjectConstructor
		);
	}

	protected BaseEntityShopObjectType(
			BaseEntityShopObjectCreationContext shopCreationContext,
			EntityType entityType,
			String identifier,
			List<? extends String> aliases,
			String permission,
			Class<@NonNull T> shopObjectType,
			ShopObjectConstructor<T> shopObjectConstructor
	) {
		super(identifier, aliases, permission, shopObjectType);
		Validate.notNull(shopCreationContext, "shopCreationContext is null");
		Validate.isTrue(entityType.isSpawnable(), "entityType is not spawnable");
		Validate.notNull(shopObjectConstructor, "shopObjectConstructor is null");
		this.shopCreationContext = shopCreationContext;
		this.entityType = entityType;
		this.shopObjectConstructor = shopObjectConstructor;
	}

	public final EntityType getEntityType() {
		return entityType;
	}

	@Override
	public abstract boolean isEnabled();

	@Override
	public boolean hasPermission(Player player) {
		return PermissionUtils.hasPermission(player, PERMISSION_ALL_ENTITY_TYPES)
				|| super.hasPermission(player);
	}

	private Permission createPermission() {
		String permission = Unsafe.assertNonNull(this.getPermission());
		String description = "Create shopkeepers of the specific entity type";
		return new Permission(permission, description, PermissionDefault.FALSE);
	}

	/**
	 * {@link PluginManager#addPermission(Permission) Registers} the permission of this shop object
	 * type, if it is not already registered.
	 */
	public void registerPermission() {
		String permission = this.getPermission();
		if (permission == null) {
			return;
		}

		PermissionUtils.registerPermission(permission, node -> this.createPermission());
	}

	@Override
	public String getDisplayName() {
		// TODO Translation support for the entity type name?
		return StringUtils.replaceArguments(Messages.shopObjectTypeEntity,
				"type", StringUtils.normalize(entityType.name())
		);
	}

	@Override
	public boolean mustBeSpawned() {
		return true; // Despawn entities on chunk unload, and spawn them again on chunk load.
	}

	@Override
	public boolean mustDespawnDuringWorldSave() {
		// Spawned entities are non-persistent and therefore already skipped during world saves:
		return false;
	}

	protected boolean isDownValidAttachedBlockFace() {
		switch (entityType) {
		case SHULKER:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player player,
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		if (!super.validateSpawnLocation(player, spawnLocation, attachedBlockFace)) {
			return false;
		}
		assert spawnLocation != null;

		World world = Unsafe.assertNonNull(spawnLocation.getWorld());

		// Check if the world's difficulty would prevent the mob from spawning:
		if (EntityUtils.isRemovedOnPeacefulDifficulty(entityType)) {
			if (world.getDifficulty() == Difficulty.PEACEFUL) {
				if (player != null) {
					TextUtils.sendMessage(player, Messages.mobCannotSpawnOnPeacefulDifficulty);
				}
				return false;
			}
		}

		if (entityType == EntityType.END_CRYSTAL
				&& !Settings.allowEndCrystalShopsInTheEnd
				&& world.getEnvironment() == Environment.THE_END) {
			if (player != null) {
				TextUtils.sendMessage(player, Messages.endCrystalDisabledInTheEnd);
			}
			return false;
		}

		if ((attachedBlockFace == BlockFace.DOWN && !this.isDownValidAttachedBlockFace())
				|| (attachedBlockFace != null && !BlockFaceUtils.isBlockSide(attachedBlockFace))) {
			if (player != null) {
				TextUtils.sendMessage(player, Messages.invalidSpawnBlockFace);
			}
			return false;
		}

		Block spawnBlock = spawnLocation.getBlock();
		// Note: We don't require a fully empty block for shulkers either, since shulkers can be
		// placed on non-empty blocks just fine (the block is preserved).
		if (!spawnBlock.isPassable()) {
			if (player != null) {
				TextUtils.sendMessage(player, Messages.spawnBlockNotEmpty);
			}
			return false;
		}

		if (!EntityUtils.canFly(entityType) && entityType != EntityType.SHULKER) {
			Location standingLocation = EntityUtils.getStandingLocation(entityType, spawnBlock);
			if (standingLocation == null) {
				if (player != null) {
					TextUtils.sendMessage(player, Messages.cannotSpawnMidair);
				}
				return false;
			}
		}

		return true;
	}

	@Override
	public final T createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		T shopObject = shopObjectConstructor.create(
				shopCreationContext,
				this,
				shopkeeper,
				creationData
		);
		Validate.State.notNull(shopObject, () -> "BaseEntityShopObjectType for entity type '"
				+ entityType + "' created null shop object!");
		assert shopObject != null;
		return shopObject;
	}
}
