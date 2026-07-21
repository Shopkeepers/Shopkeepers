package com.nisovin.shopkeepers.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.container.DefaultShopContainerTypes;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.value.DataValue;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class SKShopContainer implements ShopContainer {

	// Immutable:
	private final BlockLocation location;
	private final ShopContainerType type;

	public SKShopContainer(BlockLocation location, ShopContainerType type) {
		Validate.notNull(location, "location is null");
		Validate.isTrue(location.hasWorldName(), "location has no worldName");
		Validate.notNull(type, "type is null");
		this.location = location.immutable();
		this.type = type;
	}

	/**
	 * Gets the container's {@link BlockLocation}.
	 * 
	 * @return the immutable block location, not <code>null</code>
	 */
	public BlockLocation getLocation() {
		return location;
	}

	@Override
	public String getWorldName() {
		return Unsafe.assertNonNull(location.getWorldName());
	}

	@Override
	public int getX() {
		return location.getX();
	}

	@Override
	public int getY() {
		return location.getY();
	}

	@Override
	public int getZ() {
		return location.getZ();
	}

	@Override
	public @Nullable Block getBlock() {
		return location.getBlock();
	}

	@Override
	public @Nullable Inventory getInventory() {
		Block block = this.getBlock();
		if (block == null) return null;
		if (!ShopContainers.isSupportedContainer(block.getType())) return null;

		return ShopContainers.getInventory(block); // Not null
	}

	@Override
	public boolean openInventoryView(Player player) {
		// Check if the container still exists:
		Inventory containerInventory = this.getInventory();
		if (containerInventory == null) {
			Log.debug(() -> "Cannot open container inventory for player '" + player.getName()
					+ "': The block is no longer a valid container!");
			return false;
		}

		Log.debug(() -> "Opening container inventory for player '" + player.getName() + "'.");
		// Open the container directly for the player (no need for a custom UI):
		return player.openInventory(containerInventory) != null;
	}

	@Override
	public ShopContainerType getType() {
		// If player shops can only use a single container, that container (and any other previously
		// added containers) always serves as both the stock and the earnings container, regardless
		// of its previously stored type.
		// Note: Doing this here instead of during loading ensures that this also works on dynamic
		// config updates.
		if (Settings.maxContainersPerPlayerShop <= 1) {
			return DefaultShopContainerTypes.STOCK_AND_EARNINGS();
		}

		return type;
	}

	/**
	 * Creates a copy of this container with the given {@link ShopContainerType type}.
	 * 
	 * @param type
	 *            the container type, not <code>null</code>
	 * @return the new container
	 */
	public SKShopContainer withType(ShopContainerType type) {
		return new SKShopContainer(location, type);
	}

	@Override
	public String toString() {
		return "SKShopContainer[location=" + location + ", type=" + type + "]";
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	private static final Property<String> WORLD = new BasicProperty<String>()
			.dataKeyAccessor("world", StringSerializers.STRICT_NON_EMPTY)
			.build();
	private static final Property<Integer> X = new BasicProperty<Integer>()
			.dataKeyAccessor("x", NumberSerializers.INTEGER)
			.build();
	private static final Property<Integer> Y = new BasicProperty<Integer>()
			.dataKeyAccessor("y", NumberSerializers.INTEGER)
			.build();
	private static final Property<Integer> Z = new BasicProperty<Integer>()
			.dataKeyAccessor("z", NumberSerializers.INTEGER)
			.build();
	private static final Property<ShopContainerType> TYPE = new BasicProperty<ShopContainerType>()
			.dataKeyAccessor("type", new DataSerializer<ShopContainerType>() {
				@Override
				public @Nullable Object serialize(ShopContainerType value) {
					Validate.notNull(value, "value is null");
					return value.getIdentifier();
				}

				@Override
				public ShopContainerType deserialize(Object data) throws InvalidDataException {
					String typeId = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
					var registry = SKShopkeepersPlugin.getInstance().getShopContainerTypeRegistry();
					var type = registry.get(typeId);
					if (type == null) {
						throw new InvalidDataException("Unknown shop container type: " + typeId);
					}
					return type;
				}
			})
			.build();

	/**
	 * A {@link DataSerializer} for values of type {@link ShopContainer}.
	 */
	public static final DataSerializer<ShopContainer> SERIALIZER = new DataSerializer<ShopContainer>() {
		@Override
		public @Nullable Object serialize(ShopContainer value) {
			Validate.notNull(value, "value is null");
			DataContainer containerData = DataContainer.create();
			containerData.set(WORLD, value.getWorldName());
			containerData.set(X, value.getX());
			containerData.set(Y, value.getY());
			containerData.set(Z, value.getZ());
			containerData.set(TYPE, value.getType());
			return containerData.serialize();
		}

		@Override
		public ShopContainer deserialize(Object data) throws InvalidDataException {
			DataContainer containerData = DataContainerSerializers.DEFAULT.deserialize(data);
			try {
				String worldName = containerData.get(WORLD);
				int x = containerData.get(X);
				int y = containerData.get(Y);
				int z = containerData.get(Z);
				ShopContainerType type = containerData.get(TYPE);

				return new SKShopContainer(new BlockLocation(worldName, x, y, z), type);
			} catch (MissingDataException e) {
				throw new InvalidDataException(e.getMessage(), e);
			}
		}
	};

	/**
	 * A {@link DataSerializer} for lists of {@link ShopContainer}s.
	 * <p>
	 * All contained elements are expected to not be <code>null</code>.
	 */
	public static final DataSerializer<List<? extends ShopContainer>> LIST_SERIALIZER
			= new DataSerializer<List<? extends ShopContainer>>() {
				@Override
				public @Nullable Object serialize(@ReadOnly List<? extends ShopContainer> value) {
					Validate.notNull(value, "value is null");
					DataContainer containerListData = DataContainer.create();
					int id = 1;
					for (ShopContainer container : value) {
						Validate.notNull(container, "list of containers contains null");
						containerListData.set(String.valueOf(id), SERIALIZER.serialize(container));
						id++;
					}
					return containerListData.serialize();
				}

				@Override
				public List<? extends ShopContainer> deserialize(Object data)
						throws InvalidDataException {
					DataContainer containerListData = DataContainerSerializers.DEFAULT.deserialize(data);
					Set<? extends String> keys = containerListData.getKeys();
					List<ShopContainer> containers = new ArrayList<>(keys.size());
					for (String id : keys) {
						Object containerData = Unsafe.assertNonNull(containerListData.get(id));
						ShopContainer container;
						try {
							container = SERIALIZER.deserialize(containerData);
						} catch (InvalidDataException e) {
							throw new InvalidDataException(
									"Invalid shop container " + id + ": " + e.getMessage(),
									e
							);
						}

						// Check for duplicate containers:
						for (var otherContainer : containers) {
							if (otherContainer.getX() == container.getX()
									&& otherContainer.getY() == container.getY()
									&& otherContainer.getZ() == container.getZ()
									&& otherContainer.getWorldName().equals(container.getWorldName())) {
								throw new InvalidDataException(
										"Invalid shop container " + id + ": Duplicate container!"
								);
							}
						}

						containers.add(container);
					}
					return containers;
				}
			};

	public static void saveContainers(
			DataValue dataValue,
			@ReadOnly @Nullable List<? extends ShopContainer> containers
	) {
		Validate.notNull(dataValue, "dataValue is null");
		if (containers == null) {
			dataValue.clear();
			return;
		}

		Object containerListData = LIST_SERIALIZER.serialize(containers);
		dataValue.set(containerListData);
	}

	public static List<? extends ShopContainer> loadContainers(DataValue dataValue)
			throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");
		Object containerListData = dataValue.get();
		if (containerListData == null) {
			// No data. -> Return an empty list.
			return Collections.emptyList();
		}
		return LIST_SERIALIZER.deserialize(containerListData);
	}
}
