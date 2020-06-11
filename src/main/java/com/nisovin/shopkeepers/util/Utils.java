package com.nisovin.shopkeepers.util;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class Utils {

	private Utils() {
	}

	// note: doesn't work for primitive arrays
	@SafeVarargs
	public static <T> T[] concat(T[] array1, T... array2) {
		if (array1 == null) return array2;
		if (array2 == null) return array1;

		int length1 = array1.length;
		int length2 = array2.length;
		T[] result = Arrays.copyOf(array1, length1 + length2);
		System.arraycopy(array2, 0, result, length1, length2);
		return result;
	}

	public static <T> Stream<T> stream(Iterable<T> iterable) {
		if (iterable instanceof Collection) {
			return ((Collection<T>) iterable).stream();
		} else {
			return StreamSupport.stream(iterable.spliterator(), false);
		}
	}

	// Note: The returned Iterable can only be iterated once!
	public static <T> Iterable<T> toIterable(Stream<T> stream) {
		return stream::iterator;
	}

	public static <R> Stream<R> mapReferents(Stream<? extends Reference<R>> referenceStream) {
		return referenceStream.map(ref -> ref.get()).filter(Objects::nonNull);
	}

	public static void printRegisteredListeners(Event event) {
		HandlerList handlerList = event.getHandlers();
		Log.info("Registered listeners for event " + event.getEventName() + ":");
		for (RegisteredListener rl : handlerList.getRegisteredListeners()) {
			Log.info(" - " + rl.getPlugin().getName() + " (" + rl.getListener().getClass().getName() + ")"
					+ ", priority: " + rl.getPriority() + ", ignoring cancelled: " + rl.isIgnoringCancelled());
		}
	}

	/**
	 * Checks if the player can interact with the given block.
	 * <p>
	 * This works by clearing the player's items in main and off hand, calling a dummy PlayerInteractEvent for plugins
	 * to react to and then restoring the player's items in main and off hand.
	 * <p>
	 * Since this involves calling a dummy PlayerInteractEvent, plugins reacting to the event might cause all kinds of
	 * side effects. Therefore, this should only be used in very specific situations, such as for specific blocks.
	 * 
	 * @param player
	 *            the player
	 * @param block
	 *            the block to check interaction with
	 * @return <code>true</code> if no plugin denied block interaction
	 */
	public static boolean checkBlockInteract(Player player, Block block) {
		// simulating right click on the block to check if access is denied:
		// making sure that block access is really denied, and that the event is not cancelled because of denying
		// usage with the items in hands:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		ItemStack itemInOffHand = playerInventory.getItemInOffHand();
		playerInventory.setItemInMainHand(null);
		playerInventory.setItemInOffHand(null);

		TestPlayerInteractEvent dummyInteractEvent = new TestPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP);
		Bukkit.getPluginManager().callEvent(dummyInteractEvent);
		boolean canAccessBlock = (dummyInteractEvent.useInteractedBlock() != Result.DENY);

		// resetting items in main and off hand:
		playerInventory.setItemInMainHand(itemInMainHand);
		playerInventory.setItemInOffHand(itemInOffHand);
		return canAccessBlock;
	}

	/**
	 * Checks if the player can interact with the given entity.
	 * <p>
	 * This works by clearing the player's items in main and off hand, calling a dummy PlayerInteractEntityEvent for
	 * plugins to react to and then restoring the player's items in main and off hand.
	 * <p>
	 * Since this involves calling a dummy PlayerInteractEntityEvent, plugins reacting to the event might cause all
	 * kinds of side effects. Therefore, this should only be used in very specific situations, such as for specific
	 * entities, and its usage should be optional (i.e. guarded by a config setting).
	 * 
	 * @param player
	 *            the player
	 * @param entity
	 *            the entity to check interaction with
	 * @return <code>true</code> if no plugin denied interaction
	 */
	public static boolean checkEntityInteract(Player player, Entity entity) {
		// simulating right click on the entity to check if access is denied:
		// making sure that entity access is really denied, and that the event is not cancelled because of denying usage
		// with the items in hands:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		ItemStack itemInOffHand = playerInventory.getItemInOffHand();
		playerInventory.setItemInMainHand(null);
		playerInventory.setItemInOffHand(null);

		TestPlayerInteractEntityEvent dummyInteractEvent = new TestPlayerInteractEntityEvent(player, entity);
		Bukkit.getPluginManager().callEvent(dummyInteractEvent);
		boolean canAccessEntity = !dummyInteractEvent.isCancelled();

		// resetting items in main and off hand:
		playerInventory.setItemInMainHand(itemInMainHand);
		playerInventory.setItemInOffHand(itemInOffHand);
		return canAccessEntity;
	}

	public static String getServerCBVersion() {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		String cbVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		return cbVersion;
	}

	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS;
	static {
		Map<Class<?>, Class<?>> primitiveWrappers = new HashMap<>();
		primitiveWrappers.put(boolean.class, Boolean.class);
		primitiveWrappers.put(byte.class, Byte.class);
		primitiveWrappers.put(char.class, Character.class);
		primitiveWrappers.put(double.class, Double.class);
		primitiveWrappers.put(float.class, Float.class);
		primitiveWrappers.put(int.class, Integer.class);
		primitiveWrappers.put(long.class, Long.class);
		primitiveWrappers.put(short.class, Short.class);
		PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(primitiveWrappers);
	}

	public static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
		Validate.isTrue(primitive.isPrimitive(), "Second argument has to be a primitive!");
		return (PRIMITIVE_WRAPPERS.get(primitive) == targetClass);
	}

	public static boolean isAssignableFrom(Class<?> to, Class<?> from) {
		if (to.isAssignableFrom(from)) {
			return true;
		}
		if (to.isPrimitive()) {
			return isPrimitiveWrapperOf(from, to);
		}
		if (from.isPrimitive()) {
			return isPrimitiveWrapperOf(to, from);
		}
		return false;
	}

	/**
	 * Checks if the given locations represent the same world and coordinates (ignores pitch and yaw).
	 * 
	 * @param location1
	 *            location 1
	 * @param location2
	 *            location 2
	 * @return <code>true</code> if the locations correspond to the same position
	 */
	public static boolean isEqualPosition(Location location1, Location location2) {
		if (location1 == location2) return true; // also handles both being null
		if (location1 == null || location2 == null) return false;
		if (!Objects.equals(location1.getWorld(), location2.getWorld())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getX()) != Double.doubleToLongBits(location2.getX())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getY()) != Double.doubleToLongBits(location2.getY())) {
			return false;
		}
		if (Double.doubleToLongBits(location1.getZ()) != Double.doubleToLongBits(location2.getZ())) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the squared distance between the given location.
	 * <p>
	 * Both locations are required to have a valid (non-<code>null</code>) world. If the locations are located in
	 * different worlds, this returns {@link Double#MAX_VALUE}.
	 * 
	 * @param location1
	 *            the first location, not <code>null</code>
	 * @param location2
	 *            the second location, not <code>null</code>
	 * @return the squared distance
	 */
	public static double getDistanceSquared(Location location1, Location location2) {
		Validate.notNull(location1, "First location is null!");
		Validate.notNull(location2, "Second location is null!");
		World world1 = location1.getWorld();
		World world2 = location2.getWorld();
		Validate.notNull(world1, "World of first location is null!");
		Validate.notNull(world2, "World of second location is null!");
		if (world1 != world2) return Double.MAX_VALUE; // different worlds
		// Note: Not using Location#distanceSquared to avoid redundant precondition checks.
		double dx = location1.getX() - location2.getX();
		double dy = location1.getY() - location2.getY();
		double dz = location1.getZ() - location2.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Gets the block's center location.
	 * 
	 * @param block
	 *            the block
	 * @return the block's center location
	 */
	public static Location getBlockCenterLocation(Block block) {
		Validate.notNull(block, "Block is null!");
		return block.getLocation().add(0.5D, 0.5D, 0.5D);
	}

	// temporary objects getting re-used during ray tracing:
	private static final Location TEMP_START_LOCATION = new Location(null, 0, 0, 0);
	private static final Vector TEMP_START_POSITION = new Vector();
	private static final Vector DOWN_DIRECTION = new Vector(0.0D, -1.0D, 0.0D);
	private static final double RAY_TRACE_OFFSET = 0.01D;

	/**
	 * Get the distance to the nearest block collision in the range of the given <code>maxDistance</code>.
	 * <p>
	 * This performs a ray trace through the blocks' collision boxes, ignoring fluids and passable blocks.
	 * <p>
	 * The ray tracing gets slightly offset (by <code>0.01</code>) in order to make sure that we don't miss any block
	 * directly at the start location. If this results in a hit above the start location, we ignore it and return
	 * <code>0.0</code>.
	 * 
	 * @param startLocation
	 *            the start location, has to use a valid world, does not get modified
	 * @param maxDistance
	 *            the max distance to check for block collisions, has to be positive
	 * @return the distance to the ground, or <code>maxDistance</code> if there are no block collisions within the
	 *         specified range
	 */
	public static double getCollisionDistanceToGround(Location startLocation, double maxDistance) {
		World world = startLocation.getWorld();
		assert world != null;
		// setup re-used offset start location:
		TEMP_START_LOCATION.setWorld(world);
		TEMP_START_LOCATION.setX(startLocation.getX());
		TEMP_START_LOCATION.setY(startLocation.getY() + RAY_TRACE_OFFSET);
		TEMP_START_LOCATION.setZ(startLocation.getZ());

		// considers block collision boxes, ignoring fluids and passable blocks:
		RayTraceResult rayTraceResult = world.rayTraceBlocks(TEMP_START_LOCATION, DOWN_DIRECTION, maxDistance + RAY_TRACE_OFFSET, FluidCollisionMode.NEVER, true);
		TEMP_START_LOCATION.setWorld(null); // cleanup temporarily used start location

		double distanceToGround;
		if (rayTraceResult == null) {
			// no collision with the range:
			distanceToGround = maxDistance;
		} else {
			TEMP_START_POSITION.setX(TEMP_START_LOCATION.getX());
			TEMP_START_POSITION.setY(TEMP_START_LOCATION.getY());
			TEMP_START_POSITION.setZ(TEMP_START_LOCATION.getZ());
			distanceToGround = TEMP_START_POSITION.distance(rayTraceResult.getHitPosition()) - RAY_TRACE_OFFSET;
			// might be negative if the hit is between the start location and the offset start location, we ignore it
			// then:
			if (distanceToGround < 0.0D) distanceToGround = 0.0D;
		}
		return distanceToGround;
	}
}
